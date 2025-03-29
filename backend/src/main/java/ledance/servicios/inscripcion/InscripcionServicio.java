package ledance.servicios.inscripcion;

import ledance.dto.asistencia.AsistenciaMensualMapper;
import ledance.dto.inscripcion.InscripcionMapper;
import ledance.dto.inscripcion.request.InscripcionRegistroRequest;
import ledance.dto.response.EstadisticasInscripcionResponse;
import ledance.dto.inscripcion.response.InscripcionResponse;
import ledance.entidades.*;
import ledance.infra.errores.TratadorDeErrores;
import ledance.repositorios.*;
import ledance.servicios.asistencia.AsistenciaDiariaServicio;
import ledance.servicios.asistencia.AsistenciaMensualServicio;
import ledance.servicios.matricula.MatriculaServicio;
import ledance.servicios.mensualidad.MensualidadServicio;
import ledance.servicios.pago.PagoServicio;
import ledance.servicios.pago.PaymentProcessor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import jakarta.transaction.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class InscripcionServicio implements IInscripcionServicio {

    private static final Logger log = LoggerFactory.getLogger(InscripcionServicio.class);

    private final InscripcionRepositorio inscripcionRepositorio;
    private final AlumnoRepositorio alumnoRepositorio;
    private final DisciplinaRepositorio disciplinaRepositorio;
    private final BonificacionRepositorio bonificacionRepositorio;
    private final InscripcionMapper inscripcionMapper;
    private final AsistenciaMensualServicio asistenciaMensualServicio;
    private final MensualidadServicio mensualidadServicio;
    private final AsistenciaAlumnoMensualRepositorio asistenciaAlumnoMensualRepositorio;
    private final MatriculaServicio matriculaServicio;
    private final PaymentProcessor paymentProcessor;
    private final PagoRepositorio pagoRepositorio;
    private final AsistenciaDiariaServicio asistenciaDiariaServicio;

    public InscripcionServicio(InscripcionRepositorio inscripcionRepositorio,
                               AlumnoRepositorio alumnoRepositorio,
                               DisciplinaRepositorio disciplinaRepositorio,
                               BonificacionRepositorio bonificacionRepositorio,
                               InscripcionMapper inscripcionMapper,
                               AsistenciaMensualRepositorio asistenciaMensualRepositorio,
                               AsistenciaMensualServicio asistenciaMensualServicio,
                               MensualidadServicio mensualidadServicio,
                               AsistenciaMensualMapper asistenciaMensualMapper,
                               AsistenciaAlumnoMensualRepositorio asistenciaAlumnoMensualRepositorio, MatriculaServicio matriculaServicio, PaymentProcessor paymentProcessor, PagoServicio pagoServicio, PagoRepositorio pagoRepositorio, AsistenciaDiariaServicio asistenciaDiariaServicio) {
        this.inscripcionRepositorio = inscripcionRepositorio;
        this.alumnoRepositorio = alumnoRepositorio;
        this.disciplinaRepositorio = disciplinaRepositorio;
        this.bonificacionRepositorio = bonificacionRepositorio;
        this.inscripcionMapper = inscripcionMapper;
        this.asistenciaMensualServicio = asistenciaMensualServicio;
        this.mensualidadServicio = mensualidadServicio;
        this.asistenciaAlumnoMensualRepositorio = asistenciaAlumnoMensualRepositorio;
        this.matriculaServicio = matriculaServicio;
        this.paymentProcessor = paymentProcessor;
        this.pagoRepositorio = pagoRepositorio;
        this.asistenciaDiariaServicio = asistenciaDiariaServicio;
    }

    /**
     * Registrar una nueva inscripcion.
     * Se genera automaticamente una cuotaOCantidad para el mes vigente y se incorpora al alumno a la planilla de asistencia (por disciplina).
     */
    @Transactional
    public InscripcionResponse crearInscripcion(InscripcionRegistroRequest request) {
        log.info("[crearInscripcion] Iniciando creacion de inscripcion para alumnoId={}, disciplinaId={}",
                request.alumno().id(), request.disciplina().id());

        // 1. Recuperar y validar alumno y disciplina
        Alumno alumno = alumnoRepositorio.findById(request.alumno().id())
                .orElseThrow(() -> new IllegalArgumentException("Alumno no encontrado."));
        log.info("[crearInscripcion] Alumno encontrado: id={}, nombre={}", alumno.getId(), alumno.getNombre());

        Disciplina disciplina = disciplinaRepositorio.findById(request.disciplina().id())
                .orElseThrow(() -> new IllegalArgumentException("Disciplina no encontrada."));
        log.info("[crearInscripcion] Disciplina encontrada: id={}, nombre={}", disciplina.getId(), disciplina.getNombre());

        if (disciplina.getProfesor() == null) {
            log.warn("[crearInscripcion] La disciplina id={} no tiene profesor asignado.", disciplina.getId());
            throw new IllegalStateException("La disciplina indicada no tiene profesor asignado.");
        }

        // 2. Recuperar bonificacion (si corresponde)
        Bonificacion bonificacion = null;
        if (request.bonificacionId() != null) {
            bonificacion = bonificacionRepositorio.findById(request.bonificacionId()).orElse(null);
            if (bonificacion != null) {
                log.info("[crearInscripcion] Bonificacion encontrada: id={}, descripcion={}",
                        bonificacion.getId(), bonificacion.getDescripcion());
            } else {
                log.info("[crearInscripcion] No se encontro bonificacion con id={}", request.bonificacionId());
            }
        }

        // 3. Mapear y persistir la inscripcion
        Inscripcion inscripcion = inscripcionMapper.toEntity(request);
        inscripcion.setAlumno(alumno);
        inscripcion.setDisciplina(disciplina);
        inscripcion.setBonificacion(bonificacion);
        LocalDate fechaInscripcion = (request.fechaInscripcion() != null) ? request.fechaInscripcion() : LocalDate.now();
        inscripcion.setFechaInscripcion(fechaInscripcion);
        Inscripcion inscripcionGuardada = inscripcionRepositorio.save(inscripcion);
        log.info("[crearInscripcion] Inscripcion guardada con ID: {}", inscripcionGuardada.getId());

        // 4. Verificar y/o crear un Pago pendiente para el alumno
        Pago pagoPendiente = obtenerOCrearPagoPendiente(alumno, fechaInscripcion);
        pagoPendiente = pagoRepositorio.save(pagoPendiente); // Persistir el pago pendiente
        log.info("[crearInscripcion] Pago pendiente persistido con ID: {}", pagoPendiente.getId());

        // 5. Generar cuota automatica y gestionar matricula asociandolos al Pago pendiente
        try {
            // Generar cuota automatica para la inscripcion
            DetallePago mensualidad = mensualidadServicio.generarCuotaAutomatica(inscripcionGuardada, pagoPendiente);
            log.info("[crearInscripcion] Cuota automatica generada para inscripcion id={}", inscripcionGuardada.getId());
            pagoPendiente.getDetallePagos().add(mensualidad);
        } catch (Exception e) {
            log.warn("[crearInscripcion] Error al generar cuota automatica: {}", e.getMessage());
        }
        try {
            DetallePago matricula = matriculaServicio.obtenerOMarcarPendienteAutomatica(alumno.getId(), pagoPendiente);
            log.info("[crearInscripcion] Matricula verificada o creada automaticamente para alumno id={}", alumno.getId());

        } catch (Exception e) {
            log.warn("[crearInscripcion] Error al obtener o marcar matricula pendiente: {}", e.getMessage());
        }

        // 6. Agregar alumno a la planilla de asistencia
        int mesActual = LocalDate.now().getMonthValue();
        int anioActual = LocalDate.now().getYear();
        log.info("[crearInscripcion] Agregando alumno a planilla de asistencia para mes={}, año={}", mesActual, anioActual);
        asistenciaMensualServicio.agregarAlumnoAPlanilla(inscripcionGuardada.getId(), mesActual, anioActual);
        log.info("[crearInscripcion] Inscripcion finalizada exitosamente para alumno id={}", alumno.getId());

        // Actualizar totales del pago basandose en los DetallePago obtenidos
        paymentProcessor.recalcularTotales(pagoPendiente);
        pagoRepositorio.save(pagoPendiente);
        log.info("[crearInscripcion] Totales de Pago actualizados. Pago ID: {}", pagoPendiente.getId());

        return inscripcionMapper.toDTO(inscripcionGuardada);
    }

    /**
     * Verifica si ya existe un pago pendiente para el alumno.
     * Si no existe, crea uno nuevo asignando valores iniciales realistas.
     * Los montos y saldos se actualizaran al final del proceso.
     */
    private Pago obtenerOCrearPagoPendiente(Alumno alumno, LocalDate fecha) {
        Pago pagoPendiente = paymentProcessor.obtenerUltimoPagoPendienteEntidad(alumno.getId());
        if (pagoPendiente == null) {
            Pago nuevoPago = new Pago();
            nuevoPago.setFecha(fecha);
            // Asignar una fecha de vencimiento realista, por ejemplo 5 dias despues de la inscripcion
            nuevoPago.setFechaVencimiento(fecha.plusDays(30));
            nuevoPago.setAlumno(alumno);
            // Inicializacion de valores monetarios: estos se recalcularan mas adelante segun DetallePago
            nuevoPago.setMonto(0.0);
            nuevoPago.setValorBase(0.0);
            nuevoPago.setImporteInicial(0.0);
            nuevoPago.setMontoPagado(0.0);
            nuevoPago.setSaldoRestante(0.0);
            // Definir el estado y observaciones (opcional)
            nuevoPago.setEstadoPago(EstadoPago.ACTIVO);
            nuevoPago.setObservaciones("Pago pendiente creado automaticamente durante la inscripcion.");
            pagoPendiente = nuevoPago;
            log.info("[obtenerOCrearPagoPendiente] Nuevo pago creado para alumno id={} con ID={}", alumno.getId(), nuevoPago.getId());
        } else {
            log.info("[obtenerOCrearPagoPendiente] Pago pendiente existente encontrado para alumno id={} con ID={}", alumno.getId(), pagoPendiente.getId());
        }
        return pagoPendiente;
    }

    // Logica de calculo trasladada al servicio (sin utilizar el mapper)
    private Double calcularCosto(Inscripcion inscripcion) {
        Disciplina d = inscripcion.getDisciplina();
        double valorCuota = d.getValorCuota() != null ? d.getValorCuota() : 0.0;
        double claseSuelta = d.getClaseSuelta() != null ? d.getClaseSuelta() : 0.0;
        double clasePrueba = d.getClasePrueba() != null ? d.getClasePrueba() : 0.0;
        double total = valorCuota + claseSuelta + clasePrueba;
        if (inscripcion.getBonificacion() != null && inscripcion.getBonificacion().getPorcentajeDescuento() != null) {
            int descuento = inscripcion.getBonificacion().getPorcentajeDescuento();
            total = total * (100 - descuento) / 100.0;
        }
        return total;
    }

    @Override
    public InscripcionResponse obtenerPorId(Long id) {
        Inscripcion inscripcion = inscripcionRepositorio.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Inscripcion no encontrada."));
        return inscripcionMapper.toDTO(inscripcion);
    }

    @Override
    @Transactional
    public InscripcionResponse actualizarInscripcion(Long id, InscripcionRegistroRequest request) {
        log.info("Actualizando inscripcion con id: {}", id);
        Inscripcion inscripcion = inscripcionRepositorio.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Inscripcion no encontrada."));
        inscripcion = inscripcionMapper.updateEntityFromRequest(request, inscripcion);
        if (request.fechaBaja() != null) {
            inscripcion.setFechaBaja(request.fechaBaja());
            inscripcion.setEstado(EstadoInscripcion.BAJA);
        }
        Inscripcion actualizada = inscripcionRepositorio.save(inscripcion);
        return inscripcionMapper.toDTO(actualizada);
    }

    @Override
    public List<InscripcionResponse> listarPorDisciplina(Long disciplinaId) {
        List<Inscripcion> inscripciones = inscripcionRepositorio.findByDisciplinaId(disciplinaId);
        if (inscripciones.isEmpty()) {
            throw new IllegalArgumentException("No hay inscripciones en esta disciplina.");
        }
        return inscripciones.stream()
                .map(inscripcionMapper::toDTO)
                .collect(Collectors.toList());
    }

    @Override
    public List<InscripcionResponse> listarPorAlumno(Long alumnoId) {
        List<Inscripcion> inscripciones = inscripcionRepositorio.findAllByAlumno_IdAndEstado(alumnoId, EstadoInscripcion.ACTIVA);
        if (inscripciones.isEmpty()) {
            throw new IllegalArgumentException("No hay inscripciones para este alumno.");
        }
        return inscripciones.stream()
                .map(inscripcionMapper::toDTO)
                .collect(Collectors.toList());
    }

    @Transactional
    public void eliminarInscripcion(Long id) {
        Inscripcion inscripcion = inscripcionRepositorio.findById(id)
                .orElseThrow(() -> new TratadorDeErrores.RecursoNoEncontradoException("Inscripción no encontrada."));

        // Eliminar registros de AsistenciaAlumnoMensual asociados a la inscripción
        List<AsistenciaAlumnoMensual> asistencias = asistenciaAlumnoMensualRepositorio.findByInscripcionId(inscripcion.getId());
        if (asistencias != null && !asistencias.isEmpty()) {
            for (AsistenciaAlumnoMensual asistenciaAlumnoMensual : asistencias) {
                asistenciaDiariaServicio.eliminarAsistenciaAlumnoMensual(asistenciaAlumnoMensual.getId());
            }
        }

        // Forzar el sincronizado del contexto de persistencia
        inscripcionRepositorio.flush();

        // Eliminar la inscripción (las mensualidades se eliminarán en cascada si están configuradas)
        inscripcionRepositorio.delete(inscripcion);
    }

    @Transactional
    public void darBajaInscripcion(Long id) {
        Inscripcion inscripcion = inscripcionRepositorio.findById(id)
                .orElseThrow(() -> new TratadorDeErrores.RecursoNoEncontradoException("Inscripcion no encontrada."));
        inscripcion.setEstado(EstadoInscripcion.BAJA);
        inscripcionRepositorio.save(inscripcion);
    }

    @Transactional
    public List<InscripcionResponse> listarInscripciones() {
        return inscripcionRepositorio.findAllWithDetails().stream()
                .map(inscripcionMapper::toDTO)
                .collect(Collectors.toList());
    }

    @Transactional
    public List<InscripcionResponse> crearInscripcionesMasivas(List<InscripcionRegistroRequest> requests) {
        return requests.stream()
                .map(this::crearInscripcion)
                .collect(Collectors.toList());
    }

    public EstadisticasInscripcionResponse obtenerEstadisticas() {
        long totalInscripciones = inscripcionRepositorio.count();
        Map<String, Long> inscripcionesPorDisciplina = inscripcionRepositorio.countByDisciplinaGrouped().stream()
                .collect(Collectors.toMap(
                        arr -> (String) arr[0],
                        arr -> (Long) arr[1]
                ));
        Map<Integer, Long> inscripcionesPorMes = inscripcionRepositorio.countByMonthGrouped().stream()
                .collect(Collectors.toMap(
                        arr -> (Integer) arr[0],
                        arr -> (Long) arr[1]
                ));
        return new EstadisticasInscripcionResponse(totalInscripciones, inscripcionesPorDisciplina, inscripcionesPorMes);
    }

    @Transactional
    public InscripcionResponse obtenerInscripcionActiva(Long alumnoId) {
        Inscripcion inscripcion = inscripcionRepositorio
                .findFirstByAlumno_IdAndEstadoOrderByIdAsc(alumnoId, EstadoInscripcion.ACTIVA)
                .orElseThrow(() -> new IllegalArgumentException("Inscripcion activa no encontrada para el alumno " + alumnoId));
        return inscripcionMapper.toDTO(inscripcion);
    }

    // Ejemplo en el servicio de inscripciones
    private String obtenerEstadoMensualidad(Inscripcion inscripcion) {
        // Se asume que Inscripcion tiene una lista de mensualidades
        if (inscripcion.getMensualidades() != null && !inscripcion.getMensualidades().isEmpty()) {
            // Por ejemplo, se ordenan por fechaCuota descendente y se toma la primera
            Mensualidad ultima = inscripcion.getMensualidades().stream()
                    .sorted((m1, m2) -> m2.getFechaCuota().compareTo(m1.getFechaCuota()))
                    .findFirst().orElse(null);
            if (ultima != null) {
                return ultima.getEstado().name();
            }
        }
        return "Sin mensualidad"; // O el valor que consideres adecuado
    }

}

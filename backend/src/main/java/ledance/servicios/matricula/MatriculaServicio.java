package ledance.servicios.matricula;

import ledance.dto.matricula.MatriculaMapper;
import ledance.dto.matricula.request.MatriculaRegistroRequest;
import ledance.dto.matricula.response.MatriculaResponse;
import ledance.entidades.*;
import ledance.repositorios.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.Year;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Service
public class MatriculaServicio {

    private static final Logger log = LoggerFactory.getLogger(MatriculaServicio.class);

    private final MatriculaRepositorio matriculaRepositorio;
    private final AlumnoRepositorio alumnoRepositorio;
    private final MatriculaMapper matriculaMapper;
    private final DetallePagoRepositorio detallePagoRepositorio;
    private final ConceptoRepositorio conceptoRepositorio;
    private final ProcesoEjecutadoRepositorio procesoEjecutadoRepositorio;
    private final PagoRepositorio pagoRepositorio;

    public MatriculaServicio(MatriculaRepositorio matriculaRepositorio, AlumnoRepositorio alumnoRepositorio, MatriculaMapper matriculaMapper, DetallePagoRepositorio detallePagoRepositorio, ConceptoRepositorio conceptoRepositorio, ProcesoEjecutadoRepositorio procesoEjecutadoRepositorio, PagoRepositorio pagoRepositorio) {
        this.matriculaRepositorio = matriculaRepositorio;
        this.alumnoRepositorio = alumnoRepositorio;
        this.matriculaMapper = matriculaMapper;
        this.detallePagoRepositorio = detallePagoRepositorio;
        this.conceptoRepositorio = conceptoRepositorio;
        this.procesoEjecutadoRepositorio = procesoEjecutadoRepositorio;
        this.pagoRepositorio = pagoRepositorio;
    }

    @Transactional
    public void obtenerOMarcarPendienteAutomatica(Long alumnoId, Pago pagoPendiente) {
        int anio = LocalDate.now().getYear();
        log.info("[MatriculaAutoService] Procesando matrícula automática para alumno id={}, año={}", alumnoId, anio);

        // Obtiene o crea la matrícula pendiente para el alumno en el año indicado.
        Matricula matricula = obtenerOMarcarPendienteMatricula(alumnoId, anio);
        log.info("Matrícula pendiente: id={} para alumnoId={}", matricula.getId(), alumnoId);

        // Si ya existe un DetallePago asociado a la matrícula, se finaliza el proceso.
        Optional<DetallePago> detalleOpt = detallePagoRepositorio.findByMatriculaId(matricula.getId());
        if (detalleOpt.isPresent()) {
            log.info("DetallePago ya existe para la matrícula id={}", matricula.getId());
            return;
        }

        // Si no existe, se registra un nuevo DetallePago para la matrícula.
        registrarDetallePagoMatriculaAutomatica(matricula, pagoPendiente);
    }

    @Transactional
    public Matricula obtenerOMarcarPendienteMatricula(Long alumnoId, int anio) {
        // Busca la matrícula pendiente ya registrada para el alumno en el año indicado
        return matriculaRepositorio.findByAlumnoIdAndAnio(alumnoId, anio).orElseGet(() -> {
            Alumno alumno = alumnoRepositorio.findById(alumnoId).orElseThrow(() -> new IllegalArgumentException("Alumno no encontrado."));
            Matricula nuevaMatricula = new Matricula();
            nuevaMatricula.setAlumno(alumno);
            nuevaMatricula.setAnio(anio);
            nuevaMatricula.setPagada(false);
            nuevaMatricula = matriculaRepositorio.save(nuevaMatricula);
            matriculaRepositorio.flush(); // Asegura visibilidad en la transacción actual
            log.info("Nueva matrícula creada: id={} para alumnoId={}", nuevaMatricula.getId(), alumnoId);
            return nuevaMatricula;
        });
    }

    @Transactional
    protected void registrarDetallePagoMatriculaAutomatica(Matricula matricula, Pago pagoPendiente) {
        log.info("[registrarDetallePagoMatricula] Iniciando registro del DetallePago para matrícula id={}", matricula.getId());

        Alumno alumno = matricula.getAlumno();
        // Buscar el último pago pendiente para el alumno
        pagoPendiente = obtenerUltimoPagoPendienteEntidad(alumno.getId());
        Pago pagoAsociado;
        if (pagoPendiente != null) {
            pagoAsociado = pagoPendiente;
            log.info("[registrarDetallePagoMatricula] Se encontró un pago pendiente para alumno id={}: Pago id={}", alumno.getId(), pagoAsociado.getId());
        } else {
            // No se encontró un pago pendiente: se crea uno nuevo.
            pagoAsociado = new Pago();
            pagoAsociado.setAlumno(alumno);
            pagoAsociado.setFecha(LocalDate.now());
            // Asigna una fecha de vencimiento (por ejemplo, 30 días después)
            pagoAsociado.setFechaVencimiento(LocalDate.now().plusDays(30));
            // Inicializamos importes en 0 (o según tu lógica de negocio)
            pagoAsociado.setImporteInicial(0.0);
            pagoAsociado.setMonto(0.0);
            pagoAsociado.setSaldoRestante(0.0);
            pagoAsociado.setEstadoPago(EstadoPago.ACTIVO);
            // Si el pago debe tener otros campos (como observaciones o método de pago), asígnalos acá.
            pagoRepositorio.save(pagoAsociado);
            log.info("[registrarDetallePagoMatricula] No se encontró un pago pendiente; se creó un nuevo pago con ID={}", pagoAsociado.getId());
        }

        // Crear el nuevo DetallePago para la matrícula.
        DetallePago detalle = new DetallePago();
        // Se asigna la matrícula y el alumno
        detalle.setMatricula(matricula);
        detalle.setAlumno(alumno);

        // Asignar una descripción basada en el año actual
        int anio = LocalDate.now().getYear();
        String descripcionConcepto = "MATRICULA " + anio;
        detalle.setDescripcionConcepto(descripcionConcepto);

        // Tipo de detalle: MATRÍCULA
        detalle.setTipo(TipoDetallePago.MATRICULA);
        detalle.setFechaRegistro(LocalDate.now());

        // Verificar duplicidad (según tu lógica de negocio)
        if (verificarDetallePagoUnico(detalle)) {
            return;
        }

        // Asignar Concepto y SubConcepto (según tu lógica de negocio)
        asignarConceptoDetallePago(detalle);

        // Suponiendo que el precio del concepto determina el valor base
        Double valorBase = detalle.getConcepto().getPrecio();
        detalle.setValorBase(valorBase);
        detalle.setImporteInicial(valorBase);
        detalle.setImportePendiente(valorBase);
        detalle.setACobrar(0.0);
        detalle.setCobrado(false);

        // Asociar el pago al detalle
        detalle.setPago(pagoAsociado);

        // Guardar el DetallePago
        detallePagoRepositorio.save(detalle);
        log.info("[registrarDetallePagoMatricula] DetallePago para matrícula id={} creado y guardado exitosamente.", matricula.getId());

        // Actualizar el pago: agregar el detalle y sumar el importe
        if (pagoAsociado.getDetallePagos() == null) {
            pagoAsociado.setDetallePagos(new ArrayList<>());
        }
        pagoAsociado.getDetallePagos().add(detalle);
        double nuevoMonto = (pagoAsociado.getMonto() != null ? pagoAsociado.getMonto() : 0.0);
        pagoAsociado.setMonto(nuevoMonto);
        double nuevoSaldo = (pagoAsociado.getSaldoRestante() != null ? pagoAsociado.getSaldoRestante() : 0.0);
        pagoAsociado.setSaldoRestante(nuevoSaldo);
        pagoRepositorio.save(pagoAsociado);
        log.info("[registrarDetallePagoMatricula] Pago (ID={}) actualizado: nuevo monto={} y saldo restante={}",
                pagoAsociado.getId(), nuevoMonto, nuevoSaldo);
    }

    /**
     * Verifica que no exista un DetallePago duplicado para un alumno basado en la descripción y el tipo.
     * Si la descripción contiene "MATRICULA" y ya existe un registro, se lanza una excepción.
     */
    @Transactional
    public boolean verificarDetallePagoUnico(DetallePago detalle) {
        Long alumnoId = detalle.getAlumno().getId();
        String descripcion = detalle.getDescripcionConcepto();
        if (descripcion != null && descripcion.toUpperCase().contains("MATRICULA")) {
            log.info("[verificarDetallePagoUnico] Verificando duplicidad para alumnoId={} con descripción '{}'", alumnoId, descripcion);
            boolean existeDetalleDuplicado = detallePagoRepositorio.existsByAlumnoIdAndDescripcionConceptoIgnoreCaseAndTipo(alumnoId, descripcion, TipoDetallePago.MATRICULA);
            if (existeDetalleDuplicado) {
                log.error("[verificarDetallePagoUnico] DetallePago duplicado encontrado para alumnoId={} con descripción '{}'", alumnoId, descripcion);
                return true;
            }
        } else {
            log.info("[verificarDetallePagoUnico] No se requiere verificación de duplicidad para la descripción '{}'", descripcion);
        }
        return false;
    }

    /**
     * Asigna el Concepto y SubConcepto al DetallePago según la descripción "MATRICULA <anio>".
     */
    private void asignarConceptoDetallePago(DetallePago detalle) {
        int anioActual = Year.now().getValue();
        String descripcionConcepto = "MATRICULA " + anioActual;
        log.info("[asignarConceptoDetallePago] Buscando Concepto con descripción '{}'", descripcionConcepto);

        Optional<Concepto> optConcepto = conceptoRepositorio.findByDescripcionIgnoreCase(descripcionConcepto);
        if (optConcepto.isEmpty()) {
            log.error("[asignarConceptoDetallePago] No se encontró Concepto con descripción '{}' para DetallePago", descripcionConcepto);
            throw new IllegalStateException("No se encontró Concepto para Matrícula con descripción: " + descripcionConcepto);
        }
        Concepto concepto = optConcepto.get();
        detalle.setConcepto(concepto);
        detalle.setSubConcepto(concepto.getSubConcepto());
        log.info("[asignarConceptoDetallePago] Se asignaron Concepto: {} y SubConcepto: {} al DetallePago", detalle.getConcepto(), detalle.getSubConcepto());
    }

    @Transactional
    public void verificarMatriculaNoDuplicada(DetallePago detalle) {
        Long alumnoId = detalle.getAlumno().getId();
        String descripcion = detalle.getDescripcionConcepto();
        log.info("[verificarMatriculaNoDuplicada] Verificando existencia de matrícula para alumnoId={} con descripción '{}'", alumnoId, descripcion);

        // Solo se verifica si la descripción contiene "MATRICULA"
        if (descripcion != null && descripcion.toUpperCase().contains("MATRICULA")) {
            boolean existeDetalleActivo = detallePagoRepositorio.existsByAlumnoIdAndDescripcionConceptoIgnoreCaseAndTipoAndEstadoPagoNot(alumnoId, descripcion, TipoDetallePago.MATRICULA, EstadoPago.ANULADO);
            if (existeDetalleActivo) {
                log.error("[verificarMatriculaNoDuplicada] Ya existe un DetallePago activo (no anulado) con descripción '{}' para alumnoId={}", descripcion, alumnoId);
                throw new IllegalStateException("MATRICULA YA COBRADA");
            }
        } else {
            log.info("[verificarMatriculaNoDuplicada] La descripción '{}' no contiene 'MATRICULA', no se realiza verificación.", descripcion);
        }
    }

    // Metodo para actualizar el estado de la matricula (ya existente)
    @Transactional
    public MatriculaResponse actualizarEstadoMatricula(Long matriculaId, MatriculaRegistroRequest request) {
        Matricula m = matriculaRepositorio.findById(matriculaId).orElseThrow(() -> new IllegalArgumentException("Matricula no encontrada."));
        matriculaMapper.updateEntityFromRequest(request, m);
        return matriculaMapper.toResponse(matriculaRepositorio.save(m));
    }

    @Transactional
    public void generarMatriculasAnioVigente() {
        LocalDate today = LocalDate.now();
        int anioActual = today.getYear();

        ProcesoEjecutado proceso = procesoEjecutadoRepositorio.findByProceso("MATRICULA_AUTOMATICA").orElse(new ProcesoEjecutado("MATRICULA_AUTOMATICA", null));

        YearMonth mesActual = YearMonth.from(today);
        if (proceso.getUltimaEjecucion() != null && YearMonth.from(proceso.getUltimaEjecucion()).equals(mesActual)) {
            log.info("El proceso MATRÍCULA_AUTOMATICA ya fue ejecutado este mes: {}", proceso.getUltimaEjecucion());
            return;
        }

        List<Alumno> alumnosConInscripciones = alumnoRepositorio.findAll().stream().filter(alumno -> Boolean.TRUE.equals(alumno.getActivo()) && alumno.getInscripciones() != null && !alumno.getInscripciones().isEmpty()).toList();

        log.info("Total de alumnos con al menos una inscripción activa: {}", alumnosConInscripciones.size());

        for (Alumno alumno : alumnosConInscripciones) {
            log.info("Procesando alumno id: {} - {}", alumno.getId(), alumno.getNombre());

            Optional<Matricula> optMatricula = matriculaRepositorio.findByAlumnoIdAndAnio(alumno.getId(), anioActual);

            Matricula matricula;
            if (optMatricula.isPresent()) {
                matricula = optMatricula.get();
                log.info("Ya existe matrícula para el alumno id={}, matrícula id={}", alumno.getId(), matricula.getId());
            } else {
                matricula = new Matricula();
                matricula.setAlumno(alumno);
                matricula.setAnio(anioActual);
                matricula.setPagada(false);
                matricula = matriculaRepositorio.save(matricula);
                log.info("Nueva matrícula creada para alumno id={}, matrícula id={}", alumno.getId(), matricula.getId());
            }

            // Verificamos si ya hay un DetallePago asociado a la matrícula
            boolean detalleExiste = detallePagoRepositorio.existsByMatriculaId(matricula.getId());

            if (!detalleExiste) {
                log.info("No existe DetallePago para la matrícula id={}. Se procederá a registrar uno.", matricula.getId());
                registrarDetallePagoMatriculaAutomatica(matricula, null); // null porque no hay pago aún
            } else {
                log.info("Ya existe un DetallePago asociado a la matrícula id={}", matricula.getId());
            }
        }

        proceso.setUltimaEjecucion(today);
        procesoEjecutadoRepositorio.save(proceso);
        log.info("Proceso MATRÍCULA_AUTOMATICA completado. Flag actualizado a {}", today);
    }

    @Transactional
    public Pago obtenerUltimoPagoPendienteEntidad(Long alumnoId) {
        log.info("[obtenerUltimoPagoPendienteEntidad] Buscando el último pago pendiente para alumnoId={}", alumnoId);
        Pago ultimo = pagoRepositorio.findTopByAlumnoIdAndEstadoPagoAndSaldoRestanteGreaterThanOrderByFechaDesc(alumnoId, EstadoPago.ACTIVO, 0.0).orElse(null);
        log.info("[obtenerUltimoPagoPendienteEntidad] Resultado para alumno id={}: {}", alumnoId, ultimo);
        return ultimo;
    }

}

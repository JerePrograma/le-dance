package ledance.servicios.inscripcion;

import ledance.dto.asistencia.AsistenciaMensualMapper;
import ledance.dto.inscripcion.InscripcionMapper;
import ledance.dto.inscripcion.request.InscripcionRegistroRequest;
import ledance.dto.matricula.response.MatriculaResponse;
import ledance.dto.response.EstadisticasInscripcionResponse;
import ledance.dto.inscripcion.response.InscripcionResponse;
import ledance.entidades.*;
import ledance.infra.errores.TratadorDeErrores;
import ledance.repositorios.*;
import ledance.servicios.asistencia.AsistenciaMensualServicio;
import ledance.servicios.matricula.MatriculaServicio;
import ledance.servicios.mensualidad.MensualidadServicio;
import ledance.dto.mensualidad.response.MensualidadResponse;
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

    public InscripcionServicio(InscripcionRepositorio inscripcionRepositorio,
                               AlumnoRepositorio alumnoRepositorio,
                               DisciplinaRepositorio disciplinaRepositorio,
                               BonificacionRepositorio bonificacionRepositorio,
                               InscripcionMapper inscripcionMapper,
                               AsistenciaMensualRepositorio asistenciaMensualRepositorio,
                               AsistenciaMensualServicio asistenciaMensualServicio,
                               MensualidadServicio mensualidadServicio,
                               AsistenciaMensualMapper asistenciaMensualMapper,
                               AsistenciaAlumnoMensualRepositorio asistenciaAlumnoMensualRepositorio, MatriculaServicio matriculaServicio) {
        this.inscripcionRepositorio = inscripcionRepositorio;
        this.alumnoRepositorio = alumnoRepositorio;
        this.disciplinaRepositorio = disciplinaRepositorio;
        this.bonificacionRepositorio = bonificacionRepositorio;
        this.inscripcionMapper = inscripcionMapper;
        this.asistenciaMensualServicio = asistenciaMensualServicio;
        this.mensualidadServicio = mensualidadServicio;
        this.asistenciaAlumnoMensualRepositorio = asistenciaAlumnoMensualRepositorio;
        this.matriculaServicio = matriculaServicio;
    }

    /**
     * Registrar una nueva inscripcion.
     * Se genera automáticamente una cuotaOCantidad para el mes vigente y se incorpora al alumno a la planilla de asistencia (por disciplina).
     */
    @Transactional
    public InscripcionResponse crearInscripcion(InscripcionRegistroRequest request) {
        log.info("Registrando inscripción para alumnoId: {} en disciplinaId: {}",
                request.alumno().id(), request.disciplina().id());

        Alumno alumno = alumnoRepositorio.findById(request.alumno().id())
                .orElseThrow(() -> new IllegalArgumentException("Alumno no encontrado."));
        Disciplina disciplina = disciplinaRepositorio.findById(request.disciplina().id())
                .orElseThrow(() -> new IllegalArgumentException("Disciplina no encontrada."));
        if (disciplina.getProfesor() == null) {
            throw new IllegalStateException("La disciplina indicada no tiene profesor asignado.");
        }
        Bonificacion bonificacion = null;
        if (request.bonificacionId() != null) {
            bonificacion = bonificacionRepositorio.findById(request.bonificacionId())
                    .orElse(null);
        }

        // Convertimos el DTO a entidad Inscripción y asignamos relaciones
        Inscripcion inscripcion = inscripcionMapper.toEntity(request);
        inscripcion.setAlumno(alumno);
        inscripcion.setDisciplina(disciplina);
        inscripcion.setBonificacion(bonificacion);
        inscripcion.setFechaInscripcion(request.fechaInscripcion() == null ? LocalDate.now() : request.fechaInscripcion());

        // Guardamos la inscripción
        Inscripcion guardada = inscripcionRepositorio.save(inscripcion);
        log.info("Inscripción guardada con ID: {}", guardada.getId());

        // Calcular el costo utilizando la lógica de negocio
        Double costoCalculado = calcularCosto(guardada);
        log.info("Costo calculado para inscripción id {}: {}", guardada.getId(), costoCalculado);

        try {
            int mesActual = LocalDate.now().getMonthValue();
            int anioActual = LocalDate.now().getYear();
            MensualidadResponse cuotaGenerada = mensualidadServicio.generarCuota(guardada.getId(), mesActual, anioActual);
            log.info("Cuota generada automáticamente para inscripción id: {} con cuotaOCantidad id: {}",
                    guardada.getId(), cuotaGenerada.id());
        } catch (IllegalStateException e) {
            log.warn("No se generó cuotaOCantidad automática para la inscripción id {}: {}", guardada.getId(), e.getMessage());
        }

        int mes = LocalDate.now().getMonthValue();
        int anio = LocalDate.now().getYear();
        log.info("Incorporando alumno a la planilla de asistencia para {}/{}.", mes, anio);
        asistenciaMensualServicio.agregarAlumnoAPlanilla(guardada.getId(), mes, anio);

        // Verificar o crear la matrícula para el año vigente utilizando el servicio correspondiente
        MatriculaResponse matriculaResponse = matriculaServicio.obtenerOMarcarPendiente(alumno.getId());
        log.info("Matrícula obtenida o creada para el alumno id {}: {}", alumno.getId(), matriculaResponse);

        return inscripcionMapper.toDTO(guardada);
    }

    // Logica de cálculo trasladada al servicio (sin utilizar el mapper)
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
                .orElseThrow(() -> new TratadorDeErrores.RecursoNoEncontradoException("Inscripcion no encontrada."));

        // Eliminar registros relacionados en otras entidades antes de eliminar la inscripción
        List<AsistenciaAlumnoMensual> registros = asistenciaAlumnoMensualRepositorio.findByInscripcionId(inscripcion.getId());
        if (!registros.isEmpty()) {
            asistenciaAlumnoMensualRepositorio.deleteAll(registros);
        }

        // Refrescar la entidad en el contexto de persistencia
        inscripcionRepositorio.flush();

        // Ahora eliminamos la inscripción (sus mensualidades serán eliminadas en cascada)
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

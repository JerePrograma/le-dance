package ledance.servicios.inscripcion;

import ledance.dto.inscripcion.InscripcionMapper;
import ledance.dto.inscripcion.request.InscripcionModificacionRequest;
import ledance.dto.inscripcion.request.InscripcionRegistroRequest;
import ledance.dto.response.EstadisticasInscripcionResponse;
import ledance.dto.inscripcion.response.InscripcionResponse;
import ledance.entidades.*;
import ledance.infra.errores.TratadorDeErrores;
import ledance.repositorios.*;
import jakarta.transaction.Transactional;
import ledance.servicios.asistencia.AsistenciaMensualServicio;
import ledance.servicios.mensualidad.MensualidadServicio;
import ledance.dto.mensualidad.response.MensualidadResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

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
    private final AsistenciaMensualRepositorio asistenciaMensualRepositorio;
    private final AsistenciaMensualServicio asistenciaMensualServicio;
    private final MensualidadServicio mensualidadServicio; // Inyectamos el servicio de mensualidades

    public InscripcionServicio(InscripcionRepositorio inscripcionRepositorio,
                               AlumnoRepositorio alumnoRepositorio,
                               DisciplinaRepositorio disciplinaRepositorio,
                               BonificacionRepositorio bonificacionRepositorio,
                               InscripcionMapper inscripcionMapper,
                               AsistenciaMensualRepositorio asistenciaMensualRepositorio,
                               AsistenciaMensualServicio asistenciaMensualServicio,
                               MensualidadServicio mensualidadServicio) {
        this.inscripcionRepositorio = inscripcionRepositorio;
        this.alumnoRepositorio = alumnoRepositorio;
        this.disciplinaRepositorio = disciplinaRepositorio;
        this.bonificacionRepositorio = bonificacionRepositorio;
        this.inscripcionMapper = inscripcionMapper;
        this.asistenciaMensualRepositorio = asistenciaMensualRepositorio;
        this.asistenciaMensualServicio = asistenciaMensualServicio;
        this.mensualidadServicio = mensualidadServicio;
    }

    /**
     * ✅ Registrar una nueva inscripción.
     * Se genera automáticamente, para el mes vigente, una cuota asociada a esta inscripción.
     */
    @Transactional
    public InscripcionResponse crearInscripcion(InscripcionRegistroRequest request) {
        log.info("Registrando inscripción para alumnoId: {} en disciplinaId: {}",
                request.alumnoId(), request.inscripcion().disciplinaId());

        Alumno alumno = alumnoRepositorio.findById(request.alumnoId())
                .orElseThrow(() -> new IllegalArgumentException("Alumno no encontrado."));

        Disciplina disciplina = disciplinaRepositorio.findById(request.inscripcion().disciplinaId())
                .orElseThrow(() -> new IllegalArgumentException("Disciplina no encontrada."));

        if (disciplina.getProfesor() == null) {
            throw new IllegalStateException("La disciplina indicada no tiene profesor asignado.");
        }

        Bonificacion bonificacion = null;
        if (request.inscripcion().bonificacionId() != null) {
            bonificacion = bonificacionRepositorio.findById(request.inscripcion().bonificacionId())
                    .orElse(null);
        }

        // Convertir request → entidad (sin asignar fecha aún)
        Inscripcion inscripcion = inscripcionMapper.toEntity(request);
        inscripcion.setAlumno(alumno);
        inscripcion.setDisciplina(disciplina);
        inscripcion.setBonificacion(bonificacion);

        // Si "fechaInscripcion" vino nula, usar LocalDate.now()
        if (request.fechaInscripcion() == null) {
            inscripcion.setFechaInscripcion(LocalDate.now());
        } else {
            inscripcion.setFechaInscripcion(request.fechaInscripcion());
        }

        // Guardar inscripción
        Inscripcion guardada = inscripcionRepositorio.save(inscripcion);
        log.info("Inscripción guardada con ID: {}", guardada.getId());

        // Lógica adicional: Generar automáticamente la cuota del mes vigente para esta inscripción.
        try {
            int mesActual = LocalDate.now().getMonthValue();
            int anioActual = LocalDate.now().getYear();
            MensualidadResponse cuotaGenerada = mensualidadServicio.generarCuota(guardada.getId(), mesActual, anioActual);
            log.info("Cuota generada automáticamente para inscripción id: {} con cuota id: {}",
                    guardada.getId(), cuotaGenerada.id());
        } catch (IllegalStateException e) {
            // En caso de que ya exista una cuota para este mes (o algún otro error de validación), se registra la advertencia.
            log.warn("No se generó cuota automática para la inscripción id {}: {}", guardada.getId(), e.getMessage());
        }

        // NUEVA LÓGICA: Si es la primera inscripción activa para la disciplina, crear asistencia mensual.
        List<Inscripcion> inscripcionesActivas = inscripcionRepositorio
                .findAllByDisciplina_IdAndEstado(disciplina.getId(), EstadoInscripcion.ACTIVA);
        if (inscripcionesActivas.size() == 1) { // Es la primera inscripción activa
            int mes = LocalDate.now().getMonthValue();
            int anio = LocalDate.now().getYear();
            log.info("Primera inscripción activa en la disciplina. Creando asistencia mensual para {}/{}.", mes, anio);
            asistenciaMensualServicio.crearAsistenciaPorDisciplina(disciplina.getId(), mes, anio);
        }

        return inscripcionMapper.toDTO(guardada);
    }

    @Override
    public InscripcionResponse obtenerPorId(Long id) {
        Inscripcion inscripcion = inscripcionRepositorio.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Inscripción no encontrada."));
        return inscripcionMapper.toDTO(inscripcion);
    }

    @Override
    @Transactional
    public InscripcionResponse actualizarInscripcion(Long id, InscripcionModificacionRequest request) {
        log.info("Actualizando inscripción con id: {}", id);
        // 1) Buscar la inscripción existente
        Inscripcion inscripcion = inscripcionRepositorio.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Inscripción no encontrada."));

        // 2) Mapear los cambios desde el DTO
        inscripcionMapper.updateEntityFromRequest(request, inscripcion);

        // 3) Si fechaBaja no es null, cambiar estado a BAJA
        if (request.fechaBaja() != null) {
            inscripcion.setFechaBaja(request.fechaBaja());
            inscripcion.setEstado(EstadoInscripcion.BAJA);
        }

        // 4) Guardar los cambios
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
    public void crearAsistenciaMensualParaInscripcionesActivas(int mes, int anio) {
        List<Inscripcion> inscripcionesActivas = inscripcionRepositorio.findByEstado(EstadoInscripcion.ACTIVA);
        for (Inscripcion inscripcion : inscripcionesActivas) {
            AsistenciaMensual asistenciaMensual = new AsistenciaMensual();
            asistenciaMensual.setMes(mes);
            asistenciaMensual.setAnio(anio);
            asistenciaMensual.setInscripcion(inscripcion);
            asistenciaMensualRepositorio.save(asistenciaMensual);
        }
    }

    @Override
    @Transactional
    public void eliminarInscripcion(Long id) {
        Inscripcion inscripcion = inscripcionRepositorio.findById(id)
                .orElseThrow(() -> new TratadorDeErrores.RecursoNoEncontradoException("Inscripción no encontrada."));
        inscripcionRepositorio.delete(inscripcion);
    }

    @Transactional
    public void darBajaInscripcion(Long id) {
        Inscripcion inscripcion = inscripcionRepositorio.findById(id)
                .orElseThrow(() -> new TratadorDeErrores.RecursoNoEncontradoException("Inscripción no encontrada."));
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
                .orElseThrow(() -> new IllegalArgumentException("Inscripción activa no encontrada para el alumno " + alumnoId));
        return inscripcionMapper.toDTO(inscripcion);
    }
}

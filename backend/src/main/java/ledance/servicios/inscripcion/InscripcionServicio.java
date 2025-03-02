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

    public InscripcionServicio(InscripcionRepositorio inscripcionRepositorio,
                               AlumnoRepositorio alumnoRepositorio,
                               DisciplinaRepositorio disciplinaRepositorio,
                               BonificacionRepositorio bonificacionRepositorio,
                               InscripcionMapper inscripcionMapper, AsistenciaMensualRepositorio asistenciaMensualRepositorio, AsistenciaMensualServicio asistenciaMensualServicio) {
        this.inscripcionRepositorio = inscripcionRepositorio;
        this.alumnoRepositorio = alumnoRepositorio;
        this.disciplinaRepositorio = disciplinaRepositorio;
        this.bonificacionRepositorio = bonificacionRepositorio;
        this.inscripcionMapper = inscripcionMapper;
        this.asistenciaMensualRepositorio = asistenciaMensualRepositorio;
        this.asistenciaMensualServicio = asistenciaMensualServicio;
    }

    /**
     * ✅ Registrar una nueva inscripcion.
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

        // Convertir request -> entidad (sin asignar fecha aún)
        Inscripcion inscripcion = inscripcionMapper.toEntity(request);
        inscripcion.setAlumno(alumno);
        inscripcion.setDisciplina(disciplina);
        inscripcion.setBonificacion(bonificacion);

        // ✅ Si "fechaInscripcion" vino nula, usar "LocalDate.now()"
        if (request.fechaInscripcion() == null) {
            inscripcion.setFechaInscripcion(LocalDate.now());
        } else {
            inscripcion.setFechaInscripcion(request.fechaInscripcion());
        }

        // Guardar inscripción
        Inscripcion guardada = inscripcionRepositorio.save(inscripcion);
        log.info("Inscripción guardada con ID: {}", guardada.getId());

        // NUEVA LÓGICA: Verificar si es la primera inscripción activa para la disciplina.
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

    /**
     * ✅ Obtener una inscripcion por ID.
     */
    @Override
    public InscripcionResponse obtenerPorId(Long id) {
        Inscripcion inscripcion = inscripcionRepositorio.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Inscripcion no encontrada."));
        return inscripcionMapper.toDTO(inscripcion);
    }

    /**
     * ✅ Actualizar una inscripcion.
     */
    @Override
    @Transactional
    public InscripcionResponse actualizarInscripcion(Long id, InscripcionModificacionRequest request) {
        log.info("Actualizando inscripcion con id: {}", id);

        // 1) Buscar la inscripcion existente
        Inscripcion inscripcion = inscripcionRepositorio.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Inscripcion no encontrada."));

        // 2) Mapear los cambios desde el DTO
        inscripcionMapper.updateEntityFromRequest(request, inscripcion);

        // 3) Logica adicional: si la fechaBaja no es null, cambiar estado a BAJA
        if (request.fechaBaja() != null) {
            inscripcion.setFechaBaja(request.fechaBaja());
            // opcional: si tu mapper no asigno ya la fechaBaja
            inscripcion.setEstado(EstadoInscripcion.BAJA);
            // asume que "EstadoInscripcion" tiene un valor "BAJA"
        }

        // 4) Guardar los cambios
        Inscripcion actualizada = inscripcionRepositorio.save(inscripcion);

        // 5) Retornar la inscripcion actualizada
        return inscripcionMapper.toDTO(actualizada);
    }

    /**
     * ✅ Listar inscripciones por disciplina.
     */
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

    /**
     * ✅ Listar inscripciones por alumno.
     */
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

        // Convert the List<Object[]> to Map<String, Long>
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
}
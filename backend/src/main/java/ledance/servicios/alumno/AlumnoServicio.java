package ledance.servicios.alumno;

import jakarta.persistence.EntityNotFoundException;
import ledance.dto.alumno.AlumnoMapper;
import ledance.dto.alumno.request.AlumnoRegistroRequest;
import ledance.dto.alumno.response.AlumnoDataResponse;
import ledance.dto.alumno.response.AlumnoListadoResponse;
import ledance.dto.alumno.response.AlumnoResponse;
import ledance.dto.disciplina.DisciplinaMapper;
import ledance.dto.disciplina.response.DisciplinaResponse;
import ledance.dto.pago.DetallePagoMapper;
import ledance.dto.pago.response.DetallePagoResponse;
import ledance.entidades.*;
import ledance.repositorios.*;
import jakarta.transaction.Transactional;
import ledance.servicios.inscripcion.InscripcionServicio;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.Period;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class AlumnoServicio implements IAlumnoServicio {

    private static final Logger log = LoggerFactory.getLogger(AlumnoServicio.class);

    private final AlumnoRepositorio alumnoRepositorio;
    private final AlumnoMapper alumnoMapper;
    private final DisciplinaMapper disciplinaMapper;
    private final DetallePagoRepositorio detallePagoRepositorio;
    private final DetallePagoMapper detallePagoMapper;
    private final InscripcionServicio inscripcionServicio;

    public AlumnoServicio(AlumnoRepositorio alumnoRepositorio, AlumnoMapper alumnoMapper,
                          DisciplinaMapper disciplinaMapper,
                          DetallePagoRepositorio detallePagoRepositorio,
                          DetallePagoMapper detallePagoMapper,
                          InscripcionServicio inscripcionServicio) {
        this.alumnoRepositorio = alumnoRepositorio;
        this.alumnoMapper = alumnoMapper;
        this.disciplinaMapper = disciplinaMapper;
        this.detallePagoRepositorio = detallePagoRepositorio;
        this.detallePagoMapper = detallePagoMapper;
        this.inscripcionServicio = inscripcionServicio;
    }

    @Override
    @Transactional
    public AlumnoResponse registrarAlumno(AlumnoRegistroRequest requestDTO) {
        log.info("Registrando alumno: {}", requestDTO.nombre());

        // Verificacion de duplicados por nombre + apellido
        if (alumnoRepositorio.existsByNombreIgnoreCaseAndApellidoIgnoreCase(requestDTO.nombre().trim(), requestDTO.apellido().trim())) {
            String msg = String.format("Ya existe un alumno con el nombre '%s' y apellido '%s'",
                    requestDTO.nombre(), requestDTO.apellido());
            log.warn("[registrarAlumno] {}", msg);
            throw new IllegalStateException(msg); // o podrias usar una excepcion custom si preferis
        }

        Alumno alumno = alumnoMapper.toEntity(requestDTO);
        if (alumno.getId() == 0) {
            alumno.setId(null);
        }

        // Calcular edad automaticamente
        if (alumno.getFechaNacimiento() != null) {
            alumno.setEdad(calcularEdad(requestDTO.fechaNacimiento()));
        }

        // Guardar el alumno
        Alumno alumnoGuardado = alumnoRepositorio.save(alumno);
        return alumnoMapper.toResponse(alumnoGuardado);
    }

    @Override
    public AlumnoResponse obtenerAlumnoPorId(Long id) {
        Alumno alumno = alumnoRepositorio
                .findByIdAndActivoTrue(id)
                .orElseThrow(() -> new EntityNotFoundException("Alumno no encontrado."));
        return alumnoMapper.toResponse(alumno);
    }

    @Override
    public List<AlumnoResponse> listarAlumnos() {
        return alumnoRepositorio.findAll().stream()
                .map(alumnoMapper::toResponse)
                .collect(Collectors.toList());
    }


    @Override
    @Transactional
    public AlumnoResponse actualizarAlumno(Long id, AlumnoRegistroRequest dto) {
        Alumno alumno = alumnoRepositorio
                .findByIdAndActivoTrue(id)
                .orElseThrow(() -> new EntityNotFoundException("Alumno no encontrado."));
        alumnoMapper.updateEntityFromRequest(dto, alumno);
        if (alumno.getFechaNacimiento() != null) {
            alumno.setEdad(calcularEdad(dto.fechaNacimiento()));
        }
        return alumnoMapper.toResponse(alumnoRepositorio.save(alumno));
    }

    @Override
    @Transactional
    public void darBajaAlumno(Long id) {
        Alumno alumno = alumnoRepositorio
                .findByIdAndActivoTrue(id)
                .orElseThrow(() -> new EntityNotFoundException("Alumno no encontrado."));
        alumno.setActivo(false);
        alumno.setFechaDeBaja(LocalDate.now());
        alumnoRepositorio.save(alumno);
    }

    @Override
    public List<AlumnoResponse> listarAlumnosSimplificado() {
        return alumnoRepositorio.findByActivoTrue().stream()
                .map(alumnoMapper::toResponse)
                .collect(Collectors.toList());
    }

    @Override
    public List<AlumnoResponse> buscarPorNombre(String nombre) {
        return alumnoRepositorio.buscarPorNombreCompleto(nombre).stream()
                .map(alumnoMapper::toResponse)
                .collect(Collectors.toList());
    }

    @Override
    public List<DisciplinaResponse> obtenerDisciplinasDeAlumno(Long alumnoId) {
        Alumno alumno = alumnoRepositorio
                .findByIdAndActivoTrue(alumnoId)
                .orElseThrow(() -> new EntityNotFoundException("Alumno no encontrado."));
        return alumno.getInscripciones().stream()
                .map(ins -> disciplinaMapper.toResponse(ins.getDisciplina()))
                .collect(Collectors.toList());
    }

    private int calcularEdad(LocalDate fechaNacimiento) {
        if (fechaNacimiento != null) {
            return Period.between(fechaNacimiento, LocalDate.now()).getYears();
        }
        return 0;
    }


    /**
     * Baja lógica del alumno:
     * 1) Para cada inscripción, elimina sus asistencias (no su estado).
     * 2) Limpia la lista de inscripciones (orphanRemoval si lo tienes configurado).
     * 3) Limpia las matrículas si querés ocultarlas.
     * 4) Marca al alumno inactivo.
     */
    @Transactional
    public void eliminarAlumno(Long id) {
        Alumno alumno = alumnoRepositorio.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Alumno no encontrado."));

        // 1) Baja lógica del alumno
        alumno.setActivo(false);
        alumno.setFechaDeBaja(LocalDate.now());

        // 2) Para cada inscripción:
        for (Inscripcion ins : alumno.getInscripciones()) {
            // 2.a) Eliminar todas las asistencias mensuales (orphanRemoval)
            ins.getAsistenciasAlumnoMensual().clear();

            // 2.b) Marcar la inscripción como de BAJA
            ins.setEstado(EstadoInscripcion.INACTIVA);
            ins.setFechaBaja(LocalDate.now());
        }

        // 3) (Opcional) Marcar las matrículas como inactivas
        // alumno.getMatriculas().forEach(m -> m.setPagada(false));

        // 4) Guardar cambios en cascada
        alumnoRepositorio.save(alumno);
    }

    @Transactional
    public AlumnoDataResponse obtenerAlumnoData(Long alumnoId) {
        Alumno alumno = alumnoRepositorio
                .findByIdAndActivoTrue(alumnoId)
                .orElseThrow(() -> new EntityNotFoundException("Alumno no encontrado."));
        List<DetallePago> pendientes = detallePagoRepositorio
                .findByAlumnoIdAndImportePendienteGreaterThan(alumnoId, 0.0);
        List<DetallePagoResponse> dtos = pendientes.stream()
                .map(detallePagoMapper::toDTO)
                .toList();
        AlumnoListadoResponse listado = alumnoMapper.toAlumnoListadoResponse(alumno);
        return new AlumnoDataResponse(listado, dtos);
    }
}
package ledance.servicios;

import ledance.dto.mappers.AlumnoMapper;
import ledance.dto.mappers.DisciplinaMapper;
import ledance.dto.request.AlumnoModificacionRequest;
import ledance.dto.request.AlumnoRegistroRequest;
import ledance.dto.response.AlumnoDetalleResponse;
import ledance.dto.response.AlumnoListadoResponse;
import ledance.dto.response.DisciplinaListadoResponse;
import ledance.entidades.Alumno;
import ledance.repositorios.AlumnoRepositorio;
import jakarta.transaction.Transactional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.Period;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class AlumnoServicio implements IAlumnoServicio {

    private static final Logger log = LoggerFactory.getLogger(AlumnoServicio.class);

    private final AlumnoRepositorio alumnoRepositorio;
    private final AlumnoMapper alumnoMapper;
    private final DisciplinaMapper disciplinaMapper;

    public AlumnoServicio(AlumnoRepositorio alumnoRepositorio, AlumnoMapper alumnoMapper, DisciplinaMapper disciplinaMapper) {
        this.alumnoRepositorio = alumnoRepositorio;
        this.alumnoMapper = alumnoMapper;
        this.disciplinaMapper = disciplinaMapper;
    }

    @Override
    @Transactional
    public AlumnoDetalleResponse registrarAlumno(AlumnoRegistroRequest requestDTO) {
        log.info("Registrando alumno: {}", requestDTO.nombre());

        Alumno alumno = alumnoMapper.toEntity(requestDTO);

        // Calcular edad automaticamente
        if (alumno.getFechaNacimiento() != null) {
            alumno.setEdad(calcularEdad(requestDTO.fechaNacimiento()));
        }

        // Guardar el alumno
        Alumno alumnoGuardado = alumnoRepositorio.save(alumno);
        return alumnoMapper.toDetalleResponse(alumnoGuardado);
    }

    @Override
    public AlumnoDetalleResponse obtenerAlumnoPorId(Long id) {
        Alumno alumno = alumnoRepositorio.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Alumno no encontrado."));
        return alumnoMapper.toDetalleResponse(alumno);
    }

    @Override
    public List<AlumnoListadoResponse> listarAlumnos() {
        return alumnoRepositorio.findAll().stream()
                .map(alumnoMapper::toListadoResponse)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public AlumnoDetalleResponse actualizarAlumno(Long id, AlumnoModificacionRequest requestDTO) {
        log.info("Actualizando alumno con id: {}", id);
        Alumno alumno = alumnoRepositorio.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Alumno no encontrado."));

        alumnoMapper.updateEntityFromRequest(requestDTO, alumno);

        // Recalcular edad si cambia la fecha de nacimiento
        if (alumno.getFechaNacimiento() != null) {
            alumno.setEdad(calcularEdad(requestDTO.fechaNacimiento()));
        }

        Alumno alumnoActualizado = alumnoRepositorio.save(alumno);
        return alumnoMapper.toDetalleResponse(alumnoActualizado);
    }

    @Override
    @Transactional
    public void darBajaAlumno(Long id) {
        log.info("Dando de baja (baja logica) al alumno con id: {}", id);
        Alumno alumno = alumnoRepositorio.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Alumno no encontrado."));

        alumno.setActivo(false);
        alumno.setFechaDeBaja(LocalDate.now());  // Guardar la fecha de baja
        alumnoRepositorio.save(alumno);
    }

    @Override
    public List<AlumnoListadoResponse> listarAlumnosSimplificado() {
        return alumnoRepositorio.findByActivoTrue().stream()
                .map(alumnoMapper::toListadoResponse)
                .collect(Collectors.toList());
    }

    @Override
    public List<AlumnoListadoResponse> buscarPorNombre(String nombre) {
        return alumnoRepositorio.buscarPorNombreCompleto(nombre).stream()
                .map(alumnoMapper::toListadoResponse)
                .collect(Collectors.toList());
    }

    @Override
    public List<DisciplinaListadoResponse> obtenerDisciplinasDeAlumno(Long alumnoId) {
        Alumno alumno = alumnoRepositorio.findById(alumnoId)
                .orElseThrow(() -> new IllegalArgumentException("Alumno no encontrado."));
        return alumno.getInscripciones().stream()
                .map(inscripcion -> disciplinaMapper.toListadoResponse(inscripcion.getDisciplina())) // ✅ Uso correcto del mapper
                .collect(Collectors.toList());
    }

    private int calcularEdad(LocalDate fechaNacimiento) {
        if (fechaNacimiento != null) {
            return Period.between(fechaNacimiento, LocalDate.now()).getYears();
        }
        return 0;
    }

}

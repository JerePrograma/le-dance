package ledance.servicios;

import ledance.dto.request.AlumnoRequest;
import ledance.dto.response.AlumnoListadoResponse;
import ledance.dto.response.AlumnoResponse;
import ledance.entidades.Alumno;
import ledance.dto.mappers.AlumnoMapper;
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

    public AlumnoServicio(AlumnoRepositorio alumnoRepositorio, AlumnoMapper alumnoMapper) {
        this.alumnoRepositorio = alumnoRepositorio;
        this.alumnoMapper = alumnoMapper;
    }

    @Override
    @Transactional
    public AlumnoResponse registrarAlumno(AlumnoRequest requestDTO) {
        log.info("Registrando alumno: {}", requestDTO.nombre());
        Alumno alumno = alumnoMapper.toEntity(requestDTO);
        if (alumno.getFechaNacimiento() != null) {
            int years = Period.between(alumno.getFechaNacimiento(), LocalDate.now()).getYears();
            alumno.setEdad(years);
        }
        Alumno alumnoGuardado = alumnoRepositorio.save(alumno);
        return alumnoMapper.toDTO(alumnoGuardado);
    }

    @Override
    public AlumnoResponse obtenerAlumnoPorId(Long id) {
        Alumno alumno = alumnoRepositorio.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Alumno no encontrado."));
        return alumnoMapper.toDTO(alumno);
    }

    @Override
    public List<AlumnoResponse> listarAlumnos() {
        return alumnoRepositorio.findAll().stream()
                .map(alumnoMapper::toDTO)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public AlumnoResponse actualizarAlumno(Long id, AlumnoRequest requestDTO) {
        log.info("Actualizando alumno con id: {}", id);
        Alumno alumno = alumnoRepositorio.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Alumno no encontrado."));
        alumnoMapper.updateEntityFromRequest(alumno, requestDTO);
        if (alumno.getFechaNacimiento() != null) {
            int years = Period.between(alumno.getFechaNacimiento(), LocalDate.now()).getYears();
            alumno.setEdad(years);
        }
        Alumno alumnoActualizado = alumnoRepositorio.save(alumno);
        return alumnoMapper.toDTO(alumnoActualizado);
    }

    @Override
    @Transactional
    public void eliminarAlumno(Long id) {
        log.info("Eliminando (baja logica) alumno con id: {}", id);
        Alumno alumno = alumnoRepositorio.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Alumno no encontrado."));
        alumno.setActivo(false);
        alumnoRepositorio.save(alumno);
    }

    @Override
    public List<AlumnoListadoResponse> listarAlumnosSimplificado() {
        return alumnoRepositorio.findAll().stream()
                .map(alumnoMapper::toListadoResponse)
                .collect(Collectors.toList());
    }

    @Override
    public List<AlumnoListadoResponse> buscarPorNombre(String nombre) {
        return alumnoRepositorio.buscarPorNombreCompleto(nombre).stream()
                .map(alumnoMapper::toListadoResponse)
                .collect(Collectors.toList());
    }
}

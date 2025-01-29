package ledance.servicios;

import ledance.dto.request.AlumnoRequest;
import ledance.dto.response.AlumnoListadoResponse;
import ledance.dto.response.AlumnoResponse;
import ledance.entidades.Alumno;
import ledance.dto.mappers.AlumnoMapper;
import ledance.repositorios.AlumnoRepositorio;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.Period;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class AlumnoServicio {

    private final AlumnoRepositorio alumnoRepositorio;
    private final AlumnoMapper alumnoMapper;

    public AlumnoServicio(AlumnoRepositorio alumnoRepositorio,
                          AlumnoMapper alumnoMapper) {
        this.alumnoRepositorio = alumnoRepositorio;
        this.alumnoMapper = alumnoMapper;
    }

    public AlumnoResponse registrarAlumno(AlumnoRequest requestDTO) {
        Alumno alumno = alumnoMapper.toEntity(requestDTO);

        // Calcular edad si hay fechaNacimiento
        if (alumno.getFechaNacimiento() != null) {
            int years = Period.between(alumno.getFechaNacimiento(), LocalDate.now()).getYears();
            alumno.setEdad(years);
        }

        Alumno alumnoGuardado = alumnoRepositorio.save(alumno);
        return alumnoMapper.toDTO(alumnoGuardado);
    }

    public AlumnoResponse obtenerAlumnoPorId(Long id) {
        Alumno alumno = alumnoRepositorio.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Alumno no encontrado."));
        return alumnoMapper.toDTO(alumno);
    }

    public List<AlumnoResponse> listarAlumnos() {
        return alumnoRepositorio.findAll().stream()
                .map(alumnoMapper::toDTO)
                .collect(Collectors.toList());
    }

    public AlumnoResponse actualizarAlumno(Long id, AlumnoRequest requestDTO) {
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

    public void eliminarAlumno(Long id) {
        Alumno alumno = alumnoRepositorio.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Alumno no encontrado."));
        alumno.setActivo(false);
        alumnoRepositorio.save(alumno);
    }

    /**
     * Métodos que estaban relacionados con Disciplinas / Bonificaciones se mueven o se eliminan
     * porque ahora se gestionan en InscripcionServicio.
     */

    public List<AlumnoListadoResponse> listarAlumnosSimplificado() {
        return alumnoRepositorio.findAll().stream()
                .map(alumno -> new AlumnoListadoResponse(
                        alumno.getId(),
                        alumno.getNombre(),
                        alumno.getApellido()
                ))
                .collect(Collectors.toList());
    }

    public List<AlumnoResponse> buscarPorNombre(String nombre) {
        return alumnoRepositorio.buscarPorNombreCompleto(nombre)
                .stream()
                .map(alumnoMapper::toDTO)
                .collect(Collectors.toList());
    }


}

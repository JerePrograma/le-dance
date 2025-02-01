package ledance.servicios;

import ledance.dto.request.AlumnoRequest;
import ledance.dto.response.AlumnoListadoResponse;
import ledance.dto.response.AlumnoResponse;

import java.util.List;

public interface IAlumnoServicio {
    AlumnoResponse registrarAlumno(AlumnoRequest requestDTO);
    AlumnoResponse obtenerAlumnoPorId(Long id);
    List<AlumnoResponse> listarAlumnos();
    AlumnoResponse actualizarAlumno(Long id, AlumnoRequest requestDTO);
    void eliminarAlumno(Long id);
    List<AlumnoListadoResponse> listarAlumnosSimplificado();
    List<AlumnoListadoResponse> buscarPorNombre(String nombre);
}

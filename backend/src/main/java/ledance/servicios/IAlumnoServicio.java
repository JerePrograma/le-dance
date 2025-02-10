package ledance.servicios;

import ledance.dto.request.AlumnoModificacionRequest;
import ledance.dto.request.AlumnoRegistroRequest;
import ledance.dto.response.AlumnoDetalleResponse;
import ledance.dto.response.AlumnoListadoResponse;
import ledance.dto.response.DisciplinaListadoResponse;

import java.util.List;

public interface IAlumnoServicio {
    AlumnoDetalleResponse registrarAlumno(AlumnoRegistroRequest requestDTO);
    AlumnoDetalleResponse obtenerAlumnoPorId(Long id);
    List<AlumnoListadoResponse> listarAlumnos();
    AlumnoDetalleResponse actualizarAlumno(Long id, AlumnoModificacionRequest requestDTO);
    void darBajaAlumno(Long id);
    List<AlumnoListadoResponse> listarAlumnosSimplificado();
    List<AlumnoListadoResponse> buscarPorNombre(String nombre);
    List<DisciplinaListadoResponse> obtenerDisciplinasDeAlumno(Long alumnoId);
}

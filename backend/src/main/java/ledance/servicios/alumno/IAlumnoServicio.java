package ledance.servicios.alumno;

import ledance.dto.alumno.request.AlumnoModificacionRequest;
import ledance.dto.alumno.request.AlumnoRegistroRequest;
import ledance.dto.alumno.response.AlumnoDetalleResponse;
import ledance.dto.alumno.response.AlumnoListadoResponse;
import ledance.dto.disciplina.response.DisciplinaListadoResponse;

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
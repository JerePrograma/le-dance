package ledance.servicios.alumno;

import ledance.dto.alumno.request.AlumnoRegistroRequest;
import ledance.dto.alumno.response.AlumnoResponse;
import ledance.dto.disciplina.response.DisciplinaResponse;

import java.util.List;

public interface IAlumnoServicio {
    AlumnoResponse registrarAlumno(AlumnoRegistroRequest requestDTO);

    AlumnoResponse obtenerAlumnoPorId(Long id);

    List<AlumnoResponse> listarAlumnos();

    AlumnoResponse actualizarAlumno(Long id, AlumnoRegistroRequest requestDTO);

    void darBajaAlumno(Long id);

    List<AlumnoResponse> listarAlumnosSimplificado();

    List<AlumnoResponse> buscarPorNombre(String nombre);

    List<DisciplinaResponse> obtenerDisciplinasDeAlumno(Long alumnoId);
}
package ledance.servicios.matricula;

import ledance.dto.matricula.MatriculaMapper;
import ledance.dto.matricula.request.MatriculaModificacionRequest;
import ledance.dto.matricula.response.MatriculaResponse;
import ledance.entidades.Alumno;
import ledance.entidades.Matricula;
import ledance.repositorios.AlumnoRepositorio;
import ledance.repositorios.MatriculaRepositorio;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.Year;
import java.util.List;

@Service
public class MatriculaServicio {

    private final MatriculaRepositorio matriculaRepositorio;
    private final AlumnoRepositorio alumnoRepositorio;
    private final MatriculaMapper matriculaMapper;

    public MatriculaServicio(MatriculaRepositorio matriculaRepositorio,
                             AlumnoRepositorio alumnoRepositorio,
                             MatriculaMapper matriculaMapper) {
        this.matriculaRepositorio = matriculaRepositorio;
        this.alumnoRepositorio = alumnoRepositorio;
        this.matriculaMapper = matriculaMapper;
    }

    /**
     * Obtiene la matrícula pendiente del alumno para el año actual.
     * Si no existe, se crea una nueva pendiente.
     */
    @Transactional
    public MatriculaResponse obtenerOMarcarPendiente(Long alumnoId) {
        int anioActual = Year.now().getValue();
        List<Matricula> matriculas = matriculaRepositorio.findByAlumnoIdAndAnio(alumnoId, anioActual);
        Matricula matricula;
        if (matriculas.isEmpty()) {
            Alumno alumno = alumnoRepositorio.findById(alumnoId)
                    .orElseThrow(() -> new IllegalArgumentException("Alumno no encontrado."));
            matricula = new Matricula();
            matricula.setAlumno(alumno);
            matricula.setAnio(anioActual);
            matricula.setPagada(false);
            matricula = matriculaRepositorio.save(matricula);
        } else {
            matricula = matriculas.stream()
                    .filter(m -> !m.getPagada())
                    .findFirst()
                    .orElse(matriculas.get(0));
        }
        return matriculaMapper.toResponse(matricula);
    }

    /**
     * Actualiza el estado de la matrícula.
     */
    @Transactional
    public MatriculaResponse actualizarEstadoMatricula(Long matriculaId, MatriculaModificacionRequest request) {
        Matricula m = matriculaRepositorio.findById(matriculaId)
                .orElseThrow(() -> new IllegalArgumentException("Matrícula no encontrada."));
        matriculaMapper.updateEntityFromRequest(request, m);
        return matriculaMapper.toResponse(matriculaRepositorio.save(m));
    }
}

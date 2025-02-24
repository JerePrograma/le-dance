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
import java.util.Optional;

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
     * Obtiene la matrícula de un alumno para el año actual.
     * Si no existe, se crea un registro pendiente.
     */
    @Transactional
    public MatriculaResponse obtenerOMarcarPendiente(Long alumnoId) {
        try {
            int anioActual = Year.now().getValue();
            Optional<Matricula> opt = matriculaRepositorio.findByAlumnoIdAndAnio(alumnoId, anioActual);
            Matricula matricula;
            if (opt.isPresent()) {
                matricula = opt.get();
            } else {
                Alumno alumno = alumnoRepositorio.findById(alumnoId)
                        .orElseThrow(() -> new IllegalArgumentException("Alumno no encontrado."));
                matricula = new Matricula();
                matricula.setAlumno(alumno);
                matricula.setAnio(anioActual);
                matricula.setPagada(false);
                matricula = matriculaRepositorio.save(matricula);
            }
            // Log para verificar el contenido de la matrícula antes de mapearla
            System.out.println("Matrícula obtenida: " + matricula);
            return matriculaMapper.toResponse(matricula);
        } catch (Exception e) {
            // Log de la excepción completa para depuración
            e.printStackTrace();
            throw e; // o lanzar una excepción personalizada
        }
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
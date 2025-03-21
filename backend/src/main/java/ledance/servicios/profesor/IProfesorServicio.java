package ledance.servicios.profesor;

import ledance.dto.profesor.request.ProfesorModificacionRequest;
import ledance.dto.profesor.request.ProfesorRegistroRequest;
import ledance.dto.profesor.response.ProfesorResponse;
import ledance.dto.profesor.response.ProfesorResponse;

import java.util.List;

public interface IProfesorServicio {
    ProfesorResponse registrarProfesor(ProfesorRegistroRequest request);

    ProfesorResponse obtenerProfesorPorId(Long id);

    List<ProfesorResponse> listarProfesores();

    ProfesorResponse actualizarProfesor(Long id, ProfesorModificacionRequest request);

    void eliminarProfesor(Long id);

    // ✅ Metodos adicionales:
    List<ProfesorResponse> buscarPorNombre(String nombre);

    List<ProfesorResponse> listarProfesoresActivos();
}
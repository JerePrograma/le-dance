package ledance.servicios.profesor;

import ledance.dto.profesor.request.ProfesorModificacionRequest;
import ledance.dto.profesor.request.ProfesorRegistroRequest;
import ledance.dto.profesor.response.ProfesorDetalleResponse;
import ledance.dto.profesor.response.ProfesorListadoResponse;

import java.util.List;

public interface IProfesorServicio {
    ProfesorDetalleResponse registrarProfesor(ProfesorRegistroRequest request);

    ProfesorDetalleResponse obtenerProfesorPorId(Long id);

    List<ProfesorListadoResponse> listarProfesores();

    ProfesorDetalleResponse actualizarProfesor(Long id, ProfesorModificacionRequest request);

    void eliminarProfesor(Long id);

    // ✅ Metodos adicionales:
    List<ProfesorListadoResponse> buscarPorNombre(String nombre);

    List<ProfesorListadoResponse> listarProfesoresActivos();
}
package ledance.servicios;

import ledance.dto.request.ProfesorModificacionRequest;
import ledance.dto.request.ProfesorRegistroRequest;
import ledance.dto.response.ProfesorDetalleResponse;
import ledance.dto.response.ProfesorListadoResponse;

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

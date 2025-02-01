package ledance.servicios;

import ledance.dto.request.InscripcionRequest;
import ledance.dto.response.InscripcionResponse;

import java.util.List;

public interface IInscripcionServicio {
    InscripcionResponse crearInscripcion(InscripcionRequest request);
    InscripcionResponse obtenerPorId(Long id);
    List<InscripcionResponse> listarInscripciones();
    InscripcionResponse actualizarInscripcion(Long id, InscripcionRequest request);
    void eliminarInscripcion(Long id);
    List<InscripcionResponse> listarPorAlumno(Long alumnoId);
}

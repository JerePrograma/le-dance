package ledance.servicios;

import ledance.dto.request.InscripcionModificacionRequest;
import ledance.dto.request.InscripcionRegistroRequest;
import ledance.dto.response.InscripcionResponse;

import java.util.List;

public interface IInscripcionServicio {
    InscripcionResponse crearInscripcion(InscripcionRegistroRequest request);
    InscripcionResponse obtenerPorId(Long id);
    List<InscripcionResponse> listarInscripciones();
    InscripcionResponse actualizarInscripcion(Long id, InscripcionModificacionRequest request);
    void eliminarInscripcion(Long id);
    List<InscripcionResponse> listarPorAlumno(Long alumnoId);
    List<InscripcionResponse> listarPorDisciplina(Long disciplinaId); // ✅ Nuevo metodo
}

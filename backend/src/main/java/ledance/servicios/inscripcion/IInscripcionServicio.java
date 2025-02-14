package ledance.servicios.inscripcion;

import ledance.dto.inscripcion.request.InscripcionModificacionRequest;
import ledance.dto.inscripcion.request.InscripcionRegistroRequest;
import ledance.dto.inscripcion.response.InscripcionResponse;

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
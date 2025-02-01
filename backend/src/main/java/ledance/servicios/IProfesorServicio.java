package ledance.servicios;

import ledance.dto.request.ProfesorRegistroRequest;
import ledance.dto.response.DatosRegistroProfesorResponse;
import ledance.dto.response.ProfesorListadoResponse;

import java.util.List;

public interface IProfesorServicio {
    DatosRegistroProfesorResponse registrarProfesor(ProfesorRegistroRequest request);
    DatosRegistroProfesorResponse obtenerProfesorPorId(Long id);
    List<DatosRegistroProfesorResponse> listarProfesores();
    void asignarUsuario(Long profesorId, Long usuarioId);
    void asignarDisciplina(Long profesorId, Long disciplinaId);
    List<ProfesorListadoResponse> listarProfesoresSimplificados();
}

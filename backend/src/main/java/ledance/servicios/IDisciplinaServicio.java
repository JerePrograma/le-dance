package ledance.servicios;

import ledance.dto.request.DisciplinaModificacionRequest;
import ledance.dto.request.DisciplinaRegistroRequest;
import ledance.dto.response.AlumnoListadoResponse;
import ledance.dto.response.DisciplinaDetalleResponse;
import ledance.dto.response.DisciplinaListadoResponse;
import ledance.dto.response.ProfesorListadoResponse;

import java.time.LocalTime;
import java.util.List;

public interface IDisciplinaServicio {

    /**
     * ✅ Registrar una nueva disciplina.
     */
    DisciplinaDetalleResponse crearDisciplina(DisciplinaRegistroRequest requestDTO);

    /**
     * ✅ Listar TODAS las disciplinas con detalles completos.
     */
    List<DisciplinaDetalleResponse> listarDisciplinas();

    /**
     * ✅ Listar disciplinas en formato simplificado (id, nombre, horario, activo).
     */
    List<DisciplinaListadoResponse> listarDisciplinasSimplificadas();

    /**
     * ✅ Obtener una disciplina por ID con detalles completos.
     */
    DisciplinaDetalleResponse obtenerDisciplinaPorId(Long id);

    /**
     * ✅ Actualizar una disciplina existente.
     */
    DisciplinaDetalleResponse actualizarDisciplina(Long id, DisciplinaModificacionRequest requestDTO);

    /**
     * ✅ Dar de baja (baja logica) a una disciplina.
     */
    void eliminarDisciplina(Long id);

    /**
     * ✅ Obtener disciplinas activas segun una fecha especifica.
     */
    List<DisciplinaListadoResponse> obtenerDisciplinasPorFecha(String fecha);

    /**
     * ✅ Obtener disciplinas activas segun su horario de inicio.
     */
    List<DisciplinaListadoResponse> obtenerDisciplinasPorHorario(LocalTime horarioInicio);

    /**
     * ✅ Obtener alumnos de una disciplina especifica.
     */
    List<AlumnoListadoResponse> obtenerAlumnosDeDisciplina(Long disciplinaId);

    /**
     * ✅ Obtener el profesor de una disciplina especifica.
     */
    ProfesorListadoResponse obtenerProfesorDeDisciplina(Long disciplinaId);
}

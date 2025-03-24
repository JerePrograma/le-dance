package ledance.servicios.disciplina;

import ledance.dto.disciplina.request.DisciplinaModificacionRequest;
import ledance.dto.disciplina.request.DisciplinaRegistroRequest;
import ledance.dto.disciplina.request.DisciplinaHorarioRequest;
import ledance.dto.disciplina.response.DisciplinaResponse;
import ledance.dto.disciplina.response.DisciplinaResponse;
import ledance.dto.disciplina.response.DisciplinaHorarioResponse;
import ledance.dto.profesor.response.ProfesorResponse;
import ledance.dto.alumno.response.AlumnoResponse;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

public interface IDisciplinaServicio {

    /**
     * ✅ Registrar una nueva disciplina con horarios por dia.
     */
    DisciplinaResponse crearDisciplina(DisciplinaRegistroRequest requestDTO);

    /**
     * ✅ Listar TODAS las disciplinas con detalles completos.
     */
    List<DisciplinaResponse> listarDisciplinas();

    /**
     * ✅ Listar disciplinas en formato simplificado sin horarios detallados.
     */
    List<DisciplinaResponse> listarDisciplinasSimplificadas();

    /**
     * ✅ Obtener una disciplina por ID con detalles completos.
     */
    DisciplinaResponse obtenerDisciplinaPorId(Long id);

    /**
     * ✅ Actualizar una disciplina existente y sus horarios.
     */
    DisciplinaResponse actualizarDisciplina(Long id, DisciplinaModificacionRequest requestDTO);

    /**
     * ✅ Dar de baja (baja logica) a una disciplina.
     */
    void eliminarDisciplina(Long id);

    /**
     * ✅ Obtener disciplinas activas segun una fecha especifica.
     * ⚠️ Ahora funciona con horarios multiples por dia.
     */
    List<DisciplinaResponse> obtenerDisciplinasPorFecha(String fecha);

    /**
     * ✅ Obtener disciplinas activas segun su horario de inicio.
     */
    List<DisciplinaResponse> obtenerDisciplinasPorHorario(LocalTime horarioInicio);

    /**
     * ✅ Obtener alumnos de una disciplina especifica.
     */
    List<AlumnoResponse> obtenerAlumnosDeDisciplina(Long disciplinaId);

    /**
     * ✅ Obtener el profesor de una disciplina especifica.
     */
    ProfesorResponse obtenerProfesorDeDisciplina(Long disciplinaId);

    /**
     * ✅ Buscar disciplinas por nombre.
     */
    List<DisciplinaResponse> buscarPorNombre(String nombre);

    /**
     * ✅ Obtener los dias de clase de una disciplina en un mes y año especificos.
     */
    List<LocalDate> obtenerDiasClase(Long disciplinaId, Integer mes, Integer anio);

    /**
     * ✅ Obtener los horarios de una disciplina especifica.
     */
    List<DisciplinaHorarioResponse> obtenerHorarios(Long disciplinaId);
}

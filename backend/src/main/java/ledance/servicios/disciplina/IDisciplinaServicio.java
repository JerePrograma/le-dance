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
     * ✅ Registrar una nueva disciplina con horarios por día.
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
     * ✅ Obtener disciplinas activas según una fecha específica.
     * ⚠️ Ahora funciona con horarios múltiples por día.
     */
    List<DisciplinaResponse> obtenerDisciplinasPorFecha(String fecha);

    /**
     * ✅ Obtener disciplinas activas según su horario de inicio.
     */
    List<DisciplinaResponse> obtenerDisciplinasPorHorario(LocalTime horarioInicio);

    /**
     * ✅ Obtener alumnos de una disciplina específica.
     */
    List<AlumnoResponse> obtenerAlumnosDeDisciplina(Long disciplinaId);

    /**
     * ✅ Obtener el profesor de una disciplina específica.
     */
    ProfesorResponse obtenerProfesorDeDisciplina(Long disciplinaId);

    /**
     * ✅ Buscar disciplinas por nombre.
     */
    List<DisciplinaResponse> buscarPorNombre(String nombre);

    /**
     * ✅ Obtener los días de clase de una disciplina en un mes y año específicos.
     */
    List<LocalDate> obtenerDiasClase(Long disciplinaId, Integer mes, Integer anio);

    /**
     * ✅ Obtener los horarios de una disciplina específica.
     */
    List<DisciplinaHorarioResponse> obtenerHorarios(Long disciplinaId);
}

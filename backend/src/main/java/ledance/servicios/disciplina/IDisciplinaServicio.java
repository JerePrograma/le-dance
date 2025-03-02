package ledance.servicios.disciplina;

import ledance.dto.disciplina.request.DisciplinaModificacionRequest;
import ledance.dto.disciplina.request.DisciplinaRegistroRequest;
import ledance.dto.disciplina.request.DisciplinaHorarioRequest;
import ledance.dto.disciplina.response.DisciplinaDetalleResponse;
import ledance.dto.disciplina.response.DisciplinaListadoResponse;
import ledance.dto.disciplina.response.DisciplinaHorarioResponse;
import ledance.dto.profesor.response.ProfesorListadoResponse;
import ledance.dto.alumno.response.AlumnoListadoResponse;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

public interface IDisciplinaServicio {

    /**
     * ✅ Registrar una nueva disciplina con horarios por día.
     */
    DisciplinaDetalleResponse crearDisciplina(DisciplinaRegistroRequest requestDTO);

    /**
     * ✅ Listar TODAS las disciplinas con detalles completos.
     */
    List<DisciplinaDetalleResponse> listarDisciplinas();

    /**
     * ✅ Listar disciplinas en formato simplificado sin horarios detallados.
     */
    List<DisciplinaListadoResponse> listarDisciplinasSimplificadas();

    /**
     * ✅ Obtener una disciplina por ID con detalles completos.
     */
    DisciplinaDetalleResponse obtenerDisciplinaPorId(Long id);

    /**
     * ✅ Actualizar una disciplina existente y sus horarios.
     */
    DisciplinaDetalleResponse actualizarDisciplina(Long id, DisciplinaModificacionRequest requestDTO);

    /**
     * ✅ Dar de baja (baja lógica) a una disciplina.
     */
    void eliminarDisciplina(Long id);

    /**
     * ✅ Obtener disciplinas activas según una fecha específica.
     * ⚠️ Ahora funciona con horarios múltiples por día.
     */
    List<DisciplinaListadoResponse> obtenerDisciplinasPorFecha(String fecha);

    /**
     * ✅ Obtener disciplinas activas según su horario de inicio.
     */
    List<DisciplinaListadoResponse> obtenerDisciplinasPorHorario(LocalTime horarioInicio);

    /**
     * ✅ Obtener alumnos de una disciplina específica.
     */
    List<AlumnoListadoResponse> obtenerAlumnosDeDisciplina(Long disciplinaId);

    /**
     * ✅ Obtener el profesor de una disciplina específica.
     */
    ProfesorListadoResponse obtenerProfesorDeDisciplina(Long disciplinaId);

    /**
     * ✅ Buscar disciplinas por nombre.
     */
    List<DisciplinaListadoResponse> buscarPorNombre(String nombre);

    /**
     * ✅ Obtener los días de clase de una disciplina en un mes y año específicos.
     */
    List<LocalDate> obtenerDiasClase(Long disciplinaId, Integer mes, Integer anio);

    /**
     * ✅ Crear horarios individuales para una disciplina.
     */
    void crearHorarios(Long disciplinaId, List<DisciplinaHorarioRequest> horarios);

    /**
     * ✅ Actualizar horarios individuales de una disciplina.
     */
    void actualizarHorarios(Long disciplinaId, List<DisciplinaHorarioRequest> horarios);

    /**
     * ✅ Obtener los horarios de una disciplina específica.
     */
    List<DisciplinaHorarioResponse> obtenerHorarios(Long disciplinaId);
}

package ledance.repositorios;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import ledance.entidades.AsistenciaDiaria;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Repository
public interface AsistenciaDiariaRepositorio extends JpaRepository<AsistenciaDiaria, Long> {

    /**
     * ✅ Verifica si existe una asistencia para un alumno en una fecha y horario específicos.
     */
    Page<AsistenciaDiaria> findByDisciplinaHorarioDisciplinaIdAndFecha(Long disciplinaId, LocalDate fecha, Pageable pageable);

    /**
     * ✅ Busca una asistencia por su ID, alumno y fecha.
     */
    Optional<AsistenciaDiaria> findByIdAndAlumnoIdAndFecha(Long id, Long alumnoId, LocalDate fecha);

    Map<Long, Integer> contarAsistenciasPorAlumno(Long disciplinaId, LocalDate fechaInicio, LocalDate fechaFin);

    /**
     * ✅ Verifica si existe una asistencia para un alumno en una fecha y horario específicos.
     */
    boolean existsByAlumnoIdAndFechaAndDisciplinaHorarioId(Long alumnoId, LocalDate fecha, Long disciplinaHorarioId);

    /**
     * ✅ Buscar asistencias por asistencia mensual.
     */
    List<AsistenciaDiaria> findByAsistenciaMensualId(Long asistenciaMensualId);

    /**
     * ✅ Verifica si una asistencia ya existe para un alumno en un horario específico.
     */
    Optional<AsistenciaDiaria> findByAlumnoIdAndFechaAndDisciplinaHorarioId(Long alumnoId, LocalDate fecha, Long disciplinaHorarioId);

}

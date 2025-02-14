package ledance.repositorios;

import ledance.entidades.AsistenciaDiaria;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public interface AsistenciaDiariaRepositorio extends JpaRepository<AsistenciaDiaria, Long> {

    // ✅ Método optimizado con nombre más claro
    Optional<AsistenciaDiaria> findByAsistenciaMensualIdAndAlumnoIdAndFecha(Long asistenciaMensualId, Long alumnoId, LocalDate fecha);

    // ✅ Buscar asistencias en un rango de fechas con paginación (puede mejorar rendimiento)
    Page<AsistenciaDiaria> findByAsistenciaMensual_Inscripcion_Disciplina_IdAndFechaBetween(Long disciplinaId, LocalDate fechaInicio, LocalDate fechaFin, Pageable pageable);

    // ✅ Mejor nombre para consulta con fecha
    boolean existsByAsistenciaMensualInscripcionAlumnoIdAndFecha(Long alumnoId, LocalDate fecha);

    List<AsistenciaDiaria> findByAsistenciaMensualId(Long asistenciaMensualId);

    boolean existsByAlumnoIdAndFecha(Long alumnoId, LocalDate fecha); // Verificar duplicados

    Page<AsistenciaDiaria> findByAsistenciaMensual_Inscripcion_Disciplina_IdAndFecha(Long disciplinaId, LocalDate fecha, Pageable pageable);

    @Query("SELECT a.alumno.id, COUNT(a) FROM AsistenciaDiaria a " +
            "WHERE a.asistenciaMensual.inscripcion.disciplina.id = :disciplinaId " +
            "AND a.fecha BETWEEN :fechaInicio AND :fechaFin " +
            "AND a.estado = 'PRESENTE' " +
            "GROUP BY a.alumno.id")
    Map<Long, Integer> contarAsistenciasPorAlumno(
            @Param("disciplinaId") Long disciplinaId,
            @Param("fechaInicio") LocalDate fechaInicio,
            @Param("fechaFin") LocalDate fechaFin);
}
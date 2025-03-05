package ledance.repositorios;

import ledance.entidades.AsistenciaDiaria;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Repository
public interface AsistenciaDiariaRepositorio extends JpaRepository<AsistenciaDiaria, Long> {

    // Recupera las asistencias diarias filtradas por disciplina a través de la planilla
    Page<AsistenciaDiaria> findByAsistenciaMensual_Disciplina_IdAndFecha(Long disciplinaId, LocalDate fecha, Pageable pageable);

    Optional<AsistenciaDiaria> findByIdAndAlumnoIdAndFecha(Long id, Long alumnoId, LocalDate fecha);

    @Query("SELECT a.alumno.id, COUNT(a) FROM AsistenciaDiaria a " +
            "WHERE a.asistenciaMensual.disciplina.id = :disciplinaId " +
            "AND a.fecha BETWEEN :fechaInicio AND :fechaFin " +
            "GROUP BY a.alumno.id")
    List<Object[]> contarAsistenciasPorAlumnoRaw(@Param("disciplinaId") Long disciplinaId,
                                                 @Param("fechaInicio") LocalDate fechaInicio,
                                                 @Param("fechaFin") LocalDate fechaFin);

    default Map<Long, Integer> contarAsistenciasPorAlumno(Long disciplinaId, LocalDate fechaInicio, LocalDate fechaFin) {
        List<Object[]> resultados = contarAsistenciasPorAlumnoRaw(disciplinaId, fechaInicio, fechaFin);
        Map<Long, Integer> mapa = new HashMap<>();
        for (Object[] row : resultados) {
            Long alumnoId = (Long) row[0];
            Integer count = ((Long) row[1]).intValue();
            mapa.put(alumnoId, count);
        }
        return mapa;
    }

    // Verifica si ya existe una asistencia para un alumno en una fecha
    boolean existsByAlumnoIdAndFecha(Long alumnoId, LocalDate fecha);

    Optional<AsistenciaDiaria> findByAlumnoIdAndFecha(Long alumnoId, LocalDate fecha);

    // Recupera todas las asistencias asociadas a una planilla de asistencia
    List<AsistenciaDiaria> findByAsistenciaMensualId(Long asistenciaMensualId);

    // Elimina asistencias diarias de una planilla cuya fecha sea mayor o igual a la indicada
    void deleteByAsistenciaMensualIdAndFechaGreaterThanEqual(Long asistenciaMensualId, LocalDate fecha);

    // Verifica si ya existen registros para un alumno en una planilla
    boolean existsByAlumnoIdAndAsistenciaMensualId(Long alumnoId, Long planillaId);
}

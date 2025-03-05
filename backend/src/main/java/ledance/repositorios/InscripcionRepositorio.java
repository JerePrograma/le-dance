package ledance.repositorios;

import ledance.entidades.EstadoInscripcion;
import ledance.entidades.Inscripcion;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;

@Repository
public interface InscripcionRepositorio extends JpaRepository<Inscripcion, Long> {

    @EntityGraph(attributePaths = {"alumno", "disciplina", "bonificacion"})
    @Query("SELECT i FROM Inscripcion i")
    List<Inscripcion> findAllWithDetails();

    List<Inscripcion> findByDisciplinaId(Long disciplinaId);

    @Query("SELECT i.disciplina.nombre as disciplina, COUNT(i) as count FROM Inscripcion i GROUP BY i.disciplina.nombre")
    List<Object[]> countByDisciplinaGrouped();

    @Query("SELECT FUNCTION('MONTH', i.fechaInscripcion) as month, COUNT(i) as count FROM Inscripcion i GROUP BY FUNCTION('MONTH', i.fechaInscripcion)")
    List<Object[]> countByMonthGrouped();

    List<Inscripcion> findAllByDisciplinaIdAndEstado(Long disciplinaId, EstadoInscripcion estado);

    List<Inscripcion> findAllByAlumno_IdAndEstado(Long alumnoId, EstadoInscripcion estado);

    List<Inscripcion> findByEstado(EstadoInscripcion estado);

    @Query("SELECT i.disciplina.nombre, SUM(i.disciplina.valorCuota) " +
            "FROM Inscripcion i " +
            "WHERE i.disciplina.id = :disciplinaId " +
            "GROUP BY i.disciplina.nombre")
    List<Object[]> obtenerRecaudacionPorDisciplina(@Param("disciplinaId") Long disciplinaId);

    Optional<Inscripcion> findByAlumno_IdAndEstado(Long alumnoId, EstadoInscripcion estado);

    Optional<Inscripcion> findFirstByAlumno_IdAndEstadoOrderByIdAsc(Long alumnoId, EstadoInscripcion estado);

    List<Inscripcion> findAllByDisciplina_IdAndEstado(Long id, EstadoInscripcion estadoInscripcion);
}

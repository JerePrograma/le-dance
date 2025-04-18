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
    @Query("SELECT I FROM Inscripcion I")
    List<Inscripcion> findAllWithDetails();

    List<Inscripcion> findByDisciplinaId(Long disciplinaId);

    @Query("SELECT I.disciplina.nombre as disciplina, COUNT(I) as count FROM Inscripcion I GROUP BY I.disciplina.nombre")
    List<Object[]> countByDisciplinaGrouped();

    @Query("SELECT FUNCTION('MONTH', I.fechaInscripcion) as month, COUNT(I) as count FROM Inscripcion I GROUP BY FUNCTION('MONTH', I.fechaInscripcion)")
    List<Object[]> countByMonthGrouped();

    List<Inscripcion> findAllByDisciplinaIdAndEstado(Long disciplinaId, EstadoInscripcion estado);

    List<Inscripcion> findAllByAlumno_IdAndEstado(Long alumnoId, EstadoInscripcion estado);

    List<Inscripcion> findByEstado(EstadoInscripcion estado);

    @Query("SELECT I.disciplina.nombre, SUM(I.disciplina.valorCuota) " +
            "FROM Inscripcion I " +
            "WHERE I.disciplina.id = :disciplinaId " +
            "GROUP BY I.disciplina.nombre")
    List<Object[]> obtenerRecaudacionPorDisciplina(@Param("disciplinaId") Long disciplinaId);

    Optional<Inscripcion> findByAlumno_IdAndEstado(Long alumnoId, EstadoInscripcion estado);

    Optional<Inscripcion> findFirstByAlumno_IdAndEstadoOrderByIdAsc(Long alumnoId, EstadoInscripcion estado);

    Optional<Inscripcion> findByAlumnoIdAndDisciplinaIdAndEstado(Long alumnoId, Long disciplinaId, EstadoInscripcion estado);
}

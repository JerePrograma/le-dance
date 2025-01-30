package ledance.repositorios;

import ledance.dto.response.InscripcionResponse;
import ledance.entidades.Inscripcion;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface InscripcionRepositorio extends JpaRepository<Inscripcion, Long> {

    List<Inscripcion> findByAlumnoId(Long alumnoId);

    List<Inscripcion> findByDisciplinaId(Long disciplinaId);

    List<InscripcionResponse> findAllByAlumnoId(Long alumnoId);

    @Query("SELECT i.disciplina.nombre, SUM(i.costoParticular) FROM Inscripcion i " +
            "WHERE i.alumno.activo = true GROUP BY i.disciplina.nombre")
    List<Object[]> obtenerRecaudacionPorDisciplina();
}
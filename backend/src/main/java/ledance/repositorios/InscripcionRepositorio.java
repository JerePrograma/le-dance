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

    List<Inscripcion> findAllByAlumnoId(Long alumnoId);

    @Query("SELECT d.nombre, SUM(i.costoParticular) FROM Inscripcion i JOIN i.disciplina d GROUP BY d.nombre")
    List<Object[]> obtenerRecaudacionPorDisciplina();
}
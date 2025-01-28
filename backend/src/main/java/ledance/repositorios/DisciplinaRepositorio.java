package ledance.repositorios;

import jakarta.validation.constraints.NotNull;
import ledance.entidades.Disciplina;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface DisciplinaRepositorio extends JpaRepository<Disciplina, Long> {
    boolean existsByNombre(String nombre);

    boolean existsByNombreAndHorario(@NotNull String nombre, String horario);

    @Query("SELECT d FROM Disciplina d WHERE LOWER(d.horario) LIKE %:diaSemana%")
    List<Disciplina> findDisciplinasPorDia(@Param("diaSemana") String diaSemana);

    List<Disciplina> findByActivoTrue();
}

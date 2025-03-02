package ledance.repositorios;

import ledance.entidades.DiaSemana;
import ledance.entidades.DisciplinaHorario;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface DisciplinaHorarioRepositorio extends JpaRepository<DisciplinaHorario, Long> {
    List<DisciplinaHorario> findByDisciplinaId(Long disciplinaId);
    void deleteByDisciplinaId(Long disciplinaId);

    List<DisciplinaHorario> findByDiaSemana(DiaSemana diaSemana);
}

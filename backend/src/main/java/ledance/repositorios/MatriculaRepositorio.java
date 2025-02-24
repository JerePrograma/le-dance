package ledance.repositorios;

import ledance.entidades.Matricula;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface MatriculaRepositorio extends JpaRepository<Matricula, Long> {
    Optional<Matricula> findByAlumnoIdAndAnio(Long alumnoId, Integer anio);
}

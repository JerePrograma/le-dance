package ledance.repositorios;

import jakarta.validation.constraints.NotNull;
import ledance.dto.pago.PagoMapper;
import ledance.entidades.Matricula;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface MatriculaRepositorio extends JpaRepository<Matricula, Long> {
    Optional<Matricula> findFirstByAlumnoIdAndAnioOrderByIdDesc(Long alumnoId, @NotNull Integer anio);

    Optional<Matricula> findFirstByAnio(int anio);
}
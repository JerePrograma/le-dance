package ledance.repositorios;

import ledance.entidades.Mensualidad;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface MensualidadRepositorio extends JpaRepository<Mensualidad, Long> {
    List<Mensualidad> findByInscripcionIdOrderByAnioDescMesDesc(Long inscripcionId);
    Optional<Mensualidad> findByInscripcionIdAndAnioAndMes(Long inscripcionId, Integer anio, Integer mes);
}

package ledance.repositorios;

import ledance.entidades.Recargo;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface RecargoRepositorio extends JpaRepository<Recargo, Long> {
    Optional<Recargo> findByDiaDelMesAplicacion(Integer diaDelMes);
}

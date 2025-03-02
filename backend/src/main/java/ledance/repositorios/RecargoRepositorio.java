package ledance.repositorios;

import ledance.entidades.Recargo;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface RecargoRepositorio extends JpaRepository<Recargo, Long> {
    List<Recargo> findByDiaDelMesAplicacion(Integer diaDelMes);
}

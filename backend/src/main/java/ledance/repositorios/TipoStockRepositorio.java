package ledance.repositorios;

import ledance.entidades.TipoStock;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface TipoStockRepositorio extends JpaRepository<TipoStock, Long> {
    List<TipoStock> findByActivoTrue();
}

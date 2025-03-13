package ledance.repositorios;

import ledance.entidades.Stock;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;

public interface StockRepositorio extends JpaRepository<Stock, Long> {
    List<Stock> findByActivoTrue();

}

package ledance.repositorios;

import ledance.entidades.Stock;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface StockRepositorio extends JpaRepository<Stock, Long> {
    List<Stock> findByActivoTrue();

    Optional<Stock> findByNombreIgnoreCase(String nombre);

}

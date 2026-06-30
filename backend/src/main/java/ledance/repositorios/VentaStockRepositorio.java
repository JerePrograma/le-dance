package ledance.repositorios;

import ledance.entidades.VentaStock;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface VentaStockRepositorio extends JpaRepository<VentaStock, Long> {
    Optional<VentaStock> findByIdempotencyKey(String idempotencyKey);
}

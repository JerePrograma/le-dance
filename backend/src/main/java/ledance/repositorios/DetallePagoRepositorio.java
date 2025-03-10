package ledance.repositorios;

import ledance.entidades.DetallePago;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface DetallePagoRepositorio extends JpaRepository<DetallePago, Long> {
}

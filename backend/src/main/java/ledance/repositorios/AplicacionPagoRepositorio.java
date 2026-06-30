package ledance.repositorios;

import ledance.entidades.AplicacionPago;
import ledance.entidades.EstadoAplicacionPago;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.util.List;

public interface AplicacionPagoRepositorio extends JpaRepository<AplicacionPago, Long> {
    List<AplicacionPago> findByPagoIdOrderById(Long pagoId);
    List<AplicacionPago> findByPagoIdAndEstadoOrderById(Long pagoId, EstadoAplicacionPago estado);

    @Query("select coalesce(sum(a.importeAplicado), 0) from AplicacionPago a where a.cargo.id = :cargoId and a.estado = :estado")
    BigDecimal sumByCargoAndEstado(@Param("cargoId") Long cargoId, @Param("estado") EstadoAplicacionPago estado);
}

package ledance.repositorios;

import ledance.entidades.MovimientoCaja;
import ledance.entidades.TipoMovimientoCaja;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface MovimientoCajaRepositorio extends JpaRepository<MovimientoCaja, Long> {
    Optional<MovimientoCaja> findByPagoIdAndTipo(Long pagoId, TipoMovimientoCaja tipo);
    Optional<MovimientoCaja> findByEgresoIdAndTipo(Long egresoId, TipoMovimientoCaja tipo);
    List<MovimientoCaja> findByFechaBetweenOrderByFechaAscIdAsc(LocalDate desde, LocalDate hasta);
}

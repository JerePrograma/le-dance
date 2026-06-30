package ledance.repositorios;

import ledance.entidades.MovimientoCaja;
import ledance.entidades.TipoMovimientoCaja;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.math.BigDecimal;

import java.time.LocalDate;
import java.util.Optional;

public interface MovimientoCajaRepositorio extends JpaRepository<MovimientoCaja, Long> {
    Optional<MovimientoCaja> findByPagoIdAndTipo(Long pagoId, TipoMovimientoCaja tipo);
    Optional<MovimientoCaja> findByEgresoIdAndTipo(Long egresoId, TipoMovimientoCaja tipo);
    Page<MovimientoCaja> findByFechaBetween(LocalDate desde, LocalDate hasta, Pageable pageable);

    @Query(value = """
            WITH importes AS (
                SELECT CASE
                    WHEN m.tipo IN ('INGRESO_PAGO','AJUSTE_INGRESO') THEN m.importe
                    WHEN m.tipo IN ('EGRESO','AJUSTE_EGRESO') THEN -m.importe
                    WHEN o.tipo IN ('INGRESO_PAGO','AJUSTE_INGRESO') THEN -m.importe
                    ELSE m.importe
                END AS firmado
                FROM movimientos_caja m
                LEFT JOIN movimientos_caja o ON o.id = m.movimiento_revertido_id
                WHERE m.fecha BETWEEN :desde AND :hasta
            )
            SELECT coalesce(sum(greatest(firmado, 0)), 0) AS "totalIngresos",
                   coalesce(sum(greatest(-firmado, 0)), 0) AS "totalEgresos"
            FROM importes
            """, nativeQuery = true)
    CajaTotales totales(@Param("desde") LocalDate desde, @Param("hasta") LocalDate hasta);

    interface CajaTotales {
        BigDecimal getTotalIngresos();
        BigDecimal getTotalEgresos();
    }
}

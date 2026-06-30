package ledance.servicios.caja;

import ledance.dto.caja.response.MovimientoCajaResponse;
import ledance.dto.caja.response.ResumenCajaResponse;
import ledance.entidades.MovimientoCaja;
import ledance.entidades.TipoMovimientoCaja;
import ledance.repositorios.MovimientoCajaRepositorio;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.List;

@Service
public class CajaServicio {
    private static final BigDecimal CERO = new BigDecimal("0.00");
    private final MovimientoCajaRepositorio movimientos;

    public CajaServicio(MovimientoCajaRepositorio movimientos) {
        this.movimientos = movimientos;
    }

    @Transactional(readOnly = true)
    public ResumenCajaResponse obtenerResumen(LocalDate desde, LocalDate hasta) {
        if (hasta.isBefore(desde)) {
            throw new IllegalArgumentException("La fecha hasta no puede ser anterior a desde");
        }
        List<MovimientoCaja> lista = movimientos.findByFechaBetweenOrderByFechaAscIdAsc(desde, hasta);
        BigDecimal ingresos = lista.stream().map(this::importeFirmado)
                .filter(v -> v.signum() > 0).reduce(CERO, BigDecimal::add);
        BigDecimal egresos = lista.stream().map(this::importeFirmado)
                .filter(v -> v.signum() < 0).map(BigDecimal::abs).reduce(CERO, BigDecimal::add);
        return new ResumenCajaResponse(desde, hasta, decimal(ingresos), decimal(egresos),
                decimal(ingresos.subtract(egresos)), lista.stream().map(this::respuesta).toList());
    }

    private BigDecimal importeFirmado(MovimientoCaja movimiento) {
        return switch (movimiento.getTipo()) {
            case INGRESO_PAGO, AJUSTE_INGRESO -> movimiento.getImporte();
            case EGRESO, AJUSTE_EGRESO -> movimiento.getImporte().negate();
            case REVERSO -> importeFirmado(movimiento.getMovimientoRevertido()).negate();
        };
    }

    private MovimientoCajaResponse respuesta(MovimientoCaja movimiento) {
        return new MovimientoCajaResponse(movimiento.getId(), movimiento.getTipo().name(), movimiento.getFecha(),
                decimal(movimiento.getImporte()), movimiento.getMetodoPago().getId(),
                movimiento.getPago() == null ? null : movimiento.getPago().getId(),
                movimiento.getEgreso() == null ? null : movimiento.getEgreso().getId(),
                movimiento.getMotivo(), movimiento.getCreatedAt());
    }

    private static String decimal(BigDecimal importe) {
        return importe.setScale(2, RoundingMode.UNNECESSARY).toPlainString();
    }
}

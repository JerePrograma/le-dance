package ledance.servicios.caja;

import ledance.dto.caja.response.MovimientoCajaResponse;
import ledance.dto.caja.response.ResumenCajaResponse;
import ledance.dto.PageResponse;
import ledance.entidades.MovimientoCaja;
import ledance.repositorios.MovimientoCajaRepositorio;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.data.domain.Pageable;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;

@Service
public class CajaServicio {
    private final MovimientoCajaRepositorio movimientos;

    public CajaServicio(MovimientoCajaRepositorio movimientos) {
        this.movimientos = movimientos;
    }

    @Transactional(readOnly = true)
    public ResumenCajaResponse obtenerResumen(LocalDate desde, LocalDate hasta, Pageable pageable) {
        if (hasta.isBefore(desde)) {
            throw new IllegalArgumentException("La fecha hasta no puede ser anterior a desde");
        }
        var totales = movimientos.totales(desde, hasta);
        BigDecimal ingresos = totales.getTotalIngresos();
        BigDecimal egresos = totales.getTotalEgresos();
        var pagina = movimientos.findByFechaBetween(desde, hasta, pageable).map(this::respuesta);
        return new ResumenCajaResponse(desde, hasta, decimal(ingresos), decimal(egresos),
                decimal(ingresos.subtract(egresos)), PageResponse.from(pagina));
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

package ledance.servicios.detallepago;

import ledance.entidades.DetallePago;
import ledance.entidades.Recargo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;

@Service
public class DetallePagoServicio {

    private static final Logger log = LoggerFactory.getLogger(DetallePagoServicio.class);

    /**
     * Calcula el importe de un DetallePago a partir de su valor base, descuentos y recargos.
     */
    public void calcularImporte(DetallePago detalle) {
        log.debug("Calculando importe para DetallePago id {} - Concepto: {}", detalle.getId(), detalle.getConcepto());
        double base = (detalle.getValorBase() != null ? detalle.getValorBase() : 0.0);
        double descuento = calcularDescuento(detalle, base);
        double recargoValor = (detalle.getRecargo() != null ? obtenerValorRecargo(detalle, base) : 0.0);
        double totalAjustado = base - descuento + recargoValor;
        double abonoPago = detalle.getaCobrar() != null ? detalle.getaCobrar() : totalAjustado;
        double calculoImporte = totalAjustado - abonoPago;

        BigDecimal bd = new BigDecimal(calculoImporte).setScale(2, RoundingMode.HALF_UP);
        detalle.setImporte(bd.doubleValue());
        log.debug("DetallePago id {}: Importe final = {}, aCobrar = {}, Total ajustado = {}",
                detalle.getId(), detalle.getImporte(), abonoPago, totalAjustado);
    }

    private double calcularDescuento(DetallePago detalle, double base) {
        if (detalle.getBonificacion() != null) {
            double descuentoFijo = (detalle.getBonificacion().getValorFijo() != null ? detalle.getBonificacion().getValorFijo() : 0.0);
            double descuentoPorcentaje = (detalle.getBonificacion().getPorcentajeDescuento() != null ?
                    (detalle.getBonificacion().getPorcentajeDescuento() / 100.0 * base) : 0.0);
            double totalDescuento = descuentoFijo + descuentoPorcentaje;
            log.debug("Bonificación aplicada: fijo={}, porcentaje={}, total descuento={}",
                    descuentoFijo, descuentoPorcentaje, totalDescuento);
            return totalDescuento;
        }
        log.debug("No hay bonificación para DetallePago id {}", detalle.getId());
        return 0.0;
    }

    private double obtenerValorRecargo(DetallePago detalle, double base) {
        Recargo recargo = detalle.getRecargo();
        if (recargo != null) {
            int diaActual = LocalDate.now().getDayOfMonth();
            log.debug("Evaluando recargo para DetallePago id {}: día actual={} vs. día de aplicación={}",
                    detalle.getId(), diaActual, recargo.getDiaDelMesAplicacion());
            if (diaActual != recargo.getDiaDelMesAplicacion()) {
                log.debug("Día actual no coincide con el día de aplicación del recargo. No se aplica recargo.");
                return 0.0;
            }
            double recargoFijo = (recargo.getValorFijo() != null ? recargo.getValorFijo() : 0.0);
            double recargoPorcentaje = (recargo.getPorcentaje() != null ?
                    (recargo.getPorcentaje() / 100.0 * base) : 0.0);
            log.debug("Recargo: fijo={}, porcentaje={}", recargoFijo, recargoPorcentaje);
            return recargoFijo + recargoPorcentaje;
        }
        log.debug("No hay recargo definido para DetallePago id {}", detalle.getId());
        return 0.0;
    }
}

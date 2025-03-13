package ledance.servicios.detallepago;

import ledance.entidades.DetallePago;
import ledance.entidades.Recargo;
import ledance.repositorios.DetallePagoRepositorio;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.Optional;

@Service
public class DetallePagoServicio {

    private static final Logger log = LoggerFactory.getLogger(DetallePagoServicio.class);
    private final DetallePagoRepositorio detallePagoRepositorio;

    public DetallePagoServicio(DetallePagoRepositorio detallePagoRepositorio) {
        this.detallePagoRepositorio = detallePagoRepositorio;
    }

    /**
     * Calcula el importe final de un DetallePago usando:
     * totalAjustado = valorBase - descuento + recargo
     * importe = totalAjustado - aCobrar (acumulado)
     */
    public void calcularImporte(DetallePago detalle) {
        log.info("[calcularImporte] Calculando importe inicial para DetallePago id={} | Concepto='{}'",
                detalle.getId(), detalle.getConcepto());
        double base = Optional.ofNullable(detalle.getValorBase()).orElse(0.0);
        double descuento = calcularDescuento(detalle, base);
        double recargo = (detalle.getRecargo() != null) ? obtenerValorRecargo(detalle, base) : 0.0;
        double importeInicial = base - descuento + recargo;
        log.info("[calcularImporte] Importe inicial calculado = {}", importeInicial);
        // Asignar el importe inicial y, si aún no se ha establecido, inicializar el importe pendiente.
        detalle.setImporteInicial(importeInicial);
        if (detalle.getImportePendiente() == null) {
            detalle.setImportePendiente(importeInicial);
        }
    }

    public double calcularDescuento(DetallePago detalle, double base) {
        if (detalle.getBonificacion() != null) {
            double descuentoFijo = detalle.getBonificacion().getValorFijo() != null ? detalle.getBonificacion().getValorFijo() : 0.0;
            double descuentoPorcentaje = detalle.getBonificacion().getPorcentajeDescuento() != null ?
                    (detalle.getBonificacion().getPorcentajeDescuento() / 100.0 * base) : 0.0;
            double totalDescuento = descuentoFijo + descuentoPorcentaje;
            log.debug("[calcularDescuento] Detalle id={} | Descuento fijo={} | %={} | Total Descuento={}",
                    detalle.getId(), descuentoFijo, descuentoPorcentaje, totalDescuento);
            return totalDescuento;
        }
        log.debug("[calcularDescuento] Detalle id={} sin bonificación, descuento=0", detalle.getId());
        return 0.0;
    }

    public double obtenerValorRecargo(DetallePago detalle, double base) {
        Recargo recargo = detalle.getRecargo();
        if (recargo != null) {
            int diaActual = LocalDate.now().getDayOfMonth();
            log.debug("[obtenerValorRecargo] Detalle id={} | Día actual={} | Día de aplicación={}",
                    detalle.getId(), diaActual, recargo.getDiaDelMesAplicacion());
            if (diaActual != recargo.getDiaDelMesAplicacion()) {
                log.debug("[obtenerValorRecargo] Día actual no coincide; recargo=0");
                return 0.0;
            }
            double recargoFijo = recargo.getValorFijo() != null ? recargo.getValorFijo() : 0.0;
            double recargoPorcentaje = recargo.getPorcentaje() != null ? (recargo.getPorcentaje() / 100.0 * base) : 0.0;
            double totalRecargo = recargoFijo + recargoPorcentaje;
            log.debug("[obtenerValorRecargo] Detalle id={} | Recargo fijo={} | %={} | Total Recargo={}",
                    detalle.getId(), recargoFijo, recargoPorcentaje, totalRecargo);
            return totalRecargo;
        }
        log.debug("[obtenerValorRecargo] Detalle id={} sin recargo; recargo=0", detalle.getId());
        return 0.0;
    }

    private double importeRedondeado(double importe) {
        return BigDecimal.valueOf(importe).setScale(2, RoundingMode.HALF_UP).doubleValue();
    }

}

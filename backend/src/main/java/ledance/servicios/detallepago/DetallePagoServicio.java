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
     * Calcula el importe de un DetallePago a partir de su valor base, descuentos y recargos.
     */
    public void calcularImporte(DetallePago detalle) {
        log.info("Detalle id={}, importe actual: {}, aCobrar actual: {}", detalle.getId(), detalle.getImporte(), detalle.getaCobrar());
        log.debug("Calculando importe para DetallePago id={} | Concepto={}", detalle.getId(), detalle.getConcepto());

        double base = Optional.ofNullable(detalle.getValorBase()).orElse(0.0);
        double descuento = calcularDescuento(detalle, base);
        double recargo = detalle.getRecargo() != null ? obtenerValorRecargo(detalle, base) : 0.0;

        double totalAjustado = base - descuento + recargo;

        // Aquí está la corrección importante:
        double abonoPago = detalle.getaCobrar();

        // El importe final se define como el monto restante por cobrar después de considerar el abono.
        double importeFinal = totalAjustado - abonoPago;

        log.info("DetallePago ID={} | Base={} | Descuento={} | Recargo={} | Total Ajustado={} | aCobrar={} | Importe Calculado={}",
                detalle.getId(), base, descuento, recargo, totalAjustado, abonoPago, importeFinal);

        detalle.setImporte(importeRedondeado(importeFinal));

        log.debug("DetallePago ID={} | Base={} | Descuento={} | Recargo={} | TotalAjustado={} | aCobrar={} | Importe final={}",
                detalle.getId(), base, descuento, recargo, totalAjustado, abonoPago, detalle.getImporte());

        log.info("Importe recalculado para detalle {}: {}", detalle.getId(), detalle.getImporte());
    }

    private double totalAjustado(double base, double descuento, double recargo, double abono) {
        return base - descuento + recargo - abono;
    }

    private double importeFinal(double totalAjustado, double abono) {
        return totalAjustado - abono;
    }

    private double importeRedondeado(double importe) {
        return BigDecimal.valueOf(importe).setScale(2, RoundingMode.HALF_UP).doubleValue();
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

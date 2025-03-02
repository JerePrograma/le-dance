package ledance.servicios.pago;

import ledance.entidades.Bonificacion;
import ledance.entidades.Inscripcion;
import ledance.dto.pago.request.PagoRegistroRequest;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;

@Service
public class PaymentCalculationService {

    /**
     * Calcula el costo base de una inscripcion sumando los valores de cuota, clase suelta y clase prueba.
     */
    public double calcularCostoBase(Inscripcion inscripcion) {
        double cuota = inscripcion.getDisciplina().getValorCuota() != null ? inscripcion.getDisciplina().getValorCuota() : 0.0;
        double claseSuelta = inscripcion.getDisciplina().getClaseSuelta() != null ? inscripcion.getDisciplina().getClaseSuelta() : 0.0;
        double clasePrueba = inscripcion.getDisciplina().getClasePrueba() != null ? inscripcion.getDisciplina().getClasePrueba() : 0.0;
        return redondear(cuota + claseSuelta + clasePrueba);
    }

    /**
     * Calcula el costo final aplicando el descuento global de la bonificacion.
     * El recargo se calcula a nivel de DetallePago, por lo que no se aplica aqui.
     */
    public double calcularCostoFinal(PagoRegistroRequest request, Inscripcion inscripcion, double costoBase) {
        double descuentoFijo = 0.0;
        double descuentoPorcentaje = 0.0;
        if (Boolean.TRUE.equals(request.bonificacionAplicada()) && inscripcion.getBonificacion() != null) {
            Bonificacion b = inscripcion.getBonificacion();
            descuentoFijo = (b.getValorFijo() != null ? b.getValorFijo() : 0.0);
            descuentoPorcentaje = (b.getPorcentajeDescuento() != null ?
                    (b.getPorcentajeDescuento() / 100.0 * costoBase) : 0.0);
        }
        double costoFinal = costoBase - (descuentoFijo + descuentoPorcentaje);
        return redondear(costoFinal);
    }

    /**
     * Calcula el saldo restante: costo final menos la suma de pagos previos.
     */
    public double calcularSaldoRestante(double costoFinal, double sumPagosPrevios) {
        double saldo = costoFinal - sumPagosPrevios;
        return redondear(saldo);
    }

    private double redondear(double valor) {
        BigDecimal bd = new BigDecimal(valor).setScale(2, RoundingMode.HALF_UP);
        return bd.doubleValue();
    }
}

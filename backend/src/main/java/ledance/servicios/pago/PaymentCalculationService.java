package ledance.servicios.pago;

import ledance.entidades.Inscripcion;
import ledance.entidades.Recargo;
import ledance.dto.pago.request.PagoRegistroRequest;
import org.springframework.stereotype.Service;

@Service
public class PaymentCalculationService {

    /**
     * Calcula el costo base de una inscripción usando valores de la disciplina.
     */
    public double calcularCostoBase(Inscripcion inscripcion) {
        double cuota = (inscripcion.getDisciplina().getValorCuota() != null ? inscripcion.getDisciplina().getValorCuota() : 0.0);
        double matricula = (inscripcion.getDisciplina().getMatricula() != null ? inscripcion.getDisciplina().getMatricula() : 0.0);
        double claseSuelta = (inscripcion.getDisciplina().getClaseSuelta() != null ? inscripcion.getDisciplina().getClaseSuelta() : 0.0);
        double clasePrueba = (inscripcion.getDisciplina().getClasePrueba() != null ? inscripcion.getDisciplina().getClasePrueba() : 0.0);
        return cuota + matricula + claseSuelta + clasePrueba;
    }

    /**
     * Calcula el costo final aplicando descuento y recargo.
     */
    public double calcularCostoFinal(PagoRegistroRequest request, Inscripcion inscripcion, double costoBase) {
        double descuento = 0.0;
        if (Boolean.TRUE.equals(request.bonificacionAplicada()) && inscripcion.getBonificacion() != null) {
            // Suponemos que bonificacionAplicada ahora es un indicador para aplicar el descuento, pero el monto del descuento se obtiene del objeto Bonificacion.
            descuento = inscripcion.getBonificacion().getPorcentajeDescuento() / 100.0;
        }
        double incremento = 0.0;
        if (Boolean.TRUE.equals(request.recargoAplicado()) && inscripcion.getDisciplina().getRecargo() != null) {
            Recargo recargo = inscripcion.getDisciplina().getRecargo();
            if (recargo.getDetalles() != null && !recargo.getDetalles().isEmpty()) {
                // Para este ejemplo se utiliza el porcentaje del primer detalle
                incremento = recargo.getDetalles().get(0).getPorcentaje() / 100.0;
            }
        }
        return costoBase * (1 - descuento) * (1 + incremento);
    }

    /**
     * Calcula el saldo restante: costo final menos la suma de pagos previos.
     */
    public double calcularSaldoRestante(double costoFinal, double sumPagosPrevios) {
        return costoFinal - sumPagosPrevios;
    }
}

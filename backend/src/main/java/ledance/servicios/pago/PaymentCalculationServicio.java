package ledance.servicios.pago;

import ledance.dto.matricula.request.MatriculaRegistroRequest;
import ledance.dto.matricula.response.MatriculaResponse;
import ledance.entidades.*;
import ledance.servicios.detallepago.DetallePagoServicio;
import ledance.servicios.matricula.MatriculaServicio;
import ledance.servicios.mensualidad.MensualidadServicio;
import ledance.servicios.stock.StockServicio;
import ledance.dto.mensualidad.response.MensualidadResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.Optional;

@Service
public class PaymentCalculationServicio {

    private static final Logger log = LoggerFactory.getLogger(PaymentCalculationServicio.class);

    private final MatriculaServicio matriculaServicio;
    private final MensualidadServicio mensualidadServicio;
    private final StockServicio stockServicio;
    private final DetallePagoServicio detallePagoServicio;

    public PaymentCalculationServicio(MatriculaServicio matriculaServicio,
                                      MensualidadServicio mensualidadServicio,
                                      StockServicio stockServicio,
                                      DetallePagoServicio detallePagoServicio) {
        this.matriculaServicio = matriculaServicio;
        this.mensualidadServicio = mensualidadServicio;
        this.stockServicio = stockServicio;
        this.detallePagoServicio = detallePagoServicio;
    }

    private boolean esConceptoMensualidad(String concepto) {
        return concepto.contains("CUOTA")
                || concepto.contains("CLASE SUELTA")
                || concepto.contains("CLASE DE PRUEBA");
    }

    // Los métodos procesarMensualidad, procesarMatricula y procesarStock se mantienen igual que antes
    private void procesarMensualidad(Pago pago, DetallePago detalle) {
        if (pago.getInscripcion() == null) {
            log.warn("[procesarMensualidad] Pago id={} sin inscripción; no se procesa mensualidad.", pago.getId());
            return;
        }
        MensualidadResponse mensualidad = mensualidadServicio.buscarMensualidadPendientePorDescripcion(
                pago.getInscripcion(), detalle.getDescripcionConcepto());

        if (mensualidad != null) {
            double totalAbonado = pago.getDetallePagos().stream()
                    .filter(d -> d.getDescripcionConcepto().equalsIgnoreCase(detalle.getDescripcionConcepto()))
                    .mapToDouble(DetallePago::getaCobrar)
                    .sum();
            if (totalAbonado >= mensualidad.importeInicial() || detalle.getImportePendiente() == 0.0) {
                mensualidadServicio.marcarComoPagada(mensualidad.id(), pago.getFecha());
            } else {
                mensualidadServicio.actualizarAbonoParcial(mensualidad.id(), totalAbonado);
            }
        } else {
            mensualidadServicio.crearMensualidadPagada(pago.getInscripcion().getId(), detalle.getDescripcionConcepto(), pago.getFecha());
        }
    }

    private void procesarMatricula(Pago pago, DetallePago detalle) {
        if (pago.getAlumno() == null) {
            log.warn("[procesarMatricula] Pago sin alumno asignado, detalle id={}", detalle.getId());
            return;
        }
        String[] partes = detalle.getDescripcionConcepto().split(" ");
        if (partes.length >= 2) {
            try {
                int anio = Integer.parseInt(partes[1]);
                MatriculaResponse matricula = matriculaServicio.obtenerOMarcarPendiente(pago.getAlumno().getId());
                matriculaServicio.actualizarEstadoMatricula(matricula.id(),
                        new MatriculaRegistroRequest(matricula.alumnoId(), anio, true, pago.getFecha()));
            } catch (NumberFormatException e) {
                log.warn("[procesarMatricula] Año inválido en concepto '{}'.", detalle.getDescripcionConcepto());
            }
        }
    }

    private void procesarStock(DetallePago detalle) {
        int cantidad = 1;
        if (detalle.getCuota() != null) {
            try {
                cantidad = Integer.parseInt(detalle.getCuota().trim());
            } catch (NumberFormatException e) {
                log.warn("Cuota inválida para stock '{}'. Usando cantidad 1.", detalle.getDescripcionConcepto());
            }
        }
        stockServicio.reducirStock(detalle.getDescripcionConcepto(), cantidad);
    }

    private boolean existeStockConNombre(String nombre) {
        return stockServicio.obtenerStockPorNombre(nombre);
    }

    // Actualiza los totales del Pago según la suma de aCobrar y los importes pendientes de los detalles
    public void calcularYActualizarImportes(Pago pago) {
        double totalAbonado = 0.0;
        double totalPendiente = 0.0;

        for (DetallePago detalle : pago.getDetallePagos()) {
            totalAbonado += detalle.getaCobrar();
            totalPendiente += detalle.getImportePendiente() != null ? detalle.getImportePendiente() : 0.0;
        }

        // Para el nuevo pago: el monto es la suma de lo abonado (ej. 1500)
        // y el saldoRestante es la suma de los importes pendientes (ej. 1000 si partíamos de 2500 y se abonó 1500)
        pago.setMonto(totalAbonado);
        // Se ignora montoBasePago (solo de lectura) en el cálculo
        pago.setMontoPagado(totalAbonado);
        pago.setSaldoRestante(totalPendiente);

        if (pago.getSaldoRestante() <= 0.0) {
            pago.setEstadoPago(EstadoPago.HISTORICO);
        }
    }

    // Cálculo "completo" (usado en el registro inicial) basado en el importe (importeInicial y luego importePendiente)
    public void calcularImporte(DetallePago detalle, Inscripcion inscripcion) {
        double base = detalle.getMontoOriginal();
        double descuento = detallePagoServicio.calcularDescuento(detalle, base);
        double recargo = detalle.getRecargo() != null ? detallePagoServicio.obtenerValorRecargo(detalle, base) : 0.0;
        double importeInicialCalculado = base - descuento + recargo;

        procesarAbonoEnDetalle(detalle, detalle.getaCobrar(), importeInicialCalculado);

        if (detalle.getImportePendiente() <= 0.0) {
            detalle.setCobrado(true);
        }

        // Se determina el tipo, pero dado que el cálculo se hace mediante el importe, no afecta el cálculo
        determinarTipoDetalle(detalle.getDescripcionConcepto());
    }

    public void calcularImporte(DetallePago detalle) {
        calcularImporte(detalle, null);
    }

    // Método para aplicar un abono a un DetallePago ya existente (para pagos posteriores)
    public void aplicarAbono(DetallePago detalle, double montoAbono) {
        double currentPendiente = detalle.getImportePendiente();
        double abono = Math.min(montoAbono, currentPendiente); // limitar el abono al pendiente
        detalle.setaCobrar(abono);
        detalle.setImportePendiente(Math.max(currentPendiente - abono, 0.0));

        if (detalle.getImportePendiente() <= 0.0) {
            detalle.setCobrado(true);
            detalle.setImportePendiente(0.0);

            if (detalle.getTipo() == TipoDetallePago.MENSUALIDAD && detalle.getMensualidad() != null) {
                mensualidadServicio.marcarComoPagada(detalle.getMensualidad().getId(), LocalDate.now());
            }
            if (detalle.getTipo() == TipoDetallePago.MATRICULA && detalle.getMatricula() != null) {
                matriculaServicio.actualizarEstadoMatricula(
                        detalle.getMatricula().getId(),
                        new MatriculaRegistroRequest(detalle.getAlumno().getId(), detalle.getMatricula().getAnio(), true, LocalDate.now())
                );
            }
            if (detalle.getTipo() == TipoDetallePago.STOCK && detalle.getStock() != null) {
                stockServicio.reducirStock(detalle.getStock().getNombre(), 1);
            }
        }
    }

    // Método para determinar el tipo de detalle basado en la descripción (solo lectura)
    public TipoDetallePago determinarTipoDetalle(String descripcionConcepto) {
        if (descripcionConcepto == null) {
            return TipoDetallePago.CONCEPTO;
        }
        String conceptoNormalizado = descripcionConcepto.trim().toUpperCase();
        if (stockServicio.obtenerStockPorNombre(conceptoNormalizado(conceptoNormalizado(descripcionConcepto)))) {
            return TipoDetallePago.STOCK;
        } else if (conceptoNormalizado(conceptoNormalizado(descripcionConcepto)).startsWith("MATRICULA")) {
            return TipoDetallePago.MATRICULA;
        } else if (esMensualidad(conceptoNormalizado(descripcionConcepto))) {
            return TipoDetallePago.MENSUALIDAD;
        } else {
            return TipoDetallePago.CONCEPTO;
        }
    }

    private String conceptoNormalizado(String concepto) {
        return concepto.trim().toUpperCase();
    }

    private boolean esMensualidad(String conceptoNormalizado) {
        return conceptoNormalizado.contains("CUOTA") ||
                conceptoNormalizado.contains("CLASE SUELTA") ||
                conceptoNormalizado.contains("CLASE DE PRUEBA");
    }

    /**
     * Método principal que decide cómo calcular el importe (descuentos, recargos, etc.)
     * según el tipo de detalle (Mensualidad, Matricula, Stock u Otro)
     * y aplica el proceso de abono correspondiente.
     *
     * @param pago        La entidad Pago que agrupa los detalles (sirve para obtener inscripcion, alumno, etc.)
     * @param detalle     El DetallePago a calcular
     * @param inscripcion (Opcional) si deseas pasarla cuando la tengas (puede extraerse de pago.getInscripcion())
     */
    public void calcularImportesDetalle(Pago pago, DetallePago detalle, Inscripcion inscripcion) {
        // Normalizar la descripción para identificar el tipo de concepto
        String conceptoDesc = Optional.ofNullable(detalle.getDescripcionConcepto())
                .orElse("")
                .toUpperCase()
                .trim();

        // Determinar el tipo de detalle
        if (esConceptoMensualidad(conceptoDesc)) {
            calcularMensualidad(detalle, inscripcion, pago);
        } else if (existeStockConNombre(conceptoDesc)) {
            calcularStock(detalle);
        } else if (conceptoDesc.startsWith("MATRICULA")) {
            calcularMatricula(detalle, pago);
        } else {
            calcularConceptoGeneral(detalle);
        }
    }

    // ----------------------------------------------------------------
    // Ejemplo de submétodo: cálculo para Mensualidad
    // ----------------------------------------------------------------
    private void calcularMensualidad(DetallePago detalle, Inscripcion inscripcion, Pago pago) {
        double base = detalle.getMontoOriginal();

        // Si la Inscripcion trae bonificación, calculamos a partir de ahí;
        // en caso contrario, llamamos al método de detallePagoServicio para ver si tenía bonificación
        double descuento = (inscripcion != null && inscripcion.getBonificacion() != null)
                ? calcularDescuentoPorInscripcion(base, inscripcion)
                : detallePagoServicio.calcularDescuento(detalle, base);

        double recargo = (detalle.getRecargo() != null)
                ? detallePagoServicio.obtenerValorRecargo(detalle, base)
                : 0.0;

        double importeInicialCalculado = base - descuento + recargo;

        // Aplica la lógica de abono (actualiza importePendiente, aCobrar, etc.)
        procesarAbonoEnDetalle(detalle, detalle.getaCobrar(), importeInicialCalculado);

        // Asignar el tipo para uso posterior
        detalle.setTipo(TipoDetallePago.MENSUALIDAD);

        // Marcar como cobrado si se saldó
        detalle.setCobrado(detalle.getImportePendiente() == 0);

        // Llamada opcional para actualizar estados de la Mensualidad (pagada)
        procesarMensualidad(pago, detalle);
    }

    private double calcularDescuentoPorInscripcion(double base, Inscripcion inscripcion) {
        double descuentoFijo = Optional.ofNullable(inscripcion.getBonificacion().getValorFijo()).orElse(0.0);
        double descuentoPorcentaje = Optional.ofNullable(inscripcion.getBonificacion().getPorcentajeDescuento())
                .orElse(0) / 100.0 * base;
        return descuentoFijo + descuentoPorcentaje;
    }

    // ----------------------------------------------------------------
    // Ejemplo de submétodo: cálculo para Matricula
    // ----------------------------------------------------------------
    private void calcularMatricula(DetallePago detalle, Pago pago) {
        double importeInicialCalculado = detalle.getMontoOriginal();
        procesarAbonoEnDetalle(detalle, detalle.getaCobrar(), importeInicialCalculado);

        detalle.setTipo(TipoDetallePago.MATRICULA);
        detalle.setCobrado(detalle.getImportePendiente() == 0);

        procesarMatricula(pago, detalle);
    }

    // ----------------------------------------------------------------
    // Ejemplo de submétodo: cálculo para Stock
    // ----------------------------------------------------------------
    private void calcularStock(DetallePago detalle) {
        double base = Optional.ofNullable(detalle.getMontoOriginal()).orElse(0.0);
        // Para stock no aplicamos descuentos ni recargos
        double importeInicialCalculado = base;

        procesarAbonoEnDetalle(detalle, detalle.getaCobrar(), importeInicialCalculado);

        detalle.setTipo(TipoDetallePago.STOCK);
        detalle.setCobrado(detalle.getImportePendiente() == 0);

        procesarStock(detalle);
    }

    // ----------------------------------------------------------------
    // Ejemplo de submétodo: cálculo para Concepto General
    // ----------------------------------------------------------------
    private void calcularConceptoGeneral(DetallePago detalle) {
        double base = detalle.getMontoOriginal();
        double descuento = detallePagoServicio.calcularDescuento(detalle, base);
        double recargo = (detalle.getRecargo() != null)
                ? detallePagoServicio.obtenerValorRecargo(detalle, base)
                : 0.0;

        double importeInicialCalculado = base - descuento + recargo;
        procesarAbonoEnDetalle(detalle, detalle.getaCobrar(), importeInicialCalculado);

        detalle.setTipo(TipoDetallePago.CONCEPTO);
        detalle.setCobrado(detalle.getImportePendiente() == 0);
    }

    // ----------------------------------------------------------------
    // Lógica común para aplicar el abono (importePendiente, aCobrar, etc.)
    // ----------------------------------------------------------------
    private void procesarAbonoEnDetalle(DetallePago detalle, double montoAbono, double importeInicialCalculado) {
        if (detalle.getImporteInicial() == null) {
            detalle.setImporteInicial(importeInicialCalculado);
            double abono = Math.min(montoAbono, importeInicialCalculado);
            detalle.setaCobrar(abono);
            detalle.setImportePendiente(Math.max(importeInicialCalculado - abono, 0.0));
        } else {
            double currentPendiente = detalle.getImportePendiente() != null ? detalle.getImportePendiente() : 0.0;
            double abono = Math.min(montoAbono, currentPendiente);
            detalle.setaCobrar(abono);
            detalle.setImportePendiente(Math.max(currentPendiente - abono, 0.0));
        }

        if (detalle.getImportePendiente() <= 0.0) {
            detalle.setCobrado(true);
            detalle.setImportePendiente(0.0);
        }
    }
}

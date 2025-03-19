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

    public void calcularImportesDetalle(Pago pago, DetallePago detalle, Inscripcion inscripcion) {
        String concepto = Optional.ofNullable(detalle.getDescripcionConcepto()).orElse("").toUpperCase().trim();

        if (esConceptoMensualidad(concepto)) {
            calcularMensualidad(detalle, inscripcion, pago);
        } else if (existeStockConNombre(concepto)) {
            calcularStock(detalle);
        } else if (concepto.startsWith("MATRICULA")) {
            calcularMatricula(detalle, pago);
        } else {
            calcularConceptoGeneral(detalle);
        }
    }

    private boolean esConceptoMensualidad(String concepto) {
        return concepto.contains("CUOTA")
                || concepto.contains("CLASE SUELTA")
                || concepto.contains("CLASE DE PRUEBA");
    }

    private void calcularMensualidad(DetallePago detalle, Inscripcion inscripcion, Pago pago) {
        double base = detalle.getMontoOriginal();

        double descuento = (inscripcion != null && inscripcion.getBonificacion() != null)
                ? calcularDescuentoPorInscripcion(base, inscripcion)
                : detallePagoServicio.calcularDescuento(detalle, base);

        double recargo = (detalle.getRecargo() != null)
                ? detallePagoServicio.obtenerValorRecargo(detalle, base)
                : 0.0;

        double importeInicial = base - descuento + recargo;
        double importePendiente = importeInicial - detalle.getaCobrar();
        detalle.setImporteInicial(importeInicial);
        detalle.setImportePendiente(Math.max(importePendiente, 0.0));
        detalle.setTipo(TipoDetallePago.MENSUALIDAD);
        detalle.setCobrado(detalle.getImportePendiente() == 0);

        procesarMensualidad(pago, detalle);
    }

    private double calcularDescuentoPorInscripcion(double base, Inscripcion inscripcion) {
        double descuentoFijo = Optional.ofNullable(inscripcion.getBonificacion().getValorFijo()).orElse(0.0);
        double descuentoPorcentaje = Optional.ofNullable(inscripcion.getBonificacion().getPorcentajeDescuento()).orElse(0) / 100.0 * base;
        return descuentoFijo + descuentoPorcentaje;
    }

    private void calcularStock(DetallePago detalle) {
        double base = Optional.ofNullable(detalle.getMontoOriginal()).orElse(0.0);
        detalle.setImporteInicial(base);
        detalle.setImportePendiente(Math.max(base - detalle.getaCobrar(), 0.0));
        detalle.setTipo(TipoDetallePago.STOCK);
        detalle.setCobrado(detalle.getImportePendiente() == 0);

        procesarStock(detalle);
    }

    private void calcularMatricula(DetallePago detalle, Pago pago) {
        detalle.setImporteInicial(detalle.getMontoOriginal());
        detalle.setImportePendiente(Math.max(detalle.getMontoOriginal() - detalle.getaCobrar(), 0.0));
        detalle.setTipo(TipoDetallePago.MATRICULA);
        detalle.setCobrado(detalle.getImportePendiente() == 0);

        procesarMatricula(pago, detalle);
    }

    private void calcularConceptoGeneral(DetallePago detalle) {
        double base = detalle.getMontoOriginal();
        detalle.setImporteInicial(base);
        detalle.setImportePendiente(Math.max(base - detalle.getaCobrar(), 0.0));
        detalle.setTipo(TipoDetallePago.CONCEPTO);
        detalle.setCobrado(detalle.getImportePendiente() == 0);
    }

    private void procesarMensualidad(Pago pago, DetallePago detalle) {
        if (pago.getInscripcion() == null) {
            log.warn("[procesarMensualidad] Pago id={} sin inscripción; no se procesa mensualidad.", pago.getId());
            return;
        }

        MensualidadResponse mensualidad = mensualidadServicio.buscarMensualidadPendientePorDescripcion(pago.getInscripcion(), detalle.getDescripcionConcepto());

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
                matriculaServicio.actualizarEstadoMatricula(matricula.id(), new MatriculaRegistroRequest(matricula.alumnoId(), anio, true, pago.getFecha()));
            } catch (NumberFormatException e) {
                log.warn("[procesarMatricula] Año inválido en concepto '{}'.", detalle.getDescripcionConcepto());
            }
        }
    }

    private boolean existeStockConNombre(String nombre) {
        return stockServicio.obtenerStockPorNombre(nombre);
    }

    public void calcularYActualizarImportes(Pago pago) {
        double totalInicial = 0.0;
        double totalPendiente = 0.0;

        for (DetallePago detalle : pago.getDetallePagos()) {
            calcularImporte(detalle, pago.getInscripcion());
            totalInicial += detalle.getImporteInicial();
            totalPendiente += detalle.getImportePendiente();
        }

        pago.setMontoBasePago(totalInicial);
        pago.setSaldoRestante(totalPendiente);
        pago.setMontoPagado(totalInicial - totalPendiente);

        if (pago.getSaldoRestante() <= 0.0) {
            pago.setEstadoPago(EstadoPago.HISTORICO);
        }
    }

    public void calcularImporte(DetallePago detalle, Inscripcion inscripcion) {
        double base = detalle.getMontoOriginal();
        double descuento = detallePagoServicio.calcularDescuento(detalle, base);
        double recargo = detalle.getRecargo() != null ? detallePagoServicio.obtenerValorRecargo(detalle, base) : 0.0;

        double importeInicial = base - descuento + recargo;
        double importePendiente = importeInicial - detalle.getaCobrar();

        detalle.setImporteInicial(importeInicial);
        detalle.setImportePendiente(Math.max(importePendiente, 0.0));

        if (detalle.getImportePendiente() <= 0.0) {
            detalle.setCobrado(true);
        }

        determinarTipoDetalle(detalle.getDescripcionConcepto());
    }

    public void calcularImporte(DetallePago detalle) {
        calcularImporte(detalle, null);
    }

    public void aplicarAbono(DetallePago detalle, double montoAbono) {
        double importePendiente = detalle.getImportePendiente();
        double nuevoImportePendiente = importePendiente - montoAbono;

        detalle.setaCobrar(montoAbono);
        detalle.setImportePendiente(Math.max(nuevoImportePendiente, 0.0));

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
}

package ledance.servicios.pago;

import jakarta.persistence.EntityNotFoundException;
import jakarta.persistence.PersistenceContext;
import jakarta.transaction.Transactional;
import ledance.entidades.*;
import ledance.repositorios.DetallePagoRepositorio;
import ledance.repositorios.MatriculaRepositorio;
import ledance.servicios.detallepago.DetallePagoServicio;
import ledance.servicios.mensualidad.MensualidadServicio;
import ledance.servicios.stock.StockServicio;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.*;

/**
 * Refactor del servicio PaymentCalculationServicio.
 * <p>
 * Se unifican las operaciones clave:
 * - Calculo del importe inicial, validacion y aplicacion de descuentos/recargos.
 * - Procesamiento del abono, actualizando el estado y el importe pendiente.
 * - Procesamiento del detalle segun su tipo (MENSUALIDAD, MATRICULA, STOCK o CONCEPTO).
 * - Reatach de asociaciones para garantizar que las entidades esten en estado managed.
 */
@Service
public class PaymentCalculationServicio {

    private final MatriculaRepositorio matriculaRepositorio;
    @PersistenceContext
    private jakarta.persistence.EntityManager entityManager;

    private static final Logger log = LoggerFactory.getLogger(PaymentCalculationServicio.class);

    private final MensualidadServicio mensualidadServicio;
    private final StockServicio stockServicio;
    private final DetallePagoServicio detallePagoServicio;
    private final DetallePagoRepositorio detallePagoRepositorio;

    public PaymentCalculationServicio(MensualidadServicio mensualidadServicio,
                                      StockServicio stockServicio,
                                      DetallePagoServicio detallePagoServicio,
                                      DetallePagoRepositorio detallePagoRepositorio, MatriculaRepositorio matriculaRepositorio) {
        this.mensualidadServicio = mensualidadServicio;
        this.stockServicio = stockServicio;
        this.detallePagoServicio = detallePagoServicio;
        this.detallePagoRepositorio = detallePagoRepositorio;
        this.matriculaRepositorio = matriculaRepositorio;
    }

    // ============================================================
    // METODO AUXILIAR: CALCULAR IMPORTE INICIAL
    // ============================================================

    /**
     * Calcula el importeInicial segun la formula:
     * importeInicial = valorBase – descuento + recargo.
     * Para matricula, se asume que no se aplican descuentos ni recargos.
     *
     * @param detalle     El DetallePago.
     * @param inscripcion (Opcional) La inscripcion para aplicar descuentos en mensualidades.
     * @param esMatricula Si es true, se omiten descuentos/recargos.
     * @return Importe inicial calculado.
     */
    public double calcularImporteInicial(DetallePago detalle, Inscripcion inscripcion, boolean esMatricula) {
        log.info("[calcularImporteInicial] Iniciando calculo para DetallePago id={}", detalle.getId());
        double base = detalle.getValorBase();
        log.info("[calcularImporteInicial] Valor base obtenido: {} para DetallePago id={}", base, detalle.getId());

        if (!detalle.getTieneRecargo()) {
            detalle.setTieneRecargo(false);
            log.info("[calcularImporteInicial] Se omite recargo para Detalle id={}", detalle.getId());
        }
        if (esMatricula) {
            log.info("[calcularImporteInicial] Matricula detectada; retornando base sin modificaciones.");
            return base;
        }

        double descuento;
        if (inscripcion != null && inscripcion.getBonificacion() != null) {
            descuento = calcularDescuentoPorInscripcion(base, inscripcion);
            log.info("[calcularImporteInicial] Descuento por inscripcion: {} para DetallePago id={}", descuento, detalle.getId());
        } else {
            descuento = detallePagoServicio.calcularDescuento(detalle, base);
            log.info("[calcularImporteInicial] Descuento calculado: {} para DetallePago id={}", descuento, detalle.getId());
        }
        double recargo = detalle.getRecargo() != null ? detallePagoServicio.obtenerValorRecargo(detalle, base) : 0.0;
        log.info("[calcularImporteInicial] Recargo obtenido: {} para DetallePago id={}", recargo, detalle.getId());

        double importeInicial = base - descuento + recargo;
        log.info("[calcularImporteInicial] Importe Inicial final calculado: {} para DetallePago id={}", importeInicial, detalle.getId());
        return importeInicial;
    }

    /**
     * Procesa el abono de un detalle: valida el monto, actualiza aCobrar e importePendiente
     * y ajusta el estado del detalle (marcando como cobrado si corresponde).
     */
    void procesarAbono(DetallePago detalle, Double montoAbono, Double importeInicialCalculado) {
        log.info("[procesarAbono] INICIO - Procesando abono para DetallePago ID: {}", detalle.getId());
        if (montoAbono == null || montoAbono < 0) {
            log.error("[procesarAbono] Monto de abono invalido: {}", montoAbono);
            throw new IllegalArgumentException("Monto del abono invalido.");
        }
        detalle.setImporteInicial(importeInicialCalculado);

        Double importePendienteActual = (detalle.getImportePendiente() == null)
                ? detalle.getImporteInicial()
                : detalle.getImportePendiente();
        if (importePendienteActual == null) {
            throw new IllegalStateException("No se pudo determinar el importe pendiente.");
        }
        detalle.setACobrar(montoAbono);
        double nuevoPendiente = importePendienteActual - montoAbono;
        detalle.setImportePendiente(nuevoPendiente);
        if (nuevoPendiente <= 0) {
            detalle.setImportePendiente(0.0);
            detalle.setEstadoPago(EstadoPago.HISTORICO);
            detalle.setCobrado(true);
            if (detalle.getTipo() == TipoDetallePago.MENSUALIDAD && detalle.getMensualidad() != null) {
                detalle.getMensualidad().setEstado(EstadoMensualidad.PAGADO);
            }
            if (detalle.getTipo() == TipoDetallePago.MATRICULA && detalle.getMatricula() != null) {
                detalle.getMatricula().setPagada(true);
            }
        } else {
            detalle.setCobrado(false);
        }
        log.info("[procesarAbono] FIN - Detalle id={} procesado. Nuevo pendiente: {}, Cobrado: {}",
                detalle.getId(), detalle.getImportePendiente(), detalle.getCobrado());
    }

    /**
     * Unifica el procesamiento y calculo de un DetallePago.
     * Se normaliza la descripcion, se determina el tipo, se reatachan asociaciones
     * y se invoca la logica especifica segun el tipo.
     */
    // --------------------------------------------------
    // 4) Orquesta todo el proceso para cada detalle
    @Transactional
    public void procesarYCalcularDetalle(DetallePago detalle, Inscripcion inscripcion) {
        log.info("[procesarYCalcularDetalle] INICIO id={}", detalle.getId());

        // Normalización de descripción
        String desc = Optional.ofNullable(detalle.getDescripcionConcepto())
                .orElse("").trim().toUpperCase();
        detalle.setDescripcionConcepto(desc);

        // Tipo si falta
        if (detalle.getTipo() == null) {
            detalle.setTipo(determinarTipoDetalle(desc));
        }

        // Reatach de Concepto/SubConcepto
        if (detalle.getConcepto() != null && detalle.getConcepto().getId() != null) {
            Concepto c = entityManager.find(Concepto.class, detalle.getConcepto().getId());
            if (c == null) throw new EntityNotFoundException("Concepto id=" + detalle.getConcepto().getId());
            detalle.setConcepto(c);
            if (detalle.getSubConcepto() == null && c.getSubConcepto() != null) {
                detalle.setSubConcepto(c.getSubConcepto());
            }
        }

        // Flag recargo
        detalle.setTieneRecargo(Boolean.TRUE.equals(detalle.getTieneRecargo()));

        // Llamada al cálculo según tipo
        switch (detalle.getTipo()) {
            case MENSUALIDAD:
                double pendienteReq = detalle.getImportePendiente();
                double aCobrarReq  = detalle.getACobrar();
                calcularMensualidad(detalle, inscripcion, pendienteReq, aCobrarReq);

                if (desc.contains("CUOTA")) {
                    Mensualidad m = mensualidadServicio.obtenerOMarcarPendienteMensualidad(
                            detalle.getAlumno().getId(), desc);
                    mensualidadServicio.procesarAbonoMensualidad(m, detalle);
                }
                break;

            case MATRICULA:
                calcularMatricula(detalle, detalle.getPago());
                break;

            case STOCK:
                calcularStock(detalle);
                break;

            default:
                detalle.setTipo(TipoDetallePago.CONCEPTO);
                calcularConceptoGeneral(detalle);
                if (desc.contains("CLASE SUELTA")) {
                    double nuevoCredito = Optional.ofNullable(detalle.getAlumno().getCreditoAcumulado()).orElse(0.0)
                            + Optional.ofNullable(detalle.getACobrar()).orElse(0.0);
                    detalle.getAlumno().setCreditoAcumulado(nuevoCredito);
                }
                break;
        }

        // Consistencia final
        detalle.setImporteInicial(Optional.ofNullable(detalle.getImporteInicial())
                .filter(v -> v > 0).orElse(Optional.ofNullable(detalle.getValorBase()).orElse(0.0)));
        detalle.setACobrar(Optional.ofNullable(detalle.getACobrar()).filter(v -> v > 0).orElse(0.0));
        boolean finalCobrado = Optional.ofNullable(detalle.getImportePendiente()).orElse(0.0) <= 0;
        detalle.setCobrado(finalCobrado);
        if (finalCobrado) {
            detalle.setEstadoPago(EstadoPago.HISTORICO);
            detalle.setImportePendiente(0.0);
        }

        detallePagoRepositorio.save(detalle);
        log.info("[procesarYCalcularDetalle] FIN id={}", detalle.getId());
    }

    @Transactional
    public void calcularMatricula(DetallePago detalle, Pago pago) {
        // … tu código de base/calculo de importeInicial …
        double base;
        if (detalle.getMensualidad() == null) {
            base = calcularImporteInicial(detalle, null, true);
        } else {
            base = calcularImporteInicial(detalle, detalle.getMensualidad().getInscripcion(), true);
        }
        detalle.setImporteInicial(base);

        // **AQUÍ** restamos el aCobrar
        double cobro = Optional.ofNullable(detalle.getACobrar()).orElse(0.0);
        double pendiente = base - cobro;
        detalle.setImportePendiente(pendiente);

        // flags
        detalle.setCobrado(pendiente <= 0);
        detalle.setEstadoPago(pendiente <= 0
                ? EstadoPago.HISTORICO
                : EstadoPago.ACTIVO);

        // matrícula pagada?
        if (pendiente <= 0 && detalle.getMatricula() != null) {
            detalle.getMatricula().setPagada(true);
            matriculaRepositorio.save(detalle.getMatricula());
        }

        detallePagoRepositorio.save(detalle);
    }

    void calcularConceptoGeneral(DetallePago detalle) {
        double importeInicialCalculado = calcularImporteInicial(detalle, null, false);
        procesarAbono(detalle, detalle.getACobrar(), importeInicialCalculado);
        detalle.setCobrado(detalle.getImportePendiente() == 0.0);
    }

    private int parseCantidad(String cuota) {
        try {
            return Integer.parseInt(cuota.trim());
        } catch (NumberFormatException e) {
            return 1;
        }
    }

    // ============================================================
    // METODOS DE ACTUALIZACION DE TOTALES Y ESTADOS DEL PAGO
    // ============================================================

    /**
     * Determina el tipo de detalle basado en la descripcion normalizada.
     */
    @Transactional
    public TipoDetallePago determinarTipoDetalle(String descripcionConcepto) {
        if (descripcionConcepto == null) {
            return TipoDetallePago.CONCEPTO;
        }
        String conceptoNorm = descripcionConcepto.trim().toUpperCase();
        if (conceptoNorm.startsWith("MATRICULA")) {
            return TipoDetallePago.MATRICULA;
        }
        if (conceptoNorm.contains("CUOTA") ||
                conceptoNorm.contains("CLASE SUELTA") ||
                conceptoNorm.contains("CLASE DE PRUEBA")) {
            return TipoDetallePago.MENSUALIDAD;
        }
        if (existeStockConNombre(conceptoNorm)) {
            return TipoDetallePago.STOCK;
        }
        return TipoDetallePago.CONCEPTO;
    }

    private boolean existeStockConNombre(String nombre) {
        return stockServicio.obtenerStockPorNombre(nombre);
    }

    private double calcularDescuentoPorInscripcion(double base, Inscripcion inscripcion) {
        double descuentoFijo = Optional.ofNullable(inscripcion.getBonificacion().getValorFijo()).orElse(0.0);
        double descuentoPorcentaje = Optional.ofNullable(inscripcion.getBonificacion().getPorcentajeDescuento())
                .orElse(0) / 100.0 * base;
        return descuentoFijo + descuentoPorcentaje;
    }

    @Transactional
    public void reatacharAsociaciones(DetallePago detalle, Pago pago) {
        if (detalle.getACobrar() == null) {
            detalle.setACobrar(0.0);
        }
        if (detalle.getAlumno() == null && pago.getAlumno() != null) {
            detalle.setAlumno(pago.getAlumno());
        }
        if (detalle.getConcepto() != null && detalle.getConcepto().getId() != null) {
            Concepto managedConcepto = entityManager.find(Concepto.class, detalle.getConcepto().getId());
            if (managedConcepto != null) {
                detalle.setConcepto(managedConcepto);
                if (detalle.getSubConcepto() == null && managedConcepto.getSubConcepto() != null) {
                    detalle.setSubConcepto(managedConcepto.getSubConcepto());
                }
            }
        }
        if (detalle.getDescripcionConcepto().contains("CUOTA") &&
                detalle.getMensualidad() != null &&
                detalle.getMensualidad().getId() != null) {
            Mensualidad managedMensualidad = entityManager.find(Mensualidad.class, detalle.getMensualidad().getId());
            if (managedMensualidad != null) {
                detalle.setMensualidad(managedMensualidad);
            }
        }
        if (detalle.getMatricula() != null && detalle.getMatricula().getId() != null) {
            Matricula managedMatricula = entityManager.find(Matricula.class, detalle.getMatricula().getId());
            if (managedMatricula != null) {
                detalle.setMatricula(managedMatricula);
            }
        }
        if (detalle.getStock() != null && detalle.getStock().getId() != null) {
            Stock managedStock = entityManager.find(Stock.class, detalle.getStock().getId());
            if (managedStock != null) {
                detalle.setStock(managedStock);
            }
        }
    }

    @Transactional
    public void calcularStock(DetallePago detalle) {
        log.info("[calcularStock] Iniciando calculo para DetallePago id={} de tipo STOCK", detalle.getId());

        // Calcular el importe inicial basado en la logica especifica para STOCK
        double importeInicialCalculado = calcularImporteInicial(detalle, null, false);
        log.info("[calcularStock] Detalle id={} - Importe Inicial Calculado: {}", detalle.getId(), importeInicialCalculado);

        // Procesar abono para el detalle STOCK (unica llamada para este tipo)
        log.info("[calcularStock] Detalle id={} - Procesando abono para STOCK. ACobrar: {}, importeInicialCalculado: {}",
                detalle.getId(), detalle.getACobrar(), importeInicialCalculado);
        procesarAbono(detalle, detalle.getACobrar(), importeInicialCalculado);

        // Marcar como procesado (podrias setear una bandera en el detalle, por ejemplo, detalle.setAbonoProcesado(true))
        // Aqui usamos el hecho de que el detalle ya esta cobrado y su importe pendiente es 0.
        boolean estaCobrado = (detalle.getImportePendiente() != null && detalle.getImportePendiente() == 0.0);
        detalle.setCobrado(estaCobrado);
        log.info("[calcularStock] Detalle id={} - Estado luego de abono: Cobrado={}, Importe pendiente: {}",
                detalle.getId(), estaCobrado, detalle.getImportePendiente());

        // Procesar reduccion de stock
        procesarStockInterno(detalle);
    }

    /**
     * Calcula el importe inicial de una mensualidad para un DetallePago, usando la inscripcion (si esta disponible)
     * para aplicar descuentos y recargos.
     */
    // --------------------------------------------------
// 3) Cálculo de mensualidad usando SIEMPRE los valores del frontend
    @Transactional
    public void calcularMensualidad(
            DetallePago detalle,
            Inscripcion inscripcion,
            double pendienteInicialRequest,
            double aCobrarRequest
    ) {
        log.info("[calcularMensualidad] START id={} pendienteRequest={} aCobrarRequest={} recargoFlag={}",
                detalle.getId(),
                pendienteInicialRequest,
                aCobrarRequest,
                detalle.getTieneRecargo());

        // 1) Base: valor puro del frontend
        double base = pendienteInicialRequest;

        // 2) Recargo (si corresponde)
        double recargo = 0.0;
        if (Boolean.TRUE.equals(detalle.getTieneRecargo()) && detalle.getRecargo() != null) {
            recargo = MensualidadServicio.validarRecargo(base, detalle.getRecargo());
            log.debug("[calcularMensualidad] recargo calculado={}", recargo);
        }

        // 3) Pago recibido (del frontend)

        // 4) Nuevo pendiente = base + recargo – pago
        double pendienteNuevo = Math.max(0.0, base + recargo - aCobrarRequest);

        // 5) Actualizar entidad
        detalle.setImportePendiente(pendienteNuevo);
        boolean cobrado = pendienteNuevo <= 0.0;
        detalle.setCobrado(cobrado);
        if (cobrado) {
            detalle.setEstadoPago(EstadoPago.HISTORICO);
        }

        log.info("[calcularMensualidad] END id={} nuevoPendiente={} cobrado={}",
                detalle.getId(), pendienteNuevo, cobrado);
    }

    private void procesarStockInterno(DetallePago detalle) {
        log.info("[procesarStockInterno] Procesando reduccion de stock para DetallePago id={}", detalle.getId());
        int cantidad = parseCantidad(detalle.getCuotaOCantidad());
        log.info("[procesarStockInterno] Detalle id={} - Cantidad a reducir del stock: {}", detalle.getId(), cantidad);
        stockServicio.reducirStock(detalle.getDescripcionConcepto(), cantidad);
    }

}

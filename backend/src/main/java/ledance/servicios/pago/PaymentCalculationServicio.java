package ledance.servicios.pago;

import jakarta.transaction.Transactional;
import ledance.entidades.*;
import ledance.servicios.stock.StockServicio;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

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

    private static final Logger log = LoggerFactory.getLogger(PaymentCalculationServicio.class);

    private final StockServicio stockServicio;

    public PaymentCalculationServicio(
            StockServicio stockServicio) {
        this.stockServicio = stockServicio;
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
        String conceptoNorm = descripcionConcepto.trim().toUpperCase();
        if (conceptoNorm.contains("MATRICULA")) {
            return TipoDetallePago.MATRICULA;
        }
        if (conceptoNorm.contains("CUOTA") || conceptoNorm.contains("CLASE SUELTA") || conceptoNorm.contains("CLASE DE PRUEBA")) {
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

    @Transactional
    public void calcularStock(DetallePago detalle) {
        log.info("[calcularStock] Iniciando calculo para DetallePago id={} de tipo STOCK", detalle.getId());

        // Procesar reduccion de stock
        procesarStockInterno(detalle);
    }

    private void procesarStockInterno(DetallePago detalle) {
        log.info("[procesarStockInterno] Procesando reduccion de stock para DetallePago id={}", detalle.getId());
        int cantidad = parseCantidad(detalle.getCuotaOCantidad());
        log.info("[procesarStockInterno] Detalle id={} - Cantidad a reducir del stock: {}", detalle.getId(), cantidad);
        stockServicio.reducirStock(detalle.getDescripcionConcepto(), cantidad);
    }

}

package ledance.servicios.pago;

import jakarta.persistence.EntityNotFoundException;
import jakarta.persistence.PersistenceContext;
import jakarta.transaction.Transactional;
import ledance.entidades.*;
import ledance.repositorios.AlumnoRepositorio;
import ledance.repositorios.MatriculaRepositorio;
import ledance.servicios.matricula.MatriculaServicio;
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
    private final AlumnoRepositorio alumnoRepositorio;
    private final MatriculaServicio matriculaServicio;
    @PersistenceContext
    private jakarta.persistence.EntityManager entityManager;

    private static final Logger log = LoggerFactory.getLogger(PaymentCalculationServicio.class);

    private final MensualidadServicio mensualidadServicio;
    private final StockServicio stockServicio;

    public PaymentCalculationServicio(MensualidadServicio mensualidadServicio,
                                      StockServicio stockServicio,
                                      MatriculaRepositorio matriculaRepositorio, AlumnoRepositorio alumnoRepositorio, MatriculaServicio matriculaServicio) {
        this.mensualidadServicio = mensualidadServicio;
        this.stockServicio = stockServicio;
        this.matriculaRepositorio = matriculaRepositorio;
        this.alumnoRepositorio = alumnoRepositorio;
        this.matriculaServicio = matriculaServicio;
    }

    /**
     * Unifica el procesamiento y calculo de un DetallePago.
     * Se normaliza la descripcion, se determina el tipo, se reatachan asociaciones
     * y se invoca la logica especifica segun el tipo.
     */
    // --------------------------------------------------
    // 4) Orquesta todo el proceso para cada detalle
    @Transactional
    public void procesarDetalle(DetallePago detalle) {
        log.info("[procesarYCalcularDetalle] INICIO {}", detalle);

        String desc = detalle.getDescripcionConcepto();
        detalle.setDescripcionConcepto(desc);

        detalle.setTipo(determinarTipoDetalle(desc));

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

        switch (detalle.getTipo()) {
            case MENSUALIDAD:
                if (desc.contains("CUOTA")) {
                    Mensualidad m = mensualidadServicio.obtenerOMarcarPendienteMensualidad(detalle.getAlumno().getId(), desc);
                    mensualidadServicio.procesarAbonoMensualidad(m, detalle);
                }
                if (desc.contains("CLASE SUELTA")) {
                    double nuevoCredito = Optional.ofNullable(detalle.getAlumno().getCreditoAcumulado()).orElse(0.0) + Optional.ofNullable(detalle.getACobrar()).orElse(0.0);
                    detalle.getAlumno().setCreditoAcumulado(nuevoCredito);
                }
                detalle.setTipo(TipoDetallePago.MENSUALIDAD);
                break;
            case STOCK:
                calcularStock(detalle);
                detalle.setTipo(TipoDetallePago.STOCK);
                break;
            case MATRICULA:
                calcularMatricula(detalle);
                detalle.setTipo(TipoDetallePago.MATRICULA);
                break;
            default:
                detalle.setTipo(TipoDetallePago.CONCEPTO);
                break;
        }

        log.info("[procesarYCalcularDetalle] FIN id={}", detalle.getId());
        log.info("[procesarYCalcularDetalle] FIN {}", detalle);
    }

    @Transactional
    public void calcularMatricula(DetallePago detalle) {

        MesAnio mesAnio = extraerAnio(detalle.getDescripcionConcepto());

        Matricula matricula = matriculaServicio.obtenerOMarcarPendienteMatricula(detalle.getAlumno().getId(), mesAnio.anio);
        detalle.getAlumno().setCreditoAcumulado(0.0);
        detalle.setMatricula(matricula);
        alumnoRepositorio.save(detalle.getAlumno());
        if (detalle.getImportePendiente() <= 0) {
            matricula.setPagada(true);
            matriculaRepositorio.save(matricula);
        }
    }

    private MesAnio extraerAnio(String descripcion) {
        if (descripcion == null || descripcion.isBlank()) {
            return null;
        }

        // 1) Normalizar y dividir por espacios
        String[] partes = descripcion.trim().toUpperCase().split("\\s+");
        String tokenFinal = partes[partes.length - 1];

        // 2) Intentar parsear un año de 4 dígitos
        try {
            if (tokenFinal.matches("\\d{4}")) {
                int anio = Integer.parseInt(tokenFinal);
                // 3) Devolver MesAnio; uso mes=1 como placeholder
                return new MesAnio(1, anio);
            } else {
                log.warn("[extraerAnio] Último token no es un año válido: {}", tokenFinal);
            }
        } catch (NumberFormatException e) {
            log.error("[extraerAnio] Error al parsear año: {}", tokenFinal, e);
        }
        return null;
    }

    private Integer convertirNombreMesANumero(String mesStr) {
        Map<String, Integer> meses = Map.ofEntries(
                Map.entry("ENERO", 1),
                Map.entry("FEBRERO", 2),
                Map.entry("MARZO", 3),
                Map.entry("ABRIL", 4),
                Map.entry("MAYO", 5),
                Map.entry("JUNIO", 6),
                Map.entry("JULIO", 7),
                Map.entry("AGOSTO", 8),
                Map.entry("SEPTIEMBRE", 9),
                Map.entry("OCTUBRE", 10),
                Map.entry("NOVIEMBRE", 11),
                Map.entry("DICIEMBRE", 12)
        );
        return meses.get(mesStr);
    }

    private record MesAnio(int mes, int anio) {
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

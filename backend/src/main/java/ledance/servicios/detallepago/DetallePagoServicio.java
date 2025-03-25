package ledance.servicios.detallepago;

import ledance.dto.pago.response.DetallePagoResponse;
import ledance.entidades.DetallePago;
import ledance.entidades.Recargo;
import ledance.entidades.TipoDetallePago;
import ledance.repositorios.DetallePagoRepositorio;
import org.flywaydb.core.internal.util.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

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
        // Usar el campo unificado para la descripcion del concepto
        String conceptoDesc = (detalle.getDescripcionConcepto() != null)
                ? detalle.getDescripcionConcepto()
                : "N/A";
        log.info("[calcularImporte] Iniciando calculo para DetallePago id={} (Concepto: '{}')",
                detalle.getId(), conceptoDesc);

        double base = Optional.ofNullable(detalle.getValorBase()).orElse(0.0);
        log.info("[calcularImporte] Base para DetallePago id={} es {}", detalle.getId(), base);

        double descuento;
        // Ahora se utiliza la bonificacion asignada directamente en el DetallePago
        if (TipoDetallePago.MENSUALIDAD.equals(detalle.getTipo())
                && detalle.getBonificacion() != null) {
            double descuentoFijo = (detalle.getBonificacion().getValorFijo() != null)
                    ? detalle.getBonificacion().getValorFijo() : 0.0;
            double descuentoPorcentaje = (detalle.getBonificacion().getPorcentajeDescuento() != null)
                    ? (detalle.getBonificacion().getPorcentajeDescuento() / 100.0 * base) : 0.0;
            descuento = descuentoFijo + descuentoPorcentaje;
            log.info("[calcularImporte] Detalle id={} (Mensualidad): Descuento calculado basado en bonificacion = {}",
                    detalle.getId(), descuento);
        } else {
            descuento = calcularDescuento(detalle, base);
            log.info("[calcularImporte] Detalle id={} (No Mensualidad): Descuento calculado = {}",
                    detalle.getId(), descuento);
        }

        double recargo = (detalle.getRecargo() != null) ? obtenerValorRecargo(detalle, base) : 0.0;
        log.info("[calcularImporte] Detalle id={} : Recargo calculado = {}", detalle.getId(), recargo);

        double importeInicial = base - descuento + recargo;
        log.info("[calcularImporte] Detalle id={} : ImporteInicial calculado = {}", detalle.getId(), importeInicial);
        detalle.setImporteInicial(importeInicial);

        if (detalle.getImportePendiente() == null) {
            detalle.setImportePendiente(importeInicial);
            log.info("[calcularImporte] Detalle id={} : ImportePendiente no definido, se asigna = {}",
                    detalle.getId(), importeInicial);
        }
    }

    public double calcularDescuento(DetallePago detalle, double base) {
        if (detalle.getBonificacion() != null) {
            double descuentoFijo = detalle.getBonificacion().getValorFijo() != null ? detalle.getBonificacion().getValorFijo() : 0.0;
            double descuentoPorcentaje = detalle.getBonificacion().getPorcentajeDescuento() != null ?
                    (detalle.getBonificacion().getPorcentajeDescuento() / 100.0 * base) : 0.0;
            double totalDescuento = descuentoFijo + descuentoPorcentaje;
            log.info("[calcularDescuento] Detalle id={} | Descuento fijo={} | %={} | Total Descuento={}",
                    detalle.getId(), descuentoFijo, descuentoPorcentaje, totalDescuento);
            return totalDescuento;
        }
        log.info("[calcularDescuento] Detalle id={} sin bonificacion, descuento=0", detalle.getId());
        return 0.0;
    }

    public double obtenerValorRecargo(DetallePago detalle, double base) {
        Recargo recargo = detalle.getRecargo();
        if (recargo != null) {
            int diaActual = LocalDate.now().getDayOfMonth();
            log.info("[obtenerValorRecargo] Detalle id={} | Dia actual={} | Dia de aplicacion={}",
                    detalle.getId(), diaActual, recargo.getDiaDelMesAplicacion());
            if (diaActual != recargo.getDiaDelMesAplicacion()) {
                log.info("[obtenerValorRecargo] Dia actual no coincide; recargo=0");
                return 0.0;
            }
            double recargoFijo = recargo.getValorFijo() != null ? recargo.getValorFijo() : 0.0;
            double recargoPorcentaje = recargo.getPorcentaje() != null ? (recargo.getPorcentaje() / 100.0 * base) : 0.0;
            double totalRecargo = recargoFijo + recargoPorcentaje;
            log.info("[obtenerValorRecargo] Detalle id={} | Recargo fijo={} | %={} | Total Recargo={}",
                    detalle.getId(), recargoFijo, recargoPorcentaje, totalRecargo);
            return totalRecargo;
        }
        log.info("[obtenerValorRecargo] Detalle id={} sin recargo; recargo=0", detalle.getId());
        return 0.0;
    }

    private double importeRedondeado(double importe) {
        return BigDecimal.valueOf(importe).setScale(2, RoundingMode.HALF_UP).doubleValue();
    }

    public List<DetallePagoResponse> filtrarDetalles(LocalDate fechaRegistroDesde,
                                                     LocalDate fechaRegistroHasta,
                                                     String detalleConcepto, // Filtra por parte del nombre en conceptoEntity.descripcion
                                                     String stock,
                                                     String subConcepto,     // Texto para filtrar por sub concepto
                                                     String disciplina) {
        log.info("Inicio del metodo filtrarDetalles con parametros:");
        log.info("  fechaRegistroDesde: {}", fechaRegistroDesde);
        log.info("  fechaRegistroHasta: {}", fechaRegistroHasta);
        log.info("  detalleConcepto: {}", detalleConcepto);
        log.info("  stock: {}", stock);
        log.info("  subConcepto: {}", subConcepto);
        log.info("  disciplina: {}", disciplina);

        Specification<DetallePago> spec = Specification.where(null);

        // Filtrado por rango de fecha en DetallePago
        if (fechaRegistroDesde != null) {
            log.info("Aplicando filtro: fechaRegistro >= {}", fechaRegistroDesde);
            spec = spec.and((root, query, cb) ->
                    cb.greaterThanOrEqualTo(root.get("fechaRegistro"), fechaRegistroDesde)
            );
        }
        if (fechaRegistroHasta != null) {
            log.info("Aplicando filtro: fechaRegistro <= {}", fechaRegistroHasta);
            spec = spec.and((root, query, cb) ->
                    cb.lessThanOrEqualTo(root.get("fechaRegistro"), fechaRegistroHasta)
            );
        }

        // Filtrado por stock: se filtra por detalles cuyo tipo sea STOCK
        if (StringUtils.hasText(stock)) {
            log.info("Aplicando filtro: tipo = {}", TipoDetallePago.STOCK);
            spec = spec.and((root, query, cb) ->
                    cb.equal(root.get("tipo"), TipoDetallePago.STOCK)
            );
        }

        // Logica de filtrado para Concepto/SubConcepto:
        // 1. Si se proporciona subConcepto, se filtra por el campo de la relacion SubConcepto.
        // 2. Si no se proporciono subConcepto pero si detalleConcepto, se filtra por el concepto.
        if (StringUtils.hasText(subConcepto)) {
            String pattern = "%" + subConcepto.toLowerCase() + "%";
            log.info("Aplicando filtro prioritario: subConcepto.descripcion LIKE {}", pattern);
            spec = spec.and((root, query, cb) ->
                    cb.like(cb.lower(root.get("subConcepto").get("descripcion")), pattern)
            );
        } else if (StringUtils.hasText(detalleConcepto)) {
            String pattern = "%" + detalleConcepto.toLowerCase() + "%";
            log.info("Aplicando filtro: descripcionConcepto LIKE {}", pattern);
            spec = spec.and((root, query, cb) ->
                    cb.like(cb.lower(root.get("descripcionConcepto")), pattern)
            );
        }

        // Filtrado por disciplina (tipo de detalle)
        if (StringUtils.hasText(disciplina)) {
            try {
                TipoDetallePago tipo = TipoDetallePago.valueOf(disciplina.toUpperCase());
                log.info("Aplicando filtro: tipo = {}", tipo);
                spec = spec.and((root, query, cb) ->
                        cb.equal(root.get("tipo"), tipo)
                );
            } catch (IllegalArgumentException ex) {
                log.warn("Valor de disciplina no valido: {}", disciplina);
            }
        }

        // Se realiza la consulta utilizando la Specification compuesta
        List<DetallePago> detalles = detallePagoRepositorio.findAll(spec);
        log.info("Consulta realizada. Numero de registros encontrados: {}", detalles.size());

        // Conversion de la entidad a DTO
        List<DetallePagoResponse> responses = detalles.stream()
                .map(this::mapToDetallePagoResponse)
                .collect(Collectors.toList());
        log.info("Conversion a DetallePagoResponse completada. Regresando respuesta.");

        return responses;
    }

    private DetallePagoResponse mapToDetallePagoResponse(DetallePago detalle) {
        String conceptoDesc = (detalle.getDescripcionConcepto() != null)
                ? detalle.getDescripcionConcepto()
                : "N/A";

        return new DetallePagoResponse(
                detalle.getId(),
                detalle.getVersion(),
                conceptoDesc,
                detalle.getCuotaOCantidad(),
                detalle.getValorBase(),
                detalle.getBonificacion() != null ? detalle.getBonificacion().getId() : null,
                detalle.getRecargo() != null ? detalle.getRecargo().getId() : null,
                detalle.getaCobrar(),
                detalle.getCobrado(),
                detalle.getConcepto() != null ? detalle.getConcepto().getId() : null,
                detalle.getSubConcepto() != null ? detalle.getSubConcepto().getId() : null,
                detalle.getMensualidad() != null ? detalle.getMensualidad().getId() : null,
                detalle.getMatricula() != null ? detalle.getMatricula().getId() : null,
                detalle.getStock() != null ? detalle.getStock().getId() : null,
                detalle.getImporteInicial(),
                detalle.getImportePendiente(),
                detalle.getTipo(),
                detalle.getFechaRegistro(),
                detalle.getPago().getId(),
                (detalle.getAlumno().getNombre() + " " + detalle.getAlumno().getApellido())
        );
    }

}

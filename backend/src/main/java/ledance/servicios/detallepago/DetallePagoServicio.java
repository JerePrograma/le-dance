package ledance.servicios.detallepago;

import jakarta.persistence.EntityNotFoundException;
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

    public List<DetallePagoResponse> filtrarDetalles(
            LocalDate fechaRegistroDesde,
            LocalDate fechaRegistroHasta,
            String disciplina,
            String tarifa,
            String stock,
            String subConcepto,
            String detalleConcepto
    ) {
        Specification<DetallePago> spec = Specification.where(null);

        // Filtrado por rango de fecha (fechaRegistro)
        if (fechaRegistroDesde != null) {
            spec = spec.and((root, query, cb) ->
                    cb.greaterThanOrEqualTo(root.get("fechaRegistro"), fechaRegistroDesde));
        }
        if (fechaRegistroHasta != null) {
            spec = spec.and((root, query, cb) ->
                    cb.lessThanOrEqualTo(root.get("fechaRegistro"), fechaRegistroHasta));
        }

        // Filtrado por disciplina:
        // Si se envía también tarifa se busca que inicie con "DISCIPLINA - TARIFA",
        // de lo contrario se usa un patrón que encuentre la cadena en cualquier parte.
        if (StringUtils.hasText(disciplina)) {
            String pattern;
            if (StringUtils.hasText(tarifa)) {
                pattern = (disciplina + " - " + tarifa).toUpperCase() + "%";
            } else {
                pattern = "%" + disciplina.toUpperCase() + "%";
            }
            spec = spec.and((root, query, cb) ->
                    cb.like(root.get("descripcionConcepto"), pattern));
        }

        // Filtrado por Stock (usa el campo stock.nombre)
        if (StringUtils.hasText(stock)) {
            String pattern = "%" + stock.toLowerCase() + "%";
            spec = spec.and((root, query, cb) ->
                    cb.like(cb.lower(root.get("stock").get("nombre")), pattern));
        }

        // Filtrado por SubConcepto (usa subConcepto.descripcion)
        if (StringUtils.hasText(subConcepto)) {
            String pattern = "%" + subConcepto.toLowerCase() + "%";
            spec = spec.and((root, query, cb) ->
                    cb.like(cb.lower(root.get("subConcepto").get("descripcion")), pattern));
        }

        // Filtrado por Concepto (usando el campo descripcionConcepto)
        if (StringUtils.hasText(detalleConcepto)) {
            String pattern = "%" + detalleConcepto.toUpperCase() + "%";
            spec = spec.and((root, query, cb) ->
                    cb.like(root.get("descripcionConcepto"), pattern));
        }

        // Ejecutar la consulta con la Specification compuesta
        List<DetallePago> detalles = detallePagoRepositorio.findAll(spec);
        log.info("Consulta realizada. Número de registros encontrados: {}", detalles.size());

        // Conversión de las entidades a DTOs
        List<DetallePagoResponse> responses = detalles.stream()
                .map(this::mapToDetallePagoResponse)
                .collect(Collectors.toList());
        log.info("Conversión a DetallePagoResponse completada. Regresando respuesta.");

        return responses;
    }

    // =====================================================
    // Métodos CRUD
    // =====================================================

    // Crear un nuevo DetallePago
    public DetallePagoResponse crearDetallePago(DetallePago detalle) {
        // Calcular importes antes de persistir
        calcularImporte(detalle);
        DetallePago detalleGuardado = detallePagoRepositorio.save(detalle);
        log.info("DetallePago creado con id={}", detalleGuardado.getId());
        return mapToDetallePagoResponse(detalleGuardado);
    }

    // Obtener un DetallePago por su ID
    public DetallePagoResponse obtenerDetallePagoPorId(Long id) {
        DetallePago detalle = detallePagoRepositorio.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("DetallePago con id " + id + " no encontrado"));
        return mapToDetallePagoResponse(detalle);
    }

    // Actualizar un DetallePago existente
    public DetallePagoResponse actualizarDetallePago(Long id, DetallePago detalleActualizado) {
        DetallePago detalleExistente = detallePagoRepositorio.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("DetallePago con id " + id + " no encontrado"));

        // Actualizar campos; dependiendo de tu lógica, puedes actualizar sólo ciertos atributos.
        detalleExistente.setDescripcionConcepto(detalleActualizado.getDescripcionConcepto());
        detalleExistente.setConcepto(detalleActualizado.getConcepto());
        detalleExistente.setSubConcepto(detalleActualizado.getSubConcepto());
        detalleExistente.setCuotaOCantidad(detalleActualizado.getCuotaOCantidad());
        detalleExistente.setBonificacion(detalleActualizado.getBonificacion());
        detalleExistente.setRecargo(detalleActualizado.getRecargo());
        detalleExistente.setValorBase(detalleActualizado.getValorBase());
        // Si es necesario, actualiza otros campos relacionados (importeInicial, importePendiente, aCobrar, etc.)

        // Recalcular importes
        calcularImporte(detalleExistente);
        DetallePago detalleGuardado = detallePagoRepositorio.save(detalleExistente);
        log.info("DetallePago actualizado con id={}", detalleGuardado.getId());
        return mapToDetallePagoResponse(detalleGuardado);
    }

    // Eliminar un DetallePago por su ID
    public void eliminarDetallePago(Long id) {
        if (!detallePagoRepositorio.existsById(id)) {
            throw new EntityNotFoundException("DetallePago con id " + id + " no encontrado");
        }
        detallePagoRepositorio.deleteById(id);
        log.info("DetallePago eliminado con id={}", id);
    }

    // Listar todos los DetallePagos
    public List<DetallePagoResponse> listarDetallesPagos() {
        List<DetallePago> detalles = detallePagoRepositorio.findAll();
        log.info("Listado de DetallePagos obtenido. Total registros: {}", detalles.size());
        return detalles.stream()
                .map(this::mapToDetallePagoResponse)
                .collect(Collectors.toList());
    }

    // =====================================================
    // Método privado para mappear a DTO
    // =====================================================
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

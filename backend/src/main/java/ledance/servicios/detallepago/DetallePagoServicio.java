package ledance.servicios.detallepago;

import jakarta.persistence.EntityNotFoundException;
import jakarta.validation.Valid;
import ledance.dto.pago.DetallePagoMapper;
import ledance.dto.pago.request.DetallePagoRegistroRequest;
import ledance.dto.pago.response.DetallePagoResponse;
import ledance.entidades.DetallePago;
import ledance.entidades.Mensualidad;
import ledance.entidades.Recargo;
import ledance.entidades.TipoDetallePago;
import ledance.repositorios.DetallePagoRepositorio;
import ledance.repositorios.MensualidadRepositorio;
import org.flywaydb.core.internal.util.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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
    private final DetallePagoMapper detallePagoMapper;
    private final MensualidadRepositorio mensualidadRepositorio;

    public DetallePagoServicio(DetallePagoRepositorio detallePagoRepositorio, DetallePagoMapper detallePagoMapper, MensualidadRepositorio mensualidadRepositorio) {
        this.detallePagoRepositorio = detallePagoRepositorio;
        this.detallePagoMapper = detallePagoMapper;
        this.mensualidadRepositorio = mensualidadRepositorio;
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
        double recargo = 0;
        if (detalle.getTieneRecargo()) {
            recargo = (detalle.getRecargo() != null) ? obtenerValorRecargo(detalle, base) : 0.0;
            log.info("[calcularImporte] Detalle id={} : Recargo calculado = {}", detalle.getId(), recargo);
        } else {
            recargo = 0;
        }

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
        log.info("[calcularDescuento] Detalle id={} sin bonificación, descuento=0", detalle.getId());
        return 0.0;
    }

    public double obtenerValorRecargo(DetallePago detalle, double base) {
        Recargo recargo = detalle.getRecargo();
        if (!detalle.getTieneRecargo()) {
            return 0.0;
        }
        if (recargo != null) {
            int diaActual = LocalDate.now().getDayOfMonth();
            log.info("[obtenerValorRecargo] Detalle id={} | Día actual={} | Día de aplicación={}",
                    detalle.getId(), diaActual, recargo.getDiaDelMesAplicacion());
            if (diaActual != recargo.getDiaDelMesAplicacion()) {
                log.info("[obtenerValorRecargo] Día actual no coincide; recargo=0");
                return 0.0;
            }
            double recargoFijo = (recargo.getValorFijo() != null) ? recargo.getValorFijo() : 0.0;
            double recargoPorcentaje = (recargo.getPorcentaje() != null) ? (recargo.getPorcentaje() / 100.0 * base) : 0.0;
            double totalRecargo = recargoFijo + recargoPorcentaje;
            log.info("[obtenerValorRecargo] Detalle id={} | Recargo fijo={} | %={} | Total Recargo={}",
                    detalle.getId(), recargoFijo, recargoPorcentaje, totalRecargo);
            return totalRecargo;
        }
        log.info("[obtenerValorRecargo] Detalle id={} sin recargo; recargo=0", detalle.getId());
        return 0.0;
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

        // Filtrado por disciplina
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

        // Ejecutar la consulta con la Specification compuesta y ordenar de forma descendente por id
        List<DetallePago> detalles = detallePagoRepositorio.findAll(spec, Sort.by(Sort.Direction.DESC, "id"));
        log.info("Consulta realizada. Número de registros encontrados: {}", detalles.size());

        // Conversión de las entidades a DTOs
        List<DetallePagoResponse> responses = detalles.stream()
                .map(detallePagoMapper::toDTO)
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
        return detallePagoMapper.toDTO(detalleGuardado);
    }

    // Obtener un DetallePago por su ID
    public DetallePagoResponse obtenerDetallePagoPorId(Long id) {
        DetallePago detalle = detallePagoRepositorio.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("DetallePago con id " + id + " no encontrado"));
        return detallePagoMapper.toDTO(detalle);
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
        return detallePagoMapper.toDTO(detalleGuardado);
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
                .map(detallePagoMapper::toDTO)
                .collect(Collectors.toList());
    }

    @Transactional
    public void verificarMensualidadNoDuplicada(DetallePago detalle) {
        Long alumnoId = detalle.getAlumno().getId();
        String descripcion = detalle.getDescripcionConcepto();
        log.info("Verificando existencia de mensualidad o detalle de pago para alumnoId={} con descripción '{}'", alumnoId, descripcion);

        // Verificar si ya existe una mensualidad para el alumno con la misma descripción
        Optional<Mensualidad> mensualidadOpt = mensualidadRepositorio.findByInscripcionAlumnoIdAndDescripcionIgnoreCase(alumnoId, descripcion);

        // Verificar si existe un DetallePago de tipo MENSUALIDAD para el mismo alumno y con la misma descripción
        boolean existeDetalleDuplicado = detallePagoRepositorio.existsByAlumnoIdAndDescripcionConceptoIgnoreCaseAndTipo(
                alumnoId, descripcion, TipoDetallePago.MENSUALIDAD
        );

        if (mensualidadOpt.isPresent() || existeDetalleDuplicado) {
            log.error("Ya existe una mensualidad o detalle de pago con descripción '{}' para alumnoId={}", descripcion, alumnoId);
            throw new IllegalStateException("MENSUALIDAD YA COBRADA");
        }
    }

}

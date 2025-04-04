package ledance.servicios.detallepago;

import jakarta.persistence.EntityNotFoundException;
import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.JoinType;
import jakarta.persistence.criteria.Predicate;
import ledance.dto.pago.DetallePagoMapper;
import ledance.dto.pago.response.DetallePagoResponse;
import ledance.entidades.*;
import ledance.repositorios.DetallePagoRepositorio;
import ledance.repositorios.MatriculaRepositorio;
import ledance.repositorios.MensualidadRepositorio;
import ledance.repositorios.PagoRepositorio;
import org.flywaydb.core.internal.util.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class DetallePagoServicio {

    private static final Logger log = LoggerFactory.getLogger(DetallePagoServicio.class);
    private final DetallePagoRepositorio detallePagoRepositorio;
    private final DetallePagoMapper detallePagoMapper;
    private final MensualidadRepositorio mensualidadRepositorio;
    private final PagoRepositorio pagoRepositorio;

    public DetallePagoServicio(DetallePagoRepositorio detallePagoRepositorio, DetallePagoMapper detallePagoMapper, MensualidadRepositorio mensualidadRepositorio, PagoRepositorio pagoRepositorio) {
        this.detallePagoRepositorio = detallePagoRepositorio;
        this.detallePagoMapper = detallePagoMapper;
        this.mensualidadRepositorio = mensualidadRepositorio;
        this.pagoRepositorio = pagoRepositorio;
    }

    /**
     * Calcula el importe final de un DetallePago usando:
     * totalAjustado = valorBase - descuento + recargo
     * importe = totalAjustado - aCobrar (acumulado)
     */
    public void calcularImporte(DetallePago detalle) {
        // 1. Inicio y validación de concepto
        String conceptoDesc = (detalle.getDescripcionConcepto() != null) ? detalle.getDescripcionConcepto() : "N/A";
        log.info("[calcularImporte] INICIO - Cálculo para Detalle ID: {} | Concepto: '{}' | Tipo: {}", detalle.getId(), conceptoDesc, detalle.getTipo());
        log.info("[calcularImporte] Detalle completo al inicio: {}", detalle.toString());

        // 2. Obtención de valor base
        double base = Optional.ofNullable(detalle.getValorBase()).orElse(0.0);
        log.info("[calcularImporte] Valor base obtenido: {}", base);
        log.info("[calcularImporte] Cuota/Cantidad: {}", detalle.getCuotaOCantidad());

        // 3. Cálculo de descuentos
        double descuento;
        if (TipoDetallePago.MENSUALIDAD.equals(detalle.getTipo()) && detalle.getBonificacion() != null) {
            log.info("[calcularImporte] Procesando descuento para MENSUALIDAD con bonificación");

            double descuentoFijo = Optional.ofNullable(detalle.getBonificacion().getValorFijo()).orElse(0.0);
            log.info("[calcularImporte] Descuento fijo: {}", descuentoFijo);

            Integer porcentaje = Optional.ofNullable(detalle.getBonificacion().getPorcentajeDescuento()).orElse(0);
            double descuentoPorcentaje = (porcentaje / 100.0) * base;
            log.info("[calcularImporte] Descuento porcentual ({}%): {}", porcentaje, descuentoPorcentaje);

            descuento = descuentoFijo + descuentoPorcentaje;
            log.info("[calcularImporte] Descuento TOTAL para mensualidad: {}", descuento);
        } else {
            log.info("[calcularImporte] Calculando descuento estándar");
            descuento = calcularDescuento(detalle, base);
            log.info("[calcularImporte] Descuento calculado: {}", descuento);
        }

        // 4. Cálculo de recargos
        double recargo = 0;
        if (detalle.getTieneRecargo()) {
            log.info("[calcularImporte] Procesando recargo activo");
            recargo = (detalle.getRecargo() != null) ? obtenerValorRecargo(detalle, base) : 0.0;
            log.info("[calcularImporte] Recargo aplicado: {}", recargo);
        } else {
            log.info("[calcularImporte] Sin recargo aplicable");
        }

        // 5. Cálculo de importe inicial
        double importeInicial = base - descuento + recargo;
        log.info("[calcularImporte] Cálculo final: {} (base) - {} (descuento) + {} (recargo) = {}", base, descuento, recargo, importeInicial);

        log.info("[calcularImporte] Asignando importe inicial: {}", importeInicial);
        detalle.setImporteInicial(importeInicial);

        // 6. Gestión de importe pendiente
        if (detalle.getImportePendiente() == null) {
            log.info("[calcularImporte] Importe pendiente nulo - Asignando valor inicial: {}", importeInicial);
        } else {
            log.info("[calcularImporte] Importe pendiente mantiene su valor actual: {}", detalle.getImportePendiente());
        }

        log.info("[calcularImporte] FIN - Resultados para Detalle ID: {} | Inicial: {} | Pendiente: {} | Cobrado: {}", detalle.getId(), detalle.getImporteInicial(), detalle.getImportePendiente(), detalle.getCobrado());
        log.info("[calcularImporte] Estado final del detalle: {}", detalle.toString());
    }

    public double calcularDescuento(DetallePago detalle, double base) {
        log.info("[calcularDescuento] INICIO - Cálculo para Detalle ID: {}", detalle.getId());

        if (detalle.getBonificacion() != null) {
            log.info("[calcularDescuento] Bonificación encontrada: ID {}", detalle.getBonificacion().getId());

            double descuentoFijo = Optional.ofNullable(detalle.getBonificacion().getValorFijo()).orElse(0.0);
            log.info("[calcularDescuento] Componente fijo: {}", descuentoFijo);

            Integer porcentaje = Optional.ofNullable(detalle.getBonificacion().getPorcentajeDescuento()).orElse(0);
            double descuentoPorcentaje = (porcentaje / 100.0) * base;
            log.info("[calcularDescuento] Componente porcentual ({}% de {}): {}", porcentaje, base, descuentoPorcentaje);

            double totalDescuento = descuentoFijo + descuentoPorcentaje;
            log.info("[calcularDescuento] Descuento TOTAL calculado: {}", totalDescuento);

            return totalDescuento;
        }

        log.info("[calcularDescuento] Sin bonificación aplicable - Descuento: 0");
        return 0.0;
    }

    public double obtenerValorRecargo(DetallePago detalle, double base) {
        Recargo recargo = detalle.getRecargo();
        if (!detalle.getTieneRecargo()) {
            return 0.0;
        }
        if (recargo != null) {
            int diaActual = LocalDate.now().getDayOfMonth();
            log.info("[obtenerValorRecargo] Detalle id={} | Día actual={} | Día de aplicación={}", detalle.getId(), diaActual, recargo.getDiaDelMesAplicacion());
            if (diaActual != recargo.getDiaDelMesAplicacion()) {
                log.info("[obtenerValorRecargo] Día actual no coincide; recargo=0");
                return 0.0;
            }
            double recargoFijo = (recargo.getValorFijo() != null) ? recargo.getValorFijo() : 0.0;
            double recargoPorcentaje = (recargo.getPorcentaje() != null) ? (recargo.getPorcentaje() / 100.0 * base) : 0.0;
            double totalRecargo = recargoFijo + recargoPorcentaje;
            log.info("[obtenerValorRecargo] Detalle id={} | Recargo fijo={} | %={} | Total Recargo={}", detalle.getId(), recargoFijo, recargoPorcentaje, totalRecargo);
            return totalRecargo;
        }
        log.info("[obtenerValorRecargo] Detalle id={} sin recargo; recargo=0", detalle.getId());
        return 0.0;
    }

    public List<DetallePagoResponse> filtrarDetalles(LocalDate fechaRegistroDesde, LocalDate fechaRegistroHasta, String categoria,         // Se espera: DISCIPLINAS, STOCK, CONCEPTOS o MATRICULA
                                                     String disciplina, String tarifa, String stock, String subConcepto, String detalleConcepto, String alumnoId          // NUEVO: Filtrado por alumno
    ) {
        // Se inicia la Specification con el filtro por fecha
        Specification<DetallePago> spec = Specification.where(filtrarPorFechaRegistro(fechaRegistroDesde, fechaRegistroHasta));

        // Agregar filtro por alumno si se proporciona
        if (StringUtils.hasText(alumnoId)) {
            spec = spec.and((root, query, cb) -> cb.equal(root.get("alumno").get("id"), Long.valueOf(alumnoId)));
        }

        // Si se envía categoría, se aplica el filtro correspondiente
        if (StringUtils.hasText(categoria)) {
            spec = spec.and(filtrarPorCategoria(categoria, disciplina, tarifa, stock, subConcepto, detalleConcepto));
        }

        List<DetallePago> detalles = detallePagoRepositorio.findAll(spec, Sort.by(Sort.Direction.DESC, "id"));
        log.info("Consulta realizada. Número de registros encontrados: {}", detalles.size());

        List<DetallePagoResponse> responses = detalles.stream().map(detallePagoMapper::toDTO).collect(Collectors.toList());
        log.info("Conversión a DetallePagoResponse completada. Regresando respuesta.");

        return responses;
    }

    private Specification<DetallePago> filtrarPorFechaRegistro(LocalDate desde, LocalDate hasta) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            if (desde != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.get("fechaRegistro"), desde));
            }
            if (hasta != null) {
                predicates.add(cb.lessThanOrEqualTo(root.get("fechaRegistro"), hasta));
            }
            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }

    private Specification<DetallePago> filtrarPorCategoria(String categoria, String disciplina, String tarifa, String stock, String subConcepto, String detalleConcepto) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            String categoriaUpper = categoria.toUpperCase();

            switch (categoriaUpper) {
                case "DISCIPLINAS":
                    if (StringUtils.hasText(disciplina)) {
                        String pattern = StringUtils.hasText(tarifa) ? (disciplina + " - " + tarifa).toUpperCase() + "%" : "%" + disciplina.toUpperCase() + "%";
                        predicates.add(cb.like(cb.upper(root.get("descripcionConcepto")), pattern));
                    } else {
                        predicates.add(cb.equal(root.get("tipo"), TipoDetallePago.MENSUALIDAD));
                    }
                    break;
                case "STOCK":
                    if (StringUtils.hasText(stock)) {
                        String pattern = "%" + stock.toLowerCase() + "%";
                        predicates.add(cb.like(cb.lower(root.get("stock").get("nombre")), pattern));
                    } else {
                        predicates.add(cb.equal(root.get("tipo"), TipoDetallePago.STOCK));
                    }
                    break;
                case "CONCEPTOS":
                    // Se listan todos los detalles con tipo CONCEPTO
                    predicates.add(cb.equal(root.get("tipo"), TipoDetallePago.CONCEPTO));
                    // Si se envía un subConcepto, aplicar filtro
                    if (StringUtils.hasText(subConcepto)) {
                        if ("MATRICULA".equalsIgnoreCase(subConcepto)) {
                            // Filtra por descripción que contenga "MATRICULA"
                            predicates.add(cb.like(cb.upper(root.get("descripcionConcepto")), "%MATRICULA%"));
                        } else {
                            String pattern = "%" + subConcepto.toUpperCase() + "%";
                            Predicate pDirect = cb.like(cb.upper(root.get("subConcepto").get("descripcion")), pattern);
                            Join<DetallePago, Concepto> joinConcepto = root.join("concepto", JoinType.LEFT);
                            Predicate pViaConcepto = cb.like(cb.upper(joinConcepto.get("subConcepto").get("descripcion")), pattern);
                            predicates.add(cb.or(pDirect, pViaConcepto));
                        }
                    }
                    if (StringUtils.hasText(detalleConcepto)) {
                        String pattern = "%" + detalleConcepto.toUpperCase() + "%";
                        predicates.add(cb.like(cb.upper(root.get("descripcionConcepto")), pattern));
                    }
                    break;
                case "MATRICULA":
                    predicates.add(cb.like(cb.upper(root.get("descripcionConcepto")), "%MATRICULA%"));
                    break;
                default:
                    // Sin filtro adicional
                    break;
            }
            return cb.and(predicates.toArray(new Predicate[0]));
        };
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
        DetallePago detalle = detallePagoRepositorio.findById(id).orElseThrow(() -> new EntityNotFoundException("DetallePago con id " + id + " no encontrado"));
        return detallePagoMapper.toDTO(detalle);
    }

    public DetallePagoResponse actualizarDetallePago(Long id, DetallePago detalleActualizado) {
        DetallePago detalleExistente = detallePagoRepositorio.findById(id).orElseThrow(() -> new EntityNotFoundException("DetallePago con id " + id + " no encontrado"));

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

    @Transactional
    public void eliminarDetallePago(Long id) {
        log.info("Iniciando eliminación de DetallePago con id={}", id);

        // 1. Buscar el DetallePago
        DetallePago detalle = detallePagoRepositorio.findById(id).orElseThrow(() -> {
            log.error("DetallePago con id {} no encontrado", id);
            return new EntityNotFoundException("DetallePago con id " + id + " no encontrado");
        });
        log.info("DetallePago encontrado: {}", detalle);

        // 2. Obtener el Pago asociado
        Pago pago = detalle.getPago();
        if (pago != null) {
            log.info("Pago asociado encontrado: {}", pago);

            // 3. Remover el detalle de la colección del pago (orphanRemoval se encargará de eliminarlo)
            pago.removerDetalle(detalle);
            log.info("Detalle removido de la colección del pago");

            // 4. Recalcular nuevos montos para el pago
            double valorACobrar = detalle.getaCobrar();
            double montoActual = pago.getMonto();
            double montoPagadoActual = pago.getMontoPagado();
            log.info("Valores actuales - Monto: {}, MontoPagado: {}, aCobrar: {}",
                    montoActual, montoPagadoActual, valorACobrar);

            double nuevoMonto = montoActual - valorACobrar;
            double nuevoMontoPagado = montoPagadoActual - valorACobrar;
            log.info("Nuevos valores calculados - Monto: {}, MontoPagado: {}",
                    nuevoMonto, nuevoMontoPagado);

            // 5. Actualizar montos del pago y guardar cambios
            pago.setMonto(nuevoMonto);
            pago.setMontoPagado(nuevoMontoPagado);
            log.info("Montos actualizados en el objeto Pago");

            // Guardar cambios en el pago
            pagoRepositorio.save(pago);
            log.info("Pago guardado exitosamente");
        } else {
            log.info("No se encontró pago asociado al detalle");
        }

        // No es necesario eliminar explícitamente el DetallePago,
        // ya que al removerlo de la colección se elimina automáticamente.
        log.info("Eliminación de DetallePago finalizada para id={}", id);
    }

    @Transactional
    public DetallePagoResponse anularDetallePago(Long id) {
        log.info("Iniciando anulación de DetallePago con id={}", id);
        DetallePago detalle = detallePagoRepositorio.findById(id).orElseThrow(() -> {
            log.error("DetallePago con id {} no encontrado", id);
            return new EntityNotFoundException("DetallePago con id " + id + " no encontrado");
        });
        log.info("DetallePago encontrado: {}", detalle);

        Pago pago = detalle.getPago();
        if (pago != null) {
            log.info("Pago encontrado: {}", pago);

            log.info("Calculando nuevos montos para el pago");
            double valorACobrar = detalle.getaCobrar();
            double montoActual = pago.getMonto();
            double montoPagadoActual = pago.getMontoPagado();
            log.info("Valores actuales - Monto: {}, MontoPagado: {}, aCobrar: {}", montoActual, montoPagadoActual, valorACobrar);

            double nuevoMonto = montoActual - valorACobrar;
            double nuevoMontoPagado = montoPagadoActual - valorACobrar;
            log.info("Nuevos valores calculados - Monto: {}, MontoPagado: {}", nuevoMonto, nuevoMontoPagado);
            if (nuevoMonto == pago.getMetodoPago().getRecargo()) {
                nuevoMonto = 0;
                nuevoMontoPagado = 0;
            }
            log.info("Actualizando montos del pago");
            pago.setMonto(nuevoMonto);
            pago.setMontoPagado(nuevoMontoPagado);
            log.info("Montos actualizados en el objeto Pago");

            log.info("Guardando cambios en el pago");
            pagoRepositorio.save(pago);
            log.info("Pago guardado exitosamente");

            log.info("Filtrando detalles activos del pago");
            // Primero, romper la asociación en todos los detalles y hacer flush

            // Ahora, actualizar el detalle actual antes de eliminar el Pago
            detalle.setEstadoPago(EstadoPago.ANULADO);
            detalle.setCobrado(false);
            detalle.setaCobrar(0.0);
            detallePagoRepositorio.save(detalle);
        } else {
            log.info("No se encontró pago asociado al detalle");
        }

        DetallePagoResponse response = detallePagoMapper.toDTO(detalle);
        log.info("DetallePago anulado exitosamente con id={}, estado={}", detalle.getId(), detalle.getEstadoPago());
        return response;
    }

    public List<DetallePagoResponse> listarDetallesPagos() {
        List<DetallePago> detalles = detallePagoRepositorio.findAll();
        log.info("Listado de DetallePagos obtenido. Total registros: {}", detalles.size());
        return detalles.stream().map(detallePagoMapper::toDTO).collect(Collectors.toList());
    }

    public List<DetallePagoResponse> listarDetallesPagos(LocalDate fechaDesde, LocalDate fechaHasta) {
        List<DetallePago> detalles;
        if (fechaDesde != null && fechaHasta != null) {
            detalles = detallePagoRepositorio.findByFechaRegistroBetween(fechaDesde, fechaHasta);
        } else {
            // Si no se reciben fechas o estado, se devuelven todos
            detalles = detallePagoRepositorio.findAll();
        }
        log.info("Listado de DetallePagos obtenido. Total registros: {}", detalles.size());
        return detalles.stream().map(detallePagoMapper::toDTO).collect(Collectors.toList());
    }


    @Transactional
    public void verificarMensualidadNoDuplicada(DetallePago detalle) {
        Long alumnoId = detalle.getAlumno().getId();
        String descripcion = detalle.getDescripcionConcepto();
        log.info("Verificando existencia de mensualidad o detalle de pago para alumnoId={} con descripción '{}'", alumnoId, descripcion);

        // Solo se verifica si la descripción contiene "CUOTA"
        if (descripcion != null && descripcion.toUpperCase().contains("CUOTA")) {
            Optional<Mensualidad> mensualidadOpt = mensualidadRepositorio.findByInscripcionAlumnoIdAndDescripcionIgnoreCase(alumnoId, descripcion);

            if (mensualidadOpt.isPresent()) {
                // Si se encontró una mensualidad que contenga "CUOTA", se verifica si ya existe un detalle duplicado
                boolean existeDetalleDuplicado = detallePagoRepositorio.existsByAlumnoIdAndDescripcionConceptoIgnoreCaseAndTipoAndEstadoPago(alumnoId, descripcion, TipoDetallePago.MENSUALIDAD, EstadoPago.HISTORICO);
                boolean existeDetalleDuplicado2 = detallePagoRepositorio.existsByAlumnoIdAndDescripcionConceptoIgnoreCaseAndTipoAndEstadoPago(alumnoId, descripcion, TipoDetallePago.MENSUALIDAD, EstadoPago.ACTIVO);
                if (existeDetalleDuplicado || existeDetalleDuplicado2) {
                    log.error("Ya existe una mensualidad o detalle de pago con descripción '{}' para alumnoId={}", descripcion, alumnoId);
                    throw new IllegalStateException("MENSUALIDAD YA COBRADA");
                }
            }
        }
    }


}

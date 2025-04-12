package ledance.servicios.detallepago;

import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityNotFoundException;
import jakarta.persistence.PersistenceContext;
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
import java.util.*;
import java.util.stream.Collectors;

@Service
public class DetallePagoServicio {

    @PersistenceContext
    private EntityManager entityManager;

    private static final Logger log = LoggerFactory.getLogger(DetallePagoServicio.class);
    private final DetallePagoRepositorio detallePagoRepositorio;
    private final DetallePagoMapper detallePagoMapper;
    private final MensualidadRepositorio mensualidadRepositorio;
    private final PagoRepositorio pagoRepositorio;
    private final MatriculaRepositorio matriculaRepositorio;

    public DetallePagoServicio(DetallePagoRepositorio detallePagoRepositorio, DetallePagoMapper detallePagoMapper, MensualidadRepositorio mensualidadRepositorio, PagoRepositorio pagoRepositorio,
                               MatriculaRepositorio matriculaRepositorio) {
        this.detallePagoRepositorio = detallePagoRepositorio;
        this.detallePagoMapper = detallePagoMapper;
        this.mensualidadRepositorio = mensualidadRepositorio;
        this.pagoRepositorio = pagoRepositorio;
        this.matriculaRepositorio = matriculaRepositorio;
    }

    /**
     * Calcula el importe final de un DetallePago usando:
     * totalAjustado = valorBase - descuento + recargo
     * importe = totalAjustado - ACobrar (acumulado)
     */
    public void calcularImporte(DetallePago detalle) {
        // 1. Inicio y validación de concepto
        String conceptoDesc = (detalle.getDescripcionConcepto() != null) ? detalle.getDescripcionConcepto() : "N/A";
        log.info("[calcularImporte] INICIO - Cálculo para Detalle ID: {} | Concepto: '{}' | Tipo: {}", detalle.getId(), conceptoDesc, detalle.getTipo());

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
            if (detalle.getACobrar() == null) {
                detalle.setACobrar(0.0);
            }
            detalle.setImportePendiente(importeInicial - detalle.getACobrar());
        } else {
            log.info("[calcularImporte] Importe pendiente mantiene su valor actual: {}", detalle.getImportePendiente());
        }

        log.info("[calcularImporte] FIN - Resultados para Detalle ID: {} | Inicial: {} | Pendiente: {} | Cobrado: {}", detalle.getId(), detalle.getImporteInicial(), detalle.getImportePendiente(), detalle.getCobrado());
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
        // Si es necesario, actualiza otros campos relacionados (importeInicial, importePendiente, ACobrar, etc.)

        // Recalcular importes
        calcularImporte(detalleExistente);
        DetallePago detalleGuardado = detallePagoRepositorio.save(detalleExistente);
        log.info("DetallePago actualizado con id={}", detalleGuardado.getId());
        return detallePagoMapper.toDTO(detalleGuardado);
    }

    @Transactional
    public void eliminarDetallePago(Long id) {
        log.info("[eliminarDetallePago] Iniciando eliminación de DetallePago con id={}", id);

        // 1. Buscar el DetallePago
        Optional<DetallePago> optionalDetalle = detallePagoRepositorio.findById(id);
        if (optionalDetalle.isEmpty()) {
            log.warn("[eliminarDetallePago] DetallePago con id={} no encontrado. No se realizará acción.", id);
            return;
        }

        DetallePago detalle = optionalDetalle.get();
        log.info("[eliminarDetallePago] DetallePago encontrado: {}", detalle);

        // 2. Obtener el Pago asociado
        Pago pago = detalle.getPago();
        if (pago != null) {
            log.info("[eliminarDetallePago] Pago asociado encontrado: id={}", pago.getId());

            // 3. Remover el detalle de la colección del pago (orphanRemoval se encargará de eliminarlo)
            pago.removerDetalle(detalle);
            log.info("[eliminarDetallePago] Detalle removido de la colección del pago");

            // 4. Recalcular nuevos montos para el pago
            double valorACobrar = detalle.getACobrar();
            double nuevoMonto = pago.getMonto() - valorACobrar;
            double nuevoMontoPagado = pago.getMontoPagado() - valorACobrar;

            if (nuevoMonto <= 0 || nuevoMonto == pago.getMetodoPago().getRecargo()) {
                nuevoMonto = 0.0;
                nuevoMontoPagado = 0.0;
            }

            pago.setMonto(nuevoMonto);
            pago.setMontoPagado(nuevoMontoPagado);
            log.info("[eliminarDetallePago] Montos actualizados - monto={}, pagado={}", nuevoMonto, nuevoMontoPagado);

            // 5. Guardar el pago (y eliminar el detalle automáticamente)
            pagoRepositorio.save(pago);
        } else {
            log.warn("[eliminarDetallePago] No se encontró pago asociado al detalle");
        }

        // 6. Eliminar mensualidad asociada si corresponde
        Mensualidad mensualidad = obtenerMensualidadSiExiste(detalle);
        if (mensualidad != null) {
            mensualidadRepositorio.delete(mensualidad);
            log.info("[eliminarDetallePago] Mensualidad eliminada: {}", mensualidad.getId());
        }

        // 7. Eliminar matrícula asociada si corresponde
        Matricula matricula = obtenerMatriculaSiExiste(detalle);
        if (matricula != null && matricula.getId() != null) {
            matricula.setPagada(false);
            matricula.setFechaPago(null);
            log.info("Matrícula eliminada: {}", matricula.getId());
        }

        log.info("[eliminarDetallePago] Eliminación finalizada para DetallePago id={}", id);
    }

    @Transactional
    public DetallePagoResponse anularDetallePago(Long id) {
        log.info("Iniciando anulación de DetallePago con id={}", id);

        // 1. Buscar el DetallePago a anular
        DetallePago detalle = detallePagoRepositorio.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("DetallePago con id " + id + " no encontrado"));
        log.info("DetallePago encontrado: {}", detalle);

        // Almacenar valores originales antes de modificarlos:
        double originalACobrar = detalle.getACobrar();
        double originalImportePendiente = detalle.getImportePendiente();

        // 2. Actualizar el Pago asociado: descontar el valor aCobrar del total y monto pagado
        Pago pago = detalle.getPago();
        if (pago != null) {
            log.info("Pago encontrado: {}", pago);
            double montoActual = pago.getMonto();
            double montoPagadoActual = pago.getMontoPagado();
            log.info("Valores actuales - Monto: {}, MontoPagado: {}, ACobrar: {}", montoActual, montoPagadoActual, originalACobrar);

            double nuevoMonto = montoActual - originalACobrar;
            double nuevoMontoPagado = montoPagadoActual - originalACobrar;
            if (nuevoMonto <= 0) {
                nuevoMonto = 0.0;
                nuevoMontoPagado = 0.0;
            }
            pago.setMonto(nuevoMonto);
            pago.setMontoPagado(nuevoMontoPagado);

            // Actualizar las observaciones: reemplazar "SALDA" por "ANULA" si corresponde
            String obs = pago.getObservaciones();
            if (obs != null && obs.contains("SALDA")) {
                pago.setObservaciones(obs.replace("SALDA", "ANULA"));
            }
            pagoRepositorio.save(pago);
            log.info("Pago actualizado y guardado exitosamente: {}", pago);
        } else {
            log.info("No se encontró pago asociado al detalle");
        }

        // 3. Actualizar el DetallePago original para marcarlo como anulado.
        detalle.setCobrado(false);
        detalle.setACobrar(0.0);
        detalle.setImportePendiente(0.0);
        detalle.setEstadoPago(EstadoPago.ANULADO);
        detalle.setRemovido(true);
        detallePagoRepositorio.save(detalle);
        log.info("DetallePago original (id={}) actualizado a estado ANULADO", detalle.getId());

        // 4. Clonar el detalle para registrar la anulación históricamente.
        // Si se "salda" el item (importe pendiente igual a aCobrar), el clon debe quedar con ambos en 0.
        double nuevoImportePendienteClon = (originalImportePendiente == originalACobrar)
                ? 0.0
                : originalImportePendiente + originalACobrar;

        DetallePago cloneDetalle = cloneDetallePago(detalle);
        cloneDetalle.setCobrado(false);
        cloneDetalle.setACobrar(0.0);
        cloneDetalle.setImportePendiente(nuevoImportePendienteClon);
        cloneDetalle.setEstadoPago(EstadoPago.ANULADO);
        // Marcar el clon como histórico y, a fin de que no sea reactivado en nuevos cobros, también marcarlo como removido.
        cloneDetalle.setEsClon(true);
        cloneDetalle.setRemovido(true);
        detallePagoRepositorio.save(cloneDetalle);
        log.info("DetallePago clonado (id={}) marcado como ANULADO y guardado como registro histórico", cloneDetalle.getId());

        // 5. Si existe un detalle hermano (no cancelado) para el mismo concepto, fusionar sus valores sumándole originalACobrar al importePendiente.
        if (pago != null) {
            Optional<DetallePago> detalleHermanoOpt = pago.getDetallePagos().stream()
                    .filter(d -> !d.getId().equals(detalle.getId())
                            && d.getDescripcionConcepto().equalsIgnoreCase(detalle.getDescripcionConcepto())
                            && d.getEstadoPago() != EstadoPago.ANULADO)
                    .findFirst();
            if (detalleHermanoOpt.isPresent()) {
                DetallePago detalleHermano = detalleHermanoOpt.get();
                log.info("Detalle hermano encontrado (id={}), se procederá a fusionar los valores", detalleHermano.getId());
                double nuevoPendiente = detalleHermano.getImportePendiente() + originalACobrar;
                detalleHermano.setACobrar(0.0);
                detalleHermano.setImportePendiente(nuevoPendiente);
                detalleHermano.setCobrado(nuevoPendiente == 0.0);
                detallePagoRepositorio.save(detalleHermano);
                log.info("Detalle hermano (id={}) actualizado: nuevo importe pendiente={}", detalleHermano.getId(), nuevoPendiente);
            } else {
                log.info("No se encontró detalle hermano para fusionar.");
            }
        }

        // 6. Desvincular y eliminar Mensualidad o Matrícula, si corresponde.
        if (detalle.getMensualidad() != null) {
            Mensualidad mensualidad = detalle.getMensualidad();
            detalle.setMensualidad(null);
            detallePagoRepositorio.save(detalle);
            entityManager.flush();
            mensualidadRepositorio.delete(mensualidad);
            log.info("Mensualidad desvinculada y eliminada: {}", mensualidad.getId());
        }
        if (detalle.getMatricula() != null) {
            Matricula matricula = detalle.getMatricula();
            detalle.setMatricula(null);
            detallePagoRepositorio.save(detalle);
            entityManager.flush();
            matriculaRepositorio.delete(matricula);
            log.info("Matrícula desvinculada y eliminada: {}", matricula.getId());
        }

        log.info("Proceso de anulación completado para DetallePago id={}", detalle.getId());
        return detallePagoMapper.toDTO(detalle);
    }

    /**
     * Método auxiliar que clona un DetallePago (sin id ni versión) copiando los atributos relevantes.
     */
    private DetallePago cloneDetallePago(DetallePago original) {
        DetallePago clone = new DetallePago();
        // Copiar atributos básicos
        clone.setDescripcionConcepto(original.getDescripcionConcepto());
        clone.setConcepto(original.getConcepto());
        clone.setSubConcepto(original.getSubConcepto());
        clone.setCuotaOCantidad(original.getCuotaOCantidad());
        clone.setBonificacion(original.getBonificacion());
        clone.setRecargo(original.getRecargo());
        clone.setValorBase(original.getValorBase());
        clone.setImporteInicial(original.getImporteInicial());
        clone.setImportePendiente(original.getImportePendiente());
        clone.setACobrar(original.getACobrar());
        clone.setPago(original.getPago());
        clone.setMensualidad(original.getMensualidad());
        clone.setMatricula(original.getMatricula());
        clone.setStock(original.getStock());
        clone.setAlumno(original.getAlumno());
        clone.setCobrado(original.getCobrado());
        clone.setTipo(original.getTipo());
        clone.setFechaRegistro(original.getFechaRegistro());
        clone.setTieneRecargo(original.getTieneRecargo());
        clone.setUsuario(original.getUsuario());
        clone.setRemovido(false);
        return clone;
    }

    @Transactional
    public Matricula obtenerMatriculaSiExiste(DetallePago detalle) {
        Long alumnoId = detalle.getAlumno().getId();
        String descripcion = detalle.getDescripcionConcepto();
        log.info("[obtenerMatriculaSiExiste] Verificando existencia de matrícula para alumnoId={} con descripción '{}'", alumnoId, descripcion);

        if (descripcion != null && descripcion.toUpperCase().contains("MATRICULA")) {
            // Obtener todos los detalles de tipo MATRICULA para el alumno y la descripción dada
            List<DetallePago> detalles = detallePagoRepositorio
                    .findAllByAlumnoIdAndDescripcionConceptoIgnoreCaseAndTipo(alumnoId, descripcion, TipoDetallePago.MATRICULA);

            if (detalles != null && !detalles.isEmpty()) {
                // Ordenar los detalles por ID de forma ascendente
                detalles.sort(Comparator.comparing(DetallePago::getId));
                DetallePago primerDetalle = detalles.get(0);

                // Si el primer detalle no está anulado, se fuerza a ANULADO (o se lanza excepción, según la lógica de negocio)
                if (primerDetalle.getEstadoPago() != EstadoPago.ANULADO) {
                    log.error("[obtenerMatriculaSiExiste] Se encontró un DetallePago activo (no anulado) con descripción '{}' para alumnoId={}. Forzando anulación.", descripcion, alumnoId);
                    primerDetalle.setEstadoPago(EstadoPago.ANULADO);
                    detallePagoRepositorio.save(primerDetalle);
                }

                // Desligar la matrícula de todos los DetallePago asociados para evitar problemas al eliminar la matrícula
                for (DetallePago dp : detalles) {
                    if (dp.getMatricula() != null) {
                        dp.setMatricula(null);
                        detallePagoRepositorio.save(dp);
                        log.info("[obtenerMatriculaSiExiste] Se desvinculó la matrícula del DetallePago id={}", dp.getId());
                    }
                }
                Matricula matricula = primerDetalle.getMatricula(); // Ahora debería quedar la referencia "vieja" en el primer detalle
                log.info("[obtenerMatriculaSiExiste] Se retorna la matrícula asociada al primer DetallePago (id={}) para alumnoId={}", primerDetalle.getId(), alumnoId);
                return matricula;
            } else {
                log.info("[obtenerMatriculaSiExiste] No se encontró ningún DetallePago para alumnoId={} con descripción '{}'.", alumnoId, descripcion);
                return null;
            }
        } else {
            log.info("[obtenerMatriculaSiExiste] La descripción '{}' no contiene 'MATRICULA', no se realiza verificación.", descripcion);
            return null;
        }
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
    public Mensualidad obtenerMensualidadSiExiste(DetallePago detalle) {
        Long alumnoId = detalle.getAlumno().getId();
        String descripcion = detalle.getDescripcionConcepto();
        log.info("Verificando existencia de mensualidad para alumnoId={} con descripción '{}'", alumnoId, descripcion);

        if (descripcion != null && descripcion.toUpperCase().contains("CUOTA")) {
            Optional<Mensualidad> mensualidadOpt = mensualidadRepositorio
                    .findByInscripcionAlumnoIdAndDescripcionIgnoreCase(alumnoId, descripcion);
            if (mensualidadOpt.isPresent()) {
                // Verificar si ya existe un detalle duplicado, ya sea activo o histórico.
                boolean existeDetalleDuplicado = detallePagoRepositorio.existsByAlumnoIdAndDescripcionConceptoIgnoreCaseAndTipoAndEstadoPago(
                        alumnoId, descripcion, TipoDetallePago.MENSUALIDAD, EstadoPago.HISTORICO);
                boolean existeDetalleDuplicado2 = detallePagoRepositorio.existsByAlumnoIdAndDescripcionConceptoIgnoreCaseAndTipoAndEstadoPago(
                        alumnoId, descripcion, TipoDetallePago.MENSUALIDAD, EstadoPago.ACTIVO);
                if (existeDetalleDuplicado || existeDetalleDuplicado2) {
                    log.error("Ya existe una mensualidad o detalle de pago con descripción '{}' para alumnoId={}", descripcion, alumnoId);
                }
                log.info("Mensualidad encontrada para alumnoId={} con descripción '{}'", alumnoId, descripcion);
                return mensualidadOpt.get();
            } else {
                log.info("No se encontró mensualidad para alumnoId={} con descripción '{}'", alumnoId, descripcion);
                return null;
            }
        } else {
            log.info("La descripción '{}' no corresponde a una mensualidad", descripcion);
            return null;
        }
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

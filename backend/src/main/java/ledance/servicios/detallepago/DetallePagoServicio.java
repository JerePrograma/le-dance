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
import ledance.repositorios.*;
import ledance.servicios.stock.StockServicio;
import org.flywaydb.core.internal.util.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.*;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Service
public class DetallePagoServicio {

    private final StockServicio stockServicio;
    @PersistenceContext
    private EntityManager entityManager;

    private static final Logger log = LoggerFactory.getLogger(DetallePagoServicio.class);
    private final DetallePagoRepositorio detallePagoRepositorio;
    private final DetallePagoMapper detallePagoMapper;
    private final MensualidadRepositorio mensualidadRepositorio;
    private final PagoRepositorio pagoRepositorio;
    private final MatriculaRepositorio matriculaRepositorio;
    private final SubConceptoRepositorio subConceptoRepo;
    private final ConceptoRepositorio conceptoRepo;


    public DetallePagoServicio(DetallePagoRepositorio detallePagoRepositorio, DetallePagoMapper detallePagoMapper, MensualidadRepositorio mensualidadRepositorio, PagoRepositorio pagoRepositorio,
                               MatriculaRepositorio matriculaRepositorio, SubConceptoRepositorio subConceptoRepo, ConceptoRepositorio conceptoRepo, StockServicio stockServicio) {
        this.detallePagoRepositorio = detallePagoRepositorio;
        this.detallePagoMapper = detallePagoMapper;
        this.mensualidadRepositorio = mensualidadRepositorio;
        this.pagoRepositorio = pagoRepositorio;
        this.matriculaRepositorio = matriculaRepositorio;
        this.subConceptoRepo = subConceptoRepo;
        this.conceptoRepo = conceptoRepo;
        this.stockServicio = stockServicio;
    }

    /**
     * Calcula el importe final de un DetallePago usando:
     * totalAjustado = valorBase - descuento + recargo
     * importe = totalAjustado - ACobrar (acumulado)
     */
    public void calcularImporte(DetallePago detalle) {
        // 1. Inicio y validacion de concepto
        String conceptoDesc = (detalle.getDescripcionConcepto() != null) ? detalle.getDescripcionConcepto() : "N/A";
        log.info("[calcularImporte] INICIO - Calculo para Detalle ID: {} | Concepto: '{}' | Tipo: {}", detalle.getId(), conceptoDesc, detalle.getTipo());

        // 2. Obtencion de valor base
        double base = Optional.ofNullable(detalle.getValorBase()).orElse(0.0);
        log.info("[calcularImporte] Valor base obtenido: {}", base);
        log.info("[calcularImporte] Cuota/Cantidad: {}", detalle.getCuotaOCantidad());

        // 3. Calculo de descuentos
        double descuento;
        if (TipoDetallePago.MENSUALIDAD.equals(detalle.getTipo()) && detalle.getBonificacion() != null) {
            log.info("[calcularImporte] Procesando descuento para MENSUALIDAD con bonificacion");

            double descuentoFijo = Optional.ofNullable(detalle.getBonificacion().getValorFijo()).orElse(0.0);
            log.info("[calcularImporte] Descuento fijo: {}", descuentoFijo);

            Integer porcentaje = Optional.ofNullable(detalle.getBonificacion().getPorcentajeDescuento()).orElse(0);
            double descuentoPorcentaje = (porcentaje / 100.0) * base;
            log.info("[calcularImporte] Descuento porcentual ({}%): {}", porcentaje, descuentoPorcentaje);

            descuento = descuentoFijo + descuentoPorcentaje;
            log.info("[calcularImporte] Descuento TOTAL para mensualidad: {}", descuento);
        } else {
            log.info("[calcularImporte] Calculando descuento estandar");
            descuento = calcularDescuento(detalle, base);
            log.info("[calcularImporte] Descuento calculado: {}", descuento);
        }

        // 4. Calculo de recargos
        double recargo = 0;
        if (detalle.getTieneRecargo()) {
            log.info("[calcularImporte] Procesando recargo activo");
            recargo = (detalle.getRecargo() != null) ? obtenerValorRecargo(detalle, base) : 0.0;
            log.info("[calcularImporte] Recargo aplicado: {}", recargo);
        } else {
            log.info("[calcularImporte] Sin recargo aplicable");
        }

        // 5. Calculo de importe inicial
        double importeInicial = base - descuento + recargo;
        log.info("[calcularImporte] Calculo final: {} (base) - {} (descuento) + {} (recargo) = {}", base, descuento, recargo, importeInicial);

        log.info("[calcularImporte] Asignando importe inicial: {}", importeInicial);
        detalle.setImporteInicial(importeInicial);

        // 6. Gestion de importe pendiente
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
        log.info("[calcularDescuento] INICIO - Calculo para Detalle ID: {}", detalle.getId());

        if (detalle.getBonificacion() != null) {
            log.info("[calcularDescuento] Bonificacion encontrada: ID {}", detalle.getBonificacion().getId());

            double descuentoFijo = Optional.ofNullable(detalle.getBonificacion().getValorFijo()).orElse(0.0);
            log.info("[calcularDescuento] Componente fijo: {}", descuentoFijo);

            Integer porcentaje = Optional.ofNullable(detalle.getBonificacion().getPorcentajeDescuento()).orElse(0);
            double descuentoPorcentaje = (porcentaje / 100.0) * base;
            log.info("[calcularDescuento] Componente porcentual ({}% de {}): {}", porcentaje, base, descuentoPorcentaje);

            double totalDescuento = descuentoFijo + descuentoPorcentaje;
            log.info("[calcularDescuento] Descuento TOTAL calculado: {}", totalDescuento);

            return totalDescuento;
        }

        log.info("[calcularDescuento] Sin bonificacion aplicable - Descuento: 0");
        return 0.0;
    }

    public double obtenerValorRecargo(DetallePago detalle, double base) {
        Recargo recargo = detalle.getRecargo();
        if (!detalle.getTieneRecargo()) {
            return 0.0;
        }
        if (recargo != null) {
            int diaActual = LocalDate.now().getDayOfMonth();
            log.info("[obtenerValorRecargo] Detalle id={} | Dia actual={} | Dia de aplicacion={}", detalle.getId(), diaActual, recargo.getDiaDelMesAplicacion());
            if (diaActual != recargo.getDiaDelMesAplicacion()) {
                log.info("[obtenerValorRecargo] Dia actual no coincide; recargo=0");
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

        // Si se envia categoria, se aplica el filtro correspondiente
        if (StringUtils.hasText(categoria)) {
            spec = spec.and(filtrarPorCategoria(categoria, disciplina, tarifa, stock, subConcepto, detalleConcepto));
        }

        List<DetallePago> detalles = detallePagoRepositorio.findAll(spec, Sort.by(Sort.Direction.DESC, "id"));
        log.info("Consulta realizada. Numero de registros encontrados: {}", detalles.size());

        List<DetallePagoResponse> responses = detalles.stream().map(detallePagoMapper::toDTO).collect(Collectors.toList());
        log.info("Conversion a DetallePagoResponse completada. Regresando respuesta.");

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
                    // 1) Siempre filtramos por tipo CONCEPTO
                    predicates.add(cb.equal(root.get("tipo"), TipoDetallePago.CONCEPTO));
                    log.info("  • Añadido predicate → tipo = CONCEPTO");

                    // 2) Filtrar SubConcepto por descripción (exacta, tras trim)
                    if (StringUtils.hasText(subConcepto)) {
                        List<String> descSubs = Arrays.stream(subConcepto.split(","))
                                .map(String::trim)
                                .filter(StringUtils::hasText)
                                .toList();
                        log.info("  • SubConcepto raw split & trim → {}", descSubs);

                        List<Long> subIds = subConceptoRepo
                                .findByDescripcionInIgnoreCase(descSubs)
                                .stream()
                                .map(SubConcepto::getId)
                                .toList();
                        log.info("  • SubConcepto IDs encontrados → {}", subIds);

                        if (!subIds.isEmpty()) {
                            predicates.add(root.get("subConcepto").get("id").in(subIds));
                            log.info("    – Añadido predicate → subConcepto.id IN {}", subIds);
                        }
                    }

                    // 3) Filtrar Concepto (detalle) por descripción
                    if (StringUtils.hasText(detalleConcepto)) {
                        List<String> descCons = Arrays.stream(detalleConcepto.split(","))
                                .map(String::trim)
                                .filter(StringUtils::hasText)
                                .toList();
                        log.info("  • DetalleConcepto raw split & trim → {}", descCons);

                        List<Long> conIds = conceptoRepo
                                .findByDescripcionInIgnoreCase(descCons)
                                .stream()
                                .map(Concepto::getId)
                                .toList();
                        log.info("  • Concepto IDs encontrados → {}", conIds);

                        if (!conIds.isEmpty()) {
                            predicates.add(root.get("concepto").get("id").in(conIds));
                            log.info("    – Añadido predicate → concepto.id IN {}", conIds);
                        }
                    }
                    break;

                case "MATRICULA":
                    predicates.add(cb.like(cb.upper(root.get("descripcionConcepto")), "%MATRICULA%"));
                    log.info("  • Añadido predicate → descripcionConcepto LIKE '%MATRICULA%'");
                    break;

                default:
                    log.info("  • Categoría desconocida, no se añaden filtros adicionales");
                    break;
            }

            Predicate finalPredicate = cb.and(predicates.toArray(new Predicate[0]));
            log.info("✅ Predicado final: {}", finalPredicate);
            return finalPredicate;
        };
    }

    // =====================================================
    // Metodos CRUD
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

        // Actualizar campos; dependiendo de tu logica, puedes actualizar solo ciertos atributos.
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
            log.warn("[eliminarDetallePago] DetallePago id={} no encontrado. Abortando.", id);
            return;
        }
        DetallePago detalle = optionalDetalle.get();
        log.info("[eliminarDetallePago] DetallePago encontrado: {}", detalle);

        // 2. Obtener el Pago asociado
        Pago pago = detalle.getPago();
        if (pago != null) {
            log.info("[eliminarDetallePago] Pago asociado id={}", pago.getId());

            // 3. Remover el detalle de la colección (orphanRemoval eliminará la entidad)
            pago.removerDetalle(detalle);
            log.info("[eliminarDetallePago] Detalle removido de la colección del pago.");

            // 4. Recalcular montos
            double valorACobrar = detalle.getACobrar();
            double nuevoMonto = pago.getMonto() - valorACobrar;
            double nuevoMontoPagado = pago.getMontoPagado() - valorACobrar;
            if (nuevoMonto <= 0) {
                nuevoMonto = 0.0;
                nuevoMontoPagado = 0.0;
            }
            pago.setMonto(nuevoMonto);
            pago.setMontoPagado(nuevoMontoPagado);
            log.info("[eliminarDetallePago] Montos actualizados - monto={}, pagado={}",
                    nuevoMonto, nuevoMontoPagado);

            // 5. Guardar el pago para aplicar orphanRemoval
            pagoRepositorio.save(pago);

            // 6. Eliminar de observaciones la entrada de este detalle
            String descripcion = detalle.getDescripcionConcepto();
            String obs = pago.getObservaciones();
            if (obs != null && descripcion != null) {
                // Regex case-insensitive para cualquier etiqueta anterior sobre esta descripción
                String regex = "(?i)\\b(?:CTA|DEUDA|SALDA|ANULADO)\\s+"
                        + Pattern.quote(descripcion) + "\\b";

                // 1) Quitar todas las ocurrencias previas
                String limpio = obs
                        .replaceAll(regex, "")
                        .replaceAll("\\s{2,}", " ")  // colapsar espacios dobles
                        .trim();

                // 2) Usar sólo el resto (sin prefijo "ANULADO")
                String nuevaObs = limpio.isEmpty() ? "" : limpio + "\n";

                // 3) Guardar cambios
                pago.setObservaciones(nuevaObs.trim());
                pagoRepositorio.save(pago);
                log.info("[eliminarDetallePago] Observaciones actualizadas: {}", nuevaObs);
            }

            // 7. Si ya no quedan más DetallePago para este pago, eliminar el Pago
            long restantes = detallePagoRepositorio.countByPagoId(pago.getId());
            if (restantes == 0) {
                pagoRepositorio.delete(pago);
                log.info("[eliminarDetallePago] Pago id={} eliminado (sin detalles restantes)", pago.getId());
            }
        } else {
            log.warn("[eliminarDetallePago] No se encontró pago asociado. Solo se eliminará el detalle.");
        }

        // 8. Eliminar mensualidad si corresponde
        Mensualidad mensualidad = obtenerMensualidadSiExiste(detalle);
        if (mensualidad != null) {
            mensualidadRepositorio.delete(mensualidad);
            log.info("[eliminarDetallePago] Mensualidad eliminada id={}", mensualidad.getId());
        }

        // 9. “Eliminar” matrícula si corresponde
        Matricula matricula = obtenerMatriculaSiExiste(detalle);
        if (matricula != null && matricula.getId() != null) {
            matricula.setPagada(false);
            matricula.setFechaPago(null);
            // si tienes que guardar la matrícula:
            matriculaRepositorio.save(matricula);
            log.info("[eliminarDetallePago] Matrícula desmarcada como no pagada id={}", matricula.getId());
        }

        log.info("[eliminarDetallePago] Proceso completado para DetallePago id={}", id);
    }

    /**
     * Anula un DetallePago:
     * 1. Crea Pago si no existe.
     * 2. Clona el detalle original para histórico.
     * 3. Anula el original y limpia asociaciones.
     * 4. Actualiza montos del Pago.
     * 4a. Reemplaza en observaciones la entrada de este detalle por "ANULADO descripción".
     * 5. Unifica detalles activos del mismo alumno+descripción.
     * 6. Si no hay más detalles activos, anula el Pago.
     */
    @Transactional
    public DetallePagoResponse anularDetallePago(Long id) {
        // 1. Cargo el detalle y aseguro que exista un Pago
        DetallePago original = detallePagoRepositorio.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("DetallePago id=" + id + " no encontrado"));
        Pago pago = obtenerOPrepararPago(original);

        Long alumnoId = original.getAlumno().getId();
        String descripcion = original.getDescripcionConcepto();
        double montoAnular = Optional.ofNullable(original.getACobrar()).orElse(0.0);

        // 2. Clon HISTÓRICO **antes** de modificar el original
        DetallePago historico = crearClonHistorico(original);
        detallePagoRepositorio.save(historico);

        // 3. Anulo el original y limpio sus asociaciones
        anularDetalleOriginal(original);
        eliminarAsociados(original);

        // 3a. Si era un detalle de STOCK, restauro el stock
        if (original.getTipo() == TipoDetallePago.STOCK) {
            // Suponemos que original.getCuotaOCantidad() guarda la cantidad de unidades
            int cantidad = Integer.parseInt(original.getCuotaOCantidad());
            stockServicio.incrementarStock(descripcion, cantidad);
        }

        // 4. Ajusto los montos en el Pago, incluyendo el saldoRestante
        actualizarPago(pago, montoAnular);

        // 5. Marco en las observaciones que este detalle fue anulado
        actualizarObservacionesPorAnulado(pago, descripcion);

        // 6. Unifico cualquier detalle activo con la misma descripción
        unificarDetallesPorAlumno(alumnoId, descripcion);

        // 7. Si no queda ningún detalle activo, anulo todo el Pago
        verificarYAnularPagoSiCorresponda(pago);

        return detallePagoMapper.toDTO(original);
    }

    private Pago obtenerOPrepararPago(DetallePago original) {
        Pago pago = original.getPago();
        if (pago == null) {
            pago = new Pago();
            pago.setFecha(LocalDate.now());
            pago.setMonto(0.0);
            pago.setMontoPagado(0.0);
            pago.setObservaciones("CREADO_POR_ANULACION");
            pagoRepositorio.save(pago);
            original.setPago(pago);
        }
        return pago;
    }

    private DetallePago crearClonHistorico(DetallePago o) {
        DetallePago c = new DetallePago();
        // ---- Copiar asociaciones válidas ----
        c.setAlumno(o.getAlumno());
        c.setPago(o.getPago());
        c.setConcepto(o.getConcepto());
        c.setSubConcepto(o.getSubConcepto());
        c.setMensualidad(o.getMensualidad());
        c.setMatricula(o.getMatricula());
        c.setStock(o.getStock());
        c.setBonificacion(o.getBonificacion());
        c.setRecargo(o.getRecargo());

        // ---- Copiar datos básicos ----
        c.setDescripcionConcepto(o.getDescripcionConcepto());
        c.setTipo(o.getTipo());
        c.setCuotaOCantidad(o.getCuotaOCantidad());
        c.setValorBase(o.getValorBase());
        c.setImporteInicial(o.getImporteInicial());

        // ---- Los montos originales para histórico ----
        double importePendienteOriginal = Optional.ofNullable(o.getImportePendiente()).orElse(o.getImporteInicial());
        c.setImportePendiente(importePendienteOriginal + Optional.ofNullable(o.getACobrar()).orElse(0.0));
        c.setACobrar(0.0);
        c.setCobrado(false);

        // ---- Flags y estado ----
        c.setTieneRecargo(Boolean.TRUE.equals(o.getTieneRecargo()));
        c.setEstadoPago(EstadoPago.ACTIVO);
        c.setEsClon(true);
        c.setRemovido(false);

        // ---- Meta-datos ----
        c.setFechaRegistro(o.getFechaRegistro());
        c.setUsuario(o.getUsuario());

        return c;
    }

    private void anularDetalleOriginal(DetallePago d) {
        d.setCobrado(false);
        d.setACobrar(0.0);
        d.setImportePendiente(0.0);
        d.setEstadoPago(EstadoPago.ANULADO);
        d.setRemovido(true);
        detallePagoRepositorio.save(d);
    }

    private void eliminarAsociados(DetallePago d) {
        // Mensualidad: la dejamos pendiente de nuevo
        if (d.getMensualidad() != null) {
            Mensualidad m = d.getMensualidad();
            d.setMensualidad(null);
            detallePagoRepositorio.save(d);
            m.setEstado(EstadoMensualidad.PENDIENTE);
            mensualidadRepositorio.save(m);
        }
        // Matrícula: la marcamos como no pagada
        if (d.getMatricula() != null) {
            Matricula m = d.getMatricula();
            d.setMatricula(null);
            detallePagoRepositorio.save(d);
            m.setPagada(false);
            matriculaRepositorio.save(m);
        }
    }

    private void actualizarPago(Pago pago, double montoAnular) {
        // 1) Nuevo monto total y monto pagado
        double nuevoMonto = Math.max(0.0, pago.getMonto() - montoAnular);
        double nuevoMontoPagado = Math.max(0.0, pago.getMontoPagado() - montoAnular);

        // 2) Nuevo saldoRestante: lo podemos recalcular de dos formas
        // Opción A) Sumar el anulado al saldo que quedaba:
        double nuevoSaldo = pago.getImporteInicial() - (pago.getMontoPagado() - montoAnular);

        // Opción B) Recalcularlo desde importeInicial:
        // double nuevoSaldo = pago.getImporteInicial() - nuevoMontoPagado;

        // 3) Asignar y guardar
        pago.setMonto(nuevoMonto);
        pago.setMontoPagado(nuevoMontoPagado);
        pago.setSaldoRestante(nuevoSaldo);
        pagoRepositorio.save(pago);
    }

    private void actualizarObservacionesPorAnulado(Pago p, String descripcion) {
        String obs = p.getObservaciones();
        if (obs != null && descripcion != null) {
            String regex = "\\b(CTA|DEUDA|SALDA)\\s+" + Pattern.quote(descripcion) + "\\b";
            String reemplazo = "ANULADO " + descripcion + "\n";
            p.setObservaciones(obs.replaceAll(regex, reemplazo));
            pagoRepositorio.save(p);
        }
    }

    @Transactional
    public void unificarDetallesPorAlumno(Long alumnoId, String descripcion) {
        List<DetallePago> activos = detallePagoRepositorio
                .findByAlumnoIdAndDescripcionConceptoAndEstadoPago(alumnoId, descripcion, EstadoPago.ACTIVO);
        if (activos.size() <= 1) {
            return;
        }

        // 1) Elijo el primero como “principal”
        DetallePago principal = activos.get(0);

        // 2) Sumo todos los importes pendientes
        double sumaPendientes = activos.stream()
                .mapToDouble(DetallePago::getImportePendiente)
                .sum();

        // 3) Para cada duplicado:
        for (int i = 1; i < activos.size(); i++) {
            DetallePago dup = activos.get(i);

            // 3a) Si está asociada a una matrícula, la desvinculamos (sin borrar)
            if (dup.getMatricula() != null) {
                dup.setMatricula(null);
                detallePagoRepositorio.save(dup);
            }

            // 3b) Ahora sí borramos el detalle (no arrastra la matrícula)
            detallePagoRepositorio.delete(dup);
        }

        // 4) Actualizamos el detalle “principal”
        principal.setImportePendiente(sumaPendientes);
        principal.setCobrado(sumaPendientes == 0.0);
        detallePagoRepositorio.save(principal);

        // 5) Si viene de una mensualidad, sincronizamos su estado también
        if (principal.getMensualidad() != null) {
            Mensualidad m = principal.getMensualidad();
            m.setEstado(sumaPendientes == 0.0
                    ? EstadoMensualidad.PAGADO
                    : EstadoMensualidad.PENDIENTE);
            mensualidadRepositorio.save(m);
        }
    }

    @Transactional
    public Matricula obtenerMatriculaSiExiste(DetallePago detalle) {
        Long alumnoId = detalle.getAlumno().getId();
        String descripcion = detalle.getDescripcionConcepto();
        log.info("[obtenerMatriculaSiExiste] Verificando existencia de matricula para alumnoId={} con descripcion '{}'", alumnoId, descripcion);

        if (descripcion != null && descripcion.toUpperCase().contains("MATRICULA")) {
            // Obtener todos los detalles de tipo MATRICULA para el alumno y la descripcion dada
            List<DetallePago> detalles = detallePagoRepositorio
                    .findAllByAlumnoIdAndDescripcionConceptoIgnoreCaseAndTipo(alumnoId, descripcion, TipoDetallePago.MATRICULA);

            if (detalles != null && !detalles.isEmpty()) {
                // Ordenar los detalles por ID de forma ascendente
                detalles.sort(Comparator.comparing(DetallePago::getId));
                DetallePago primerDetalle = detalles.get(0);

                // Si el primer detalle no esta anulado, se fuerza a ANULADO (o se lanza excepcion, segun la logica de negocio)
                if (primerDetalle.getEstadoPago() != EstadoPago.ANULADO) {
                    log.error("[obtenerMatriculaSiExiste] Se encontro un DetallePago activo (no anulado) con descripcion '{}' para alumnoId={}. Forzando anulacion.", descripcion, alumnoId);
                    primerDetalle.setEstadoPago(EstadoPago.ANULADO);
                    detallePagoRepositorio.save(primerDetalle);
                }

                // Desligar la matricula de todos los DetallePago asociados para evitar problemas al eliminar la matricula
                for (DetallePago dp : detalles) {
                    if (dp.getMatricula() != null) {
                        dp.setMatricula(null);
                        detallePagoRepositorio.save(dp);
                        log.info("[obtenerMatriculaSiExiste] Se desvinculo la matricula del DetallePago id={}", dp.getId());
                    }
                }
                Matricula matricula = primerDetalle.getMatricula(); // Ahora deberia quedar la referencia "vieja" en el primer detalle
                log.info("[obtenerMatriculaSiExiste] Se retorna la matricula asociada al primer DetallePago (id={}) para alumnoId={}", primerDetalle.getId(), alumnoId);
                return matricula;
            } else {
                log.info("[obtenerMatriculaSiExiste] No se encontro ningun DetallePago para alumnoId={} con descripcion '{}'.", alumnoId, descripcion);
                return null;
            }
        } else {
            log.info("[obtenerMatriculaSiExiste] La descripcion '{}' no contiene 'MATRICULA', no se realiza verificacion.", descripcion);
            return null;
        }
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
        log.info("Verificando existencia de mensualidad para alumnoId={} con descripcion '{}'", alumnoId, descripcion);

        if (descripcion != null && descripcion.toUpperCase().contains("CUOTA")) {
            Optional<Mensualidad> mensualidadOpt = mensualidadRepositorio
                    .findByInscripcionAlumnoIdAndDescripcionIgnoreCase(alumnoId, descripcion);
            if (mensualidadOpt.isPresent()) {
                // Verificar si ya existe un detalle duplicado, ya sea activo o historico.
                boolean existeDetalleDuplicado = detallePagoRepositorio.existsByAlumnoIdAndDescripcionConceptoIgnoreCaseAndTipoAndEstadoPago(
                        alumnoId, descripcion, TipoDetallePago.MENSUALIDAD, EstadoPago.HISTORICO);
                boolean existeDetalleDuplicado2 = detallePagoRepositorio.existsByAlumnoIdAndDescripcionConceptoIgnoreCaseAndTipoAndEstadoPago(
                        alumnoId, descripcion, TipoDetallePago.MENSUALIDAD, EstadoPago.ACTIVO);
                if (existeDetalleDuplicado || existeDetalleDuplicado2) {
                    log.error("Ya existe una mensualidad o detalle de pago con descripcion '{}' para alumnoId={}", descripcion, alumnoId);
                }
                log.info("Mensualidad encontrada para alumnoId={} con descripcion '{}'", alumnoId, descripcion);
                return mensualidadOpt.get();
            } else {
                log.info("No se encontro mensualidad para alumnoId={} con descripcion '{}'", alumnoId, descripcion);
                return null;
            }
        } else {
            log.info("La descripcion '{}' no corresponde a una mensualidad", descripcion);
            return null;
        }
    }

    @Transactional
    public void verificarMensualidadNoDuplicada(DetallePago detalle) {
        Long alumnoId = detalle.getAlumno().getId();
        String descripcion = detalle.getDescripcionConcepto();
        log.info("Verificando existencia de mensualidad o detalle de pago para alumnoId={} con descripcion '{}'", alumnoId, descripcion);

        // Solo se verifica si la descripcion contiene "CUOTA"
        if (descripcion != null && descripcion.toUpperCase().contains("CUOTA")) {
            Optional<Mensualidad> mensualidadOpt = mensualidadRepositorio.findByInscripcionAlumnoIdAndDescripcionIgnoreCase(alumnoId, descripcion);

            if (mensualidadOpt.isPresent()) {
                // Si se encontro una mensualidad que contenga "CUOTA", se verifica si ya existe un detalle duplicado
                boolean existeDetalleDuplicado = detallePagoRepositorio.existsByAlumnoIdAndDescripcionConceptoIgnoreCaseAndTipoAndEstadoPago(alumnoId, descripcion, TipoDetallePago.MENSUALIDAD, EstadoPago.HISTORICO);
                boolean existeDetalleDuplicado2 = detallePagoRepositorio.existsByAlumnoIdAndDescripcionConceptoIgnoreCaseAndTipoAndEstadoPago(alumnoId, descripcion, TipoDetallePago.MENSUALIDAD, EstadoPago.ACTIVO);
                if (existeDetalleDuplicado || existeDetalleDuplicado2) {
                    log.error("Ya existe una mensualidad o detalle de pago con descripcion '{}' para alumnoId={}", descripcion, alumnoId);
                    throw new IllegalStateException("MENSUALIDAD YA COBRADA");
                }
            }
        }
    }

    /**
     * Si el pago solo tiene un DetallePago, o todos sus DetallePago están
     * anulados o aCobrar == 0, anula por completo el Pago.
     */
    private void verificarYAnularPagoSiCorresponda(Pago pago) {
        boolean todosInactivos = pago.getDetallePagos().stream()
                .allMatch(d ->
                        d.getEstadoPago() == EstadoPago.ANULADO
                                || Double.valueOf(0.0).equals(d.getACobrar())
                );

        if (todosInactivos) {
            pago.setMonto(0.0);
            pago.setMontoPagado(0.0);
            pago.setSaldoRestante(0.0);
            pago.setEstadoPago(EstadoPago.ANULADO);
            pagoRepositorio.save(pago);
        }
    }

    public List<DetallePagoResponse> listarPorAlumno(Long alumnoId) {
        List<DetallePago> detalles = detallePagoRepositorio.findPorAlumnoConACobrarMayorQue(alumnoId, 0.0);
        return detalles.stream()
                .map(detallePagoMapper::toDTO)
                .collect(Collectors.toList());
    }
}
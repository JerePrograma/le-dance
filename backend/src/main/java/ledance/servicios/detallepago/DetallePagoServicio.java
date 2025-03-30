package ledance.servicios.detallepago;

import jakarta.persistence.EntityNotFoundException;
import ledance.dto.pago.DetallePagoMapper;
import ledance.dto.pago.response.DetallePagoResponse;
import ledance.entidades.*;
import ledance.repositorios.DetallePagoRepositorio;
import ledance.repositorios.MatriculaRepositorio;
import ledance.repositorios.MensualidadRepositorio;
import org.flywaydb.core.internal.util.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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
    private final MatriculaRepositorio matriculaRepositorio;

    public DetallePagoServicio(DetallePagoRepositorio detallePagoRepositorio, DetallePagoMapper detallePagoMapper, MensualidadRepositorio mensualidadRepositorio, MatriculaRepositorio matriculaRepositorio) {
        this.detallePagoRepositorio = detallePagoRepositorio;
        this.detallePagoMapper = detallePagoMapper;
        this.mensualidadRepositorio = mensualidadRepositorio;
        this.matriculaRepositorio = matriculaRepositorio;
    }

    /**
     * Calcula el importe final de un DetallePago usando:
     * totalAjustado = valorBase - descuento + recargo
     * importe = totalAjustado - aCobrar (acumulado)
     */
    public void calcularImporte(DetallePago detalle) {
        // 1. Inicio y validación de concepto
        String conceptoDesc = (detalle.getDescripcionConcepto() != null)
                ? detalle.getDescripcionConcepto()
                : "N/A";
        log.info("[calcularImporte] INICIO - Cálculo para Detalle ID: {} | Concepto: '{}' | Tipo: {}",
                detalle.getId(), conceptoDesc, detalle.getTipo());
        log.debug("[calcularImporte] Detalle completo al inicio: {}", detalle.toString());

        // 2. Obtención de valor base
        double base = Optional.ofNullable(detalle.getValorBase()).orElse(0.0);
        log.info("[calcularImporte] Valor base obtenido: {}", base);
        log.debug("[calcularImporte] Cuota/Cantidad: {}", detalle.getCuotaOCantidad());

        // 3. Cálculo de descuentos
        double descuento;
        if (TipoDetallePago.MENSUALIDAD.equals(detalle.getTipo()) && detalle.getBonificacion() != null) {
            log.info("[calcularImporte] Procesando descuento para MENSUALIDAD con bonificación");

            double descuentoFijo = Optional.ofNullable(detalle.getBonificacion().getValorFijo()).orElse(0.0);
            log.debug("[calcularImporte] Descuento fijo: {}", descuentoFijo);

            Integer porcentaje = Optional.ofNullable(detalle.getBonificacion().getPorcentajeDescuento()).orElse(0);
            double descuentoPorcentaje = (porcentaje / 100.0) * base;
            log.debug("[calcularImporte] Descuento porcentual ({}%): {}", porcentaje, descuentoPorcentaje);

            descuento = descuentoFijo + descuentoPorcentaje;
            log.info("[calcularImporte] Descuento TOTAL para mensualidad: {}", descuento);
        } else {
            log.info("[calcularImporte] Calculando descuento estándar");
            descuento = calcularDescuento(detalle, base);
            log.info("[calcularImporte] Descuento calculado: {}", descuento);
        }

        // 4. Cálculo de recargos
        double recargo = 0;
        if (Boolean.TRUE.equals(detalle.getTieneRecargo())) {
            log.info("[calcularImporte] Procesando recargo activo");
            recargo = (detalle.getRecargo() != null) ? obtenerValorRecargo(detalle, base) : 0.0;
            log.info("[calcularImporte] Recargo aplicado: {}", recargo);
        } else {
            log.debug("[calcularImporte] Sin recargo aplicable");
        }

        // 5. Cálculo de importe inicial
        double importeInicial = base - descuento + recargo;
        log.info("[calcularImporte] Cálculo final: {} (base) - {} (descuento) + {} (recargo) = {}",
                base, descuento, recargo, importeInicial);

        log.info("[calcularImporte] Asignando importe inicial: {}", importeInicial);
        detalle.setImporteInicial(importeInicial);

        // 6. Gestión de importe pendiente
        if (detalle.getImportePendiente() == null) {
            log.info("[calcularImporte] Importe pendiente nulo - Asignando valor inicial: {}", importeInicial);
            detalle.setImportePendiente(importeInicial);
        } else {
            log.debug("[calcularImporte] Importe pendiente mantiene su valor actual: {}", detalle.getImportePendiente());
        }

        log.info("[calcularImporte] FIN - Resultados para Detalle ID: {} | Inicial: {} | Pendiente: {} | Cobrado: {}",
                detalle.getId(),
                detalle.getImporteInicial(),
                detalle.getImportePendiente(),
                detalle.getCobrado());
        log.debug("[calcularImporte] Estado final del detalle: {}", detalle.toString());
    }

    public double calcularDescuento(DetallePago detalle, double base) {
        log.info("[calcularDescuento] INICIO - Cálculo para Detalle ID: {}", detalle.getId());

        if (detalle.getBonificacion() != null) {
            log.debug("[calcularDescuento] Bonificación encontrada: ID {}", detalle.getBonificacion().getId());

            double descuentoFijo = Optional.ofNullable(detalle.getBonificacion().getValorFijo()).orElse(0.0);
            log.debug("[calcularDescuento] Componente fijo: {}", descuentoFijo);

            Integer porcentaje = Optional.ofNullable(detalle.getBonificacion().getPorcentajeDescuento()).orElse(0);
            double descuentoPorcentaje = (porcentaje / 100.0) * base;
            log.debug("[calcularDescuento] Componente porcentual ({}% de {}): {}", porcentaje, base, descuentoPorcentaje);

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

        // Solo se verifica si la descripción contiene "CUOTA"
        if (descripcion != null && descripcion.toUpperCase().contains("CUOTA")) {
            Optional<Mensualidad> mensualidadOpt = mensualidadRepositorio.findByInscripcionAlumnoIdAndDescripcionIgnoreCase(alumnoId, descripcion);

            if (mensualidadOpt.isPresent()) {
                // Si se encontró una mensualidad que contenga "CUOTA", se verifica si ya existe un detalle duplicado
                boolean existeDetalleDuplicado = detallePagoRepositorio.existsByAlumnoIdAndDescripcionConceptoIgnoreCaseAndTipo(
                        alumnoId, descripcion, TipoDetallePago.MENSUALIDAD
                );
                if (existeDetalleDuplicado) {
                    log.error("Ya existe una mensualidad o detalle de pago con descripción '{}' para alumnoId={}", descripcion, alumnoId);
                    throw new IllegalStateException("MENSUALIDAD YA COBRADA");
                }
            }
        }
    }

    @Transactional
    public void verificarMatriculaNoDuplicada(DetallePago detalle) {
        Long alumnoId = detalle.getAlumno().getId();
        String descripcion = detalle.getDescripcionConcepto();
        log.info("[verificarMatriculaNoDuplicada] Verificando existencia de matrícula o detalle de pago para alumnoId={} con descripción '{}'",
                alumnoId, descripcion);

        // Solo se verifica si la descripción contiene "MATRICULA"
        if (descripcion != null && descripcion.toUpperCase().contains("MATRICULA")) {
            // Ejemplo: Se busca la matrícula para el alumno para el año actual.
            int anioActual = LocalDate.now().getYear();
            log.info("[verificarMatriculaNoDuplicada] Buscando matrícula para alumnoId={} en el año {}", alumnoId, anioActual);
            Optional<Matricula> matriculaOpt = matriculaRepositorio.findByAlumnoIdAndAnio(alumnoId, anioActual);

            if (matriculaOpt.isPresent()) {
                Matricula matricula = matriculaOpt.get();
                log.info("[verificarMatriculaNoDuplicada] Matrícula encontrada: id={}, pagada={}", matricula.getId(), matricula.getPagada());
                // Si la matrícula ya está pagada, se considera duplicada.
                if (Boolean.TRUE.equals(matricula.getPagada())) {
                    log.info("[verificarMatriculaNoDuplicada] Matrícula ya pagada para alumnoId={}", alumnoId);
                    // Verificar si existe un detalle de pago de tipo MATRÍCULA duplicado.
                    boolean existeDetalleDuplicado = detallePagoRepositorio.existsByAlumnoIdAndDescripcionConceptoIgnoreCaseAndTipo(
                            alumnoId, descripcion, TipoDetallePago.MATRICULA);
                    log.info("[verificarMatriculaNoDuplicada] Resultado verificación detalle duplicado: {}", existeDetalleDuplicado);
                    if (existeDetalleDuplicado) {
                        log.error("[verificarMatriculaNoDuplicada] Ya existe una matrícula o detalle de pago con descripción '{}' para alumnoId={}",
                                descripcion, alumnoId);
                        throw new IllegalStateException("MATRICULA YA COBRADA");
                    }
                } else {
                    log.info("[verificarMatriculaNoDuplicada] Matrícula encontrada para alumnoId={} no está pagada, no se considera duplicada.", alumnoId);
                }
            } else {
                log.info("[verificarMatriculaNoDuplicada] No se encontró matrícula para alumnoId={} en el año {}", alumnoId, anioActual);
            }
        } else {
            log.info("[verificarMatriculaNoDuplicada] La descripción '{}' no contiene 'MATRICULA', no se realiza verificación.", descripcion);
        }
    }

}

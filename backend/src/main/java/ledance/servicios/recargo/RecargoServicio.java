package ledance.servicios.recargo;

import ledance.dto.recargo.RecargoMapper;
import ledance.dto.recargo.request.RecargoRegistroRequest;
import ledance.dto.recargo.response.RecargoResponse;
import ledance.entidades.*;
import ledance.repositorios.DetallePagoRepositorio;
import ledance.repositorios.MensualidadRepositorio;
import ledance.repositorios.ProcesoEjecutadoRepositorio;
import ledance.repositorios.RecargoRepositorio;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Service
@Transactional
public class RecargoServicio {

    private static final Logger log = LoggerFactory.getLogger(RecargoServicio.class);

    private final RecargoRepositorio recargoRepositorio;
    private final RecargoMapper recargoMapper;
    private final MensualidadRepositorio mensualidadRepositorio;
    private final DetallePagoRepositorio detallePagoRepositorio;
    private final ProcesoEjecutadoRepositorio procesoEjecutadoRepositorio;

    public RecargoServicio(RecargoRepositorio recargoRepositorio, RecargoMapper recargoMapper, MensualidadRepositorio mensualidadRepositorio, DetallePagoRepositorio detallePagoRepositorio,
                           ProcesoEjecutadoRepositorio procesoEjecutadoRepositorio) {
        this.recargoRepositorio = recargoRepositorio;
        this.recargoMapper = recargoMapper;
        this.mensualidadRepositorio = mensualidadRepositorio;
        this.detallePagoRepositorio = detallePagoRepositorio;
        this.procesoEjecutadoRepositorio = procesoEjecutadoRepositorio;
    }

    public RecargoResponse crearRecargo(RecargoRegistroRequest request) {
        Recargo recargo = recargoMapper.toEntity(request);
        recargoRepositorio.save(recargo);
        return recargoMapper.toResponse(recargo);
    }

    @Transactional(readOnly = true)
    public List<RecargoResponse> listarRecargos() {
        return recargoRepositorio.findAll().stream()
                .map(recargoMapper::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public RecargoResponse obtenerRecargo(Long id) {
        Recargo recargo = recargoRepositorio.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Recargo no encontrado con id: " + id));
        return recargoMapper.toResponse(recargo);
    }

    public RecargoResponse actualizarRecargo(Long id, RecargoRegistroRequest request) {
        Recargo recargo = recargoRepositorio.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Recargo no encontrado con id: " + id));

        recargo.setDescripcion(request.descripcion());
        recargo.setPorcentaje(request.porcentaje());
        recargo.setValorFijo(request.valorFijo());
        recargo.setDiaDelMesAplicacion(request.diaDelMesAplicacion());

        recargoRepositorio.save(recargo);
        return recargoMapper.toResponse(recargo);
    }

    public void eliminarRecargo(Long id) {
        Recargo recargo = recargoRepositorio.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Recargo no encontrado con id: " + id));
        recargoRepositorio.delete(recargo);
    }

    /**
     * Se invoca al login para aplicar recargos automáticos, tanto de día 15 como de día 1 (mes vencido).
     */
    @Transactional
    public void aplicarRecargosAutomaticos() {
        LocalDate today = LocalDate.now();
        log.info("Iniciando aplicación de recargos automáticos en login. Fecha actual: {}", today);

        // Recupera los recargos para el día 15 y 1
        Recargo recargo15 = obtenerRecargo(15);
        Recargo recargo1 = obtenerRecargo(1);

        // Aplica recargo de día 15
        aplicarRecargoDia15(today, recargo15);

        // Aplica recargo de día 1 (mes vencido)
        aplicarRecargoDia1(today, recargo1);
    }

    /**
     * Recupera el recargo correspondiente a un día específico y registra la acción.
     */
    private Recargo obtenerRecargo(int dia) {
        Optional<Recargo> optRecargo = recargoRepositorio.findByDiaDelMesAplicacion(dia);
        if (optRecargo.isEmpty()) {
            log.info("No se encontró recargo para el día {} en la base de datos.", dia);
            return null;
        }
        Recargo recargo = optRecargo.get();
        log.info("Recargo de día {} encontrado: id={}, porcentaje={}, valorFijo={}",
                dia, recargo.getId(), recargo.getPorcentaje(), recargo.getValorFijo());
        return recargo;
    }

    /**
     * Aplica el recargo de día 15 a las mensualidades pendientes, siempre que hoy sea
     * igual o posterior al día 15 del mes actual y no se haya aplicado ya en el período.
     */
    private void aplicarRecargoDia15(LocalDate today, Recargo recargo15) {
        if (recargo15 == null) {
            return;
        }

        LocalDate dia15DelMes = today.withDayOfMonth(15);
        if (today.isBefore(dia15DelMes)) {
            log.warn("No se ejecuta RECARGO_DIA_15 porque hoy ({}) es anterior al día 15 del mes actual ({})", today, dia15DelMes);
            return;
        }

        ProcesoEjecutado proceso15 = procesoEjecutadoRepositorio
                .findByProceso("RECARGO_DIA_15")
                .orElse(new ProcesoEjecutado("RECARGO_DIA_15", null));
        log.info("Proceso RECARGO_DIA_15, última ejecución registrada: {}", proceso15.getUltimaEjecucion());

        if (proceso15.getUltimaEjecucion() != null && !isDifferentPeriod(proceso15.getUltimaEjecucion(), today)) {
            log.info("Proceso RECARGO_DIA_15 ya fue ejecutado en el período actual (última ejecución: {})", proceso15.getUltimaEjecucion());
            return;
        }

        log.info("Ejecutando proceso RECARGO_DIA_15 para el período actual.");
        List<Mensualidad> mensualidades = mensualidadRepositorio
                .findByDescripcionContainingIgnoreCaseAndEstado("CUOTA", EstadoMensualidad.PENDIENTE);
        log.info("Se encontraron {} mensualidades pendientes para evaluar recargo de día 15.", mensualidades.size());

        for (Mensualidad m : mensualidades) {
            // Se toma la fecha de cuota y se reemplaza el día por el del recargo (15)
            LocalDate fechaComparacion15 = m.getFechaCuota().withDayOfMonth(recargo15.getDiaDelMesAplicacion());
            log.info("Procesando mensualidad id={} con fechaCuota={}, fechaComparacion15={}", m.getId(), m.getFechaCuota(), fechaComparacion15);

            if (!today.isBefore(fechaComparacion15)) { // today >= fechaComparacion15
                log.info("La fecha actual {} es igual o posterior a fechaComparacion15={}", today, fechaComparacion15);
                if (m.getRecargo() == null || !m.getRecargo().getId().equals(recargo15.getId())) {
                    log.info("Aplicando recargo de día 15 a mensualidad id={}", m.getId());
                    m.setRecargo(recargo15);
                    mensualidadRepositorio.save(m);
                    recalcularImporteMensualidad(m);

                    detallePagoRepositorio.findTopByMensualidadOrderByFechaRegistroDesc(m).ifPresent(detalle -> {
                        detalle.setRecargo(recargo15);
                        detalle.setTieneRecargo(true);
                        log.info("Recalculando importe en DetallePago id={} para recargo de día 15", detalle.getId());
                        recalcularImporteDetalle(detalle);
                    });
                } else {
                    log.info("Mensualidad id={} ya tiene aplicado el recargo de día 15.", m.getId());
                    recalcularImporteMensualidad(m);
                    detallePagoRepositorio.findByMensualidad(m).ifPresent(this::recalcularImporteDetalle);
                }
            } else {
                log.info("No se aplica recargo de día 15 para mensualidad id={} porque hoy {} es anterior a fechaComparacion15={}",
                        m.getId(), today, fechaComparacion15);
            }
        }

        proceso15.setUltimaEjecucion(today);
        procesoEjecutadoRepositorio.save(proceso15);
        log.info("Proceso RECARGO_DIA_15 completado. Flag actualizado a {}", today);
    }

    /**
     * Aplica el recargo de día 1 a las mensualidades vencidas, siempre que:
     * - Hoy sea posterior al primer día del mes siguiente (para evitar aplicar el recargo
     *   en el mismo día si la mensualidad fue creada el día 1).
     * - La mensualidad tenga fecha de cuota anterior al primer día del mes actual.
     * - Se cumpla que la fecha actual sea posterior a la fecha de cuota incrementada en un mes.
     */
    private void aplicarRecargoDia1(LocalDate today, Recargo recargo1) {
        if (recargo1 == null) {
            return;
        }

        // Solo se procesan mensualidades vencidas si hoy es posterior al primer día del mes siguiente
        LocalDate primerDiaMesSiguiente = today.withDayOfMonth(1).plusMonths(1);
        if (today.isBefore(primerDiaMesSiguiente)) {
            log.warn("No se ejecuta RECARGO_DIA_1 porque hoy ({}) es anterior al primer día del mes siguiente ({})", today, primerDiaMesSiguiente);
            return;
        }

        ProcesoEjecutado proceso1 = procesoEjecutadoRepositorio
                .findByProceso("RECARGO_DIA_1")
                .orElse(new ProcesoEjecutado("RECARGO_DIA_1", null));
        log.info("Proceso RECARGO_DIA_1, última ejecución registrada: {}", proceso1.getUltimaEjecucion());

        if (proceso1.getUltimaEjecucion() != null && !isDifferentPeriod(proceso1.getUltimaEjecucion(), today)) {
            log.info("Proceso RECARGO_DIA_1 ya fue ejecutado en el período actual (última ejecución: {})", proceso1.getUltimaEjecucion());
            return;
        }

        log.info("Ejecutando proceso RECARGO_DIA_1 para el período actual.");
        LocalDate primerDiaMesActual = today.withDayOfMonth(1);
        List<Mensualidad> vencidas = mensualidadRepositorio.findByFechaCuotaBeforeAndEstado(primerDiaMesActual, EstadoMensualidad.PENDIENTE);
        log.info("Se encontraron {} mensualidades vencidas (fechaCuota < {})", vencidas.size(), primerDiaMesActual);

        for (Mensualidad m : vencidas) {
            // Se suma un mes a la fecha de cuota para obtener la fecha límite
            LocalDate fechaComparacion1 = m.getFechaCuota().plusMonths(1);
            log.info("Procesando mensualidad id={} con fechaCuota={}, fechaComparacion1={}", m.getId(), m.getFechaCuota(), fechaComparacion1);

            /*
             * Para evitar aplicar recargo a mensualidades creadas el día 1, se requiere que hoy sea
             * posterior (no igual) a la fechaComparacion1. Así, si la mensualidad fue creada el 01/03/2025,
             * fechaComparacion1 será 01/04/2025 y no se aplicará recargo si hoy es 01/04/2025.
             */
            if (!today.isBefore(fechaComparacion1)) {
                log.info("La fecha actual {} es posterior a fechaComparacion1={}", today, fechaComparacion1);
                // Se aplica recargo si no se ha asignado uno o si el recargo actual es de menor prioridad (día 15)
                if (m.getRecargo() == null || m.getRecargo().getDiaDelMesAplicacion() == 15) {
                    log.info("Aplicando recargo de día 1 a mensualidad id={}", m.getId());
                    m.setRecargo(recargo1);
                    mensualidadRepositorio.save(m);
                    recalcularImporteMensualidad(m);

                    detallePagoRepositorio.findTopByMensualidadOrderByFechaRegistroDesc(m).ifPresent(detalle -> {
                        detalle.setRecargo(recargo1);
                        detalle.setTieneRecargo(true);
                        log.info("Recalculando importe en DetallePago id={} para recargo de día 1", detalle.getId());
                        recalcularImporteDetalle(detalle);
                    });
                } else {
                    log.info("Mensualidad id={} ya tiene aplicado un recargo de mayor prioridad; no se aplica recargo de día 1.", m.getId());
                    recalcularImporteMensualidad(m);
                    detallePagoRepositorio.findByMensualidad(m).ifPresent(this::recalcularImporteDetalle);
                }
            } else {
                log.info("No se aplica recargo de día 1 para mensualidad id={} porque hoy {} no es posterior a fechaComparacion1={}",
                        m.getId(), today, fechaComparacion1);
            }
        }

        proceso1.setUltimaEjecucion(today);
        procesoEjecutadoRepositorio.save(proceso1);
        log.info("Proceso RECARGO_DIA_1 completado. Flag actualizado a {}", today);
    }

    /**
     * Calcula el valor del recargo en base a la base dada (importeInicial) y los valores de la entidad Recargo.
     */
    private double calcularRecargo(Recargo recargo, double base) {
        if (recargo == null) {
            return 0.0;
        }
        double recargoFijo = recargo.getValorFijo() != null ? recargo.getValorFijo() : 0.0;
        double recargoPorcentaje = recargo.getPorcentaje() != null ? (recargo.getPorcentaje() / 100.0 * base) : 0.0;
        double totalRecargo = recargoFijo + recargoPorcentaje;
        log.info("Calculado recargo: fijo={} + porcentaje={} (sobre base {}) = {}", recargoFijo, recargoPorcentaje, base, totalRecargo);
        return totalRecargo;
    }

    /**
     * Recalcula el importe pendiente de una mensualidad, sumando el recargo (calculado sobre el importeInicial) y descontando el monto abonado.
     */
    public void recalcularImporteMensualidad(Mensualidad m) {
        log.info("Iniciando recálculo de mensualidad para id={}", m.getId());

        // Obtener importe inicial
        double base = m.getImporteInicial();
        log.info("Obtenido importe inicial: base={}", base);

        // Calcular recargo
        log.info("Calculando recargo para mensualidad id={} con porcentaje={}", m.getId(), m.getRecargo());
        double recargoValue = calcularRecargo(m.getRecargo(), base);
        log.info("Recargo calculado: recargoValue={}", recargoValue);

        // Calcular nuevo total
        double nuevoTotal = base + recargoValue;
        log.info("Nuevo total calculado: base={} + recargoValue={} = nuevoTotal={}",
                base, recargoValue, nuevoTotal);

        // Obtener monto abonado
        double montoAbonado = m.getMontoAbonado();
        log.info("Monto abonado obtenido: montoAbonado={}", montoAbonado);

        // Calcular nuevo pendiente
        double nuevoPendiente = nuevoTotal - montoAbonado;
        log.info("Nuevo pendiente calculado: nuevoTotal={} - montoAbonado={} = nuevoPendiente={}",
                nuevoTotal, montoAbonado, nuevoPendiente);

        // Actualizar importe pendiente
        log.info("Actualizando importe pendiente a nuevoPendiente={}", nuevoPendiente);
        m.setImportePendiente(nuevoPendiente);

        // Resumen de la operación
        log.info("Mensualidad id={} recalculada: base={}, recargoValue={}, nuevoTotal={}, montoAbonado={}, nuevoPendiente={}",
                m.getId(), base, recargoValue, nuevoTotal, montoAbonado, nuevoPendiente);

        // Guardar cambios
        log.info("Guardando cambios en repositorio para mensualidad id={}", m.getId());
        mensualidadRepositorio.save(m);
        log.info("Mensualidad id={} guardada exitosamente", m.getId());
    }

    /**
     * Recalcula el importe pendiente de un DetallePago, sumando el recargo (calculado sobre el importeInicial) y descontando lo que ya se haya cobrado (ACobrar).
     */
    public void recalcularImporteDetalle(DetallePago detalle) {
        double base = detalle.getImporteInicial();
        double recargoValue = 0;
        if (detalle.getTieneRecargo() && detalle.getMensualidad() != null || detalle.getTipo() == TipoDetallePago.MENSUALIDAD) {
            recargoValue = calcularRecargo(detalle.getRecargo(), base);
        }
        double nuevoTotal = base + recargoValue;
        double nuevoPendiente = nuevoTotal - detalle.getACobrar();
        detalle.setImportePendiente(nuevoPendiente);
        log.info("DetallePago id={} recalculado: base={}, recargoValue={}, nuevoTotal={}, ACobrar={}, nuevoPendiente={}",
                detalle.getId(), base, recargoValue, nuevoTotal, detalle.getACobrar(), nuevoPendiente);
        detallePagoRepositorio.save(detalle);
    }

    /**
     * Determina si la última ejecución NO corresponde al mismo mes y año que 'today'.
     * Devuelve true si las fechas pertenecen a períodos diferentes.
     */
    private boolean isDifferentPeriod(LocalDate lastExecution, LocalDate today) {
        boolean different = lastExecution.getYear() != today.getYear() || lastExecution.getMonth() != today.getMonth();
        log.info("Comparación de períodos: lastExecution={} vs today={} => Diferente: {}", lastExecution, today, different);
        return different;
    }

}

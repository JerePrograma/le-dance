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

    @Transactional
    public void aplicarRecargosAutomaticos() {
        LocalDate today = LocalDate.now();
        log.info("Iniciando aplicacion de recargos automaticos en login. Fecha actual: {}", today);

        // Recupera los recargos para el dia 15 y para el dia 1
        Recargo recargo15 = obtenerRecargo(15);
        Recargo recargo1 = obtenerRecargo(1);

        // Aplica recargo de dia 15 si corresponde
        aplicarRecargoDia15(today, recargo15);

        // Aplica recargo de dia 1 si corresponde
        aplicarRecargoDia1(today, recargo1);
    }

    /**
     * Recupera el recargo para un dia especifico.
     */
    private Recargo obtenerRecargo(int dia) {
        Optional<Recargo> optRecargo = recargoRepositorio.findByDiaDelMesAplicacion(dia);
        if (optRecargo.isEmpty()) {
            log.info("No se encontro recargo para el dia {} en la base de datos.", dia);
            return null;
        }
        Recargo recargo = optRecargo.get();
        log.info("Recargo de dia {} encontrado: id={}, porcentaje={}, valorFijo={}",
                dia, recargo.getId(), recargo.getPorcentaje(), recargo.getValorFijo());
        return recargo;
    }

    /**
     * Obtiene el ultimo DetallePago elegible para recargo de una mensualidad.
     * Se filtran aquellos que estan activos, no removidos y con importe pendiente > 0.
     */
    private Optional<DetallePago> obtenerUltimoDetalleElegible(Mensualidad m) {
        // Se asume que el repositorio tiene este metodo o se filtra en memoria.
        return detallePagoRepositorio.findTopByMensualidadOrderByFechaRegistroDesc(m)
                .filter(det -> !det.getRemovido() && det.getImportePendiente() > 0
                        && det.getEstadoPago() == EstadoPago.ACTIVO);
    }

    /**
     * Aplica el recargo del dia 15 a las mensualidades pendientes, siempre que:
     * - La fecha actual sea igual o posterior al dia 15.
     * - La mensualidad tenga importePendiente > 0 y estado PENDIENTE.
     * - Aun no se haya aplicado este recargo.
     */
    private void aplicarRecargoDia15(LocalDate today, Recargo recargo15) {
        if (recargo15 == null) {
            return;
        }

        LocalDate dia15DelMes = today.withDayOfMonth(15);
        if (today.isBefore(dia15DelMes)) {
            log.warn("No se ejecuta RECARGO_DIA_15 porque hoy ({}) es anterior al dia 15 del mes actual ({})", today, dia15DelMes);
            return;
        }

        ProcesoEjecutado proceso15 = procesoEjecutadoRepositorio
                .findByProceso("RECARGO_DIA_15")
                .orElse(new ProcesoEjecutado("RECARGO_DIA_15", null));
        log.info("Proceso RECARGO_DIA_15, ultima ejecucion registrada: {}", proceso15.getUltimaEjecucion());

        if (proceso15.getUltimaEjecucion() != null && !isDifferentPeriod(proceso15.getUltimaEjecucion(), today)) {
            log.info("Proceso RECARGO_DIA_15 ya fue ejecutado en el periodo actual (ultima ejecucion: {})", proceso15.getUltimaEjecucion());
            return;
        }

        log.info("Ejecutando proceso RECARGO_DIA_15 para el periodo actual.");
        // Se obtienen las mensualidades pendientes (verificando descripcion y estado)
        List<Mensualidad> mensualidades = mensualidadRepositorio
                .findByDescripcionContainingIgnoreCaseAndEstado("CUOTA", EstadoMensualidad.PENDIENTE);
        log.info("Se encontraron {} mensualidades pendientes para evaluar recargo de dia 15.", mensualidades.size());

        for (Mensualidad m : mensualidades) {
            // Solo procesamos mensualidades con importePendiente > 0
            if (m.getImportePendiente() == 0) {
                continue;
            }

            // Se toma la fecha de cuota y se reemplaza el dia por el del recargo (15)
            LocalDate fechaComparacion15 = m.getFechaCuota().withDayOfMonth(recargo15.getDiaDelMesAplicacion());
            log.info("Procesando mensualidad id={} con fechaCuota={}, fechaComparacion15={}", m.getId(), m.getFechaCuota(), fechaComparacion15);

            if (!today.isBefore(fechaComparacion15)) { // hoy >= fechaComparacion15
                log.info("La fecha actual {} es igual o posterior a fechaComparacion15={}", today, fechaComparacion15);
                // Si no se ha aplicado aun el recargo para dia 15
                if (m.getRecargo() == null || !m.getRecargo().getId().equals(recargo15.getId())) {
                    log.info("Aplicando recargo de dia 15 a mensualidad id={}", m.getId());
                    m.setRecargo(recargo15);
                    mensualidadRepositorio.save(m);
                    recalcularImporteMensualidad(m);

                    obtenerUltimoDetalleElegible(m).ifPresent(detalle -> {
                        detalle.setRecargo(recargo15);
                        detalle.setTieneRecargo(true);
                        log.info("Recalculando importe en DetallePago id={} para recargo de dia 15", detalle.getId());
                        recalcularImporteDetalle(detalle);
                    });
                } else {
                    log.info("Mensualidad id={} ya tiene aplicado el recargo de dia 15.", m.getId());
                    recalcularImporteMensualidad(m);
                    // Se recalcula en el ultimo detalle, en caso de actualizacion
                    obtenerUltimoDetalleElegible(m).ifPresent(this::recalcularImporteDetalle);
                }
            } else {
                log.info("No se aplica recargo de dia 15 para mensualidad id={} porque hoy {} es anterior a fechaComparacion15={}",
                        m.getId(), today, fechaComparacion15);
            }
        }

        proceso15.setUltimaEjecucion(today);
        procesoEjecutadoRepositorio.save(proceso15);
        log.info("Proceso RECARGO_DIA_15 completado. Flag actualizado a {}", today);
    }

    /**
     * Aplica el recargo del dia 1 a las mensualidades vencidas, siempre que:
     * - Hoy sea posterior al primer dia del mes siguiente.
     * - La mensualidad tenga fecha de cuota anterior al primer dia del mes actual.
     * - La mensualidad presente importePendiente > 0.
     * <p>
     * Si la mensualidad no tiene recargo previo, se aplica el recargo del dia 1 completo.
     * Si ya tiene recargo (por ejemplo, de dia 15), se calcula y aplica la diferencia adicional.
     */
    private void aplicarRecargoDia1(LocalDate today, Recargo recargo1) {
        if (recargo1 == null) {
            return;
        }

        LocalDate primerDiaMesSiguiente = today.withDayOfMonth(1).plusMonths(1);
        if (today.isBefore(primerDiaMesSiguiente)) {
            log.warn("No se ejecuta RECARGO_DIA_1 porque hoy ({}) es anterior al primer dia del mes siguiente ({})", today, primerDiaMesSiguiente);
            return;
        }

        ProcesoEjecutado proceso1 = procesoEjecutadoRepositorio
                .findByProceso("RECARGO_DIA_1")
                .orElse(new ProcesoEjecutado("RECARGO_DIA_1", null));
        log.info("Proceso RECARGO_DIA_1, ultima ejecucion registrada: {}", proceso1.getUltimaEjecucion());

        if (proceso1.getUltimaEjecucion() != null && !isDifferentPeriod(proceso1.getUltimaEjecucion(), today)) {
            log.info("Proceso RECARGO_DIA_1 ya fue ejecutado en el periodo actual (ultima ejecucion: {})", proceso1.getUltimaEjecucion());
            return;
        }

        log.info("Ejecutando proceso RECARGO_DIA_1 para el periodo actual.");
        LocalDate primerDiaMesActual = today.withDayOfMonth(1);
        List<Mensualidad> vencidas = mensualidadRepositorio.findByFechaCuotaBeforeAndEstado(primerDiaMesActual, EstadoMensualidad.PENDIENTE);
        log.info("Se encontraron {} mensualidades vencidas (fechaCuota < {})", vencidas.size(), primerDiaMesActual);

        for (Mensualidad m : vencidas) {
            // Solo procesar mensualidades con saldo pendiente
            if (m.getImportePendiente() == 0) {
                continue;
            }
            // Se suma un mes a la fecha de cuota para obtener la fecha limite
            LocalDate fechaComparacion1 = m.getFechaCuota().plusMonths(1);
            log.info("Procesando mensualidad id={} con fechaCuota={}, fechaComparacion1={}", m.getId(), m.getFechaCuota(), fechaComparacion1);

            if (!today.isBefore(fechaComparacion1)) { // hoy > fechaComparacion1
                log.info("La fecha actual {} es posterior a fechaComparacion1={}", today, fechaComparacion1);
                // Si la mensualidad no tiene recargo, se aplica el recargo del dia 1 completo
                if (m.getRecargo() == null) {
                    log.info("Aplicando recargo de dia 1 completo a mensualidad id={}", m.getId());
                    m.setRecargo(recargo1);
                    mensualidadRepositorio.save(m);
                    recalcularImporteMensualidad(m);

                    obtenerUltimoDetalleElegible(m).ifPresent(detalle -> {
                        detalle.setRecargo(recargo1);
                        detalle.setTieneRecargo(true);
                        log.info("Recalculando importe en DetallePago id={} para recargo de dia 1 (completo)", detalle.getId());
                        recalcularImporteDetalle(detalle);
                    });
                }
                // Si ya tiene recargo del dia 15, se debe aplicar solo la diferencia acumulativa
                else if (m.getRecargo().getDiaDelMesAplicacion() == 15) {
                    double base = m.getImporteInicial();
                    double recargoCompleto = calcularRecargo(recargo1, base);
                    double recargoPrevio = calcularRecargo(m.getRecargo(), base);
                    double adicional = recargoCompleto - recargoPrevio;
                    if (adicional > 0) {
                        log.info("Mensualidad id={} tiene recargo de dia 15; aplicando diferencia adicional de {} al recargo de dia 1", m.getId(), adicional);
                        // Actualizar el recargo de la mensualidad a recargo1 para indicar que ahora se ha completado el recargo total
                        m.setRecargo(recargo1);
                        mensualidadRepositorio.save(m);
                        recalcularImporteMensualidad(m);

                        obtenerUltimoDetalleElegible(m).ifPresent(detalle -> {
                            // Sumar la diferencia adicional al importe pendiente del detalle
                            double nuevoImporteDetalle = detalle.getImportePendiente() + adicional;
                            detalle.setImportePendiente(nuevoImporteDetalle);
                            detalle.setRecargo(recargo1); // Indicar que ahora se aplico recargo dia 1
                            detalle.setTieneRecargo(true);
                            log.info("Recalculando importe en DetallePago id={} para diferencia adicional de recargo de dia 1", detalle.getId());
                            recalcularImporteDetalle(detalle);
                        });
                    } else {
                        log.info("Mensualidad id={} ya tiene aplicado el recargo maximo; no se aplica recargo adicional de dia 1.", m.getId());
                        recalcularImporteMensualidad(m);
                        obtenerUltimoDetalleElegible(m).ifPresent(this::recalcularImporteDetalle);
                    }
                } else {
                    log.info("Mensualidad id={} ya tiene aplicado recargo de dia 1; no se realiza accion adicional.", m.getId());
                    recalcularImporteMensualidad(m);
                    obtenerUltimoDetalleElegible(m).ifPresent(this::recalcularImporteDetalle);
                }
            } else {
                log.info("No se aplica recargo de dia 1 para mensualidad id={} porque hoy {} no es posterior a fechaComparacion1={}",
                        m.getId(), today, fechaComparacion1);
            }
        }

        proceso1.setUltimaEjecucion(today);
        procesoEjecutadoRepositorio.save(proceso1);
        log.info("Proceso RECARGO_DIA_1 completado. Flag actualizado a {}", today);
    }

    /**
     * Calcula el valor del recargo para una base dada usando los valores configurados en la entidad Recargo.
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
     * Recalcula el importe pendiente de una Mensualidad, considerando:
     * - El importe inicial.
     * - El recargo (calculado sobre el importeInicial).
     * - El monto abonado.
     * Se utiliza para sincronizar el estado despues de aplicar recargos.
     */
    public void recalcularImporteMensualidad(Mensualidad m) {
        log.info("Iniciando recalculo de mensualidad para id={}", m.getId());

        double base = m.getImporteInicial();
        log.info("Obtenido importe inicial: base={}", base);

        double recargoValue = calcularRecargo(m.getRecargo(), base);
        log.info("Recargo calculado: recargoValue={}", recargoValue);

        double nuevoTotal = base + recargoValue;
        log.info("Nuevo total calculado: {} + {} = {}", base, recargoValue, nuevoTotal);

        double montoAbonado = m.getMontoAbonado();
        log.info("Monto abonado obtenido: {}", montoAbonado);

        double nuevoPendiente = nuevoTotal - montoAbonado;
        log.info("Nuevo pendiente calculado: {} - {} = {}", nuevoTotal, montoAbonado, nuevoPendiente);

        m.setImportePendiente(nuevoPendiente);
        log.info("Actualizando importe pendiente de mensualidad id={} a {}", m.getId(), nuevoPendiente);

        mensualidadRepositorio.save(m);
        log.info("Mensualidad id={} guardada exitosamente", m.getId());
    }

    /**
     * Recalcula el importe pendiente de un DetallePago.
     * Se suma el recargo (calculado sobre el importeInicial) y se descuenta lo ya cobrado (ACobrar).
     */
    public void recalcularImporteDetalle(DetallePago detalle) {
        double base = detalle.getImporteInicial();
        double recargoValue = 0;
        // Solo se calcula recargo si el detalle esta marcado como recargado y es de tipo MENSUALIDAD, o tiene asociacion a una mensualidad
        if (detalle.getTieneRecargo() && detalle.getMensualidad() != null
                || detalle.getTipo() == TipoDetallePago.MENSUALIDAD) {
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
     * Determina si la ultima ejecucion NO corresponde al mismo mes y año que 'today'.
     * Devuelve true si las fechas pertenecen a periodos diferentes.
     */
    private boolean isDifferentPeriod(LocalDate lastExecution, LocalDate today) {
        boolean different = lastExecution.getYear() != today.getYear() || lastExecution.getMonth() != today.getMonth();
        log.info("Comparacion de periodos: lastExecution={} vs today={} => Diferente: {}", lastExecution, today, different);
        return different;
    }

}

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

import java.math.BigDecimal;
import java.math.RoundingMode;
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
        log.info("Iniciando aplicación de recargos automáticos. Fecha: {}", today);

        Recargo recargo15 = obtenerRecargo(15);
        Recargo recargo1  = obtenerRecargo(1);

        aplicarRecargoDia15(today, recargo15);
        aplicarRecargoDia1(today, recargo1);
    }

    /**
     * Recupera el recargo para un dia especifico.
     */
    private Recargo obtenerRecargo(int dia) {
        return recargoRepositorio
                .findByDiaDelMesAplicacion(dia)
                .orElseGet(() -> {
                    log.info("No existe recargo para día {}", dia);
                    return null;
                });
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
        if (recargo15 == null) return;

        LocalDate dia15 = today.withDayOfMonth(15);
        if (today.isBefore(dia15)) return;

        ProcesoEjecutado proc = procesoEjecutadoRepositorio
                .findByProceso("RECARGO_DIA_15")
                .orElse(new ProcesoEjecutado("RECARGO_DIA_15", null));

        if (proc.getUltimaEjecucion() != null && !isDifferentPeriod(proc.getUltimaEjecucion(), today))
            return;

        List<Mensualidad> pendientes = mensualidadRepositorio
                .findByDescripcionContainingIgnoreCaseAndEstado("CUOTA", EstadoMensualidad.PENDIENTE);

        for (Mensualidad m : pendientes) {
            if (m.getImportePendiente() <= 0) continue;
            LocalDate comp15 = m.getFechaCuota().withDayOfMonth(recargo15.getDiaDelMesAplicacion());
            if (today.isBefore(comp15)) continue;

            // Aplico recargo sobre el saldo actual
            aplicarRecargoSobreSaldo(m, recargo15);

            // Si manejas detalles:
            obtenerUltimoDetalleElegible(m)
                    .ifPresent(d -> aplicarRecargoSobreSaldoDetalle(d, recargo15));
        }

        proc.setUltimaEjecucion(today);
        procesoEjecutadoRepositorio.save(proc);
    }

    private void aplicarRecargoSobreSaldoDetalle(DetallePago d, Recargo recargo) {
        double saldoPrevio = d.getImportePendiente();
        double recargoValue = calcularRecargo(recargo, saldoPrevio);
        double nuevoSaldo   = redondear(saldoPrevio + recargoValue);

        log.info("[RecargoDetalle] DetallePago id={} → saldo {} + recargo {} = {}",
                d.getId(), saldoPrevio, recargoValue, nuevoSaldo);

        d.setImportePendiente(nuevoSaldo);
        d.setRecargo(recargo);
        d.setTieneRecargo(true);
        detallePagoRepositorio.save(d);
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
        if (recargo1 == null) return;

        LocalDate primerDiaSiguiente = today.withDayOfMonth(1).plusMonths(1);
        if (today.isBefore(primerDiaSiguiente)) return;

        ProcesoEjecutado proc = procesoEjecutadoRepositorio
                .findByProceso("RECARGO_DIA_1")
                .orElse(new ProcesoEjecutado("RECARGO_DIA_1", null));

        if (proc.getUltimaEjecucion() != null && !isDifferentPeriod(proc.getUltimaEjecucion(), today))
            return;

        LocalDate primerDiaActual = today.withDayOfMonth(1);
        List<Mensualidad> vencidas = mensualidadRepositorio
                .findByFechaCuotaBeforeAndEstado(primerDiaActual, EstadoMensualidad.PENDIENTE);

        for (Mensualidad m : vencidas) {
            if (m.getImportePendiente() <= 0) continue;
            LocalDate comp1 = m.getFechaCuota().plusMonths(1);
            if (today.isBefore(comp1)) continue;

            // Aplico recargo completo o diferencia
            if (m.getRecargo() == null) {
                aplicarRecargoSobreSaldo(m, recargo1);
            } else if (m.getRecargo().getDiaDelMesAplicacion() == 15) {
                // calculo diferencia entre recargo1 y recargo15 sobre el saldo
                // (si necesitas un cálculo especial de diferencia, hazlo aquí)
                aplicarRecargoSobreSaldo(m, recargo1);
            }
            obtenerUltimoDetalleElegible(m)
                    .ifPresent(d -> aplicarRecargoSobreSaldoDetalle(d, recargo1));
        }

        proc.setUltimaEjecucion(today);
        procesoEjecutadoRepositorio.save(proc);
    }

    /**
     * Aplica el recargo SOBRE EL SALDO pendiente actual de la mensualidad.
     */
    private void aplicarRecargoSobreSaldo(Mensualidad m, Recargo recargo) {
        if (recargo == null) return;

        double saldoPrevio    = m.getImportePendiente();                // 1) tomo el saldo real
        double recargoValue   = calcularRecargo(recargo, saldoPrevio);  // 2) calculo recargo sobre ese saldo
        double saldoConRecargo = redondear(saldoPrevio + recargoValue);

        log.info("[RecargoSobreSaldo] Mensualidad id={} → saldo {} + recargo {} = {}",
                m.getId(), saldoPrevio, recargoValue, saldoConRecargo);

        m.setImportePendiente(saldoConRecargo);
        m.setRecargo(recargo);  // si quieres seguir guardando qué recargo se aplicó
        mensualidadRepositorio.save(m);
    }


    /**
     * Calcula el valor del recargo para una base dada usando los valores configurados en la entidad Recargo.
     */
    private double calcularRecargo(Recargo recargo, double base) {
        if (recargo == null) return 0;
        double fijo       = recargo.getValorFijo() != null ? recargo.getValorFijo() : 0;
        double porcentaje = recargo.getPorcentaje() != null
                ? recargo.getPorcentaje() / 100.0 * base
                : 0;
        return fijo + porcentaje;
    }

    /**
     * Metodo para redondear numeros a 2 decimales.
     */
    private double redondear(double val) {
        return BigDecimal.valueOf(val)
                .setScale(2, RoundingMode.HALF_UP)
                .doubleValue();
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

package ledance.servicios.caja;

import ledance.dto.caja.CajaDetalleDTO;
import ledance.dto.caja.CajaDiariaDTO;
import ledance.dto.caja.RendicionDTO;
import ledance.entidades.Egreso;
import ledance.entidades.Pago;
import ledance.entidades.MetodoPago;
import ledance.repositorios.EgresoRepositorio;
import ledance.repositorios.PagoRepositorio;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class CajaServicio {

    private final PagoRepositorio pagoRepositorio;
    private final EgresoRepositorio egresoRepositorio;

    public CajaServicio(PagoRepositorio pagoRepositorio, EgresoRepositorio egresoRepositorio) {
        this.pagoRepositorio = pagoRepositorio;
        this.egresoRepositorio = egresoRepositorio;
    }

    // -------------------------------------------------------------------------
    // 1. Planilla General de Caja: lista diaria con totales de efectivo, transferencia, etc.
    //    - Muestra cada dia dentro de un rango [start, end].
    //    - Suma los Pagos segun metodo de pago, y los Egresos para ese dia.
    //    - Retorna un listado de CajaDiariaDTO (uno por dia).
    // -------------------------------------------------------------------------
    public List<CajaDiariaDTO> obtenerPlanillaGeneral(LocalDate start, LocalDate end) {
        // a) Obtener todos los pagos activos en el rango
        List<Pago> pagos = pagoRepositorio.findByFechaBetweenAndActivoTrue(start, end);

        // b) Obtener todos los egresos en el rango
        List<Egreso> egresos = egresoRepositorio.findByFechaBetween(start, end);

        // c) Agrupar pagos por fecha
        Map<LocalDate, List<Pago>> pagosPorDia = pagos.stream()
                .collect(Collectors.groupingBy(Pago::getFecha));

        // d) Agrupar egresos por fecha
        Map<LocalDate, List<Egreso>> egresosPorDia = egresos.stream()
                .collect(Collectors.groupingBy(Egreso::getFecha));

        // e) Tomar todas las fechas involucradas (pueden ser dias en que hay pagos o egresos)
        Set<LocalDate> fechasCompletas = new HashSet<>();
        fechasCompletas.addAll(pagosPorDia.keySet());
        fechasCompletas.addAll(egresosPorDia.keySet());

        // f) Construir la lista de resultados
        List<CajaDiariaDTO> resultado = new ArrayList<>();

        for (LocalDate dia : fechasCompletas) {
            List<Pago> pagosDia = pagosPorDia.getOrDefault(dia, Collections.emptyList());
            List<Egreso> egresosDia = egresosPorDia.getOrDefault(dia, Collections.emptyList());

            double totalEfectivo = sumarPorMetodoPago(pagosDia, "EFECTIVO");
            double totalDebito   = sumarPorMetodoPago(pagosDia, "DEBITO");

            double totalEgresos    = egresosDia.stream().mapToDouble(Egreso::getMonto).sum();
            String rangoRecibos    = calcularRangoRecibos(pagosDia);

            double totalNeto = (totalEfectivo + totalDebito) - totalEgresos;

            CajaDiariaDTO dto = new CajaDiariaDTO(
                    dia,
                    rangoRecibos,
                    totalEfectivo,
                    totalDebito,
                    totalEgresos,
                    totalNeto
            );
            resultado.add(dto);
        }

        // g) Ordenar por fecha ascendente y retornar
        resultado.sort(Comparator.comparing(CajaDiariaDTO::fecha));
        return resultado;
    }

    /**
     * Auxiliar: Suma los montos de los Pagos que tengan metodoPago.descripcion == metodoDescripcion
     */
    private double sumarPorMetodoPago(List<Pago> pagos, String metodoDescripcion) {
        return pagos.stream()
                .filter(p -> p.getMetodoPago() != null
                        && p.getMetodoPago().getDescripcion().equalsIgnoreCase(metodoDescripcion))
                .mapToDouble(Pago::getMonto)
                .sum();
    }

    /**
     * Auxiliar: Devuelve un string que describa el rango de recibos (IDs) del dia
     *           p. ej. “Recibo #15 al #20” o “Sin Recibos”.
     */
    private String calcularRangoRecibos(List<Pago> pagos) {
        if (pagos.isEmpty()) {
            return "Sin Recibos";
        }
        long min = pagos.stream().mapToLong(Pago::getId).min().orElse(0);
        long max = pagos.stream().mapToLong(Pago::getId).max().orElse(0);
        if (min == max) {
            return "Recibo #" + min;
        }
        return String.format("Recibo #%d al #%d", min, max);
    }

    // -------------------------------------------------------------------------
    // 2. Caja Diaria: lista de Pagos (recibos) y Egresos de un dia
    // -------------------------------------------------------------------------
    public CajaDetalleDTO obtenerCajaDiaria(LocalDate fecha) {
        List<Pago>   pagosDia   = pagoRepositorio.findByFechaAndActivoTrue(fecha);
        List<Egreso> egresosDia = egresoRepositorio.findByFecha(fecha);

        return new CajaDetalleDTO(pagosDia, egresosDia);
    }

    // -------------------------------------------------------------------------
    // 3. Registrar un nuevo Egreso en un dia
    // -------------------------------------------------------------------------
    @Transactional
    public Egreso agregarEgreso(LocalDate fecha, Double monto, String obs, MetodoPago metodo) {
        Egreso egreso = new Egreso();
        egreso.setFecha(fecha);
        egreso.setMonto(monto);
        egreso.setObservaciones(obs);
        egreso.setMetodoPago(metodo); // Por defecto EFECTIVO, o lo que pases
        return egresoRepositorio.save(egreso);
    }

    // -------------------------------------------------------------------------
    // 4. Rendicion General de Caja: detalles y totales de un rango
    // -------------------------------------------------------------------------
    public RendicionDTO obtenerRendicionGeneral(LocalDate start, LocalDate end) {
        List<Pago>   pagos   = pagoRepositorio.findByFechaBetweenAndActivoTrue(start, end);
        List<Egreso> egresos = egresoRepositorio.findByFechaBetween(start, end);

        double totalEfectivo = sumarPorMetodoPago(pagos, "EFECTIVO");
        double totalDebito   = sumarPorMetodoPago(pagos, "DEBITO");

        double totalEgresos    = egresos.stream().mapToDouble(Egreso::getMonto).sum();

        return new RendicionDTO(pagos, egresos, totalEfectivo, totalDebito, totalEgresos);
    }

    // -------------------------------------------------------------------------
    // 5. Funcionalidades Adicionales
    // -------------------------------------------------------------------------

    /**
     * 5a) Eliminar (o anular) un Egreso.
     *     Dejas activo=false para no perder registro historico.
     */
    @Transactional
    public void anularEgreso(Long egresoId) {
        Egreso egreso = egresoRepositorio.findById(egresoId)
                .orElseThrow(() -> new IllegalArgumentException("Egreso no encontrado."));
        egreso.setActivo(false);
        egresoRepositorio.save(egreso);
    }

    /**
     * 5b) Actualizar un Egreso existente (fecha, monto, observaciones, metodoPago)
     */
    @Transactional
    public Egreso actualizarEgreso(Long egresoId,
                                   LocalDate nuevaFecha,
                                   Double nuevoMonto,
                                   String nuevasObs,
                                   MetodoPago nuevoMetodo) {
        Egreso egreso = egresoRepositorio.findById(egresoId)
                .orElseThrow(() -> new IllegalArgumentException("Egreso no encontrado."));
        egreso.setFecha(nuevaFecha);
        egreso.setMonto(nuevoMonto);
        egreso.setObservaciones(nuevasObs);
        egreso.setMetodoPago(nuevoMetodo);
        return egresoRepositorio.save(egreso);
    }

    /**
     * 5c) Calcular “saldo total” de la caja en un rango (ingresos - egresos).
     *     Aunque ya se muestra en rendicion, a veces se necesita un metodo directo.
     */
    public double calcularSaldoCaja(LocalDate start, LocalDate end) {
        List<Pago> pagos   = pagoRepositorio.findByFechaBetweenAndActivoTrue(start, end);
        double totalIngresos = pagos.stream().mapToDouble(Pago::getMonto).sum();

        List<Egreso> egresos = egresoRepositorio.findByFechaBetween(start, end);
        double totalEgresos = egresos.stream().mapToDouble(Egreso::getMonto).sum();

        return totalIngresos - totalEgresos;
    }

    /**
     * 5d) Filtrar pagos y egresos por metodo de pago (p.ej. Efectivo) en un rango
     */
    public List<Pago> obtenerPagosPorMetodo(LocalDate start, LocalDate end, String metodoDescripcion) {
        return pagoRepositorio.findByFechaBetweenAndActivoTrue(start, end).stream()
                .filter(p -> p.getMetodoPago() != null &&
                        p.getMetodoPago().getDescripcion().equalsIgnoreCase(metodoDescripcion))
                .collect(Collectors.toList());
    }

    public List<Egreso> obtenerEgresosPorMetodo(LocalDate start, LocalDate end, String metodoDescripcion) {
        return egresoRepositorio.findByFechaBetween(start, end).stream()
                .filter(e -> e.getMetodoPago() != null &&
                        e.getMetodoPago().getDescripcion().equalsIgnoreCase(metodoDescripcion))
                .collect(Collectors.toList());
    }
}

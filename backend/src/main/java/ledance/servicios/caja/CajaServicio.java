package ledance.servicios.caja;

import com.lowagie.text.DocumentException;
import ledance.dto.alumno.response.AlumnoResponse;
import ledance.dto.bonificacion.response.BonificacionResponse;
import ledance.dto.caja.*;
import ledance.dto.caja.response.CobranzasDataResponse;
import ledance.dto.concepto.response.ConceptoResponse;
import ledance.dto.disciplina.response.DisciplinaResponse;
import ledance.dto.egreso.EgresoMapper;
import ledance.dto.egreso.response.EgresoResponse;
import ledance.dto.metodopago.response.MetodoPagoResponse;
import ledance.dto.pago.PagoMapper;
import ledance.dto.pago.response.PagoResponse;
import ledance.dto.recargo.response.RecargoResponse;
import ledance.dto.stock.response.StockResponse;
import ledance.entidades.Egreso;
import ledance.entidades.Pago;
import ledance.entidades.MetodoPago;
import ledance.repositorios.EgresoRepositorio;
import ledance.repositorios.PagoRepositorio;
import ledance.servicios.alumno.AlumnoServicio;
import ledance.servicios.bonificacion.BonificacionServicio;
import ledance.servicios.concepto.ConceptoServicio;
import ledance.servicios.disciplina.DisciplinaServicio;
import ledance.servicios.pago.MetodoPagoServicio;
import ledance.servicios.pdfs.PdfService;
import ledance.servicios.recargo.RecargoServicio;
import ledance.servicios.stock.StockServicio;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.text.Normalizer;
import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

import static java.util.stream.Collectors.groupingBy;
import static java.util.stream.Collectors.summingDouble;

@Service
public class CajaServicio {

    private final PagoRepositorio pagoRepositorio;
    private final EgresoRepositorio egresoRepositorio;
    private final PagoMapper pagoMapper;
    private final EgresoMapper egresoMapper;
    private final AlumnoServicio alumnoServicio;
    private final DisciplinaServicio disciplinaServicio;
    private final ConceptoServicio conceptoServicio;
    private final StockServicio stockServicio;
    private final MetodoPagoServicio metodoPagoServicio;
    private final BonificacionServicio bonificacionServicio;
    private final RecargoServicio recargoServicio;
    private final PdfService pdfService;

    public CajaServicio(PagoRepositorio pagoRepositorio,
                        EgresoRepositorio egresoRepositorio,
                        PagoMapper pagoMapper,
                        EgresoMapper egresoMapper,
                        AlumnoServicio alumnoServicio,
                        DisciplinaServicio disciplinaServicio,
                        ConceptoServicio conceptoServicio,
                        StockServicio stockServicio,
                        MetodoPagoServicio metodoPagoServicio,
                        BonificacionServicio bonificacionServicio,
                        RecargoServicio recargoServicio,
                        PdfService pdfService) {
        this.pagoRepositorio = pagoRepositorio;
        this.egresoRepositorio = egresoRepositorio;
        this.pagoMapper = pagoMapper;
        this.egresoMapper = egresoMapper;
        this.alumnoServicio = alumnoServicio;
        this.disciplinaServicio = disciplinaServicio;
        this.conceptoServicio = conceptoServicio;
        this.stockServicio = stockServicio;
        this.metodoPagoServicio = metodoPagoServicio;
        this.bonificacionServicio = bonificacionServicio;
        this.recargoServicio = recargoServicio;
        this.pdfService = pdfService;
    }

    // -------------------------------------------------------------------------
    // Helpers de normalización y null-safety para métodos de pago
    // -------------------------------------------------------------------------
    private static String norm(String s) {
        if (s == null) return "SIN_METODO";
        String n = Normalizer.normalize(s, Normalizer.Form.NFD);
        // quita tildes y normaliza a MAYÚSCULAS sin espacios extremos
        return n.replaceAll("\\p{M}+", "").toUpperCase(Locale.ROOT).trim();
    }

    private static String metodoDesc(Pago p) {
        MetodoPago mp = p.getMetodoPago();
        return (mp == null || mp.getDescripcion() == null) ? "SIN_METODO" : norm(mp.getDescripcion());
    }

    private static String metodoDesc(MetodoPago mp) {
        return (mp == null || mp.getDescripcion() == null) ? "SIN_METODO" : norm(mp.getDescripcion());
    }

    // -------------------------------------------------------------------------
    // 1. Planilla General de Caja: Lista diaria con totales de ingresos y egresos.
    //    (Se mantiene el comportamiento original: columnas EFECTIVO/DEBITO)
    //    Se agregó null-safety y normalización para evitar NPEs y tildes.
    // -------------------------------------------------------------------------
    public List<CajaPlanillaDTO> obtenerPlanillaGeneral(LocalDate start, LocalDate end) {
        var pagos = pagoRepositorio.findPagosConAlumnoPorFecha(start, end);
        var egresos = egresoRepositorio.findByFechaBetween(start, end);

        var pagosPorDia = pagos.stream().collect(Collectors.groupingBy(Pago::getFecha));
        var egresosPorDia = egresos.stream().collect(Collectors.groupingBy(Egreso::getFecha));

        var todasFechas = new HashSet<LocalDate>();
        todasFechas.addAll(pagosPorDia.keySet());
        todasFechas.addAll(egresosPorDia.keySet());

        var resultado = new ArrayList<CajaPlanillaDTO>();
        for (var dia : todasFechas) {
            var pDia = pagosPorDia.getOrDefault(dia, List.of());
            var eDia = egresosPorDia.getOrDefault(dia, List.of());

            // Agrupo por descripción normalizada para evitar NPE y variantes con tildes
            var totalesPorMetodo = pDia.stream()
                    .collect(Collectors.groupingBy(CajaServicio::metodoDesc, Collectors.summingDouble(Pago::getMonto)));

            double ef = totalesPorMetodo.getOrDefault("EFECTIVO", 0.0);
            double db = totalesPorMetodo.getOrDefault("DEBITO", 0.0);

            double egTot = eDia.stream().mapToDouble(Egreso::getMonto).sum();
            double neto = (ef + db) - egTot;

            String rango = pDia.isEmpty()
                    ? ""
                    : pDia.stream().map(Pago::getId)
                    .sorted()
                    .collect(Collectors.collectingAndThen(
                            Collectors.toList(),
                            ids -> ids.get(0) + "-" + ids.get(ids.size() - 1)
                    ));

            resultado.add(new CajaPlanillaDTO(dia, rango, ef, db, egTot, neto));
        }

        resultado.sort(Comparator.comparing(CajaPlanillaDTO::fecha));
        return resultado;
    }

    // -------------------------------------------------------------------------
    // 1.b FLEX: Planilla General por día y por CADA método de pago (dinámica)
    // -------------------------------------------------------------------------
    public List<CajaPlanillaFlexDTO> obtenerPlanillaGeneralFlex(LocalDate start, LocalDate end) {
        var pagos = pagoRepositorio.findPagosConAlumnoPorFecha(start, end);
        var egresos = egresoRepositorio.findByFechaBetween(start, end);

        // Ingresos agrupados por fecha y método
        var ingresosPorDiaYMetodo = pagos.stream().collect(
                groupingBy(Pago::getFecha,
                        groupingBy(p -> new MetodoKey(
                                        p.getMetodoPago() == null ? null : p.getMetodoPago().getId(),
                                        metodoDesc(p)
                                ),
                                summingDouble(Pago::getMonto))));

        // Egresos agrupados por fecha y método (si Egreso tiene MetodoPago)
        var egresosPorDiaYMetodo = egresos.stream().collect(
                groupingBy(Egreso::getFecha,
                        groupingBy(e -> new MetodoKey(
                                        e.getMetodoPago() == null ? null : e.getMetodoPago().getId(),
                                        metodoDesc(e.getMetodoPago())
                                ),
                                summingDouble(Egreso::getMonto))));

        // Fechas presentes
        var fechas = new HashSet<LocalDate>();
        fechas.addAll(ingresosPorDiaYMetodo.keySet());
        fechas.addAll(egresosPorDiaYMetodo.keySet());

        var resultado = new ArrayList<CajaPlanillaFlexDTO>();

        for (var dia : fechas) {
            var mapIng = ingresosPorDiaYMetodo.getOrDefault(dia, Map.of());
            var mapEgr = egresosPorDiaYMetodo.getOrDefault(dia, Map.of());

            var ingresosPorMetodo = mapIng.entrySet().stream()
                    .sorted(Comparator.comparing(e -> e.getKey().nombre))
                    .map(e -> new MetodoTotalDTO(e.getKey().id, e.getKey().nombre, e.getValue()))
                    .toList();

            var egresosPorMetodo = mapEgr.entrySet().stream()
                    .sorted(Comparator.comparing(e -> e.getKey().nombre))
                    .map(e -> new MetodoTotalDTO(e.getKey().id, e.getKey().nombre, e.getValue()))
                    .toList();

            double totalIngresos = ingresosPorMetodo.stream().mapToDouble(MetodoTotalDTO::total).sum();
            double totalEgresos = egresosPorMetodo.stream().mapToDouble(MetodoTotalDTO::total).sum();
            double neto = totalIngresos - totalEgresos;

            // Rango de IDs de pagos del día
            var idsDelDia = pagos.stream()
                    .filter(p -> p.getFecha().equals(dia))
                    .map(Pago::getId)
                    .sorted()
                    .toList();
            String rango = idsDelDia.isEmpty() ? "" : (idsDelDia.get(0) + "-" + idsDelDia.get(idsDelDia.size() - 1));

            resultado.add(new CajaPlanillaFlexDTO(
                    dia,
                    rango,
                    ingresosPorMetodo,
                    totalIngresos,
                    egresosPorMetodo,
                    totalEgresos,
                    neto
            ));
        }

        resultado.sort(Comparator.comparing(CajaPlanillaFlexDTO::fecha));
        return resultado;
    }

    public CobranzasDataResponse obtenerDatosCobranzas() {
        // 1. Listado simplificado de alumnos
        List<AlumnoResponse> alumnos = alumnoServicio.listarAlumnosSimplificado();

        // 2. Disciplinas básicas
        List<DisciplinaResponse> disciplinas = disciplinaServicio.listarDisciplinasSimplificadas();

        // 3. Stocks
        List<StockResponse> stocks = stockServicio.listarStocksActivos();

        // 4. Métodos de pago
        List<MetodoPagoResponse> metodosPago = metodoPagoServicio.listar();

        // 5. Conceptos
        List<ConceptoResponse> conceptos = conceptoServicio.listarConceptos();

        // 6. Bonificaciones / Recargos
        List<BonificacionResponse> bonificaciones = bonificacionServicio.listarBonificaciones();
        List<RecargoResponse> recargos = recargoServicio.listarRecargos();

        // 7. DTO unificado
        return new CobranzasDataResponse(alumnos, disciplinas, stocks, metodosPago, conceptos, bonificaciones, recargos);
    }

    public CajaDetalleDTO obtenerCajaMensual(LocalDate start, LocalDate end) {
        List<Pago> pagosMes = pagoRepositorio.findPagosConAlumnoPorFecha(start, end);
        List<Egreso> egresosMes = egresoRepositorio.findByFechaBetween(start, end);
        return new CajaDetalleDTO(pagoMapper.toDTOList(pagosMes), egresoMapper.toDTOList(egresosMes));
    }

    public byte[] generarRendicionMensualPdf(CajaRendicionDTO caja) throws DocumentException, IOException {
        return pdfService.generarRendicionMensualPdf(caja);
    }

    /**
     * Devuelve la descripción del método de pago en mayúsculas,
     * o "EFECTIVO" si el método es null o su descripción es null.
     * (Se mantiene lógica original para Rendición)
     */
    private String descripcionMetodoSeguro(PagoResponse p) {
        var mp = p.metodoPago();
        return (mp == null || mp.descripcion() == null)
                ? "EFECTIVO"
                : norm(mp.descripcion());
    }

    public CajaRendicionDTO obtenerCajaRendicionMensual(LocalDate start, LocalDate end) {
        CajaDetalleDTO base = obtenerCajaMensual(start, end);

        // 1) Filtrar pagos inválidos: monto == 0 y observaciones vacías/null
        List<PagoResponse> pagosValidos = base.pagosDelDia().stream()
                .filter(p -> !(p.monto() == 0
                        && (p.observaciones() == null || p.observaciones().isBlank())))
                .toList();

        // 2) Totales de pagos (EFECTIVO/DEBITO) según lógica existente
        double totalEfectivo = pagosValidos.stream()
                .filter(p -> descripcionMetodoSeguro(p).equals("EFECTIVO"))
                .mapToDouble(PagoResponse::monto)
                .sum();

        double totalDebito = pagosValidos.stream()
                .filter(p -> descripcionMetodoSeguro(p).equals("DEBITO"))
                .mapToDouble(PagoResponse::monto)
                .sum();

        double totalCobrado = totalEfectivo + totalDebito;

        // 3) Totales de egresos (igual que antes)
        double totalEgresosEfectivo = base.egresosDelDia().stream()
                .filter(e -> {
                    var mp = e.metodoPago();
                    return mp != null && "EFECTIVO".equalsIgnoreCase(mp.descripcion());
                })
                .mapToDouble(EgresoResponse::monto)
                .sum();

        double totalEgresosDebito = base.egresosDelDia().stream()
                .filter(e -> {
                    var mp = e.metodoPago();
                    return mp != null && "DEBITO".equalsIgnoreCase(mp.descripcion());
                })
                .mapToDouble(EgresoResponse::monto)
                .sum();

        double totalEgresos = totalEgresosEfectivo + totalEgresosDebito;
        double totalNeto = totalCobrado - totalEgresos;

        // 4) DTO de salida
        return new CajaRendicionDTO(
                pagosValidos,
                base.egresosDelDia(),
                totalEfectivo,
                totalDebito,
                totalCobrado,
                totalEgresosEfectivo,
                totalEgresosDebito,
                totalEgresos,
                totalNeto
        );
    }

    /**
     * Devuelve la descripción del método de pago en mayúsculas,
     * o "EFECTIVO" si no existe método o su descripción es null.
     * (Se mantiene lógica original para CajaDiaria)
     */
    private String descripcionMetodoPagoSeguro(PagoResponse p) {
        var mp = p.metodoPago();
        return (mp == null || mp.descripcion() == null)
                ? "EFECTIVO"
                : norm(mp.descripcion());
    }

    /**
     * Caja diaria (comportamiento actual: totales EFECTIVO/DEBITO).
     */
    public CajaDiariaImp obtenerCajaDiaria(LocalDate fecha) {
        // 1) Traer entidades
        List<PagoResponse> pagos = pagoMapper.toDTOList(
                pagoRepositorio.findPagosConAlumnoPorFecha(fecha, fecha)
        );
        List<EgresoResponse> egresos = egresoMapper.toDTOList(
                egresoRepositorio.findByFecha(fecha)
        );

        // 2) Totales de pagos (EFECTIVO/DEBITO)
        double totalEfectivo = pagos.stream()
                .filter(p -> descripcionMetodoPagoSeguro(p).equals("EFECTIVO"))
                .mapToDouble(PagoResponse::monto)
                .sum();

        double totalDebito = pagos.stream()
                .filter(p -> descripcionMetodoPagoSeguro(p).equals("DEBITO"))
                .mapToDouble(PagoResponse::monto)
                .sum();

        double totalCobrado = totalEfectivo + totalDebito;

        // 3) Totales de egresos (efectivo y débito)
        double totalEgresosEfectivo = egresos.stream()
                .filter(e -> {
                    var mp = e.metodoPago();
                    return mp != null && "EFECTIVO".equalsIgnoreCase(mp.descripcion());
                })
                .mapToDouble(EgresoResponse::monto)
                .sum();

        double totalEgresosDebito = egresos.stream()
                .filter(e -> {
                    var mp = e.metodoPago();
                    return mp != null && "DEBITO".equalsIgnoreCase(mp.descripcion());
                })
                .mapToDouble(EgresoResponse::monto)
                .sum();

        double totalEgresos = totalEgresosEfectivo + totalEgresosDebito;

        // 4) Neto
        double totalNeto = totalCobrado - totalEgresos;

        // 5) DTO final
        return new CajaDiariaImp(
                pagos,
                egresos,
                totalEfectivo,
                totalDebito,
                totalCobrado,
                totalEgresosEfectivo,
                totalEgresosDebito,
                totalEgresos,
                totalNeto
        );
    }

    // -------------------------------------------------------------------------
    // 2.b FLEX: Caja diaria por CADA método de pago (dinámica)
    // -------------------------------------------------------------------------
    public CajaDiariaFlexDTO obtenerCajaDiariaFlex(LocalDate fecha) {
        var pagos = pagoMapper.toDTOList(pagoRepositorio.findPagosConAlumnoPorFecha(fecha, fecha));
        var egresos = egresoMapper.toDTOList(egresoRepositorio.findByFecha(fecha));

        // Ingresos por método
        var ingMap = pagos.stream().collect(
                groupingBy(p -> {
                    var mp = p.metodoPago();
                    Long id = (mp == null) ? null : mp.id();
                    String nombre = (mp == null || mp.descripcion() == null) ? "SIN_METODO" : norm(mp.descripcion());
                    return new MetodoKey(id, nombre);
                }, summingDouble(PagoResponse::monto)));

        var ingresosPorMetodo = ingMap.entrySet().stream()
                .sorted(Comparator.comparing(e -> e.getKey().nombre))
                .map(e -> new MetodoTotalDTO(e.getKey().id, e.getKey().nombre, e.getValue()))
                .toList();

        // Egresos por método
        var egrMap = egresos.stream().collect(
                groupingBy(e -> {
                    var mp = e.metodoPago();
                    Long id = (mp == null) ? null : mp.id();
                    String nombre = (mp == null || mp.descripcion() == null) ? "SIN_METODO" : norm(mp.descripcion());
                    return new MetodoKey(id, nombre);
                }, summingDouble(EgresoResponse::monto)));

        var egresosPorMetodo = egrMap.entrySet().stream()
                .sorted(Comparator.comparing(e -> e.getKey().nombre))
                .map(e -> new MetodoTotalDTO(e.getKey().id, e.getKey().nombre, e.getValue()))
                .toList();

        double totalIngresos = ingresosPorMetodo.stream().mapToDouble(MetodoTotalDTO::total).sum();
        double totalEgresos = egresosPorMetodo.stream().mapToDouble(MetodoTotalDTO::total).sum();
        double neto = totalIngresos - totalEgresos;

        return new CajaDiariaFlexDTO(
                pagos,
                egresos,
                ingresosPorMetodo,
                totalIngresos,
                egresosPorMetodo,
                totalEgresos,
                neto
        );
    }

    public byte[] generarCajaDiariaPdf(CajaDiariaImp cajaDetalleImp) {
        return pdfService.generarCajaDiariaPdf(cajaDetalleImp);
    }

    // -------------------------------------------------------------------------
    // Key para agrupar por método (id + nombre normalizado)
    // -------------------------------------------------------------------------
    private static final class MetodoKey {
        final Long id;
        final String nombre;

        MetodoKey(Long id, String nombre) {
            this.id = id;
            this.nombre = nombre;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (!(o instanceof MetodoKey)) return false;
            MetodoKey key = (MetodoKey) o;
            return Objects.equals(id, key.id) && Objects.equals(nombre, key.nombre);
        }

        @Override
        public int hashCode() {
            return Objects.hash(id, nombre);
        }
    }
}

package ledance.servicios.caja;

import com.lowagie.text.DocumentException;
import ledance.dto.alumno.response.AlumnoResponse;
import ledance.dto.bonificacion.response.BonificacionResponse;
import ledance.dto.caja.CajaDetalleDTO;
import ledance.dto.caja.CajaDiariaDTO;
import ledance.dto.caja.CajaDiariaImp;
import ledance.dto.caja.CajaRendicionDTO;
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
import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

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
                        AlumnoServicio alumnoServicio, DisciplinaServicio disciplinaServicio, ConceptoServicio conceptoServicio, StockServicio stockServicio, MetodoPagoServicio metodoPagoServicio, BonificacionServicio bonificacionServicio, RecargoServicio recargoServicio, PdfService pdfService) {
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
    // 1. Planilla General de Caja: Lista diaria con totales de ingresos y egresos.
    // -------------------------------------------------------------------------
    public List<CajaDiariaDTO> obtenerPlanillaGeneral(LocalDate start, LocalDate end) {
        // a) Obtener pagos activos (o HISTORICOS segun convenga) en el rango
        List<Pago> pagos = pagoRepositorio.findPagosConAlumnoPorFecha(start, end);
        // b) Obtener egresos en el rango
        List<Egreso> egresos = egresoRepositorio.findByFechaBetween(start, end);

        // Para agrupar, usaremos las fechas (asumimos que en los DTOs la fecha es de tipo LocalDate)
        Map<LocalDate, List<Pago>> pagosPorDia = pagos.stream()
                .collect(Collectors.groupingBy(Pago::getFecha));
        Map<LocalDate, List<Egreso>> egresosPorDia = egresos.stream()
                .collect(Collectors.groupingBy(Egreso::getFecha));

        // Todas las fechas involucradas
        Set<LocalDate> fechasCompletas = new HashSet<>();
        fechasCompletas.addAll(pagosPorDia.keySet());
        fechasCompletas.addAll(egresosPorDia.keySet());

        List<CajaDiariaDTO> resultado = new ArrayList<>();
        for (LocalDate dia : fechasCompletas) {
            List<Pago> pagosDia = pagosPorDia.getOrDefault(dia, Collections.emptyList());
            List<Egreso> egresosDia = egresosPorDia.getOrDefault(dia, Collections.emptyList());

            double totalEfectivo = sumarPorMetodoPago(pagosDia, "EFECTIVO");
            double totalDebito = sumarPorMetodoPago(pagosDia, "DEBITO");
            double totalEgresos = egresosDia.stream().mapToDouble(Egreso::getMonto).sum();

            String rangoRecibos = calcularRangoRecibos(pagosDia);
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

        resultado.sort(Comparator.comparing(CajaDiariaDTO::fecha));
        return resultado;
    }

    /**
     * Suma los montos de pagos que tengan el metodo de pago con la descripcion indicada.
     */
    private double sumarPorMetodoPago(List<Pago> pagos, String metodoDescripcion) {
        return pagos.stream()
                .filter(p -> p.getMetodoPago() != null
                        && p.getMetodoPago().getDescripcion().equalsIgnoreCase(metodoDescripcion))
                .mapToDouble(Pago::getMonto)
                .sum();
    }

    /**
     * Calcula el rango de recibos (IDs) para una lista de pagos.
     */
    private String calcularRangoRecibos(List<Pago> pagos) {
        if (pagos.isEmpty()) return "Sin Recibos";
        long min = pagos.stream().mapToLong(Pago::getId).min().orElse(0);
        long max = pagos.stream().mapToLong(Pago::getId).max().orElse(0);
        return min == max ? "Recibo #" + min : String.format("Recibo #%d al #%d", min, max);
    }

    /**
     * Obtiene la caja diaria para una fecha dada, mapea los pagos y egresos
     * y calcula todos los totales para devolver un CajaDiariaImp completo.
     */
    public CajaDiariaImp obtenerCajaDiaria(LocalDate fecha) {
        // 1) Traer datos
        List<Pago> pagosEnt = pagoRepositorio.findPagosConAlumnoPorFecha(fecha, fecha);
        List<Egreso> egresosEnt = egresoRepositorio.findByFecha(fecha);

        // 2) Mapear a DTOs
        List<PagoResponse> pagos = pagoMapper.toDTOList(pagosEnt);
        List<EgresoResponse> egresos = egresoMapper.toDTOList(egresosEnt);

        // 3) Calcular totales de pagos
        double totalEfectivo = pagos.stream()
                .filter(p -> p.metodoPago() != null
                        && "EFECTIVO".equalsIgnoreCase(p.metodoPago().descripcion()))
                .mapToDouble(PagoResponse::monto)
                .sum();

        double totalDebito = pagos.stream()
                .filter(p -> p.metodoPago() != null
                        && "DEBITO".equalsIgnoreCase(p.metodoPago().descripcion()))
                .mapToDouble(PagoResponse::monto)
                .sum();

        double totalCobrado = totalEfectivo + totalDebito;

        // 4) Calcular totales de egresos
        double totalEgresosEfectivo = egresos.stream()
                .filter(e -> e.metodoPago() != null
                        && "EFECTIVO".equalsIgnoreCase(e.metodoPago().descripcion()))
                .mapToDouble(EgresoResponse::monto)
                .sum();

        double totalEgresosDebito = egresos.stream()
                .filter(e -> e.metodoPago() != null
                        && "DEBITO".equalsIgnoreCase(e.metodoPago().descripcion()))
                .mapToDouble(EgresoResponse::monto)
                .sum();

        double totalEgresos = totalEgresosEfectivo + totalEgresosDebito;

        // 5) Calcular neto
        double totalNeto = totalCobrado - totalEgresos;

        // 6) Devolver DTO completo
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

    public CobranzasDataResponse obtenerDatosCobranzas() {
        // 1. Obtener listado simplificado de alumnos.
        List<AlumnoResponse> alumnos = alumnoServicio.listarAlumnosSimplificado();

        // 2. Obtener listado basico de disciplinas.
        List<DisciplinaResponse> disciplinas = disciplinaServicio.listarDisciplinasSimplificadas();

        // 3. Obtener listado de stocks.
        List<StockResponse> stocks = stockServicio.listarStocksActivos();

        // 4. Obtener listado de metodos de pago.
        List<MetodoPagoResponse> metodosPago = metodoPagoServicio.listar();

        // 5. Obtener listado de conceptos.
        List<ConceptoResponse> conceptos = conceptoServicio.listarConceptos();

        List<BonificacionResponse> bonificaciones = bonificacionServicio.listarBonificaciones();

        List<RecargoResponse> recargos = recargoServicio.listarRecargos();

        // 6. Armar y retornar el DTO unificado de cobranzas.
        return new CobranzasDataResponse(alumnos, disciplinas, stocks, metodosPago, conceptos, bonificaciones, recargos);
    }

    public CajaDetalleDTO obtenerCajaMensual(LocalDate start, LocalDate end) {
        List<Pago> pagosMes = pagoRepositorio.findPagosConAlumnoPorFecha(start, end);
        List<Egreso> egresosMes = egresoRepositorio.findByFechaBetween(start, end);

        // Se mapean las entidades a DTOs (si tienes mappers)
        return new CajaDetalleDTO(pagoMapper.toDTOList(pagosMes), egresoMapper.toDTOList(egresosMes));
    }

    public byte[] generarRendicionMensualPdf(CajaRendicionDTO caja) throws DocumentException {
        return pdfService.generarRendicionMensualPdf(caja);
    }


    public CajaRendicionDTO obtenerCajaRendicionMensual(LocalDate start, LocalDate end) {
        CajaDetalleDTO base = obtenerCajaMensual(start, end); // tu método existente

        // Cálculos de pagos
        double totalEfectivo = base.pagosDelDia().stream()
                .filter(p -> "EFECTIVO".equalsIgnoreCase(p.metodoPago().descripcion()))
                .mapToDouble(PagoResponse::monto)
                .sum();
        double totalDebito = base.pagosDelDia().stream()
                .filter(p -> "DEBITO".equalsIgnoreCase(p.metodoPago().descripcion()))
                .mapToDouble(PagoResponse::monto)
                .sum();
        double totalCobrado = totalEfectivo + totalDebito;

        // Cálculos de egresos
        double totalEgresosEfectivo = base.egresosDelDia().stream()
                .filter(e -> e.metodoPago() != null && "EFECTIVO".equalsIgnoreCase(e.metodoPago().descripcion()))
                .mapToDouble(EgresoResponse::monto)
                .sum();
        double totalEgresosDebito = base.egresosDelDia().stream()
                .filter(e -> e.metodoPago() != null && "DEBITO".equalsIgnoreCase(e.metodoPago().descripcion()))
                .mapToDouble(EgresoResponse::monto)
                .sum();
        double totalEgresos = totalEgresosEfectivo + totalEgresosDebito;

        // Neto
        double totalNeto = totalCobrado - totalEgresos;

        return new CajaRendicionDTO(
                base.pagosDelDia(),
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

    public byte[] generarCajaDiariaPdf(CajaDiariaImp cajaDetalleImp) {
        return pdfService.generarCajaDiariaPdf(cajaDetalleImp);
    }
}

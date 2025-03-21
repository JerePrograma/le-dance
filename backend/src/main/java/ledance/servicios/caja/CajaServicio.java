package ledance.servicios.caja;

import ledance.dto.alumno.response.AlumnoResponse;
import ledance.dto.caja.CajaDetalleDTO;
import ledance.dto.caja.CajaDiariaDTO;
import ledance.dto.caja.RendicionDTO;
import ledance.dto.caja.response.CobranzasDataResponse;
import ledance.dto.concepto.response.ConceptoResponse;
import ledance.dto.disciplina.response.DisciplinaResponse;
import ledance.dto.egreso.request.EgresoRegistroRequest;
import ledance.dto.egreso.response.EgresoResponse;
import ledance.dto.egreso.EgresoMapper;
import ledance.dto.metodopago.response.MetodoPagoResponse;
import ledance.dto.pago.PagoMapper;
import ledance.dto.stock.response.StockResponse;
import ledance.entidades.Egreso;
import ledance.entidades.EstadoPago;
import ledance.entidades.Pago;
import ledance.entidades.MetodoPago;
import ledance.repositorios.EgresoRepositorio;
import ledance.repositorios.MetodoPagoRepositorio;
import ledance.repositorios.PagoRepositorio;
import ledance.servicios.alumno.AlumnoServicio;
import ledance.servicios.concepto.ConceptoServicio;
import ledance.servicios.disciplina.DisciplinaServicio;
import ledance.servicios.pago.MetodoPagoServicio;
import ledance.servicios.stock.StockServicio;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class CajaServicio {

    private final PagoRepositorio pagoRepositorio;
    private final EgresoRepositorio egresoRepositorio;
    private final PagoMapper pagoMapper;
    private final EgresoMapper egresoMapper;
    private final MetodoPagoRepositorio metodoPagoRepositorio;
    private final AlumnoServicio alumnoServicio;
    private final DisciplinaServicio disciplinaServicio;
    private final ConceptoServicio conceptoServicio;
    private final StockServicio stockServicio;
    private final MetodoPagoServicio metodoPagoServicio;

    public CajaServicio(PagoRepositorio pagoRepositorio,
                        EgresoRepositorio egresoRepositorio,
                        PagoMapper pagoMapper,
                        EgresoMapper egresoMapper, MetodoPagoRepositorio metodoPagoRepositorio, AlumnoServicio alumnoServicio, DisciplinaServicio disciplinaServicio, ConceptoServicio conceptoServicio, StockServicio stockServicio, MetodoPagoServicio metodoPagoServicio) {
        this.pagoRepositorio = pagoRepositorio;
        this.egresoRepositorio = egresoRepositorio;
        this.pagoMapper = pagoMapper;
        this.egresoMapper = egresoMapper;
        this.metodoPagoRepositorio = metodoPagoRepositorio;
        this.alumnoServicio = alumnoServicio;
        this.disciplinaServicio = disciplinaServicio;
        this.conceptoServicio = conceptoServicio;
        this.stockServicio = stockServicio;
        this.metodoPagoServicio = metodoPagoServicio;
    }

    // -------------------------------------------------------------------------
    // 1. Planilla General de Caja: Lista diaria con totales de ingresos y egresos.
    // -------------------------------------------------------------------------
    public List<CajaDiariaDTO> obtenerPlanillaGeneral(LocalDate start, LocalDate end) {
        // a) Obtener pagos activos (o HISTÓRICOS según convenga) en el rango
        List<Pago> pagos = pagoRepositorio.findByFechaBetweenAndEstadoPago(start, end, EstadoPago.HISTORICO);
        // b) Obtener egresos en el rango
        List<Egreso> egresos = egresoRepositorio.findByFechaBetween(start, end);

        // Mapear entidades a DTOs para desacoplar la lógica:
        List<?> pagosDTO = pagos.stream()
                .map(pagoMapper::toDTO)
                .collect(Collectors.toList());
        List<?> egresosDTO = egresos.stream()
                .map(egresoMapper::toDTO)
                .collect(Collectors.toList());

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
     * Suma los montos de pagos que tengan el método de pago con la descripción indicada.
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

    // -------------------------------------------------------------------------
    // 2. Caja Diaria: Obtener pagos y egresos de un día específico.
    // -------------------------------------------------------------------------
    public CajaDetalleDTO obtenerCajaDiaria(LocalDate fecha) {
        List<Pago> pagosDia = pagoRepositorio.findByFechaBetweenAndEstadoPago(fecha, fecha, EstadoPago.HISTORICO);
        List<Egreso> egresosDia = egresoRepositorio.findByFecha(fecha);

        // Se pueden mapear a DTOs si se requiere
        return new CajaDetalleDTO(pagoMapper.toDTOList(pagosDia), egresoMapper.toDTOList(egresosDia));
    }

    // -------------------------------------------------------------------------
    // 3. Rendición General de Caja: Detalles y totales en un rango.
    // -------------------------------------------------------------------------
    public RendicionDTO obtenerRendicionGeneral(LocalDate start, LocalDate end) {
        List<Pago> pagos = pagoRepositorio.findByFechaBetweenAndEstadoPago(start, end, EstadoPago.HISTORICO);
        List<Egreso> egresos = egresoRepositorio.findByFechaBetween(start, end);

        double totalEfectivo = sumarPorMetodoPago(pagos, "EFECTIVO");
        double totalDebito = sumarPorMetodoPago(pagos, "DEBITO");
        double totalEgresos = egresos.stream().mapToDouble(Egreso::getMonto).sum();

        // Se mapean los pagos y egresos a DTOs si se desea, o se usan las entidades directamente
        return new RendicionDTO(pagoMapper.toDTOList(pagos), egresoMapper.toDTOList(egresos),
                totalEfectivo, totalDebito, totalEgresos);
    }

    // -------------------------------------------------------------------------
    // 4. Funcionalidades CRUD para Egresos
    // -------------------------------------------------------------------------

    @Transactional
    public void anularEgreso(Long egresoId) {
        Egreso egreso = egresoRepositorio.findById(egresoId)
                .orElseThrow(() -> new IllegalArgumentException("Egreso no encontrado."));
        egreso.setActivo(false);
        egresoRepositorio.save(egreso);
    }

    @Transactional
    public EgresoResponse actualizarEgreso(Long egresoId,
                                           EgresoRegistroRequest request) {
        Egreso egreso = egresoRepositorio.findById(egresoId)
                .orElseThrow(() -> new IllegalArgumentException("Egreso no encontrado."));
        // Actualiza los campos básicos mediante el mapper
        egresoMapper.updateEntityFromRequest(request, egreso);
        // Actualiza el método de pago si se envía
        if (request.metodoPagoId() != null) {
            MetodoPago metodo = metodoPagoRepositorio.findById(request.metodoPagoId())
                    .orElseThrow(() -> new IllegalArgumentException("Método de pago no encontrado."));
            egreso.setMetodoPago(metodo);
        }
        Egreso saved = egresoRepositorio.save(egreso);
        return egresoMapper.toDTO(saved);
    }

    public EgresoResponse obtenerEgresoPorId(Long id) {
        Egreso egreso = egresoRepositorio.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Egreso no encontrado."));
        return egresoMapper.toDTO(egreso);
    }

    public List<EgresoResponse> listarEgresos() {
        return egresoRepositorio.findAll()
                .stream()
                .filter(e -> e.getActivo() != null && e.getActivo())
                .map(egresoMapper::toDTO)
                .collect(Collectors.toList());
    }

    /**
     * 5d) Filtrar pagos y egresos por método de pago en un rango.
     */
    public List<Pago> obtenerPagosPorMetodo(LocalDate start, LocalDate end, String metodoDescripcion) {
        return pagoRepositorio.findByFechaBetweenAndEstadoPago(start, end, EstadoPago.ACTIVO)
                .stream()
                .filter(p -> p.getMetodoPago() != null &&
                        p.getMetodoPago().getDescripcion().equalsIgnoreCase(metodoDescripcion))
                .collect(Collectors.toList());
    }

    public List<Egreso> obtenerEgresosPorMetodo(LocalDate start, LocalDate end, String metodoDescripcion) {
        return egresoRepositorio.findByFechaBetween(start, end)
                .stream()
                .filter(e -> e.getMetodoPago() != null &&
                        e.getMetodoPago().getDescripcion().equalsIgnoreCase(metodoDescripcion))
                .collect(Collectors.toList());
    }

    /**
     * 5e) Calcular “saldo total” de la caja en un rango (ingresos - egresos).
     */
    public double calcularSaldoCaja(LocalDate start, LocalDate end) {
        double totalIngresos = pagoRepositorio.findByFechaBetweenAndEstadoPago(start, end, EstadoPago.HISTORICO)
                .stream()
                .mapToDouble(Pago::getMonto)
                .sum();
        double totalEgresos = egresoRepositorio.findByFechaBetween(start, end)
                .stream()
                .mapToDouble(Egreso::getMonto)
                .sum();
        return totalIngresos - totalEgresos;
    }

    public CobranzasDataResponse obtenerDatosCobranzas() {
        // 1. Obtener listado simplificado de alumnos.
        List<AlumnoResponse> alumnos = alumnoServicio.listarAlumnosSimplificado();

        // 2. Obtener listado básico de disciplinas.
        List<DisciplinaResponse> disciplinas = disciplinaServicio.listarDisciplinasSimplificadas();

        // 3. Obtener listado de stocks.
        List<StockResponse> stocks = stockServicio.listarStocksActivos();

        // 4. Obtener listado de métodos de pago.
        List<MetodoPagoResponse> metodosPago = metodoPagoServicio.listar();

        // 5. Obtener listado de conceptos.
        List<ConceptoResponse> conceptos = conceptoServicio.listarConceptos();

        // 6. Armar y retornar el DTO unificado de cobranzas.
        return new CobranzasDataResponse(alumnos, disciplinas, stocks, metodosPago, conceptos);
    }
}

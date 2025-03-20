package ledance.servicios.pago;

import jakarta.persistence.EntityNotFoundException;
import jakarta.persistence.PersistenceContext;
import jakarta.transaction.Transactional;
import ledance.dto.alumno.AlumnoMapper;
import ledance.dto.inscripcion.InscripcionMapper;
import ledance.dto.matricula.MatriculaMapper;
import ledance.dto.matricula.request.MatriculaRegistroRequest;
import ledance.dto.matricula.response.MatriculaResponse;
import ledance.dto.mensualidad.response.MensualidadResponse;
import ledance.dto.pago.PagoMapper;
import ledance.dto.pago.request.DetallePagoRegistroRequest;
import ledance.dto.pago.request.PagoRegistroRequest;
import ledance.entidades.*;
import ledance.repositorios.InscripcionRepositorio;
import ledance.repositorios.MatriculaRepositorio;
import ledance.repositorios.PagoRepositorio;
import ledance.servicios.detallepago.DetallePagoServicio;
import ledance.servicios.matricula.MatriculaServicio;
import ledance.servicios.mensualidad.MensualidadServicio;
import ledance.servicios.stock.StockServicio;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * -------------------------------------------------------------------------------------------------
 * 📌 Análisis del Servicio PaymentProcessor (ADAPTADO)
 * <p>
 * - Objetivo: Gestiona pagos de alumnos, incluyendo matrículas, mensualidades y otros conceptos.
 * - Flujo principal:
 * 1) crearPagoSegunInscripcion -> decide si se actualiza un pago pendiente (cobranza histórica) o se crea uno nuevo
 * 2) actualizarCobranzaHistorica -> aplica abonos sobre un pago con saldo pendiente
 * 3) processFirstPayment -> crea un nuevo pago desde cero (o clonando detalles pendientes)
 * 4) Métodos privados para actualizar importes, procesarMensualidades, matrículas, stock, etc.
 * <p>
 * Se apoya en PaymentCalculationServicio para el cálculo de descuentos/recargos y actualización de importes.
 * -------------------------------------------------------------------------------------------------
 */
@Service
public class PaymentProcessor {

    private static final Logger log = LoggerFactory.getLogger(PaymentProcessor.class);

    @PersistenceContext
    private jakarta.persistence.EntityManager entityManager;

    // Repositorios y Servicios
    private final PagoRepositorio pagoRepositorio;
    private final MatriculaRepositorio matriculaRepositorio;
    private final InscripcionRepositorio inscripcionRepositorio;

    // Mappers
    private final MatriculaMapper matriculaMapper;
    private final AlumnoMapper alumnoMapper;
    private final PagoMapper pagoMapper;
    private final InscripcionMapper inscripcionMapper;

    // Servicios auxiliares
    private final PaymentCalculationServicio calculationServicio;
    private final MatriculaServicio matriculaServicio;
    private final MensualidadServicio mensualidadServicio;
    private final StockServicio stockServicio;
    private final DetallePagoServicio detallePagoServicio;

    public PaymentProcessor(PagoRepositorio pagoRepositorio,
                            PaymentCalculationServicio calculationServicio,
                            MatriculaServicio matriculaServicio,
                            MensualidadServicio mensualidadServicio,
                            StockServicio stockServicio,
                            DetallePagoServicio detallePagoServicio,
                            MatriculaRepositorio matriculaRepositorio,
                            MatriculaMapper matriculaMapper,
                            AlumnoMapper alumnoMapper,
                            InscripcionRepositorio inscripcionRepositorio,
                            PagoMapper pagoMapper,
                            InscripcionMapper inscripcionMapper) {
        this.pagoRepositorio = pagoRepositorio;
        this.calculationServicio = calculationServicio;
        this.matriculaServicio = matriculaServicio;
        this.mensualidadServicio = mensualidadServicio;
        this.stockServicio = stockServicio;
        this.detallePagoServicio = detallePagoServicio;
        this.matriculaRepositorio = matriculaRepositorio;
        this.matriculaMapper = matriculaMapper;
        this.alumnoMapper = alumnoMapper;
        this.inscripcionRepositorio = inscripcionRepositorio;
        this.pagoMapper = pagoMapper;
        this.inscripcionMapper = inscripcionMapper;
    }

    // -------------------------------------------------------------------------------------------
    // 1️⃣ Creación de Pago según la Inscripción
    //     - Busca si existe un pago pendiente
    //     - Si es aplicable la cobranza histórica, actualiza ese pago
    //     - Caso contrario, crea un pago nuevo con processFirstPayment
    // -------------------------------------------------------------------------------------------
    @Transactional
    public Pago crearPagoSegunInscripcion(PagoRegistroRequest request) {
        log.info("[crearPagoSegunInscripcion] Procesando pago para inscripción ID={}",
                request.inscripcion() != null ? request.inscripcion().id() : "null");

        // Mapeamos el alumno
        Alumno alumno = alumnoMapper.toEntity(request.alumno());

        // Recuperamos (opcional) la inscripción
        Inscripcion inscripcion = null;
        if (request.inscripcion() != null && request.inscripcion().id() != null && request.inscripcion().id() > 0) {
            inscripcion = inscripcionRepositorio.findById(request.inscripcion().id())
                    .orElseThrow(() -> new EntityNotFoundException("Inscripción no encontrada con id="
                            + request.inscripcion().id()));
        }

        // Obtenemos el último pago pendiente del alumno
        Pago ultimoPendiente = obtenerUltimoPagoPendienteEntidad(alumno.getId());
        if (ultimoPendiente != null) {
            log.info("[crearPagoSegunInscripcion] Último pago pendiente: id={}, saldoRestante={}",
                    ultimoPendiente.getId(), ultimoPendiente.getSaldoRestante());
        }

        // Validamos si este nuevo request "encaja" con la cobranza histórica
        boolean esAplicablePagoHistorico = ultimoPendiente != null
                && ultimoPendiente.getSaldoRestante() > 0
                && esPagoHistoricoAplicable(ultimoPendiente, request);

        if (esAplicablePagoHistorico) {
            log.info("[crearPagoSegunInscripcion] Pago histórico aplicable, actualizando el Pago ID={}",
                    ultimoPendiente.getId());
            return actualizarCobranzaHistorica(ultimoPendiente.getId(), request);
        } else {
            log.info("[crearPagoSegunInscripcion] Creando un nuevo Pago para el alumno ID={}", alumno.getId());

            // Construimos la entidad Pago usando tu mapper
            Pago nuevoPago = pagoMapper.toEntity(request);
            // Añadimos alumno e inscripción (si existen)
            nuevoPago.setAlumno(alumno);
            nuevoPago.setInscripcion(inscripcion);

            // 2) Llamamos a la lógica de “primer pago”
            return processFirstPayment(nuevoPago);
        }
    }

    // -------------------------------------------------------------------------------------------
    // 2️⃣ Actualización de Cobranza Histórica
    //     - Aplica abonos sobre detalles de un pago con saldo pendiente
    //     - Marca el pago como HISTÓRICO si se salda
    //     - Crea un nuevo pago con los detalles que hayan quedado pendientes
    // -------------------------------------------------------------------------------------------
    @Transactional
    public Pago actualizarCobranzaHistorica(Long pagoId, PagoRegistroRequest request) {
        Pago historico = pagoRepositorio.findById(pagoId)
                .orElseThrow(() -> new IllegalArgumentException("Pago histórico no encontrado con ID=" + pagoId));

        // Mapeamos los abonos: se crea una clave "conceptoId_subConceptoId" y se suman los aCobrar
        Map<String, Double> mapaAbonos = request.detallePagos().stream()
                .collect(Collectors.toMap(
                        dto -> dto.conceptoId() + "_" + dto.subConceptoId(),
                        DetallePagoRegistroRequest::aCobrar,
                        Double::sum
                ));

        // Iteramos en cada detalle del pago histórico, aplicándole el abono correspondiente
        historico.getDetallePagos().forEach(detalle -> {
            String key = (detalle.getConcepto() != null ? detalle.getConcepto().getId() : "null")
                    + "_" + (detalle.getSubConcepto() != null ? detalle.getSubConcepto().getId() : "null");
            Double abono = mapaAbonos.getOrDefault(key, 0.0);

            // Usamos PaymentCalculationServicio para aplicar el abono
            calculationServicio.aplicarAbono(detalle, abono);

            // Si el detalle llega a 0, actualizamos estados relacionados (mensualidad/matrícula pagada, etc.)
            actualizarEstadosRelacionados(detalle, historico.getFecha());
        });

        // Marcamos este pago histórico como saldado
        historico.setEstadoPago(EstadoPago.HISTORICO);
        historico.setSaldoRestante(0.0);
        pagoRepositorio.save(historico);

        // Creamos un nuevo pago, clonando solo los detalles que sigan pendientes
        Pago nuevoPago = crearNuevoPagoDesdeHistorico(historico, request);
        return nuevoPago;
    }

    /**
     * Procesa el primer pago de un alumno:
     * - Toma la lista de DetallePago, asigna cada campo proveniente del request.
     * - Si un detalle ya existe (detalle.getId() != null), hacemos "clonarConPendiente" para partir de ese estado.
     * - Llamamos a "calcularImporte" en cada detalle para aplicar descuentos, recargos, etc.
     * - Actualizamos estados relacionados (mensualidad, matrícula, stock).
     * - Ajustamos los importes totales del Pago.
     *
     * @param pago Entidad Pago que llega con la lista de DetallePago (mapeados desde tu request).
     * @return El mismo Pago, con todos los detalles procesados (y sin persistir todavía).
     */
    @Transactional
    public Pago processFirstPayment(Pago pago) {
        log.info("[processFirstPayment] Iniciando procesamiento de primer pago, ID temporal={}", pago.getId());

        // Si no hay detalles, se asume importe=0
        List<DetallePago> detalles = pago.getDetallePagos();
        if (detalles == null || detalles.isEmpty()) {
            pago.setMontoPagado(0.0);
            pago.setSaldoRestante(0.0);
            // Se sugiere NO guardar aquí para no tener problemas con @NotNull (montoBasePago).
            // Sino, retornar el Pago y que lo guarde "afuera".
            return pago;
        }

        // Clonamos detalles si provienen de un pago anterior (detalle.getId() != null)
        List<DetallePago> nuevosDetalles = new ArrayList<>();
        for (DetallePago detalle : detalles) {
            // 1) Decide si clonar
            DetallePago procesado = (detalle.getId() != null)
                    ? detalle.clonarConPendiente(pago)
                    : detalle;
            procesado.setPago(pago);  // Importante para que haya referencia recíproca
            procesado.setAlumno(pago.getAlumno());
            // 2) Asignar valores tal cual vienen del request
            //    (Si no lo has hecho ya en un mapper, por ejemplo)
            procesado.setDescripcionConcepto(detalle.getDescripcionConcepto());
            procesado.setMontoOriginal(detalle.getMontoOriginal());
            procesado.setImportePendiente(detalle.getImportePendiente());
            procesado.setaCobrar(detalle.getaCobrar());
            procesado.setCuota(detalle.getCuota());
            procesado.setCobrado(detalle.getCobrado());
            // Campos de recargo/bonificación (si se manejan en la entidad):
            procesado.setBonificacion(detalle.getBonificacion());
            procesado.setRecargo(detalle.getRecargo());

            calculationServicio.calcularImportesDetalle(pago, detalle, pago.getInscripcion());

            if (detalle.getImportePendiente() == null) {
                // Ejemplo: setear a importeInicial - aCobrar, o a importeInicial, etc.
                detalle.setImportePendiente(detalle.getImporteInicial() - detalle.getaCobrar());
            }

            // 4) (Opcional) Determinar el tipo: MENSUALIDAD, MATRICULA, etc., si lo manejas en la lógica
            //    procesado.setTipo(determinarTipoSegúnConcepto(...));

            // 5) Si ya se saldó por completo (importePendiente==0), marcar estados relacionados
            actualizarEstadosRelacionados(procesado, pago.getFecha());

            // 6) Agregar el detalle procesado a la lista
            nuevosDetalles.add(procesado);
        }

        // 7) Reemplazar los detalles del Pago con la nueva lista
        pago.setDetallePagos(nuevosDetalles);

        // 8) Recalcular importes totales en el Pago (sumas de aCobrar, etc.)
        actualizarImportesTotalesPago(pago);

        // 9) Verificar si saldo==0 -> Pago HISTÓRICO
        verificarSaldoRestante(pago);

        // 10) Retornar el Pago *sin* persistirlo aquí para que "afuera" se asigne montoBasePago si es @NotNull
        //     y se haga el save final.
        return pago;
    }

    // -------------------------------------------------------------------------------------------
    // 4️⃣ Actualización de Importes del Pago
    //     - Calcula cuánto se ha abonado (montoPagado)
    //     - Calcula el saldoRestante según los importesPendientes de cada detalle
    //     - Marca el pago como HISTÓRICO si no queda nada pendiente
    // -------------------------------------------------------------------------------------------
    public void actualizarImportesTotalesPago(Pago pago) {
        log.info("[actualizarImportesTotalesPago] Actualizando totales para pago ID={}", pago.getId());

        double totalAbonado = pago.getDetallePagos().stream()
                .mapToDouble(det -> Optional.ofNullable(det.getaCobrar()).orElse(0.0))
                .sum();

        double totalPendiente = pago.getDetallePagos().stream()
                .mapToDouble(det -> Optional.ofNullable(det.getImportePendiente()).orElse(0.0))
                .sum();

        pago.setMonto(totalAbonado);
        pago.setSaldoRestante(totalPendiente);
        pago.setMontoPagado(totalAbonado);

        if (totalPendiente == 0.0) {
            pago.setEstadoPago(EstadoPago.HISTORICO);
            log.info("[actualizarImportesTotalesPago] Pago ID={} marcado como HISTÓRICO (saldado)", pago.getId());
        } else {
            pago.setEstadoPago(EstadoPago.ACTIVO);
            log.info("[actualizarImportesTotalesPago] Pago ID={} permanece ACTIVO, saldoRestante={}",
                    pago.getId(), totalPendiente);
        }
    }

    // Método “wrapper” para garantizar que nunca quede saldo negativo y marcar Histórico si llega a 0
    private void verificarSaldoRestante(Pago pago) {
        if (pago.getSaldoRestante() < 0) {
            pago.setSaldoRestante(0.0);
        }
        if (pago.getSaldoRestante() == 0.0) {
            pago.setEstadoPago(EstadoPago.HISTORICO);
        }
    }

    // -------------------------------------------------------------------------------------------
    // 5️⃣ Métodos Auxiliares: crear nuevo Pago desde histórico, actualizar estados relacionados, etc.
    // -------------------------------------------------------------------------------------------

    private Pago crearNuevoPagoDesdeHistorico(Pago historico, PagoRegistroRequest request) {
        Pago nuevoPago = new Pago();
        nuevoPago.setFecha(request.fecha());
        nuevoPago.setFechaVencimiento(request.fechaVencimiento());
        nuevoPago.setAlumno(historico.getAlumno());
        nuevoPago.setInscripcion(historico.getInscripcion());
        nuevoPago.setMetodoPago(historico.getMetodoPago());
        nuevoPago.setTipoPago(TipoPago.RESUMEN);
        nuevoPago.setEstadoPago(EstadoPago.ACTIVO);
        nuevoPago.setObservaciones(historico.getObservaciones());
        nuevoPago.setSaldoAFavor(0.0);

        // Clonamos sólo los detalles que aún tengan importePendiente > 0 en el histórico
        List<DetallePago> pendientes = historico.getDetallePagos().stream()
                .filter(det -> Optional.ofNullable(det.getImportePendiente()).orElse(0.0) > 0)
                .map(det -> {
                    DetallePago nuevoDet = det.clonarConPendiente(nuevoPago);
                    calculationServicio.calcularImporte(nuevoDet);  // recalculamos su importe
                    return nuevoDet;
                })
                .collect(Collectors.toList());

        nuevoPago.setDetallePagos(pendientes);
        actualizarImportesTotalesPago(nuevoPago);

        return nuevoPago;
    }

    /**
     * Actualiza estados específicos al “saldar” un detalle:
     * - Mensualidad se marca como PAGADA
     * - Matrícula se marca como pagada
     * - Stock se descuenta
     */
    private void actualizarEstadosRelacionados(DetallePago detalle, LocalDate fechaPago) {
        // Si el detalle ya quedó en 0, y no estaba cobrado, lo marcamos como cobrado
        if (detalle.getImportePendiente() != null && detalle.getImportePendiente() <= 0 && !detalle.getCobrado()) {
            detalle.setCobrado(true);

            switch (detalle.getTipo()) {
                case MENSUALIDAD -> {
                    if (detalle.getMensualidad() != null) {
                        mensualidadServicio.marcarComoPagada(detalle.getMensualidad().getId(), fechaPago);
                    }
                }
                case MATRICULA -> {
                    if (detalle.getMatricula() != null) {
                        matriculaServicio.actualizarEstadoMatricula(
                                detalle.getMatricula().getId(),
                                new MatriculaRegistroRequest(detalle.getAlumno().getId(),
                                        detalle.getMatricula().getAnio(),
                                        true,
                                        fechaPago));
                    }
                }
                case STOCK -> {
                    if (detalle.getStock() != null) {
                        stockServicio.reducirStock(detalle.getStock().getNombre(), 1);
                    }
                }
                default -> {
                    // Para CONCEPTO u otros tipos no hay lógica de actualización extra
                }
            }
        }
    }

    // -------------------------------------------------------------------------------------------
    // 6️⃣ Consulta auxiliar: obtener último pago pendiente
    // -------------------------------------------------------------------------------------------
    public Pago obtenerUltimoPagoPendienteEntidad(Long alumnoId) {
        return pagoRepositorio.findTopByAlumnoIdAndEstadoPagoAndSaldoRestanteGreaterThanOrderByFechaDesc(
                alumnoId, EstadoPago.ACTIVO, 0.0
        ).orElse(null);
    }

    // -------------------------------------------------------------------------------------------
    // 7️⃣ Validación: determina si el “request” encaja con los detalles del pago pendiente anterior
    // -------------------------------------------------------------------------------------------
    private boolean esPagoHistoricoAplicable(Pago ultimoPendiente, PagoRegistroRequest request) {
        if (ultimoPendiente == null || ultimoPendiente.getDetallePagos() == null) return false;

        Set<String> clavesHistoricas = ultimoPendiente.getDetallePagos().stream()
                .map(det -> (det.getConcepto() != null ? det.getConcepto().getId() : "null")
                        + "_" + (det.getSubConcepto() != null ? det.getSubConcepto().getId() : "null"))
                .collect(Collectors.toSet());

        Set<String> clavesRequest = request.detallePagos().stream()
                .map(dto -> dto.conceptoId() + "_" + dto.subConceptoId())
                .collect(Collectors.toSet());

        boolean aplicable = clavesHistoricas.containsAll(clavesRequest);
        log.info("[esPagoHistoricoAplicable] clavesHistoricas={}, clavesRequest={}, aplicable={}",
                clavesHistoricas, clavesRequest, aplicable);
        return aplicable;
    }

}

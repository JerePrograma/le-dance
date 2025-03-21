package ledance.servicios.pago;

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
import ledance.repositorios.DetallePagoRepositorio;
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
    private final DetallePagoRepositorio detallePagoRepositorio;

    @PersistenceContext
    private jakarta.persistence.EntityManager entityManager;

    // Repositorios y Servicios
    private final PagoRepositorio pagoRepositorio;

    // Mappers
    private final MatriculaMapper matriculaMapper;
    private final AlumnoMapper alumnoMapper;
    private final PagoMapper pagoMapper;

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
                            InscripcionMapper inscripcionMapper, DetallePagoRepositorio detallePagoRepositorio) {
        this.pagoRepositorio = pagoRepositorio;
        this.calculationServicio = calculationServicio;
        this.matriculaServicio = matriculaServicio;
        this.mensualidadServicio = mensualidadServicio;
        this.stockServicio = stockServicio;
        this.detallePagoServicio = detallePagoServicio;
        this.matriculaMapper = matriculaMapper;
        this.alumnoMapper = alumnoMapper;
        this.pagoMapper = pagoMapper;
        this.detallePagoRepositorio = detallePagoRepositorio;
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

        // 1. Merge del pago para obtener la entidad gestionada
        Pago pagoManaged = entityManager.merge(pago);
        log.info("[processFirstPayment] Pago mergeado. ID gestionado={}, Fecha={}", pagoManaged.getId(), pagoManaged.getFecha());

        List<DetallePago> detalles = pagoManaged.getDetallePagos();
        log.info("[processFirstPayment] Detalles asociados al pago (cantidad={}): {}",
                (detalles != null ? detalles.size() : 0), detalles);

        if (detalles == null || detalles.isEmpty()) {
            log.info("[processFirstPayment] No se encontraron detalles en el pago. Asignando montoPagado y saldoRestante a 0.");
            pagoManaged.setMontoPagado(0.0);
            pagoManaged.setSaldoRestante(0.0);
            return pagoManaged;
        }

        // 2. Para cada detalle, se obtiene y actualiza el registro persistido a través del helper
        List<DetallePago> nuevosDetalles = new ArrayList<>();
        for (DetallePago detalle : detalles) {
            DetallePago detallePersistido = obtenerYActualizarDetallePago(detalle, pagoManaged, pago.getAlumno());
            nuevosDetalles.add(detallePersistido);
        }

        // 3. Se actualiza la lista de detalles del pago gestionado
        pagoManaged.setDetallePagos(nuevosDetalles);

        // 4. Actualizar totales: monto, montoPagado y saldoRestante
        log.info("[processFirstPayment] Actualizando totales del pago.");
        actualizarImportesTotalesPago(pagoManaged);
        log.info("[processFirstPayment] Totales tras actualizarImportesTotalesPago: monto={}, montoPagado={}, saldoRestante={}",
                pagoManaged.getMonto(), pagoManaged.getMontoPagado(), pagoManaged.getSaldoRestante());

        // 5. Verificar saldo restante y validar que no sea null
        verificarSaldoRestante(pagoManaged);
        log.info("[processFirstPayment] Totales tras verificarSaldoRestante: saldoRestante={}", pagoManaged.getSaldoRestante());
        if (pagoManaged.getSaldoRestante() == null) {
            log.error("[processFirstPayment] El saldoRestante es null tras las actualizaciones.");
            throw new IllegalStateException("El saldoRestante no se ha calculado correctamente.");
        }

        // 6. Sincronizar cambios con la base de datos
        log.info("[processFirstPayment] Ejecutando entityManager.flush() para sincronización con la BD.");
        entityManager.flush();
        log.info("[processFirstPayment] Pago final persistido: id={}, monto={}, saldoRestante={}",
                pagoManaged.getId(), pagoManaged.getMonto(), pagoManaged.getSaldoRestante());

        return pagoManaged;
    }

    /**
     * Obtiene el registro de DetallePago persistido para el detalle recibido,
     * actualizando sus campos con la información proveniente del objeto 'detalle'
     * que forma parte del objeto 'pago' recibido en el proceso.
     */
    private DetallePago obtenerYActualizarDetallePago(DetallePago detalle, Pago pagoManaged, Alumno alumno) {
        // Verificar que el detalle tenga asignado un ID
        if (detalle.getId() == null) {
            log.error("[obtenerYActualizarDetallePago] El detalle no tiene ID asignado.");
            throw new IllegalStateException("Se esperaba que todos los detalles a transferir tuvieran ID asignado");
        }

        // Buscar el registro persistido a través del entityManager o mediante JPQL
        DetallePago detallePersistido = entityManager.find(DetallePago.class, detalle.getId());
        if (detallePersistido == null) {
            log.info("[obtenerYActualizarDetallePago] entityManager.find() devolvió null. Se intenta con JPQL para id={}", detalle.getId());
            detallePersistido = detallePagoRepositorio.buscarPorIdJPQL(detalle.getId());
        }
        if (detallePersistido == null) {
            log.error("[obtenerYActualizarDetallePago] No se encontró detalle con id={} en la base de datos.", detalle.getId());
            throw new IllegalStateException("El detalle con id=" + detalle.getId() + " no se encontró en la base de datos");
        }
        log.info("[obtenerYActualizarDetallePago] Detalle encontrado y gestionado: id={}, version={}",
                detallePersistido.getId(), detallePersistido.getVersion());

        // Actualización de campos comunes
        detallePersistido.setDescripcionConcepto(detalle.getDescripcionConcepto());
        detallePersistido.setCuotaOCantidad(detalle.getCuotaOCantidad());
        detallePersistido.setValorBase(detalle.getValorBase());
        detallePersistido.setBonificacion(detalle.getBonificacion());
        detallePersistido.setRecargo(detalle.getRecargo());

        // Reasociar el detalle al pago gestionado y al alumno
        detallePersistido.setPago(pagoManaged);
        detallePersistido.setAlumno(alumno);

        // Cálculo de importes: se verifica y calcula importeInicial e importePendiente si es necesario
        if (detallePersistido.getImporteInicial() == null) {
            log.info("[obtenerYActualizarDetallePago] Detalle id={} sin importeInicial. Se ejecuta calcularImporte().", detallePersistido.getId());
            calcularImporte(detallePersistido);
            log.info("[obtenerYActualizarDetallePago] Tras calcularImporte(), importeInicial={} en detalle id={}",
                    detallePersistido.getImporteInicial(), detallePersistido.getId());
        } else {
            log.info("[obtenerYActualizarDetallePago] Detalle id={} ya tiene importeInicial={}",
                    detallePersistido.getId(), detallePersistido.getImporteInicial());
        }
        if (detallePersistido.getImportePendiente() == null) {
            double aCobrar = Optional.ofNullable(detallePersistido.getaCobrar()).orElse(0.0);
            double nuevoPendiente = detallePersistido.getImporteInicial() - aCobrar;
            log.info("[obtenerYActualizarDetallePago] Calculando importePendiente para detalle id={}: importeInicial={} - aCobrar={} = nuevoPendiente={}",
                    detallePersistido.getId(), detallePersistido.getImporteInicial(), aCobrar, nuevoPendiente);
            detallePersistido.setImportePendiente(nuevoPendiente);
        } else {
            log.info("[obtenerYActualizarDetallePago] Detalle id={} ya tiene importePendiente={}",
                    detallePersistido.getId(), detallePersistido.getImportePendiente());
        }

        // Actualizar estados relacionados (por ejemplo, cobrado, tipo, etc.) según la fecha del pago
        actualizarEstadosRelacionados(detallePersistido, pagoManaged.getFecha());
        log.info("[obtenerYActualizarDetallePago] Estado de detalle id={} tras actualizarEstadosRelacionados: cobrado={}, tipo={}",
                detallePersistido.getId(), detallePersistido.getCobrado(), detallePersistido.getTipo());

        return detallePersistido;
    }

    // -------------------------------------------------------------------------------------------
    // 4️⃣ Actualización de Importes del Pago
    //     - Calcula cuánto se ha abonado (montoPagado)
    //     - Calcula el saldoRestante según los importesPendientes de cada detalle
    //     - Marca el pago como HISTÓRICO si no queda nada pendiente
    // -------------------------------------------------------------------------------------------
    public void actualizarImportesTotalesPago(Pago pago) {
        log.info("[actualizarImportesTotalesPago] Iniciando actualización de totales para pago ID={}", pago.getId());

        double totalAbonado = pago.getDetallePagos().stream()
                .mapToDouble(det -> Optional.ofNullable(det.getaCobrar()).orElse(0.0))
                .sum();
        double totalPendiente = pago.getDetallePagos().stream()
                .mapToDouble(det -> Optional.ofNullable(det.getImportePendiente()).orElse(0.0))
                .sum();

        log.info("[actualizarImportesTotalesPago] Totales calculados: totalAbonado={}, totalPendiente={}", totalAbonado, totalPendiente);

        pago.setMonto(totalAbonado);
        pago.setSaldoRestante(totalPendiente);
        pago.setMontoPagado(totalAbonado);

        if (totalPendiente == 0.0) {
            pago.setEstadoPago(EstadoPago.HISTORICO);
            log.info("[actualizarImportesTotalesPago] Pago ID={} marcado como HISTÓRICO (saldado).", pago.getId());
        } else {
            pago.setEstadoPago(EstadoPago.ACTIVO);
            log.info("[actualizarImportesTotalesPago] Pago ID={} permanece ACTIVO, saldoRestante={}.", pago.getId(), totalPendiente);
        }
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

    /**
     * Procesa un detalle de pago determinando si es matrícula, mensualidad, stock, etc.
     * Asigna la inscripción dentro del detalle (vía la mensualidad o la matrícula),
     * y nunca en el pago global.
     * <p>
     * Lógica resumida:
     * 1) Asegura que 'detalle.alumno' tenga al menos 'pago.alumno' si no está definido.
     * 2) Según concepto:
     * - STOCK => procesarStock
     * - MATRÍCULA => busca/crea la Matrícula y la asocia al detalle (detalle.setMatricula(...))
     * - MENSUALIDAD => busca/crea la Mensualidad y la asocia al detalle (detalle.setMensualidad(...))
     * - OTRO => se procesa genéricamente
     * 3) Si 'detalle.importePendiente <= 0', se marca 'detalle.cobrado = true'
     */
    public void procesarDetallePago(Pago pago, DetallePago detalle) {
        // 1) Asignar alumno si es nulo
        if (detalle.getAlumno() == null && pago.getAlumno() != null) {
            detalle.setAlumno(pago.getAlumno());
        }

        // Concepto en mayúsculas
        String conceptoDesc = (detalle.getDescripcionConcepto() != null ? detalle.getDescripcionConcepto().trim() : "")
                .toUpperCase();

        log.info("[procesarDetallePago] Procesando detalle id={}, concepto='{}'", detalle.getId(), conceptoDesc);

        if (conceptoDescCorrespondeAStock(conceptoDesc)) {
            // STOCK
            detalle.setTipo(TipoDetallePago.STOCK);
            calculationServicio.calcularStock(detalle);

        } else if (conceptoDescCorrespondeAMatricula(conceptoDesc)) {
            // MATRÍCULA
            detalle.setTipo(TipoDetallePago.MATRICULA);
            procesarMatriculaEnDetalle(detalle);

            // Realiza los cálculos propios de la matrícula
            calculationServicio.calcularMatricula(detalle, pago);

        } else if (conceptoDescCorrespondeAMensualidad(conceptoDesc)) {
            // MENSUALIDAD
            detalle.setTipo(TipoDetallePago.MENSUALIDAD);
            procesarMensualidadEnDetalle(detalle);

            // Se calculan importes, descuentos, recargos, etc.
            calculationServicio.calcularMensualidad(detalle, /*inscripcion??*/ null, pago);

        } else {
            // OTRO CONCEPTO
            detalle.setTipo(TipoDetallePago.CONCEPTO);
            calculationServicio.calcularConceptoGeneral(detalle);
        }

        // 3) Marcar detalle como cobrado si importePendiente <= 0
        if (detalle.getImportePendiente() != null && detalle.getImportePendiente() <= 0.0 && !detalle.getCobrado()) {
            detalle.setCobrado(true);
        }
        log.info("[procesarDetallePago] Finalizado procesamiento del detalle id={}, tipo={}",
                detalle.getId(), detalle.getTipo());
    }

    /**
     * Determina la matrícula asociada al 'detalle' (para un año, o la actual).
     * - Si existe y está pagada => excepción.
     * - Si existe y saldo>0 => abono parcial => la asignas en 'detalle.setMatricula(...)'.
     * - Si no existe => la creas y la asignas.
     */
    private void procesarMatriculaEnDetalle(DetallePago detalle) {
        Alumno alumno = detalle.getAlumno();
        if (alumno == null) {
            throw new IllegalArgumentException("Detalle sin alumno definido para Matrícula");
        }
        // Por ejemplo, parsear el año de la descripción
        // "MATRICULA 2025", etc.
        int anio = extraerAnioDeDescripcion(detalle.getDescripcionConcepto());

        MatriculaResponse matriculaResp = matriculaServicio.obtenerOMarcarPendiente(alumno.getId());
        if (matriculaResp.pagada()) {
            throw new IllegalArgumentException("La matrícula para el año " + anio + " ya está saldada.");
        }
        // Si no está pagada, actualizamos abonos, o la creamos, etc.
        Matricula nuevaOModificada = matriculaMapper.toEntity(matriculaResp);
        // Asignar la matrícula en el detalle
        detalle.setMatricula(nuevaOModificada);
        log.info("[procesarMatriculaEnDetalle] Asignada matrícula id={} al detalle id={}", nuevaOModificada.getId(), detalle.getId());
    }

    /**
     * Determina la mensualidad asociada al 'detalle' (ej., 'CUOTA MARZO').
     * - Si existe y está PAGADO => error.
     * - Si existe y saldo>0 => abono parcial => la asignas en 'detalle.setMensualidad(...)'.
     * - Si no existe => la creas => 'detalle.setMensualidad(...)'.
     */
    private void procesarMensualidadEnDetalle(DetallePago detalle) {
        Alumno alumno = detalle.getAlumno();
        if (alumno == null) {
            throw new IllegalArgumentException("Detalle sin alumno definido para Mensualidad");
        }
        // Por ejemplo, la descripción "CUOTA MARZO 2025" => parsear "MARZO 2025"
        String descNormalizado = detalle.getDescripcionConcepto().toUpperCase().trim();

        MensualidadResponse mensualidadResp = mensualidadServicio.obtenerOMarcarPendiente(alumno.getId(), descNormalizado);
        if (mensualidadResp.estado().equalsIgnoreCase("PAGADO")) {
            throw new IllegalArgumentException("La mensualidad para '" + descNormalizado + "' ya está pagada.");
        }
        // Sino, la convertimos a entidad
        Mensualidad mensualidadEntity = mensualidadServicio.toEntity(mensualidadResp);
        // Se asocia en el detalle
        detalle.setMensualidad(mensualidadEntity);
        log.info("[procesarMensualidadEnDetalle] Asignada mensualidad id={} al detalle id={}",
                mensualidadEntity.getId(), detalle.getId());
    }

    // ======= Metodos de verificación de concepto =======
    private boolean conceptoDescCorrespondeAStock(String desc) {
        return desc.contains("STOCK") || desc.contains("PRODUCTO");
    }

    private boolean conceptoDescCorrespondeAMatricula(String desc) {
        return desc.startsWith("MATRICULA");
    }

    private boolean conceptoDescCorrespondeAMensualidad(String desc) {
        return desc.contains("CUOTA") || desc.contains("MENSUALIDAD") || desc.contains("CLASE SUELTA");
    }

    // ======= Ejemplo de parse de año =======
    private int extraerAnioDeDescripcion(String desc) {
        // Ejemplo: si la descripción es "MATRICULA 2025"
        try {
            String[] partes = desc.split(" ");
            return Integer.parseInt(partes[1]);
        } catch (Exception e) {
            throw new IllegalArgumentException("No se pudo extraer el año de la matrícula en '" + desc + "'");
        }
    }

    @Transactional
    public Pago crearPagoSegunInscripcion(PagoRegistroRequest request) {
        // Mapeamos el alumno
        Alumno alumno = alumnoMapper.toEntity(request.alumno());
        log.info("[crearPagoSegunInscripcion] Alumno mapeado: id={}", alumno.getId());

        // Obtenemos el último pago pendiente del alumno
        Pago ultimoPendiente = obtenerUltimoPagoPendienteEntidad(alumno.getId());
        if (ultimoPendiente != null) {
            log.info("[crearPagoSegunInscripcion] Último pago pendiente encontrado: id={}, saldoRestante={}",
                    ultimoPendiente.getId(), ultimoPendiente.getSaldoRestante());
        } else {
            log.info("[crearPagoSegunInscripcion] No se encontró pago pendiente para el alumno id={}", alumno.getId());
        }

        // Validamos si el nuevo request encaja con una cobranza histórica
        boolean esAplicablePagoHistorico = ultimoPendiente != null
                && ultimoPendiente.getSaldoRestante() > 0
                && esPagoHistoricoAplicable(ultimoPendiente, request);
        log.info("[crearPagoSegunInscripcion] Pago histórico aplicable: {}", esAplicablePagoHistorico);

        if (esAplicablePagoHistorico) {
            log.info("[crearPagoSegunInscripcion] Pago histórico aplicable, actualizando el Pago ID={}",
                    ultimoPendiente.getId());
            return actualizarCobranzaHistorica(ultimoPendiente.getId(), request);
        } else {
            log.info("[crearPagoSegunInscripcion] Creando un nuevo Pago para el alumno ID={}", alumno.getId());
            Pago nuevoPago = pagoMapper.toEntity(request);
            nuevoPago.setAlumno(alumno);
            log.info("[crearPagoSegunInscripcion] Nuevo Pago mapeado: fecha={}, fechaVencimiento={}, monto={}, importeInicial={}",
                    nuevoPago.getFecha(), nuevoPago.getFechaVencimiento(), nuevoPago.getMonto(), nuevoPago.getImporteInicial());
            return processFirstPayment(nuevoPago);
        }
    }

    public Pago obtenerUltimoPagoPendienteEntidad(Long alumnoId) {
        Pago ultimo = pagoRepositorio.findTopByAlumnoIdAndEstadoPagoAndSaldoRestanteGreaterThanOrderByFechaDesc(
                alumnoId, EstadoPago.ACTIVO, 0.0).orElse(null);
        log.info("[obtenerUltimoPagoPendienteEntidad] Resultado para alumno id={}: {}", alumnoId, ultimo);
        return ultimo;
    }

    private Pago marcarDetallesConImportePendienteCero(Pago pago) {
        log.info("[marcarDetallesConImportePendienteCero] Procesando {} detalles en el pago id={}",
                pago.getDetallePagos().size(), pago.getId());
        pago.getDetallePagos().forEach(detalle -> {
            if (detalle.getImportePendiente() != null && detalle.getImportePendiente() == 0.0) {
                detalle.setCobrado(true);
                log.info("[marcarDetallesConImportePendienteCero] Detalle id={} marcado como cobrado (importePendiente=0)",
                        detalle.getId());
            }
        });
        return verificarSaldoRestante(pago);
    }

    private Pago verificarSaldoRestante(Pago pago) {
        if (pago.getSaldoRestante() < 0) {
            log.warn("[verificarSaldoRestante] Saldo negativo detectado en pago id={} (saldo: {}). Ajustando a 0.",
                    pago.getId(), pago.getSaldoRestante());
            pago.setSaldoRestante(0.0);
        } else {
            log.info("[verificarSaldoRestante] Saldo restante para el pago id={} es: {}",
                    pago.getId(), pago.getSaldoRestante());
        }
        return pago;
    }

    private boolean esPagoHistoricoAplicable(Pago ultimoPendiente, PagoRegistroRequest request) {
        if (ultimoPendiente == null || ultimoPendiente.getDetallePagos() == null) {
            log.info("[esPagoHistoricoAplicable] No se puede aplicar pago histórico, detallePagos es nulo.");
            return false;
        }
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

    @Transactional
    public Pago actualizarCobranzaHistorica(Long pagoId, PagoRegistroRequest request) {
        log.info("[actualizarCobranzaHistorica] Iniciando actualización para pago histórico id={}", pagoId);
        Pago historico = pagoRepositorio.findById(pagoId)
                .orElseThrow(() -> new IllegalArgumentException("Pago histórico no encontrado con ID=" + pagoId));
        log.info("[actualizarCobranzaHistorica] Pago histórico obtenido: id={}, saldoRestante={}",
                historico.getId(), historico.getSaldoRestante());

        // Mapeamos los abonos: clave "conceptoId_subConceptoId" y sumamos los aCobrar
        Map<String, Double> mapaAbonos = request.detallePagos().stream()
                .collect(Collectors.toMap(
                        dto -> dto.conceptoId() + "_" + dto.subConceptoId(),
                        DetallePagoRegistroRequest::aCobrar,
                        Double::sum
                ));
        log.info("[actualizarCobranzaHistorica] Mapa de abonos: {}", mapaAbonos);

        // Aplicamos abonos a cada detalle del pago histórico
        historico.getDetallePagos().forEach(detalle -> {
            String key = (detalle.getConcepto() != null ? detalle.getConcepto().getId() : "null")
                    + "_" + (detalle.getSubConcepto() != null ? detalle.getSubConcepto().getId() : "null");
            Double abono = mapaAbonos.getOrDefault(key, 0.0);
            log.info("[actualizarCobranzaHistorica] Para detalle id={} (clave={}), abono asignado={}",
                    detalle.getId(), key, abono);

            // Aplicamos el abono mediante el servicio de cálculo
            calculationServicio.aplicarAbono(detalle, abono);
            log.info("[actualizarCobranzaHistorica] Después de aplicar abono, detalle id={} tiene importePendiente={}, aCobrar={}, cobrado={}",
                    detalle.getId(), detalle.getImportePendiente(), detalle.getaCobrar(), detalle.getCobrado());

            // Actualizamos estados relacionados (mensualidad, matrícula, etc.)
            actualizarEstadosRelacionados(detalle, historico.getFecha());
        });

        // Marcamos el pago histórico como saldado
        historico.setEstadoPago(EstadoPago.HISTORICO);
        historico.setSaldoRestante(0.0);
        pagoRepositorio.save(historico);
        log.info("[actualizarCobranzaHistorica] Pago histórico id={} marcado como HISTORICO y guardado.", historico.getId());

        // Creamos un nuevo pago a partir del histórico
        Pago nuevoPago = crearNuevoPagoDesdeHistorico(historico, request);
        pagoRepositorio.save(nuevoPago);
        log.info("[actualizarCobranzaHistorica] Nuevo pago creado a partir del histórico: id={}, monto={}, saldoRestante={}",
                nuevoPago.getId(), nuevoPago.getMonto(), nuevoPago.getSaldoRestante());
        return nuevoPago;
    }

    public void aplicarAbono(DetallePago detalle, double montoAbono) {
        log.info("[aplicarAbono] Aplicando abono para detalle id={}. MontoAbono={}, importePendiente actual={}",
                detalle.getId(), montoAbono, detalle.getImportePendiente());
        double currentPendiente = detalle.getImportePendiente();
        double abono = Math.min(montoAbono, currentPendiente);
        detalle.setaCobrar(abono);
        detalle.setImportePendiente(Math.max(currentPendiente - abono, 0.0));
        log.info("[aplicarAbono] Después de abono: detalle id={} tiene aCobrar={}, importePendiente={}",
                detalle.getId(), detalle.getaCobrar(), detalle.getImportePendiente());

        if (detalle.getImportePendiente() <= 0.0) {
            detalle.setCobrado(true);
            detalle.setImportePendiente(0.0);
            log.info("[aplicarAbono] Detalle id={} completado (cobrado).", detalle.getId());
            // Actualizaciones específicas según el tipo
            if (detalle.getTipo() == TipoDetallePago.MENSUALIDAD && detalle.getMensualidad() != null) {
                mensualidadServicio.marcarComoPagada(detalle.getMensualidad().getId(), LocalDate.now());
                log.info("[aplicarAbono] Mensualidad id={} marcada como pagada.", detalle.getMensualidad().getId());
            }
            if (detalle.getTipo() == TipoDetallePago.MATRICULA && detalle.getMatricula() != null) {
                matriculaServicio.actualizarEstadoMatricula(
                        detalle.getMatricula().getId(),
                        new MatriculaRegistroRequest(detalle.getAlumno().getId(), detalle.getMatricula().getAnio(), true, LocalDate.now())
                );
                log.info("[aplicarAbono] Matrícula id={} actualizada a pagada.", detalle.getMatricula().getId());
            }
            if (detalle.getTipo() == TipoDetallePago.STOCK && detalle.getStock() != null) {
                stockServicio.reducirStock(detalle.getStock().getNombre(), 1);
                log.info("[aplicarAbono] Stock de '{}' reducido.", detalle.getStock().getNombre());
            }
        }
    }

    private void procesarAbonoEnDetalle(DetallePago detalle, double montoAbono, double importeInicialCalculado) {
        log.info("[procesarAbonoEnDetalle] Procesando abono para detalle id={}. MontoAbono={}, importeInicialCalculado={}",
                detalle.getId(), montoAbono, importeInicialCalculado);
        if (detalle.getImporteInicial() == null) {
            detalle.setImporteInicial(importeInicialCalculado);
            double abono = Math.min(montoAbono, importeInicialCalculado);
            detalle.setaCobrar(abono);
            detalle.setImportePendiente(Math.max(importeInicialCalculado - abono, 0.0));
            log.info("[procesarAbonoEnDetalle] Inicializado: importeInicial={}, aCobrar={}, importePendiente={}",
                    detalle.getImporteInicial(), detalle.getaCobrar(), detalle.getImportePendiente());
        } else {
            double currentPendiente = detalle.getImportePendiente() != null ? detalle.getImportePendiente() : 0.0;
            double abono = Math.min(montoAbono, currentPendiente);
            detalle.setaCobrar(abono);
            detalle.setImportePendiente(Math.max(currentPendiente - abono, 0.0));
            log.info("[procesarAbonoEnDetalle] Actualizado: currentPendiente={}, abono={}, nuevo importePendiente={}",
                    currentPendiente, abono, detalle.getImportePendiente());
        }
        if (detalle.getImportePendiente() <= 0.0) {
            detalle.setCobrado(true);
            detalle.setImportePendiente(0.0);
            log.info("[procesarAbonoEnDetalle] Detalle id={} completado y marcado como cobrado.", detalle.getId());
        }
    }

    public void calcularImporte(DetallePago detalle) {
        calcularImporte(detalle, null);
    }

    public void calcularImporte(DetallePago detalle, Inscripcion inscripcion) {
        log.info("[calcularImporte] Calculando importe para detalle id={}", detalle.getId());

        double base = detalle.getValorBase();
        log.info("[calcularImporte] Valor base: {}", base);

        double descuento = detallePagoServicio.calcularDescuento(detalle, base);
        log.info("[calcularImporte] Descuento calculado: {}", descuento);

        double recargo = (detalle.getRecargo() != null)
                ? detallePagoServicio.obtenerValorRecargo(detalle, base)
                : 0.0;
        log.info("[calcularImporte] Recargo calculado: {}", recargo);

        double importeInicialCalculado = base - descuento + recargo;
        log.info("[calcularImporte] Importe inicial calculado: {}", importeInicialCalculado);

        // Procesa el abono y asigna los importes
        procesarAbonoEnDetalle(detalle, detalle.getaCobrar(), importeInicialCalculado);

        // Asignar el tipo de detalle en base a la descripción
        TipoDetallePago tipoDeterminado = determinarTipoDetalle(detalle.getDescripcionConcepto());
        detalle.setTipo(tipoDeterminado);
        log.info("[calcularImporte] Tipo de detalle asignado: {} para la descripción '{}'.", tipoDeterminado, detalle.getDescripcionConcepto());

        if (detalle.getImportePendiente() <= 0.0) {
            detalle.setCobrado(true);
            log.info("[calcularImporte] Detalle id={} marcado como cobrado (importePendiente=0).", detalle.getId());
        }
    }

    private Pago crearNuevoPagoDesdeHistorico(Pago historico, PagoRegistroRequest request) {
        log.info("[crearNuevoPagoDesdeHistorico] Creando nuevo pago a partir del histórico id={}", historico.getId());
        Pago nuevoPago = new Pago();
        nuevoPago.setFecha(request.fecha());
        nuevoPago.setFechaVencimiento(request.fechaVencimiento());
        nuevoPago.setAlumno(historico.getAlumno());
        nuevoPago.setMetodoPago(historico.getMetodoPago());
        nuevoPago.setEstadoPago(EstadoPago.ACTIVO);
        nuevoPago.setObservaciones(historico.getObservaciones());
        log.info("[crearNuevoPagoDesdeHistorico] Datos básicos asignados: fecha={}, fechaVencimiento={}, alumno id={}",
                nuevoPago.getFecha(), nuevoPago.getFechaVencimiento(), nuevoPago.getAlumno().getId());

        // Clonamos sólo los detalles pendientes del histórico
        List<DetallePago> pendientes = historico.getDetallePagos().stream()
                .filter(det -> Optional.ofNullable(det.getImportePendiente()).orElse(0.0) > 0)
                .map(det -> {
                    DetallePago nuevoDet = det.clonarConPendiente(nuevoPago);
                    log.info("[crearNuevoPagoDesdeHistorico] Clonando detalle id={} para nuevo pago.", det.getId());
                    calcularImporte(nuevoDet);  // recalcula su importe
                    log.info("[crearNuevoPagoDesdeHistorico] Detalle clonado id={} recalculado: importeInicial={}, importePendiente={}",
                            nuevoDet.getId(), nuevoDet.getImporteInicial(), nuevoDet.getImportePendiente());
                    return nuevoDet;
                })
                .collect(Collectors.toList());

        nuevoPago.setDetallePagos(pendientes);
        actualizarImportesTotalesPago(nuevoPago);
        log.info("[crearNuevoPagoDesdeHistorico] Nuevo pago con detalles pendientes asignados. Totales actualizados: monto={}, saldoRestante={}",
                nuevoPago.getMonto(), nuevoPago.getSaldoRestante());
        return nuevoPago;
    }


    // Método para determinar el tipo de detalle basado en la descripción (solo lectura)
    public TipoDetallePago determinarTipoDetalle(String descripcionConcepto) {
        if (descripcionConcepto == null) {
            log.info("[determinarTipoDetalle] Descripción nula. Retornando TipoDetallePago.CONCEPTO.");
            return TipoDetallePago.CONCEPTO;
        }
        String conceptoNorm = descripcionConcepto.trim().toUpperCase();
        log.info("[determinarTipoDetalle] Concepto normalizado: {}", conceptoNorm);

        if (stockServicio.obtenerStockPorNombre(conceptoNorm)) {
            log.info("[determinarTipoDetalle] Stock encontrado para '{}'. Retornando TipoDetallePago.STOCK.", conceptoNorm);
            return TipoDetallePago.STOCK;
        } else if (conceptoNorm.startsWith("MATRICULA")) {
            log.info("[determinarTipoDetalle] Concepto comienza con 'MATRICULA'. Retornando TipoDetallePago.MATRICULA.");
            return TipoDetallePago.MATRICULA;
        } else if (esMensualidad(conceptoNorm)) {
            log.info("[determinarTipoDetalle] Concepto identificado como 'MENSUALIDAD'. Retornando TipoDetallePago.MENSUALIDAD.");
            return TipoDetallePago.MENSUALIDAD;
        } else {
            log.info("[determinarTipoDetalle] No se cumplió ninguna condición específica. Retornando TipoDetallePago.CONCEPTO.");
            return TipoDetallePago.CONCEPTO;
        }
    }

    private boolean esMensualidad(String conceptoNormalizado) {
        return conceptoNormalizado.contains("CUOTA") ||
                conceptoNormalizado.contains("CLASE SUELTA") ||
                conceptoNormalizado.contains("CLASE DE PRUEBA");
    }
}

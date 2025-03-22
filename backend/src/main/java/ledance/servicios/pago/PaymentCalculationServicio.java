package ledance.servicios.pago;

import jakarta.persistence.EntityNotFoundException;
import ledance.dto.concepto.ConceptoMapper;
import ledance.dto.matricula.MatriculaMapper;
import ledance.dto.matricula.request.MatriculaRegistroRequest;
import ledance.dto.matricula.response.MatriculaResponse;
import ledance.dto.mensualidad.response.MensualidadResponse;
import ledance.entidades.*;
import ledance.repositorios.ConceptoRepositorio;
import ledance.servicios.concepto.ConceptoServicio;
import ledance.servicios.detallepago.DetallePagoServicio;
import ledance.servicios.matricula.MatriculaServicio;
import ledance.servicios.mensualidad.MensualidadServicio;
import ledance.servicios.stock.StockServicio;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.Optional;

@Service
public class PaymentCalculationServicio {

    private static final Logger log = LoggerFactory.getLogger(PaymentCalculationServicio.class);

    private final MatriculaServicio matriculaServicio;
    private final MensualidadServicio mensualidadServicio;
    private final StockServicio stockServicio;
    private final DetallePagoServicio detallePagoServicio;
    private final MatriculaMapper matriculaMapper;
    private final ConceptoRepositorio conceptoRepositorio;

    public PaymentCalculationServicio(MatriculaServicio matriculaServicio,
                                      MensualidadServicio mensualidadServicio,
                                      StockServicio stockServicio,
                                      DetallePagoServicio detallePagoServicio,
                                      MatriculaMapper matriculaMapper, ConceptoServicio conceptoServicio, ConceptoMapper conceptoMapper, ConceptoRepositorio conceptoRepositorio) {
        this.matriculaServicio = matriculaServicio;
        this.mensualidadServicio = mensualidadServicio;
        this.stockServicio = stockServicio;
        this.detallePagoServicio = detallePagoServicio;
        this.matriculaMapper = matriculaMapper;
        this.conceptoRepositorio = conceptoRepositorio;
    }

    /**
     * Actualiza los estados relacionados de un detalle cuando se salda (importePendiente <= 0).
     * Invoca los servicios de mensualidad, matrícula o stock según corresponda.
     */
    private void actualizarEstadosRelacionados(DetallePago detalle, LocalDate fechaPago) {
        if (detalle.getImportePendiente() != null && detalle.getImportePendiente() <= 0.0 && !detalle.getCobrado()) {
            detalle.setCobrado(true);
            switch (detalle.getTipo()) {
                case MENSUALIDAD:
                    if (detalle.getMensualidad() != null) {
                        mensualidadServicio.marcarComoPagada(detalle.getMensualidad().getId(), fechaPago);
                    }
                    break;
                case MATRICULA:
                    if (detalle.getMatricula() != null) {
                        matriculaServicio.actualizarEstadoMatricula(
                                detalle.getMatricula().getId(),
                                new MatriculaRegistroRequest(detalle.getAlumno().getId(),
                                        detalle.getMatricula().getAnio(),
                                        true,
                                        fechaPago));
                    }
                    break;
                case STOCK:
                    if (detalle.getStock() != null) {
                        stockServicio.reducirStock(detalle.getStock().getNombre(), 1);
                    }
                    break;
                default:
                    // Otros conceptos no requieren acción adicional
                    break;
            }
        }
    }

    // ============================================================
    // MÉTODO AUXILIAR: CALCULAR IMPORTE INICIAL
    // ============================================================

    /**
     * Calcula el importeInicial según la fórmula:
     * importeInicial = valorBase – descuento + recargo.
     * Para el caso de matrícula, se asume que no se aplican descuentos ni recargos.
     *
     * @param detalle     El detalle de pago.
     * @param inscripcion (Opcional) La inscripción, para aplicar descuento en mensualidades.
     * @return El importeInicial calculado.
     */
    private double calcularImporteInicial(DetallePago detalle, Inscripcion inscripcion, boolean esMatricula) {
        log.info("[calcularImporteInicial] Iniciando cálculo para DetallePago id={}", detalle.getId());
        double base = detalle.getValorBase();
        if (esMatricula) {
            log.info("[calcularImporteInicial] Es matrícula, retornando base: {} para DetallePago id={}", base, detalle.getId());
            return base;
        }
        double descuento;
        if (inscripcion != null && inscripcion.getBonificacion() != null) {
            descuento = calcularDescuentoPorInscripcion(base, inscripcion);
            log.info("[calcularImporteInicial] Descuento por inscripción: {} para DetallePago id={}", descuento, detalle.getId());
        } else {
            descuento = detallePagoServicio.calcularDescuento(detalle, base);
            log.info("[calcularImporteInicial] Descuento calculado: {} para DetallePago id={}", descuento, detalle.getId());
        }
        double recargo = (detalle.getRecargo() != null)
                ? detallePagoServicio.obtenerValorRecargo(detalle, base)
                : 0.0;
        log.info("[calcularImporteInicial] Recargo: {} para DetallePago id={}", recargo, detalle.getId());
        double importeInicial = base - descuento + recargo;
        log.info("[calcularImporteInicial] Importe Inicial final: {} para DetallePago id={}", importeInicial, detalle.getId());
        return importeInicial;
    }

    // ============================================================
    // MÉTODO UNIFICADO: PROCESAR EL ABONO
    // ============================================================

    /**
     * Procesa el abono de un detalle, asegurándose de que:
     *  - El monto abonado no exceda el importe pendiente.
     *  - Se actualicen los campos aCobrar e importePendiente correctamente.
     */
    // 5. Procesar abono: asegura que el abono se aplique de forma única
    void procesarAbono(DetallePago detalle, Double montoAbono, Double importeInicialCalculado) {
        log.info("[procesarAbono] Iniciando procesamiento de abono para DetallePago id={}. MontoAbono recibido: {}, importeInicialCalculado: {}",
                detalle.getId(), montoAbono, importeInicialCalculado);
        if (montoAbono < 0) {
            log.error("[procesarAbono] Monto de abono negativo para DetallePago id={}", detalle.getId());
            throw new IllegalArgumentException("El monto del abono no puede ser negativo.");
        }
        // Asignar importeInicial solo si no está establecido
        if (detalle.getImporteInicial() == null && importeInicialCalculado != null) {
            detalle.setImporteInicial(importeInicialCalculado);
            log.info("[procesarAbono] Asignado importeInicial={} para DetallePago id={}", importeInicialCalculado, detalle.getId());
        } else {
            log.info("[procesarAbono] DetallePago id={} ya tiene importeInicial establecido: {}", detalle.getId(), detalle.getImporteInicial());
        }
        double importePendienteActual = (detalle.getImportePendiente() != null)
                ? detalle.getImportePendiente()
                : detalle.getImporteInicial();
        log.info("[procesarAbono] DetallePago id={} - importePendienteActual antes de abono: {}", detalle.getId(), importePendienteActual);

        double abonoAplicado = Math.min(montoAbono, importePendienteActual);
        detalle.setaCobrar(abonoAplicado);
        log.info("[procesarAbono] DetallePago id={} - abonoAplicado: {}", detalle.getId(), abonoAplicado);

        double nuevoPendiente = Math.max(importePendienteActual - abonoAplicado, 0.0);
        detalle.setImportePendiente(nuevoPendiente);
        log.info("[procesarAbono] DetallePago id={} - nuevo importePendiente: {}", detalle.getId(), nuevoPendiente);

        if (nuevoPendiente <= 0.0) {
            detalle.setCobrado(true);
            detalle.setImportePendiente(0.0);
            log.info("[procesarAbono] DetallePago id={} marcado como cobrado.", detalle.getId());
        } else {
            log.info("[procesarAbono] DetallePago id={} no se marca como cobrado. Importe pendiente: {}", detalle.getId(), nuevoPendiente);
        }
    }

    // ============================================================
    // MÉTODO CENTRAL UNIFICADO: PROCESAR Y CALCULAR DETALLE
    // ============================================================

    // 4. Procesar y calcular cada detalle (unifica cálculos y marca como cobrado si corresponde)
    public void procesarYCalcularDetalle(Pago pago, DetallePago detalle, Inscripcion inscripcion) {
        log.info("[procesarYCalcularDetalle] Iniciando para DetallePago id={}", detalle.getId());

        // Normalización y asignación de descripción
        String descripcion = Optional.ofNullable(detalle.getDescripcionConcepto())
                .orElse("")
                .trim()
                .toUpperCase();
        detalle.setDescripcionConcepto(descripcion);
        log.info("[procesarYCalcularDetalle] Detalle id={} - Descripción normalizada: '{}'", detalle.getId(), descripcion);

        // Determinación del tipo de detalle
        TipoDetallePago tipo = determinarTipoDetalle(descripcion);
        detalle.setTipo(tipo);
        log.info("[procesarYCalcularDetalle] Detalle id={} - Tipo determinado: {}", detalle.getId(), tipo);

        // Cálculo específico según el tipo
        log.info("[procesarYCalcularDetalle] Detalle id={} - Iniciando cálculo específico para tipo: {}", detalle.getId(), tipo);
        switch (tipo) {
            case MENSUALIDAD:
                calcularMensualidad(detalle, inscripcion, pago);
                break;
            case MATRICULA:
                calcularMatricula(detalle, pago);
                break;
            case STOCK:
                calcularStock(detalle);
                break;
            default:
                calcularConceptoGeneral(detalle);
                break;
        }
        log.info("[procesarYCalcularDetalle] Detalle id={} - Finalizado cálculo específico.", detalle.getId());

        // Procesar el abono usando el importeInicial ya calculado
        log.info("[procesarYCalcularDetalle] Detalle id={} - Iniciando proceso de abono. aCobrar actual: {}, importeInicial actual: {}",
                detalle.getId(), detalle.getaCobrar(), detalle.getImporteInicial());
        procesarAbono(detalle, detalle.getaCobrar(), detalle.getImporteInicial());

        // Verificar y marcar como cobrado si corresponde
        if (detalle.getImportePendiente() == null || detalle.getImportePendiente() <= 0.0) {
            detalle.setCobrado(true);
            detalle.setImportePendiente(0.0);
            log.info("[procesarYCalcularDetalle] Detalle id={} marcado como cobrado.", detalle.getId());
        } else {
            log.info("[procesarYCalcularDetalle] Detalle id={} - Importe pendiente tras abono: {}", detalle.getId(), detalle.getImportePendiente());
        }
    }

    // -----------------------------------------------------------------
    // MÉTODOS ESPECÍFICOS DE CÁLCULO DE DETALLES
    // -----------------------------------------------------------------

    /**
     * Calcula el detalle de tipo MATRÍCULA.
     * Se asume que el importe inicial es el valorBase (sin descuentos ni recargos).
     */
    public void calcularMatricula(DetallePago detalle, Pago pago) {
        log.info("[calcularMatricula] Iniciando procesamiento para DetallePago id={}", detalle.getId());

        // Calcular importe inicial para matrícula
        double importeInicialCalculado = calcularImporteInicial(detalle, null, true);
        log.info("[calcularMatricula] Detalle id={} - Importe Inicial calculado: {}", detalle.getId(), importeInicialCalculado);

        // Procesar el abono usando el importe calculado
        procesarAbono(detalle, detalle.getaCobrar(), importeInicialCalculado);
        log.info("[calcularMatricula] Detalle id={} - Abono procesado. aCobrar: {}, Importe Pendiente: {}",
                detalle.getId(), detalle.getaCobrar(), detalle.getImportePendiente());

        // Verificar y asignar Concepto y SubConcepto si alguno es nulo
        if (detalle.getConcepto() == null || detalle.getSubConcepto() == null) {
            log.info("[calcularMatricula] Detalle id={} - Concepto o SubConcepto no asignados. Se procederá a buscar por ID.", detalle.getId());
            if (detalle.getConcepto() != null && detalle.getConcepto().getId() != null) {
                Optional<Concepto> optionalConcepto = conceptoRepositorio.findById(detalle.getConcepto().getId());
                if (optionalConcepto.isPresent()) {
                    Concepto conceptoCompleto = optionalConcepto.get();
                    detalle.setConcepto(conceptoCompleto);
                    detalle.setSubConcepto(conceptoCompleto.getSubConcepto());
                    log.info("[calcularMatricula] Detalle id={} - Asignados Concepto: {} y SubConcepto: {}",
                            detalle.getId(), detalle.getConcepto(), detalle.getSubConcepto());
                } else {
                    log.warn("[calcularMatricula] Detalle id={} - No se encontró Concepto para el ID especificado.", detalle.getId());
                }
            } else {
                log.warn("[calcularMatricula] Detalle id={} - No se proporcionó un Concepto con ID.", detalle.getId());
            }
        } else {
            log.info("[calcularMatricula] Detalle id={} - Ya tienen asignados Concepto y SubConcepto.", detalle.getId());
        }

        // Procesar matrícula (actualización o creación según corresponda)
        procesarMatricula(pago, detalle);
        log.info("[calcularMatricula] Detalle id={} - Finalizado procesamiento de matrícula.", detalle.getId());
    }

    /**
     * Calcula el detalle de tipo STOCK.
     * No se aplican descuentos ni recargos.
     */
    void calcularStock(DetallePago detalle) {
        double importeInicialCalculado = calcularImporteInicial(detalle, null, false);
        procesarAbono(detalle, detalle.getaCobrar(), importeInicialCalculado);
        detalle.setCobrado(detalle.getImportePendiente() == 0.0);
        procesarStock(detalle);
    }

    /**
     * Calcula el detalle de tipo MENSUALIDAD.
     * Se aplican descuentos y recargos, utilizando la inscripción si está disponible.
     */
    void calcularMensualidad(DetallePago detalle, Inscripcion inscripcion, Pago pago) {
        double importeInicialCalculado = calcularImporteInicial(detalle, inscripcion, false);
        procesarAbono(detalle, detalle.getaCobrar(), importeInicialCalculado);
        detalle.setCobrado(detalle.getImportePendiente() == 0.0);
        procesarMensualidad(pago, detalle);
    }

    /**
     * Calcula el detalle para un concepto genérico.
     */
    void calcularConceptoGeneral(DetallePago detalle) {
        double importeInicialCalculado = calcularImporteInicial(detalle, null, false);
        procesarAbono(detalle, detalle.getaCobrar(), importeInicialCalculado);
        detalle.setCobrado(detalle.getImportePendiente() == 0.0);
    }

    // ============================================================
    // MÉTODOS DE PROCESAMIENTO ESPECÍFICO (ASOCIACIÓN Y ESTADOS)
    // ============================================================

    /**
     * Procesa la matrícula para un detalle.
     * Extrae el año de la descripción (ej.: "MATRICULA 2025") y asocia o actualiza la matrícula.
     */
    private void procesarMatricula(Pago pago, DetallePago detalle) {
        log.info("[procesarMatricula] Iniciando procesamiento de matrícula para DetallePago id={}", detalle.getId());

        // Se extrae el año de la descripción, suponiendo un formato como "Matricula 2025".
        String[] partes = detalle.getDescripcionConcepto().split(" ");
        if (partes.length < 2) {
            log.error("[procesarMatricula] No se pudo extraer el año de matrícula de la descripción: {} para DetallePago id={}", detalle.getDescripcionConcepto(), detalle.getId());
            throw new IllegalArgumentException("No se pudo extraer el año de matrícula de: " + detalle.getDescripcionConcepto());
        }
        int anio;
        try {
            anio = Integer.parseInt(partes[1]);
            log.info("[procesarMatricula] Año extraído: {} para DetallePago id={}", anio, detalle.getId());
        } catch (NumberFormatException e) {
            log.error("[procesarMatricula] Error al parsear el año en la descripción: {} para DetallePago id={}", detalle.getDescripcionConcepto(), detalle.getId());
            throw new IllegalArgumentException("Error al parsear el año en: " + detalle.getDescripcionConcepto());
        }

        Alumno alumno = pago.getAlumno();
        if (alumno == null) {
            log.error("[procesarMatricula] Alumno no definido en el pago para DetallePago id={}", detalle.getId());
            throw new EntityNotFoundException("Alumno no definido en el pago");
        }
        log.info("[procesarMatricula] Verificando existencia de matrícula para Alumno id={} y año={}", alumno.getId(), anio);

        // Permitir la actualización o clonación en caso de abono parcial: si existe matrícula pero aún no está saldada.
        if (matriculaServicio.existeMatriculaParaAnio(alumno.getId(), anio)) {
            log.info("[procesarMatricula] Matrícula ya existe para Alumno id={} y año={}", alumno.getId(), anio);
            MatriculaResponse matriculaResp = matriculaServicio.obtenerOMarcarPendiente(alumno.getId());
            if (matriculaResp.pagada()) {
                log.error("[procesarMatricula] La matrícula para el año {} ya está saldada para Alumno id={}", anio, alumno.getId());
                throw new IllegalArgumentException("La matrícula para el año " + anio + " ya está saldada.");
            }
            Matricula matriculaEntity = matriculaMapper.toEntity(matriculaResp);
            detalle.setMatricula(matriculaEntity);
            log.info("[procesarMatricula] Se asignó matrícula existente al DetallePago id={}", detalle.getId());
        } else {
            log.info("[procesarMatricula] No existe matrícula para Alumno id={} y año={}. Creando nueva matrícula.", alumno.getId(), anio);
            MatriculaResponse matriculaResp = matriculaServicio.obtenerOMarcarPendiente(alumno.getId());
            MatriculaRegistroRequest req = new MatriculaRegistroRequest(alumno.getId(), anio, true, pago.getFecha());
            matriculaServicio.actualizarEstadoMatricula(matriculaResp.id(), req);
            Matricula matriculaEntity = matriculaMapper.toEntity(matriculaResp);
            detalle.setMatricula(matriculaEntity);
            log.info("[procesarMatricula] Creada y asignada nueva matrícula al DetallePago id={}", detalle.getId());
        }
        log.info("[procesarMatricula] Finalizado procesamiento de matrícula para DetallePago id={}", detalle.getId());
    }

    /**
     * Procesa la mensualidad para un detalle.
     * Busca (o marca pendiente) la mensualidad para el período indicado en la descripción,
     * actualiza el abono parcial y asocia la mensualidad al detalle.
     */
    private void procesarMensualidad(Pago pago, DetallePago detalle) {
        log.info("[procesarMensualidad] Procesando mensualidad para detalle id={}", detalle.getId());
        Alumno alumno = pago.getAlumno();
        if (alumno == null) {
            throw new EntityNotFoundException("Alumno no definido para mensualidad");
        }
        MensualidadResponse mensualidadResp = obtenerMensualidadParaAlumno(alumno.getId(), detalle.getDescripcionConcepto());
        Mensualidad mensualidadEntity = mensualidadServicio.toEntity(mensualidadResp);
        // Actualizar el abono parcial en la mensualidad
        mensualidadServicio.actualizarAbonoParcial(mensualidadEntity.getId(), detalle.getaCobrar());
        MensualidadResponse mensualidadActualizada = mensualidadServicio.obtenerMensualidad(mensualidadEntity.getId());
        detalle.setMensualidad(mensualidadServicio.toEntity(mensualidadActualizada));
    }


    MensualidadResponse obtenerMensualidadParaAlumno(Long alumnoId, String descripcion) {
        String descNormalizado = descripcion.toUpperCase().trim();
        MensualidadResponse mensualidadResp = mensualidadServicio.obtenerOMarcarPendiente(alumnoId, descNormalizado);
        if (mensualidadResp.estado().equalsIgnoreCase("PAGADO") || mensualidadResp.importePendiente() <= 0.0) {
            throw new IllegalArgumentException("La mensualidad para '" + descNormalizado + "' ya está pagada.");
        }
        return mensualidadResp;
    }

    /**
     * Procesa el stock asociado al detalle, reduciendo la cantidad según el valor en cuotaOCantidad.
     */
    private void procesarStock(DetallePago detalle) {
        log.info("[procesarStock] Procesando detalle id={} para Stock", detalle.getId());
        int cantidad = parseCantidad(detalle.getCuotaOCantidad());
        stockServicio.reducirStock(detalle.getDescripcionConcepto(), cantidad);
    }

    /**
     * Parsea la cantidad a partir de cuotaOCantidad, usando 1 como valor predeterminado.
     */
    private int parseCantidad(String cuota) {
        try {
            return Integer.parseInt(cuota.trim());
        } catch (NumberFormatException e) {
            log.warn("Error al parsear cantidad desde '{}'. Usando valor predeterminado 1.", cuota);
            return 1;
        }
    }

    // ============================================================
    // MÉTODOS DE ACTUALIZACIÓN DE TOTALES Y ESTADOS DEL PAGO
    // ============================================================

    /**
     * Actualiza los totales del Pago en función de los detalles:
     * monto = suma de aCobrar; saldoRestante = suma de importePendiente.
     * Además, se actualiza el estado del pago (ACTIVO o HISTÓRICO).
     */
    public void actualizarImportesTotalesPago(Pago pago) {
        double totalAbonado = pago.getDetallePagos().stream()
                .mapToDouble(det -> Optional.ofNullable(det.getaCobrar()).orElse(0.0))
                .sum();
        double totalPendiente = pago.getDetallePagos().stream()
                .mapToDouble(det -> Optional.ofNullable(det.getImportePendiente()).orElse(0.0))
                .sum();

        pago.setMonto(totalAbonado);
        pago.setMontoPagado(totalAbonado);
        pago.setSaldoRestante(totalPendiente);

        if (totalPendiente <= 0.0) {
            pago.setEstadoPago(EstadoPago.HISTORICO);
            log.info("[actualizarImportesTotalesPago] Pago marcado como HISTORICO.");
        } else {
            pago.setEstadoPago(EstadoPago.ACTIVO);
            log.info("[actualizarImportesTotalesPago] Pago marcado como ACTIVO.");
        }
        log.info("[actualizarImportesTotalesPago] Totales actualizados: Monto abonado={}, Saldo restante={}",
                totalAbonado, totalPendiente);
    }

    /**
     * Determina el tipo de detalle basado en la descripción normalizada.
     */
    public TipoDetallePago determinarTipoDetalle(String descripcionConcepto) {
        if (descripcionConcepto == null) {
            return TipoDetallePago.CONCEPTO;
        }
        String conceptoNorm = descripcionConcepto.trim().toUpperCase();
        if (existeStockConNombre(conceptoNorm)) {
            return TipoDetallePago.STOCK;
        } else if (conceptoNorm.startsWith("MATRICULA")) {
            return TipoDetallePago.MATRICULA;
        } else if (conceptoNorm.contains("CUOTA") || conceptoNorm.contains("CLASE SUELTA") || conceptoNorm.contains("CLASE DE PRUEBA")) {
            return TipoDetallePago.MENSUALIDAD;
        } else {
            return TipoDetallePago.CONCEPTO;
        }
    }

    /**
     * Verifica si existe un stock asociado al nombre (según lógica del servicio de stock).
     */
    private boolean existeStockConNombre(String nombre) {
        return stockServicio.obtenerStockPorNombre(nombre);
    }

    /**
     * Calcula el descuento en función de la bonificación de la inscripción.
     */
    private double calcularDescuentoPorInscripcion(double base, Inscripcion inscripcion) {
        double descuentoFijo = Optional.ofNullable(inscripcion.getBonificacion().getValorFijo()).orElse(0.0);
        double descuentoPorcentaje = Optional.ofNullable(inscripcion.getBonificacion().getPorcentajeDescuento())
                .orElse(0) / 100.0 * base;
        return descuentoFijo + descuentoPorcentaje;
    }
}

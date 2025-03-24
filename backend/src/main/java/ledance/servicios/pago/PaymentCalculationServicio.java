package ledance.servicios.pago;

import jakarta.persistence.EntityNotFoundException;
import ledance.dto.concepto.ConceptoMapper;
import ledance.dto.matricula.MatriculaMapper;
import ledance.dto.matricula.request.MatriculaRegistroRequest;
import ledance.dto.mensualidad.MensualidadMapper;
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
    private final ConceptoRepositorio conceptoRepositorio;
    private final MensualidadMapper mensualidadMapper;

    public PaymentCalculationServicio(MatriculaServicio matriculaServicio,
                                      MensualidadServicio mensualidadServicio,
                                      StockServicio stockServicio,
                                      DetallePagoServicio detallePagoServicio,
                                      MatriculaMapper matriculaMapper, ConceptoServicio conceptoServicio, ConceptoMapper conceptoMapper, ConceptoRepositorio conceptoRepositorio, MensualidadMapper mensualidadMapper) {
        this.matriculaServicio = matriculaServicio;
        this.mensualidadServicio = mensualidadServicio;
        this.stockServicio = stockServicio;
        this.detallePagoServicio = detallePagoServicio;
        this.mensualidadMapper = mensualidadMapper;
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
        log.info("[calcularImporteInicial] Valor base obtenido: {} para DetallePago id={}", base, detalle.getId());

        if (esMatricula) {
            log.info("[calcularImporteInicial] Se detecta matrícula. Retornando base sin modificaciones: {} para DetallePago id={}", base, detalle.getId());
            return base;
        }

        double descuento = 0.0;
        if (inscripcion != null && inscripcion.getBonificacion() != null) {
            descuento = calcularDescuentoPorInscripcion(base, inscripcion);
            log.info("[calcularImporteInicial] Descuento por inscripción aplicado: {} para DetallePago id={}", descuento, detalle.getId());
        } else {
            descuento = detallePagoServicio.calcularDescuento(detalle, base);
            log.info("[calcularImporteInicial] Descuento calculado: {} para DetallePago id={}", descuento, detalle.getId());
        }

        double recargo = (detalle.getRecargo() != null)
                ? detallePagoServicio.obtenerValorRecargo(detalle, base)
                : 0.0;
        log.info("[calcularImporteInicial] Recargo obtenido: {} para DetallePago id={}", recargo, detalle.getId());

        double importeInicial = base - descuento + recargo;
        log.info("[calcularImporteInicial] Importe Inicial final calculado: {} para DetallePago id={}", importeInicial, detalle.getId());
        return importeInicial;
    }

    // ============================================================
    // MÉTODO UNIFICADO: PROCESAR EL ABONO
    // ============================================================

    /**
     * Procesa el abono de un detalle, asegurándose de que:
     * - El monto abonado no exceda el importe pendiente.
     * - Se actualicen los campos aCobrar e importePendiente correctamente.
     */
    // 5. Procesar abono: asegura que el abono se aplique de forma única
    void procesarAbono(DetallePago detalle, Double montoAbono, Double importeInicialCalculado) {
        log.info("[procesarAbono] Iniciando procesamiento de abono para DetallePago id={}. MontoAbono recibido: {}, ImporteInicialCalculado: {}",
                detalle.getId(), montoAbono, importeInicialCalculado);

        if (montoAbono < 0) {
            log.error("[procesarAbono] Monto de abono negativo detectado para DetallePago id={}", detalle.getId());
            throw new IllegalArgumentException("El monto del abono no puede ser negativo.");
        }

        // Si aún no se asignó el importeInicial, se asigna; de lo contrario, se mantiene el valor histórico.
        if (detalle.getImporteInicial() == null && importeInicialCalculado != null) {
            detalle.setImporteInicial(importeInicialCalculado);
            log.info("[procesarAbono] Se asigna importeInicial={} para DetallePago id={}", importeInicialCalculado, detalle.getId());
        } else {
            log.info("[procesarAbono] DetallePago id={} ya posee importeInicial establecido: {}", detalle.getId(), detalle.getImporteInicial());
        }

        double importePendienteActual = (detalle.getImportePendiente() != null)
                ? detalle.getImportePendiente()
                : detalle.getImporteInicial();
        log.info("[procesarAbono] DetallePago id={} - Importe pendiente actual antes de abono: {}", detalle.getId(), importePendienteActual);

        // Se aplica el abono sin exceder el importe pendiente
        double abonoAplicado = Math.min(montoAbono, importePendienteActual);
        detalle.setaCobrar(abonoAplicado);
        log.info("[procesarAbono] DetallePago id={} - Abono aplicado: {}", detalle.getId(), abonoAplicado);

        double nuevoPendiente = Math.max(importePendienteActual - abonoAplicado, 0.0);
        detalle.setImportePendiente(nuevoPendiente);
        log.info("[procesarAbono] DetallePago id={} - Nuevo importe pendiente calculado: {}", detalle.getId(), nuevoPendiente);

        if (nuevoPendiente <= 0.0) {
            detalle.setCobrado(true);
            detalle.setImportePendiente(0.0);
            log.info("[procesarAbono] DetallePago id={} marcado como cobrado, ya que el importe pendiente es 0.", detalle.getId());
        } else {
            log.info("[procesarAbono] DetallePago id={} permanece sin marcar como cobrado. Importe pendiente: {}", detalle.getId(), nuevoPendiente);
        }
    }

    // ============================================================
    // MÉTODO CENTRAL UNIFICADO: PROCESAR Y CALCULAR DETALLE
    // ============================================================

    // 4. Procesar y calcular cada detalle (unifica cálculos y marca como cobrado si corresponde)
    public void procesarYCalcularDetalle(Pago pago, DetallePago detalle, Inscripcion inscripcion) {
        log.info("[procesarYCalcularDetalle] Iniciando procesamiento para DetallePago id={}", detalle.getId());

        // Normalización y asignación de la descripción
        String descripcion = Optional.ofNullable(detalle.getDescripcionConcepto())
                .orElse("")
                .trim()
                .toUpperCase();
        detalle.setDescripcionConcepto(descripcion);
        log.info("[procesarYCalcularDetalle] Detalle id={} - Descripción normalizada: '{}'", detalle.getId(), descripcion);

        // Determinación del tipo de detalle basado en la descripción
        TipoDetallePago tipo = determinarTipoDetalle(descripcion);
        detalle.setTipo(tipo);
        log.info("[procesarYCalcularDetalle] Detalle id={} - Tipo determinado: {}", detalle.getId(), tipo);

        // Llamar a la lógica específica según el tipo de detalle
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
        log.info("[procesarYCalcularDetalle] Detalle id={} - Cálculo específico finalizado.", detalle.getId());

        // Procesar de forma centralizada el abono para todos los tipos
        log.info("[procesarYCalcularDetalle] Detalle id={} - Iniciando procesamiento de abono centralizado. aCobrar: {}, importeInicial: {}",
                detalle.getId(), detalle.getaCobrar(), detalle.getImporteInicial());
        procesarAbono(detalle, detalle.getaCobrar(), detalle.getImporteInicial());

        // Para el tipo MENSUALIDAD, actualizar la entidad Mensualidad usando el detalle actualizado
        if (tipo == TipoDetallePago.MENSUALIDAD) {
            Mensualidad mensualidad = mensualidadServicio.obtenerOMarcarPendienteMensualidad(
                    pago.getAlumno().getId(), detalle.getDescripcionConcepto());
            mensualidad = mensualidadServicio.procesarAbonoMensualidad(mensualidad, detalle);
        }

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

        // Calcular el importe inicial para matrícula (se usa el flag 'true' para indicar que es matrícula)
        double importeInicialCalculado = calcularImporteInicial(detalle, null, true);
        log.info("[calcularMatricula] Detalle id={} - Importe Inicial calculado: {}", detalle.getId(), importeInicialCalculado);

        // Actualizamos el importe inicial en el detalle; no se llama a procesarAbono aquí
        detalle.setImporteInicial(importeInicialCalculado);

        // Verificar y asignar Concepto y SubConcepto si son nulos
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
        // Calcular el importeInicial para el detalle (sin procesar el abono aún)
        double importeInicialCalculado = calcularImporteInicial(detalle, inscripcion, false);
        log.info("[calcularMensualidad] DetallePago id={} - Importe Inicial calculado: {}", detalle.getId(), importeInicialCalculado);

        // Asignar el importeInicial calculado al detalle
        detalle.setImporteInicial(importeInicialCalculado);

        // Se omite la llamada a procesarAbono aquí para evitar duplicidad.
        log.info("[calcularMensualidad] Proceso de cálculo para DetallePago id={} finalizado.", detalle.getId());
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

        // Extraer el año de la descripción (se asume formato "Matricula 2025")
        int anio = extraerAnioDeDescripcion(detalle.getDescripcionConcepto(), detalle.getId());

        Alumno alumno = pago.getAlumno();
        if (alumno == null) {
            log.error("[procesarMatricula] Alumno no definido en el pago para DetallePago id={}", detalle.getId());
            throw new EntityNotFoundException("Alumno no definido en el pago");
        }
        log.info("[procesarMatricula] Procesando matrícula para Alumno id={} y año={}", alumno.getId(), anio);

        // Obtener o crear la matrícula para el alumno en el año indicado
        Matricula matricula = matriculaServicio.obtenerOMarcarPendienteMatricula(alumno.getId(), anio);

        // Verificar si la matrícula ya está saldada
        if (matricula.getPagada()) {
            log.error("[procesarMatricula] La matrícula para el año {} ya está saldada para Alumno id={}", anio, alumno.getId());
            throw new IllegalArgumentException("La matrícula para el año " + anio + " ya está saldada.");
        }

        // Asignar la matrícula obtenida al detalle
        detalle.setMatricula((matricula));
        log.info("[procesarMatricula] Se asignó matrícula al DetallePago id={}", detalle.getId());

        log.info("[procesarMatricula] Finalizado procesamiento de matrícula para DetallePago id={}", detalle.getId());
    }

    // Método auxiliar para extraer el año de la descripción
    private int extraerAnioDeDescripcion(String descripcion, Long detalleId) {
        String[] partes = descripcion.split(" ");
        if (partes.length < 2) {
            log.error("[extraerAnioDeDescripcion] No se pudo extraer el año de la descripción: {} para DetallePago id={}", descripcion, detalleId);
            throw new IllegalArgumentException("No se pudo extraer el año de: " + descripcion);
        }
        try {
            int anio = Integer.parseInt(partes[1]);
            log.info("[extraerAnioDeDescripcion] Año extraído: {} para DetallePago id={}", anio, detalleId);
            return anio;
        } catch (NumberFormatException e) {
            log.error("[extraerAnioDeDescripcion] Error al parsear el año en la descripción: {} para DetallePago id={}", descripcion, detalleId);
            throw new IllegalArgumentException("Error al parsear el año en: " + descripcion);
        }
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
        mensualidadServicio.actualizarAbonoParcialMensualidad(mensualidadEntity, detalle.getaCobrar());
        MensualidadResponse mensualidadActualizada = mensualidadServicio.obtenerMensualidad(mensualidadEntity.getId());
        detalle.setMensualidad(mensualidadServicio.toEntity(mensualidadActualizada));
    }


    MensualidadResponse obtenerMensualidadParaAlumno(Long alumnoId, String descripcion) {
        String descNormalizado = descripcion.toUpperCase().trim();
        MensualidadResponse mensualidadResp = mensualidadMapper.toDTO(mensualidadServicio.obtenerOMarcarPendienteMensualidad(alumnoId, descNormalizado));
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

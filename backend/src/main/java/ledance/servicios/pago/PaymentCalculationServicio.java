package ledance.servicios.pago;

import jakarta.persistence.EntityNotFoundException;
import jakarta.transaction.Transactional;
import ledance.entidades.*;
import ledance.repositorios.ConceptoRepositorio;
import ledance.repositorios.DisciplinaRepositorio;
import ledance.servicios.detallepago.DetallePagoServicio;
import ledance.servicios.matricula.MatriculaServicio;
import ledance.servicios.mensualidad.MensualidadServicio;
import ledance.servicios.stock.StockServicio;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
public class PaymentCalculationServicio {

    private static final Logger log = LoggerFactory.getLogger(PaymentCalculationServicio.class);

    private final MatriculaServicio matriculaServicio;
    private final MensualidadServicio mensualidadServicio;
    private final StockServicio stockServicio;
    private final DetallePagoServicio detallePagoServicio;
    private final ConceptoRepositorio conceptoRepositorio;
    private final DisciplinaRepositorio disciplinaRepositorio;

    public PaymentCalculationServicio(MatriculaServicio matriculaServicio,
                                      MensualidadServicio mensualidadServicio,
                                      StockServicio stockServicio,
                                      DetallePagoServicio detallePagoServicio,
                                      ConceptoRepositorio conceptoRepositorio, DisciplinaRepositorio disciplinaRepositorio) {
        this.matriculaServicio = matriculaServicio;
        this.mensualidadServicio = mensualidadServicio;
        this.stockServicio = stockServicio;
        this.detallePagoServicio = detallePagoServicio;
        this.conceptoRepositorio = conceptoRepositorio;
        this.disciplinaRepositorio = disciplinaRepositorio;
    }

    // ============================================================
    // METODO AUXILIAR: CALCULAR IMPORTE INICIAL
    // ============================================================

    /**
     * Calcula el importeInicial segun la formula:
     * importeInicial = valorBase – descuento + recargo.
     * Para el caso de matricula, se asume que no se aplican descuentos ni recargos.
     *
     * @param detalle     El detalle de pago.
     * @param inscripcion (Opcional) La inscripcion, para aplicar descuento en mensualidades.
     * @return El importeInicial calculado.
     */
    public double calcularImporteInicial(DetallePago detalle, Inscripcion inscripcion, boolean esMatricula) {
        log.info("[calcularImporteInicial] Iniciando cálculo para DetallePago id={}", detalle.getId());
        double base = detalle.getValorBase();
        log.info("[calcularImporteInicial] Valor base obtenido: {} para DetallePago id={}", base, detalle.getId());

        if (!detalle.getTieneRecargo() || detalle.getTieneRecargo() == null) {
            detalle.setRecargo(null);
            log.info("[calcularImporteInicial] Se omite recargo para Detalle id={} (tieneRecargo=false o nulo)", detalle.getId());
        }
        if (esMatricula) {
            log.info("[calcularImporteInicial] Se detecta matrícula. Retornando base sin modificaciones: {} para DetallePago id={}", base, detalle.getId());
            return base;
        }
        double descuento;
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
    // METODO UNIFICADO: PROCESAR EL ABONO
    // ============================================================

    /**
     * Procesa el abono de un detalle, asegurandose de que:
     * - El monto abonado no exceda el importe pendiente.
     * - Se actualicen los campos aCobrar e importePendiente correctamente.
     */
    // Método corregido para procesar correctamente los abonos y actualizar estados
    void procesarAbono(DetallePago detalle, Double montoAbono, Double importeInicialCalculado) {
        log.info("[procesarAbono] Procesando abono para DetallePago id={}", detalle.getId());

        if (montoAbono == null || montoAbono < 0) {
            throw new IllegalArgumentException("Monto del abono inválido.");
        }
        // Si no existe importe inicial, asignarlo
        if (detalle.getImporteInicial() == null && importeInicialCalculado != null) {
            detalle.setImporteInicial(importeInicialCalculado);
        }
        Double importePendienteActual = null;

        if (detalle.getImportePendiente() == null) {
            importePendienteActual = importeInicialCalculado;
        } else {
            importePendienteActual = detalle.getImportePendiente();
        }
        if (!detalle.getTieneRecargo() || detalle.getTieneRecargo() == null) {
            detalle.setImportePendiente(detalle.getImporteInicial());
            detalle.setTieneRecargo(false);
        }

        if (montoAbono > importePendienteActual) {
            montoAbono = importePendienteActual;
        }

        detalle.setaCobrar(montoAbono);
        detalle.setImportePendiente(importePendienteActual - montoAbono);

        // Marcar como cobrado si no queda importe pendiente
        if (detalle.getImportePendiente() <= 0) {
            detalle.setImportePendiente(0.0);
            detalle.setCobrado(true);

            if (detalle.getTipo() == TipoDetallePago.MENSUALIDAD && detalle.getMensualidad() != null) {
                detalle.getMensualidad().setEstado(EstadoMensualidad.PAGADO);
            }

            if (detalle.getTipo() == TipoDetallePago.MATRICULA && detalle.getMatricula() != null) {
                detalle.getMatricula().setPagada(true);
            }
        } else {
            detalle.setCobrado(false);
        }

        log.info("[procesarAbono] Finalizado procesamiento para DetallePago id={}, pendiente restante: {}",
                detalle.getId(), detalle.getImportePendiente());
    }

    // ============================================================
    // METODO CENTRAL UNIFICADO: PROCESAR Y CALCULAR DETALLE
    // ============================================================

    // 4. Procesar y calcular cada detalle (unifica calculos y marca como cobrado si corresponde)
    public void procesarYCalcularDetalle(Pago pago, DetallePago detalle, Inscripcion inscripcion) {
        log.info("[procesarYCalcularDetalle] Iniciando procesamiento para DetallePago id={}", detalle.getId());
        detalle.setImportePendiente(detalle.getImporteInicial());
        log.info("[procesarYCalcularDetalle] Se asigna importePendiente={} al DetallePago id={}", detalle.getImporteInicial(), detalle.getId());

        // Normalizar descripción
        String descripcion = Optional.ofNullable(detalle.getDescripcionConcepto()).orElse("").trim().toUpperCase();
        detalle.setDescripcionConcepto(descripcion);
        log.info("[procesarYCalcularDetalle] Detalle id={} - Descripción normalizada: '{}'", detalle.getId(), descripcion);

        // Determinar tipo de detalle
        TipoDetallePago tipo = determinarTipoDetalle(descripcion);
        detalle.setTipo(tipo);
        log.info("[procesarYCalcularDetalle] Detalle id={} - Tipo determinado: {}", detalle.getId(), tipo);

        if (!detalle.getTieneRecargo() || detalle.getTieneRecargo() == null) {
            detalle.setRecargo(null);
            log.info("[procesarYCalcularDetalle] Se omite recargo para Detalle id={} (tieneRecargo=false o nulo)", detalle.getId());
        }

        // Lógica específica según el tipo
        switch (tipo) {
            case MENSUALIDAD:
                if (inscripcion != null) {
                    log.info("[procesarYCalcularDetalle] Inscripción encontrada para Detalle id={}", detalle.getId());
                    calcularMensualidad(detalle, inscripcion);
                    log.info("[procesarYCalcularDetalle] Método calcularMensualidad ejecutado para Detalle id={}", detalle.getId());
                } else {
                    if (descripcion.contains("CLASE DE PRUEBA")) {
                        log.info("[procesarYCalcularDetalle] 'CLASE DE PRUEBA' detectada para Detalle id={}", detalle.getId());
                        procesarClaseDePrueba(detalle, descripcion, pago);
                    } else {
                        log.warn("[procesarYCalcularDetalle] No se encontró inscripción para Detalle id={} y no es CLASE DE PRUEBA. Se procederá con cálculo genérico.", detalle.getId());
                        calcularMensualidad(detalle, inscripcion); // inscripcion es nulo, se debe manejar
                    }
                }
                break;
            case MATRICULA:
                log.info("[procesarYCalcularDetalle] Tipo MATRICULA detectado para Detalle id={}", detalle.getId());
                aplicarDescuentoCreditoEnMatricula(pago, detalle);
                log.info("[procesarYCalcularDetalle] Descuento de crédito aplicado para MATRICULA en Detalle id={}", detalle.getId());
                calcularMatricula(detalle, pago);
                log.info("[procesarYCalcularDetalle] Método calcularMatricula ejecutado para Detalle id={}", detalle.getId());
                break;
            case STOCK:
                log.info("[procesarYCalcularDetalle] Tipo STOCK detectado para Detalle id={}", detalle.getId());
                calcularStock(detalle);
                log.info("[procesarYCalcularDetalle] Método calcularStock ejecutado para Detalle id={}", detalle.getId());
                break;
            default:
                log.info("[procesarYCalcularDetalle] Tipo DEFAULT detectado para Detalle id={}", detalle.getId());
                if (descripcion.contains("CLASE SUELTA")) {
                    log.info("[procesarYCalcularDetalle] 'CLASE SUELTA' detectada para Detalle id={}", detalle.getId());
                    calcularConceptoGeneral(detalle);
                    Alumno alumno = pago.getAlumno();
                    double creditoActual = (alumno.getCreditoAcumulado() != null) ? alumno.getCreditoAcumulado() : 0.0;
                    log.info("[procesarYCalcularDetalle] Acumulando crédito por CLASE SUELTA: {} para Detalle id={}", detalle.getaCobrar(), detalle.getId());
                    alumno.setCreditoAcumulado(creditoActual + detalle.getaCobrar());
                    log.info("[procesarYCalcularDetalle] Nuevo crédito acumulado del alumno: {} (anterior: {})", (creditoActual + detalle.getaCobrar()), creditoActual);
                } else {
                    log.info("[procesarYCalcularDetalle] Ejecutando cálculo general para Detalle id={}", detalle.getId());
                    calcularConceptoGeneral(detalle);
                }
                break;
        }
        log.info("[procesarYCalcularDetalle] Cálculo específico finalizado para Detalle id={}", detalle.getId());

        if (tipo != TipoDetallePago.STOCK) {
            log.info("[procesarYCalcularDetalle] Iniciando procesamiento de abono centralizado para Detalle id={}. aCobrar: {}, Importe Inicial: {}",
                    detalle.getId(), detalle.getaCobrar(), detalle.getImporteInicial());
            procesarAbono(detalle, detalle.getaCobrar(), detalle.getImporteInicial());
            log.info("[procesarYCalcularDetalle] Procesamiento de abono centralizado finalizado para Detalle id={}", detalle.getId());
        } else {
            log.info("[procesarYCalcularDetalle] Tipo STOCK: se omite abono central para Detalle id={}", detalle.getId());
        }

        if (tipo == TipoDetallePago.MENSUALIDAD && descripcion.contains("CUOTA")) {
            log.info("[procesarYCalcularDetalle] Iniciando actualización de mensualidad para Detalle id={}", detalle.getId());
            Mensualidad mensualidad = mensualidadServicio.obtenerOMarcarPendienteMensualidad(
                    pago.getAlumno().getId(), detalle.getDescripcionConcepto());
            mensualidadServicio.procesarAbonoMensualidad(mensualidad, detalle);
            log.info("[procesarYCalcularDetalle] Mensualidad actualizada para Detalle id={}", detalle.getId());
        }

        if (detalle.getImportePendiente() <= 0.0) {
            detalle.setCobrado(true);
            detalle.setImportePendiente(0.0);
            log.info("[procesarYCalcularDetalle] Detalle id={} marcado como cobrado, ya que el importe pendiente es 0.", detalle.getId());
        } else {
            log.info("[procesarYCalcularDetalle] Detalle id={} - Importe pendiente tras abono: {}", detalle.getId(), detalle.getImportePendiente());
        }
        log.info("[procesarYCalcularDetalle] Procesamiento finalizado para DetallePago id={}", detalle.getId());
    }

    private void procesarClaseDePrueba(DetallePago detalle, String descripcion, Pago pago) {
        log.info("[procesarYCalcularDetalle] 'CLASE DE PRUEBA' detectada sin inscripción para Detalle id={}", detalle.getId());
        String nombreDisciplina = extraerNombreDisciplina(descripcion);
        Disciplina disciplina = disciplinaRepositorio.findByNombreContainingIgnoreCase(nombreDisciplina);
        if (disciplina != null) {
            double importeInicialCalculado = disciplina.getClasePrueba();
            detalle.setImporteInicial(importeInicialCalculado);
            log.info("[procesarYCalcularDetalle] Se asigna importeInicial desde disciplina (CLASE DE PRUEBA): {} para Detalle id={}",
                    importeInicialCalculado, detalle.getId());
            // Acumular crédito en el alumno
            Alumno alumno = pago.getAlumno();
            double creditoActual = (alumno.getCreditoAcumulado() != null) ? alumno.getCreditoAcumulado() : 0.0;
            alumno.setCreditoAcumulado(creditoActual + importeInicialCalculado);
            log.info("[procesarYCalcularDetalle] Nuevo crédito acumulado del alumno: {} (anterior: {})",
                    (creditoActual + importeInicialCalculado), creditoActual);
        } else {
            log.error("[procesarYCalcularDetalle] No se encontró disciplina para nombre '{}'", nombreDisciplina);
        }
    }

    /**
     * Método auxiliar para extraer el nombre de la disciplina.
     * Se asume que la descripción tiene el formato "DISCIPLINA - TARIFA - PERIODO".
     */
    String extraerNombreDisciplina(String descripcion) {
        String[] partes = descripcion.split(" - ");
        String nombre = (partes.length > 0) ? partes[0].trim() : descripcion.trim();
        log.info("[extraerNombreDisciplina] Nombre extraído: '{}'", nombre);
        return nombre;
    }

    private void aplicarDescuentoCreditoEnMatricula(Pago pago, DetallePago detalle) {
        Alumno alumno = pago.getAlumno();
        double creditoAlumno = (alumno.getCreditoAcumulado() != null) ? alumno.getCreditoAcumulado() : 0.0;
        double montoMatricula = detalle.getaCobrar();

        // Se suma el crédito acumulado al monto que paga el estudiante
        double totalACobrar = montoMatricula + creditoAlumno;

        log.info("[procesarYCalcularDetalle] Sumando crédito acumulado de {} en matrícula. Monto request: {}. Total a cobrar: {}",
                creditoAlumno, montoMatricula, totalACobrar);

        detalle.setaCobrar(totalACobrar);

        // Si se consume el crédito, se resetea a 0; de lo contrario, se puede ajustar según la lógica de negocio.
        alumno.setCreditoAcumulado(0.0);
    }

    // -----------------------------------------------------------------
    // METODOS ESPECIFICOS DE CALCULO DE DETALLES
    // -----------------------------------------------------------------

    /**
     * Calcula el detalle de tipo MATRICULA.
     * Se asume que el importe inicial es el valorBase (sin descuentos ni recargos).
     */
    public void calcularMatricula(DetallePago detalle, Pago pago) {
        log.info("[calcularMatricula] Iniciando procesamiento para DetallePago id={}", detalle.getId());

        // Calcular el importe inicial para matricula (se usa el flag 'true' para indicar que es matricula)
        double importeInicialCalculado = calcularImporteInicial(detalle, null, true);
        log.info("[calcularMatricula] Detalle id={} - Importe Inicial calculado: {}", detalle.getId(), importeInicialCalculado);

        // Actualizamos el importe inicial en el detalle; no se llama a procesarAbono aqui
        detalle.setImporteInicial(importeInicialCalculado);

        // Verificar y asignar Concepto y SubConcepto si son nulos
        if (detalle.getConcepto() == null || detalle.getSubConcepto() == null) {
            log.info("[calcularMatricula] Detalle id={} - Concepto o SubConcepto no asignados. Se procedera a buscar por ID.", detalle.getId());
            if (detalle.getConcepto() != null && detalle.getConcepto().getId() != null) {
                Optional<Concepto> optionalConcepto = conceptoRepositorio.findById(detalle.getConcepto().getId());
                if (optionalConcepto.isPresent()) {
                    Concepto conceptoCompleto = optionalConcepto.get();
                    detalle.setConcepto(conceptoCompleto);
                    detalle.setSubConcepto(conceptoCompleto.getSubConcepto());
                    log.info("[calcularMatricula] Detalle id={} - Asignados Concepto: {} y SubConcepto: {}",
                            detalle.getId(), detalle.getConcepto(), detalle.getSubConcepto());
                } else {
                    log.warn("[calcularMatricula] Detalle id={} - No se encontro Concepto para el ID especificado.", detalle.getId());
                }
            } else {
                log.warn("[calcularMatricula] Detalle id={} - No se proporciono un Concepto con ID.", detalle.getId());
            }
        } else {
            log.info("[calcularMatricula] Detalle id={} - Ya tienen asignados Concepto y SubConcepto.", detalle.getId());
        }

        // Procesar matricula (actualizacion o creacion segun corresponda)
        procesarMatricula(pago, detalle);
        log.info("[calcularMatricula] Detalle id={} - Finalizado procesamiento de matricula.", detalle.getId());
    }

    @Transactional
    public void calcularStock(DetallePago detalle) {
        log.info("[calcularStock] Iniciando calculo para DetallePago id={} de tipo STOCK", detalle.getId());

        // Calcular el importe inicial basado en la logica especifica para STOCK
        double importeInicialCalculado = calcularImporteInicial(detalle, null, false);
        log.info("[calcularStock] Detalle id={} - Importe Inicial Calculado: {}", detalle.getId(), importeInicialCalculado);

        // Procesar abono para el detalle STOCK (unica llamada para este tipo)
        log.info("[calcularStock] Detalle id={} - Procesando abono para STOCK. aCobrar: {}, importeInicialCalculado: {}",
                detalle.getId(), detalle.getaCobrar(), importeInicialCalculado);
        procesarAbono(detalle, detalle.getaCobrar(), importeInicialCalculado);

        // Marcar como procesado (podrias setear una bandera en el detalle, por ejemplo, detalle.setAbonoProcesado(true))
        // Aqui usamos el hecho de que el detalle ya esta cobrado y su importe pendiente es 0.
        boolean estaCobrado = (detalle.getImportePendiente() != null && detalle.getImportePendiente() == 0.0);
        detalle.setCobrado(estaCobrado);
        log.info("[calcularStock] Detalle id={} - Estado luego de abono: Cobrado={}, Importe pendiente: {}",
                detalle.getId(), estaCobrado, detalle.getImportePendiente());

        // Procesar reduccion de stock
        procesarStockInterno(detalle);
    }

    private void procesarStockInterno(DetallePago detalle) {
        log.info("[procesarStockInterno] Procesando reduccion de stock para DetallePago id={}", detalle.getId());
        int cantidad = parseCantidad(detalle.getCuotaOCantidad());
        log.info("[procesarStockInterno] Detalle id={} - Cantidad a reducir del stock: {}", detalle.getId(), cantidad);
        stockServicio.reducirStock(detalle.getDescripcionConcepto(), cantidad);
    }

    /**
     * Calcula el detalle de tipo MENSUALIDAD.
     * Se aplican descuentos y recargos, utilizando la inscripcion si esta disponible.
     */
    void calcularMensualidad(DetallePago detalle, Inscripcion inscripcion) {
        log.info("[calcularMensualidad] Iniciando cálculo de mensualidad para DetallePago id={}", detalle.getId());
        if (!detalle.getTieneRecargo() || detalle.getTieneRecargo() == null) {
            detalle.setRecargo(null);
            log.info("[calcularMensualidad] Se omite recargo para Detalle id={} (tieneRecargo=false o nulo)", detalle.getId());
        }
        double importeInicialCalculado = calcularImporteInicial(detalle, inscripcion, false);
        log.info("[calcularMensualidad] DetallePago id={} - Importe Inicial calculado: {}", detalle.getId(), importeInicialCalculado);
        detalle.setImporteInicial(importeInicialCalculado);
        log.info("[calcularMensualidad] Proceso de cálculo finalizado para DetallePago id={}", detalle.getId());
    }

    /**
     * Calcula el detalle para un concepto generico.
     */
    void calcularConceptoGeneral(DetallePago detalle) {
        double importeInicialCalculado = calcularImporteInicial(detalle, null, false);
        procesarAbono(detalle, detalle.getaCobrar(), importeInicialCalculado);
        detalle.setCobrado(detalle.getImportePendiente() == 0.0);
    }

    // ============================================================
    // METODOS DE PROCESAMIENTO ESPECIFICO (ASOCIACION Y ESTADOS)
    // ============================================================

    /**
     * Procesa la matricula para un detalle.
     * Extrae el año de la descripcion (ej.: "MATRICULA 2025") y asocia o actualiza la matricula.
     */
    private void procesarMatricula(Pago pago, DetallePago detalle) {
        log.info("[procesarMatricula] Iniciando procesamiento de matricula para DetallePago id={}", detalle.getId());

        // Extraer el año de la descripcion (se asume formato "Matricula 2025")
        int anio = extraerAnioDeDescripcion(detalle.getDescripcionConcepto(), detalle.getId());

        Alumno alumno = pago.getAlumno();
        if (alumno == null) {
            log.error("[procesarMatricula] Alumno no definido en el pago para DetallePago id={}", detalle.getId());
            throw new EntityNotFoundException("Alumno no definido en el pago");
        }
        log.info("[procesarMatricula] Procesando matricula para Alumno id={} y año={}", alumno.getId(), anio);

        // Obtener o crear la matricula para el alumno en el año indicado
        Matricula matricula = matriculaServicio.obtenerOMarcarPendienteMatricula(alumno.getId(), anio);

        // Verificar si la matricula ya esta saldada
        if (matricula.getPagada()) {
            log.error("[procesarMatricula] La matricula para el año {} ya esta saldada para Alumno id={}", anio, alumno.getId());
            throw new IllegalArgumentException("La matricula para el año " + anio + " ya esta saldada.");
        }

        // Asignar la matricula obtenida al detalle
        detalle.setMatricula((matricula));
        log.info("[procesarMatricula] Se asigno matricula al DetallePago id={}", detalle.getId());

        log.info("[procesarMatricula] Finalizado procesamiento de matricula para DetallePago id={}", detalle.getId());
    }

    // Metodo auxiliar para extraer el año de la descripcion
    private int extraerAnioDeDescripcion(String descripcion, Long detalleId) {
        String[] partes = descripcion.split(" ");
        if (partes.length < 2) {
            log.error("[extraerAnioDeDescripcion] No se pudo extraer el año de la descripcion: {} para DetallePago id={}", descripcion, detalleId);
            throw new IllegalArgumentException("No se pudo extraer el año de: " + descripcion);
        }
        try {
            int anio = Integer.parseInt(partes[1]);
            log.info("[extraerAnioDeDescripcion] Año extraido: {} para DetallePago id={}", anio, detalleId);
            return anio;
        } catch (NumberFormatException e) {
            log.error("[extraerAnioDeDescripcion] Error al parsear el año en la descripcion: {} para DetallePago id={}", descripcion, detalleId);
            throw new IllegalArgumentException("Error al parsear el año en: " + descripcion);
        }
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
    // METODOS DE ACTUALIZACION DE TOTALES Y ESTADOS DEL PAGO
    // ============================================================

    /**
     * Determina el tipo de detalle basado en la descripcion normalizada.
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
     * Verifica si existe un stock asociado al nombre (segun logica del servicio de stock).
     */
    private boolean existeStockConNombre(String nombre) {
        return stockServicio.obtenerStockPorNombre(nombre);
    }

    /**
     * Calcula el descuento en funcion de la bonificacion de la inscripcion.
     */
    private double calcularDescuentoPorInscripcion(double base, Inscripcion inscripcion) {
        log.info("[calcularDescuentoPorInscripcion] Iniciando cálculo para Inscripción id={}", inscripcion.getId());
        double descuentoFijo = Optional.ofNullable(inscripcion.getBonificacion().getValorFijo()).orElse(0.0);
        log.info("[calcularDescuentoPorInscripcion] Descuento fijo obtenido: {} para Inscripción id={}", descuentoFijo, inscripcion.getId());
        double descuentoPorcentaje = Optional.ofNullable(inscripcion.getBonificacion().getPorcentajeDescuento())
                .orElse(0) / 100.0 * base;
        log.info("[calcularDescuentoPorInscripcion] Descuento porcentaje calculado: {} para base {} en Inscripción id={}",
                descuentoPorcentaje, base, inscripcion.getId());
        double totalDescuento = descuentoFijo + descuentoPorcentaje;
        log.info("[calcularDescuentoPorInscripcion] Total descuento para Inscripción id={} es: {}", inscripcion.getId(), totalDescuento);
        return totalDescuento;
    }
}

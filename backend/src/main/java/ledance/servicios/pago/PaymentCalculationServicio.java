package ledance.servicios.pago;

import jakarta.persistence.EntityNotFoundException;
import jakarta.persistence.PersistenceContext;
import jakarta.transaction.Transactional;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import ledance.entidades.*;
import ledance.repositorios.ConceptoRepositorio;
import ledance.repositorios.DetallePagoRepositorio;
import ledance.repositorios.DisciplinaRepositorio;
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
 * Refactor del servicio PaymentCalculationServicio.
 * <p>
 * Se unifican las operaciones clave:
 * - Cálculo del importe inicial, validación y aplicación de descuentos/recargos.
 * - Procesamiento del abono, actualizando el estado y el importe pendiente.
 * - Procesamiento del detalle según su tipo (MENSUALIDAD, MATRICULA, STOCK o CONCEPTO).
 * - Reatach de asociaciones para garantizar que las entidades estén en estado managed.
 */
@Service
public class PaymentCalculationServicio {

    @PersistenceContext
    private jakarta.persistence.EntityManager entityManager;

    private static final Logger log = LoggerFactory.getLogger(PaymentCalculationServicio.class);

    private final MatriculaServicio matriculaServicio;
    private final MensualidadServicio mensualidadServicio;
    private final StockServicio stockServicio;
    private final DetallePagoServicio detallePagoServicio;
    private final ConceptoRepositorio conceptoRepositorio;
    private final DisciplinaRepositorio disciplinaRepositorio;
    private final DetallePagoRepositorio detallePagoRepositorio;

    public PaymentCalculationServicio(MatriculaServicio matriculaServicio,
                                      MensualidadServicio mensualidadServicio,
                                      StockServicio stockServicio,
                                      DetallePagoServicio detallePagoServicio,
                                      ConceptoRepositorio conceptoRepositorio,
                                      DisciplinaRepositorio disciplinaRepositorio,
                                      DetallePagoRepositorio detallePagoRepositorio) {
        this.matriculaServicio = matriculaServicio;
        this.mensualidadServicio = mensualidadServicio;
        this.stockServicio = stockServicio;
        this.detallePagoServicio = detallePagoServicio;
        this.conceptoRepositorio = conceptoRepositorio;
        this.disciplinaRepositorio = disciplinaRepositorio;
        this.detallePagoRepositorio = detallePagoRepositorio;
    }

    // ============================================================
    // METODO AUXILIAR: CALCULAR IMPORTE INICIAL
    // ============================================================

    /**
     * Calcula el importeInicial según la fórmula:
     * importeInicial = valorBase – descuento + recargo.
     * Para matrícula, se asume que no se aplican descuentos ni recargos.
     *
     * @param detalle     El DetallePago.
     * @param inscripcion (Opcional) La inscripción para aplicar descuentos en mensualidades.
     * @param esMatricula Si es true, se omiten descuentos/recargos.
     * @return Importe inicial calculado.
     */
    public double calcularImporteInicial(DetallePago detalle, Inscripcion inscripcion, boolean esMatricula) {
        log.info("[calcularImporteInicial] Iniciando cálculo para DetallePago id={}", detalle.getId());
        double base = detalle.getValorBase();
        log.info("[calcularImporteInicial] Valor base obtenido: {} para DetallePago id={}", base, detalle.getId());

        if (!detalle.getTieneRecargo()) {
            detalle.setTieneRecargo(false);
            log.info("[calcularImporteInicial] Se omite recargo para Detalle id={}", detalle.getId());
        }
        if (esMatricula) {
            log.info("[calcularImporteInicial] Matrícula detectada; retornando base sin modificaciones.");
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
        double recargo = detalle.getRecargo() != null ? detallePagoServicio.obtenerValorRecargo(detalle, base) : 0.0;
        log.info("[calcularImporteInicial] Recargo obtenido: {} para DetallePago id={}", recargo, detalle.getId());

        double importeInicial = base - descuento + recargo;
        log.info("[calcularImporteInicial] Importe Inicial final calculado: {} para DetallePago id={}", importeInicial, detalle.getId());
        return importeInicial;
    }

    /**
     * Procesa el abono de un detalle: valida el monto, actualiza aCobrar e importePendiente
     * y ajusta el estado del detalle (marcando como cobrado si corresponde).
     */
    void procesarAbono(DetallePago detalle, Double montoAbono, Double importeInicialCalculado) {
        log.info("[procesarAbono] INICIO - Procesando abono para DetallePago ID: {}", detalle.getId());
        if (montoAbono == null || montoAbono < 0) {
            log.error("[procesarAbono] Monto de abono inválido: {}", montoAbono);
            throw new IllegalArgumentException("Monto del abono inválido.");
        }
        if (detalle.getImporteInicial() == null) {
            detalle.setImporteInicial(importeInicialCalculado != null ? importeInicialCalculado : montoAbono);
        }
        Double importePendienteActual = (detalle.getImportePendiente() == null)
                ? detalle.getImporteInicial()
                : detalle.getImportePendiente();
        if (importePendienteActual == null) {
            throw new IllegalStateException("No se pudo determinar el importe pendiente.");
        }
        detalle.setACobrar(montoAbono);
        double nuevoPendiente = importePendienteActual - montoAbono;
        detalle.setImportePendiente(nuevoPendiente);
        if (nuevoPendiente <= 0) {
            detalle.setImportePendiente(0.0);
            detalle.setEstadoPago(EstadoPago.HISTORICO);
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
        log.info("[procesarAbono] FIN - Detalle id={} procesado. Nuevo pendiente: {}, Cobrado: {}",
                detalle.getId(), detalle.getImportePendiente(), detalle.getCobrado());
    }

    // ============================================================
    // METODO CENTRAL UNIFICADO: PROCESAR Y CALCULAR DETALLE
    // ============================================================

    /**
     * Unifica el procesamiento y cálculo de un DetallePago.
     * Se normaliza la descripción, se determina el tipo, se reatachan asociaciones
     * y se invoca la lógica específica según el tipo.
     */
    @Transactional
    public void procesarYCalcularDetalle(DetallePago detalle, Inscripcion inscripcion) {
        log.info("[procesarYCalcularDetalle] INICIO para DetallePago id={}", detalle.getId());
        // 1. Normalización de la descripción
        String descripcion = (detalle.getDescripcionConcepto() != null ? detalle.getDescripcionConcepto() : "").trim().toUpperCase();
        detalle.setDescripcionConcepto(descripcion);
        log.info("[procesarYCalcularDetalle] Descripción normalizada: '{}'", descripcion);
        // 2. Determinar tipo si no está definido
        if (detalle.getTipo() == null) {
            TipoDetallePago tipo = determinarTipoDetalle(descripcion);
            detalle.setTipo(tipo);
            log.info("[procesarYCalcularDetalle] Tipo determinado: {}", tipo);
        }
        // 3. Reatach de Concepto y SubConcepto (si corresponde)
        if (detalle.getConcepto() != null && detalle.getConcepto().getId() != null) {
            Concepto managedConcepto = entityManager.find(Concepto.class, detalle.getConcepto().getId());
            if (managedConcepto == null) {
                throw new EntityNotFoundException("Concepto con ID " + detalle.getConcepto().getId() + " no encontrado.");
            }
            detalle.setConcepto(managedConcepto);
            if (detalle.getSubConcepto() == null && managedConcepto.getSubConcepto() != null) {
                detalle.setSubConcepto(managedConcepto.getSubConcepto());
                log.info("[procesarYCalcularDetalle] SubConcepto asignado: {}", managedConcepto.getSubConcepto().getId());
            }
        }
        // 4. Validar flag de recargo
        if (!detalle.getTieneRecargo()) {
            detalle.setTieneRecargo(false);
            log.info("[procesarYCalcularDetalle] Recargo desactivado para Detalle id={}", detalle.getId());
        }
        // 5. Procesar según tipo de detalle
        switch (detalle.getTipo()) {
            case MENSUALIDAD:
                if (inscripcion != null) {
                    calcularMensualidad(detalle, inscripcion);
                } else if (descripcion.contains("CLASE DE PRUEBA")) {
                    procesarClaseDePrueba(detalle, descripcion, detalle.getPago());
                } else {
                    calcularMensualidad(detalle, null);
                    log.warn("[procesarYCalcularDetalle] Mensualidad sin inscripción ni clase de prueba.");
                }
                if (descripcion.contains("CUOTA")) {
                    Mensualidad mensualidad = mensualidadServicio.obtenerOMarcarPendienteMensualidad(detalle.getAlumno().getId(), descripcion);
                    mensualidadServicio.procesarAbonoMensualidad(mensualidad, detalle);
                }
                break;
            case MATRICULA:
                calcularMatricula(detalle, detalle.getPago());
                break;
            case STOCK:
                calcularStock(detalle);
                break;
            default:
                detalle.setTipo(TipoDetallePago.CONCEPTO);
                calcularConceptoGeneral(detalle);
                if (descripcion.contains("CLASE SUELTA")) {
                    double creditoActual = Optional.ofNullable(detalle.getAlumno().getCreditoAcumulado()).orElse(0.0);
                    double nuevoCredito = creditoActual + detalle.getACobrar();
                    detalle.getAlumno().setCreditoAcumulado(nuevoCredito);
                    log.info("[procesarYCalcularDetalle] Crédito actualizado para alumno id={}: {}", detalle.getAlumno().getId(), nuevoCredito);
                }
                break;
        }
        // 6. Validar consistencia final: importeInicial, aCobrar y estado de cobro
        Double impInicialFinal = Optional.ofNullable(detalle.getImporteInicial()).orElse(detalle.getValorBase());
        if (impInicialFinal == null || impInicialFinal <= 0) {
            impInicialFinal = Optional.ofNullable(detalle.getImportePendiente()).orElse(0.0);
            detalle.setImporteInicial(impInicialFinal);
            log.warn("[procesarYCalcularDetalle] Importe inicial inválido, asignado nuevo valor: {}", impInicialFinal);
        }
        double aCobrarFinal = Optional.ofNullable(detalle.getACobrar()).orElse(0.0);
        if (aCobrarFinal < 0) {
            aCobrarFinal = 0.0;
            log.warn("[procesarYCalcularDetalle] aCobrar inválido, asignado 0.0");
        }
        detalle.setACobrar(aCobrarFinal);
        Double impPendienteFinal = Optional.ofNullable(detalle.getImportePendiente()).orElse(0.0);
        boolean cobrado = impPendienteFinal <= 0.0;
        detalle.setCobrado(cobrado);
        if (cobrado) {
            detalle.setEstadoPago(EstadoPago.HISTORICO);
            detalle.setCobrado(true);
            detalle.setImportePendiente(0.0);
            log.info("[procesarYCalcularDetalle] Detalle marcado como cobrado.");
        }
        log.info("[procesarYCalcularDetalle] FIN - Detalle actualizado: {}", detalle);
        detallePagoRepositorio.save(detalle);
    }

    private void procesarClaseDePrueba(DetallePago detalle, String descripcion, Pago pago) {
        log.info("[procesarClaseDePrueba] Procesando clase de prueba para Detalle id={}", detalle.getId());
        String nombreDisciplina = extraerNombreDisciplina(descripcion);
        Disciplina disciplina = disciplinaRepositorio.findByNombreContainingIgnoreCase(nombreDisciplina);
        if (disciplina == null) {
            log.error("[procesarClaseDePrueba] No se encontró disciplina para nombre '{}'", nombreDisciplina);
            return;
        }
        double importeClasePrueba = disciplina.getClasePrueba();
        detalle.setImporteInicial(importeClasePrueba);
        double valorACobrar = detalle.getACobrar();
        double importePendienteOriginal = (detalle.getImportePendiente() != null && detalle.getImportePendiente() > 0)
                ? detalle.getImportePendiente() : importeClasePrueba;
        double nuevoImportePendiente = importePendienteOriginal - valorACobrar;
        if (nuevoImportePendiente <= 0) {
            nuevoImportePendiente = 0.0;
            detalle.setEstadoPago(EstadoPago.HISTORICO);
            detalle.setCobrado(true);
            detalle.setImportePendiente(0.0);
        } else {
            detalle.setImportePendiente(nuevoImportePendiente);
        }
        Alumno alumno = pago.getAlumno();
        if (alumno != null) {
            double creditoActual = Optional.ofNullable(alumno.getCreditoAcumulado()).orElse(0.0);
            double nuevoCredito = creditoActual + valorACobrar;
            alumno.setCreditoAcumulado(nuevoCredito);
        }
    }

    String extraerNombreDisciplina(String descripcion) {
        String[] partes = descripcion.split(" - ");
        if (partes.length > 0) {
            return partes[0].trim();
        } else {
            return descripcion.trim();
        }
    }

    private void aplicarDescuentoCreditoEnMatricula(Pago pago, DetallePago detalle) {
        Alumno alumno = pago.getAlumno();
        if (alumno == null) {
            throw new IllegalArgumentException("El alumno del pago es requerido");
        }
        double creditoAlumno = alumno.getCreditoAcumulado() != null ? alumno.getCreditoAcumulado() : 0.0;
        double nuevoValorBase = detalle.getImportePendiente() - creditoAlumno;
        detalle.setImporteInicial(nuevoValorBase);
        detalle.setImportePendiente(nuevoValorBase);
        alumno.setCreditoAcumulado(0.0);
    }

    @Transactional
    public void calcularMatricula(DetallePago detalle, Pago pago) {
        Double impInicial = detalle.getImporteInicial();
        if (impInicial == null || impInicial <= 0) {
            impInicial = calcularImporteInicial(detalle, null, true);
            detalle.setImporteInicial(impInicial);
        }
        if (detalle.getImportePendiente() == null || detalle.getImportePendiente() < 0) {
            detalle.setImportePendiente(detalle.getImporteInicial());
        }
        if (!detalle.getTieneRecargo() || detalle.getTipo() == TipoDetallePago.MENSUALIDAD) {
            detalle.setTieneRecargo(false);
        }
        Matricula matricula = procesarMatricula(pago, detalle);
        aplicarDescuentoCreditoEnMatricula(pago, detalle);
        detalle.setImporteInicial(detalle.getImportePendiente());
        if (detalle.getACobrar() != null && detalle.getACobrar() > 0) {
            double nuevoPendiente = detalle.getImportePendiente() - detalle.getACobrar();
            detalle.setImportePendiente(nuevoPendiente);
        }
        if (detalle.getImportePendiente() <= 0) {
            detalle.setCobrado(true);
            detalle.setImportePendiente(0.0);
            detalle.setEstadoPago(EstadoPago.HISTORICO);
            matricula.setPagada(true);
        } else {
            detalle.setCobrado(false);
        }
        detallePagoRepositorio.save(detalle);
    }

    void calcularConceptoGeneral(DetallePago detalle) {
        double importeInicialCalculado = calcularImporteInicial(detalle, null, false);
        procesarAbono(detalle, detalle.getACobrar(), importeInicialCalculado);
        detalle.setCobrado(detalle.getImportePendiente() == 0.0);
    }

    private Matricula procesarMatricula(Pago pago, DetallePago detalle) {
        String descripcion = detalle.getDescripcionConcepto();
        int anio = extraerAnioDeDescripcion(descripcion, detalle.getId());
        Alumno alumno = pago.getAlumno();
        if (alumno == null) {
            throw new EntityNotFoundException("Alumno no definido en el pago");
        }
        Matricula matricula = matriculaServicio.obtenerOMarcarPendienteMatricula(alumno.getId(), anio);
        detalle.setMatricula(matricula);
        return matricula;
    }

    private int extraerAnioDeDescripcion(String descripcion, Long detalleId) {
        String[] partes = descripcion.split(" ");
        if (partes.length < 2) {
            throw new IllegalArgumentException("No se pudo extraer el año de: " + descripcion);
        }
        try {
            return Integer.parseInt(partes[1]);
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Error al parsear el año en: " + descripcion);
        }
    }

    private int parseCantidad(String cuota) {
        try {
            return Integer.parseInt(cuota.trim());
        } catch (NumberFormatException e) {
            return 1;
        }
    }

    // ============================================================
    // METODOS DE ACTUALIZACION DE TOTALES Y ESTADOS DEL PAGO
    // ============================================================

    /**
     * Determina el tipo de detalle basado en la descripción normalizada.
     */
    @Transactional
    public TipoDetallePago determinarTipoDetalle(String descripcionConcepto) {
        if (descripcionConcepto == null) {
            return TipoDetallePago.CONCEPTO;
        }
        String conceptoNorm = descripcionConcepto.trim().toUpperCase();
        if (conceptoNorm.startsWith("MATRICULA")) {
            return TipoDetallePago.MATRICULA;
        }
        if (conceptoNorm.contains("CUOTA") ||
                conceptoNorm.contains("CLASE SUELTA") ||
                conceptoNorm.contains("CLASE DE PRUEBA")) {
            if (conceptoNorm.contains("CUOTA")) return TipoDetallePago.MENSUALIDAD;
            else return TipoDetallePago.MENSUALIDAD;
        }
        if (existeStockConNombre(conceptoNorm)) {
            return TipoDetallePago.STOCK;
        }
        return TipoDetallePago.CONCEPTO;
    }

    private boolean existeStockConNombre(String nombre) {
        return stockServicio.obtenerStockPorNombre(nombre);
    }

    private double calcularDescuentoPorInscripcion(double base, Inscripcion inscripcion) {
        double descuentoFijo = Optional.ofNullable(inscripcion.getBonificacion().getValorFijo()).orElse(0.0);
        double descuentoPorcentaje = Optional.ofNullable(inscripcion.getBonificacion().getPorcentajeDescuento())
                .orElse(0) / 100.0 * base;
        return descuentoFijo + descuentoPorcentaje;
    }

    @Transactional
    public void reatacharAsociaciones(DetallePago detalle, Pago pago) {
        if (detalle.getACobrar() == null) {
            detalle.setACobrar(0.0);
        }
        if (detalle.getAlumno() == null && pago.getAlumno() != null) {
            detalle.setAlumno(pago.getAlumno());
        }
        if (detalle.getConcepto() != null && detalle.getConcepto().getId() != null) {
            Concepto managedConcepto = entityManager.find(Concepto.class, detalle.getConcepto().getId());
            if (managedConcepto != null) {
                detalle.setConcepto(managedConcepto);
                if (detalle.getSubConcepto() == null && managedConcepto.getSubConcepto() != null) {
                    detalle.setSubConcepto(managedConcepto.getSubConcepto());
                }
            }
        }
        if (detalle.getDescripcionConcepto().contains("CUOTA") &&
                detalle.getMensualidad() != null &&
                detalle.getMensualidad().getId() != null) {
            Mensualidad managedMensualidad = entityManager.find(Mensualidad.class, detalle.getMensualidad().getId());
            if (managedMensualidad != null) {
                detalle.setMensualidad(managedMensualidad);
            }
        }
        if (detalle.getMatricula() != null && detalle.getMatricula().getId() != null) {
            Matricula managedMatricula = entityManager.find(Matricula.class, detalle.getMatricula().getId());
            if (managedMatricula != null) {
                detalle.setMatricula(managedMatricula);
            }
        }
        if (detalle.getStock() != null && detalle.getStock().getId() != null) {
            Stock managedStock = entityManager.find(Stock.class, detalle.getStock().getId());
            if (managedStock != null) {
                detalle.setStock(managedStock);
            }
        }
    }

    @Transactional
    public void calcularStock(DetallePago detalle) {
        log.info("[calcularStock] Iniciando calculo para DetallePago id={} de tipo STOCK", detalle.getId());

        // Calcular el importe inicial basado en la logica especifica para STOCK
        double importeInicialCalculado = calcularImporteInicial(detalle, null, false);
        log.info("[calcularStock] Detalle id={} - Importe Inicial Calculado: {}", detalle.getId(), importeInicialCalculado);

        // Procesar abono para el detalle STOCK (unica llamada para este tipo)
        log.info("[calcularStock] Detalle id={} - Procesando abono para STOCK. ACobrar: {}, importeInicialCalculado: {}",
                detalle.getId(), detalle.getACobrar(), importeInicialCalculado);
        procesarAbono(detalle, detalle.getACobrar(), importeInicialCalculado);

        // Marcar como procesado (podrias setear una bandera en el detalle, por ejemplo, detalle.setAbonoProcesado(true))
        // Aqui usamos el hecho de que el detalle ya esta cobrado y su importe pendiente es 0.
        boolean estaCobrado = (detalle.getImportePendiente() != null && detalle.getImportePendiente() == 0.0);
        detalle.setCobrado(estaCobrado);
        log.info("[calcularStock] Detalle id={} - Estado luego de abono: Cobrado={}, Importe pendiente: {}",
                detalle.getId(), estaCobrado, detalle.getImportePendiente());

        // Procesar reduccion de stock
        procesarStockInterno(detalle);
    }

    /**
     * Calcula el importe inicial de una mensualidad para un DetallePago, usando la inscripcion (si está disponible)
     * para aplicar descuentos y recargos.
     */
    @Transactional
    public void calcularMensualidad(DetallePago detalle, Inscripcion inscripcion) {
        log.info("[calcularMensualidad] INICIO cálculo para DetallePago id={}", detalle.getId());
        log.info("[calcularMensualidad] Iniciando cálculo DetallePago: {}", detalle);

        // 1. Establecer importeInicial: usar el valor enviado o calcular si es inválido
        Double impInicial = detalle.getImporteInicial();
        if (impInicial == null || impInicial <= 0) {
            impInicial = detalle.getImportePendiente();
            log.info("[calcularMensualidad] ImporteInicial calculado: {} para Detalle id={}", impInicial, detalle.getId());
        } else {
            log.info("[calcularMensualidad] Usando importeInicial proporcionado: {} para Detalle id={}", impInicial, detalle.getId());
        }

        // 2. Forzar la recalculación del importe pendiente según la intención del cliente:
        // Si tieneRecargo es false, se ignora cualquier recargo y se fuerza que
        // el importePendiente sea igual al importeInicial.
        if (!detalle.getTieneRecargo()) {
            detalle.setTieneRecargo(false);
            log.info("[calcularMensualidad] Sin recargo para Detalle id={}. ImportePendiente forzado a importeInicial: {}",
                    detalle.getId(), impInicial);
        } else {
            // Si tieneRecargo es true, se calcula el recargo y se suma al importeInicial
            double recargo = MensualidadServicio.validarRecargo(impInicial, detalle.getRecargo());
            double nuevoImportePendiente = impInicial + recargo;
            detalle.setImportePendiente(nuevoImportePendiente);
            log.info("[calcularMensualidad] Recargo aplicado: {}. Nuevo importePendiente: {} para Detalle id={}",
                    recargo, nuevoImportePendiente, detalle.getId());
        }

        // 3. Procesar abono: descontar ACobrar si corresponde
        if (detalle.getACobrar() != null && detalle.getACobrar() > 0) {
            double nuevoPendiente = detalle.getImportePendiente() - detalle.getACobrar();
            detalle.setImportePendiente(nuevoPendiente);
            log.info("[calcularMensualidad] Abono procesado: ACobrar={} -> nuevo importePendiente={}",
                    detalle.getACobrar(), nuevoPendiente);
        }

        // 4. Actualizar estado de cobro según el importe pendiente
        if (detalle.getImportePendiente() <= 0.0) {
            detalle.setEstadoPago(EstadoPago.HISTORICO);
            detalle.setCobrado(true);
            detalle.setImportePendiente(0.0);
            log.info("[calcularMensualidad] Detalle id={} marcado como cobrado", detalle.getId());
        } else {
            log.info("[calcularMensualidad] Detalle id={} NO cobrado (importe pendiente > 0)", detalle.getId());
        }
        log.info("[calcularMensualidad] Finalizando cálculo DetallePago: {}", detalle);

        // 5. Persistir cambios
        detallePagoRepositorio.save(detalle);
        log.info("[calcularMensualidad] FIN cálculo. Detalle id={} persistido con importeInicial={} e importePendiente={}",
                detalle.getId(), detalle.getImporteInicial(), detalle.getImportePendiente());
    }

    private void procesarStockInterno(DetallePago detalle) {
        log.info("[procesarStockInterno] Procesando reduccion de stock para DetallePago id={}", detalle.getId());
        int cantidad = parseCantidad(detalle.getCuotaOCantidad());
        log.info("[procesarStockInterno] Detalle id={} - Cantidad a reducir del stock: {}", detalle.getId(), cantidad);
        stockServicio.reducirStock(detalle.getDescripcionConcepto(), cantidad);
    }

}

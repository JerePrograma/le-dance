package ledance.servicios.pago;

import jakarta.persistence.EntityNotFoundException;
import jakarta.persistence.PersistenceContext;
import jakarta.transaction.Transactional;
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

import java.util.Optional;

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
                                      ConceptoRepositorio conceptoRepositorio, DisciplinaRepositorio disciplinaRepositorio, DetallePagoRepositorio detallePagoRepositorio) {
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

        if (!detalle.getTieneRecargo()) {
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

    /**
     * Procesa el abono de un detalle, asegurandose de que:
     * - El monto abonado no exceda el importe pendiente.
     * - Se actualicen los campos aCobrar e importePendiente correctamente.
     */
    // Método corregido para procesar correctamente los abonos y actualizar estados
    void procesarAbono(DetallePago detalle, Double montoAbono, Double importeInicialCalculado) {
        // 1. Inicio y validaciones iniciales
        log.info("[procesarAbono] INICIO - Procesando abono para DetallePago ID: {}", detalle.getId());
        log.debug("[procesarAbono] Parámetros recibidos - MontoAbono: {}, ImporteInicialCalculado: {}",
                montoAbono, importeInicialCalculado);
        log.debug("[procesarAbono] Estado inicial del detalle: {}", detalle.toString());

        // 2. Validación de monto de abono
        if (montoAbono == null || montoAbono < 0) {
            log.error("[procesarAbono] ERROR - Monto de abono inválido: {}", montoAbono);
            throw new IllegalArgumentException("Monto del abono inválido.");
        }
        log.info("[procesarAbono] Monto de abono validado: {}", montoAbono);

        // 3. Gestión de importe inicial
        log.info("[procesarAbono] Verificando importe inicial del detalle");
        if (detalle.getImporteInicial() == null) {
            log.info("[procesarAbono] Importe inicial no definido - Asignando valor");
            if (importeInicialCalculado != null) {
                log.debug("[procesarAbono] Usando importe inicial calculado: {}", importeInicialCalculado);
                detalle.setImporteInicial(importeInicialCalculado);
            } else {
                log.warn("[procesarAbono] Usando monto de abono como importe inicial (valor por defecto)");
                detalle.setImporteInicial(montoAbono);
            }
        }
        log.debug("[procesarAbono] Importe inicial final: {}", detalle.getImporteInicial());

        // 4. Cálculo de importe pendiente
        log.info("[procesarAbono] Calculando importe pendiente actual");
        Double importePendienteActual = (detalle.getImportePendiente() == null)
                ? detalle.getImporteInicial()
                : detalle.getImportePendiente();
        log.debug("[procesarAbono] Importe pendiente calculado: {}", importePendienteActual);

        if (importePendienteActual == null) {
            log.error("[procesarAbono] ERROR - No se puede determinar el importe pendiente");
            throw new IllegalStateException("No se puede determinar el importe pendiente.");
        }

        // 5. Ajuste de monto de abono
        log.info("[procesarAbono] Ajustando monto de abono al mínimo entre {} y {}", montoAbono, importePendienteActual);
        montoAbono = Math.min(montoAbono, importePendienteActual);
        log.debug("[procesarAbono] Monto de abono ajustado: {}", montoAbono);

        // 6. Actualización de valores
        log.info("[procesarAbono] Actualizando valores del detalle");
        detalle.setaCobrar(montoAbono);
        log.debug("[procesarAbono] aCobrar asignado: {}", detalle.getaCobrar());

        double nuevoPendiente = importePendienteActual - montoAbono;
        detalle.setImportePendiente(nuevoPendiente);
        log.debug("[procesarAbono] Nuevo importe pendiente: {}", nuevoPendiente);

        // 7. Gestión de estado de cobro
        if (nuevoPendiente <= 0) {
            log.info("[procesarAbono] Detalle completamente pagado - Marcando como cobrado");
            detalle.setImportePendiente(0.0);
            detalle.setCobrado(true);
            log.debug("[procesarAbono] Estado cobrado actualizado: {}", detalle.getCobrado());

            // 7.1 Actualización de entidades relacionadas
            if (detalle.getTipo() == TipoDetallePago.MENSUALIDAD && detalle.getMensualidad() != null) {
                log.info("[procesarAbono] Actualizando estado de mensualidad a PAGADO");
                detalle.getMensualidad().setEstado(EstadoMensualidad.PAGADO);
                log.debug("[procesarAbono] Estado mensualidad actualizado: {}",
                        detalle.getMensualidad().getEstado());
            }

            if (detalle.getTipo() == TipoDetallePago.MATRICULA && detalle.getMatricula() != null) {
                log.info("[procesarAbono] Marcando matrícula como pagada");
                detalle.getMatricula().setPagada(true);
                log.debug("[procesarAbono] Estado matrícula actualizado: {}",
                        detalle.getMatricula().getPagada());
            }
        } else {
            log.info("[procesarAbono] Detalle parcialmente pagado - Pendiente restante: {}", nuevoPendiente);
            detalle.setCobrado(false);
        }

        log.info("[procesarAbono] FIN - Procesamiento completado para DetallePago ID: {} | Pendiente final: {} | Cobrado: {}",
                detalle.getId(), detalle.getImportePendiente(), detalle.getCobrado());
        log.debug("[procesarAbono] Estado final del detalle: {}", detalle.toString());
    }

    // ============================================================
    // METODO CENTRAL UNIFICADO: PROCESAR Y CALCULAR DETALLE
    // ============================================================

    /**
     * Refactor de procesarYCalcularDetalle:
     * - Se normaliza la descripción y se determina el tipo.
     * - Según el tipo se invoca la lógica específica de cálculo.
     * - Finalmente, se procesa el abono central y se marca el detalle como cobrado si el importe pendiente es 0.
     */
    @Transactional
    public void procesarYCalcularDetalle(DetallePago detalle, Inscripcion inscripcion) {
        log.info("[procesarYCalcularDetalle] INICIO. Procesando DetallePago id={}", detalle.getId());

        // Normalizar descripción
        String descripcion = Optional.ofNullable(detalle.getDescripcionConcepto())
                .orElse("")
                .trim()
                .toUpperCase();
        log.info("[procesarYCalcularDetalle] Asignando descripción normalizada: {}", descripcion);
        detalle.setDescripcionConcepto(descripcion);

        // Determinar tipo si no está seteado
        if (detalle.getTipo() == null) {
            log.info("[procesarYCalcularDetalle] Tipo no seteado, determinando tipo...");
            TipoDetallePago tipo = determinarTipoDetalle(descripcion);
            log.info("[procesarYCalcularDetalle] Tipo determinado: {}", tipo);
            detalle.setTipo(tipo);
        }

        // Validar recargo
        if (!Boolean.TRUE.equals(detalle.getTieneRecargo())) {
            log.info("[procesarYCalcularDetalle] No tiene recargo, asignando recargo=null");
            detalle.setRecargo(null);
        }

        // Procesar según tipo de detalle
        switch (detalle.getTipo()) {
            case MENSUALIDAD:
                if (inscripcion != null) {
                    log.info("[procesarYCalcularDetalle] Procesando mensualidad con inscripción");
                    calcularMensualidad(detalle, inscripcion);
                } else if (descripcion.contains("CLASE DE PRUEBA")) {
                    log.info("[procesarYCalcularDetalle] Procesando clase de prueba");
                    procesarClaseDePrueba(detalle, descripcion, detalle.getPago());
                } else {
                    log.info("[procesarYCalcularDetalle] Procesando mensualidad sin inscripción");
                    calcularMensualidad(detalle, null);
                    log.warn("[procesarYCalcularDetalle] Mensualidad sin inscripción ni clase prueba.");
                }

                if (descripcion.contains("CUOTA")) {
                    log.info("[procesarYCalcularDetalle] Obteniendo o marcando mensualidad pendiente");
                    Mensualidad mensualidad = mensualidadServicio.obtenerOMarcarPendienteMensualidad(
                            detalle.getAlumno().getId(), descripcion);
                    log.info("[procesarYCalcularDetalle] Procesando abono de mensualidad");
                    mensualidadServicio.procesarAbonoMensualidad(mensualidad, detalle);
                }

                break;

            case MATRICULA:
                log.info("[procesarYCalcularDetalle] Procesando matrícula");
                aplicarDescuentoCreditoEnMatricula(detalle.getPago(), detalle);
                calcularMatricula(detalle, detalle.getPago());
                break;

            case STOCK:
                log.info("[procesarYCalcularDetalle] Procesando stock");
                calcularStock(detalle);
                break;
            default:
                // 1. Inicio de procesamiento para concepto general
                log.info("[procesarYCalcularDetalle] INICIO - Procesando CONCEPTO GENERAL | Detalle ID: {} | Descripción: '{}'",
                        detalle.getId(), detalle.getDescripcionConcepto());

                // 2. Asignación de tipo CONCEPTO
                log.debug("[procesarYCalcularDetalle] Asignando tipo CONCEPTO al detalle");
                detalle.setTipo(TipoDetallePago.CONCEPTO);
                log.info("[procesarYCalcularDetalle] Tipo asignado: {}", detalle.getTipo());

                // 3. Gestión del Concepto principal
                if (detalle.getConcepto() != null && detalle.getConcepto().getId() != null) {
                    log.info("[procesarYCalcularDetalle] Buscando Concepto en BD | ID: {}", detalle.getConcepto().getId());

                    Concepto managedConcepto = entityManager.find(Concepto.class, detalle.getConcepto().getId());
                    if (managedConcepto != null) {
                        // 3.1 Reattach del Concepto
                        log.debug("[procesarYCalcularDetalle] Concepto encontrado | ID: {} | Nombre: '{}'",
                                managedConcepto.getId(), managedConcepto.getDescripcion());
                        detalle.setConcepto(managedConcepto);
                        log.info("[procesarYCalcularDetalle] Concepto reatachado exitosamente");

                        // 3.2 Gestión del SubConcepto
                        if (detalle.getSubConcepto() == null && managedConcepto.getSubConcepto() != null) {
                            log.debug("[procesarYCalcularDetalle] Propagando SubConcepto desde Concepto principal");
                            detalle.setSubConcepto(managedConcepto.getSubConcepto());
                            log.info("[procesarYCalcularDetalle] SubConcepto asignado | ID: {} | Nombre: '{}'",
                                    managedConcepto.getSubConcepto().getId(), managedConcepto.getSubConcepto().getDescripcion());
                        } else {
                            log.debug("[procesarYCalcularDetalle] SubConcepto no asignado - Razón: {}",
                                    (detalle.getSubConcepto() != null ? "ya existente" : "no disponible en Concepto"));
                        }
                    } else {
                        log.warn("[procesarYCalcularDetalle] ADVERTENCIA - Concepto no encontrado en BD | ID Solicitado: {}",
                                detalle.getConcepto().getId());
                    }
                } else {
                    log.warn("[procesarYCalcularDetalle] ADVERTENCIA - Detalle sin Concepto válido | Concepto: {} | ID: {}",
                            (detalle.getConcepto() != null ? "presente" : "ausente"),
                            (detalle.getConcepto() != null ? detalle.getConcepto().getId() : "N/A"));
                }

                // 4. Cálculo del concepto general
                log.info("[procesarYCalcularDetalle] Invocando cálculo para concepto general");
                calcularConceptoGeneral(detalle);
                log.debug("[procesarYCalcularDetalle] Resultados cálculo - aCobrar: {} | Pendiente: {}",
                        detalle.getaCobrar(), detalle.getImportePendiente());

                // 5. Procesamiento especial para CLASE SUELTA
                if (descripcion.contains("CLASE SUELTA")) {
                    log.info("[procesarYCalcularDetalle] DETECTADO CLASE SUELTA - Actualizando crédito del alumno");

                    double creditoActual = Optional.ofNullable(detalle.getAlumno().getCreditoAcumulado()).orElse(0.0);
                    log.debug("[procesarYCalcularDetalle] Crédito actual del alumno ID {}: {}",
                            detalle.getAlumno().getId(), creditoActual);

                    double nuevoCredito = creditoActual + detalle.getaCobrar();
                    log.info("[procesarYCalcularDetalle] Nuevo crédito calculado: {} + {} = {}",
                            creditoActual, detalle.getaCobrar(), nuevoCredito);

                    detalle.getAlumno().setCreditoAcumulado(nuevoCredito);
                    log.info("[procesarYCalcularDetalle] Crédito actualizado para alumno ID {} | Nuevo valor: {}",
                            detalle.getAlumno().getId(), nuevoCredito);
                } else {
                    log.debug("[procesarYCalcularDetalle] No es clase suelta - No se modifica crédito");
                }

                log.info("[procesarYCalcularDetalle] FIN - Procesamiento completado para CONCEPTO GENERAL | Detalle ID: {}",
                        detalle.getId());
                break;
        }

        // Asegurar importes consistentes antes del abono
        Double importeInicial = Optional.ofNullable(detalle.getImporteInicial()).orElse(detalle.getValorBase());
        log.info("[procesarYCalcularDetalle] Importe inicial: {}", importeInicial);
        if (importeInicial == null || importeInicial <= 0) {
            log.warn("[procesarYCalcularDetalle] Importe inicial inválido. Se forzará a importePendiente o valor por defecto.");
            importeInicial = Optional.ofNullable(detalle.getImportePendiente()).orElse(0.0);
            log.info("[procesarYCalcularDetalle] Nuevo importe inicial: {}", importeInicial);
            detalle.setImporteInicial(importeInicial);
        }

        Double aCobrar = Optional.ofNullable(detalle.getaCobrar()).orElse(0.0);
        log.info("[procesarYCalcularDetalle] aCobrar: {}", aCobrar);
        if (aCobrar < 0) {
            log.warn("[procesarYCalcularDetalle] aCobrar no seteado. Se usará importeInicial.");
            aCobrar = 0.0;
            log.info("[procesarYCalcularDetalle] Nuevo aCobrar: {}", aCobrar);
        }

        log.info("[procesarYCalcularDetalle] Asignando aCobrar final: {}", aCobrar);
        detalle.setaCobrar(aCobrar);

        // Procesar abono correctamente
        log.info("[procesarYCalcularDetalle] Procesando abono");
        //procesarAbono(detalle, aCobrar, importeInicial);

        // Determinar estado cobrado
        Double importePendiente = Optional.ofNullable(detalle.getImportePendiente()).orElse(0.0);
        log.info("[procesarYCalcularDetalle] Importe pendiente: {}", importePendiente);
        boolean cobrado = importePendiente <= 0.0;
        log.info("[procesarYCalcularDetalle] Asignando cobrado={}", cobrado);
        detalle.setCobrado(cobrado);
        if (detalle.getCobrado()) {
            log.info("[procesarYCalcularDetalle] Asignando importe pendiente=0.0");
            detalle.setImportePendiente(0.0);
            log.info("[procesarYCalcularDetalle] DetallePago marcado como cobrado.");
        }

        log.info("[procesarYCalcularDetalle] Guardando detalle de pago");
        detallePagoRepositorio.save(detalle);

        log.info("[procesarYCalcularDetalle] FIN. DetallePago actualizado con éxito (id={}).", detalle.getId());
    }

    private void procesarClaseDePrueba(DetallePago detalle, String descripcion, Pago pago) {
        log.info("[procesarClaseDePrueba] INICIO - Procesando clase de prueba para Detalle id={}", detalle.getId());
        log.info("[procesarClaseDePrueba] Descripción recibida: '{}'", descripcion);
        log.info("[procesarClaseDePrueba] Pago asociado id={}", pago.getId());

        // 1. Extraer el nombre de la disciplina a partir de la descripción.
        String nombreDisciplina = extraerNombreDisciplina(descripcion);
        log.info("[procesarClaseDePrueba] Nombre de disciplina extraído: '{}'", nombreDisciplina);

        // 2. Buscar la disciplina en el repositorio.
        Disciplina disciplina = disciplinaRepositorio.findByNombreContainingIgnoreCase(nombreDisciplina);
        if (disciplina == null) {
            log.error("[procesarClaseDePrueba] ERROR - No se encontró disciplina para nombre '{}'", nombreDisciplina);
            log.warn("[procesarClaseDePrueba] No se pudo procesar la clase de prueba para detalle id={}", detalle.getId());
            return;
        }
        log.info("[procesarClaseDePrueba] Disciplina encontrada: id={}, nombre='{}'", disciplina.getId(), disciplina.getNombre());

        // 3. Obtener el importe de la clase de prueba de la disciplina y asignarlo como importeInicial.
        double importeClasePrueba = disciplina.getClasePrueba();
        log.info("[procesarClaseDePrueba] Importe de clase de prueba obtenido: {}", importeClasePrueba);
        detalle.setImporteInicial(importeClasePrueba);

        // 4. Mantener el valor de aCobrar que viene del detalle.
        double valorACobrar = detalle.getaCobrar();
        log.info("[procesarClaseDePrueba] Valor aCobrar recibido del detalle: {}", valorACobrar);
        // No se modifica, se utiliza el valor que ya tiene el detalle.

        // 5. Calcular el importe pendiente:
        // Si el detalle ya tiene un importe pendiente válido, se usa ese valor; de lo contrario, se asume que es igual al importeInicial.
        double importePendienteOriginal = (detalle.getImportePendiente() != null && detalle.getImportePendiente() > 0)
                ? detalle.getImportePendiente() : importeClasePrueba;
        log.info("[procesarClaseDePrueba] Importe pendiente original: {}", importePendienteOriginal);

        double nuevoImportePendiente = importePendienteOriginal - valorACobrar;
        // Evitamos valores negativos, en caso de que se supere el cobro.
        if (nuevoImportePendiente < 0) {
            log.warn("[procesarClaseDePrueba] Nuevo importe pendiente calculado es negativo ({}). Se ajusta a 0.", nuevoImportePendiente);
            nuevoImportePendiente = 0.0;
        }
        detalle.setImportePendiente(nuevoImportePendiente);
        log.info("[procesarClaseDePrueba] Nuevo importe pendiente calculado: {} (Original: {} - aCobrar: {})",
                nuevoImportePendiente, importePendienteOriginal, valorACobrar);

        // 6. Actualizar el crédito acumulado del alumno asociado al pago.
        Alumno alumno = pago.getAlumno();
        if (alumno != null) {
            double creditoActual = (alumno.getCreditoAcumulado() != null) ? alumno.getCreditoAcumulado() : 0.0;
            double nuevoCredito = creditoActual + valorACobrar;
            alumno.setCreditoAcumulado(nuevoCredito);
            log.info("[procesarClaseDePrueba] Crédito actualizado para alumno id={}: {} -> {}", alumno.getId(), creditoActual, nuevoCredito);
        } else {
            log.warn("[procesarClaseDePrueba] No se encontró alumno asociado al pago id={}", pago.getId());
        }

        log.info("[procesarClaseDePrueba] FIN - Proceso completado para detalle id={}", detalle.getId());
    }

    /**
     * Método auxiliar para extraer el nombre de la disciplina.
     * Se asume que la descripción tiene el formato "DISCIPLINA - TARIFA - PERIODO".
     */
    String extraerNombreDisciplina(String descripcion) {
        log.info("[extraerNombreDisciplina] INICIO - Extrayendo nombre de disciplina de: '{}'", descripcion);

        log.debug("[extraerNombreDisciplina] Separando descripción por ' - '");
        String[] partes = descripcion.split(" - ");
        log.debug("[extraerNombreDisciplina] Partes encontradas: {}", (Object) partes);

        String nombre;
        if (partes.length > 0) {
            nombre = partes[0].trim();
            log.debug("[extraerNombreDisciplina] Primera parte seleccionada: '{}'", partes[0]);
        } else {
            nombre = descripcion.trim();
            log.debug("[extraerNombreDisciplina] Usando descripción completa: '{}'", descripcion);
        }

        log.info("[extraerNombreDisciplina] Nombre final extraído: '{}'", nombre);
        log.debug("[extraerNombreDisciplina] Longitud del nombre: {}", nombre.length());

        log.info("[extraerNombreDisciplina] FIN - Retornando nombre extraído");
        return nombre;
    }

    private void aplicarDescuentoCreditoEnMatricula(Pago pago, DetallePago detalle) {
        log.info("[aplicarDescuentoCreditoEnMatricula] INICIO. Pago id={}, DetallePago id={}",
                pago.getId(), detalle.getId());

        Alumno alumno = pago.getAlumno();
        log.info("[aplicarDescuentoCreditoEnMatricula] Alumno obtenido: id={}", alumno.getId());

        double creditoAlumno = (alumno.getCreditoAcumulado() != null) ? alumno.getCreditoAcumulado() : 0.0;
        log.info("[aplicarDescuentoCreditoEnMatricula] Crédito del alumno: {}", creditoAlumno);

        double montoMatricula = detalle.getaCobrar();
        log.info("[aplicarDescuentoCreditoEnMatricula] Monto original de matrícula: {}", montoMatricula);

        // Se suma el crédito acumulado al monto que paga el estudiante
        log.info("[aplicarDescuentoCreditoEnMatricula] Calculando total a cobrar: {} + {} = {}",
                montoMatricula, creditoAlumno, montoMatricula);

        log.info("[aplicarDescuentoCreditoEnMatricula] Sumando crédito acumulado de {} en matrícula. Monto request: {}. Total a cobrar: {}",
                creditoAlumno, montoMatricula, montoMatricula);

        log.info("[aplicarDescuentoCreditoEnMatricula] Asignando nuevo valor a cobrar: {}", montoMatricula);
        detalle.setaCobrar(montoMatricula);

        // Si se consume el crédito, se resetea a 0; de lo contrario, se puede ajustar según la lógica de negocio.
        log.info("[aplicarDescuentoCreditoEnMatricula] Reseteando crédito acumulado a 0");
        alumno.setCreditoAcumulado(0.0);

        log.info("[aplicarDescuentoCreditoEnMatricula] FIN. Crédito aplicado y reseteado.");
    }

    // -----------------------------------------------------------------
    // METODOS ESPECIFICOS DE CALCULO DE DETALLES
    // -----------------------------------------------------------------

    /**
     * Calcula el detalle de tipo MATRICULA.
     * Se asume que el importe inicial es el valorBase (sin descuentos ni recargos).
     */
    @Transactional
    public void calcularMatricula(DetallePago detalle, Pago pago) {
        log.info("[calcularMatricula] Iniciando procesamiento para DetallePago id={}", detalle.getId());

        // 1. Calcular o asignar el importe inicial
        Double importeInicialFrontend = detalle.getImporteInicial();
        if (Boolean.TRUE.equals(detalle.getEsClon()) && importeInicialFrontend != null) {
            log.info("[calcularMatricula] Detalle es clon, usando importe inicial existente: {}", importeInicialFrontend);
        } else if (importeInicialFrontend != null && importeInicialFrontend > 0) {
            log.info("[calcularMatricula] Usando importeInicial proporcionado: {}", importeInicialFrontend);
        } else {
            log.info("[calcularMatricula] Calculando nuevo importe inicial para matrícula");
            double importeCalculado = calcularImporteInicial(detalle, null, true);
            detalle.setImporteInicial(importeCalculado);
            log.info("[calcularMatricula] ImporteInicial calculado internamente: {} para Detalle id={}",
                    importeCalculado, detalle.getId());
        }

        // 2. Asignar importe pendiente (si no viene del front, se iguala a importeInicial)
        if (detalle.getImportePendiente() != null && detalle.getImportePendiente() >= 0) {
            log.info("[calcularMatricula] Usando importePendiente proporcionado: {} para Detalle id={}",
                    detalle.getImportePendiente(), detalle.getId());
        } else {
            detalle.setImportePendiente(detalle.getImporteInicial());
            log.info("[calcularMatricula] Asignado importePendiente igual a importeInicial: {} para Detalle id={}",
                    detalle.getImporteInicial(), detalle.getId());
        }

        // 3. Si no tiene recargo, limpiar
        if (!Boolean.TRUE.equals(detalle.getTieneRecargo())) {
            detalle.setRecargo(null);
            log.info("[calcularMatricula] Sin recargo para Detalle id={}", detalle.getId());
        } else {
            log.info("[calcularMatricula] Con recargo: {} para Detalle id={}", detalle.getRecargo(), detalle.getId());
        }

        // -- Procesar la entidad Matricula (como ya lo haces) --
        log.info("[calcularMatricula] Procesando matrícula para pago id={}", pago.getId());
        procesarMatricula(pago, detalle);  // asigna la 'Matricula' en el detalle

        // 4. Fase de abono: descontar lo que se está “aCobrar”
        Double aCobrar = Optional.ofNullable(detalle.getaCobrar()).orElse(0.0);
        if (aCobrar > 0) {
            double nuevoPendiente = detalle.getImportePendiente() - aCobrar;
            detalle.setImportePendiente(nuevoPendiente);
            log.info("[calcularMatricula] Abono procesado: aCobrar={} => nuevo importePendiente={}",
                    aCobrar, nuevoPendiente);
        }

        int anio = extraerAnioDeDescripcion(detalle.getDescripcionConcepto(), detalle.getId());

        Matricula matricula = matriculaServicio.obtenerOMarcarPendienteMatricula(detalle.getAlumno().getId(), anio);

        // 5. Marcar como cobrado si el pendiente queda 0 o menos
        if (detalle.getImportePendiente() <= 0.0) {
            detalle.setCobrado(true);
            detalle.setImportePendiente(0.0);
            matricula.setPagada(true);

            log.info("[calcularMatricula] Detalle id={} marcado como cobrado", detalle.getId());
        } else {
            detalle.setCobrado(false);
            log.info("[calcularMatricula] Detalle id={} NO cobrado (importe pendiente > 0)", detalle.getId());
        }

        // Guardar los cambios en el detalle
        detallePagoRepositorio.save(detalle);

        log.info("[calcularMatricula] Detalle id={} - Finalizado procesamiento de matricula con importeInicial={}, importePendiente={}",
                detalle.getId(), detalle.getImporteInicial(), detalle.getImportePendiente());
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
     * Calcula el importe inicial de una mensualidad para un DetallePago, usando la inscripcion (si está disponible)
     * para aplicar descuentos y recargos.
     */
    @Transactional
    public void calcularMensualidad(DetallePago detalle, Inscripcion inscripcion) {
        log.info("[calcularMensualidad] INICIO cálculo para DetallePago id={}", detalle.getId());

        // 1. Establecer importeInicial: usar valor de frontend o calcular internamente
        Double importeInicialFrontend = detalle.getImporteInicial();
        if (importeInicialFrontend != null && importeInicialFrontend > 0) {
            log.info("[calcularMensualidad] Usando importeInicial proporcionado: {}", importeInicialFrontend);
        } else {
            double importeCalculado = calcularImporteInicial(detalle, inscripcion, false);
            detalle.setImporteInicial(importeCalculado);
            log.info("[calcularMensualidad] ImporteInicial calculado internamente: {} para Detalle id={}", importeCalculado, detalle.getId());
        }

        // 2. Establecer importePendiente: si viene desde el frontend se respeta; de lo contrario, igualarlo a importeInicial.
        Double importePendienteFrontend = detalle.getImportePendiente();
        if (importePendienteFrontend != null && importePendienteFrontend >= 0) {
            log.info("[calcularMensualidad] Usando importePendiente proporcionado: {} para Detalle id={}", importePendienteFrontend, detalle.getId());
        } else {
            detalle.setImportePendiente(detalle.getImporteInicial());
            log.info("[calcularMensualidad] Asignado importePendiente igual a importeInicial: {} para Detalle id={}", detalle.getImporteInicial(), detalle.getId());
        }

        // 3. Ajuste de recargo: Si no tiene recargo, se limpia
        if (!Boolean.TRUE.equals(detalle.getTieneRecargo())) {
            detalle.setRecargo(null);
            log.info("[calcularMensualidad] Sin recargo para Detalle id={}", detalle.getId());
        } else {
            log.info("[calcularMensualidad] Con recargo: {} para Detalle id={}", detalle.getRecargo(), detalle.getId());
        }

        // 4. Procesar el abono: Si se proporcionó aCobrar, se descuenta del importeInicial
        Double aCobrar = detalle.getaCobrar();
        if (aCobrar != null && aCobrar > 0) {
            double nuevoPendiente = detalle.getImporteInicial() - aCobrar;
            detalle.setImportePendiente(nuevoPendiente);
            log.info("[calcularMensualidad] Abono procesado: aCobrar={} => nuevo importePendiente={}", aCobrar, nuevoPendiente);
        }

        // 5. Actualizar estado: marcar como cobrado si no queda pendiente
        if (detalle.getImportePendiente() <= 0.0) {
            detalle.setCobrado(true);
            detalle.setImportePendiente(0.0);
            log.info("[calcularMensualidad] Detalle id={} marcado como cobrado", detalle.getId());
        } else {
            detalle.setCobrado(false);
            log.info("[calcularMensualidad] Detalle id={} NO cobrado (importe pendiente > 0)", detalle.getId());
        }

        // Persistir cambios
        detallePagoRepositorio.save(detalle);
        log.info("[calcularMensualidad] FIN cálculo. Detalle id={} persistido con importeInicial={} e importePendiente={}",
                detalle.getId(), detalle.getImporteInicial(), detalle.getImportePendiente());
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

        log.info("[procesarMatricula] Extrayendo año de descripción: {}", detalle.getDescripcionConcepto());
        int anio = extraerAnioDeDescripcion(detalle.getDescripcionConcepto(), detalle.getId());
        log.info("[procesarMatricula] Año extraído: {}", anio);

        Alumno alumno = pago.getAlumno();
        if (alumno == null) {
            log.error("[procesarMatricula] Alumno no definido en el pago para DetallePago id={}", detalle.getId());
            throw new EntityNotFoundException("Alumno no definido en el pago");
        }
        log.info("[procesarMatricula] Procesando matricula para Alumno id={} y año={}", alumno.getId(), anio);

        log.info("[procesarMatricula] Obteniendo o creando matrícula pendiente");
        Matricula matricula = matriculaServicio.obtenerOMarcarPendienteMatricula(alumno.getId(), anio);
        log.info("[procesarMatricula] Matrícula obtenida: id={}", matricula.getId());

        log.info("[procesarMatricula] Verificando estado de pago de matrícula");
        if (matricula.getPagada()) {
            log.error("[procesarMatricula] La matricula para el año {} ya esta saldada para Alumno id={}", anio, alumno.getId());
            throw new IllegalArgumentException("La matricula para el año " + anio + " ya esta saldada.");
        }

        log.info("[procesarMatricula] Asignando matrícula al detalle");
        detalle.setMatricula(matricula);
        log.info("[procesarMatricula] Se asigno matricula id={} al DetallePago id={}", matricula.getId(), detalle.getId());

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
    @Transactional
    public TipoDetallePago determinarTipoDetalle(String descripcionConcepto) {
        // 1. Validación inicial y normalización
        log.info("[determinarTipoDetalle] INICIO - Determinando tipo para descripción: '{}'", descripcionConcepto);

        if (descripcionConcepto == null) {
            log.warn("[determinarTipoDetalle] Descripción nula - Asignando tipo CONCEPTO por defecto");
            return TipoDetallePago.CONCEPTO;
        }

        String conceptoNorm = descripcionConcepto.trim().toUpperCase();
        log.debug("[determinarTipoDetalle] Descripción normalizada: '{}'", conceptoNorm);

        // 2. Verificación de MATRÍCULA
        if (conceptoNorm.startsWith("MATRICULA")) {
            log.info("[determinarTipoDetalle] Tipo MATRICULA detectado - Patrón: 'MATRICULA'");
            return TipoDetallePago.MATRICULA;
        }

        // 3. Verificación de MENSUALIDAD
        if (conceptoNorm.contains("CUOTA") ||
                conceptoNorm.contains("CLASE SUELTA") ||
                conceptoNorm.contains("CLASE DE PRUEBA")) {

            String patron = "";
            if (conceptoNorm.contains("CUOTA")) patron = "CUOTA";
            else if (conceptoNorm.contains("CLASE SUELTA")) patron = "CLASE SUELTA";
            else patron = "CLASE DE PRUEBA";

            log.info("[determinarTipoDetalle] Tipo MENSUALIDAD detectado - Patrón: '{}'", patron);
            return TipoDetallePago.MENSUALIDAD;
        }

        // 4. Verificación de STOCK
        log.debug("[determinarTipoDetalle] Verificando existencia en stock");
        if (existeStockConNombre(conceptoNorm)) {
            log.info("[determinarTipoDetalle] Tipo STOCK detectado - Existe en inventario");
            return TipoDetallePago.STOCK;
        } else {
            log.debug("[determinarTipoDetalle] No existe coincidencia en stock");
        }

        // 5. Tipo por defecto
        log.info("[determinarTipoDetalle] Asignando tipo CONCEPTO por defecto");
        return TipoDetallePago.CONCEPTO;
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


    /**
     * Reatacha las asociaciones de un DetallePago (alumno, mensualidad, matricula, stock)
     * para garantizar que las entidades esten en estado managed y evitar errores de detached.
     *
     * @param detalle el objeto DetallePago a reatachar.
     * @param pago    el objeto Pago asociado.
     */
    @Transactional
    public void reatacharAsociaciones(DetallePago detalle, Pago pago) {
        log.info("[reatacharAsociaciones] INICIO: Reatachamiento para DetallePago id={}", detalle.getId());

        // Asignar 'aCobrar' si es nulo
        if (detalle.getaCobrar() == null) {
            detalle.setaCobrar(0.0);
            log.info("[reatacharAsociaciones] Se asignó aCobrar=0.0 para DetallePago id={}", detalle.getId());
        }

        // Reatachar Alumno
        if (detalle.getAlumno() == null && pago.getAlumno() != null) {
            log.info("[reatacharAsociaciones] Se asignará el alumno del pago a DetallePago");
            detalle.setAlumno(pago.getAlumno());
        } else {
            log.info("[reatacharAsociaciones] Alumno ya asignado: {}",
                    (detalle.getAlumno() != null ? detalle.getAlumno().getId() : "null"));
        }

        if ((detalle.getConcepto() == null || detalle.getConcepto().getId() == null) && detalle.getConcepto() != null) {
            Concepto managedConcepto = entityManager.find(Concepto.class, detalle.getConcepto());
            if (managedConcepto != null) {
                detalle.setConcepto(managedConcepto);
                log.info("[reatacharAsociaciones] Concepto reatachado desde conceptoId: {}", managedConcepto.getId());
                if (detalle.getSubConcepto() == null && managedConcepto.getSubConcepto() != null) {
                    detalle.setSubConcepto(managedConcepto.getSubConcepto());
                    log.info("[reatacharAsociaciones] SubConcepto reatachado: {}", managedConcepto.getSubConcepto().getId());
                }
            } else {
                log.warn("[reatacharAsociaciones] No se encontró Concepto con id={}", detalle.getConcepto());
            }
        }

        // Reatachar Mensualidad, si existe
        if (detalle.getMensualidad() != null && detalle.getMensualidad().getId() != null) {
            log.info("[reatacharAsociaciones] Reatachando Mensualidad para DetallePago id={}", detalle.getId());
            Mensualidad managedMensualidad = entityManager.find(Mensualidad.class, detalle.getMensualidad().getId());
            if (managedMensualidad != null) {
                detalle.setMensualidad(managedMensualidad);
                log.info("[reatacharAsociaciones] Mensualidad reatachada: {}", managedMensualidad.getId());
            } else {
                log.warn("[reatacharAsociaciones] No se encontró Mensualidad con id={}", detalle.getMensualidad().getId());
            }
        }

        // Reatachar Matrícula, si existe
        if (detalle.getMatricula() != null && detalle.getMatricula().getId() != null) {
            log.info("[reatacharAsociaciones] Reatachando Matrícula para DetallePago id={}", detalle.getId());
            Matricula managedMatricula = entityManager.find(Matricula.class, detalle.getMatricula().getId());
            if (managedMatricula != null) {
                detalle.setMatricula(managedMatricula);
                log.info("[reatacharAsociaciones] Matrícula reatachada: {}", managedMatricula.getId());
            } else {
                log.warn("[reatacharAsociaciones] No se encontró Matrícula con id={}", detalle.getMatricula().getId());
            }
        }

        // Reatachar Stock, si existe
        if (detalle.getStock() != null && detalle.getStock().getId() != null) {
            log.info("[reatacharAsociaciones] Reatachando Stock para DetallePago id={}", detalle.getId());
            Stock managedStock = entityManager.find(Stock.class, detalle.getStock().getId());
            if (managedStock != null) {
                detalle.setStock(managedStock);
                log.info("[reatacharAsociaciones] Stock reatachado: {}", managedStock.getId());
            } else {
                log.warn("[reatacharAsociaciones] No se encontró Stock con id={}", detalle.getStock().getId());
            }
        }

        log.info("[reatacharAsociaciones] FIN: Reatachamiento finalizado para DetallePago id={}", detalle.getId());
    }

}

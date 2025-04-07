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

import java.util.List;
import java.util.Objects;
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
            detalle.setTieneRecargo(false);
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
     * - Se actualicen los campos ACobrar e importePendiente correctamente.
     */
    // Método corregido para procesar correctamente los abonos y actualizar estados
    void procesarAbono(DetallePago detalle, Double montoAbono, Double importeInicialCalculado) {
        // 1. Inicio y validaciones iniciales
        log.info("[procesarAbono] INICIO - Procesando abono para DetallePago ID: {}", detalle.getId());
        log.info("[procesarAbono] Parámetros recibidos - MontoAbono: {}, ImporteInicialCalculado: {}",
                montoAbono, importeInicialCalculado);
        log.info("[procesarAbono] Estado inicial del detalle: {}", detalle);

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
                log.info("[procesarAbono] Usando importe inicial calculado: {}", importeInicialCalculado);
                detalle.setImporteInicial(importeInicialCalculado);
            } else {
                log.warn("[procesarAbono] Usando monto de abono como importe inicial (valor por defecto)");
                detalle.setImporteInicial(montoAbono);
            }
        }
        log.info("[procesarAbono] Importe inicial final: {}", detalle.getImporteInicial());

        // 4. Cálculo de importe pendiente
        log.info("[procesarAbono] Calculando importe pendiente actual");
        Double importePendienteActual = (detalle.getImportePendiente() == null)
                ? detalle.getImporteInicial()
                : detalle.getImportePendiente();
        log.info("[procesarAbono] Importe pendiente calculado: {}", importePendienteActual);

        if (importePendienteActual == null) {
            log.error("[procesarAbono] ERROR - No se puede determinar el importe pendiente");
            throw new IllegalStateException("No se puede determinar el importe pendiente.");
        }

        // 5. Ajuste de monto de abono
        log.info("[procesarAbono] Monto de abono ajustado: {}", montoAbono);

        // 6. Actualización de valores
        log.info("[procesarAbono] Actualizando valores del detalle");
        detalle.setACobrar(montoAbono);
        log.info("[procesarAbono] ACobrar asignado: {}", detalle.getACobrar());

        double nuevoPendiente = importePendienteActual - montoAbono;
        detalle.setImportePendiente(nuevoPendiente);
        log.info("[procesarAbono] Nuevo importe pendiente: {}", nuevoPendiente);

        // 7. Gestión de estado de cobro
        if (nuevoPendiente <= 0) {
            log.info("[procesarAbono] Detalle completamente pagado - Marcando como cobrado");
            detalle.setImportePendiente(0.0);
            detalle.setEstadoPago(EstadoPago.HISTORICO);
            detalle.setCobrado(true);
            log.info("[procesarAbono] Estado cobrado actualizado: {}", detalle.getCobrado());

            // 7.1 Actualización de entidades relacionadas
            if (detalle.getTipo() == TipoDetallePago.MENSUALIDAD) {
                log.info("[procesarAbono] Actualizando estado de mensualidad a PAGADO");
                detalle.getMensualidad().setEstado(EstadoMensualidad.PAGADO);
                log.info("[procesarAbono] Estado mensualidad actualizado: {}",
                        detalle.getMensualidad().getEstado());
            }

            if (detalle.getTipo() == TipoDetallePago.MATRICULA && detalle.getMatricula() != null) {
                log.info("[procesarAbono] Marcando matrícula como pagada");
                detalle.getMatricula().setPagada(true);
                log.info("[procesarAbono] Estado matrícula actualizado: {}",
                        detalle.getMatricula().getPagada());
            }
        } else {
            log.info("[procesarAbono] Detalle parcialmente pagado - Pendiente restante: {}", nuevoPendiente);
            detalle.setCobrado(false);
        }

        log.info("[procesarAbono] FIN - Procesamiento completado para DetallePago ID: {} | Pendiente final: {} | Cobrado: {}",
                detalle.getId(), detalle.getImportePendiente(), detalle.getCobrado());
        log.info("[procesarAbono] Estado final del detalle: {}", detalle.toString());
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
        log.info("[procesarYCalcularDetalle] Iniciando cálculo DetallePago: {}", detalle);

        // 1. Normalización de la descripción
        String descripcion = Optional.ofNullable(detalle.getDescripcionConcepto())
                .orElse("")
                .trim()
                .toUpperCase();
        detalle.setDescripcionConcepto(descripcion);
        log.info("[procesarYCalcularDetalle] Descripción normalizada: {}", descripcion);

        // 2. Determinar tipo si no está seteado
        if (detalle.getTipo() == null) {
            TipoDetallePago tipo = determinarTipoDetalle(descripcion);
            detalle.setTipo(tipo);
            log.info("[procesarYCalcularDetalle] Tipo determinado: {}", tipo);
        }

        // 3. Reattach de Concepto (y SubConcepto) de forma correcta
        if (detalle.getConcepto() != null && detalle.getConcepto().getId() != null) {
            log.info("[procesarYCalcularDetalle] Buscando Concepto en BD | ID: {}", detalle.getConcepto().getId());
            Concepto managedConcepto = entityManager.find(Concepto.class, detalle.getConcepto().getId());
            if (managedConcepto == null) {
                log.error("[procesarYCalcularDetalle] Concepto con ID {} no encontrado en BD.", detalle.getConcepto().getId());
                // Puedes lanzar una excepción o asignar un concepto por defecto.
                throw new EntityNotFoundException("Concepto con ID " + detalle.getConcepto().getId() + " no encontrado en BD.");
            }
            detalle.setConcepto(managedConcepto);
            if (detalle.getSubConcepto() == null && managedConcepto.getSubConcepto() != null) {
                detalle.setSubConcepto(managedConcepto.getSubConcepto());
                log.info("[procesarYCalcularDetalle] SubConcepto asignado: {}", managedConcepto.getSubConcepto().getId());
            }
        }

        // 4. Validar recargo: respetar el flag tieneRecargo del cliente.
        if (!detalle.getTieneRecargo()) {
            detalle.setTieneRecargo(false);
            log.info("[procesarYCalcularDetalle] Recargo desactivado (tieneRecargo=false) para Detalle id={}", detalle.getId());
        } else {
            log.info("[procesarYCalcularDetalle] Recargo activo; manteniendo valor existente: {}", detalle.getRecargo());
        }

        // 5. Procesamiento según el tipo de detalle
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
                    log.warn("[procesarYCalcularDetalle] Mensualidad sin inscripción ni clase de prueba.");
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
                calcularMatricula(detalle, detalle.getPago());
                break;
            case STOCK:
                log.info("[procesarYCalcularDetalle] Procesando stock");
                calcularStock(detalle);
                break;
            default:
                log.info("[procesarYCalcularDetalle] Procesando concepto general para Detalle id={}", detalle.getId());
                detalle.setTipo(TipoDetallePago.CONCEPTO);
                // Aquí ya se hizo el reattach del concepto
                log.info("[procesarYCalcularDetalle] Invocando cálculo para concepto general");
                calcularConceptoGeneral(detalle);
                if (descripcion.contains("CLASE SUELTA")) {
                    log.info("[procesarYCalcularDetalle] Actualizando crédito del alumno por CLASE SUELTA");
                    double creditoActual = Optional.ofNullable(detalle.getAlumno().getCreditoAcumulado()).orElse(0.0);
                    double nuevoCredito = creditoActual + detalle.getACobrar();
                    detalle.getAlumno().setCreditoAcumulado(nuevoCredito);
                    log.info("[procesarYCalcularDetalle] Crédito actualizado para alumno ID {}: {}", detalle.getAlumno().getId(), nuevoCredito);
                } else {
                    log.info("[procesarYCalcularDetalle] No se modifica crédito para Detalle id={}", detalle.getId());
                }
                break;
        }

        // 6. Validar consistencia en importes: importeInicial, ACobrar y estado de cobro
        Double impInicialFinal = Optional.ofNullable(detalle.getImporteInicial()).orElse(detalle.getValorBase());
        if (impInicialFinal == null || impInicialFinal <= 0) {
            impInicialFinal = Optional.ofNullable(detalle.getImportePendiente()).orElse(0.0);
            detalle.setImporteInicial(impInicialFinal);
            log.warn("[procesarYCalcularDetalle] Importe inicial inválido; se asignó nuevo valor: {}", impInicialFinal);
        }
        double ACobrarFinal = Optional.ofNullable(detalle.getACobrar()).orElse(0.0);
        if (ACobrarFinal < 0) {
            ACobrarFinal = 0.0;
            log.warn("[procesarYCalcularDetalle] ACobrar inválido; se asignó 0.0");
        }
        detalle.setACobrar(ACobrarFinal);

        Double impPendienteFinal = Optional.ofNullable(detalle.getImportePendiente()).orElse(0.0);
        boolean cobrado = impPendienteFinal <= 0.0;
        detalle.setCobrado(cobrado);
        if (cobrado) {
            detalle.setEstadoPago(EstadoPago.HISTORICO);
            detalle.setCobrado(true);
            detalle.setImportePendiente(0.0);
            log.info("[procesarYCalcularDetalle] DetallePago marcado como cobrado");
        }
        log.info("[procesarYCalcularDetalle] Finalizando cálculo DetallePago: {}", detalle);

        // 7. Persistir cambios
        detallePagoRepositorio.save(detalle);
        log.info("[procesarYCalcularDetalle] FIN. DetallePago actualizado con éxito (id={})", detalle.getId());
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

        // 4. Mantener el valor de ACobrar que viene del detalle.
        double valorACobrar = detalle.getACobrar();
        log.info("[procesarClaseDePrueba] Valor ACobrar recibido del detalle: {}", valorACobrar);
        // No se modifica, se utiliza el valor que ya tiene el detalle.

        // 5. Calcular el importe pendiente:
        // Si el detalle ya tiene un importe pendiente válido, se usa ese valor; de lo contrario, se asume que es igual al importeInicial.
        double importePendienteOriginal = (detalle.getImportePendiente() != null && detalle.getImportePendiente() > 0)
                ? detalle.getImportePendiente() : importeClasePrueba;
        log.info("[procesarClaseDePrueba] Importe pendiente original: {}", importePendienteOriginal);

        double nuevoImportePendiente = importePendienteOriginal - valorACobrar;
        // Evitamos valores negativos, en caso de que se supere el cobro.
        if (nuevoImportePendiente <= 0) {
            log.warn("[procesarClaseDePrueba] Nuevo importe pendiente calculado es negativo ({}). Se ajusta a 0.", nuevoImportePendiente);
            nuevoImportePendiente = 0.0;
            detalle.setEstadoPago(EstadoPago.HISTORICO);
            detalle.setCobrado(true);
            detalle.setImportePendiente(0.0);
        }
        detalle.setImportePendiente(nuevoImportePendiente);
        log.info("[procesarClaseDePrueba] Nuevo importe pendiente calculado: {} (Original: {} - ACobrar: {})",
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

        log.info("[extraerNombreDisciplina] Separando descripción por ' - '");
        String[] partes = descripcion.split(" - ");
        log.info("[extraerNombreDisciplina] Partes encontradas: {}", (Object) partes);

        String nombre;
        if (partes.length > 0) {
            nombre = partes[0].trim();
            log.info("[extraerNombreDisciplina] Primera parte seleccionada: '{}'", partes[0]);
        } else {
            nombre = descripcion.trim();
            log.info("[extraerNombreDisciplina] Usando descripción completa: '{}'", descripcion);
        }

        log.info("[extraerNombreDisciplina] Nombre final extraído: '{}'", nombre);
        log.info("[extraerNombreDisciplina] Longitud del nombre: {}", nombre.length());

        log.info("[extraerNombreDisciplina] FIN - Retornando nombre extraído");
        return nombre;
    }

    /**
     * Aplica el descuento del crédito acumulado al detalle de matrícula.
     * En vez de modificar el monto a cobrar (ACobrar), se actualiza el importeInicial
     * (y también el importePendiente) para reflejar el descuento.
     */
    private void aplicarDescuentoCreditoEnMatricula(Pago pago, DetallePago detalle) {
        log.info("[aplicarDescuentoCreditoEnMatricula] INICIO. Pago id={}, DetallePago id={}",
                pago.getId(), detalle.getId());

        Alumno alumno = pago.getAlumno();
        if (alumno == null) {
            throw new IllegalArgumentException("El alumno del pago es requerido");
        }
        log.info("[aplicarDescuentoCreditoEnMatricula] Alumno obtenido: id={}", alumno.getId());

        // Obtener el crédito del alumno (0.0 si es null)
        double creditoAlumno = alumno.getCreditoAcumulado() != null ? alumno.getCreditoAcumulado() : 0.0;
        log.info("[aplicarDescuentoCreditoEnMatricula] Crédito del alumno: {}", creditoAlumno);

        // Suponemos que el payload ya trae:
        //   importeInicial = 35000.0
        //   importePendiente = 27000.0
        // Por lo tanto, la diferencia (crédito aplicado) es:
        double importeInicialOriginal = detalle.getImporteInicial(); // 35000.0
        double importePendienteActual = detalle.getImportePendiente(); // 27000.0
        log.info("[aplicarDescuentoCreditoEnMatricula] Actualizado importeInicial: {}  e importePendiente a: {}", detalle.getImporteInicial(), detalle.getImportePendiente());

        double diferencia = importeInicialOriginal - importePendienteActual; // 35000 - 27000 = 8000
        // Se aplica como máximo el crédito disponible o la diferencia obtenida
        double creditoAplicable = Math.min(creditoAlumno, diferencia);

        // Ahora, en vez de modificar ACobrar, actualizamos el importeInicial y el importePendiente
        double nuevoValorBase = importeInicialOriginal - creditoAplicable; // 35000 - 8000 = 27000
        detalle.setImporteInicial(nuevoValorBase);
        detalle.setImportePendiente(nuevoValorBase);
        log.info("[aplicarDescuentoCreditoEnMatricula] Actualizado importeInicial e importePendiente a: {}", nuevoValorBase);

        // Se consume el crédito aplicado del alumno
        alumno.setCreditoAcumulado(creditoAlumno - creditoAplicable);
        log.info("[aplicarDescuentoCreditoEnMatricula] Crédito aplicado: {}. Crédito restante: {}",
                creditoAplicable, alumno.getCreditoAcumulado());
        log.info("[aplicarDescuentoCreditoEnMatricula] FIN.");
    }

    /**
     * Calcula y procesa el detalle de pago para la matrícula.
     * Se realiza la asignación del importe inicial, se aplica el descuento por crédito,
     * se actualiza el importe base para futuros cálculos y se procesa el abono.
     */
    @Transactional
    public void calcularMatricula(DetallePago detalle, Pago pago) {
        log.info("[calcularMatricula] INICIO. Procesando DetallePago id={}", detalle.getId());
        log.info("[calcularMatricula] Detalle recibido: {}", detalle);

        // 1. Asignar o calcular el importeInicial (si no se ha recibido)
        Double importeInicialFrontend = detalle.getImporteInicial();
        if (Boolean.TRUE.equals(detalle.getEsClon()) && importeInicialFrontend != null) {
            log.info("[calcularMatricula] Detalle es clon; usando importeInicial existente: {}", importeInicialFrontend);
        } else if (importeInicialFrontend != null && importeInicialFrontend > 0) {
            log.info("[calcularMatricula] Usando importeInicial proporcionado: {}", importeInicialFrontend);
        } else {
            log.info("[calcularMatricula] Calculando importeInicial para matrícula");
            double importeCalculado = calcularImporteInicial(detalle, null, true);
            detalle.setImporteInicial(importeCalculado);
            log.info("[calcularMatricula] ImporteInicial calculado: {} para Detalle id={}", importeCalculado, detalle.getId());
        }

        // 2. Si no se ha definido el importe pendiente, se asigna igual al importe inicial
        if (detalle.getImportePendiente() == null || detalle.getImportePendiente() < 0) {
            detalle.setImportePendiente(detalle.getImporteInicial());
            log.info("[calcularMatricula] Asignado importePendiente = importeInicial: {} para Detalle id={}",
                    detalle.getImporteInicial(), detalle.getId());
        } else {
            log.info("[calcularMatricula] Usando importePendiente proporcionado: {} para Detalle id={}",
                    detalle.getImportePendiente(), detalle.getId());
        }

        // 3. Manejo del recargo
        if ((!detalle.getTieneRecargo()) ||
                detalle.getTipo() == TipoDetallePago.MENSUALIDAD) {
            detalle.setTieneRecargo(false);
            log.info("[calcularMatricula] Sin recargo para Detalle id={}", detalle.getId());
        } else {
            log.info("[calcularMatricula] Con recargo: {} para Detalle id={}", detalle.getRecargo(), detalle.getId());
        }

        // 4. Procesar la matrícula y obtenerla (única llamada)
        Matricula matricula = procesarMatricula(pago, detalle);

        // 5. Aplicar descuento de crédito (actualiza importeInicial e importePendiente)
        aplicarDescuentoCreditoEnMatricula(pago, detalle);

        // 6. Actualizar el importeInicial para que refleje el nuevo monto base (después del descuento)
        detalle.setImporteInicial(detalle.getImportePendiente());
        log.info("[calcularMatricula] Actualizado importeInicial a: {} tras aplicar descuento", detalle.getImporteInicial());

        // 7. Fase de abono: se descuenta el valor abonado del importe pendiente
        double ACobrar = detalle.getACobrar() != null ? detalle.getACobrar() : 0.0;
        if (ACobrar > 0) {
            double nuevoPendiente = detalle.getImportePendiente() - ACobrar;
            detalle.setImportePendiente(nuevoPendiente);
            log.info("[calcularMatricula] Abono procesado: ACobrar={} => nuevo importePendiente={}", ACobrar, nuevoPendiente);
        }

        // 8. Marcar el detalle como cobrado si el importe pendiente es cero o menor
        if (detalle.getImportePendiente() <= 0.0) {
            detalle.setCobrado(true);
            detalle.setImportePendiente(0.0);
            detalle.setEstadoPago(EstadoPago.HISTORICO);
            matricula.setPagada(true);
            log.info("[calcularMatricula] Detalle id={} marcado como cobrado", detalle.getId());
        } else {
            detalle.setCobrado(false);
            log.info("[calcularMatricula] Detalle id={} NO cobrado (importe pendiente > 0)", detalle.getId());
        }

        // 9. Persistir los cambios en el detalle
        detallePagoRepositorio.save(detalle);
        log.info("[calcularMatricula] FIN. Detalle id={} finalizado. ImporteInicial={}, ImportePendiente={}",
                detalle.getId(), detalle.getImporteInicial(), detalle.getImportePendiente());
    }

    /**
     * Calcula el importe inicial total a partir de una lista de DetallePago.
     * Se suma el importeInicial de cada detalle (usando 0.0 si es null).
     */
    private @NotNull @Min(value = 0, message = "El monto base no puede ser negativo")
    Double calcularImporteInicialDesdeDetalles(List<DetallePago> detallePagos) {
        log.info("[calcularImporteInicialDesdeDetalles] Iniciando calculo del importe inicial.");

        if (detallePagos == null || detallePagos.isEmpty()) {
            log.info("[calcularImporteInicialDesdeDetalles] Lista de DetallePagos nula o vacía. Retornando 0.0");
            return 0.0;
        }

        double total = detallePagos.stream()
                .filter(Objects::nonNull)
                .mapToDouble(detalle -> Optional.ofNullable(detalle.getImporteInicial()).orElse(0.0))
                .sum();

        total = Math.max(0.0, total); // Se asegura que no sea negativo
        log.info("[calcularImporteInicialDesdeDetalles] Total calculado: {}", total);

        return total;
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
        log.info("[calcularMensualidad] Iniciando cálculo DetallePago: {}", detalle);

        // 1. Establecer importeInicial: usar el valor enviado o calcular si es inválido
        Double impInicial = detalle.getImporteInicial();
        if (impInicial == null || impInicial <= 0) {
            impInicial = calcularImporteInicial(detalle, inscripcion, false);
            detalle.setImporteInicial(impInicial);
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

    /**
     * Calcula el detalle para un concepto generico.
     */
    void calcularConceptoGeneral(DetallePago detalle) {
        double importeInicialCalculado = calcularImporteInicial(detalle, null, false);
        procesarAbono(detalle, detalle.getACobrar(), importeInicialCalculado);
        detalle.setCobrado(detalle.getImportePendiente() == 0.0);
    }

    // ============================================================
    // METODOS DE PROCESAMIENTO ESPECIFICO (ASOCIACION Y ESTADOS)
    // ============================================================

    /**
     * Procesa la matricula para un detalle.
     * Extrae el año de la descripcion (ej.: "MATRICULA 2025") y asocia o actualiza la matricula.
     */
    /**
     * Procesa la matrícula para el detalle, extrayendo el año a partir de la descripción,
     * obteniendo o creando la matrícula pendiente y asignándola al detalle.
     * Retorna la matrícula obtenida.
     */
    private Matricula procesarMatricula(Pago pago, DetallePago detalle) {
        log.info("[procesarMatricula] Iniciando procesamiento de matrícula para DetallePago id={}", detalle.getId());

        // Extraer año de la descripción
        String descripcion = detalle.getDescripcionConcepto();
        log.info("[procesarMatricula] Extrayendo año de descripción: {}", descripcion);
        int anio = extraerAnioDeDescripcion(descripcion, detalle.getId());
        log.info("[procesarMatricula] Año extraído: {}", anio);

        // Validar alumno
        Alumno alumno = pago.getAlumno();
        if (alumno == null) {
            log.error("[procesarMatricula] Alumno no definido en el pago para DetallePago id={}", detalle.getId());
            throw new EntityNotFoundException("Alumno no definido en el pago");
        }
        log.info("[procesarMatricula] Procesando matrícula para Alumno id={} y año={}", alumno.getId(), anio);

        // Obtener o crear matrícula pendiente (única llamada)
        Matricula matricula = matriculaServicio.obtenerOMarcarPendienteMatricula(alumno.getId(), anio);
        log.info("[procesarMatricula] Matrícula obtenida: id={}", matricula.getId());

        // Asignar la matrícula al detalle
        detalle.setMatricula(matricula);
        log.info("[procesarMatricula] Se asignó matrícula id={} al DetallePago id={}", matricula.getId(), detalle.getId());

        log.info("[procesarMatricula] Finalizado procesamiento de matrícula para DetallePago id={}", detalle.getId());
        return matricula;
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
        log.info("[determinarTipoDetalle] Descripción normalizada: '{}'", conceptoNorm);

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
        log.info("[determinarTipoDetalle] Verificando existencia en stock");
        if (existeStockConNombre(conceptoNorm)) {
            log.info("[determinarTipoDetalle] Tipo STOCK detectado - Existe en inventario");
            return TipoDetallePago.STOCK;
        } else {
            log.info("[determinarTipoDetalle] No existe coincidencia en stock");
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
        log.info("[reatacharAsociaciones] INICIO: Reatachando asociaciones para DetallePago id={}", detalle.getId());

        // 1. Asegurar que ACobrar tenga un valor válido
        if (detalle.getACobrar() == null) {
            detalle.setACobrar(0.0);
            log.info("[reatacharAsociaciones] Se asignó ACobrar=0.0 para DetallePago id={}", detalle.getId());
        }

        // 2. Reatachar Alumno desde el pago si es necesario
        if (detalle.getAlumno() == null && pago.getAlumno() != null) {
            detalle.setAlumno(pago.getAlumno());
            log.info("[reatacharAsociaciones] Alumno asignado desde pago para DetallePago id={}", detalle.getId());
        }

        // 3. Reatachar Concepto y, si aplica, SubConcepto
        if (detalle.getConcepto() != null && detalle.getConcepto().getId() != null) {
            Concepto managedConcepto = entityManager.find(Concepto.class, detalle.getConcepto().getId());
            if (managedConcepto != null) {
                detalle.setConcepto(managedConcepto);
                log.info("[reatacharAsociaciones] Concepto reatachado: {}", managedConcepto.getId());
                if (detalle.getSubConcepto() == null && managedConcepto.getSubConcepto() != null) {
                    detalle.setSubConcepto(managedConcepto.getSubConcepto());
                    log.info("[reatacharAsociaciones] SubConcepto reatachado: {}", managedConcepto.getSubConcepto().getId());
                }
            } else {
                log.warn("[reatacharAsociaciones] No se encontró Concepto con id={}", detalle.getConcepto().getId());
            }
        }

        // 4. Reatachar Mensualidad, Matrícula y Stock (si existen)
        if (detalle.getDescripcionConcepto().contains("CUOTA") &&
                detalle.getMensualidad() != null &&
                detalle.getMensualidad().getId() != null) {
            Mensualidad managedMensualidad = entityManager.find(Mensualidad.class, detalle.getMensualidad().getId());
            if (managedMensualidad != null) {
                detalle.setMensualidad(managedMensualidad);
                log.info("[reatacharAsociaciones] Mensualidad reatachada: {}", managedMensualidad.getId());
            } else {
                log.warn("[reatacharAsociaciones] No se encontró Mensualidad con id={}", detalle.getMensualidad().getId());
            }
        }

        if (detalle.getMatricula() != null && detalle.getMatricula().getId() != null) {
            Matricula managedMatricula = entityManager.find(Matricula.class, detalle.getMatricula().getId());
            if (managedMatricula != null) {
                detalle.setMatricula(managedMatricula);
                log.info("[reatacharAsociaciones] Matrícula reatachada: {}", managedMatricula.getId());
            } else {
                log.warn("[reatacharAsociaciones] No se encontró Matrícula con id={}", detalle.getMatricula().getId());
            }
        }

        if (detalle.getStock() != null && detalle.getStock().getId() != null) {
            Stock managedStock = entityManager.find(Stock.class, detalle.getStock().getId());
            if (managedStock != null) {
                detalle.setStock(managedStock);
                log.info("[reatacharAsociaciones] Stock reatachado: {}", managedStock.getId());
            } else {
                log.warn("[reatacharAsociaciones] No se encontró Stock con id={}", detalle.getStock().getId());
            }
        }

        log.info("[reatacharAsociaciones] FIN: Reatachamiento completado para DetallePago id={}", detalle);
    }

}

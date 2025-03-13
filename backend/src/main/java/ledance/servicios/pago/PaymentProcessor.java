package ledance.servicios.pago;

import ledance.dto.matricula.request.MatriculaModificacionRequest;
import ledance.dto.matricula.response.MatriculaResponse;
import ledance.dto.mensualidad.response.MensualidadResponse;
import ledance.dto.pago.request.DetallePagoRegistroRequest;
import ledance.dto.pago.request.PagoRegistroRequest;
import ledance.entidades.*;
import ledance.repositorios.*;
import jakarta.transaction.Transactional;
import ledance.servicios.detallepago.DetallePagoServicio;
import ledance.servicios.matricula.MatriculaServicio;
import ledance.servicios.mensualidad.MensualidadServicio;
import ledance.dto.pago.DetallePagoMapper;
import ledance.dto.pago.PagoMapper;
import ledance.dto.pago.PagoMedioMapper;
import ledance.dto.inscripcion.InscripcionMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

@Component
public class PaymentProcessor {

    private static final Logger log = LoggerFactory.getLogger(PaymentProcessor.class);

    private final PagoRepositorio pagoRepositorio;
    private final AlumnoRepositorio alumnoRepositorio;
    private final InscripcionRepositorio inscripcionRepositorio;
    private final MetodoPagoRepositorio metodoPagoRepositorio;
    private final PagoMapper pagoMapper;
    private final MatriculaServicio matriculaServicio;
    private final MensualidadServicio mensualidadServicio;
    private final RecargoRepositorio recargoRepositorio;
    private final BonificacionRepositorio bonificacionRepositorio;
    private final DetallePagoServicio detallePagoServicio;
    private final DetallePagoMapper detallePagoMapper;
    private final PagoMedioMapper pagoMedioMapper;
    private final InscripcionMapper inscripcionMapper;

    public PaymentProcessor(PagoRepositorio pagoRepositorio,
                            AlumnoRepositorio alumnoRepositorio,
                            InscripcionRepositorio inscripcionRepositorio,
                            MetodoPagoRepositorio metodoPagoRepositorio,
                            PagoMapper pagoMapper,
                            MatriculaServicio matriculaServicio,
                            DetallePagoMapper detallePagoMapper,
                            MensualidadServicio mensualidadServicio,
                            RecargoRepositorio recargoRepositorio,
                            BonificacionRepositorio bonificacionRepositorio,
                            DetallePagoServicio detallePagoServicio,
                            PagoMedioMapper pagoMedioMapper,
                            InscripcionMapper inscripcionMapper) {
        this.pagoRepositorio = pagoRepositorio;
        this.alumnoRepositorio = alumnoRepositorio;
        this.inscripcionRepositorio = inscripcionRepositorio;
        this.metodoPagoRepositorio = metodoPagoRepositorio;
        this.pagoMapper = pagoMapper;
        this.matriculaServicio = matriculaServicio;
        this.detallePagoMapper = detallePagoMapper;
        this.mensualidadServicio = mensualidadServicio;
        this.recargoRepositorio = recargoRepositorio;
        this.bonificacionRepositorio = bonificacionRepositorio;
        this.detallePagoServicio = detallePagoServicio;
        this.pagoMedioMapper = pagoMedioMapper;
        this.inscripcionMapper = inscripcionMapper;
    }

    // ---------------------- Creación de Pagos -----------------------

    // 1. Pago GENERAL (sin inscripción)
    Pago processGeneralPayment(PagoRegistroRequest request) {
        log.info("[processGeneralPayment] Iniciando procesamiento de pago GENERAL (sin inscripción).");
        Pago pago = new Pago();
        pago.setFecha(request.fecha());
        pago.setFechaVencimiento(request.fechaVencimiento());
        pago.setRecargoAplicado(Boolean.TRUE.equals(request.recargoAplicado()));
        pago.setBonificacionAplicada(Boolean.TRUE.equals(request.bonificacionAplicada()));
        pago.setActivo(true);
        pago.setInscripcion(null);
        pago.setTipoPago(TipoPago.GENERAL);

        // Obtener alumno
        Long alumnoId = request.alumno().id();
        Alumno alumno = alumnoRepositorio.findById(alumnoId)
                .orElseThrow(() -> new IllegalArgumentException("Alumno no encontrado para pago general."));
        pago.setAlumno(alumno);

        // Inicializar saldos (se asignarán luego según detalles)
        pago.setSaldoAFavor(0.0);

        // Asignar método de pago si se envía
        if (request.metodoPagoId() != null) {
            MetodoPago metodo = metodoPagoRepositorio.findById(request.metodoPagoId())
                    .orElseThrow(() -> new IllegalArgumentException("Método de pago no encontrado."));
            pago.setMetodoPago(metodo);
        }

        // Obtener nuevos detalles (sin ID) y procesarlos
        // Procesar cada detalle:
        List<DetallePago> detallesNuevos = obtenerDetallesNuevos(request, pago);
        for (DetallePago det : detallesNuevos) {
            // Si el DTO no envía aCobrar, se asume que es igual al valorBase.
            double abono = (det.getaCobrar() != null && det.getaCobrar() > 0) ? det.getaCobrar() : det.getValorBase();
            // Se calcula el importeInicial (usando calcularImporte ya lo establece)
            // Se ajusta para que no supere el importeInicial:
            if (abono > det.getImporteInicial()) {
                abono = det.getImporteInicial();
            }
            det.setaCobrar(abono);
            double pendiente = det.getImporteInicial() - abono;
            det.setImportePendiente(pendiente);
            det.setCobrado(pendiente == 0.0);
        }
        pago.setDetallePagos(detallesNuevos);

// Monto del pago: la suma de los aCobrar de cada detalle.
        double montoTotal = detallesNuevos.stream()
                .mapToDouble(DetallePago::getaCobrar)
                .sum();
        pago.setMonto(montoTotal);
// El saldo restante es la suma de los importesPendientes.
        double saldoRestante = detallesNuevos.stream()
                .mapToDouble(DetallePago::getImportePendiente)
                .sum();
        pago.setSaldoRestante(saldoRestante);

// Y asegúrate de asignar explícitamente el tipo GENERAL:
        pago.setTipoPago(TipoPago.GENERAL);

        // Persistir y actualizar importes (actualizarImportesPago recalcula montoPagado)
        Pago guardado = pagoRepositorio.save(pago);
        actualizarImportesPago(guardado);
        guardado = pagoRepositorio.save(guardado);
        procesarPagosEspecificos(guardado);

        log.info("[processGeneralPayment] Pago GENERAL procesado: id={}, monto={}, saldoRestante={}",
                guardado.getId(), guardado.getMonto(), guardado.getSaldoRestante());
        return guardado;
    }

    @Transactional
    public Pago processFirstPayment(PagoRegistroRequest request, Inscripcion inscripcion, Alumno alumno) {
        log.info("[processFirstPayment] Iniciando primer pago para inscripción id={} y alumno id={}",
                inscripcion.getId(), alumno.getId());

        // Crear pago base. Se inicializa con monto 0 y se actualizará luego.
        Pago pago = crearPagoBase(request, alumno, inscripcion);
        log.debug("[processFirstPayment] Pago base creado: {}", pago);

        // Mapear los detalles del request al nuevo pago
        List<DetallePago> detalles = request.detallePagos().stream()
                .map(detDTO -> {
                    DetallePago det = detallePagoMapper.toEntity(detDTO);
                    det.setId(null); // Asegurarse de que la entidad se considere nueva
                    det.setPago(pago);

                    if (det.getaCobrar() == null) {
                        det.setaCobrar(0.0);
                        log.debug("[processFirstPayment] No se recibió aCobrar; asignando 0.0 para detalle: {}", det);
                    }
                    double base = det.getValorBase();
                    double descuento = detallePagoServicio.calcularDescuento(det, base);
                    double recargo = (det.getRecargo() != null)
                            ? detallePagoServicio.obtenerValorRecargo(det, base)
                            : 0.0;
                    double importeInicial = base - descuento + recargo;
                    log.info("[processFirstPayment] Para detalle con concepto '{}' - Valor base: {}, descuento: {}, recargo: {} => Importe inicial calculado = {}",
                            det.getConcepto(), base, descuento, recargo, importeInicial);
                    det.setImporteInicial(importeInicial);
                    double importePendiente = importeInicial - det.getaCobrar();
                    if (importePendiente < 0) {
                        log.warn("[processFirstPayment] Importe pendiente negativo para detalle con concepto '{}'. Se asigna 0.", det.getConcepto());
                        importePendiente = 0;
                    }
                    det.setImportePendiente(importePendiente);
                    log.debug("[processFirstPayment] Detalle final procesado: {}", det);
                    return det;
                })
                .filter(det -> det.getImportePendiente() != null && det.getImportePendiente() >= 0)
                .collect(Collectors.toList());
        pago.setDetallePagos(detalles);
        log.info("[processFirstPayment] Detalles asignados al pago: {}", detalles);

        // Calcular el monto total del pago como la suma de los aCobrar de cada detalle
        double montoTotal = detalles.stream().mapToDouble(det -> det.getaCobrar() != null ? det.getaCobrar() : 0.0).sum();
        pago.setMonto(montoTotal);
        log.info("[processFirstPayment] Monto total del pago calculado = {}", montoTotal);

        // Antes de persistir, loguear estado completo del objeto Pago
        log.debug("[processFirstPayment] Estado del pago antes de guardar: {}", pago);
        for (DetallePago det : pago.getDetallePagos()) {
            log.debug("  -> DetallePago: ID={}, Concepto={}, aCobrar={}, ImporteInicial={}, ImportePendiente={}, Cobrado={}",
                    det.getId(), det.getConcepto(), det.getaCobrar(), det.getImporteInicial(), det.getImportePendiente(), det.getCobrado());
        }

        // Guardar el pago y actualizar importes
        Pago guardado = pagoRepositorio.save(pago);
        log.info("[processFirstPayment] Pago guardado por primera vez: id={}, monto={}, saldoRestante={}",
                guardado.getId(), guardado.getMonto(), guardado.getSaldoRestante());
        actualizarImportesPago(guardado);
        // Vuelvo a guardar para persistir cambios de importes
        guardado = pagoRepositorio.save(guardado);
        log.info("[processFirstPayment] Pago guardado tras actualizar importes: id={}, monto={}, saldoRestante={}, montoPagado={}",
                guardado.getId(), guardado.getMonto(), guardado.getSaldoRestante(), guardado.getMontoPagado());
        procesarPagosEspecificos(guardado);
        log.info("[processFirstPayment] Primer pago procesado: id={}, monto={}, saldoRestante={}",
                guardado.getId(), guardado.getMonto(), guardado.getSaldoRestante());

        return guardado;
    }

    private void marcarDetallesCobradosSiImporteEsCero(Pago pago) {
        log.info("[marcarDetallesCobradosSiImporteEsCero] Revisando detalles para pago id={}", pago.getId());
        if (pago.getDetallePagos() != null) {
            pago.getDetallePagos().forEach(detalle -> {
                if (detalle.getImportePendiente() != null && detalle.getImportePendiente() == 0.0) {
                    detalle.setCobrado(true);
                    log.info("[marcarDetallesCobradosSiImporteEsCero] Detalle id={} marcado como COBRADO (importePendiente=0).", detalle.getId());
                } else {
                    log.debug("[marcarDetallesCobradosSiImporteEsCero] Detalle id={} con importePendiente={}", detalle.getId(), detalle.getImportePendiente());
                }
            });
        } else {
            log.warn("[marcarDetallesCobradosSiImporteEsCero] Pago id={} no tiene detalles.", pago.getId());
        }
        pagoRepositorio.save(pago);
    }

    private static void verificarSaldoRestante(Pago pagoFinal) {
        if (pagoFinal.getSaldoRestante() < 0) {
            log.error("Error: Saldo restante negativo detectado en pago ID={}. Ajustando a 0.", pagoFinal.getId());
            pagoFinal.setSaldoRestante(0.0);
        }
    }

    // 3. Pago con pago previo (actualización parcial)
    public Pago processPaymentWithPrevious(PagoRegistroRequest request, Inscripcion inscripcion, Alumno alumno, Pago pagoExistente) {
        log.info("[processPaymentWithPrevious] Actualizando pago existente id={} para alumno id={}",
                pagoExistente.getId(), alumno.getId());
        // Actualiza campos comunes
        actualizarCamposPago(pagoExistente, request, alumno, inscripcion);

        // Nuevo abono que se aplicará a cada detalle existente
        double nuevoAbono = request.monto();
        if (nuevoAbono < 0) {
            throw new IllegalArgumentException("El monto del abono no puede ser negativo.");
        }

        // Actualizar detalles existentes: descontar del importePendiente
        pagoExistente.getDetallePagos().forEach(det -> {
            double pendienteActual = det.getImportePendiente();
            double nuevoPendiente = pendienteActual - nuevoAbono;
            if (nuevoPendiente < 0) {
                nuevoPendiente = 0;
            }
            log.info("[processPaymentWithPrevious] Detalle id={} | Pendiente actual: {} | Nuevo abono: {} | Nuevo pendiente: {}",
                    det.getId(), pendienteActual, nuevoAbono, nuevoPendiente);
            det.setImportePendiente(nuevoPendiente);
            if (nuevoPendiente == 0) {
                det.setCobrado(true);
                log.info("[processPaymentWithPrevious] Detalle id={} marcado como COBRADO.", det.getId());
            } else {
                det.setCobrado(false);
                log.info("[processPaymentWithPrevious] Detalle id={} NO se marca como COBRADO.", det.getId());
            }
        });

        // Procesar nuevos detalles (los que no tienen ID)
        List<DetallePago> nuevosDetalles = request.detallePagos().stream()
                .filter(dto -> dto.id() == null)
                .map(dto -> {
                    DetallePago nuevo = detallePagoMapper.toEntity(dto);
                    nuevo.setPago(pagoExistente);
                    // Calcular el importe inicial para el nuevo detalle
                    double importeInicial = nuevo.getValorBase();
                    if (nuevo.getRecargo() != null) {
                        importeInicial += detallePagoServicio.obtenerValorRecargo(nuevo, nuevo.getValorBase());
                    }
                    double descuento = detallePagoServicio.calcularDescuento(nuevo, nuevo.getValorBase());
                    if (descuento > 0) {
                        importeInicial -= descuento;
                    }
                    nuevo.setImporteInicial(importeInicial);
                    // Aplicar el abono (si se envía) sobre el importe inicial para obtener el importe pendiente
                    double abono = (dto.aCobrar() != null && dto.aCobrar() > 0) ? dto.aCobrar() : 0;
                    double pendiente = Math.max(importeInicial - abono, 0);
                    nuevo.setImportePendiente(pendiente);
                    nuevo.setCobrado(pendiente == 0);
                    return nuevo;
                }).collect(Collectors.toList());
        if (!nuevosDetalles.isEmpty()) {
            log.info("[processPaymentWithPrevious] Nuevos detalles a agregar: {}", nuevosDetalles);
            pagoExistente.getDetallePagos().addAll(nuevosDetalles);
        }

        actualizarImportesPago(pagoExistente);
        procesarPagosEspecificos(pagoExistente);
        Pago guardado = pagoRepositorio.save(pagoExistente);
        log.info("[processPaymentWithPrevious] Pago actualizado: id={}, monto={}, saldoRestante={}",
                guardado.getId(), guardado.getMonto(), guardado.getSaldoRestante());
        return guardado;
    }

    private Pago crearPagoBase(PagoRegistroRequest request, Alumno alumno, Inscripcion inscripcion) {
        log.info("[crearPagoBase] Creando pago base para alumno {} y inscripción {}",
                alumno.getId(), (inscripcion != null ? inscripcion.getId() : "N/A"));
        Pago pago = new Pago();
        pago.setFecha(request.fecha());
        pago.setFechaVencimiento(request.fechaVencimiento());
        pago.setMonto(request.monto());
        pago.setAlumno(alumno);
        pago.setInscripcion(inscripcion);
        pago.setSaldoRestante(request.monto());
        pago.setSaldoAFavor(0.0);
        pago.setActivo(true);
        if (request.metodoPagoId() != null) {
            MetodoPago metodo = metodoPagoRepositorio.findById(request.metodoPagoId())
                    .orElseThrow(() -> new IllegalArgumentException("Método de pago no encontrado."));
            pago.setMetodoPago(metodo);
        }
        return pago;
    }

    // Actualiza los campos comunes de un pago existente
    private void actualizarCamposPago(Pago pago, PagoRegistroRequest request, Alumno alumno, Inscripcion inscripcion) {
        log.info("[actualizarCamposPago] Actualizando datos principales para pago id={}", pago.getId());
        pago.setFecha(request.fecha());
        pago.setFechaVencimiento(request.fechaVencimiento());
        pago.setRecargoAplicado(Boolean.TRUE.equals(request.recargoAplicado()));
        pago.setBonificacionAplicada(Boolean.TRUE.equals(request.bonificacionAplicada()));
        pago.setAlumno(alumno);
        pago.setInscripcion(inscripcion);
        pago.setActivo(true);
        if (request.metodoPagoId() != null) {
            MetodoPago metodo = metodoPagoRepositorio.findById(request.metodoPagoId())
                    .orElseThrow(() -> new IllegalArgumentException("Método de pago no encontrado."));
            pago.setMetodoPago(metodo);
        }
        pago.setMonto(request.monto());
        pago.setSaldoAFavor(0.0);
    }

    // Obtiene nuevos detalles a partir del request (los que no tengan ID)
    private List<DetallePago> obtenerDetallesNuevos(PagoRegistroRequest request, Pago pago) {
        List<DetallePago> nuevos = request.detallePagos().stream()
                .filter(detDTO -> detDTO.id() == null)
                .map(detDTO -> {
                    DetallePago det = detallePagoMapper.toEntity(detDTO);

                    // Si aCobrar no se envía, se asigna 0.0
                    if (det.getaCobrar() == null) {
                        det.setaCobrar(0.0);
                        log.debug("[obtenerDetallesNuevos] No se recibió aCobrar; asignando 0.0 para detalle con valorBase={}", det.getValorBase());
                    }

                    // Asignar el pago al detalle
                    det.setPago(pago);

                    // Calcular importeInicial y, a partir de este, el importePendiente
                    // Aquí se asume que importeInicial es igual a valorBase; si existe lógica adicional (descuentos/recargos), aplicarla aquí.
                    if (det.getValorBase() != null) {
                        double importeInicial = det.getValorBase(); // Puedes ajustar este cálculo según tus reglas de negocio.
                        det.setImporteInicial(importeInicial);
                        // El importe pendiente se calcula restando lo que se abona (aCobrar) al importe inicial.
                        det.setImportePendiente(importeInicial - det.getaCobrar());
                    }
                    return det;
                })
                .filter(det -> det.getValorBase() != null && det.getValorBase() > 0)
                .collect(Collectors.toCollection(ArrayList::new));
        log.debug("[obtenerDetallesNuevos] Nuevos detalles obtenidos: {}", nuevos);
        return nuevos;
    }

    public void actualizarImportesPago(Pago pago) {
        // Recalcular cada detalle para actualizar importeInicial e importePendiente.
        pago.getDetallePagos().forEach(detallePagoServicio::calcularImporte);

        // Sumar los importes pendientes de cada detalle para obtener el saldo restante.
        double totalPendiente = pago.getDetallePagos().stream()
                .mapToDouble(DetallePago::getImportePendiente)
                .sum();
        pago.setSaldoRestante(totalPendiente);

        // Calcular el monto abonado como la diferencia entre la suma de los importes iniciales y el saldo restante.
        double totalInicial = pago.getDetallePagos().stream()
                .mapToDouble(DetallePago::getImporteInicial)
                .sum();
        pago.setMontoPagado(totalInicial - totalPendiente);

        // Verificar que el saldo restante no sea negativo y guardar el pago.
        verificarSaldoRestante(pago);
        pagoRepositorio.save(pago);
    }

    // Procesa los detalles según el tipo (MATRICULA, MENSUALIDAD, GENERAL)
    public void procesarPagosEspecificos(Pago pago) {
        log.info("[procesarPagosEspecificos] Iniciando procesamiento de detalles para pago id={}", pago.getId());
        if (pago.getDetallePagos() != null) {
            for (DetallePago detalle : pago.getDetallePagos()) {
                if (detalle.getTipo() == null) {
                    if (detalle.getConcepto() != null) {
                        String concepto = detalle.getConcepto().trim().toUpperCase();
                        log.debug("[procesarPagosEspecificos] Concepto normalizado: '{}'", concepto);
                        if (concepto.startsWith("MATRICULA")) {
                            procesarMatricula(pago, detalle);
                        } else if (concepto.contains("CUOTA")) {
                            procesarMensualidad(pago, detalle);
                        } else {
                            procesarDetalleGeneral(pago, detalle);
                        }
                    } else {
                        log.warn("[procesarPagosEspecificos] Detalle id={} sin concepto, se omite", detalle.getId());
                    }
                } else {
                    switch (detalle.getTipo()) {
                        case MATRICULA:
                            procesarMatricula(pago, detalle);
                            break;
                        case MENSUALIDAD:
                            procesarMensualidad(pago, detalle);
                            break;
                        case GENERAL:
                        default:
                            procesarDetalleGeneral(pago, detalle);
                            break;
                    }
                }
            }
        } else {
            log.warn("[procesarPagosEspecificos] El pago id={} no contiene detalles", pago.getId());
        }
        log.info("[procesarPagosEspecificos] Finalizado procesamiento de detalles para pago id={}", pago.getId());
    }

    // Procesamiento para matrícula (ejemplo sin cambios significativos)
    private void procesarMatricula(Pago pago, DetallePago detalle) {
        log.info("[procesarMatricula] Iniciando procesamiento de matrícula para detalle id={}", detalle.getId());
        if (pago.getAlumno() != null) {
            String[] partes = detalle.getConcepto().split(" ");
            log.debug("[procesarMatricula] Partes del concepto: {}", Arrays.toString(partes));
            if (partes.length >= 2) {
                try {
                    int anio = Integer.parseInt(partes[1]);
                    log.info("[procesarMatricula] Procesando matrícula para el año {}", anio);
                    MatriculaResponse matResp = matriculaServicio.obtenerOMarcarPendiente(pago.getAlumno().getId());
                    log.debug("[procesarMatricula] Matrícula pendiente encontrada: {}", matResp);
                    MatriculaModificacionRequest modRequest = new MatriculaModificacionRequest(matResp.anio(), true, pago.getFecha());
                    matriculaServicio.actualizarEstadoMatricula(matResp.id(), modRequest);
                } catch (NumberFormatException e) {
                    log.warn("[procesarMatricula] Error al extraer el año del concepto '{}': {}", detalle.getConcepto(), e.getMessage());
                }
            } else {
                log.warn("[procesarMatricula] El concepto '{}' no tiene el formato esperado para matrícula", detalle.getConcepto());
            }
        } else {
            log.warn("[procesarMatricula] No se pudo procesar matrícula, pago sin alumno asignado.");
        }
        log.info("[procesarMatricula] Finalizado procesamiento de matrícula para detalle id={}", detalle.getId());
    }

    // Procesamiento para mensualidad
    private void procesarMensualidad(Pago pago, DetallePago detalle) {
        log.info("[procesarMensualidad] Iniciando procesamiento de mensualidad para detalle id={}", detalle.getId());
        if (pago.getInscripcion() != null) {
            String descripcionDetalle = detalle.getConcepto().trim();
            MensualidadResponse mensPendiente = mensualidadServicio.buscarMensualidadPendientePorDescripcion(pago.getInscripcion(), descripcionDetalle);
            if (mensPendiente != null) {
                double montoAbonadoAcumulado = pago.getDetallePagos().stream()
                        .filter(d -> d.getConcepto().trim().equalsIgnoreCase(descripcionDetalle))
                        .mapToDouble(DetallePago::getaCobrar)
                        .sum();
                log.info("[procesarMensualidad] Mensualidad id={} ('{}'): monto abonado acumulado={} y total a pagar={}",
                        mensPendiente.id(), descripcionDetalle, montoAbonadoAcumulado, mensPendiente.totalPagar());
                if (montoAbonadoAcumulado >= mensPendiente.totalPagar()) {
                    log.info("[procesarMensualidad] Condición cumplida: {} >= {}. Marcando mensualidad id={} como PAGADO.",
                            montoAbonadoAcumulado, mensPendiente.totalPagar(), mensPendiente.id());
                    mensualidadServicio.marcarComoPagada(mensPendiente.id(), pago.getFecha());
                } else {
                    log.info("[procesarMensualidad] Abono parcial: {} < {}. Actualizando abono parcial para mensualidad id={}.",
                            montoAbonadoAcumulado, mensPendiente.totalPagar(), mensPendiente.id());
                    mensualidadServicio.actualizarAbonoParcial(mensPendiente.id(), montoAbonadoAcumulado);
                }
            } else {
                log.info("[procesarMensualidad] No se encontró mensualidad pendiente para '{}'. Se creará una nueva.",
                        descripcionDetalle);
                mensualidadServicio.crearMensualidadPagada(pago.getInscripcion().getId(), descripcionDetalle, pago.getFecha());
            }
        } else {
            log.info("[procesarMensualidad] Pago GENERAL: se omite procesamiento de mensualidades sin inscripción.");
        }
    }

    // Procesamiento para detalle general (sin lógica adicional)
    private void procesarDetalleGeneral(Pago pago, DetallePago detalle) {
        log.info("[procesarDetalleGeneral] Iniciando procesamiento de detalle general para detalle id={} con concepto '{}'",
                detalle.getId(), detalle.getConcepto());
        log.debug("[procesarDetalleGeneral] Detalle general sin procesamiento adicional. Se registra tal cual.");
        log.info("[procesarDetalleGeneral] Finalizado procesamiento de detalle general para detalle id={}", detalle.getId());
    }

    @Transactional
    public Pago actualizarCobranzaHistorica(Long pagoId, PagoRegistroRequest request) {
        // 1. Obtener el pago histórico (aún con saldo pendiente)
        Pago historico = pagoRepositorio.findById(pagoId)
                .orElseThrow(() -> new IllegalArgumentException("Pago no encontrado para actualización."));
        if (historico.getSaldoRestante() == 0) {
            throw new IllegalArgumentException("El pago ya está saldado y no se puede actualizar como histórico.");
        }
        log.info("[actualizarCobranzaHistorica] Procesando pago histórico: id={}, monto={}, saldoRestante={}",
                historico.getId(), historico.getMonto(), historico.getSaldoRestante());

        // 2. Mapear los nuevos abonos enviados en el request por concepto (se normaliza el string)
        Map<String, DetallePagoRegistroRequest> nuevosAbonos = request.detallePagos().stream()
                .collect(Collectors.toMap(dto -> dto.concepto().trim().toLowerCase(), Function.identity()));

        List<DetallePago> detallesHistoricoActualizados = new ArrayList<>();
        List<DetallePago> detallesNuevoPago = new ArrayList<>();

        // 3. Procesar cada detalle del pago histórico
        for (DetallePago detalle : historico.getDetallePagos()) {
            String conceptoKey = detalle.getConcepto().trim().toLowerCase();
            DetallePagoRegistroRequest dto = nuevosAbonos.get(conceptoKey);
            if (dto == null) {
                throw new IllegalArgumentException("No se encontró abono actualizado para el concepto: " + detalle.getConcepto());
            }

            double abonoAplicado = dto.aCobrar();
            double pendienteActual = detalle.getImportePendiente();
            if (abonoAplicado > pendienteActual) {
                abonoAplicado = pendienteActual;
            }

            double remanente = pendienteActual - abonoAplicado;

            // Actualizar el detalle histórico: se cierra el detalle
            detalle.setaCobrar(abonoAplicado);
            detalle.setImportePendiente(0.0);
            detalle.setCobrado(true);
            detallesHistoricoActualizados.add(detalle);

            // Clonar el detalle para el nuevo pago
            DetallePago detalleNuevo = new DetallePago();
            detalleNuevo.setId(null); // Se asegura que se cree una nueva entidad
            detalleNuevo.setCodigoConcepto(detalle.getCodigoConcepto());
            detalleNuevo.setConcepto(detalle.getConcepto());
            detalleNuevo.setCuota(detalle.getCuota());
            // Reutilización de asociaciones (o clonarlas si fuera necesario)
            detalleNuevo.setBonificacion(detalle.getBonificacion());
            detalleNuevo.setRecargo(detalle.getRecargo());
            detalleNuevo.setAFavor(detalle.getAFavor());
            detalleNuevo.setValorBase(detalle.getValorBase());
            detalleNuevo.setTipo(detalle.getTipo());

            if (remanente > 0) {
                detalleNuevo.setImporteInicial(remanente);
                detalleNuevo.setImportePendiente(remanente);
                detalleNuevo.setaCobrar(remanente);
                detalleNuevo.setCobrado(false);
            } else {
                detalleNuevo.setImporteInicial(abonoAplicado);
                detalleNuevo.setImportePendiente(0.0);
                detalleNuevo.setaCobrar(abonoAplicado);
                detalleNuevo.setCobrado(true);
            }
            detallesNuevoPago.add(detalleNuevo);
        }

        // 4. Actualizar y cerrar el pago histórico
        historico.getDetallePagos().clear();
        historico.getDetallePagos().addAll(detallesHistoricoActualizados);
        historico.setSaldoRestante(0.0);
        historico.setActivo(false);
        pagoRepositorio.save(historico);
        log.info("[actualizarCobranzaHistorica] Pago histórico cerrado: id={}", historico.getId());

        // 5. Crear el nuevo pago usando los detalles clonados
        double montoNuevo = detallesNuevoPago.stream()
                .mapToDouble(d -> d.getaCobrar() != null ? d.getaCobrar() : 0.0)
                .sum();
        Pago nuevoPago = new Pago();
        nuevoPago.setFecha(request.fecha());
        nuevoPago.setFechaVencimiento(request.fechaVencimiento());
        nuevoPago.setAlumno(historico.getAlumno());
        nuevoPago.setInscripcion(historico.getInscripcion());
        nuevoPago.setMetodoPago(historico.getMetodoPago());
        nuevoPago.setRecargoAplicado(historico.getRecargoAplicado());
        nuevoPago.setBonificacionAplicada(historico.getBonificacionAplicada());
        nuevoPago.setTipoPago(historico.getTipoPago());
        nuevoPago.setObservaciones(historico.getObservaciones());
        nuevoPago.setActivo(true);
        nuevoPago.setSaldoAFavor(0.0);

        detallesNuevoPago.forEach(d -> d.setPago(nuevoPago));
        nuevoPago.setDetallePagos(detallesNuevoPago);
        nuevoPago.setMonto(montoNuevo);
        nuevoPago.setSaldoRestante(montoNuevo);

        Pago guardadoNuevo = pagoRepositorio.save(nuevoPago);
        actualizarImportesPago(guardadoNuevo);
        log.info("[actualizarCobranzaHistorica] Nuevo pago generado: id={}, montoNuevo={}, saldoRestante={}",
                guardadoNuevo.getId(), guardadoNuevo.getMonto(), guardadoNuevo.getSaldoRestante());
        return guardadoNuevo;
    }

    // Método auxiliar para decidir el flujo según los conceptos existentes
    private boolean esPagoHistoricoAplicable(Pago ultimoPendiente, PagoRegistroRequest request) {
        if (ultimoPendiente == null) return false;
        return request.detallePagos().stream().allMatch(dto ->
                ultimoPendiente.getDetallePagos().stream().anyMatch(det ->
                        det.getConcepto().trim().equalsIgnoreCase(dto.concepto().trim())
                )
        );
    }
    // Método que decide qué flujo ejecutar según la inscripción y los conceptos
    Pago crearPagoSegunInscripcion(PagoRegistroRequest request) {
        if (request.inscripcion().id() == null || request.inscripcion().id() == -1) {
            return processGeneralPayment(request);
        }

        Inscripcion inscripcion = inscripcionRepositorio.findById(request.inscripcion().id())
                .orElseThrow(() -> new IllegalArgumentException("Inscripción no encontrada."));
        Alumno alumno = inscripcion.getAlumno();

        Pago ultimoPendiente = obtenerUltimoPagoPendienteEntidad(alumno.getId());
        if (esPagoHistoricoAplicable(ultimoPendiente, request)) {
            log.info("Se detectó un pago histórico existente; se procederá a actualizarlo.");
            return actualizarCobranzaHistorica(ultimoPendiente.getId(), request);
        } else {
            log.info("No se cumple la condición para actualizar el histórico; se procesa como nuevo pago.");
            return processFirstPayment(request, inscripcion, alumno);
        }
    }

    Pago obtenerUltimoPagoPendienteEntidad(Long alumnoId) {
        return pagoRepositorio.findTopByAlumnoIdAndActivoTrueOrderByFechaDesc(alumnoId).orElse(null);
    }
}

package ledance.servicios.pago;

import ledance.dto.matricula.request.MatriculaModificacionRequest;
import ledance.dto.matricula.response.MatriculaResponse;
import ledance.dto.mensualidad.response.MensualidadResponse;
import ledance.dto.pago.request.PagoRegistroRequest;
import ledance.entidades.Alumno;
import ledance.entidades.DetallePago;
import ledance.entidades.Inscripcion;
import ledance.entidades.MetodoPago;
import ledance.entidades.Pago;
import ledance.entidades.TipoPago;
import ledance.dto.pago.DetallePagoMapper;
import ledance.repositorios.AlumnoRepositorio;
import ledance.repositorios.InscripcionRepositorio;
import ledance.repositorios.MetodoPagoRepositorio;
import ledance.repositorios.PagoRepositorio;
import ledance.servicios.detallepago.DetallePagoServicio;
import ledance.servicios.matricula.MatriculaServicio;
import ledance.servicios.mensualidad.MensualidadServicio;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Component
public class PaymentProcessor {

    private static final Logger log = LoggerFactory.getLogger(PaymentProcessor.class);

    private final PagoRepositorio pagoRepositorio;
    private final AlumnoRepositorio alumnoRepositorio;
    private final MetodoPagoRepositorio metodoPagoRepositorio;
    private final DetallePagoMapper detallePagoMapper;
    private final MatriculaServicio matriculaServicio;
    private final MensualidadServicio mensualidadServicio;
    private final DetallePagoServicio detallePagoServicio;

    public PaymentProcessor(PagoRepositorio pagoRepositorio,
                            AlumnoRepositorio alumnoRepositorio,
                            InscripcionRepositorio inscripcionRepositorio,
                            MetodoPagoRepositorio metodoPagoRepositorio,
                            DetallePagoMapper detallePagoMapper,
                            MatriculaServicio matriculaServicio,
                            MensualidadServicio mensualidadServicio,
                            DetallePagoServicio detallePagoServicio) {
        this.pagoRepositorio = pagoRepositorio;
        this.alumnoRepositorio = alumnoRepositorio;
        this.metodoPagoRepositorio = metodoPagoRepositorio;
        this.detallePagoMapper = detallePagoMapper;
        this.matriculaServicio = matriculaServicio;
        this.mensualidadServicio = mensualidadServicio;
        this.detallePagoServicio = detallePagoServicio;
    }

    public Pago processGeneralPayment(PagoRegistroRequest request) {
        log.info("[PaymentProcessor] Procesando pago GENERAL (sin inscripción).");
        Pago pago = new Pago();
        pago.setFecha(request.fecha());
        pago.setFechaVencimiento(request.fechaVencimiento());
        pago.setRecargoAplicado(Boolean.TRUE.equals(request.recargoAplicado()));
        pago.setBonificacionAplicada(Boolean.TRUE.equals(request.bonificacionAplicada()));
        pago.setActivo(true);
        pago.setInscripcion(null);
        pago.setTipoPago(TipoPago.GENERAL);

        Long alumnoId = obtenerAlumnoIdParaPagoGeneral(request);
        Alumno alumno = alumnoRepositorio.findById(alumnoId)
                .orElseThrow(() -> new IllegalArgumentException("Alumno no encontrado para pago general."));
        pago.setAlumno(alumno);

        pago.setMonto(request.monto());
        pago.setSaldoRestante(request.monto());
        pago.setSaldoAFavor(0.0);

        if (request.metodoPagoId() != null) {
            MetodoPago metodo = metodoPagoRepositorio.findById(request.metodoPagoId())
                    .orElseThrow(() -> new IllegalArgumentException("Método de pago no encontrado."));
            pago.setMetodoPago(metodo);
        }

        List<DetallePago> detallesNuevos = obtenerDetallesNuevos(request, pago);
        pago.setDetallePagos(detallesNuevos);

        Pago guardado = pagoRepositorio.save(pago);
        log.info("[PaymentProcessor] Pago general creado: id={}, monto={}, saldoRestante={}", guardado.getId(), guardado.getMonto(), guardado.getSaldoRestante());
        actualizarImportesPago(guardado);
        guardado = pagoRepositorio.save(guardado);
        procesarPagosEspecificos(guardado);
        return guardado;
    }

    private Long obtenerAlumnoIdParaPagoGeneral(PagoRegistroRequest request) {
        // En producción, este dato debería venir en el DTO o extraerse del contexto.
        return 1L;
    }

    private List<DetallePago> obtenerDetallesNuevos(PagoRegistroRequest request, Pago pago) {
        return request.detallePagos().stream()
                .filter(detDTO -> detDTO.id() == null)
                .map(detDTO -> {
                    DetallePago det = detallePagoMapper.toEntity(detDTO);
                    if (det.getaCobrar() == null) {
                        det.setaCobrar(det.getValorBase());
                    }
                    det.setPago(pago);
                    return det;
                })
                .filter(det -> det.getValorBase() != null && det.getValorBase() > 0)
                .collect(Collectors.toCollection(ArrayList::new));
    }

    public Pago processPaymentWithPrevious(PagoRegistroRequest request, Inscripcion inscripcion, Alumno alumno, Pago pagoAnterior) {
        log.info("[PaymentProcessor] Procesando pago con anterior (id {}), para alumno {}.", pagoAnterior.getId(), alumno.getId());
        pagoAnterior.setActivo(false);
        pagoRepositorio.save(pagoAnterior);
        log.info("[PaymentProcessor] Pago anterior inactivado: id={}", pagoAnterior.getId());

        Pago nuevoPago = new Pago();
        nuevoPago.setFecha(request.fecha());
        nuevoPago.setFechaVencimiento(request.fechaVencimiento());
        nuevoPago.setRecargoAplicado(Boolean.TRUE.equals(request.recargoAplicado()));
        nuevoPago.setBonificacionAplicada(Boolean.TRUE.equals(request.bonificacionAplicada()));
        nuevoPago.setAlumno(alumno);
        nuevoPago.setInscripcion(inscripcion);
        nuevoPago.setActivo(true);
        if (request.metodoPagoId() != null) {
            MetodoPago metodo = metodoPagoRepositorio.findById(request.metodoPagoId())
                    .orElseThrow(() -> new IllegalArgumentException("Método de pago no encontrado."));
            nuevoPago.setMetodoPago(metodo);
        }
        nuevoPago.setMonto(request.monto());
        nuevoPago.setSaldoAFavor(0.0);
        nuevoPago.setSaldoRestante(0.0);

        List<DetallePago> detallesArrastrados = pagoAnterior.getDetallePagos().stream()
                .filter(dp -> dp.getImporte() != null && dp.getImporte() > 0)
                .map(dp -> {
                    log.info("Detalle anterior id={} pendiente importe={}", dp.getId(), dp.getImporte());
                    DetallePago nuevoDet = new DetallePago();
                    nuevoDet.setConcepto(dp.getConcepto());
                    nuevoDet.setCodigoConcepto(dp.getCodigoConcepto());
                    nuevoDet.setCuota(dp.getCuota());
                    nuevoDet.setValorBase(dp.getValorBase());
                    nuevoDet.setImporte(dp.getImporte());
                    nuevoDet.setaCobrar(dp.getaCobrar());
                    nuevoDet.setAbono(0.0);
                    nuevoDet.setPago(nuevoPago);
                    return nuevoDet;
                })
                .collect(Collectors.toCollection(ArrayList::new));

        List<DetallePago> detallesNuevos = obtenerDetallesNuevos(request, nuevoPago);
        List<DetallePago> todosDetalles = new ArrayList<>();
        todosDetalles.addAll(detallesArrastrados);
        todosDetalles.addAll(detallesNuevos);
        nuevoPago.setDetallePagos(todosDetalles);

        Pago guardado = pagoRepositorio.save(nuevoPago);
        actualizarImportesPago(guardado);
        guardado = pagoRepositorio.save(guardado);
        procesarPagosEspecificos(guardado);
        log.info("[PaymentProcessor] Nuevo pago creado: id={}, monto={}, saldoRestante={}", nuevoPago.getId(), nuevoPago.getMonto(), nuevoPago.getSaldoRestante());

        return guardado;
    }

    public Pago processFirstPayment(PagoRegistroRequest request, Inscripcion inscripcion, Alumno alumno) {
        log.info("Creando primer pago sin arrastre.");
        Pago pago = new Pago();
        pago.setFecha(request.fecha());
        pago.setFechaVencimiento(request.fechaVencimiento());
        pago.setMonto(request.monto());
        pago.setAlumno(alumno);
        pago.setInscripcion(inscripcion);
        if (request.metodoPagoId() != null) {
            MetodoPago metodo = metodoPagoRepositorio.findById(request.metodoPagoId())
                    .orElseThrow(() -> new IllegalArgumentException("Método de pago no encontrado."));
            pago.setMetodoPago(metodo);
        }
        List<DetallePago> detalleEntidades = request.detallePagos().stream()
                .map(detDTO -> {
                    DetallePago det = detallePagoMapper.toEntity(detDTO);
                    det.setPago(pago);
                    // En lugar de llamar a det.calcularImporte(), se utiliza el servicio:
                    detallePagoServicio.calcularImporte(det);
                    return det;
                })
                .filter(det -> det.getImporte() != null && det.getImporte() > 0)
                .collect(Collectors.toList());
        pago.setDetallePagos(detalleEntidades);

        Pago guardado = pagoRepositorio.save(pago);
        actualizarImportesPago(guardado);
        guardado = pagoRepositorio.save(guardado);
        procesarPagosEspecificos(guardado);
        return guardado;
    }

    public Pago processPaymentWithoutNewDetails(PagoRegistroRequest request, Inscripcion inscripcion) {
        log.info("Creando/actualizando pago sin nuevos detalles.");
        Pago pago = new Pago();
        pago.setFecha(request.fecha());
        pago.setFechaVencimiento(request.fechaVencimiento());
        pago.setMonto(request.monto());
        pago.setInscripcion(inscripcion);
        pago.setAlumno(inscripcion.getAlumno());
        Pago guardado = pagoRepositorio.save(pago);
        log.info("Pago sin nuevos detalles creado/actualizado: id={}", guardado.getId());
        return guardado;
    }

    private void actualizarImportesPago(Pago pago) {
        if (pago.getDetallePagos() != null) {
            // Se recalcula el importe de cada detalle usando DetallePagoServicio.
            pago.getDetallePagos().forEach(detallePagoServicio::calcularImporte);
        }
        double totalImporte = (pago.getDetallePagos() != null && !pago.getDetallePagos().isEmpty())
                ? pago.getDetallePagos().stream().mapToDouble(det -> det.getImporte() != null ? det.getImporte() : 0.0).sum()
                : pago.getMonto();
        pago.setSaldoRestante(totalImporte);
    }

    public void procesarPagosEspecificos(Pago pago) {
        if (pago.getDetallePagos() != null) {
            for (DetallePago detalle : pago.getDetallePagos()) {
                if (detalle.getConcepto() != null) {
                    String concepto = detalle.getConcepto().trim().toUpperCase();
                    if (concepto.startsWith("MATRICULA")) {
                        if (pago.getAlumno() != null) {
                            String[] partes = concepto.split(" ");
                            if (partes.length >= 2) {
                                try {
                                    int anio = Integer.parseInt(partes[1]);
                                    log.info("Procesando pago de matrícula para año {}", anio);
                                    MatriculaResponse matResp = matriculaServicio.obtenerOMarcarPendiente(pago.getAlumno().getId());
                                    matriculaServicio.actualizarEstadoMatricula(matResp.id(),
                                            new MatriculaModificacionRequest(matResp.anio(), true, pago.getFecha()));
                                } catch (NumberFormatException e) {
                                    log.warn("No se pudo extraer el año de matrícula del concepto: {}", concepto);
                                }
                            }
                        }
                    } else if (concepto.contains("CUOTA")) {
                        if (pago.getInscripcion() != null) {
                            String[] partes = concepto.split(" - ");
                            if (partes.length >= 3) {
                                String periodo = partes[2].trim();
                                log.info("Procesando pago de mensualidad para periodo: {}", periodo);
                                MensualidadResponse mensPendiente = mensualidadServicio.buscarMensualidadPendientePorDescripcion(pago.getInscripcion(), periodo);
                                if (mensPendiente != null) {
                                    log.info("Mensualidad pendiente encontrada: id={}", mensPendiente.id());
                                    mensualidadServicio.marcarComoPagada(mensPendiente.id(), pago.getFecha());
                                } else {
                                    log.info("No se encontró mensualidad pendiente para el periodo '{}'; se creará una nueva.", periodo);
                                    mensualidadServicio.crearMensualidadPagada(pago.getInscripcion().getId(), periodo, pago.getFecha());
                                }
                            }
                        } else {
                            log.info("Pago GENERAL: se omite procesamiento de mensualidades por falta de inscripción.");
                        }
                    }
                }
            }
        }
    }
}

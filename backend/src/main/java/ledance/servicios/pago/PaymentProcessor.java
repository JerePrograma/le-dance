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

        // Se obtiene el alumno (en producción este dato vendría del contexto o del DTO)
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
        log.info("[PaymentProcessor] Pago general creado: id={}, monto={}, saldoRestante={}",
                guardado.getId(), guardado.getMonto(), guardado.getSaldoRestante());
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
                    // Si no se envía "aCobrar", se inicializa con el valorBase
                    if (det.getaCobrar() == null) {
                        det.setaCobrar(det.getValorBase());
                    }
                    det.setPago(pago);
                    return det;
                })
                .filter(det -> det.getValorBase() != null && det.getValorBase() > 0)
                .collect(Collectors.toCollection(ArrayList::new));
    }

    public Pago processPaymentWithPrevious(PagoRegistroRequest request, Inscripcion inscripcion, Alumno alumno, Pago pagoExistente) {
        log.info("[PaymentProcessor] Actualizando pago existente (id {}), para alumno {}.", pagoExistente.getId(), alumno.getId());

        // Actualiza los datos principales del pago existente
        pagoExistente.setFecha(request.fecha());
        pagoExistente.setFechaVencimiento(request.fechaVencimiento());
        pagoExistente.setRecargoAplicado(Boolean.TRUE.equals(request.recargoAplicado()));
        pagoExistente.setBonificacionAplicada(Boolean.TRUE.equals(request.bonificacionAplicada()));
        pagoExistente.setAlumno(alumno);
        pagoExistente.setInscripcion(inscripcion);
        pagoExistente.setActivo(true);

        if (request.metodoPagoId() != null) {
            MetodoPago metodo = metodoPagoRepositorio.findById(request.metodoPagoId())
                    .orElseThrow(() -> new IllegalArgumentException("Método de pago no encontrado."));
            pagoExistente.setMetodoPago(metodo);
        }

        // Actualiza el monto y resetea saldo a favor
        pagoExistente.setMonto(request.monto());
        pagoExistente.setSaldoAFavor(0.0);

        // Recorre los detalles existentes y recalcula el importe basándose en "aCobrar"
        pagoExistente.getDetallePagos().forEach(det -> {
            if (det.getaCobrar() != null && det.getaCobrar() > 0) {
                detallePagoServicio.calcularImporte(det);
                if (det.getaCobrar() <= 0.0) {
                    det.setCobrado(true);
                }
            }
        });

        // Agrega los nuevos detalles (si es que hay) al pago existente
        List<DetallePago> nuevosDetalles = obtenerDetallesNuevos(request, pagoExistente);
        pagoExistente.getDetallePagos().addAll(nuevosDetalles);

        // Recalcula importes y saldo restante
        actualizarImportesPago(pagoExistente);
        procesarPagosEspecificos(pagoExistente);

        Pago guardado = pagoRepositorio.save(pagoExistente);
        log.info("[PaymentProcessor] Pago actualizado: id={}, monto={}, saldoRestante={}",
                guardado.getId(), guardado.getMonto(), guardado.getSaldoRestante());

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
                    // Inicializa "aCobrar" con valorBase si no se envía
                    if (det.getaCobrar() == null) {
                        det.setaCobrar(det.getValorBase());
                    }
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
            // Se recalcula el importe de cada detalle utilizando "aCobrar"
            pago.getDetallePagos().forEach(detallePagoServicio::calcularImporte);
        }
        double totalImporte = (pago.getDetallePagos() != null && !pago.getDetallePagos().isEmpty())
                ? pago.getDetallePagos().stream()
                .mapToDouble(det -> det.getImporte() != null ? det.getImporte() : 0.0)
                .sum()
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

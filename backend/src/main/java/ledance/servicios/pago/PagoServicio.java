package ledance.servicios.pago;

import ledance.dto.deudas.DeudasPendientesResponse;
import ledance.dto.inscripcion.InscripcionMapper;
import ledance.dto.matricula.request.MatriculaModificacionRequest;
import ledance.dto.matricula.response.MatriculaResponse;
import ledance.dto.mensualidad.response.MensualidadResponse;
import ledance.dto.pago.DetallePagoMapper;
import ledance.dto.pago.PagoMapper;
import ledance.dto.pago.PagoMedioMapper;
import ledance.dto.pago.request.DetallePagoRegistroRequest;
import ledance.dto.pago.request.PagoMedioRegistroRequest;
import ledance.dto.pago.request.PagoModificacionRequest;
import ledance.dto.pago.request.PagoRegistroRequest;
import ledance.dto.pago.response.DetallePagoResponse;
import ledance.dto.pago.response.PagoMedioResponse;
import ledance.dto.pago.response.PagoResponse;
import ledance.dto.cobranza.CobranzaDTO;
import ledance.dto.cobranza.DetalleCobranzaDTO;
import ledance.entidades.*;
import ledance.repositorios.*;
import jakarta.transaction.Transactional;
import ledance.servicios.matricula.MatriculaServicio;
import ledance.servicios.mensualidad.MensualidadServicio;
import ledance.servicios.detallepago.DetallePagoServicio;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class PagoServicio {

    private static final Logger log = LoggerFactory.getLogger(PagoServicio.class);

    private final PagoRepositorio pagoRepositorio;
    private final AlumnoRepositorio alumnoRepositorio;
    private final InscripcionRepositorio inscripcionRepositorio;
    private final MetodoPagoRepositorio metodoPagoRepositorio;
    private final PagoMapper pagoMapper;
    private final MatriculaServicio matriculaServicio;
    private final MensualidadServicio mensualidadServicio;
    private final RecargoRepositorio recargoRepositorio;
    private final BonificacionRepositorio bonificacionRepositorio;

    // Servicios para delegar la lógica de cálculo y procesamiento
    private final DetallePagoServicio detallePagoServicio;
    private final PaymentProcessor paymentProcessor;
    private final DetallePagoMapper detallePagoMapper;
    private final PagoMedioMapper pagoMedioMapper;
    private final InscripcionMapper inscripcionMapper;

    public PagoServicio(AlumnoRepositorio alumnoRepositorio,
                        PagoRepositorio pagoRepositorio,
                        InscripcionRepositorio inscripcionRepositorio,
                        MetodoPagoRepositorio metodoPagoRepositorio,
                        PagoMapper pagoMapper,
                        MatriculaServicio matriculaServicio,
                        DetallePagoMapper detallePagoMapper,
                        MensualidadServicio mensualidadServicio, RecargoRepositorio recargoRepositorio, BonificacionRepositorio bonificacionRepositorio, DetallePagoRepositorio detallePagoRepositorio,
                        DetallePagoServicio detallePagoServicio, PaymentProcessor paymentProcessor, DetallePagoMapper detallePagoMapper1, PagoMedioMapper pagoMedioMapper, InscripcionMapper inscripcionMapper) {
        this.alumnoRepositorio = alumnoRepositorio;
        this.pagoRepositorio = pagoRepositorio;
        this.inscripcionRepositorio = inscripcionRepositorio;
        this.metodoPagoRepositorio = metodoPagoRepositorio;
        this.pagoMapper = pagoMapper;
        this.matriculaServicio = matriculaServicio;
        this.mensualidadServicio = mensualidadServicio;
        this.recargoRepositorio = recargoRepositorio;
        this.bonificacionRepositorio = bonificacionRepositorio;
        this.detallePagoServicio = detallePagoServicio;
        this.paymentProcessor = paymentProcessor;
        this.detallePagoMapper = detallePagoMapper;
        this.pagoMedioMapper = pagoMedioMapper;
        this.inscripcionMapper = inscripcionMapper;
    }


    @Transactional
    public PagoResponse registrarPago(PagoRegistroRequest request) {
        // Se determina el pago final (la entidad Pago)
        Pago pagoFinal = paymentProcessor.crearPagoSegunInscripcion(request);

        // Se marcan como cobrados los detalles cuyo importe pendiente es 0
        marcarDetallesCobradosSiImporteEsCero(pagoFinal);

        // Se agregan los medios de pago (si los hubiera)
        registrarMediosDePago(pagoFinal, request.pagoMedios());

        // Se actualizan importes y se guarda
        paymentProcessor.actualizarImportesPago(pagoFinal);
        verificarSaldoRestante(pagoFinal);
        pagoRepositorio.save(pagoFinal);

        if (pagoFinal.getSaldoRestante() == 0 && pagoFinal.getAlumno() != null) {
            actualizarEstadoDeudas(pagoFinal.getAlumno().getId(), pagoFinal.getFecha());
        }

        // Se convierte la entidad a DTO antes de retornar
        return pagoMapper.toDTO(pagoFinal);
    }

    private static void verificarSaldoRestante(Pago pagoFinal) {
        if (pagoFinal.getSaldoRestante() < 0) {
            log.error("Error: Saldo restante negativo detectado en pago ID={}. Ajustando a 0.", pagoFinal.getId());
            pagoFinal.setSaldoRestante(0.0);
        }
    }

    @Transactional
    public PagoResponse actualizarPago(Long id, PagoModificacionRequest request) {
        Pago pago = pagoRepositorio.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Pago no encontrado."));

        pago.setFecha(request.fecha());
        pago.setFechaVencimiento(request.fechaVencimiento());
        pago.setRecargoAplicado(request.recargoAplicado());
        pago.setBonificacionAplicada(request.bonificacionAplicada());
        pago.setActivo(request.activo());

        if (request.metodoPagoId() != null) {
            MetodoPago metodo = metodoPagoRepositorio.findById(request.metodoPagoId())
                    .orElseThrow(() -> new IllegalArgumentException("Método de pago no encontrado."));
            pago.setMetodoPago(metodo);
        }

        actualizarDetallesPago(pago, request.detallePagos());
        paymentProcessor.actualizarImportesPago(pago);

        verificarSaldoRestante(pago);
        pagoRepositorio.save(pago);

        if (pago.getSaldoRestante() == 0) {
            actualizarEstadoDeudas(pago.getAlumno().getId(), pago.getFecha());
        }

        return pagoMapper.toDTO(pago);
    }

    private void actualizarDetallesPago(Pago pago, List<DetallePagoRegistroRequest> detallesDTO) {
        log.info("[actualizarDetallesPago] Actualizando detalles para pago id={}", pago.getId());
        // Crear un mapa de detalles existentes por su ID
        Map<Long, DetallePago> existentes = pago.getDetallePagos().stream()
                .collect(Collectors.toMap(DetallePago::getId, Function.identity()));

        // Para cada detalle del DTO, si ya existe, se conservan los valores acumulados (especialmente aCobrar)
        List<DetallePago> detallesFinales = detallesDTO.stream()
                .map(dto -> obtenerODefinirDetallePago(dto, existentes, pago))
                .collect(Collectors.toList());

        pago.getDetallePagos().clear();
        pago.getDetallePagos().addAll(detallesFinales);
        // Recalcular el importe de cada detalle (se preserva el aCobrar acumulado)
        pago.getDetallePagos().forEach(detalle -> {
            log.info("[actualizarDetallesPago] Recalculando importe para detalle id={}", detalle.getId());
            detallePagoServicio.calcularImporte(detalle);
        });
    }

    private DetallePago obtenerODefinirDetallePago(DetallePagoRegistroRequest dto,
                                                   Map<Long, DetallePago> existentes, Pago pago) {
        if (dto.id() != null && existentes.containsKey(dto.id())) {
            DetallePago detalle = existentes.get(dto.id());
            log.info("[obtenerODefinirDetallePago] Actualizando detalle existente id={}", detalle.getId());
            detalle.setConcepto(dto.concepto());
            detalle.setValorBase(dto.valorBase());
            // No sobrescribir aCobrar para preservar el acumulado
            // Actualizar otros campos:
            detalle.setBonificacion(Optional.ofNullable(dto.bonificacionId())
                    .flatMap(bonificacionRepositorio::findById).orElse(null));
            detalle.setRecargo(dto.recargoId() != null
                    ? recargoRepositorio.findById(dto.recargoId()).orElse(null)
                    : null);
            // Se puede actualizar otros campos que no afecten al histórico de abonos
            return detalle;
        } else {
            log.info("[obtenerODefinirDetallePago] Creando nuevo detalle para concepto '{}'", dto.concepto());
            DetallePago nuevo = new DetallePago();
            nuevo.setPago(pago);
            nuevo.setConcepto(dto.concepto());
            nuevo.setValorBase(dto.valorBase());
            nuevo.setaCobrar((dto.aCobrar() != null && dto.aCobrar() > 0) ? dto.aCobrar() : dto.valorBase());
            // Para nuevos detalles se calculará importeInicial y se asignará al importePendiente
            nuevo.setBonificacion(Optional.ofNullable(dto.bonificacionId())
                    .flatMap(bonificacionRepositorio::findById).orElse(null));
            nuevo.setRecargo(dto.recargoId() != null
                    ? recargoRepositorio.findById(dto.recargoId()).orElse(null)
                    : null);
            return nuevo;
        }
    }

    public PagoResponse obtenerUltimoPagoPendiente(Long alumnoId) {
        Optional<Pago> pagoOpt = pagoRepositorio.findTopByAlumnoIdAndActivoTrueOrderByFechaDesc(alumnoId);
        return pagoOpt.map(pagoMapper::toDTO).orElse(null);
    }

    private void actualizarEstadoDeudas(Long alumnoId, LocalDate fechaPago) {

        MatriculaResponse matResp = matriculaServicio.obtenerOMarcarPendiente(alumnoId);
        if (matResp != null && !matResp.pagada()) {
            matriculaServicio.actualizarEstadoMatricula(matResp.id(),
                    new MatriculaModificacionRequest(matResp.anio(), true, fechaPago));
        }

        List<MensualidadResponse> pendientes = mensualidadServicio.listarMensualidadesPendientesPorAlumno(alumnoId);
        pendientes.forEach(mens -> {
            if ("PENDIENTE".equalsIgnoreCase(mens.estado()) || "OMITIDO".equalsIgnoreCase(mens.estado())) {
                mensualidadServicio.marcarComoPagada(mens.id(), fechaPago);
            }
        });
    }

    private void registrarMediosDePago(Pago pago, List<PagoMedioRegistroRequest> mediosPago) {
        if (mediosPago == null || mediosPago.isEmpty()) {
            return;
        }

        List<PagoMedio> medios = mediosPago.stream()
                .map(request -> {
                    MetodoPago metodo = metodoPagoRepositorio.findById(request.metodoPagoId())
                            .orElseThrow(() -> new IllegalArgumentException("Método de pago no encontrado con ID: " + request.metodoPagoId()));
                    PagoMedio pagoMedio = new PagoMedio();
                    pagoMedio.setMonto(request.montoAbonado());
                    pagoMedio.setMetodo(metodo);
                    pagoMedio.setPago(pago);
                    return pagoMedio;
                })
                .toList();

        pago.getPagoMedios().addAll(medios);
    }

    private void marcarDetallesCobradosSiImporteEsCero(Pago pago) {
        pago.getDetallePagos().forEach(detalle -> {
            if (detalle.getImportePendiente() != null && detalle.getImportePendiente() == 0.0) {
                detalle.setCobrado(true);
            }
        });
        verificarSaldoRestante(pago);
        pagoRepositorio.save(pago);
    }

    @Transactional
    public PagoResponse quitarRecargoManual(Long id) {
        Pago pago = pagoRepositorio.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Pago no encontrado."));
        pago.setRecargoAplicado(false);

        if (pago.getDetallePagos() != null) {
            for (DetallePago detalle : pago.getDetallePagos()) {
                // Quitar el recargo
                detalle.setRecargo(null);
                // Recalcular el importe inicial y, si corresponde, actualizar el importe pendiente.
                detallePagoServicio.calcularImporte(detalle);
                // Si el detalle ya tenía abonos (importeInicial - importePendiente > 0), se puede conservar la diferencia.
                // Para simplificar, en este ejemplo, si no se ha abonado, se iguala el pendiente al nuevo importe inicial.
                // En casos más complejos, se podría recalcular preservando los abonos ya realizados.
                if (!detalle.getCobrado()) {
                    detalle.setImportePendiente(detalle.getImporteInicial());
                }
            }
        }

        // Recalcular el monto total del pago sumando los importes pendientes de cada detalle
        assert pago.getDetallePagos() != null;
        double nuevoMonto = pago.getDetallePagos().stream()
                .mapToDouble(DetallePago::getImportePendiente)
                .sum();
        pago.setMonto(nuevoMonto);

        // Si hay pagos previos para la inscripción, se calcula el total abonado en ellos.
        double sumPagosPrevios = 0;
        if (pago.getInscripcion() != null) {
            sumPagosPrevios = pagoRepositorio.findByInscripcionIdOrderByFechaDesc(pago.getInscripcion().getId())
                    .stream()
                    .mapToDouble(Pago::getMonto)
                    .sum();
        }
        pago.setSaldoRestante(nuevoMonto - sumPagosPrevios);
        verificarSaldoRestante(pago);
        Pago actualizado = pagoRepositorio.save(pago);
        return pagoMapper.toDTO(actualizado);
    }

    // Método refactorizado para registrar un pago parcial sin usar "abono"
    @Transactional
    public PagoResponse registrarPagoParcial(Long pagoId, Double montoAbonado,
                                             Map<Long, Double> montosPorDetalle, Long metodoPagoId) {

        log.info("[registrarPagoParcial] Iniciando para pagoId={}, montoAbonado={}, metodoPagoId={}",
                pagoId, montoAbonado, metodoPagoId);
        log.info("[registrarPagoParcial] Montos por detalle recibidos: {}", montosPorDetalle);

        // Recuperar el pago
        Pago pago = pagoRepositorio.findById(pagoId)
                .orElseThrow(() -> new IllegalArgumentException("Pago no encontrado."));
        log.info("[registrarPagoParcial] Pago obtenido: id={}, monto={}, saldoRestante={}",
                pago.getId(), pago.getMonto(), pago.getSaldoRestante());

        // Obtener la entidad MetodoPago
        MetodoPago metodo = metodoPagoRepositorio.findById(metodoPagoId)
                .orElseThrow(() -> new IllegalArgumentException("Método de pago no encontrado."));
        log.info("[registrarPagoParcial] MetodoPago obtenido: id={}, descripcion={}",
                metodo.getId(), metodo.getDescripcion());

        // Crear y asignar el objeto PagoMedio
        PagoMedio pagoMedio = new PagoMedio();
        pagoMedio.setMonto(montoAbonado);
        pagoMedio.setMetodo(metodo);
        pagoMedio.setPago(pago);
        pago.getPagoMedios().add(pagoMedio);
        log.info("[registrarPagoParcial] PagoMedio creado: monto={}, asignado a pago id={}",
                montoAbonado, pago.getId());

        // Actualizar cada detalle: descontar el abono del importePendiente
        for (DetallePago detalle : pago.getDetallePagos()) {
            if (montosPorDetalle.containsKey(detalle.getId())) {
                double abono = montosPorDetalle.get(detalle.getId());
                log.info("[registrarPagoParcial] Procesando detalle id={}. Abono a aplicar: {}",
                        detalle.getId(), abono);
                if (abono < 0) {
                    throw new IllegalArgumentException("No se permite abonar un monto negativo para el detalle id="
                            + detalle.getId());
                }
                double pendienteActual = detalle.getImportePendiente();
                double nuevoPendiente = pendienteActual - abono;
                if (nuevoPendiente < 0) {
                    nuevoPendiente = 0;
                }
                log.info("[registrarPagoParcial] Detalle id={} | Pendiente actual: {} | Nuevo pendiente: {}",
                        detalle.getId(), pendienteActual, nuevoPendiente);
                detalle.setImportePendiente(nuevoPendiente);
                if (nuevoPendiente == 0) {
                    detalle.setCobrado(true);
                    log.info("[registrarPagoParcial] Detalle id={} marcado como cobrado", detalle.getId());
                }
            } else {
                log.info("[registrarPagoParcial] Detalle id={} sin abono asignado, pendiente se mantiene: {}",
                        detalle.getId(), detalle.getImportePendiente());
            }
        }

        // Actualizar el total del pago basándose en los importes pendientes
        actualizarImportesPagoParcial(pago);
        log.info("[registrarPagoParcial] Tras actualizar importes, saldoRestante={}", pago.getSaldoRestante());

        verificarSaldoRestante(pago);
        pagoRepositorio.save(pago);
        log.info("[registrarPagoParcial] Pago guardado. Nuevo saldoRestante={}", pago.getSaldoRestante());

        PagoResponse response = pagoMapper.toDTO(pago);
        log.info("[registrarPagoParcial] Respuesta generada: {}", response);
        return response;
    }

    private void actualizarImportesPagoParcial(Pago pago) {
        double totalPendiente = pago.getDetallePagos().stream()
                .mapToDouble(DetallePago::getImportePendiente)
                .sum();
        log.info("[actualizarImportesPagoParcial] Suma de importes pendientes de detalles: {}", totalPendiente);
        pago.setSaldoRestante(totalPendiente);
        verificarSaldoRestante(pago);
        pagoRepositorio.save(pago);
        log.info("[actualizarImportesPagoParcial] Pago actualizado, saldoRestante={}", pago.getSaldoRestante());
    }

    public CobranzaDTO generarCobranzaPorAlumno(Long alumnoId) {
        Alumno alumno = alumnoRepositorio.findById(alumnoId)
                .orElseThrow(() -> new IllegalArgumentException("Alumno no encontrado."));
        List<Pago> pagosPendientes = pagoRepositorio.findByAlumnoIdAndActivoTrueOrderByFechaDesc(alumnoId)
                .stream()
                .filter(p -> p.getSaldoRestante() != null && p.getSaldoRestante() > 0)
                .filter(p -> !p.getEstado().equalsIgnoreCase("ACTIVO") || p.getSaldoRestante() > 0)
                .toList();

        Map<String, Double> conceptosPendientes = new HashMap<>();
        double totalPendiente = 0.0;
        for (Pago pago : pagosPendientes) {
            if (pago.getDetallePagos() != null) {
                for (DetallePago detalle : pago.getDetallePagos()) {
                    double pendiente = detalle.getValorBase() - (detalle.getAFavor() != null ? detalle.getAFavor() : 0.0);
                    if (pendiente > 0) {
                        String concepto = detalle.getConcepto();
                        conceptosPendientes.put(concepto, conceptosPendientes.getOrDefault(concepto, 0.0) + pendiente);
                        totalPendiente += pendiente;
                    }
                }
            }
        }
        List<DetalleCobranzaDTO> detalles = conceptosPendientes.entrySet().stream()
                .map(e -> new DetalleCobranzaDTO(e.getKey(), e.getValue()))
                .collect(Collectors.toList());
        return new CobranzaDTO(alumno.getId(), alumno.getNombre() + " " + alumno.getApellido(), totalPendiente, detalles);
    }

    @Transactional
    public PagoResponse obtenerUltimoPagoPendientePorAlumno(Long alumnoId) {
        // Se asume que se agregó la condición en el repositorio:
        // findTopByAlumnoIdAndActivoTrueAndSaldoRestanteGreaterThanOrderByFechaDesc
        Pago pago = pagoRepositorio
                .findTopByAlumnoIdAndActivoTrueAndSaldoRestanteGreaterThanOrderByFechaDesc(alumnoId, 0.0)
                .orElseThrow(() -> new IllegalArgumentException("No se encontró pago pendiente para el alumno con ID: " + alumnoId));

        // Se obtienen únicamente los detalles que no se han cobrado.
        List<DetallePagoResponse> detallesFiltrados = pago.getDetallePagos().stream()
                .filter(detalle -> !detalle.getCobrado())
                .map(detallePagoMapper::toDTO)
                .toList();

        List<PagoMedioResponse> mediosPago = pago.getPagoMedios().stream()
                .map(pagoMedioMapper::toDTO)
                .toList();

        return new PagoResponse(
                pago.getId(),
                pago.getFecha(),
                pago.getFechaVencimiento(),
                pago.getMonto(),
                pago.getMetodoPago().getDescripcion(),
                pago.getRecargoAplicado(),
                pago.getBonificacionAplicada(),
                pago.getSaldoRestante(),
                pago.getSaldoAFavor(),
                pago.getActivo(),
                pago.getEstado(),
                pago.getInscripcion() != null ? inscripcionMapper.toDTO(pago.getInscripcion()) : null,
                pago.getAlumno() != null ? pago.getAlumno().getId() : null,
                pago.getObservaciones(),
                detallesFiltrados,
                mediosPago,
                pago.getTipoPago().toString()
        );
    }

    @Transactional
    public DeudasPendientesResponse listarDeudasPendientesPorAlumno(Long alumnoId) {
        List<Pago> pagosPendientes = pagoRepositorio.findByAlumnoIdAndActivoTrueOrderByFechaDesc(alumnoId)
                .stream()
                .filter(p -> p.getSaldoRestante() != null && p.getSaldoRestante() > 0)
                .filter(p -> !p.getEstado().equalsIgnoreCase("ACTIVO") || p.getSaldoRestante() > 0)
                .toList();

        List<PagoResponse> pagosPendientesDTO = pagosPendientes.stream()
                .map(pago -> {
                    List<DetallePagoResponse> detallesFiltrados = pago.getDetallePagos().stream()
                            .filter(detalle -> !detalle.getCobrado())
                            .map(detallePagoMapper::toDTO)
                            .toList();
                    return new PagoResponse(
                            pago.getId(),
                            pago.getFecha(),
                            pago.getFechaVencimiento(),
                            pago.getMonto(),
                            pago.getMetodoPago().getDescripcion(),
                            pago.getRecargoAplicado(),
                            pago.getBonificacionAplicada(),
                            pago.getSaldoRestante(),
                            pago.getSaldoAFavor(),
                            pago.getActivo(),
                            pago.getEstado(),
                            pago.getInscripcion() != null ? inscripcionMapper.toDTO(pago.getInscripcion()) : null,
                            pago.getAlumno() != null ? pago.getAlumno().getId() : null,
                            pago.getObservaciones(),
                            detallesFiltrados,
                            pago.getPagoMedios().stream().map(pagoMedioMapper::toDTO).toList(),
                            pago.getTipoPago().toString()
                    );
                })
                .toList();

        MatriculaResponse matriculaDTO = matriculaServicio.obtenerOMarcarPendiente(alumnoId);
        if (matriculaDTO != null && matriculaDTO.pagada()) {
            matriculaDTO = null;
        }

        List<MensualidadResponse> mensualidadesPendientesDTO = mensualidadServicio.listarMensualidadesPendientesPorAlumno(alumnoId);
        double totalPagos = pagosPendientes.stream().mapToDouble(Pago::getSaldoRestante).sum();
        double totalMensualidades = mensualidadesPendientesDTO.stream()
                .mapToDouble(m -> m.totalPagar() != null ? m.totalPagar() : 0)
                .sum();
        double totalDeuda = totalPagos + totalMensualidades;
        Alumno alumno = pagosPendientes.isEmpty() ? alumnoRepositorio.findById(alumnoId).orElse(null)
                : pagosPendientes.get(0).getAlumno();
        String alumnoNombre = (alumno != null) ? alumno.getNombre() + " " + alumno.getApellido() : "Desconocido";

        return new DeudasPendientesResponse(
                alumno != null ? alumno.getId() : null,
                alumnoNombre,
                pagosPendientesDTO,
                matriculaDTO,
                mensualidadesPendientesDTO,
                totalDeuda
        );
    }

    private void actualizarDatosPrincipalesPago(Pago pago, PagoModificacionRequest request) {
        pago.setFecha(request.fecha());
        pago.setFechaVencimiento(request.fechaVencimiento());
        pago.setRecargoAplicado(request.recargoAplicado());
        pago.setBonificacionAplicada(request.bonificacionAplicada());
        pago.setActivo(request.activo());
    }

    private void actualizarMetodoPago(Pago pago, Long metodoPagoId) {
        if (metodoPagoId != null) {
            MetodoPago metodo = metodoPagoRepositorio.findById(metodoPagoId)
                    .orElseThrow(() -> new IllegalArgumentException("Método de pago no encontrado."));
            pago.setMetodoPago(metodo);
        }
    }

    private double recalcularSaldoRestante(Pago pago) {
        return pago.getDetallePagos().stream()
                .mapToDouble(detalle -> detalle.getaCobrar() != null ? detalle.getaCobrar() : 0.0)
                .sum();
    }

    public PagoResponse obtenerPagoPorId(Long id) {
        Pago pago = pagoRepositorio.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Pago no encontrado."));

        // Filtrar solo los `DetallePago` que no estén cobrados
        List<DetallePago> detallesPendientes = pago.getDetallePagos().stream()
                .filter(detalle -> !detalle.getCobrado())
                .collect(Collectors.toList());

        pago.setDetallePagos(detallesPendientes);
        return pagoMapper.toDTO(pago);
    }

    public List<PagoResponse> listarPagos() {
        return pagoRepositorio.findAll()
                .stream()
                .map(pagoMapper::toDTO)
                .collect(Collectors.toList());
    }

    @Transactional
    public void eliminarPago(Long id) {
        Pago pago = pagoRepositorio.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Pago no encontrado."));
        pago.setActivo(false);
        if (pago.getDetallePagos() != null) {
            pago.getDetallePagos().forEach(detalle -> detalle.setPago(pago));
        }
        verificarSaldoRestante(pago);
        pagoRepositorio.save(pago);
    }

    public List<PagoResponse> listarPagosPorInscripcion(Long inscripcionId) {
        return pagoRepositorio.findByInscripcionIdOrderByFechaDesc(inscripcionId)
                .stream()
                .map(pagoMapper::toDTO)
                .collect(Collectors.toList());
    }

    public List<PagoResponse> listarPagosPorAlumno(Long alumnoId) {
        return pagoRepositorio.findByInscripcionAlumnoIdOrderByFechaDesc(alumnoId)
                .stream()
                .filter(p -> p.getSaldoRestante() > 0) // Solo pagos con deuda
                .map(pagoMapper::toDTO)
                .collect(Collectors.toList());

    }

    public List<PagoResponse> listarPagosVencidos() {
        LocalDate hoy = LocalDate.now();
        return pagoRepositorio.findPagosVencidos(hoy)
                .stream()
                .map(pagoMapper::toDTO)
                .collect(Collectors.toList());
    }

    @Transactional
    public void generarCuotasParaAlumnosActivos() {
        List<Inscripcion> inscripcionesActivas = inscripcionRepositorio.findByEstado(EstadoInscripcion.ACTIVA);
        for (Inscripcion inscripcion : inscripcionesActivas) {
            Pago nuevoPago = new Pago();
            nuevoPago.setFecha(LocalDate.now());
            nuevoPago.setFechaVencimiento(LocalDate.now().plusDays(30));
            nuevoPago.setMonto(inscripcion.getDisciplina().getValorCuota());
            nuevoPago.setSaldoRestante(inscripcion.getDisciplina().getValorCuota());
            nuevoPago.setActivo(true);
            nuevoPago.setInscripcion(inscripcion);
            nuevoPago.setAlumno(inscripcion.getAlumno());
            verificarSaldoRestante(nuevoPago);
            pagoRepositorio.save(nuevoPago);
        }
    }

    public double calcularDeudaAlumno(Long alumnoId) {
        // Se listan los pagos activos que tienen saldo pendiente
        List<Pago> pagosPendientes = pagoRepositorio.findByAlumnoIdAndActivoTrueOrderByFechaDesc(alumnoId)
                .stream()
                .filter(p -> p.getSaldoRestante() != null && p.getSaldoRestante() > 0) // Solo pagos con saldo pendiente
                .filter(p -> !p.getEstado().equalsIgnoreCase("ACTIVO") || p.getSaldoRestante() > 0) // Evita pagos saldados
                .toList();

        return pagosPendientes.stream().mapToDouble(Pago::getSaldoRestante).sum();
    }

    public PagoResponse obtenerUltimoPagoPorAlumno(Long alumnoId) {
        return pagoMapper.toDTO(paymentProcessor.obtenerUltimoPagoPendienteEntidad(alumnoId));
    }

}

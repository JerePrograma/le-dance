package ledance.servicios.pago;

import ledance.dto.matricula.request.MatriculaModificacionRequest;
import ledance.dto.matricula.response.MatriculaResponse;
import ledance.dto.pago.PagoMapper;
import ledance.dto.pago.PagoMedioMapper;
import ledance.dto.pago.request.PagoModificacionRequest;
import ledance.dto.pago.request.PagoRegistroRequest;
import ledance.dto.pago.request.DetallePagoRegistroRequest;
import ledance.dto.pago.response.PagoResponse;
import ledance.dto.cobranza.CobranzaDTO;
import ledance.dto.cobranza.DetalleCobranzaDTO;
import ledance.entidades.*;
import ledance.repositorios.*;
import jakarta.transaction.Transactional;
import ledance.servicios.inscripcion.InscripcionServicio;
import ledance.servicios.matricula.MatriculaServicio;
import ledance.servicios.mensualidad.MensualidadServicio;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Service
public class PagoServicio implements IPagoServicio {

    private static final Logger log = LoggerFactory.getLogger(PagoServicio.class);

    private final PagoRepositorio pagoRepositorio;
    private final AlumnoRepositorio alumnoRepositorio;
    private final InscripcionRepositorio inscripcionRepositorio;
    private final MetodoPagoRepositorio metodoPagoRepositorio;
    private final PagoMapper pagoMapper;
    private final PaymentCalculationService calculationService;
    private final MatriculaServicio matriculaServicio;
    private final ledance.dto.pago.DetallePagoMapper detallePagoMapper;
    private final MatriculaRepositorio matriculaRepositorio;
    private final MensualidadServicio mensualidadServicio;

    public PagoServicio(AlumnoRepositorio alumnoRepositorio, PagoRepositorio pagoRepositorio,
                        InscripcionRepositorio inscripcionRepositorio,
                        MetodoPagoRepositorio metodoPagoRepositorio,
                        PagoMapper pagoMapper,
                        PaymentCalculationService calculationService,
                        MatriculaServicio matriculaServicio,
                        InscripcionServicio inscripcionServicio,
                        RecargoRepositorio recargoRepositorio,
                        BonificacionRepositorio bonificacionRepositorio,
                        ledance.dto.pago.DetallePagoMapper detallePagoMapper,
                        PagoMedioMapper pagoMedioMapper, MatriculaRepositorio matriculaRepositorio, MensualidadServicio mensualidadServicio) {
        this.alumnoRepositorio = alumnoRepositorio;
        this.pagoRepositorio = pagoRepositorio;
        this.inscripcionRepositorio = inscripcionRepositorio;
        this.metodoPagoRepositorio = metodoPagoRepositorio;
        this.pagoMapper = pagoMapper;
        this.calculationService = calculationService;
        this.matriculaServicio = matriculaServicio;
        this.detallePagoMapper = detallePagoMapper;
        this.matriculaRepositorio = matriculaRepositorio;
        this.mensualidadServicio = mensualidadServicio;
    }

    @Override
    @Transactional
    public PagoResponse registrarPago(PagoRegistroRequest request) {
        log.info("Iniciando registro de pago para inscripcionId: {}", request.inscripcionId());
        log.info("Datos recibidos en el request: fecha={}, fechaVencimiento={}, monto={}, cantidad de detallePagos={}",
                request.fecha(), request.fechaVencimiento(), request.monto(), request.detallePagos().size());

        // 1. Buscar la Inscripción y el Alumno
        Inscripcion inscripcion = inscripcionRepositorio.findById(request.inscripcionId())
                .orElseThrow(() -> new IllegalArgumentException("Inscripción no encontrada."));
        Alumno alumno = inscripcion.getAlumno();
        log.info("Inscripción encontrada: id={}, Alumno: id={}, nombre={}", inscripcion.getId(), alumno.getId(), alumno.getNombre());

        // 2. Detectar si hay nuevos DetallePago (con ID nulo)
        boolean hayDetalleNuevos = request.detallePagos().stream().anyMatch(det -> det.id() == null);
        log.info("¿Hay nuevos detalles? {}", hayDetalleNuevos);

        // 3. Buscar el pago anterior activo (con saldo pendiente) del alumno
        List<Pago> pagosPendientes = pagoRepositorio.findPagosPendientesByAlumno(alumno.getId());
        Pago pagoAnterior = pagosPendientes.isEmpty() ? null : pagosPendientes.get(0);
        if (pagoAnterior != null) {
            log.info("Pago anterior activo encontrado: id={}, monto={}, saldoRestante={}",
                    pagoAnterior.getId(), pagoAnterior.getMonto(), pagoAnterior.getSaldoRestante());
        } else {
            log.info("No se encontró pago anterior activo para el alumno id: {}", alumno.getId());
        }

        // 4. Procesar según existencia de nuevos detalles y pago anterior
        Pago pagoFinal;
        if (hayDetalleNuevos) {
            if (pagoAnterior != null) {
                pagoFinal = processPaymentWithPrevious(request, inscripcion, alumno, pagoAnterior);
            } else {
                pagoFinal = processFirstPayment(request, inscripcion, alumno);
            }
        } else {
            pagoFinal = processPaymentWithoutNewDetails(request, inscripcion);
        }

        return pagoMapper.toDTO(pagoFinal);
    }

    /**
     * Procesa el pago cuando existe un pago anterior activo.
     * Se inactiva el pago anterior y se crea un nuevo pago que "arrastra" los detalles pendientes.
     */
    private Pago processPaymentWithPrevious(PagoRegistroRequest request, Inscripcion inscripcion, Alumno alumno, Pago pagoAnterior) {
        // Inactivar el pago anterior
        pagoAnterior.setActivo(false);
        pagoRepositorio.save(pagoAnterior);
        log.info("Pago anterior (id={}) inactivado.", pagoAnterior.getId());

        // Crear el nuevo pago con arrastre
        Pago nuevoPago = new Pago();
        nuevoPago.setFecha(request.fecha());
        nuevoPago.setFechaVencimiento(request.fechaVencimiento());
        nuevoPago.setRecargoAplicado(request.recargoAplicado() != null ? request.recargoAplicado() : false);
        nuevoPago.setBonificacionAplicada(request.bonificacionAplicada() != null ? request.bonificacionAplicada() : false);
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

        // Arrastrar los detalles pendientes del pago anterior
        List<DetallePago> detallesArrastrados = pagoAnterior.getDetallePagos().stream()
                .filter(dp -> dp.getImporte() != null)
                .map(dp -> {
                    double abonoPrevio = dp.getAbono() != null ? dp.getAbono() : 0.0;
                    double pendiente = dp.getImporte() - abonoPrevio;
                    log.info("Detalle anterior id={}, importe={}, pendiente={}", dp.getId(), dp.getImporte(), pendiente);
                    if (pendiente > 0) {
                        DetallePago nuevoDet = new DetallePago();
                        nuevoDet.setConcepto(dp.getConcepto());
                        nuevoDet.setCodigoConcepto(dp.getCodigoConcepto());
                        nuevoDet.setValorBase(dp.getValorBase());
                        nuevoDet.setaCobrar(pendiente);
                        nuevoDet.setAbono(0.0);
                        nuevoDet.setPago(nuevoPago);
                        return nuevoDet;
                    }
                    return null;
                })
                .filter(Objects::nonNull)
                .collect(Collectors.toList());

        // Procesar los nuevos detalles enviados en el request
        List<DetallePago> detallesNuevos = request.detallePagos().stream()
                .filter(detDTO -> detDTO.id() == null)
                .map(detDTO -> {
                    DetallePago det = detallePagoMapper.toEntity(detDTO);
                    if (det.getaCobrar() == null || det.getaCobrar() == 0) {
                        det.setaCobrar(det.getValorBase());
                    }
                    det.setPago(nuevoPago);
                    return det;
                })
                .filter(det -> det.getValorBase() != null && det.getValorBase() > 0)
                .collect(Collectors.toList());

        List<DetallePago> todosDetalles = Stream.concat(detallesArrastrados.stream(), detallesNuevos.stream())
                .filter(det -> det.getImporte() != 0)
                .collect(Collectors.toList());
        nuevoPago.setDetallePagos(todosDetalles);

        Pago guardado = pagoRepositorio.save(nuevoPago);
        guardado.recalcularSaldoRestante();
        guardado = pagoRepositorio.save(guardado);

        // Procesar pagos específicos: matrícula y mensualidad
        procesarPagosEspecificos(guardado);

        return guardado;
    }

    /**
     * Procesa el primer pago, cuando no existe un pago anterior activo.
     * Se esperan nuevos detalles en el request.
     */
    private Pago processFirstPayment(PagoRegistroRequest request, Inscripcion inscripcion, Alumno alumno) {
        log.info("Creando primer pago sin arrastre.");
        Pago pago = pagoMapper.toEntity(request);
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
                    log.info("Detalle procesado para primer pago: concepto={}, valorBase={}, aCobrar={}",
                            det.getConcepto(), det.getValorBase(), det.getaCobrar());
                    return det;
                })
                .filter(det -> det.getValorBase() != null && det.getValorBase() > 0)
                .collect(Collectors.toList());
        pago.setDetallePagos(detalleEntidades);

        Pago guardado = pagoRepositorio.save(pago);
        guardado.recalcularSaldoRestante();
        guardado = pagoRepositorio.save(guardado);

        // Procesar pagos específicos (por matrícula/mensualidad)
        procesarPagosEspecificos(guardado);

        return guardado;
    }

    /**
     * Procesa el pago cuando no se detectan nuevos detalles en el request.
     * En este caso se crea o actualiza el pago sin procesar detalles.
     */
    private Pago processPaymentWithoutNewDetails(PagoRegistroRequest request, Inscripcion inscripcion) {
        log.info("Creando/actualizando pago sin nuevos detalles.");
        Pago pago = pagoMapper.toEntity(request);
        pago.setInscripcion(inscripcion);
        pago.setAlumno(inscripcion.getAlumno());
        Pago guardado = pagoRepositorio.save(pago);
        log.info("Pago sin nuevos detalles creado/actualizado: id={}", guardado.getId());
        return guardado;
    }

    /**
     * Método privado que recorre cada detalle del pago para distinguir si es de matrícula o mensualidad.
     * Para:
     * - Matrícula: Se reconoce cuando el concepto inicia con "MATRICULA" (ej.: "MATRICULA 2025").
     * - Mensualidad: Se asume el formato "(DISCIPLINA) - CUOTA - MES DE AÑO" (ej.: "DANZA - CUOTA - MARZO DE 2025").
     * Según corresponda, se delega en MatriculaServicio o MensualidadServicio.
     */
    private void procesarPagosEspecificos(Pago pago) {
        if (pago.getDetallePagos() != null) {
            for (DetallePago detalle : pago.getDetallePagos()) {
                if (detalle.getConcepto() != null) {
                    String concepto = detalle.getConcepto().trim().toUpperCase();
                    if (concepto.startsWith("MATRICULA")) {
                        // Procesar matrícula: Se espera el formato "MATRICULA 2025"
                        String[] partes = concepto.split(" ");
                        if (partes.length >= 2) {
                            try {
                                int anio = Integer.parseInt(partes[1]);
                                log.info("Procesando pago de matrícula para año {}", anio);
                                // Se delega al MatriculaServicio (se asume que el método actualizarEstadoMatricula usa la matrícula pendiente)
                                MatriculaResponse matResp = matriculaServicio.obtenerOMarcarPendiente(pago.getAlumno().getId());
                                // Se crea un request de modificación para marcar la matrícula como pagada.
                                // (Se asume que MatriculaModificacionRequest tiene parámetros: matriculaId, anio, fechaPago, etc.)
                                // Aquí se debe adaptar a la firma real de ese DTO.
                                matriculaServicio.actualizarEstadoMatricula(matResp.id(),
                                        new MatriculaModificacionRequest(anio, true, pago.getFecha()));
                            } catch (NumberFormatException e) {
                                log.warn("No se pudo extraer el año de matrícula del concepto: {}", concepto);
                            }
                        }
                    } else if (concepto.contains("CUOTA")) {
                        // Procesar mensualidad: Se espera el formato "(DISCIPLINA) - CUOTA - MES DE AÑO"
                        String[] partes = concepto.split(" - ");
                        if (partes.length >= 3) {
                            String periodo = partes[2].trim(); // Ej.: "MARZO DE 2025"
                            log.info("Procesando pago de mensualidad para periodo: {}", periodo);
                            // Se busca en MensualidadServicio una mensualidad pendiente para esta inscripción y periodo.
                            Mensualidad mensPendiente = mensualidadServicio.buscarMensualidadPendientePorDescripcion(
                                    pago.getInscripcion(), periodo);
                            if (mensPendiente != null) {
                                log.info("Mensualidad pendiente encontrada: id={}", mensPendiente.getId());
                                // Marcar como pagada
                                mensualidadServicio.marcarComoPagada(mensPendiente.getId(), pago.getFecha());
                            } else {
                                log.info("No se encontró mensualidad pendiente para el periodo '{}'; se creará una nueva.", periodo);
                                // Crear nueva mensualidad ya pagada
                                mensualidadServicio.crearMensualidadPagada(pago.getInscripcion().getId(), periodo, pago.getFecha());
                            }
                        }
                    }
                }
            }
        }
    }

    @Override
    public PagoResponse obtenerPagoPorId(Long id) {
        log.info("Obteniendo pago con id: {}", id);
        Pago pago = pagoRepositorio.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Pago no encontrado."));
        pago.getDetallePagos().forEach(detalle -> {
            if (detalle.getaCobrar() != null) {
                detalle.setImporte(detalle.getValorBase() - detalle.getaCobrar());
            } else {
                detalle.setImporte(detalle.getValorBase());
            }
            log.info("Detalle recalculado: id={}, concepto={}, importe={}, aCobrar={}",
                    detalle.getId(), detalle.getConcepto(), detalle.getImporte(), detalle.getaCobrar());
        });
        log.info("Pago obtenido: id={}, monto={}", pago.getId(), pago.getMonto());
        return pagoMapper.toDTO(pago);
    }

    @Override
    public List<PagoResponse> listarPagos() {
        log.info("Listando todos los pagos");
        List<PagoResponse> pagos = pagoRepositorio.findAll()
                .stream()
                .map(pagoMapper::toDTO)
                .collect(Collectors.toList());
        log.info("Total de pagos listados: {}", pagos.size());
        return pagos;
    }

    @Override
    @Transactional
    public PagoResponse actualizarPago(Long id, PagoModificacionRequest request) {
        log.info("Iniciando actualización del pago con id: {}", id);
        Pago pago = pagoRepositorio.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Pago no encontrado."));
        pago.setFecha(request.fecha());
        pago.setFechaVencimiento(request.fechaVencimiento());
        pago.setRecargoAplicado(request.recargoAplicado());
        pago.setBonificacionAplicada(request.bonificacionAplicada());
        pago.setActivo(request.activo());
        if (request.metodoPagoId() != null) {
            MetodoPago metodoPago = metodoPagoRepositorio.findById(request.metodoPagoId())
                    .orElseThrow(() -> new IllegalArgumentException("Método de pago no encontrado."));
            pago.setMetodoPago(metodoPago);
        }
        if (request.detallePagos() != null && !request.detallePagos().isEmpty()) {
            Map<Long, DetallePago> detallesExistentes = pago.getDetallePagos().stream()
                    .collect(Collectors.toMap(DetallePago::getId, d -> d));
            List<DetallePago> detallesActualizados = new ArrayList<>();
            for (DetallePagoRegistroRequest detalleDTO : request.detallePagos()) {
                DetallePago detalle;
                if (detalleDTO.id() != null && detallesExistentes.containsKey(detalleDTO.id())) {
                    detalle = detallesExistentes.get(detalleDTO.id());
                    // Si el detalle ya está pagado, lo dejamos intacto
                    if (detalle.getImporte() != null && detalle.getImporte() == 0) {
                        detallesActualizados.add(detalle);
                        continue;
                    }
                } else {
                    detalle = detallePagoMapper.toEntity(detalleDTO);
                    detalle.setPago(pago);
                }
                // Aseguramos que aCobrar no sea null (si viene null, se usa 0)
                detalle.setaCobrar(detalleDTO.aCobrar() != null ? detalleDTO.aCobrar() : 0);
                // Recalcular el importe (la entidad se encarga de actualizar aFavor y demás)
                detalle.calcularImporte();
                detallesActualizados.add(detalle);
                log.info("Detalle actualizado: concepto={}, importe={}, aCobrar={}",
                        detalle.getConcepto(), detalle.getImporte(), detalle.getaCobrar());
            }
            pago.getDetallePagos().clear();
            pago.getDetallePagos().addAll(detallesActualizados);
        }
        // Calcular el total abonado sumando aCobrar (usando 0 si es null)
        double totalCobrado = pago.getDetallePagos().stream()
                .mapToDouble(detalle -> detalle.getaCobrar() != null ? detalle.getaCobrar() : 0)
                .sum();
        double saldoRestante = pago.getMonto() - totalCobrado;
        pago.setSaldoRestante(saldoRestante < 0 ? 0 : saldoRestante);
        log.info("Pago actualizado: id={}, monto={}, saldoRestante={}", pago.getId(), pago.getMonto(), pago.getSaldoRestante());
        Pago actualizado = pagoRepositorio.save(pago);
        return pagoMapper.toDTO(actualizado);
    }

    @Override
    @Transactional
    public void eliminarPago(Long id) {
        log.info("Eliminando (marcando inactivo) pago con id: {}", id);
        Pago pago = pagoRepositorio.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Pago no encontrado."));
        pago.setActivo(false);
        if (pago.getDetallePagos() != null) {
            pago.getDetallePagos().forEach(detalle -> detalle.setPago(pago));
        }
        pagoRepositorio.save(pago);
        log.info("Pago marcado como inactivo: id={}", id);
    }

    @Override
    public List<PagoResponse> listarPagosPorInscripcion(Long inscripcionId) {
        log.info("Listando pagos para inscripcion id: {}", inscripcionId);
        List<PagoResponse> pagos = pagoRepositorio.findByInscripcionIdOrderByFechaDesc(inscripcionId).stream()
                .map(pagoMapper::toDTO)
                .collect(Collectors.toList());
        log.info("Total de pagos para inscripcion {}: {}", inscripcionId, pagos.size());
        return pagos;
    }

    @Override
    public List<PagoResponse> listarPagosPorAlumno(Long alumnoId) {
        log.info("Listando pagos para alumno id: {}", alumnoId);
        List<PagoResponse> pagos = pagoRepositorio.findByInscripcionAlumnoIdOrderByFechaDesc(alumnoId).stream()
                .map(pagoMapper::toDTO)
                .collect(Collectors.toList());
        log.info("Total de pagos para alumno {}: {}", alumnoId, pagos.size());
        return pagos;
    }

    @Override
    public List<PagoResponse> listarPagosVencidos() {
        LocalDate hoy = LocalDate.now();
        log.info("Listando pagos vencidos para fecha: {}", hoy);
        List<PagoResponse> pagos = pagoRepositorio.findPagosVencidos(hoy).stream()
                .map(pagoMapper::toDTO)
                .collect(Collectors.toList());
        log.info("Pagos vencidos encontrados: {}", pagos.size());
        return pagos;
    }

    @Transactional
    public void generarCuotasParaAlumnosActivos() {
        log.info("Generando cuotas para alumnos activos...");
        List<Inscripcion> inscripcionesActivas = inscripcionRepositorio.findByEstado(EstadoInscripcion.ACTIVA);
        for (Inscripcion inscripcion : inscripcionesActivas) {
            double costoBase = calculationService.calcularCostoBase(inscripcion);
            double costoFinal = costoBase;  // Ajustar según lógica de descuentos y recargos
            Pago nuevoPago = new Pago();
            nuevoPago.setFecha(LocalDate.now());
            nuevoPago.setFechaVencimiento(LocalDate.now().plusDays(30));
            nuevoPago.setMonto(costoFinal);
            nuevoPago.setSaldoRestante(costoFinal);
            nuevoPago.setActivo(true);
            nuevoPago.setInscripcion(inscripcion);
            nuevoPago.setAlumno(inscripcion.getAlumno());
            pagoRepositorio.save(nuevoPago);
            log.info("Cuota generada para inscripcion id {}: monto={}", inscripcion.getId(), costoFinal);
        }
    }

    public double calcularDeudaAlumno(Long alumnoId) {
        log.info("Calculando deuda para alumno id: {}", alumnoId);
        List<Pago> pagosPendientes = pagoRepositorio.findByInscripcionAlumnoIdOrderByFechaDesc(alumnoId)
                .stream()
                .filter(p -> p.getSaldoRestante() > 0)
                .collect(Collectors.toList());
        double deuda = pagosPendientes.stream().mapToDouble(Pago::getSaldoRestante).sum();
        log.info("Deuda total para alumno id {}: {}", alumnoId, deuda);
        return deuda;
    }

    public String getEstadoPago(Pago pago) {
        return pago.getEstado();
    }

    @Transactional
    public PagoResponse quitarRecargoManual(Long id) {
        log.info("Quitando recargo manual del pago id: {}", id);
        Pago pago = pagoRepositorio.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Pago no encontrado."));
        pago.setRecargoAplicado(false);
        if (pago.getDetallePagos() != null) {
            for (DetallePago detalle : pago.getDetallePagos()) {
                detalle.setRecargo(null);
                detalle.calcularImporte();
                log.info("Detalle id {} recalculado tras quitar recargo: importe={}, aCobrar={}",
                        detalle.getId(), detalle.getImporte(), detalle.getaCobrar());
            }
        }
        double nuevoMonto = pago.getDetallePagos().stream().mapToDouble(DetallePago::getImporte).sum();
        pago.setMonto(nuevoMonto);
        double sumPagosPrevios = pagoRepositorio.findByInscripcionIdOrderByFechaDesc(
                pago.getInscripcion().getId()).stream().mapToDouble(Pago::getMonto).sum();
        pago.setSaldoRestante(nuevoMonto - sumPagosPrevios);
        Pago actualizado = pagoRepositorio.save(pago);
        log.info("Recargo quitado y pago actualizado: id={}, nuevoMonto={}, saldoRestante={}",
                actualizado.getId(), actualizado.getMonto(), actualizado.getSaldoRestante());
        return pagoMapper.toDTO(actualizado);
    }

    @Transactional
    public PagoResponse registrarPagoParcial(Long pagoId, Double montoAbonado, Map<Long, Double> abonosPorDetalle) {
        log.info("Registrando pago parcial para pago id: {} con monto abonado: {}", pagoId, montoAbonado);
        Pago pago = pagoRepositorio.findById(pagoId)
                .orElseThrow(() -> new IllegalArgumentException("Pago no encontrado."));

        // Registrar abono parcial en PagoMedio
        PagoMedio pagoMedio = new PagoMedio();
        pagoMedio.setMonto(montoAbonado);
        pagoMedio.setPago(pago);
        pago.getPagoMedios().add(pagoMedio);

        // Actualizar cada detalle que tenga abono parcial indicado
        pago.getDetallePagos().forEach(detalle -> {
            if (abonosPorDetalle.containsKey(detalle.getId())) {
                Double abono = abonosPorDetalle.get(detalle.getId());
                // Se asigna el valor de abono; este valor se usará en la lógica interna de calcularImporte
                detalle.setAbono(abono);
                detalle.calcularImporte();
                log.info("Detalle id {} actualizado: nuevo abono={}, aCobrar recalculado={}",
                        detalle.getId(), abono, detalle.getaCobrar());
            }
        });
        // Recalcular el saldo restante del pago
        pago.recalcularSaldoRestante();
        Pago guardado = pagoRepositorio.save(pago);
        return pagoMapper.toDTO(guardado);
    }

    public CobranzaDTO generarCobranzaPorAlumno(Long alumnoId) {
        log.info("Generando cobranza consolidada para alumno id: {}", alumnoId);
        Alumno alumno = alumnoRepositorio.findById(alumnoId)
                .orElseThrow(() -> new IllegalArgumentException("Alumno no encontrado."));
        List<Pago> pagosPendientes = pagoRepositorio.findByInscripcionAlumnoIdOrderByFechaDesc(alumnoId)
                .stream()
                .filter(p -> p.getSaldoRestante() > 0)
                .collect(Collectors.toList());
        Map<String, Double> conceptosPendientes = new HashMap<>();
        double totalPendiente = 0.0;
        for (Pago pago : pagosPendientes) {
            if (pago.getDetallePagos() != null) {
                for (DetallePago detalle : pago.getDetallePagos()) {
                    double abono = detalle.getAbono() != null ? detalle.getAbono() : 0.0;
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
        log.info("Cobranza generada para alumno id {}: totalPendiente={}, conceptos={}",
                alumnoId, totalPendiente, detalles.size());
        return new CobranzaDTO(alumno.getId(), alumno.getNombre() + " " + alumno.getApellido(), totalPendiente, detalles);
    }

    @Transactional
    public PagoResponse obtenerUltimoPagoPorAlumno(Long alumnoId) {
        Pago pago = pagoRepositorio.findTopByInscripcionAlumnoIdAndActivoTrueOrderByFechaDesc(alumnoId)
                .orElseThrow(() -> new IllegalArgumentException("No se encontró pago para el alumno con ID: " + alumnoId));
        log.info("Último pago activo obtenido para alumno id {}: id={}, monto={}", alumnoId, pago.getId(), pago.getMonto());
        return pagoMapper.toDTO(pago);
    }
}

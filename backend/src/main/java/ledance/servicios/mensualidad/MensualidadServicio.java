package ledance.servicios.mensualidad;

import jakarta.persistence.criteria.Expression;
import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.Predicate;
import ledance.dto.alumno.response.AlumnoResponse;
import ledance.dto.bonificacion.response.BonificacionResponse;
import ledance.dto.disciplina.response.DisciplinaResponse;
import ledance.dto.mensualidad.MensualidadMapper;
import ledance.dto.mensualidad.request.MensualidadRegistroRequest;
import ledance.dto.mensualidad.response.MensualidadResponse;
import ledance.dto.pago.response.DetallePagoResponse;
import ledance.dto.reporte.ReporteMensualidadDTO;
import ledance.entidades.*;
import ledance.repositorios.*;
import ledance.servicios.detallepago.DetallePagoServicio;
import ledance.servicios.recargo.RecargoServicio;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.TextStyle;
import java.time.temporal.TemporalAdjusters;
import java.util.*;
import java.util.Comparator;
import java.util.Locale;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@Transactional
public class MensualidadServicio implements IMensualidadService {

    private static final Logger log = LoggerFactory.getLogger(MensualidadServicio.class);

    private final DetallePagoRepositorio detallePagoRepositorio;
    private final MensualidadRepositorio mensualidadRepositorio;
    private final InscripcionRepositorio inscripcionRepositorio;
    private final MensualidadMapper mensualidadMapper;
    private final RecargoRepositorio recargoRepositorio;
    private final BonificacionRepositorio bonificacionRepositorio;
    private final ProcesoEjecutadoRepositorio procesoEjecutadoRepositorio;
    private final RecargoServicio recargoServicio;

    public MensualidadServicio(DetallePagoRepositorio detallePagoRepositorio, MensualidadRepositorio mensualidadRepositorio,
                               InscripcionRepositorio inscripcionRepositorio,
                               MensualidadMapper mensualidadMapper,
                               RecargoRepositorio recargoRepositorio,
                               BonificacionRepositorio bonificacionRepositorio, DetallePagoServicio detallePagoServicio, ProcesoEjecutadoRepositorio procesoEjecutadoRepositorio, RecargoServicio recargoServicio) {
        this.detallePagoRepositorio = detallePagoRepositorio;
        this.mensualidadRepositorio = mensualidadRepositorio;
        this.inscripcionRepositorio = inscripcionRepositorio;
        this.mensualidadMapper = mensualidadMapper;
        this.recargoRepositorio = recargoRepositorio;
        this.bonificacionRepositorio = bonificacionRepositorio;
        this.procesoEjecutadoRepositorio = procesoEjecutadoRepositorio;
        this.recargoServicio = recargoServicio;
    }

    @Override
    public MensualidadResponse crearMensualidad(MensualidadRegistroRequest request) {
        log.info("Iniciando creacion de mensualidad para inscripcion id: {}", request.inscripcionId());
        Inscripcion inscripcion = inscripcionRepositorio.findById(request.inscripcionId())
                .orElseThrow(() -> new IllegalArgumentException("Inscripcion no encontrada"));
        log.info("Inscripcion encontrada: {}", inscripcion);

        Mensualidad mensualidad = mensualidadMapper.toEntity(request);
        log.info("Entidad mensualidad mapeada: {}", mensualidad);

        if (request.recargoId() != null) {
            Recargo recargo = recargoRepositorio.findById(request.recargoId())
                    .orElseThrow(() -> new IllegalArgumentException("Recargo no encontrado"));
            mensualidad.setRecargo(recargo);
            log.info("Recargo asignado a mensualidad: {}", recargo);
        }
        if (request.bonificacionId() != null) {
            Bonificacion bonificacion = bonificacionRepositorio.findById(request.bonificacionId())
                    .orElseThrow(() -> new IllegalArgumentException("Bonificacion no encontrada"));
            mensualidad.setBonificacion(bonificacion);
            log.info("Bonificacion asignada a mensualidad: {}", bonificacion);
        }
        mensualidad.setInscripcion(inscripcion);

        // Calcular el total a pagar (fijo) segun la formula:
        // importeInicial = (valorBase + recargos) - (descuentos)
        calcularImporteInicial(mensualidad);
        // Inicializar montoAbonado en 0
        mensualidad.setMontoAbonado(0.0);
        // Con base en importeInicial y montoAbonado se calcula el importe pendiente y se actualiza el estado.
        recalcularImportePendiente(mensualidad);
        asignarDescripcion(mensualidad);

        mensualidad = mensualidadRepositorio.save(mensualidad);
        log.info("Mensualidad creada con id: {} y total a pagar (fijo): {}", mensualidad.getId(), mensualidad.getImporteInicial());
        return mensualidadMapper.toDTO(mensualidad);
    }

    private void asignarDescripcion(Mensualidad mensualidad) {
        log.info("Asignando descripcion para mensualidad id: {}", mensualidad.getId());
        String nombreDisciplina = mensualidad.getInscripcion().getDisciplina().getNombre();
        String mes = mensualidad.getFechaGeneracion()
                .getMonth()
                .getDisplayName(TextStyle.FULL, new Locale("es", "ES")).toUpperCase();
        int anio = mensualidad.getFechaGeneracion().getYear();
        String descripcion = nombreDisciplina + " - CUOTA - " + mes + " DE " + anio;
        mensualidad.setDescripcion(descripcion);
        log.info("Descripcion asignada: {}", descripcion);
    }

    /**
     * Calcula el importeInicial (fijo) a partir del valor base, recargos y descuentos.
     * Esta operacion se realiza una unica vez (al crear o configurar la mensualidad).
     *
     * @return
     */
    private double calcularImporteInicial(Mensualidad mensualidad) {
        double valorBase = mensualidad.getValorBase();

        // Calcular descuentos y recargos, controlando valores nulos
        double totalDescuento = calcularDescuento(valorBase, mensualidad.getBonificacion());
        double totalRecargo = calcularRecargo(valorBase, mensualidad.getRecargo());

        // Calcular el total a pagar y redondear para evitar discrepancias decimales
        double totalPagar = redondear((valorBase + totalRecargo) - totalDescuento);

        mensualidad.setImporteInicial(totalPagar);
        mensualidad.setImportePendiente(totalPagar);

        log.info("Total a pagar (fijo) calculado: {}", totalPagar);
        return totalPagar;
    }

    private double calcularDescuento(double valorBase, Bonificacion bonificacion) {
        if (bonificacion == null) {
            return 0.0;
        }
        double descuentoFijo = bonificacion.getValorFijo();
        double descuentoPorcentaje = (bonificacion.getPorcentajeDescuento() / 100.0) * valorBase;
        return descuentoFijo + descuentoPorcentaje;
    }

    private double calcularRecargo(double valorBase, Recargo recargo) {
        if (recargo == null) {
            return 0.0;
        }
        double recargoFijo = 0;
        try {
            recargoFijo = recargo.getValorFijo();
        } catch (Exception e) {
            recargoFijo = 0.0;
        }
        double recargoPorcentaje = (recargo.getPorcentaje() / 100.0) * valorBase;
        return recargoFijo + recargoPorcentaje;
    }

    /**
     * Recalcula el importe pendiente (saldo) en base al importeInicial fijo y al montoAbonado acumulado.
     * Actualiza el estado: si importePendiente es 0 (redondeado), se marca como PAGADO, en caso contrario PENDIENTE.
     */
    public void recalcularImportePendiente(Mensualidad mensualidad) {
        double totalPagar = mensualidad.getImporteInicial();
        double montoAbonado = mensualidad.getMontoAbonado();
        double importePendiente = totalPagar - montoAbonado;
        importePendiente = redondear(importePendiente);
        mensualidad.setImporteInicial(importePendiente); // aqui se usa importeInicial para reflejar el saldo restante
        if (importePendiente == 0.0) {
            mensualidad.setEstado(EstadoMensualidad.PAGADO);
            mensualidad.setFechaPago(LocalDate.now());
        } else {
            mensualidad.setEstado(EstadoMensualidad.PENDIENTE);
        }
        log.info("Importe pendiente recalculado: {}. Estado: {}", importePendiente, mensualidad.getEstado());
    }

    /**
     * Metodo para redondear numeros a 2 decimales.
     */
    private double redondear(double valor) {
        return BigDecimal.valueOf(valor).setScale(2, RoundingMode.HALF_UP).doubleValue();
    }

    @Transactional
    public List<MensualidadResponse> generarMensualidadesParaMesVigente() {
        LocalDate today = LocalDate.now();
        // Consultar el flag para el proceso de mensualidades automáticas
        ProcesoEjecutado proceso = procesoEjecutadoRepositorio
                .findByProceso("MENSUALIDAD_AUTOMATICA")
                .orElse(new ProcesoEjecutado("MENSUALIDAD_AUTOMATICA", null));

        // Si ya se ejecutó hoy, se omite la generación
        if (proceso.getUltimaEjecucion() != null && proceso.getUltimaEjecucion().isEqual(today)) {
            log.info("El proceso MENSUALIDAD_AUTOMATICA ya fue ejecutado hoy: {}", today);
            return new ArrayList<>();
        }

        YearMonth ym = YearMonth.now();
        LocalDate primerDiaMes = ym.atDay(1);
        String periodo = ym.getMonth().getDisplayName(TextStyle.FULL, new Locale("es", "ES")).toUpperCase()
                + " DE " + ym.getYear();

        log.info("Generando mensualidades para el periodo: {} - {}", primerDiaMes, periodo);

        List<Inscripcion> inscripcionesActivas = inscripcionRepositorio.findByEstado(EstadoInscripcion.ACTIVA)
                .stream()
                .filter(ins -> ins.getAlumno() != null && Boolean.TRUE.equals(ins.getAlumno().getActivo()))
                .toList();
        log.info("Total de inscripciones activas encontradas: {}", inscripcionesActivas.size());

        List<MensualidadResponse> respuestas = new ArrayList<>();
        for (Inscripcion inscripcion : inscripcionesActivas) {
            log.info("Procesando inscripcion id: {}", inscripcion.getId());
            String descripcionEsperada = inscripcion.getDisciplina().getNombre() + " - CUOTA - " + periodo;

            Optional<Mensualidad> optMensualidad = mensualidadRepositorio
                    .findByInscripcionIdAndFechaGeneracionAndDescripcion(inscripcion.getId(), primerDiaMes, descripcionEsperada);
            Mensualidad mensualidad;
            if (optMensualidad.isPresent()) {
                mensualidad = optMensualidad.get();
                log.info("Mensualidad existente para inscripcion id {}: mensualidad id {}", inscripcion.getId(), mensualidad.getId());
                mensualidad.setValorBase(inscripcion.getDisciplina().getValorCuota());
                mensualidad.setBonificacion(inscripcion.getBonificacion());
                calcularImporteInicial(mensualidad);
                recalcularImportePendiente(mensualidad);
                mensualidad.setDescripcion(descripcionEsperada);
                mensualidad = mensualidadRepositorio.save(mensualidad);
                if (!detallePagoRepositorio.existsByMensualidadId(mensualidad.getId())) {
                    DetallePago detalle = registrarDetallePagoMensualidad(mensualidad, null);
                    log.info("DetallePago creado para mensualidad id {}: detalle id {}", mensualidad.getId(), detalle.getId());
                }
            } else {
                log.info("No existe mensualidad para inscripcion id {} en el periodo; creando nueva mensualidad.", inscripcion.getId());
                mensualidad = new Mensualidad();
                mensualidad.setInscripcion(inscripcion);
                mensualidad.setFechaCuota(primerDiaMes);
                mensualidad.setFechaGeneracion(primerDiaMes);
                mensualidad.setValorBase(inscripcion.getDisciplina().getValorCuota());
                mensualidad.setBonificacion(inscripcion.getBonificacion());
                mensualidad.setMontoAbonado(0.0);
                calcularImporteInicial(mensualidad);
                recalcularImportePendiente(mensualidad);
                mensualidad.setEstado(EstadoMensualidad.PENDIENTE);
                mensualidad.setDescripcion(descripcionEsperada);
                mensualidad = mensualidadRepositorio.save(mensualidad);
                log.info("Mensualidad creada para inscripcion id {}: mensualidad id {}, importe pendiente = {}",
                        inscripcion.getId(), mensualidad.getId(), mensualidad.getImporteInicial());

                DetallePago detalle = registrarDetallePagoMensualidad(mensualidad, null);
                log.info("DetallePago creado para mensualidad id {}: detalle id {}", mensualidad.getId(), detalle.getId());
            }

            // --- Aplicar recargo basado en el primer día del mes generado ---
            // Aquí ya se utiliza la fecha de cuota (primer día del mes)
            LocalDate fechaComparacion = mensualidad.getFechaCuota();
            log.info("Para mensualidad id {}: fechaCuota={}, hoy={}",
                    mensualidad.getId(), mensualidad.getFechaCuota(), today);
            if (today.isAfter(fechaComparacion) || today.isEqual(fechaComparacion)) {
                // Se aplica el recargo si no se ha asignado o si es diferente.
                // En este ejemplo, asumimos que se usa un recargo configurado para el día 1.
                Optional<Recargo> optRecargo = recargoRepositorio.findByDiaDelMesAplicacion(1);
                if (optRecargo.isPresent()) {
                    Recargo recargo = optRecargo.get();
                    if (mensualidad.getRecargo() == null || !mensualidad.getRecargo().getId().equals(recargo.getId())) {
                        log.info("Aplicando recargo de día {} a mensualidad id={}", recargo.getDiaDelMesAplicacion(), mensualidad.getId());
                        mensualidad.setRecargo(recargo);
                        mensualidadRepositorio.save(mensualidad);
                        recargoServicio.recalcularImporteMensualidad(mensualidad);

                        Optional<DetallePago> optDetalle = detallePagoRepositorio.findByMensualidad(mensualidad);
                        if (optDetalle.isPresent()) {
                            DetallePago detalle = optDetalle.get();
                            detalle.setRecargo(recargo);
                            detalle.setTieneRecargo(true);
                            log.info("Recalculando importe en DetallePago id={} para recargo", detalle.getId());
                            recargoServicio.recalcularImporteDetalle(detalle);
                        }
                    } else {
                        log.info("Mensualidad id {} ya tiene asignado el recargo adecuado.", mensualidad.getId());
                        recargoServicio.recalcularImporteMensualidad(mensualidad);
                        detallePagoRepositorio.findByMensualidad(mensualidad)
                                .ifPresent(recargoServicio::recalcularImporteDetalle);
                    }
                } else {
                    log.info("No se encontró un recargo configurado para el día 1 en la base de datos.");
                }
            } else {
                log.info("No se aplica recargo para mensualidad id {}: hoy {} es anterior a la fecha de cuota {}",
                        mensualidad.getId(), today, fechaComparacion);
            }
            // --- Fin de la aplicación de recargo ---

            respuestas.add(mensualidadMapper.toDTO(mensualidad));
        }
        proceso.setUltimaEjecucion(today);
        procesoEjecutadoRepositorio.save(proceso);
        log.info("Proceso MENSUALIDAD_AUTOMATICA completado. Flag actualizado a {}", today);

        return respuestas;
    }

    @Override
    public MensualidadResponse actualizarMensualidad(Long id, MensualidadRegistroRequest request) {
        log.info("Iniciando actualizacion de mensualidad id: {}", id);
        Mensualidad mensualidad = mensualidadRepositorio.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Mensualidad no encontrada"));
        log.info("Mensualidad actual antes de actualizar: {}", mensualidad);

        mensualidad.setFechaCuota(request.fechaCuota());
        mensualidad.setValorBase(request.valorBase());
        mensualidad.setEstado(Enum.valueOf(EstadoMensualidad.class, request.estado()));
        if (request.recargoId() != null) {
            Recargo recargo = recargoRepositorio.findById(request.recargoId())
                    .orElseThrow(() -> new IllegalArgumentException("Recargo no encontrado"));
            mensualidad.setRecargo(recargo);
            log.info("Recargo actualizado en mensualidad: {}", recargo);
        } else {
            mensualidad.setRecargo(null);
            log.info("Recargo eliminado de mensualidad id: {}", id);
        }
        if (request.bonificacionId() != null) {
            Bonificacion bonificacion = bonificacionRepositorio.findById(request.bonificacionId())
                    .orElseThrow(() -> new IllegalArgumentException("Bonificacion no encontrada"));
            mensualidad.setBonificacion(bonificacion);
            log.info("Bonificacion actualizada en mensualidad: {}", bonificacion);
        } else {
            mensualidad.setBonificacion(null);
            log.info("Bonificacion eliminada de mensualidad id: {}", id);
        }
        // Se recalcula el total fijo y luego se recalcula el importe pendiente
        calcularImporteInicial(mensualidad);
        recalcularImportePendiente(mensualidad);
        asignarDescripcion(mensualidad);
        mensualidad = mensualidadRepositorio.save(mensualidad);
        log.info("Mensualidad actualizada: id={}, importe pendiente = {}", mensualidad.getId(), mensualidad.getImporteInicial());
        return mensualidadMapper.toDTO(mensualidad);
    }

    @Transactional
    public DetallePago generarCuota(Long inscripcionId, int mes, int anio, Pago pagoPendiente) {
        log.info("[generarCuota] Generando cuota para inscripción id: {} para {}/{} y asociando pago id: {}",
                inscripcionId, mes, anio, pagoPendiente.getId());
        LocalDate inicio = LocalDate.of(anio, mes, 1);
        // Generar (o recuperar) la mensualidad
        Mensualidad mensualidad = generarOModificarMensualidad(inscripcionId, inicio, false);
        if (mensualidad == null) {
            return null;
        }

        // Verificar si ya existe un DetallePago para esta mensualidad para evitar duplicados
        DetallePago detallePago;
        if (detallePagoRepositorio.existsByMensualidadId(mensualidad.getId())) {
            detallePago = detallePagoRepositorio.findByMensualidad(mensualidad).get();
            log.info("Ya existe DetallePago para Mensualidad id={}", mensualidad.getId());
        } else {
            detallePago = registrarDetallePagoMensualidad(mensualidad, pagoPendiente);
            log.info("[generarCuota] DetallePago para Mensualidad id={} creado.", mensualidad.getId());
        }
        if(detallePago.getTieneRecargo()){

        }
        // --- Aplicar recargo para el mismo mes (recargo configurado para el día 15) ---
        Optional<Recargo> optRecargo15 = recargoRepositorio.findByDiaDelMesAplicacion(15);
        // Se calcula la fecha de comparación: se toma la fecha de cuota (el 1ro) y se reemplaza el día por 15
        LocalDate fechaComparacion = mensualidad.getFechaCuota().withDayOfMonth(15);
        LocalDate today = LocalDate.now();
        log.info("Para mensualidad id {}: fechaCuota={}, fechaComparacion (día 15)={}, hoy={}",
                mensualidad.getId(), mensualidad.getFechaCuota(), fechaComparacion, today);
        if (today.isAfter(fechaComparacion) || today.isEqual(fechaComparacion)) {
            Long recargoIdActual = (mensualidad.getRecargo() != null) ? mensualidad.getRecargo().getId() : null;
            Long recargoIdNuevo = optRecargo15.map(Recargo::getId).orElse(null);
            if (recargoIdActual == null || !recargoIdActual.equals(recargoIdNuevo)) {
                if (optRecargo15.isPresent()) {
                    Recargo recargo15 = optRecargo15.get();
                    log.info("Aplicando recargo de día {} a mensualidad id={}",
                            recargo15.getDiaDelMesAplicacion(), mensualidad.getId());
                    mensualidad.setRecargo(recargo15);
                    mensualidadRepositorio.save(mensualidad);
                    recargoServicio.recalcularImporteMensualidad(mensualidad);

                    // Actualizar el DetallePago asociado
                    Optional<DetallePago> optDetalle = detallePagoRepositorio.findByMensualidad(mensualidad);
                    if (optDetalle.isPresent()) {
                        DetallePago detalle = optDetalle.get();
                        detalle.setRecargo(recargo15);
                        detalle.setTieneRecargo(true);
                        log.info("Recalculando importe en DetallePago id={} para recargo", detalle.getId());
                        recargoServicio.recalcularImporteDetalle(detalle);
                    }
                } else {
                    log.info("No se encontró un recargo configurado para el día 15.");
                }
            } else {
                log.info("Mensualidad id {} ya tiene asignado el recargo adecuado.", mensualidad.getId());
                recargoServicio.recalcularImporteMensualidad(mensualidad);
                detallePagoRepositorio.findByMensualidad(mensualidad)
                        .ifPresent(recargoServicio::recalcularImporteDetalle);
            }
        } else {
            log.info("No se aplica recargo para mensualidad id {}: hoy {} es anterior a la fechaComparacion (día 15) {}",
                    mensualidad.getId(), today, fechaComparacion);
        }
        // --- Fin de la lógica de recargo ---

        return detallePago;
    }

    @Transactional
    public Mensualidad generarCuota(Long inscripcionId, int mes, int anio) {
        log.info("[generarCuota] Generando cuota para inscripción id: {} para {}/{}", inscripcionId, mes, anio);
        LocalDate inicio = LocalDate.of(anio, mes, 1);
        Mensualidad mensualidad = generarOModificarMensualidad(inscripcionId, inicio, true);
        if (mensualidad != null) {
            // Si no existe aún un DetallePago para esta mensualidad, se crea; de lo contrario se deja como está
            if (!detallePagoRepositorio.existsByMensualidadId(mensualidad.getId())) {
                registrarDetallePagoMensualidad(mensualidad);
                log.info("[generarCuota] DetallePago para Mensualidad id={} creado.", mensualidad.getId());
            } else {
                log.info("Ya existe DetallePago para Mensualidad id={}", mensualidad.getId());
            }

            // --- Aplicar recargo para el mismo mes (usar recargo del día 15) ---
            Optional<Recargo> optRecargo15 = recargoRepositorio.findByDiaDelMesAplicacion(15);
            LocalDate fechaComparacion = mensualidad.getFechaCuota().withDayOfMonth(15);
            LocalDate today = LocalDate.now();
            log.info("Para mensualidad id {}: fechaCuota={}, fechaComparacion (día 15)={}, hoy={}",
                    mensualidad.getId(), mensualidad.getFechaCuota(), fechaComparacion, today);
            if (today.isAfter(fechaComparacion) || today.isEqual(fechaComparacion)) {
                Long recargoIdActual = (mensualidad.getRecargo() != null) ? mensualidad.getRecargo().getId() : null;
                Long recargoIdNuevo = optRecargo15.map(Recargo::getId).orElse(null);
                if (recargoIdActual == null || !recargoIdActual.equals(recargoIdNuevo)) {
                    if (optRecargo15.isPresent()) {
                        Recargo recargo15 = optRecargo15.get();
                        log.info("Aplicando recargo de día {} a mensualidad id={}",
                                recargo15.getDiaDelMesAplicacion(), mensualidad.getId());
                        mensualidad.setRecargo(recargo15);
                        mensualidadRepositorio.save(mensualidad);
                        recargoServicio.recalcularImporteMensualidad(mensualidad);

                        Optional<DetallePago> optDetalle = detallePagoRepositorio.findByMensualidad(mensualidad);
                        if (optDetalle.isPresent()) {
                            DetallePago detalle = optDetalle.get();
                            detalle.setRecargo(recargo15);
                            detalle.setTieneRecargo(true);
                            log.info("Recalculando importe en DetallePago id={} para recargo", detalle.getId());
                            recargoServicio.recalcularImporteDetalle(detalle);
                        }
                    } else {
                        log.info("No se encontró un recargo configurado para el día 15.");
                    }
                } else {
                    log.info("Mensualidad id {} ya tiene asignado el recargo adecuado.", mensualidad.getId());
                    recargoServicio.recalcularImporteMensualidad(mensualidad);
                    detallePagoRepositorio.findByMensualidad(mensualidad)
                            .ifPresent(recargoServicio::recalcularImporteDetalle);
                }
            } else {
                log.info("No se aplica recargo para mensualidad id {}: hoy {} es anterior a la fechaComparacion (día 15) {}",
                        mensualidad.getId(), today, fechaComparacion);
            }
            // --- Fin de la lógica de recargo ---
        }
        return mensualidad;
    }

    /**
     * Método que busca o crea una mensualidad para una inscripción en base a:
     * - La fecha de generación (que debe ser el primer día del mes).
     * - La descripción, que sigue el formato "[DISCIPLINA] - CUOTA - [PERIODO]".
     *
     * @param inscripcionId     ID de la inscripción.
     * @param inicio            El primer día del mes (usado tanto para fechaCuota como para fechaGeneracion).
     * @param devolverExistente Si es true, se retorna la mensualidad existente; de lo contrario, se retorna null.
     * @return La mensualidad encontrada o generada, o null si ya existía y no se requiere devolverla.
     */
    private Mensualidad generarOModificarMensualidad(Long inscripcionId, LocalDate inicio, boolean devolverExistente) {
        // Recuperar la inscripción
        Inscripcion inscripcion = inscripcionRepositorio.findById(inscripcionId)
                .orElseThrow(() -> new IllegalArgumentException("Inscripción no encontrada"));

        // Construir el período y la descripción esperada, por ejemplo: "DANZA - CUOTA - MARZO DE 2025"
        YearMonth ym = YearMonth.of(inicio.getYear(), inicio.getMonth());
        String periodo = ym.getMonth().getDisplayName(TextStyle.FULL, new Locale("es", "ES")).toUpperCase()
                + " DE " + ym.getYear();
        String descripcionEsperada = inscripcion.getDisciplina().getNombre() + " - CUOTA - " + periodo;

        // Buscar si ya existe una mensualidad para esa inscripción con la fecha de generación (inicio) y descripción
        Optional<Mensualidad> optMensualidad = mensualidadRepositorio
                .findByInscripcionIdAndFechaGeneracionAndDescripcion(inscripcionId, inicio, descripcionEsperada);
        if (optMensualidad.isPresent()) {
            log.info("[generarCuota] Mensualidad ya existe para inscripción id: {} para {}", inscripcionId, inicio);
            Mensualidad mensualidadExistente = optMensualidad.get();
            // Actualizar la descripción (opcional: en mayúsculas)
            mensualidadExistente.setDescripcion(mensualidadExistente.getDescripcion().toUpperCase());
            // Si el estado no es PENDIENTE, actualizarlo y guardar
            if (!mensualidadExistente.getEstado().equals(EstadoMensualidad.PENDIENTE)) {
                mensualidadExistente.setEstado(EstadoMensualidad.PENDIENTE);
                mensualidadExistente = mensualidadRepositorio.save(mensualidadExistente);
                log.info("[generarCuota] Mensualidad actualizada a PENDIENTE, id: {}", mensualidadExistente.getId());
            }
            // Actualizar la bonificación
            mensualidadExistente.setBonificacion(inscripcion.getBonificacion());
            return devolverExistente ? mensualidadExistente : null;
        }

        // Si no existe, se crea una nueva mensualidad.
        Mensualidad nuevaMensualidad = new Mensualidad();
        nuevaMensualidad.setInscripcion(inscripcion);
        // Fijar cuota y fecha de generación al primer día del mes (inicio)
        nuevaMensualidad.setFechaCuota(inicio);
        nuevaMensualidad.setFechaGeneracion(inicio);
        nuevaMensualidad.setValorBase(inscripcion.getDisciplina().getValorCuota());
        nuevaMensualidad.setBonificacion(inscripcion.getBonificacion());
        nuevaMensualidad.setRecargo(null);
        nuevaMensualidad.setMontoAbonado(0.0);

        double importeInicial = calcularImporteInicial(nuevaMensualidad);
        nuevaMensualidad.setImporteInicial(importeInicial);
        nuevaMensualidad.setImportePendiente(importeInicial);
        nuevaMensualidad.setEstado(EstadoMensualidad.PENDIENTE);
        nuevaMensualidad.setDescripcion(descripcionEsperada);
        log.info("[generarCuota] Descripción asignada a la mensualidad: {}", nuevaMensualidad.getDescripcion());

        nuevaMensualidad = mensualidadRepositorio.save(nuevaMensualidad);
        log.info("[generarCuota] Cuota generada: id={} para inscripción id {} con importeInicial={}",
                nuevaMensualidad.getId(), inscripcionId, nuevaMensualidad.getImporteInicial());
        return nuevaMensualidad;
    }

    @Transactional
    public void registrarDetallePagoMensualidad(Mensualidad mensualidad) {
        log.info("[registrarDetallePagoMensualidad] Iniciando registro del DetallePago para Mensualidad id={}", mensualidad.getId());

        // Verificar si ya existe un DetallePago asociado a esta mensualidad
        boolean existeDetalle = detallePagoRepositorio.existsByMensualidadId(mensualidad.getId());
        if (existeDetalle) {
            log.info("[registrarDetallePagoMensualidad] Ya existe un DetallePago para Mensualidad id={}. No se creará uno nuevo.", mensualidad.getId());
            return;
        }

        // Crear el nuevo DetallePago
        DetallePago detalle = new DetallePago();
        detalle.setVersion(0L);

        // Asignar datos del alumno y la descripción a partir de la mensualidad
        Alumno alumno = mensualidad.getInscripcion().getAlumno();
        detalle.setAlumno(alumno);
        detalle.setDescripcionConcepto(mensualidad.getDescripcion());

        // Asignar el valor base y calcular importes
        Double valorBase = mensualidad.getValorBase();
        detalle.setValorBase(valorBase);
        double importeInicial = mensualidad.getImporteInicial();
        detalle.setImporteInicial(importeInicial);

        // Inicialmente, aCobrar es 0.0
        double aCobrar = 0.0;
        detalle.setaCobrar(aCobrar);
        double importePendiente = importeInicial - aCobrar;
        detalle.setImportePendiente(importePendiente);
        detalle.setCobrado(importePendiente == 0);

        // Asignar tipo y fecha de registro
        detalle.setTipo(TipoDetallePago.MENSUALIDAD);
        detalle.setFechaRegistro(LocalDate.now());

        // Relacionar la mensualidad con el DetallePago
        detalle.setMensualidad(mensualidad);

        // --- Aplicar recargo si corresponde ---
        if (mensualidad.getRecargo() != null) {
            detalle.setRecargo(mensualidad.getRecargo());
            detalle.setTieneRecargo(true);
            log.info("[registrarDetallePagoMensualidad] Aplicando recargo id={} al DetallePago para Mensualidad id={}",
                    mensualidad.getRecargo().getId(), mensualidad.getId());
            recargoServicio.recalcularImporteDetalle(detalle);
        }
        // --- Fin de lógica de recargo ---

        // Guardar el DetallePago
        DetallePago savedDetalle = detallePagoRepositorio.save(detalle);
        log.info("[registrarDetallePagoMensualidad] DetallePago para Mensualidad id={} creado con importeInicial={} e importePendiente={}",
                mensualidad.getId(), importeInicial, importePendiente);
    }

    @Transactional
    public DetallePago registrarDetallePagoMensualidad(Mensualidad mensualidad, Pago pagoPendiente) {
        log.info("[registrarDetallePagoMensualidad] Iniciando registro del DetallePago para Mensualidad id={}", mensualidad.getId());

        // Verificar existencia previa del DetallePago para la mensualidad
        boolean existeDetalle = detallePagoRepositorio.existsByMensualidadId(mensualidad.getId());
        log.info("[registrarDetallePagoMensualidad] Existe DetallePago para mensualidad id={}: {}", mensualidad.getId(), existeDetalle);
        if (existeDetalle) {
            log.info("[registrarDetallePagoMensualidad] Ya existe un DetallePago para Mensualidad id={}. No se creará uno nuevo.", mensualidad.getId());
            return null;
        }

        // Crear nueva instancia de DetallePago
        DetallePago detalle = new DetallePago();
        detalle.setVersion(0L);
        log.info("[registrarDetallePagoMensualidad] Se creó instancia de DetallePago con version=0");

        // Asignar datos del alumno y la descripción a partir de la mensualidad
        Alumno alumno = mensualidad.getInscripcion().getAlumno();
        detalle.setAlumno(alumno);
        log.info("[registrarDetallePagoMensualidad] Alumno asignado: id={}, nombre={}", alumno.getId(), alumno.getNombre());
        detalle.setDescripcionConcepto(mensualidad.getDescripcion());
        log.info("[registrarDetallePagoMensualidad] Descripción asignada al DetallePago: {}", mensualidad.getDescripcion());

        // Asignar el valor base y calcular importes
        Double valorBase = mensualidad.getValorBase();
        detalle.setValorBase(valorBase);
        log.info("[registrarDetallePagoMensualidad] Valor base asignado: {}", valorBase);
        detalle.setBonificacion(mensualidad.getBonificacion());
        if (mensualidad.getBonificacion() != null) {
            log.info("[registrarDetallePagoMensualidad] Bonificación asignada: id={}, descripcion={}",
                    mensualidad.getBonificacion().getId(), mensualidad.getBonificacion().getDescripcion());
        } else {
            log.info("[registrarDetallePagoMensualidad] No se asignó bonificación (es null).");
        }
        double importeInicial = mensualidad.getImporteInicial();
        detalle.setImporteInicial(importeInicial);
        log.info("[registrarDetallePagoMensualidad] Importe inicial asignado: {}", importeInicial);

        // Inicialmente, aCobrar es 0.0
        double aCobrar = 0.0;
        detalle.setaCobrar(aCobrar);
        log.info("[registrarDetallePagoMensualidad] aCobrar inicial asignado: {}", aCobrar);
        double importePendiente = importeInicial - aCobrar;
        detalle.setImportePendiente(importePendiente);
        log.info("[registrarDetallePagoMensualidad] Importe pendiente calculado (importeInicial - aCobrar): {} - {} = {}", importeInicial, aCobrar, importePendiente);
        detalle.setCobrado(importePendiente == 0);
        log.info("[registrarDetallePagoMensualidad] Estado 'cobrado' asignado: {}", (importePendiente == 0));

        // Asignar tipo y fecha de registro
        detalle.setTipo(TipoDetallePago.MENSUALIDAD);
        log.info("[registrarDetallePagoMensualidad] Tipo asignado: {}", TipoDetallePago.MENSUALIDAD);
        LocalDate fechaRegistro = LocalDate.now();
        detalle.setFechaRegistro(fechaRegistro);
        log.info("[registrarDetallePagoMensualidad] Fecha de registro asignada: {}", fechaRegistro);

        // Asociar la mensualidad y el pago pendiente al DetallePago
        detalle.setMensualidad(mensualidad);
        log.info("[registrarDetallePagoMensualidad] Mensualidad asociada al DetallePago: id={}", mensualidad.getId());
        detalle.setPago(pagoPendiente);
        log.info("[registrarDetallePagoMensualidad] Pago pendiente asociado al DetallePago: id={}", pagoPendiente.getId());

        // --- Aplicar recargo si la mensualidad tiene asignado uno Y si el flag 'tieneRecargo' lo permite ---
        // Se verifica si el front end indica explícitamente que NO se debe aplicar recargo.
        if (detalle.getTieneRecargo() != null && !detalle.getTieneRecargo()) {
            log.info("[registrarDetallePagoMensualidad] El detalle indica que NO se debe aplicar recargo (tieneRecargo=false).");
            detalle.setRecargo(null);
            detalle.setImportePendiente(importeInicial - aCobrar);
        } else if (mensualidad.getRecargo() != null) {
            Recargo recargo = mensualidad.getRecargo();
            detalle.setRecargo(recargo);
            // Si el flag no estaba definido, se establece en true (aplicar recargo)
            detalle.setTieneRecargo(true);
            log.info("[registrarDetallePagoMensualidad] Se asigna recargo al DetallePago: id del recargo={}", recargo.getId());
            // Calcular el valor del recargo sobre la base
            double recargoValue = calcularRecargo(importeInicial, recargo);
            log.info("[registrarDetallePagoMensualidad] Valor calculado del recargo: {}", recargoValue);
            double nuevoTotal = importeInicial + recargoValue;
            log.info("[registrarDetallePagoMensualidad] Nuevo total calculado (base + recargo): {} + {} = {}", importeInicial, recargoValue, nuevoTotal);
            double nuevoPendiente = nuevoTotal - aCobrar;
            detalle.setImportePendiente(nuevoPendiente);
            log.info("[registrarDetallePagoMensualidad] Nuevo importe pendiente calculado: {} - {} = {}", nuevoTotal, aCobrar, nuevoPendiente);
        } else {
            log.info("[registrarDetallePagoMensualidad] No se aplicó recargo (la mensualidad no tiene recargo asignado).");
        }
        // --- Fin de la lógica de recargo ---

        // Persistir el DetallePago
        DetallePago savedDetalle = detallePagoRepositorio.save(detalle);
        log.info("[registrarDetallePagoMensualidad] DetallePago para Mensualidad id={} creado y guardado. Importe inicial={}, Importe pendiente={}",
                mensualidad.getId(), importeInicial, savedDetalle.getImportePendiente());
        return savedDetalle;
    }

    public MensualidadResponse obtenerMensualidad(Long id) {
        log.info("Obteniendo mensualidad con id: {}", id);
        Mensualidad mensualidad = mensualidadRepositorio.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Mensualidad no encontrada"));
        log.info("Mensualidad obtenida: {}", mensualidad);
        return mensualidadMapper.toDTO(mensualidad);
    }

    public List<MensualidadResponse> listarMensualidades() {
        log.info("Listando todas las mensualidades");
        List<MensualidadResponse> respuestas = mensualidadRepositorio.findAll()
                .stream()
                .map(mensualidadMapper::toDTO)
                .collect(Collectors.toList());
        log.info("Total de mensualidades listadas: {}", respuestas.size());
        return respuestas;
    }

    @Override
    public List<MensualidadResponse> listarPorInscripcion(Long inscripcionId) {
        log.info("Listando mensualidades para inscripcion id: {}", inscripcionId);
        List<MensualidadResponse> respuestas = mensualidadRepositorio.findByInscripcionId(inscripcionId)
                .stream()
                .map(mensualidadMapper::toDTO)
                .collect(Collectors.toList());
        log.info("Mensualidades encontradas para inscripcion id {}: {}", inscripcionId, respuestas.size());
        return respuestas;
    }

    @Override
    public void eliminarMensualidad(Long id) {
        log.info("Eliminando mensualidad con id: {}", id);
        if (!mensualidadRepositorio.existsById(id)) {
            throw new IllegalArgumentException("Mensualidad no encontrada");
        }
        mensualidadRepositorio.deleteById(id);
        log.info("Mensualidad eliminada: id={}", id);
    }

    public List<ReporteMensualidadDTO> buscarMensualidadesAlumnoPorMes(LocalDate fechaMes, String alumnoNombre) {
        log.info("Buscando mensualidades para alumno '{}' en el mes de {}", alumnoNombre, fechaMes);
        LocalDate primerDia = fechaMes.withDayOfMonth(1);
        LocalDate ultimoDia = fechaMes.withDayOfMonth(fechaMes.lengthOfMonth());
        Specification<Mensualidad> spec = (root, query, cb) ->
                cb.between(root.get("fechaGeneracion"), primerDia, ultimoDia);
        if (alumnoNombre != null && !alumnoNombre.isEmpty()) {
            spec = spec.and((root, query, cb) -> {
                Join<Mensualidad, Inscripcion> inscripcion = root.join("inscripcion");
                Predicate inscripcionActiva = cb.equal(inscripcion.get("estado"), EstadoInscripcion.ACTIVA);
                Join<Inscripcion, Alumno> alumno = inscripcion.join("alumno");
                Expression<String> fullName = cb.concat(cb.concat(cb.lower(alumno.get("nombre")), " "), cb.lower(alumno.get("apellido")));
                Predicate fullNameLike = cb.like(fullName, "%" + alumnoNombre.toLowerCase() + "%");
                return cb.and(inscripcionActiva, fullNameLike);
            });
        }
        List<Mensualidad> mensualidades = mensualidadRepositorio.findAll(spec);
        log.info("Total de mensualidades encontradas: {}", mensualidades.size());
        return mensualidades.stream()
                .map(this::mapearReporte)
                .collect(Collectors.toList());
    }

    public ReporteMensualidadDTO mapearReporte(Mensualidad mensualidad) {
        // Buscar el DetallePago asociado de tipo MENSUALIDAD (se asume que existe al menos uno)
        DetallePago detallePago = mensualidad.getDetallePagos().stream()
                .filter(detalle -> detalle.getTipo() == TipoDetallePago.MENSUALIDAD)
                .findFirst()
                .orElse(null);

        // Si no se encuentra un detalle de pago correspondiente, se puede optar por retornar null
        if (detallePago == null) {
            return null;
        }

        // Determinar si se debe considerar la mensualidad como abonada:
        // - Si su estado es PAGADO, o
        // - Si el importe pendiente es menor que el importe inicial (lo que indica que se realizaron abonos parciales)
        boolean abonado = mensualidad.getEstado() == EstadoMensualidad.PAGADO
                || (mensualidad.getImportePendiente() != null
                && mensualidad.getImporteInicial() != null
                && mensualidad.getImportePendiente() < mensualidad.getImporteInicial());
        String estadoReporte = abonado ? "Abonado" : "Pendiente";

        // Mapeo de datos relacionados (si cuentas con los mappers correspondientes)
        AlumnoResponse alumnoResponse = null;
        DisciplinaResponse disciplinaResponse = null;
        BonificacionResponse bonificacionResponse = null;
        // Ejemplo:
        // alumnoResponse = alumnoMapper.toResponse(mensualidad.getInscripcion().getAlumno());
        // disciplinaResponse = disciplinaMapper.toResponse(mensualidad.getInscripcion().getDisciplina());
        // if (detallePago.getBonificacion() != null) {
        //     bonificacionResponse = bonificacionMapper.toResponse(detallePago.getBonificacion());
        // }

        return new ReporteMensualidadDTO(
                mensualidad.getId(),                                           // Id de la mensualidad
                alumnoResponse,                                                // Alumno (a mapear si se requiere)
                detallePago.getCuotaOCantidad() != null
                        ? detallePago.getCuotaOCantidad()
                        : "CUOTA",                                             // Tipo o cantidad, usando el valor del detalle o un valor por defecto
                detallePago.getValorBase() != null
                        ? detallePago.getValorBase()
                        : mensualidad.getValorBase(),                          // Importe base (se prioriza el del detalle)
                bonificacionResponse,                                            // Bonificación (si aplica)
                detallePago.getImporteInicial() != null
                        ? detallePago.getImporteInicial()
                        : mensualidad.getImporteInicial(),                     // Total (usando el importe inicial del detalle o de la mensualidad)
                0.0,                                                           // Recargo (puedes mapearlo si tienes el dato en detalle o mensualidad)
                estadoReporte,                                                 // Estado determinado ("Abonado" o "Pendiente")
                disciplinaResponse,                                              // Disciplina (a mapear si se requiere)
                mensualidad.getDescripcion()                                   // Descripción adicional
        );
    }

    public String determinarTipoCuota(Mensualidad mensualidad) {
        Double valorBase = mensualidad.getValorBase();
        Disciplina disciplina = mensualidad.getInscripcion().getDisciplina();
        if (valorBase.equals(disciplina.getValorCuota())) {
            return "CUOTA";
        } else if (valorBase.equals(disciplina.getClaseSuelta())) {
            return "CLASE SUELTA";
        } else if (valorBase.equals(disciplina.getClasePrueba())) {
            return "CLASE DE PRUEBA";
        }
        return "CUOTA";
    }

    public List<DetallePagoResponse> buscarMensualidades(LocalDate fechaInicio, LocalDate fechaFin, String disciplinaNombre, String profesorNombre) {
        log.info("Buscando mensualidades entre {} y {} con disciplina '{}' y profesor '{}'", fechaInicio, fechaFin, disciplinaNombre, profesorNombre);

        Specification<Mensualidad> spec = (root, query, cb) ->
                cb.between(root.get("fechaCuota"), fechaInicio, fechaFin);

        if (disciplinaNombre != null && !disciplinaNombre.isEmpty()) {
            spec = spec.and((root, query, cb) -> {
                Join<Mensualidad, Inscripcion> inscripcion = root.join("inscripcion");
                Join<Inscripcion, Disciplina> disciplina = inscripcion.join("disciplina");
                return cb.like(cb.lower(disciplina.get("nombre")), "%" + disciplinaNombre.toLowerCase() + "%");
            });
        }
        if (profesorNombre != null && !profesorNombre.isEmpty()) {
            spec = spec.and((root, query, cb) -> {
                Join<Mensualidad, Inscripcion> inscripcion = root.join("inscripcion");
                Join<Inscripcion, Disciplina> disciplina = inscripcion.join("disciplina");
                Join<Disciplina, Profesor> profesor = disciplina.join("profesor");
                return cb.like(cb.lower(profesor.get("nombre")), "%" + profesorNombre.toLowerCase() + "%");
            });
        }

        List<Mensualidad> mensualidades = mensualidadRepositorio.findAll(spec);
        log.info("Total de mensualidades encontradas: {}", mensualidades.size());

        List<DetallePagoResponse> detallePagoResponses = new ArrayList<>();
        // Para cada mensualidad, evaluamos la condición y mapeamos sus detalles de pago de tipo MENSUALIDAD
        for (Mensualidad m : mensualidades) {
            boolean procesar = m.getEstado() == EstadoMensualidad.PAGADO ||
                    (m.getImportePendiente() != null && m.getImporteInicial() != null &&
                            m.getImportePendiente() < m.getImporteInicial());
            if (procesar && m.getDetallePagos() != null) {
                List<DetallePago> detalles = m.getDetallePagos().stream()
                        .filter(detalle -> detalle.getTipo() == TipoDetallePago.MENSUALIDAD)
                        .collect(Collectors.toList());
                for (DetallePago d : detalles) {
                    DetallePagoResponse response = mapearDetallePagoResponse(d);
                    if (response != null) {
                        detallePagoResponses.add(response);
                    }
                }
            }
        }
        return detallePagoResponses;
    }

    public DetallePagoResponse mapearDetallePagoResponse(DetallePago detalle) {
        return new DetallePagoResponse(
                detalle.getId(),
                detalle.getVersion(),
                detalle.getDescripcionConcepto(),
                detalle.getCuotaOCantidad(),
                detalle.getValorBase(),
                detalle.getBonificacion() != null ? detalle.getBonificacion().getId() : null,
                detalle.getBonificacion() != null ? detalle.getBonificacion().getDescripcion() : null,
                detalle.getRecargo() != null ? detalle.getRecargo().getId() : null,
                detalle.getaCobrar(),
                detalle.getCobrado(),
                detalle.getConcepto() != null ? detalle.getConcepto().getId() : null,
                detalle.getSubConcepto() != null ? detalle.getSubConcepto().getId() : null,
                detalle.getMensualidad() != null ? detalle.getMensualidad().getId() : null,
                detalle.getMatricula() != null ? detalle.getMatricula().getId() : null,
                detalle.getStock() != null ? detalle.getStock().getId() : null,
                detalle.getImporteInicial(),
                detalle.getImportePendiente(),
                detalle.getTipo(),
                detalle.getFechaRegistro(),
                detalle.getPago() != null ? detalle.getPago().getId() : null,
                detalle.getAlumno() != null
                        ? detalle.getAlumno().getNombre() + " " + detalle.getAlumno().getApellido()
                        : "",
                detalle.getTieneRecargo()
        );
    }

    public Mensualidad actualizarAbonoParcialMensualidad(Mensualidad mensualidad, double abonoRecibido) {
        log.info("[actualizarAbonoParcialMensualidad] Iniciando actualizacion para Mensualidad id={}. Abono recibido: {}",
                mensualidad.getId(), abonoRecibido);

        // Asegurarse que el importeInicial este establecido y no se modifique en la actualizacion
        if (mensualidad.getImporteInicial() == null || mensualidad.getImporteInicial() <= 0.0) {
            // Por ejemplo, se utiliza el valorBase como importeInicial, o se llama a un metodo de calculo
            double importeInicialCalculado = mensualidad.getValorBase();
            mensualidad.setImporteInicial(importeInicialCalculado);
            log.info("[actualizarAbonoParcialMensualidad] Se establece importeInicial={} para Mensualidad id={}",
                    importeInicialCalculado, mensualidad.getId());
        }

        // Calcular el nuevo monto abonado acumulado
        double montoAbonadoActual = mensualidad.getMontoAbonado() != null ? mensualidad.getMontoAbonado() : 0.0;
        double nuevoMontoAbonado = montoAbonadoActual + abonoRecibido;
        mensualidad.setMontoAbonado(nuevoMontoAbonado);
        log.info("[actualizarAbonoParcialMensualidad] Mensualidad id={} - Nuevo monto abonado acumulado: {}",
                mensualidad.getId(), nuevoMontoAbonado);

        // Calcular el nuevo importe pendiente
        double nuevoImportePendiente = mensualidad.getImporteInicial() - nuevoMontoAbonado;
        log.info("[actualizarAbonoParcialMensualidad] Mensualidad id={} - Nuevo importe pendiente calculado: {}",
                mensualidad.getId(), nuevoImportePendiente);

        if (nuevoImportePendiente <= 0.0) {
            mensualidad.setEstado(EstadoMensualidad.PAGADO);
            mensualidad.setImportePendiente(0.0);
            mensualidad.setFechaPago(LocalDate.now());
            log.info("[actualizarAbonoParcialMensualidad] Mensualidad id={} marcada como PAGADO. Importe pendiente: 0.0", mensualidad.getId());
        } else {
            mensualidad.setEstado(EstadoMensualidad.PENDIENTE);
            mensualidad.setImportePendiente(nuevoImportePendiente);
            log.info("[actualizarAbonoParcialMensualidad] Mensualidad id={} actualizada a PENDIENTE. Importe pendiente: {}",
                    mensualidad.getId(), nuevoImportePendiente);
        }
        return mensualidad;
    }

    /**
     * Busca una mensualidad para el alumno (a traves de su inscripcion) con la descripcion indicada.
     * Si existe y no se encuentra en estado PENDIENTE, actualiza su estado.
     * Si no existe, crea una nueva mensualidad en estado PENDIENTE.
     *
     * @param alumnoId            El id del alumno (usado en la propiedad anidada inscripcion.alumno.id)
     * @param descripcionConcepto La descripcion del concepto de la mensualidad (p.ej., "Danza - CUOTA - MARZO DE 2025")
     * @return Mensualidad con los datos de la mensualidad en estado pendiente
     */
    public Mensualidad obtenerOMarcarPendienteMensualidad(Long alumnoId, String descripcionConcepto) {
        log.info("[obtenerOMarcarPendienteMensualidad] Iniciando busqueda de mensualidad para alumnoId={}, concepto={}", alumnoId, descripcionConcepto);

        Optional<Mensualidad> optionalMensualidad = mensualidadRepositorio.findByInscripcionAlumnoIdAndDescripcion(alumnoId, descripcionConcepto);

        Mensualidad mensualidad;
        if (optionalMensualidad.isPresent()) {
            mensualidad = optionalMensualidad.get();
            log.info("[obtenerOMarcarPendienteMensualidad] Mensualidad encontrada: id={}, estado={}", mensualidad.getId(), mensualidad.getEstado());

            if (!mensualidad.getEstado().equals(EstadoMensualidad.PENDIENTE)) {
                mensualidad.setEstado(EstadoMensualidad.PENDIENTE);
                log.info("[obtenerOMarcarPendienteMensualidad] Estado de mensualidad actualizado a PENDIENTE para id={}", mensualidad.getId());
                mensualidad = mensualidadRepositorio.save(mensualidad);
                log.info("[obtenerOMarcarPendienteMensualidad] Mensualidad guardada con nuevo estado PENDIENTE.");
            } else {
                log.info("[obtenerOMarcarPendienteMensualidad] Mensualidad ya se encuentra en estado PENDIENTE. No se realizan cambios.");
            }
            return mensualidad;
        } else {
            log.info("[obtenerOMarcarPendienteMensualidad] No se encontro mensualidad con ese concepto. Buscando inscripcion activa para alumnoId={}", alumnoId);
            Optional<Inscripcion> optionalInscripcion = inscripcionRepositorio.findByAlumnoIdAndEstado(alumnoId, EstadoInscripcion.ACTIVA);

            if (optionalInscripcion.isPresent()) {
                Inscripcion inscripcion = optionalInscripcion.get();
                log.info("[obtenerOMarcarPendienteMensualidad] Inscripcion activa encontrada: id={}", inscripcion.getId());
                LocalDate now = LocalDate.now();
                int mes = now.getMonthValue();
                int anio = now.getYear();
                log.info("[obtenerOMarcarPendienteMensualidad] Generando cuota para mes={}, año={}", mes, anio);
                return generarCuota(inscripcion.getId(), mes, anio);
            } else {
                log.warn("[obtenerOMarcarPendienteMensualidad] No se encontro inscripcion activa para alumnoId={}", alumnoId);
                throw new IllegalArgumentException("No se encontro inscripcion activa para el alumno con id: " + alumnoId);
            }
        }
    }

    public Mensualidad toEntity(MensualidadResponse mensualidadResp) {
        if (mensualidadResp == null) {
            return null;
        }
        Mensualidad entidad = new Mensualidad();
        entidad.setId(mensualidadResp.id());
        entidad.setDescripcion(mensualidadResp.descripcion());
        entidad.setEstado(EstadoMensualidad.valueOf(mensualidadResp.estado()));

        // Si MensualidadResponse contiene otros campos, se asignan a la entidad
        // Por ejemplo:
        // entidad.setFechaVencimiento(mensualidadResp.getFechaVencimiento());
        // entidad.setMontoOriginal(mensualidadResp.getMontoOriginal());

        // Si existe una relacion con Inscripcion, se debe mapear tambien
        // Suponiendo que mensualidadResp tiene un objeto InscripcionResponse
        // if (mensualidadResp.getInscripcion() != null) {
        //     entidad.setInscripcion(inscripcionMapper.toEntity(mensualidadResp.getInscripcion()));
        // }

        return entidad;
    }

    public DetallePago generarCuotaAutomatica(Inscripcion inscripcion, Pago pagoPendiente) {
        int mesActual = LocalDate.now().getMonthValue();
        int anioActual = LocalDate.now().getYear();
        log.info("[MensualidadAutoService] Generando cuota automatica para inscripcion id: {} en {}/{} y asociando pago id: {}",
                inscripcion.getId(), mesActual, anioActual, pagoPendiente.getId());
        // Delegar la generacion de la cuota a la logica ya existente, pasando el pago pendiente
        return generarCuota(inscripcion.getId(), mesActual, anioActual, pagoPendiente);
    }

    public void procesarAbonoMensualidad(Mensualidad mensualidad, DetallePago detalle) {
        log.info("[procesarAbonoMensualidad] Iniciando procesamiento de abono para Mensualidad id={} con DetallePago id={}",
                mensualidad.getId(), detalle.getId());
        double abonoRecibido = detalle.getaCobrar();
        log.info("[procesarAbonoMensualidad] Abono recibido: {}", abonoRecibido);
        if (!detalle.getTieneRecargo() || detalle.getTieneRecargo() == null) {
            mensualidad.setRecargo(null);
            detalle.setRecargo(null);
        }
        // Actualizar el abono parcial en la mensualidad
        mensualidad = actualizarAbonoParcialMensualidad(mensualidad, abonoRecibido);

        // Persistir la actualizacion
        Mensualidad nuevaMensualidad = mensualidadRepositorio.save(mensualidad);
        log.info("[procesarAbonoMensualidad] Mensualidad id={} actualizada: ImporteInicial={}, MontoAbonado={}, ImportePendiente={}, Estado={}",
                nuevaMensualidad.getId(), nuevaMensualidad.getImporteInicial(), nuevaMensualidad.getMontoAbonado(), nuevaMensualidad.getImportePendiente(), nuevaMensualidad.getEstado());

    }

    /**
     * Aplica el recargo al detalle si y sólo si el flag 'tieneRecargo' es true.
     * Si viene en false, se asegura de que no se aplique recargo.
     *
     * @param detalle       El detalle de pago a procesar.
     * @param mensualidad   La mensualidad asociada.
     * @param importeInicial El importe base sin recargo.
     * @param aCobrar       El monto ya abonado.
     */
    private void aplicarRecargoSiCorresponde(DetallePago detalle, Mensualidad mensualidad, double importeInicial, double aCobrar) {
        // Si el detalle viene marcado explícitamente sin recargo, no se aplica
        if (detalle.getTieneRecargo() != null && !detalle.getTieneRecargo()) {
            detalle.setRecargo(null);
            detalle.setImportePendiente(importeInicial - aCobrar);
            return;
        }

        // Si la mensualidad tiene recargo asignado, se aplica
        if (mensualidad.getRecargo() != null) {
            Recargo recargo = mensualidad.getRecargo();
            detalle.setRecargo(recargo);
            // Aseguramos que el flag se establezca en true si no fue forzado en false
            detalle.setTieneRecargo(true);
            double recargoValue = calcularRecargo(importeInicial, recargo);
            double nuevoTotal = importeInicial + recargoValue;
            double nuevoPendiente = nuevoTotal - aCobrar;
            detalle.setImportePendiente(nuevoPendiente);
        } else {
            // En caso de no tener recargo asignado en la mensualidad, se deja sin recargo.
            detalle.setRecargo(null);
            detalle.setImportePendiente(importeInicial - aCobrar);
        }
    }

}

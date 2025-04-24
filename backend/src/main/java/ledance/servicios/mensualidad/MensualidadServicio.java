package ledance.servicios.mensualidad;

import jakarta.persistence.criteria.Expression;
import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.Predicate;
import ledance.dto.alumno.AlumnoMapper;
import ledance.dto.mensualidad.MensualidadMapper;
import ledance.dto.mensualidad.request.MensualidadRegistroRequest;
import ledance.dto.mensualidad.response.MensualidadResponse;
import ledance.dto.pago.response.DetallePagoResponse;
import ledance.dto.reporte.ReporteMensualidadDTO;
import ledance.entidades.*;
import ledance.repositorios.*;
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
import java.util.*;
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
    private final DisciplinaRepositorio disciplinaRepositorio;
    private final PagoRepositorio pagoRepositorio;
    private final UsuarioRepositorio usuarioRepositorio;
    private final AlumnoMapper alumnoMapper;
    private final AlumnoRepositorio alumnoRepositorio;

    public MensualidadServicio(DetallePagoRepositorio detallePagoRepositorio, MensualidadRepositorio mensualidadRepositorio,
                               InscripcionRepositorio inscripcionRepositorio,
                               MensualidadMapper mensualidadMapper,
                               RecargoRepositorio recargoRepositorio,
                               BonificacionRepositorio bonificacionRepositorio,
                               ProcesoEjecutadoRepositorio procesoEjecutadoRepositorio, RecargoServicio recargoServicio, DisciplinaRepositorio disciplinaRepositorio,
                               PagoRepositorio pagoRepositorio, UsuarioRepositorio usuarioRepositorio, AlumnoMapper alumnoMapper, AlumnoRepositorio alumnoRepositorio) {
        this.detallePagoRepositorio = detallePagoRepositorio;
        this.mensualidadRepositorio = mensualidadRepositorio;
        this.inscripcionRepositorio = inscripcionRepositorio;
        this.mensualidadMapper = mensualidadMapper;
        this.recargoRepositorio = recargoRepositorio;
        this.bonificacionRepositorio = bonificacionRepositorio;
        this.procesoEjecutadoRepositorio = procesoEjecutadoRepositorio;
        this.recargoServicio = recargoServicio;
        this.disciplinaRepositorio = disciplinaRepositorio;
        this.pagoRepositorio = pagoRepositorio;
        this.usuarioRepositorio = usuarioRepositorio;
        this.alumnoMapper = alumnoMapper;
        this.alumnoRepositorio = alumnoRepositorio;
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
     */
    private double calcularImporteInicial(Mensualidad mensualidad) {
        double valorBase = mensualidad.getValorBase();

        // Calcular descuentos y recargos, controlando valores nulos
        double totalDescuento = calcularDescuento(valorBase, mensualidad.getBonificacion());
        double totalRecargo = validarRecargo(valorBase, mensualidad.getRecargo());

        // Se suma el recargo cuando corresponda: el importe a pagar es (valorBase + totalRecargo) - totalDescuento
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

    public static double validarRecargo(double valorBase, Recargo recargo) {
        if (recargo == null) {
            return 0.0;
        }
        double recargoFijo;
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
        YearMonth ym = YearMonth.now();
        LocalDate primerDiaMes = ym.atDay(1);
        String periodo = ym.getMonth().getDisplayName(TextStyle.FULL, new Locale("es", "ES")).toUpperCase()
                + " DE " + ym.getYear();

        log.info("Generando mensualidades para el periodo: {} - {}", primerDiaMes, periodo);

        // Validacion del flag del proceso automatico
        ProcesoEjecutado proceso = procesoEjecutadoRepositorio.findByProceso("MENSUALIDAD_AUTOMATICA")
                .orElse(new ProcesoEjecutado("MENSUALIDAD_AUTOMATICA", null));
        if (proceso.getUltimaEjecucion() != null && proceso.getUltimaEjecucion().isEqual(today)) {
            log.info("El proceso MENSUALIDAD_AUTOMATICA ya fue ejecutado hoy: {}", today);
            return Collections.emptyList();
        }

        // Obtener las inscripciones activas (con alumno activo)
        List<Inscripcion> inscripcionesActivas = inscripcionRepositorio.findByEstado(EstadoInscripcion.ACTIVA)
                .stream()
                .filter(ins -> ins.getAlumno() != null && Boolean.TRUE.equals(ins.getAlumno().getActivo()))
                .collect(Collectors.toList());
        log.info("Total de inscripciones activas encontradas: {}", inscripcionesActivas.size());

        List<MensualidadResponse> respuestas = new ArrayList<>();

        for (Inscripcion inscripcion : inscripcionesActivas) {
            log.info("Procesando inscripcion id: {}", inscripcion.getId());
            String descripcionEsperada = inscripcion.getDisciplina().getNombre() + " - CUOTA - " + periodo;

            // Buscar la mensualidad por inscripcion, fecha de generacion y descripcion
            Optional<Mensualidad> optMensualidad = mensualidadRepositorio
                    .findByInscripcionIdAndFechaGeneracionAndDescripcion(inscripcion.getId(), primerDiaMes, descripcionEsperada);

            Mensualidad mensualidad;
            if (optMensualidad.isPresent()) {
                mensualidad = optMensualidad.get();
                log.info("Mensualidad existente para inscripcion id {}: mensualidad id {}",
                        inscripcion.getId(), mensualidad.getId());
                // Si no se ha registrado el detalle de pago, se lo registra
                if (!detallePagoRepositorio.existsByMensualidadId(mensualidad.getId())) {
                    registrarDetallePagoMensualidad(mensualidad);
                    log.info("DetallePago creado para mensualidad id {}", mensualidad.getId());
                }
            } else {
                log.info("No existe mensualidad para inscripcion id {} en el periodo; creando nueva mensualidad.",
                        inscripcion.getId());
                mensualidad = new Mensualidad();
                mensualidad.setInscripcion(inscripcion);
                mensualidad.setFechaCuota(primerDiaMes);
                mensualidad.setFechaGeneracion(primerDiaMes);
                mensualidad.setValorBase(inscripcion.getDisciplina().getValorCuota());
                mensualidad.setBonificacion(inscripcion.getBonificacion());
                mensualidad.setMontoAbonado(0.0);
                // Calculos segun la logica de negocio
                calcularImporteInicial(mensualidad);
                recalcularImportePendiente(mensualidad);
                mensualidad.setEstado(EstadoMensualidad.PENDIENTE);
                mensualidad.setDescripcion(descripcionEsperada);
                mensualidad = mensualidadRepositorio.save(mensualidad);
                log.info("Mensualidad creada para inscripcion id {}: mensualidad id {}, importe pendiente = {}",
                        inscripcion.getId(), mensualidad.getId(), mensualidad.getImporteInicial());
                registrarDetallePagoMensualidad(mensualidad);
                log.info("DetallePago creado para mensualidad id {}", mensualidad.getId());
            }
            respuestas.add(mensualidadMapper.toDTO(mensualidad));
        }

        proceso.setUltimaEjecucion(today);
        procesoEjecutadoRepositorio.save(proceso);
        log.info("Proceso MENSUALIDAD_AUTOMATICA completado. Flag actualizado a {}", today);
        return respuestas;
    }

    private void mensajeAplicarRecargo(Mensualidad mensualidad, Recargo recargo) {
        log.info("[mensajeAplicarRecargo] INICIO - Procesando mensualidad id={}", mensualidad.getId());

        // Se asigna y guarda el recargo en la mensualidad
        mensualidad.setRecargo(recargo);
        mensualidadRepositorio.save(mensualidad);
        log.info("[mensajeAplicarRecargo] Recargo guardado en mensualidad id={}", mensualidad.getId());

        // Se recalcula el importe de la mensualidad tras la asignacion del recargo
        recargoServicio.recalcularImporteMensualidad(mensualidad);
        log.info("[mensajeAplicarRecargo] Importe de mensualidad id={} recalculado.", mensualidad.getId());

        // Se busca el DetallePago asociado a la mensualidad y se le asigna el recargo
        detallePagoRepositorio.findByMensualidadAndEstadoPago(mensualidad, EstadoPago.ACTIVO)
                .ifPresentOrElse(detalle -> {
                    detalle.setRecargo(recargo);
                    detalle.setTieneRecargo(true);
                    detallePagoRepositorio.save(detalle);
                    log.info("[mensajeAplicarRecargo] Recargo asignado y guardado en DetallePago id={}", detalle.getId());
                    recargoServicio.recalcularImporteDetalle(detalle);
                    log.info("[mensajeAplicarRecargo] Importe de DetallePago id={} recalculado.", detalle.getId());
                }, () -> log.warn("[mensajeAplicarRecargo] No se encontro DetallePago para la mensualidad id={}", mensualidad.getId()));

        log.info("[mensajeAplicarRecargo] FIN - Procesamiento completado para mensualidad id={}", mensualidad.getId());
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
        log.info("[generarCuota] Generando cuota para inscripcion id: {} para {}/{} y asociando pago id: {}",
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
            detallePago = detallePagoRepositorio.findByMensualidadAndEstadoPago(mensualidad, EstadoPago.ACTIVO).get();
            log.info("Ya existe DetallePago para Mensualidad id={}", mensualidad.getId());
        } else {
            detallePago = registrarDetallePagoMensualidad(mensualidad, pagoPendiente);
            log.info("[generarCuota] DetallePago para Mensualidad id={} creado.", mensualidad.getId());
        }
        // --- Aplicar recargo para el mismo mes (recargo configurado para el dia 15) ---
        aplicarRecargoSiSeEncuentra(mensualidad);
        // --- Fin de la logica de recargo ---

        return detallePago;
    }

    @Transactional
    public Mensualidad generarCuota(Long alumnoId, Long inscripcionId, int mes, int anio) {
        log.info("[generarCuota] Generando cuota para inscripcion id: {} para {}/{}", inscripcionId, mes, anio);
        LocalDate inicio = LocalDate.of(anio, mes, 1);
        Mensualidad mensualidad = generarOModificarMensualidad(inscripcionId, inicio, true);
        if (mensualidad != null) {
            // Si no existe aun un DetallePago para esta mensualidad, se crea; de lo contrario se deja como esta
            if (!detallePagoRepositorio.existsByAlumnoIdAndDescripcionConceptoIgnoreCaseAndTipo(alumnoId, mensualidad.getDescripcion(), TipoDetallePago.MENSUALIDAD)) {
                registrarDetallePagoMensualidad(mensualidad);
                log.info("[generarCuota] DetallePago para Mensualidad id={} creado.", mensualidad.getId());
            } else {
                log.info("Ya existe DetallePago para Mensualidad id={}", mensualidad.getId());
            }
            // --- Aplicar recargo para el mismo mes (usar recargo del dia 15) ---
            aplicarRecargoSiSeEncuentra(mensualidad);
            // --- Fin de la logica de recargo ---
        }
        return mensualidad;
    }

    private void aplicarRecargoSiSeEncuentra(Mensualidad mensualidad) {
        LocalDate today = LocalDate.now();

        // Se recupera el recargo configurado para el dia 15
        Optional<Recargo> optRecargo15 = recargoRepositorio.findByDiaDelMesAplicacion(15);
        if (optRecargo15.isEmpty()) {
            log.info("No se encontro recargo configurado para el dia 15.");
            return;
        }
        Recargo recargo15 = optRecargo15.get();

        // Se determina la fecha de comparacion usando la fecha de cuota de la mensualidad
        LocalDate fechaComparacion = mensualidad.getFechaCuota().withDayOfMonth(recargo15.getDiaDelMesAplicacion());
        log.info("Procesando mensualidad id={} con fechaCuota={}, fechaComparacion={}, hoy={}",
                mensualidad.getId(), mensualidad.getFechaCuota(), fechaComparacion, today);

        // Si la fecha actual es anterior a la fecha de comparacion, no se aplica recargo
        if (today.isBefore(fechaComparacion)) {
            log.info("No se aplica recargo para mensualidad id={} porque hoy {} es anterior a fechaComparacion={}",
                    mensualidad.getId(), today, fechaComparacion);
            return;
        }

        // Si la mensualidad no tiene recargo o el recargo asignado es diferente al configurado, se procede a aplicarlo
        if (mensualidad.getRecargo() == null || !mensualidad.getRecargo().getId().equals(recargo15.getId())) {
            log.info("Aplicando recargo de dia {} a mensualidad id={}",
                    recargo15.getDiaDelMesAplicacion(), mensualidad.getId());
            mensajeAplicarRecargo(mensualidad, recargo15);
        } else {
            log.info("Mensualidad id={} ya tiene aplicado el recargo de dia {}.",
                    mensualidad.getId(), recargo15.getDiaDelMesAplicacion());

            // Si ya tiene recargo, solo lo recalculamos sobre el DETALLE activo
            detallePagoRepositorio
                    .findByMensualidadAndEstadoPago(mensualidad, EstadoPago.ACTIVO)
                    .ifPresent(detalleActivo -> {
                        recargoServicio.recalcularImporteMensualidad(mensualidad);
                        recargoServicio.recalcularImporteDetalle(detalleActivo);
                    });
        }
    }

    /**
     * Metodo que busca o crea una mensualidad para una inscripcion en base a:
     * - La fecha de generacion (que debe ser el primer dia del mes).
     * - La descripcion, que sigue el formato "[DISCIPLINA] - CUOTA - [PERIODO]".
     *
     * @param inscripcionId     ID de la inscripcion.
     * @param inicio            El primer dia del mes (usado tanto para fechaCuota como para fechaGeneracion).
     * @param devolverExistente Si es true, se retorna la mensualidad existente; de lo contrario, se retorna null.
     * @return La mensualidad encontrada o generada, o null si ya existia y no se requiere devolverla.
     */
    private Mensualidad generarOModificarMensualidad(Long inscripcionId, LocalDate inicio, boolean devolverExistente) {
        // Recuperar la inscripcion
        Inscripcion inscripcion = inscripcionRepositorio.findById(inscripcionId)
                .orElseThrow(() -> new IllegalArgumentException("Inscripcion no encontrada"));

        // Construir el periodo y la descripcion esperada, por ejemplo: "DANZA - CUOTA - MARZO DE 2025"
        YearMonth ym = YearMonth.of(inicio.getYear(), inicio.getMonth());
        String periodo = ym.getMonth().getDisplayName(TextStyle.FULL, new Locale("es", "ES")).toUpperCase()
                + " DE " + ym.getYear();
        String descripcionEsperada = inscripcion.getDisciplina().getNombre() + " - CUOTA - " + periodo;

        // Buscar si ya existe una mensualidad para esa inscripcion con la fecha de generacion (inicio) y descripcion
        Optional<Mensualidad> optMensualidad = mensualidadRepositorio
                .findByInscripcionIdAndFechaGeneracionAndDescripcion(inscripcionId, inicio, descripcionEsperada);
        if (optMensualidad.isPresent()) {
            log.info("[generarCuota] Mensualidad ya existe para inscripcion id: {} para {}", inscripcionId, inicio);
            Mensualidad mensualidadExistente = optMensualidad.get();
            // Actualizar la descripcion (opcional: en mayusculas)
            mensualidadExistente.setDescripcion(mensualidadExistente.getDescripcion());
            // Si el estado no es PENDIENTE, actualizarlo y guardar
            if (!mensualidadExistente.getEstado().equals(EstadoMensualidad.PENDIENTE)) {
                mensualidadExistente.setEstado(EstadoMensualidad.PENDIENTE);
                mensualidadExistente = mensualidadRepositorio.save(mensualidadExistente);
                log.info("[generarCuota] Mensualidad actualizada a PENDIENTE, id: {}", mensualidadExistente.getId());
            }
            // Actualizar la bonificacion
            mensualidadExistente.setBonificacion(inscripcion.getBonificacion());
            return devolverExistente ? mensualidadExistente : null;
        }

        // Si no existe, se crea una nueva mensualidad.
        Mensualidad nuevaMensualidad = new Mensualidad();
        nuevaMensualidad.setInscripcion(inscripcion);
        // Fijar cuota y fecha de generacion al primer dia del mes (inicio)
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
        log.info("[generarCuota] Descripcion asignada a la mensualidad: {}", nuevaMensualidad.getDescripcion());

        nuevaMensualidad = mensualidadRepositorio.save(nuevaMensualidad);
        log.info("[generarCuota] Cuota generada: id={} para inscripcion id {} con importeInicial={}",
                nuevaMensualidad.getId(), inscripcionId, nuevaMensualidad.getImporteInicial());
        return nuevaMensualidad;
    }

    @Transactional
    public void registrarDetallePagoMensualidad(Mensualidad mensualidad) {
        log.info("[registrarDetallePagoMensualidad] Iniciando registro del DetallePago para Mensualidad id={}", mensualidad.getId());
        // Verificar si ya existe un detalle asociado
        if (detallePagoRepositorio.existsByMensualidadId(mensualidad.getId())) {
            log.info("[registrarDetallePagoMensualidad] Ya existe un DetallePago para Mensualidad id={}. No se crea uno nuevo.", mensualidad.getId());
            return;
        }

        // Obtener el alumno de la inscripcion
        Alumno alumno = mensualidad.getInscripcion().getAlumno();
        // Obtener o crear un unico pago pendiente para el alumno
        Pago pagoAsociado = obtenerOPersistirPagoPendiente(alumno.getId());

        DetallePago detalle = new DetallePago();
        detalle.setAlumno(alumno);
        detalle.setDescripcionConcepto(mensualidad.getDescripcion());
        detalle.setCuotaOCantidad(extraerCuotaOCantidad(mensualidad.getDescripcion()));
        detalle.setValorBase(mensualidad.getValorBase());
        double importeInicial = mensualidad.getImporteInicial();
        detalle.setImporteInicial(importeInicial);
        detalle.setACobrar(0.0);
        detalle.setImportePendiente(importeInicial);
        detalle.setCobrado(importeInicial == 0.0);
        detalle.setTipo(TipoDetallePago.MENSUALIDAD);
        detalle.setFechaRegistro(LocalDate.now());
        detalle.setMensualidad(mensualidad);
        detalle.setPago(pagoAsociado);

        detallePagoRepositorio.save(detalle);
        log.info("[registrarDetallePagoMensualidad] DetallePago creado para Mensualidad id={} con importeInicial={} e importePendiente={}",
                mensualidad.getId(), importeInicial, importeInicial);

        // Actualizar el pago: agregar este detalle y ajustar los importes acumulados
        if (pagoAsociado.getDetallePagos() == null) {
            pagoAsociado.setDetallePagos(new ArrayList<>());
        }
        pagoAsociado.getDetallePagos().add(detalle);
        pagoAsociado.setMonto((pagoAsociado.getMonto() == null ? 0.0 : pagoAsociado.getMonto()) + importeInicial);
        pagoAsociado.setSaldoRestante((pagoAsociado.getSaldoRestante() == null ? 0.0 : pagoAsociado.getSaldoRestante()) + importeInicial);
        pagoRepositorio.save(pagoAsociado);
        log.info("[registrarDetallePagoMensualidad] Pago (ID={}) actualizado: nuevo monto={} y saldo restante={}",
                pagoAsociado.getId(), pagoAsociado.getMonto(), pagoAsociado.getSaldoRestante());
    }

    /**
     * Extrae todo lo que esta despues del primer guion ("-") en la descripcion.
     * Si no se encuentra el guion, se retorna una cadena vacia o un valor por defecto.
     */
    private String extraerCuotaOCantidad(String descripcion) {
        if (descripcion != null && descripcion.contains("-")) {
            // Se obtiene la posicion del primer guion y se extrae lo que viene a continuacion.
            int primerGuionIndex = descripcion.indexOf("-");
            return descripcion.substring(primerGuionIndex + 1).trim();
        }
        return "";  // O bien, retornar "1" si se requiere un valor por defecto.
    }

    @Transactional
    protected Pago obtenerOPersistirPagoPendiente(Long alumnoId) {
        // Se utiliza la busqueda de un pago "pendiente" segun la logica del repositorio.
        Pago pagoExistente = obtenerUltimoPagoPendienteEntidad(alumnoId);
        if (pagoExistente != null) {
            log.info("Se encontro un pago pendiente para el alumno id={}: Pago id={}", alumnoId, pagoExistente.getId());
            return pagoExistente;
        }
        Pago nuevoPago = new Pago();
        Alumno alumno = alumnoRepositorio.findById(alumnoId)
                .orElseThrow(() -> new RuntimeException("Alumno no encontrado para id=" + alumnoId));
        nuevoPago.setAlumno(alumno);
        nuevoPago.setFecha(LocalDate.now());
        nuevoPago.setFechaVencimiento(LocalDate.now().plusDays(30));
        nuevoPago.setImporteInicial(0.0);
        nuevoPago.setMonto(0.0);
        nuevoPago.setSaldoRestante(0.0);
        nuevoPago.setEstadoPago(EstadoPago.ACTIVO);
        pagoRepositorio.save(nuevoPago);
        log.info("No se encontro un pago pendiente; se creo un nuevo pago con ID={}", nuevoPago.getId());
        return nuevoPago;
    }

    @Transactional
    public DetallePago registrarDetallePagoMensualidad(Mensualidad mensualidad, Pago pagoPendiente) {
        log.info("[registrarDetallePagoMensualidad] Iniciando registro del DetallePago para Mensualidad id={}", mensualidad.getId());

        Optional<DetallePago> detalleExistenteOpt = detallePagoRepositorio.findByMensualidadAndEstadoPago(mensualidad, EstadoPago.ACTIVO);
        DetallePago detalle;
        if (detalleExistenteOpt.isPresent()) {
            detalle = detalleExistenteOpt.get();
            log.info("[registrarDetallePagoMensualidad] DetallePago ya existe para Mensualidad id={}. Se actualizara.", mensualidad.getId());
        } else {
            detalle = new DetallePago();
            detalle.setVersion(0L);
            log.info("[registrarDetallePagoMensualidad] Se creo instancia nueva de DetallePago con version=0");
        }

        // Asignar alumno y descripcion
        Alumno alumno = mensualidad.getInscripcion().getAlumno();
        detalle.setAlumno(alumno);
        log.info("[registrarDetallePagoMensualidad] Alumno asignado: id={}, nombre={}", alumno.getId(), alumno.getNombre());
        detalle.setDescripcionConcepto(mensualidad.getDescripcion());
        log.info("[registrarDetallePagoMensualidad] Descripcion asignada: {}", mensualidad.getDescripcion());

        // Asignar valores base, bonificacion, importeInicial, etc.
        Double valorBase = mensualidad.getValorBase();
        detalle.setValorBase(valorBase);
        detalle.setBonificacion(mensualidad.getBonificacion());
        double importeInicial = mensualidad.getImporteInicial();
        detalle.setImporteInicial(importeInicial);
        double ACobrar = 0.0;
        detalle.setACobrar(ACobrar);
        detalle.setImportePendiente(importeInicial - ACobrar);
        detalle.setCobrado((importeInicial - ACobrar) == 0);
        detalle.setTipo(TipoDetallePago.MENSUALIDAD);
        detalle.setFechaRegistro(LocalDate.now());

        // Asociar la mensualidad y el pago pendiente
        detalle.setMensualidad(mensualidad);
        detalle.setPago(pagoPendiente);
        log.info("[registrarDetallePagoMensualidad] Asociaciones establecidas: Mensualidad id={}, Pago id={}",
                mensualidad.getId(), pagoPendiente.getId());

        // Aplicar recargo si corresponde
        if (detalle.getRecargo() != null && !detalle.getTieneRecargo()) {
            log.info("[registrarDetallePagoMensualidad] El detalle indica NO aplicar recargo (tieneRecargo=false).");
            detalle.setTieneRecargo(false);
            detalle.setImportePendiente(importeInicial - ACobrar);
        } else if (mensualidad.getRecargo() != null) {
            Recargo recargo = mensualidad.getRecargo();
            detalle.setRecargo(recargo);
            detalle.setTieneRecargo(true);
            double recargoValue = validarRecargo(importeInicial, recargo);
            double nuevoTotal = importeInicial + recargoValue;
            double nuevoPendiente = nuevoTotal - ACobrar;
            detalle.setImportePendiente(nuevoPendiente);
            log.info("[registrarDetallePagoMensualidad] Se aplica recargo: {}. Nuevo total: {}. Nuevo pendiente: {}",
                    recargoValue, nuevoTotal, nuevoPendiente);
        } else {
            log.info("[registrarDetallePagoMensualidad] No se aplica recargo, ya que la mensualidad no tiene recargo asignado.");
        }

        // Persistir inmediatamente el detalle para asignarle un id
        DetallePago savedDetalle = detallePagoRepositorio.save(detalle);
        log.info("[registrarDetallePagoMensualidad] DetallePago guardado para Mensualidad id={}. Importe inicial={}, Importe pendiente={}",
                mensualidad.getId(), importeInicial, savedDetalle.getImportePendiente());
        return savedDetalle;
    }

    @Transactional
    public Pago obtenerUltimoPagoPendienteEntidad(Long alumnoId) {
        log.info("[obtenerUltimoPagoPendienteEntidad] Buscando el ultimo pago pendiente para alumnoId={}", alumnoId);
        Pago ultimo = pagoRepositorio.findTopByAlumnoIdAndEstadoPagoAndSaldoRestanteGreaterThanOrderByFechaDesc(alumnoId, EstadoPago.ACTIVO, 0.0).orElse(null);
        log.info("[obtenerUltimoPagoPendienteEntidad] Resultado para alumno id={}: {}", alumnoId, ultimo);
        return ultimo;
    }

    public MensualidadResponse obtenerMensualidad(Long id) {
        log.info("Obteniendo mensualidad con id: {}", id);
        Mensualidad mensualidad = mensualidadRepositorio.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Mensualidad no encontrada"));
        log.info("Mensualidad obtenida: {}", mensualidad);
        return mensualidadMapper.toDTO(mensualidad);
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

        return new ReporteMensualidadDTO(
                mensualidad.getId(),                                           // Id de la mensualidad
                null,                                                // Alumno (a mapear si se requiere)
                detallePago.getCuotaOCantidad() != null
                        ? detallePago.getCuotaOCantidad()
                        : "CUOTA",                                             // Tipo o cantidad, usando el valor del detalle o un valor por defecto
                detallePago.getValorBase() != null
                        ? detallePago.getValorBase()
                        : mensualidad.getValorBase(),                          // Importe base (se prioriza el del detalle)
                null,                                            // Bonificacion (si aplica)
                detallePago.getImporteInicial() != null
                        ? detallePago.getImporteInicial()
                        : mensualidad.getImporteInicial(),                     // Total (usando el importe inicial del detalle o de la mensualidad)
                0.0,                                                           // Recargo (puedes mapearlo si tienes el dato en detalle o mensualidad)
                estadoReporte,                                                 // Estado determinado ("Abonado" o "Pendiente")
                null,                                              // Disciplina (a mapear si se requiere)
                mensualidad.getDescripcion()                                   // Descripcion adicional
        );
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
        // Para cada mensualidad, evaluamos la condicion y mapeamos sus detalles de pago de tipo MENSUALIDAD
        for (Mensualidad m : mensualidades) {
            boolean procesar = m.getEstado() == EstadoMensualidad.PAGADO ||
                    (m.getImportePendiente() != null && m.getImporteInicial() != null &&
                            m.getImportePendiente() < m.getImporteInicial());
            if (procesar && m.getDetallePagos() != null) {
                List<DetallePago> detalles = m.getDetallePagos().stream()
                        .filter(detalle -> detalle.getTipo() == TipoDetallePago.MENSUALIDAD)
                        .toList();
                for (DetallePago d : detalles) {
                    d.setConcepto(d.getConcepto());
                    d.setSubConcepto(d.getSubConcepto());
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
                detalle.getACobrar(),
                detalle.getCobrado(),
                detalle.getConcepto() != null ? detalle.getConcepto().getId() : null,
                detalle.getSubConcepto() != null ? detalle.getSubConcepto().getId() : null,
                detalle.getDescripcionConcepto().contains("CUOTA") ? detalle.getMensualidad().getId() : null,
                detalle.getMatricula() != null ? detalle.getMatricula().getId() : null,
                detalle.getStock() != null ? detalle.getStock().getId() : null,
                detalle.getImporteInicial(),
                detalle.getImportePendiente(),
                detalle.getTipo(),
                detalle.getFechaRegistro(),
                detalle.getPago() != null ? detalle.getPago().getId() : null,
                alumnoMapper.toAlumnoListadoResponse(detalle.getAlumno()),
                detalle.getTieneRecargo(),
                detalle.getUsuario().getId(),
                detalle.getEstadoPago().toString()
        );
    }

    /**
     * Actualiza el abono parcial de la mensualidad en base al abono recibido.
     * Se asegura que importeInicial este establecido y calcula el nuevo monto abonado y pendiente.
     */
    public Mensualidad actualizarAbonoParcialMensualidad(Mensualidad mensualidad, double abonoRecibido) {
        log.info("[actualizarAbonoParcialMensualidad] INICIO para Mensualidad id={}. Abono recibido: {}", mensualidad.getId(), abonoRecibido);

        // Verificar si el importeInicial es valido, de lo contrario, usar valorBase
        if (mensualidad.getImporteInicial() == null || mensualidad.getImporteInicial() <= 0.0) {
            double importeInicialCalculado = mensualidad.getValorBase();
            mensualidad.setImporteInicial(importeInicialCalculado);
            log.info("[actualizarAbonoParcialMensualidad] Se establece importeInicial={} para Mensualidad id={}", importeInicialCalculado, mensualidad.getId());
        } else {
            log.info("[actualizarAbonoParcialMensualidad] Mensualidad id={} ya tiene importeInicial establecido: {}", mensualidad.getId(), mensualidad.getImporteInicial());
        }

        // Calcular el nuevo monto abonado acumulado
        double montoAbonadoActual = mensualidad.getMontoAbonado() != null ? mensualidad.getMontoAbonado() : 0.0;
        log.info("[actualizarAbonoParcialMensualidad] Mensualidad id={} - Monto abonado actual: {}", mensualidad.getId(), montoAbonadoActual);

        double nuevoMontoAbonado = montoAbonadoActual + abonoRecibido;
        mensualidad.setMontoAbonado(nuevoMontoAbonado);
        log.info("[actualizarAbonoParcialMensualidad] Mensualidad id={} - Nuevo monto abonado acumulado: {}", mensualidad.getId(), nuevoMontoAbonado);

        // Calcular el nuevo importe pendiente
        double nuevoImportePendiente = mensualidad.getImporteInicial() - nuevoMontoAbonado;
        log.info("[actualizarAbonoParcialMensualidad] Mensualidad id={} - Calculado nuevo importe pendiente: {}", mensualidad.getId(), nuevoImportePendiente);

        // Actualizar estado y, si procede, fecha de pago
        if (nuevoImportePendiente <= 0.0) {
            mensualidad.setEstado(EstadoMensualidad.PAGADO);
            mensualidad.setImportePendiente(0.0);
            //CORREGIR ACÁ FECHA PAGO
            mensualidad.setFechaPago(LocalDate.now());
            log.info("[actualizarAbonoParcialMensualidad] Mensualidad id={} marcada como PAGADO. Importe pendiente ajustado a 0.0 y fechaPago asignada a {}", mensualidad.getId(), LocalDate.now());
        } else {
            mensualidad.setEstado(EstadoMensualidad.PENDIENTE);
            mensualidad.setImportePendiente(nuevoImportePendiente);
            log.info("[actualizarAbonoParcialMensualidad] Mensualidad id={} actualizada a PENDIENTE. Importe pendiente: {}", mensualidad.getId(), nuevoImportePendiente);
        }
        log.info("[actualizarAbonoParcialMensualidad] FIN actualizacion para Mensualidad id={}", mensualidad.getId());
        return mensualidad;
    }

    /**
     * Busca la mensualidad existente para el alumno y concepto, o la genera si no existe,
     * y se asegura de que quede en estado PENDIENTE.
     */
    @Transactional
    public Mensualidad obtenerOMarcarPendienteMensualidad(Long alumnoId, String descripcionConcepto) {
        log.info("[obtenerOMarcarPendienteMensualidad] INICIO para alumnoId={}, concepto='{}'", alumnoId, descripcionConcepto);

        // Buscamos mensualidades originales (no clones) que coincidan con la descripcion completa
        List<Mensualidad> mensualidades = mensualidadRepositorio
                .findAllByInscripcionAlumnoIdAndDescripcionAndEsClonFalse(alumnoId, descripcionConcepto);
        log.info("[obtenerOMarcarPendienteMensualidad] Mensualidades encontradas (originales): {}", mensualidades.size());

        if (!mensualidades.isEmpty()) {
            Mensualidad mensualidad = mensualidades.get(0);
            if (mensualidades.size() > 1) {
                log.warn("[obtenerOMarcarPendienteMensualidad] Se encontraron {} mensualidades para alumnoId={} y concepto='{}'. Se usara la primera.",
                        mensualidades.size(), alumnoId, descripcionConcepto);
            }
            log.info("[obtenerOMarcarPendienteMensualidad] Mensualidad encontrada: id={}, estado={}", mensualidad.getId(), mensualidad.getEstado());

            // Actualizamos el estado a PENDIENTE si no lo esta
            if (!EstadoMensualidad.PENDIENTE.equals(mensualidad.getEstado())) {
                log.info("[obtenerOMarcarPendienteMensualidad] Actualizando estado a PENDIENTE para Mensualidad id={}", mensualidad.getId());
                mensualidad.setEstado(EstadoMensualidad.PENDIENTE);
                mensualidad = mensualidadRepositorio.save(mensualidad);
                log.info("[obtenerOMarcarPendienteMensualidad] Mensualidad id={} actualizada a PENDIENTE", mensualidad.getId());
            } else {
                log.info("[obtenerOMarcarPendienteMensualidad] Mensualidad id={} ya se encuentra en estado PENDIENTE", mensualidad.getId());
            }
            return mensualidad;
        } else {
            log.info("[obtenerOMarcarPendienteMensualidad] No se encontro mensualidad original con concepto='{}'. Se procedera a buscar la inscripcion activa asociada a la disciplina.", descripcionConcepto);

            // Extraemos la disciplina a partir de la descripcion (por ejemplo: "DANZA - CUOTA - ABRIL DE 2025")
            String nombreDisciplina = extraerDisciplina(descripcionConcepto);
            if (nombreDisciplina == null) {
                log.warn("[obtenerOMarcarPendienteMensualidad] No se pudo extraer la disciplina de la descripcion '{}'", descripcionConcepto);
                throw new IllegalArgumentException("La descripcion del concepto no contiene informacion de disciplina valida: " + descripcionConcepto);
            }
            log.info("[obtenerOMarcarPendienteMensualidad] Disciplina extraida: '{}'", nombreDisciplina);

            // Buscamos la disciplina en el repositorio usando la parte extraida
            Disciplina disciplina = disciplinaRepositorio.findByNombreContainingIgnoreCase(nombreDisciplina);
            if (disciplina == null) {
                log.warn("[obtenerOMarcarPendienteMensualidad] No se encontro una disciplina que coincida con '{}'", nombreDisciplina);
                throw new IllegalArgumentException("No se encontro una disciplina que coincida con la descripcion: " + nombreDisciplina);
            }
            log.info("[obtenerOMarcarPendienteMensualidad] Disciplina encontrada: id={}, nombre={}", disciplina.getId(), disciplina.getNombre());

            // Buscamos la inscripcion activa para el alumno en la disciplina encontrada
            Optional<Inscripcion> optionalInscripcion = inscripcionRepositorio
                    .findByAlumnoIdAndDisciplinaIdAndEstado(alumnoId, disciplina.getId(), EstadoInscripcion.ACTIVA);
            if (optionalInscripcion.isPresent()) {
                Inscripcion inscripcion = optionalInscripcion.get();
                log.info("[obtenerOMarcarPendienteMensualidad] Inscripcion activa encontrada: id={}, disciplina={}",
                        inscripcion.getId(), inscripcion.getDisciplina().getNombre());

                MesAnio mesAnio = extraerMesYAnio(descripcionConcepto);
                if (mesAnio == null) {
                    log.warn("[obtenerOMarcarPendienteMensualidad] No se pudo extraer mes y año de la descripcion '{}'. Se usara la fecha actual.", descripcionConcepto);
                    LocalDate now = LocalDate.now();
                    mesAnio = new MesAnio(now.getMonthValue(), now.getYear());
                }
                log.info("[obtenerOMarcarPendienteMensualidad] Generando cuota para mes={}, año={}", mesAnio.mes(), mesAnio.anio());
                return generarCuota(alumnoId, inscripcion.getId(), mesAnio.mes(), mesAnio.anio());
            } else {
                log.warn("[obtenerOMarcarPendienteMensualidad] No se encontro inscripcion activa para alumnoId={} en la disciplina id={}",
                        alumnoId, disciplina.getId());
                throw new IllegalArgumentException("No se encontro inscripcion activa para el alumno con id: " + alumnoId +
                        " en la disciplina: " + disciplina.getNombre());
            }
        }
    }

    /**
     * Extrae la parte de la descripcion correspondiente al nombre de la disciplina.
     * Se asume que la descripcion tiene el formato "DISCIPLINA - CUOTA - PERIODO".
     *
     * @param descripcionConcepto la descripcion completa del concepto
     * @return el nombre de la disciplina, o null si no se puede extraer
     */
    private String extraerDisciplina(String descripcionConcepto) {
        if (descripcionConcepto != null && !descripcionConcepto.isEmpty()) {
            String[] partes = descripcionConcepto.split("-");
            if (partes.length > 0) {
                return partes[0].trim();
            }
        }
        return null;
    }

    /**
     * Metodo auxiliar para extraer el mes y el año desde la descripcion.
     * Se asume que la descripcion contiene una parte con formato: "[...]- MES DE AAAA" (por ejemplo, "FEBRERO DE 2025").
     */
    private MesAnio extraerMesYAnio(String descripcion) {
        if (descripcion == null || descripcion.trim().isEmpty()) {
            return null;
        }
        // Se asume que la parte relevante es la ultima: "MES DE AAAA"
        String[] partes = descripcion.trim().toUpperCase().split("-");
        String posibleFecha = partes[partes.length - 1].trim(); // Ej: "FEBRERO DE 2025"
        String[] tokens = posibleFecha.split(" DE ");
        if (tokens.length != 2) {
            return null;
        }
        String mesStr = tokens[0].trim();
        String anioStr = tokens[1].trim();
        Integer mes = convertirNombreMesANumero(mesStr);
        try {
            int anio = Integer.parseInt(anioStr);
            if (mes != null) {
                return new MesAnio(mes, anio);
            }
        } catch (NumberFormatException e) {
            log.error("Error al parsear el año de la descripcion: {}", anioStr, e);
        }
        return null;
    }

    /**
     * Metodo auxiliar para convertir el nombre del mes en su numero (ej. "FEBRERO" -> 2).
     */
    private Integer convertirNombreMesANumero(String mesStr) {
        Map<String, Integer> meses = Map.ofEntries(
                Map.entry("ENERO", 1),
                Map.entry("FEBRERO", 2),
                Map.entry("MARZO", 3),
                Map.entry("ABRIL", 4),
                Map.entry("MAYO", 5),
                Map.entry("JUNIO", 6),
                Map.entry("JULIO", 7),
                Map.entry("AGOSTO", 8),
                Map.entry("SEPTIEMBRE", 9),
                Map.entry("OCTUBRE", 10),
                Map.entry("NOVIEMBRE", 11),
                Map.entry("DICIEMBRE", 12)
        );
        return meses.get(mesStr);
    }

    /**
     * Clase auxiliar para transportar mes y año.
     */
    private record MesAnio(int mes, int anio) {
    }

    public Mensualidad toEntity(MensualidadResponse mensualidadResp) {
        if (mensualidadResp == null) {
            return null;
        }
        Mensualidad entidad = new Mensualidad();
        entidad.setId(mensualidadResp.id());
        entidad.setDescripcion(mensualidadResp.descripcion());
        entidad.setEstado(EstadoMensualidad.valueOf(mensualidadResp.estado()));

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

    /**
     * Procesa el abono para una mensualidad a partir de un DetallePago.
     * Actualiza el monto abonado, recalcula el importe pendiente y actualiza el estado.
     */
    public void procesarAbonoMensualidad(Mensualidad mensualidad, DetallePago detalle) {
        log.info("[procesarAbonoMensualidad] INICIO para Mensualidad id={} con DetallePago id={}", mensualidad.getId(), detalle.getId());

        double abonoRecibido = detalle.getACobrar();
        log.info("[procesarAbonoMensualidad] Abono recibido del detalle: {}", abonoRecibido);

        // Si el detalle no tiene recargo, aseguramos que tampoco se use recargo en la mensualidad
        if (!detalle.getTieneRecargo()) {
            mensualidad.setRecargo(null);
            detalle.setTieneRecargo(false);
            log.info("[procesarAbonoMensualidad] Se ha forzado recargo null para Mensualidad id={} y Detalle id={}", mensualidad.getId(), detalle.getId());
        }

        // Actualizar el abono parcial en la mensualidad
        mensualidad = actualizarAbonoParcialMensualidad(mensualidad, abonoRecibido);
        log.info("[procesarAbonoMensualidad] Mensualidad id={} actualizada internamente con nuevo abono", mensualidad.getId());

        // Persistir la actualizacion
        Mensualidad mensualidadPersistida = mensualidadRepositorio.save(mensualidad);
        log.info("[procesarAbonoMensualidad] FIN. Mensualidad id={} persistida con: ImporteInicial={}, MontoAbonado={}, ImportePendiente={}, Estado={}",
                mensualidadPersistida.getId(), mensualidadPersistida.getImporteInicial(),
                mensualidadPersistida.getMontoAbonado(), mensualidadPersistida.getImportePendiente(),
                mensualidadPersistida.getEstado());
    }
}

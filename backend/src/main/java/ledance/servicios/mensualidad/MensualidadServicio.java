package ledance.servicios.mensualidad;

import jakarta.persistence.criteria.Expression;
import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.Predicate;
import ledance.dto.mensualidad.MensualidadMapper;
import ledance.dto.mensualidad.request.MensualidadRegistroRequest;
import ledance.dto.mensualidad.response.MensualidadResponse;
import ledance.dto.reporte.ReporteMensualidadDTO;
import ledance.entidades.*;
import ledance.repositorios.*;
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

    public MensualidadServicio(DetallePagoRepositorio detallePagoRepositorio, MensualidadRepositorio mensualidadRepositorio,
                               InscripcionRepositorio inscripcionRepositorio,
                               MensualidadMapper mensualidadMapper,
                               RecargoRepositorio recargoRepositorio,
                               BonificacionRepositorio bonificacionRepositorio) {
        this.detallePagoRepositorio = detallePagoRepositorio;
        this.mensualidadRepositorio = mensualidadRepositorio;
        this.inscripcionRepositorio = inscripcionRepositorio;
        this.mensualidadMapper = mensualidadMapper;
        this.recargoRepositorio = recargoRepositorio;
        this.bonificacionRepositorio = bonificacionRepositorio;
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

        // Calcular el total a pagar (fijo) según la formula:
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
     * Esta operacion se realiza una única vez (al crear o configurar la mensualidad).
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
        double recargoFijo = recargo.getValorFijo();
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
        importePendiente = redondear(Math.max(importePendiente, 0.0));
        mensualidad.setImporteInicial(importePendiente); // aquí se usa importeInicial para reflejar el saldo restante
        if (importePendiente == 0.0) {
            mensualidad.setEstado(EstadoMensualidad.PAGADO);
            mensualidad.setFechaPago(LocalDate.now());
        } else {
            mensualidad.setEstado(EstadoMensualidad.PENDIENTE);
        }
        log.info("Importe pendiente recalculado: {}. Estado: {}", importePendiente, mensualidad.getEstado());
    }

    /**
     * Método para redondear números a 2 decimales.
     */
    private double redondear(double valor) {
        return BigDecimal.valueOf(valor).setScale(2, RoundingMode.HALF_UP).doubleValue();
    }

    public List<MensualidadResponse> generarMensualidadesParaMesVigente() {
        int mes = LocalDate.now().getMonthValue();
        int anio = LocalDate.now().getYear();
        YearMonth ym = YearMonth.of(anio, mes);
        LocalDate inicioMes = ym.atDay(1);
        LocalDate finMes = ym.atEndOfMonth();
        log.info("Generando mensualidades para el periodo: {} - {}", inicioMes, finMes);

        List<Inscripcion> inscripcionesActivas = inscripcionRepositorio.findByEstado(EstadoInscripcion.ACTIVA)
                .stream()
                .filter(ins -> ins.getAlumno() != null && Boolean.TRUE.equals(ins.getAlumno().getActivo()))
                .toList();
        log.info("Total de inscripciones activas encontradas: {}", inscripcionesActivas.size());

        List<MensualidadResponse> respuestas = new ArrayList<>();
        for (Inscripcion inscripcion : inscripcionesActivas) {
            log.info("Procesando inscripcion id: {}", inscripcion.getId());
            Optional<Mensualidad> optMensualidad = mensualidadRepositorio
                    .findByInscripcionIdAndFechaCuotaBetween(inscripcion.getId(), inicioMes, finMes);
            if (optMensualidad.isPresent()) {
                Mensualidad mensualidadExistente = optMensualidad.get();
                log.info("Mensualidad existente encontrada para inscripcion id {}: mensualidad id {}",
                        inscripcion.getId(), mensualidadExistente.getId());
                // Actualizamos la cuotaOCantidad (por ejemplo, si el valor base o bonificacion han cambiado)
                mensualidadExistente.setValorBase(inscripcion.getDisciplina().getValorCuota());
                mensualidadExistente.setBonificacion(inscripcion.getBonificacion());
                Recargo recargo = determinarRecargoAutomatico(inicioMes.getDayOfMonth());
                mensualidadExistente.setRecargo(recargo);
                // El importeInicial fijo se recalcula (pero montoAbonado se conserva)
                calcularImporteInicial(mensualidadExistente);
                recalcularImportePendiente(mensualidadExistente);
                asignarDescripcion(mensualidadExistente);
                mensualidadExistente = mensualidadRepositorio.save(mensualidadExistente);
                log.info("Mensualidad actualizada para inscripcion id {}: mensualidad id {}, importe pendiente = {}",
                        inscripcion.getId(), mensualidadExistente.getId(), mensualidadExistente.getImporteInicial());
                respuestas.add(mensualidadMapper.toDTO(mensualidadExistente));
            } else {
                log.info("No existe mensualidad para inscripcion id {} en el periodo; creando nueva mensualidad.", inscripcion.getId());
                Mensualidad nuevaMensualidad = new Mensualidad();
                nuevaMensualidad.setInscripcion(inscripcion);
                nuevaMensualidad.setFechaCuota(inicioMes);
                nuevaMensualidad.setFechaGeneracion(inicioMes);
                nuevaMensualidad.setValorBase(inscripcion.getDisciplina().getValorCuota());
                nuevaMensualidad.setBonificacion(inscripcion.getBonificacion());
                Recargo recargo = determinarRecargoAutomatico(inicioMes.getDayOfMonth());
                nuevaMensualidad.setRecargo(recargo);
                nuevaMensualidad.setMontoAbonado(0.0);
                calcularImporteInicial(nuevaMensualidad);
                recalcularImportePendiente(nuevaMensualidad);
                nuevaMensualidad.setEstado(EstadoMensualidad.PENDIENTE);
                asignarDescripcion(nuevaMensualidad);
                nuevaMensualidad = mensualidadRepositorio.save(nuevaMensualidad);
                log.info("Mensualidad creada para inscripcion id {}: mensualidad id {}, importe pendiente = {}",
                        inscripcion.getId(), nuevaMensualidad.getId(), nuevaMensualidad.getImporteInicial());
                respuestas.add(mensualidadMapper.toDTO(nuevaMensualidad));
            }
        }
        return respuestas;
    }

    private Recargo determinarRecargoAutomatico(int diaCuota) {
        log.info("Determinando recargo automático para el día de cuotaOCantidad: {}", diaCuota);
        Recargo recargo = recargoRepositorio.findAll().stream()
                .filter(r -> diaCuota > r.getDiaDelMesAplicacion())
                .max(Comparator.comparing(Recargo::getDiaDelMesAplicacion))
                .orElse(null);
        if (recargo != null) {
            log.info("Recargo determinado automáticamente: id={}, diaAplicacion={}", recargo.getId(), recargo.getDiaDelMesAplicacion());
        } else {
            log.info("No se determino recargo para el día de cuotaOCantidad: {}", diaCuota);
        }
        return recargo;
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

    public Mensualidad generarCuota(Long inscripcionId, int mes, int anio) {
        log.info("[generarCuota] Generando cuota para inscripción id: {} para {}/{}", inscripcionId, mes, anio);
        Inscripcion inscripcion = inscripcionRepositorio.findById(inscripcionId)
                .orElseThrow(() -> new IllegalArgumentException("Inscripción no encontrada"));

        // Calcular el rango del mes: primer y último día
        LocalDate inicio = LocalDate.of(anio, mes, 1);
        LocalDate fin = inicio.with(TemporalAdjusters.lastDayOfMonth());

        // Verificar si ya existe una mensualidad para este período
        Optional<Mensualidad> optMensualidad = mensualidadRepositorio.findByInscripcionIdAndFechaCuotaBetween(inscripcionId, inicio, fin);
        if (optMensualidad.isPresent()) {
            log.info("[generarCuota] Mensualidad ya existe para inscripción id: {} para {}/{}", inscripcionId, mes, anio);
            Mensualidad mensualidadExistente = optMensualidad.get();
            // Si la cuota no está en estado PENDIENTE, se actualiza
            if (!mensualidadExistente.getEstado().equals(EstadoMensualidad.PENDIENTE)) {
                mensualidadExistente.setEstado(EstadoMensualidad.PENDIENTE);
                mensualidadExistente = mensualidadRepositorio.save(mensualidadExistente);
                log.info("[generarCuota] Mensualidad actualizada a PENDIENTE, id: {}", mensualidadExistente.getId());
            }
            return mensualidadExistente;
        }

        // Si no existe, se crea una nueva mensualidad
        Mensualidad mensualidad = new Mensualidad();
        mensualidad.setInscripcion(inscripcion);
        mensualidad.setFechaCuota(inicio);  // Usamos el inicio del mes como fecha de cuota
        mensualidad.setFechaGeneracion(LocalDate.now());
        mensualidad.setValorBase(inscripcion.getDisciplina().getValorCuota());
        mensualidad.setBonificacion(inscripcion.getBonificacion());
        mensualidad.setRecargo(null);
        mensualidad.setMontoAbonado(0.0);

        double importeInicial = calcularImporteInicial(mensualidad);
        mensualidad.setImporteInicial(importeInicial);
        mensualidad.setImportePendiente(importeInicial);
        mensualidad.setEstado(EstadoMensualidad.PENDIENTE);

        // Asignar descripción (se recomienda incluir mes y año, por ejemplo: "DANZA - CUOTA - MARZO 2025")
        asignarDescripcion(mensualidad);
        log.info("[generarCuota] Descripción asignada a la mensualidad: {}", mensualidad.getDescripcion());

        mensualidad = mensualidadRepositorio.save(mensualidad);
        log.info("[generarCuota] Cuota generada: id={} para inscripción id {} con importeInicial={}",
                mensualidad.getId(), inscripcionId, mensualidad.getImporteInicial());

        // Registrar el detalle de pago para la mensualidad (verificando duplicidad)
        registrarDetallePagoMensualidad(mensualidad);
        log.info("[generarCuota] DetallePago para Mensualidad id={} registrado.", mensualidad.getId());

        return mensualidad;
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

        // Guardar el DetallePago
        DetallePago savedDetalle = detallePagoRepositorio.save(detalle);
        log.info("[registrarDetallePagoMensualidad] DetallePago para Mensualidad id={} creado con importeInicial={} y importePendiente={}",
                mensualidad.getId(), importeInicial, importePendiente);
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
        return new ReporteMensualidadDTO(
                mensualidad.getId(),
                null,
                "CUOTA",
                mensualidad.getValorBase(),
                null,
                mensualidad.getImporteInicial(),
                0.0,
                mensualidad.getEstado() == EstadoMensualidad.PAGADO ? "Abonado" : "Pendiente",
                null,
                mensualidad.getDescripcion()
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

    public List<MensualidadResponse> listarMensualidadesPendientesPorAlumno(Long alumnoId) {
        log.info("Inicio listarMensualidadesPendientesPorAlumno para alumnoId: {}", alumnoId);
        List<EstadoMensualidad> estadosPendientes = List.of(EstadoMensualidad.PENDIENTE, EstadoMensualidad.OMITIDO);

        // 1. Obtener las mensualidades de la base de datos
        List<Mensualidad> mensualidades = mensualidadRepositorio
                .findByInscripcionAlumnoIdAndEstadoInOrderByFechaCuotaDesc(alumnoId, estadosPendientes);
        log.info("Mensualidades encontradas en BD: {}", mensualidades.size());
        mensualidades.forEach(m -> log.info("Mensualidad BD id: {} - importe_inicial: {}", m.getId(), m.getImporteInicial()));

        // 2. Mapear a DTO usando MapStruct
        List<MensualidadResponse> respuestas = mensualidades.stream()
                .map(mensualidadMapper::toDTO)
                .collect(Collectors.toList());
        log.info("Mensualidades pendientes DTO generadas: {}", respuestas.size());
        respuestas.forEach(r -> log.info("DTO Mensualidad id: {} - importeInicial: {}", r.id(), r.importeInicial()));

        log.info("Fin listarMensualidadesPendientesPorAlumno");
        return respuestas;
    }

    public void marcarComoPagada(Long id, LocalDate fecha) {
        log.info("Marcando como PAGADO la mensualidad id: {} con fecha de pago: {}", id, fecha);
        Mensualidad mensualidad = mensualidadRepositorio.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Mensualidad no encontrada"));
        mensualidad.setEstado(EstadoMensualidad.PAGADO);
        mensualidad.setFechaPago(fecha);
        mensualidadRepositorio.save(mensualidad);
        log.info("Mensualidad id {} marcada como PAGADO", id);
    }

    public MensualidadResponse buscarMensualidadPendientePorDescripcion(Inscripcion inscripcion, String descripcion) {
        log.info("Buscando mensualidad pendiente para inscripcion id {} y descripcion '{}'", inscripcion.getId(), descripcion);
        try {
            Mensualidad mens = mensualidadRepositorio
                    .findByInscripcionAndDescripcionAndEstado(inscripcion, descripcion, EstadoMensualidad.PENDIENTE);
            log.info("Mensualidad pendiente encontrada: id={}, importe pendiente = {}", mens.getId(), mens.getImporteInicial());
            return mensualidadMapper.toDTO(mens);
        } catch (Exception e) {
            log.info("No se encontro mensualidad pendiente para inscripcion id {} y descripcion '{}'", inscripcion.getId(), descripcion);
            return null;
        }
    }

    /**
     * Método para crear una mensualidad que se considera pagada de forma inmediata (por ejemplo, cuando se abona el total)
     */
    public void crearMensualidadPagada(Long inscripcionId, String periodo, LocalDate fecha) {
        log.info("Creando mensualidad PAGADA para inscripcion id {} para el periodo '{}'", inscripcionId, periodo);
        Inscripcion inscripcion = inscripcionRepositorio.findById(inscripcionId)
                .orElseThrow(() -> new IllegalArgumentException("Inscripcion no encontrada"));
        Mensualidad nuevaMensualidad = new Mensualidad();
        nuevaMensualidad.setInscripcion(inscripcion);
        nuevaMensualidad.setFechaGeneracion(fecha);
        nuevaMensualidad.setFechaCuota(fecha);
        nuevaMensualidad.setValorBase(inscripcion.getDisciplina().getValorCuota());
        nuevaMensualidad.setBonificacion(inscripcion.getBonificacion());
        Recargo recargo = determinarRecargoAutomatico(fecha.getDayOfMonth());
        nuevaMensualidad.setRecargo(recargo);
        // Para marcarla como pagada, se acumula el abono completo
        nuevaMensualidad.setMontoAbonado(inscripcion.getDisciplina().getValorCuota());
        calcularImporteInicial(nuevaMensualidad);
        recalcularImportePendiente(nuevaMensualidad);
        nuevaMensualidad.setEstado(EstadoMensualidad.PAGADO);
        nuevaMensualidad.setFechaPago(fecha);
        asignarDescripcion(nuevaMensualidad);
        nuevaMensualidad = mensualidadRepositorio.save(nuevaMensualidad);
        log.info("Mensualidad creada y marcada como PAGADA: id={} para inscripcion id {}", nuevaMensualidad.getId(), inscripcionId);
    }

    public List<ReporteMensualidadDTO> buscarMensualidades(LocalDate fechaInicio, LocalDate fechaFin, String disciplinaNombre, String profesorNombre) {
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
        return mensualidades.stream()
                .map(this::mapearReporte)
                .collect(Collectors.toList());
    }

    public Mensualidad actualizarAbonoParcialMensualidad(Mensualidad mensualidad, double abonoRecibido) {
        log.info("[actualizarAbonoParcialMensualidad] Iniciando actualización para Mensualidad id={}. Abono recibido: {}",
                mensualidad.getId(), abonoRecibido);

        // Asegurarse que el importeInicial esté establecido y no se modifique en la actualización
        if (mensualidad.getImporteInicial() == null || mensualidad.getImporteInicial() <= 0.0) {
            // Por ejemplo, se utiliza el valorBase como importeInicial, o se llama a un método de cálculo
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

    public boolean existeMensualidadConDescripcion(Long alumnoId, String descripcionConcepto) {
        // Se asume que el repositorio de mensualidades permite consultar mensualidades pendientes o activas
        // que tengan una descripción (o período) determinado para un alumno.
        // Por ejemplo:
        Optional<Mensualidad> mensualidadOpt = mensualidadRepositorio.findByInscripcionAlumnoIdAndDescripcion(alumnoId, descripcionConcepto);
        return mensualidadOpt.isPresent();
    }

    /**
     * Busca una mensualidad para el alumno (a través de su inscripción) con la descripción indicada.
     * Si existe y no se encuentra en estado PENDIENTE, actualiza su estado.
     * Si no existe, crea una nueva mensualidad en estado PENDIENTE.
     *
     * @param alumnoId            El id del alumno (usado en la propiedad anidada inscripcion.alumno.id)
     * @param descripcionConcepto La descripción del concepto de la mensualidad (p.ej., "Danza - CUOTA - MARZO DE 2025")
     * @return Mensualidad con los datos de la mensualidad en estado pendiente
     */
    public Mensualidad obtenerOMarcarPendienteMensualidad(Long alumnoId, String descripcionConcepto) {
        log.info("[obtenerOMarcarPendienteMensualidad] Iniciando búsqueda de mensualidad para alumnoId={}, concepto={}", alumnoId, descripcionConcepto);

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
            log.info("[obtenerOMarcarPendienteMensualidad] No se encontró mensualidad con ese concepto. Buscando inscripción activa para alumnoId={}", alumnoId);
            Optional<Inscripcion> optionalInscripcion = inscripcionRepositorio.findByAlumnoIdAndEstado(alumnoId, EstadoInscripcion.ACTIVA);

            if (optionalInscripcion.isPresent()) {
                Inscripcion inscripcion = optionalInscripcion.get();
                log.info("[obtenerOMarcarPendienteMensualidad] Inscripción activa encontrada: id={}", inscripcion.getId());
                LocalDate now = LocalDate.now();
                int mes = now.getMonthValue();
                int anio = now.getYear();
                log.info("[obtenerOMarcarPendienteMensualidad] Generando cuota para mes={}, año={}", mes, anio);
                return generarCuota(inscripcion.getId(), mes, anio);
            } else {
                log.warn("[obtenerOMarcarPendienteMensualidad] No se encontró inscripción activa para alumnoId={}", alumnoId);
                throw new IllegalArgumentException("No se encontró inscripción activa para el alumno con id: " + alumnoId);
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

        // Si existe una relación con Inscripcion, se debe mapear también
        // Suponiendo que mensualidadResp tiene un objeto InscripcionResponse
        // if (mensualidadResp.getInscripcion() != null) {
        //     entidad.setInscripcion(inscripcionMapper.toEntity(mensualidadResp.getInscripcion()));
        // }

        return entidad;
    }

    public void generarCuotaAutomatica(Inscripcion inscripcion) {
        int mesActual = LocalDate.now().getMonthValue();
        int anioActual = LocalDate.now().getYear();
        log.info("[MensualidadAutoService] Generando cuota automática para inscripción id: {} en {}/{}",
                inscripcion.getId(), mesActual, anioActual);
        // Delegar la generación de la cuota a la lógica ya existente
        generarCuota(inscripcion.getId(), mesActual, anioActual);
    }

    public Mensualidad procesarAbonoMensualidad(Mensualidad mensualidad, DetallePago detalle) {
        log.info("[procesarAbonoMensualidad] Iniciando procesamiento de abono para Mensualidad id={} con DetallePago id={}",
                mensualidad.getId(), detalle.getId());
        double abonoRecibido = detalle.getaCobrar();
        log.info("[procesarAbonoMensualidad] Abono recibido: {}", abonoRecibido);

        // Actualizar el abono parcial en la mensualidad
        mensualidad = actualizarAbonoParcialMensualidad(mensualidad, abonoRecibido);

        // Persistir la actualización
        Mensualidad nuevaMensualidad = mensualidadRepositorio.save(mensualidad);
        log.info("[procesarAbonoMensualidad] Mensualidad id={} actualizada: ImporteInicial={}, MontoAbonado={}, ImportePendiente={}, Estado={}",
                nuevaMensualidad.getId(), nuevaMensualidad.getImporteInicial(), nuevaMensualidad.getMontoAbonado(), nuevaMensualidad.getImportePendiente(), nuevaMensualidad.getEstado());

        return nuevaMensualidad;
    }

}

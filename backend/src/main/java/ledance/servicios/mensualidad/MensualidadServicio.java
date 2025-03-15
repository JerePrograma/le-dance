package ledance.servicios.mensualidad;

import jakarta.persistence.criteria.Expression;
import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.Predicate;
import ledance.dto.mensualidad.MensualidadMapper;
import ledance.dto.mensualidad.request.MensualidadModificacionRequest;
import ledance.dto.mensualidad.request.MensualidadRegistroRequest;
import ledance.dto.mensualidad.response.MensualidadResponse;
import ledance.dto.reporte.ReporteMensualidadDTO;
import ledance.entidades.*;
import ledance.repositorios.BonificacionRepositorio;
import ledance.repositorios.InscripcionRepositorio;
import ledance.repositorios.MensualidadRepositorio;
import ledance.repositorios.RecargoRepositorio;
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
import java.util.Comparator;
import java.util.Locale;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@Transactional
public class MensualidadServicio implements IMensualidadService {

    private static final Logger log = LoggerFactory.getLogger(MensualidadServicio.class);

    private final MensualidadRepositorio mensualidadRepositorio;
    private final InscripcionRepositorio inscripcionRepositorio;
    private final MensualidadMapper mensualidadMapper;
    private final RecargoRepositorio recargoRepositorio;
    private final BonificacionRepositorio bonificacionRepositorio;

    public MensualidadServicio(MensualidadRepositorio mensualidadRepositorio,
                               InscripcionRepositorio inscripcionRepositorio,
                               MensualidadMapper mensualidadMapper,
                               RecargoRepositorio recargoRepositorio,
                               BonificacionRepositorio bonificacionRepositorio) {
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
        log.debug("Inscripcion encontrada: {}", inscripcion);

        Mensualidad mensualidad = mensualidadMapper.toEntity(request);
        log.debug("Entidad mensualidad mapeada: {}", mensualidad);

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
        // totalPagar = (valorBase + recargos) - (descuentos)
        calcularTotalPagar(mensualidad);
        // Inicializar montoAbonado en 0
        mensualidad.setMontoAbonado(0.0);
        // Con base en totalPagar y montoAbonado se calcula el importe pendiente y se actualiza el estado.
        recalcularImportePendiente(mensualidad);
        asignarDescripcion(mensualidad);

        mensualidad = mensualidadRepositorio.save(mensualidad);
        log.info("Mensualidad creada con id: {} y total a pagar (fijo): {}", mensualidad.getId(), mensualidad.getTotalPagar());
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
        log.debug("Descripcion asignada: {}", descripcion);
    }

    /**
     * Calcula el totalPagar (fijo) a partir del valor base, recargos y descuentos.
     * Esta operacion se realiza una única vez (al crear o configurar la mensualidad).
     */
    private void calcularTotalPagar(Mensualidad mensualidad) {
        double valorBase = mensualidad.getValorBase();

        // Descuentos
        double descuentoFijo = 0.0;
        double descuentoPorcentaje = 0.0;
        if (mensualidad.getBonificacion() != null) {
            descuentoFijo = mensualidad.getBonificacion().getValorFijo();
            descuentoPorcentaje = (mensualidad.getBonificacion().getPorcentajeDescuento() / 100.0) * valorBase;
        }
        double totalDescuento = descuentoFijo + descuentoPorcentaje;

        // Recargos
        double recargoFijo = 0.0;
        double recargoPorcentaje = 0.0;
        if (mensualidad.getRecargo() != null) {
            recargoFijo = mensualidad.getRecargo().getValorFijo();
            recargoPorcentaje = (mensualidad.getRecargo().getPorcentaje() / 100.0) * valorBase;
        }
        double totalRecargo = recargoFijo + recargoPorcentaje;

        double totalPagar = (valorBase + totalRecargo) - totalDescuento;
        // Redondeamos el totalPagar para evitar discrepancias decimales
        totalPagar = redondear(totalPagar);
        mensualidad.setTotalPagar(totalPagar);
        log.info("Total a pagar (fijo) calculado: {}", totalPagar);
    }

    /**
     * Recalcula el importe pendiente (saldo) en base al totalPagar fijo y al montoAbonado acumulado.
     * Actualiza el estado: si importePendiente es 0 (redondeado), se marca como PAGADO, en caso contrario PENDIENTE.
     */
    public void recalcularImportePendiente(Mensualidad mensualidad) {
        double totalPagar = mensualidad.getTotalPagar();
        double montoAbonado = mensualidad.getMontoAbonado();
        double importePendiente = totalPagar - montoAbonado;
        importePendiente = redondear(Math.max(importePendiente, 0.0));
        mensualidad.setTotalPagar(importePendiente); // aquí se usa totalPagar para reflejar el saldo restante
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
                // Actualizamos la cuota (por ejemplo, si el valor base o bonificacion han cambiado)
                mensualidadExistente.setValorBase(inscripcion.getDisciplina().getValorCuota());
                mensualidadExistente.setBonificacion(inscripcion.getBonificacion());
                Recargo recargo = determinarRecargoAutomatico(inicioMes.getDayOfMonth());
                mensualidadExistente.setRecargo(recargo);
                // El totalPagar fijo se recalcula (pero montoAbonado se conserva)
                calcularTotalPagar(mensualidadExistente);
                recalcularImportePendiente(mensualidadExistente);
                asignarDescripcion(mensualidadExistente);
                mensualidadExistente = mensualidadRepositorio.save(mensualidadExistente);
                log.info("Mensualidad actualizada para inscripcion id {}: mensualidad id {}, importe pendiente = {}",
                        inscripcion.getId(), mensualidadExistente.getId(), mensualidadExistente.getTotalPagar());
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
                calcularTotalPagar(nuevaMensualidad);
                recalcularImportePendiente(nuevaMensualidad);
                nuevaMensualidad.setEstado(EstadoMensualidad.PENDIENTE);
                asignarDescripcion(nuevaMensualidad);
                nuevaMensualidad = mensualidadRepositorio.save(nuevaMensualidad);
                log.info("Mensualidad creada para inscripcion id {}: mensualidad id {}, importe pendiente = {}",
                        inscripcion.getId(), nuevaMensualidad.getId(), nuevaMensualidad.getTotalPagar());
                respuestas.add(mensualidadMapper.toDTO(nuevaMensualidad));
            }
        }
        return respuestas;
    }

    private Recargo determinarRecargoAutomatico(int diaCuota) {
        log.info("Determinando recargo automático para el día de cuota: {}", diaCuota);
        Recargo recargo = recargoRepositorio.findAll().stream()
                .filter(r -> diaCuota > r.getDiaDelMesAplicacion())
                .max(Comparator.comparing(Recargo::getDiaDelMesAplicacion))
                .orElse(null);
        if (recargo != null) {
            log.info("Recargo determinado automáticamente: id={}, diaAplicacion={}", recargo.getId(), recargo.getDiaDelMesAplicacion());
        } else {
            log.info("No se determino recargo para el día de cuota: {}", diaCuota);
        }
        return recargo;
    }

    @Override
    public MensualidadResponse actualizarMensualidad(Long id, MensualidadModificacionRequest request) {
        log.info("Iniciando actualizacion de mensualidad id: {}", id);
        Mensualidad mensualidad = mensualidadRepositorio.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Mensualidad no encontrada"));
        log.debug("Mensualidad actual antes de actualizar: {}", mensualidad);

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
        calcularTotalPagar(mensualidad);
        recalcularImportePendiente(mensualidad);
        asignarDescripcion(mensualidad);
        mensualidad = mensualidadRepositorio.save(mensualidad);
        log.info("Mensualidad actualizada: id={}, importe pendiente = {}", mensualidad.getId(), mensualidad.getTotalPagar());
        return mensualidadMapper.toDTO(mensualidad);
    }

    public MensualidadResponse generarCuota(Long inscripcionId, int mes, int anio) {
        log.info("Generando cuota para inscripcion id: {} para {}/{}", inscripcionId, mes, anio);
        Inscripcion inscripcion = inscripcionRepositorio.findById(inscripcionId)
                .orElseThrow(() -> new IllegalArgumentException("Inscripcion no encontrada"));
        LocalDate fechaMensualidad = inscripcion.getFechaInscripcion();
        if (fechaMensualidad.getMonthValue() != mes || fechaMensualidad.getYear() != anio) {
            log.warn("La fecha de inscripcion ({}) no coincide con el mes/anio especificados ({}-{}). Se usará la fecha de inscripcion.",
                    fechaMensualidad, mes, anio);
        }
        Mensualidad mensualidad = new Mensualidad();
        mensualidad.setInscripcion(inscripcion);
        mensualidad.setFechaCuota(fechaMensualidad);
        mensualidad.setFechaGeneracion(fechaMensualidad);
        mensualidad.setValorBase(inscripcion.getDisciplina().getValorCuota());
        mensualidad.setBonificacion(inscripcion.getBonificacion());
        mensualidad.setRecargo(null);
        mensualidad.setMontoAbonado(0.0);
        calcularTotalPagar(mensualidad);
        recalcularImportePendiente(mensualidad);
        mensualidad.setEstado(EstadoMensualidad.PENDIENTE);
        asignarDescripcion(mensualidad);
        mensualidad = mensualidadRepositorio.save(mensualidad);
        log.info("Cuota generada: id={} para inscripcion id {} con importe pendiente = {}",
                mensualidad.getId(), inscripcionId, mensualidad.getTotalPagar());
        return mensualidadMapper.toDTO(mensualidad);
    }

    @Override
    public MensualidadResponse obtenerMensualidad(Long id) {
        log.info("Obteniendo mensualidad con id: {}", id);
        Mensualidad mensualidad = mensualidadRepositorio.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Mensualidad no encontrada"));
        log.debug("Mensualidad obtenida: {}", mensualidad);
        return mensualidadMapper.toDTO(mensualidad);
    }

    @Override
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
                mensualidad.getTotalPagar(),
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
        List<EstadoMensualidad> estadosPendientes = List.of(EstadoMensualidad.PENDIENTE, EstadoMensualidad.OMITIDO);
        log.info("Listando mensualidades pendientes para alumno id: {}", alumnoId);
        List<MensualidadResponse> respuestas = mensualidadRepositorio
                .findByInscripcionAlumnoIdAndEstadoInOrderByFechaCuotaDesc(alumnoId, estadosPendientes)
                .stream()
                .map(mensualidadMapper::toDTO)
                .collect(Collectors.toList());
        log.info("Mensualidades pendientes encontradas: {}", respuestas.size());
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
            log.info("Mensualidad pendiente encontrada: id={}, importe pendiente = {}", mens.getId(), mens.getTotalPagar());
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
        calcularTotalPagar(nuevaMensualidad);
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

    public void actualizarAbonoParcial(Long id, double abonoRecibido) {
        log.info("Actualizando abono parcial para mensualidad id {}: abono recibido = {}", id, abonoRecibido);
        Mensualidad mensualidad = mensualidadRepositorio.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Mensualidad no encontrada con id: " + id));
        // Se acumula el abono recibido en el montoAbonado
        mensualidad.setMontoAbonado(redondear(mensualidad.getMontoAbonado() + abonoRecibido));
        // Recalcular el importe pendiente y actualizar el estado
        recalcularImportePendiente(mensualidad);
        mensualidadRepositorio.save(mensualidad);
        log.info("Mensualidad id {} actualizada. Monto abonado acumulado: {}, importe pendiente: {}, estado: {}",
                id, mensualidad.getMontoAbonado(), mensualidad.getTotalPagar(), mensualidad.getEstado());
    }
}

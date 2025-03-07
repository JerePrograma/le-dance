package ledance.servicios.mensualidad;

import jakarta.persistence.criteria.Expression;
import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.Predicate;
import ledance.dto.alumno.response.AlumnoListadoResponse;
import ledance.dto.bonificacion.response.BonificacionResponse;
import ledance.dto.disciplina.response.DisciplinaListadoResponse;
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

import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.TextStyle;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
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
        log.info("Creando mensualidad para inscripción id: {}", request.inscripcionId());

        Inscripcion inscripcion = inscripcionRepositorio.findById(request.inscripcionId())
                .orElseThrow(() -> new IllegalArgumentException("Inscripción no encontrada"));

        Recargo recargo = determinarRecargo(request);

        Bonificacion bonificacion = (request.bonificacionId() != null) ?
                bonificacionRepositorio.findById(request.bonificacionId())
                        .orElseThrow(() -> new IllegalArgumentException("Bonificación no encontrada"))
                : null;

        Mensualidad mensualidad = mensualidadMapper.toEntity(request);
        mensualidad.setInscripcion(inscripcion);
        mensualidad.setRecargo(recargo);
        mensualidad.setBonificacion(bonificacion);
        // Calcular el total mediante la lógica del servicio
        calcularTotal(mensualidad);
        // Asignar la descripción antes de guardar
        asignarDescripcion(mensualidad);

        log.info("Total a pagar calculado: {}", mensualidad.getTotalPagar());
        mensualidad = mensualidadRepositorio.save(mensualidad);
        log.info("Mensualidad creada con id: {}", mensualidad.getId());
        return mensualidadMapper.toDTO(mensualidad);
    }

    /**
     * Método auxiliar para asignar la descripción.
     * La descripción se forma concatenando:
     * "Disciplina(nombre) + ' Cuota ' + Mes(de fechaGeneracion)"
     */
    private void asignarDescripcion(Mensualidad mensualidad) {
        String nombreDisciplina = mensualidad.getInscripcion().getDisciplina().getNombre();
        String mes = mensualidad.getFechaGeneracion()
                .getMonth()
                .getDisplayName(TextStyle.FULL, new Locale("es", "ES"));
        mensualidad.setDescripcion(nombreDisciplina + " Cuota " + mes);
    }

    /**
     * Método privado para calcular el total a pagar.
     * Suma el valor base y el recargo (si existe) y le resta la bonificación (si existe).
     */
    private void calcularTotal(Mensualidad mensualidad) {
        double descuento = 0.0;
        double recargoValor = 0.0;
        if (mensualidad.getBonificacion() != null) {
            double descuentoFijo = mensualidad.getBonificacion().getValorFijo() != null ? mensualidad.getBonificacion().getValorFijo() : 0.0;
            double descuentoPorcentaje = mensualidad.getBonificacion().getPorcentajeDescuento() != null ?
                    (mensualidad.getBonificacion().getPorcentajeDescuento() / 100.0 * mensualidad.getValorBase()) : 0.0;
            descuento = descuentoFijo + descuentoPorcentaje;
        }
        if (mensualidad.getRecargo() != null) {
            double recargoFijo = mensualidad.getRecargo().getValorFijo() != null ? mensualidad.getRecargo().getValorFijo() : 0.0;
            double recargoPorcentaje = mensualidad.getRecargo().getPorcentaje() != null ?
                    (mensualidad.getRecargo().getPorcentaje() / 100.0 * mensualidad.getValorBase()) : 0.0;
            recargoValor = recargoFijo + recargoPorcentaje;
        }
        mensualidad.setTotalPagar((mensualidad.getValorBase() + recargoValor) - descuento);
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

        List<MensualidadResponse> respuestas = new ArrayList<>();

        for (Inscripcion inscripcion : inscripcionesActivas) {
            Optional<Mensualidad> optMensualidad = mensualidadRepositorio
                    .findByInscripcionIdAndFechaCuotaBetween(inscripcion.getId(), inicioMes, finMes);
            if (optMensualidad.isPresent()) {
                Mensualidad mensualidadExistente = optMensualidad.get();
                Double valorCuota = inscripcion.getDisciplina().getValorCuota();
                mensualidadExistente.setValorBase(valorCuota);
                mensualidadExistente.setBonificacion(inscripcion.getBonificacion());
                Recargo recargo = determinarRecargoAutomatico(inicioMes.getDayOfMonth());
                mensualidadExistente.setRecargo(recargo);
                calcularTotal(mensualidadExistente);
                asignarDescripcion(mensualidadExistente);
                mensualidadExistente = mensualidadRepositorio.save(mensualidadExistente);
                log.info("Mensualidad actualizada para inscripción id {}: mensualidad id {}",
                        inscripcion.getId(), mensualidadExistente.getId());
                respuestas.add(mensualidadMapper.toDTO(mensualidadExistente));
            } else {
                Mensualidad nuevaMensualidad = new Mensualidad();
                nuevaMensualidad.setInscripcion(inscripcion);
                nuevaMensualidad.setFechaCuota(inicioMes);
                nuevaMensualidad.setValorBase(inscripcion.getDisciplina().getValorCuota());
                nuevaMensualidad.setBonificacion(inscripcion.getBonificacion());
                Recargo recargo = determinarRecargoAutomatico(inicioMes.getDayOfMonth());
                nuevaMensualidad.setRecargo(recargo);
                nuevaMensualidad.setEstado(EstadoMensualidad.PENDIENTE);
                calcularTotal(nuevaMensualidad);
                asignarDescripcion(nuevaMensualidad);
                nuevaMensualidad = mensualidadRepositorio.save(nuevaMensualidad);
                log.info("Mensualidad creada para inscripción id {}: mensualidad id {}",
                        inscripcion.getId(), nuevaMensualidad.getId());
                respuestas.add(mensualidadMapper.toDTO(nuevaMensualidad));
            }
        }
        return respuestas;
    }

    private Recargo determinarRecargoAutomatico(int diaCuota) {
        return recargoRepositorio.findAll().stream()
                .filter(r -> diaCuota > r.getDiaDelMesAplicacion())
                .max(Comparator.comparing(Recargo::getDiaDelMesAplicacion))
                .orElse(null);
    }

    private Recargo determinarRecargo(MensualidadRegistroRequest request) {
        if (request.recargoId() != null) {
            Recargo recargo = recargoRepositorio.findById(request.recargoId())
                    .orElseThrow(() -> new IllegalArgumentException("Recargo no encontrado"));
            log.info("Recargo asignado por id: {}", recargo.getId());
            return recargo;
        } else {
            int cuotaDay = request.fechaCuota().getDayOfMonth();
            Recargo recargo = recargoRepositorio.findAll().stream()
                    .filter(r -> cuotaDay > r.getDiaDelMesAplicacion())
                    .max(Comparator.comparing(Recargo::getDiaDelMesAplicacion))
                    .orElse(null);
            if (recargo != null) {
                log.info("Recargo determinado automáticamente: id={}, diaAplicacion={}", recargo.getId(), recargo.getDiaDelMesAplicacion());
            } else {
                log.info("No se determinó recargo automáticamente para el día: {}", cuotaDay);
            }
            return recargo;
        }
    }

    @Override
    public MensualidadResponse actualizarMensualidad(Long id, MensualidadModificacionRequest request) {
        log.info("Actualizando mensualidad id: {}", id);
        Mensualidad mensualidad = mensualidadRepositorio.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Mensualidad no encontrada"));

        mensualidad.setFechaCuota(request.fechaCuota());
        mensualidad.setValorBase(request.valorBase());
        mensualidad.setEstado(Enum.valueOf(EstadoMensualidad.class, request.estado()));

        if (request.recargoId() != null) {
            Recargo recargo = recargoRepositorio.findById(request.recargoId())
                    .orElseThrow(() -> new IllegalArgumentException("Recargo no encontrado"));
            mensualidad.setRecargo(recargo);
        } else {
            mensualidad.setRecargo(null);
        }

        if (request.bonificacionId() != null) {
            Bonificacion bonificacion = bonificacionRepositorio.findById(request.bonificacionId())
                    .orElseThrow(() -> new IllegalArgumentException("Bonificación no encontrada"));
            mensualidad.setBonificacion(bonificacion);
        } else {
            mensualidad.setBonificacion(null);
        }

        calcularTotal(mensualidad);
        asignarDescripcion(mensualidad);

        mensualidad = mensualidadRepositorio.save(mensualidad);
        log.info("Mensualidad actualizada: id={}, totalPagar={}", mensualidad.getId(), mensualidad.getTotalPagar());
        return mensualidadMapper.toDTO(mensualidad);
    }

    public MensualidadResponse generarCuota(Long inscripcionId, int mes, int anio) {
        log.info("Generando cuota para inscripción id: {} para {}/{}", inscripcionId, mes, anio);

        Inscripcion inscripcion = inscripcionRepositorio.findById(inscripcionId)
                .orElseThrow(() -> new IllegalArgumentException("Inscripción no encontrada"));

        LocalDate fechaMensualidad = inscripcion.getFechaInscripcion();
        if (fechaMensualidad.getMonthValue() != mes || fechaMensualidad.getYear() != anio) {
            log.warn("La fecha de inscripción ({}) no coincide con el mes/anio especificados ({}-{}). Se usará la fecha de inscripción.",
                    fechaMensualidad, mes, anio);
        }

        Mensualidad mensualidad = new Mensualidad();
        mensualidad.setInscripcion(inscripcion);
        mensualidad.setFechaCuota(fechaMensualidad);
        mensualidad.setFechaGeneracion(fechaMensualidad);
        mensualidad.setValorBase(inscripcion.getDisciplina().getValorCuota());
        mensualidad.setBonificacion(inscripcion.getBonificacion());
        mensualidad.setRecargo(null);
        mensualidad.setEstado(EstadoMensualidad.PENDIENTE);

        calcularTotal(mensualidad);
        asignarDescripcion(mensualidad);

        mensualidad = mensualidadRepositorio.save(mensualidad);
        log.info("Cuota generada con id: {} y total a pagar: {}", mensualidad.getId(), mensualidad.getTotalPagar());

        return mensualidadMapper.toDTO(mensualidad);
    }

    @Override
    public MensualidadResponse obtenerMensualidad(Long id) {
        Mensualidad mensualidad = mensualidadRepositorio.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Mensualidad no encontrada"));
        return mensualidadMapper.toDTO(mensualidad);
    }

    @Override
    public List<MensualidadResponse> listarMensualidades() {
        List<MensualidadResponse> respuestas = mensualidadRepositorio.findAll()
                .stream()
                .map(mensualidadMapper::toDTO)
                .collect(Collectors.toList());
        log.info("Total de mensualidades listadas: {}", respuestas.size());
        return respuestas;
    }

    @Override
    public List<MensualidadResponse> listarPorInscripcion(Long inscripcionId) {
        List<MensualidadResponse> respuestas = mensualidadRepositorio.findByInscripcionId(inscripcionId)
                .stream()
                .map(mensualidadMapper::toDTO)
                .collect(Collectors.toList());
        log.info("Mensualidades encontradas para inscripción id {}: {}", inscripcionId, respuestas.size());
        return respuestas;
    }

    @Override
    public void eliminarMensualidad(Long id) {
        if (!mensualidadRepositorio.existsById(id)) {
            throw new IllegalArgumentException("Mensualidad no encontrada");
        }
        mensualidadRepositorio.deleteById(id);
        log.info("Mensualidad eliminada: id={}", id);
    }

    public List<ReporteMensualidadDTO> buscarMensualidades(
            LocalDate fechaInicio,
            LocalDate fechaFin,
            String disciplinaNombre,
            String profesorNombre
    ) {
        log.info("Buscando mensualidades con fecha entre {} y {}, disciplinaNombre={}, profesorNombre={}",
                fechaInicio, fechaFin, disciplinaNombre, profesorNombre);

        Specification<Mensualidad> spec = Specification.where((root, query, cb) ->
                cb.between(root.get("fechaCuota"), fechaInicio, fechaFin)
        );

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

    public ReporteMensualidadDTO mapearReporte(Mensualidad mensualidad) {
        AlumnoListadoResponse alumno = new AlumnoListadoResponse(
                mensualidad.getInscripcion().getAlumno().getId(),
                mensualidad.getInscripcion().getAlumno().getNombre(),
                mensualidad.getInscripcion().getAlumno().getApellido(),
                mensualidad.getInscripcion().getAlumno().getActivo()
        );

        String cuota = determinarTipoCuota(mensualidad);
        Double importe = mensualidad.getValorBase();

        BonificacionResponse bonificacionResponse = null;
        if (mensualidad.getBonificacion() != null) {
            double valorFijo = mensualidad.getBonificacion().getValorFijo() != null ? mensualidad.getBonificacion().getValorFijo() : 0.0;
            double porcentaje = mensualidad.getBonificacion().getPorcentajeDescuento() != null ?
                    mensualidad.getBonificacion().getPorcentajeDescuento() / 100.0 * mensualidad.getValorBase() : 0.0;
            double computedBonificacion = valorFijo + porcentaje;

            bonificacionResponse = new BonificacionResponse(
                    mensualidad.getBonificacion().getId(),
                    mensualidad.getBonificacion().getDescripcion(),
                    mensualidad.getBonificacion().getPorcentajeDescuento(),
                    mensualidad.getBonificacion().getActivo(),
                    mensualidad.getBonificacion().getObservaciones(),
                    computedBonificacion
            );
        }

        double recargo = 0.0;
        if (mensualidad.getRecargo() != null) {
            double recargoFijo = mensualidad.getRecargo().getValorFijo() != null ? mensualidad.getRecargo().getValorFijo() : 0.0;
            double recargoPorcentaje = mensualidad.getRecargo().getPorcentaje() != null ?
                    mensualidad.getRecargo().getPorcentaje() / 100.0 * mensualidad.getValorBase() : 0.0;
            recargo = recargoFijo + recargoPorcentaje;
        }

        Double total = importe - (bonificacionResponse != null ? bonificacionResponse.valorFijo() : 0.0) + recargo;
        String estado = mensualidad.getEstado() == EstadoMensualidad.PAGADO ? "Abonado" : "Pendiente";

        DisciplinaListadoResponse disciplinaResponse = new DisciplinaListadoResponse(
                mensualidad.getInscripcion().getDisciplina().getId(),
                mensualidad.getInscripcion().getDisciplina().getNombre(),
                mensualidad.getInscripcion().getDisciplina().getActivo(),
                mensualidad.getInscripcion().getDisciplina().getProfesor().getId(),
                mensualidad.getInscripcion().getDisciplina().getProfesor().getNombre(),
                mensualidad.getInscripcion().getDisciplina().getClaseSuelta(),
                mensualidad.getInscripcion().getDisciplina().getClasePrueba(),
                mensualidad.getInscripcion().getDisciplina().getValorCuota()
        );

        return new ReporteMensualidadDTO(
                mensualidad.getId(),
                alumno,
                cuota,
                importe,
                bonificacionResponse,
                total,
                recargo,
                estado,
                disciplinaResponse,
                mensualidad.getDescripcion()  // Incluir la descripción calculada
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

    public List<ReporteMensualidadDTO> buscarMensualidadesAlumnoPorMes(LocalDate fechaMes, String alumnoNombre) {
        log.info("Buscando mensualidades para alumno '{}' en el mes de {}", alumnoNombre, fechaMes);

        LocalDate primerDia = fechaMes.withDayOfMonth(1);
        LocalDate ultimoDia = fechaMes.withDayOfMonth(fechaMes.lengthOfMonth());

        Specification<Mensualidad> spec = Specification.where((root, query, cb) ->
                cb.between(root.get("fechaGeneracion"), primerDia, ultimoDia)
        );

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
}

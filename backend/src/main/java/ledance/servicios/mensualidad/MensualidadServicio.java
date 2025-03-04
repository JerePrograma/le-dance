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
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
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
        mensualidad.calcularTotal();
        log.info("Total a pagar calculado: {}", mensualidad.getTotalPagar());

        mensualidad = mensualidadRepositorio.save(mensualidad);
        log.info("Mensualidad creada con id: {}", mensualidad.getId());
        return mensualidadMapper.toDTO(mensualidad);
    }

    /**
     * Método de generación automática o actualización de mensualidades para el mes vigente.
     * Para cada inscripción activa de un alumno activo se genera (o actualiza) la mensualidad.
     */
    public List<MensualidadResponse> generarMensualidadesParaMesVigente() {
        int mes = LocalDate.now().getMonthValue();
        int anio = LocalDate.now().getYear();
        YearMonth ym = YearMonth.of(anio, mes);
        LocalDate inicioMes = ym.atDay(1);
        LocalDate finMes = ym.atEndOfMonth();

        log.info("Generando mensualidades para el periodo: {} - {}", inicioMes, finMes);

        // Obtener todas las inscripciones activas cuyo alumno esté activo
        List<Inscripcion> inscripcionesActivas = inscripcionRepositorio.findByEstado(EstadoInscripcion.ACTIVA)
                .stream()
                .filter(ins -> ins.getAlumno() != null && Boolean.TRUE.equals(ins.getAlumno().getActivo()))
                .collect(Collectors.toList());

        List<MensualidadResponse> respuestas = new ArrayList<>();

        for (Inscripcion inscripcion : inscripcionesActivas) {
            Optional<Mensualidad> optMensualidad = mensualidadRepositorio
                    .findByInscripcionIdAndFechaCuotaBetween(inscripcion.getId(), inicioMes, finMes);
            if (optMensualidad.isPresent()) {
                // Actualizar la mensualidad existente
                Mensualidad mensualidadExistente = optMensualidad.get();
                // Actualiza el valor base según la disciplina
                Double valorCuota = inscripcion.getDisciplina().getValorCuota();
                mensualidadExistente.setValorBase(valorCuota);
                // Se actualiza la bonificación asignada en la inscripción
                mensualidadExistente.setBonificacion(inscripcion.getBonificacion());
                // Se determina y actualiza el recargo automáticamente (según la lógica actual)
                Recargo recargo = determinarRecargoAutomatico(inicioMes.getDayOfMonth());
                mensualidadExistente.setRecargo(recargo);
                // Se recalcula el total a pagar
                mensualidadExistente.calcularTotal();
                mensualidadExistente = mensualidadRepositorio.save(mensualidadExistente);
                log.info("Mensualidad actualizada para inscripción id {}: mensualidad id {}", inscripcion.getId(), mensualidadExistente.getId());
                respuestas.add(mensualidadMapper.toDTO(mensualidadExistente));
            } else {
                // Crear una nueva mensualidad para la inscripción
                Mensualidad nuevaMensualidad = new Mensualidad();
                nuevaMensualidad.setInscripcion(inscripcion);
                // Se asigna la fecha de cuota como el primer día del mes vigente
                nuevaMensualidad.setFechaCuota(inicioMes);
                // Valor base tomado de la disciplina
                nuevaMensualidad.setValorBase(inscripcion.getDisciplina().getValorCuota());
                // Se asigna la bonificación actual de la inscripción
                nuevaMensualidad.setBonificacion(inscripcion.getBonificacion());
                // Se determina el recargo automáticamente (si corresponde)
                Recargo recargo = determinarRecargoAutomatico(inicioMes.getDayOfMonth());
                nuevaMensualidad.setRecargo(recargo);
                // Estado inicial de la cuota: PENDIENTE (suponiendo que ese valor existe en el enum)
                nuevaMensualidad.setEstado(EstadoMensualidad.PENDIENTE);
                // Calcular el total a pagar
                nuevaMensualidad.calcularTotal();
                nuevaMensualidad = mensualidadRepositorio.save(nuevaMensualidad);
                log.info("Mensualidad creada para inscripción id {}: mensualidad id {}", inscripcion.getId(), nuevaMensualidad.getId());
                respuestas.add(mensualidadMapper.toDTO(nuevaMensualidad));
            }
        }

        return respuestas;
    }

    /**
     * Método auxiliar para determinar el recargo de forma automática.
     * Se utiliza la lógica: si el día de la cuota es mayor a "diaDelMesAplicacion" de algún recargo, se toma el de mayor día.
     */
    private Recargo determinarRecargoAutomatico(int diaCuota) {
        return recargoRepositorio.findAll().stream()
                .filter(r -> diaCuota > r.getDiaDelMesAplicacion())
                .max(Comparator.comparing(Recargo::getDiaDelMesAplicacion))
                .orElse(null);
    }

    // ... (otros métodos existentes, por ejemplo actualizarMensualidad, listarMensualidades, etc.)

    // El método determinarRecargo que recibe una MensualidadRegistroRequest se mantiene para los casos manuales.
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

    public MensualidadResponse generarCuota(Long inscripcionId, int mes, int anio) {
        log.info("Generando cuota para inscripción id: {} para {}/{}", inscripcionId, mes, anio);

        // 1. Obtener la inscripción
        Inscripcion inscripcion = inscripcionRepositorio.findById(inscripcionId)
                .orElseThrow(() -> new IllegalArgumentException("Inscripción no encontrada"));

        // 2. Validar que no exista ya una cuota para el mes y año indicados
        YearMonth yearMonth = YearMonth.of(anio, mes);
        LocalDate inicioMes = yearMonth.atDay(1);
        LocalDate finMes = yearMonth.atEndOfMonth();
        Optional<Mensualidad> cuotaExistente = mensualidadRepositorio.findByInscripcionIdAndFechaCuotaBetween(inscripcionId, inicioMes, finMes);
        if (cuotaExistente.isPresent()) {
            throw new IllegalStateException("La cuota para este mes ya fue generada para esta inscripción.");
        }

        // 3. Crear la nueva Mensualidad
        Mensualidad mensualidad = new Mensualidad();
        mensualidad.setInscripcion(inscripcion);
        // Se asigna la fecha de cuota como el primer día del mes (puedes modificar este comportamiento)
        mensualidad.setFechaCuota(inicioMes);
        // El valor base es el valorCuota de la disciplina
        Double valorCuota = inscripcion.getDisciplina().getValorCuota();
        mensualidad.setValorBase(valorCuota);
        // Se copia la bonificación actual de la inscripción (valor original)
        mensualidad.setBonificacion(inscripcion.getBonificacion());
        // No se aplica recargo en la generación a demanda
        mensualidad.setRecargo(null);
        // Estado inicial de la cuota: PENDIENTE (se asume que el enum tiene, por ejemplo, PENDIENTE y PAGADO)
        mensualidad.setEstado(EstadoMensualidad.PENDIENTE);
        // Calcular el total a pagar
        mensualidad.calcularTotal();

        // 4. Persistir la cuota
        mensualidad = mensualidadRepositorio.save(mensualidad);
        log.info("Cuota generada con id: {} y total a pagar: {}", mensualidad.getId(), mensualidad.getTotalPagar());

        return mensualidadMapper.toDTO(mensualidad);
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

        mensualidad.calcularTotal();
        mensualidad = mensualidadRepositorio.save(mensualidad);
        log.info("Mensualidad actualizada: id={}, totalPagar={}", mensualidad.getId(), mensualidad.getTotalPagar());
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

    /**
     * Busca mensualidades según filtros y devuelve un Page de ReporteMensualidadDTO.
     * Filtros:
     * - fechaInicio y fechaFin: rango de fechas aplicable a Mensualidad.fechaCuota.
     * - disciplinaId: opcional, filtra por el ID de la Disciplina (a través de la Inscripción).
     * - profesorId: opcional, filtra por el ID del Profesor (desde Disciplina en la Inscripción).
     */
    // Suponiendo que modificamos el método para recibir también "disciplinaNombre" y "profesorNombre"
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
                // Join de Mensualidad -> Inscripcion -> Disciplina
                Join<Mensualidad, Inscripcion> inscripcion = root.join("inscripcion");
                Join<Inscripcion, Disciplina> disciplina = inscripcion.join("disciplina");
                return cb.like(cb.lower(disciplina.get("nombre")), "%" + disciplinaNombre.toLowerCase() + "%");
            });
        }

        if (profesorNombre != null && !profesorNombre.isEmpty()) {
            spec = spec.and((root, query, cb) -> {
                // Join de Mensualidad -> Inscripcion -> Disciplina -> Profesor
                Join<Mensualidad, Inscripcion> inscripcion = root.join("inscripcion");
                Join<Inscripcion, Disciplina> disciplina = inscripcion.join("disciplina");
                Join<Disciplina, Profesor> profesor = disciplina.join("profesor");
                return cb.like(cb.lower(profesor.get("nombre")), "%" + profesorNombre.toLowerCase() + "%");
            });
        }

        List<Mensualidad> mensualidades = mensualidadRepositorio.findAll(spec);
        log.info("Total de mensualidades encontradas: {}", mensualidades.size());

        // Utilizamos el método mapearReporte para transformar cada entidad en un ReporteMensualidadDTO.
        return mensualidades.stream()
                .map(this::mapearReporte)
                .collect(Collectors.toList());
    }

    public ReporteMensualidadDTO mapearReporte(Mensualidad mensualidad) {
        // Mapeo del alumno
        AlumnoListadoResponse alumno = new AlumnoListadoResponse(
                mensualidad.getInscripcion().getAlumno().getId(),
                mensualidad.getInscripcion().getAlumno().getNombre(),
                mensualidad.getInscripcion().getAlumno().getApellido(),
                mensualidad.getInscripcion().getAlumno().getActivo()
        );

        // Determinar el tipo de cuota (método auxiliar que ya tienes definido)
        String cuota = determinarTipoCuota(mensualidad);

        // Valor base de la mensualidad
        Double importe = mensualidad.getValorBase();

        // Calcular y mapear la bonificación
        BonificacionResponse bonificacionResponse = null;
        if (mensualidad.getBonificacion() != null) {
            double valorFijo = mensualidad.getBonificacion().getValorFijo() != null
                    ? mensualidad.getBonificacion().getValorFijo() : 0.0;
            double porcentaje = mensualidad.getBonificacion().getPorcentajeDescuento() != null
                    ? mensualidad.getBonificacion().getPorcentajeDescuento() / 100.0 * mensualidad.getValorBase()
                    : 0.0;
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

        // Calcular recargo (se asume que mensualidad.getRecargo() retorna un objeto similar a Bonificacion)
        double recargo = 0.0;
        if (mensualidad.getRecargo() != null) {
            double recargoFijo = mensualidad.getRecargo().getValorFijo() != null
                    ? mensualidad.getRecargo().getValorFijo() : 0.0;
            double recargoPorcentaje = mensualidad.getRecargo().getPorcentaje() != null
                    ? mensualidad.getRecargo().getPorcentaje() / 100.0 * mensualidad.getValorBase()
                    : 0.0;
            recargo = recargoFijo + recargoPorcentaje;
        }

        // Calcular el total: importe - bonificación + recargo
        Double total = importe - (bonificacionResponse != null ? bonificacionResponse.valorFijo() : 0.0) + recargo;

        // Determinar el estado
        String estado = mensualidad.getEstado() == EstadoMensualidad.PAGADO ? "Abonado" : "Pendiente";

        // Mapear la disciplina
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

        // Construir y retornar el DTO
        return new ReporteMensualidadDTO(
                mensualidad.getId(),
                alumno,
                cuota,
                importe,
                bonificacionResponse,
                total,
                recargo,
                estado,
                disciplinaResponse
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
        // En caso de que no coincida con ninguno, se puede devolver un valor por defecto:
        return "CUOTA";
    }

    public List<ReporteMensualidadDTO> buscarMensualidadesAlumnoPorMes(LocalDate fechaMes, String alumnoNombre) {
        log.info("Buscando mensualidades para alumno '{}' en el mes de {}", alumnoNombre, fechaMes);

        // Calcular primer y último día del mes indicado.
        LocalDate primerDia = fechaMes.withDayOfMonth(1);
        LocalDate ultimoDia = fechaMes.withDayOfMonth(fechaMes.lengthOfMonth());

        Specification<Mensualidad> spec = Specification.where((root, query, cb) ->
                cb.between(root.get("fechaGeneracion"), primerDia, ultimoDia)
        );

        // Filtrar por alumno (a través del join: Mensualidad -> Inscripcion -> Alumno)
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

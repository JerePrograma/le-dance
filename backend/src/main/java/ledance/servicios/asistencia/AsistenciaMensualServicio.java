package ledance.servicios.asistencia;

import ledance.dto.asistencia.request.AsistenciaMensualModificacionRequest;
import ledance.dto.asistencia.response.AsistenciaMensualDetalleResponse;
import ledance.dto.asistencia.response.AsistenciaMensualListadoResponse;
import ledance.dto.asistencia.AsistenciaMensualMapper;
import ledance.dto.asistencia.response.AsistenciasActivasResponse;
import ledance.entidades.*;
import ledance.repositorios.*;
import ledance.servicios.disciplina.DisciplinaServicio;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

@Service
@Transactional
public class AsistenciaMensualServicio {

    private final AsistenciaMensualRepositorio asistenciaMensualRepositorio;
    private final InscripcionRepositorio inscripcionRepositorio;
    private final DisciplinaRepositorio disciplinaRepositorio;
    private final AsistenciaMensualMapper asistenciaMensualMapper;
    private final DisciplinaServicio disciplinaServicio;
    private final AsistenciaDiariaRepositorio asistenciaDiariaRepositorio;
    private final AsistenciaAlumnoMensualRepositorio asistenciaAlumnoMensualRepositorio;

    public AsistenciaMensualServicio(
            AsistenciaMensualRepositorio asistenciaMensualRepositorio,
            InscripcionRepositorio inscripcionRepositorio,
            DisciplinaRepositorio disciplinaRepositorio,
            AsistenciaMensualMapper asistenciaMensualMapper,
            @Lazy DisciplinaServicio disciplinaServicio,
            AsistenciaDiariaRepositorio asistenciaDiariaRepositorio, AsistenciaAlumnoMensualRepositorio asistenciaAlumnoMensualRepositorio) {
        this.asistenciaMensualRepositorio = asistenciaMensualRepositorio;
        this.inscripcionRepositorio = inscripcionRepositorio;
        this.disciplinaRepositorio = disciplinaRepositorio;
        this.asistenciaMensualMapper = asistenciaMensualMapper;
        this.disciplinaServicio = disciplinaServicio;
        this.asistenciaDiariaRepositorio = asistenciaDiariaRepositorio;
        this.asistenciaAlumnoMensualRepositorio = asistenciaAlumnoMensualRepositorio;
    }

    /**
     * Crea o recupera la planilla de asistencia para una disciplina en un mes y año dados.
     */
    @Transactional
    public AsistenciaMensualDetalleResponse crearPlanilla(Long disciplinaId, int mes, int anio) {
        // Se busca la disciplina
        Disciplina disciplina = disciplinaRepositorio.findById(disciplinaId)
                .orElseThrow(() -> new IllegalArgumentException("Disciplina no encontrada con ID: " + disciplinaId));
        // Buscar planilla existente para (disciplina, mes, anio)
        Optional<AsistenciaMensual> planillaOpt = asistenciaMensualRepositorio.findByDisciplina_IdAndMesAndAnio(disciplinaId, mes, anio);
        AsistenciaMensual planilla;
        if (planillaOpt.isPresent()) {
            planilla = planillaOpt.get();
        } else {
            // Crear nueva planilla asociada a la disciplina
            planilla = new AsistenciaMensual();
            planilla.setMes(mes);
            planilla.setAnio(anio);
            planilla.setDisciplina(disciplina);
            planilla = asistenciaMensualRepositorio.save(planilla);
        }
        return asistenciaMensualMapper.toDetalleDTO(planilla);
    }

    /**
     * Incorpora un alumno (a través de su inscripcion) a la planilla de asistencia de su disciplina.
     */
    @Transactional
    public void agregarAlumnoAPlanilla(Long inscripcionId, int mes, int anio) {
        // Buscar inscripcion
        Inscripcion inscripcion = inscripcionRepositorio.findById(inscripcionId)
                .orElseThrow(() -> new IllegalArgumentException("Inscripcion no encontrada con ID: " + inscripcionId));
        Long disciplinaId = inscripcion.getDisciplina().getId();

        // Buscar o crear la planilla mensual para la disciplina en el mes y año indicados
        AsistenciaMensualDetalleResponse planillaDTO = crearPlanilla(disciplinaId, mes, anio);
        AsistenciaMensual planilla = asistenciaMensualRepositorio.findById(planillaDTO.id())
                .orElseThrow(() -> new IllegalArgumentException("Planilla no encontrada con ID: " + planillaDTO.id()));

        // Verificar si ya existe un registro para esta inscripcion en la planilla
        boolean yaExiste = planilla.getAsistenciasAlumnoMensual().stream()
                .anyMatch(aam -> aam.getInscripcion().getId().equals(inscripcionId));
        if (yaExiste) {
            // Si ya está incorporado, se omite la creacion
            return;
        }

        // Crear el registro mensual del alumno
        AsistenciaAlumnoMensual alumnoMensual = new AsistenciaAlumnoMensual();
        alumnoMensual.setInscripcion(inscripcion);
        alumnoMensual.setAsistenciaMensual(planilla);
        alumnoMensual.setObservacion(null); // Se inicia sin observacion

        // Agregar el registro a la planilla y guardarlo para asignarle un ID
        planilla.getAsistenciasAlumnoMensual().add(alumnoMensual);
        alumnoMensual = asistenciaAlumnoMensualRepositorio.save(alumnoMensual);

        // Generar las asistencias diarias según los días de clase
        List<LocalDate> fechasClase = disciplinaServicio.obtenerDiasClase(disciplinaId, mes, anio);
        AsistenciaAlumnoMensual finalAlumnoMensual = alumnoMensual;
        List<AsistenciaDiaria> nuevasAsistencias = fechasClase.stream()
                .map(fecha -> new AsistenciaDiaria(null, fecha, EstadoAsistencia.AUSENTE, finalAlumnoMensual))
                .toList();
        alumnoMensual.getAsistenciasDiarias().addAll(nuevasAsistencias);

        // Guardar nuevamente el registro para que se persistan las asistencias diarias
        asistenciaAlumnoMensualRepositorio.save(alumnoMensual);
    }

    @Transactional(readOnly = true)
    public AsistenciaMensualDetalleResponse obtenerPlanillaPorDisciplinaYMes(Long disciplinaId, int mes, int anio) {
        Optional<AsistenciaMensual> planillaOpt = asistenciaMensualRepositorio.findByDisciplina_IdAndMesAndAnioFetch(disciplinaId, mes, anio);
        return planillaOpt.map(asistenciaMensualMapper::toDetalleDTO)
                .orElseThrow(() -> new NoSuchElementException("No se encontro planilla para (Disciplina ID: "
                        + disciplinaId + ", mes: " + mes + ", anio: " + anio + ")"));
    }

    @Transactional
    public AsistenciaMensualDetalleResponse actualizarPlanillaAsistencia(Long id, AsistenciaMensualModificacionRequest request) {
        AsistenciaMensual existente = asistenciaMensualRepositorio.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("No existe planilla con ID: " + id));

        // Actualizamos las observaciones de cada registro de alumno
        request.asistenciasAlumnoMensual().forEach(modReq -> {
            // Buscamos en la planilla el registro que corresponde (por su id)
            existente.getAsistenciasAlumnoMensual().forEach(aam -> {
                if (aam.getId().equals(modReq.id())) {
                    aam.setObservacion(modReq.observacion());
                    // Si se requiere actualizar asistencias diarias, se puede iterar por modReq.asistenciasDiarias() aquí
                }
            });
        });

        AsistenciaMensual actualizada = asistenciaMensualRepositorio.save(existente);
        return asistenciaMensualMapper.toDetalleDTO(actualizada);
    }

    @Transactional(readOnly = true)
    public List<AsistenciaMensualListadoResponse> listarPlanillas(Long profesorId, Long disciplinaId, Integer mes, Integer anio) {
        List<AsistenciaMensual> planillas = asistenciaMensualRepositorio.buscarPlanillas(profesorId, disciplinaId, mes, anio);
        return planillas.stream()
                .map(asistenciaMensualMapper::toListadoDTO)
                .collect(Collectors.toList());
    }

    /**
     * Actualiza la planilla en caso de cambios de horario: elimina las asistencias diarias desde una fecha y genera nuevas fechas para cada alumno.
     */
    @Transactional
    public void actualizarPlanillaPorCambioHorario(Long disciplinaId, LocalDate fechaCambio) {
        int mes = fechaCambio.getMonthValue();
        int anio = fechaCambio.getYear();
        Optional<AsistenciaMensual> optionalPlanilla = asistenciaMensualRepositorio.findByDisciplina_IdAndMesAndAnio(disciplinaId, mes, anio);
        if (optionalPlanilla.isPresent()) {
            AsistenciaMensual planilla = optionalPlanilla.get();
            // Para cada registro (por alumno) en la planilla
            planilla.getAsistenciasAlumnoMensual().forEach(registro -> {
                // Se elimina las asistencias diarias cuya fecha sea >= fechaCambio para este alumno
                asistenciaDiariaRepositorio.deleteByAsistenciaAlumnoMensualIdAndFechaGreaterThanEqual(registro.getId(), fechaCambio);
                // Obtener las nuevas fechas de clase a partir de fechaCambio
                List<LocalDate> nuevasFechas = disciplinaServicio.obtenerDiasClase(disciplinaId, mes, anio)
                        .stream().filter(f -> !f.isBefore(fechaCambio)).toList();
                // Crear nuevas asistencias diarias para el registro del alumno
                List<AsistenciaDiaria> nuevasAsistencias = nuevasFechas.stream()
                        .map(fecha -> new AsistenciaDiaria(null, fecha, EstadoAsistencia.AUSENTE, registro))
                        .collect(Collectors.toList());
                asistenciaDiariaRepositorio.saveAll(nuevasAsistencias);
            });
        }
    }

    @Transactional(readOnly = true)
    public AsistenciaMensualDetalleResponse obtenerAsistenciaMensualPorParametros(Long disciplinaId, int mes, int anio) {
        Optional<AsistenciaMensual> planillaOpt = asistenciaMensualRepositorio.findByDisciplina_IdAndMesAndAnio(disciplinaId, mes, anio);
        return planillaOpt.map(asistenciaMensualMapper::toDetalleDTO)
                .orElseThrow(() -> new NoSuchElementException("No se encontro planilla para (Disciplina ID: "
                        + disciplinaId + ", mes: " + mes + ", anio: " + anio + ")"));
    }

    @Transactional
    public AsistenciasActivasResponse crearAsistenciasParaInscripcionesActivasDetallado() {
        int mes = LocalDate.now().getMonthValue();
        int anio = LocalDate.now().getYear();

        List<Inscripcion> inscripcionesActivas = inscripcionRepositorio.findByEstado(EstadoInscripcion.ACTIVA);
        // Agrupar las inscripciones activas por disciplina
        Map<Disciplina, List<Inscripcion>> inscripcionesPorDisciplina =
                inscripcionesActivas.stream().collect(Collectors.groupingBy(Inscripcion::getDisciplina));

        int planillasCreadas = 0;
        int totalAsistenciasGeneradas = 0;
        List<String> detalles = new ArrayList<>();

        for (Map.Entry<Disciplina, List<Inscripcion>> entry : inscripcionesPorDisciplina.entrySet()) {
            Disciplina disciplina = entry.getKey();
            List<Inscripcion> inscripciones = entry.getValue();

            // Buscar o crear la planilla para esta disciplina, mes y año
            Optional<AsistenciaMensual> planillaOpt = asistenciaMensualRepositorio.findByDisciplina_IdAndMesAndAnio(disciplina.getId(), mes, anio);
            AsistenciaMensual planilla;
            if (planillaOpt.isPresent()) {
                planilla = planillaOpt.get();
            } else {
                planilla = new AsistenciaMensual();
                planilla.setMes(mes);
                planilla.setAnio(anio);
                planilla.setDisciplina(disciplina);
                planilla = asistenciaMensualRepositorio.save(planilla);
                planillasCreadas++;
            }

            // Obtener los días de clase para la disciplina
            List<LocalDate> fechasClase = disciplinaServicio.obtenerDiasClase(disciplina.getId(), mes, anio);
            int asistenciasGeneradasParaDisciplina = 0;
            // Para cada inscripcion de esta disciplina
            for (Inscripcion inscripcion : inscripciones) {
                // Verificar si ya existe un registro de asistencia mensual para esta inscripcion
                boolean existe = asistenciaAlumnoMensualRepositorio.existsByInscripcionIdAndAsistenciaMensualId(inscripcion.getId(), planilla.getId());
                if (!existe) {
                    // Crear el registro de asistencia mensual para el alumno
                    AsistenciaAlumnoMensual alumnoMensual = new AsistenciaAlumnoMensual();
                    alumnoMensual.setInscripcion(inscripcion);
                    alumnoMensual.setAsistenciaMensual(planilla);
                    alumnoMensual.setObservacion(null); // O un valor por defecto
                    alumnoMensual = asistenciaAlumnoMensualRepositorio.save(alumnoMensual);

                    // Generar las asistencias diarias para este alumno
                    AsistenciaAlumnoMensual finalAlumnoMensual = alumnoMensual;
                    List<AsistenciaDiaria> nuevasAsistencias = fechasClase.stream()
                            .map(fecha -> new AsistenciaDiaria(null, fecha, EstadoAsistencia.AUSENTE, finalAlumnoMensual))
                            .collect(Collectors.toList());
                    asistenciaDiariaRepositorio.saveAll(nuevasAsistencias);
                    asistenciasGeneradasParaDisciplina += nuevasAsistencias.size();
                }
            }
            detalles.add("Disciplina " + disciplina.getNombre() + " - Inscripciones: " + inscripciones.size()
                    + ", Asistencias generadas: " + asistenciasGeneradasParaDisciplina);
            totalAsistenciasGeneradas += asistenciasGeneradasParaDisciplina;
        }

        return new AsistenciasActivasResponse(
                inscripcionesActivas.size(),
                planillasCreadas,
                totalAsistenciasGeneradas,
                detalles
        );
    }

}

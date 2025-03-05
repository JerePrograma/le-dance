package ledance.servicios.asistencia;

import ledance.dto.asistencia.request.AsistenciaMensualModificacionRequest;
import ledance.dto.asistencia.request.AsistenciaMensualRegistroRequest;
import ledance.dto.asistencia.response.AsistenciaMensualDetalleResponse;
import ledance.dto.asistencia.response.AsistenciaMensualListadoResponse;
import ledance.dto.asistencia.AsistenciaMensualMapper;
import ledance.entidades.*;
import ledance.repositorios.AsistenciaMensualRepositorio;
import ledance.repositorios.DisciplinaRepositorio;
import ledance.repositorios.AsistenciaDiariaRepositorio;
import ledance.repositorios.InscripcionRepositorio;
import ledance.servicios.disciplina.DisciplinaServicio;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDate;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@Transactional
public class AsistenciaMensualServicio {

    private final AsistenciaMensualRepositorio asistenciaMensualRepositorio;
    private final InscripcionRepositorio inscripcionRepositorio;
    private final DisciplinaRepositorio disciplinaRepositorio;
    private final AsistenciaMensualMapper asistenciaMensualMapper;
    private final AsistenciaDiariaServicio asistenciaDiariaServicio;
    private final DisciplinaServicio disciplinaServicio;
    private final AsistenciaDiariaRepositorio asistenciaDiariaRepositorio;

    public AsistenciaMensualServicio(
            AsistenciaMensualRepositorio asistenciaMensualRepositorio,
            InscripcionRepositorio inscripcionRepositorio,
            DisciplinaRepositorio disciplinaRepositorio,
            AsistenciaMensualMapper asistenciaMensualMapper,
            @Lazy AsistenciaDiariaServicio asistenciaDiariaServicio,
            @Lazy DisciplinaServicio disciplinaServicio,
            AsistenciaDiariaRepositorio asistenciaDiariaRepositorio) {
        this.asistenciaMensualRepositorio = asistenciaMensualRepositorio;
        this.inscripcionRepositorio = inscripcionRepositorio;
        this.disciplinaRepositorio = disciplinaRepositorio;
        this.asistenciaMensualMapper = asistenciaMensualMapper;
        this.asistenciaDiariaServicio = asistenciaDiariaServicio;
        this.disciplinaServicio = disciplinaServicio;
        this.asistenciaDiariaRepositorio = asistenciaDiariaRepositorio;
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
     * Incorpora un alumno (a través de su inscripción) a la planilla de asistencia de su disciplina.
     */
    @Transactional
    public AsistenciaMensualDetalleResponse agregarAlumnoAPlanilla(Long inscripcionId, int mes, int anio) {
        Inscripcion inscripcion = inscripcionRepositorio.findById(inscripcionId)
                .orElseThrow(() -> new IllegalArgumentException("Inscripción no encontrada con ID: " + inscripcionId));
        Long disciplinaId = inscripcion.getDisciplina().getId();
        // Crear o recuperar la planilla para la disciplina
        AsistenciaMensualDetalleResponse planillaDTO = crearPlanilla(disciplinaId, mes, anio);
        // Incorporar al alumno generando sus asistencias diarias en la planilla (si aún no existen)
        asistenciaDiariaServicio.registrarAsistenciasParaNuevoAlumno(inscripcionId, planillaDTO.id());
        return planillaDTO;
    }

    @Transactional(readOnly = true)
    public AsistenciaMensualDetalleResponse obtenerPlanillaPorDisciplinaYMes(Long disciplinaId, int mes, int anio) {
        Optional<AsistenciaMensual> planillaOpt = asistenciaMensualRepositorio.findByDisciplina_IdAndMesAndAnio(disciplinaId, mes, anio);
        return planillaOpt.map(asistenciaMensualMapper::toDetalleDTO)
                .orElseThrow(() -> new NoSuchElementException("No se encontró planilla para (Disciplina ID: "
                        + disciplinaId + ", mes: " + mes + ", anio: " + anio + ")"));
    }

    @Transactional
    public AsistenciaMensualDetalleResponse actualizarPlanillaAsistencia(Long id, AsistenciaMensualModificacionRequest request) {
        AsistenciaMensual existente = asistenciaMensualRepositorio.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("No existe planilla con ID: " + id));
        asistenciaMensualMapper.updateEntityFromRequest(request, existente);
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
            // Borrar asistencias diarias cuya fecha sea >= fechaCambio
            asistenciaDiariaRepositorio.deleteByAsistenciaMensualIdAndFechaGreaterThanEqual(planilla.getId(), fechaCambio);
            // Obtener nuevas fechas de clase a partir de fechaCambio
            List<LocalDate> nuevasFechas = disciplinaServicio.obtenerDiasClase(disciplinaId, mes, anio)
                    .stream().filter(f -> !f.isBefore(fechaCambio)).toList();
            // Para cada alumno ya incorporado en la planilla, agregar las nuevas asistencias
            planilla.getAsistenciasDiarias().stream()
                    .map(ad -> ad.getAlumno())
                    .distinct()
                    .forEach(alumno -> {
                        List<AsistenciaDiaria> nuevasAsistencias = nuevasFechas.stream()
                                .map(fecha -> new AsistenciaDiaria(null, fecha, EstadoAsistencia.AUSENTE, alumno, planilla))
                                .collect(Collectors.toList());
                        asistenciaDiariaRepositorio.saveAll(nuevasAsistencias);
                    });
        }
    }

    @Transactional(readOnly = true)
    public AsistenciaMensualDetalleResponse obtenerAsistenciaMensualPorParametros(Long disciplinaId, int mes, int anio) {
        Optional<AsistenciaMensual> planillaOpt = asistenciaMensualRepositorio.findByDisciplina_IdAndMesAndAnio(disciplinaId, mes, anio);
        return planillaOpt.map(asistenciaMensualMapper::toDetalleDTO)
                .orElseThrow(() -> new NoSuchElementException("No se encontró planilla para (Disciplina ID: "
                        + disciplinaId + ", mes: " + mes + ", anio: " + anio + ")"));
    }

    @Transactional
    public void crearAsistenciasParaInscripcionesActivas() {
        int mes = LocalDate.now().getMonthValue();
        int anio = LocalDate.now().getYear();
        // Obtiene todas las inscripciones activas
        List<Inscripcion> inscripcionesActivas = inscripcionRepositorio.findByEstado(EstadoInscripcion.ACTIVA);
        for (Inscripcion inscripcion : inscripcionesActivas) {
            // Se utiliza el método existente para agregar al alumno a la planilla de su disciplina
            agregarAlumnoAPlanilla(inscripcion.getId(), mes, anio);
        }
    }
}

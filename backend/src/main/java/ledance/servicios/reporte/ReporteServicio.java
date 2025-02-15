package ledance.servicios.reporte;

import ledance.dto.reporte.ReporteMapper;
import ledance.dto.reporte.request.ReporteRegistroRequest;
import ledance.dto.reporte.response.ReporteResponse;
import ledance.entidades.Reporte;
import ledance.entidades.Usuario;
import ledance.repositorios.InscripcionRepositorio;
import ledance.repositorios.ReporteRepositorio;
import ledance.repositorios.UsuarioRepositorio;
import jakarta.transaction.Transactional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class ReporteServicio implements IReporteServicio {

    private static final Logger log = LoggerFactory.getLogger(ReporteServicio.class);

    private final ReporteRepositorio reporteRepositorio;
    private final InscripcionRepositorio inscripcionRepositorio;
    private final UsuarioRepositorio usuarioRepositorio;
    private final ReporteMapper reporteMapper;

    public ReporteServicio(ReporteRepositorio reporteRepositorio,
                           InscripcionRepositorio inscripcionRepositorio,
                           UsuarioRepositorio usuarioRepositorio,
                           ReporteMapper reporteMapper) {
        this.reporteRepositorio = reporteRepositorio;
        this.inscripcionRepositorio = inscripcionRepositorio;
        this.usuarioRepositorio = usuarioRepositorio;
        this.reporteMapper = reporteMapper;
    }

    /**
     * Genera un reporte genérico a partir de un request.
     */
    @Override
    @Transactional
    public ReporteResponse generarReporte(ReporteRegistroRequest request) {
        log.info("Generando reporte de tipo: {}", request.tipo());
        Reporte nuevoReporte = new Reporte();
        nuevoReporte.setTipo(request.tipo());
        nuevoReporte.setDescripcion("Reporte generado de forma genérica.");
        nuevoReporte.setFechaGeneracion(LocalDate.now());
        nuevoReporte.setActivo(true);
        if (request.usuarioId() != null) {
            Usuario usuario = usuarioRepositorio.findById(request.usuarioId())
                    .orElseThrow(() -> new IllegalArgumentException("Usuario no encontrado."));
            nuevoReporte.setUsuario(usuario);
        }
        Reporte guardado = reporteRepositorio.save(nuevoReporte);
        return reporteMapper.toDTO(guardado);
    }

    /**
     * Genera un reporte de recaudación por disciplina.
     */
    @Override
    @Transactional
    public ReporteResponse generarReporteRecaudacionPorDisciplina(Long disciplinaId, Long usuarioId) {
        log.info("Generando reporte de recaudación para la disciplina con ID: {}", disciplinaId);
        List<Object[]> resultados = inscripcionRepositorio.obtenerRecaudacionPorDisciplina(disciplinaId);
        String descripcion = resultados.isEmpty()
                ? "No se encontraron datos de recaudación para la disciplina."
                : resultados.stream()
                .map(r -> "Disciplina: " + r[0] + " | Monto Total: $" + r[1])
                .collect(Collectors.joining("\n"));

        Reporte nuevoReporte = new Reporte();
        nuevoReporte.setTipo("Recaudación por Disciplina");
        nuevoReporte.setDescripcion(descripcion);
        nuevoReporte.setFechaGeneracion(LocalDate.now());
        nuevoReporte.setActivo(true);
        if (usuarioId != null) {
            Usuario usuario = usuarioRepositorio.findById(usuarioId)
                    .orElseThrow(() -> new IllegalArgumentException("Usuario no encontrado."));
            nuevoReporte.setUsuario(usuario);
        }
        Reporte guardado = reporteRepositorio.save(nuevoReporte);
        return reporteMapper.toDTO(guardado);
    }

    /**
     * Genera un reporte de asistencias por alumno.
     * (En un caso real, aquí se consultaría un repositorio de asistencias.)
     */
    @Override
    @Transactional
    public ReporteResponse generarReporteAsistenciasPorAlumno(Long alumnoId, Long usuarioId) {
        log.info("Generando reporte de asistencias para el alumno con ID: {}", alumnoId);
        String descripcion = "Reporte de asistencias para el alumno con ID " + alumnoId;
        Reporte nuevoReporte = new Reporte();
        nuevoReporte.setTipo("Asistencias por Alumno");
        nuevoReporte.setDescripcion(descripcion);
        nuevoReporte.setFechaGeneracion(LocalDate.now());
        nuevoReporte.setActivo(true);
        if (usuarioId != null) {
            Usuario usuario = usuarioRepositorio.findById(usuarioId)
                    .orElseThrow(() -> new IllegalArgumentException("Usuario no encontrado."));
            nuevoReporte.setUsuario(usuario);
        }
        Reporte guardado = reporteRepositorio.save(nuevoReporte);
        return reporteMapper.toDTO(guardado);
    }

    /**
     * Genera un reporte de asistencias por disciplina.
     */
    @Override
    @Transactional
    public ReporteResponse generarReporteAsistenciasPorDisciplina(Long disciplinaId, Long usuarioId) {
        log.info("Generando reporte de asistencias para la disciplina con ID: {}", disciplinaId);
        String descripcion = "Reporte de asistencias para la disciplina con ID " + disciplinaId;
        Reporte nuevoReporte = new Reporte();
        nuevoReporte.setTipo("Asistencias por Disciplina");
        nuevoReporte.setDescripcion(descripcion);
        nuevoReporte.setFechaGeneracion(LocalDate.now());
        nuevoReporte.setActivo(true);
        if (usuarioId != null) {
            Usuario usuario = usuarioRepositorio.findById(usuarioId)
                    .orElseThrow(() -> new IllegalArgumentException("Usuario no encontrado."));
            nuevoReporte.setUsuario(usuario);
        }
        Reporte guardado = reporteRepositorio.save(nuevoReporte);
        return reporteMapper.toDTO(guardado);
    }

    /**
     * Genera un reporte de asistencias por disciplina y alumno.
     */
    @Override
    @Transactional
    public ReporteResponse generarReporteAsistenciasPorDisciplinaAlumno(Long disciplinaId, Long alumnoId, Long usuarioId) {
        log.info("Generando reporte de asistencias para el alumno con ID {} en la disciplina con ID {}", alumnoId, disciplinaId);
        String descripcion = "Reporte de asistencias para el alumno con ID " + alumnoId +
                " en la disciplina con ID " + disciplinaId;
        Reporte nuevoReporte = new Reporte();
        nuevoReporte.setTipo("Asistencias por Disciplina y Alumno");
        nuevoReporte.setDescripcion(descripcion);
        nuevoReporte.setFechaGeneracion(LocalDate.now());
        nuevoReporte.setActivo(true);
        if (usuarioId != null) {
            Usuario usuario = usuarioRepositorio.findById(usuarioId)
                    .orElseThrow(() -> new IllegalArgumentException("Usuario no encontrado."));
            nuevoReporte.setUsuario(usuario);
        }
        Reporte guardado = reporteRepositorio.save(nuevoReporte);
        return reporteMapper.toDTO(guardado);
    }

    @Override
    public List<ReporteResponse> listarReportes() {
        return reporteRepositorio.findAll().stream()
                .map(reporteMapper::toDTO)
                .collect(Collectors.toList());
    }

    @Override
    public ReporteResponse obtenerReportePorId(Long id) {
        Reporte reporte = reporteRepositorio.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Reporte no encontrado."));
        return reporteMapper.toDTO(reporte);
    }

    @Override
    @Transactional
    public void eliminarReporte(Long id) {
        Reporte reporte = reporteRepositorio.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Reporte no encontrado."));
        reporte.setActivo(false);
        reporteRepositorio.save(reporte);
    }
}

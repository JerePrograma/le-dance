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
     * ✅ Generar un nuevo reporte segun el tipo solicitado.
     */
    @Override
    @Transactional
    public ReporteResponse generarReporte(ReporteRegistroRequest request) {
        log.info("Generando reporte de tipo: {}", request.tipo());

//        String descripcion = generarDescripcion(request.tipo(), request.disciplinaId(), request.alumnoId());

        Reporte nuevoReporte = new Reporte();
        nuevoReporte.setTipo(request.tipo());
//        nuevoReporte.setDescripcion(descripcion);
        nuevoReporte.setFechaGeneracion(LocalDate.now());
        nuevoReporte.setActivo(true);

        // Asociamos el usuario si se provee en el request
        if (request.usuarioId() != null) {
            Usuario usuario = usuarioRepositorio.findById(request.usuarioId())
                    .orElseThrow(() -> new IllegalArgumentException("Usuario no encontrado."));
            nuevoReporte.setUsuario(usuario);
        }

        // Guardamos el reporte en la BD
        Reporte guardado = reporteRepositorio.save(nuevoReporte);
        return reporteMapper.toDTO(guardado);
    }

    /**
     * ✅ Generar la descripcion del reporte segun su tipo.
     */
//    private String generarDescripcion(String tipo, Long disciplinaId, Long alumnoId) {
//        return switch (tipo) {
//            case "Recaudacion" -> generarReporteRecaudacionPorDisciplina(disciplinaId);
//            case "Asistencia" -> generarReporteAsistenciasPorAlumno(alumnoId);
//            default -> throw new IllegalArgumentException("Tipo de reporte no valido.");
//        };
//    }

    /**
     * ✅ Obtener la recaudacion por disciplina.
     */
//    private String generarReporteRecaudacionPorDisciplina(Long disciplinaId) {
//        List<Object[]> resultados = inscripcionRepositorio.obtenerRecaudacionPorDisciplina();
//        if (resultados.isEmpty()) {
//            return "No se encontraron datos de recaudacion por disciplina.";
//        }
//
//        return resultados.stream()
//                .map(r -> "Disciplina: " + r[0] + " | Monto Total: $" + r[1])
//                .collect(Collectors.joining("\n"));
//    }

    /**
     * ✅ Obtener asistencias de un alumno especifico.
     */
    private String generarReporteAsistenciasPorAlumno(Long alumnoId) {
        return "Reporte de asistencias para el alumno con ID " + alumnoId;
    }

    /**
     * ✅ Listar todos los reportes generados.
     */
    @Override
    public List<ReporteResponse> listarReportes() {
        return reporteRepositorio.findAll().stream()
                .map(reporteMapper::toDTO)
                .collect(Collectors.toList());
    }

    /**
     * ✅ Obtener un reporte por ID.
     */
    @Override
    public ReporteResponse obtenerReportePorId(Long id) {
        Reporte reporte = reporteRepositorio.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Reporte no encontrado."));
        return reporteMapper.toDTO(reporte);
    }

    /**
     * ✅ Eliminar un reporte (baja logica).
     */
    @Override
    @Transactional
    public void eliminarReporte(Long id) {
        Reporte reporte = reporteRepositorio.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Reporte no encontrado."));
        reporte.setActivo(false);
        reporteRepositorio.save(reporte);
    }
}
package ledance.servicios;

import ledance.dto.request.ReporteRequest;
import ledance.dto.response.ReporteResponse;
import ledance.entidades.Reporte;
import ledance.repositorios.AsistenciaRepositorio;
import ledance.repositorios.InscripcionRepositorio;
import ledance.repositorios.ReporteRepositorio;
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

    private final InscripcionRepositorio inscripcionRepositorio;
    private final ReporteRepositorio reporteRepositorio;
    private final AsistenciaRepositorio asistenciaRepositorio;

    public ReporteServicio(InscripcionRepositorio inscripcionRepositorio,
                           ReporteRepositorio reporteRepositorio,
                           AsistenciaRepositorio asistenciaRepositorio) {
        this.inscripcionRepositorio = inscripcionRepositorio;
        this.reporteRepositorio = reporteRepositorio;
        this.asistenciaRepositorio = asistenciaRepositorio;
    }

    @Override
    @Transactional
    public List<ReporteResponse> generarReporte(ReporteRequest request) {
        log.info("Generando reporte de tipo: {}", request.tipo());
        List<ReporteResponse> reportes;

        switch (request.tipo()) {
            case "Recaudacion":
                reportes = generarReporteRecaudacionPorDisciplina();
                break;
            case "AsistenciaAlumno":
                reportes = generarReporteAsistenciasPorAlumno(request.alumnoId());
                break;
            case "AsistenciaDisciplina":
                reportes = generarReporteAsistenciasPorDisciplina(request.disciplinaId());
                break;
            case "AsistenciaDisciplinaAlumno":
                reportes = generarReporteAsistenciasPorDisciplinaYAlumno(request.disciplinaId(), request.alumnoId());
                break;
            default:
                throw new IllegalArgumentException("Tipo de reporte no válido.");
        }

        // Guardamos en BD y agregamos el reporte generado
        return guardarReportesEnBD(request.tipo(), reportes);
    }

    private List<ReporteResponse> generarReporteRecaudacionPorDisciplina() {
        List<Object[]> resultados = inscripcionRepositorio.obtenerRecaudacionPorDisciplina();
        return resultados.stream()
                .map(r -> new ReporteResponse(
                        null,  // Se asignará un ID luego
                        "Recaudación por Disciplina",
                        "Disciplina: " + r[0] + " | Monto: $" + r[1],
                        LocalDate.now(),
                        true
                ))
                .collect(Collectors.toList());
    }

    private List<ReporteResponse> generarReporteAsistenciasPorAlumno(Long alumnoId) {
        List<Object[]> resultados = asistenciaRepositorio.obtenerAsistenciasPorAlumno(alumnoId);
        return resultados.stream()
                .map(r -> new ReporteResponse(
                        null,
                        "Asistencias por Alumno",
                        "Alumno: " + r[0] + " | Asistencias: " + r[1],
                        LocalDate.now(),
                        true
                ))
                .collect(Collectors.toList());
    }

    private List<ReporteResponse> generarReporteAsistenciasPorDisciplina(Long disciplinaId) {
        List<Object[]> resultados = asistenciaRepositorio.obtenerAsistenciasPorDisciplina(disciplinaId);
        return resultados.stream()
                .map(r -> new ReporteResponse(
                        null,
                        "Asistencias por Disciplina",
                        "Disciplina: " + r[0] + " | Asistencias: " + r[1],
                        LocalDate.now(),
                        true
                ))
                .collect(Collectors.toList());
    }

    private List<ReporteResponse> generarReporteAsistenciasPorDisciplinaYAlumno(Long disciplinaId, Long alumnoId) {
        List<Object[]> resultados = asistenciaRepositorio.obtenerAsistenciasPorDisciplinaYAlumno(disciplinaId, alumnoId);
        return resultados.stream()
                .map(r -> new ReporteResponse(
                        null,
                        "Asistencias por Disciplina y Alumno",
                        "Disciplina: " + r[0] + " | Alumno: " + r[1] + " | Asistencias: " + r[2],
                        LocalDate.now(),
                        true
                ))
                .collect(Collectors.toList());
    }

    private List<ReporteResponse> guardarReportesEnBD(String tipo, List<ReporteResponse> reportes) {
        Reporte nuevoReporte = new Reporte();
        nuevoReporte.setTipo(tipo);
        nuevoReporte.setDescripcion("Reporte de " + tipo + " generado el " + LocalDate.now());
        nuevoReporte.setFechaGeneracion(LocalDate.now());
        nuevoReporte.setActivo(true);

        // Guardamos el nuevo reporte en la BD
        Reporte guardado = reporteRepositorio.save(nuevoReporte);

        // Convertimos el reporte guardado en DTO y lo agregamos a la lista
        reportes.add(new ReporteResponse(
                guardado.getId(),
                guardado.getTipo(),
                guardado.getDescripcion(),
                guardado.getFechaGeneracion(),
                guardado.getActivo()
        ));

        return reportes;
    }
}

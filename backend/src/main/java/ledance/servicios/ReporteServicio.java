package ledance.servicios;

import ledance.dto.request.ReporteRequest;
import ledance.dto.response.ReporteResponse;
import ledance.entidades.Reporte;
import ledance.repositorios.AsistenciaRepositorio;
import ledance.repositorios.InscripcionRepositorio;
import ledance.repositorios.ReporteRepositorio;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class ReporteServicio {

    private final InscripcionRepositorio inscripcionRepositorio;
    private final ReporteRepositorio reporteRepositorio;
    private final AsistenciaRepositorio asistenciaRepositorio;

    public ReporteServicio(InscripcionRepositorio inscripcionRepositorio, ReporteRepositorio reporteRepositorio, AsistenciaRepositorio asistenciaRepositorio) {
        this.inscripcionRepositorio = inscripcionRepositorio;
        this.reporteRepositorio = reporteRepositorio;
        this.asistenciaRepositorio = asistenciaRepositorio;
    }

    public List<ReporteResponse> generarReporte(ReporteRequest request) {
        List<ReporteResponse> reporte;

        switch (request.tipo()) {
            case "Recaudacion":
                reporte = generarReporteRecaudacionPorDisciplina();
                break;
            case "AsistenciaAlumno":
                reporte = generarReporteAsistenciasPorAlumno(request.alumnoId());
                break;
            case "AsistenciaDisciplina":
                reporte = generarReporteAsistenciasPorDisciplina(request.disciplinaId());
                break;
            case "AsistenciaDisciplinaAlumno":
                reporte = generarReporteAsistenciasPorDisciplinaYAlumno(request.disciplinaId(), request.alumnoId());
                break;
            default:
                throw new IllegalArgumentException("Tipo de reporte no válido.");
        }

        guardarReporteEnBD(request.tipo(), reporte);
        return reporte;
    }

    private List<ReporteResponse> generarReporteRecaudacionPorDisciplina() {
        List<Object[]> resultados = inscripcionRepositorio.obtenerRecaudacionPorDisciplina();

        return resultados.stream()
                .map(r -> new ReporteResponse("Recaudación por Disciplina",
                        "Disciplina: " + r[0] + " | Monto: $" + r[1]))
                .collect(Collectors.toList());
    }

    private List<ReporteResponse> generarReporteAsistenciasPorAlumno(Long alumnoId) {
        List<Object[]> resultados = asistenciaRepositorio.obtenerAsistenciasPorAlumno(alumnoId);

        return resultados.stream()
                .map(r -> new ReporteResponse("Asistencias por Alumno",
                        "Alumno: " + r[0] + " | Asistencias: " + r[1]))
                .collect(Collectors.toList());
    }

    private List<ReporteResponse> generarReporteAsistenciasPorDisciplina(Long disciplinaId) {
        List<Object[]> resultados = asistenciaRepositorio.obtenerAsistenciasPorDisciplina(disciplinaId);

        return resultados.stream()
                .map(r -> new ReporteResponse("Asistencias por Disciplina",
                        "Disciplina: " + r[0] + " | Asistencias: " + r[1]))
                .collect(Collectors.toList());
    }

    private List<ReporteResponse> generarReporteAsistenciasPorDisciplinaYAlumno(Long disciplinaId, Long alumnoId) {
        List<Object[]> resultados = asistenciaRepositorio.obtenerAsistenciasPorDisciplinaYAlumno(disciplinaId, alumnoId);

        return resultados.stream()
                .map(r -> new ReporteResponse("Asistencias por Disciplina y Alumno",
                        "Disciplina: " + r[0] + " | Alumno: " + r[1] + " | Asistencias: " + r[2]))
                .collect(Collectors.toList());
    }

    private void guardarReporteEnBD(String tipo, List<ReporteResponse> reporte) {
        Reporte nuevoReporte = new Reporte();
        nuevoReporte.setTipo(tipo);
        nuevoReporte.setDescripcion("Reporte de " + tipo + " generado el " + LocalDate.now());
        nuevoReporte.setFechaGeneracion(LocalDate.now());
        nuevoReporte.setActivo(true);
        reporteRepositorio.save(nuevoReporte);
    }
}

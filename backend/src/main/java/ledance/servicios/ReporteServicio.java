package ledance.servicios;

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

    public List<String> generarReporteRecaudacionPorDisciplina() {
        List<Object[]> resultados = inscripcionRepositorio.obtenerRecaudacionPorDisciplina();

        List<String> reporte = resultados.stream()
                .map(r -> "Disciplina: " + r[0] + " | Recaudación: $" + r[1])
                .collect(Collectors.toList());

        Reporte nuevoReporte = new Reporte();
        nuevoReporte.setTipo("Recaudacion");
        nuevoReporte.setDescripcion("Reporte de recaudación generado el " + LocalDate.now());
        nuevoReporte.setFechaGeneracion(LocalDate.now());
        nuevoReporte.setActivo(true);
        reporteRepositorio.save(nuevoReporte);

        return reporte;
    }

    public List<String> generarReporteAsistencias() {
        List<Object[]> resultados = asistenciaRepositorio.obtenerAsistenciasPorAlumnoYDisciplina();

        List<String> reporte = resultados.stream()
                .map(r -> "Alumno: " + r[0] + " | Disciplina: " + r[1] + " | Asistencias: " + r[2])
                .collect(Collectors.toList());

        Reporte nuevoReporte = new Reporte();
        nuevoReporte.setTipo("Asistencia");
        nuevoReporte.setDescripcion("Reporte de asistencias generado el " + LocalDate.now());
        nuevoReporte.setFechaGeneracion(LocalDate.now());
        nuevoReporte.setActivo(true);
        reporteRepositorio.save(nuevoReporte);

        return reporte;
    }
}

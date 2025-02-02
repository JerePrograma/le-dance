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
                throw new IllegalArgumentException("Tipo de reporte no valido.");
        }
        guardarReporteEnBD(request.tipo(), reporte);
        return reporte;
    }

    private List<ReporteResponse> generarReporteRecaudacionPorDisciplina() {
        List<Object[]> resultados = inscripcionRepositorio.obtenerRecaudacionPorDisciplina();
        return resultados.stream()
                .map(r -> new ReporteResponse(
                        null,  // ❌ No se tiene ID en estos resultados
                        "Recaudacion por Disciplina",
                        "Disciplina: " + r[0] + " | Monto: $" + r[1],
                        LocalDate.now(),  // ✅ Se asigna la fecha actual
                        true  // ✅ Se asume que el reporte esta activo
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
        List<Object[]> resultados = asistenciaRepositorio.obtenerAsistenciasPorDisciplina(disciplinaId);  // ✅ Corregido el error tipografico
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


    private ReporteResponse guardarReporteEnBD(String tipo, List<ReporteResponse> reporte) {
        Reporte nuevoReporte = new Reporte();
        nuevoReporte.setTipo(tipo);
        nuevoReporte.setDescripcion("Reporte de " + tipo + " generado el " + LocalDate.now());
        nuevoReporte.setFechaGeneracion(LocalDate.now());  // ✅ Asignar fecha antes de guardar
        nuevoReporte.setActivo(true);  // ✅ Asignar activo antes de guardar
        Reporte guardado = reporteRepositorio.save(nuevoReporte);

        return new ReporteResponse(
                guardado.getId(),
                guardado.getTipo(),
                guardado.getDescripcion(),
                guardado.getFechaGeneracion(),
                guardado.getActivo()
        );
    }


}

package ledance.servicios.reporte;

import ledance.dto.reporte.response.ReporteResponse;
import ledance.dto.reporte.request.ReporteRegistroRequest;
import java.util.List;

public interface IReporteServicio {
    ReporteResponse generarReporte(ReporteRegistroRequest request);
    ReporteResponse generarReporteRecaudacionPorDisciplina(Long disciplinaId, Long usuarioId);
    ReporteResponse generarReporteAsistenciasPorAlumno(Long alumnoId, Long usuarioId);
    ReporteResponse generarReporteAsistenciasPorDisciplina(Long disciplinaId, Long usuarioId);
    ReporteResponse generarReporteAsistenciasPorDisciplinaAlumno(Long disciplinaId, Long alumnoId, Long usuarioId);
    List<ReporteResponse> listarReportes();
    ReporteResponse obtenerReportePorId(Long id);
    void eliminarReporte(Long id);
}

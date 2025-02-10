package ledance.servicios;

import ledance.dto.request.ReporteRegistroRequest;
import ledance.dto.response.ReporteResponse;

import java.util.List;

public interface IReporteServicio {
    ReporteResponse generarReporte(ReporteRegistroRequest request);
    List<ReporteResponse> listarReportes();
    ReporteResponse obtenerReportePorId(Long id);
    void eliminarReporte(Long id);
}

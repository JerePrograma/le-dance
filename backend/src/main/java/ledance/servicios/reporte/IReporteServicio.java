package ledance.servicios.reporte;

import ledance.dto.reporte.request.ReporteRegistroRequest;
import ledance.dto.reporte.response.ReporteResponse;

import java.util.List;

public interface IReporteServicio {
    ReporteResponse generarReporte(ReporteRegistroRequest request);

    List<ReporteResponse> listarReportes();

    ReporteResponse obtenerReportePorId(Long id);

    void eliminarReporte(Long id);
}
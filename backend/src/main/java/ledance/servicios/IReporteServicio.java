package ledance.servicios;

import ledance.dto.request.ReporteRequest;
import ledance.dto.response.ReporteResponse;

import java.util.List;

public interface IReporteServicio {
    List<ReporteResponse> generarReporte(ReporteRequest request);
}

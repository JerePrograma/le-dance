package ledance.dto.caja.response;

import ledance.dto.alumno.response.AlumnoListadoResponse;
import ledance.dto.concepto.response.ConceptoResponse;
import ledance.dto.disciplina.response.DisciplinaListadoResponse;
import ledance.dto.metodopago.response.MetodoPagoResponse;
import ledance.dto.stock.response.StockResponse;

import java.util.List;

public record CobranzasDataResponse(
        List<AlumnoListadoResponse> alumnos,
        List<DisciplinaListadoResponse> disciplinas,
        List<StockResponse> stocks,
        List<MetodoPagoResponse> metodosPago,
        List<ConceptoResponse> conceptos
) {}

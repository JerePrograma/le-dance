package ledance.dto.caja.response;

import ledance.dto.alumno.response.AlumnoResponse;
import ledance.dto.bonificacion.response.BonificacionResponse;
import ledance.dto.concepto.response.ConceptoResponse;
import ledance.dto.disciplina.response.DisciplinaResponse;
import ledance.dto.metodopago.response.MetodoPagoResponse;
import ledance.dto.recargo.response.RecargoResponse;
import ledance.dto.stock.response.StockResponse;
import ledance.entidades.Recargo;

import java.util.List;

public record CobranzasDataResponse(
        List<AlumnoResponse> alumnos,
        List<DisciplinaResponse> disciplinas,
        List<StockResponse> stocks,
        List<MetodoPagoResponse> metodosPago,
        List<ConceptoResponse> conceptos,
        List<BonificacionResponse> bonificaciones,
        List<RecargoResponse> recargos
) {
}

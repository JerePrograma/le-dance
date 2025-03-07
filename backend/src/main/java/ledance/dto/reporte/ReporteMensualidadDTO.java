package ledance.dto.reporte;

import ledance.dto.alumno.response.AlumnoListadoResponse;
import ledance.dto.bonificacion.response.BonificacionResponse;
import ledance.dto.disciplina.response.DisciplinaListadoResponse;

public record ReporteMensualidadDTO(
        Long mensualidadId,
        AlumnoListadoResponse alumno,
        String cuota,
        Double importe,
        BonificacionResponse bonificacion,
        Double total,
        Double recargo,
        String estado,
        DisciplinaListadoResponse disciplina,
        String descripcion  // Nuevo campo agregado
) {}

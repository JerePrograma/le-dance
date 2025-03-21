package ledance.dto.reporte;

import ledance.dto.alumno.response.AlumnoResponse;
import ledance.dto.bonificacion.response.BonificacionResponse;
import ledance.dto.disciplina.response.DisciplinaResponse;

public record ReporteMensualidadDTO(
        Long mensualidadId,
        AlumnoResponse alumno,
        String cuota,
        Double importe,
        BonificacionResponse bonificacion,
        Double total,
        Double recargo,
        String estado,
        DisciplinaResponse disciplina,
        String descripcion  // Nuevo campo agregado
) {}

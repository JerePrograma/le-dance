package ledance.entidades;

import java.time.LocalDate;

public class ReportCriteria {
    private String reportType; // "asistencias_alumno", "recaudacion_disciplina", etc.
    private Long alumnoId;
    private Long disciplinaId;
    private LocalDate fechaInicio;
    private LocalDate fechaFin;
    // Otros filtros especificos (por concepto, stock, etc.)
    // Getters y setters
}

package ledance.dto.reporte;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ReportCriteria {
    private String reportType;
    private Long alumnoId;
    private Long disciplinaId;
    private LocalDate fechaInicio;
    private LocalDate fechaFin;
    // Constructores, getters, setters

}

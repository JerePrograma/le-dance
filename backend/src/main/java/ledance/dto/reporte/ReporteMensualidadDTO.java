// ReporteMensualidadDTO.java
package ledance.dto.reporte;

public record ReporteMensualidadDTO(
        Long mensualidadId,
        String alumnoNombre,
        String cuota,
        Double importe,
        Double bonificacion,
        Double total,
        Double recargo,
        String estado,
        String disciplina
) {}

package ledance.dto.matricula.request;

public record MatriculaModificacionRequest(
        Integer anio,
        Boolean pagada,
        java.time.LocalDate fechaPago
) { }

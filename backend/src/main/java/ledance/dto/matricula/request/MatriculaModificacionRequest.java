package ledance.dto.matricula.request;

public record MatriculaModificacionRequest(
        Boolean pagada,
        java.time.LocalDate fechaPago
) {
}
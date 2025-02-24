package ledance.dto.matricula.response;

public record MatriculaResponse(
        Long id,
        Integer anio,
        Boolean pagada,
        java.time.LocalDate fechaPago,
        Long alumnoId
) {
}
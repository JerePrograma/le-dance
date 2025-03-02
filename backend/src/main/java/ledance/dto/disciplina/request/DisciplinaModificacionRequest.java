package ledance.dto.disciplina.request;

import jakarta.validation.constraints.NotNull;
import java.util.List;

/**
 * Petición para modificar una disciplina existente.
 */
public record DisciplinaModificacionRequest(
        @NotNull String nombre,
        @NotNull Long salonId,
        @NotNull Long profesorId,
        Long recargoId,
        @NotNull Double valorCuota,
        @NotNull Double matricula,
        Double claseSuelta,
        Double clasePrueba,
        Boolean activo, // ✅ Ahora se puede modificar el estado
        List<DisciplinaHorarioRequest> horarios // ✅ Lista de horarios independientes
) {}

// DisciplinaModificacionRequest.java
package ledance.dto.disciplina.request;

import jakarta.validation.constraints.NotNull;
import java.util.List;

public record DisciplinaModificacionRequest(
        @NotNull String nombre,
        @NotNull Long salonId,
        @NotNull Long profesorId,
        Long recargoId,
        Double valorCuota,
        Double matricula,
        Double claseSuelta,
        Double clasePrueba,
        Boolean activo,
        List<DisciplinaHorarioModificacionRequest> horarios  // Cambiado para incluir ID en cada horario
) { }

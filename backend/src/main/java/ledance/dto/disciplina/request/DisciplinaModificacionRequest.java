package ledance.dto.disciplina.request;

import jakarta.validation.constraints.NotNull;
import java.util.List;

public record DisciplinaModificacionRequest(
        @NotNull String nombre,
        @NotNull Long salonId,
        @NotNull Long profesorId,
        Long recargoId,
        @NotNull Double valorCuota,
        @NotNull Double matricula,
        Double claseSuelta,
        Double clasePrueba,
        Boolean activo,
        List<DisciplinaHorarioRequest> horarios
) {}

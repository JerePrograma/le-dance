package ledance.dto.disciplina.request;

import jakarta.validation.constraints.NotNull;
import java.util.List;

public record DisciplinaRegistroRequest(
        Long id,
        @NotNull String nombre,
        @NotNull Long salonId,
        @NotNull Long profesorId,
        Long recargoId,
        Double valorCuota,
        Double matricula,
        Double claseSuelta,
        Double clasePrueba,
        List<DisciplinaHorarioRequest> horarios
) {}

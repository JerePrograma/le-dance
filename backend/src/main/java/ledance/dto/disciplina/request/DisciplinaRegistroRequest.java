package ledance.dto.disciplina.request;

import jakarta.validation.constraints.NotNull;
import java.util.List;

/**
 * Peticion para registrar una nueva disciplina.
 * - "activo" se asigna automaticamente en el servicio.
 */
public record DisciplinaRegistroRequest(
        @NotNull String nombre,
        @NotNull Long salonId, // ✅ Relación con "Salon"
        @NotNull Long profesorId,
        Long recargoId, // ✅ Relación con "Recargo" opcional
        @NotNull Double valorCuota,
        @NotNull Double matricula,
        Double claseSuelta, // ✅ Opcional
        Double clasePrueba, // ✅ Opcional
        List<DisciplinaHorarioRequest> horarios // ✅ Lista de horarios para cada día
) {}

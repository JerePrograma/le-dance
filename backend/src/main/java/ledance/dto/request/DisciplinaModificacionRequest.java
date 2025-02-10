package ledance.dto.request;

import java.time.LocalTime;
import java.util.Set;
import jakarta.validation.constraints.NotNull;
import ledance.entidades.DiaSemana;

/**
 * Petición para modificar una disciplina existente.
 * - Permite cambiar "activo" (activar o desactivar la disciplina).
 */
public record DisciplinaModificacionRequest(
        @NotNull String nombre,
        @NotNull Set<DiaSemana> diasSemana,
        Integer frecuenciaSemanal,
        @NotNull LocalTime horarioInicio,
        @NotNull Double duracion,
        @NotNull Long salonId,
        @NotNull Long profesorId,
        Long recargoId,
        @NotNull Double valorCuota,
        @NotNull Double matricula,
        Double claseSuelta,
        Double clasePrueba,
        Boolean activo // ✅ Ahora se puede modificar el estado
) {}

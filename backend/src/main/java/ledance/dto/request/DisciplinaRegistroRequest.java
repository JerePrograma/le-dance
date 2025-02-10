package ledance.dto.request;

import java.time.LocalTime;
import java.util.Set;
import jakarta.validation.constraints.NotNull;
import ledance.entidades.DiaSemana;

/**
 * Petición para registrar una nueva disciplina.
 * - "activo" se asigna automáticamente en el servicio.
 */
public record DisciplinaRegistroRequest(
        @NotNull String nombre,
        @NotNull Set<DiaSemana> diasSemana, // ✅ Días seleccionables (LUNES-SÁBADO)
        Integer frecuenciaSemanal,
        @NotNull LocalTime horarioInicio, // ✅ Nuevo campo agregado
        @NotNull Double duracion, // ✅ En horas
        @NotNull Long salonId, // ✅ Relación con "Salón"
        @NotNull Long profesorId,
        Long recargoId, // ✅ Relación con "Recargo" opcional
        @NotNull Double valorCuota,
        @NotNull Double matricula,
        Double claseSuelta, // ✅ Opcional
        Double clasePrueba // ✅ Opcional
) {}

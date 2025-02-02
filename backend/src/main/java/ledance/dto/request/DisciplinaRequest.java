package ledance.dto.request;

public record DisciplinaRequest(
        String nombre,
        String horario,
        Integer frecuenciaSemanal,
        String duracion,
        String salon,
        Double valorCuota,
        Double matricula,
        Long profesorId,
        Boolean activo // ✅ Agregado para corregir el error
) {}

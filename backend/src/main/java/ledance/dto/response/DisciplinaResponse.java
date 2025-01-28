package ledance.dto.response;

public record DisciplinaResponse(
        Long id,
        String nombre,
        String horario,
        Integer frecuenciaSemanal,
        String duracion,
        String salon,
        Double valorCuota,
        Double matricula,
        Long profesorId,
        int inscritos // Nuevo campo que indica la cantidad de alumnos inscritos
) {}

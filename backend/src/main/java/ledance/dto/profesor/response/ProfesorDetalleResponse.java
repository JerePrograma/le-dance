package ledance.dto.profesor.response;

import ledance.dto.disciplina.response.DisciplinaListadoResponse;

import java.time.LocalDate;
import java.util.List;

public record ProfesorDetalleResponse(
        Long id,
        String nombre,
        String apellido,
        LocalDate fechaNacimiento, // ✅ NUEVO
        Integer edad, // ✅ Se calcula automaticamente
        String telefono, // ✅ NUEVO
        Boolean activo,
        List<DisciplinaListadoResponse> disciplinas // ✅ Lista de disciplinas que imparte
) {
}
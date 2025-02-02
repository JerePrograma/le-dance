package ledance.dto.response;

import java.time.LocalDate;

public record AsistenciaResponseDTO(
        Long id,
        LocalDate fecha,
        Boolean presente,
        String observacion,
        AlumnoListadoResponse alumno,
        DisciplinaSimpleResponse disciplina,
        ProfesorResponse profesor,  // ✅ Nuevo campo opcional
        Boolean activo               // ✅ Se agrega el campo activo
) {}

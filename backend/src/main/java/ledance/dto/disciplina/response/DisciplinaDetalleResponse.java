package ledance.dto.disciplina.response;

import ledance.entidades.DiaSemana;

import java.time.LocalTime;
import java.util.Set;

public record DisciplinaDetalleResponse(
        Long id,
        String nombre,
        Set<DiaSemana> diasSemana,
        LocalTime horarioInicio,
        Double duracion,
        String salon,
        Long salonId,
        Double valorCuota,
        Double matricula,
        String profesorNombre,
        String profesorApellido,
        Long profesorId,
        Integer inscritos,
        Boolean activo,
        Double claseSuelta,
        Double clasePrueba
) {
}
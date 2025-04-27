package ledance.dto.disciplina;

import java.time.LocalTime;
import javax.annotation.processing.Generated;
import ledance.dto.disciplina.response.DisciplinaHorarioResponse;
import ledance.entidades.DiaSemana;
import ledance.entidades.DisciplinaHorario;
import org.springframework.stereotype.Component;

@Generated(
    value = "org.mapstruct.ap.MappingProcessor",
    date = "2025-04-26T21:11:52-0300",
    comments = "version: 1.6.3, compiler: javac, environment: Java 17.0.14 (Amazon.com Inc.)"
)
@Component
public class DisciplinaHorarioMapperImpl implements DisciplinaHorarioMapper {

    @Override
    public DisciplinaHorarioResponse toResponse(DisciplinaHorario horario) {
        if ( horario == null ) {
            return null;
        }

        Long id = null;
        DiaSemana diaSemana = null;
        LocalTime horarioInicio = null;
        Double duracion = null;

        id = horario.getId();
        diaSemana = horario.getDiaSemana();
        horarioInicio = horario.getHorarioInicio();
        duracion = horario.getDuracion();

        DisciplinaHorarioResponse disciplinaHorarioResponse = new DisciplinaHorarioResponse( id, diaSemana, horarioInicio, duracion );

        return disciplinaHorarioResponse;
    }
}

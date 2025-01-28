package ledance.validaciones.disciplinas;

import ledance.dto.request.DisciplinaRequest;
import ledance.repositorios.DisciplinaRepositorio;
import ledance.validaciones.Validador;
import org.springframework.stereotype.Component;

@Component
public class ValidadorDisciplinaDuplicada implements Validador<DisciplinaRequest> {

    private final DisciplinaRepositorio disciplinaRepositorio;

    public ValidadorDisciplinaDuplicada(DisciplinaRepositorio disciplinaRepositorio) {
        this.disciplinaRepositorio = disciplinaRepositorio;
    }

    @Override
    public void validar(DisciplinaRequest datos) {
        if (disciplinaRepositorio.existsByNombreAndHorario(datos.nombre(), datos.horario())) {
            throw new RuntimeException("La disciplina ya esta registrada con el mismo nombre y horario: "
                    + datos.nombre());
        }
    }
}

package ledance.validaciones.profesores;

import ledance.dto.request.ProfesorRegistroRequest;
import ledance.repositorios.ProfesorRepositorio;
import ledance.validaciones.Validador;
import org.springframework.stereotype.Component;

@Component
public class ValidadorProfesorDuplicado implements Validador<ProfesorRegistroRequest> {

    private final ProfesorRepositorio profesorRepositorio;

    public ValidadorProfesorDuplicado(ProfesorRepositorio profesorRepositorio) {
        this.profesorRepositorio = profesorRepositorio;
    }

    @Override
    public void validar(ProfesorRegistroRequest datos) {
        if (profesorRepositorio.existsByNombreAndApellido(datos.nombre(), datos.apellido())) {
            throw new RuntimeException("El profesor ya esta registrado: "
                    + datos.nombre() + " " + datos.apellido());
        }
    }
}

package ledance.validaciones.alumnos;

import ledance.dto.alumno.request.AlumnoRegistroRequest;
import ledance.repositorios.AlumnoRepositorio;
import ledance.validaciones.Validador;
import org.springframework.stereotype.Component;

@Component
public class ValidadorAlumnoDuplicado implements Validador<AlumnoRegistroRequest> {

    private final AlumnoRepositorio alumnoRepositorio;

    public ValidadorAlumnoDuplicado(AlumnoRepositorio alumnoRepositorio) {
        this.alumnoRepositorio = alumnoRepositorio;
    }

    @Override
    public void validar(AlumnoRegistroRequest datos) {
        if (alumnoRepositorio.existsByNombreAndDocumento(datos.nombre(), datos.documento())) {
            throw new RuntimeException("El alumno ya esta registrado con el documento: "
                    + datos.documento());
        }
    }
}

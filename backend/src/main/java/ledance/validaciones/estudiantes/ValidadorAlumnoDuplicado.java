package ledance.validaciones.alumnos;

import ledance.dto.request.AlumnoRequest;
import ledance.repositorios.AlumnoRepositorio;
import ledance.validaciones.Validador;
import org.springframework.stereotype.Component;

@Component
public class ValidadorAlumnoDuplicado implements Validador<AlumnoRequest> {

    private final AlumnoRepositorio alumnoRepositorio;

    public ValidadorAlumnoDuplicado(AlumnoRepositorio alumnoRepositorio) {
        this.alumnoRepositorio = alumnoRepositorio;
    }

    @Override
    public void validar(AlumnoRequest datos) {
        if (alumnoRepositorio.existsByNombreAndDocumento(datos.nombre(), datos.documento())) {
            throw new RuntimeException("El alumno ya esta registrado con el documento: "
                    + datos.documento());
        }
    }
}

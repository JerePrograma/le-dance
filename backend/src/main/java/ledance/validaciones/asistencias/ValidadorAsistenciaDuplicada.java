package ledance.validaciones.asistencias;

import ledance.dto.request.AsistenciaRequest;
import ledance.repositorios.AsistenciaRepositorio;
import ledance.validaciones.Validador;
import org.springframework.stereotype.Component;

@Component
public class ValidadorAsistenciaDuplicada implements Validador<AsistenciaRequest> {

    private final AsistenciaRepositorio asistenciaRepositorio;

    public ValidadorAsistenciaDuplicada(AsistenciaRepositorio asistenciaRepositorio) {
        this.asistenciaRepositorio = asistenciaRepositorio;
    }

    @Override
    public void validar(AsistenciaRequest datos) {
        if (asistenciaRepositorio.existsByAlumnoIdAndFecha(datos.alumnoId(), datos.fecha())) {
            throw new RuntimeException("Ya se ha registrado asistencia para este alumno en la fecha: "
                    + datos.fecha());
        }
    }
}

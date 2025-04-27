package ledance.dto.mappers;

import javax.annotation.processing.Generated;
import ledance.dto.request.ObservacionMensualRequest;
import ledance.dto.response.ObservacionMensualResponse;
import ledance.entidades.Alumno;
import ledance.entidades.ObservacionMensual;
import org.springframework.stereotype.Component;

@Generated(
    value = "org.mapstruct.ap.MappingProcessor",
    date = "2025-04-26T21:11:52-0300",
    comments = "version: 1.6.3, compiler: javac, environment: Java 17.0.14 (Amazon.com Inc.)"
)
@Component
public class ObservacionMensualMapperImpl implements ObservacionMensualMapper {

    @Override
    public ObservacionMensualResponse toDTO(ObservacionMensual observacionMensual) {
        if ( observacionMensual == null ) {
            return null;
        }

        Long alumnoId = null;
        Long id = null;
        String observacion = null;

        alumnoId = observacionMensualAlumnoId( observacionMensual );
        id = observacionMensual.getId();
        observacion = observacionMensual.getObservacion();

        ObservacionMensualResponse observacionMensualResponse = new ObservacionMensualResponse( id, alumnoId, observacion );

        return observacionMensualResponse;
    }

    @Override
    public ObservacionMensual toEntity(ObservacionMensualRequest request) {
        if ( request == null ) {
            return null;
        }

        ObservacionMensual observacionMensual = new ObservacionMensual();

        observacionMensual.setObservacion( request.observacion() );

        return observacionMensual;
    }

    private Long observacionMensualAlumnoId(ObservacionMensual observacionMensual) {
        Alumno alumno = observacionMensual.getAlumno();
        if ( alumno == null ) {
            return null;
        }
        return alumno.getId();
    }
}

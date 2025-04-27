package ledance.dto.reporte.observacion;

import java.time.LocalDate;
import javax.annotation.processing.Generated;
import ledance.entidades.ObservacionProfesor;
import ledance.entidades.Profesor;
import org.springframework.stereotype.Component;

@Generated(
    value = "org.mapstruct.ap.MappingProcessor",
    date = "2025-04-26T21:11:51-0300",
    comments = "version: 1.6.3, compiler: javac, environment: Java 17.0.14 (Amazon.com Inc.)"
)
@Component
public class ObservacionProfesorMapperImpl implements ObservacionProfesorMapper {

    @Override
    public ObservacionProfesorDTO toDTO(ObservacionProfesor entity) {
        if ( entity == null ) {
            return null;
        }

        Long profesorId = null;
        Long id = null;
        LocalDate fecha = null;
        String observacion = null;

        profesorId = entityProfesorId( entity );
        id = entity.getId();
        fecha = entity.getFecha();
        observacion = entity.getObservacion();

        ObservacionProfesorDTO observacionProfesorDTO = new ObservacionProfesorDTO( id, profesorId, fecha, observacion );

        return observacionProfesorDTO;
    }

    @Override
    public ObservacionProfesor toEntity(ObservacionProfesorDTO dto) {
        if ( dto == null ) {
            return null;
        }

        ObservacionProfesor observacionProfesor = new ObservacionProfesor();

        observacionProfesor.setId( dto.id() );
        observacionProfesor.setFecha( dto.fecha() );
        observacionProfesor.setObservacion( dto.observacion() );

        return observacionProfesor;
    }

    private Long entityProfesorId(ObservacionProfesor observacionProfesor) {
        Profesor profesor = observacionProfesor.getProfesor();
        if ( profesor == null ) {
            return null;
        }
        return profesor.getId();
    }
}

package ledance.dto.matricula;

import java.time.LocalDate;
import javax.annotation.processing.Generated;
import ledance.dto.matricula.request.MatriculaRegistroRequest;
import ledance.dto.matricula.response.MatriculaResponse;
import ledance.entidades.Alumno;
import ledance.entidades.Inscripcion;
import ledance.entidades.Matricula;
import org.springframework.stereotype.Component;

@Generated(
    value = "org.mapstruct.ap.MappingProcessor",
    date = "2025-04-27T11:12:54-0300",
    comments = "version: 1.6.3, compiler: javac, environment: Java 17.0.14 (Amazon.com Inc.)"
)
@Component
public class MatriculaMapperImpl implements MatriculaMapper {

    @Override
    public MatriculaResponse toResponse(Matricula matricula) {
        if ( matricula == null ) {
            return null;
        }

        Long alumnoId = null;
        Long id = null;
        Integer anio = null;
        Boolean pagada = null;
        LocalDate fechaPago = null;

        alumnoId = matriculaAlumnoId( matricula );
        id = matricula.getId();
        anio = matricula.getAnio();
        pagada = matricula.getPagada();
        fechaPago = matricula.getFechaPago();

        MatriculaResponse matriculaResponse = new MatriculaResponse( id, anio, pagada, fechaPago, alumnoId );

        return matriculaResponse;
    }

    @Override
    public Matricula toEntity(MatriculaRegistroRequest request) {
        if ( request == null ) {
            return null;
        }

        Matricula matricula = new Matricula();

        matricula.setAlumno( matriculaRegistroRequestToAlumno( request ) );
        matricula.setAnio( request.anio() );

        matricula.setPagada( false );

        return matricula;
    }

    @Override
    public Matricula toEntity(MatriculaResponse response) {
        if ( response == null ) {
            return null;
        }

        Matricula matricula = new Matricula();

        matricula.setAlumno( matriculaResponseToAlumno( response ) );
        matricula.setId( response.id() );
        matricula.setAnio( response.anio() );
        matricula.setPagada( response.pagada() );
        matricula.setFechaPago( response.fechaPago() );

        return matricula;
    }

    @Override
    public void updateEntityFromRequest(MatriculaRegistroRequest request, Matricula matricula) {
        if ( request == null ) {
            return;
        }

        matricula.setAnio( request.anio() );
        matricula.setPagada( request.pagada() );
        matricula.setFechaPago( request.fechaPago() );
    }

    @Override
    public MatriculaRegistroRequest toRegistroRequest(Inscripcion inscripcionObtenida) {
        if ( inscripcionObtenida == null ) {
            return null;
        }

        Long alumnoId = null;

        alumnoId = inscripcionObtenidaAlumnoId( inscripcionObtenida );

        Integer anio = inscripcionObtenida.getFechaInscripcion() != null ? inscripcionObtenida.getFechaInscripcion().getYear() : null;
        Boolean pagada = false;
        LocalDate fechaPago = null;

        MatriculaRegistroRequest matriculaRegistroRequest = new MatriculaRegistroRequest( alumnoId, anio, pagada, fechaPago );

        return matriculaRegistroRequest;
    }

    private Long matriculaAlumnoId(Matricula matricula) {
        Alumno alumno = matricula.getAlumno();
        if ( alumno == null ) {
            return null;
        }
        return alumno.getId();
    }

    protected Alumno matriculaRegistroRequestToAlumno(MatriculaRegistroRequest matriculaRegistroRequest) {
        if ( matriculaRegistroRequest == null ) {
            return null;
        }

        Alumno alumno = new Alumno();

        alumno.setId( matriculaRegistroRequest.alumnoId() );

        return alumno;
    }

    protected Alumno matriculaResponseToAlumno(MatriculaResponse matriculaResponse) {
        if ( matriculaResponse == null ) {
            return null;
        }

        Alumno alumno = new Alumno();

        alumno.setId( matriculaResponse.alumnoId() );

        return alumno;
    }

    private Long inscripcionObtenidaAlumnoId(Inscripcion inscripcion) {
        Alumno alumno = inscripcion.getAlumno();
        if ( alumno == null ) {
            return null;
        }
        return alumno.getId();
    }
}

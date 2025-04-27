package ledance.dto.asistencia;

import java.time.LocalDate;
import java.util.List;
import javax.annotation.processing.Generated;
import ledance.dto.alumno.response.AlumnoResponse;
import ledance.dto.asistencia.request.AsistenciaDiariaModificacionRequest;
import ledance.dto.asistencia.request.AsistenciaDiariaRegistroRequest;
import ledance.dto.asistencia.response.AsistenciaDiariaDetalleResponse;
import ledance.dto.inscripcion.response.InscripcionResponse;
import ledance.entidades.AsistenciaAlumnoMensual;
import ledance.entidades.AsistenciaDiaria;
import ledance.entidades.AsistenciaMensual;
import ledance.entidades.Disciplina;
import ledance.entidades.EstadoAsistencia;
import ledance.entidades.Inscripcion;
import org.springframework.stereotype.Component;

@Generated(
    value = "org.mapstruct.ap.MappingProcessor",
    date = "2025-04-26T21:11:50-0300",
    comments = "version: 1.6.3, compiler: javac, environment: Java 17.0.14 (Amazon.com Inc.)"
)
@Component
public class AsistenciaDiariaMapperImpl implements AsistenciaDiariaMapper {

    @Override
    public AsistenciaDiariaDetalleResponse toDTO(AsistenciaDiaria asistenciaDiaria) {
        if ( asistenciaDiaria == null ) {
            return null;
        }

        Long id = null;
        LocalDate fecha = null;
        EstadoAsistencia estado = null;
        AlumnoResponse alumno = null;
        Long asistenciaAlumnoMensualId = null;
        Long asistenciaMensualId = null;
        Long disciplinaId = null;

        id = asistenciaDiaria.getId();
        fecha = asistenciaDiaria.getFecha();
        estado = asistenciaDiaria.getEstado();
        alumno = inscripcionToAlumnoResponse( asistenciaDiariaAsistenciaAlumnoMensualInscripcion( asistenciaDiaria ) );
        asistenciaAlumnoMensualId = asistenciaDiariaAsistenciaAlumnoMensualId( asistenciaDiaria );
        asistenciaMensualId = asistenciaDiariaAsistenciaAlumnoMensualAsistenciaMensualId( asistenciaDiaria );
        disciplinaId = asistenciaDiariaAsistenciaAlumnoMensualAsistenciaMensualDisciplinaId( asistenciaDiaria );

        AsistenciaDiariaDetalleResponse asistenciaDiariaDetalleResponse = new AsistenciaDiariaDetalleResponse( id, fecha, estado, alumno, asistenciaMensualId, disciplinaId, asistenciaAlumnoMensualId );

        return asistenciaDiariaDetalleResponse;
    }

    @Override
    public AsistenciaDiaria toEntity(AsistenciaDiariaRegistroRequest request) {
        if ( request == null ) {
            return null;
        }

        AsistenciaDiaria asistenciaDiaria = new AsistenciaDiaria();

        asistenciaDiaria.setId( request.id() );
        asistenciaDiaria.setFecha( request.fecha() );
        asistenciaDiaria.setEstado( request.estado() );

        return asistenciaDiaria;
    }

    @Override
    public void updateEntityFromRequest(AsistenciaDiariaModificacionRequest request, AsistenciaDiaria asistenciaDiaria) {
        if ( request == null ) {
            return;
        }

        asistenciaDiaria.setId( request.id() );
        asistenciaDiaria.setFecha( request.fecha() );
        asistenciaDiaria.setEstado( request.estado() );
    }

    private Inscripcion asistenciaDiariaAsistenciaAlumnoMensualInscripcion(AsistenciaDiaria asistenciaDiaria) {
        AsistenciaAlumnoMensual asistenciaAlumnoMensual = asistenciaDiaria.getAsistenciaAlumnoMensual();
        if ( asistenciaAlumnoMensual == null ) {
            return null;
        }
        return asistenciaAlumnoMensual.getInscripcion();
    }

    protected AlumnoResponse inscripcionToAlumnoResponse(Inscripcion inscripcion) {
        if ( inscripcion == null ) {
            return null;
        }

        Long id = null;

        id = inscripcion.getId();

        String nombre = null;
        String apellido = null;
        LocalDate fechaNacimiento = null;
        LocalDate fechaIncorporacion = null;
        Integer edad = null;
        String celular1 = null;
        String celular2 = null;
        String email = null;
        String documento = null;
        LocalDate fechaDeBaja = null;
        Boolean deudaPendiente = null;
        String nombrePadres = null;
        Boolean autorizadoParaSalirSolo = null;
        Boolean activo = null;
        String otrasNotas = null;
        Double cuotaTotal = null;
        List<InscripcionResponse> inscripciones = null;
        Double creditoAcumulado = null;

        AlumnoResponse alumnoResponse = new AlumnoResponse( id, nombre, apellido, fechaNacimiento, fechaIncorporacion, edad, celular1, celular2, email, documento, fechaDeBaja, deudaPendiente, nombrePadres, autorizadoParaSalirSolo, activo, otrasNotas, cuotaTotal, inscripciones, creditoAcumulado );

        return alumnoResponse;
    }

    private Long asistenciaDiariaAsistenciaAlumnoMensualId(AsistenciaDiaria asistenciaDiaria) {
        AsistenciaAlumnoMensual asistenciaAlumnoMensual = asistenciaDiaria.getAsistenciaAlumnoMensual();
        if ( asistenciaAlumnoMensual == null ) {
            return null;
        }
        return asistenciaAlumnoMensual.getId();
    }

    private Long asistenciaDiariaAsistenciaAlumnoMensualAsistenciaMensualId(AsistenciaDiaria asistenciaDiaria) {
        AsistenciaAlumnoMensual asistenciaAlumnoMensual = asistenciaDiaria.getAsistenciaAlumnoMensual();
        if ( asistenciaAlumnoMensual == null ) {
            return null;
        }
        AsistenciaMensual asistenciaMensual = asistenciaAlumnoMensual.getAsistenciaMensual();
        if ( asistenciaMensual == null ) {
            return null;
        }
        return asistenciaMensual.getId();
    }

    private Long asistenciaDiariaAsistenciaAlumnoMensualAsistenciaMensualDisciplinaId(AsistenciaDiaria asistenciaDiaria) {
        AsistenciaAlumnoMensual asistenciaAlumnoMensual = asistenciaDiaria.getAsistenciaAlumnoMensual();
        if ( asistenciaAlumnoMensual == null ) {
            return null;
        }
        AsistenciaMensual asistenciaMensual = asistenciaAlumnoMensual.getAsistenciaMensual();
        if ( asistenciaMensual == null ) {
            return null;
        }
        Disciplina disciplina = asistenciaMensual.getDisciplina();
        if ( disciplina == null ) {
            return null;
        }
        return disciplina.getId();
    }
}

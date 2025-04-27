package ledance.dto.alumno;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import javax.annotation.processing.Generated;
import ledance.dto.alumno.request.AlumnoRegistroRequest;
import ledance.dto.alumno.response.AlumnoListadoResponse;
import ledance.dto.alumno.response.AlumnoResponse;
import ledance.dto.inscripcion.InscripcionMapper;
import ledance.dto.inscripcion.request.InscripcionRegistroRequest;
import ledance.dto.inscripcion.response.InscripcionResponse;
import ledance.entidades.Alumno;
import ledance.entidades.Inscripcion;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Generated(
    value = "org.mapstruct.ap.MappingProcessor",
    date = "2025-04-26T21:11:51-0300",
    comments = "version: 1.6.3, compiler: javac, environment: Java 17.0.14 (Amazon.com Inc.)"
)
@Component
public class AlumnoMapperImpl implements AlumnoMapper {

    @Autowired
    private InscripcionMapper inscripcionMapper;

    @Override
    public AlumnoResponse toResponse(Alumno alumno) {
        if ( alumno == null ) {
            return null;
        }

        Long id = null;
        String nombre = null;
        String apellido = null;
        LocalDate fechaNacimiento = null;
        String celular1 = null;
        Boolean activo = null;
        LocalDate fechaIncorporacion = null;
        Integer edad = null;
        String celular2 = null;
        String email = null;
        String documento = null;
        LocalDate fechaDeBaja = null;
        Boolean deudaPendiente = null;
        String nombrePadres = null;
        Boolean autorizadoParaSalirSolo = null;
        String otrasNotas = null;
        Double cuotaTotal = null;
        Double creditoAcumulado = null;

        id = alumno.getId();
        nombre = alumno.getNombre();
        apellido = alumno.getApellido();
        fechaNacimiento = alumno.getFechaNacimiento();
        celular1 = alumno.getCelular1();
        activo = alumno.getActivo();
        fechaIncorporacion = alumno.getFechaIncorporacion();
        edad = alumno.getEdad();
        celular2 = alumno.getCelular2();
        email = alumno.getEmail();
        documento = alumno.getDocumento();
        fechaDeBaja = alumno.getFechaDeBaja();
        deudaPendiente = alumno.getDeudaPendiente();
        nombrePadres = alumno.getNombrePadres();
        autorizadoParaSalirSolo = alumno.getAutorizadoParaSalirSolo();
        otrasNotas = alumno.getOtrasNotas();
        cuotaTotal = alumno.getCuotaTotal();
        creditoAcumulado = alumno.getCreditoAcumulado();

        List<InscripcionResponse> inscripciones = null;

        AlumnoResponse alumnoResponse = new AlumnoResponse( id, nombre, apellido, fechaNacimiento, fechaIncorporacion, edad, celular1, celular2, email, documento, fechaDeBaja, deudaPendiente, nombrePadres, autorizadoParaSalirSolo, activo, otrasNotas, cuotaTotal, inscripciones, creditoAcumulado );

        return alumnoResponse;
    }

    @Override
    public AlumnoResponse toSimpleResponse(Alumno alumno) {
        if ( alumno == null ) {
            return null;
        }

        Long id = null;
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
        Double creditoAcumulado = null;

        id = alumno.getId();
        nombre = alumno.getNombre();
        apellido = alumno.getApellido();
        fechaNacimiento = alumno.getFechaNacimiento();
        fechaIncorporacion = alumno.getFechaIncorporacion();
        edad = alumno.getEdad();
        celular1 = alumno.getCelular1();
        celular2 = alumno.getCelular2();
        email = alumno.getEmail();
        documento = alumno.getDocumento();
        fechaDeBaja = alumno.getFechaDeBaja();
        deudaPendiente = alumno.getDeudaPendiente();
        nombrePadres = alumno.getNombrePadres();
        autorizadoParaSalirSolo = alumno.getAutorizadoParaSalirSolo();
        activo = alumno.getActivo();
        otrasNotas = alumno.getOtrasNotas();
        cuotaTotal = alumno.getCuotaTotal();
        creditoAcumulado = alumno.getCreditoAcumulado();

        List<InscripcionResponse> inscripciones = null;

        AlumnoResponse alumnoResponse = new AlumnoResponse( id, nombre, apellido, fechaNacimiento, fechaIncorporacion, edad, celular1, celular2, email, documento, fechaDeBaja, deudaPendiente, nombrePadres, autorizadoParaSalirSolo, activo, otrasNotas, cuotaTotal, inscripciones, creditoAcumulado );

        return alumnoResponse;
    }

    @Override
    public Alumno toEntity(AlumnoRegistroRequest request) {
        if ( request == null ) {
            return null;
        }

        Alumno alumno = new Alumno();

        alumno.setId( request.id() );
        alumno.setNombre( request.nombre() );
        alumno.setApellido( request.apellido() );
        alumno.setFechaNacimiento( request.fechaNacimiento() );
        alumno.setEdad( request.edad() );
        alumno.setCelular1( request.celular1() );
        alumno.setCelular2( request.celular2() );
        alumno.setEmail( request.email() );
        alumno.setDocumento( request.documento() );
        alumno.setFechaIncorporacion( request.fechaIncorporacion() );
        alumno.setFechaDeBaja( request.fechaDeBaja() );
        alumno.setDeudaPendiente( request.deudaPendiente() );
        alumno.setNombrePadres( request.nombrePadres() );
        alumno.setAutorizadoParaSalirSolo( request.autorizadoParaSalirSolo() );
        alumno.setOtrasNotas( request.otrasNotas() );
        alumno.setCuotaTotal( request.cuotaTotal() );
        alumno.setInscripciones( inscripcionRegistroRequestListToInscripcionList( request.inscripciones() ) );

        alumno.setActivo( true );

        return alumno;
    }

    @Override
    public void updateEntityFromRequest(AlumnoRegistroRequest request, Alumno alumno) {
        if ( request == null ) {
            return;
        }

        alumno.setId( request.id() );
        alumno.setNombre( request.nombre() );
        alumno.setApellido( request.apellido() );
        alumno.setFechaNacimiento( request.fechaNacimiento() );
        alumno.setEdad( request.edad() );
        alumno.setCelular1( request.celular1() );
        alumno.setCelular2( request.celular2() );
        alumno.setEmail( request.email() );
        alumno.setDocumento( request.documento() );
        alumno.setFechaIncorporacion( request.fechaIncorporacion() );
        alumno.setFechaDeBaja( request.fechaDeBaja() );
        alumno.setDeudaPendiente( request.deudaPendiente() );
        alumno.setNombrePadres( request.nombrePadres() );
        alumno.setAutorizadoParaSalirSolo( request.autorizadoParaSalirSolo() );
        alumno.setOtrasNotas( request.otrasNotas() );
        alumno.setCuotaTotal( request.cuotaTotal() );
        alumno.setActivo( request.activo() );
    }

    @Override
    public AlumnoListadoResponse toAlumnoListadoResponse(Alumno alumno) {
        if ( alumno == null ) {
            return null;
        }

        Long id = null;
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
        Double creditoAcumulado = null;

        id = alumno.getId();
        nombre = alumno.getNombre();
        apellido = alumno.getApellido();
        fechaNacimiento = alumno.getFechaNacimiento();
        fechaIncorporacion = alumno.getFechaIncorporacion();
        edad = alumno.getEdad();
        celular1 = alumno.getCelular1();
        celular2 = alumno.getCelular2();
        email = alumno.getEmail();
        documento = alumno.getDocumento();
        fechaDeBaja = alumno.getFechaDeBaja();
        deudaPendiente = alumno.getDeudaPendiente();
        nombrePadres = alumno.getNombrePadres();
        autorizadoParaSalirSolo = alumno.getAutorizadoParaSalirSolo();
        activo = alumno.getActivo();
        otrasNotas = alumno.getOtrasNotas();
        cuotaTotal = alumno.getCuotaTotal();
        creditoAcumulado = alumno.getCreditoAcumulado();

        AlumnoListadoResponse alumnoListadoResponse = new AlumnoListadoResponse( id, nombre, apellido, fechaNacimiento, fechaIncorporacion, edad, celular1, celular2, email, documento, fechaDeBaja, deudaPendiente, nombrePadres, autorizadoParaSalirSolo, activo, otrasNotas, cuotaTotal, creditoAcumulado );

        return alumnoListadoResponse;
    }

    protected List<Inscripcion> inscripcionRegistroRequestListToInscripcionList(List<InscripcionRegistroRequest> list) {
        if ( list == null ) {
            return null;
        }

        List<Inscripcion> list1 = new ArrayList<Inscripcion>( list.size() );
        for ( InscripcionRegistroRequest inscripcionRegistroRequest : list ) {
            list1.add( inscripcionMapper.toEntity( inscripcionRegistroRequest ) );
        }

        return list1;
    }
}

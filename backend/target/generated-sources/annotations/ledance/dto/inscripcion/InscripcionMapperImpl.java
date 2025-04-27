package ledance.dto.inscripcion;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import javax.annotation.processing.Generated;
import ledance.dto.CommonMapper;
import ledance.dto.alumno.response.AlumnoListadoResponse;
import ledance.dto.bonificacion.response.BonificacionResponse;
import ledance.dto.disciplina.response.DisciplinaHorarioResponse;
import ledance.dto.disciplina.response.DisciplinaResponse;
import ledance.dto.inscripcion.request.InscripcionRegistroRequest;
import ledance.dto.inscripcion.response.InscripcionResponse;
import ledance.dto.recargo.response.RecargoResponse;
import ledance.entidades.Alumno;
import ledance.entidades.Bonificacion;
import ledance.entidades.DiaSemana;
import ledance.entidades.Disciplina;
import ledance.entidades.DisciplinaHorario;
import ledance.entidades.EstadoInscripcion;
import ledance.entidades.Inscripcion;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Generated(
    value = "org.mapstruct.ap.MappingProcessor",
    date = "2025-04-27T11:12:55-0300",
    comments = "version: 1.6.3, compiler: javac, environment: Java 17.0.14 (Amazon.com Inc.)"
)
@Component
public class InscripcionMapperImpl implements InscripcionMapper {

    @Autowired
    private CommonMapper commonMapper;

    @Override
    public InscripcionResponse toDTO(Inscripcion inscripcion) {
        if ( inscripcion == null ) {
            return null;
        }

        Long id = null;
        LocalDate fechaInscripcion = null;
        EstadoInscripcion estado = null;
        DisciplinaResponse disciplina = null;
        AlumnoListadoResponse alumno = null;
        BonificacionResponse bonificacion = null;

        id = inscripcion.getId();
        fechaInscripcion = inscripcion.getFechaInscripcion();
        estado = inscripcion.getEstado();
        disciplina = disciplinaToDisciplinaResponse( inscripcion.getDisciplina() );
        alumno = alumnoToAlumnoListadoResponse( inscripcion.getAlumno() );
        bonificacion = bonificacionToBonificacionResponse( inscripcion.getBonificacion() );

        Double costoCalculado = null;
        String mensualidadEstado = null;
        RecargoResponse recargo = null;

        InscripcionResponse inscripcionResponse = new InscripcionResponse( id, alumno, disciplina, fechaInscripcion, estado, costoCalculado, bonificacion, mensualidadEstado, recargo );

        return inscripcionResponse;
    }

    @Override
    public Inscripcion toEntity(InscripcionRegistroRequest request) {
        if ( request == null ) {
            return null;
        }

        Inscripcion inscripcion = new Inscripcion();

        inscripcion.setFechaInscripcion( request.fechaInscripcion() );
        inscripcion.setFechaBaja( request.fechaBaja() );

        inscripcion.setDisciplina( mapDisciplina(request.disciplina().id()) );
        inscripcion.setBonificacion( request.bonificacionId() != null ? mapBonificacion(request.bonificacionId()) : null );
        inscripcion.setEstado( EstadoInscripcion.ACTIVA );

        return inscripcion;
    }

    @Override
    public Inscripcion updateEntityFromRequest(InscripcionRegistroRequest request, Inscripcion inscripcion) {
        if ( request == null ) {
            return inscripcion;
        }

        inscripcion.setFechaBaja( request.fechaBaja() );
        inscripcion.setId( request.id() );
        inscripcion.setFechaInscripcion( request.fechaInscripcion() );

        inscripcion.setDisciplina( mapDisciplina(request.disciplina().id()) );
        inscripcion.setBonificacion( request.bonificacionId() != null ? mapBonificacion(request.bonificacionId()) : null );

        return inscripcion;
    }

    protected DisciplinaHorarioResponse disciplinaHorarioToDisciplinaHorarioResponse(DisciplinaHorario disciplinaHorario) {
        if ( disciplinaHorario == null ) {
            return null;
        }

        Long id = null;
        DiaSemana diaSemana = null;
        LocalTime horarioInicio = null;
        Double duracion = null;

        id = disciplinaHorario.getId();
        diaSemana = disciplinaHorario.getDiaSemana();
        horarioInicio = disciplinaHorario.getHorarioInicio();
        duracion = disciplinaHorario.getDuracion();

        DisciplinaHorarioResponse disciplinaHorarioResponse = new DisciplinaHorarioResponse( id, diaSemana, horarioInicio, duracion );

        return disciplinaHorarioResponse;
    }

    protected List<DisciplinaHorarioResponse> disciplinaHorarioListToDisciplinaHorarioResponseList(List<DisciplinaHorario> list) {
        if ( list == null ) {
            return null;
        }

        List<DisciplinaHorarioResponse> list1 = new ArrayList<DisciplinaHorarioResponse>( list.size() );
        for ( DisciplinaHorario disciplinaHorario : list ) {
            list1.add( disciplinaHorarioToDisciplinaHorarioResponse( disciplinaHorario ) );
        }

        return list1;
    }

    protected DisciplinaResponse disciplinaToDisciplinaResponse(Disciplina disciplina) {
        if ( disciplina == null ) {
            return null;
        }

        Long id = null;
        String nombre = null;
        String salon = null;
        Double valorCuota = null;
        Boolean activo = null;
        Double claseSuelta = null;
        Double clasePrueba = null;
        List<DisciplinaHorarioResponse> horarios = null;

        id = disciplina.getId();
        nombre = disciplina.getNombre();
        salon = commonMapper.mapSalonToString( disciplina.getSalon() );
        valorCuota = disciplina.getValorCuota();
        activo = disciplina.getActivo();
        claseSuelta = disciplina.getClaseSuelta();
        clasePrueba = disciplina.getClasePrueba();
        horarios = disciplinaHorarioListToDisciplinaHorarioResponseList( disciplina.getHorarios() );

        Long salonId = null;
        Double matricula = null;
        String profesorNombre = null;
        String profesorApellido = null;
        Long profesorId = null;
        Integer inscritos = null;

        DisciplinaResponse disciplinaResponse = new DisciplinaResponse( id, nombre, salon, salonId, valorCuota, matricula, profesorNombre, profesorApellido, profesorId, inscritos, activo, claseSuelta, clasePrueba, horarios );

        return disciplinaResponse;
    }

    protected AlumnoListadoResponse alumnoToAlumnoListadoResponse(Alumno alumno) {
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

    protected BonificacionResponse bonificacionToBonificacionResponse(Bonificacion bonificacion) {
        if ( bonificacion == null ) {
            return null;
        }

        Long id = null;
        String descripcion = null;
        Integer porcentajeDescuento = null;
        Boolean activo = null;
        String observaciones = null;
        Double valorFijo = null;

        id = bonificacion.getId();
        descripcion = bonificacion.getDescripcion();
        porcentajeDescuento = bonificacion.getPorcentajeDescuento();
        activo = bonificacion.getActivo();
        observaciones = bonificacion.getObservaciones();
        valorFijo = bonificacion.getValorFijo();

        BonificacionResponse bonificacionResponse = new BonificacionResponse( id, descripcion, porcentajeDescuento, activo, observaciones, valorFijo );

        return bonificacionResponse;
    }
}

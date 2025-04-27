package ledance.dto.asistencia;

import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import javax.annotation.processing.Generated;
import ledance.dto.alumno.AlumnoMapper;
import ledance.dto.alumno.response.AlumnoResponse;
import ledance.dto.asistencia.request.AsistenciaMensualModificacionRequest;
import ledance.dto.asistencia.request.AsistenciaMensualRegistroRequest;
import ledance.dto.asistencia.response.AsistenciaAlumnoMensualDetalleResponse;
import ledance.dto.asistencia.response.AsistenciaDiariaDetalleResponse;
import ledance.dto.asistencia.response.AsistenciaMensualDetalleResponse;
import ledance.dto.asistencia.response.AsistenciaMensualListadoResponse;
import ledance.dto.disciplina.response.DisciplinaHorarioResponse;
import ledance.dto.disciplina.response.DisciplinaResponse;
import ledance.entidades.Alumno;
import ledance.entidades.AsistenciaAlumnoMensual;
import ledance.entidades.AsistenciaDiaria;
import ledance.entidades.AsistenciaMensual;
import ledance.entidades.DiaSemana;
import ledance.entidades.Disciplina;
import ledance.entidades.DisciplinaHorario;
import ledance.entidades.Inscripcion;
import ledance.entidades.Profesor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Generated(
    value = "org.mapstruct.ap.MappingProcessor",
    date = "2025-04-27T11:12:54-0300",
    comments = "version: 1.6.3, compiler: javac, environment: Java 17.0.14 (Amazon.com Inc.)"
)
@Component
public class AsistenciaMensualMapperImpl implements AsistenciaMensualMapper {

    @Autowired
    private AsistenciaDiariaMapper asistenciaDiariaMapper;
    @Autowired
    private AlumnoMapper alumnoMapper;

    @Override
    public AsistenciaMensualDetalleResponse toDetalleDTO(AsistenciaMensual asistenciaMensual) {
        if ( asistenciaMensual == null ) {
            return null;
        }

        DisciplinaResponse disciplina = null;
        String profesor = null;
        List<AsistenciaAlumnoMensualDetalleResponse> alumnos = null;
        Long id = null;
        Integer mes = null;
        Integer anio = null;

        disciplina = disciplinaToDisciplinaResponse( asistenciaMensual.getDisciplina() );
        profesor = asistenciaMensualDisciplinaProfesorNombre( asistenciaMensual );
        alumnos = toAlumnoDetalleDTOList( asistenciaMensual.getAsistenciasAlumnoMensual() );
        id = asistenciaMensual.getId();
        mes = asistenciaMensual.getMes();
        anio = asistenciaMensual.getAnio();

        AsistenciaMensualDetalleResponse asistenciaMensualDetalleResponse = new AsistenciaMensualDetalleResponse( id, mes, anio, disciplina, profesor, alumnos );

        return asistenciaMensualDetalleResponse;
    }

    @Override
    public AsistenciaMensualListadoResponse toListadoDTO(AsistenciaMensual asistenciaMensual) {
        if ( asistenciaMensual == null ) {
            return null;
        }

        DisciplinaResponse disciplina = null;
        String profesor = null;
        Integer mes = null;
        Integer anio = null;
        Long id = null;

        disciplina = disciplinaToDisciplinaResponse( asistenciaMensual.getDisciplina() );
        profesor = asistenciaMensualDisciplinaProfesorNombre( asistenciaMensual );
        mes = asistenciaMensual.getMes();
        anio = asistenciaMensual.getAnio();
        id = asistenciaMensual.getId();

        Integer cantidadAlumnos = null;

        AsistenciaMensualListadoResponse asistenciaMensualListadoResponse = new AsistenciaMensualListadoResponse( id, mes, anio, disciplina, profesor, cantidadAlumnos );

        return asistenciaMensualListadoResponse;
    }

    @Override
    public AsistenciaMensual toEntity(AsistenciaMensualRegistroRequest request) {
        if ( request == null ) {
            return null;
        }

        AsistenciaMensual asistenciaMensual = new AsistenciaMensual();

        asistenciaMensual.setMes( request.mes() );
        asistenciaMensual.setAnio( request.anio() );

        return asistenciaMensual;
    }

    @Override
    public void updateEntityFromRequest(AsistenciaMensualModificacionRequest request, AsistenciaMensual asistenciaMensual) {
        if ( request == null ) {
            return;
        }
    }

    @Override
    public List<AsistenciaAlumnoMensualDetalleResponse> toAlumnoDetalleDTOList(List<AsistenciaAlumnoMensual> alumnos) {
        if ( alumnos == null ) {
            return null;
        }

        List<AsistenciaAlumnoMensualDetalleResponse> list = new ArrayList<AsistenciaAlumnoMensualDetalleResponse>( alumnos.size() );
        for ( AsistenciaAlumnoMensual asistenciaAlumnoMensual : alumnos ) {
            list.add( toAlumnoDetalleDTO( asistenciaAlumnoMensual ) );
        }

        return list;
    }

    @Override
    public AsistenciaAlumnoMensualDetalleResponse toAlumnoDetalleDTO(AsistenciaAlumnoMensual alumno) {
        if ( alumno == null ) {
            return null;
        }

        Long inscripcionId = null;
        String observacion = null;
        Long asistenciaMensualId = null;
        List<AsistenciaDiariaDetalleResponse> asistenciasDiarias = null;
        AlumnoResponse alumno1 = null;
        Long id = null;

        inscripcionId = alumnoInscripcionId( alumno );
        observacion = alumno.getObservacion();
        asistenciaMensualId = alumnoAsistenciaMensualId( alumno );
        asistenciasDiarias = asistenciaDiariaListToAsistenciaDiariaDetalleResponseList( alumno.getAsistenciasDiarias() );
        alumno1 = alumnoMapper.toResponse( alumnoInscripcionAlumno( alumno ) );
        id = alumno.getId();

        AsistenciaAlumnoMensualDetalleResponse asistenciaAlumnoMensualDetalleResponse = new AsistenciaAlumnoMensualDetalleResponse( id, inscripcionId, alumno1, observacion, asistenciaMensualId, asistenciasDiarias );

        return asistenciaAlumnoMensualDetalleResponse;
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
        salon = map( disciplina.getSalon() );
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

    private String asistenciaMensualDisciplinaProfesorNombre(AsistenciaMensual asistenciaMensual) {
        Disciplina disciplina = asistenciaMensual.getDisciplina();
        if ( disciplina == null ) {
            return null;
        }
        Profesor profesor = disciplina.getProfesor();
        if ( profesor == null ) {
            return null;
        }
        return profesor.getNombre();
    }

    private Long alumnoInscripcionId(AsistenciaAlumnoMensual asistenciaAlumnoMensual) {
        Inscripcion inscripcion = asistenciaAlumnoMensual.getInscripcion();
        if ( inscripcion == null ) {
            return null;
        }
        return inscripcion.getId();
    }

    private Long alumnoAsistenciaMensualId(AsistenciaAlumnoMensual asistenciaAlumnoMensual) {
        AsistenciaMensual asistenciaMensual = asistenciaAlumnoMensual.getAsistenciaMensual();
        if ( asistenciaMensual == null ) {
            return null;
        }
        return asistenciaMensual.getId();
    }

    protected List<AsistenciaDiariaDetalleResponse> asistenciaDiariaListToAsistenciaDiariaDetalleResponseList(List<AsistenciaDiaria> list) {
        if ( list == null ) {
            return null;
        }

        List<AsistenciaDiariaDetalleResponse> list1 = new ArrayList<AsistenciaDiariaDetalleResponse>( list.size() );
        for ( AsistenciaDiaria asistenciaDiaria : list ) {
            list1.add( asistenciaDiariaMapper.toDTO( asistenciaDiaria ) );
        }

        return list1;
    }

    private Alumno alumnoInscripcionAlumno(AsistenciaAlumnoMensual asistenciaAlumnoMensual) {
        Inscripcion inscripcion = asistenciaAlumnoMensual.getInscripcion();
        if ( inscripcion == null ) {
            return null;
        }
        return inscripcion.getAlumno();
    }
}

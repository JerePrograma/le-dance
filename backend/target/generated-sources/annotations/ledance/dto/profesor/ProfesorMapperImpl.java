package ledance.dto.profesor;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import javax.annotation.processing.Generated;
import ledance.dto.disciplina.response.DisciplinaHorarioResponse;
import ledance.dto.disciplina.response.DisciplinaResponse;
import ledance.dto.profesor.request.ProfesorModificacionRequest;
import ledance.dto.profesor.request.ProfesorRegistroRequest;
import ledance.dto.profesor.response.ProfesorResponse;
import ledance.entidades.DiaSemana;
import ledance.entidades.Disciplina;
import ledance.entidades.DisciplinaHorario;
import ledance.entidades.Profesor;
import org.springframework.stereotype.Component;

@Generated(
    value = "org.mapstruct.ap.MappingProcessor",
    date = "2025-04-26T21:11:51-0300",
    comments = "version: 1.6.3, compiler: javac, environment: Java 17.0.14 (Amazon.com Inc.)"
)
@Component
public class ProfesorMapperImpl implements ProfesorMapper {

    @Override
    public Profesor toEntity(ProfesorRegistroRequest request) {
        if ( request == null ) {
            return null;
        }

        Profesor profesor = new Profesor();

        profesor.setFechaNacimiento( request.fechaNacimiento() );
        profesor.setTelefono( request.telefono() );
        profesor.setNombre( request.nombre() );
        profesor.setApellido( request.apellido() );

        profesor.setActivo( true );

        return profesor;
    }

    @Override
    public void updateEntityFromRequest(ProfesorModificacionRequest request, Profesor profesor) {
        if ( request == null ) {
            return;
        }

        profesor.setFechaNacimiento( request.fechaNacimiento() );
        profesor.setTelefono( request.telefono() );
        profesor.setNombre( request.nombre() );
        profesor.setApellido( request.apellido() );
        profesor.setActivo( request.activo() );
    }

    @Override
    public ProfesorResponse toResponse(Profesor profesor) {
        if ( profesor == null ) {
            return null;
        }

        Long id = null;
        String nombre = null;
        String apellido = null;
        LocalDate fechaNacimiento = null;
        Integer edad = null;
        String telefono = null;
        Boolean activo = null;
        List<DisciplinaResponse> disciplinas = null;

        id = profesor.getId();
        nombre = profesor.getNombre();
        apellido = profesor.getApellido();
        fechaNacimiento = profesor.getFechaNacimiento();
        edad = profesor.getEdad();
        telefono = profesor.getTelefono();
        activo = profesor.getActivo();
        disciplinas = disciplinaListToDisciplinaResponseList( profesor.getDisciplinas() );

        ProfesorResponse profesorResponse = new ProfesorResponse( id, nombre, apellido, fechaNacimiento, edad, telefono, activo, disciplinas );

        return profesorResponse;
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
        salon = mapSalonToString( disciplina.getSalon() );
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

    protected List<DisciplinaResponse> disciplinaListToDisciplinaResponseList(List<Disciplina> list) {
        if ( list == null ) {
            return null;
        }

        List<DisciplinaResponse> list1 = new ArrayList<DisciplinaResponse>( list.size() );
        for ( Disciplina disciplina : list ) {
            list1.add( disciplinaToDisciplinaResponse( disciplina ) );
        }

        return list1;
    }
}

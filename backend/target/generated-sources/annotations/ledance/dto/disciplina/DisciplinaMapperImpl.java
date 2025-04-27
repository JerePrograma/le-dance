package ledance.dto.disciplina;

import java.util.ArrayList;
import java.util.List;
import javax.annotation.processing.Generated;
import ledance.dto.disciplina.request.DisciplinaHorarioRequest;
import ledance.dto.disciplina.request.DisciplinaModificacionRequest;
import ledance.dto.disciplina.request.DisciplinaRegistroRequest;
import ledance.dto.disciplina.response.DisciplinaHorarioResponse;
import ledance.dto.disciplina.response.DisciplinaResponse;
import ledance.entidades.Disciplina;
import ledance.entidades.DisciplinaHorario;
import ledance.entidades.Profesor;
import ledance.entidades.Salon;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Generated(
    value = "org.mapstruct.ap.MappingProcessor",
    date = "2025-04-27T11:12:55-0300",
    comments = "version: 1.6.3, compiler: javac, environment: Java 17.0.14 (Amazon.com Inc.)"
)
@Component
public class DisciplinaMapperImpl implements DisciplinaMapper {

    @Autowired
    private DisciplinaHorarioMapper disciplinaHorarioMapper;

    @Override
    public DisciplinaResponse toResponse(Disciplina disciplina) {
        if ( disciplina == null ) {
            return null;
        }

        Long id = null;
        String nombre = null;
        String salon = null;
        Long salonId = null;
        Double valorCuota = null;
        String profesorNombre = null;
        String profesorApellido = null;
        Long profesorId = null;
        Boolean activo = null;
        List<DisciplinaHorarioResponse> horarios = null;
        Double claseSuelta = null;
        Double clasePrueba = null;

        id = disciplina.getId();
        nombre = disciplina.getNombre();
        salon = disciplinaSalonNombre( disciplina );
        salonId = disciplinaSalonId( disciplina );
        valorCuota = disciplina.getValorCuota();
        profesorNombre = disciplinaProfesorNombre( disciplina );
        profesorApellido = disciplinaProfesorApellido( disciplina );
        profesorId = disciplinaProfesorId( disciplina );
        activo = disciplina.getActivo();
        horarios = disciplinaHorarioMapper.toResponseList( disciplina.getHorarios() );
        claseSuelta = disciplina.getClaseSuelta();
        clasePrueba = disciplina.getClasePrueba();

        Integer inscritos = disciplina.getInscripciones() != null ? disciplina.getInscripciones().size() : 0;
        Double matricula = null;

        DisciplinaResponse disciplinaResponse = new DisciplinaResponse( id, nombre, salon, salonId, valorCuota, matricula, profesorNombre, profesorApellido, profesorId, inscritos, activo, claseSuelta, clasePrueba, horarios );

        return disciplinaResponse;
    }

    @Override
    public Disciplina toEntity(DisciplinaRegistroRequest request) {
        if ( request == null ) {
            return null;
        }

        Disciplina disciplina = new Disciplina();

        disciplina.setSalon( disciplinaRegistroRequestToSalon( request ) );
        disciplina.setId( request.id() );
        disciplina.setNombre( request.nombre() );
        disciplina.setValorCuota( request.valorCuota() );
        disciplina.setClaseSuelta( request.claseSuelta() );
        disciplina.setClasePrueba( request.clasePrueba() );
        disciplina.setHorarios( disciplinaHorarioRequestListToDisciplinaHorarioList( request.horarios() ) );

        return disciplina;
    }

    @Override
    public void updateEntityFromRequest(DisciplinaModificacionRequest request, Disciplina disciplina) {
        if ( request == null ) {
            return;
        }

        disciplina.setNombre( request.nombre() );
        disciplina.setValorCuota( request.valorCuota() );
        disciplina.setClaseSuelta( request.claseSuelta() );
        disciplina.setClasePrueba( request.clasePrueba() );
        disciplina.setActivo( request.activo() );
    }

    private String disciplinaSalonNombre(Disciplina disciplina) {
        Salon salon = disciplina.getSalon();
        if ( salon == null ) {
            return null;
        }
        return salon.getNombre();
    }

    private Long disciplinaSalonId(Disciplina disciplina) {
        Salon salon = disciplina.getSalon();
        if ( salon == null ) {
            return null;
        }
        return salon.getId();
    }

    private String disciplinaProfesorNombre(Disciplina disciplina) {
        Profesor profesor = disciplina.getProfesor();
        if ( profesor == null ) {
            return null;
        }
        return profesor.getNombre();
    }

    private String disciplinaProfesorApellido(Disciplina disciplina) {
        Profesor profesor = disciplina.getProfesor();
        if ( profesor == null ) {
            return null;
        }
        return profesor.getApellido();
    }

    private Long disciplinaProfesorId(Disciplina disciplina) {
        Profesor profesor = disciplina.getProfesor();
        if ( profesor == null ) {
            return null;
        }
        return profesor.getId();
    }

    protected Salon disciplinaRegistroRequestToSalon(DisciplinaRegistroRequest disciplinaRegistroRequest) {
        if ( disciplinaRegistroRequest == null ) {
            return null;
        }

        Salon salon = new Salon();

        salon.setId( disciplinaRegistroRequest.salonId() );

        return salon;
    }

    protected List<DisciplinaHorario> disciplinaHorarioRequestListToDisciplinaHorarioList(List<DisciplinaHorarioRequest> list) {
        if ( list == null ) {
            return null;
        }

        List<DisciplinaHorario> list1 = new ArrayList<DisciplinaHorario>( list.size() );
        for ( DisciplinaHorarioRequest disciplinaHorarioRequest : list ) {
            list1.add( disciplinaHorarioMapper.toEntity( disciplinaHorarioRequest ) );
        }

        return list1;
    }
}

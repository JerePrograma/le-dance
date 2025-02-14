package ledance.dto.inscripcion;

import ledance.dto.inscripcion.request.InscripcionRegistroRequest;
import ledance.dto.inscripcion.request.InscripcionModificacionRequest;
import ledance.dto.inscripcion.response.InscripcionResponse;
import ledance.entidades.Alumno;
import ledance.entidades.Disciplina;
import ledance.entidades.Inscripcion;
import org.mapstruct.*;

@Mapper(componentModel = "spring")
public interface InscripcionMapper {

    @Mapping(target = "id", source = "id")
    @Mapping(target = "alumno", source = "alumno", qualifiedByName = "mapAlumnoResumen")
    @Mapping(target = "disciplina", source = "disciplina", qualifiedByName = "mapDisciplinaResumen")
    @Mapping(target = "fechaInscripcion", source = "fechaInscripcion")
    @Mapping(target = "estado", source = "estado")
    @Mapping(target = "notas", source = "notas")
    // Calcula el costo utilizando el método auxiliar
    @Mapping(target = "costoCalculado", expression = "java(mapCostoCalculado(inscripcion))")
    InscripcionResponse toDTO(Inscripcion inscripcion);

    @Named("mapAlumnoResumen")
    default InscripcionResponse.AlumnoResumen mapAlumnoResumen(Alumno alumno) {
        return new InscripcionResponse.AlumnoResumen(alumno.getId(), alumno.getNombre(), alumno.getApellido());
    }

    @Named("mapDisciplinaResumen")
    default InscripcionResponse.DisciplinaResumen mapDisciplinaResumen(Disciplina disciplina) {
        return new InscripcionResponse.DisciplinaResumen(disciplina.getId(), disciplina.getNombre(), disciplina.getProfesor().getNombre());
    }

    // Método auxiliar para calcular el costo
    default Double mapCostoCalculado(Inscripcion inscripcion) {
        Disciplina d = inscripcion.getDisciplina();
        double valorCuota = (d.getValorCuota() != null ? d.getValorCuota() : 0.0);
        double matricula = (d.getMatricula() != null ? d.getMatricula() : 0.0);
        double claseSuelta = (d.getClaseSuelta() != null ? d.getClaseSuelta() : 0.0);
        double clasePrueba = (d.getClasePrueba() != null ? d.getClasePrueba() : 0.0);
        double total = valorCuota + matricula + claseSuelta + clasePrueba;
        if (inscripcion.getBonificacion() != null) {
            int descuento = inscripcion.getBonificacion().getPorcentajeDescuento();
            total = total * (100 - descuento) / 100.0;
        }
        return total;
    }

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "alumno", ignore = true)
    @Mapping(target = "disciplina", ignore = true)
    @Mapping(target = "bonificacion", ignore = true)
    @Mapping(target = "estado", constant = "ACTIVA")
    Inscripcion toEntity(InscripcionRegistroRequest request);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "alumno", ignore = true)
    @Mapping(target = "disciplina", ignore = true)
    @Mapping(target = "bonificacion", ignore = true)
    void updateEntityFromRequest(InscripcionModificacionRequest request, @MappingTarget Inscripcion inscripcion);
}
package ledance.dto.inscripcion;

import ledance.dto.inscripcion.request.InscripcionRegistroRequest;
import ledance.dto.inscripcion.request.InscripcionModificacionRequest;
import ledance.dto.inscripcion.response.InscripcionResponse;
import ledance.entidades.Alumno;
import ledance.entidades.Bonificacion;
import ledance.entidades.Disciplina;
import ledance.entidades.Inscripcion;
import org.mapstruct.*;

@Mapper(componentModel = "spring")
public interface InscripcionMapper {

    @Mapping(target = "id", source = "id")
    @Mapping(source = "alumno", target = "alumno")
    @Mapping(source = "disciplina", target = "disciplina")
    @Mapping(target = "fechaInscripcion", source = "fechaInscripcion")
    @Mapping(target = "estado", source = "estado")
    // Calcula el costo utilizando el metodo auxiliar
    @Mapping(target = "costoCalculado", expression = "java(mapCostoCalculado(inscripcion))")
    InscripcionResponse toDTO(Inscripcion inscripcion);

    // Metodo auxiliar para calcular el costo
    default Double mapCostoCalculado(Inscripcion inscripcion) {
        Disciplina d = inscripcion.getDisciplina();
        double valorCuota = d.getValorCuota() != null ? d.getValorCuota() : 0.0;
        double claseSuelta = d.getClaseSuelta() != null ? d.getClaseSuelta() : 0.0;
        double clasePrueba = d.getClasePrueba() != null ? d.getClasePrueba() : 0.0;
        double total = valorCuota + claseSuelta + clasePrueba;
        if (inscripcion.getBonificacion() != null && inscripcion.getBonificacion().getPorcentajeDescuento() != null) {
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

    @Mapping(target = "alumno", ignore = true)
    @Mapping(target = "disciplina", expression = "java(mapDisciplina(request.inscripcion().disciplinaId()))")
    @Mapping(target = "bonificacion", expression = "java(request.inscripcion().bonificacionId() != null ? mapBonificacion(request.inscripcion().bonificacionId()) : null)")
    @Mapping(target = "fechaBaja", source = "fechaBaja")
    Inscripcion updateEntityFromRequest(InscripcionModificacionRequest request, @MappingTarget Inscripcion inscripcion);

    default Disciplina mapDisciplina(Long disciplinaId) {
        Disciplina d = new Disciplina(); // Se usa el constructor vacío
        d.setId(disciplinaId);
        return d;
    }

    default Bonificacion mapBonificacion(Long bonificacionId) {
        Bonificacion b = new Bonificacion();
        b.setId(bonificacionId);
        return b;
    }


}
package ledance.dto.mensualidad;

import ledance.dto.mensualidad.request.MensualidadRegistroRequest;
import ledance.dto.mensualidad.response.MensualidadResponse;
import ledance.entidades.Mensualidad;
import org.mapstruct.*;

@Mapper(componentModel = "spring")
public interface MensualidadMapper {

    // Mapea explicitamente el campo importeInicial
    @Mapping(target = "recargoId", source = "recargo.id")
    @Mapping(target = "bonificacion", source = "bonificacion")
    @Mapping(target = "inscripcionId", source = "inscripcion.id")
    @Mapping(target = "importeInicial", source = "importeInicial")  // <-- Agregado explícitamente
    MensualidadResponse toDTO(Mensualidad mensualidad);

    @Mapping(target = "id", ignore = true)
    Mensualidad toEntity(MensualidadRegistroRequest dto);

    @Mapping(target = "id", ignore = true)
    void updateEntityFromRequest(MensualidadRegistroRequest dto, @MappingTarget Mensualidad mensualidad);
}

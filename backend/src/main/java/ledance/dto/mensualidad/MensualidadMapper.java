package ledance.dto.mensualidad;

import ledance.dto.mensualidad.request.MensualidadRegistroRequest;
import ledance.dto.mensualidad.request.MensualidadModificacionRequest;
import ledance.dto.mensualidad.response.MensualidadResponse;
import ledance.entidades.Mensualidad;
import org.mapstruct.*;

@Mapper(componentModel = "spring")
public interface MensualidadMapper {

    // Aquí se mapea el DTO a entidad sin resolver IDs
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "totalPagar", ignore = true) // Se calcula en el servicio
    Mensualidad toEntity(MensualidadRegistroRequest dto);

    @Mapping(target = "recargoId", source = "recargo.id")
    @Mapping(target = "bonificacionId", source = "bonificacion.id")
    @Mapping(target = "inscripcionId", source = "inscripcion.id")
    MensualidadResponse toDTO(Mensualidad mensualidad);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "totalPagar", ignore = true)
    void updateEntityFromRequest(MensualidadModificacionRequest dto, @MappingTarget Mensualidad mensualidad);
}

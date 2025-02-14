package ledance.dto.bonificacion;

import ledance.dto.bonificacion.request.BonificacionRegistroRequest;
import ledance.dto.bonificacion.request.BonificacionModificacionRequest;
import ledance.dto.bonificacion.response.BonificacionResponse;
import ledance.entidades.Bonificacion;
import org.mapstruct.*;

@Mapper(componentModel = "spring")
public interface BonificacionMapper {

    /**
     * ✅ Mapea una bonificación de registro a la entidad.
     * - "activo" se asigna automáticamente como `true`.
     */
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "activo", constant = "true") // ✅ Se asigna automáticamente en true
    Bonificacion toEntity(BonificacionRegistroRequest request);

    /**
     * ✅ Mapea una bonificación a su DTO de respuesta.
     */
    @Mapping(target = "id", source = "id")
    BonificacionResponse toDTO(Bonificacion bonificacion);

    /**
     * ✅ Actualiza una bonificación existente con nuevos datos.
     */
    @Mapping(target = "id", ignore = true)
    void updateEntityFromRequest(BonificacionModificacionRequest request, @MappingTarget Bonificacion bonificacion);
}

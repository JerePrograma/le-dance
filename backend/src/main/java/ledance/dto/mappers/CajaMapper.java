package ledance.dto.mappers;

import ledance.dto.request.CajaRegistroRequest;
import ledance.dto.request.CajaModificacionRequest;
import ledance.dto.response.CajaResponse;
import ledance.entidades.Caja;
import org.mapstruct.*;

@Mapper(componentModel = "spring")
public interface CajaMapper {

    /**
     * ✅ Mapea un registro de caja a la entidad.
     * - "activo" se asigna automáticamente como `true`.
     */
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "activo", constant = "true") // ✅ Se asigna automáticamente en true
    Caja toEntity(CajaRegistroRequest request);

    /**
     * ✅ Mapea una entidad de caja a su DTO de respuesta.
     */
    @Mapping(target = "id", source = "id")
    CajaResponse toDTO(Caja caja);

    /**
     * ✅ Actualiza un movimiento de caja existente con nuevos datos.
     */
    @Mapping(target = "id", ignore = true)
    void updateEntityFromRequest(CajaModificacionRequest request, @MappingTarget Caja caja);
}

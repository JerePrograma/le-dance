package ledance.dto.caja;

import ledance.dto.caja.request.CajaRegistroRequest;
import ledance.dto.caja.request.CajaModificacionRequest;
import ledance.dto.caja.response.CajaResponse;
import ledance.entidades.Caja;
import org.mapstruct.*;

@Mapper(componentModel = "spring")
public interface CajaMapper {

    /**
     * Mapea un registro de caja a la entidad.
     * Se ignora el ID y se asigna "activo" como true por defecto.
     */
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "activo", constant = "true")
    Caja toEntity(CajaRegistroRequest request);

    /**
     * Mapea una entidad de caja a su DTO de respuesta.
     */
    @Mapping(target = "id", source = "id")
    CajaResponse toDTO(Caja caja);

    /**
     * Actualiza un movimiento de caja existente con nuevos datos.
     */
    @Mapping(target = "id", ignore = true)
    void updateEntityFromRequest(CajaModificacionRequest request, @MappingTarget Caja caja);
}

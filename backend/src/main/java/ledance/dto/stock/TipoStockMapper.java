package ledance.dto.stock;

import ledance.dto.stock.request.TipoStockRegistroRequest;
import ledance.dto.stock.request.TipoStockModificacionRequest;
import ledance.dto.stock.response.TipoStockResponse;
import ledance.entidades.TipoStock;
import org.mapstruct.*;

@Mapper(componentModel = "spring")
public interface TipoStockMapper {

    /**
     * Convierte un TipoStockRegistroRequest en una entidad TipoStock.
     * Se ignora el id y se asigna 'activo' como true por defecto.
     */
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "activo", constant = "true")
    TipoStock toEntity(TipoStockRegistroRequest request);

    /**
     * Convierte un TipoStock en TipoStockResponse.
     */
    @Mapping(target = "id", source = "id")
    TipoStockResponse toDTO(TipoStock tipoStock);

    /**
     * Actualiza una entidad TipoStock con los datos del TipoStockModificacionRequest.
     * Se ignora el id.
     */
    @Mapping(target = "id", ignore = true)
    void updateEntityFromRequest(TipoStockModificacionRequest request, @MappingTarget TipoStock tipoStock);
}

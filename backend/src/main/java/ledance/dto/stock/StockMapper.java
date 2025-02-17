package ledance.dto.stock;

import ledance.dto.stock.request.StockRegistroRequest;
import ledance.dto.stock.request.StockModificacionRequest;
import ledance.dto.stock.response.StockResponse;
import ledance.entidades.Stock;
import org.mapstruct.*;

@Mapper(componentModel = "spring", uses = {ledance.dto.stock.TipoStockMapper.class})
public interface StockMapper {

    /**
     * Convierte un Stock en StockResponse.
     * Nota: Aquí se mapea el campo 'tipo' para que en la respuesta se muestre la descripción.
     */
    @Mapping(target = "tipo", source = "tipo")
    StockResponse toDTO(Stock stock);

    /**
     * Convierte un StockRegistroRequest en una entidad Stock.
     * Se ignora el id y se asigna 'activo' como true.
     * El campo 'tipo' se asignará en el servicio.
     */
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "activo", constant = "true")
    @Mapping(target = "tipo", ignore = true)
    Stock toEntity(StockRegistroRequest request);

    /**
     * Actualiza un stock existente con los datos del StockModificacionRequest.
     * Se ignora el id y el campo 'tipo' (que se asigna en el servicio).
     */
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "tipo", ignore = true)
    void updateEntityFromRequest(StockModificacionRequest request, @MappingTarget Stock stock);
}

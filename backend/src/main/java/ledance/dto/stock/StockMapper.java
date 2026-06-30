package ledance.dto.stock;

import ledance.dto.stock.request.StockRegistroRequest;
import ledance.entidades.Stock;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;

@Mapper(componentModel = "spring")
public interface StockMapper {
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "activo", constant = "true")
    @Mapping(target = "version", ignore = true)
    Stock toEntity(StockRegistroRequest request);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "stock", ignore = true)
    @Mapping(target = "version", ignore = true)
    void updateEntityFromRequest(StockRegistroRequest request, @MappingTarget Stock stock);
}

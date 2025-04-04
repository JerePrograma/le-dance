package ledance.dto.stock;

import ledance.dto.stock.request.StockRegistroRequest;
import ledance.dto.stock.response.StockResponse;
import ledance.entidades.Stock;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;

@Mapper(componentModel = "spring")
public interface StockMapper {

    StockResponse toDTO(Stock stock);

    // Mapea el registro: se ignoran campos que se asignan en el servicio y se convierte el tipoEgreso
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "activo", constant = "true")
    Stock toEntity(StockRegistroRequest request);

    @Mapping(target = "id", ignore = true)
    void updateEntityFromRequest(StockRegistroRequest request, @MappingTarget Stock stock);
}

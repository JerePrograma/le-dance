package ledance.dto.stock;

import ledance.dto.stock.request.StockRegistroRequest;
import ledance.dto.stock.request.StockModificacionRequest;
import ledance.dto.stock.response.StockResponse;
import ledance.entidades.Stock;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;

@Mapper(componentModel = "spring", uses = {ledance.dto.stock.TipoStockMapper.class})
public interface StockMapper {

    // Mapea de entidad Stock a StockResponse, convirtiendo el enum a String
    @Mapping(target = "tipo", source = "tipo")
    StockResponse toDTO(Stock stock);

    // Mapea el registro: se ignoran campos que se asignan en el servicio y se convierte el tipoEgreso
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "activo", constant = "true")
    @Mapping(target = "tipo", ignore = true)
    Stock toEntity(StockRegistroRequest request);

    void updateEntityFromRequest(StockModificacionRequest request, @MappingTarget Stock stock);
}

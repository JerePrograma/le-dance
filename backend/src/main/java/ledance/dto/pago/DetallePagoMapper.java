package ledance.dto.pago;

import ledance.dto.pago.request.DetallePagoRegistroRequest;
import ledance.entidades.DetallePago;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface DetallePagoMapper {
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "bonificacion", ignore = true)
    @Mapping(target = "recargo", ignore = true)
    @Mapping(target = "pago", ignore = true)
    @Mapping(target = "aCobrar", source = "aCobrar")
    @Mapping(target = "cuota", source = "cuota")
    // Aseguramos que importe se asigne con el valorBase del request
    @Mapping(target = "importe", source = "valorBase")
    DetallePago toEntity(DetallePagoRegistroRequest request);
}

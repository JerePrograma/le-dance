package ledance.dto.pago;

import ledance.dto.pago.request.DetallePagoRegistroRequest;
import ledance.entidades.DetallePago;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface DetallePagoMapper {
    DetallePago toEntity(DetallePagoRegistroRequest request);
}
package ledance.dto.pago;

import ledance.dto.pago.request.PagoMedioRegistroRequest;
import ledance.entidades.PagoMedio;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface PagoMedioMapper {
    PagoMedio toEntity(PagoMedioRegistroRequest request);
}

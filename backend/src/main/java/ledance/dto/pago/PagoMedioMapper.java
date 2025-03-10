package ledance.dto.pago;

import ledance.dto.pago.request.PagoMedioRegistroRequest;
import ledance.dto.pago.response.PagoMedioResponse;
import ledance.entidades.PagoMedio;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface PagoMedioMapper {

    PagoMedio toEntity(PagoMedioRegistroRequest request);

    @Mapping(source = "metodo.id", target = "metodoPagoId")
    @Mapping(source = "metodo.descripcion", target = "metodoPagoDescripcion")
    PagoMedioResponse toDTO(PagoMedio pagoMedio);
}

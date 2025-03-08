package ledance.dto.pago;

import ledance.dto.pago.request.PagoModificacionRequest;
import ledance.dto.pago.request.PagoRegistroRequest;
import ledance.dto.pago.response.PagoResponse;
import ledance.entidades.Pago;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;

@Mapper(componentModel = "spring", uses = {DetallePagoMapper.class, PagoMedioMapper.class})
public interface PagoMapper {

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "inscripcion", ignore = true)
    // En el mapping de toEntity, se ignora el campo metodoPago para evitar la conversión de String a MetodoPago
    @Mapping(target = "metodoPago", ignore = true)
    Pago toEntity(PagoRegistroRequest request);

    @Mapping(target = "inscripcionId", source = "inscripcion.id")
    @Mapping(target = "alumnoId", source = "alumno.id")
    // En toDTO, se mapea metodoPago a partir de su descripción
    @Mapping(target = "metodoPago", expression = "java(pago.getMetodoPago() != null ? pago.getMetodoPago().getDescripcion() : \"\")")
    @Mapping(target = "saldoAFavor", expression = "java(pago.getDetallePagos().stream().mapToDouble(dp -> dp.getAFavor() != null ? dp.getAFavor() : 0.0).sum())")
    @Mapping(target = "estadoPago", expression = "java(pago.getActivo() != null && pago.getActivo() ? \"ACTIVO\" : \"HISTÓRICO\")")
    PagoResponse toDTO(Pago pago);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "inscripcion", ignore = true)
    @Mapping(target = "metodoPago", ignore = true)
    void updateEntityFromRequest(PagoModificacionRequest request, @MappingTarget Pago pago);
}

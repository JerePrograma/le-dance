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
    @Mapping(target = "metodoPago", ignore = true)  // ⚠️ Se ignora aqui, ¿deberia asignarse?
    @Mapping(target = "saldoRestante", source = "saldoRestante")
        // ⚠️ Asegurar que se mapea
    Pago toEntity(PagoRegistroRequest request);

    @Mapping(target = "inscripcionId", source = "inscripcion.id")
    @Mapping(target = "alumnoId", source = "alumno.id")
    @Mapping(target = "metodoPago", expression = "java(pago.getMetodoPago() != null ? pago.getMetodoPago().getDescripcion() : \"\")")
    @Mapping(target = "estadoPago", expression = "java(pago.getEstado())")
    @Mapping(target = "bonificacionAplicada", expression = "java(pago.getBonificacionAplicada())")
    PagoResponse toDTO(Pago pago);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "inscripcion", ignore = true)
    @Mapping(target = "metodoPago", ignore = true)
    void updateEntityFromRequest(PagoModificacionRequest request, @MappingTarget Pago pago);
}

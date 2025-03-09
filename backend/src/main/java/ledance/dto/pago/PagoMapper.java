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
    @Mapping(target = "metodoPago", ignore = true)
    // Usamos el nombre completamente calificado para el enum TipoPago
    @Mapping(target = "tipoPago", expression = "java((request.inscripcionId() != null && request.inscripcionId() == -1) ? ledance.entidades.TipoPago.GENERAL : ledance.entidades.TipoPago.SUBSCRIPTION)")
    Pago toEntity(PagoRegistroRequest request);

    @Mapping(target = "inscripcionId", source = "inscripcion.id")
    @Mapping(target = "alumnoId", source = "alumno.id")
    @Mapping(target = "metodoPago", expression = "java(pago.getMetodoPago() != null ? pago.getMetodoPago().getDescripcion() : \"\")")
    @Mapping(target = "saldoAFavor", expression = "java(pago.getDetallePagos().stream().mapToDouble(dp -> dp.getAFavor() != null ? dp.getAFavor() : 0.0).sum())")
    @Mapping(target = "estadoPago", expression = "java(pago.getActivo() != null && pago.getActivo() ? \"ACTIVO\" : \"HISTÓRICO\")")
    @Mapping(target = "tipoPago", expression = "java(pago.getTipoPago() != null ? pago.getTipoPago().name() : \"\")")
    @Mapping(target = "detallePagos", expression = "java( pago.getDetallePagos().stream()" +
            ".filter(dp -> !pago.getActivo() || (dp.getImporte() != null && dp.getImporte() > 0))" +
            ".map(dp -> detallePagoMapper.toDTO(dp))" +
            ".collect(java.util.stream.Collectors.toList()) )")
    PagoResponse toDTO(Pago pago);

    @Mapping(target = "inscripcion", ignore = true)
    @Mapping(target = "metodoPago", ignore = true)
        // No se actualiza el campo tipoPago en la modificación; se conserva el original.
    void updateEntityFromRequest(PagoModificacionRequest request, @MappingTarget Pago pago);
}

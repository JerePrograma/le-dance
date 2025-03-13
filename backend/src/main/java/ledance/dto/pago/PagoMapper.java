package ledance.dto.pago;

import ledance.dto.pago.request.PagoModificacionRequest;
import ledance.dto.pago.request.PagoRegistroRequest;
import ledance.dto.pago.response.PagoResponse;
import ledance.entidades.Pago;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import ledance.dto.inscripcion.InscripcionMapper;

@Mapper(componentModel = "spring",
        uses = {DetallePagoMapper.class, PagoMedioMapper.class, InscripcionMapper.class})
public interface PagoMapper {

    @Mapping(target = "id", ignore = true)
    // Se asigna la inscripción directamente; la conversión se delega en InscripcionMapper.
    @Mapping(target = "inscripcion", source = "inscripcion")
    @Mapping(target = "metodoPago", ignore = true)
    // Si la inscripción existe se considera SUBSCRIPTION; de lo contrario, GENERAL.
    @Mapping(target = "tipoPago", expression = "java(request.inscripcion() != null ? ledance.entidades.TipoPago.SUBSCRIPTION : ledance.entidades.TipoPago.GENERAL)")
    @Mapping(target = "alumno", ignore = true)
    Pago toEntity(PagoRegistroRequest request);

    @Mapping(target = "inscripcion", source = "inscripcion")
    @Mapping(target = "alumnoId", source = "alumno.id")
    @Mapping(target = "metodoPago", expression = "java(pago.getMetodoPago() != null ? pago.getMetodoPago().getDescripcion() : \"\")")
    // Se puede calcular saldoAFavor y estadoPago de forma similar a como se hacía previamente
    @Mapping(target = "saldoAFavor", expression = "java(pago.getDetallePagos().stream().mapToDouble(dp -> dp.getAFavor() != null ? dp.getAFavor() : 0.0).sum())")
    @Mapping(target = "estadoPago", expression = "java(pago.getActivo() != null && pago.getActivo() ? \"ACTIVO\" : \"HISTÓRICO\")")
    // Se mapea el enum a su nombre usando getTipoPago().name()
    @Mapping(target = "tipoPago", expression = "java(pago.getTipoPago() != null ? pago.getTipoPago().name() : \"\")")
    @Mapping(target = "detallePagos", expression = "java( pago.getDetallePagos().stream()" +
            ".filter(dp -> !pago.getActivo() || (dp.getImportePendiente() != null && dp.getImportePendiente() > 0))" +
            ".map(dp -> detallePagoMapper.toDTO(dp))" +
            ".collect(java.util.stream.Collectors.toList()) )")
    PagoResponse toDTO(Pago pago);

    @Mapping(target = "inscripcion", ignore = true)
    @Mapping(target = "metodoPago", ignore = true)
        // No se actualiza el campo tipoPago en la modificación; se conserva el original.
    void updateEntityFromRequest(PagoModificacionRequest request, @MappingTarget Pago pago);
}

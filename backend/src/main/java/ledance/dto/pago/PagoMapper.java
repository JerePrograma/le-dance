package ledance.dto.pago;

import ledance.dto.pago.request.PagoRegistroRequest;
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
    @Mapping(target = "inscripcion", source = "inscripcion")
    @Mapping(target = "metodoPago", ignore = true)
    // Si la inscripcion existe se considera SUBSCRIPTION; de lo contrario, GENERAL.
    @Mapping(target = "tipoPago", expression = "java(request.inscripcion() != null ? ledance.entidades.TipoPago.SUBSCRIPTION : ledance.entidades.TipoPago.GENERAL)")
    @Mapping(target = "alumno", ignore = true)
    Pago toEntity(PagoRegistroRequest request);

    @Mapping(target = "inscripcion", source = "inscripcion")
    @Mapping(target = "alumnoId", source = "alumno.id")
    @Mapping(target = "metodoPago", expression = "java(pago.getMetodoPago() != null ? pago.getMetodoPago().getDescripcion() : \"\")")
    // Se calcula saldoAFavor sumando los valores a favor de cada detalle.
    @Mapping(target = "saldoAFavor", expression = "java(pago.getDetallePagos().stream().mapToDouble(dp -> dp.getAFavor() != null ? dp.getAFavor() : 0.0).sum())")
    // Se determina "activo" a partir del estado: ACTIVO -> true, lo demás -> false.
    @Mapping(target = "activo", expression = "java(pago.getEstadoPago() != null && pago.getEstadoPago().equals(ledance.entidades.EstadoPago.ACTIVO))")
    // Se asigna el nombre del enum para el campo estadoPago.
    @Mapping(target = "estadoPago", expression = "java(pago.getEstadoPago() != null ? pago.getEstadoPago().name() : \"\")")
    // Se mapea el enum de tipo de pago a su nombre.
    @Mapping(target = "tipoPago", expression = "java(pago.getTipoPago() != null ? pago.getTipoPago().name() : \"\")")
    // Solo se mapean los detalles con importe pendiente > 0.
    @Mapping(target = "detallePagos", expression = "java( pago.getDetallePagos().stream()" +
            ".filter(dp -> dp.getImportePendiente() != null && dp.getImportePendiente() > 0)" +
            ".map(dp -> detallePagoMapper.toDTO(dp))" +
            ".collect(java.util.stream.Collectors.toList()) )")
    PagoResponse toDTO(Pago pago);

    @Mapping(target = "inscripcion", ignore = true)
    @Mapping(target = "metodoPago", ignore = true)
        // No se actualiza el campo tipoPago en la modificacion; se conserva el original.
    void updateEntityFromRequest(PagoRegistroRequest request, @MappingTarget Pago pago);
}

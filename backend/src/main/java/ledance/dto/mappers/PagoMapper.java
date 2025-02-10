package ledance.dto.mappers;

import ledance.dto.request.PagoRegistroRequest;
import ledance.dto.request.PagoModificacionRequest;
import ledance.dto.response.PagoResponse;
import ledance.entidades.Pago;
import org.mapstruct.*;

@Mapper(componentModel = "spring", uses = {MetodoPagoMapper.class})
public interface PagoMapper {

    /**
     * ✅ Convierte un `Pago` a `PagoResponse`.
     */
    @Mapping(target = "id", source = "id")
    @Mapping(target = "inscripcionId", source = "inscripcion.id") // ✅ Mapea el ID de la inscripción
    @Mapping(target = "metodoPago", source = "metodoPago.descripcion", defaultValue = "Desconocido") // ✅ Asegurar que siempre tenga valor
    PagoResponse toDTO(Pago pago);

    /**
     * ✅ Convierte un `PagoRegistroRequest` en una entidad `Pago`.
     */
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "inscripcion", ignore = true) // ✅ Se asigna en el servicio
    @Mapping(target = "metodoPago", ignore = true) // ✅ Se asigna en el servicio
    @Mapping(target = "activo", constant = "true") // ✅ Se asigna automáticamente en true
    Pago toEntity(PagoRegistroRequest request);

    /**
     * ✅ Actualiza un pago existente con nuevos datos.
     */
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "inscripcion", ignore = true) // ✅ No se actualiza la inscripción
    @Mapping(target = "metodoPago", ignore = true) // ✅ No se actualiza el método de pago
    void updateEntityFromRequest(PagoModificacionRequest request, @MappingTarget Pago pago);
}

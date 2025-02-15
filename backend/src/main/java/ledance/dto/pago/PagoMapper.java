package ledance.dto.pago;

import ledance.dto.metodopago.MetodoPagoMapper;
import ledance.dto.pago.request.PagoModificacionRequest;
import ledance.dto.pago.request.PagoRegistroRequest;
import ledance.dto.pago.response.PagoResponse;
import ledance.entidades.Pago;
import org.mapstruct.*;

@Mapper(componentModel = "spring", uses = {MetodoPagoMapper.class})
public interface PagoMapper {

    /**
     * Convierte una entidad Pago en un DTO PagoResponse.
     * Se mapea el ID de la inscripción y el texto del método de pago.
     */
    @Mapping(target = "inscripcionId", source = "inscripcion.id")
    @Mapping(target = "metodoPago", source = "metodoPago.descripcion", defaultValue = "Desconocido")
    PagoResponse toDTO(Pago pago);

    /**
     * Convierte un PagoRegistroRequest en una entidad Pago.
     * Se ignoran las propiedades que se asignarán manualmente (por ejemplo, inscripcion, método de pago,
     * saldoAFavor, observaciones).
     */
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "inscripcion", ignore = true)
    @Mapping(target = "metodoPago", ignore = true)
    @Mapping(target = "activo", constant = "true")
    @Mapping(target = "saldoAFavor", ignore = true)
    @Mapping(target = "observaciones", ignore = true)
    Pago toEntity(PagoRegistroRequest request);

    /**
     * Actualiza una entidad Pago existente con los datos del request.
     * Se ignoran propiedades que no se desean actualizar desde el request (inscripcion y método de pago).
     */
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "inscripcion", ignore = true)
    @Mapping(target = "metodoPago", ignore = true)
    void updateEntityFromRequest(PagoModificacionRequest request, @MappingTarget Pago pago);
}

package ledance.dto.pago;

import ledance.dto.metodopago.MetodoPagoMapper;
import ledance.dto.pago.request.PagoRegistroRequest;
import ledance.dto.pago.request.PagoModificacionRequest;
import ledance.dto.pago.response.PagoResponse;
import ledance.entidades.Pago;
import org.mapstruct.*;

@Mapper(componentModel = "spring", uses = {MetodoPagoMapper.class})
public interface PagoMapper {

    /**
     * Convierte una entidad Pago a su DTO de respuesta.
     * Se mapea el ID de la inscripción y se extrae la descripción del método de pago.
     */
    @Mapping(target = "id", source = "id")
    @Mapping(target = "inscripcionId", source = "inscripcion.id")
    @Mapping(target = "metodoPago", source = "metodoPago.descripcion", defaultValue = "Desconocido")
    PagoResponse toDTO(Pago pago);

    /**
     * Convierte un PagoRegistroRequest en una entidad Pago.
     * Se ignoran la asignación de inscripción y método de pago (se realizan en el servicio),
     * y se asigna "activo" como true por defecto.
     */
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "inscripcion", ignore = true)
    @Mapping(target = "metodoPago", ignore = true)
    @Mapping(target = "activo", constant = "true")
    Pago toEntity(PagoRegistroRequest request);

    /**
     * Actualiza una entidad Pago con datos del PagoModificacionRequest.
     * Se ignoran la inscripción y el método de pago, ya que estos no se modifican aquí.
     */
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "inscripcion", ignore = true)
    @Mapping(target = "metodoPago", ignore = true)
    void updateEntityFromRequest(PagoModificacionRequest request, @MappingTarget Pago pago);
}

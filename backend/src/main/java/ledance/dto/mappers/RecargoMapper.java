// ledance/dto/mappers/RecargoMapper.java
package ledance.dto.mappers;

import ledance.dto.request.RecargoDetalleRegistroRequest;
import ledance.dto.request.RecargoRegistroRequest;
import ledance.dto.response.RecargoDetalleResponse;
import ledance.dto.response.RecargoResponse;
import ledance.entidades.Recargo;
import ledance.entidades.RecargoDetalle;
import org.mapstruct.*;

import java.util.List;

@Mapper(componentModel = "spring")
public interface RecargoMapper {

    // ========== De DTO a Entidad ==========

    /**
     * Mapea un RecargoRegistroRequest a la entidad Recargo (excepto los detalles).
     * En un método @AfterMapping adjuntamos los detalles.
     */
    @Mapping(target = "id", ignore = true)           // Ignoramos el ID (autogenerado)
    @Mapping(target = "detalles", ignore = true)    // Los detalles se añaden en @AfterMapping
    Recargo toEntity(RecargoRegistroRequest dto);

    /**
     * Mapea un RecargoDetalleRegistroRequest a RecargoDetalle (sin asignar recargo).
     */
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "recargo", ignore = true) // Lo seteamos luego en @AfterMapping
    RecargoDetalle toEntity(RecargoDetalleRegistroRequest dto);

    /**
     * Mapea una lista de RecargoDetalleRegistroRequest a una lista de RecargoDetalle.
     */
    List<RecargoDetalle> toDetalleEntityList(List<RecargoDetalleRegistroRequest> dtos);

    /**
     * Tras mapear un Recargo, asignamos los detalles y les seteamos la referencia al recargo "padre".
     */
    @AfterMapping
    default void asignarDetalles(@MappingTarget Recargo recargo, RecargoRegistroRequest dto) {
        if (dto.detalles() != null) {
            List<RecargoDetalle> detalles = toDetalleEntityList(dto.detalles());
            // Asignar el recargo "padre" a cada detalle
            for (RecargoDetalle det : detalles) {
                det.setRecargo(recargo);
            }
            // Guardar la lista de detalles en el Recargo
            recargo.setDetalles(detalles);
        }
    }

    // ========== De Entidad a DTO de Respuesta ==========

    /**
     * Mapea un Recargo a RecargoResponse (con lista de RecargoDetalleResponse).
     */
    @Mapping(target = "id", source = "id")
    @Mapping(target = "descripcion", source = "descripcion")
    @Mapping(target = "detalles", source = "detalles")
    RecargoResponse toResponse(Recargo recargo);

    /**
     * Mapea un RecargoDetalle a RecargoDetalleResponse.
     */
    @Mapping(target = "id", source = "id")
    @Mapping(target = "diaDesde", source = "diaDesde")
    @Mapping(target = "porcentaje", source = "porcentaje")
    RecargoDetalleResponse toResponse(RecargoDetalle detalle);

    /**
     * Mapea una lista de RecargoDetalle a lista de RecargoDetalleResponse.
     */
    List<RecargoDetalleResponse> toDetalleResponseList(List<RecargoDetalle> detalles);

}

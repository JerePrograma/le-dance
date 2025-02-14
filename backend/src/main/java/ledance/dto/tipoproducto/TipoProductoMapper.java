package ledance.dto.tipoproducto;

import ledance.dto.tipoproducto.request.TipoProductoRegistroRequest;
import ledance.dto.tipoproducto.request.TipoProductoModificacionRequest;
import ledance.dto.tipoproducto.response.TipoProductoResponse;
import ledance.entidades.TipoProducto;
import org.mapstruct.*;

@Mapper(componentModel = "spring")
public interface TipoProductoMapper {

    /**
     * ✅ Convierte `TipoProductoRegistroRequest` en una entidad `TipoProducto`.
     * ✔️ Se ignora el ID (se genera automáticamente).
     * ✔️ `activo` se establece automáticamente en `true` en el servicio.
     */
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "activo", constant = "true") // Se asigna `true` por defecto
    TipoProducto toEntity(TipoProductoRegistroRequest request);

    /**
     * ✅ Convierte `TipoProducto` en `TipoProductoResponse` para enviar al frontend.
     */
    @Mapping(target = "id", source = "id")
    TipoProductoResponse toDTO(TipoProducto tipoProducto);

    /**
     * ✅ Actualiza una entidad `TipoProducto` con los valores de `TipoProductoModificacionRequest`.
     * ✔️ `activo` se puede modificar en la actualización.
     */
    @Mapping(target = "id", ignore = true)
    void updateEntityFromRequest(TipoProductoModificacionRequest request, @MappingTarget TipoProducto tipoProducto);
}

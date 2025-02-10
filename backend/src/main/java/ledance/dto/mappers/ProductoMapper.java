package ledance.dto.mappers;

import ledance.dto.request.ProductoRegistroRequest;
import ledance.dto.request.ProductoModificacionRequest;
import ledance.dto.response.ProductoResponse;
import ledance.entidades.Producto;
import org.mapstruct.*;

@Mapper(componentModel = "spring", uses = {TipoProductoMapper.class})
public interface ProductoMapper {

    /**
     * ✅ Convierte un `Producto` a `ProductoResponse`.
     */
    @Mapping(target = "id", source = "id")
    @Mapping(target = "tipoProducto", source = "tipo.descripcion")
    ProductoResponse toDTO(Producto producto);

    /**
     * ✅ Convierte un `ProductoRegistroRequest` en una entidad `Producto`.
     */
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "activo", constant = "true") // ✅ Se asigna automáticamente en true
    @Mapping(target = "tipo", ignore = true)
    Producto toEntity(ProductoRegistroRequest request);

    /**
     * ✅ Actualiza un producto existente con nuevos datos.
     */
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "tipo", ignore = true)
    void updateEntityFromRequest(ProductoModificacionRequest request, @MappingTarget Producto producto);
}

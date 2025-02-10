package ledance.dto.mappers;

import ledance.dto.request.RolRegistroRequest;
import ledance.dto.request.RolModificacionRequest;
import ledance.dto.response.RolResponse;
import ledance.entidades.Rol;
import org.mapstruct.*;

@Mapper(componentModel = "spring")
public interface RolMapper {

    /**
     * ✅ Convierte `RolRegistroRequest` en una entidad `Rol`.
     * ✔ `id` se ignora porque se genera automáticamente.
     * ✔ `activo` se establece en `true` por defecto.
     */
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "activo", constant = "true")
    Rol toEntity(RolRegistroRequest request);

    /**
     * ✅ Convierte `Rol` en `RolResponse`.
     */
    @Mapping(target = "id", source = "id")
    @Mapping(target = "descripcion", source = "descripcion")
    @Mapping(target = "activo", source = "activo")
    RolResponse toDTO(Rol rol);

    /**
     * ✅ Modifica una entidad `Rol` con datos de `RolModificacionRequest`.
     * ✔ `id` no se modifica.
     */
    @Mapping(target = "id", ignore = true)
    void updateEntityFromRequest(RolModificacionRequest request, @MappingTarget Rol rol);
}

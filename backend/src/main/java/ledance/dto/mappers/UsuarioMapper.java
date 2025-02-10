package ledance.dto.mappers;

import ledance.dto.request.UsuarioRegistroRequest;
import ledance.dto.request.UsuarioModificacionRequest;
import ledance.dto.response.UsuarioResponse;
import ledance.entidades.Usuario;
import org.mapstruct.*;

@Mapper(componentModel = "spring")
public interface UsuarioMapper {

    /**
     * ✅ Convierte `UsuarioRegistroRequest` en una entidad `Usuario`.
     * ✔ `id` se ignora porque se genera automáticamente.
     * ✔ `contrasena` y `rol` se asignan en el servicio.
     * ✔ `activo` se establece en `true` por defecto.
     */
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "contrasena", ignore = true) // Se encripta en el servicio
    @Mapping(target = "rol", ignore = true) // Se asigna en el servicio
    @Mapping(target = "activo", constant = "true")
    @Mapping(target = "authorities", ignore = true)
    Usuario toEntity(UsuarioRegistroRequest request);

    /**
     * ✅ Convierte `Usuario` en `UsuarioResponse`.
     */
    @Mapping(target = "id", source = "id")
    @Mapping(target = "nombreUsuario", source = "nombreUsuario")
    @Mapping(target = "email", source = "email")
    @Mapping(target = "rolDescripcion", source = "rol.descripcion") // ✅ Se extrae el nombre del rol
    @Mapping(target = "activo", source = "activo")
    UsuarioResponse toDTO(Usuario usuario);

    /**
     * ✅ Modifica una entidad `Usuario` con datos de `UsuarioModificacionRequest`.
     * ✔ `id`, `contrasena` y `rol` no se modifican aquí.
     */
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "contrasena", ignore = true) // Se mantiene sin cambios
    @Mapping(target = "rol", ignore = true) // No se permite cambiar el rol en la modificación
    @Mapping(target = "authorities", ignore = true)
    void updateEntityFromRequest(UsuarioModificacionRequest request, @MappingTarget Usuario usuario);
}

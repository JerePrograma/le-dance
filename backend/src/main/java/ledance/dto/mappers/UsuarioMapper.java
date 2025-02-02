package ledance.dto.mappers;

import ledance.dto.request.UsuarioRegistroRequest;
import ledance.dto.response.UsuarioResponse;
import ledance.entidades.Usuario;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface UsuarioMapper {

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "contrasena", ignore = true) // Se encripta en el servicio
    @Mapping(target = "rol", ignore = true) // Se asigna en el servicio
    @Mapping(target = "activo", source = "activo", defaultValue = "true")
    Usuario toEntity(UsuarioRegistroRequest request);

    @Mapping(target = "id", source = "id")
    @Mapping(target = "nombreUsuario", source = "nombreUsuario")
    @Mapping(target = "email", source = "email")
    @Mapping(target = "rolDescripcion", source = "rol.descripcion")
    @Mapping(target = "activo", source = "activo")
    UsuarioResponse toDTO(Usuario usuario);
}

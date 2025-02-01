package ledance.dto.mappers;

import ledance.dto.request.UsuarioRegistroRequest;
import ledance.dto.response.UsuarioResponse;
import ledance.entidades.Usuario;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface UsuarioMapper {

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "contrasena", ignore = true) // Se asigna en el servicio después de encriptar
    @Mapping(target = "rol", ignore = true) // Se asigna manualmente en el servicio
    Usuario toEntity(UsuarioRegistroRequest request);

    @Mapping(target = "rolDescripcion", expression = "java(usuario.getRol() != null ? usuario.getRol().getDescripcion() : null)")
    UsuarioResponse toDTO(Usuario usuario);
}

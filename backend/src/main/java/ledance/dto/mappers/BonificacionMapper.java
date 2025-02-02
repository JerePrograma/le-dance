package ledance.dto.mappers;

import ledance.dto.request.BonificacionRequest;
import ledance.dto.response.BonificacionResponse;
import ledance.entidades.Bonificacion;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface BonificacionMapper {

    @Mapping(target = "id", ignore = true) // Se ignora porque es autogenerado
    Bonificacion toEntity(BonificacionRequest request); // ✅ Se agrega este metodo para corregir el error

    @Mapping(target = "id", source = "id") // Asegura que el ID sea parte del DTO
    BonificacionResponse toDTO(Bonificacion bonificacion);
}

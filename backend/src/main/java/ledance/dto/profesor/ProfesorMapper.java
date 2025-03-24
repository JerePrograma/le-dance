package ledance.dto.profesor;

import ledance.dto.profesor.request.ProfesorRegistroRequest;
import ledance.dto.profesor.request.ProfesorModificacionRequest;
import ledance.dto.profesor.response.ProfesorResponse;
import ledance.entidades.Profesor;
import ledance.entidades.Salon;
import org.mapstruct.*;

@Mapper(componentModel = "spring")
public interface ProfesorMapper {

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "activo", constant = "true")
    @Mapping(target = "disciplinas", ignore = true)
    @Mapping(target = "usuario", ignore = true)
    @Mapping(target = "fechaNacimiento", source = "fechaNacimiento")
    @Mapping(target = "telefono", source = "telefono")
    Profesor toEntity(ProfesorRegistroRequest request);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "disciplinas", ignore = true)
    @Mapping(target = "usuario", ignore = true)
    @Mapping(target = "fechaNacimiento", source = "fechaNacimiento")
    @Mapping(target = "telefono", source = "telefono")
    void updateEntityFromRequest(ProfesorModificacionRequest request, @MappingTarget Profesor profesor);

    @Mapping(target = "id", source = "id")
    @Mapping(target = "nombre", source = "nombre")
    ProfesorResponse toResponse(Profesor profesor);

    default String mapSalonToString(Salon salon) {
        if (salon == null) {
            return null;
        }
        // Ajusta segun la propiedad que quieras usar
        // Por ejemplo, si tu Salon tiene un campo "nombre" o "descripcion":
        return salon.getNombre();
    }
}


package ledance.dto.mappers;

import ledance.dto.request.ProfesorRegistroRequest;
import ledance.dto.response.ProfesorResponse;
import ledance.dto.response.ProfesorListadoResponse;
import ledance.dto.response.DatosRegistroProfesorResponse;
import ledance.entidades.Profesor;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.util.Collections; // ✅ Importación añadida

@Mapper(componentModel = "spring", imports = {Collections.class}) // ✅ `imports = Collections.class`
public interface ProfesorMapper {

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "disciplinas", ignore = true)
    @Mapping(target = "usuario", ignore = true)
    Profesor toEntity(ProfesorRegistroRequest request);

    @Mapping(target = "disciplinas", expression = "java(java.util.Collections.emptyList())") // ✅ `java.util.Collections.emptyList()`
    @Mapping(target = "activo", source = "activo")
    ProfesorResponse toDTO(Profesor profesor);

    @Mapping(target = "id", source = "id")
    @Mapping(target = "nombre", source = "nombre")
    @Mapping(target = "apellido", source = "apellido")
    ProfesorListadoResponse toListadoDTO(Profesor profesor);

    @Mapping(target = "id", source = "id")
    @Mapping(target = "nombre", source = "nombre")
    @Mapping(target = "apellido", source = "apellido")
    @Mapping(target = "especialidad", source = "especialidad")
    @Mapping(target = "activo", source = "activo")
    DatosRegistroProfesorResponse toDatosRegistroDTO(Profesor profesor);
}

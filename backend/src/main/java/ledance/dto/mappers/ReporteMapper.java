package ledance.dto.mappers;

import ledance.dto.request.ReporteRequest;
import ledance.dto.response.ReporteResponse;
import ledance.entidades.Reporte;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface ReporteMapper {

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "fechaGeneracion", ignore = true)
    @Mapping(target = "activo", ignore = true)
    Reporte toEntity(ReporteRequest request);

    @Mapping(target = "id", source = "id")
    @Mapping(target = "tipo", source = "tipo")
    @Mapping(target = "descripcion", source = "descripcion") // ✅ Correccion: se mapea correctamente
    @Mapping(target = "fechaGeneracion", source = "fechaGeneracion")
    @Mapping(target = "activo", source = "activo")
    ReporteResponse toDTO(Reporte reporte);
}

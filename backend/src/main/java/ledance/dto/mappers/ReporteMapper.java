package ledance.dto.mappers;

import ledance.dto.request.ReporteRequest;
import ledance.dto.response.ReporteResponse;
import ledance.entidades.Reporte;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface ReporteMapper {

    // Si tuvieras un ReporteRequest para crear Reporte, lo mapeas
    @Mapping(target = "id", ignore = true)
    Reporte toEntity(ReporteRequest request);

    ReporteResponse toDTO(Reporte reporte);
}

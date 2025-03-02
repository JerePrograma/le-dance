package ledance.dto.recargo;

import ledance.dto.recargo.request.RecargoRegistroRequest;
import ledance.dto.recargo.response.RecargoResponse;
import ledance.entidades.Recargo;
import org.mapstruct.*;

@Mapper(componentModel = "spring")
public interface RecargoMapper {

    @Mapping(target = "id", ignore = true)
    Recargo toEntity(RecargoRegistroRequest dto);

    RecargoResponse toResponse(Recargo recargo);
}

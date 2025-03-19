package ledance.dto.egreso;

import ledance.dto.egreso.request.EgresoRegistroRequest;
import ledance.dto.egreso.response.EgresoResponse;
import ledance.entidades.Egreso;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;

import java.util.List;
import java.util.stream.Collectors;

@Mapper(componentModel = "spring", uses = { ledance.dto.metodopago.MetodoPagoMapper.class })
public interface EgresoMapper {

    // Mapear desde el request a la entidad.
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "metodoPago", ignore = true)  // Se asignará en el servicio según el ID
    Egreso toEntity(EgresoRegistroRequest request);

    // Mapear desde la entidad a su DTO de respuesta.
    @Mapping(target = "metodoPago", source = "metodoPago")
    EgresoResponse toDTO(Egreso egreso);

    // Actualizar la entidad a partir del request (útil para operaciones de actualización)
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "metodoPago", ignore = true)
    void updateEntityFromRequest(EgresoRegistroRequest request, @MappingTarget Egreso egreso);

    // Método default para convertir una lista de Egreso en una lista de EgresoResponse
    default List<EgresoResponse> toDTOList(List<Egreso> egresos) {
        return egresos.stream()
                .map(this::toDTO)
                .collect(Collectors.toList());
    }
}

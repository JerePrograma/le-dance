package ledance.dto.pago;

import ledance.dto.alumno.AlumnoMapper;
import ledance.dto.pago.request.PagoRegistroRequest;
import ledance.dto.pago.response.PagoResponse;
import ledance.entidades.Pago;
import ledance.entidades.Salon;
import ledance.entidades.Usuario;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.Named;

import java.util.List;
import java.util.stream.Collectors;

@Mapper(componentModel = "spring", uses = {AlumnoMapper.class, DetallePagoMapper.class})
public interface PagoMapper {

    @Mapping(target = "id", ignore = true)
    // Mapear la lista de detalles usando el DetallePagoMapper
    @Mapping(target = "detallePagos", source = "detallePagos")
    @Mapping(target = "pagoMedios", ignore = true)
    @Mapping(target = "saldoRestante", ignore = true)
    @Mapping(target = "estadoPago", ignore = true)
    @Mapping(target = "observaciones", ignore = true)
    @Mapping(target = "montoPagado", ignore = true)
    @Mapping(target = "recargoMetodoPagoAplicado", source = "recargoMetodoPagoAplicado")
    Pago toEntity(PagoRegistroRequest request);

    @Mapping(target = "alumno", expression = "java(alumnoMapper.toResponse(pago.getAlumno()))")
    @Mapping(target = "usuarioId", source = "usuario.id")
    PagoResponse toDTO(Pago pago);

    // Actualizacion de una entidad existente con datos del request.
    @Mapping(target = "metodoPago", ignore = true)
    @Mapping(target = "detallePagos", ignore = true)
    @Mapping(target = "pagoMedios", ignore = true)
    @Mapping(target = "estadoPago", ignore = true)
    @Mapping(target = "observaciones", ignore = true)
    @Mapping(target = "montoPagado", ignore = true)
    @Mapping(target = "recargoMetodoPagoAplicado", source = "recargoMetodoPagoAplicado")
    void updateEntityFromRequest(PagoRegistroRequest request, @MappingTarget Pago pago);

    default String map(Salon salon) {
        return salon == null ? null : salon.getNombre();
    }

    default List<PagoResponse> toDTOList(List<Pago> pagosDia) {
        return pagosDia.stream()
                .map(this::toDTO)
                .collect(Collectors.toList());
    }
}

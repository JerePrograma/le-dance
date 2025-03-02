package ledance.dto.mensualidad;

import ledance.dto.mensualidad.request.MensualidadRegistroRequest;
import ledance.dto.mensualidad.request.MensualidadModificacionRequest;
import ledance.dto.mensualidad.response.MensualidadResponse;
import ledance.entidades.Mensualidad;
import ledance.entidades.Recargo;
import ledance.entidades.Bonificacion;
import ledance.repositorios.RecargoRepositorio;
import ledance.repositorios.BonificacionRepositorio;
import org.mapstruct.*;
import org.springframework.beans.factory.annotation.Autowired;

@Mapper(componentModel = "spring")
public abstract class MensualidadMapper {

    @Autowired
    protected RecargoRepositorio recargoRepositorio;

    @Autowired
    protected BonificacionRepositorio bonificacionRepositorio;

    @Mapping(target = "id", ignore = true) // Ignorar ID porque es autogenerado
    @Mapping(target = "recargo", source = "recargoId", qualifiedByName = "mapRecargo")
    @Mapping(target = "bonificacion", source = "bonificacionId", qualifiedByName = "mapBonificacion")
    @Mapping(target = "totalPagar", ignore = true) // Se calculará antes de persistir
    public abstract Mensualidad toEntity(MensualidadRegistroRequest dto); // ✅ Ahora es público

    @Mapping(target = "recargoId", source = "recargo.id")
    @Mapping(target = "bonificacionId", source = "bonificacion.id")
    @Mapping(target = "inscripcionId", source = "inscripcion.id")
    public abstract MensualidadResponse toDTO(Mensualidad mensualidad); // ✅ Ahora es público

    @Mapping(target = "recargo", source = "recargoId", qualifiedByName = "mapRecargo")
    @Mapping(target = "bonificacion", source = "bonificacionId", qualifiedByName = "mapBonificacion")
    @Mapping(target = "id", ignore = true) // No se debe modificar el ID
    @Mapping(target = "totalPagar", ignore = true) // Se calculará antes de persistir
    public abstract void updateEntityFromRequest(MensualidadModificacionRequest dto, @MappingTarget Mensualidad mensualidad); // ✅ Ahora es público

    @Named("mapRecargo")
    public Recargo mapRecargo(Long recargoId) {
        return (recargoId != null) ? recargoRepositorio.findById(recargoId).orElse(null) : null;
    }

    @Named("mapBonificacion")
    public Bonificacion mapBonificacion(Long bonificacionId) {
        return (bonificacionId != null) ? bonificacionRepositorio.findById(bonificacionId).orElse(null) : null;
    }
}

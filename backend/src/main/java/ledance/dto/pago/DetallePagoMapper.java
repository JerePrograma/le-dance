package ledance.dto.pago;

import ledance.dto.alumno.AlumnoMapper;
import ledance.dto.pago.request.DetallePagoRegistroRequest;
import ledance.dto.pago.response.DetallePagoResponse;
import ledance.entidades.DetallePago;
import ledance.entidades.Matricula;
import ledance.entidades.Mensualidad;
import ledance.entidades.Stock;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;

import java.util.List;

@Mapper(componentModel = "spring", uses = {AlumnoMapper.class})
public interface DetallePagoMapper {

    @Mapping(target = "alumno", source = "alumno")
    @Mapping(target = "mensualidad", source = "mensualidadId", qualifiedByName = "mapMensualidad")
    @Mapping(target = "matricula", source = "matriculaId", qualifiedByName = "mapMatricula")
    @Mapping(target = "stock", source = "stockId", qualifiedByName = "mapStock")
    @Mapping(target = "concepto", ignore = true)
    @Mapping(target = "subConcepto", ignore = true)
    @Mapping(target = "bonificacion", ignore = true)
    @Mapping(target = "recargo", ignore = true)
    @Mapping(target = "pago", ignore = true)
    @Mapping(target = "importeInicial", ignore = true)
    @Mapping(target = "importePendiente", ignore = true)
    @Mapping(target = "descripcionConcepto", expression = "java(request.descripcionConcepto() != null ? request.descripcionConcepto().trim().toUpperCase() : null)")
    @Mapping(target = "valorBase", source = "valorBase")
    @Mapping(target = "cuotaOCantidad", source = "cuotaOCantidad")
    DetallePago toEntity(DetallePagoRegistroRequest request);

    List<DetallePago> toEntity(List<DetallePagoRegistroRequest> requests);

    @Mapping(target = "version", source = "version")
    @Mapping(target = "bonificacionId", expression = "java(detallePago.getBonificacion() != null ? detallePago.getBonificacion().getId() : null)")
    @Mapping(target = "recargoId", expression = "java(detallePago.getRecargo() != null ? detallePago.getRecargo().getId() : null)")
    @Mapping(target = "mensualidadId", expression = "java(detallePago.getMensualidad() != null ? detallePago.getMensualidad().getId() : null)")
    @Mapping(target = "matriculaId", expression = "java(detallePago.getMatricula() != null ? detallePago.getMatricula().getId() : null)")
    @Mapping(target = "stockId", expression = "java(detallePago.getStock() != null ? detallePago.getStock().getId() : null)")
    @Mapping(target = "fechaRegistro", source = "fechaRegistro")
    DetallePagoResponse toDTO(DetallePago detallePago);

    // Estos métodos "helper" se encargan de construir instancias con el ID
    @Named("mapMensualidad")
    default Mensualidad mapMensualidad(Long id) {
        if (id == null) return null;
        Mensualidad m = new Mensualidad();
        m.setId(id);
        return m;
    }

    @Named("mapMatricula")
    default Matricula mapMatricula(Long id) {
        if (id == null) return null;
        Matricula m = new Matricula();
        m.setId(id);
        return m;
    }

    @Named("mapStock")
    default Stock mapStock(Long id) {
        if (id == null) return null;
        Stock s = new Stock();
        s.setId(id);
        return s;
    }
}

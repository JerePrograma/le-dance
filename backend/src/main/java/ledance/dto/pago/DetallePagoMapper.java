package ledance.dto.pago;

import ledance.dto.alumno.AlumnoMapper;
import ledance.dto.pago.request.DetallePagoRegistroRequest;
import ledance.dto.pago.response.DetallePagoResponse;
import ledance.entidades.*;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.Named;

import java.util.List;

@Mapper(componentModel = "spring", uses = {AlumnoMapper.class})
public interface DetallePagoMapper {

    @Mapping(target = "id", expression = "java((request.id() != null && request.id() == 0) ? null : request.id())")
    @Mapping(target = "version", ignore = true)
    @Mapping(target = "alumno", source = "alumno")  // Se mapea directamente el objeto alumno
    @Mapping(target = "mensualidad", source = "mensualidadId", qualifiedByName = "mapMensualidad")
    @Mapping(target = "matricula", source = "matriculaId", qualifiedByName = "mapMatricula")
    @Mapping(target = "stock", source = "stockId", qualifiedByName = "mapStock")
    @Mapping(target = "bonificacion", ignore = true)
    @Mapping(target = "recargo", ignore = true)
    @Mapping(target = "pago", source = "pagoId", qualifiedByName = "mapPago")
    @Mapping(target = "concepto", source = "conceptoId", qualifiedByName = "mapConcepto")
    @Mapping(target = "subConcepto", source = "subConceptoId", qualifiedByName = "mapSubConcepto")
    @Mapping(target = "descripcionConcepto",
            expression = "java(request.descripcionConcepto() != null ? request.descripcionConcepto().trim().toUpperCase() : null)")
    @Mapping(target = "valorBase", source = "valorBase")
    @Mapping(target = "cuotaOCantidad", source = "cuotaOCantidad")
    @Mapping(target = "tipo", expression = "java(determineTipo(request))")
    @Mapping(target = "ACobrar", source = "ACobrar")
    @Mapping(target = "tieneRecargo", source = "tieneRecargo")
    @Mapping(target = "fechaRegistro", ignore = true)
    @Mapping(target = "esClon", ignore = true)
    @Mapping(target = "estadoPago", ignore = true)
    DetallePago toEntity(DetallePagoRegistroRequest request);

    // Actualización de entidad existente
    @Mapping(target = "id", expression = "java((request.id() != null && request.id() == 0) ? null : request.id())")
    @Mapping(target = "version", ignore = true)
    @Mapping(target = "alumno", source = "alumno")
    @Mapping(target = "tieneRecargo", source = "tieneRecargo")
    @Mapping(target = "mensualidad", source = "mensualidadId", qualifiedByName = "mapMensualidad")
    @Mapping(target = "matricula", source = "matriculaId", qualifiedByName = "mapMatricula")
    @Mapping(target = "stock", source = "stockId", qualifiedByName = "mapStock")
    @Mapping(target = "descripcionConcepto",
            expression = "java(request.descripcionConcepto() != null ? request.descripcionConcepto().trim().toUpperCase() : null)")
    @Mapping(target = "valorBase", source = "valorBase")
    @Mapping(target = "cuotaOCantidad", source = "cuotaOCantidad")
    @Mapping(target = "tipo", expression = "java(determineTipo(request))")
    @Mapping(target = "fechaRegistro", ignore = true)
    @Mapping(target = "esClon", ignore = true)
    @Mapping(target = "estadoPago", ignore = true)
    void updateDetallePagoFromDTO(DetallePagoRegistroRequest request, @MappingTarget DetallePago detallePago);

    default TipoDetallePago determineTipo(DetallePagoRegistroRequest request) {
        if (request.mensualidadId() != null) {
            return TipoDetallePago.MENSUALIDAD;
        } else if (request.matriculaId() != null) {
            return TipoDetallePago.MATRICULA;
        } else if (request.stockId() != null) {
            return TipoDetallePago.STOCK;
        } else {
            return TipoDetallePago.CONCEPTO;
        }
    }

    List<DetallePago> toEntity(List<DetallePagoRegistroRequest> requests);

    // Mapeo para DTO de salida
    @Mapping(target = "id", source = "id")
    @Mapping(target = "version", source = "version")
    @Mapping(target = "descripcionConcepto", source = "descripcionConcepto")
    @Mapping(target = "cuotaOCantidad", source = "cuotaOCantidad")
    @Mapping(target = "valorBase", source = "valorBase")
    @Mapping(target = "importeInicial", source = "importeInicial")
    @Mapping(target = "importePendiente", source = "importePendiente")
    @Mapping(target = "ACobrar", source = "ACobrar")
    @Mapping(target = "cobrado", source = "cobrado")
    @Mapping(target = "tipo", source = "tipo")
    @Mapping(target = "fechaRegistro", source = "fechaRegistro")
    @Mapping(target = "conceptoId", expression = "java(detallePago.getConcepto() != null ? detallePago.getConcepto().getId() : null)")
    @Mapping(target = "subConceptoId", expression = "java(detallePago.getSubConcepto() != null ? detallePago.getSubConcepto().getId() : null)")
    @Mapping(target = "bonificacionId", expression = "java(detallePago.getBonificacion() != null ? detallePago.getBonificacion().getId() : null)")
    @Mapping(target = "bonificacionNombre",
            expression = "java(detallePago.getBonificacion() != null ? detallePago.getBonificacion().getDescripcion() : \"\")")
    @Mapping(target = "recargoId", expression = "java(detallePago.getRecargo() != null ? detallePago.getRecargo().getId() : null)")
    @Mapping(target = "mensualidadId", expression = "java(detallePago.getMensualidad() != null ? detallePago.getMensualidad().getId() : null)")
    @Mapping(target = "matriculaId", expression = "java(detallePago.getMatricula() != null ? detallePago.getMatricula().getId() : null)")
    @Mapping(target = "stockId", expression = "java(detallePago.getStock() != null ? detallePago.getStock().getId() : null)")
    @Mapping(target = "pagoId", expression = "java(detallePago.getPago() != null ? detallePago.getPago().getId() : null)")
    @Mapping(target = "alumno", source = "alumno")  // Se mapea el objeto alumno mediante AlumnoMapper
    @Mapping(target = "tieneRecargo", source = "tieneRecargo")
    @Mapping(target = "estadoPago", source = "estadoPago")
    DetallePagoResponse toDTO(DetallePago detallePago);

    // Métodos helper para asociaciones

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

    @Named("mapPago")
    default Pago mapPago(Long id) {
        if (id == null) return null;
        Pago p = new Pago();
        p.setId(id);
        return p;
    }

    @Named("mapConcepto")
    default Concepto mapConcepto(Long id) {
        if (id == null) return null;
        Concepto c = new Concepto();
        c.setId(id);
        return c;
    }

    @Named("mapSubConcepto")
    default SubConcepto mapSubConcepto(Long id) {
        if (id == null) return null;
        SubConcepto sc = new SubConcepto();
        sc.setId(id);
        return sc;
    }

    @Named("mapUsuario")
    default Usuario mapUsuario(Long usuarioId) {
        if (usuarioId == null) {
            return null;
        }
        Usuario usuario = new Usuario();
        usuario.setId(usuarioId);
        return usuario;
    }

    @Named("mapAlumno")
    default Alumno mapAlumno(Long alumnoId) {
        if (alumnoId == null) {
            return null;
        }
        Alumno alumno = new Alumno();
        alumno.setId(alumnoId);
        return alumno;
    }
}

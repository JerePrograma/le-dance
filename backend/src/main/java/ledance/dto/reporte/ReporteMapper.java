package ledance.dto.reporte;

import ledance.dto.reporte.request.ReporteRegistroRequest;
import ledance.dto.reporte.request.ReporteModificacionRequest;
import ledance.dto.reporte.response.ReporteResponse;
import ledance.entidades.Reporte;
import org.mapstruct.*;

@Mapper(componentModel = "spring")
public interface ReporteMapper {

    /**
     * ✅ Convierte `ReporteRegistroRequest` en una entidad `Reporte`.
     */
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "activo", constant = "true") // ✅ Siempre inicia activo
    @Mapping(target = "usuario", ignore = true) // ✅ Se asigna en el servicio
    @Mapping(target = "tipo", source = "tipo") // ✅ Se mapea correctamente el tipo
    @Mapping(target = "descripcion", source = "descripcion", defaultValue = "Sin descripción") // ✅ Manejo de valores nulos
    Reporte toEntity(ReporteRegistroRequest request);

    /**
     * ✅ Convierte `Reporte` en `ReporteResponse`.
     */
    @Mapping(target = "id", source = "id")
    @Mapping(target = "tipo", source = "tipo")
    @Mapping(target = "descripcion", source = "descripcion")
    @Mapping(target = "fechaGeneracion", source = "fechaGeneracion")
    @Mapping(target = "activo", source = "activo")
    ReporteResponse toDTO(Reporte reporte);

    /**
     * ✅ Modifica una entidad `Reporte` con datos de `ReporteModificacionRequest`.
     */
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "fechaGeneracion", ignore = true) // ✅ No se puede modificar la fecha
    @Mapping(target = "usuario", ignore = true) // ✅ No se permite cambiar el usuario
    @Mapping(target = "tipo", ignore = true) // ✅ No se permite cambiar el tipo
    void updateEntityFromRequest(ReporteModificacionRequest request, @MappingTarget Reporte reporte);
}

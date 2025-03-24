package ledance.dto.mensualidad;

import ledance.dto.mensualidad.request.MensualidadRegistroRequest;
import ledance.dto.mensualidad.response.MensualidadResponse;
import ledance.entidades.EstadoMensualidad;
import ledance.entidades.Mensualidad;
import org.mapstruct.*;

import java.time.LocalDate;

@Mapper(componentModel = "spring")
public interface MensualidadMapper {

    // Mapea explicitamente el campo importeInicial
    @Mapping(target = "recargoId", source = "recargo.id")
    @Mapping(target = "bonificacion", source = "bonificacion")
    @Mapping(target = "inscripcionId", source = "inscripcion.id")
    @Mapping(target = "importeInicial", source = "importeInicial")  // <-- Agregado explicitamente
    MensualidadResponse toDTO(Mensualidad mensualidad);

    @Mapping(target = "id", ignore = true)
    void updateEntityFromRequest(MensualidadRegistroRequest dto, @MappingTarget Mensualidad mensualidad);

    default Mensualidad toEntity(MensualidadRegistroRequest dto) {
        Mensualidad mensualidad = new Mensualidad();
        // Asignar datos que vienen en el request
        mensualidad.setFechaCuota(dto.fechaCuota());
        mensualidad.setValorBase(dto.valorBase());
        mensualidad.setEstado(EstadoMensualidad.valueOf(dto.estado()));
        // Asignar relaciones, por ejemplo, la inscripcion (quizas se resuelva en el servicio)
        // mensualidad.setInscripcion( ... );
        // Valores por defecto para campos obligatorios que no vienen en el DTO:
        mensualidad.setFechaGeneracion(LocalDate.now());
        mensualidad.setImporteInicial(dto.valorBase());
        mensualidad.setImportePendiente(dto.valorBase());
        // Otros campos (por ejemplo, descripcion) se pueden dejar en null o asignarles un valor predeterminado
        return mensualidad;
    }

}

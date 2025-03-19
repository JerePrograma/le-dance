package ledance.dto.pago;

import ledance.dto.alumno.request.AlumnoRegistroRequest;
import ledance.dto.inscripcion.request.InscripcionRegistroRequest;
import ledance.dto.inscripcion.response.InscripcionResponse;
import ledance.dto.metodopago.response.MetodoPagoResponse;
import ledance.dto.pago.request.PagoRegistroRequest;
import ledance.dto.pago.response.PagoResponse;
import ledance.entidades.Alumno;
import ledance.entidades.Inscripcion;
import ledance.entidades.MetodoPago;
import ledance.entidades.Pago;
import ledance.entidades.TipoPago;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.stream.Collectors;

@Mapper(componentModel = "spring",
        uses = {DetallePagoMapper.class, PagoMedioMapper.class})
public interface PagoMapper {

    Logger log = LoggerFactory.getLogger(PagoMapper.class);

    // En este mapeo ya no ignoramos el campo 'alumno' sino que lo mapeamos manualmente.
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "inscripcion", expression = "java(mapInscripcionManual(request.inscripcion()))")
    @Mapping(target = "alumno", expression = "java(mapAlumno(request.alumno(), request.inscripcion()))")
    @Mapping(target = "metodoPago", ignore = true)
    @Mapping(target = "tipoPago", expression = "java((request.inscripcion() != null && request.inscripcion().id() != null && request.inscripcion().id() > 0) ? ledance.entidades.TipoPago.SUBSCRIPTION : ledance.entidades.TipoPago.GENERAL)")
    @Mapping(target = "saldoRestante", constant = "0.0")
    @Mapping(target = "saldoAFavor", constant = "0.0")
    @Mapping(target = "estadoPago", constant = "ACTIVO")
    @Mapping(target = "montoPagado", constant = "0.0")
    @Mapping(target = "observaciones", ignore = true)
    Pago toEntity(PagoRegistroRequest request);

    // Método para mapear manualmente la inscripción.
    default Inscripcion mapInscripcionManual(InscripcionRegistroRequest inscripcionRequest) {
        if (inscripcionRequest == null || inscripcionRequest.id() == null || inscripcionRequest.id() <= 0) {
            log.info("[PagoMapper] Inscripción inválida recibida (se asignará null): {}", inscripcionRequest);
            return null;
        }
        // Aquí puedes mapear los campos necesarios; por ejemplo:
        Inscripcion inscripcion = new Inscripcion();
        inscripcion.setId(inscripcionRequest.id());
        inscripcion.setFechaInscripcion(inscripcionRequest.fechaInscripcion());
        // Mapea otros campos según lo requieras...
        return inscripcion;
    }

    // Método para mapear el alumno. Se utiliza el alumno del request si es válido;
    // de lo contrario, se intenta obtenerlo de la inscripción.
    default Alumno mapAlumno(AlumnoRegistroRequest alumnoRequest, InscripcionRegistroRequest inscripcionRequest) {
        if (alumnoRequest != null && alumnoRequest.id() != null && alumnoRequest.id() > 0) {
            Alumno alumno = new Alumno();
            alumno.setId(alumnoRequest.id());
            alumno.setNombre(alumnoRequest.nombre());
            // Mapear otros campos del alumno según sea necesario.
            return alumno;
        } else if (inscripcionRequest != null && inscripcionRequest.alumno() != null &&
                inscripcionRequest.alumno().id() != null && inscripcionRequest.alumno().id() > 0) {
            Alumno alumno = new Alumno();
            alumno.setId(inscripcionRequest.alumno().id());
            alumno.setNombre(inscripcionRequest.alumno().nombre());
            // Mapear otros campos si es necesario.
            return alumno;
        }
        log.info("[PagoMapper] No se pudo mapear un alumno válido a partir del request: alumnoRequest={}, inscripcionRequest={}", alumnoRequest, inscripcionRequest);
        return null;
    }

    @Mapping(target = "inscripcion", source = "inscripcion")
    @Mapping(target = "alumnoId", source = "alumno.id")
    @Mapping(target = "metodoPago", expression = "java(mapMetodoPago(pago.getMetodoPago()))")
    @Mapping(target = "saldoAFavor", expression = "java(pago.getDetallePagos().stream().mapToDouble(dp -> dp.getAFavor() != null ? dp.getAFavor() : 0.0).sum())")
    @Mapping(target = "activo", expression = "java(pago.getEstadoPago() != null && pago.getEstadoPago().equals(ledance.entidades.EstadoPago.ACTIVO))")
    @Mapping(target = "estadoPago", expression = "java(pago.getEstadoPago() != null ? pago.getEstadoPago().name() : \"\")")
    @Mapping(target = "tipoPago", expression = "java(pago.getTipoPago() != null ? pago.getTipoPago().name() : \"\")")
    @Mapping(target = "montoBasePago", source = "montoBasePago")
    @Mapping(target = "detallePagos", expression = "java( pago.getDetallePagos().stream()" +
            ".filter(dp -> dp.getImportePendiente() != null && dp.getImportePendiente() > 0)" +
            ".map(dp -> detallePagoMapper.toDTO(dp))" +
            ".collect(java.util.stream.Collectors.toList()) )")
    PagoResponse toDTO(Pago pago);

    @Mapping(target = "inscripcion", ignore = true)
    @Mapping(target = "metodoPago", ignore = true)
    void updateEntityFromRequest(PagoRegistroRequest request, @MappingTarget Pago pago);

    default MetodoPagoResponse mapMetodoPago(MetodoPago metodo) {
        if (metodo == null) {
            return null;
        }
        return new MetodoPagoResponse(
                metodo.getId(),
                metodo.getDescripcion(),
                metodo.getActivo(),
                metodo.getRecargo()
        );
    }

    default List<PagoResponse> toDTOList(List<Pago> pagos) {
        return pagos.stream()
                .map(this::toDTO)
                .collect(Collectors.toList());
    }
}

package ledance.dto.pago;

import ledance.dto.inscripcion.request.InscripcionRegistroRequest;
import ledance.dto.pago.request.PagoRegistroRequest;
import ledance.dto.pago.response.PagoResponse;
import ledance.entidades.Inscripcion;
import ledance.entidades.Pago;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Mapper(componentModel = "spring",
        uses = {DetallePagoMapper.class, PagoMedioMapper.class})
public interface PagoMapper {

    Logger log = LoggerFactory.getLogger(PagoMapper.class);

    @Mapping(target = "id", ignore = true)
    // Usamos un método default para mapear manualmente la inscripción.
    @Mapping(target = "inscripcion", expression = "java(mapInscripcionManual(request.inscripcion()))")
    @Mapping(target = "metodoPago", ignore = true)
    // Si la inscripción es válida se asigna SUBSCRIPTION; de lo contrario, GENERAL.
    @Mapping(target = "tipoPago", expression = "java((request.inscripcion() != null && request.inscripcion().id() != null && request.inscripcion().id() > 0) ? ledance.entidades.TipoPago.SUBSCRIPTION : ledance.entidades.TipoPago.GENERAL)")
    @Mapping(target = "alumno", ignore = true)
    // Inicialización de importes y estado (ajusta según la lógica de negocio).
    @Mapping(target = "saldoRestante", constant = "0.0")
    @Mapping(target = "saldoAFavor", constant = "0.0")
    @Mapping(target = "estadoPago", constant = "ACTIVO")
    @Mapping(target = "montoPagado", constant = "0.0")
    @Mapping(target = "observaciones", ignore = true)
    Pago toEntity(PagoRegistroRequest request);

    // Mapeo manual para la inscripción.
    default Inscripcion mapInscripcionManual(InscripcionRegistroRequest inscripcionRequest) {
        if (inscripcionRequest == null || inscripcionRequest.id() == null || inscripcionRequest.id() <= 0) {
            log.info("[PagoMapper] Inscripción inválida recibida (se asignará null): {}", inscripcionRequest);
            return null;
        }
        Inscripcion inscripcion = new Inscripcion();
        // Mapea los campos necesarios.
        inscripcion.setFechaInscripcion(inscripcionRequest.fechaInscripcion());
        // Puedes mapear otros campos según lo requieras.
        return inscripcion;
    }

    // Mapeo a DTO: se incluye montoBasePago y se corrigen los nombres (por ejemplo, aFavor y montoOriginal).
    @Mapping(target = "inscripcion", source = "inscripcion")
    @Mapping(target = "alumnoId", source = "alumno.id")
    @Mapping(target = "metodoPago", expression = "java(pago.getMetodoPago() != null ? pago.getMetodoPago().getDescripcion() : \"\")")
    // Se calcula saldoAFavor sumando el aFavor de cada detalle (se utiliza el getter correcto).
    @Mapping(target = "saldoAFavor", expression = "java(pago.getDetallePagos().stream().mapToDouble(dp -> dp.getAFavor() != null ? dp.getAFavor() : 0.0).sum())")
    @Mapping(target = "activo", expression = "java(pago.getEstadoPago() != null && pago.getEstadoPago().equals(ledance.entidades.EstadoPago.ACTIVO))")
    @Mapping(target = "estadoPago", expression = "java(pago.getEstadoPago() != null ? pago.getEstadoPago().name() : \"\")")
    @Mapping(target = "tipoPago", expression = "java(pago.getTipoPago() != null ? pago.getTipoPago().name() : \"\")")
    // Se mapea el nuevo campo montoBasePago para la respuesta.
    @Mapping(target = "montoBasePago", source = "montoBasePago")
    // Se mapean los detalles con importePendiente > 0.
    @Mapping(target = "detallePagos", expression = "java( pago.getDetallePagos().stream()" +
            ".filter(dp -> dp.getImportePendiente() != null && dp.getImportePendiente() > 0)" +
            ".map(dp -> detallePagoMapper.toDTO(dp))" +
            ".collect(java.util.stream.Collectors.toList()) )")
    PagoResponse toDTO(Pago pago);

    @Mapping(target = "inscripcion", ignore = true)
    @Mapping(target = "metodoPago", ignore = true)
    void updateEntityFromRequest(PagoRegistroRequest request, @MappingTarget Pago pago);
}

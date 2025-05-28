package ledance.servicios.detallepago;

import ledance.dto.pago.DetallePagoResolucion;
import ledance.entidades.*;
import ledance.repositorios.*;
import ledance.servicios.pago.PaymentCalculationServicio;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class DetallePagoResolver {

    private static final Logger log = LoggerFactory.getLogger(DetallePagoResolver.class);

    private final MatriculaRepositorio matriculaRepo;
    private final MensualidadRepositorio mensualidadRepo;
    private final ConceptoRepositorio conceptoRepo;
    private final SubConceptoRepositorio subConceptoRepo;
    private final StockRepositorio stockRepo;
    private final PaymentCalculationServicio paymentCalculationServicio;

    @Transactional(readOnly = true)
    public DetallePagoResolucion resolver(String descripcionConcepto) {
        String norm = descripcionConcepto.trim().toUpperCase();
        TipoDetallePago tipo = paymentCalculationServicio.determinarTipoDetalle(norm);

        Long matId = null, mesId = null, conId = null, subId = null, stoId = null;

        switch (tipo) {
            case MATRICULA -> {
                // Esperamos siempre "MATRICULA <AÑO>"
                String[] partes = norm.split("\\s+", 2);
                if (partes.length == 2) {
                    try {
                        int anio = Integer.parseInt(partes[1]);
                        matId = matriculaRepo
                                .findFirstByAnio(anio)
                                .map(Matricula::getId)
                                .orElse(null);
                    } catch (NumberFormatException ex) {
                        log.warn("[resolver] Año de matrícula no válido en '{}'", norm);
                    }
                } else {
                    log.warn("[resolver] Formato inesperado para matrícula: '{}'", norm);
                }
            }
            case MENSUALIDAD -> {
                String primeraPalabra = norm.contains(" ")
                        ? norm.substring(0, norm.indexOf(' '))
                        : norm;
                mesId = mensualidadRepo
                        .findFirstByDescripcionContainingIgnoreCase(primeraPalabra)
                        .map(Mensualidad::getId)
                        .orElse(null);
            }
            case STOCK -> {
                String primeraPalabra = norm.contains(" ")
                        ? norm.substring(0, norm.indexOf(' '))
                        : norm;
                stoId = stockRepo
                        .findByNombreIgnoreCase(primeraPalabra)
                        .map(Stock::getId)
                        .orElse(null);
            }
            case CONCEPTO -> {
                String primeraPalabra = norm.contains(" ")
                        ? norm.substring(0, norm.indexOf(' '))
                        : norm;

                subId = subConceptoRepo
                        .findByDescripcionIgnoreCase(primeraPalabra)
                        .map(SubConcepto::getId)
                        .orElse(null);

                conId = conceptoRepo
                        .findFirstByDescripcionContainingIgnoreCase(norm)
                        .map(Concepto::getId)
                        .orElse(null);

                if (conId == null && subId != null) {
                    conId = conceptoRepo
                            .findFirstBySubConceptoId(subId)
                            .map(Concepto::getId)
                            .orElse(null);
                }
            }
        }

        return new DetallePagoResolucion(tipo, conId, subId, matId, mesId, stoId);
    }

}

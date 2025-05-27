package ledance.servicios.detallepago;

import ledance.dto.pago.DetallePagoResolucion;
import ledance.entidades.*;
import ledance.repositorios.*;
import ledance.servicios.pago.PaymentCalculationServicio;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class DetallePagoResolver {

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

        String primeraPalabra = norm.contains(" ")
                ? norm.substring(0, norm.indexOf(' '))
                : norm;

        Long matId = null, mesId = null, conId = null, subId = null, stoId = null;

        switch (tipo) {
            case MATRICULA -> {
                matId = matriculaRepo
                        .findFirstByAnio(Integer.parseInt(primeraPalabra))
                        .map(Matricula::getId)
                        .orElse(null);
            }
            case MENSUALIDAD -> {
                mesId = mensualidadRepo
                        .findFirstByDescripcionContainingIgnoreCase(primeraPalabra)
                        .map(Mensualidad::getId)
                        .orElse(null);
            }
            case STOCK -> {
                stoId = stockRepo
                        .findByNombreIgnoreCase(primeraPalabra)
                        .map(Stock::getId)
                        .orElse(null);
            }
            case CONCEPTO -> {
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

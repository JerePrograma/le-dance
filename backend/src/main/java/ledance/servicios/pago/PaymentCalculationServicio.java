package ledance.servicios.pago;

import jakarta.persistence.EntityNotFoundException;
import ledance.dto.matricula.MatriculaMapper;
import ledance.dto.matricula.request.MatriculaRegistroRequest;
import ledance.dto.matricula.response.MatriculaResponse;
import ledance.entidades.*;
import ledance.servicios.detallepago.DetallePagoServicio;
import ledance.servicios.matricula.MatriculaServicio;
import ledance.servicios.mensualidad.MensualidadServicio;
import ledance.servicios.stock.StockServicio;
import ledance.dto.mensualidad.response.MensualidadResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.Arrays;
import java.util.Optional;

@Service
public class PaymentCalculationServicio {

    private static final Logger log = LoggerFactory.getLogger(PaymentCalculationServicio.class);

    private final MatriculaServicio matriculaServicio;
    private final MensualidadServicio mensualidadServicio;
    private final StockServicio stockServicio;
    private final DetallePagoServicio detallePagoServicio;
    private final MatriculaMapper matriculaMapper;

    public PaymentCalculationServicio(MatriculaServicio matriculaServicio,
                                      MensualidadServicio mensualidadServicio,
                                      StockServicio stockServicio,
                                      DetallePagoServicio detallePagoServicio, MatriculaMapper matriculaMapper) {
        this.matriculaServicio = matriculaServicio;
        this.mensualidadServicio = mensualidadServicio;
        this.stockServicio = stockServicio;
        this.detallePagoServicio = detallePagoServicio;
        this.matriculaMapper = matriculaMapper;
    }

    private boolean esConceptoMensualidad(String concepto) {
        return concepto.contains("CUOTA")
                || concepto.contains("CLASE SUELTA")
                || concepto.contains("CLASE DE PRUEBA");
    }

    private boolean existeStockConNombre(String nombre) {
        return stockServicio.obtenerStockPorNombre(nombre);
    }

    // Actualiza los totales del Pago según la suma de aCobrar y los importes pendientes de los detalles
    public void calcularYActualizarImportes(Pago pago) {
        double totalAbonado = 0.0;
        double totalPendiente = 0.0;

        for (DetallePago detalle : pago.getDetallePagos()) {
            totalAbonado += detalle.getaCobrar();
            totalPendiente += detalle.getImportePendiente() != null ? detalle.getImportePendiente() : 0.0;
        }

        // Para el nuevo pago: el monto es la suma de lo abonado (ej. 1500)
        // y el saldoRestante es la suma de los importes pendientes (ej. 1000 si partíamos de 2500 y se abonó 1500)
        pago.setMonto(totalAbonado);
        // Se ignora montoBasePago (solo de lectura) en el cálculo
        pago.setMontoPagado(totalAbonado);
        pago.setSaldoRestante(totalPendiente);

        if (pago.getSaldoRestante() <= 0.0) {
            pago.setEstadoPago(EstadoPago.HISTORICO);
        }
    }

    // Método para aplicar un abono a un DetallePago ya existente (para pagos posteriores)
    public void aplicarAbono(DetallePago detalle, double montoAbono) {
        double currentPendiente = detalle.getImportePendiente();
        double abono = Math.min(montoAbono, currentPendiente); // limitar el abono al pendiente
        detalle.setaCobrar(abono);
        detalle.setImportePendiente(Math.max(currentPendiente - abono, 0.0));

        if (detalle.getImportePendiente() <= 0.0) {
            detalle.setCobrado(true);
            detalle.setImportePendiente(0.0);

            if (detalle.getTipo() == TipoDetallePago.MENSUALIDAD && detalle.getMensualidad() != null) {
                mensualidadServicio.marcarComoPagada(detalle.getMensualidad().getId(), LocalDate.now());
            }
            if (detalle.getTipo() == TipoDetallePago.MATRICULA && detalle.getMatricula() != null) {
                matriculaServicio.actualizarEstadoMatricula(
                        detalle.getMatricula().getId(),
                        new MatriculaRegistroRequest(detalle.getAlumno().getId(), detalle.getMatricula().getAnio(), true, LocalDate.now())
                );
            }
            if (detalle.getTipo() == TipoDetallePago.STOCK && detalle.getStock() != null) {
                stockServicio.reducirStock(detalle.getStock().getNombre(), 1);
            }
        }
    }

    private String conceptoNormalizado(String concepto) {
        return concepto.trim().toUpperCase();
    }

    /**
     * Método principal que decide cómo calcular el importe (descuentos, recargos, etc.)
     * según el tipo de detalle (Mensualidad, Matricula, Stock u Otro)
     * y aplica el proceso de abono correspondiente.
     *
     * @param pago        La entidad Pago que agrupa los detalles (sirve para obtener inscripcion, alumno, etc.)
     * @param detalle     El DetallePago a calcular
     * @param inscripcion (Opcional) si deseas pasarla cuando la tengas (puede extraerse de pago.getInscripcion())
     */
    public void calcularImportesDetalle(Pago pago, DetallePago detalle, Inscripcion inscripcion) {
        // Normalizar la descripción para identificar el tipo de concepto
        String conceptoDesc = Optional.ofNullable(detalle.getDescripcionConcepto())
                .orElse("")
                .toUpperCase()
                .trim();

        // Determinar el tipo de detalle
        if (esConceptoMensualidad(conceptoDesc)) {
            calcularMensualidad(detalle, inscripcion, pago);
        } else if (existeStockConNombre(conceptoDesc)) {
            calcularStock(detalle);
        } else if (conceptoDesc.startsWith("MATRICULA")) {
            calcularMatricula(detalle, pago);
        } else {
            calcularConceptoGeneral(detalle);
        }
    }

    private double calcularDescuentoPorInscripcion(double base, Inscripcion inscripcion) {
        double descuentoFijo = Optional.ofNullable(inscripcion.getBonificacion().getValorFijo()).orElse(0.0);
        double descuentoPorcentaje = Optional.ofNullable(inscripcion.getBonificacion().getPorcentajeDescuento())
                .orElse(0) / 100.0 * base;
        return descuentoFijo + descuentoPorcentaje;
    }

    // ----------------------------------------------------------------
    // Ejemplo de submétodo: cálculo para Matricula
    // ----------------------------------------------------------------
    void calcularMatricula(DetallePago detalle, Pago pago) {
        double importeInicialCalculado = detalle.getValorBase();
        procesarAbonoEnDetalle(detalle, detalle.getaCobrar(), importeInicialCalculado);

        detalle.setTipo(TipoDetallePago.MATRICULA);
        detalle.setCobrado(detalle.getImportePendiente() == 0);

        procesarMatricula(pago, detalle);
    }

    // ----------------------------------------------------------------
    // Ejemplo de submétodo: cálculo para Stock
    // ----------------------------------------------------------------
    void calcularStock(DetallePago detalle) {
        double base = Optional.ofNullable(detalle.getValorBase()).orElse(0.0);
        // Para stock no aplicamos descuentos ni recargos
        double importeInicialCalculado = base;

        procesarAbonoEnDetalle(detalle, detalle.getaCobrar(), importeInicialCalculado);

        detalle.setTipo(TipoDetallePago.STOCK);
        detalle.setCobrado(detalle.getImportePendiente() == 0);

        procesarStock(detalle);
    }

    // ----------------------------------------------------------------
    // Ejemplo de submétodo: cálculo para Mensualidad
    // ----------------------------------------------------------------
    void calcularMensualidad(DetallePago detalle, Inscripcion inscripcion, Pago pago) {
        double base = detalle.getValorBase();

        // Si la Inscripcion trae bonificación, calculamos a partir de ahí;
        // en caso contrario, llamamos al método de detallePagoServicio para ver si tenía bonificación
        double descuento = (inscripcion != null && inscripcion.getBonificacion() != null)
                ? calcularDescuentoPorInscripcion(base, inscripcion)
                : detallePagoServicio.calcularDescuento(detalle, base);

        double recargo = (detalle.getRecargo() != null)
                ? detallePagoServicio.obtenerValorRecargo(detalle, base)
                : 0.0;

        double importeInicialCalculado = base - descuento + recargo;

        // Aplica la lógica de abono (actualiza importePendiente, aCobrar, etc.)
        procesarAbonoEnDetalle(detalle, detalle.getaCobrar(), importeInicialCalculado);

        // Asignar el tipo para uso posterior
        detalle.setTipo(TipoDetallePago.MENSUALIDAD);

        // Marcar como cobrado si se saldó
        detalle.setCobrado(detalle.getImportePendiente() == 0);

        // Llamada opcional para actualizar estados de la Mensualidad (pagada)
        procesarMensualidad(pago, detalle);
    }

    // ----------------------------------------------------------------
    // Ejemplo de submétodo: cálculo para Concepto General
    // ----------------------------------------------------------------
    void calcularConceptoGeneral(DetallePago detalle) {
        double base = detalle.getValorBase();
        double descuento = detallePagoServicio.calcularDescuento(detalle, base);
        double recargo = (detalle.getRecargo() != null)
                ? detallePagoServicio.obtenerValorRecargo(detalle, base)
                : 0.0;

        double importeInicialCalculado = base - descuento + recargo;
        procesarAbonoEnDetalle(detalle, detalle.getaCobrar(), importeInicialCalculado);

        detalle.setTipo(TipoDetallePago.CONCEPTO);
        detalle.setCobrado(detalle.getImportePendiente() == 0);
    }

    // ----------------------------------------------------------------
    // Lógica común para aplicar el abono (importePendiente, aCobrar, etc.)
    // ----------------------------------------------------------------
    private void procesarAbonoEnDetalle(DetallePago detalle, double montoAbono, double importeInicialCalculado) {
        if (detalle.getImporteInicial() == null) {
            detalle.setImporteInicial(importeInicialCalculado);
            double abono = Math.min(montoAbono, importeInicialCalculado);
            detalle.setaCobrar(abono);
            detalle.setImportePendiente(Math.max(importeInicialCalculado - abono, 0.0));
        } else {
            double currentPendiente = detalle.getImportePendiente() != null ? detalle.getImportePendiente() : 0.0;
            double abono = Math.min(montoAbono, currentPendiente);
            detalle.setaCobrar(abono);
            detalle.setImportePendiente(Math.max(currentPendiente - abono, 0.0));
        }

        if (detalle.getImportePendiente() <= 0.0) {
            detalle.setCobrado(true);
            detalle.setImportePendiente(0.0);
        }
    }

    private void procesarMatricula(Pago pago, DetallePago detalle) {
        log.info("[procesarMatricula] Procesando matrícula para detalle id={}", detalle.getId());
        // Cálculo de importes
        detalle.setImporteInicial(detalle.getValorBase());
        double pendiente = detalle.getValorBase() - detalle.getaCobrar();
        detalle.setImportePendiente(pendiente);

        // Se asume que la descripción es "MATRICULA {anio}"
        String[] partes = detalle.getDescripcionConcepto().split(" ");
        log.info("[procesarMatricula] Partes del concepto: {}", Arrays.toString(partes));
        if (partes.length >= 2) {
            try {
                int anio = Integer.parseInt(partes[1]);
                Alumno alumno = pago.getAlumno();

                if (alumno == null) {
                    throw new EntityNotFoundException("No se encontró alumno asociado al pago");
                }
                // Si se envía un matriculaId en el detalle, significa que se trata de un abono parcial.
                // En ese caso, se permite actualizar la matrícula si tiene saldo > 0.
                if (detalle.getMatricula() != null) {
                    // Actualizar el abono: se asume que aCobrar se acumula y se recalcula el saldo pendiente.
                    // Aquí se podría sumar el valor de aCobrar al total abonado, si lo manejas en la entidad.
                    log.info("[procesarMatricula] Actualizando abono parcial de matrícula existente");
                } else if (matriculaServicio.existeMatriculaParaAnio(alumno.getId(), anio)) {
                    // Se consulta la matrícula pendiente para ese año
                    MatriculaResponse matriculaResp = matriculaServicio.obtenerOMarcarPendiente(alumno.getId());
                    // Si la matrícula ya está pagada o el saldo es 0, se lanza error.
                    if (matriculaResp.pagada() || /* lógica adicional para el saldo (si se modela en el DTO) */ false) {
                        throw new IllegalArgumentException("Ya existe una matrícula registrada para el año " + anio);
                    }
                    // De lo contrario, se actualiza la matrícula con el nuevo abono
                    MatriculaRegistroRequest request = new MatriculaRegistroRequest(alumno.getId(), anio, true, pago.getFecha());
                    matriculaServicio.actualizarEstadoMatricula(matriculaResp.id(), request);
                    log.info("[procesarMatricula] Matrícula actualizada para alumnoId={} año={}", alumno.getId(), anio);
                    Matricula matriculaEntity = matriculaMapper.toEntity(matriculaResp);
                    detalle.setMatricula(matriculaEntity);
                } else {
                    // Si no existe, se crea la matrícula (esto lo maneja obtenerOMarcarPendiente en MatriculaServicio)
                    MatriculaResponse matriculaResp = matriculaServicio.obtenerOMarcarPendiente(alumno.getId());
                    MatriculaRegistroRequest request = new MatriculaRegistroRequest(alumno.getId(), anio, true, pago.getFecha());
                    matriculaServicio.actualizarEstadoMatricula(matriculaResp.id(), request);
                    log.info("[procesarMatricula] Matrícula creada para alumnoId={} año={}", alumno.getId(), anio);
                    Matricula matriculaEntity = matriculaMapper.toEntity(matriculaResp);
                    detalle.setMatricula(matriculaEntity);
                }
            } catch (NumberFormatException e) {
                log.error("[procesarMatricula] Error al procesar año de matrícula: {}", partes[1]);
            }
        }
    }

    private void procesarMensualidad(Pago pago, DetallePago detalle) {
        log.info("[procesarMensualidad] Procesando mensualidad para detalle id={}", detalle.getId());
        detalle.setImporteInicial(detalle.getValorBase());
        double pendiente = detalle.getValorBase() - detalle.getaCobrar();
        detalle.setImportePendiente(pendiente);

        // Buscar o crear la mensualidad pendiente usando la descripción
        MensualidadResponse mensualidadResp = mensualidadServicio.obtenerOMarcarPendiente(
                pago.getAlumno().getId(), detalle.getDescripcionConcepto());
        // Si se envía mensualidadId en el detalle, se trata de un abono parcial
        // y se procede a actualizar acumulando el aCobrar recibido.
        // Si la mensualidad está en estado PAGADO o con saldo 0, se lanza excepción.
        if (mensualidadResp.estado().equals(EstadoMensualidad.PAGADO.name())
                || mensualidadResp.importePendiente() <= 0.0) {
            throw new IllegalArgumentException("Ya existe una mensualidad registrada para el período indicado");
        }
        // En caso contrario, se actualiza: se suma el aCobrar al monto abonado, se recalcula el importe pendiente y se actualiza el estado.
        Mensualidad mensualidadEntity = mensualidadServicio.toEntity(mensualidadResp);
        // Aquí se podría sumar el valor de aCobrar a un campo acumulador (si se maneja en la entidad),
        // luego se invoca el método que recalcula el importe pendiente.
        // Por simplicidad, asumimos que mensualidadServicio.actualizarAbonoParcial(mensualidadEntity.getId(), detalle.getaCobrar())
        // se encarga de esta lógica.
        mensualidadServicio.actualizarAbonoParcial(mensualidadEntity.getId(), detalle.getaCobrar());
        // Volvemos a obtener la mensualidad actualizada (para mapear sus datos)
        MensualidadResponse mensualidadActualizada = mensualidadServicio.obtenerMensualidad(mensualidadEntity.getId());
        // Convertir a entidad y asignar al detalle
        detalle.setMensualidad(mensualidadServicio.toEntity(mensualidadActualizada));
        log.info("[procesarMensualidad] Mensualidad actualizada para alumnoId={} con descripción={}",
                pago.getAlumno().getId(), detalle.getDescripcionConcepto());
    }

    // Los métodos procesarStock y procesarConcepto se mantienen según la lógica original
    private void procesarStock(DetallePago detalle) {
        log.info("[procesarStock] Procesando detalle id={} para Stock", detalle.getId());

        detalle.setTipo(TipoDetallePago.STOCK);
        detalle.setImporteInicial(detalle.getValorBase());

        double pendiente = detalle.getValorBase() - detalle.getaCobrar();
        detalle.setImportePendiente(pendiente);

        int cantidad = parseCantidad(detalle.getCuotaOCantidad());
        stockServicio.reducirStock(detalle.getDescripcionConcepto(), cantidad);

        log.info("[procesarStock] Stock reducido: {}, cantidad: {}", detalle.getDescripcionConcepto(), cantidad);
    }

    private void procesarConceptoGenerico(DetallePago detalle) {
        double importeInicial = detalle.getValorBase();
        double pendiente = detalle.getValorBase() - detalle.getaCobrar();

        detalle.setImporteInicial(importeInicial);
        detalle.setImportePendiente(pendiente);
        detalle.setTipo(TipoDetallePago.CONCEPTO);
    }

    /**
     * Convierte la cantidad desde una cadena (cuotaOCantidad) a entero, con fallback a 1.
     *
     * @param cuota valor en texto que representa la cantidad
     * @return cantidad parseada o 1 si es inválido
     */
    private int parseCantidad(String cuota) {
        try {
            return Integer.parseInt(cuota.trim());
        } catch (NumberFormatException e) {
            log.warn("Error al parsear cantidad desde '{}'. Usando valor predeterminado 1.", cuota);
            return 1;
        }
    }
}

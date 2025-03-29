package ledance.controladores;

import jakarta.validation.Valid;
import ledance.dto.pago.DetallePagoMapper;
import ledance.dto.pago.request.DetallePagoRegistroRequest;
import ledance.dto.pago.request.PagoMedioRegistroRequest;
import ledance.dto.pago.request.PagoRegistroRequest;
import ledance.dto.pago.response.DetallePagoResponse;
import ledance.dto.pago.response.PagoResponse;
import ledance.dto.cobranza.CobranzaDTO;
import ledance.servicios.detallepago.DetallePagoServicio;
import ledance.servicios.pago.PagoServicio;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/pagos")
@Validated
public class PagoControlador {

    private static final Logger log = LoggerFactory.getLogger(PagoControlador.class);
    private final PagoServicio pagoServicio;
    private final DetallePagoServicio detallePagoServicio;
    private final DetallePagoMapper detallePagoMapper;

    public PagoControlador(PagoServicio pagoServicio, DetallePagoServicio detallePagoServicio, DetallePagoMapper detallePagoMapper) {
        this.pagoServicio = pagoServicio;
        this.detallePagoServicio = detallePagoServicio;
        this.detallePagoMapper = detallePagoMapper;
    }

    @PostMapping
    public ResponseEntity<PagoResponse> registrarPago(@RequestBody @Validated PagoRegistroRequest request) {
        log.info("[PagoControlador] Iniciando registro de pago. Payload recibido: {}", request);
        PagoResponse response = pagoServicio.registrarPago(request);
        log.info("[PagoControlador] Registro de pago finalizado. Respuesta enviada: {}", response);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PutMapping("/{id}")
    public ResponseEntity<PagoResponse> actualizarPago(@PathVariable Long id,
                                                       @RequestBody @Validated PagoRegistroRequest request) {
        log.info("Actualizando pago con id: {}", id);
        PagoResponse response = pagoServicio.actualizarPago(id, request);
        return ResponseEntity.ok(response);
    }

    @PutMapping("/{id}/parcial")
    public ResponseEntity<PagoResponse> actualizarPagoParcial(
            @PathVariable Long id,
            @RequestBody @Validated PagoMedioRegistroRequest request) {
        PagoResponse response = pagoServicio.registrarPagoParcial(id, request.montoAbonado(), request.montosPorDetalle(), request.metodoPagoId());
        return ResponseEntity.ok(response);
    }

    @GetMapping("/{id}")
    public ResponseEntity<PagoResponse> obtenerPagoPorId(@PathVariable Long id) {
        log.info("Obteniendo pago con id: {}", id);
        PagoResponse response = pagoServicio.obtenerPagoPorId(id);
        return ResponseEntity.ok(response);
    }

    @GetMapping
    public ResponseEntity<List<PagoResponse>> listarPagos() {
        log.info("Listando todos los pagos");
        List<PagoResponse> pagos = pagoServicio.listarPagos();
        return pagos.isEmpty() ? ResponseEntity.noContent().build() : ResponseEntity.ok(pagos);
    }

    @GetMapping("/alumno/{alumnoId}")
    public ResponseEntity<List<PagoResponse>> listarPagosPorAlumno(@PathVariable Long alumnoId) {
        log.info("Listando pagos para el alumno con id: {}", alumnoId);
        List<PagoResponse> pagos = pagoServicio.listarPagosPorAlumno(alumnoId);
        return pagos.isEmpty() ? ResponseEntity.noContent().build() : ResponseEntity.ok(pagos);
    }

    @GetMapping("/alumno/{alumnoId}/facturas")
    public ResponseEntity<List<PagoResponse>> listarFacturasPorAlumno(@PathVariable Long alumnoId) {
        log.info("Listando facturas para el alumno con id: {}", alumnoId);
        List<PagoResponse> facturas = pagoServicio.listarPagosPorAlumno(alumnoId)
                .stream()
                .filter(p -> p.observaciones() != null && p.observaciones().contains("FACTURA"))
                .collect(Collectors.toList());
        return facturas.isEmpty() ? ResponseEntity.noContent().build() : ResponseEntity.ok(facturas);
    }

    @GetMapping("/vencidos")
    public ResponseEntity<List<PagoResponse>> listarPagosVencidos() {
        log.info("Listando pagos vencidos");
        List<PagoResponse> pagos = pagoServicio.listarPagosVencidos();
        return pagos.isEmpty() ? ResponseEntity.noContent().build() : ResponseEntity.ok(pagos);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> eliminarPago(@PathVariable Long id) {
        log.info("Eliminando (marcando inactivo) pago con id: {}", id);
        pagoServicio.eliminarPago(id);
        return ResponseEntity.noContent().build();
    }

    @PutMapping("/{id}/quitar-recargo")
    public ResponseEntity<PagoResponse> quitarRecargo(@PathVariable Long id) {
        PagoResponse response = pagoServicio.quitarRecargoManual(id);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/alumno/{alumnoId}/cobranza")
    public ResponseEntity<CobranzaDTO> obtenerCobranzaPorAlumno(@PathVariable Long alumnoId) {
        CobranzaDTO cobranza = pagoServicio.generarCobranzaPorAlumno(alumnoId);
        return ResponseEntity.ok(cobranza);
    }

    @GetMapping("/alumno/{alumnoId}/ultimo")
    public ResponseEntity<PagoResponse> obtenerUltimoPagoPorAlumno(@PathVariable Long alumnoId) {
        log.info("Obteniendo ultimo pago pendiente para alumno id: {}", alumnoId);
        PagoResponse pago = pagoServicio.obtenerUltimoPagoPorAlumno(alumnoId);
        return ResponseEntity.ok(pago);
    }

    /**
     * Endpoint para filtrar detalles de pago.
     * Todos los parametros son opcionales. Si algun parametro no es enviado, se considerara
     * que se requieren todos los registros para ese criterio.
     *
     * @param fechaRegistroDesde (opcional) fecha minima de registro (por ejemplo, fecha de creacion del registro)
     * @param fechaRegistroHasta (opcional) fecha maxima de registro
     * @param detalleConcepto    (opcional) valor o parte del valor del concepto en el DetallePago
     * @param stock              (opcional) tipo de stock; si no se envia, se incluyen todos los tipos
     * @param subConcepto        (opcional) sub concepto, en caso de querer filtrar aun mas el campo concepto
     * @param disciplina         (opcional) tarifa o tipo de disciplina, por ejemplo: CUOTA, MATRICULA, CLASE_SUELTA, CLASE_DE_PRUEBA
     * @return Lista de DetallePagoResponse filtrados segun los parametros enviados.
     */
    @GetMapping("/filtrar")
    public ResponseEntity<List<DetallePagoResponse>> filtrarDetalles(
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
            LocalDate fechaRegistroDesde,

            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
            LocalDate fechaRegistroHasta,

            // Para filtrar por concepto (o nombre completo del concepto)
            @RequestParam(required = false) String detalleConcepto,

            // Para filtrar por stock (se usará el nombre de Stock)
            @RequestParam(required = false) String stock,

            // Para filtrar por sub concepto (subConcepto.descripcion)
            @RequestParam(required = false) String subConcepto,

            // Para filtrar por disciplinas (se usa el campo descripcionConcepto)
            // Si se envía tarifa, se espera que el formato sea: "DISCIPLINA - TARIFA"
            @RequestParam(required = false) String disciplina,

            // Parámetro adicional para el filtro de tarifa
            @RequestParam(required = false) String tarifa
    ) {
        log.info("Filtrando detalles de pago con parametros: fechaRegistroDesde={}, fechaRegistroHasta={}, detalleConcepto={}, stock={}, subConcepto={}, disciplina={}, tarifa={}",
                fechaRegistroDesde, fechaRegistroHasta, detalleConcepto, stock, subConcepto, disciplina, tarifa);

        List<DetallePagoResponse> detalles = detallePagoServicio.filtrarDetalles(
                fechaRegistroDesde,
                fechaRegistroHasta,
                disciplina,
                tarifa,
                stock,
                subConcepto,
                detalleConcepto
        );

        return detalles.isEmpty() ? ResponseEntity.noContent().build() : ResponseEntity.ok(detalles);
    }


    @PostMapping("/verificar")
    public ResponseEntity<Void> verificarMensualidad(@Valid @RequestBody DetallePagoRegistroRequest request) {
        detallePagoServicio.verificarMensualidadNoDuplicada(detallePagoMapper.toEntity(request));
        return ResponseEntity.ok().build();
    }

}

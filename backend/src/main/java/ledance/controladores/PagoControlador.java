package ledance.controladores;

import jakarta.validation.Valid;
import ledance.dto.cobranza.CobranzaDTO;
import ledance.dto.pago.DetallePagoMapper;
import ledance.dto.pago.request.DetallePagoRegistroRequest;
import ledance.dto.pago.request.PagoMedioRegistroRequest;
import ledance.dto.pago.request.PagoRegistroRequest;
import ledance.dto.pago.response.DetallePagoResponse;
import ledance.dto.pago.response.PagoResponse;
import ledance.servicios.detallepago.DetallePagoServicio;
import ledance.servicios.matricula.MatriculaServicio;
import ledance.servicios.pago.PagoServicio;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

@RestController
@RequestMapping("/api/pagos")
@Validated
public class PagoControlador {

    private static final Logger log = LoggerFactory.getLogger(PagoControlador.class);
    private final PagoServicio pagoServicio;
    private final DetallePagoServicio detallePagoServicio;
    private final DetallePagoMapper detallePagoMapper;
    private final MatriculaServicio matriculaServicio;

    public PagoControlador(PagoServicio pagoServicio, DetallePagoServicio detallePagoServicio, DetallePagoMapper detallePagoMapper, MatriculaServicio matriculaServicio) {
        this.pagoServicio = pagoServicio;
        this.detallePagoServicio = detallePagoServicio;
        this.detallePagoMapper = detallePagoMapper;
        this.matriculaServicio = matriculaServicio;
    }

    @GetMapping("/recibo/{pagoId}")
    public ResponseEntity<Resource> descargarRecibo(@PathVariable Long pagoId) {
        try {
            Path pdfPath = Paths.get("/opt/le-dance/pdfs/recibo_" + pagoId + ".pdf");

            if (!Files.exists(pdfPath)) {
                return ResponseEntity.notFound().build();
            }

            ByteArrayResource resource = new ByteArrayResource(Files.readAllBytes(pdfPath));

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_PDF);
            headers.setContentDisposition(
                    ContentDisposition.inline()
                            .filename("recibo_" + pagoId + ".pdf")
                            .build()
            );

            return new ResponseEntity<>(resource, headers, HttpStatus.OK);
        } catch (IOException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
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
            @RequestParam(required = false) String alumnoId,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
            LocalDate fechaRegistroDesde,

            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
            LocalDate fechaRegistroHasta,

            // Filtro para concepto (nombre completo o descripción)
            @RequestParam(required = false) String detalleConcepto,

            // Filtro para stock (se usará el nombre o ID de Stock)
            @RequestParam(required = false) String stock,

            // Filtro para sub concepto (SubConcepto.descripcion)
            @RequestParam(required = false) String subConcepto,

            // Filtro para disciplinas (se usa el campo descripcionConcepto de Mensualidad)
            // Si se envía tarifa, se espera el formato: "DISCIPLINA - TARIFA"
            @RequestParam(required = false) String disciplina,

            // Parámetro adicional para el filtro de tarifa
            @RequestParam(required = false) String tarifa,

            // Parámetro opcional para indicar la categoría directamente
            @RequestParam(required = false) String categoria
    ) {
        log.info("Filtrando detalles de pago con parámetros: alumnoId={}, fechaRegistroDesde={}, fechaRegistroHasta={}, detalleConcepto={}, stock={}, subConcepto={}, disciplina={}, tarifa={}, categoria={}",
                alumnoId, fechaRegistroDesde, fechaRegistroHasta, detalleConcepto, stock, subConcepto, disciplina, tarifa, categoria);

        // Si no se envía explícitamente la categoría, se la infiere según los parámetros disponibles
        if (categoria == null || categoria.trim().isEmpty()) {
            if (stock != null && !stock.trim().isEmpty()) {
                categoria = "STOCK";
            } else if ((subConcepto != null && !subConcepto.trim().isEmpty()) ||
                    (detalleConcepto != null && !detalleConcepto.trim().isEmpty())) {
                categoria = "CONCEPTOS";
            } else if (disciplina != null && !disciplina.trim().isEmpty()) {
                // Si el parámetro 'disciplina' trae también tarifa en formato "DISCIPLINA - TARIFA"
                if (disciplina.contains("-")) {
                    String[] parts = disciplina.split("-");
                    disciplina = parts[0].trim();
                    tarifa = parts[1].trim();
                }
                categoria = "DISCIPLINAS";
            }
        }

        // Llamada al servicio pasando los parámetros en el orden correcto, incluido el alumnoId
        List<DetallePagoResponse> detalles = detallePagoServicio.filtrarDetalles(
                fechaRegistroDesde,
                fechaRegistroHasta,
                categoria,
                disciplina,
                tarifa,
                stock,
                subConcepto,
                detalleConcepto,
                alumnoId
        );

        return ResponseEntity.ok(detalles);
    }

    @PostMapping("/verificar")
    public ResponseEntity<Void> verificarMensualidadOMatricula(@Valid @RequestBody DetallePagoRegistroRequest request) {
        detallePagoServicio.verificarMensualidadNoDuplicada(detallePagoMapper.toEntity(request));
        matriculaServicio.verificarMatriculaNoDuplicada(detallePagoMapper.toEntity(request));

        return ResponseEntity.ok().build();
    }

}

package ledance.controladores;

import jakarta.validation.Valid;
import ledance.dto.pago.request.PagoAnulacionRequest;
import ledance.dto.pago.request.PagoRegistroRequest;
import ledance.dto.pago.response.PagoResponse;
import ledance.entidades.Usuario;
import ledance.infra.configuracion.AppProperties;
import ledance.repositorios.ReciboRepositorio;
import ledance.servicios.pago.PagoServicio;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

@RestController
@RequestMapping("/api/pagos")
public class PagoControlador {
    private final PagoServicio pagos;
    private final ReciboRepositorio recibos;
    private final AppProperties appProperties;

    public PagoControlador(PagoServicio pagos, ReciboRepositorio recibos, AppProperties appProperties) {
        this.pagos = pagos;
        this.recibos = recibos;
        this.appProperties = appProperties;
    }

    @PostMapping
    public ResponseEntity<PagoResponse> registrar(@Valid @RequestBody PagoRegistroRequest request,
                                                  @AuthenticationPrincipal Usuario usuario) {
        return ResponseEntity.status(HttpStatus.CREATED).body(pagos.registrarPago(request, usuario));
    }

    @PostMapping("/{id}/anulacion")
    public PagoResponse anular(@PathVariable Long id,
                               @Valid @RequestBody PagoAnulacionRequest request,
                               @AuthenticationPrincipal Usuario usuario) {
        return pagos.anularPago(id, request, usuario);
    }

    @GetMapping("/{id}")
    public PagoResponse obtener(@PathVariable Long id) {
        return pagos.obtenerPagoPorId(id);
    }

    @GetMapping("/alumno/{alumnoId}")
    public List<PagoResponse> listarPorAlumno(@PathVariable Long alumnoId) {
        return pagos.listarPagosPorAlumno(alumnoId);
    }

    @GetMapping("/recibo/{pagoId}")
    public ResponseEntity<Resource> descargarRecibo(@PathVariable Long pagoId) throws IOException {
        var recibo = recibos.findByPagoId(pagoId).orElse(null);
        if (recibo == null || recibo.getStorageKey() == null) {
            return ResponseEntity.notFound().build();
        }
        Path raiz = appProperties.receiptsPath().toAbsolutePath().normalize();
        Path archivo = raiz.resolve(recibo.getStorageKey()).normalize();
        if (!archivo.startsWith(raiz) || !Files.isRegularFile(archivo)) {
            return ResponseEntity.notFound().build();
        }
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_PDF);
        headers.setContentDisposition(ContentDisposition.inline()
                .filename("recibo_" + pagoId + ".pdf").build());
        return new ResponseEntity<>(new ByteArrayResource(Files.readAllBytes(archivo)), headers, HttpStatus.OK);
    }
}

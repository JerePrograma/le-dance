package ledance.controladores;

import com.lowagie.text.DocumentException;
import ledance.dto.caja.CajaDetalleDTO;
import ledance.dto.caja.CajaDiariaDTO;
import ledance.dto.caja.response.CobranzasDataResponse;
import ledance.servicios.caja.CajaServicio;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/caja")
public class CajaControlador {

    private final CajaServicio cajaServicio;

    public CajaControlador(CajaServicio cajaServicio) {
        this.cajaServicio = cajaServicio;
    }

    /**
     * 1) Planilla general de caja en un rango
     */
    @GetMapping("/planilla")
    public List<CajaDiariaDTO> obtenerPlanilla(
            @RequestParam("startDate")
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate start,

            @RequestParam("endDate")
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate end
    ) {
        return cajaServicio.obtenerPlanillaGeneral(start, end);
    }

    @GetMapping("/dia/{fecha}")
    public CajaDetalleDTO obtenerCajaDia(
            @PathVariable("fecha")
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fecha
    ) {
        return cajaServicio.obtenerCajaDiaria(fecha);
    }

    @GetMapping("/mes")
    public CajaDetalleDTO obtenerCajaMes(
            @RequestParam("startDate")
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate start,

            @RequestParam("endDate")
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate end
    ) {
        // Se espera que start y end delimiten el periodo del mes que se desea consultar.
        return cajaServicio.obtenerCajaMensual(start, end);
    }

    @GetMapping("/datos-unificados")
    public ResponseEntity<CobranzasDataResponse> obtenerDatosCobranzas() {
        CobranzasDataResponse response = cajaServicio.obtenerDatosCobranzas();
        return ResponseEntity.ok(response);
    }

    @GetMapping("/rendicion/imprimir")
    public ResponseEntity<byte[]> imprimirRendicion(
            @RequestParam("startDate")
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate start,
            @RequestParam("endDate")
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate end) throws IOException, DocumentException {

        // Se llama al servicio para generar el PDF; aqui se asume que cajaServicio tiene un metodo que recibe start y end.
        byte[] pdfBytes = cajaServicio.generarRendicionMensualPdf(start, end);

        // Configurar los headers para retornar un PDF descargable.
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_PDF);
        headers.setContentDisposition(
                ContentDisposition.builder("attachment")
                        .filename("rendicion_" + start + "_" + end + ".pdf")
                        .build()
        );

        return new ResponseEntity<>(pdfBytes, headers, HttpStatus.OK);
    }

}

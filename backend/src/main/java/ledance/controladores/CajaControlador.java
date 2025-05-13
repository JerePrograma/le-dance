package ledance.controladores;

import com.lowagie.text.DocumentException;
import ledance.dto.caja.CajaDetalleDTO;
import ledance.dto.caja.CajaDiariaDTO;
import ledance.dto.caja.CajaDiariaImp;
import ledance.dto.caja.CajaPlanillaDTO;
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
     * 1) Planilla general de caja en un rango de fechas
     */
    @GetMapping("/planilla")
    public List<CajaPlanillaDTO> obtenerPlanilla(
            @RequestParam("startDate") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate start,
            @RequestParam("endDate") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate end
    ) {
        return cajaServicio.obtenerPlanillaGeneral(start, end);
    }

    @GetMapping("/dia/{fecha}")
    public CajaDiariaImp obtenerCajaDia(
            @PathVariable("fecha")
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fecha
    ) {
        return cajaServicio.obtenerCajaDiaria(fecha);
    }

    @GetMapping("/dia/{fecha}/imprimir")
    public ResponseEntity<byte[]> imprimirCajaDiaria(
            @PathVariable("fecha")
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fecha
    ) throws DocumentException {
        byte[] pdf = cajaServicio.generarCajaDiariaPdf(
                cajaServicio.obtenerCajaDiaria(fecha)
        );
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_PDF);
        headers.setContentDisposition(
                ContentDisposition.builder("attachment")
                        .filename("caja_" + fecha + ".pdf")
                        .build()
        );
        return new ResponseEntity<>(pdf, headers, HttpStatus.OK);
    }


    @GetMapping("/mes")
    public CajaDetalleDTO obtenerCajaMes(
            @RequestParam("startDate")
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate start,

            @RequestParam("endDate")
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate end
    ) {
        return cajaServicio.obtenerCajaMensual(start, end);
    }

    @GetMapping("/datos-unificados")
    public ResponseEntity<CobranzasDataResponse> obtenerDatosCobranzas() {
        CobranzasDataResponse response = cajaServicio.obtenerDatosCobranzas();
        return ResponseEntity.ok(response);
    }

    @GetMapping("/rendicion/imprimir")
    public ResponseEntity<byte[]> imprimirRendicion(
            @RequestParam("startDate") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate start,
            @RequestParam("endDate") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate end
    ) throws IOException, DocumentException {
        byte[] pdf = cajaServicio.generarRendicionMensualPdf(
                cajaServicio.obtenerCajaRendicionMensual(start, end)
        );
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_PDF);
        headers.setContentDisposition(
                ContentDisposition.builder("attachment")
                        .filename("rendicion_" + start + "_" + end + ".pdf")
                        .build()
        );
        return new ResponseEntity<>(pdf, headers, HttpStatus.OK);
    }

}

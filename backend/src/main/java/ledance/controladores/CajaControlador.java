package ledance.controladores;

import ledance.dto.caja.response.ResumenCajaResponse;
import ledance.servicios.caja.CajaServicio;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;

import java.time.LocalDate;

@RestController
@RequestMapping("/api/caja")
public class CajaControlador {
    private final CajaServicio caja;

    public CajaControlador(CajaServicio caja) {
        this.caja = caja;
    }

    @GetMapping("/resumen")
    public ResumenCajaResponse obtenerResumen(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate desde,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate hasta,
            @PageableDefault(size = 50, sort = {"fecha", "id"}) Pageable pageable) {
        return caja.obtenerResumen(desde, hasta, pageable);
    }
}

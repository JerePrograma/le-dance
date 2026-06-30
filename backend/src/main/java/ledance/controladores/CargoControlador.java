package ledance.controladores;

import jakarta.validation.Valid;
import ledance.dto.cargo.request.CargoConceptoRequest;
import ledance.dto.cargo.response.CargoResponse;
import ledance.dto.PageResponse;
import ledance.servicios.cargo.CargoServicio;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;

import java.util.List;

@RestController
@RequestMapping("/api/cargos")
public class CargoControlador {
    private final CargoServicio cargos;

    public CargoControlador(CargoServicio cargos) {
        this.cargos = cargos;
    }

    @PostMapping("/concepto")
    public ResponseEntity<CargoResponse> crearPorConcepto(@Valid @RequestBody CargoConceptoRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(cargos.crearPorConcepto(request));
    }

    @GetMapping("/{id}")
    public CargoResponse obtener(@PathVariable Long id) {
        return cargos.obtener(id);
    }

    @GetMapping("/alumno/{alumnoId}/pendientes")
    public PageResponse<CargoResponse> listarPendientes(
            @PathVariable Long alumnoId,
            @PageableDefault(size = 50, sort = {"fechaVencimiento", "id"}) Pageable pageable) {
        return PageResponse.from(cargos.listarPendientes(alumnoId, pageable));
    }

    @GetMapping("/vencidos")
    public PageResponse<CargoResponse> listarVencidos(
            @PageableDefault(size = 50, sort = {"fechaVencimiento", "id"}) Pageable pageable) {
        return PageResponse.from(cargos.listarVencidos(pageable));
    }
}

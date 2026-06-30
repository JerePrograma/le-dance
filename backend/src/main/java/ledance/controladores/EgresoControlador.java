package ledance.controladores;

import jakarta.validation.Valid;
import ledance.dto.egreso.request.EgresoAnulacionRequest;
import ledance.dto.egreso.request.EgresoRegistroRequest;
import ledance.dto.egreso.response.EgresoResponse;
import ledance.entidades.Usuario;
import ledance.servicios.egreso.EgresoServicio;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/egresos")
public class EgresoControlador {
    private final EgresoServicio egresos;

    public EgresoControlador(EgresoServicio egresos) {
        this.egresos = egresos;
    }

    @PostMapping
    public EgresoResponse registrar(@Valid @RequestBody EgresoRegistroRequest request,
                                    @AuthenticationPrincipal Usuario usuario) {
        return egresos.agregarEgreso(request, usuario);
    }

    @PostMapping("/{id}/anulacion")
    public EgresoResponse anular(@PathVariable Long id,
                                 @Valid @RequestBody EgresoAnulacionRequest request,
                                 @AuthenticationPrincipal Usuario usuario) {
        return egresos.anular(id, request, usuario);
    }

    @GetMapping("/{id}")
    public EgresoResponse obtener(@PathVariable Long id) {
        return egresos.obtenerEgresoPorId(id);
    }

    @GetMapping
    public List<EgresoResponse> listar() {
        return egresos.listarEgresos();
    }
}

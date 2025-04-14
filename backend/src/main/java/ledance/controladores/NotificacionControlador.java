package ledance.controladores;

import ledance.servicios.notificaciones.NotificacionService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/notificaciones")
@Validated
public class NotificacionControlador {

    private final NotificacionService notificacionService;

    @Autowired
    public NotificacionControlador(NotificacionService notificacionService) {
        this.notificacionService = notificacionService;
    }

    /**
     * Endpoint para obtener las notificaciones de cumpleaños del día (u otros tipos si se desean).
     */
    @GetMapping("/cumpleaneros")
    public ResponseEntity<List<String>> obtenerCumpleanerosDelDia() {
        List<String> notificaciones = notificacionService.generarYObtenerCumpleanerosDelDia();
        return ResponseEntity.ok(notificaciones);
    }

    // Otros endpoints, por ejemplo, para marcar notificaciones como leídas, pueden agregarse aquí.
}


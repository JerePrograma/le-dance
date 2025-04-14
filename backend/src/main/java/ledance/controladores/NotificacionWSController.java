package ledance.controladores;

import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.SendTo;
import org.springframework.stereotype.Controller;

@Controller
public class NotificacionWSController {

    /**
     * Método para recibir mensajes desde el cliente. Por ejemplo, al marcar una notificación como leída.
     */
    @MessageMapping("/notificacion/marcarLeida")
    @SendTo("/topic/notificaciones")
    public String marcarNotificacionLeida(String notificacionId) {
        // Aquí se implementaría la lógica para actualizar la notificación en la BD.
        // Luego se podría devolver una confirmación o actualizar el listado.
        return "Notificación " + notificacionId + " marcada como leída.";
    }
}

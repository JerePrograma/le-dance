package ledance.controladores;

import ledance.servicios.email.EmailService;
import ledance.servicios.rol.RolServicio;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/email")
public class EmailControlador {

    private static final Logger log = LoggerFactory.getLogger(RolServicio.class);

    private final EmailService emailService;

    public EmailControlador(EmailService emailService) {
        this.emailService = emailService;
    }

    /**
     * Endpoint para enviar un correo de prueba.
     * Se debe pasar el destinatario (to) como parámetro de consulta.
     * Ejemplo: POST /api/email/test?to=destinatario@dominio.com
     */
    @PostMapping("/test")
    public ResponseEntity<String> sendTestEmail(@RequestParam String to) {
        try {
            emailService.sendSimpleEmail("administracion@ledance.com.ar", to, "Correo de Prueba", "Este es un correo de prueba enviado desde LeDance.");
            return ResponseEntity.ok("Correo enviado exitosamente a: " + to);
        } catch (Exception e) {
            // Imprime el stacktrace completo en los logs
            log.error("Error al enviar correo", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Error al enviar correo: " + e.getMessage());
        }
    }
}

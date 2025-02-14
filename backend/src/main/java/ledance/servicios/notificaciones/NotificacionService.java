package ledance.servicios.notificaciones;

import ledance.entidades.Pago;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.LocalDate;

@Service
public class NotificacionService implements INotificacionService {

    private static final Logger log = LoggerFactory.getLogger(NotificacionService.class);

    @Override
    public void notificarPagoProximo(Pago pago) {
        // Implementar lógica de notificación (por correo y notificación interna)
        // Por ahora, solo se registra el evento en el log.
        log.info("Notificación: Pago próximo a vencer para inscripcionId: {} (pago id: {})",
                pago.getInscripcion().getId(), pago.getId());
    }

    @Override
    public void notificarPagoVencido(Pago pago) {
        // Lógica de notificación para pagos vencidos
        log.info("Notificación: Pago vencido para inscripcionId: {} (pago id: {})",
                pago.getInscripcion().getId(), pago.getId());
    }

    @Override
    public void notificarIngresoCaja(LocalDate fecha, double totalIngresos) {
        // Lógica para notificar o registrar ingresos diarios
        log.info("Notificación: Ingresos del día {}: ${}", fecha, totalIngresos);
    }
}

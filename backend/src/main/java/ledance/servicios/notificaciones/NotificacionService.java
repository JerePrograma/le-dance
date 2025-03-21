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
        // Implementar logica de notificacion (por correo y notificacion interna)
        // Por ahora, solo se registra el evento en el log.
        log.info("Notificacion: Pago proximo a vencer para inscripcionId: {} (pago id: {})",
                pago.getId(), pago.getId());
    }

    @Override
    public void notificarPagoVencido(Pago pago) {
        // Logica de notificacion para pagos vencidos
        log.info("Notificacion: Pago vencido para inscripcionId: {} (pago id: {})",
                pago.getId(), pago.getId());
    }

    @Override
    public void notificarIngresoCaja(LocalDate fecha, double totalIngresos) {
        // Logica para notificar o registrar ingresos diarios
        log.info("Notificacion: Ingresos del dia {}: ${}", fecha, totalIngresos);
    }
}

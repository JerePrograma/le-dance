package ledance.servicios.notificaciones;

import ledance.entidades.Pago;
import java.time.LocalDate;

public interface INotificacionService {
    void notificarPagoProximo(Pago pago);
    void notificarPagoVencido(Pago pago);
    void notificarIngresoCaja(LocalDate fecha, double totalIngresos);
}

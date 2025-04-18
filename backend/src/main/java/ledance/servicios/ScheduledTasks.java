package ledance.servicios;

import jakarta.mail.MessagingException;
import ledance.servicios.asistencia.AsistenciaMensualServicio;
import ledance.servicios.matricula.MatriculaServicio;
import ledance.servicios.mensualidad.MensualidadServicio;
import ledance.servicios.notificaciones.NotificacionService;
import ledance.servicios.recargo.RecargoServicio;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.List;

@Component
public class ScheduledTasks {

    private final MensualidadServicio mensualidadServicio;
    private final MatriculaServicio matriculaServicio;
    private final RecargoServicio recargoServicio;
    private final AsistenciaMensualServicio asistenciaMensualServicio;
    private final NotificacionService notificacionService;

    @Autowired
    public ScheduledTasks(MensualidadServicio mensualidadServicio,
                          MatriculaServicio matriculaServicio,
                          RecargoServicio recargoServicio,
                          AsistenciaMensualServicio asistenciaMensualServicio,
                          NotificacionService notificacionService) {
        this.mensualidadServicio = mensualidadServicio;
        this.matriculaServicio = matriculaServicio;
        this.recargoServicio = recargoServicio;
        this.asistenciaMensualServicio = asistenciaMensualServicio;
        this.notificacionService = notificacionService;
    }

    /**
     * Genera las mensualidades para el mes vigente
     * Todos los dias 1 a medianoche.
     */
    @Scheduled(cron = "0 0 0 1 * *")
    public void generarMensualidadesMesVigente() {
        mensualidadServicio.generarMensualidadesParaMesVigente();
    }

    /**
     * Genera las matriculas para el año vigente
     * Cada 1 de enero a medianoche.
     */
    @Scheduled(cron = "0 0 0 1 1 *")
    public void generarMatriculasAnioVigente() {
        matriculaServicio.generarMatriculasAnioVigente();
    }

    /**
     * Aplica recargos automaticos
     * Todos los dias a la 1:00AM.
     */
    @Scheduled(cron = "0 0 1 * * *")
    public void aplicarRecargosAutomaticos() {
        recargoServicio.aplicarRecargosAutomaticos();
    }

    /**
     * Crea asistencias detalladas para las inscripciones activas
     * Todos los dias a las 2:00AM.
     */
    @Scheduled(cron = "0 0 2 * * *")
    public void crearAsistenciasParaInscripcionesActivas() {
        asistenciaMensualServicio.crearAsistenciasParaInscripcionesActivasDetallado();
    }

    /**
     * Genera y envia las notificaciones de cumpleaños del dia
     * Todos los dias a las 8:00AM.
     */
    @Scheduled(cron = "0 0 10 * * *")
    public void enviarNotificacionesCumpleanios() {
        try {
            List<String> mensajes = notificacionService.generarYObtenerCumpleanerosDelDia();
            // Si quieres loguear que mensajes se enviaron:
            mensajes.forEach(msg ->
                    System.out.println("[ScheduledTasks] Notificacion enviada: " + msg)
            );
        } catch (IOException | MessagingException e) {
            // Aqui podrias usar tu logger en lugar de printStackTrace
            System.err.println("[ScheduledTasks] Error al enviar notificaciones de cumpleaños:");
            e.printStackTrace();
        }
    }
}

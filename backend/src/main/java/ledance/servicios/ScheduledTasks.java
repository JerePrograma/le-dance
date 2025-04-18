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
     * Todos los días 1 a medianoche.
     */
    @Scheduled(cron = "0 0 0 1 * *")
    public void generarMensualidadesMesVigente() {
        mensualidadServicio.generarMensualidadesParaMesVigente();
    }

    /**
     * Genera las matrículas para el año vigente
     * Cada 1 de enero a medianoche.
     */
    @Scheduled(cron = "0 0 0 1 1 *")
    public void generarMatriculasAnioVigente() {
        matriculaServicio.generarMatriculasAnioVigente();
    }

    /**
     * Aplica recargos automáticos
     * Todos los días a la 1:00 AM.
     */
    @Scheduled(cron = "0 0 1 * * *")
    public void aplicarRecargosAutomaticos() {
        recargoServicio.aplicarRecargosAutomaticos();
    }

    /**
     * Crea asistencias detalladas para las inscripciones activas
     * Todos los días a las 2:00 AM.
     */
    @Scheduled(cron = "0 0 2 * * *")
    public void crearAsistenciasParaInscripcionesActivas() {
        asistenciaMensualServicio.crearAsistenciasParaInscripcionesActivasDetallado();
    }

    /**
     * Genera y envía las notificaciones de cumpleaños del día
     * Todos los días a las 8:00 AM.
     */
    @Scheduled(cron = "0 0 10 * * *")
    public void enviarNotificacionesCumpleanios() {
        try {
            List<String> mensajes = notificacionService.generarYObtenerCumpleanerosDelDia();
            // Si quieres loguear qué mensajes se enviaron:
            mensajes.forEach(msg ->
                    System.out.println("[ScheduledTasks] Notificación enviada: " + msg)
            );
        } catch (IOException | MessagingException e) {
            // Aquí podrías usar tu logger en lugar de printStackTrace
            System.err.println("[ScheduledTasks] Error al enviar notificaciones de cumpleaños:");
            e.printStackTrace();
        }
    }
}

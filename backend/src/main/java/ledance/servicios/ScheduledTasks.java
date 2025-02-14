package ledance.servicios;

import ledance.servicios.asistencia.AsistenciaMensualServicio;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class ScheduledTasks {

    private final AsistenciaMensualServicio asistenciaMensualServicio;

    public ScheduledTasks(AsistenciaMensualServicio asistenciaMensualServicio) {
        this.asistenciaMensualServicio = asistenciaMensualServicio;
    }

    @Scheduled(cron = "0 0 1 1 * ?") // Runs at 00:00 on the 1st day of every month
    public void crearAsistenciasMensuales() {
        asistenciaMensualServicio.crearAsistenciasMensualesAutomaticamente();
    }
}
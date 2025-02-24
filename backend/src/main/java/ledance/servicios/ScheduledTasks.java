package ledance.servicios;

import ledance.entidades.Matricula;
import ledance.repositorios.MatriculaRepositorio;
import ledance.servicios.asistencia.AsistenciaMensualServicio;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Year;
import java.util.List;

@Component
public class ScheduledTasks {

    private static final Logger log = LoggerFactory.getLogger(ScheduledTasks.class);

    private final AsistenciaMensualServicio asistenciaMensualServicio;
    private final MatriculaRepositorio matriculaRepositorio;

    public ScheduledTasks(AsistenciaMensualServicio asistenciaMensualServicio,
                          MatriculaRepositorio matriculaRepositorio) {
        this.asistenciaMensualServicio = asistenciaMensualServicio;
        this.matriculaRepositorio = matriculaRepositorio;
    }

    @Scheduled(cron = "0 0 1 1 * ?")
    public void crearAsistenciasMensuales() {
        log.info("Creando asistencias mensuales...");
        asistenciaMensualServicio.crearAsistenciasMensualesAutomaticamente();
    }

    // Reiniciar las matrículas para el año actual al inicio de cada año
    @Scheduled(cron = "0 0 0 1 1 *")
    public void resetMatriculasForNewYear() {
        int currentYear = Year.now().getValue();
        log.info("Reiniciando matrículas para el año: {}", currentYear);
        // Obtener todas las matrículas
        List<Matricula> matriculas = matriculaRepositorio.findAll();
        for (Matricula m : matriculas) {
            if (m.getAnio() < currentYear) {
                m.setPagada(false);
                m.setAnio(currentYear);
                m.setFechaPago(null);
            }
        }
        matriculaRepositorio.saveAll(matriculas);
        log.info("Reinicio de matrículas completado.");
    }
}

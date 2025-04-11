package ledance.servicios.notificaciones;

import ledance.entidades.Alumno;
import ledance.entidades.Profesor;
import ledance.repositorios.AlumnoRepositorio;
import ledance.repositorios.ProfesorRepositorio;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Service
public class NotificacionService implements INotificacionService {

    private static final Logger log = LoggerFactory.getLogger(NotificacionService.class);

    private final AlumnoRepositorio alumnoRepository;
    private final ProfesorRepositorio profesorRepository;

    public NotificacionService(AlumnoRepositorio alumnoRepository, ProfesorRepositorio profesorRepository) {
        this.alumnoRepository = alumnoRepository;
        this.profesorRepository = profesorRepository;
    }

    /**
     * Consulta y retorna una lista de notificaciones con los nombres de los
     * alumnos y profesores que cumplen años el día de hoy.
     *
     * @return List<String> con la notificación de cumpleaños.
     */
    public List<String> obtenerCumpleanerosDelDia() {
        LocalDate hoy = LocalDate.now();
        int mes = hoy.getMonthValue();
        int dia = hoy.getDayOfMonth();
        List<String> notificaciones = new ArrayList<>();

        // Consultar y filtrar alumnos que cumplen años hoy.
        List<Alumno> alumnos = alumnoRepository.findAll();
        for (Alumno alumno : alumnos) {
            if (alumno.getFechaNacimiento() != null
                    && alumno.getFechaNacimiento().getMonthValue() == mes
                    && alumno.getFechaNacimiento().getDayOfMonth() == dia) {
                notificaciones.add("Alumno: " + alumno.getNombre() + " " + alumno.getApellido());
            }
        }

        // Consultar y filtrar profesores que cumplen años hoy.
        List<Profesor> profesores = profesorRepository.findAll();
        for (Profesor profesor : profesores) {
            if (profesor.getFechaNacimiento() != null
                    && profesor.getFechaNacimiento().getMonthValue() == mes
                    && profesor.getFechaNacimiento().getDayOfMonth() == dia) {
                notificaciones.add("Profesor: " + profesor.getNombre() + " " + profesor.getApellido());
            }
        }

        log.info("Cumpleañeros para el día {}: {}", hoy, notificaciones);
        return notificaciones;
    }
}

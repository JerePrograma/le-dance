package ledance.servicios.notificaciones;

import ledance.entidades.Alumno;
import ledance.entidades.ProcesoEjecutado;
import ledance.entidades.Profesor;
import ledance.repositorios.AlumnoRepositorio;
import ledance.repositorios.ProcesoEjecutadoRepositorio;
import ledance.repositorios.ProfesorRepositorio;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Service
public class NotificacionService implements INotificacionService {

    private static final Logger log = LoggerFactory.getLogger(NotificacionService.class);

    private final AlumnoRepositorio alumnoRepository;
    private final ProfesorRepositorio profesorRepository;
    private final ProcesoEjecutadoRepositorio procesoEjecutadoRepository;

    // Variable para cachear el resultado del día
    private List<String> notificacionesCache = new ArrayList<>();
    // Se guarda la fecha de la última actualización de cache para validar que es la misma
    private LocalDate cacheFechaActual = null;

    public NotificacionService(AlumnoRepositorio alumnoRepository,
                               ProfesorRepositorio profesorRepository,
                               ProcesoEjecutadoRepositorio procesoEjecutadoRepository) {
        this.alumnoRepository = alumnoRepository;
        this.profesorRepository = profesorRepository;
        this.procesoEjecutadoRepository = procesoEjecutadoRepository;
    }

    public List<String> obtenerCumpleanerosDelDia() {
        LocalDate hoy = LocalDate.now();

        // Si ya se cacheó el resultado hoy, se retorna el resultado en memoria.
        if (hoy.equals(cacheFechaActual) && !notificacionesCache.isEmpty()) {
            log.info("Usando notificaciones cacheadas para el día {}", hoy);
            return notificacionesCache;
        }

        // Consultar si el proceso de cumpleaños ya se ejecutó hoy.
        Optional<ProcesoEjecutado> procesoOpt = procesoEjecutadoRepository.findByProceso("CUMPLEANOS");
        if (procesoOpt.isPresent() && hoy.equals(procesoOpt.get().getUltimaEjecucion())) {
            log.info("El proceso de CUMPLEANOS ya se ejecutó hoy según la BD.");
            // En este ejemplo, asumimos que la cache en memoria es la fuente
            // Si quisieras persistir el resultado, deberías almacenar el resultado en la entidad (por ejemplo, en un campo JSON).
            return notificacionesCache;
        }

        // Si no se ha ejecutado hoy, se procesan los cumpleaños
        List<String> notificaciones = new ArrayList<>();
        int mes = hoy.getMonthValue();
        int dia = hoy.getDayOfMonth();

        // Consulta y filtrado de alumnos
        List<Alumno> alumnos = alumnoRepository.findAll();
        for (Alumno alumno : alumnos) {
            if (alumno.getFechaNacimiento() != null &&
                    alumno.getFechaNacimiento().getMonthValue() == mes &&
                    alumno.getFechaNacimiento().getDayOfMonth() == dia) {
                notificaciones.add("Alumno: " + alumno.getNombre() + " " + alumno.getApellido());
            }
        }

        // Consulta y filtrado de profesores
        List<Profesor> profesores = profesorRepository.findAll();
        for (Profesor profesor : profesores) {
            if (profesor.getFechaNacimiento() != null &&
                    profesor.getFechaNacimiento().getMonthValue() == mes &&
                    profesor.getFechaNacimiento().getDayOfMonth() == dia) {
                notificaciones.add("Profesor: " + profesor.getNombre() + " " + profesor.getApellido());
            }
        }

        // Actualiza la variable de cache y la fecha de actualización
        notificacionesCache = notificaciones;
        cacheFechaActual = hoy;

        // Actualizar o crear el registro en la entidad ProcesoEjecutado
        ProcesoEjecutado proceso = procesoOpt.orElse(new ProcesoEjecutado("CUMPLEANOS", hoy));
        proceso.setUltimaEjecucion(hoy);
        procesoEjecutadoRepository.save(proceso);

        log.info("Cumpleañeros procesados para el día {}: {}", hoy, notificaciones);
        return notificaciones;
    }
}

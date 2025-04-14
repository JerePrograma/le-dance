package ledance.servicios.notificaciones;

import ledance.entidades.Alumno;
import ledance.entidades.Notificacion;
import ledance.entidades.Profesor;
import ledance.repositorios.AlumnoRepositorio;
import ledance.repositorios.NotificacionRepositorio;
import ledance.repositorios.ProfesorRepositorio;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Service
public class NotificacionService {

    private final AlumnoRepositorio alumnoRepository;
    private final ProfesorRepositorio profesorRepository;
    private final SimpMessagingTemplate messagingTemplate;
    private final NotificacionRepositorio notificacionRepositorio;

    public NotificacionService(AlumnoRepositorio alumnoRepository,
                               ProfesorRepositorio profesorRepository,
                               SimpMessagingTemplate messagingTemplate, NotificacionRepositorio notificacionRepositorio) {
        this.alumnoRepository = alumnoRepository;
        this.profesorRepository = profesorRepository;
        this.messagingTemplate = messagingTemplate;
        this.notificacionRepositorio = notificacionRepositorio;
    }

    /**
     * Obtiene o calcula y almacena los cumpleaños del día.
     */
    public List<String> generarYObtenerCumpleanerosDelDia() {
        LocalDateTime hoy = LocalDateTime.now();
        // Aquí podrías consultar si ya se han generado notificaciones de cumpleaños
        // en la tabla de Notificacion para evitar duplicados.
        List<Notificacion> notificacionesExistentes = notificacionRepositorio.findByTipoAndFechaCreacion("CUMPLEANOS", hoy);
        if (!notificacionesExistentes.isEmpty()) {
            // Retornar mensajes ya almacenados (formateados)
            return notificacionesExistentes.stream()
                    .map(Notificacion::getMensaje)
                    .toList();
        }

        // Si no existen para hoy, calcular y almacenarlas:
        List<String> mensajes = new ArrayList<>();
        int mes = hoy.getMonthValue();
        int dia = hoy.getDayOfMonth();

        // Buscar alumnos
        List<Alumno> alumnos = alumnoRepository.findAll();
        for (Alumno alumno : alumnos) {
            if (alumno.getFechaNacimiento() != null &&
                    alumno.getFechaNacimiento().getMonthValue() == mes &&
                    alumno.getFechaNacimiento().getDayOfMonth() == dia) {
                mensajes.add("Alumno: " + alumno.getNombre() + " " + alumno.getApellido());
            }
        }

        // Buscar profesores
        List<Profesor> profesores = profesorRepository.findAll();
        for (Profesor profesor : profesores) {
            if (profesor.getFechaNacimiento() != null &&
                    profesor.getFechaNacimiento().getMonthValue() == mes &&
                    profesor.getFechaNacimiento().getDayOfMonth() == dia) {
                mensajes.add("Profesor: " + profesor.getNombre() + " " + profesor.getApellido());
            }
        }

        mensajes.forEach(mensaje -> {
            Notificacion noti = new Notificacion();
            // Asumir que para cumpleaños se envía a todos los usuarios o a un usuario en particular;
            // en escenarios reales, se tendría que determinar el usuario destinatario.
            noti.setUsuarioId(1L);
            noti.setTipo("CUMPLEANOS");
            noti.setMensaje(mensaje);
            noti.setFechaCreacion(LocalDateTime.now());
            noti.setLeida(false);
            notificacionRepositorio.save(noti);
        });

        // (Opcional) Enviar vía WebSocket para notificaciones en tiempo real.
        messagingTemplate.convertAndSend("/topic/notificaciones", mensajes);

        return mensajes;
    }

}

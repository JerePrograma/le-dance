package ledance.servicios.notificaciones;

import jakarta.mail.MessagingException;
import ledance.entidades.Alumno;
import ledance.entidades.Notificacion;
import ledance.entidades.ProcesoEjecutado;
import ledance.entidades.Profesor;
import ledance.repositorios.AlumnoRepositorio;
import ledance.repositorios.NotificacionRepositorio;
import ledance.repositorios.ProcesoEjecutadoRepositorio;
import ledance.repositorios.ProfesorRepositorio;
import ledance.servicios.email.EmailAsyncService;
import ledance.servicios.email.IEmailService;
import org.springframework.core.env.Environment;
import org.springframework.core.env.Profiles;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Service
public class NotificacionService {

    private static final String PROCESO = "CUMPLEANOS";

    private final AlumnoRepositorio alumnoRepository;
    private final ProfesorRepositorio profesorRepository;
    private final NotificacionRepositorio notificacionRepositorio;
    private final ProcesoEjecutadoRepositorio procesoEjecutadoRepositorio;
    private final SimpMessagingTemplate messagingTemplate;
    private final Environment env;
    private final EmailAsyncService emailAsyncService;

    public NotificacionService(AlumnoRepositorio alumnoRepository,
                               ProfesorRepositorio profesorRepository,
                               NotificacionRepositorio notificacionRepositorio,
                               ProcesoEjecutadoRepositorio procesoEjecutadoRepositorio,
                               IEmailService emailService,              // <--- inyecta la interfaz
                               SimpMessagingTemplate messagingTemplate, Environment env, EmailAsyncService emailAsyncService) {
        this.alumnoRepository = alumnoRepository;
        this.profesorRepository = profesorRepository;
        this.notificacionRepositorio = notificacionRepositorio;
        this.procesoEjecutadoRepositorio = procesoEjecutadoRepositorio;
        this.messagingTemplate = messagingTemplate;
        this.env = env;
        this.emailAsyncService = emailAsyncService;
    }

    /**
     * Genera y retorna los mensajes de cumpleaños del día,
     * persiste notificaciones en la base y dispara el envío de
     * e-mails de forma asíncrona (solo en producción).
     */
    public List<String> generarYObtenerCumpleanerosDelDia() throws IOException {
        LocalDateTime ahora = LocalDateTime.now();
        LocalDate hoy = ahora.toLocalDate();
        LocalDateTime inicioDelDia = hoy.atStartOfDay();
        LocalDateTime inicioDelSiguienteDia = inicioDelDia.plusDays(1);

        // Si ya se ejecutó hoy, devolvemos solo los mensajes persistidos
        Optional<ProcesoEjecutado> procOpt =
                procesoEjecutadoRepositorio.findByProceso(PROCESO);
        if (procOpt.isPresent()
                && procOpt.get().getUltimaEjecucion().isEqual(hoy)) {
            return notificacionRepositorio
                    .findByTipoAndFechaCreacionBetween(
                            PROCESO, inicioDelDia, inicioDelSiguienteDia)
                    .stream()
                    .map(Notificacion::getMensaje)
                    .toList();
        }

        List<String> mensajes = new ArrayList<>();
        boolean isBisiesto = hoy.isLeapYear();
        boolean isProd = env.acceptsProfiles(Profiles.of("prod"));

        // Leemos la firma solo una vez si vamos a enviar mails
        byte[] firmaBytes = null;
        if (isProd) {
            String baseDir = System.getenv("LEDANCE_HOME");
            if (baseDir == null || baseDir.isBlank()) {
                throw new IllegalStateException(
                        "Variable de entorno LEDANCE_HOME no definida");
            }
            Path firmaPath = Paths.get(baseDir, "imgs",
                    "firma_mesa-de-trabajo-1.png");
            firmaBytes = Files.readAllBytes(firmaPath);
        }

        // Alumnos
        for (Alumno alumno : alumnoRepository.findAll()) {
            LocalDate nacimiento = alumno.getFechaNacimiento();
            if (nacimiento != null
                    && cumpleHoy(nacimiento, hoy, isBisiesto)) {

                String texto = "Alumno: "
                        + alumno.getNombre() + " "
                        + alumno.getApellido();
                mensajes.add(texto);

                Notificacion noti = new Notificacion();
                noti.setUsuarioId(1L);
                noti.setTipo(PROCESO);
                noti.setMensaje(texto);
                noti.setFechaCreacion(ahora);
                noti.setLeida(false);
                notificacionRepositorio.save(noti);

                // Disparamos el mail en background
                if (isProd && StringUtils.hasText(alumno.getEmail())) {
                    emailAsyncService.enviarMailCumple(
                            alumno, firmaBytes);
                }
            }
        }

        // Profesores (sin envío de mail)
        for (Profesor prof : profesorRepository.findAll()) {
            LocalDate nacimiento = prof.getFechaNacimiento();
            if (nacimiento != null
                    && cumpleHoy(nacimiento, hoy, isBisiesto)) {

                String texto = "Profesor: "
                        + prof.getNombre() + " "
                        + prof.getApellido();
                mensajes.add(texto);

                Notificacion noti = new Notificacion();
                noti.setUsuarioId(1L);
                noti.setTipo(PROCESO);
                noti.setMensaje(texto);
                noti.setFechaCreacion(ahora);
                noti.setLeida(false);
                notificacionRepositorio.save(noti);
            }
        }

        // Marcamos que ya ejecutamos hoy
        if (procOpt.isPresent()) {
            ProcesoEjecutado ejecutado = procOpt.get();
            ejecutado.setUltimaEjecucion(hoy);
            procesoEjecutadoRepositorio.save(ejecutado);
        } else {
            procesoEjecutadoRepositorio
                    .save(new ProcesoEjecutado(PROCESO, hoy));
        }

        // Emitimos por WebSocket
        messagingTemplate
                .convertAndSend("/topic/notificaciones", mensajes);

        return mensajes;
    }

    private boolean cumpleHoy(LocalDate fechaNacimiento,
                              LocalDate hoy,
                              boolean isBisiesto) {
        int mesNac = fechaNacimiento.getMonthValue();
        int diaNac = fechaNacimiento.getDayOfMonth();
        return (mesNac == hoy.getMonthValue()
                && diaNac == hoy.getDayOfMonth())
                || (!isBisiesto
                && mesNac == 2 && diaNac == 29
                && hoy.getMonthValue() == 2
                && hoy.getDayOfMonth() == 28);
    }

}

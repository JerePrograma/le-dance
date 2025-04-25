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
    private final IEmailService emailService;               // <-- tipo interfaz
    private final SimpMessagingTemplate messagingTemplate;
    private final Environment env;

    public NotificacionService(AlumnoRepositorio alumnoRepository,
                               ProfesorRepositorio profesorRepository,
                               NotificacionRepositorio notificacionRepositorio,
                               ProcesoEjecutadoRepositorio procesoEjecutadoRepositorio,
                               IEmailService emailService,              // <--- inyecta la interfaz
                               SimpMessagingTemplate messagingTemplate, Environment env) {
        this.alumnoRepository = alumnoRepository;
        this.profesorRepository = profesorRepository;
        this.notificacionRepositorio = notificacionRepositorio;
        this.procesoEjecutadoRepositorio = procesoEjecutadoRepositorio;
        this.emailService = emailService;
        this.messagingTemplate = messagingTemplate;
        this.env = env;
    }

    /**
     * Genera y retorna los mensajes de cumpleaños del dia, persiste notificaciones y envia emails
     * solo si aun no se ha ejecutado hoy.
     */
    public List<String> generarYObtenerCumpleanerosDelDia() throws MessagingException, IOException {
        LocalDateTime ahora = LocalDateTime.now();
        LocalDate hoy = ahora.toLocalDate();
        LocalDateTime inicioDelDia = hoy.atStartOfDay();
        LocalDateTime inicioDelSiguienteDia = inicioDelDia.plusDays(1);

        // 1) Verificar si ya se ejecuto hoy
        Optional<ProcesoEjecutado> procOpt = procesoEjecutadoRepositorio.findByProceso(PROCESO);
        if (procOpt.isPresent() && procOpt.get().getUltimaEjecucion().isEqual(hoy)) {
            // Ya corrio hoy: recuperamos todas las notificaciones de hoy (cualquier hora)
            return notificacionRepositorio
                    .findByTipoAndFechaCreacionBetween(PROCESO, inicioDelDia, inicioDelSiguienteDia)
                    .stream()
                    .map(Notificacion::getMensaje)
                    .toList();
        }

        List<String> mensajes = new ArrayList<>();

        int mes = hoy.getMonthValue();
        int dia = hoy.getDayOfMonth();

        // ¿Estamos corriendo en prod?
        boolean isProd = env.acceptsProfiles(Profiles.of("prod"));
        // 3) Procesar alumnos
        for (Alumno alumno : alumnoRepository.findAll()) {
            if (alumno.getFechaNacimiento() != null
                    && alumno.getFechaNacimiento().getMonthValue() == mes
                    && alumno.getFechaNacimiento().getDayOfMonth() == dia) {

                String texto = "Alumno: " + alumno.getNombre() + " " + alumno.getApellido();
                mensajes.add(texto);

                // Persistir notificacion
                Notificacion noti = new Notificacion();
                noti.setUsuarioId(1L);
                noti.setTipo(PROCESO);
                noti.setMensaje(texto);
                noti.setFechaCreacion(ahora);
                noti.setLeida(false);
                notificacionRepositorio.save(noti);

                // Enviar email con inline image

                // envío e-mail **solo** si estoy en prod y el alumno tiene e-mail
                if (isProd && StringUtils.hasText(alumno.getEmail())) {
                    // 2) Leer firma
                    String baseDir = System.getenv("LEDANCE_HOME");
                    if (baseDir == null || baseDir.isBlank()) {
                        throw new IllegalStateException("Variable de entorno LEDANCE_HOME no definida");
                    }
                    Path firmaPath = Paths.get(baseDir, "imgs", "firma_mesa-de-trabajo-1.png");
                    byte[] firmaBytes = Files.readAllBytes(firmaPath);
                    if (StringUtils.hasText(alumno.getEmail())) {
                        String subject = "¡Feliz Cumpleaños, " + alumno.getNombre() + "!";
                        String htmlBody =
                                "<p>FELICIDADES <strong>" + alumno.getNombre() + "</strong></p>"
                                        + "<p>De parte de todo el Staff de LE DANCE arte escuela, te deseamos un "
                                        + "<strong>MUY FELIZ CUMPLEAÑOS!</strong></p>"
                                        + "<p>Katia, Anto y Nati te desean un nuevo año lleno de deseos por cumplir!</p>"
                                        + "<p>Te adoramos.</p>"
                                        + "<img src='cid:signature' alt='Firma' style='max-width:200px;'/>";

                        emailService.sendEmailWithInlineImage(
                                "administracion@ledance.com.ar",
                                alumno.getEmail(),
                                subject,
                                htmlBody,
                                firmaBytes,
                                "signature",
                                "image/png"
                        );
                    }
                }
            }
        }

        // 4) Procesar profesores (sin envio de email)
        for (Profesor prof : profesorRepository.findAll()) {
            if (prof.getFechaNacimiento() != null
                    && prof.getFechaNacimiento().getMonthValue() == mes
                    && prof.getFechaNacimiento().getDayOfMonth() == dia) {

                String texto = "Profesor: " + prof.getNombre() + " " + prof.getApellido();
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

        // 5) Marcar ejecucion del proceso
        if (procOpt.isPresent()) {
            ProcesoEjecutado ejecutado = procOpt.get();
            ejecutado.setUltimaEjecucion(hoy);
            procesoEjecutadoRepositorio.save(ejecutado);
        } else {
            ProcesoEjecutado ejecutado = new ProcesoEjecutado(PROCESO, hoy);
            procesoEjecutadoRepositorio.save(ejecutado);
        }

        // 6) Enviar por WebSocket
        messagingTemplate.convertAndSend("/topic/notificaciones", mensajes);

        return mensajes;
    }
}

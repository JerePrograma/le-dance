package ledance.servicios.email;

import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Service
public class EmailService {

    private final JavaMailSender mailSender;

    public EmailService(JavaMailSender mailSender) {
        this.mailSender = mailSender;
    }

    /**
     * Envía un correo de texto simple.
     *
     * @param from    Remitente (debe ser el mismo que configuraste o uno verificado)
     * @param to      Destinatario
     * @param subject Asunto del email
     * @param text    Cuerpo del mensaje
     */
    public void sendSimpleEmail(String from, String to, String subject, String text) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(from); // Opcional, si no se establece, se usa el username configurado.
        message.setTo(to);
        message.setSubject(subject);
        message.setText(text);
        mailSender.send(message);
    }
}

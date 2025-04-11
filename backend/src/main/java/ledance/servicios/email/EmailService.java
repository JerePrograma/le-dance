package ledance.servicios.email;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

@Service
public class EmailService {

    private final JavaMailSender mailSender;

    public EmailService(JavaMailSender mailSender) {
        this.mailSender = mailSender;
    }

    /**
     * Envía un correo con adjunto y recurso inline utilizando MimeMessage.
     *
     * @param from               Remitente (debe estar configurado o verificado).
     * @param to                 Destinatario.
     * @param subject            Asunto del email.
     * @param htmlText           Cuerpo del mensaje en HTML.
     * @param attachmentData     Datos del archivo adjunto en arreglo de bytes.
     * @param attachmentFilename Nombre del archivo adjunto.
     * @param inlineData         Datos del recurso inline (firma) en arreglo de bytes.
     * @param contentId          Identificador único para referenciar el recurso inline en el HTML.
     * @param inlineMimeType     Tipo MIME del recurso inline (por ejemplo, "image/png").
     * @throws MessagingException En caso de error al preparar o enviar el email.
     */
    public void sendEmailWithAttachmentAndInlineImage(String from, String to, String subject, String htmlText,
                                                      byte[] attachmentData, String attachmentFilename,
                                                      byte[] inlineData, String contentId, String inlineMimeType)
            throws MessagingException {
        MimeMessage message = mailSender.createMimeMessage();
        // Se indica true para multipart
        MimeMessageHelper helper = new MimeMessageHelper(message, true);
        helper.setFrom(from);
        helper.setTo(to);
        helper.setSubject(subject);
        // Se envía el contenido en HTML
        helper.setText(htmlText, true);
        // Agregar el archivo adjunto
        helper.addAttachment(attachmentFilename, new ByteArrayResource(attachmentData));
        // Agregar la imagen inline identificada por el contentId
        helper.addInline(contentId, new ByteArrayResource(inlineData), inlineMimeType);
        mailSender.send(message);
    }

    /**
     * Envía un correo de texto simple.
     *
     * @param from    Remitente (debe ser el mismo que configuraste o uno verificado).
     * @param to      Destinatario.
     * @param subject Asunto del email.
     * @param text    Cuerpo del mensaje.
     */
    public void sendSimpleEmail(String from, String to, String subject, String text) {

    }
}

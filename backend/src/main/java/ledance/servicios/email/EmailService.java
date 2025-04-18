package ledance.servicios.email;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

/**
 * Servicio de envio de emails.
 */
@Service
public class EmailService {

    private final JavaMailSender mailSender;

    public EmailService(JavaMailSender mailSender) {
        this.mailSender = mailSender;
    }

    /**
     * Envia un correo HTML con un recurso inline (p.ej. firma).
     *
     * @param from           Remitente.
     * @param to             Destinatario.
     * @param subject        Asunto.
     * @param htmlText       Cuerpo en HTML (debe referenciar la imagen con cid:contentId).
     * @param inlineData     Bytes del recurso inline.
     * @param contentId      Identificador para el cid.
     * @param inlineMimeType Tipo MIME del recurso (p.ej. "image/png").
     * @throws MessagingException si hay fallo al armar o enviar el mensaje.
     */
    public void sendEmailWithInlineImage(String from,
                                         String to,
                                         String subject,
                                         String htmlText,
                                         byte[] inlineData,
                                         String contentId,
                                         String inlineMimeType)
            throws MessagingException {
        MimeMessage message = mailSender.createMimeMessage();
        // multipart=true para poder incluir inline
        MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
        helper.setFrom(from);
        helper.setTo(to);
        helper.setSubject(subject);
        helper.setText(htmlText, true);
        helper.addInline(contentId, new ByteArrayResource(inlineData), inlineMimeType);
        mailSender.send(message);
    }

    /**
     * (Tu metodo existente para adjuntos + inline)
     */
    public void sendEmailWithAttachmentAndInlineImage(String from,
                                                      String to,
                                                      String subject,
                                                      String htmlText,
                                                      byte[] attachmentData,
                                                      String attachmentFilename,
                                                      byte[] inlineData,
                                                      String contentId,
                                                      String inlineMimeType)
            throws MessagingException {
        MimeMessage message = mailSender.createMimeMessage();
        MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
        helper.setFrom(from);
        helper.setTo(to);
        helper.setSubject(subject);
        helper.setText(htmlText, true);
        helper.addAttachment(attachmentFilename, new ByteArrayResource(attachmentData));
        helper.addInline(contentId, new ByteArrayResource(inlineData), inlineMimeType);
        mailSender.send(message);
    }
}

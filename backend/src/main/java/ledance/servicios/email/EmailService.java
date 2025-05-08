package ledance.servicios.email;

import jakarta.annotation.PostConstruct;
import jakarta.mail.*;
import jakarta.mail.internet.MimeMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

import java.util.Properties;

/**
 * Servicio de envío de emails que además guarda cada mensaje en la carpeta Sent vía IMAP.
 */
@Service
public class EmailService implements IEmailService {

    private static final Logger log = LoggerFactory.getLogger(EmailService.class);

    private final JavaMailSender mailSender;
    private final String imapHost;
    private final int imapPort;
    private final String username;
    private final String password;

    public EmailService(JavaMailSender mailSender,
                        @Value("${spring.mail.imap.host:${spring.mail.host}}") String imapHost,
                        @Value("${spring.mail.imap.port:993}") int imapPort,
                        @Value("${spring.mail.username}") String username,
                        @Value("${spring.mail.password}") String password) {
        this.mailSender = mailSender;
        this.imapHost = imapHost;
        this.imapPort = imapPort;
        this.username = username;
        this.password = password;
    }

    @Override
    public void sendEmailWithInlineImage(String from,
                                         String to,
                                         String subject,
                                         String htmlText,
                                         byte[] inlineData,
                                         String contentId,
                                         String inlineMimeType) throws MessagingException {
        MimeMessage message = mailSender.createMimeMessage();
        MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

        helper.setFrom(from);
        helper.setTo(to);
        helper.setSubject(subject);
        helper.setText(htmlText, true);
        helper.addInline(contentId, new ByteArrayResource(inlineData), inlineMimeType);

        // 1) Enviamos por SMTP
        mailSender.send(message);

        // 2) Guardamos copia en Sent vía IMAP
        saveToSent(message);
    }

    @Override
    public void sendEmailWithAttachmentAndInlineImage(String from,
                                                      String to,
                                                      String subject,
                                                      String htmlText,
                                                      byte[] attachmentData,
                                                      String attachmentFilename,
                                                      byte[] inlineData,
                                                      String contentId,
                                                      String inlineMimeType) throws MessagingException {
        MimeMessage message = mailSender.createMimeMessage();
        MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

        helper.setFrom(from);
        helper.setTo(to);
        helper.setSubject(subject);
        helper.setText(htmlText, true);
        helper.addAttachment(attachmentFilename, new ByteArrayResource(attachmentData));
        helper.addInline(contentId, new ByteArrayResource(inlineData), inlineMimeType);

        // 1) Enviamos por SMTP
        mailSender.send(message);

        // 2) Guardamos copia en Sent vía IMAP
        saveToSent(message);
    }

    private void saveToSent(MimeMessage message) {
        try {
            Properties props = new Properties();
            props.put("mail.store.protocol", "imap");
            props.put("mail.imap.host", imapHost);
            props.put("mail.imap.port", String.valueOf(imapPort));
            props.put("mail.imap.starttls.enable", "true");
            // si usas SSL en vez de TLS:
            // props.put("mail.imap.ssl.enable", "true");

            Session session = Session.getInstance(props);
            Store store = session.getStore("imap");
            store.connect(imapHost, imapPort, username, password);

            Folder sentFolder = store.getFolder("Sent");
            if (!sentFolder.exists()) {
                // crea la carpeta si no existe
                sentFolder.create(Folder.HOLDS_MESSAGES);
            }
            sentFolder.open(Folder.READ_WRITE);
            sentFolder.appendMessages(new Message[]{message});
            sentFolder.close(false);
            store.close();

        } catch (MessagingException e) {
            log.error("No se pudo guardar el mensaje en Sent vía IMAP");
        }
    }
}

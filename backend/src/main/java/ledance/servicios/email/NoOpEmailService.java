package ledance.servicios.email;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;

@Service
@Profile({"!prod"})
public class NoOpEmailService implements IEmailService {
    private final Logger log = LoggerFactory.getLogger(NoOpEmailService.class);

    @Override
    public void sendEmailWithInlineImage(String from, String to, String subject, String htmlText, byte[] inlineData, String contentId, String inlineMimeType) {
        log.info("[MAIL SKIPPED en dev] to={} subject={}", to, subject);
    }

    @Override
    public void sendEmailWithAttachmentAndInlineImage(String from, String to, String subject, String htmlText, byte[] attachmentData, String attachmentFilename, byte[] inlineData, String contentId, String inlineMimeType) {
        log.info("[MAIL SKIPPED en dev] to={} subject={}", to, subject);
    }
}

package ledance.servicios.pdfs;

import ledance.entidades.Pago;
import ledance.util.FilePathResolver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

@Service
public class ReciboStorageService {

    private static final Logger LOGGER = LoggerFactory.getLogger(ReciboStorageService.class);

    private final PdfService pdfService;

    // Se inyecta PdfService (que a su vez usa EmailService para enviar el email)
    public ReciboStorageService(PdfService pdfService) {
        this.pdfService = pdfService;
    }

    /**
     * Genera, almacena y envía el recibo en PDF por email al alumno.
     * Este método se usará al registrar un pago por primera vez.
     *
     * @param pago Objeto Pago con todos sus detalles.
     */
    public void generarYAlmacenarYEnviarRecibo(Pago pago) {
        try {
            // 1. Generar el PDF del recibo.
            byte[] pdfBytes = pdfService.generarReciboPdf(pago);

            // 2. Almacenar el PDF en disco en la ruta configurada.
            String fileName = "recibo_" + pago.getId() + ".pdf";
            Path outputPath = FilePathResolver.of("pdfs", fileName);
            Files.createDirectories(outputPath.getParent());
            Files.write(outputPath, pdfBytes);

            pdfService.generarYEnviarReciboEmail(pago);
            LOGGER.info("Recibo enviado por email al alumno: {}", pago.getAlumno().getEmail());
        } catch (IOException e) {
            LOGGER.error("Error al almacenar el recibo: ", e);
        } catch (Exception e) {
            LOGGER.error("Error al generar y enviar el recibo: ", e);
        }
    }
}

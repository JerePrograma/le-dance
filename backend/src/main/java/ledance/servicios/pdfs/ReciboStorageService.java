package ledance.servicios.pdfs;

import ledance.entidades.Pago;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

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
            Path outputPath = Paths.get("/opt/le-dance/pdfs", fileName);
            Files.createDirectories(outputPath.getParent());
            Files.write(outputPath, pdfBytes);
            LOGGER.info("Recibo generado y almacenado en: {}", outputPath);

            pdfService.generarYEnviarReciboEmail(pago);
            LOGGER.info("Recibo enviado por email al alumno: {}", pago.getAlumno().getEmail1());
        } catch (IOException e) {
            LOGGER.error("Error al almacenar el recibo: ", e);
        } catch (Exception e) {
            LOGGER.error("Error al generar y enviar el recibo: ", e);
        }
    }

    /**
     * Método para generar y almacenar un recibo a partir de un pago histórico.
     * (Si se necesita en otro flujo, sin envío de email)
     *
     * @param pagoHistorico Objeto Pago histórico.
     */
    public void generarYAlmacenarReciboDesdePagoHistorico(Pago pagoHistorico) {
        try {
            byte[] pdfBytes = pdfService.generarReciboPdf(pagoHistorico);
            String fileName = "recibo_" + pagoHistorico.getId() + ".pdf";
            Path outputPath = Paths.get("/opt/le-dance/pdfs", fileName);
            Files.createDirectories(outputPath.getParent());
            Files.write(outputPath, pdfBytes);
            LOGGER.info("Recibo generado con detalles históricos y almacenado como: {}", outputPath);
        } catch (IOException e) {
            LOGGER.error("Error al generar y almacenar el recibo desde histórico: ", e);
        }
    }
}

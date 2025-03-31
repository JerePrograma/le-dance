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

    public ReciboStorageService(PdfService pdfService) {
        this.pdfService = pdfService;
    }

    public void generarYAlmacenarRecibo(Pago pago) {
        try {
            byte[] pdfBytes = pdfService.generarReciboPdf(pago);

            // Ruta donde se guardará el PDF en tu VPS
            String fileName = "recibo_" + pago.getId() + ".pdf";
            Path outputPath = Paths.get("/opt/le-dance/pdfs", fileName);

            // Crear directorios si no existen
            Files.createDirectories(outputPath.getParent());

            // Escribir en disco
            Files.write(outputPath, pdfBytes);

            LOGGER.info("Recibo generado y almacenado en: {}", outputPath.toString());

            // (Opcional) Puedes guardar en la base de datos la ruta del archivo
            // pago.setRutaReciboPdf(outputPath.toString());
            // pagoRepository.save(pago);

        } catch (IOException e) {
            LOGGER.error("Error al generar y almacenar el recibo: ", e);
        }
    }

    public void generarYAlmacenarReciboDesdePagoHistorico(Pago pagoHistorico) {
        try {
            byte[] pdfBytes = pdfService.generarReciboPdf(pagoHistorico); // Usamos el histórico

            String fileName = "recibo_" + pagoHistorico.getId() + ".pdf";
            Path outputPath = Paths.get("/opt/le-dance/pdfs", fileName);

            Files.createDirectories(outputPath.getParent());
            Files.write(outputPath, pdfBytes);

            LOGGER.info("Recibo generado con detalles históricos y almacenado como: {}", outputPath.toString());
        } catch (IOException e) {
            LOGGER.error("Error al generar y almacenar el recibo desde histórico: ", e);
        }
    }

}

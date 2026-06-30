package ledance.servicios.pdfs;

import ledance.entidades.EstadoRecibo;
import ledance.entidades.EstadoReciboPendiente;
import ledance.entidades.Recibo;
import ledance.entidades.ReciboPendiente;
import ledance.infra.configuracion.AppProperties;
import ledance.repositorios.AplicacionPagoRepositorio;
import ledance.repositorios.ReciboPendienteRepositorio;
import ledance.repositorios.ReciboRepositorio;
import ledance.servicios.email.IEmailService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.data.domain.PageRequest;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Clock;
import java.time.Duration;
import java.util.List;

@Service
@ConditionalOnProperty(name = "app.scheduling-enabled", havingValue = "true")
public class ReciboStorageService {
    private static final Logger log = LoggerFactory.getLogger(ReciboStorageService.class);
    private static final int MAX_INTENTOS = 5;
    private final PdfService pdf;
    private final IEmailService email;
    private final AppProperties properties;
    private final ReciboPendienteRepositorio pendientes;
    private final ReciboRepositorio recibos;
    private final AplicacionPagoRepositorio aplicaciones;
    private final Clock clock;

    public ReciboStorageService(PdfService pdf,
                                IEmailService email,
                                AppProperties properties,
                                ReciboPendienteRepositorio pendientes,
                                ReciboRepositorio recibos,
                                AplicacionPagoRepositorio aplicaciones,
                                Clock clock) {
        this.pdf = pdf;
        this.email = email;
        this.properties = properties;
        this.pendientes = pendientes;
        this.recibos = recibos;
        this.aplicaciones = aplicaciones;
        this.clock = clock;
    }

    @Scheduled(fixedDelayString = "${app.receipts.worker-delay-ms:30000}")
    @Transactional
    public void procesarPendientes() {
        List<ReciboPendiente> trabajos = pendientes.findPendientesForUpdate(
                List.of(EstadoReciboPendiente.PENDIENTE), clock.instant(), PageRequest.of(0, 10));
        trabajos.forEach(this::procesar);
    }

    private void procesar(ReciboPendiente trabajo) {
        Recibo recibo = recibos.findByPagoId(trabajo.getPago().getId())
                .orElseThrow(() -> new IllegalStateException("Outbox sin recibo"));
        trabajo.setEstado(EstadoReciboPendiente.PROCESANDO);
        trabajo.setIntentos(trabajo.getIntentos() + 1);
        recibo.setIntentos(recibo.getIntentos() + 1);
        try {
            String nombre = "recibo_" + trabajo.getPago().getId() + ".pdf";
            Path raiz = properties.receiptsPath().toAbsolutePath().normalize();
            Path destino = raiz.resolve(nombre).normalize();
            if (!destino.startsWith(raiz)) {
                throw new IOException("Ruta de recibo inválida");
            }
            Files.createDirectories(raiz);
            byte[] bytes = pdf.generarReciboPdf(trabajo.getPago());
            Files.write(destino, bytes);
            recibo.setStorageKey(nombre);
            recibo.setGeneradoAt(clock.instant());
            recibo.setEstado(EstadoRecibo.GENERADO);

            String destinatario = trabajo.getPago().getAlumno().getEmail();
            if (destinatario != null && !destinatario.isBlank()) {
                email.sendEmailWithAttachmentAndInlineImage(
                        "administracion@ledance.com.ar",
                        destinatario,
                        "Recibo de pago",
                        cuerpo(trabajo),
                        bytes,
                        nombre,
                        firma(),
                        "signature",
                        "image/png");
                recibo.setEstado(EstadoRecibo.ENVIADO);
                recibo.setEnviadoAt(clock.instant());
            }
            recibo.setUltimoError(null);
            trabajo.setEstado(EstadoReciboPendiente.COMPLETADO);
            trabajo.setProcessedAt(clock.instant());
            trabajo.setUltimoError(null);
            log.info("Recibo procesado pagoId={} estado={}", trabajo.getPago().getId(), recibo.getEstado());
        } catch (Exception e) {
            String error = e.getClass().getSimpleName();
            recibo.setEstado(EstadoRecibo.ERROR);
            recibo.setUltimoError(error);
            trabajo.setUltimoError(error);
            if (trabajo.getIntentos() >= MAX_INTENTOS) {
                trabajo.setEstado(EstadoReciboPendiente.ERROR);
                trabajo.setProcessedAt(clock.instant());
            } else {
                trabajo.setEstado(EstadoReciboPendiente.PENDIENTE);
                trabajo.setNextAttemptAt(clock.instant().plus(Duration.ofMinutes(5)));
            }
            log.warn("Falló efecto de recibo pagoId={} intento={} error={}",
                    trabajo.getPago().getId(), trabajo.getIntentos(), error);
        }
    }

    private String cuerpo(ReciboPendiente trabajo) {
        String conceptos = aplicaciones.findByPagoIdOrderById(trabajo.getPago().getId()).stream()
                .map(a -> escapar(a.getCargo().getDescripcion()))
                .reduce((a, b) -> a + "<br>" + b).orElse("Pago recibido");
        return "<p>Recibimos tu pago por:</p><p>" + conceptos + "</p><img src='cid:signature' alt='Firma'>";
    }

    private byte[] firma() throws IOException {
        try (InputStream entrada = getClass().getResourceAsStream("/firma_mesa-de-trabajo-1.png")) {
            if (entrada == null) {
                throw new IOException("Firma no disponible");
            }
            return entrada.readAllBytes();
        }
    }

    private static String escapar(String texto) {
        return texto.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
    }
}

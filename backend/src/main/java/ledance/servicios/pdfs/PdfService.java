package ledance.servicios.pdfs;

import com.lowagie.text.*;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter;
import jakarta.mail.MessagingException;
import jakarta.validation.constraints.NotNull;
import ledance.dto.alumno.response.AlumnoResponse;
import ledance.dto.caja.CajaDetalleDTO;
import ledance.dto.egreso.response.EgresoResponse;
import ledance.dto.pago.response.DetallePagoResponse;
import ledance.dto.pago.response.PagoResponse;
import ledance.entidades.*;
import ledance.repositorios.DisciplinaRepositorio;
import ledance.servicios.disciplina.DisciplinaServicio;
import ledance.servicios.email.IEmailService;
import ledance.util.FilePathResolver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.List;
import java.util.stream.Stream;

import static ledance.servicios.mensualidad.MensualidadServicio.validarRecargo;

@Service
public class PdfService {

    private static final Logger LOGGER = LoggerFactory.getLogger(PdfService.class);
    private final IEmailService emailService;               // <-- tipo interfaz
    private final DisciplinaServicio disciplinaServicio;
    private final DisciplinaRepositorio disciplinaRepositorio;

    public PdfService(IEmailService emailService,              // <--- inyecta la interfaz
                      DisciplinaServicio disciplinaServicio, DisciplinaRepositorio disciplinaRepositorio) {
        this.emailService = emailService;
        this.disciplinaServicio = disciplinaServicio;
        this.disciplinaRepositorio = disciplinaRepositorio;
    }

    /**
     * Genera un PDF del recibo de pago en orientacion horizontal (landscape)
     * utilizando OpenPDF.
     *
     * @param pago Objeto Pago con todos sus detalles.
     * @return Un arreglo de bytes que representa el PDF generado.
     * @throws IOException       En caso de error de entrada/salida.
     * @throws DocumentException En caso de error al crear el documento PDF.
     */
    public byte[] generarReciboPdf(Pago pago) throws IOException, DocumentException {
        ByteArrayOutputStream bos = new ByteArrayOutputStream();

        try (Document document = new Document(PageSize.A4.rotate())) {
            PdfWriter.getInstance(document, bos);
            document.open();
            document.setMargins(36, 36, 36, 36);

            PdfPTable headerTable = crearHeaderTable();
            document.add(headerTable);
            document.add(new Paragraph(" "));

            PdfPTable titleTable = crearTitleTable(pago);
            document.add(titleTable);

            document.add(new Paragraph("Fecha: " +
                    pago.getFecha().format(DateTimeFormatter.ofPattern("yyyy-MM-dd"))));
            document.add(new Paragraph("Sres. : " +
                    pago.getAlumno().getApellido() + " " +
                    pago.getAlumno().getNombre() + "        Alumno Nº: " + pago.getAlumno().getId()));

            document.add(new Paragraph("_________________________________________________________________________________________________________"));
            document.add(new Paragraph(" "));

            Font montoFont = new Font(Font.HELVETICA, 11, Font.BOLD);
            Paragraph recibido = new Paragraph(
                    "RECIBIMOS LA SUMA DE: $" +
                            String.format("%,.2f", pago.getMontoPagado()).replace('.', ',')
                            + "    " + convertirMontoEnTexto(pago.getMontoPagado()).toUpperCase() + "*********",
                    montoFont);
            document.add(recibido);
            document.add(new Paragraph(" "));
            document.add(new Paragraph("SEGUN EL SIGUIENTE DETALLE:", montoFont));
            document.add(new Paragraph(" "));

            PdfPTable table = crearTablaDetalles(pago);
            document.add(table);
            document.add(new Paragraph(" "));

            document.add(new Paragraph(pago.getObservaciones(), new Font(Font.HELVETICA, 10, Font.NORMAL)));
            document.add(new Paragraph(" "));
            document.add(new Paragraph("_________________________________________________________________________________________________________"));
            document.add(new Paragraph(" "));

            Paragraph totalParrafo = new Paragraph("TOTAL   $ " +
                    String.format("%,.2f", calcularTotal(pago)),
                    new Font(Font.HELVETICA, 12, Font.BOLD));
            totalParrafo.setAlignment(Element.ALIGN_RIGHT);
            document.add(totalParrafo);

        } catch (DocumentException e) {
            LOGGER.error("Error generando el PDF del recibo", e);
            throw e;
        }
        return bos.toByteArray();
    }

    /**
     * Genera el recibo en PDF y lo envia por email al alumno. El correo incluye
     * el PDF como adjunto y una imagen de firma inline.
     *
     * @param pago Objeto Pago del cual se generara el recibo.
     * @throws IOException        En caso de error en la generacion del PDF o lectura de archivos.
     * @throws DocumentException  En caso de error al crear el documento PDF.
     * @throws MessagingException En caso de error al enviar el email.
     */
    public void generarYEnviarReciboEmail(Pago pago)
            throws IOException, DocumentException, MessagingException {

        // 0) Validación: solo procesar si monto y montoPagado > 0
        if (pago.getMonto() == null || pago.getMontoPagado() == null
                || pago.getMonto() <= 0 || pago.getMontoPagado() <= 0) {
            return;
        }

        // 1) Generar PDF
        byte[] pdfBytes = generarReciboPdf(pago);

        // 2) Validar email del alumno
        String emailAlumno = pago.getAlumno().getEmail();
        if (emailAlumno == null || emailAlumno.isBlank()) {
            throw new IllegalArgumentException(
                    "Alumno " + pago.getAlumno().getNombre() + " no posee un email válido");
        }

        String from = "administracion@ledance.com.ar";
        String subject = "Recibo de pago";

        // 3) Construir cuerpo del mensaje
        String detallesCadena = obtenerCadenaDetalles(pago);
        String body = "<p>¡Hola " + pago.getAlumno().getNombre() + "!</p>"
                + "<p>En adjunto te enviamos el comprobante de pago correspondiente a:</p>"
                + "<p>" + detallesCadena + "</p>"
                + "<p>Muchas gracias.</p>"
                + "<p>Administración</p>"
                + "<img src='cid:signature' alt='Firma' style='max-width:200px;'/>";

        // 4) Cargar imagen de firma
        Path signaturePath = FilePathResolver.of("imgs", "firma_mesa-de-trabajo-1.png");
        byte[] signatureBytes = Files.readAllBytes(signaturePath);

        // 5) Enviar email con adjunto e imagen inline
        emailService.sendEmailWithAttachmentAndInlineImage(
                from,
                emailAlumno,
                subject,
                body,
                pdfBytes,
                "recibo_" + pago.getId() + ".pdf",
                signatureBytes,
                "signature",
                "image/png"
        );
    }

    /**
     * Metodo auxiliar que recorre los detalles del pago y retorna una cadena con
     * la descripcion de cada detalle, separada por saltos de linea HTML.
     *
     * @param pago Objeto Pago del cual se extraen los detalles.
     * @return Cadena con los detalles concatenados.
     */
    private String obtenerCadenaDetalles(Pago pago) {
        StringBuilder sb = new StringBuilder();
        for (DetallePago det : pago.getDetallePagos()) {
            if (!det.getRemovido() && det.getDescripcionConcepto() != null) {
                sb.append(det.getDescripcionConcepto()).append("<br/>");
            }
        }
        return sb.toString();
    }

    private PdfPTable crearHeaderTable() throws DocumentException {
        PdfPTable headerTable = new PdfPTable(2);
        headerTable.setWidthPercentage(100);
        headerTable.setWidths(new float[]{1, 3});

        try {
            Image logo = Image.getInstance(Objects.requireNonNull(getClass().getResource("/RECIBO.jpg")));
            logo.scaleToFit(100, 100);
            PdfPCell logoCell = new PdfPCell(logo);
            logoCell.setBorder(Rectangle.NO_BORDER);
            logoCell.setHorizontalAlignment(Element.ALIGN_CENTER);
            logoCell.setVerticalAlignment(Element.ALIGN_MIDDLE);
            headerTable.addCell(logoCell);
        } catch (Exception e) {
            PdfPCell emptyCell = new PdfPCell(new Paragraph("LE DANCE ARTE ESCUELA"));
            emptyCell.setBorder(Rectangle.NO_BORDER);
            headerTable.addCell(emptyCell);
        }

        PdfPCell infoCell = new PdfPCell();
        infoCell.setBorder(Rectangle.NO_BORDER);
        infoCell.setHorizontalAlignment(Element.ALIGN_LEFT);
        Font infoFont = new Font(Font.HELVETICA, 10, Font.NORMAL);
        Paragraph infoParagraph = new Paragraph();
        infoParagraph.add(new Paragraph("Mexico 1120", infoFont));
        infoParagraph.add(new Paragraph("1137668490", infoFont));
        infoParagraph.add(new Paragraph("www.ledance.com.ar", infoFont));
        infoCell.addElement(infoParagraph);
        headerTable.addCell(infoCell);
        return headerTable;
    }

    private PdfPTable crearTitleTable(Pago pago) throws DocumentException {
        PdfPTable titleTable = new PdfPTable(2);
        titleTable.setWidthPercentage(100);
        Font titleFont = new Font(Font.HELVETICA, 14, Font.BOLD);
        Paragraph titulo = new Paragraph("*** R E C I B O ***", titleFont);
        PdfPCell titleCell = new PdfPCell(titulo);
        titleCell.setHorizontalAlignment(Element.ALIGN_CENTER);
        titleCell.setBorder(Rectangle.NO_BORDER);
        titleTable.addCell(titleCell);
        Paragraph nroRecibo = new Paragraph("Nº " + pago.getId(), new Font(Font.HELVETICA, 12, Font.NORMAL));
        PdfPCell nroReciboCell = new PdfPCell(nroRecibo);
        nroReciboCell.setHorizontalAlignment(Element.ALIGN_RIGHT);
        nroReciboCell.setBorder(Rectangle.NO_BORDER);
        titleTable.addCell(nroReciboCell);
        return titleTable;
    }

    private PdfPTable crearTablaDetalles(Pago pago) throws DocumentException {
        PdfPTable table = new PdfPTable(8);
        table.setWidthPercentage(100);
        table.setWidths(new float[]{2, 8, 4, 4, 3, 3, 4, 4});
        Font headerFont = new Font(Font.HELVETICA, 8, Font.BOLD);
        addHeaderCell(table, "Cod.", headerFont);
        addHeaderCell(table, "Concepto", headerFont);
        addHeaderCell(table, "Cuota/Can", headerFont);
        addHeaderCell(table, "Valor", headerFont);
        addHeaderCell(table, "Bonif", headerFont);
        addHeaderCell(table, "Rec", headerFont);
        addHeaderCell(table, "Importe", headerFont);
        addHeaderCell(table, "Abonado", headerFont);

        Font cellFont = new Font(Font.HELVETICA, 8, Font.NORMAL);

        for (DetallePago det : pago.getDetallePagos()) {
            if (!det.getRemovido() && det.getACobrar() > 0) {
                addCell(table, det.getId().toString(), cellFont);
                addCell(table, det.getDescripcionConcepto(), cellFont);
                addCell(table, det.getCuotaOCantidad(), cellFont);
                addCell(table, "$ " + String.format("%,.2f", det.getValorBase() != null ? det.getValorBase() : 0.0), cellFont);
                addCell(table, "$ " + String.format("%,.2f", det.getBonificacion() != null ?
                        calcularDescuento(det.getValorBase(), det.getBonificacion()) : 0.0), cellFont);
                addCell(table, "$ " + String.format("%,.2f", det.getRecargo() != null ?
                        calcularRecargo(det.getValorBase(), det.getRecargo()) : 0.0), cellFont);
                double importe = det.getImporteInicial() != null ? det.getImporteInicial() : 0.0;
                addCell(table, "$ " + String.format("%,.2f", importe), cellFont);
                addCell(table, "$ " + String.format("%,.2f", det.getACobrar()), cellFont);
            }
        }
        return table;
    }

    private void addHeaderCell(PdfPTable table, String text, Font font) {
        PdfPCell cell = new PdfPCell(new Paragraph(text, font));
        cell.setHorizontalAlignment(Element.ALIGN_CENTER);
        cell.setVerticalAlignment(Element.ALIGN_MIDDLE);
        cell.setPadding(3);
        table.addCell(cell);
    }

    private void addCell(PdfPTable table, String text, Font font) {
        PdfPCell cell = new PdfPCell(new Paragraph(text, font));
        cell.setHorizontalAlignment(Element.ALIGN_LEFT);
        cell.setVerticalAlignment(Element.ALIGN_MIDDLE);
        cell.setPadding(3);
        table.addCell(cell);
    }

    public static String convertirMontoEnTexto(double monto) {
        long parteEntera = (long) monto;
        int centavos = (int) Math.round((monto - parteEntera) * 100);
        String texto = NumberToLetterConverter.convertir(parteEntera);
        return texto + " CON " + String.format("%02d", centavos) + "/100";
    }

    private double calcularDescuento(Double valorBase, Bonificacion bonificacion) {
        if (bonificacion == null || valorBase == null) {
            return 0.0;
        }
        double descuentoFijo = bonificacion.getValorFijo();
        double descuentoPorcentaje = (bonificacion.getPorcentajeDescuento() / 100.0) * valorBase;
        return descuentoFijo + descuentoPorcentaje;
    }

    private double calcularRecargo(Double valorBase, Recargo recargo) {
        return validarRecargo(valorBase, recargo);
    }

    private double calcularTotal(Pago pago) {
        return pago.getDetallePagos().stream()
                .filter(det -> !det.getRemovido())
                .mapToDouble(det -> det.getACobrar() != null ? det.getACobrar() : 0.0)
                .sum();
    }

    // --- Métodos para Factura basados en Pago ---
    public byte[] generarFacturaPdf(Pago pago) throws IOException, DocumentException {
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        try (Document document = new Document(PageSize.A4)) {
            PdfWriter.getInstance(document, bos);
            document.open();
            document.setMargins(36, 36, 36, 36);

            // Header factura: similar a recibo pero con título FACTURA
            PdfPTable header = crearHeaderTable();
            document.add(header);
            document.add(new Paragraph(" "));

            // Título FACTURA y número
            PdfPTable title = new PdfPTable(2);
            title.setWidthPercentage(100);
            Font titleFont = new Font(Font.HELVETICA, 14, Font.BOLD);
            PdfPCell cell1 = new PdfPCell(new Paragraph("*** FACTURA ***", titleFont));
            cell1.setBorder(Rectangle.NO_BORDER);
            cell1.setHorizontalAlignment(Element.ALIGN_CENTER);
            title.addCell(cell1);
            PdfPCell cell2 = new PdfPCell(new Paragraph("Nº " + pago.getId(), new Font(Font.HELVETICA, 12, Font.NORMAL)));
            cell2.setBorder(Rectangle.NO_BORDER);
            cell2.setHorizontalAlignment(Element.ALIGN_RIGHT);
            title.addCell(cell2);
            document.add(title);

            // Datos de factura: fecha y alumno
            document.add(new Paragraph("Fecha: " + pago.getFecha().format(DateTimeFormatter.ofPattern("yyyy-MM-dd"))));
            document.add(new Paragraph("Cliente: " + pago.getAlumno().getApellido() + " " + pago.getAlumno().getNombre()));
            document.add(new Paragraph("_________________________________________________________________________________"));
            document.add(new Paragraph(" "));

            // Detalle de ítems (mismos detalles de pago)
            PdfPTable detalles = new PdfPTable(6);
            detalles.setWidthPercentage(100);
            detalles.setWidths(new float[]{2, 6, 2, 3, 3, 4});
            Font hf = new Font(Font.HELVETICA, 8, Font.BOLD);
            for (String h : new String[]{"Cod.", "Concepto", "Cant.", "Precio", "IVA", "Subtotal"}) {
                PdfPCell hcell = new PdfPCell(new Phrase(h, hf));
                hcell.setHorizontalAlignment(Element.ALIGN_CENTER);
                hcell.setPadding(3);
                detalles.addCell(hcell);
            }
            Font cf = new Font(Font.HELVETICA, 8, Font.NORMAL);
            for (var det : pago.getDetallePagos()) {
                if (Boolean.TRUE.equals(det.getRemovido()) || det.getACobrar() <= 0) continue;
                detalles.addCell(new PdfPCell(new Phrase(String.valueOf(det.getId()), cf)));
                detalles.addCell(new PdfPCell(new Phrase(det.getDescripcionConcepto(), cf)));
                detalles.addCell(new PdfPCell(new Phrase(det.getCuotaOCantidad(), cf)));
                detalles.addCell(new PdfPCell(new Phrase(String.format("%,.2f", det.getValorBase()), cf)));
                detalles.addCell(new PdfPCell(new Phrase(String.format("%,.2f", det.getRecargo()), cf)));
                double subtotal = det.getACobrar() != null ? det.getACobrar() : 0.0;
                detalles.addCell(new PdfPCell(new Phrase(String.format("%,.2f", subtotal), cf)));
            }
            document.add(detalles);
            document.add(new Paragraph(" "));

            // Total factura
            Paragraph total = new Paragraph(
                    "TOTAL FACTURA: $ " + String.format("%,.2f", pago.getDetallePagos().stream()
                            .filter(d -> !Boolean.TRUE.equals(d.getRemovido()))
                            .mapToDouble(d -> d.getACobrar() != null ? d.getACobrar() : 0.0)
                            .sum()),
                    new Font(Font.HELVETICA, 12, Font.BOLD)
            );
            total.setAlignment(Element.ALIGN_RIGHT);
            document.add(total);
        } catch (DocumentException e) {
            LOGGER.error("Error generando el PDF de la factura", e);
            throw e;
        }
        return bos.toByteArray();
    }

    /**
     * Genera un PDF con la rendicion del mes, es decir, con TODOS los pagos y egresos
     * obtenidos a partir de un periodo (start-end). El PDF tendra dos secciones:
     * <p>
     * - Una tabla para los pagos con los encabezados:
     * ["Recibo", "Codigo", "Alumno", "Observaciones", "Importe"]
     * <p>
     * - Una tabla para los egresos con los encabezados:
     * ["ID", "Observaciones", "Monto"]
     *
     * @param caja Objeto CajaDetalleDTO que contiene las listas de pagos y egresos.
     * @return Un arreglo de bytes representando el PDF generado.
     * @throws DocumentException En caso de error al crear el documento PDF.
     */
    public byte[] generarRendicionMensualPdf(CajaDetalleDTO caja) throws DocumentException {
        // Configuracion inicial del documento
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        Document document = new Document(PageSize.A4); // Orientacion horizontal para mas ancho
        document.setMargins(15, 15, 15, 15); // Margenes ajustados
        PdfWriter.getInstance(document, bos);
        document.open();

        // Definicion de fuentes compactas
        Font titleFont = new Font(Font.HELVETICA, 14, Font.BOLD);
        Font sectionFont = new Font(Font.HELVETICA, 12, Font.BOLD);
        Font contentFont = new Font(Font.HELVETICA, 8, Font.NORMAL); // Fuente pequeña para mayor cantidad de informacion

        // Titulo principal
        Paragraph titulo = new Paragraph("RENDICION MENSUAL", titleFont);
        titulo.setAlignment(Element.ALIGN_CENTER);
        document.add(titulo);
        document.add(new Paragraph(" ", contentFont));

        // -------------------------------------------------------------------------
        // Seccion PAGOS DEL MES
        // -------------------------------------------------------------------------
        document.add(new Paragraph("PAGOS DEL MES", sectionFont));
        document.add(new Paragraph(" ", contentFont));

        List<PagoResponse> pagos = caja.pagosDelDia();
        if (pagos == null || pagos.isEmpty()) {
            document.add(new Paragraph("No hay pagos registrados para este periodo.", contentFont));
            document.add(new Paragraph(" ", contentFont));
        } else {
            // Crear tabla de pagos filtrados y ordenados
            PdfPTable tablaPagos = getPdfPTable(pagos, contentFont);
            document.add(tablaPagos);
            document.add(new Paragraph(" ", contentFont));
        }

        // -------------------------------------------------------------------------
        // Seccion EGRESOS DEL MES
        // -------------------------------------------------------------------------
        document.add(new Paragraph("EGRESOS DEL MES", sectionFont));
        document.add(new Paragraph(" ", contentFont));

        List<EgresoResponse> egresos = caja.egresosDelDia();
        if (egresos == null || egresos.isEmpty()) {
            document.add(new Paragraph("No hay egresos registrados para este periodo.", contentFont));
            document.add(new Paragraph(" ", contentFont));
        } else {
            PdfPTable tablaEgresos = getPTable(egresos, contentFont);
            document.add(tablaEgresos);
            document.add(new Paragraph(" ", contentFont));
        }

        // Cerrar documento y devolver bytes
        document.close();
        return bos.toByteArray();
    }

    private static PdfPTable getPTable(List<EgresoResponse> egresos, Font contentFont) {
        PdfPTable tablaEgresos = new PdfPTable(3);
        tablaEgresos.setWidthPercentage(100);
        float[] anchosColumnasEgresos = {15f, 70f, 15f};
        tablaEgresos.setWidths(anchosColumnasEgresos);

        for (EgresoResponse egreso : egresos) {
            Long egresoId = (egreso.id() != null) ? egreso.id() : 0L;
            String observaciones = (egreso.observaciones() != null) ? egreso.observaciones() : "";
            Double monto = (egreso.monto() != null) ? egreso.monto() : 0.0;

            PdfPCell celdaId = new PdfPCell(new Phrase(String.valueOf(egresoId), contentFont));
            PdfPCell celdaObs = new PdfPCell(new Phrase(observaciones, contentFont));
            PdfPCell celdaMonto = new PdfPCell(new Phrase(String.format("%,.2f", monto), contentFont));

            celdaId.setBorder(Rectangle.NO_BORDER);
            celdaObs.setBorder(Rectangle.NO_BORDER);
            celdaMonto.setBorder(Rectangle.NO_BORDER);
            celdaId.setPadding(2);
            celdaObs.setPadding(2);
            celdaMonto.setPadding(2);
            celdaMonto.setHorizontalAlignment(Element.ALIGN_RIGHT);

            tablaEgresos.addCell(celdaId);
            tablaEgresos.addCell(celdaObs);
            tablaEgresos.addCell(celdaMonto);
        }
        return tablaEgresos;
    }

    private static PdfPTable getPdfPTable(List<PagoResponse> pagos, Font contentFont) {
        // Crear tabla de 5 columnas
        PdfPTable tablaPagos = new PdfPTable(5);
        tablaPagos.setWidthPercentage(100);
        float[] anchosColumnasPagos = {10f, 10f, 25f, 40f, 15f};
        tablaPagos.setWidths(anchosColumnasPagos);

        // Ordenar pagos de mayor a menor (ID descendente)
        pagos.sort((p1, p2) -> Long.compare(p2.id(), p1.id()));

        // Procesar cada pago
        for (PagoResponse pago : pagos) {
            // Calcular valores con valores por defecto
            double montoPago = (pago.monto() != null) ? pago.monto() : 0.0;
            double impInicial = (pago.importeInicial() != null) ? pago.importeInicial() : 0.0;
            double saldoRestante = (pago.saldoRestante() != null) ? pago.saldoRestante() : 0.0;
            String observaciones = (pago.observaciones() != null) ? pago.observaciones() : "";
            boolean observVacias = observaciones.trim().isEmpty();

            // Evaluar detalles: determinar si ninguno de los detalles esta "ANULADO"
            boolean ningunDetalleAnulado = true;
            if (pago.detallePagos() != null && !pago.detallePagos().isEmpty()) {
                for (var detalle : pago.detallePagos()) {
                    if ("ANULADO".equalsIgnoreCase(detalle.estadoPago())) {
                        ningunDetalleAnulado = false;
                        break;
                    }
                }
            }

            // Si se cumplen todas las condiciones:
            // monto = 0, importeInicial = 0, saldoRestante = 0, observaciones vacias, y ningun detalle anulado,
            // omitir este pago (no agregarlo a la tabla)
            if (montoPago == 0.0 && impInicial == 0.0 && saldoRestante == 0.0 && observVacias && ningunDetalleAnulado) {
                continue;
            }

            // Verificar si todos los detalles estan "ANULADO" para mostrar el estado "ANULADO"
            boolean todosAnulados = false;
            if (pago.detallePagos() != null && !pago.detallePagos().isEmpty()) {
                todosAnulados = pago.detallePagos().stream().allMatch(
                        d -> "ANULADO".equalsIgnoreCase(d.estadoPago())
                );
            }
            String estado = todosAnulados ? "ANULADO" : "";

            // Preparar datos: ID, Codigo de alumno y nombre
            Long pagoId = (pago.id() != null) ? pago.id() : 0L;
            Long codigoAlumno = (pago.alumno() != null && pago.alumno().id() != null)
                    ? pago.alumno().id() : 0L;
            String nombreAlumno = "";
            if (pago.alumno() != null) {
                String nombre = (pago.alumno().nombre() != null) ? pago.alumno().nombre() : "";
                String apellido = (pago.alumno().apellido() != null) ? pago.alumno().apellido() : "";
                nombreAlumno = nombre + " " + apellido;
            }

            // Formatear el monto. Si el pago esta anulado se muestra la palabra "ANULADO"
            String montoStr;
            if (estado.equals("ANULADO")) {
                montoStr = estado;
            } else {
                Double monto = montoPago;
                montoStr = String.format("%,.2f", monto);
            }

            // Crear celdas para la tabla
            PdfPCell celdaId = new PdfPCell(new Phrase(String.valueOf(pagoId), contentFont));
            PdfPCell celdaCodigo = new PdfPCell(new Phrase(String.valueOf(codigoAlumno), contentFont));
            PdfPCell celdaNombre = new PdfPCell(new Phrase(nombreAlumno, contentFont));
            PdfPCell celdaObs = new PdfPCell(new Phrase(observaciones, contentFont));
            PdfPCell celdaMonto = new PdfPCell(new Phrase(montoStr, contentFont));

            // Configurar celdas: sin bordes, con padding y alinear el monto a la derecha.
            celdaId.setBorder(Rectangle.NO_BORDER);
            celdaCodigo.setBorder(Rectangle.NO_BORDER);
            celdaNombre.setBorder(Rectangle.NO_BORDER);
            celdaObs.setBorder(Rectangle.NO_BORDER);
            celdaMonto.setBorder(Rectangle.NO_BORDER);

            celdaId.setPadding(2);
            celdaCodigo.setPadding(2);
            celdaNombre.setPadding(2);
            celdaObs.setPadding(2);
            celdaMonto.setPadding(2);
            celdaMonto.setHorizontalAlignment(Element.ALIGN_RIGHT);

            // Agregar las celdas a la tabla
            tablaPagos.addCell(celdaId);
            tablaPagos.addCell(celdaCodigo);
            tablaPagos.addCell(celdaNombre);
            tablaPagos.addCell(celdaObs);
            tablaPagos.addCell(celdaMonto);
        }
        return tablaPagos;
    }

    /**
     * Genera un PDF con el listado de alumnos de una disciplina,
     * numerados y en orden alfabético.
     */
    public byte[] generarAlumnosDisciplinaPdf(Long disciplinaId)
            throws IOException, DocumentException {
        // 1) Obtener lista de alumnos y ordenar por "Apellido Nombre"
        List<AlumnoResponse> alumnos = disciplinaServicio
                .obtenerAlumnosDeDisciplina(disciplinaId)
                .stream()
                .sorted(Comparator
                        .comparing(a -> (a.apellido() + " " + a.nombre()).toLowerCase()))
                .toList();

        // 2) Crear documento PDF
        try (ByteArrayOutputStream bos = new ByteArrayOutputStream()) {
            Document document = new Document(PageSize.A4);
            PdfWriter.getInstance(document, bos);
            document.open();

            Disciplina disciplina = disciplinaRepositorio.findById(disciplinaId)
                    .orElseThrow(() -> new NoSuchElementException(
                            "Disciplina no encontrada para id=" + disciplinaId));

            // Título
            Font titleFont = new Font(Font.HELVETICA, 16, Font.BOLD);
            Paragraph titulo = new Paragraph(
                    "Listado de Alumnos - Disciplina " + disciplina.getNombre(),
                    titleFont);
            titulo.setAlignment(Element.ALIGN_CENTER);
            document.add(titulo);
            document.add(new Paragraph("\n"));

            // Tabla con 2 columnas: #, Nombre completo
            PdfPTable table = new PdfPTable(new float[]{1, 10});
            table.setWidthPercentage(100);

            // Encabezados
            Font headerFont = new Font(Font.HELVETICA, 12, Font.BOLD);
            Stream.of("#", "Nombre y Apellido")
                    .forEach(header -> {
                        PdfPCell cell = new PdfPCell(new Phrase(header, headerFont));
                        cell.setHorizontalAlignment(Element.ALIGN_CENTER);
                        cell.setPadding(4);
                        table.addCell(cell);
                    });

            // Filas
            Font cellFont = new Font(Font.HELVETICA, 10, Font.NORMAL);
            for (int i = 0; i < alumnos.size(); i++) {
                AlumnoResponse a = alumnos.get(i);
                // Columna índice
                PdfPCell idxCell = new PdfPCell(new Phrase(String.valueOf(i + 1), cellFont));
                idxCell.setHorizontalAlignment(Element.ALIGN_CENTER);
                idxCell.setPadding(4);
                table.addCell(idxCell);

                // Columna nombre completo
                String nombreCompleto = a.nombre() + " " + a.apellido();
                PdfPCell nameCell = new PdfPCell(new Phrase(nombreCompleto, cellFont));
                nameCell.setPadding(4);
                table.addCell(nameCell);
            }

            document.add(table);
            document.close();

            return bos.toByteArray();
        }
    }

    /**
     * Helper para crear PdfPCell con padding reducido y alineación.
     */
    private PdfPCell pCell(String text, Font font) {
        PdfPCell cell = new PdfPCell(new Phrase(text, font));
        cell.setHorizontalAlignment(Element.ALIGN_CENTER);
        cell.setVerticalAlignment(Element.ALIGN_MIDDLE);
        cell.setPadding(2);               // Padding reducido para menor altura
        cell.setFixedHeight(14f);         // Altura fija más compacta
        return cell;
    }

    /**
     * Genera el PDF de liquidación para un profesor, con márgenes superiores reducidos y ajuste de anchos.
     */
    public byte[] generarLiquidacionProfesorPdf(
            String profesorNombre,
            String disciplinaNombre,
            @NotNull LocalDate fechaInicio,
            List<DetallePagoResponse> detalles,
            Double porcentaje
    ) throws IOException, DocumentException {
        // 1) Documento con márgenes reducidos (top=20 para menos espacio vacío)
        // Márgenes reducidos superior/inferior para menos espacio
        Document doc = new Document(PageSize.A4.rotate(), 36, 36, 10, 10);
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        PdfWriter.getInstance(doc, bos);
        doc.open();

        // 2) Logo recortado y posicionado (igual)
        BufferedImage orig = ImageIO.read(
                Objects.requireNonNull(getClass().getResourceAsStream("/RECIBO.jpg"))
        );
        int crop = 50;
        BufferedImage cropImg = orig.getSubimage(
                crop, crop,
                orig.getWidth() - crop*2,
                orig.getHeight() - crop*2
        );
        ByteArrayOutputStream logoBaos = new ByteArrayOutputStream();
        ImageIO.write(cropImg, "jpg", logoBaos);
        Image logo = Image.getInstance(logoBaos.toByteArray());
        logo.scaleToFit(100, 100);
        float x = doc.leftMargin();
        float y = doc.getPageSize().getHeight() - doc.topMargin() - logo.getScaledHeight();
        logo.setAbsolutePosition(x, y);
        doc.add(logo);

        // 3) Cabecera textual
        float indent = logo.getScaledWidth() + 20;
        Font titleFont = new Font(Font.HELVETICA, 14, Font.BOLD); // Fuente un poco más pequeña
        // Incluir mes y año en el título
        DateTimeFormatter df = DateTimeFormatter.ofPattern("MMMM yyyy", new Locale("es","AR"));
        String periodo = fechaInicio.format(df).toUpperCase();
        Paragraph h1 = new Paragraph("Liquidación Profesor " + periodo, titleFont);
        h1.setIndentationLeft(indent);
        doc.add(h1);
        Font infoFont = new Font(Font.HELVETICA, 10, Font.NORMAL); // Tamaño de letra reducido
        Paragraph p2 = new Paragraph("Profesor: " + profesorNombre, infoFont);
        p2.setIndentationLeft(indent);
        doc.add(p2);
        Paragraph p3 = new Paragraph("Disciplina: " + disciplinaNombre, infoFont);
        p3.setIndentationLeft(indent);
        doc.add(p3);
        Paragraph p4 = new Paragraph(
                String.format("Porcentaje aplicado: %.2f%%", porcentaje),
                infoFont
        );
        p4.setIndentationLeft(indent);
        doc.add(p4);
        doc.add(Chunk.NEWLINE);

        // 4) Tabla de detalle sin anchos fijos
        PdfPTable table = new PdfPTable(7);
        table.setWidthPercentage(100);
        // ahora sí añadimos el espacio previo al cuerpo
        table.setSpacingBefore(8f);

        // Encabezados centrados
        Font hf = new Font(Font.HELVETICA, 9, Font.BOLD);
        for (String header : List.of(
                "Alumno", "Tarifa", "Descripción",
                "Valor Base", "Bonificación", "Monto Cobrado", "Cobrado"
        )) {
            table.addCell(pCell(header, hf));
        }

        // Filas de datos con letra más pequeña
        Font cf = new Font(Font.HELVETICA, 8, Font.NORMAL);
        for (DetallePagoResponse det : detalles) {
            String alumno = det.alumno().nombre() + " " + det.alumno().apellido();
            String full = det.descripcionConcepto();
            String desc = full, tarifa = "";
            int idx = full.indexOf("-");
            if (idx >= 0) {
                desc = full.substring(0, idx).trim();
                tarifa = full.substring(idx + 1).trim();
            }
            String base = String.format("%,.2f", det.importeInicial());
            String bonif = det.bonificacionNombre() != null ? det.bonificacionNombre() : "-";
            String monto = String.format("%,.2f", det.ACobrar());
            String estado = Boolean.TRUE.equals(det.cobrado()) ? "SALDADO" : "PENDIENTE";

            table.addCell(pCell(alumno, cf));
            table.addCell(pCell(tarifa, cf));
            table.addCell(pCell(desc, cf));
            table.addCell(pCell(base, cf));
            table.addCell(pCell(bonif, cf));
            table.addCell(pCell(monto, cf));
            table.addCell(pCell(estado, cf));
        }
        doc.add(table);

        // 5) Totales
        double totalBruto = detalles.stream()
                .mapToDouble(d -> d.ACobrar() != null ? d.ACobrar() : 0.0)
                .sum();
        double totalNeto = totalBruto * (1.0 - porcentaje / 100.0);

        Locale localeAR = new Locale("es", "AR");
        DecimalFormatSymbols symbols = DecimalFormatSymbols.getInstance(localeAR);
        DecimalFormat moneyFmt = new DecimalFormat("#,##0.00", symbols);

        Font totalFont = new Font(Font.HELVETICA, 11, Font.BOLD); // Ligeramente menor
        doc.add(Chunk.NEWLINE);
        Paragraph pBr = new Paragraph(
                "TOTAL BRUTO: $ " + moneyFmt.format(totalBruto), totalFont
        );
        pBr.setAlignment(Element.ALIGN_RIGHT);
        doc.add(pBr);
        Paragraph pNe = new Paragraph(
                "LIQUIDACIÓN NETA: $ " + moneyFmt.format(totalNeto), totalFont
        );
        pNe.setAlignment(Element.ALIGN_RIGHT);
        doc.add(pNe);

        doc.close();
        return bos.toByteArray();
    }
}

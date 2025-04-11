package ledance.servicios.pdfs;

import com.lowagie.text.*;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter;
import jakarta.mail.MessagingException;
import ledance.dto.caja.CajaDetalleDTO;
import ledance.dto.egreso.response.EgresoResponse;
import ledance.dto.pago.response.PagoResponse;
import ledance.entidades.Bonificacion;
import ledance.entidades.DetallePago;
import ledance.entidades.Pago;
import ledance.entidades.Recargo;
import ledance.servicios.email.EmailService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Objects;

import static ledance.servicios.mensualidad.MensualidadServicio.validarRecargo;

@Service
public class PdfService {

    private static final Logger LOGGER = LoggerFactory.getLogger(PdfService.class);
    private final EmailService emailService;

    public PdfService(EmailService emailService) {
        this.emailService = emailService;
    }

    /**
     * Genera un PDF del recibo de pago en orientación horizontal (landscape)
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
            document.add(new Paragraph("SEGÚN EL SIGUIENTE DETALLE:", montoFont));
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
     * Genera el recibo en PDF y lo envía por email al alumno. El correo incluye
     * el PDF como adjunto y una imagen de firma inline.
     *
     * @param pago Objeto Pago del cual se generará el recibo.
     * @throws IOException        En caso de error en la generación del PDF o lectura de archivos.
     * @throws DocumentException  En caso de error al crear el documento PDF.
     * @throws MessagingException En caso de error al enviar el email.
     */
    public void generarYEnviarReciboEmail(Pago pago)
            throws IOException, DocumentException, MessagingException {
        byte[] pdfBytes = generarReciboPdf(pago);

        String emailAlumno = pago.getAlumno().getEmail1();
        if (emailAlumno == null || emailAlumno.isBlank()) {
            throw new IllegalArgumentException("Alumno " + pago.getAlumno().getNombre() + " no posee un email válido");
        }
        String from = "administracion@ledance.com.ar";
        String subject = "Recibo de pago";

        // Se obtiene la cadena con la lista de detalles a partir de los detalles del pago.
        String detallesCadena = obtenerCadenaDetalles(pago);

        // Construir cuerpo HTML con firma inline y se inserta la cadena de detalles.
        String body = "<p>¡Hola " + pago.getAlumno().getNombre() + "!</p>" +
                "<p>En adjunto te enviamos el comprobante de pago correspondiente a:</p>" +
                "<p>" + detallesCadena + "</p>" +
                "<p>Muchas gracias.</p>" +
                "<p>Administración</p>" +
                "<img src='cid:signature' alt='Firma' style='max-width:200px;'/>";

        // Cargar la imagen de firma del sistema de archivos.
        Path signaturePath = Paths.get("/opt/le-dance/imgs/firma_mesa-de-trabajo-1.png");
        byte[] signatureBytes = Files.readAllBytes(signaturePath);

        // Enviar el email con el PDF adjunto y la imagen inline.
        emailService.sendEmailWithAttachmentAndInlineImage(
                from, emailAlumno, subject, body, pdfBytes,
                "recibo_" + pago.getId() + ".pdf", signatureBytes, "signature", "image/png"
        );
    }

    /**
     * Método auxiliar que recorre los detalles del pago y retorna una cadena con
     * la descripción de cada detalle, separada por saltos de línea HTML.
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
            if (!det.getRemovido()) {
                addCell(table, det.getId().toString(), cellFont);
                addCell(table, det.getDescripcionConcepto(), cellFont);
                addCell(table, det.getCuotaOCantidad() != null ? det.getCuotaOCantidad() : "MATRICULA", cellFont);
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

    /**
     * Genera un PDF con la rendición del mes, es decir, con TODOS los pagos y egresos
     * obtenidos a partir de un período (start-end). El PDF tendrá dos secciones:
     * <p>
     * - Una tabla para los pagos con los encabezados:
     * ["Recibo", "Código", "Alumno", "Observaciones", "Importe"]
     * <p>
     * - Una tabla para los egresos con los encabezados:
     * ["ID", "Observaciones", "Monto"]
     *
     * @param caja Objeto CajaDetalleDTO que contiene las listas de pagos y egresos.
     * @return Un arreglo de bytes representando el PDF generado.
     * @throws DocumentException En caso de error al crear el documento PDF.
     */
    public byte[] generarRendicionMensualPdf(CajaDetalleDTO caja) throws DocumentException {
        // Configuración inicial del documento
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        Document document = new Document(PageSize.A4); // Usar orientación horizontal para más ancho
        document.setMargins(15, 15, 15, 15); // Márgenes más pequeños para aprovechar espacio
        PdfWriter.getInstance(document, bos);
        document.open();

        // Definición de fuentes más compactas
        Font titleFont = new Font(Font.HELVETICA, 14, Font.BOLD);
        Font sectionFont = new Font(Font.HELVETICA, 12, Font.BOLD);
        Font contentFont = new Font(Font.HELVETICA, 8, Font.NORMAL); // Fuente más pequeña para más contenido por línea

        // Título principal
        Paragraph titulo = new Paragraph("RENDICIÓN MENSUAL", titleFont);
        titulo.setAlignment(Element.ALIGN_CENTER);
        document.add(titulo);
        document.add(new Paragraph(" ", contentFont));

        // -------------------------------------------------------------------------
        // Sección PAGOS DEL MES
        // -------------------------------------------------------------------------
        document.add(new Paragraph("PAGOS DEL MES", sectionFont));
        document.add(new Paragraph(" ", contentFont));

        // Verificar si hay pagos para mostrar
        List<PagoResponse> pagos = caja.pagosDelDia();
        if (pagos == null || pagos.isEmpty()) {
            document.add(new Paragraph("No hay pagos registrados para este período.", contentFont));
            document.add(new Paragraph(" ", contentFont));
        } else {
            // Crear tabla para mejor control del ancho
            PdfPTable tablaPagos = getPdfPTable(pagos, contentFont);

            // Agregar tabla al documento
            document.add(tablaPagos);
            document.add(new Paragraph(" ", contentFont));
        }

        // -------------------------------------------------------------------------
        // Sección EGRESOS DEL MES
        // -------------------------------------------------------------------------
        document.add(new Paragraph("EGRESOS DEL MES", sectionFont));
        document.add(new Paragraph(" ", contentFont));

        // Verificar si hay egresos para mostrar
        List<EgresoResponse> egresos = caja.egresosDelDia();
        if (egresos == null || egresos.isEmpty()) {
            document.add(new Paragraph("No hay egresos registrados para este período.", contentFont));
            document.add(new Paragraph(" ", contentFont));
        } else {
            // Crear tabla para egresos
            PdfPTable tablaEgresos = getPTable(egresos, contentFont);

            // Agregar tabla al documento
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

        // Definir anchos relativos de columnas
        float[] anchosColumnasEgresos = {15f, 70f, 15f};
        tablaEgresos.setWidths(anchosColumnasEgresos);

        // Procesar cada egreso
        for (EgresoResponse egreso : egresos) {
            // Preparar datos para mostrar
            Long egresoId = (egreso.id() != null) ? egreso.id() : 0L;
            String observaciones = (egreso.observaciones() != null) ? egreso.observaciones() : "";
            Double monto = (egreso.monto() != null) ? egreso.monto() : 0.0;

            // Crear celdas
            PdfPCell celdaId = new PdfPCell(new Phrase(String.valueOf(egresoId), contentFont));
            PdfPCell celdaObs = new PdfPCell(new Phrase(observaciones, contentFont));
            PdfPCell celdaMonto = new PdfPCell(new Phrase(String.format("%,.2f", monto), contentFont));

            // Configurar celdas sin bordes y con padding mínimo
            celdaId.setBorder(Rectangle.NO_BORDER);
            celdaObs.setBorder(Rectangle.NO_BORDER);
            celdaMonto.setBorder(Rectangle.NO_BORDER);

            celdaId.setPadding(2);
            celdaObs.setPadding(2);
            celdaMonto.setPadding(2);

            // Alinear monto a la derecha
            celdaMonto.setHorizontalAlignment(Element.ALIGN_RIGHT);

            // Añadir celdas a la tabla
            tablaEgresos.addCell(celdaId);
            tablaEgresos.addCell(celdaObs);
            tablaEgresos.addCell(celdaMonto);
        }
        return tablaEgresos;
    }

    private static PdfPTable getPdfPTable(List<PagoResponse> pagos, Font contentFont) {
        PdfPTable tablaPagos = new PdfPTable(5);
        tablaPagos.setWidthPercentage(100); // Usar 100% del ancho disponible

        // Definir anchos relativos de columnas (deben sumar 100)
        float[] anchosColumnasPagos = {10f, 10f, 25f, 40f, 15f};
        tablaPagos.setWidths(anchosColumnasPagos);

        // Procesar cada pago
        for (PagoResponse pago : pagos) {
            // Calcular suma a cobrar y verificar si todos los detalles están anulados
            double sumaACobrar = 0.0;
            boolean todosAnulados = true;

            if (pago.detallePagos() != null && !pago.detallePagos().isEmpty()) {
                for (var detalle : pago.detallePagos()) {
                    sumaACobrar += (detalle.ACobrar() != null ? detalle.ACobrar() : 0.0);
                    if (!"ANULADO".equalsIgnoreCase(detalle.estadoPago())) {
                        todosAnulados = false;
                    }
                }
            }

            // Omitir pagos con suma a cobrar igual a cero
            if (sumaACobrar == 0.0) {
                continue;
            }

            // Preparar datos para mostrar
            String estado = todosAnulados ? "ANULADO" : "";
            Long pagoId = (pago.id() != null) ? pago.id() : 0L;
            Long codigoAlumno = (pago.alumno() != null && pago.alumno().id() != null) ? pago.alumno().id() : 0L;

            // Construir nombre completo del alumno
            String nombreAlumno = "";
            if (pago.alumno() != null) {
                String nombre = pago.alumno().nombre() != null ? pago.alumno().nombre() : "";
                String apellido = pago.alumno().apellido() != null ? pago.alumno().apellido() : "";
                nombreAlumno = nombre + " " + apellido;
            }

            String observaciones = (pago.observaciones() != null) ? pago.observaciones() : "";

            // Determinar cómo mostrar el monto según el estado
            String montoStr;
            if (estado.equals("ANULADO")) {
                montoStr = estado;
            } else {
                Double monto = (pago.monto() != null) ? pago.monto() : 0.0;
                montoStr = String.format("%,.2f", monto);
            }

            // Agregar celdas a la tabla
            PdfPCell celdaId = new PdfPCell(new Phrase(String.valueOf(pagoId), contentFont));
            PdfPCell celdaCodigo = new PdfPCell(new Phrase(String.valueOf(codigoAlumno), contentFont));
            PdfPCell celdaNombre = new PdfPCell(new Phrase(nombreAlumno, contentFont));
            PdfPCell celdaObs = new PdfPCell(new Phrase(observaciones, contentFont));
            PdfPCell celdaMonto = new PdfPCell(new Phrase(montoStr, contentFont));

            // Configurar celdas sin bordes y con padding mínimo
            celdaId.setBorder(Rectangle.NO_BORDER);
            celdaCodigo.setBorder(Rectangle.NO_BORDER);
            celdaNombre.setBorder(Rectangle.NO_BORDER);
            celdaObs.setBorder(Rectangle.NO_BORDER);
            celdaMonto.setBorder(Rectangle.NO_BORDER);

            // Establecer padding mínimo
            celdaId.setPadding(2);
            celdaCodigo.setPadding(2);
            celdaNombre.setPadding(2);
            celdaObs.setPadding(2);
            celdaMonto.setPadding(2);

            // Alinear monto a la derecha
            celdaMonto.setHorizontalAlignment(Element.ALIGN_RIGHT);

            // Añadir celdas a la tabla
            tablaPagos.addCell(celdaId);
            tablaPagos.addCell(celdaCodigo);
            tablaPagos.addCell(celdaNombre);
            tablaPagos.addCell(celdaObs);
            tablaPagos.addCell(celdaMonto);
        }
        return tablaPagos;
    }
}

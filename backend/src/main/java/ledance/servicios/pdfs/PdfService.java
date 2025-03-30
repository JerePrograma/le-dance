package ledance.servicios.pdfs;

import com.lowagie.text.Document;
import com.lowagie.text.DocumentException;
import com.lowagie.text.Element;
import com.lowagie.text.Font;
import com.lowagie.text.Image;
import com.lowagie.text.PageSize;
import com.lowagie.text.Paragraph;
import com.lowagie.text.Rectangle;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter;
import ledance.entidades.Bonificacion;
import ledance.entidades.DetallePago;
import ledance.entidades.Pago;
import ledance.entidades.Recargo;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Objects;

@Service
public class PdfService {

    /**
     * Genera un PDF del recibo de pago utilizando OpenPDF.
     *
     * @param pago Objeto Pago con todos sus detalles
     * @return Un arreglo de bytes que representa el PDF generado.
     * @throws IOException En caso de error de entrada/salida
     */
    public byte[] generarReciboPdf(Pago pago) throws IOException {
        ByteArrayOutputStream bos = new ByteArrayOutputStream();

        try (Document document = new Document(PageSize.A4)) {
            PdfWriter.getInstance(document, bos);
            document.open();
            document.setMargins(36, 36, 36, 36); // Márgenes uniformes

            // 1. Encabezado con logo e información de contacto
            PdfPTable headerTable = new PdfPTable(2);
            headerTable.setWidthPercentage(100);
            headerTable.setWidths(new float[]{1, 3});

            // Logo (si está disponible)
            try {
                Image logo = Image.getInstance(Objects.requireNonNull(getClass().getResource("/RECIBO.jpg")));
                logo.scaleToFit(100, 100);
                PdfPCell logoCell = new PdfPCell(logo);
                logoCell.setBorder(Rectangle.NO_BORDER);
                logoCell.setHorizontalAlignment(Element.ALIGN_CENTER);
                logoCell.setVerticalAlignment(Element.ALIGN_MIDDLE);
                headerTable.addCell(logoCell);
            } catch (Exception e) {
                // Si no hay logo, agregar celda vacía
                PdfPCell emptyCell = new PdfPCell(new Paragraph("LE DANCE ARTE ESCUELA"));
                emptyCell.setBorder(Rectangle.NO_BORDER);
                headerTable.addCell(emptyCell);
            }

            // Información de contacto
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

            document.add(headerTable);
            document.add(new Paragraph(" "));

            // 2. Título y número de recibo
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

            document.add(titleTable);

            // 3. Información del cliente
            document.add(new Paragraph("Fecha: " + pago.getFecha()));
            document.add(new Paragraph("Sres. : " + pago.getAlumno().getApellido() + " " +
                    pago.getAlumno().getNombre() + "        Alumno Nº: " + pago.getAlumno().getId()));

            // Línea separadora
            Paragraph linea = new Paragraph("_______________________________________________________________________________________");
            document.add(linea);
            document.add(new Paragraph(" "));

            // 4. Monto recibido
            Font montoFont = new Font(Font.HELVETICA, 11, Font.BOLD);
            Paragraph recibido = new Paragraph(
                    "RECIBIMOS LA SUMA DE: $" + String.format("%,.2f", pago.getMontoPagado()).replace('.', ',') +
                            "    " + convertirMontoEnTexto(pago.getMontoPagado()).toUpperCase() + "*********",
                    montoFont);
            document.add(recibido);
            document.add(new Paragraph(" "));
            document.add(new Paragraph("SEGÚN EL SIGUIENTE DETALLE:", montoFont));
            document.add(new Paragraph(" "));

            // 5. Tabla de detalles mejorada
            PdfPTable table = new PdfPTable(8); // Cod. | Concepto | Cuota/Can | Valor | Bonif | Rec | Importe | Abonado
            table.setWidthPercentage(100);
            table.setWidths(new float[]{2, 8, 4, 4, 3, 3, 4, 4});

            Font headerFont = new Font(Font.HELVETICA, 8, Font.BOLD);

            // Encabezados de tabla
            addHeaderCell(table, "Cod.", headerFont);
            addHeaderCell(table, "Concepto", headerFont);
            addHeaderCell(table, "Cuota/Can", headerFont);
            addHeaderCell(table, "Valor", headerFont);
            addHeaderCell(table, "Bonif", headerFont);
            addHeaderCell(table, "Rec", headerFont);
            addHeaderCell(table, "Importe", headerFont);
            addHeaderCell(table, "Abonado", headerFont);

            Font cellFont = new Font(Font.HELVETICA, 8, Font.NORMAL);
            double total = 0.0;

            for (DetallePago det : pago.getDetallePagos()) {
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
                addCell(table, "$ " + String.format("%,.2f", det.getaCobrar()), cellFont); // Abonado igual al importe

                total += det.getaCobrar();
            }

            document.add(table);
            document.add(new Paragraph(" "));

            // 6. Observaciones
            document.add(new Paragraph(pago.getObservaciones(), new Font(Font.HELVETICA, 10, Font.NORMAL)));

            document.add(new Paragraph(" "));
            document.add(new Paragraph("_______________________________________________________________________________________"));
            document.add(new Paragraph(" "));

            // 7. Total
            Paragraph totalParrafo = new Paragraph("TOTAL   $ " + String.format("%,.2f", total),
                    new Font(Font.HELVETICA, 12, Font.BOLD));
            totalParrafo.setAlignment(Element.ALIGN_RIGHT);
            document.add(totalParrafo);

        } catch (DocumentException e) {
            e.printStackTrace();
        }

        return bos.toByteArray();
    }

    // Método auxiliar para agregar celdas de encabezado a la tabla
    private void addHeaderCell(PdfPTable table, String text, Font font) {
        PdfPCell cell = new PdfPCell(new Paragraph(text, font));
        cell.setHorizontalAlignment(Element.ALIGN_CENTER);
        cell.setVerticalAlignment(Element.ALIGN_MIDDLE);
        cell.setPadding(3);
        table.addCell(cell);
    }

    // Método auxiliar para agregar celdas normales a la tabla
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

    private double calcularDescuento(double valorBase, Bonificacion bonificacion) {
        if (bonificacion == null) {
            return 0.0;
        }
        double descuentoFijo = bonificacion.getValorFijo();
        double descuentoPorcentaje = (bonificacion.getPorcentajeDescuento() / 100.0) * valorBase;
        return descuentoFijo + descuentoPorcentaje;
    }

    private double calcularRecargo(double valorBase, Recargo recargo) {
        if (recargo == null) {
            return 0.0;
        }
        double recargoFijo;
        try {
            recargoFijo = recargo.getValorFijo();
        } catch (Exception e) {
            recargoFijo = 0.0;
        }
        double recargoPorcentaje = (recargo.getPorcentaje() / 100.0) * valorBase;
        return recargoFijo + recargoPorcentaje;
    }
}
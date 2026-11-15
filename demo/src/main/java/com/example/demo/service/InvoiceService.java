package com.example.demo.service;

import java.awt.Color;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.text.NumberFormat;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import javax.imageio.ImageIO;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.demo.entities.Client;
import com.example.demo.entities.Order;
import com.example.demo.entities.OrderDetail;
import com.example.demo.entities.OrderDetailAddOn;
import com.example.demo.errors.ApiException;
import com.example.demo.repository.OrderRepository;
import com.google.zxing.BarcodeFormat;
import com.google.zxing.EncodeHintType;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;
import com.lowagie.text.Document;
import com.lowagie.text.DocumentException;
import com.lowagie.text.Element;
import com.lowagie.text.Font;
import com.lowagie.text.FontFactory;
import com.lowagie.text.PageSize;
import com.lowagie.text.Paragraph;
import com.lowagie.text.Phrase;
import com.lowagie.text.Rectangle;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter;
import org.springframework.core.io.ClassPathResource;
import com.lowagie.text.Chunk;
import com.lowagie.text.Image;
import com.lowagie.text.pdf.ColumnText;
import com.lowagie.text.pdf.PdfContentByte;

// Facturas de un pedido pagado: ticket "físico" (el que abre el QR) y factura
// electrónica (la que se envía por correo). Trabaja con una copia de los datos
// (InvoiceData) para no depender de la sesión de base de datos al armar el PDF.
@Service
public class InvoiceService {

    public record Line(String name, String addOns, int quantity, double unitPrice, double total) {
    }

    public record InvoiceData(Integer orderId, Integer clientId, String number, LocalDateTime date,
            String clientName, String clientEmail, String address, List<Line> lines,
            double subtotal, double discount, double deliveryFee, double total, String couponCode) {
    }

    private static final DateTimeFormatter DATE = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    @Autowired
    private OrderRepository orders;

    @Value("${terra.invoice.secret}")
    private String secret;

    @Value("${terra.public-backend-url}")
    private String publicBackendUrl;

    @Value("${terra.invoice.company-name:Terra Galega}")
    private String company;

    @Value("${terra.invoice.nit:}")
    private String nit;

    // ---------- Datos ----------

    // Debe llamarse dentro de la transacción del pedido (lee líneas y adicionales)
    public InvoiceData snapshot(Order o) {
        List<Line> lines = new ArrayList<>();
        for (OrderDetail d : o.getDetails()) {
            double extras = 0;
            List<String> names = new ArrayList<>();
            for (OrderDetailAddOn l : d.getOrderDetailAddOns()) {
                names.add(l.getAddOn().getName());
                extras += l.getUnitPrice() != null ? l.getUnitPrice() : l.getAddOn().getPrice();
            }
            double unit = d.getUnitPrice() + extras;
            lines.add(new Line(d.getProduct().getName(), String.join(", ", names),
                    d.getQuantity(), unit, unit * d.getQuantity()));
        }
        Client c = o.getClient();
        return new InvoiceData(o.getId(), c.getId(), String.format("TG-%06d", o.getId()), o.getCreatedAt(),
                (c.getName() + " " + c.getLastName()).trim(), c.getEmail(), o.getAddress(), lines,
                o.getSubtotal(),
                o.getDiscountAmount() != null ? o.getDiscountAmount() : 0.0,
                o.getDeliveryFee() != null ? o.getDeliveryFee() : 0.0,
                o.getTotal(),
                o.getCoupon() != null ? o.getCoupon().getCode() : null);
    }

    @Transactional(transactionManager = "transactionManager", readOnly = true)
    public InvoiceData snapshotById(Integer orderId) {
        Order o = orders.findById(orderId)
                .orElseThrow(() -> ApiException.notFound("Factura inexistente."));
        if (!"PAID".equals(o.getPaymentState())) {
            throw ApiException.notFound("Factura inexistente.");
        }
        return snapshot(o);
    }

    // ---------- Enlace firmado + QR ----------

    public String token(Integer orderId) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            return Base64.getUrlEncoder().withoutPadding()
                    .encodeToString(mac.doFinal(("invoice:" + orderId).getBytes(StandardCharsets.UTF_8)));
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }

    public boolean validToken(Integer orderId, String token) {
        return token != null && MessageDigest.isEqual(
                token(orderId).getBytes(StandardCharsets.UTF_8), token.getBytes(StandardCharsets.UTF_8));
    }

    public String previewUrl(Integer orderId) {
        return publicBackendUrl + "/invoices/public/" + orderId + "?t=" + token(orderId);
    }

    public byte[] qrPng(String text) {
        try {
            int size = 360;
            BitMatrix m = new QRCodeWriter().encode(text, BarcodeFormat.QR_CODE, size, size,
                    Map.of(EncodeHintType.MARGIN, 1));
            BufferedImage img = new BufferedImage(size, size, BufferedImage.TYPE_INT_RGB);
            for (int x = 0; x < size; x++) {
                for (int y = 0; y < size; y++) {
                    img.setRGB(x, y, m.get(x, y) ? 0x000000 : 0xFFFFFF);
                }
            }
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            ImageIO.write(img, "png", out);
            return out.toByteArray();
        } catch (Exception e) {
            throw new IllegalStateException("No se pudo generar el QR", e);
        }
    }

    // ---------- PDF ----------

    // Colores de la marca (los mismos del reporte de ventas)
    private static final Color CHARCOAL = new Color(0x2c, 0x2c, 0x2c);
    private static final Color FOREST = new Color(0x55, 0x61, 0x41);
    private static final Color CREAM = new Color(0xf5, 0xf2, 0xeb);
    private static final Color TERRA = new Color(0xb2, 0x57, 0x1f);
    private static final Color SOFT_LINE = new Color(0xe4, 0xdd, 0xcf);

    public static String money(double v) {
        return "$" + NumberFormat.getIntegerInstance(Locale.forLanguageTag("es-CO")).format(Math.round(v));
    }

    // Ticket como saldría impreso (rollo de caja): lo que abre el QR
    public byte[] physicalPdf(InvoiceData d) {
        Font bold = FontFactory.getFont(FontFactory.COURIER_BOLD, 10, CHARCOAL);
        Font txt = FontFactory.getFont(FontFactory.COURIER, 9, CHARCOAL);
        Font big = FontFactory.getFont(FontFactory.COURIER_BOLD, 12, TERRA);

        return pdf(PageSize.A6, 18, 18, (doc, w) -> {
            Image logo = logo("logo-oscuro.png", 150, 70);
            if (logo != null) {
                logo.setAlignment(Element.ALIGN_CENTER);
                doc.add(logo);
            } else {
                doc.add(centered(company.toUpperCase(), bold));
            }
            if (!nit.isBlank())
                doc.add(centered("NIT " + nit, txt));
            doc.add(centered("Pedido " + d.number(), txt));
            doc.add(centered(d.date().format(DATE), txt));
            doc.add(divider(TERRA, 1f));

            doc.add(new Paragraph("Cliente: " + d.clientName(), txt));
            doc.add(new Paragraph("Entrega: " + d.address(), txt));
            doc.add(divider(TERRA, 1f));

            for (Line l : d.lines()) {
                doc.add(pair(l.quantity() + "x " + l.name(), money(l.total()), txt));
                if (!l.addOns().isBlank())
                    doc.add(new Paragraph("   + " + l.addOns(), txt));
            }
            doc.add(divider(TERRA, 1f));

            doc.add(pair("Subtotal", money(d.subtotal()), txt));
            if (d.discount() > 0) {
                doc.add(pair("Descuento" + (d.couponCode() != null ? " (" + d.couponCode() + ")" : ""),
                        "-" + money(d.discount()), txt));
            }
            doc.add(pair("Domicilio", money(d.deliveryFee()), txt));
            doc.add(divider(FOREST, 1.5f));
            doc.add(pair("TOTAL", money(d.total()), big));
            doc.add(divider(TERRA, 1f));

            doc.add(centered("Pagado con tarjeta", txt));
            doc.add(centered("¡Gracias por tu compra!", bold));
        });
    }

    // Factura electrónica en PDF (la que va adjunta al correo): franja charcoal con
    // el logo,
    // igual que el reporte de ventas
    public byte[] electronicPdf(InvoiceData d) {
        Font txt = FontFactory.getFont(FontFactory.HELVETICA, 10, CHARCOAL);
        Font bold = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 10, CHARCOAL);
        Font label = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 8, FOREST);
        Font head = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 10, CREAM);
        Font totalFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 12, CREAM);
        Font muted = FontFactory.getFont(FontFactory.HELVETICA, 8, new Color(110, 110, 110));

        return pdf(PageSize.A4, 40, 125, (doc, w) -> {
            float pageW = PageSize.A4.getWidth();
            float pageH = PageSize.A4.getHeight();
            float band = 92;

            // Franja superior charcoal con una línea terracota debajo
            PdfContentByte under = w.getDirectContentUnder();
            under.setColorFill(CHARCOAL);
            under.rectangle(0, pageH - band, pageW, band);
            under.fill();
            under.setColorFill(TERRA);
            under.rectangle(0, pageH - band - 3, pageW, 3);
            under.fill();

            Image logo = logo("logo-claro.png", 150, 64);
            if (logo != null) {
                logo.setAbsolutePosition(40, pageH - band + (band - logo.getScaledHeight()) / 2);
                doc.add(logo);
            }

            PdfContentByte over = w.getDirectContent();
            ColumnText.showTextAligned(over, Element.ALIGN_RIGHT,
                    new Phrase("Factura electrónica de venta",
                            FontFactory.getFont(FontFactory.HELVETICA_BOLD, 16, CREAM)),
                    pageW - 40, pageH - 40, 0);
            ColumnText.showTextAligned(over, Element.ALIGN_RIGHT,
                    new Phrase("No. " + d.number(), FontFactory.getFont(FontFactory.HELVETICA, 10, CREAM)),
                    pageW - 40, pageH - 58, 0);
            ColumnText.showTextAligned(over, Element.ALIGN_RIGHT,
                    new Phrase(d.date().format(DATE), FontFactory.getFont(FontFactory.HELVETICA, 10, CREAM)),
                    pageW - 40, pageH - 72, 0);

            // Datos en dos columnas
            PdfPTable info = new PdfPTable(new float[] { 1f, 1f });
            info.setWidthPercentage(100);
            info.addCell(block("CLIENTE", d.clientName() + "\n" + d.clientEmail(), label, txt));
            info.addCell(block("ENTREGA", d.address(), label, txt));
            info.addCell(block("EMISOR", company + (nit.isBlank() ? "" : "\nNIT " + nit), label, txt));
            info.addCell(block("FORMA DE PAGO", "Tarjeta (pago en línea)", label, txt));
            doc.add(info);
            doc.add(new Paragraph(" "));

            // Detalle: encabezado verde bosque y filas alternadas en crema
            PdfPTable t = new PdfPTable(new float[] { 5f, 1f, 2f, 2f });
            t.setWidthPercentage(100);
            int[] aligns = { Element.ALIGN_LEFT, Element.ALIGN_CENTER, Element.ALIGN_RIGHT, Element.ALIGN_RIGHT };
            String[] heads = { "Descripción", "Cant.", "V. unitario", "Total" };
            for (int i = 0; i < heads.length; i++) {
                t.addCell(cell(heads[i], head, aligns[i], FOREST));
            }
            int n = 0;
            for (Line l : d.lines()) {
                Color bg = (n++ % 2 == 0) ? CREAM : Color.WHITE;
                String desc = l.name() + (l.addOns().isBlank() ? "" : "\n+ " + l.addOns());
                t.addCell(cell(desc, txt, Element.ALIGN_LEFT, bg));
                t.addCell(cell(String.valueOf(l.quantity()), txt, Element.ALIGN_CENTER, bg));
                t.addCell(cell(money(l.unitPrice()), txt, Element.ALIGN_RIGHT, bg));
                t.addCell(cell(money(l.total()), txt, Element.ALIGN_RIGHT, bg));
            }
            doc.add(t);
            doc.add(divider(FOREST, 1.2f));

            // Totales: el TOTAL va en una barra terracota
            PdfPTable tot = new PdfPTable(new float[] { 3f, 2f });
            tot.setWidthPercentage(45);
            tot.setHorizontalAlignment(Element.ALIGN_RIGHT);
            tot.addCell(cell("Subtotal", txt, Element.ALIGN_LEFT, null));
            tot.addCell(cell(money(d.subtotal()), txt, Element.ALIGN_RIGHT, null));
            if (d.discount() > 0) {
                tot.addCell(cell("Descuento", txt, Element.ALIGN_LEFT, null));
                tot.addCell(cell("-" + money(d.discount()), txt, Element.ALIGN_RIGHT, null));
            }
            tot.addCell(cell("Domicilio", txt, Element.ALIGN_LEFT, null));
            tot.addCell(cell(money(d.deliveryFee()), txt, Element.ALIGN_RIGHT, null));
            tot.addCell(cell("TOTAL", totalFont, Element.ALIGN_LEFT, TERRA));
            tot.addCell(cell(money(d.total()), totalFont, Element.ALIGN_RIGHT, TERRA));
            doc.add(tot);

            doc.add(new Paragraph(" "));
            doc.add(divider(SOFT_LINE, 1f));
            doc.add(new Paragraph(
                    "Documento generado electrónicamente. Conserva este archivo como soporte de tu compra.", muted));
        });
    }

    // ---------- Utilidades de PDF ----------

    private interface Body {
        void write(Document doc, PdfWriter writer) throws DocumentException;
    }

    private byte[] pdf(Rectangle size, float side, float top, Body body) {
        try {
            Document doc = new Document(size, side, side, top, side);
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            PdfWriter writer = PdfWriter.getInstance(doc, out);
            doc.open();
            body.write(doc, writer);
            doc.close();
            return out.toByteArray();
        } catch (DocumentException e) {
            throw new IllegalStateException("No se pudo generar la factura", e);
        }
    }

    // Logo desde src/main/resources/invoice/. Si falta el archivo, la factura sale
    // sin logo.
    private Image logo(String file, float maxW, float maxH) {
        try (var in = new ClassPathResource("invoice/" + file).getInputStream()) {
            Image img = Image.getInstance(in.readAllBytes());
            img.scaleToFit(maxW, maxH);
            return img;
        } catch (Exception e) {
            return null;
        }
    }

    // Raya divisoria de color
    private PdfPTable divider(Color color, float thickness) {
        PdfPTable t = new PdfPTable(1);
        t.setWidthPercentage(100);
        t.setSpacingBefore(4);
        t.setSpacingAfter(4);
        PdfPCell c = new PdfPCell(new Phrase(" ", FontFactory.getFont(FontFactory.HELVETICA, 1)));
        c.setPadding(0);
        c.setFixedHeight(2);
        c.setBorder(Rectangle.BOTTOM);
        c.setBorderColorBottom(color);
        c.setBorderWidthBottom(thickness);
        t.addCell(c);
        return t;
    }

    // Celda con etiqueta pequeña verde y valor debajo
    private PdfPCell block(String title, String value, Font label, Font txt) {
        Phrase p = new Phrase();
        p.add(new Chunk(title + "\n", label));
        p.add(new Chunk(value, txt));
        PdfPCell c = new PdfPCell(p);
        c.setBorder(Rectangle.NO_BORDER);
        c.setPaddingBottom(8);
        return c;
    }

    private Paragraph centered(String text, Font f) {
        Paragraph p = new Paragraph(text, f);
        p.setAlignment(Element.ALIGN_CENTER);
        return p;
    }

    private PdfPTable pair(String left, String right, Font f) {
        PdfPTable t = new PdfPTable(new float[] { 3f, 1.4f });
        t.setWidthPercentage(100);
        t.addCell(cell(left, f, Element.ALIGN_LEFT, null));
        t.addCell(cell(right, f, Element.ALIGN_RIGHT, null));
        return t;
    }

    private PdfPCell cell(String text, Font f, int align, Color background) {
        PdfPCell c = new PdfPCell(new Phrase(text, f));
        c.setBorder(Rectangle.NO_BORDER);
        c.setHorizontalAlignment(align);
        c.setPadding(5);
        if (background != null)
            c.setBackgroundColor(background);
        return c;
    }
}
package com.example.demo.service;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Base64;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;
import org.springframework.core.io.ByteArrayResource;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;

// Envía los correos de la aplicación usando el servidor SMTP configurado en
// application.properties (en desarrollo, Mailpit: http://localhost:8025).
// El diseño de cada correo vive en una plantilla de src/main/resources/templates/email;
// aquí solo se llena la plantilla y se envía el mensaje.
@Service
public class EmailService {

    // Spring crea este objeto con los datos de spring.mail.*
    @Autowired
    private JavaMailSender mailSender;

    // Motor de Thymeleaf que ya usa el proyecto para sus páginas: aquí llena las
    // plantillas de correo
    @Autowired
    private TemplateEngine templateEngine;

    // Dirección que aparece como remitente
    @Value("${terra.mail.from}")
    private String from;

    // Clave de la API de Brevo (BREVO_API_KEY). Si está vacía se envía por SMTP.
    @Value("${brevo.api-key:}")
    private String brevoApiKey;

    // Correo para recuperar la contraseña: plantilla templates/email/reset-password.html
    public void sendPasswordResetEmail(String to, String name, String link) throws MessagingException {
        Context context = new Context();
        context.setVariable("name", name);
        context.setVariable("link", link);
        String html = templateEngine.process("email/reset-password", context);

        String plain = "Recupera tu contraseña de Terra Galega\n\n"
                + "Elige una contraseña nueva abriendo este enlace:\n" + link + "\n\n"
                + "El enlace vence en 30 minutos y solo se puede usar una vez. "
                + "Si no pediste cambiar tu contraseña, ignora este mensaje.";

        send(to, "Recupera tu contraseña de Terra Galega", plain, html);
    }

    // Correo para confirmar la cuenta: plantilla templates/email/verify-email.html
    public void sendVerificationEmail(String to, String name, String link) throws MessagingException {
        // Thymeleaf escapa el nombre automáticamente al llenar la plantilla
        Context context = new Context();
        context.setVariable("name", name);
        context.setVariable("link", link);
        String html = templateEngine.process("email/verify-email", context);

        // Versión en texto plano para los clientes de correo que no muestran HTML
        String plain = "¡Bienvenido a Terra Galega!\n\n"
                + "Confirma tu cuenta abriendo este enlace:\n" + link + "\n\n"
                + "El enlace vence en 24 horas. Si no creaste esta cuenta, ignora este mensaje.";

        send(to, "Confirma tu cuenta en Terra Galega", plain, html);
    }

    // LOGIN CON MICROSOFT DESACTIVADO (no se puede usar).
    // La universidad no nos da permisos en Azure (Microsoft Entra ID) para registrar la
    // aplicación y obtener el Client ID, así que no lo podemos usar. También se intentó
    // con una cuenta personal de Microsoft, pero tampoco lo permitió.
    // Se deja comentado por si en el futuro se consiguen los permisos.
    // public void sendMicrosoftCode(String to, String code) throws MessagingException {
        // String message = "Introduce este código en Terra Galega para asociar tu cuenta Microsoft: " + code
                // + ". Vence en 10 minutos. Si no lo solicitaste, ignora este mensaje.";
        // send(to, "Confirma tu acceso con Microsoft", message, "<p>" + message + "</p>");
    // }

    // Factura electrónica del pedido, adjunta en PDF: plantilla
    // templates/email/invoice-email.html
    public void sendInvoiceEmail(InvoiceService.InvoiceData d, byte[] pdf) throws MessagingException {
        Context context = new Context();
        context.setVariable("name", d.clientName());
        context.setVariable("number", d.number());
        context.setVariable("total", InvoiceService.money(d.total()));
        String html = templateEngine.process("email/invoice-email", context);

        String plain = "Hola " + d.clientName() + ",\n\n"
                + "Gracias por tu compra. Adjuntamos la factura electrónica " + d.number()
                + " por " + InvoiceService.money(d.total()) + ".";

        deliver(d.clientEmail(), "Tu factura electrónica " + d.number() + " - Terra Galega", plain, html,
                "factura-" + d.number() + ".pdf", pdf, "application/pdf");
    }

    // Informe de ventas que el admin manda a cualquier correo:
    // templates/email/report-email.html
    public void sendReportEmail(String to, String subject, String filename, byte[] data, String contentType)
            throws MessagingException {
        Context context = new Context();
        context.setVariable("filename", filename);
        String html = templateEngine.process("email/report-email", context);

        deliver(to, subject, "Adjuntamos el informe de ventas de Terra Galega: " + filename, html,
                filename, data, contentType);
    }

    // Correo sin adjunto
    private void send(String to, String subject, String plain, String html) throws MessagingException {
        deliver(to, subject, plain, html, null, null, null);
    }

    // Punto único de envío. Si hay BREVO_API_KEY se manda por la API web de Brevo (HTTPS):
    // Railway bloquea el SMTP (puertos 25/465/587) en los planes sin Pro. Sin esa clave se
    // usa el SMTP configurado en spring.mail.* (Mailpit/Gmail en el PC).
    private void deliver(String to, String subject, String plain, String html,
            String filename, byte[] data, String contentType) throws MessagingException {
        if (brevoApiKey != null && !brevoApiKey.isBlank()) {
            sendViaBrevo(to, subject, plain, html, filename, data);
            return;
        }
        MimeMessage message = mailSender.createMimeMessage();
        MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
        helper.setFrom(from);
        helper.setTo(to);
        helper.setSubject(subject);
        helper.setText(plain, html);
        if (filename != null) {
            helper.addAttachment(filename, new ByteArrayResource(data), contentType);
        }
        mailSender.send(message);
    }

    // POST https://api.brevo.com/v3/smtp/email (responde 201 si lo acepta).
    // El remitente (terra.mail.from) debe estar verificado como "Sender" en Brevo.
    private void sendViaBrevo(String to, String subject, String plain, String html,
            String filename, byte[] data) throws MessagingException {
        StringBuilder json = new StringBuilder();
        json.append("{\"sender\":{\"name\":\"Terra Galega\",\"email\":").append(jsonString(from)).append("},")
                .append("\"to\":[{\"email\":").append(jsonString(to)).append("}],")
                .append("\"subject\":").append(jsonString(subject)).append(',')
                .append("\"htmlContent\":").append(jsonString(html)).append(',')
                .append("\"textContent\":").append(jsonString(plain));
        if (filename != null) {
            json.append(",\"attachment\":[{\"name\":").append(jsonString(filename))
                    .append(",\"content\":\"").append(Base64.getEncoder().encodeToString(data)).append("\"}]");
        }
        json.append('}');

        try {
            HttpRequest request = HttpRequest.newBuilder(URI.create("https://api.brevo.com/v3/smtp/email"))
                    .timeout(Duration.ofSeconds(30))
                    .header("api-key", brevoApiKey.trim())
                    .header("Content-Type", "application/json")
                    .header("Accept", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(json.toString(), StandardCharsets.UTF_8))
                    .build();
            HttpResponse<String> response = HttpClient.newHttpClient()
                    .send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() / 100 != 2) {
                throw new MessagingException("Brevo rechazó el correo (" + response.statusCode() + "): "
                        + response.body());
            }
        } catch (java.io.IOException e) {
            throw new MessagingException("No se pudo contactar con Brevo: " + e.getMessage(), e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new MessagingException("Envío a Brevo interrumpido", e);
        }
    }

    // Convierte un texto a literal JSON (con comillas y escapes)
    private static String jsonString(String s) {
        StringBuilder sb = new StringBuilder("\"");
        for (char c : s.toCharArray()) {
            switch (c) {
                case '"' -> sb.append("\\\"");
                case '\\' -> sb.append("\\\\");
                case '\n' -> sb.append("\\n");
                case '\r' -> sb.append("\\r");
                case '\t' -> sb.append("\\t");
                default -> {
                    if (c < 0x20) {
                        sb.append(String.format("\\u%04x", (int) c));
                    } else {
                        sb.append(c);
                    }
                }
            }
        }
        return sb.append('"').toString();
    }
}

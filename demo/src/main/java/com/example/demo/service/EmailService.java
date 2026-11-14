package com.example.demo.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

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

    // Motor de Thymeleaf que ya usa el proyecto para sus páginas: aquí llena las plantillas de correo
    @Autowired
    private TemplateEngine templateEngine;

    // Dirección que aparece como remitente
    @Value("${terra.mail.from}")
    private String from;

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

    // Arma el mensaje (UTF-8, para las tildes) y lo envía
    private void send(String to, String subject, String plain, String html) throws MessagingException {
        MimeMessage message = mailSender.createMimeMessage();
        MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
        helper.setFrom(from);
        helper.setTo(to);
        helper.setSubject(subject);
        helper.setText(plain, html);
        mailSender.send(message);
    }
}
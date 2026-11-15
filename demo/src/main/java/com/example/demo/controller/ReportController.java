package com.example.demo.controller;

import java.io.IOException;
import java.util.Objects;
import java.util.Set;

import org.springframework.http.MediaType;
import org.springframework.mail.MailException;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import com.example.demo.dto.MessageDTO;
import com.example.demo.errors.ApiException;
import com.example.demo.service.EmailRules;
import com.example.demo.service.EmailService;

import jakarta.mail.MessagingException;

// Recibe el informe que el admin generó en el navegador y lo envía por correo
@RestController
@RequestMapping("/reports")
public class ReportController {

    private static final long MAX_BYTES = 15L * 1024 * 1024;
    private static final Set<String> EXTENSIONS = Set.of("pdf", "xlsx", "csv");

    private final EmailService email;

    public ReportController(EmailService email) {
        this.email = email;
    }

    @PostMapping(value = "/email", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public MessageDTO email(
            @RequestParam("to") String to,
            @RequestParam(value = "subject", required = false) String subject,
            @RequestPart("file") MultipartFile file) throws IOException {

        String recipient = to.trim();
        EmailRules.validate(recipient);

        String name = Objects.requireNonNullElse(file.getOriginalFilename(), "informe")
                .replaceAll("[^A-Za-z0-9._ -]", "_");
        String ext = name.contains(".") ? name.substring(name.lastIndexOf('.') + 1).toLowerCase() : "";

        if (file.isEmpty() || file.getSize() > MAX_BYTES || !EXTENSIONS.contains(ext)) {
            throw ApiException.badRequest("El archivo del informe no es válido.");
        }

        String subj = (subject == null || subject.isBlank())
                ? "Informe de ventas - Terra Galega"
                : subject.replaceAll("[\\r\\n]", " ").trim();
        if (subj.length() > 150)
            subj = subj.substring(0, 150);

        String contentType = switch (ext) {
            case "pdf" -> "application/pdf";
            case "xlsx" -> "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet";
            default -> "text/csv";
        };

        try {
            email.sendReportEmail(recipient, subj, name, file.getBytes(), contentType);
        } catch (MessagingException | MailException e) {
            throw ApiException.serviceUnavailable("No se pudo enviar el correo. Revisa la configuración de Gmail.");
        }
        return new MessageDTO("Informe enviado a " + recipient);
    }
}
package com.example.demo.service;

import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.Base64;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.example.demo.entities.Client;
import com.example.demo.entities.PasswordResetToken;
import com.example.demo.entities.UserEntity;
import com.example.demo.errors.ApiException;
import com.example.demo.repository.ClientRepository;
import com.example.demo.repository.PasswordResetTokenRepository;
import com.example.demo.repository.UserRepository;

// Recuperación de contraseña por correo.
// 1. requestReset: crea un token, guarda solo su hash y envía un enlace al front.
// 2. resetPassword: con el token del enlace cambia la contraseña y borra el token.
@Service
public class PasswordResetService {

    private static final Logger log = LoggerFactory.getLogger(PasswordResetService.class);

    private static final SecureRandom RANDOM = new SecureRandom();

    // Segundos que deben pasar entre una solicitud y la siguiente
    private static final long COOLDOWN_SECONDS = 60;

    private static final String INVALID_LINK_MESSAGE = "El enlace no es válido o ya fue usado. Pide uno nuevo.";
    private static final String EXPIRED_LINK_MESSAGE = "El enlace venció. Pide uno nuevo desde el inicio de sesión.";

    @Autowired
    private PasswordResetTokenRepository tokenRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ClientRepository clientRepository;

    @Autowired
    private EmailService emailService;

    @Autowired
    private PasswordEncoder passwordEncoder;

    // Misma URL del front que usa el enlace de confirmación de correo
    @Value("${terra.email-link-url:${terra.frontend-url}}")
    private String frontendUrl;

    // Cuánto dura el enlace; si no se define en application.properties son 30 minutos
    @Value("${terra.password-reset.expiration-minutes:30}")
    private long expirationMinutes;

    // Envía el enlace de recuperación. Siempre termina sin error, exista o no la
    // cuenta: así nadie puede usar este endpoint para saber qué correos están
    // registrados. Si el último enlace se pidió hace menos de un minuto no envía otro.
    @Transactional
    public void requestReset(String email) {
        if (email == null || email.isBlank()) {
            return;
        }

        userRepository.findByUsername(email.trim()).ifPresent(user -> {
            boolean tooSoon = tokenRepository.findFirstByUserOrderByCreatedAtDesc(user)
                    .map(last -> last.getCreatedAt().plusSeconds(COOLDOWN_SECONDS)
                            .isAfter(LocalDateTime.now()))
                    .orElse(false);
            if (tooSoon) {
                return;
            }

            // Solo sirve el último enlace
            tokenRepository.deleteByUser(user);

            String token = generateToken();
            LocalDateTime now = LocalDateTime.now();
            tokenRepository.save(PasswordResetToken.builder()
                    .tokenHash(EmailVerificationService.hash(token))
                    .user(user)
                    .createdAt(now)
                    .expiresAt(now.plusMinutes(expirationMinutes))
                    .build());

            Client client = clientRepository.findByEmail(user.getUsername());
            String name = client != null ? client.getName() : "";
            String link = frontendUrl + "/reset-password?token=" + token;

            try {
                emailService.sendPasswordResetEmail(user.getUsername(), name, link);
            } catch (Exception e) {
                log.error("No se pudo enviar el correo de recuperación a {}: {}", user.getUsername(), e.getMessage());
            }
        });
    }

    // Cambia la contraseña con el token del enlace. El token se borra al usarlo,
    // así que el mismo enlace no sirve dos veces.
    @Transactional
    public void resetPassword(String token, String newPassword) {
        if (token == null || token.isBlank()) {
            throw ApiException.badRequest(INVALID_LINK_MESSAGE);
        }
        if (newPassword == null || newPassword.length() < 6) {
            throw ApiException.badRequest("La nueva contraseña debe tener al menos 6 caracteres.");
        }
        if (newPassword.length() > 70) {
            throw ApiException.badRequest("La nueva contraseña es demasiado larga.");
        }

        PasswordResetToken stored = tokenRepository.findByTokenHash(EmailVerificationService.hash(token.trim()))
                .orElseThrow(() -> ApiException.badRequest(INVALID_LINK_MESSAGE));

        if (stored.getExpiresAt().isBefore(LocalDateTime.now())) {
            throw ApiException.badRequest(EXPIRED_LINK_MESSAGE);
        }

        UserEntity user = stored.getUser();
        user.setPassword(passwordEncoder.encode(newPassword));
        // Abrir el enlace demuestra que el correo es suyo: queda confirmado
        user.setEmailVerified(true);
        userRepository.save(user);
        tokenRepository.deleteByUser(user);
    }

    private String generateToken() {
        byte[] bytes = new byte[32];
        RANDOM.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }
}

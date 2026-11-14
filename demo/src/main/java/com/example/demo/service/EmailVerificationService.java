package com.example.demo.service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.HexFormat;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import com.example.demo.entities.Client;
import com.example.demo.entities.UserEntity;
import com.example.demo.entities.VerificationToken;
import com.example.demo.errors.ApiException;
import com.example.demo.repository.ClientRepository;
import com.example.demo.repository.UserRepository;
import com.example.demo.repository.VerificationTokenRepository;

// Confirmación de cuenta por correo.
// Crea un token aleatorio, guarda solo su hash (SHA-256) y envía al usuario un
// enlace al front con el token. El token original nunca se guarda en la base de datos.
@Service
public class EmailVerificationService {

    private static final Logger log = LoggerFactory.getLogger(EmailVerificationService.class);

    // Generador de números aleatorios seguro para crear los tokens
    private static final SecureRandom RANDOM = new SecureRandom();

    // Segundos que deben pasar entre un correo de confirmación y el siguiente reenvío
    private static final long RESEND_COOLDOWN_SECONDS = 60;

    private static final String INVALID_LINK_MESSAGE = "El enlace no es válido o ya fue usado.";
    private static final String EXPIRED_LINK_MESSAGE = "El enlace venció. Solicita uno nuevo desde el inicio de sesión.";

    @Autowired
    private VerificationTokenRepository tokenRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ClientRepository clientRepository;

    @Autowired
    private EmailService emailService;

    // URL del front: el enlace del correo apunta aquí
    @Value("${terra.frontend-url}")
    private String frontendUrl;

    // Cuánto dura el enlace; si no se define en application.properties son 24 horas
    @Value("${terra.verification.expiration-hours:24}")
    private long expirationHours;

    // Crea un token nuevo para el usuario y le envía el correo de confirmación.
    // Borra los tokens anteriores: solo el último enlace sirve.
    @Transactional
    public void sendVerification(UserEntity user, String email, String name) {
        tokenRepository.deleteByUser(user);

        String token = generateToken();
        LocalDateTime now = LocalDateTime.now();
        tokenRepository.save(VerificationToken.builder()
                .tokenHash(hash(token))
                .user(user)
                .createdAt(now)
                .expiresAt(now.plusHours(expirationHours))
                .build());

        String link = frontendUrl + "/verify-email?token=" + token;
        sendAfterCommit(email, name, link);
    }

    // Confirma la cuenta a partir del token que llegó en el enlace del correo.
    // Busca por el hash (el token original no se guarda), revisa que no haya vencido,
    // marca el correo como verificado y borra los tokens del usuario.
    @Transactional
    public void verify(String token) {
        if (token == null || token.isBlank()) {
            throw ApiException.badRequest(INVALID_LINK_MESSAGE);
        }

        VerificationToken stored = tokenRepository.findByTokenHash(hash(token.trim()))
                .orElseThrow(() -> ApiException.badRequest(INVALID_LINK_MESSAGE));

        if (stored.getExpiresAt().isBefore(LocalDateTime.now())) {
            throw ApiException.badRequest(EXPIRED_LINK_MESSAGE);
        }

        UserEntity user = stored.getUser();
        user.setEmailVerified(true);
        userRepository.save(user);
        tokenRepository.deleteByUser(user);
    }

    // Reenvía el correo de confirmación. Siempre termina sin error, exista o no la
    // cuenta, esté verificada o no: así nadie puede usar este endpoint para saber
    // qué correos están registrados. Si el último correo se envió hace menos de
    // un minuto no envía otro (evita usarlo para llenar de correos a alguien).
    @Transactional
    public void resend(String email) {
        if (email == null || email.isBlank()) {
            return;
        }

        userRepository.findByUsername(email.trim()).ifPresent(user -> {
            if (user.isEmailVerified()) {
                return;
            }

            boolean tooSoon = tokenRepository.findFirstByUserOrderByCreatedAtDesc(user)
                    .map(last -> last.getCreatedAt().plusSeconds(RESEND_COOLDOWN_SECONDS)
                            .isAfter(LocalDateTime.now()))
                    .orElse(false);
            if (tooSoon) {
                return;
            }

            Client client = clientRepository.findByEmail(user.getUsername());
            String name = client != null ? client.getName() : "";
            sendVerification(user, user.getUsername(), name);
        });
    }

    // El correo se envía cuando la transacción ya se guardó: así nunca llega un
    // enlace de una cuenta que se canceló. Si el envío falla, el registro no se
    // pierde (el usuario podrá pedir un reenvío).
    private void sendAfterCommit(String email, String name, String link) {
        Runnable send = () -> {
            try {
                emailService.sendVerificationEmail(email, name, link);
            } catch (Exception e) {
                log.error("No se pudo enviar el correo de confirmación a {}: {}", email, e.getMessage());
            }
        };

        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    send.run();
                }
            });
        } else {
            send.run();
        }
    }

    // 32 bytes aleatorios en Base64 seguro para URL (sin caracteres que se rompan en un enlace)
    private String generateToken() {
        byte[] bytes = new byte[32];
        RANDOM.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    // SHA-256 en hexadecimal (64 caracteres). También lo usará la confirmación
    // para buscar el token que llega en el enlace.
    public static String hash(String token) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256")
                    .digest(token.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest);
        } catch (NoSuchAlgorithmException e) {
            // SHA-256 existe en todas las versiones de Java
            throw new IllegalStateException(e);
        }
    }
}
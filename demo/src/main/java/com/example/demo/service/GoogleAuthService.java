package com.example.demo.service;

import com.example.demo.dto.AuthResponseDTO;
import com.example.demo.entities.Client;
import com.example.demo.entities.Role;
import com.example.demo.entities.RoleEntity;
import com.example.demo.entities.UserEntity;
import com.example.demo.errors.ApiException;
import com.example.demo.repository.ClientRepository;
import com.example.demo.repository.UserRepository;
import com.example.demo.security.JwtGenerator;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.oauth2.core.DelegatingOAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.core.OAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2TokenValidatorResult;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtException;
import org.springframework.security.oauth2.jwt.JwtTimestampValidator;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.UUID;

// "Continuar con Google" en la web y en la app.
// El front obtiene de Google un ID token (un JWT firmado por Google) y lo envía
// aquí. Se verifica la firma con las claves públicas de Google, el emisor, la
// caducidad y que el token sea para NUESTRO client ID. Si todo está bien:
// - si la cuenta de Google ya está enlazada, inicia sesión;
// - si existe un cliente con ese correo, enlaza Google a esa cuenta;
// - si no existe, crea el cliente con lo que da Google (nombre, apellido,
//   correo). Teléfono y dirección se piden después en "Completa tu perfil".
@Service
public class GoogleAuthService {

    private static final String JWKS_URI = "https://www.googleapis.com/oauth2/v3/certs";
    private static final Set<String> ISSUERS = Set.of("https://accounts.google.com", "accounts.google.com");

    private final List<String> clientIds;
    private final JwtDecoder decoder;

    private final UserRepository userRepository;
    private final ClientRepository clientRepository;
    private final UserAccountService userAccountService;
    private final CouponService couponService;
    private final AuthService authService;
    private final JwtGenerator jwtGenerator;

    private final NotificationService notificationService;

    public GoogleAuthService(
            // Client ID web de Google Cloud (el mismo que usan la web y la app).
            // Se pueden poner varios separados por coma.
            @Value("${terra.google.client-ids:}") String clientIds,
            UserRepository userRepository,
            ClientRepository clientRepository,
            UserAccountService userAccountService,
            CouponService couponService,
            NotificationService notificationService,
            AuthService authService,
            JwtGenerator jwtGenerator) {

        this.clientIds = Arrays.stream(clientIds.split(","))
                .map(String::trim)
                .filter(id -> !id.isEmpty())
                .toList();
        this.userRepository = userRepository;
        this.clientRepository = clientRepository;
        this.userAccountService = userAccountService;
        this.couponService = couponService;
        this.notificationService = notificationService;
        this.authService = authService;
        this.jwtGenerator = jwtGenerator;

        NimbusJwtDecoder nimbus = NimbusJwtDecoder.withJwkSetUri(JWKS_URI).build();
        nimbus.setJwtValidator(new DelegatingOAuth2TokenValidator<>(
                new JwtTimestampValidator(),
                claim(jwt -> ISSUERS.contains(jwt.getClaimAsString("iss")), "Emisor inválido"),
                claim(jwt -> jwt.getAudience() != null
                        && jwt.getAudience().stream().anyMatch(this.clientIds::contains), "Audiencia inválida")));
        this.decoder = nimbus;
    }

    private static OAuth2TokenValidator<Jwt> claim(java.util.function.Predicate<Jwt> test, String message) {
        return jwt -> test.test(jwt)
                ? OAuth2TokenValidatorResult.success()
                : OAuth2TokenValidatorResult.failure(new OAuth2Error("invalid_token", message, null));
    }

    @Transactional
    public AuthResponseDTO login(String idToken) {
        if (clientIds.isEmpty()) {
            throw ApiException.serviceUnavailable("El inicio de sesión con Google no está configurado.");
        }
        if (idToken == null || idToken.isBlank() || idToken.length() > 4096) {
            throw ApiException.badRequest("Falta el token de Google.");
        }

        Jwt jwt;
        try {
            jwt = decoder.decode(idToken);
        } catch (JwtException e) {
            throw ApiException.unauthorized("No pudimos verificar tu cuenta de Google. Inténtalo de nuevo.");
        }

        String subject = jwt.getSubject();
        String email = jwt.getClaimAsString("email");
        Boolean emailVerified = jwt.getClaimAsBoolean("email_verified");

        if (subject == null || email == null || !Boolean.TRUE.equals(emailVerified)) {
            throw ApiException.unauthorized("Tu cuenta de Google no tiene un correo verificado.");
        }
        email = email.trim().toLowerCase();

        UserEntity user = userRepository.findByGoogleSubject(subject)
                .orElseGet(() -> null);

        if (user == null) {
            UserEntity byEmail = userRepository.findByUsername(email).orElse(null);
            user = byEmail != null
                    ? link(byEmail, subject, email)
                    : createClient(jwt, subject, email);
        }

        List<String> roles = user.getRoles().stream().map(RoleEntity::getName).toList();
        String token = jwtGenerator.generateToken(user.getUsername(), roles);
        return new AuthResponseDTO(token, authService.currentUser(user.getUsername()));
    }

    // Ya existe una cuenta con ese correo. Google comprobó que el correo es suyo,
    // así que se enlaza (y queda confirmado). Solo para clientes: el personal
    // del restaurante entra siempre con su contraseña.
    private UserEntity link(UserEntity user, String subject, String email) {
        if (clientRepository.findByEmail(email) == null) {
            throw ApiException.forbidden(
                    "Esta cuenta no puede usar Google. Inicia sesión con tu correo y contraseña.");
        }
        user.setGoogleSubject(subject);
        user.setEmailVerified(true);
        return userRepository.save(user);
    }

    // Cliente nuevo con los datos de Google. La contraseña es aleatoria y nadie
    // la conoce: esta cuenta entra con Google.
    private UserEntity createClient(Jwt jwt, String subject, String email) {
        String name = firstNonBlank(jwt.getClaimAsString("given_name"), jwt.getClaimAsString("name"), "Cliente");
        String lastName = firstNonBlank(jwt.getClaimAsString("family_name"), "");

        UserEntity user = userAccountService.createUser(
                email, UUID.randomUUID().toString(), Role.CLIENT, true);
        user.setGoogleSubject(subject);

        Client client = Client.builder()
                .name(limit(name))
                .lastName(limit(lastName))
                .email(email)
                .role(Role.CLIENT)
                .user(user)
                .build();
        Client saved = clientRepository.save(client);

        // Igual que el registro normal: cupón de bienvenida
        couponService.createWelcomeCoupon(saved);
        notificationService.notifyWelcome(saved);
        return user;
    }

    private static String firstNonBlank(String... values) {
        for (String value : values) {
            if (value != null && !value.isBlank()) {
                return value.trim();
            }
        }
        return "";
    }

    private static String limit(String value) {
        return value.length() > 70 ? value.substring(0, 70) : value;
    }
}

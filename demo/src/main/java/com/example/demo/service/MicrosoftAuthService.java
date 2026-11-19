// LOGIN CON MICROSOFT DESACTIVADO (no se puede usar).
// La universidad no nos da permisos en Azure (Microsoft Entra ID) para registrar la
// aplicación y obtener el Client ID, así que no lo podemos usar. También se intentó
// con una cuenta personal de Microsoft, pero tampoco lo permitió.
// Se deja comentado por si en el futuro se consiguen los permisos.
//
// package com.example.demo.service;
//
// import com.example.demo.dto.AuthResponseDTO;
// import com.example.demo.dto.MicrosoftLoginRequestDTO;
// import com.example.demo.entities.Client;
// import com.example.demo.entities.Role;
// import com.example.demo.entities.UserEntity;
// import com.example.demo.entities.VerificationToken;
// import com.example.demo.errors.ApiException;
// import com.example.demo.repository.ClientRepository;
// import com.example.demo.repository.UserRepository;
// import com.example.demo.repository.VerificationTokenRepository;
// import com.nimbusds.jose.JWSAlgorithm;
// import com.nimbusds.jose.jwk.source.JWKSourceBuilder;
// import com.nimbusds.jose.proc.JWSVerificationKeySelector;
// import com.nimbusds.jose.proc.SecurityContext;
// import com.nimbusds.jose.util.DefaultResourceRetriever;
// import com.nimbusds.jose.util.JSONObjectUtils;
// import com.nimbusds.jose.util.Resource;
// import com.nimbusds.jwt.SignedJWT;
// import com.nimbusds.jwt.proc.DefaultJWTProcessor;
// import jakarta.mail.MessagingException;
// import java.io.IOException;
// import java.net.URI;
// import java.net.URL;
// import java.security.SecureRandom;
// import java.text.ParseException;
// import java.time.LocalDateTime;
// import java.util.Base64;
// import java.util.List;
// import java.util.Locale;
// import java.util.Map;
// import java.util.Optional;
// import java.util.UUID;
// import lombok.RequiredArgsConstructor;
// import org.springframework.beans.factory.annotation.Value;
// import org.springframework.mail.MailException;
// import org.springframework.security.oauth2.jwt.Jwt;
// import org.springframework.security.oauth2.jwt.JwtDecoder;
// import org.springframework.security.oauth2.jwt.JwtException;
// import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
// import org.springframework.stereotype.Service;
// import org.springframework.transaction.annotation.Transactional;
//
// @Service
// @RequiredArgsConstructor
// public class MicrosoftAuthService {
    // private static final String BASE = "https://login.microsoftonline.com/";
    // private static final String PERSONAL = "9188040d-6c67-4c5b-b112-36a304b66dad";
    // private static final String GUID = "(?i)[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}";
    // private static final SecureRandom RANDOM = new SecureRandom();
//
    // private final UserRepository users;
    // private final ClientRepository clients;
    // private final VerificationTokenRepository confirmations;
    // private final UserAccountService accounts;
    // private final AuthService auth;
    // private final EmailService mail;
    // private final CouponService coupons;
    // private final NotificationService notifications;
    // @Value("${TERRA_MICROSOFT_CLIENT_ID:}")
    // private String clientId;
//
    // private final JwtDecoder organizations = decoder(false);
    // private final JwtDecoder personal = decoder(true);
//
    // // Microsoft publica claves globales y claves reservadas al tenant personal.
    // // Nimbus almacena las claves en caché y las renueva cuando Microsoft las rota.
    // private static JwtDecoder decoder(boolean personalAccount) {
        // try {
            // URL jwks = URI.create(BASE + "common/discovery/v2.0/keys").toURL();
            // var downloader = new DefaultResourceRetriever(3000, 5000, 1048576);
            // var source = JWKSourceBuilder.<SecurityContext>create(jwks, url -> {
                // var resource = downloader.retrieveResource(url);
                // try {
                    // var json = JSONObjectUtils.parse(resource.getContent());
                    // List<Object> keys = JSONObjectUtils.getJSONArray(json, "keys");
                    // var allowed = keys.stream().filter(k -> k instanceof Map<?, ?> key
                            // && ((BASE + "{tenantid}/v2.0").equals(key.get("issuer"))
                                    // || (personalAccount && (BASE + PERSONAL + "/v2.0").equals(key.get("issuer")))))
                            // .toList();
                    // return new Resource(JSONObjectUtils.toJSONString(Map.of("keys", allowed)), "application/json");
                // } catch (ParseException e) {
                    // throw new IOException("Respuesta de claves de Microsoft inválida", e);
                // }
            // }).build();
            // var processor = new DefaultJWTProcessor<SecurityContext>();
            // processor.setJWSKeySelector(new JWSVerificationKeySelector<>(JWSAlgorithm.RS256, source));
            // return new NimbusJwtDecoder(processor);
        // } catch (java.net.MalformedURLException e) {
            // throw new IllegalStateException(e);
        // }
    // }
//
    // private Jwt verify(String token) {
        // if (clientId.isBlank())
            // throw ApiException.serviceUnavailable("Configura el ID de aplicación de Microsoft.");
        // if (token == null || token.isBlank() || token.length() > 32768) {
            // throw ApiException.badRequest("Falta el ID token de Microsoft o supera el tamaño permitido.");
        // }
        // try {
            // // El dato sin verificar solo selecciona uno de dos verificadores fijos.
            // String hint = SignedJWT.parse(token).getJWTClaimsSet().getStringClaim("tid");
            // Jwt jwt = (PERSONAL.equals(hint) ? personal : organizations).decode(token);
            // String tenant = jwt.getClaimAsString("tid");
            // String oid = jwt.getClaimAsString("oid");
            // String nonce = jwt.getClaimAsString("nonce");
            // if (tenant != null && tenant.matches(GUID) && oid != null && oid.matches(GUID)
                    // && (BASE + tenant + "/v2.0").equals(jwt.getClaimAsString("iss"))
                    // && List.of(clientId).equals(jwt.getAudience()) && jwt.getExpiresAt() != null
                    // && jwt.getSubject() != null && nonce != null && !nonce.isBlank()
                    // && "2.0".equals(jwt.getClaimAsString("ver")) && !jwt.hasClaim("scp"))
                // return jwt;
        // } catch (ParseException | JwtException | IllegalArgumentException e) {
            // throw ApiException.unauthorized("No se pudo verificar el ID token de Microsoft.");
        // }
        // throw ApiException.unauthorized("El token no corresponde a esta aplicación.");
    // }
//
    // @Transactional
    // public Optional<AuthResponseDTO> login(MicrosoftLoginRequestDTO request) {
        // if (request == null)
            // throw ApiException.badRequest("Faltan los datos del acceso con Microsoft.");
        // Jwt jwt = verify(request.idToken());
        // String identity = (jwt.getClaimAsString("tid") + ":" + jwt.getClaimAsString("oid")).toLowerCase(Locale.ROOT);
        // var linked = users.findByMicrosoftIdentity(identity);
        // if (linked.isPresent())
            // return Optional.of(session(linked.get()));
//
        // String email = jwt.getClaimAsString("email");
        // if (email == null)
            // email = jwt.getClaimAsString("preferred_username");
        // if (email == null || email.length() > 120)
            // throw ApiException.badRequest("Microsoft no proporcionó un correo válido.");
        // email = email.strip().toLowerCase(Locale.ROOT);
        // EmailRules.validate(email);
        // LocalDateTime now = LocalDateTime.now();
        // confirmations.removeExpiredMicrosoft(now);
        // var pending = confirmations.findByMicrosoftIdentity(identity);
        // if (request.confirmationCode() == null) {
            // sendCode(request.idToken(), identity, email, pending, now);
            // return Optional.empty();
        // }
        // var challenge = pending.orElseThrow(MicrosoftAuthService::invalidCode);
        // if (request.confirmationCode().length() != 12 || !challenge.getExpiresAt().isAfter(now)
                // || !challenge.getTokenHash().equals(hash(request.idToken(), request.confirmationCode())))
            // throw invalidCode();
//
        // var user = users.findFirstByUsername(email).orElse(null);
        // if (user == null) {
            // user = accounts.createUser(email, UUID.randomUUID().toString(), Role.CLIENT, true);
            // String name = jwt.getClaimAsString("name");
            // if (name == null || name.isBlank())
                // name = "Cliente";
            // var client = clients.save(Client.builder().name(name.substring(0, Math.min(name.length(), 70)))
                    // .lastName("").email(email).role(Role.CLIENT).user(user).build());
            // coupons.createWelcomeCoupon(client);
            // notifications.notifyWelcome(client);
        // } else if (user.getMicrosoftIdentity() != null) {
            // throw ApiException.conflict("Ya hay otra identidad Microsoft vinculada a esta cuenta.");
        // }
        // var session = session(user);
        // user.setMicrosoftIdentity(identity);
        // confirmations.delete(challenge);
        // return Optional.of(session);
    // }
//
    // private AuthResponseDTO session(UserEntity user) {
        // if (!user.isEmailVerified() || user.getRoles().size() != 1
                // || !Role.CLIENT.name().equals(user.getRoles().get(0).getName())
                // || clients.findByEmail(user.getUsername()) == null) {
            // throw ApiException.forbidden("Microsoft está disponible para cuentas de cliente con correo confirmado.");
        // }
        // return auth.refresh(user.getUsername());
    // }
//
    // private void sendCode(String token, String identity, String email, Optional<VerificationToken> pending,
            // LocalDateTime now) {
        // if (pending.isPresent() && pending.get().getCreatedAt().plusMinutes(1).isAfter(now)) {
            // throw ApiException.conflict("Espera un minuto antes de solicitar otro código.");
        // }
        // byte[] bytes = new byte[9];
        // RANDOM.nextBytes(bytes);
        // String code = Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
        // var challenge = pending.orElseGet(VerificationToken::new);
        // challenge.setMicrosoftIdentity(identity);
        // challenge.setTokenHash(hash(token, code));
        // challenge.setCreatedAt(now);
        // challenge.setExpiresAt(now.plusMinutes(10));
        // confirmations.saveAndFlush(challenge);
        // try {
            // mail.sendMicrosoftCode(email, code);
        // } catch (MailException | MessagingException e) {
            // throw ApiException.serviceUnavailable("No pudimos enviar el código de confirmación.");
        // }
    // }
//
    // private static String hash(String token, String code) {
        // return EmailVerificationService.hash(token + ":" + code);
    // }
//
    // private static ApiException invalidCode() {
        // return ApiException.badRequest("Código incorrecto o vencido. Si venció, vuelve a iniciar con Microsoft.");
    // }
// }

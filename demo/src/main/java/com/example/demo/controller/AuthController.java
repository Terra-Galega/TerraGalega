package com.example.demo.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import com.example.demo.dto.AuthResponseDTO;
import com.example.demo.dto.ForgotPasswordRequestDTO;
import com.example.demo.dto.ResetPasswordRequestDTO;
import com.example.demo.service.PasswordResetService;
import com.example.demo.dto.GoogleLoginRequestDTO;
// import com.example.demo.dto.MicrosoftLoginRequestDTO;
// import com.example.demo.service.MicrosoftAuthService;
import com.example.demo.errors.ApiException;
import com.example.demo.service.GoogleAuthService;
import com.example.demo.dto.LoginRequestDTO;
import com.example.demo.dto.LoginResponseDTO;
import com.example.demo.dto.MessageDTO;
import com.example.demo.dto.ResendVerificationRequestDTO;
import com.example.demo.dto.SignupRequestDTO;
import com.example.demo.dto.VerifyEmailRequestDTO;
import com.example.demo.security.AuthenticatedUser;
import com.example.demo.service.AuthService;
import com.example.demo.service.ClientService;
import com.example.demo.service.EmailVerificationService;
import io.swagger.v3.oas.annotations.security.SecurityRequirements;

import io.swagger.v3.oas.annotations.Operation;

@RestController
@RequestMapping("/auth")
@CrossOrigin(origins = "http://localhost:4200")
public class AuthController {

    @Autowired
    private AuthService authService;

    @Autowired
    private ClientService clientService;

    @Autowired
    private AuthenticatedUser authenticatedUser;

    @Autowired
    private EmailVerificationService emailVerificationService;

    @Autowired
    private GoogleAuthService googleAuthService;

    // Devuelve el JWT y los datos básicos si el correo y la contraseña son
    // correctos
    @SecurityRequirements
    @Operation(summary = "Login with email and password, returns a JWT")
    @PostMapping("/login")
    public AuthResponseDTO login(@RequestBody LoginRequestDTO request) {
        return authService.login(request.getEmail(), request.getPassword());
    }

    // "Continuar con Google": crea la cuenta o inicia sesión con el ID token de
    // Google. Si profileComplete es false, el front pide los datos que faltan.
    @SecurityRequirements
    @Operation(summary = "Sign up or log in with a Google ID token, returns a JWT")
    @PostMapping("/google")
    public AuthResponseDTO google(@RequestBody GoogleLoginRequestDTO request) {
        return googleAuthService.login(request == null ? null : request.idToken());
    }

    // LOGIN CON MICROSOFT DESACTIVADO (no se puede usar).
    // La universidad no nos da permisos en Azure (Microsoft Entra ID) para registrar la
    // aplicación y obtener el Client ID, así que no lo podemos usar. También se intentó
    // con una cuenta personal de Microsoft, pero tampoco lo permitió.
    // Se deja comentado por si en el futuro se consiguen los permisos.
    // @Autowired
    // private MicrosoftAuthService microsoftAuthService;
//
    // @SecurityRequirements
    // @Operation(summary = "Sign up or log in with a Microsoft ID token")
    // @PostMapping("/microsoft")
    // public AuthResponseDTO microsoft(@RequestBody MicrosoftLoginRequestDTO request) {
        // return microsoftAuthService.login(request).orElseThrow(() -> ApiException.emailNotVerified(
                // "Introduce el código que enviamos a tu correo para vincular tu cuenta Microsoft."));
    // }

    // Registra un cliente. No inicia sesión: primero debe confirmar su correo
    // con el enlace que se le envía.
    @SecurityRequirements
    @Operation(summary = "Register a new client and send the confirmation email")
    @PostMapping("/signup")
    public MessageDTO signup(@RequestBody SignupRequestDTO request) {
        clientService.addClient(request.toEntity());
        return new MessageDTO("Cuenta creada. Te enviamos un correo para confirmarla.");
    }

    // Confirma el correo con el token del enlace
    @Operation(summary = "Confirm the email address with the token from the email link")
    @PostMapping("/verify-email")
    public MessageDTO verifyEmail(@RequestBody VerifyEmailRequestDTO request) {
        emailVerificationService.verify(request.getToken());
        return new MessageDTO("Correo confirmado. Ya puedes iniciar sesión.");
    }

    @Autowired
    private PasswordResetService passwordResetService;

    // Envía el enlace para recuperar la contraseña. Responde igual exista o no la cuenta.
    @SecurityRequirements
    @Operation(summary = "Send a password reset link to the email")
    @PostMapping("/forgot-password")
    public MessageDTO forgotPassword(@RequestBody ForgotPasswordRequestDTO request) {
        passwordResetService.requestReset(request.email());
        return new MessageDTO("Si hay una cuenta con ese correo, te enviamos un enlace para recuperar tu contraseña.");
    }

    // Cambia la contraseña con el token del enlace del correo
    @SecurityRequirements
    @Operation(summary = "Set a new password with the token from the email link")
    @PostMapping("/reset-password")
    public MessageDTO resetPassword(@RequestBody ResetPasswordRequestDTO request) {
        passwordResetService.resetPassword(request.token(), request.newPassword());
        return new MessageDTO("Tu contraseña se cambió. Ya puedes iniciar sesión.");
    }

    // Reenvía el correo de confirmación. Responde igual exista o no la cuenta.
    @Operation(summary = "Resend the confirmation email")
    @PostMapping("/resend-verification")
    public MessageDTO resendVerification(@RequestBody ResendVerificationRequestDTO request) {
        emailVerificationService.resend(request.getEmail());
        return new MessageDTO("Si la cuenta existe y falta confirmarla, te enviamos un nuevo correo.");
    }

    // Datos del usuario con sesión iniciada, según el token que envía
    @Operation(summary = "Get the logged-in user from the JWT")
    @GetMapping("/me")
    public LoginResponseDTO me() {
        return authService.currentUser(authenticatedUser.email());
    }

    // Renueva el JWT antes de que venza, para no cerrar la sesión de quien la está
    // usando.
    // Exige un token todavía válido: con uno vencido hay que iniciar sesión de
    // nuevo.
    @Operation(summary = "Renew the JWT of the logged-in user before it expires")
    @PostMapping("/refresh")
    public AuthResponseDTO refresh() {
        return authService.refresh(authenticatedUser.email());
    }
}

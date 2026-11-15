import { Component, OnDestroy } from '@angular/core';
import { ActivatedRoute, Router } from '@angular/router';
import { inject } from '@angular/core';
import { Role } from '../../models/role';
import { CreateClient } from '../../models/client';
import { NavbarComponent } from '../../components/navbar/navbar.component';
import { FooterComponent } from '../../components/footer/footer.component';
import { LoginHeaderComponent } from './components/login-header/login-header.component';
import { LoginFormsComponent } from './components/login-forms/login-forms.component';
import { GoogleButtonComponent } from '../../components/google-button/google-button.component';
import { ToastService } from '../../services/toast.service';
import { TutorialAuthService } from '../../services/tutorial-auth.service';
import { TutorialService } from '../../services/Tutorial.service';
import { AuthService, LoginResponse } from '../../services/auth.service';
import { getErrorMessage } from '../../utils/http-error';
import { EMAIL_ERROR_MESSAGE, isValidEmail } from '../../utils/email';

// Segundos que el botón de reenviar queda bloqueado (el back usa el mismo límite).
const RESEND_COOLDOWN_SECONDS = 60;

// Código con el que el back avisa que la contraseña es correcta pero falta confirmar el correo.
const EMAIL_NOT_VERIFIED_CODE = 'EMAIL_NOT_VERIFIED';

// A dónde vuelve el cliente según el parámetro ?redirect=
const REDIRECTS: Record<string, string> = {
  checkout: '/checkout',
  reservations: '/reservations',
  home: '/aboutUs',
  aboutUs: '/aboutUs',
};

// Página a la que vuelve si viene de un tutorial (aunque la URL no traiga redirect)
const TOUR_REDIRECTS = {
  order: '/checkout',
  reservation: '/reservations',
} as const;

@Component({
  selector: 'app-login',
  imports: [
    NavbarComponent,
    FooterComponent,
    LoginHeaderComponent,
    LoginFormsComponent,
    GoogleButtonComponent,
  ],
  templateUrl: './login.component.html',
})
export class LoginComponent implements OnDestroy {
  activeTab: 'login' | 'signup' = 'login';
  // Controla la visibilidad de las contraseñas en los formularios
  showLoginPassword = false;
  showSignupPassword = false;
  //campos de login
  loginEmail = '';
  loginPassword = '';

  loginError = '';
  //campos de signup
  signupError = '';

  signupName = '';
  signupLastName = '';
  signupEmail = '';
  signupPassword = '';
  signupPhone = '';
  signupAddress = '';

  // Confirmación de correo
  // Correo al que se envió el enlace tras registrarse (muestra la pantalla "revisa tu correo")
  pendingEmail = '';
  // true si el login falló porque el correo aún no está confirmado
  emailNotVerified = false;
  // Texto que se muestra después de pedir un reenvío
  resendMessage = '';
  resendError = '';
  // Segundos que faltan para poder reenviar otra vez (0 = ya se puede)
  resendCooldown = 0;
  private cooldownTimer: ReturnType<typeof setInterval> | null = null;

  // Servicios inyectados
  private router = inject(Router);
  private route = inject(ActivatedRoute);
  private auth = inject(AuthService);
  private toast = inject(ToastService);
  private tutorialAuth = inject(TutorialAuthService);
  // Para saber si el cliente viene de un tutorial y devolverlo a su página
  private tutorial = inject(TutorialService);
  constructor() {
    // /login?tab=signup abre directo la pestaña de crear cuenta
    if (this.route.snapshot.queryParamMap.get('tab') === 'signup') {
      this.activeTab = 'signup';
    }
    this.tutorialAuth.resumeLogin();
  }

  // Ruta a la que se va después de entrar:
  // 1. Si viene de un tutorial, a la página donde sigue (checkout o reservas).
  // 2. Si la URL trae ?redirect=, a esa página.
  // 3. Si no, a su cuenta.
  private afterAuthRoute(): string {
    const flow = this.tutorial.currentFlow();
    if (flow) return TOUR_REDIRECTS[flow];

    const redirect = this.route.snapshot.queryParamMap.get('redirect') ?? '';
    return REDIRECTS[redirect] ?? '/account';
  }

  // Después de iniciar sesión como cliente
  private goAfterClientAuth(): void {
    this.router.navigate([this.afterAuthRoute()]);
  }
  // Cambia la pestaña activa entre login y signup
  selectTab(tab: 'login' | 'signup') {
    this.activeTab = tab;
    this.loginError = '';
    this.signupError = '';
    this.emailNotVerified = false;
    this.pendingEmail = '';
    this.resendMessage = '';
    this.resendError = '';
  }
  // Alterna la visibilidad de la contraseña en el formulario de login
  toggleLoginPassword() {
    this.showLoginPassword = !this.showLoginPassword;
  }
  // Alterna la visibilidad de la contraseña en el formulario de signup
  toggleSignupPassword() {
    this.showSignupPassword = !this.showSignupPassword;
  }
  // Maneja el inicio de sesión del usuario
  setLoginEmail(event: Event) {
    const input = event.target as HTMLInputElement;

    this.loginEmail = input.value;
  }
  // Maneja el inicio de sesión del usuario
  setLoginPassword(event: Event) {
    const input = event.target as HTMLInputElement;

    this.loginPassword = input.value;
  }
  // Maneja el registro de un nuevo cliente
  fillDemoUser(email: string, password: string) {
    this.loginEmail = email;
    this.loginPassword = password;
    this.loginError = '';
  }

  // Redirección según el rol del usuario (la ruta de cada rol se define en AuthService)
  private goByRole(user: LoginResponse): void {
    if (user.role === Role.CLIENT) {
      this.goAfterClientAuth();
      return;
    }
    this.router.navigate([this.auth.homeRoute()]);
  }
  // Error de "Continuar con Google"
  googleError = '';

  // Google entregó el ID token: el back crea la cuenta o inicia sesión.
  // Si a la cuenta le faltan datos (teléfono, dirección...) se piden antes de seguir.
  googleLogin(idToken: string) {
    this.googleError = '';
    this.auth.googleLogin(idToken).subscribe({
      next: (user) => {
        this.auth.setSession(user);
        if (user.profileComplete === false) {
          // Se pasa a dónde volver: el del tutorial o el de la URL
          const flow = this.tutorial.currentFlow();
          const redirect = flow
            ? (flow === 'order' ? 'checkout' : 'reservations')
            : this.route.snapshot.queryParamMap.get('redirect');
          this.router.navigate(['/complete-profile'], {
            queryParams: redirect ? { redirect } : {},
          });
          return;
        }
        this.goByRole(user);
      },
      error: (error) => {
        this.googleError = getErrorMessage(
          error,
          'No se pudo continuar con Google. Inténtalo de nuevo.',
        );
      },
    });
  }

  // Maneja el inicio de sesión del usuario
  login() {
    this.loginError = '';
    this.emailNotVerified = false;
    this.resendMessage = '';
    this.resendError = '';
    if (!isValidEmail(this.loginEmail)) {
      this.loginError = EMAIL_ERROR_MESSAGE;
      return;
    }
    this.auth.login(this.loginEmail, this.loginPassword).subscribe({
      next: (user) => {
        this.auth.setSession(user);
        this.goByRole(user);
      },
      error: (error) => {
        this.loginError = getErrorMessage(
          error,
          'No se pudo conectar con el servidor.',
        );
        // Contraseña correcta pero correo sin confirmar: se ofrece reenviar el enlace
        if (error?.error?.code === EMAIL_NOT_VERIFIED_CODE) {
          this.emailNotVerified = true;
          this.pendingEmail = this.loginEmail.trim();
        }
      },
    });
  }

  // Pide otro correo de confirmación para pendingEmail
  resendVerification() {
    if (!this.pendingEmail || this.resendCooldown > 0) {
      return;
    }
    this.resendMessage = '';
    this.resendError = '';

    this.auth.resendVerification(this.pendingEmail).subscribe({
      next: () => {
        this.resendMessage = 'Te enviamos un nuevo correo. Revisa tu bandeja de entrada.';
        this.startCooldown();
      },
      error: (error) => {
        this.resendError = getErrorMessage(
          error,
          'No se pudo reenviar el correo. Inténtalo de nuevo.',
        );
      },
    });
  }

  // Vuelve al formulario de inicio de sesión desde la pantalla "revisa tu correo"
  backToLogin() {
    const email = this.pendingEmail;
    this.selectTab('login');
    this.loginEmail = email;
    this.loginPassword = '';
  }

  private startCooldown() {
    this.stopCooldown();
    this.resendCooldown = RESEND_COOLDOWN_SECONDS;
    this.cooldownTimer = setInterval(() => {
      this.resendCooldown--;
      if (this.resendCooldown <= 0) {
        this.stopCooldown();
      }
    }, 1000);
  }

  private stopCooldown() {
    if (this.cooldownTimer) {
      clearInterval(this.cooldownTimer);
      this.cooldownTimer = null;
    }
    this.resendCooldown = 0;
  }

  ngOnDestroy() {
    this.stopCooldown();
  }
  // Maneja el registro de un nuevo cliente
  setSignupName(value: string) {
    this.signupName = value;
  }

  setSignupLastName(value: string) {
    this.signupLastName = value;
  }

  setSignupEmail(value: string) {
    this.signupEmail = value;
  }

  setSignupPassword(value: string) {
    this.signupPassword = value;
  }

  setSignupPhone(value: string) {
    this.signupPhone = value;
  }
  setSignupAddress(value: string) {
    this.signupAddress = value;
  }
  //valida los campos del formulario de registro y llama al servicio para crear un nuevo cliente
  signup() {
    this.signupError = '';

    if (
      !this.signupName.trim() ||
      !this.signupLastName.trim() ||
      !this.signupEmail.trim() ||
      !this.signupPassword.trim() ||
      !this.signupPhone.trim() ||
      !this.signupAddress.trim()
    ) {
      this.signupError = 'Todos los campos son obligatorios.';
      return;
    }
    if (!isValidEmail(this.signupEmail)) {
      this.signupError = EMAIL_ERROR_MESSAGE;
      return;
    }
    //crear un objeto CreateClient con los datos del formulario
    const newClient: CreateClient = {
      name: this.signupName.trim(),
      lastName: this.signupLastName.trim(),
      email: this.signupEmail.trim(),
      password: this.signupPassword,
      phone: this.signupPhone.trim(),
      role: Role.CLIENT,
      addresses: [{ label: this.signupAddress.trim() }],
    };
    // El back crea la cuenta y envía el correo de confirmación.
    // No devuelve token: el cliente entra después de confirmar su correo.
    this.auth.signup(newClient).subscribe({
      next: () => {
        this.pendingEmail = newClient.email;
        this.signupName = '';
        this.signupLastName = '';
        this.signupEmail = '';
        this.signupPassword = '';
        this.signupPhone = '';
        this.signupAddress = '';
        this.resendMessage = '';
        this.resendError = '';
        this.startCooldown();
        this.toast.show(
          'Cuenta creada',
          'Revisa tu correo para confirmarla.',
        );
      },
      error: (error) => {
        console.error('Error creando cuenta:', error);
        this.signupError = getErrorMessage(
          error,
          'No se pudo crear la cuenta. Inténtalo de nuevo.',
        );
      },
    });
  }
}
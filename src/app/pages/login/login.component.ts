import { Component } from '@angular/core';
import { ActivatedRoute, Router } from '@angular/router';
import { inject } from '@angular/core';
import { Role } from '../../models/role';
import { CreateClient } from '../../models/client';
import { NavbarComponent } from '../../components/navbar/navbar.component';
import { FooterComponent } from '../../components/footer/footer.component';
import { ClientService } from '../../services/client.service';
import { LoginHeaderComponent } from './components/login-header/login-header.component';
import { LoginFormsComponent } from './components/login-forms/login-forms.component';
import { ToastService } from '../../services/toast.service';
import { AuthService } from '../../services/auth.service';
import { getErrorMessage } from '../../utils/http-error';
import { EMAIL_ERROR_MESSAGE, isValidEmail } from '../../utils/email';

@Component({
  selector: 'app-login',
  imports: [
    NavbarComponent,
    FooterComponent,
    LoginHeaderComponent,
    LoginFormsComponent,
  ],
  templateUrl: './login.component.html',
})
export class LoginComponent {
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
// Servicios inyectados
  private clientService = inject(ClientService);
  private router = inject(Router);
  private route = inject(ActivatedRoute);
  private auth = inject(AuthService);
  private toast = inject(ToastService);

  // Si llegaron desde /checkout sin sesión, después de entrar vuelven allí
  private goAfterClientAuth(clientId: number): void {
    const redirect = this.route.snapshot.queryParamMap.get('redirect');
    // Si venían de checkout, los redirigimos allí
    if (redirect === 'checkout') {
      this.router.navigate(['/checkout']);
      return;
    }
    // Si no, van a su perfil
    this.router.navigate(['/account', clientId]);
  }
  // Cambia la pestaña activa entre login y signup
  selectTab(tab: 'login' | 'signup') {
    this.activeTab = tab;
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

  // Redirección según el rol del usuario que devolvió el back
  private goByRole(user: any): void {
    switch (user.role) {
      case Role.ADMIN:
        this.router.navigate(['/admin', user.id]);
        break;

      case Role.CLIENT:
        this.goAfterClientAuth(user.id);
        break;

      case Role.OPERATOR:
        this.router.navigate(['/operator', user.id]);
        break;

      default:
        this.router.navigate(['/home']);
    }
  }
  // Maneja el inicio de sesión del usuario
  login() {
    this.loginError = '';
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
      },
    });
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
      !this.signupPassword.trim()
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
      address: this.signupAddress.trim(),
    };
    //llama al servicio para crear un nuevo cliente
    this.clientService.addClient(newClient).subscribe({
      next: (createdClient) => {
        console.log('Cliente creado:', createdClient);

        this.auth.setSession(createdClient);
        this.toast.show('Cuenta creada correctamente');
        this.goByRole(createdClient);
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

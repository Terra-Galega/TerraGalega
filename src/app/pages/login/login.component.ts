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
import { AuthService } from '../../services/auth.service';
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

  showLoginPassword = false;
  showSignupPassword = false;

  loginEmail = '';
  loginPassword = '';

  loginError = '';

  signupError = '';

  signupName = '';
  signupLastName = '';
  signupEmail = '';
  signupPassword = '';
  signupPhone = '';

  private clientService = inject(ClientService);
  private router = inject(Router);
  private route = inject(ActivatedRoute);
  private auth = inject(AuthService);

  // Si llegaron desde /checkout sin sesión, después de entrar vuelven allí
  private goAfterClientAuth(clientId: number): void {
    const redirect = this.route.snapshot.queryParamMap.get('redirect');

    if (redirect === 'checkout') {
      this.router.navigate(['/checkout']);
      return;
    }

    this.router.navigate(['/account', clientId]);
  }

  selectTab(tab: 'login' | 'signup') {
    this.activeTab = tab;
  }

  toggleLoginPassword() {
    this.showLoginPassword = !this.showLoginPassword;
  }

  toggleSignupPassword() {
    this.showSignupPassword = !this.showSignupPassword;
  }

  setLoginEmail(event: Event) {
    const input = event.target as HTMLInputElement;

    this.loginEmail = input.value;
  }

  setLoginPassword(event: Event) {
    const input = event.target as HTMLInputElement;

    this.loginPassword = input.value;
  }

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

      // case Role.OPERATOR:
      //   this.router.navigate(['/operator', user.id]);
      //   break;

      // case Role.DELIVERY_PERSON:
      //   this.router.navigate(['/delivery', user.id]);
      //   break;

      default:
        this.router.navigate(['/home']);
    }
  }

  login() {
    this.loginError = '';

    this.auth.login(this.loginEmail, this.loginPassword).subscribe({
      next: (user) => {
        this.auth.setSession(user);
        this.goByRole(user);
      },
      error: (error) => {
        this.loginError =
          error.error?.message ?? 'No se pudo conectar con el servidor.';
      },
    });
  }
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

  signup() {
    this.signupError = '';

    if (
      !this.signupName.trim() ||
      !this.signupLastName.trim() ||
      !this.signupEmail.trim() ||
      !this.signupPassword.trim() ||
      !this.signupPhone.trim()
    ) {
      this.signupError = 'Todos los campos son obligatorios.';
      return;
    }

    const newClient: CreateClient = {
      name: this.signupName.trim(),
      lastName: this.signupLastName.trim(),
      email: this.signupEmail.trim(),
      password: this.signupPassword,
      phone: this.signupPhone.trim(),
      role: Role.CLIENT,
    };

    this.clientService.addClient(newClient).subscribe({
      next: (createdClient) => {
        console.log('Cliente creado:', createdClient);

        this.auth.setSession(createdClient);
        this.goByRole(createdClient);
      },
      error: (error) => {
        console.error('Error creando cuenta:', error);
        console.error('Status:', error.status);
        console.error('Respuesta backend:', error.error);

        this.signupError =
          error?.error?.message ??
          'No se pudo crear la cuenta. Inténtalo de nuevo.';
      },
    });
  }
}

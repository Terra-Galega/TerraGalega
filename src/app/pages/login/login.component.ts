import { Component } from '@angular/core';
import { Router } from '@angular/router';
import { inject } from '@angular/core';
import { Role } from '../../models/role';

import { NavbarComponent } from '../../components/navbar/navbar.component';
import { FooterComponent } from '../../components/footer/footer.component';
import { ClientService } from '../../services/client.service';
import { AdminService } from '../../services/admin.service';
import { LoginHeaderComponent } from './components/login-header/login-header.component';
import { LoginFormsComponent } from './components/login-forms/login-forms.component';

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
  private adminService = inject(AdminService);
  private router = inject(Router);

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

  login() {
    this.loginError = '';

    // Primero comprobamos si es administrador
    const admin = this.adminService.login(this.loginEmail, this.loginPassword);

    if (admin) {
      localStorage.setItem('currentUser', JSON.stringify(admin));

      localStorage.setItem('userType', 'admin');

      this.router.navigate(['/admin', admin.id]);
      return;
    }

    const client = this.clientService.login(
      this.loginEmail,
      this.loginPassword,
    );

    if (client) {
      localStorage.setItem('currentUser', JSON.stringify(client));

      localStorage.setItem('userType', 'client');

      this.router.navigate(['/account', client.id]);
      return;
    }

    this.loginError = 'Correo o contraseña incorrectos.';

    this.loginError = 'Correo o contraseña incorrectos.';
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

    const client = {
      id: 0,
      name: this.signupName.trim(),
      lastName: this.signupLastName.trim(),
      email: this.signupEmail.trim(),
      password: this.signupPassword,
      phone: this.signupPhone.trim(),
      role: Role.CLIENT,
    };

    this.clientService.addClient(client).subscribe({
      next: (createdClient) => {
        localStorage.setItem('currentUser', JSON.stringify(createdClient));

        localStorage.setItem('userType', 'client');

        this.router.navigate(['/account', createdClient.id]);
      },
      error: (error) => {
        console.error('Error registrando cliente:', error);

        this.signupError =
          error?.error?.message ?? 'No se pudo crear la cuenta.';
      },
    });
  }
}

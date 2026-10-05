import { Component } from '@angular/core';
import { Router } from '@angular/router';
import { inject } from '@angular/core';

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
}

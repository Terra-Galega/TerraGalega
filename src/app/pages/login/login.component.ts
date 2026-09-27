import { Component } from '@angular/core';
import { Router } from '@angular/router';

import { NavbarComponent } from '../../components/navbar/navbar.component';
import { FooterComponent } from '../../components/footer/footer.component';

import { UserService } from '../../services/user.service';
import { AdminService } from '../../services/admin.service';

@Component({
  selector: 'app-login',
  imports: [NavbarComponent, FooterComponent],
  templateUrl: './login.component.html',
  styleUrl: './login.component.scss',
})
export class LoginComponent {
  activeTab: 'login' | 'signup' = 'login';

  showLoginPassword = false;
  showSignupPassword = false;

  loginEmail = '';
  loginPassword = '';

  loginError = '';

  signupError = '';

  constructor(
    private userService: UserService,
    private adminService: AdminService,
    private router: Router,
  ) {}

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
      localStorage.setItem('currentAdmin', JSON.stringify(admin));

      localStorage.setItem('userType', 'admin');

      this.router.navigate(['/admin']);

      return;
    }

    // Si no es administrador, comprobamos usuario
    const user = this.userService.login(this.loginEmail, this.loginPassword);

    if (user) {
      localStorage.setItem('currentUser', JSON.stringify(user));

      localStorage.setItem('userType', 'user');

      this.router.navigate(['/']);

      return;
    }

    this.loginError = 'Correo o contraseña incorrectos.';
  }
}

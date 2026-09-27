import { Component } from '@angular/core';
import { NavbarComponent } from '../../components/navbar/navbar.component';
import { FooterComponent } from '../../components/footer/footer.component';

@Component({
  selector: 'app-login',
  imports: [
    NavbarComponent,
    FooterComponent
  ],
  templateUrl: './login.component.html',
  styleUrl: './login.component.scss'
})
export class LoginComponent {

  activeTab: 'login' | 'signup' = 'login';

  showLoginPassword = false;
  showSignupPassword = false;

  selectTab(tab: 'login' | 'signup') {
    this.activeTab = tab;
  }

  toggleLoginPassword() {
    this.showLoginPassword = !this.showLoginPassword;
  }

  toggleSignupPassword() {
    this.showSignupPassword = !this.showSignupPassword;
  }
}
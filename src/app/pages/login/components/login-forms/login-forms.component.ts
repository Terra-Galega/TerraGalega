import { Component, EventEmitter, Input, Output } from '@angular/core';
import { ButtonComponent } from '../../../../components/button/button.component';

@Component({
  selector: 'app-login-forms',
  imports: [ButtonComponent],
  templateUrl: './login-forms.component.html',
  styleUrl: './login-forms.component.scss',
})
export class LoginFormsComponent {
  @Input() activeTab: 'login' | 'signup' = 'login';

  @Input() loginEmail = '';
  @Input() loginPassword = '';
  @Input() loginError = '';
  @Input() signupError = '';

  @Input() showLoginPassword = false;
  @Input() showSignupPassword = false;

  @Output() loginEmailChange = new EventEmitter<string>();
  @Output() loginPasswordChange = new EventEmitter<string>();

  @Output() loginSubmit = new EventEmitter<void>();

  @Output() loginPasswordToggle = new EventEmitter<void>();
  @Output() signupPasswordToggle = new EventEmitter<void>();

  @Output() demoUser = new EventEmitter<{
    email: string;
    password: string;
  }>();

  setLoginEmail(event: Event) {
    const input = event.target as HTMLInputElement;

    this.loginEmailChange.emit(input.value);
  }

  setLoginPassword(event: Event) {
    const input = event.target as HTMLInputElement;

    this.loginPasswordChange.emit(input.value);
  }

  login() {
    this.loginSubmit.emit();
  }

  toggleLoginPassword() {
    this.loginPasswordToggle.emit();
  }

  toggleSignupPassword() {
    this.signupPasswordToggle.emit();
  }

  fillDemoUser(email: string, password: string) {
    this.demoUser.emit({ email, password });
  }
}

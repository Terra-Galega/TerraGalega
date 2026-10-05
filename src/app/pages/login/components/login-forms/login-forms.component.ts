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
  @Input() signupName = '';
  @Input() signupLastName = '';
  @Input() signupEmail = '';
  @Input() signupPassword = '';
  @Input() signupPhone = '';

  @Input() showLoginPassword = false;
  @Input() showSignupPassword = false;

  @Output() loginEmailChange = new EventEmitter<string>();
  @Output() loginPasswordChange = new EventEmitter<string>();

  @Output() loginSubmit = new EventEmitter<void>();

  @Output() loginPasswordToggle = new EventEmitter<void>();
  @Output() signupPasswordToggle = new EventEmitter<void>();
  @Output() signupNameChange = new EventEmitter<string>();
  @Output() signupLastNameChange = new EventEmitter<string>();
  @Output() signupEmailChange = new EventEmitter<string>();
  @Output() signupPasswordChange = new EventEmitter<string>();
  @Output() signupPhoneChange = new EventEmitter<string>();

  @Output() signupSubmit = new EventEmitter<void>();
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

  setSignupName(event: Event) {
    this.signupNameChange.emit((event.target as HTMLInputElement).value);
  }

  setSignupLastName(event: Event) {
    this.signupLastNameChange.emit((event.target as HTMLInputElement).value);
  }

  setSignupEmail(event: Event) {
    this.signupEmailChange.emit((event.target as HTMLInputElement).value);
  }

  setSignupPassword(event: Event) {
    this.signupPasswordChange.emit((event.target as HTMLInputElement).value);
  }

  setSignupPhone(event: Event) {
    this.signupPhoneChange.emit((event.target as HTMLInputElement).value);
  }

  signup() {
    this.signupSubmit.emit();
  }
}

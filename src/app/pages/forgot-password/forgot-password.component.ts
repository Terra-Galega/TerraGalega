import { Component, inject } from '@angular/core';
import { RouterLink } from '@angular/router';
import { NavbarComponent } from '../../components/navbar/navbar.component';
import { FooterComponent } from '../../components/footer/footer.component';
import { ButtonComponent } from '../../components/button/button.component';
import { TranslatePipe } from '../../services/translate.pipe';
import { AuthService } from '../../services/auth.service';
import { getErrorMessage } from '../../utils/http-error';
import { EMAIL_ERROR_MESSAGE, isValidEmail } from '../../utils/email';

// Página /forgot-password: el cliente escribe su correo y el back le envía
// un enlace para elegir una contraseña nueva.
@Component({
  selector: 'app-forgot-password',
  imports: [NavbarComponent, FooterComponent, ButtonComponent, TranslatePipe, RouterLink],
  templateUrl: './forgot-password.component.html',
})
export class ForgotPasswordComponent {
  private auth = inject(AuthService);

  email = '';
  error = '';
  sending = false;
  sent = false;

  setEmail(event: Event) {
    this.email = (event.target as HTMLInputElement).value;
  }

  submit() {
    if (this.sending) return;
    const email = this.email.trim();
    if (!isValidEmail(email)) {
      this.error = EMAIL_ERROR_MESSAGE;
      return;
    }

    this.error = '';
    this.sending = true;
    this.auth.forgotPassword(email).subscribe({
      next: () => {
        this.sending = false;
        this.sent = true;
      },
      error: (error) => {
        this.sending = false;
        this.error = getErrorMessage(error, 'No se pudo enviar el enlace. Inténtalo de nuevo.');
      },
    });
  }
}

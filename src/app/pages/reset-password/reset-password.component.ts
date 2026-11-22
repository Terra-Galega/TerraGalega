import { Component, OnInit, inject } from '@angular/core';
import { NgTemplateOutlet } from '@angular/common';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { NavbarComponent } from '../../components/navbar/navbar.component';
import { FooterComponent } from '../../components/footer/footer.component';
import { ButtonComponent } from '../../components/button/button.component';
import { TranslatePipe } from '../../pipes/translate.pipe';
import { AuthService } from '../../services/auth.service';
import { getErrorMessage } from '../../utils/http-error';

// Estados de la pantalla: formulario, contraseña cambiada o enlace con problema
type ResetState = 'form' | 'done' | 'invalid';

// Página a la que lleva el enlace del correo: /reset-password?token=...
// Pide la contraseña nueva dos veces y se la envía al back con el token.
@Component({
  selector: 'app-reset-password',
  imports: [NavbarComponent, FooterComponent, ButtonComponent, TranslatePipe, RouterLink, NgTemplateOutlet],
  templateUrl: './reset-password.component.html',
})
export class ResetPasswordComponent implements OnInit {
  private route = inject(ActivatedRoute);
  private router = inject(Router);
  private auth = inject(AuthService);

  state: ResetState = 'form';
  token = '';
  password = '';
  confirm = '';
  // Cada campo tiene su propio ojo para ver u ocultar lo que se escribe
  showPassword = false;
  showConfirm = false;
  error = '';
  message = '';
  saving = false;

  ngOnInit() {
    this.token = this.route.snapshot.queryParamMap.get('token') ?? '';

    // Quita el token de la URL para que no quede en el historial del navegador
    this.router.navigate([], { queryParams: {}, replaceUrl: true });

    if (!this.token) {
      this.state = 'invalid';
      this.message = 'El enlace no es válido.';
    }
  }

  setPassword(event: Event) {
    this.password = (event.target as HTMLInputElement).value;
  }

  setConfirm(event: Event) {
    this.confirm = (event.target as HTMLInputElement).value;
  }

  submit() {
    if (this.saving) return;
    if (this.password.length < 6) {
      this.error = 'La nueva contraseña debe tener al menos 6 caracteres.';
      return;
    }
    if (this.password.length > 70) {
      this.error = 'La nueva contraseña es demasiado larga.';
      return;
    }
    if (this.password !== this.confirm) {
      this.error = 'Las contraseñas no coinciden.';
      return;
    }

    this.error = '';
    this.saving = true;
    this.auth.resetPassword(this.token, this.password).subscribe({
      next: (response) => {
        this.saving = false;
        this.state = 'done';
        this.message = response.message;
      },
      error: (error) => {
        this.saving = false;
        // Token vencido, usado o inválido: ya no sirve reintentar con este enlace
        if (error?.status === 400 && /enlace/i.test(error?.error?.message ?? '')) {
          this.state = 'invalid';
          this.message = error.error.message;
          return;
        }
        this.error = getErrorMessage(error, 'No se pudo cambiar la contraseña. Inténtalo de nuevo.');
      },
    });
  }

  goLogin() {
    this.router.navigate(['/login']);
  }
}

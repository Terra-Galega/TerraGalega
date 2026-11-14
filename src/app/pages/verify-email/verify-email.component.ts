import { Component, OnInit, inject } from '@angular/core';
import { ActivatedRoute, Router } from '@angular/router';
import { NavbarComponent } from '../../components/navbar/navbar.component';
import { FooterComponent } from '../../components/footer/footer.component';
import { ButtonComponent } from '../../components/button/button.component';
import { AuthService } from '../../services/auth.service';
import { getErrorMessage } from '../../utils/http-error';

// Estados de la pantalla: confirmando, correo confirmado o enlace con problema.
type VerifyState = 'loading' | 'success' | 'error';

// Página a la que lleva el enlace del correo: /verify-email?token=...
// Lee el token, se lo envía al back y muestra el resultado.
@Component({
  selector: 'app-verify-email',
  imports: [NavbarComponent, FooterComponent, ButtonComponent],
  templateUrl: './verify-email.component.html',
})
export class VerifyEmailComponent implements OnInit {
  private route = inject(ActivatedRoute);
  private router = inject(Router);
  private auth = inject(AuthService);

  state: VerifyState = 'loading';
  message = '';

  ngOnInit() {
    const token = this.route.snapshot.queryParamMap.get('token');

    // Quita el token de la URL para que no quede en el historial del navegador
    this.router.navigate([], { queryParams: {}, replaceUrl: true });

    if (!token) {
      this.state = 'error';
      this.message = 'El enlace no es válido.';
      return;
    }

    // Se confirma con un POST (no con el GET del enlace) para que los antivirus o
    // previsualizadores de correo que abren los enlaces no gasten el token.
    this.auth.verifyEmail(token).subscribe({
      next: (response) => {
        this.state = 'success';
        this.message = response.message;
      },
      error: (error) => {
        this.state = 'error';
        this.message = getErrorMessage(
          error,
          'No se pudo confirmar tu correo. Inténtalo de nuevo.',
        );
      },
    });
  }

  goLogin() {
    this.router.navigate(['/login']);
  }

  goHome() {
    this.router.navigate(['/home']);
  }
}

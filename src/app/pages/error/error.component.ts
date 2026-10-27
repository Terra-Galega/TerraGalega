import { Component } from '@angular/core';
import { ActivatedRoute, Router } from '@angular/router';
import { NavbarComponent } from '../../components/navbar/navbar.component';
import { FooterComponent } from '../../components/footer/footer.component';
import { ButtonComponent } from '../../components/button/button.component';
import { AuthService } from '../../services/auth.service';
import { getErrorTitle } from '../../utils/http-error';

@Component({
  selector: 'app-error',
  // Componentes utilizados para mostrar la página de error y sus opciones de navegación
  imports: [NavbarComponent, FooterComponent, ButtonComponent],
  templateUrl: './error.component.html',
})
export class ErrorComponent {
  // Mensaje y título que se muestran cuando ocurre un error
  message = 'Ha ocurrido un error inesperado. Por favor intenta de nuevo.';
  title = 'Error';

  // Información del rol y del panel al que pertenece el usuario autenticado
  isAdmin = false;
  isOperator = false;
  panelId: number | null = null;

  constructor(
    private router: Router,
    private route: ActivatedRoute,
    private auth: AuthService,
  ) {
    // Obtiene la sesión actual para identificar el tipo de usuario y su panel
    const session = this.auth.session;

    this.isAdmin = session?.type === 'admin';
    this.isOperator = session?.type === 'operator';
    this.panelId = session?.id ?? null;

    // Obtiene el mensaje de error desde los parámetros de la URL o los datos de la ruta
    const message =
      this.route.snapshot.queryParamMap.get('message') ??
      this.route.snapshot.data['message'];

    if (message) {
      this.message = message;
    }

    // El código HTTP (si llegó) define el título de la página
    const status =
      this.route.snapshot.queryParamMap.get('status') ??
      this.route.snapshot.data['status'];
    if (status !== null && status !== undefined) {
      this.title = getErrorTitle(Number(status));
    }
  }

  // Indica si el usuario tiene acceso a un panel administrativo o de operador
  get hasPanel(): boolean {
    return this.isAdmin || this.isOperator;
  }

  // Redirige al panel correspondiente según el rol del usuario
  goToPanel() {
    this.router.navigate([this.isAdmin ? '/admin' : '/operator', this.panelId]);
  }

  // Redirige a la página principal del sitio
  goHome() {
    this.router.navigate(['/home']);
  }

  // Redirige a la página de la carta de productos
  goMenu() {
    this.router.navigate(['/menu']);
  }
}
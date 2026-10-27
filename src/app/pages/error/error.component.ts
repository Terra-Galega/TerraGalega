import { Component } from '@angular/core';
import { ActivatedRoute, Router } from '@angular/router';
import { NavbarComponent } from '../../components/navbar/navbar.component';
import { FooterComponent } from '../../components/footer/footer.component';
import { ButtonComponent } from '../../components/button/button.component';
import { AuthService } from '../../services/auth.service';
import { getErrorTitle } from '../../utils/http-error';

@Component({
  selector: 'app-error',
  imports: [NavbarComponent, FooterComponent, ButtonComponent],
  templateUrl: './error.component.html',
})
export class ErrorComponent {
  message = 'Ha ocurrido un error inesperado. Por favor intenta de nuevo.';
  title = 'Error';


  isAdmin = false;
  isOperator = false;
  panelId: number | null = null;

  constructor(
    private router: Router,
    private route: ActivatedRoute,
    private auth: AuthService,
  ) {
    const session = this.auth.session;

    this.isAdmin = session?.type === 'admin';
    this.isOperator = session?.type === 'operator';
    this.panelId = session?.id ?? null;

    
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
  get hasPanel(): boolean {
    return this.isAdmin || this.isOperator;
  }

  goToPanel() {
    this.router.navigate([this.isAdmin ? '/admin' : '/operator', this.panelId]);
  }

  goHome() {
    this.router.navigate(['/home']);
  }

  goMenu() {
    this.router.navigate(['/menu']);
  }
}
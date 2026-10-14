import { Component } from '@angular/core';
import { ActivatedRoute, Router } from '@angular/router';
import { NavbarComponent } from '../../components/navbar/navbar.component';
import { FooterComponent } from '../../components/footer/footer.component';
import { ButtonComponent } from '../../components/button/button.component';
import { AuthService } from '../../services/auth.service';

@Component({
  selector: 'app-error',
  imports: [NavbarComponent, FooterComponent, ButtonComponent],
  templateUrl: './error.component.html',
})
export class ErrorComponent {
  message = 'Ha ocurrido un error inesperado. Por favor intenta de nuevo.';

  // Un administrador no ve la navbar ni el footer generales
  isAdmin = false;
  adminId: number | null = null;

  constructor(
    private router: Router,
    private route: ActivatedRoute,
    private auth: AuthService,
  ) {
    const session = this.auth.session;

    this.isAdmin = session?.type === 'admin';
    this.adminId = session?.id ?? null;

    // El mensaje llega en la URL: /error?message=...
    const message = this.route.snapshot.queryParamMap.get('message');

    if (message) {
      this.message = message;
    }
  }

  goToPanel() {
    this.router.navigate(['/admin', this.adminId]);
  }

  goHome() {
    this.router.navigate(['/home']);
  }

  goMenu() {
    this.router.navigate(['/menu']);
  }
}

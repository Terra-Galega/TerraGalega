import { Component, inject } from '@angular/core';
import { Router } from '@angular/router';

import { AuthService } from '../../services/auth.service';
import { OperatorTopbarComponent } from '../operator/components/operator-topbar/operator-topbar.component';
import { KitchenBoardComponent } from './components/kitchen-board/kitchen-board.component';

// Portal de cocina (/kitchen). Solo entra la cuenta de cocina (rol KITCHEN):
// lo protege kitchenGuard en la web y, en el back, las rutas de cocina solo
// aceptan su token. Muestra los pedidos de mesa que envían los meseros.
@Component({
  selector: 'app-kitchen',
  imports: [OperatorTopbarComponent, KitchenBoardComponent],
  templateUrl: './kitchen.component.html',
})
export class KitchenComponent {
  private auth = inject(AuthService);
  private router = inject(Router);

  // Nombre de la cuenta para la barra superior ("Cocina Terra Galega")
  name = `${this.auth.session?.name ?? ''} ${this.auth.session?.lastName ?? ''}`.trim();

  logout(): void {
    this.auth.logout();
    this.router.navigate(['/login'], { replaceUrl: true });
  }
}

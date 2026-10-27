import { Component } from '@angular/core';
import { ButtonComponent } from '../button/button.component';

// Tarjeta de invitación al final de la página de pedidos para quien no
// ha iniciado sesión: ofrece iniciar sesión, crear una cuenta o ver la carta.
// No tiene lógica propia; los botones solo navegan a otras rutas.
@Component({
  selector: 'app-ready-to-order',
  imports: [ButtonComponent],
  templateUrl: './ready-to-order.component.html',
  styleUrl: './ready-to-order.component.scss',
})
export class ReadyToOrderComponent {}
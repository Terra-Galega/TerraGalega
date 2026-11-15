import { Component, Input } from '@angular/core';
import { ButtonComponent } from '../button/button.component';

// Tarjeta de invitación para quien no ha iniciado sesión (pedidos y reservas):
// ofrece iniciar sesión, crear una cuenta o ver la carta.
// No tiene lógica propia; los botones solo navegan a otras rutas.
@Component({
  selector: 'app-ready-to-order',
  imports: [ButtonComponent],
  templateUrl: './ready-to-order.component.html',
  styleUrl: './ready-to-order.component.scss',
})
export class ReadyToOrderComponent {
  @Input() title = '¿Listo para pedir?';
  @Input() message = 'Inicia sesión si ya tienes cuenta, o regístrate en un minuto.';
}
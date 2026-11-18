import { TranslatePipe } from '../../services/translate.pipe';
import { Component, Input } from '@angular/core';
import { Params } from '@angular/router';
import { ButtonComponent } from '../button/button.component';

// Tarjeta de invitación para quien no ha iniciado sesión (pedidos y reservas):
// ofrece iniciar sesión, crear una cuenta o ver la carta.
// No tiene lógica propia; los botones solo navegan a otras rutas.
@Component({
  selector: 'app-ready-to-order',
  imports: [ButtonComponent, TranslatePipe],
  templateUrl: './ready-to-order.component.html',
  styleUrl: './ready-to-order.component.scss',
})
export class ReadyToOrderComponent {
  @Input() title = '¿Listo para pedir?';
  @Input() message = 'Inicia sesión si ya tienes cuenta, o regístrate en un minuto.';
  // A dónde volver después de entrar (p. ej. 'reservations'). Vacío = a la cuenta.
  @Input() redirect = '';

  // Parámetros del botón «Iniciar sesión»
  get loginParams(): Params {
    return this.redirect ? { redirect: this.redirect } : {};
  }

  // Parámetros del botón «Crear una cuenta»: abre la pestaña de registro
  get signupParams(): Params {
    return { tab: 'signup', ...this.loginParams };
  }
}
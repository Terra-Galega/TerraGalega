import { Injectable, inject, signal } from '@angular/core';
import { DELIVERY_FEE } from '../models/checkout';
import { AuthService } from './auth.service';
import { ClientService } from './client.service';

// Costo del domicilio que se le muestra al cliente desde el carrito.
// Antes solo aparecía en el último paso del checkout y el total "saltaba" de golpe;
// ahora el carrito y los dos pasos muestran lo mismo desde el principio.
// Premium = domicilio gratis. Quien cobra de verdad es el back: esto solo lo refleja.
@Injectable({ providedIn: 'root' })
export class DeliveryFeeService {
  private auth = inject(AuthService);
  private clientService = inject(ClientService);

  // true si el cliente con sesión es Premium
  readonly free = signal(false);

  // Lo que se cobra de domicilio: 0 para Premium
  get fee(): number {
    return this.free() ? 0 : DELIVERY_FEE;
  }

  // Vuelve a preguntar si es Premium (al abrir el carrito o el checkout:
  // así se refleja también si se acaba de suscribir)
  refresh(): void {
    const session = this.auth.session;
    if (!session || session.type !== 'client') {
      this.free.set(false);
      return;
    }

    this.clientService.getMe().subscribe({
      next: (client) => this.free.set(client.subscribed === true),
      // Si falla se deja el valor anterior; el back cobra lo que corresponda
      error: () => undefined,
    });
  }
}

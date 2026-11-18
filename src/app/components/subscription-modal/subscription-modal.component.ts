import { TranslatePipe } from '../../services/translate.pipe';
import { Component, EventEmitter, HostListener, Input, Output } from '@angular/core';
import { DialogDirective } from '../../directives/dialog.directive';
import { ButtonComponent } from '../button/button.component';
import { SubscriptionStatus } from '../../models/subscription';

// Modal de la suscripción Premium: precio, beneficios y aviso de que no hay reembolso.
// El padre controla si está abierto y reacciona a confirm (pagar) y cancel (cerrar).
@Component({
  selector: 'app-subscription-modal',
  imports: [DialogDirective, ButtonComponent, TranslatePipe],
  templateUrl: './subscription-modal.component.html',
})
export class SubscriptionModalComponent {
  @Input() open = false;
  // Precio, duración y días restantes: vienen del back
  @Input() subscription: SubscriptionStatus | null = null;
  // true desde que se pulsa "Pagar" hasta salir hacia Stripe
  @Input() starting = false;

  @Output() confirm = new EventEmitter<void>();
  @Output() cancel = new EventEmitter<void>();

  // Escape cierra el modal (solo si está abierto)
  @HostListener('document:keydown.escape')
  onEscape(): void {
    if (this.open) {
      this.cancel.emit();
    }
  }

  // true si ya es Premium y está renovando
  get renewing(): boolean {
    return this.subscription?.subscribed === true;
  }

  get title(): string {
    return this.renewing ? 'Renueva tu Premium' : 'Hazte Premium';
  }

  get price(): string {
    return `$${(this.subscription?.priceCop ?? 0).toLocaleString('es-CO')}`;
  }

  get durationDays(): number {
    return this.subscription?.durationDays ?? 0;
  }

  get daysLeftText(): string {
    const days = this.subscription?.daysLeft ?? 0;
    return `${days} ${days === 1 ? 'día' : 'días'}`;
  }

  get payLabel(): string {
    return this.starting ? 'Abriendo el pago…' : `Pagar ${this.price}`;
  }
}
import { TranslatePipe } from '../../../../pipes/translate.pipe';
import { Component, EventEmitter, Input, Output } from '@angular/core';
import { ButtonComponent } from '../../../../components/button/button.component';
import { SubscriptionStatus } from '../../../../models/subscription';
import { formatSubscriptionDate } from '../../../../services/subscription.service';

@Component({
  selector: 'app-account-actions',
  imports: [ButtonComponent, TranslatePipe],
  templateUrl: './account-actions.component.html',
  host: { class: 'block h-full' },
})
export class AccountActionsComponent {
  // Estado de la suscripción (null mientras carga o si el back no respondió)
  @Input() subscription: SubscriptionStatus | null = null;
  // true mientras se confirma un pago al volver de Stripe: bloquea el botón
  @Input() confirmingPayment = false;

  //evento de cerrar sesión
  @Output() logout = new EventEmitter<void>();
  // El cliente pulsó el botón Premium; el padre abre el modal
  @Output() upgrade = new EventEmitter<void>();

  // Freemium -> "Suscribirme a Premium" | Premium >5 días -> "Premium activo" (deshabilitado)
  // Premium <=5 días -> "Renovar Premium". La regla la decide el back (canSubscribe).
  get premiumLabel(): string {
    const s = this.subscription;

    if (s?.subscribed) {
      return s.canSubscribe ? 'Renovar Premium' : 'Premium activo';
    }

    return 'Suscribirme a Premium';
  }

  get premiumDisabled(): boolean {
    return this.confirmingPayment || !this.subscription?.canSubscribe;
  }

  // Texto pequeño bajo el botón
  get premiumHint(): string {
    if (this.confirmingPayment) {
      return 'Confirmando tu pago…';
    }

    const s = this.subscription;
    if (!s) {
      return '';
    }

    if (!s.subscribed) {
      return `Domicilios gratis por ${s.durationDays} días.`;
    }

    const days = `${s.daysLeft} ${s.daysLeft === 1 ? 'día' : 'días'}`;
    const date = formatSubscriptionDate(s.subscriptionEndsAt);

    return s.canSubscribe
      ? `Te quedan ${days} (vence el ${date}). Ya puedes renovar.`
      : `Vence el ${date}. Te quedan ${days}.`;
  }

  onUpgrade(): void {
    // Por si el clic llega con el botón deshabilitado
    if (!this.premiumDisabled) {
      this.upgrade.emit();
    }
  }
}
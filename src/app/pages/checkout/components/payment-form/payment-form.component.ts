import { Component, EventEmitter, Input, Output } from '@angular/core';
import { FormGroup, ReactiveFormsModule } from '@angular/forms';

@Component({
  selector: 'app-payment-form',
  standalone: true,
  imports: [ReactiveFormsModule],
  templateUrl: './payment-form.component.html',
})
export class PaymentFormComponent {
  @Input({ required: true }) form!: FormGroup;

  // Errores por campo: { number: '...', cvv: '...' }
  @Input() errors: Record<string, string> = {};

  @Output() fillTestData = new EventEmitter<void>();

  methods = [
    {
      value: 'cash',
      title: 'Contra entrega',
      description: 'Pagas en efectivo cuando recibas tu pedido.',
    },
    {
      value: 'card',
      title: 'Tarjeta',
      description: 'Crédito o débito, con pasarela de pago segura.',
    },
  ];

  installments = [1, 3, 6, 12, 24, 36];

  get method(): string {
    return this.form.get('method')?.value ?? '';
  }

  // Mensajes de error de la tarjeta, uno debajo del otro
  get cardErrors(): string[] {
    return ['holder', 'documentNumber', 'number', 'expiry', 'cvv']
      .map((key) => this.errors[key])
      .filter(Boolean);
  }

  // 4111111111111111 -> 4111 1111 1111 1111
  formatCardNumber(event: Event): void {
    const input = event.target as HTMLInputElement;
    const digits = input.value.replace(/\D/g, '').slice(0, 16);
    input.value = digits.replace(/(.{4})/g, '$1 ').trim();

    this.form.get('number')?.setValue(input.value, { emitEvent: false });
  }

  // 1228 -> 12/28
  formatExpiry(event: Event): void {
    const input = event.target as HTMLInputElement;
    const digits = input.value.replace(/\D/g, '').slice(0, 4);
    input.value =
      digits.length > 2 ? `${digits.slice(0, 2)}/${digits.slice(2)}` : digits;

    this.form.get('expiry')?.setValue(input.value, { emitEvent: false });
  }

  formatCvv(event: Event): void {
    const input = event.target as HTMLInputElement;
    input.value = input.value.replace(/\D/g, '').slice(0, 3);

    this.form.get('cvv')?.setValue(input.value, { emitEvent: false });
  }
}
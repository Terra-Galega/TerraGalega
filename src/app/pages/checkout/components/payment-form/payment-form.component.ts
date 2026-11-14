import { Component, Input } from '@angular/core';
import { FormGroup, ReactiveFormsModule } from '@angular/forms';
import {
  CheckoutErrors,
  emptyErrors,
} from '../../../../models/checkout-errors';

// Selección del método de pago. Trabaja sobre el formulario del padre, que es
// quien valida y guarda. Con tarjeta no se piden datos aquí: el cliente los
// escribe en la página segura de Stripe al confirmar el pedido.
@Component({
  selector: 'app-payment-form',
  standalone: true,
  imports: [ReactiveFormsModule],
  templateUrl: './payment-form.component.html',
})
export class PaymentFormComponent {
  // Formulario del padre (control: method)
  @Input({ required: true }) form!: FormGroup;

  // Mensaje de error de cada campo del checkout
  @Input() errors: CheckoutErrors = emptyErrors();

  // Opciones de pago que se pintan como tarjetas seleccionables
  methods = [
    {
      value: 'cash',
      title: 'Contra entrega',
      description: 'Pagas en efectivo cuando recibas tu pedido.',
    },
    {
      value: 'card',
      title: 'Tarjeta',
      description: 'Crédito o débito, en la pasarela segura de Stripe.',
    },
  ];

  // Método elegido actualmente ('cash' | 'card' | '')
  get method(): string {
    return this.form.get('method')?.value ?? '';
  }
}

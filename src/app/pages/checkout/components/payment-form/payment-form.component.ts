import { Component, EventEmitter, Input, Output } from '@angular/core';
import { FormGroup, ReactiveFormsModule } from '@angular/forms';
import { FormFieldComponent } from '../../../../components/form-field/form-field.component';
import { ButtonComponent } from '../../../../components/button/button.component';

@Component({
  selector: 'app-payment-form',
  standalone: true,
  imports: [
    ReactiveFormsModule,
    FormFieldComponent,
    ButtonComponent,
  ],
  templateUrl: './payment-form.component.html',
})
export class PaymentFormComponent {
  @Input({ required: true }) form!: FormGroup;
  @Input() paying = false;
  @Input() cardError = '';
  @Input() serverError = '';

  @Output() cancel = new EventEmitter<void>();

  formatCardNumber(event: Event): void {
    const input = event.target as HTMLInputElement;
    const digits = input.value.replace(/\D/g, '').slice(0, 16);
    input.value = digits.replace(/(.{4})/g, '$1 ').trim();

    this.form.get('cardNumber')?.setValue(input.value, {
      emitEvent: false,
    });
  }

  formatExpiry(event: Event): void {
    const input = event.target as HTMLInputElement;
    const digits = input.value.replace(/\D/g, '').slice(0, 4);
    input.value =
      digits.length > 2
        ? `${digits.slice(0, 2)}/${digits.slice(2)}`
        : digits;

    this.form.get('cardExpiry')?.setValue(input.value, {
      emitEvent: false,
    });
  }

  formatCvv(event: Event): void {
    const input = event.target as HTMLInputElement;
    input.value = input.value.replace(/\D/g, '').slice(0, 3);

    this.form.get('cardCvv')?.setValue(input.value, {
      emitEvent: false,
    });
  }
}
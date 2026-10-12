import { Component, EventEmitter, Input, Output } from '@angular/core';
import { FormGroup, ReactiveFormsModule } from '@angular/forms';
import { ButtonComponent } from '../../../../components/button/button.component';
import { FormFieldComponent } from '../../../../components/form-field/form-field.component';

@Component({
  selector: 'app-delivery-address',
  standalone: true,
  imports: [
    ReactiveFormsModule,
    FormFieldComponent,
    ButtonComponent,
  ],
  templateUrl: './delivery-address.component.html',
})
export class DeliveryAddressComponent {
  @Input({ required: true }) form!: FormGroup;
  @Input() addressError = false;

  @Output() fillTestData = new EventEmitter<void>();
}
import { Component, EventEmitter, Input, Output } from '@angular/core';
import { FormGroup, ReactiveFormsModule } from '@angular/forms';

@Component({
  selector: 'app-delivery-address',
  standalone: true,
  imports: [ReactiveFormsModule],
  templateUrl: './delivery-address.component.html',
})
export class DeliveryAddressComponent {
  @Input({ required: true }) form!: FormGroup;

  // Mensaje de error (vacío = sin error)
  @Input() error = '';

  // Aviso de que se actualizará la dirección del perfil (vacío = no se muestra)
  @Input() notice = '';

  @Input() savedAddress = '';

  @Output() useSavedAddress = new EventEmitter<void>();

  // Cuántos caracteres lleva escritos (para el contador)
  get length(): number {
    return (this.form.get('address')?.value ?? '').length;
  }

  // El botón solo aparece si la dirección escrita es distinta a la guardada
  get canRestore(): boolean {
    const current = (this.form.get('address')?.value ?? '').trim();
    return !!this.savedAddress && current !== this.savedAddress.trim();
  }
}
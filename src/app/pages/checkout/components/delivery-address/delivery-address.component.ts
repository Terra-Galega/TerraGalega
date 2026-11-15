import {
  Component,
  ElementRef,
  Input,
  OnChanges,
  SimpleChanges,
  ViewChild,
} from '@angular/core';
import { FormGroup, ReactiveFormsModule } from '@angular/forms';
import { Address } from '../../../../models/address';
import { AddressIconComponent } from '../../../../components/address-icon/address-icon.component';
import { AddressMapPickerComponent } from '../../../../components/address-map-picker/address-map-picker.component';

// Dirección de entrega del checkout: elegir una guardada o escribir otra.
@Component({
  selector: 'app-delivery-address',
  standalone: true,
  imports: [
    ReactiveFormsModule,
    AddressIconComponent,
    AddressMapPickerComponent,
  ],
  templateUrl: './delivery-address.component.html',
})
export class DeliveryAddressComponent implements OnChanges {
  // Formulario del padre; usa sus controles `address`, `saveAddress` y `addressName`
  @Input({ required: true }) form!: FormGroup;

  // Mensaje de error de la dirección (vacío = sin error)
  @Input() error = '';

  // Mensaje de error del nombre de la dirección nueva
  @Input() nameError = '';

  // Direcciones guardadas del cliente
  @Input() addresses: Address[] = [];

  // true mientras el padre carga las direcciones guardadas
  @Input() loading = false;

  @ViewChild('newAddressInput') newAddressInput?: ElementRef<HTMLInputElement>;

  // true = el cliente está escribiendo una dirección que no está en su lista
  newMode = false;

  // El cliente movió el mapa o hizo clic: el texto pasa a ser la dirección de ese punto
  onMapPicked(address: string): void {
    const control = this.form.get('address');

    control?.setValue(address);
    control?.markAsDirty();
    control?.markAsTouched();
  }
  // Cuando llegan las direcciones se decide si se muestra el campo de texto
  ngOnChanges(changes: SimpleChanges): void {
    if (changes['addresses'] || changes['loading']) {
      this.syncMode();
    }
  }

  // Dirección escrita o elegida, sin espacios sobrantes
  get current(): string {
    return (this.form.get('address')?.value ?? '').trim();
  }

  // Cuántos caracteres lleva escritos (para el contador)
  get length(): number {
    return (this.form.get('address')?.value ?? '').length;
  }

  // ¿Está marcado "Guardar esta dirección en mi perfil"?
  get saveChecked(): boolean {
    return this.form.get('saveAddress')?.value === true;
  }

  // El checkbox de guardar solo tiene sentido con una dirección nueva
  get showSaveOption(): boolean {
    return (
      this.newMode && this.current !== '' && !this.matchesSaved(this.current)
    );
  }

  // ¿Es esta la dirección elegida?
  isSelected(address: Address): boolean {
    return (
      !this.newMode &&
      this.current.toLowerCase() === address.label.trim().toLowerCase()
    );
  }

  // Elige una dirección guardada
  chooseSaved(address: Address): void {
    this.newMode = false;
    this.form.get('address')?.setValue(address.label);
  }

  // Pasa a escribir otra dirección (por defecto se ofrece guardarla)
  chooseNew(): void {
    this.newMode = true;

    this.form.get('address')?.setValue('');
    this.form.get('addressName')?.setValue('');
    this.form.get('saveAddress')?.setValue(true);

    setTimeout(() => this.newAddressInput?.nativeElement.focus());
  }

  // Alterna el checkbox de guardar (el control real está oculto)
  toggleSave(): void {
    this.form.get('saveAddress')?.setValue(!this.saveChecked);
  }

  // Sin direcciones guardadas, o con una escrita que no está en la lista,
  // se muestra el campo de texto
  private syncMode(): void {
    if (this.loading) {
      return;
    }
    this.newMode =
      this.addresses.length === 0 ||
      (this.current !== '' && !this.matchesSaved(this.current));
  }

  // Misma regla que el back: sin importar mayúsculas
  private matchesSaved(text: string): boolean {
    const value = text.trim().toLowerCase();
    return this.addresses.some((a) => a.label.trim().toLowerCase() === value);
  }
}

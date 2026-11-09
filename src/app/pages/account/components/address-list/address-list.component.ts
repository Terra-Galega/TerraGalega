import { Component, Input, OnInit, inject } from '@angular/core';
import { Address } from '../../../../models/address';
import { AddressService } from '../../../../services/address.service';
import { ToastService } from '../../../../services/toast.service';
import { ButtonComponent } from '../../../../components/button/button.component';
import { ModalComponent } from '../../../../components/modal/modal.component';
import { getErrorMessage } from '../../../../utils/http-error';
import { FormFieldComponent } from '../../../../components/form-field/form-field.component';
import { AddressIconComponent } from '../../../../components/address-icon/address-icon.component';

// Tarjeta "Mis direcciones": lista, agrega, edita, elimina y marca la predeterminada.
@Component({
  selector: 'app-address-list',
  imports: [
    ButtonComponent,
    ModalComponent,
    FormFieldComponent,
    AddressIconComponent,
  ],
  templateUrl: './address-list.component.html',
  host: { class: 'block' },
})
export class AddressListComponent implements OnInit {
  // Cliente dueño de las direcciones
  @Input({ required: true }) clientId!: number;

  private addressService = inject(AddressService);
  private toast = inject(ToastService);

  // Máximos que admite el back
  readonly labelMax = 120;
  readonly nameMax = 40;

  addresses: Address[] = [];
  loading = true;

  // Formulario para agregar (el nombre es opcional: sin él se llama "Casa")
  newName = '';
  newLabel = '';

  // Dirección que se está editando (null = ninguna) y sus campos
  editingId: number | null = null;
  editName = '';
  editLabel = '';

  // Dirección pendiente de confirmar para eliminar (null = modal cerrado)
  addressToDelete: Address | null = null;

  // Mensaje de error de la última acción (vacío = sin error)
  error = '';

  ngOnInit(): void {
    this.load();
  }

  // Trae la lista del back. Se vuelve a llamar tras cada cambio porque el back
  // puede mover la marca de predeterminada y el orden.
  private load(): void {
    this.addressService.getAddresses(this.clientId).subscribe({
      next: (addresses) => {
        this.addresses = addresses;
        this.loading = false;
      },
      error: (error) => {
        console.error('Error cargando direcciones:', error);
        this.loading = false;
        this.error = getErrorMessage(
          error,
          'No se pudieron cargar tus direcciones.',
        );
      },
    });
  }

  // Texto del modal de eliminación
  get deleteMessage(): string {
    const address = this.addressToDelete;
    return address
      ? `¿Seguro que quieres eliminar la dirección «${address.name}»?`
      : '';
  }

  setNewName(event: Event): void {
    this.newName = (event.target as HTMLInputElement).value;
  }

  setNewLabel(event: Event): void {
    this.newLabel = (event.target as HTMLInputElement).value;
  }

  setEditName(event: Event): void {
    this.editName = (event.target as HTMLInputElement).value;
  }

  setEditLabel(event: Event): void {
    this.editLabel = (event.target as HTMLInputElement).value;
  }

  // Agrega la dirección escrita
  add(): void {
    const label = this.newLabel.trim();
    if (!label) {
      this.error = 'Escribe la dirección.';
      return;
    }

    this.error = '';
    this.addressService
      .addAddress(this.clientId, { label, name: this.newName.trim() })
      .subscribe({
        next: () => {
          this.newName = '';
          this.newLabel = '';
          this.toast.show('Dirección agregada correctamente');
          this.load();
        },
        error: (error) => {
          this.error = getErrorMessage(
            error,
            'No se pudo agregar la dirección.',
          );
        },
      });
  }

  // Pasa una dirección a modo edición
  startEdit(address: Address): void {
    this.error = '';
    this.editingId = address.id;
    this.editName = address.name;
    this.editLabel = address.label;
  }

  cancelEdit(): void {
    this.error = '';
    this.editingId = null;
    this.editName = '';
    this.editLabel = '';
  }

  // Guarda el nombre y el texto editados
  saveEdit(): void {
    const address = this.addresses.find((a) => a.id === this.editingId);
    if (!address) {
      return;
    }

    const name = this.editName.trim();
    const label = this.editLabel.trim();
    if (!name) {
      this.error = 'Escribe el nombre de la dirección.';
      return;
    }
    if (!label) {
      this.error = 'Escribe la dirección.';
      return;
    }

    this.error = '';
    this.addressService
      .updateAddress(this.clientId, address.id, {
        name,
        label,
        defaultAddress: address.defaultAddress,
      })
      .subscribe({
        next: () => {
          this.cancelEdit();
          this.toast.show('Dirección actualizada correctamente');
          this.load();
        },
        error: (error) => {
          this.error = getErrorMessage(
            error,
            'No se pudo actualizar la dirección.',
          );
        },
      });
  }

  // Marca una dirección como predeterminada (el back se la quita a la anterior)
  makeDefault(address: Address): void {
    this.error = '';
    this.addressService
      .updateAddress(this.clientId, address.id, {
        name: address.name,
        label: address.label,
        defaultAddress: true,
      })
      .subscribe({
        next: () => {
          this.toast.show('Dirección predeterminada actualizada');
          this.load();
        },
        error: (error) => {
          this.error = getErrorMessage(
            error,
            'No se pudo cambiar la dirección predeterminada.',
          );
        },
      });
  }

  // Abre el modal de confirmación
  askDelete(address: Address): void {
    this.error = '';
    this.addressToDelete = address;
  }

  cancelDelete(): void {
    this.addressToDelete = null;
  }

  // Elimina la dirección confirmada
  confirmDelete(): void {
    const address = this.addressToDelete;
    this.addressToDelete = null;
    if (!address) {
      return;
    }

    this.addressService.deleteAddress(this.clientId, address.id).subscribe({
      next: () => {
        this.toast.show('Dirección eliminada correctamente');
        this.load();
      },
      error: (error) => {
        this.error = getErrorMessage(
          error,
          'No se pudo eliminar la dirección.',
        );
      },
    });
  }
}

import { Component, EventEmitter, Input, Output, inject } from '@angular/core';
import { DecimalPipe } from '@angular/common';
import { EmptyStateComponent } from '../../../../components/empty-state/empty-state.component';
import { IconButtonComponent } from '../../../../components/icon-button/icon-button.component';
import { AddOn } from '../../../../models/addOn';
import { AddOnService } from '../../../../services/addOn.service';
import { ModalComponent, getErrorMessage } from '../../../../components/modal/modal.component';

@Component({
  selector: 'app-admin-addons',
  imports: [DecimalPipe, EmptyStateComponent, IconButtonComponent, ModalComponent],
  templateUrl: './admin-addons.component.html',
})
export class AdminAddonsComponent {
  private addOnService = inject(AddOnService);

  @Input() searchTerm = '';

  @Output() editAddon = new EventEmitter<AddOn>();

  readonly columns = [
    'Adicional',
    'Categorías',
    'Precio',
    'Estado',
    'Acciones',
  ];

  addons: AddOn[] = [];
  filteredAddons: AddOn[] = [];

  showDeleteModal = false;
  showErrorModal = false;

  selectedAddonId: number | null = null;

  confirmTitle = 'Eliminar adicional';
  confirmMessage = '¿Estás seguro de que deseas eliminar este adicional?';

  errorTitle = '';
  errorMessage = '';

  constructor() {
    this.loadAddons();

    this.addOnService.refresh$.subscribe(() => {
      this.loadAddons();
    });
  }

  loadAddons() {
    this.addOnService.getAddOns().subscribe({
      next: (addons) => {
        this.addons = addons;
        this.filterAddons();
      },
      error: (error) => {
        console.error('Error cargando adicionales:', error);
      },
    });
  }

  ngOnChanges() {
    this.filterAddons();
  }

  filterAddons() {
    const value = this.searchTerm.trim().toLowerCase();

    this.filteredAddons = this.addons.filter((addon) =>
      addon.name.toLowerCase().includes(value),
    );
  }

  toggleAddon(addon: AddOn) {
    this.addOnService.toggleAddOnActiveStatus(addon.id).subscribe({
      next: () => {
        this.addOnService.refresh();
      },
      error: (error) => {
        console.error('Error cambiando estado del adicional:', error);
      },
    });
  }

  openEdit(addon: AddOn) {
    this.editAddon.emit(addon);
  }

  removeAddon(addon: AddOn) {
    this.selectedAddonId = addon.id;
    this.showDeleteModal = true;
  }

  cancelDelete() {
    this.showDeleteModal = false;
    this.selectedAddonId = null;
  }

  confirmDelete() {
    if (this.selectedAddonId === null) {
      return;
    }

    const id = this.selectedAddonId;

    this.showDeleteModal = false;
    this.selectedAddonId = null;

    this.addOnService.deleteAddOn(id).subscribe({
      next: () => {
        this.addOnService.refresh();
      },
      error: (error) => {
        console.error('Error eliminando adicional:', error);
        this.openErrorModal(
          'No se pudo eliminar el adicional',
          getErrorMessage(
            error,
            'Ocurrió un error al eliminar el adicional. Por favor, inténtalo de nuevo.',
          ),
        );
      },
    });
  }

  private openErrorModal(title: string, message: string) {
    this.errorTitle = title;
    this.errorMessage = message;
    this.showErrorModal = true;
  }

  closeErrorModal() {
    this.showErrorModal = false;
    this.errorTitle = '';
    this.errorMessage = '';
  }
}
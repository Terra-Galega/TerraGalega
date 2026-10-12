import { Component, EventEmitter, Input, Output, inject } from '@angular/core';
import { DecimalPipe } from '@angular/common';
import { EmptyStateComponent } from '../../../../components/empty-state/empty-state.component';
import { IconButtonComponent } from '../../../../components/icon-button/icon-button.component';
import { AddOn } from '../../../../models/addOn';
import { AddOnService } from '../../../../services/addOn.service';

@Component({
  selector: 'app-admin-addons',
  imports: [DecimalPipe, EmptyStateComponent, IconButtonComponent],
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
    const confirmed = window.confirm('¿Eliminar este adicional?');

    if (!confirmed) {
      return;
    }

    this.addOnService.deleteAddOn(addon.id).subscribe({
      next: () => {
        this.addOnService.refresh();
      },
      error: (error) => {
        console.error('Error eliminando adicional:', error);
        window.alert(
          error.error?.message ?? 'No se pudo eliminar el adicional.',
        );
      },
    });
  }
}

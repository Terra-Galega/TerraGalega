import { Component, EventEmitter, Input, Output, inject } from '@angular/core';
import { DecimalPipe } from '@angular/common';
import { EmptyStateComponent } from '../../../../components/empty-state/empty-state.component';
import { IconButtonComponent } from '../../../../components/icon-button/icon-button.component';
import { AddOn } from '../../../../models/addOn';
import { AddOnService } from '../../../../services/addOn.service';
import { ModalComponent } from '../../../../components/modal/modal.component';
import { ToastService } from '../../../../services/toast.service';
import { getErrorMessage } from '../../../../utils/http-error';

/**
 * Tabla de adicionales del panel de administración.
 * Muestra cada adicional con sus categorías, precio y estado; permite
 * filtrar por nombre, activar/desactivar, editar y eliminar (con ventana
 * de confirmación y ventana de error si el servidor rechaza la eliminación).
 */
@Component({
  selector: 'app-admin-addons',
  imports: [
    DecimalPipe,
    EmptyStateComponent,
    IconButtonComponent,
    ModalComponent,
  ],
  templateUrl: './admin-addons.component.html',
})
export class AdminAddonsComponent {
  private addOnService = inject(AddOnService);
  private toast = inject(ToastService);

  // Texto de búsqueda que escribe el admin en la barra superior
  @Input() searchTerm = '';

  // Avisa al padre qué adicional se quiere editar (el padre abre el modal)
  @Output() editAddon = new EventEmitter<AddOn>();

  // Encabezados de la tabla
  readonly columns = [
    'Adicional',
    'Categorías',
    'Precio',
    'Estado',
    'Acciones',
  ];

  // Todos los adicionales que devuelve el servidor
  addons: AddOn[] = [];
  // Adicionales que coinciden con la búsqueda (los que se dibujan)
  filteredAddons: AddOn[] = [];

  // Controlan la ventana de confirmación y la ventana de error
  showDeleteModal = false;
  showErrorModal = false;

  // Id del adicional que se está a punto de eliminar
  selectedAddonId: number | null = null;

  // Textos de la ventana de confirmación
  confirmTitle = 'Eliminar adicional';
  confirmMessage = '¿Estás seguro de que deseas eliminar este adicional?';

  // Textos de la ventana de error (se llenan según el fallo)
  errorTitle = '';
  errorMessage = '';

  constructor() {
    this.loadAddons();

    // Cada vez que otro componente pide refrescar (por ejemplo, tras guardar
    // en el modal), se vuelve a cargar la lista
    this.addOnService.refresh$.subscribe(() => {
      this.loadAddons();
    });
  }

  // Pide los adicionales al servidor y vuelve a aplicar el filtro
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

  // Angular lo llama cuando cambia un @Input (aquí, searchTerm)
  ngOnChanges() {
    this.filterAddons();
  }

  // Deja solo los adicionales cuyo nombre contiene el texto buscado
  // (sin distinguir mayúsculas de minúsculas)
  filterAddons() {
    const value = this.searchTerm.trim().toLowerCase();

    this.filteredAddons = this.addons.filter((addon) =>
      addon.name.toLowerCase().includes(value),
    );
  }

  // Activa o desactiva el adicional (botón "Activo/Inactivo" de la fila)
  toggleAddon(addon: AddOn) {
    this.addOnService.toggleAddOnActiveStatus(addon.id).subscribe({
      next: () => {
        // Se recarga la lista para ver el nuevo estado
        this.addOnService.refresh();
      },
      error: (error) => {
        console.error('Error cambiando estado del adicional:', error);
      },
    });
  }

  // Reenvía al padre el adicional que se quiere editar
  openEdit(addon: AddOn) {
    this.editAddon.emit(addon);
  }

  // Paso 1 de eliminar: guarda cuál es el adicional y abre la confirmación
  removeAddon(addon: AddOn) {
    this.selectedAddonId = addon.id;
    this.showDeleteModal = true;
  }

  // El admin se arrepintió: se cierra la confirmación y se olvida el adicional
  cancelDelete() {
    this.showDeleteModal = false;
    this.selectedAddonId = null;
  }

  // Paso 2 de eliminar: el admin confirmó
  confirmDelete() {
    if (this.selectedAddonId === null) {
      return;
    }

    // Se guarda el id en una variable local porque se limpia justo después
    const id = this.selectedAddonId;

    this.showDeleteModal = false;
    this.selectedAddonId = null;

    this.addOnService.deleteAddOn(id).subscribe({
      next: () => {
        this.addOnService.refresh();
        this.toast.show('Adicional eliminado correctamente');
      },
      error: (error) => {
        console.error('Error eliminando adicional:', error);
        // Se muestra el mensaje que mandó el servidor, o uno genérico
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

  // Abre la ventana de error con el título y mensaje indicados
  private openErrorModal(title: string, message: string) {
    this.errorTitle = title;
    this.errorMessage = message;
    this.showErrorModal = true;
  }

  // Cierra la ventana de error y limpia sus textos
  closeErrorModal() {
    this.showErrorModal = false;
    this.errorTitle = '';
    this.errorMessage = '';
  }
}

import {
  Component,
  EventEmitter,
  Input,
  OnChanges,
  Output,
  inject,
} from '@angular/core';
import { EmptyStateComponent } from '../../../../components/empty-state/empty-state.component';
import { IconButtonComponent } from '../../../../components/icon-button/icon-button.component';
import { DeliveryPerson } from '../../../../models/deliveryPerson';
import { DeliveryPersonService } from '../../../../services/delivery-person.service';
import { ModalComponent, getErrorMessage } from '../../../../components/modal/modal.component';
import { ChangePasswordModalComponent } from '../../../../components/change-password-modal/change-password-modal.component';


// Tabla de domiciliarios del portal admin: editar, eliminar y activar/desactivar
@Component({
  selector: 'app-admin-delivery-persons',
  imports: [EmptyStateComponent, IconButtonComponent, ModalComponent, ChangePasswordModalComponent],
  templateUrl: './admin-delivery-persons.component.html',
})
export class AdminDeliveryPersonsComponent implements OnChanges {
  private deliveryPersonService = inject(DeliveryPersonService);

  passwordDeliveryPerson: DeliveryPerson | null = null;
  savePassword = (current: string, next: string) =>
  this.deliveryPersonService.changePassword(this.passwordDeliveryPerson!.id, current, next);

  @Input() searchTerm = '';

  @Output() editDeliveryPerson = new EventEmitter<DeliveryPerson>();

  readonly columns = [
    'Domiciliario',
    'Identificación',
    'Teléfono',
    'Disponibilidad',
    'Estado',
    'Acciones',
  ];

  deliveryPersons: DeliveryPerson[] = [];
  filteredDeliveryPersons: DeliveryPerson[] = [];

  showDeleteModal = false;
  showErrorModal = false;

  selectedDeliveryPersonId: number | null = null;

  confirmTitle = 'Eliminar domiciliario';
  confirmMessage = '¿Estás seguro de que deseas eliminar este domiciliario?';

  errorTitle = '';
  errorMessage = '';

  constructor() {
    this.loadDeliveryPersons();

    this.deliveryPersonService.refresh$.subscribe(() => {
      this.loadDeliveryPersons();
    });
  }

  loadDeliveryPersons() {
    this.deliveryPersonService.getDeliveryPersons().subscribe({
      next: (deliveryPersons) => {
        this.deliveryPersons = deliveryPersons;
        this.filterDeliveryPersons();
      },
      error: (error) => {
        console.error('Error cargando domiciliarios:', error);
      },
    });
  }

  ngOnChanges() {
    this.filterDeliveryPersons();
  }

  // Busca por nombre, apellido, correo o identificación
  filterDeliveryPersons() {
    const value = this.searchTerm.trim().toLowerCase();

    this.filteredDeliveryPersons = this.deliveryPersons.filter((person) =>
      `${person.name} ${person.lastName} ${person.email} ${person.identification}`
        .toLowerCase()
        .includes(value),
    );
  }

  // Un domiciliario inactivo no aparece al enviar pedidos
  toggleDeliveryPerson(person: DeliveryPerson) {
    this.deliveryPersonService.toggleDeliveryPersonActive(person.id).subscribe({
      next: () => {
        this.deliveryPersonService.refresh();
      },
      error: (error) => {
        console.error('Error cambiando estado del domiciliario:', error);
      },
    });
  }

  openEdit(person: DeliveryPerson) {
    this.editDeliveryPerson.emit(person);
  }

  removeDeliveryPerson(person: DeliveryPerson) {
    this.selectedDeliveryPersonId = person.id;
    this.showDeleteModal = true;
  }

  cancelDelete() {
    this.showDeleteModal = false;
    this.selectedDeliveryPersonId = null;
  }

  confirmDelete() {
    if (this.selectedDeliveryPersonId === null) {
      return;
    }

    const id = this.selectedDeliveryPersonId;

    this.showDeleteModal = false;
    this.selectedDeliveryPersonId = null;

    this.deliveryPersonService.deleteDeliveryPerson(id).subscribe({
      next: () => {
        this.deliveryPersonService.refresh();
      },
      error: (error) => {
        console.error('Error eliminando domiciliario:', error);
        this.openErrorModal(
          'No se pudo eliminar el domiciliario',
          getErrorMessage(
            error,
            'Ocurrió un error al eliminar el domiciliario. Por favor, inténtalo de nuevo.',
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

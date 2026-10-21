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

// Tabla de domiciliarios del portal admin: editar, eliminar y activar/desactivar
@Component({
  selector: 'app-admin-delivery-persons',
  imports: [EmptyStateComponent, IconButtonComponent],
  templateUrl: './admin-delivery-persons.component.html',
})
export class AdminDeliveryPersonsComponent implements OnChanges {
  private deliveryPersonService = inject(DeliveryPersonService);

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
    const confirmed = window.confirm('¿Eliminar este domiciliario?');

    if (!confirmed) {
      return;
    }

    this.deliveryPersonService.deleteDeliveryPerson(person.id).subscribe({
      next: () => {
        this.deliveryPersonService.refresh();
      },
      error: (error) => {
        console.error('Error eliminando domiciliario:', error);
        window.alert(
          error.error?.message ?? 'No se pudo eliminar el domiciliario.',
        );
      },
    });
  }
}

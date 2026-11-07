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
import { ModalComponent } from '../../../../components/modal/modal.component';
import { ChangePasswordModalComponent } from '../../../../components/change-password-modal/change-password-modal.component';
import { ToastService } from '../../../../services/toast.service';
import { getErrorMessage } from '../../../../utils/http-error';

// Tabla de domiciliarios del portal admin: editar, eliminar y activar/desactivar.
// Es un componente "tonto" respecto a la edición: no abre el formulario, solo
// avisa al padre (editDeliveryPerson) para que él muestre el modal de edición.
@Component({
  selector: 'app-admin-delivery-persons',
  imports: [
    EmptyStateComponent,
    IconButtonComponent,
    ModalComponent,
    ChangePasswordModalComponent,
  ],
  templateUrl: './admin-delivery-persons.component.html',
})
export class AdminDeliveryPersonsComponent implements OnChanges {
  // Servicio que habla con el backend (CRUD de domiciliarios)
  private deliveryPersonService = inject(DeliveryPersonService);
  // Servicio para mostrar mensajes flotantes de éxito
  private toast = inject(ToastService);

  // Domiciliario al que se le está cambiando la contraseña.
  // Si es null, el modal de contraseña no se muestra (ver @if en el HTML).
  passwordDeliveryPerson: DeliveryPerson | null = null;

  // Función que recibe el modal de contraseña y que ejecuta el cambio.
  // Se define como arrow function (propiedad) para conservar el `this`
  // cuando el modal hijo la invoque. El "!" es seguro porque el modal
  // solo existe mientras passwordDeliveryPerson tiene valor.
  savePassword = (current: string, next: string) =>
    this.deliveryPersonService.changePassword(
      this.passwordDeliveryPerson!.id,
      current,
      next,
    );

  // Texto de búsqueda que llega desde el padre (barra de búsqueda del admin)
  @Input() searchTerm = '';

  // Se emite cuando el admin pulsa "Editar"; el padre abre el modal con ese domiciliario
  @Output() editDeliveryPerson = new EventEmitter<DeliveryPerson>();

  // Encabezados de la tabla, en el mismo orden de las celdas del HTML
  readonly columns = [
    'Domiciliario',
    'Identificación',
    'Teléfono',
    'Disponibilidad',
    'Estado',
    'Acciones',
  ];

  // Lista completa que viene del backend
  deliveryPersons: DeliveryPerson[] = [];
  // Lista que realmente se pinta: la completa filtrada por searchTerm
  filteredDeliveryPersons: DeliveryPerson[] = [];

  // Controlan la visibilidad de los modales de confirmación y de error
  showDeleteModal = false;
  showErrorModal = false;

  // Id del domiciliario que se va a eliminar (queda guardado mientras se confirma)
  selectedDeliveryPersonId: number | null = null;

  // Textos del modal de confirmación de eliminación
  confirmTitle = 'Eliminar domiciliario';
  confirmMessage = '¿Estás seguro de que deseas eliminar este domiciliario?';

  // Textos del modal de error; se rellenan dinámicamente en openErrorModal()
  errorTitle = '';
  errorMessage = '';

  constructor() {
    // Carga inicial de la lista al crear el componente
    this.loadDeliveryPersons();

    // Cada vez que otro componente llama a refresh(), se vuelve a cargar la lista del backend
    this.deliveryPersonService.refresh$.subscribe(() => {
      this.loadDeliveryPersons();
    });
  }

  // Trae los domiciliarios del backend y aplica el filtro actual
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

  // Se ejecuta cuando cambia algún @Input (aquí, searchTerm): vuelve a filtrar
  ngOnChanges() {
    this.filterDeliveryPersons();
  }

  // Busca por nombre, apellido, correo o identificación y actualiza filteredDeliveryPersons
  filterDeliveryPersons() {
    const value = this.searchTerm.trim().toLowerCase();

    this.filteredDeliveryPersons = this.deliveryPersons.filter((person) =>
      `${person.name} ${person.lastName} ${person.email} ${person.identification}`
        .toLowerCase()
        .includes(value),
    );
  }

  // Un domiciliario inactivo no aparece al enviar pedidos.
  // Tras cambiar el estado se pide un refresh para que la tabla se actualice.
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

  // Delega la edición en el componente padre
  openEdit(person: DeliveryPerson) {
    this.editDeliveryPerson.emit(person);
  }

  // Guarda el id y abre el modal de confirmación
  removeDeliveryPerson(person: DeliveryPerson) {
    this.selectedDeliveryPersonId = person.id;
    this.showDeleteModal = true;
  }

  // Cierra el modal y olvida el id seleccionado
  cancelDelete() {
    this.showDeleteModal = false;
    this.selectedDeliveryPersonId = null;
  }

  // El admin confirmó, se llama al backend
  confirmDelete() {
    // Protección: si por alguna razón no hay id seleccionado, no hace nada
    if (this.selectedDeliveryPersonId === null) {
      return;
    }

    // Se copia el id a una variable local porque justo después se limpia el estado
    const id = this.selectedDeliveryPersonId;

    this.showDeleteModal = false;
    this.selectedDeliveryPersonId = null;

    this.deliveryPersonService.deleteDeliveryPerson(id).subscribe({
      next: () => {
        this.deliveryPersonService.refresh();
        this.toast.show('Domiciliario eliminado correctamente');
      },
      error: (error) => {
        console.error('Error eliminando domiciliario:', error);
        // Se muestra el mensaje del backend si existe
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

  // Rellena y abre el modal de error
  private openErrorModal(title: string, message: string) {
    this.errorTitle = title;
    this.errorMessage = message;
    this.showErrorModal = true;
  }

  // Cierra el modal de error y limpia sus textos
  closeErrorModal() {
    this.showErrorModal = false;
    this.errorTitle = '';
    this.errorMessage = '';
  }
}
import {
  Component,
  inject,
  Output,
  EventEmitter,
  Input,
  OnChanges,
} from '@angular/core';
import { OperatorService } from '../../../../services/operator.service';
import { Operator } from '../../../../models/operator';
import { IconButtonComponent } from '../../../../components/icon-button/icon-button.component';
import { ModalComponent } from '../../../../components/modal/modal.component';
import { ChangePasswordModalComponent } from '../../../../components/change-password-modal/change-password-modal.component';
import { ToastService } from '../../../../services/toast.service';
import { getErrorMessage } from '../../../../utils/http-error';

// Lista de operadores del panel de administración: permite activar o desactivar, editar, cambiar contraseña y eliminar.
@Component({
  selector: 'app-admin-operators',
  imports: [IconButtonComponent, ModalComponent, ChangePasswordModalComponent],
  templateUrl: './admin-operators.component.html',
})
export class AdminOperatorsComponent implements OnChanges {
  // Servicio con las operaciones sobre operadores en el backend.
  private operatorService = inject(OperatorService);
  // Muestra avisos temporales de éxito.
  private toast = inject(ToastService);

  // Texto de búsqueda que llega desde el panel de administración para filtrar la lista.
  @Input() searchTerm = '';

  // Avisa al padre qué operador se quiere editar para que abra el modal de edición.
  @Output() editOperator = new EventEmitter<Operator>();

  // Operador al que se le cambia la contraseña; mientras sea null el modal de contraseña permanece cerrado.
  passwordOperator: Operator | null = null;

  // Todos los operadores traídos del backend.
  operators: Operator[] = [];
  // Operadores que coinciden con el texto de búsqueda; son los que se muestran.
  filteredOperators: Operator[] = [];

  // Controlan si se muestran el modal de confirmación de eliminación y el de error.
  showDeleteModal = false;
  showErrorModal = false;

  // Identificador del operador pendiente de eliminar; null si no hay ninguno.
  selectedOperatorId: number | null = null;

  // Textos del modal de confirmación de eliminación.
  confirmTitle = 'Eliminar operador';
  confirmMessage = '¿Estás seguro de que deseas eliminar este operador?';

  // Textos del modal de error; se completan cuando ocurre un fallo.
  errorTitle = '';
  errorMessage = '';

  // Carga la lista inicial y queda atento a las solicitudes de recarga.
  constructor() {
    this.loadOperators();

    // Recarga la lista cuando otro componente (por ejemplo el modal de edición) avisa de un cambio.
    this.operatorService.refresh$.subscribe(() => {
      this.loadOperators(); // ya no hace falta filterOperators() aquí
    });
  }

  // Consulta los operadores y aplica el filtro de búsqueda; ante un fallo abre el modal de error.
  loadOperators() {
    this.operatorService.getOperators().subscribe({
      next: (operators) => {
        this.operators = operators;
        this.filterOperators();
      },
      error: (error) => {
        console.error('Error cargando operadores:', error);
        this.openErrorModal(
          'Error al cargar operadores',
          getErrorMessage(error, 'No se pudieron cargar los operadores.'),
        );
      },
    });
  }

  // Delega en el padre la edición del operador seleccionado.
  openEdit(operator: Operator) {
    this.editOperator.emit(operator);
  }

  // Guarda el operador seleccionado y abre el modal de confirmación para eliminarlo.
  removeOperator(id: number) {
    this.selectedOperatorId = id;
    this.showDeleteModal = true;
  }

  // Cierra el modal de confirmación sin eliminar nada.
  cancelDelete() {
    this.showDeleteModal = false;
    this.selectedOperatorId = null;
  }

  // Elimina el operador seleccionado tras la confirmación del usuario.
  confirmDelete() {
    // Evita continuar si no hay un operador seleccionado.
    if (this.selectedOperatorId === null) {
      return;
    }

    // Se guarda el id y se limpia el estado antes de la petición, de modo que el modal se cierra de inmediato.
    const id = this.selectedOperatorId;

    this.showDeleteModal = false;
    this.selectedOperatorId = null;

    // Pide eliminar el operador; si sale bien recarga la lista y avisa, si falla muestra el modal de error.
    this.operatorService.deleteOperator(id).subscribe({
      next: () => {
        this.operatorService.refresh();
        this.toast.show('Operador eliminado correctamente');
      },
      error: (error) => {
        console.error('Error eliminando operador:', error);
        this.openErrorModal(
          'No se pudo eliminar el operador',
          getErrorMessage(
            error,
            'Ocurrió un error al eliminar el operador. Por favor, inténtalo de nuevo.',
          ),
        );
      },
    });
  }

  // Alterna el estado activo o inactivo del operador y recarga la lista para reflejarlo.
  toggleOperator(operator: Operator) {
    this.operatorService.toggleOperatorActiveStatus(operator.id).subscribe({
      next: () => {
        this.operatorService.refresh();
      },
      error: (error) => {
        console.error('Error cambiando estado del operador:', error);
        this.openErrorModal(
          'No se pudo cambiar el estado',
          getErrorMessage(error, 'No se pudo cambiar el estado del operador.'),
        );
      },
    });
  }

  // Configura el título y el mensaje del modal de error y lo muestra.
  private openErrorModal(title: string, message: string) {
    this.errorTitle = title;
    this.errorMessage = message;
    this.showErrorModal = true;
  }

  // Cierra el modal de error y limpia sus textos.
  closeErrorModal() {
    this.showErrorModal = false;
    this.errorTitle = '';
    this.errorMessage = '';
  }

  // Función que se entrega al modal de contraseña; envía al backend la contraseña actual y la nueva del operador seleccionado.
  savePassword = (current: string, next: string) =>
    this.operatorService.changePassword(
      this.passwordOperator!.id,
      current,
      next,
    );

  // Cuando cambia el texto de búsqueda se vuelve a filtrar la lista.
  ngOnChanges() {
    this.filterOperators();
  }

  // Filtra por nombre, usuario o correo, ignorando mayúsculas y espacios sobrantes.
  filterOperators() {
    const value = this.searchTerm.trim().toLowerCase();

    this.filteredOperators = this.operators.filter((operator) =>
      `${operator.name} ${operator.username} ${operator.email}`
        .toLowerCase()
        .includes(value),
    );
  }
}
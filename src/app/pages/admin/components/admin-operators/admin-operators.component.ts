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
import {
  ModalComponent,
  getErrorMessage,
} from '../../../../components/modal/modal.component';
import { ChangePasswordModalComponent } from '../../../../components/change-password-modal/change-password-modal.component';
import { ToastService } from '../../../../services/toast.service';

@Component({
  selector: 'app-admin-operators',
  imports: [IconButtonComponent, ModalComponent, ChangePasswordModalComponent],
  templateUrl: './admin-operators.component.html',
})
export class AdminOperatorsComponent implements OnChanges {
  private operatorService = inject(OperatorService);
  private toast = inject(ToastService);
  
  @Input() searchTerm = '';

  @Output() editOperator = new EventEmitter<Operator>();

  passwordOperator: Operator | null = null;

  operators: Operator[] = [];
  filteredOperators: Operator[] = [];

  showDeleteModal = false;
  showErrorModal = false;

  selectedOperatorId: number | null = null;

  confirmTitle = 'Eliminar operador';
  confirmMessage = '¿Estás seguro de que deseas eliminar este operador?';

  errorTitle = '';
  errorMessage = '';

  constructor() {
    this.loadOperators();

    this.operatorService.refresh$.subscribe(() => {
      this.loadOperators(); // ya no hace falta filterOperators() aquí
    });
  }

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

  openEdit(operator: Operator) {
    this.editOperator.emit(operator);
  }

  removeOperator(id: number) {
    this.selectedOperatorId = id;
    this.showDeleteModal = true;
  }

  cancelDelete() {
    this.showDeleteModal = false;
    this.selectedOperatorId = null;
  }

  confirmDelete() {
    if (this.selectedOperatorId === null) {
      return;
    }

    const id = this.selectedOperatorId;

    this.showDeleteModal = false;
    this.selectedOperatorId = null;

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

  savePassword = (current: string, next: string) =>
    this.operatorService.changePassword(
      this.passwordOperator!.id,
      current,
      next,
    );

  ngOnChanges() {
    this.filterOperators();
  }

  filterOperators() {
    const value = this.searchTerm.trim().toLowerCase();

    this.filteredOperators = this.operators.filter((operator) =>
      `${operator.name} ${operator.username} ${operator.email}`
        .toLowerCase()
        .includes(value),
    );
  }
}

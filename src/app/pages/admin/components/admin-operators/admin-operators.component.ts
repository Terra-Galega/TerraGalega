
import { Component, inject, Output, EventEmitter } from '@angular/core';
import { OperatorService } from '../../../../services/operator.service';
import { Operator } from '../../../../models/operator';
import { IconButtonComponent } from '../../../../components/icon-button/icon-button.component';
import { ModalComponent, getErrorMessage } from '../../../../components/modal/modal.component';

@Component({
  selector: 'app-admin-operators',
  imports: [IconButtonComponent, ModalComponent],
  templateUrl: './admin-operators.component.html',
})
export class AdminOperatorsComponent {
  private operatorService = inject(OperatorService);

  @Output() editOperator = new EventEmitter<Operator>();

  operators: Operator[] = [];

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
      this.loadOperators();
    });
  }

  loadOperators() {
    this.operatorService.getOperators().subscribe({
      next: (operators) => {
        this.operators = operators;
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
}

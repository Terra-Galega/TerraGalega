import { Component, inject, Output, EventEmitter } from '@angular/core';
import { OperatorService } from '../../../../services/operator.service';
import { Operator } from '../../../../models/operator';
import { IconButtonComponent } from '../../../../components/icon-button/icon-button.component';

@Component({
  selector: 'app-admin-operators',
  imports: [IconButtonComponent],
  templateUrl: './admin-operators.component.html',
  styleUrl: './admin-operators.component.scss',
})
export class AdminOperatorsComponent {
  private operatorService = inject(OperatorService);

  @Output() editOperator = new EventEmitter<Operator>();

  operators: Operator[] = [];

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
      },
    });
  }

  openEdit(operator: Operator) {
    this.editOperator.emit(operator);
  }

  removeOperator(id: number) {
    const confirmation = window.confirm(
      '¿Estás seguro de que deseas eliminar este operador?',
    );
    if (!confirmation) {
      return;
    }

    this.operatorService.deleteOperator(id).subscribe({
      next: () => {
        this.loadOperators();
      },
      error: (error) => {
        console.error('Error eliminando operador:', error);
        window.alert(
          'Ocurrió un error al eliminar el operador. Por favor, inténtalo de nuevo.',
        );
      },
    });
  }
}

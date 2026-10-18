import { Component, inject } from '@angular/core';
import { OperatorService } from '../../../../services/operator.service';
import { Operator } from '../../../../models/operator';

@Component({
  selector: 'app-admin-operators',
  imports: [],
  templateUrl: './admin-operators.component.html',
  styleUrl: './admin-operators.component.scss'
})
export class AdminOperatorsComponent {
private operatorService = inject(OperatorService);

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
}

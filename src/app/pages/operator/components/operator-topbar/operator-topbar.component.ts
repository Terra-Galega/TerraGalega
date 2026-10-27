import { Component, EventEmitter, Input, Output } from '@angular/core';
import { ButtonComponent } from '../../../../components/button/button.component';

@Component({
  selector: 'app-operator-topbar',
  imports: [ButtonComponent],
  templateUrl: './operator-topbar.component.html',
})
export class OperatorTopbarComponent {
  // Recibe del componente padre el nombre que se muestra en la barra superior.
  @Input() operatorName = '';

  // Recibe el nombre de usuario para mostrarlo junto al nombre completo.
  @Input() operatorUsername = '';

  // Notifica al componente padre que se solicitó cerrar sesión, delegándole la lógica correspondiente.
  @Output() logoutClicked = new EventEmitter<void>();
}

import { Component, EventEmitter, Input, Output } from '@angular/core';
import { SettingsMenuComponent } from '../../../../components/settings-menu/settings-menu.component';

@Component({
  selector: 'app-operator-topbar',
  imports: [SettingsMenuComponent],
  styleUrl: './operator-topbar.component.scss',
  templateUrl: './operator-topbar.component.html',
})
export class OperatorTopbarComponent {
  // Recibe del componente padre el nombre que se muestra en la barra superior.
  @Input() operatorName = '';

  // Recibe el nombre de usuario para mostrarlo junto al nombre completo.
  @Input() operatorUsername = '';

  // Nombre del portal en la barra (el de cocina reutiliza esta misma barra)
  @Input() panelLabel = 'PANEL OPERADOR';

  // Notifica al componente padre que se solicitó cerrar sesión, delegándole la lógica correspondiente.
  @Output() logoutClicked = new EventEmitter<void>();
}

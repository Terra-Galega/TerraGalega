import { TranslatePipe } from '../../../../pipes/translate.pipe';
import { Component, EventEmitter, Input, Output } from '@angular/core';

@Component({
  selector: 'app-login-header',
  imports: [TranslatePipe],
  templateUrl: './login-header.component.html',
  styleUrl: './login-header.component.scss',
})
export class LoginHeaderComponent {
  @Input() activeTab: 'login' | 'signup' = 'login';
  // Clave de traducción con el motivo ("para terminar tu pedido"...). Vacía = sin aviso.
  @Input() reasonKey = '';
  //eventos para cambiar de "pestaña"
  @Output() tabChange = new EventEmitter<'login' | 'signup'>();

  selectTab(tab: 'login' | 'signup') {
    this.tabChange.emit(tab);
  }
}

import { Component, EventEmitter, Input, Output } from '@angular/core';
import { ButtonComponent } from '../../../../components/button/button.component';

@Component({
  selector: 'app-operator-topbar',
  imports: [ButtonComponent],
  templateUrl: './operator-topbar.component.html',
})
export class OperatorTopbarComponent {
  @Input() operatorName = '';
  @Input() operatorUsername = '';
  @Output() logoutClicked = new EventEmitter<void>();
}
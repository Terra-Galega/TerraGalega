import { Component, EventEmitter, Output } from '@angular/core';
import { ButtonComponent } from '../../../../components/button/button.component';

@Component({
  selector: 'app-account-actions',
  imports: [ButtonComponent],
  templateUrl: './account-actions.component.html',
  host: { class: 'block h-full' },
})
export class AccountActionsComponent {
  @Output() logout = new EventEmitter<void>();
}

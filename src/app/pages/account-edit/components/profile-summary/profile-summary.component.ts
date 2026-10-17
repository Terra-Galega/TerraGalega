import { Component, Input } from '@angular/core';
import { UserAvatarComponent } from '../../../../components/user-avatar/user-avatar.component';

// Tarjeta lateral que se actualiza mientras el usuario escribe en el formulario.
@Component({
  selector: 'app-profile-summary',
  imports: [UserAvatarComponent],
  templateUrl: './profile-summary.component.html',
  host: { class: 'block' },
})
export class ProfileSummaryComponent {
  @Input() name = '';
  @Input() lastName = '';
  @Input() email = '';
}

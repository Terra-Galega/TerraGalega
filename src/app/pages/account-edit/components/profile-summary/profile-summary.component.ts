import { Component, Input } from '@angular/core';
import { UserAvatarComponent } from '../../../../components/user-avatar/user-avatar.component';

// Tarjeta lateral que muestra un resumen del perfil y se actualiza mientras el usuario escribe en el formulario.
@Component({
  selector: 'app-profile-summary',
  // Componente utilizado para mostrar el avatar del usuario
  imports: [UserAvatarComponent],
  templateUrl: './profile-summary.component.html',
  host: { class: 'block' },
})
export class ProfileSummaryComponent {
  // Datos recibidos del componente padre para mostrar la información del perfil
  @Input() name = '';
  @Input() lastName = '';
  @Input() email = '';
}

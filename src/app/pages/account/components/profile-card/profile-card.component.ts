import { Component, Input } from '@angular/core';
import { Client } from '../../../../models/client';
import { UserAvatarComponent } from '../../../../components/user-avatar/user-avatar.component';

// Encabezado del perfil: banner, avatar con iniciales, nombre y correo.
@Component({
  selector: 'app-profile-card',
  imports: [UserAvatarComponent],
  templateUrl: './profile-card.component.html',
  host: { class: 'block' },
})
export class ProfileCardComponent {
  @Input({ required: true }) client!: Client;
}

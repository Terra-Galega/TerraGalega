import { Component, inject } from '@angular/core';
import { CelebrationService } from '../../../services/celebration.service';

@Component({
  selector: 'app-celebration-avatar-decoration',
  templateUrl: './celebration-avatar-decoration.component.html',
  styleUrl: './celebration-avatar-decoration.component.scss',
})
export class CelebrationAvatarDecorationComponent {
  protected readonly celebration = inject(CelebrationService).current;
}

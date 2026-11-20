import { Component, inject } from '@angular/core';
import { CelebrationService } from '../../../services/celebration.service';
import { ThemeService } from '../../../services/theme.service';
import { CelebrationCanvasComponent } from '../celebration-canvas/celebration-canvas.component';

// Lluvia decorativa a lo largo de la página de cuenta, solo en fecha festiva.
@Component({
  selector: 'app-celebration-rain',
  imports: [CelebrationCanvasComponent],
  templateUrl: './celebration-rain.component.html',
  styleUrl: './celebration-rain.component.scss',
})
export class CelebrationRainComponent {
  private readonly celebrations = inject(CelebrationService);
  readonly celebration = this.celebrations.current;
  readonly forceMotion = this.celebrations.forceMotion;
  readonly theme = inject(ThemeService).theme;
}
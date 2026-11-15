import { Component, inject } from '@angular/core';
import { CelebrationService } from '../celebration.service';
import { CelebrationCanvasComponent } from '../celebration-canvas/celebration-canvas.component';
import { CelebrationIconComponent } from '../celebration-icon/celebration-icon.component';

// Tarjeta pequeña de la página de la cuenta: "Hoy celebramos ...".
// No muestra nada si hoy no hay festividad.
@Component({
  selector: 'app-celebration-banner',
  imports: [CelebrationCanvasComponent, CelebrationIconComponent],
  templateUrl: './celebration-banner.component.html',
  styleUrl: './celebration-banner.component.scss',
})
export class CelebrationBannerComponent {
  private celebrations = inject(CelebrationService);

  celebration = this.celebrations.current;
  forceMotion = this.celebrations.forceMotion;
}

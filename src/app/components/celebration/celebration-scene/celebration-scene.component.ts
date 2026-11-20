import { Component, inject } from '@angular/core';
import { CelebrationService } from '../../../services/celebration.service';
import { CelebrationCanvasComponent } from '../celebration-canvas/celebration-canvas.component';
import { CelebrationIconComponent } from '../celebration-icon/celebration-icon.component';

// Decoración de la portada (hero) del home: brillo, íconos flotando, placa con el nombre
// de la festividad y figuras cayendo. No muestra nada si hoy no hay festividad.
@Component({
  selector: 'app-celebration-scene',
  imports: [CelebrationCanvasComponent, CelebrationIconComponent],
  templateUrl: './celebration-scene.component.html',
  styleUrl: './celebration-scene.component.scss',
})
export class CelebrationSceneComponent {
  private celebrations = inject(CelebrationService);

  celebration = this.celebrations.current;
  forceMotion = this.celebrations.forceMotion;
}

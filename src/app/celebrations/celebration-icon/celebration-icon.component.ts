import { Component, Input } from '@angular/core';
import { CELEBRATION_ICONS, IconPart } from '../celebration-icons';
import { CelebrationShape } from '../celebrations.data';

// Ícono SVG de la forma de una festividad. Toma el color del texto (currentColor).
@Component({
  selector: 'app-celebration-icon',
  templateUrl: './celebration-icon.component.html',
  styleUrl: './celebration-icon.component.scss',
})
export class CelebrationIconComponent {
  @Input() set shape(value: CelebrationShape) {
    this.parts = CELEBRATION_ICONS[value];
  }

  parts: IconPart[] = [];
}

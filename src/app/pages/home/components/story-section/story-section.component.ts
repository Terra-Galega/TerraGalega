import { TranslatePipe } from '../../../../pipes/translate.pipe';
import { Component } from '@angular/core';
import { RevealDirective } from '../../../../directives/reveal.directive';

// Sección Nuestra Historia del home: texto y cifras a un lado e imagen al otro.
@Component({
  selector: 'app-story-section',
  imports: [RevealDirective, TranslatePipe],
  templateUrl: './story-section.component.html',
  styleUrl: './story-section.component.scss',
})
export class StorySectionComponent {
  // Cifras del restaurante; value es el número mostrado y label su descripción.
  stats = [
    { value: '6+', label: 'Años' },
    { value: '150+', label: 'Platos/día' },
    { value: '4.9★', label: 'Valoración' },
  ];
}

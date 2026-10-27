import { Component } from '@angular/core';
import { ButtonComponent } from '../../../../components/button/button.component';

// Sección de portada (hero) del home: contenido estático, sin lógica propia.
@Component({
  selector: 'app-hero-section',
  // ButtonComponent se usa para los botones Explorar Menú y Nuestra Historia.
  imports: [ButtonComponent],
  templateUrl: './hero-section.component.html',
  styleUrl: './hero-section.component.scss',
})
export class HeroSectionComponent {}
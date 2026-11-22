import { TranslatePipe } from '../../../../pipes/translate.pipe';
import { Component } from '@angular/core';

@Component({
  imports: [TranslatePipe],
  selector: 'app-about-hero-section',
  templateUrl: './about-hero-section.component.html',
  styleUrl: './about-hero-section.component.scss',
})
export class AboutHeroSectionComponent {}

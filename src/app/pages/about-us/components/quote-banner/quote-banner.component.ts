import { Component, Input } from '@angular/core';

// Frase destacada sobre una imagen de fondo oscurecida, con comillas doradas.
@Component({
  selector: 'app-quote-banner',
  templateUrl: './quote-banner.component.html',
  styleUrl: './quote-banner.component.scss',
})
export class QuoteBannerComponent {
  @Input({ required: true }) quote = '';
  @Input({ required: true }) image = '';
  @Input() imageAlt = '';
}

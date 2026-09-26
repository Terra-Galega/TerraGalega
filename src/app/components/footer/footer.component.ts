import { Component } from '@angular/core';
import { RouterLink } from '@angular/router';

@Component({
selector: 'app-footer',
standalone: true,
imports: [
RouterLink
],
templateUrl: './footer.component.html',
styleUrl: './footer.component.scss'
})
export class FooterComponent {

/**

* Año actual para el copyright.
*
* En develop estaba escrito directamente:
*
* © 2026 Terra Galega
  */
  readonly currentYear = new Date().getFullYear();

}

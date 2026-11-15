import { Injectable, signal } from '@angular/core';

// Coordina el logo grande del hero del home con el logo de la navbar.
// Mientras el logo del hero está a la vista (o subiendo hacia la barra), la navbar
// oculta el suyo; cuando llega a su sitio, la navbar lo muestra y pasa a modo vidrio.
@Injectable({ providedIn: 'root' })
export class HeroLogoService {
  // true: el logo está en la navbar (fuera del home siempre es así)
  readonly docked = signal(true);
}

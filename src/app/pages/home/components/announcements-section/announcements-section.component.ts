import { TranslatePipe } from '../../../../services/translate.pipe';
import { Component, Input, OnChanges } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { timer } from 'rxjs';
import { Announcement } from '../../../../models/announcement';
import { SectionHeadingComponent } from '../../../../components/section-heading/section-heading.component';
import { RevealDirective } from '../../../../directives/reveal.directive';

// Cada cuánto avanza solo el carrusel (en milisegundos)
const AUTOPLAY_MS = 6_000;

/**
 * Carrusel horizontal de anuncios del home (máximo 3). Muestra uno a la vez,
 * avanza solo cada AUTOPLAY_MS, se detiene mientras el mouse está encima (o si el
 * usuario pulsa pausa) y se puede mover con las flechas, los puntos o deslizando el dedo.
 * Si el sistema pide reducir el movimiento, no avanza solo.
 */
@Component({
  selector: 'app-announcements-section',
  imports: [SectionHeadingComponent, RevealDirective, TranslatePipe],
  templateUrl: './announcements-section.component.html',
})
export class AnnouncementsSectionComponent implements OnChanges {
  // Anuncios que llegan del home
  @Input() announcements: Announcement[] = [];

  // Posición del anuncio visible
  current = 0;
  // true mientras el mouse (o el foco) está sobre el carrusel
  paused = false;
  // true si el usuario pulsó el botón de pausa
  userPaused = false;

  // Si el sistema pide menos movimiento, el carrusel solo cambia con las flechas o puntos
  private reducedMotion =
    typeof matchMedia !== 'undefined' &&
    matchMedia('(prefers-reduced-motion: reduce)').matches;

  // Posición horizontal donde empezó el toque (para detectar el deslizamiento)
  private touchStartX = 0;

  constructor() {
    // Avance automático; se cancela solo cuando el componente se destruye
    timer(AUTOPLAY_MS, AUTOPLAY_MS)
      .pipe(takeUntilDestroyed())
      .subscribe(() => {
        if (
          !this.paused &&
          !this.userPaused &&
          !this.reducedMotion &&
          this.announcements.length > 1
        ) {
          this.next();
        }
      });
  }

  // Si el admin borra anuncios y la posición queda fuera de rango, se reinicia
  ngOnChanges() {
    if (this.current >= this.announcements.length) {
      this.current = 0;
    }
  }

  // Anuncio siguiente (al final vuelve al primero)
  next() {
    this.current = (this.current + 1) % this.announcements.length;
  }

  // Anuncio anterior (al inicio salta al último)
  prev() {
    this.current =
      (this.current - 1 + this.announcements.length) % this.announcements.length;
  }

  // Deslizar el dedo: a la izquierda avanza, a la derecha retrocede (mínimo 50 px)
  onTouchStart(event: TouchEvent) {
    this.touchStartX = event.changedTouches[0].clientX;
  }

  onTouchEnd(event: TouchEvent) {
    if (this.announcements.length < 2) {
      return;
    }
    const delta = event.changedTouches[0].clientX - this.touchStartX;
    if (Math.abs(delta) < 50) {
      return;
    }
    if (delta < 0) {
      this.next();
    } else {
      this.prev();
    }
  }

  // Salta directo a un anuncio (puntos de abajo)
  goTo(index: number) {
    this.current = index;
  }
}

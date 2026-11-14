import { Component, Input, OnChanges } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { timer } from 'rxjs';
import { Announcement } from '../../../../models/announcement';

// Cada cuánto avanza solo el carrusel (en milisegundos)
const AUTOPLAY_MS = 6_000;

/**
 * Carrusel horizontal de anuncios del home (máximo 3). Muestra uno a la vez,
 * avanza solo cada AUTOPLAY_MS, se detiene mientras el mouse está encima y
 * se puede mover con las flechas o los puntos.
 */
@Component({
    selector: 'app-announcements-section',
    templateUrl: './announcements-section.component.html',
})
export class AnnouncementsSectionComponent implements OnChanges {
    // Anuncios que llegan del home
    @Input() announcements: Announcement[] = [];

    // Posición del anuncio visible
    current = 0;
    // true mientras el mouse (o el foco) está sobre el carrusel
    paused = false;

    constructor() {
        // Avance automático; se cancela solo cuando el componente se destruye
        timer(AUTOPLAY_MS, AUTOPLAY_MS)
            .pipe(takeUntilDestroyed())
            .subscribe(() => {
                if (!this.paused && this.announcements.length > 1) {
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

    // Salta directo a un anuncio (puntos de abajo)
    goTo(index: number) {
        this.current = index;
    }
}
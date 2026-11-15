import { Component, inject, input, output } from '@angular/core';
import { TourName, TutorialService } from '../../services/Tutorial.service';

// Botón flotante que muestra los tutoriales disponibles en la pantalla.
@Component({
    selector: 'app-tutorial-fab',
    templateUrl: './tutorial-fab.component.html',
})
export class TutorialFabComponent {
    private tutorial = inject(TutorialService);
    readonly adminPanel = input(false);
    readonly adminTourRequested = output<void>();

    // true mientras se ve la lista de tutoriales
    open = false;

    toggle(): void {
        this.open = !this.open;
    }

    // Cierra la lista y arranca el tutorial elegido
    startTour(name: TourName): void {
        this.open = false;
        this.tutorial.start(name);
    }

    startAdminTour(): void {
        this.open = false;
        this.adminTourRequested.emit();
    }
}

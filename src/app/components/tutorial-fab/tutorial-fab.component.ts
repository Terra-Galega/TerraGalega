import { Component, inject } from '@angular/core';
import { TourName, TutorialService } from '../../services/Tutorial.service';

// Botón flotante a la derecha: al tocarlo muestra los tutoriales disponibles
// ("hacer un pedido" y "hacer una reserva") y arranca el que elija el cliente
@Component({
    selector: 'app-tutorial-fab',
    templateUrl: './tutorial-fab.component.html',
})
export class TutorialFabComponent {
    private tutorial = inject(TutorialService);

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
}
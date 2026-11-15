import { Component, inject } from '@angular/core';
import { TutorialService } from '../../services/Tutorial.service';


// Botón flotante a la derecha que inicia el tutorial de "hacer un pedido"
@Component({
  selector: 'app-tutorial-fab',
  templateUrl: './tutorial-fab.component.html',
})
export class TutorialFabComponent {
  private tutorial = inject(TutorialService);

  startOrderTour(): void {
    this.tutorial.start('order');
  }
}
import { Component, EventEmitter, Input, Output } from '@angular/core';
import { Client } from '../../../../models/client';
import { InfoTileComponent } from '../info-tile/info-tile.component';

@Component({
  selector: 'app-personal-info',
  imports: [InfoTileComponent],
  templateUrl: './personal-info.component.html',
  host: { class: 'block h-full' },
})
export class PersonalInfoComponent {
  @Input({ required: true }) client!: Client;
  //Evento para mandar a editar
  @Output() edit = new EventEmitter<void>();
}

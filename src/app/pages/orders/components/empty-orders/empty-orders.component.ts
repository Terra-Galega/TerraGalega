import { Component} from '@angular/core';
import {RouterLink} from "@angular/router";
import { ButtonComponent } from '../../../../components/button/button.component';

// Estado vacío: el cliente aún no ha hecho pedidos. Invita a explorar la carta.
@Component({
  selector: 'app-empty-orders',
  imports: [RouterLink, ButtonComponent],
  templateUrl: './empty-orders.component.html',
  styleUrl: './empty-orders.component.scss'
})
export class EmptyOrdersComponent {
}

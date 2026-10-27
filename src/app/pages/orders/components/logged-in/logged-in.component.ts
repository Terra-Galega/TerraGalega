import { Component, Input } from '@angular/core';
import { RouterLink } from '@angular/router';
import { ButtonComponent } from '../../../../components/button/button.component';
import { Order } from '../../../../models/order';
// Encabezado de "Mis pedidos" para clientes con sesión
@Component({
  selector: 'app-logged-in',
  imports: [RouterLink, ButtonComponent],
  templateUrl: './logged-in.component.html',
  styleUrl: './logged-in.component.scss'
})
export class LoggedInComponent {

  // Pedidos del cliente; solo se usa para saber si hay al menos uno
  @Input() orders: Order[] = [];

}

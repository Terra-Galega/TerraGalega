import { Component } from '@angular/core';
import { NavbarComponent } from '../../components/navbar/navbar.component';
import { FooterComponent } from '../../components/footer/footer.component';
import { RouterLink } from '@angular/router';
import { OrderStepsComponent } from '../../components/order-steps/order-steps.component';
import { ReadyToOrderComponent } from '../../components/ready-to-order/ready-to-order.component';

@Component({
  selector: 'app-orders',
  imports: [
    RouterLink,
    NavbarComponent,
    FooterComponent,
    OrderStepsComponent,
    ReadyToOrderComponent
  ],
  templateUrl: './orders.component.html',
  styleUrl: './orders.component.scss'
})
export class OrdersComponent {

  // Después esto vendrá del estado de autenticación
  isLoggedIn = false;
}
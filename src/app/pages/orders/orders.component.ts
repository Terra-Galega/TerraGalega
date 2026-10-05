import { Component } from '@angular/core';
import { NavbarComponent } from '../../components/navbar/navbar.component';
import { FooterComponent } from '../../components/footer/footer.component';
import { LoggedOutComponent } from './components/logged-out/logged-out.component';
import { LoggedInComponent } from './components/logged-in/logged-in.component';
import { OrderStepsComponent } from '../../components/order-steps/order-steps.component';
import { ReadyToOrderComponent } from '../../components/ready-to-order/ready-to-order.component';
import { EmptyOrdersComponent } from './components/empty-orders/empty-orders.component';

@Component({
  selector: 'app-orders',
  imports: [
    NavbarComponent,
    FooterComponent,
    OrderStepsComponent,
    LoggedOutComponent,
    LoggedInComponent,
    ReadyToOrderComponent,
    EmptyOrdersComponent,
  ],
  templateUrl: './orders.component.html',
})
export class OrdersComponent {
  // Después esto vendrá del estado de autenticación
  isLoggedIn = true;
}

import { Component, OnInit, inject } from '@angular/core';
import { NavbarComponent } from '../../components/navbar/navbar.component';
import { FooterComponent } from '../../components/footer/footer.component';
import { LoggedOutComponent } from './components/logged-out/logged-out.component';
import { LoggedInComponent } from './components/logged-in/logged-in.component';
import { OrderStepsComponent } from '../../components/order-steps/order-steps.component';
import { ReadyToOrderComponent } from '../../components/ready-to-order/ready-to-order.component';
import { EmptyOrdersComponent } from './components/empty-orders/empty-orders.component';
import { OrderService } from '../../services/order.service';
import { Order } from '../../models/order';
import { OrderedComponent } from './components/ordered/ordered.component';

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
    OrderedComponent
],
  templateUrl: './orders.component.html',
})
export class OrdersComponent implements OnInit {
  private orderService = inject(OrderService);

  isLoggedIn: boolean = false;
  loading: boolean = true;
  orders: Order[] = [];

  ngOnInit() {
    const user = localStorage.getItem('user');
    const type = localStorage.getItem('type');
    if (user && type !== 'client') {
      this.isLoggedIn = false;
      return;
    }

    this.isLoggedIn = true;
    const clientId = JSON.parse(user!).id;

    this.orderService.getOrdersByClient(clientId).subscribe({
      next: (orders) => {
        this.orders = orders;
        this.loading = false;
      },
      error: (error) => {
        console.error('Error cargando pedidos:', error);
        this.loading = false;
      },
    });
  }
}

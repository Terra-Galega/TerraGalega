import { Component, OnInit, inject } from '@angular/core';
import { ActivatedRoute, Router } from '@angular/router';
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
import { AuthService } from '../../services/auth.service';

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
    OrderedComponent,
  ],
  templateUrl: './orders.component.html',
})

export class OrdersComponent implements OnInit {
  private auth = inject(AuthService);
  private orderService = inject(OrderService);
  private route = inject(ActivatedRoute);
  private router = inject(Router);

  paidSuccess = false;
  isLoggedIn = false;
  loading = true;
  orders: Order[] = [];

  ngOnInit(): void {
    if (this.route.snapshot.queryParamMap.get('paid') === 'true') {
      this.paidSuccess = true;

      this.router.navigate([], {
        relativeTo: this.route,
        replaceUrl: true,
        queryParams: {},
      });
    }

    const session = this.auth.session;

    if (!session || session.type !== 'client') {
      this.loading = false;
      return;
    }

    this.isLoggedIn = true;

    this.orderService.getOrdersByClient(session.id).subscribe({
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
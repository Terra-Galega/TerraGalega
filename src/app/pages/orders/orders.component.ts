import { Component, DestroyRef, OnInit, inject } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { ActivatedRoute, Router } from '@angular/router';
import {
  EMPTY,
  Subject,
  catchError,
  filter,
  fromEvent,
  merge,
  switchMap,
  timer,
} from 'rxjs';
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
import { getErrorMessage } from '../../utils/http-error';

// Cada cuánto se vuelven a pedir los pedidos al servidor (en milisegundos)
const REFRESH_MS = 10_000;
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
  private destroyRef = inject(DestroyRef);

  // Pide la lista de inmediato (por ejemplo, después de cancelar un pedido)
  private reload$ = new Subject<void>();

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
    this.watchOrders(session.id);
  }

  // Carga los pedidos al abrir, los actualiza cada REFRESH_MS, al volver a la
  // pestaña y cuando se pide recargar. Así, cuando el operador cambia el estado
  // de un pedido, se ve sin recargar la página.
  private watchOrders(clientId: number): void {
    merge(
      timer(0, REFRESH_MS),
      fromEvent(document, 'visibilitychange'),
      this.reload$,
    )
      .pipe(
        filter(() => document.visibilityState === 'visible'),
        switchMap(() =>
          this.orderService.getOrdersByClient(clientId).pipe(
            catchError((error) => {
              this.handleLoadError(error);
              return EMPTY; // se queda la lista que ya se veía
            }),
          ),
        ),
        takeUntilDestroyed(this.destroyRef),
      )
      .subscribe((orders) => {
        this.orders = orders;
        this.loading = false;
      });
  }

  private handleLoadError(error: any): void {
    console.error('Error cargando pedidos:', error);

    // Si ya había pedidos en pantalla, el fallo (por ejemplo, sin conexión)
    // se ignora y se reintenta en el siguiente ciclo
    if (!this.loading) {
      return;
    }

    this.loading = false;
    this.router.navigate(['/error'], {
      replaceUrl: true,
      queryParams: {
        status: error.status,
        message: getErrorMessage(error, 'No se pudieron cargar tus pedidos.'),
      },
    });
  }

  onOrderCancelled() {
    this.reload$.next();
  }

  loadOrders(clientId: number) {
    this.orderService.getOrdersByClient(clientId).subscribe({
      next: (orders) => {
        this.orders = orders;
        this.loading = false;
      },
      error: (error) => {
        console.error('Error cargando pedidos:', error);
        this.loading = false;
        this.router.navigate(['/error'], {
          replaceUrl: true,
          queryParams: {
            status: error.status,
            message: getErrorMessage(
              error,
              'No se pudieron cargar tus pedidos.',
            ),
          },
        });
      },
    });
  }

  // Un pedido está activo mientras no se haya entregado ni cancelado
  get activeOrders(): Order[] {
    return this.orders.filter(
      (order) => order.status !== 'DELIVERED' && order.status !== 'CANCELLED',
    );
  }

  // Pedidos ya terminados (entregados o cancelados)
  get pastOrders(): Order[] {
    return this.orders.filter(
      (order) => order.status === 'DELIVERED' || order.status === 'CANCELLED',
    );
  }
}

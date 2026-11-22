import { Injectable, inject } from '@angular/core';
import { Observable, catchError, forkJoin, map, of } from 'rxjs';
import { OrderService } from './order.service';
import { ProductService } from './product.service';
import { TableOrderService } from './table-order.service';
import { DashboardModel, buildModel } from '../utils/dashboard-metrics';

// Reúne los tres orígenes de datos que necesita el dashboard del admin:
//  - pedidos completos (platos, cantidades, adicionales, total)
//  - resúmenes de pedidos (traen el nombre del cliente)
//  - productos (traen la categoría y el precio final)
// y los une en un único modelo con las ganancias ya calculadas.
@Injectable({ providedIn: 'root' })
export class DashboardService {
  private orderService = inject(OrderService);
  private productService = inject(ProductService);
  private tableOrderService = inject(TableOrderService);

  load(): Observable<DashboardModel> {
    return forkJoin({
      orders: this.orderService.getAllOrders(),
      summaries: this.orderService.getAllOrderSummaries(),
      products: this.productService.getProducts(),
      // Órdenes en punto físico (meseros). Si fallan, el dashboard sigue con los domicilios.
      tableOrders: this.tableOrderService.adminOrders().pipe(
        catchError((error) => {
          console.error('Error cargando órdenes en punto físico:', error);
          return of([]);
        }),
      ),
    }).pipe(
      map(({ orders, summaries, products, tableOrders }) =>
        buildModel(orders, summaries, products, tableOrders),
      ),
    );
  }
}
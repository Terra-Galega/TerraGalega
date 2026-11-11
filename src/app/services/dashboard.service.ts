import { Injectable, inject } from '@angular/core';
import { Observable, forkJoin, map } from 'rxjs';
import { OrderService } from './order.service';
import { ProductService } from './product.service';
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

  load(): Observable<DashboardModel> {
    return forkJoin({
      orders: this.orderService.getAllOrders(),
      summaries: this.orderService.getAllOrderSummaries(),
      products: this.productService.getProducts(),
    }).pipe(
      map(({ orders, summaries, products }) =>
        buildModel(orders, summaries, products),
      ),
    );
  }
}
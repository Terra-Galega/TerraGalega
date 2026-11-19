import { Component, DestroyRef, OnInit, inject } from '@angular/core';
import { DatePipe } from '@angular/common';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { EMPTY, catchError, exhaustMap, filter, merge, fromEvent, timer } from 'rxjs';

import { KitchenTable, OrderItemStatus, TableOrder, TableOrderItem } from '../../../../models/table-order';
import { TableOrderService } from '../../../../services/table-order.service';
import { getErrorMessage } from '../../../../utils/http-error';

// Cada cuánto se consulta la cocina (el resto del panel usa el mismo mecanismo)
const REFRESH_MS = 3000;

// Tablero de la cocina (portal /kitchen): pedidos de mesa que mandan los meseros desde la app.
// Se actualiza sola cada 3 s (y al volver a la pestaña). La cocina avanza cada
// plato: En espera -> En preparación -> Listo. Cuando todo está listo, el pedido
// queda "Listo, esperando al mesero" hasta que él lo entregue desde la app.
@Component({
  selector: 'app-kitchen-board',
  imports: [DatePipe],
  templateUrl: './kitchen-board.component.html',
})
export class KitchenBoardComponent implements OnInit {
  private service = inject(TableOrderService);
  private destroyRef = inject(DestroyRef);

  tables: KitchenTable[] = [];
  loading = true;
  errorMessage = '';
  checkedAt: Date | null = null;

  // Platos con un cambio en curso (evita dobles clics)
  private busy = new Set<number>();
  // Pedidos con un cambio de "todo el pedido" en curso
  private busyOrders = new Set<number>();

  ngOnInit(): void {
    merge(timer(0, REFRESH_MS), fromEvent(document, 'visibilitychange'))
      .pipe(
        filter(() => document.visibilityState === 'visible'),
        // Si una respuesta tarda, no se acumulan peticiones
        exhaustMap(() =>
          this.service.kitchen().pipe(
            catchError((error) => {
              this.errorMessage = getErrorMessage(error, 'No se pudo actualizar la cocina.');
              this.loading = false;
              return EMPTY;
            }),
          ),
        ),
        takeUntilDestroyed(this.destroyRef),
      )
      .subscribe((tables) => {
        this.tables = tables;
        this.loading = false;
        this.errorMessage = '';
        this.checkedAt = new Date();
      });
  }

  get orderCount(): number {
    return this.tables.reduce((n, t) => n + t.orders.length, 0);
  }

  // Minutos desde que llegó el pedido a cocina
  minutesSince(startTime: string): number {
    return Math.max(0, Math.floor((Date.now() - new Date(startTime).getTime()) / 60000));
  }

  isBusy(item: TableOrderItem): boolean {
    return this.busy.has(item.id);
  }

  // Siguiente estado del plato (null si ya está listo)
  nextStatus(item: TableOrderItem): OrderItemStatus | null {
    return item.status === 'PENDING' ? 'PREPARING' : item.status === 'PREPARING' ? 'READY' : null;
  }

  advance(order: TableOrder, item: TableOrderItem): void {
    const next = this.nextStatus(item);
    if (!next || this.busy.has(item.id) || this.busyOrders.has(order.id)) return;

    this.busy.add(item.id);
    this.service.updateItemStatus(order.id, item.id, next).subscribe({
      next: (updated) => {
        this.busy.delete(item.id);
        this.replaceOrder(updated);
      },
      error: (error) => {
        this.busy.delete(item.id);
        this.errorMessage = getErrorMessage(error, 'No se pudo cambiar el estado del plato.');
      },
    });
  }

  // ---------- Pedido completo ----------

  // ¿Queda algún plato en espera? (para "Preparar todo")
  canPrepareAll(order: TableOrder): boolean {
    return order.items.some((i) => i.status === 'PENDING');
  }

  // ¿Queda algún plato sin terminar? (para "Todo listo")
  canReadyAll(order: TableOrder): boolean {
    return order.items.some((i) => i.status !== 'READY');
  }

  isOrderBusy(order: TableOrder): boolean {
    return this.busyOrders.has(order.id) || order.items.some((i) => this.busy.has(i.id));
  }

  advanceOrder(order: TableOrder, status: OrderItemStatus): void {
    if (this.isOrderBusy(order)) return;

    this.busyOrders.add(order.id);
    this.service.updateOrderStatus(order.id, status).subscribe({
      next: (updated) => {
        this.busyOrders.delete(order.id);
        this.replaceOrder(updated);
      },
      error: (error) => {
        this.busyOrders.delete(order.id);
        this.errorMessage = getErrorMessage(error, 'No se pudo cambiar el estado del pedido.');
      },
    });
  }

  // Pinta el pedido actualizado sin esperar a la siguiente consulta
  private replaceOrder(updated: TableOrder): void {
    this.tables = this.tables.map((t) => ({
      ...t,
      orders: t.orders.map((o) => (o.id === updated.id ? updated : o)),
    }));
  }

  orderLabel(order: TableOrder): string {
    if (order.ready) return 'Listo, esperando al mesero';
    return order.status === 'PENDING' ? 'En espera' : 'En preparación';
  }

  itemLabel(status: OrderItemStatus): string {
    return status === 'PENDING' ? 'En espera' : status === 'PREPARING' ? 'En preparación' : 'Listo';
  }
}

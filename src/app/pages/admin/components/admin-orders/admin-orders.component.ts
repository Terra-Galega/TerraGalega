import { Component, inject } from '@angular/core';
import { DatePipe, DecimalPipe } from '@angular/common';
import { OrderService } from '../../../../services/order.service';
import { ProductService } from '../../../../services/product.service';
import { OrderSummary, ORDER_STATUS_LABELS } from '../../../../models/order';
import { Product } from '../../../../models/product';
import { getErrorMessage } from '../../../../utils/http-error';

type OrderFilter = 'all' | 'active' | 'completed';

@Component({
  selector: 'app-admin-orders',
  imports: [DatePipe, DecimalPipe],
  templateUrl: './admin-orders.component.html',
  styleUrl: './admin-orders.component.scss',
})
export class AdminOrdersComponent {
  private orderService = inject(OrderService);
  private productService = inject(ProductService);

  ordersSummary: OrderSummary[] = [];
  products: Product[] = [];
  statusLabels = ORDER_STATUS_LABELS;

  // Filtros que se resuelven en el back
  dateFrom = '';
  dateTo = '';
  productId: number | null = null;
  orderNumber = '';
  errorMessage = '';

  // Filtro de la lista: todos, solo activos o solo completados
  filter: OrderFilter = 'all';

  readonly filterOptions: { value: OrderFilter; label: string }[] = [
    { value: 'all', label: 'Todos' },
    { value: 'active', label: 'Activos' },
    { value: 'completed', label: 'Completados' },
  ];

  constructor() {
    this.loadOrders();
    this.orderService.refresh$.subscribe(() => this.loadOrders());

    this.productService.getProducts().subscribe({
      next: (products) =>
        (this.products = [...products].sort((a, b) =>
          a.name.localeCompare(b.name),
        )),
      error: (error) => console.error('Error cargando productos:', error),
    });
  }

  loadOrders() {
    const parsed = this.orderNumber.trim() ? Number(this.orderNumber) : null;

    this.orderService
      .getAllOrderSummaries({
        from: this.dateFrom,
        to: this.dateTo,
        productId: this.productId,
        orderId: parsed !== null && !Number.isNaN(parsed) ? parsed : null,
      })
      .subscribe({
        next: (orders) => {
          this.ordersSummary = orders;
          this.errorMessage = '';
        },
        error: (error) => {
          console.error('Error cargando pedidos:', error);
          this.ordersSummary = [];
          this.errorMessage = getErrorMessage(
            error,
            'No se pudieron cargar los pedidos.',
          );
        },
      });
  }

  onDateFromChange(event: Event) {
    this.dateFrom = (event.target as HTMLInputElement).value;
    this.loadOrders();
  }

  onDateToChange(event: Event) {
    this.dateTo = (event.target as HTMLInputElement).value;
    this.loadOrders();
  }

  onProductChange(event: Event) {
    const value = (event.target as HTMLSelectElement).value;
    this.productId = value ? Number(value) : null;
    this.loadOrders();
  }

  onOrderNumberChange(event: Event) {
    this.orderNumber = (event.target as HTMLInputElement).value;
    this.loadOrders();
  }

  get hasFilters(): boolean {
    return (
      !!this.dateFrom ||
      !!this.dateTo ||
      this.productId !== null ||
      !!this.orderNumber.trim()
    );
  }

  clearFilters() {
    this.dateFrom = '';
    this.dateTo = '';
    this.productId = null;
    this.orderNumber = '';
    this.loadOrders();
  }

  // Un pedido está completado cuando se entregó o se canceló
  private isFinished(order: OrderSummary): boolean {
    return order.status === 'DELIVERED' || order.status === 'CANCELLED';
  }

  get filteredOrders(): OrderSummary[] {
    if (this.filter === 'all') {
      return this.ordersSummary;
    }

    return this.ordersSummary.filter((order) =>
      this.filter === 'completed'
        ? this.isFinished(order)
        : !this.isFinished(order),
    );
  }
}
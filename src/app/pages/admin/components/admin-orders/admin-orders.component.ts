import { Component, inject } from '@angular/core';
import { DatePipe, DecimalPipe } from '@angular/common';
import { OrderService } from '../../../../services/order.service';
import { ProductService } from '../../../../services/product.service';
import { OrderSummary, ORDER_STATUS_LABELS } from '../../../../models/order';
import { ProductOption } from '../../../../models/product';
import { getErrorMessage } from '../../../../utils/http-error';

// Opciones del filtro rápido de la lista según el estado del pedido: todos, activos o completados.
type OrderFilter = 'all' | 'active' | 'completed';

// Lista de pedidos del panel de administración: permite filtrar por fechas, producto, número de pedido y estado.
@Component({
  selector: 'app-admin-orders',
  imports: [DatePipe, DecimalPipe],
  templateUrl: './admin-orders.component.html',
})
export class AdminOrdersComponent {
  // Servicio de pedidos: consulta la lista y avisa cuando hay que recargarla.
  private orderService = inject(OrderService);
  // Servicio de productos: aporta las opciones del desplegable de producto.
  private productService = inject(ProductService);

  // Resumen de pedidos devueltos por el backend según los filtros aplicados.
  ordersSummary: OrderSummary[] = [];
  // Productos (solo id y nombre) para el desplegable de filtro.
  products: ProductOption[] = [];
  // Traduce cada estado del pedido al texto que se muestra en pantalla.
  statusLabels = ORDER_STATUS_LABELS;

  // Filtros que se resuelven en el back
  dateFrom = '';
  dateTo = '';
  productId: number | null = null;
  orderNumber = '';
  // Mensaje de error de la carga; queda vacío cuando todo sale bien.
  errorMessage = '';

  // Filtro de la lista: todos, solo activos o solo completados
  filter: OrderFilter = 'all';

  // Botones del filtro: valor interno y texto visible de cada uno.
  readonly filterOptions: { value: OrderFilter; label: string }[] = [
    { value: 'all', label: 'Todos' },
    { value: 'active', label: 'Activos' },
    { value: 'completed', label: 'Completados' },
  ];

  // Al iniciar carga los pedidos, queda atento a las solicitudes de recarga y carga los productos del desplegable.
  constructor() {
    this.loadOrders();
    // Recarga la lista cada vez que otro componente pide actualizarla.
    this.orderService.refresh$.subscribe(() => this.loadOrders());

    // Carga los productos del filtro y los ordena alfabéticamente por nombre.
    this.productService.getProductOptions().subscribe({
      next: (products) =>
        (this.products = [...products].sort((a, b) =>
          a.name.localeCompare(b.name),
        )),
      error: (error) => console.error('Error cargando productos:', error),
    });
  }

  // Consulta los pedidos al backend aplicando los filtros actuales.
  loadOrders() {
    // Convierte el número de pedido escrito a número; si está vacío queda en null.
    const parsed = this.orderNumber.trim() ? Number(this.orderNumber) : null;

    this.orderService
      .getAllOrderSummaries({
        from: this.dateFrom,
        to: this.dateTo,
        productId: this.productId,
        // Solo se envía el número de pedido si es un valor numérico válido.
        orderId: parsed !== null && !Number.isNaN(parsed) ? parsed : null,
      })
      .subscribe({
        // Guarda los pedidos recibidos y limpia cualquier error anterior.
        next: (orders) => {
          this.ordersSummary = orders;
          this.errorMessage = '';
        },
        // Si falla, vacía la lista y muestra el mensaje del servidor o uno genérico.
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

  // Guarda la fecha inicial elegida y vuelve a consultar los pedidos.
  onDateFromChange(event: Event) {
    this.dateFrom = (event.target as HTMLInputElement).value;
    this.loadOrders();
  }

  // Guarda la fecha final elegida y vuelve a consultar los pedidos.
  onDateToChange(event: Event) {
    this.dateTo = (event.target as HTMLInputElement).value;
    this.loadOrders();
  }

  // Guarda el producto elegido (null si se selecciona Todos) y vuelve a consultar los pedidos.
  onProductChange(event: Event) {
    const value = (event.target as HTMLSelectElement).value;
    this.productId = value ? Number(value) : null;
    this.loadOrders();
  }

  // Guarda el número de pedido escrito y vuelve a consultar los pedidos.
  onOrderNumberChange(event: Event) {
    this.orderNumber = (event.target as HTMLInputElement).value;
    this.loadOrders();
  }

  // Indica si hay algún filtro aplicado; sirve para mostrar el botón Limpiar filtros.
  get hasFilters(): boolean {
    return (
      !!this.dateFrom ||
      !!this.dateTo ||
      this.productId !== null ||
      !!this.orderNumber.trim()
    );
  }

  // Reinicia todos los filtros y recarga la lista completa.
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

  // Aplica en el front el filtro de estado (todos, activos o completados) sobre los pedidos ya traídos del back.
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
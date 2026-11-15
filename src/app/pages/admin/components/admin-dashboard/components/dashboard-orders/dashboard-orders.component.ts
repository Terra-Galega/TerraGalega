import {
  ChangeDetectionStrategy,
  Component,
  EventEmitter,
  Input,
  Output,
} from '@angular/core';
import { orderStatusLabel } from '../../../../../../models/order';
import {
  DashOrder,
  DelayedOrder,
  statusStyle,
  dateTimeLabel,
  fmtMoney,
  formatWait,
  minutesSince,
} from '../../../../../../utils/dashboard-metrics';

// Dos listas: los últimos pedidos recibidos y los que llevan demasiado tiempo sin terminar.
@Component({
  selector: 'app-dashboard-orders',
  templateUrl: './dashboard-orders.component.html',
  styleUrl: './dashboard-orders.component.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class DashboardOrdersComponent {
  @Input({ required: true }) latest: DashOrder[] = [];
  @Input({ required: true }) delayed: DelayedOrder[] = [];
  // Momento de la última actualización (para calcular "hace X min")
  @Input({ required: true }) now: Date = new Date();
  // Minutos de espera a partir de los cuales un pedido se considera atrasado
  @Input() threshold = 30;
  @Output() thresholdChange = new EventEmitter<number>();

  readonly thresholds = [15, 30, 45, 60];
  readonly money = fmtMoney;
  readonly statusLabel = orderStatusLabel;
  readonly statusStyle = statusStyle;
  readonly wait = formatWait;
  readonly dateLabel = dateTimeLabel;

  ago(order: DashOrder): string {
    return `hace ${formatWait(minutesSince(order.date, this.now))}`;
  }

  // Gravedad: el doble del umbral o más se marca en rojo, el resto en dorado
  isSevere(order: DelayedOrder): boolean {
    return order.waitMin >= this.threshold * 2;
  }

  onThreshold(event: Event) {
    this.thresholdChange.emit(
      Number((event.target as HTMLSelectElement).value),
    );
  }
}

import { Component, Input } from '@angular/core';
import { AnimatedNumberComponent } from '../../../../../../components/animated-number/animated-number.component';
import { DeltaBadgeComponent } from '../../../../../../components/delta-badge/delta-badge.component';
import {
  Kpis,
  PROFIT_MARKUP,
  formatWait,
} from '../../../../../../utils/dashboard-metrics';

// Minutos a partir de los cuales el pedido pendiente más antiguo se marca como urgente
const URGENT_MIN = 15;

// Tarjetas de indicadores del día: ventas, pedidos pendientes, productos vendidos y ganancia.
@Component({
  selector: 'app-dashboard-kpis',
  imports: [AnimatedNumberComponent, DeltaBadgeComponent],
  templateUrl: './dashboard-kpis.component.html',
  styleUrl: './dashboard-kpis.component.scss',
})
export class DashboardKpisComponent {
  @Input({ required: true }) kpis!: Kpis;

  readonly profitPct = Math.round(PROFIT_MARKUP * 100);

  // Hay pendientes esperando más de URGENT_MIN minutos
  get urgent(): boolean {
    return (
      this.kpis.pending > 0 &&
      this.kpis.pendingOldestMin !== null &&
      this.kpis.pendingOldestMin >= URGENT_MIN
    );
  }

  get oldestText(): string {
    return this.kpis.pendingOldestMin === null
      ? 'Todo al día'
      : `El más antiguo espera ${formatWait(this.kpis.pendingOldestMin)}`;
  }
}

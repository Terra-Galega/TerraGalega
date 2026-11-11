import {
  ChangeDetectionStrategy,
  Component,
  Input,
  OnChanges,
} from '@angular/core';
import { SearchInputComponent } from '../../../../../../components/search-input/search-input.component';
import {
  DishRow,
  PROFIT_MARKUP,
  fmtMoney,
} from '../../../../../../utils/dashboard-metrics';

type SortKey = 'name' | 'price' | 'units' | 'sales' | 'profit';

// Tabla con todos los platos de la carta: precio final, costo, ganancia del 20 %
// por unidad y lo que se vendió / ganó en el rango elegido.
@Component({
  selector: 'app-dashboard-dishes',
  imports: [SearchInputComponent],
  templateUrl: './dashboard-dishes.component.html',
  styleUrl: './dashboard-dishes.component.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class DashboardDishesComponent implements OnChanges {
  @Input({ required: true }) rows: DishRow[] = [];
  // Rango mostrado en el título ("Hoy", "7 días"…)
  @Input() rangeLabel = '';

  readonly money = fmtMoney;
  readonly profitPct = Math.round(PROFIT_MARKUP * 100);

  readonly columns: { key: SortKey | null; label: string; right: boolean }[] = [
    { key: 'name', label: 'Plato', right: false },
    { key: 'price', label: 'Precio final', right: true },
    { key: null, label: 'Costo', right: true },
    { key: null, label: `Ganancia ${this.profitPct} %`, right: true },
    { key: 'units', label: 'Uds.', right: true },
    { key: 'sales', label: 'Ventas', right: true },
    { key: 'profit', label: 'Ganancia', right: true },
  ];

  sortKey: SortKey = 'sales';
  sortDir: 1 | -1 = -1;
  term = '';

  // Filas ya filtradas y ordenadas (se recalculan solo al cambiar datos o filtros)
  view: DishRow[] = [];
  totals = { units: 0, sales: 0, profit: 0 };

  ngOnChanges() {
    this.update();
  }

  onSearch(term: string) {
    this.term = term;
    this.update();
  }

  sortBy(key: SortKey | null) {
    if (!key) return;
    if (this.sortKey === key) {
      this.sortDir = this.sortDir === 1 ? -1 : 1;
    } else {
      this.sortKey = key;
      // Texto de A a Z; números de mayor a menor
      this.sortDir = key === 'name' ? 1 : -1;
    }
    this.update();
  }

  private update() {
    const term = this.term.trim().toLowerCase();
    const rows = this.rows.filter(
      (r) =>
        !term ||
        r.name.toLowerCase().includes(term) ||
        r.category.toLowerCase().includes(term),
    );

    const key = this.sortKey;
    this.view = [...rows].sort((a, b) => {
      const diff =
        key === 'name' ? a.name.localeCompare(b.name) : a[key] - b[key];
      return diff * this.sortDir || a.name.localeCompare(b.name);
    });

    this.totals = rows.reduce(
      (t, r) => ({
        units: t.units + r.units,
        sales: t.sales + r.sales,
        profit: t.profit + r.profit,
      }),
      { units: 0, sales: 0, profit: 0 },
    );
  }
}

import { Component, Input, OnChanges, OnDestroy, SimpleChanges } from '@angular/core';
import { fmtMoney } from '../../utils/dashboard-metrics';
import { tween } from '../../utils/tween';

// Número que "corre" desde el valor anterior hasta el nuevo cada vez que cambia.
// Uso: <app-animated-number [value]="1234" kind="money" />
@Component({
  selector: 'app-animated-number',
  template: `{{ text }}`,
})
export class AnimatedNumberComponent implements OnChanges, OnDestroy {
  @Input({ required: true }) value = 0;
  // money = $1,234 · int = 1,234
  @Input() kind: 'money' | 'int' = 'int';

  text = '';
  private shown = 0;
  private cancel: (() => void) | null = null;

  ngOnChanges(changes: SimpleChanges) {
    if (!changes['value']) return;
    this.cancel?.();
    this.cancel = tween([this.shown], [this.value], 800, ([v]) => {
      this.shown = v;
      this.text = this.format(v);
    });
  }

  ngOnDestroy() {
    this.cancel?.();
  }

  private format(v: number): string {
    return this.kind === 'money'
      ? fmtMoney(v)
      : Math.round(v).toLocaleString('en-US');
  }
}
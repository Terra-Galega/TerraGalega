import { Component, Input } from '@angular/core';
import { CommonModule } from '@angular/common';

@Component({
  selector: 'app-order-summary',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './order-summary.component.html',
})
export class OrderSummaryComponent {
  @Input() items: any[] = [];
  @Input() total = 0;
  @Input() unitPrice: (item: any) => number = () => 0;
}
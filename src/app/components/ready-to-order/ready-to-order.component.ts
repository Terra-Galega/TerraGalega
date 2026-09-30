import { Component } from '@angular/core';
import { ButtonComponent } from '../button/button.component';

@Component({
  selector: 'app-ready-to-order',
  imports: [ButtonComponent],
  templateUrl: './ready-to-order.component.html',
  styleUrl: './ready-to-order.component.scss',
})
export class ReadyToOrderComponent {}
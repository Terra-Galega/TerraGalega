import { Component, Input } from '@angular/core';
import { RouterLink } from '@angular/router';
import { ButtonComponent } from '../../../../components/button/button.component';
import { Order } from '../../../../models/order';
@Component({
  selector: 'app-logged-in',
  imports: [RouterLink, ButtonComponent],
  templateUrl: './logged-in.component.html',
  styleUrl: './logged-in.component.scss'
})
export class LoggedInComponent {

  @Input() orders: Order[] = [];

}

import { Component} from '@angular/core';
import {RouterLink} from "@angular/router";
import { ButtonComponent } from '../../../../components/button/button.component';




@Component({
  selector: 'app-empty-orders',
  imports: [RouterLink, ButtonComponent],
  templateUrl: './empty-orders.component.html',
  styleUrl: './empty-orders.component.scss'
})
export class EmptyOrdersComponent {
}

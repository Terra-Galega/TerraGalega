import { TranslatePipe } from '../../pipes/translate.pipe';
import { Component, inject } from '@angular/core';
import { ToastService } from '../../services/toast.service';

@Component({
  imports: [TranslatePipe],
  selector: 'app-toast',
  templateUrl: './toast.component.html',
  styleUrl: './toast.component.scss',
})
export class ToastComponent {
  //Injectar el servicio
  toastService = inject(ToastService);
}
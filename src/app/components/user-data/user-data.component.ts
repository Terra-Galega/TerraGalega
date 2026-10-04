import { Component, Input } from '@angular/core';
import { CommonModule } from '@angular/common';
import { User } from '../../interfaces/user';

@Component({
  selector: 'app-user-data',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './user-data.component.html',
  styleUrls: ['./user-data.component.scss']
})
export class UserDataComponent {
  // Recibimos el user desde el padre
  @Input() user!: User; 
}
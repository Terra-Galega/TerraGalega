import { Component, EventEmitter, Input, Output } from '@angular/core';
import { StarIconComponent } from '../../../../components/star-icon/star-icon.component';

@Component({
  selector: 'app-category-filter',
  // Grupo con nombre: el lector dice "Filtrar por categoría, grupo" y cada botón "pulsado" o no
  host: { role: 'group', 'aria-label': 'Filtrar por categoría' },
  imports: [StarIconComponent],
  templateUrl: './category-filter.component.html',
  styleUrl: './category-filter.component.scss',
})
export class CategoryFilterComponent {
  @Input() categories: string[] = [];
  @Input() selected = '';

  @Output() selectedChange = new EventEmitter<string>();
}
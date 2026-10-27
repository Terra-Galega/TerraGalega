import { Component, EventEmitter, Input, Output } from '@angular/core';
import { ButtonComponent } from '../../../../components/button/button.component';
import { SearchInputComponent } from '../../../../components/search-input/search-input.component';

@Component({
  selector: 'app-admin-topbar',
  imports: [ButtonComponent, SearchInputComponent],
  templateUrl: './admin-topbar.component.html',
  styleUrl: './admin-topbar.component.scss',
})
export class AdminTopbarComponent {
  // Propiedades configurables para personalizar la barra superior desde el componente padre.
  @Input() title = 'Productos';
  @Input() showSearch = true;
  @Input() showAddButton = true;
  @Input() addLabel = 'Añadir producto';

  // Eventos que notifican al componente padre los cambios en la búsqueda y los clics en el botón de añadir.
  @Output() searchChanged = new EventEmitter<string>();
  @Output() addClicked = new EventEmitter<void>();
}

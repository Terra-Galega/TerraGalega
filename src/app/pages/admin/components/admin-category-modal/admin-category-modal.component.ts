import {
  Component,
  EventEmitter,
  Input,
  Output,
  SimpleChanges,
  inject,
} from '@angular/core';
import { Category, CreateCategory } from '../../../../models/category';
import { CategoryService } from '../../../../services/category.service';
import { ButtonComponent } from '../../../../components/button/button.component';
import { IconButtonComponent } from '../../../../components/icon-button/icon-button.component';
import { FormFieldComponent } from '../../../../components/form-field/form-field.component';
import { ToastService } from '../../../../services/toast.service';
import { getErrorMessage } from '../../../../utils/http-error';

/**
 * Ventana flotante para crear o editar una categoría.
 * Si recibe una categoría, la edita; si recibe null, crea una nueva.
 * Valida nombre y descripción y llama al servicio correspondiente.
 * Avisa al padre con "saved" y "closed".
 */
@Component({
  selector: 'app-admin-category-modal',
  imports: [ButtonComponent, IconButtonComponent, FormFieldComponent],
  templateUrl: './admin-category-modal.component.html',
})
export class AdminCategoryModalComponent {
  private categoryService = inject(CategoryService);
  private toast = inject(ToastService);

  // Categoría a editar; null significa "crear una nueva"
  @Input() category: Category | null = null;

  // closed: se debe cerrar la ventana. saved: se guardó con éxito.
  @Output() closed = new EventEmitter<void>();
  @Output() saved = new EventEmitter<void>();

  // Campos del formulario
  name = '';
  description = '';

  // Mensaje de error que se muestra dentro del formulario
  errorMessage = '';

  // Se ejecuta cada vez que el padre cambia el @Input "category":
  // llena el formulario con la categoría (editar) o lo deja vacío (crear)
  ngOnChanges(changes: SimpleChanges) {
    if (!changes['category']) {
      return;
    }

    this.errorMessage = '';

    if (this.category) {
      this.name = this.category.name;
      this.description = this.category.description ?? '';
    } else {
      this.name = '';
      this.description = '';
    }
  }

  // Guardan en el componente lo que el usuario escribe
  setName(event: Event) {
    this.name = (event.target as HTMLInputElement).value;
  }

  setDescription(event: Event) {
    this.description = (event.target as HTMLTextAreaElement).value;
  }

  // Botón "Guardar cambios" / "Añadir categoría"
  save() {
    // Validación: nombre y descripción son obligatorios
    if (!this.name.trim()) {
      this.errorMessage = 'El nombre de la categoría es obligatorio.';
      return;
    }
    if (!this.description.trim()) {
      this.errorMessage = 'La descripción de la categoría es obligatoria.';
      return;
    }

    // EDITAR
    if (this.category) {
      // Se parte de la categoría original (conserva su id) y se
      // sobreescriben los campos del formulario
      const updated: Category = {
        ...this.category,
        name: this.name.trim(),
        description: this.description,
      };

      this.categoryService.updateCategory(updated).subscribe({
        next: () =>
          this.onSuccess('Cambios de la categoría guardados correctamente'),
        error: (error) => this.onError(error),
      });

      return;
    }

    // CREAR (no se manda id, lo genera el backend)
    const created: CreateCategory = {
      name: this.name.trim(),
      description: this.description,
    };

    this.categoryService.createCategory(created).subscribe({
      next: () => this.onSuccess('Categoría creada correctamente'),
      error: (error) => this.onError(error),
    });
  }

  // Guardado exitoso: refresca la tabla, avisa al usuario y cierra la ventana
  private onSuccess(message: string) {
    this.categoryService.refresh();
    this.toast.show(message);
    this.saved.emit();
    this.closed.emit();
  }

  // Error al guardar: se muestra dentro del formulario y la ventana sigue abierta
  private onError(error: any) {
    console.error('Error guardando categoría:', error);
    this.errorMessage = getErrorMessage(
      error,
      'No se pudo guardar la categoría.',
    );
  }

  // Botón "Cancelar" y la X: pide al padre cerrar la ventana
  close() {
    this.closed.emit();
  }
}

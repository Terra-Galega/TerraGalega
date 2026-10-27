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

@Component({
  selector: 'app-admin-category-modal',
  imports: [ButtonComponent, IconButtonComponent, FormFieldComponent],
  templateUrl: './admin-category-modal.component.html',
})
export class AdminCategoryModalComponent {
  private categoryService = inject(CategoryService);
  private toast = inject(ToastService);

  @Input() category: Category | null = null;

  @Output() closed = new EventEmitter<void>();
  @Output() saved = new EventEmitter<void>();

  name = '';
  description = '';

  errorMessage = '';

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

  setName(event: Event) {
    this.name = (event.target as HTMLInputElement).value;
  }

  setDescription(event: Event) {
    this.description = (event.target as HTMLTextAreaElement).value;
  }

  save() {
    if (!this.name.trim()) {
      this.errorMessage = 'El nombre de la categoría es obligatorio.';
      return;
    }

    // EDITAR
    if (this.category) {
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

  private onSuccess(message: string) {
    this.categoryService.refresh();
    this.toast.show(message);
    this.saved.emit();
    this.closed.emit();
  }

  private onError(error: any) {
    console.error('Error guardando categoría:', error);
    this.errorMessage =
      getErrorMessage(error, 'No se pudo guardar la categoría.');
  }

  close() {
    this.closed.emit();
  }
}
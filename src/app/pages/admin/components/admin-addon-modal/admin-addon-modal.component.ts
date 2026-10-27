import {
  Component,
  EventEmitter,
  Input,
  Output,
  SimpleChanges,
  inject,
} from '@angular/core';
import { AddOn, CreateAddOn } from '../../../../models/addOn';
import { Category } from '../../../../models/category';
import { AddOnService } from '../../../../services/addOn.service';
import { CategoryService } from '../../../../services/category.service';
import { ButtonComponent } from '../../../../components/button/button.component';
import { IconButtonComponent } from '../../../../components/icon-button/icon-button.component';
import { FormFieldComponent } from '../../../../components/form-field/form-field.component';
import { CheckOptionComponent } from '../../../../components/check-option/check-option.component';
import { ToastService } from '../../../../services/toast.service';
import { getErrorMessage } from '../../../../utils/http-error';

@Component({
  selector: 'app-admin-addon-modal',
  imports: [
    ButtonComponent,
    IconButtonComponent,
    FormFieldComponent,
    CheckOptionComponent,
  ],
  templateUrl: './admin-addon-modal.component.html',
})
export class AdminAddonModalComponent {
  private addOnService = inject(AddOnService);
  private categoryService = inject(CategoryService);
  private toast = inject(ToastService);

  @Input() addon: AddOn | null = null;

  @Output() closed = new EventEmitter<void>();
  @Output() saved = new EventEmitter<void>();

  categories: Category[] = [];

  name = '';
  description = '';
  price = 0;
  active = true;
  selectedCategoryIds: number[] = [];

  errorMessage = '';

  constructor() {
    this.categoryService.getCategories().subscribe({
      next: (categories) => {
        this.categories = categories;
      },
      error: (error) => {
        console.error('Error cargando categorías:', error);
      },
    });
  }

  ngOnChanges(changes: SimpleChanges) {
    if (!changes['addon']) {
      return;
    }

    this.errorMessage = '';

    if (this.addon) {
      this.name = this.addon.name;
      this.description = this.addon.description ?? '';
      this.price = this.addon.price;
      this.active = this.addon.active;
      this.selectedCategoryIds = (this.addon.categories ?? []).map((c) => c.id);
    } else {
      this.name = '';
      this.description = '';
      this.price = 0;
      this.active = true;
      this.selectedCategoryIds = [];
    }
  }

  setName(event: Event) {
    this.name = (event.target as HTMLInputElement).value;
  }

  setDescription(event: Event) {
    this.description = (event.target as HTMLTextAreaElement).value;
  }

  setPrice(event: Event) {
    const input = event.target as HTMLInputElement;
    const value = Number(input.value);

    this.price = Math.round(value / 100) * 100;
    input.value = String(this.price);
  }

  toggleActive() {
    this.active = !this.active;
  }

  isCategorySelected(id: number): boolean {
    return this.selectedCategoryIds.includes(id);
  }

  toggleCategory(id: number) {
    if (this.isCategorySelected(id)) {
      this.selectedCategoryIds = this.selectedCategoryIds.filter(
        (c) => c !== id,
      );
    } else {
      this.selectedCategoryIds = [...this.selectedCategoryIds, id];
    }
  }

  private validate(): string {
    if (!this.name.trim()) return 'El nombre del addon es obligatorio.';
    if (!this.price || this.price <= 0)
      return 'El precio debe ser mayor que cero.';
    if (!this.description.trim()) return 'La descripción es obligatoria.';
    return '';
  }

  save() {
    this.errorMessage = this.validate();
    
    if (this.errorMessage) {
      return;
    }

    const selectedCategories = this.categories.filter((category) =>
      this.selectedCategoryIds.includes(category.id),
    );

    // EDITAR
    if (this.addon) {
      const updated: AddOn = {
        ...this.addon,
        name: this.name.trim(),
        description: this.description,
        price: this.price,
        active: this.active,
        categories: selectedCategories,
      };

      this.addOnService.updateAddOn(updated).subscribe({
        next: () =>
          this.onSuccess('Cambios del adicional guardados correctamente'),
        error: (error) => this.onError(error),
      });

      return;
    }

    // CREAR
    const created: CreateAddOn = {
      name: this.name.trim(),
      description: this.description,
      price: this.price,
      active: this.active,
      categories: selectedCategories,
    };

    this.addOnService.createAddOn(created).subscribe({
      next: () => this.onSuccess('Adicional creado correctamente'),
      error: (error) => this.onError(error),
    });
  }

  private onSuccess(message: string) {
    this.addOnService.refresh();
    this.toast.show(message);
    this.saved.emit();
    this.closed.emit();
  }

  private onError(error: any) {
    console.error('Error guardando adicional:', error);
    this.errorMessage =
      getErrorMessage(error, 'No se pudo guardar el adicional.');
  }

  close() {
    this.closed.emit();
  }
}
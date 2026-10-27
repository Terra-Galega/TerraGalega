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

/**
 * Ventana flotante para crear o editar un adicional.
 * Si recibe un adicional, lo edita; si recibe null, crea uno nuevo.
 * Permite elegir en qué categorías aparece, valida el formulario y llama
 * al servicio correspondiente. Avisa al padre con "saved" y "closed".
 */
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

  // Adicional a editar; null significa "crear uno nuevo"
  @Input() addon: AddOn | null = null;

  // closed: se debe cerrar la ventana. saved: se guardó con éxito.
  @Output() closed = new EventEmitter<void>();
  @Output() saved = new EventEmitter<void>();

  // Categorías disponibles para marcar (una casilla por categoría)
  categories: Category[] = [];

  // Campos del formulario
  name = '';
  description = '';
  price = 0;
  // Si está activo, se ofrece a los clientes al pedir
  active = true;
  // Ids de las categorías marcadas
  selectedCategoryIds: number[] = [];

  // Mensaje de error que se muestra dentro del formulario
  errorMessage = '';

  constructor() {
    // Se cargan las categorías una sola vez para armar las casillas
    this.categoryService.getCategories().subscribe({
      next: (categories) => {
        this.categories = categories;
      },
      error: (error) => {
        console.error('Error cargando categorías:', error);
      },
    });
  }

  // Se ejecuta cada vez que el padre cambia el @Input "addon":
  // llena el formulario con el adicional (editar) o lo deja vacío (crear)
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
      // De las categorías del adicional solo se guardan los ids
      this.selectedCategoryIds = (this.addon.categories ?? []).map((c) => c.id);
    } else {
      this.name = '';
      this.description = '';
      this.price = 0;
      this.active = true;
      this.selectedCategoryIds = [];
    }
  }

  // Los siguientes "set..." guardan en el componente lo que el usuario escribe
  setName(event: Event) {
    this.name = (event.target as HTMLInputElement).value;
  }

  setDescription(event: Event) {
    this.description = (event.target as HTMLTextAreaElement).value;
  }

  // Al salir del campo, el precio se redondea al múltiplo de 50 más cercano
  // (por ejemplo, 3020 queda en 3000) y se actualiza lo que se ve en el campo
  setPrice(event: Event) {
    const input = event.target as HTMLInputElement;
    const value = Number(input.value);

    this.price = Math.round(value / 50) * 50;
    input.value = String(this.price);
  }

  // Marca o desmarca "Activo" (sección "Estado")
  toggleActive() {
    this.active = !this.active;
  }

  // Indica si la casilla de una categoría está marcada
  isCategorySelected(id: number): boolean {
    return this.selectedCategoryIds.includes(id);
  }

  // Marca o desmarca una categoría. Se crea una lista nueva en vez de
  // modificar la anterior para que Angular detecte el cambio.
  toggleCategory(id: number) {
    if (this.isCategorySelected(id)) {
      this.selectedCategoryIds = this.selectedCategoryIds.filter(
        (c) => c !== id,
      );
    } else {
      this.selectedCategoryIds = [...this.selectedCategoryIds, id];
    }
  }

  // Revisa los campos obligatorios. Devuelve el mensaje del primer error,
  // o un texto vacío si todo está bien
  private validate(): string {
    if (!this.name.trim()) return 'El nombre del addon es obligatorio.';
    if (!this.price || this.price <= 0)
      return 'El precio debe ser mayor que cero.';
    if (!this.description.trim()) return 'La descripción es obligatoria.';
    return '';
  }

  // Botón "Guardar cambios" / "Añadir adicional"
  save() {
    // Si hay un error de validación, se muestra y no se envía nada
    this.errorMessage = this.validate();

    if (this.errorMessage) {
      return;
    }

    // Se convierten los ids marcados en los objetos categoría completos
    const selectedCategories = this.categories.filter((category) =>
      this.selectedCategoryIds.includes(category.id),
    );

    // EDITAR
    if (this.addon) {
      // Se parte del adicional original (conserva su id) y se
      // sobreescriben los campos del formulario
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

    // CREAR (no se manda id, lo genera el backend)
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

  // Guardado exitoso: refresca la tabla, avisa al usuario y cierra la ventana
  private onSuccess(message: string) {
    this.addOnService.refresh();
    this.toast.show(message);
    this.saved.emit();
    this.closed.emit();
  }

  // Error al guardar: se muestra dentro del formulario y la ventana sigue abierta
  private onError(error: any) {
    console.error('Error guardando adicional:', error);
    this.errorMessage = getErrorMessage(
      error,
      'No se pudo guardar el adicional.',
    );
  }

  // Botón "Cancelar" y la X: pide al padre cerrar la ventana
  close() {
    this.closed.emit();
  }
}

import {
  Component,
  EventEmitter,
  Input,
  Output,
  OnChanges,
  SimpleChanges,
  inject,
} from '@angular/core';

import { Product, CreateProduct } from '../../../../models/product';
import { ProductService } from '../../../../services/product.service';
import { Category } from '../../../../models/category';
import { CategoryService } from '../../../../services/category.service';
import { FEATURES, FeatureKey } from '../../../../models/feature';
import { ButtonComponent } from '../../../../components/button/button.component';
import { IconButtonComponent } from '../../../../components/icon-button/icon-button.component';
import { FormFieldComponent } from '../../../../components/form-field/form-field.component';
import { CheckOptionComponent } from '../../../../components/check-option/check-option.component';
import { ToastService } from '../../../../services/toast.service';
import { getErrorMessage } from '../../../../utils/http-error';

// Claves de las casillas de "Características": "popular" más las del
// catálogo FEATURES (vegetariano, picante, frutos secos, etc.)
type CheckKey = 'popular' | FeatureKey;

/**
 * Ventana flotante para crear o editar un producto.
 * Si recibe un producto, lo edita; si recibe null, crea uno nuevo.
 * Valida el formulario, busca la categoría elegida y llama al servicio
 * correspondiente. Avisa al padre con "saved" y "closed".
 */
@Component({
  selector: 'app-admin-product-modal',
  imports: [
    ButtonComponent,
    IconButtonComponent,
    FormFieldComponent,
    CheckOptionComponent,
  ],
  templateUrl: './admin-product-modal.component.html',
  styleUrl: './admin-product-modal.component.scss',
})
export class AdminProductModalComponent implements OnChanges {
  private productService = inject(ProductService);
  private categoryService = inject(CategoryService);
  private toast = inject(ToastService);

  // Producto a editar; null significa "crear uno nuevo"
  @Input() product: Product | null = null;

  // closed: se debe cerrar la ventana. saved: se guardó con éxito.
  @Output() closed = new EventEmitter<void>();
  @Output() saved = new EventEmitter<void>();

  // Categorías disponibles para el selector
  categories: Category[] = [];

  // Casillas de la sección "Características"
  checks: { key: CheckKey; label: string }[] = [
    { key: 'popular', label: 'Popular' },
    ...FEATURES,
  ];

  // Mensaje de error que se muestra dentro del formulario
  errorMessage = '';

  // Campos del formulario
  name = '';
  description = '';
  price = 0;
  imageUrl = '';

  // La categoría se maneja por su nombre (el valor del <select>)
  categoryName = '';

  // Estado: aparece en el menú solo si está activo
  active = true;
  // Si es popular, se destaca con una estrella
  popular = false;

  // Características del plato
  vegetarian = false;
  spicyMild = false;
  spicyHot = false;

  // Alérgenos
  containsNuts = false;
  containsSeafood = false;
  containsGluten = false;

  constructor() {
    this.loadCategories();

    // Si cambian las categorías (se crea o elimina una), se recarga el selector
    this.categoryService.refresh$.subscribe(() => {
      this.loadCategories();
    });
  }

  // Pide las categorías al servidor para llenar el selector
  loadCategories() {
    this.categoryService.getCategories().subscribe({
      next: (categories) => {
        this.categories = categories;
      },
      error: (error) => {
        console.error('Error cargando categorías:', error);
      },
    });
  }

  // Se ejecuta cada vez que el padre cambia el @Input "product":
  // llena el formulario con el producto (editar) o lo deja vacío (crear)
  ngOnChanges(changes: SimpleChanges) {
    if (!changes['product']) {
      return;
    }
    this.errorMessage = '';

    if (this.product) {
      this.name = this.product.name;
      this.description = this.product.description;
      this.price = this.product.price;
      this.imageUrl = this.product.imageUrl;

      this.categoryName = this.product.category?.name ?? '';

      this.active = this.product.active;
      this.popular = this.product.popular;

      this.vegetarian = this.product.vegetarian;
      this.spicyMild = this.product.spicyMild;
      this.spicyHot = this.product.spicyHot;

      this.containsNuts = this.product.containsNuts;
      this.containsSeafood = this.product.containsSeafood;
      this.containsGluten = this.product.containsGluten;
    } else {
      this.resetForm();
    }
  }

  // Deja el formulario en blanco (valores por defecto de un producto nuevo)
  resetForm() {
    this.name = '';
    this.description = '';
    this.price = 0;
    this.imageUrl = '';

    this.categoryName = '';

    this.active = true;
    this.popular = false;

    this.vegetarian = false;
    this.spicyMild = false;
    this.spicyHot = false;

    this.containsNuts = false;
    this.containsSeafood = false;
    this.containsGluten = false;
  }

  // Los siguientes "set..." guardan en el componente lo que el usuario escribe
  setName(event: Event) {
    this.name = (event.target as HTMLInputElement).value;
  }

  setDescription(event: Event) {
    this.description = (event.target as HTMLTextAreaElement).value;
  }

  // Al salir del campo, el precio se redondea al múltiplo de 100 más cercano
  // (por ejemplo, 38949 queda en 38900) y se actualiza lo que se ve en el campo
  setPrice(event: Event) {
    const input = event.target as HTMLInputElement;
    const value = Number(input.value);

    this.price = Math.round(value / 100) * 100;
    input.value = String(this.price);
  }

  setImageUrl(event: Event) {
    this.imageUrl = (event.target as HTMLInputElement).value;
  }

  setCategory(event: Event) {
    this.categoryName = (event.target as HTMLSelectElement).value;
  }

  // Indica si una casilla de "Características" está marcada
  isChecked(key: CheckKey): boolean {
    return this[key];
  }

  // Marca o desmarca una casilla de "Características"
  toggle(key: CheckKey) {
    this[key] = !this[key];
  }

  // Marca o desmarca "Activo" (sección "Estado")
  toggleActive() {
    this.active = !this.active;
  }

  // Revisa los campos obligatorios. Devuelve el mensaje del primer error,
  // o un texto vacío si todo está bien
  private validate(): string {
    if (!this.name.trim()) return 'El nombre del producto es obligatorio.';
    if (!this.price || this.price <= 0)
      return 'El precio debe ser mayor que cero.';
    if (!this.categoryName) return 'La categoría del producto es obligatoria.';
    if (!this.description.trim()) return 'La descripción es obligatoria.';
    if (!this.imageUrl.trim()) return 'La URL de la imagen es obligatoria.';
    return '';
  }

  // Botón "Guardar cambios" / "Añadir producto"
  save() {
    // Si hay un error de validación, se muestra y no se envía nada
    this.errorMessage = this.validate();

    if (this.errorMessage) {
      return;
    }

    // Se busca el objeto categoría completo a partir del nombre elegido
    const selectedCategory = this.categories.find(
      (category) => category.name === this.categoryName,
    );

    if (!selectedCategory) {
      this.errorMessage = 'No se encontró la categoría seleccionada.';
      return;
    }

    // EDITAR PRODUCTO
    if (this.product) {
      // Se parte del producto original (conserva su id) y se sobreescriben
      // los campos del formulario
      const updatedProduct: Product = {
        ...this.product,

        name: this.name,
        description: this.description,
        price: this.price,
        imageUrl: this.imageUrl,

        category: selectedCategory,
        active: this.active,
        popular: this.popular,

        vegetarian: this.vegetarian,
        spicyMild: this.spicyMild,
        spicyHot: this.spicyHot,

        containsNuts: this.containsNuts,
        containsSeafood: this.containsSeafood,
        containsGluten: this.containsGluten,
      };

      this.productService.updateProduct(updatedProduct).subscribe({
        next: () => {
          // Refresca la tabla, avisa al usuario y cierra la ventana
          this.productService.refresh();
          this.toast.show('Cambios del producto guardados correctamente');
          this.saved.emit();
          this.closed.emit();
        },
        error: (error) => {
          console.error('Error guardando producto:', error);
          // El error se muestra dentro del formulario y la ventana sigue abierta
          this.errorMessage = getErrorMessage(
            error,
            'No se pudo guardar el producto.',
          );
        },
      });

      return;
    }

    // CREAR PRODUCTO
    // No se envía ID porque el backend lo genera.
    const newProduct: CreateProduct = {
      name: this.name,
      description: this.description,
      price: this.price,
      category: selectedCategory,
      imageUrl: this.imageUrl,
      active: this.active,
      popular: this.popular,
      vegetarian: this.vegetarian,
      spicyMild: this.spicyMild,
      spicyHot: this.spicyHot,
      containsNuts: this.containsNuts,
      containsSeafood: this.containsSeafood,
      containsGluten: this.containsGluten,
    };

    this.productService.createProduct(newProduct).subscribe({
      next: (product) => {
        console.log('Producto creado:', product);

        this.productService.refresh();
        this.toast.show('Producto creado correctamente');
        this.saved.emit();
        this.closed.emit();
      },
      error: (error) => {
        console.error('Error guardando producto:', error);
        this.errorMessage = getErrorMessage(
          error,
          'No se pudo guardar el producto.',
        );
      },
    });
  }

  // Botón "Cancelar" y la X: pide al padre cerrar la ventana
  close() {
    this.closed.emit();
  }
}

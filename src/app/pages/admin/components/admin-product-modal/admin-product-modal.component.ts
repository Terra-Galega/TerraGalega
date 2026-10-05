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

type CheckKey = 'active' | 'popular' | FeatureKey;

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

  @Input() product: Product | null = null;

  @Output() closed = new EventEmitter<void>();
  @Output() saved = new EventEmitter<void>();

  categories: Category[] = [];

  checks: { key: CheckKey; label: string }[] = [
    { key: 'active', label: 'Activo' },
    { key: 'popular', label: 'Popular' },
    ...FEATURES,
  ];

  name = '';
  description = '';
  price = 0;
  imageUrl = '';

  categoryName = '';

  active = true;
  popular = false;

  vegetarian = false;
  spicyMild = false;
  spicyHot = false;

  containsNuts = false;
  containsSeafood = false;
  containsGluten = false;

  constructor() {
    this.categories = this.categoryService.getCategories();
  }

  ngOnChanges(changes: SimpleChanges) {
    if (!changes['product']) {
      return;
    }

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

  setName(event: Event) {
    this.name = (event.target as HTMLInputElement).value;
  }

  setDescription(event: Event) {
    this.description = (event.target as HTMLTextAreaElement).value;
  }

  setPrice(event: Event) {
    this.price = Number((event.target as HTMLInputElement).value);
  }

  setImageUrl(event: Event) {
    this.imageUrl = (event.target as HTMLInputElement).value;
  }

  setCategory(event: Event) {
    this.categoryName = (event.target as HTMLSelectElement).value;
  }

  isChecked(key: CheckKey): boolean {
    return this[key];
  }

  toggle(key: CheckKey) {
    this[key] = !this[key];
  }

  save() {
    if (!this.name.trim()) {
      console.error('El nombre del producto es obligatorio.');
      return;
    }

    if (!this.categoryName) {
      console.error('La categoría del producto es obligatoria.');
      return;
    }

    if (this.price <= 0) {
      console.error('El precio debe ser mayor que cero.');
      return;
    }

    const selectedCategory = this.categories.find(
      (category) => category.name === this.categoryName,
    );

    if (!selectedCategory) {
      console.error('No se encontró la categoría seleccionada.');
      return;
    }

    // EDITAR PRODUCTO
    if (this.product) {
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
          this.productService.refresh();
          this.saved.emit();
          this.closed.emit();
        },
        error: (error) => {
          console.error('Error actualizando producto:', error);
          console.error('Status:', error.status);
          console.error('Respuesta backend:', error.error);
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
        this.saved.emit();
        this.closed.emit();
      },
      error: (error) => {
        console.error('Error creando producto:', error);
        console.error('Status:', error.status);
        console.error('Respuesta backend:', error.error);
      },
    });
  }

  close() {
    this.closed.emit();
  }
}

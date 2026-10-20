import { Component, Input, inject } from '@angular/core';
import { Router } from '@angular/router';
import { Product } from '../../../../models/product';
import { DecimalPipe } from '@angular/common';
import { AddOn } from '../../../../models/addOn';
import { AuthService } from '../../../../services/auth.service';
import { CartService } from '../../../../services/cart.service';
import { ButtonComponent } from '../../../../components/button/button.component';
import { FeatureChipsComponent } from '../../../../components/feature-chips/feature-chips.component';
import { StarIconComponent } from '../../../../components/star-icon/star-icon.component';
import { AddonOptionComponent } from '../addon-option/addon-option.component';
import { QuantitySelectorComponent } from '../quantity-selector/quantity-selector.component';
import { AddOnService } from '../../../../services/addOn.service';

@Component({
  selector: 'app-product-info-card',
  imports: [
    DecimalPipe,
    ButtonComponent,
    FeatureChipsComponent,
    StarIconComponent,
    AddonOptionComponent,
    QuantitySelectorComponent,
  ],
  templateUrl: './product-info-card.component.html',
  styleUrl: './product-info-card.component.scss',
})
export class ProductInfoCardComponent {
  private router = inject(Router);
  private auth = inject(AuthService);
  private cartService = inject(CartService);
  private addOnService = inject(AddOnService);

  @Input() product!: Product;

  addons: AddOn[] = [];
  selectedAddOns: AddOn[] = [];

  quantity = 1;

  // Muestra el mensaje "añadido" por unos segundos
  added = false;

  ngOnInit(): void {
    this.loadAddOns();
  }

  private loadAddOns(): void {
    const categoryId = this.product.category?.id;

    if (!categoryId) {
      this.addons = [];
      return;
    }

    this.addOnService.getActiveAddOnsByCategory(categoryId).subscribe({
      next: (addOns) => (this.addons = addOns),
      error: (error) => {
        console.error('Error cargando adicionales:', error);
        this.addons = [];
      },
    });
  }

  // Cuántas unidades de este adicional hay seleccionadas
  addOnQuantity(addOn: AddOn): number {
    return this.selectedAddOns.filter((selected) => selected.id === addOn.id)
      .length;
  }

  // delta = +1 agrega una unidad, delta = -1 quita una
  changeAddOnQuantity(addOn: AddOn, delta: number): void {
    if (delta > 0) {
      this.selectedAddOns = [...this.selectedAddOns, addOn];
      return;
    }

    const index = this.selectedAddOns.findIndex(
      (selected) => selected.id === addOn.id,
    );
    if (index !== -1) {
      this.selectedAddOns = this.selectedAddOns.filter((_, i) => i !== index);
    }
  }

  get addOnsTotal(): number {
    return this.selectedAddOns.reduce((total, addOn) => total + addOn.price, 0);
  }

  get totalPrice(): number {
    return (this.product.price + this.addOnsTotal) * this.quantity;
  }

  addToOrder(): void {
    this.cartService.addItem(this.product, this.quantity, this.selectedAddOns);

    // Limpiar la selección después de agregar el producto
    this.selectedAddOns = [];
    this.quantity = 1;
    this.added = true;

    setTimeout(() => {
      this.added = false;
    }, 2500);
  }
}

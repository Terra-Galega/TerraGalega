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
    this.addons = (this.product.category?.addOns ?? []).filter(
      (addOn) => addOn.active,
    );
  }

  toggleAddOn(addOn: AddOn): void {
    const isSelected = this.isAddOnSelected(addOn);

    if (isSelected) {
      this.selectedAddOns = this.selectedAddOns.filter(
        (selected) => selected.id !== addOn.id,
      );
    } else {
      this.selectedAddOns = [...this.selectedAddOns, addOn];
    }
  }

  isAddOnSelected(addOn: AddOn): boolean {
    return this.selectedAddOns.some((selected) => selected.id === addOn.id);
  }

  get addOnsTotal(): number {
    return this.selectedAddOns.reduce((total, addOn) => total + addOn.price, 0);
  }

  get totalPrice(): number {
    return (this.product.price + this.addOnsTotal) * this.quantity;
  }

  addToOrder(): void {
    this.cartService.addItem(
      this.product,
      this.quantity,
      this.selectedAddOns
    );

    // Limpiar la selección después de agregar el producto
    this.selectedAddOns = [];
    this.quantity = 1;
    this.added = true;

    setTimeout(() => {
      this.added = false;
    }, 2500);
  }
}

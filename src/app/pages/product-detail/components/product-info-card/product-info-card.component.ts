import { Component, Input, inject } from '@angular/core';
import { Product } from '../../../../models/product';
import { DecimalPipe } from '@angular/common';
import { CategoryService } from '../../../../services/category.service';
import { AddOn } from '../../../../models/addOn';

@Component({
  selector: 'app-product-info-card',
  imports: [DecimalPipe],
  templateUrl: './product-info-card.component.html',
  styleUrl: './product-info-card.component.scss',
})
export class ProductInfoCardComponent {
  private categoryService = inject(CategoryService);
  @Input() product!: Product;

  addons: AddOn[] = [];
  selectedAddOns: AddOn[] = [];

  quantity = 1;

  ngOnInit(): void {
    this.loadAddOns();
  }

  private loadAddOns(): void {
    const categoryId = this.product.category?.id;

    if (!categoryId) {
      this.addons = [];
      return;
    }

    const category = this.categoryService.getCategoryById(categoryId);

    this.addons = category?.addOns.filter((addOn) => addOn.active) ?? [];
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

  increaseQuantity(): void {
    this.quantity++;
  }

  decreaseQuantity(): void {
    if (this.quantity > 1) {
      this.quantity--;
    }
  }

  get addOnsTotal(): number {
    return this.selectedAddOns.reduce((total, addOn) => total + addOn.price, 0);
  }

  get totalPrice(): number {
    return (this.product.price + this.addOnsTotal) * this.quantity;
  }

  addToOrder(): void {}
}

import { Component, Input, inject, DestroyRef, OnInit } from '@angular/core';
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
export class ProductInfoCardComponent implements OnInit {
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
    // Carga los adicionales al abrir, los actualiza cada REFRESH_MS y al volver
    // a la pestaña. Así si el admin cambia, agrega o desactiva un adicional,
    // se refleja sin recargar y sin perder lo que el cliente ya eligió.
    merge(timer(0, REFRESH_MS), fromEvent(document, 'visibilitychange'))
      .pipe(
        filter(() => document.visibilityState === 'visible'),
        switchMap(() => {
          // Se lee en cada ciclo por si el admin cambió la categoría del producto
          const categoryId = this.product.category?.id;

          if (!categoryId) {
            return of<AddOn[]>([]);
          }

          return this.addOnService.getActiveAddOnsByCategory(categoryId).pipe(
            catchError((error) => {
              console.error('Error cargando adicionales:', error);
              return EMPTY; // si falla, se quedan los que ya se veían
            }),
          );
        }),
        takeUntilDestroyed(this.destroyRef),
      )
      .subscribe((addOns) => {
        this.addons = addOns;
        this.syncSelectedAddOns();
      });
  }

  // Los adicionales elegidos se actualizan con los datos nuevos (precio,
  // nombre); si el admin desactivó alguno, se quita de la selección.
  private syncSelectedAddOns(): void {
    this.selectedAddOns = this.selectedAddOns
      .map((selected) => this.addons.find((addOn) => addOn.id === selected.id))
      .filter((addOn): addOn is AddOn => !!addOn);
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
    // Se vuelve a consultar el producto: pudo desactivarse mientras el cliente
    this.productService.getProductById(this.product.id).subscribe({
      next: (currentProduct) => {
        this.cartService.addItem(
          currentProduct,
          this.quantity,
          this.selectedAddOns,
        );

        this.selectedAddOns = [];
        this.quantity = 1;
        this.added = true;

        setTimeout(() => {
          this.added = false;
        }, 2500);
      },
      error: (error) => {
        console.error('Error validando producto:', error);
        this.router.navigate(['/error'], {
          replaceUrl: true,
          queryParams: {
            message:
              error.error?.message ?? 'Este producto ya no está disponible.',
          },
        });
      },
    });
  }
}

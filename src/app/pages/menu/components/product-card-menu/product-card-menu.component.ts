import { TranslatePipe } from '../../../../pipes/translate.pipe';
import { Component, Input, inject } from '@angular/core';
import { RouterLink } from '@angular/router';
import { DecimalPipe } from '@angular/common';
import { FeatureChipsComponent } from '../../../../components/feature-chips/feature-chips.component';
import { StarIconComponent } from '../../../../components/star-icon/star-icon.component';
import { Product } from '../../../../models/product';
import { CartService } from '../../../../services/cart.service';
import { ProductService } from '../../../../services/product.service';
import { ToastService } from '../../../../services/toast.service';
import { getErrorMessage, isNetworkOrServerError } from '../../../../utils/http-error';

// Tarjeta de un producto en la carta: imagen, insignias, nombre, descripción,
// características y precio. Al hacer clic lleva al detalle del producto.
// (Es distinta de app-product-card, que se usa en el inicio.)
@Component({
  selector: 'app-product-card-menu',
  standalone: true,
  imports: [DecimalPipe, FeatureChipsComponent, StarIconComponent, RouterLink, TranslatePipe],
  templateUrl: './product-card-menu.component.html',
  styleUrl: './product-card-menu.component.scss',
})
export class ProductCardMenuComponent {
  private cartService = inject(CartService);
  private productService = inject(ProductService);
  private toast = inject(ToastService);
  verifying = false;

  // Producto que muestra la tarjeta (obligatorio, lo pasa la carta)
  @Input() product!: Product;

  // El acceso rápido añade una unidad sin adicionales; el enlace de la tarjeta
  // lleva al detalle para elegir extras y cantidad.
  addToCart(): void {
    if (this.verifying) return;
    this.verifying = true;

    this.productService.getProductById(this.product.id).subscribe({
      next: (currentProduct) => {
        this.verifying = false;
        if (currentProduct.active === false) {
          this.toast.error('Producto no disponible', 'Este producto ya no está disponible.');
          return;
        }

        this.cartService.addItem(currentProduct, 1, []);
        this.toast.show('Añadido al carrito', `1× ${currentProduct.name}`, 5000, 'success', {
          label: 'Ver carrito',
          run: () => this.cartService.requestOpen(),
        });
      },
      error: (error) => {
        this.verifying = false;
        this.toast.error(
          'No se pudo añadir el producto',
          isNetworkOrServerError(error)
            ? 'No se pudo verificar el producto. Revisa tu conexión e intenta de nuevo.'
            : getErrorMessage(error, 'Este producto ya no está disponible.'),
        );
      },
    });
  }
}

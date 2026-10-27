import { Component, DestroyRef, Input, OnInit, inject } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { Router } from '@angular/router';
import { DecimalPipe } from '@angular/common';
import {
  EMPTY,
  catchError,
  filter,
  fromEvent,
  merge,
  of,
  switchMap,
  timer,
} from 'rxjs';
import { ProductDetail } from '../../../../models/product';
import { AddOnOption } from '../../../../models/addOn';
import { AuthService } from '../../../../services/auth.service';
import { CartService } from '../../../../services/cart.service';
import { ProductService } from '../../../../services/product.service';
import { AddOnService } from '../../../../services/addOn.service';
import { ButtonComponent } from '../../../../components/button/button.component';
import { FeatureChipsComponent } from '../../../../components/feature-chips/feature-chips.component';
import { StarIconComponent } from '../../../../components/star-icon/star-icon.component';
import { AddonOptionComponent } from '../addon-option/addon-option.component';
import { QuantitySelectorComponent } from '../quantity-selector/quantity-selector.component';
import { ToastService } from '../../../../services/toast.service';
import {
  getErrorMessage,
  isNetworkOrServerError,
} from '../../../../utils/http-error';

// Cada cuánto se vuelven a pedir los adicionales al servidor
const REFRESH_MS = 10_000;

/**
 * Tarjeta principal del detalle de un producto: imagen, datos, categoría,
 * adicionales opcionales, cantidad, total y botón para añadir al carrito.
 * Calcula el total (producto + adicionales) × cantidad y, antes de agregar
 * al carrito, vuelve a consultar el producto para confirmar que sigue
 * disponible y tomar su precio actual.
 */
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
  private productService = inject(ProductService);
  private addOnService = inject(AddOnService);
  private destroyRef = inject(DestroyRef);
  private toast = inject(ToastService);

  // Producto que muestra la tarjeta (lo pasa la página de detalle)
  @Input() product!: ProductDetail;

  // Adicionales activos de la categoría del producto (los que se pueden elegir)
  addons: AddOnOption[] = [];
  // Adicionales que el cliente eligió. Si elige el mismo dos veces, aparece
  // dos veces en la lista (una entrada por unidad).
  selectedAddOns: AddOnOption[] = [];

  // Cantidad de platos que quiere el cliente
  quantity = 1;

  // Evita agregar dos veces si se pulsa el botón mientras se verifica
  private verifying = false;

  ngOnInit(): void {
    // Carga los adicionales al abrir, los actualiza cada REFRESH_MS y al volver
    // a la pestaña. Así si el admin cambia, agrega o desactiva un adicional,
    // se refleja sin recargar y sin perder lo que el cliente ya eligió.
    merge(timer(0, REFRESH_MS), fromEvent(document, 'visibilitychange'))
      .pipe(
        // Solo se pide si la pestaña está visible
        filter(() => document.visibilityState === 'visible'),
        // switchMap cancela la petición anterior si llega una nueva antes de terminar
        switchMap(() => {
          // Se lee en cada ciclo por si el admin cambió la categoría del producto
          const categoryId = this.product.category?.id;

          // Sin categoría no hay adicionales que ofrecer
          if (!categoryId) {
            return of<AddOnOption[]>([]);
          }

          return this.addOnService.getActiveAddOnsByCategory(categoryId).pipe(
            catchError((error) => {
              console.error('Error cargando adicionales:', error);
              return EMPTY; // si falla, se quedan los que ya se veían
            }),
          );
        }),
        // Deja de actualizar cuando el componente se destruye
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
      .filter((addOn): addOn is AddOnOption => !!addOn);
  }

  // Cuántas unidades de este adicional hay seleccionadas
  addOnQuantity(addOn: AddOnOption): number {
    return this.selectedAddOns.filter((selected) => selected.id === addOn.id)
      .length;
  }

  // delta = +1 agrega una unidad, delta = -1 quita una
  changeAddOnQuantity(addOn: AddOnOption, delta: number): void {
    if (delta > 0) {
      this.selectedAddOns = [...this.selectedAddOns, addOn];
      return;
    }

    // Para quitar, se busca la primera unidad de ese adicional y se elimina solo esa
    const index = this.selectedAddOns.findIndex(
      (selected) => selected.id === addOn.id,
    );
    if (index !== -1) {
      this.selectedAddOns = this.selectedAddOns.filter((_, i) => i !== index);
    }
  }

  // Suma de los precios de todos los adicionales elegidos (para un plato)
  get addOnsTotal(): number {
    return this.selectedAddOns.reduce((total, addOn) => total + addOn.price, 0);
  }

  // Total a pagar: (precio del plato + adicionales) × cantidad
  get totalPrice(): number {
    return (this.product.price + this.addOnsTotal) * this.quantity;
  }

  // Botón "Añadir a la orden"
  addToOrder(): void {
    // Si ya hay una verificación en curso, se ignora el clic
    if (this.verifying) {
      return;
    }
    this.verifying = true;

    // Se vuelve a consultar el producto: pudo desactivarse mientras el cliente
    // lo miraba, y el precio pudo cambiar desde la última actualización.
    this.productService.getProductById(this.product.id).subscribe({
      next: (currentProduct) => {
        this.verifying = false;

        // El admin lo desactivó: no se agrega al pedido
        if (currentProduct.active === false) {
          this.showUnavailable('Este producto ya no está disponible.');
          return;
        }

        // Se agrega con los datos que tiene el servidor ahora (precio actual)
        const quantity = this.quantity;

        this.cartService.addItem(currentProduct, quantity, this.selectedAddOns);

        // Limpiar la selección después de agregar el producto
        this.selectedAddOns = [];
        this.quantity = 1;

        // Aviso de que se agregó al carrito
        this.toast.show(
          'Añadido al carrito',
          `${quantity}× ${currentProduct.name}`,
        );
      },
      error: (error) => {
        this.verifying = false;
        console.error('Error validando producto:', error);

        // Sin conexión o error del servidor: no se sabe si sigue disponible
        if (isNetworkOrServerError(error)) {
          this.showUnavailable(
            'No se pudo verificar el producto. Revisa tu conexión e intenta de nuevo.',
          );
          return;
        }

        // Si fue eliminado, el servidor no devuelve un mensaje legible
        this.showUnavailable(
          getErrorMessage(error, 'Este producto ya no está disponible.'),
        );
      },
    });
  }

  // Lleva a la página de error con un mensaje (replaceUrl evita que "atrás"
  // regrese a esta página)
  private showUnavailable(message: string): void {
    this.router.navigate(['/error'], {
      replaceUrl: true,
      queryParams: { message },
    });
  }
}

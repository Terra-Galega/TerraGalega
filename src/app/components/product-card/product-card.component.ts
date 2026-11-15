import { Component, Input, inject } from '@angular/core';
import { DecimalPipe } from '@angular/common';
import { Router, RouterLink } from '@angular/router';
import { Product } from '../../models/product';

/**
 * Tarjeta de un producto (imagen, categoría, nombre, precio y descripción).
 * Al hacer clic navega al detalle del producto. Se usa en el inicio
 * ("Favoritos de la casa") y en "También te puede gustar" del detalle.
 */
@Component({
  selector: 'app-product-card',
  imports: [DecimalPipe, RouterLink],
  templateUrl: './product-card.component.html',
  styleUrl: './product-card.component.scss',
})
export class ProductCardComponent {
  // Producto que muestra la tarjeta (obligatorio, lo pasa quien la usa)
  @Input() product!: Product;
  // false en las copias del carrusel animado del inicio (ocultas al lector de pantalla): así el
  // Tab no pasa dos veces por el mismo plato ni entra en contenido oculto
  @Input() focusable = true;
  router = inject(Router);

  // Lleva a la página de detalle del producto
  goToProductDetail(): void {
    this.router.navigate(['productDetails', this.product.id]);
  }
}

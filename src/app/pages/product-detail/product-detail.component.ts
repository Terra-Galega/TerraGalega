import { Component, inject, OnInit } from '@angular/core';
import { ActivatedRoute, Router } from '@angular/router';
import { NavbarComponent } from '../../components/navbar/navbar.component';
import { FooterComponent } from '../../components/footer/footer.component';
import { Product } from '../../models/product';
import { ProductService } from '../../services/product.service';
import { ProductInfoCardComponent } from './components/product-info-card/product-info-card.component';
import { ProductCardComponent } from '../../components/product-card/product-card.component';

@Component({
  selector: 'app-product-detail',
  standalone: true,
  imports: [
    NavbarComponent,
    ProductInfoCardComponent,
    ProductCardComponent,
    FooterComponent,
  ],
  templateUrl: './product-detail.component.html',
})
export class ProductDetailComponent implements OnInit {
  private route = inject(ActivatedRoute);
  private router = inject(Router);
  private productService = inject(ProductService);

  product: Product | undefined;

  relatedProducts: Product[] = [];

  ngOnInit(): void {
    const id = Number(this.route.snapshot.paramMap.get('id'));

    this.productService.getProductById(id).subscribe({
      next: (product) => {
        this.product = product;

        const categoryId = this.product.category?.id;

        if (categoryId) {
          this.productService.getActiveProducts().subscribe({
            next: (products) => {
              this.relatedProducts = products
                .filter(
                  (relatedProduct) =>
                    relatedProduct.id !== this.product!.id &&
                    relatedProduct.category?.id === categoryId,
                )
                .slice(0, 3);
            },
            error: (error) => {
              console.error('Error cargando productos relacionados:', error);
            },
          });
        }
      },
      error: (error) => {
        console.error('Error cargando producto:', error);
        this.router.navigate(['/menu']);
      },
    });
  }

  goBackToMenu(): void {
    this.router.navigate(['/menu']);
  }
}

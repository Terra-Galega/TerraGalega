import { Component } from '@angular/core';
import { Router } from '@angular/router';

import { AdminSidebarComponent } from './components/admin-sidebar/admin-sidebar.component';

import { AdminTopbarComponent } from './components/admin-topbar/admin-topbar.component';

import { AdminProductsComponent } from './components/admin-products/admin-products.component';

import { AdminOrdersComponent } from './components/admin-orders/admin-orders.component';

import { AdminUsersComponent } from './components/admin-users/admin-users.component';

import { AdminProductModalComponent } from './components/admin-product-modal/admin-product-modal.component';

import { Product } from '../../models/product';

@Component({
  selector: 'app-admin',

  imports: [
    AdminSidebarComponent,
    AdminTopbarComponent,
    AdminProductsComponent,
    AdminOrdersComponent,
    AdminUsersComponent,
    AdminProductModalComponent,
  ],

  templateUrl: './admin.component.html',
  styleUrl: './admin.component.scss',
})
export class AdminComponent {
  activeTab: 'products' | 'orders' | 'users' = 'products';

  productModalOpen = false;

  selectedProduct: Product | null = null;

  searchTerm = '';

  adminName = 'Administrador';

  constructor(private router: Router) {}

  changeTab(tab: 'products' | 'orders' | 'users') {
    this.activeTab = tab;
  }

  get pageTitle() {
    if (this.activeTab === 'products') {
      return 'Productos';
    }

    if (this.activeTab === 'orders') {
      return 'Ordenes';
    }

    return 'Usuarios';
  }

  searchProducts(term: string) {
    this.searchTerm = term;
  }

  openAddProduct() {
    this.selectedProduct = null;

    this.productModalOpen = true;
  }

  openEditProduct(product: Product) {
    this.selectedProduct = product;

    this.productModalOpen = true;
  }

  closeProductModal() {
    this.productModalOpen = false;

    this.selectedProduct = null;
  }

  logout() {
    localStorage.removeItem('currentUser');
    localStorage.removeItem('userType');

    this.router.navigate(['/login']);
  }
}

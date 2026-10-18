import { Component } from '@angular/core';
import { Router } from '@angular/router';
import { AuthService } from '../../services/auth.service';
import {
  AdminSidebarComponent,
  AdminTab,
} from './components/admin-sidebar/admin-sidebar.component';
import { AdminTopbarComponent } from './components/admin-topbar/admin-topbar.component';
import { AdminProductsComponent } from './components/admin-products/admin-products.component';
import { AdminOrdersComponent } from './components/admin-orders/admin-orders.component';
import { AdminUsersComponent } from './components/admin-users/admin-users.component';
import { AdminOperatorsComponent } from './components/admin-operators/admin-operators.component';
import { AdminProductModalComponent } from './components/admin-product-modal/admin-product-modal.component';
import { AdminCategoriesComponent } from './components/admin-categories/admin-categories.component';
import { AdminCategoryModalComponent } from './components/admin-category-modal/admin-category-modal.component';
import { AdminAddonsComponent } from './components/admin-addons/admin-addons.component';
import { AdminAddonModalComponent } from './components/admin-addon-modal/admin-addon-modal.component';
import { Product } from '../../models/product';
import { Category } from '../../models/category';
import { AddOn } from '../../models/addOn';

@Component({
  selector: 'app-admin',
  imports: [
    AdminSidebarComponent,
    AdminTopbarComponent,
    AdminProductsComponent,
    AdminOrdersComponent,
    AdminUsersComponent,
    AdminOperatorsComponent,
    AdminProductModalComponent,
    AdminCategoriesComponent,
    AdminCategoryModalComponent,
    AdminAddonsComponent,
    AdminAddonModalComponent,
  ],
  templateUrl: './admin.component.html',
  styleUrl: './admin.component.scss',
})
export class AdminComponent {
  activeTab: AdminTab = 'products';

  productModalOpen = false;
  selectedProduct: Product | null = null;

  categoryModalOpen = false;
  selectedCategory: Category | null = null;

  addonModalOpen = false;
  selectedAddon: AddOn | null = null;

  searchTerm = '';

  adminName = '';

  constructor(
    private router: Router,
    private auth: AuthService,
  ) {
    const session = this.auth.session;

    if (!session || session.type !== 'admin') {
      this.router.navigate(['/login']);
      return;
    }

    this.adminName = `${session.name} ${session.lastName}`.trim();
  }

  changeTab(tab: AdminTab) {
    this.activeTab = tab;
    this.searchTerm = '';
  }

  get pageTitle() {
    switch (this.activeTab) {
      case 'products':
        return 'Productos';
      case 'categories':
        return 'Categorías';
      case 'addons':
        return 'Adicionales';
      case 'orders':
        return 'Ordenes';
      case 'users':
        return 'Usuarios';
      case 'operators':
        return 'Operadores';
      default:
        return 'Usuarios';
    }
  }

  get showAddButton() {
    return (
      this.activeTab === 'products' ||
      this.activeTab === 'categories' ||
      this.activeTab === 'addons'
    );
  }

  get addLabel() {
    switch (this.activeTab) {
      case 'categories':
        return 'Añadir categoría';
      case 'addons':
        return 'Añadir adicional';
      default:
        return 'Añadir producto';
    }
  }

  searchProducts(term: string) {
    this.searchTerm = term;
  }

  // El botón "Añadir" de la barra abre el modal de la pestaña activa
  onAddClicked() {
    if (this.activeTab === 'products') {
      this.openAddProduct();
    } else if (this.activeTab === 'categories') {
      this.openAddCategory();
    } else if (this.activeTab === 'addons') {
      this.openAddAddon();
    }
  }

  // Productos
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

  // Categorías
  openAddCategory() {
    this.selectedCategory = null;
    this.categoryModalOpen = true;
  }

  openEditCategory(category: Category) {
    this.selectedCategory = category;
    this.categoryModalOpen = true;
  }

  closeCategoryModal() {
    this.categoryModalOpen = false;
    this.selectedCategory = null;
  }

  // Adicionales
  openAddAddon() {
    this.selectedAddon = null;
    this.addonModalOpen = true;
  }

  openEditAddon(addon: AddOn) {
    this.selectedAddon = addon;
    this.addonModalOpen = true;
  }

  closeAddonModal() {
    this.addonModalOpen = false;
    this.selectedAddon = null;
  }

  logout() {
    this.auth.logout();
    this.router.navigate(['/login']);
  }
}

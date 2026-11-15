import { Component, DestroyRef, inject } from '@angular/core';
import { Router } from '@angular/router';
import { AuthService } from '../../services/auth.service';
import { AdminDeliveryPersonsComponent } from './components/admin-delivery-persons/admin-delivery-persons.component';
import { AdminDeliveryPersonModalComponent } from './components/admin-delivery-person-modal/admin-delivery-person-modal.component';
import { DeliveryPerson } from '../../models/deliveryPerson';
import {
  AdminSidebarComponent,
  AdminTab,
} from './components/admin-sidebar/admin-sidebar.component';
import { AdminTopbarComponent } from './components/admin-topbar/admin-topbar.component';
import { AdminProductsComponent } from './components/admin-products/admin-products.component';
import { AdminOrdersComponent } from './components/admin-orders/admin-orders.component';
import { AdminUsersComponent } from './components/admin-users/admin-users.component';
import { AdminDashboardComponent } from './components/admin-dashboard/admin-dashboard.component';
import { AdminOperatorsComponent } from './components/admin-operators/admin-operators.component';
import { AdminProductModalComponent } from './components/admin-product-modal/admin-product-modal.component';
import { AdminCategoriesComponent } from './components/admin-categories/admin-categories.component';
import { AdminCategoryModalComponent } from './components/admin-category-modal/admin-category-modal.component';
import { AdminAddonsComponent } from './components/admin-addons/admin-addons.component';
import { AdminAddonModalComponent } from './components/admin-addon-modal/admin-addon-modal.component';
import { Product } from '../../models/product';
import { AdminCouponsComponent } from './components/admin-coupons/admin-coupons.component';
import { AdminCouponModalComponent } from './components/admin-coupon-modal/admin-coupon-modal.component';
import { Coupon } from '../../models/coupon'; 
import { AdminAnnouncementsComponent } from './components/admin-announcements/admin-announcements.component';
import { AdminAnnouncementModalComponent } from './components/admin-announcement-modal/admin-announcement-modal.component';
import { Announcement } from '../../models/announcement';
import { AdminReviewsComponent } from './components/admin-reviews/admin-reviews.component';
import { AdminOperatorModalComponent } from './components/admin-operator-modal/admin-operator-modal.component';
import { Operator } from '../../models/operator';
import { Category } from '../../models/category';
import { AddOn } from '../../models/addOn';
import { OperatorService } from '../../services/operator.service';
import { DeliveryPersonService } from '../../services/delivery-person.service';
import { ChangePasswordModalComponent } from '../../components/change-password-modal/change-password-modal.component';
import { ToastService } from '../../services/toast.service';
import { TutorialFabComponent } from '../../components/tutorial-fab/tutorial-fab.component';
import { TutorialService } from '../../services/Tutorial.service';

// Componente principal del panel de administración.
@Component({
  selector: 'app-admin',
  // Componentes utilizados para mostrar las secciones y sus respectivos modales.
  imports: [
    AdminSidebarComponent,
    AdminDashboardComponent,
    AdminTopbarComponent,
    AdminProductsComponent,
    AdminOrdersComponent,
    AdminUsersComponent,
    AdminOperatorsComponent,
    AdminDeliveryPersonsComponent,
    AdminDeliveryPersonModalComponent,
    AdminProductModalComponent,
    AdminCategoriesComponent,
    AdminCategoryModalComponent,
    AdminCouponsComponent,
    AdminCouponModalComponent,
    AdminAnnouncementsComponent,
    AdminAnnouncementModalComponent,
    AdminReviewsComponent,
    AdminAddonsComponent,
    AdminAddonModalComponent,
    AdminOperatorModalComponent,
    ChangePasswordModalComponent,
    TutorialFabComponent,
  ],
  templateUrl: './admin.component.html',
})
export class AdminComponent {
  // Servicios encargados de gestionar operadores y domiciliarios.
  private operatorService = inject(OperatorService);
  private deliveryPersonService = inject(DeliveryPersonService);
  private tutorial = inject(TutorialService);
  private destroyRef = inject(DestroyRef);
  private tutorialStarted = false;

  // Usuarios seleccionados para cambiar sus contraseñas.
  passwordOperator: Operator | null = null;
  passwordDeliveryPerson: DeliveryPerson | null = null;

  // Función que solicita el cambio de contraseña de un operador.
  saveOperatorPassword = (current: string, next: string) =>
    this.operatorService.changePassword(
      this.passwordOperator!.id,
      current,
      next,
    );

  // Función que solicita el cambio de contraseña de un domiciliario.
  saveDeliveryPersonPassword = (current: string, next: string) =>
    this.deliveryPersonService.changePassword(
      this.passwordDeliveryPerson!.id,
      current,
      next,
    );

  // Pestaña que se muestra actualmente en el panel.
  activeTab: AdminTab = 'dashboard';

  // Control y selección de elementos para los modales de productos.
  productModalOpen = false;
  selectedProduct: Product | null = null;

  // Control y selección de elementos para los modales de categorías.
  categoryModalOpen = false;
  selectedCategory: Category | null = null;

  // Control y selección de elementos para los modales de adicionales.
  addonModalOpen = false;
  selectedAddon: AddOn | null = null;

  // Control y selección de elementos para los modales de cupones.
  couponModalOpen = false;
  selectedCoupon: Coupon | null = null;

  // Control y selección de elementos para los modales de anuncios.
  announcementModalOpen = false;
  selectedAnnouncement: Announcement | null = null;

  // Control y selección de elementos para los modales de domiciliarios.
  deliveryModalOpen = false;
  selectedDeliveryPerson: DeliveryPerson | null = null;

  // Texto utilizado para filtrar los elementos de las secciones.
  searchTerm = '';

  // Nombre completo del administrador autenticado.
  adminName = '';

  // Verifica que exista una sesión de administrador al inicializar el componente.
  constructor(
    private router: Router,
    private auth: AuthService,
  ) {
    const session = this.auth.session;

    // Si no hay sesión válida de administrador, redirige al inicio de sesión.
    if (!session || session.type !== 'admin') {
      this.router.navigate(['/login']);
      return;
    }

    // Obtiene el nombre completo para mostrarlo en el panel.
    this.adminName = `${session.name} ${session.lastName}`.trim();
  }

  // Cambia la pestaña activa y limpia el filtro de búsqueda.
  changeTab(tab: AdminTab) {
    this.activeTab = tab;
    this.searchTerm = '';
  }

  startAdminTutorial(): void {
    if (!this.tutorialStarted) {
      this.tutorialStarted = true;
      this.destroyRef.onDestroy(() => this.tutorial.stop());
    }

    this.changeTab('dashboard');
    this.tutorial.start('admin', () => {
      if (this.productModalOpen && !this.selectedProduct) this.closeProductModal();
    });
  }

  // Devuelve el título correspondiente a la pestaña seleccionada.
  get pageTitle() {
    switch (this.activeTab) {
      case 'dashboard':
        return 'Dashboard';
      case 'products':
        return 'Productos';
      case 'categories':
        return 'Categorías';
      case 'addons':
        return 'Adicionales';
      case 'coupons':
        return 'Cupones';
      case 'announcements':
        return 'Anuncios';
      case 'reviews':
        return 'Reseñas';
      case 'orders':
        return 'Ordenes';
      case 'users':
        return 'Usuarios';
      case 'operators':
        return 'Operadores';
      case 'deliveries':
        return 'Domiciliarios';
      default:
        return 'Usuarios';
    }
  }

  // Determina en qué pestañas debe mostrarse el botón para añadir elementos.
  get showAddButton() {
    return (
      this.activeTab === 'products' ||
      this.activeTab === 'categories' ||
      this.activeTab === 'addons' ||
      this.activeTab === 'coupons' ||
      this.activeTab === 'announcements' ||
      this.activeTab === 'deliveries' ||
      this.activeTab === 'operators'
    );
  }

  // Devuelve el texto del botón de añadir según la pestaña activa.
  get addLabel() {
    switch (this.activeTab) {
      case 'categories':
        return 'Añadir categoría';
      case 'addons':
        return 'Añadir adicional';
      case 'coupons':
        return 'Añadir cupón';
      case 'announcements':
        return 'Añadir anuncio';
      case 'deliveries':
        return 'Añadir domiciliario';
      case 'operators':
        return 'Añadir operador';
      default:
        return 'Añadir producto';
    }
  }

  // Actualiza el término utilizado para filtrar los resultados.
  searchProducts(term: string) {
    this.searchTerm = term;
  }

  // Abre el modal de creación correspondiente a la pestaña activa.
  onAddClicked() {
    if (this.activeTab === 'products') {
      this.openAddProduct();
    } else if (this.activeTab === 'categories') {
      this.openAddCategory();
    } else if (this.activeTab === 'addons') {
      this.openAddAddon();
    } else if (this.activeTab === 'coupons') {
      this.openAddCoupon();  
    } else if (this.activeTab === 'announcements')  {
      this.openAddAnnouncement();
    } else if (this.activeTab === 'deliveries') {
      this.openAddDeliveryPerson();
    } else if (this.activeTab === 'operators') {
      this.openAddOperator();
    }
  }

  // Productos

  // Prepara el modal para registrar un producto nuevo.
  openAddProduct() {
    this.selectedProduct = null;
    this.productModalOpen = true;
  }

  // Prepara el modal con los datos del producto seleccionado para editarlo.
  openEditProduct(product: Product) {
    this.selectedProduct = product;
    this.productModalOpen = true;
  }

  // Cierra el modal de productos y limpia la selección.
  closeProductModal() {
    this.productModalOpen = false;
    this.selectedProduct = null;
  }

  // Categorías

  // Prepara el modal para crear una categoría.
  openAddCategory() {
    this.selectedCategory = null;
    this.categoryModalOpen = true;
  }

  // Abre el modal con los datos de la categoría seleccionada.
  openEditCategory(category: Category) {
    this.selectedCategory = category;
    this.categoryModalOpen = true;
  }

  // Cierra el modal de categorías y limpia la selección.
  closeCategoryModal() {
    this.categoryModalOpen = false;
    this.selectedCategory = null;
  }

  // Adicionales

  // Prepara el modal para registrar un adicional nuevo.
  openAddAddon() {
    this.selectedAddon = null;
    this.addonModalOpen = true;
  }

  // Abre el modal con los datos del adicional seleccionado.
  openEditAddon(addon: AddOn) {
    this.selectedAddon = addon;
    this.addonModalOpen = true;
  }

  // Cierra el modal de adicionales y limpia la selección.
  closeAddonModal() {
    this.addonModalOpen = false;
    this.selectedAddon = null;
  }

    // Cupones

  // Prepara el modal para registrar un cupón nuevo.
  openAddCoupon() {
    this.selectedCoupon = null;
    this.couponModalOpen = true;
  }

  // Abre el modal con los datos del cupón seleccionado.
  openEditCoupon(coupon: Coupon) {
    this.selectedCoupon = coupon;
    this.couponModalOpen = true;
  }

  // Cierra el modal de cupones y limpia la selección.
  closeCouponModal() {
    this.couponModalOpen = false;
    this.selectedCoupon = null;
  }

  // Anuncios

  // Prepara el modal para crear un anuncio nuevo.
  openAddAnnouncement() {
    this.selectedAnnouncement = null;
    this.announcementModalOpen = true;
  }

  // Abre el modal con los datos del anuncio seleccionado.
  openEditAnnouncement(announcement: Announcement) {
    this.selectedAnnouncement = announcement;
    this.announcementModalOpen = true;
  }

  // Cierra el modal de anuncios y limpia la selección.
  closeAnnouncementModal() {
    this.announcementModalOpen = false;
    this.selectedAnnouncement = null;
  }

  // Domiciliarios

  // Prepara el modal para registrar un domiciliario nuevo.
  openAddDeliveryPerson() {
    this.selectedDeliveryPerson = null;
    this.deliveryModalOpen = true;
  }

  // Abre el modal con los datos del domiciliario seleccionado.
  openEditDeliveryPerson(deliveryPerson: DeliveryPerson) {
    this.selectedDeliveryPerson = deliveryPerson;
    this.deliveryModalOpen = true;
  }

  // Cierra el modal de domiciliarios y limpia la selección.
  closeDeliveryModal() {
    this.deliveryModalOpen = false;
    this.selectedDeliveryPerson = null;
  }

  // Finaliza la sesión del administrador y lo redirige al inicio de sesión.
  logout() {
    this.auth.logout();
    this.router.navigate(['/login']);
  }

  // Operadores

  // Control y selección de elementos para los modales de operadores.
  operatorModalOpen = false;
  selectedOperator: Operator | null = null;

  // Prepara el modal para registrar un operador nuevo.
  openAddOperator() {
    this.selectedOperator = null;
    this.operatorModalOpen = true;
  }

  // Abre el modal con los datos del operador seleccionado.
  openEditOperator(operator: Operator) {
    this.selectedOperator = operator;
    this.operatorModalOpen = true;
  }

  // Cierra el modal de operadores y limpia la selección.
  closeOperatorModal() {
    this.operatorModalOpen = false;
    this.selectedOperator = null;
  }

  // Cierra el modal de edición y prepara el cambio de contraseña del operador.
  openOperatorPassword() {
    this.passwordOperator = this.selectedOperator;
    this.closeOperatorModal();
  }

  // Cierra el modal de edición y prepara el cambio de contraseña del domiciliario.
  openDeliveryPersonPassword() {
    this.passwordDeliveryPerson = this.selectedDeliveryPerson;
    this.closeDeliveryModal();
  }
}

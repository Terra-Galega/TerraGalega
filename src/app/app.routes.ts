import { Routes } from '@angular/router';
import { HomeComponent } from './pages/home/home.component';
import { ContactComponent } from './pages/contact/contact.component';
import { OrdersComponent } from './pages/orders/orders.component';
import { LoginComponent } from './pages/login/login.component';
import { MenuComponent } from './pages/menu/menu.component';
import { AdminComponent } from './pages/admin/admin.component';
import { OperatorComponent } from './pages/operator/operator.component';
import { AccountComponent } from './pages/account/account.component';
import { AccountEditComponent } from './pages/account-edit/account-edit.component';
import { ProductDetailComponent } from './pages/product-detail/product-detail.component';
import { AboutUsComponent } from './pages/about-us/about-us.component';
import { CheckoutComponent } from './pages/checkout/checkout.component';
import { ErrorComponent } from './pages/error/error.component';
import {
  adminGuard,
  clientAreaGuard,
  operatorGuard,
} from './guards/role.guards';
import { CheckoutConfirmComponent } from './pages/checkout/chekout-confirm/checkout-confirm.component';

export const routes: Routes = [
  {
    // Ruta raíz ('/'): muestra la página de inicio.
    path: '',
    component: HomeComponent,

    // Solo accesible si el guard de zona cliente lo permite.
    canActivate: [clientAreaGuard],
  },
  {
    // Página "Sobre nosotros".
    path: 'aboutUs',
    component: AboutUsComponent,
    canActivate: [clientAreaGuard],
  },
  {
    // Catálogo / menú de productos.
    path: 'menu',
    component: MenuComponent,
    canActivate: [clientAreaGuard],
  },
  {
    // Página de contacto.
    path: 'contact',
    component: ContactComponent,
    canActivate: [clientAreaGuard],
  },
  {
    // Alias de la ruta raíz: también muestra la página de inicio.
    path: 'home',
    component: HomeComponent,
    canActivate: [clientAreaGuard],
  },
  {
    // Historial de pedidos del cliente.
    path: 'orders',
    component: OrdersComponent,
    canActivate: [clientAreaGuard],
  },
  {
    // Paso de compra: carrito y datos de envío/pago.
    path: 'checkout',
    component: CheckoutComponent,
    canActivate: [clientAreaGuard],
  },
  {
    // Pantalla de confirmación del pedido tras finalizar la compra.
    path: 'checkout/confirm',
    component: CheckoutConfirmComponent,
    canActivate: [clientAreaGuard],
  },
  {
    // Inicio de sesión (clientes, operadores y administradores).
    path: 'login',
    component: LoginComponent,
    canActivate: [clientAreaGuard],
  },
  {
    // Panel de administración; ':id' es un parámetro dinámico con el identificador del administrador.
    path: 'admin/:id',
    component: AdminComponent,
    // Solo pueden entrar usuarios con rol de administrador.
    canActivate: [adminGuard],
  },
  {
    // Panel del operador; ':id' es el identificador del operador autenticado.
    path: 'operator/:id',
    component: OperatorComponent,
    // Solo pueden entrar usuarios con rol de operador.
    canActivate: [operatorGuard],
  },
  {
    // Perfil de la cuenta del cliente; ':id' identifica al cliente.
    path: 'account/:id',
    component: AccountComponent,
    canActivate: [clientAreaGuard],
  },
  {
    // Edición de los datos de la cuenta del cliente.
    path: 'account/:id/edit',
    component: AccountEditComponent,
    canActivate: [clientAreaGuard],
  },
  {
    // Detalle de un producto; ':id' es el identificador del producto.
    path: 'productDetails/:id',
    component: ProductDetailComponent,
    canActivate: [clientAreaGuard],
  },
  {
    // Página de error genérica; recibe estado y mensaje mediante parámetros de consulta (queryParams).
    path: 'error',
    component: ErrorComponent,
  },
  {
    // Ruta comodín: captura cualquier URL no definida arriba (debe ir siempre al final).
    path: '**',
    component: ErrorComponent,
    // Datos estáticos que la página de error lee para mostrar el código 404 y su mensaje.
    data: { status: 404, message: 'La página que buscas no existe.' },
  },
];

import { Routes } from '@angular/router';
import { HomeComponent } from './pages/home/home.component';
import { ErrorComponent } from './pages/error/error.component';
import {
  adminGuard,
  clientAreaGuard,
  clientGuard,
  completeProfileGuard,
  operatorGuard,
  redirectByRoleGuard,
} from './guards/role.guards';

// Las rutas del usuario no llevan su id: el back lo identifica por el token,
// así nadie puede cambiar un número en la URL para ver la cuenta de otra persona.
export const routes: Routes = [
  {
    // Ruta raíz: con sesión redirige a la página del rol; sin sesión muestra el inicio.
    path: '',
    component: HomeComponent,
    canActivate: [redirectByRoleGuard],
  },
  {
    // Alias de la raíz, con la misma regla.
    path: 'home',
    component: HomeComponent,
    canActivate: [clientAreaGuard],
  },
  {
    // Login: con sesión iniciada se va a la página del rol.
    path: 'login',
    loadComponent: () => import('./pages/login/login.component').then((m) => m.LoginComponent),
    canActivate: [redirectByRoleGuard],
  },
  {
    // Destino del enlace del correo de confirmación: /verify-email?token=...
    // Sin guard: debe abrir igual con o sin sesión iniciada.
    path: 'verify-email',
    loadComponent: () => import('./pages/verify-email/verify-email.component').then((m) => m.VerifyEmailComponent),
  },
  {
    // Después de crear la cuenta con Google: pide teléfono, apellido y dirección
    path: 'complete-profile',
    loadComponent: () => import('./pages/complete-profile/complete-profile.component').then((m) => m.CompleteProfileComponent),
    canActivate: [completeProfileGuard],
  },
  {
    // Página "Sobre nosotros".
    path: 'aboutUs',
    loadComponent: () => import('./pages/about-us/about-us.component').then((m) => m.AboutUsComponent),
    canActivate: [clientAreaGuard],
  },
  {
    // Catálogo / menú de productos.
    path: 'menu',
    loadComponent: () => import('./pages/menu/menu.component').then((m) => m.MenuComponent),
    canActivate: [clientAreaGuard],
  },
  {
    // Página de reservas.
    // Carga diferida: el plano es pesado y no hace falta en la carga inicial.
    path: 'reservations',
    loadComponent: () =>
      import('./pages/reservations/reservations.component').then((m) => m.ReservationsComponent),
    canActivate: [clientAreaGuard],
  },
  {
    // Vuelta desde Stripe tras pagar el anticipo: /reservations/result?reservation=ID[&canceled=true]
    path: 'reservations/result',
    loadComponent: () =>
      import('./pages/reservations/reservation-result/reservation-result.component').then(
        (m) => m.ReservationResultComponent,
      ),
    canActivate: [clientAreaGuard],
  },
  {
    // Historial de pedidos del cliente.
    path: 'orders',
    loadComponent: () => import('./pages/orders/orders.component').then((m) => m.OrdersComponent),
    canActivate: [clientAreaGuard],
  },
  {
    // Carrito y datos de envío/pago.
    path: 'checkout',
    loadComponent: () => import('./pages/checkout/checkout.component').then((m) => m.CheckoutComponent),
    canActivate: [clientAreaGuard],
  },
  {
    // Confirmación del pedido tras finalizar la compra.
    path: 'checkout/confirm',
    loadComponent: () => import('./pages/checkout/chekout-confirm/checkout-confirm.component').then((m) => m.CheckoutConfirmComponent),
    canActivate: [clientAreaGuard],
  },
  {
    // Vuelta desde Stripe: /checkout/result?order=ID[&canceled=true]
    path: 'checkout/result',
    loadComponent: () => import('./pages/checkout/checkout-result/checkout-result.component').then((m) => m.CheckoutResultComponent),
    canActivate: [clientAreaGuard],
  },
  {
    // Detalle de un producto; ':id' es el id del producto.
    path: 'productDetails/:id',
    loadComponent: () => import('./pages/product-detail/product-detail.component').then((m) => m.ProductDetailComponent),
    canActivate: [clientAreaGuard],
  },
  {
    // Cupones personales del cliente.
    path: 'coupons',
    loadComponent: () => import('./pages/coupons/coupons.component').then((m) => m.CouponsComponent),
    canActivate: [clientGuard],
  },
  {
    // Cuenta del cliente con sesión iniciada.
    path: 'account',
    loadComponent: () => import('./pages/account/account.component').then((m) => m.AccountComponent),
    canActivate: [clientGuard],
  },
  {
    // Edición de los datos de la cuenta del cliente.
    path: 'account/edit',
    loadComponent: () => import('./pages/account-edit/account-edit.component').then((m) => m.AccountEditComponent),
    canActivate: [clientGuard],
  },
  {
    // Panel de administración. Carga diferida: el panel (con Chart.js)
    // solo se descarga cuando entra un administrador.
    path: 'admin',
    loadComponent: () =>
      import('./pages/admin/admin.component').then((m) => m.AdminComponent),
    canActivate: [adminGuard],
  },
  {
    // Panel del operador. Carga diferida.
    path: 'operator',
    loadComponent: () =>
      import('./pages/operator/operator.component').then(
        (m) => m.OperatorComponent,
      ),
    canActivate: [operatorGuard],
  },
  {
    // Página de error genérica; recibe estado y mensaje por queryParams.
    path: 'error',
    component: ErrorComponent,
  },
  {
    // Ruta comodín: cualquier URL no definida arriba (debe ir siempre al final).
    path: '**',
    component: ErrorComponent,
    data: { status: 404, message: 'La página que buscas no existe.' },
  },
];

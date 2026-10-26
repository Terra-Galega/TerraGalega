import { Routes } from '@angular/router';
import { HomeComponent } from './pages/home/home.component';
import { ContactComponent } from './pages/contact/contact.component';
import { OrdersComponent } from './pages/orders/orders.component';
import { LoginComponent } from './pages/login/login.component';
import { MenuComponent } from './pages/menu/menu.component';
import { AdminComponent } from './pages/admin/admin.component';
import {OperatorComponent} from './pages/operator/operator.component';
import { AccountComponent } from './pages/account/account.component';
import { AccountEditComponent } from './pages/account-edit/account-edit.component';
import { ProductDetailComponent } from './pages/product-detail/product-detail.component';
import { AboutUsComponent } from './pages/about-us/about-us.component';
import { CheckoutComponent } from './pages/checkout/checkout.component';
import { ErrorComponent } from './pages/error/error.component';
import { adminGuard, clientAreaGuard, operatorGuard } from './guards/role.guards';
import { CheckoutConfirmComponent } from './pages/checkout/checkout-confirm.component';

export const routes: Routes = [
  {
    path: '',
    component: HomeComponent,

    canActivate: [clientAreaGuard],
  },
  {
    path: 'aboutUs',
    component: AboutUsComponent,
    canActivate: [clientAreaGuard],
  },
  {
    path: 'menu',
    component: MenuComponent,
    canActivate: [clientAreaGuard],
  },
  {
    path: 'contact',
    component: ContactComponent,
    canActivate: [clientAreaGuard],
  },
  {
    path: 'home',
    component: HomeComponent,
    canActivate: [clientAreaGuard],
  },
  {
    path: 'orders',
    component: OrdersComponent,
    canActivate: [clientAreaGuard],
  },
  {
    path: 'checkout',
    component: CheckoutComponent,
    canActivate: [clientAreaGuard],
  },
  {
    path: 'checkout/confirm',
    component: CheckoutConfirmComponent,
    canActivate: [clientAreaGuard],
  },
  {
    path: 'login',
    component: LoginComponent,
    canActivate: [clientAreaGuard],
  },
  {
    path: 'admin/:id',
    component: AdminComponent,
    canActivate: [adminGuard],
  },
  {
    path: 'operator/:id',
    component: OperatorComponent,
    canActivate: [operatorGuard],
  },
  {
    path: 'account/:id',
    component: AccountComponent,
    canActivate: [clientAreaGuard],
  },
  {
    path: 'account/:id/edit',
    component: AccountEditComponent,
    canActivate: [clientAreaGuard],
  },
  {
    path: 'productDetails/:id',
    component: ProductDetailComponent,
    canActivate: [clientAreaGuard],
  },
  {
    path: 'error',
    component: ErrorComponent,
  },
  {
    path: '**',
    component: ErrorComponent,
    data: { message: 'La página que buscas no existe.' },
  },

];
import { NavigationEnd, Router } from '@angular/router';
import { CartComponent } from '../cart/cart.component';
import { Component, HostListener, OnDestroy } from '@angular/core';
import { filter, Subscription } from 'rxjs';
import { AuthService } from '../../services/auth.service';
import { CartService } from '../../services/cart.service';
@Component({
  selector: 'app-navbar',
  standalone: true,
  imports: [CartComponent],
  templateUrl: './navbar.component.html',
  styleUrl: './navbar.component.scss',
})
export class NavbarComponent implements OnDestroy {
  // =========================
  // MENÚ MOBILE
  // =========================

  mobileMenuOpen = false;

  // =========================
  // NAVBAR
  // =========================

  navBarStayed = false;

  // =========================
  // AUTENTICACIÓN
  // =========================

  isLoggedIn = false;
  isLoggedAdmin = false;
  isClient = false;
  isOperator = false;

  username = '';
  userId: number | null = null;

  // =========================
  // CARRITO
  // =========================

  cartCount = 0;

  private sub: Subscription;
  private cartSub: Subscription;
  private routerSub: Subscription;

  constructor(
    private router: Router,
    private auth: AuthService,
    private cartService: CartService,
  ) {
    this.updateNavbar();

    this.sub = this.auth.session$.subscribe((session) => {
      this.isLoggedIn = session !== null;
      this.isLoggedAdmin = session?.type === 'admin';
      this.isClient = session?.type === 'client';
      this.isOperator = session?.type === 'operator';
      this.username = session
        ? `${session.name} ${session.lastName}`.trim()
        : '';
      this.userId = session?.id ?? null;
    });

    this.cartSub = this.cartService.items$.subscribe(() => {
      this.cartCount = this.cartService.count;
    });

    // Al construirse, router.url aún puede ser '/' (la navegación no terminó).
    // Recalculamos el estado cuando la navegación realmente termina.
    this.routerSub = this.router.events
      .pipe(filter((event) => event instanceof NavigationEnd))
      .subscribe(() => this.updateNavbar());
  }
  // =========================
  // NAVEGACIÓN
  // =========================

  goToHome(): void {
    this.router.navigate(['/home']);
    this.navBarStayed = true;
    this.closeMobileMenu();
  }

  goToAboutUs(): void {
    this.router.navigate(['/aboutUs']);
    this.navBarStayed = true;
    this.closeMobileMenu();
  }

  goToMenu(): void {
    this.router.navigate(['/menu']);
    this.navBarStayed = true;
    this.closeMobileMenu();
  }

  goToContact(): void {
    this.router.navigate(['/contact']);
    this.navBarStayed = true;
    this.closeMobileMenu();
  }

  goToOrders(): void {
    this.router.navigate(['/orders']);
    this.navBarStayed = true;
    this.closeMobileMenu();
  }

  goToLogin(): void {
    this.router.navigate(['/login']);
    this.navBarStayed = true;
    this.closeMobileMenu();
  }

  goToProfile(): void {
    if (this.userId === null) {
      return;
    }

    if (this.isLoggedAdmin) {
      this.router.navigate(['/admin', this.userId]);
      this.navBarStayed = true;
    } else {
      this.router.navigate(['/account', this.userId]);
      this.navBarStayed = true;
    }

    this.closeMobileMenu();
  }

  // =========================
  // NAVBAR / SCROLL
  // =========================

  @HostListener('window:scroll')
  onWindowScroll(): void {
    this.updateNavbar();
  }

  private updateNavbar(): void {
    const currentUrl = this.router.url.split('?')[0].split('#')[0];

    const isHome = currentUrl === '/home' || currentUrl === '/';

    if (isHome) {
      this.navBarStayed = window.scrollY > 20;
    } else {
      this.navBarStayed = true;
    }
  }

  // =========================
  // MENÚ MOBILE
  // =========================

  toggleMobileMenu(): void {
    this.mobileMenuOpen = !this.mobileMenuOpen;
  }

  closeMobileMenu(): void {
    this.mobileMenuOpen = false;
  }

  // =========================
  // HERO
  // =========================

  scrollToHero(): void {
    document.getElementById('hero')?.scrollIntoView({
      behavior: 'smooth',
    });

    this.closeMobileMenu();
  }

  cartOpen = false;

  openCart() {
    this.cartOpen = true;
  }

  closeCart() {
    this.cartOpen = false;
  }

  // =========================
  // USUARIO
  // =========================

  get fullName(): string {
    return this.username.toUpperCase();
  }

  ngOnDestroy(): void {
    this.sub.unsubscribe();
    this.cartSub.unsubscribe();
    this.routerSub.unsubscribe();
  }

  logout(): void {
    this.auth.logout();
    this.closeMobileMenu();
    this.router.navigate(['/login']);
  }
}
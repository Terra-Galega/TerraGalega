import { Component, HostListener } from '@angular/core';
import { Router } from '@angular/router';

@Component({
  selector: 'app-navbar',
  standalone: true,
  imports: [],
  templateUrl: './navbar.component.html',
  styleUrl: './navbar.component.scss',
})
export class NavbarComponent {

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

  username = '';
  userId: number | null = null;

  // =========================
  // CARRITO
  // =========================

  cartCount = 0;

  constructor(private router: Router) {}

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

    const isHome =
      currentUrl === '/home' ||
      currentUrl === '/';

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
      behavior: 'smooth'
    });

    this.closeMobileMenu();
  }

  // =========================
  // CARRITO
  // =========================

  openCart(): void {
    this.closeMobileMenu();

    console.log('Abrir carrito');
  }

  // =========================
  // USUARIO
  // =========================

  get fullName(): string {
    return this.username.toUpperCase();
  }
}
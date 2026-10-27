import { NavigationEnd, Router } from '@angular/router';
import { CartComponent } from '../cart/cart.component';
import { Component, HostListener, OnDestroy } from '@angular/core';
import { filter, Subscription } from 'rxjs';
import { AuthService } from '../../services/auth.service';
import { CartService } from '../../services/cart.service';
// Barra de navegación principal: enlaces, sesión del usuario, carrito y menú móvil.
@Component({
  selector: 'app-navbar',
  standalone: true,
  // Importa el carrito, que se muestra como panel desde la barra.
  imports: [CartComponent],
  templateUrl: './navbar.component.html',
  styleUrl: './navbar.component.scss',
})
export class NavbarComponent implements OnDestroy {
  // =========================
  // MENÚ MOBILE
  // =========================

  // Indica si el menú desplegable de móvil está abierto.
  mobileMenuOpen = false;

  // =========================
  // NAVBAR
  // =========================

  // Si es true, la barra se muestra con fondo de vidrio; si es false, es transparente sobre el hero.
  navBarStayed = false;

  // =========================
  // AUTENTICACIÓN
  // =========================

  // Indicadores de sesión: si hay usuario autenticado y de qué tipo es (admin, cliente u operador).
  isLoggedIn = false;
  isLoggedAdmin = false;
  isClient = false;
  isOperator = false;

  // Nombre completo y identificador del usuario autenticado; se usan para mostrarlo y para ir a su perfil.
  username = '';
  userId: number | null = null;

  // =========================
  // CARRITO
  // =========================

  // Cantidad de productos del carrito que se muestra en la insignia.
  cartCount = 0;

  // Suscripciones guardadas para poder cancelarlas en ngOnDestroy y evitar fugas de memoria.
  private sub: Subscription;
  private cartSub: Subscription;
  private routerSub: Subscription;

  // Inyecta el router, el servicio de autenticación y el del carrito.
  constructor(
    private router: Router,
    private auth: AuthService,
    private cartService: CartService,
  ) {
    // Calcula el aspecto inicial de la barra según la ruta actual.
    this.updateNavbar();

    // Actualiza los datos de sesión cada vez que cambian (inicio o cierre de sesión).
    this.sub = this.auth.session$.subscribe((session) => {
      this.isLoggedIn = session !== null;
      // Deduce el rol del usuario a partir del tipo de sesión.
      this.isLoggedAdmin = session?.type === 'admin';
      this.isClient = session?.type === 'client';
      this.isOperator = session?.type === 'operator';
      // Une nombre y apellido sin espacios sobrantes; queda vacío si no hay sesión.
      this.username = session
        ? `${session.name} ${session.lastName}`.trim()
        : '';
      this.userId = session?.id ?? null;
    });

    // Refresca el contador cada vez que cambian los productos del carrito.
    this.cartSub = this.cartService.items$.subscribe(() => {
      this.cartCount = this.cartService.count;
    });

    // Al construirse, router.url aún puede ser '/' (la navegación no terminó).
    // Recalculamos el estado cuando la navegación realmente termina.
    this.routerSub = this.router.events
      // Solo reacciona cuando una navegación termina, ignorando el resto de eventos del router.
      .pipe(filter((event) => event instanceof NavigationEnd))
      .subscribe(() => this.updateNavbar());
  }
  // =========================
  // NAVEGACIÓN
  // =========================

  // Los métodos goTo... navegan a su página, dejan la barra en modo vidrio y cierran el menú móvil.
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

  // Lleva al perfil que corresponde al rol del usuario: panel de admin, panel de operador o cuenta de cliente.
  goToProfile(): void {
    // Sin sesión no hay perfil al que ir, así que no hace nada.
    if (this.userId === null) {
      return;
    }

    if (this.isLoggedAdmin) {
      this.router.navigate(['/admin', this.userId]);
      this.navBarStayed = true;
    } else if (this.isOperator) {
      this.router.navigate(['/operator', this.userId]);
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

  // Escucha el scroll de la ventana para actualizar el aspecto de la barra.
  @HostListener('window:scroll')
  onWindowScroll(): void {
    this.updateNavbar();
  }

  // Decide si la barra se muestra transparente o en modo vidrio.
  private updateNavbar(): void {
    // Obtiene la ruta actual sin parámetros de consulta ni fragmento.
    const currentUrl = this.router.url.split('?')[0].split('#')[0];

    // Considera como inicio tanto '/home' como '/'.
    const isHome = currentUrl === '/home' || currentUrl === '/';

    // En inicio, el modo vidrio solo se activa tras bajar más de 20 px; en las demás páginas está siempre activo.
    if (isHome) {
      this.navBarStayed = window.scrollY > 20;
    } else {
      this.navBarStayed = true;
    }
  }

  // =========================
  // MENÚ MOBILE
  // =========================

  // Abre el menú móvil si está cerrado y lo cierra si está abierto.
  toggleMobileMenu(): void {
    this.mobileMenuOpen = !this.mobileMenuOpen;
  }

  // Cierra el menú móvil.
  closeMobileMenu(): void {
    this.mobileMenuOpen = false;
  }

  // =========================
  // HERO
  // =========================

  // Desplaza suavemente hasta el elemento con id 'hero' y cierra el menú móvil.
  scrollToHero(): void {
    document.getElementById('hero')?.scrollIntoView({
      behavior: 'smooth',
    });

    this.closeMobileMenu();
  }

  // Indica si el panel del carrito está abierto.
  cartOpen = false;

  // Abre el panel del carrito.
  openCart() {
    this.cartOpen = true;
  }

  // Cierra el panel del carrito.
  closeCart() {
    this.cartOpen = false;
  }

  // =========================
  // USUARIO
  // =========================

  // Nombre del usuario en mayúsculas, tal como se muestra en la barra.
  get fullName(): string {
    return this.username.toUpperCase();
  }

  // Cancela las suscripciones al destruir el componente.
  ngOnDestroy(): void {
    this.sub.unsubscribe();
    this.cartSub.unsubscribe();
    this.routerSub.unsubscribe();
  }

  // Cierra la sesión, cierra el menú móvil y redirige al inicio de sesión.
  logout(): void {
    this.auth.logout();
    this.closeMobileMenu();
    this.router.navigate(['/login']);
  }
}

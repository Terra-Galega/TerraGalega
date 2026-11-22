import { TranslatePipe } from '../../pipes/translate.pipe';
import { NavigationEnd, Router } from '@angular/router';
import { CartComponent } from '../cart/cart.component';
import { Component, HostListener, OnDestroy, inject } from '@angular/core';
import { filter, Subscription } from 'rxjs';
import { AuthService } from '../../services/auth.service';
import { CartService } from '../../services/cart.service';
import { SettingsMenuComponent } from '../settings-menu/settings-menu.component';
import { LanguageService } from '../../services/language.service';
import { HeroLogoService } from '../../services/hero-logo.service';

import { AsyncPipe, NgTemplateOutlet } from '@angular/common';
import {
  AppNotification,
  NotificationCenterService,
} from '../../services/notification-center.service';

// Partículas que van pegadas al apellido o nombre siguiente ("De la Cruz", "Van Dyke")
const NAME_PARTICLES = new Set([
  'de',
  'del',
  'la',
  'las',
  'los',
  'y',
  'da',
  'das',
  'do',
  'dos',
  'van',
  'von',
]);

// Primera palabra de un nombre o apellido. Si empieza con partículas, las conserva junto
// a la palabra que las sigue: "Pérez Gómez" -> "Pérez", "de la Cruz Rojas" -> "de la Cruz".
function firstWord(value: string | null | undefined): string {
  const words = (value ?? '').trim().split(/\s+/).filter(Boolean);
  const result: string[] = [];
  for (const word of words) {
    result.push(word);
    if (!NAME_PARTICLES.has(word.toLowerCase())) break;
  }
  return result.join(' ');
}

// Barra de navegación principal: enlaces, sesión del usuario, carrito y menú móvil.
@Component({
  selector: 'app-navbar',
  standalone: true,
  // Importa el carrito, que se muestra como panel desde la barra.
  imports: [
    CartComponent,
    AsyncPipe,
    SettingsMenuComponent,
    NgTemplateOutlet,
    TranslatePipe,
  ],
  templateUrl: './navbar.component.html',
  styleUrl: './navbar.component.scss',
})
export class NavbarComponent implements OnDestroy {
  // =========================
  // MENÚ MOBILE
  // =========================

  // Estado del logo del hero en el home (si ya llegó a la barra)
  readonly heroLogo = inject(HeroLogoService);
  readonly language = inject(LanguageService);

  // Indica si el menú desplegable de móvil está abierto.
  mobileMenuOpen = false;

  // =========================
  // NAVBAR
  // =========================

  // Si es true, la barra se muestra con fondo de vidrio; si es false, es transparente sobre el hero.
  navBarStayed = false;

  // Ruta actual (sin parámetros); sirve para resaltar el enlace de la página donde está el usuario.
  currentUrl = '';

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
      // Solo el primer nombre y el primer apellido, para que la barra no se desborde con
      // nombres largos ("Juan Carlos" "Pérez Gómez" -> "Juan Pérez"); vacío si no hay sesión.
      this.username = session
        ? `${firstWord(session.name)} ${firstWord(session.lastName)}`.trim()
        : '';
      this.userId = session?.id ?? null;
    });

    // Refresca el contador cada vez que cambian los productos del carrito.
    this.cartSub = this.cartService.items$.subscribe(() => {
      this.cartCount = this.cartService.count;
    });

    // "Ver carrito" desde un aviso: abre el panel del carrito
    this.cartSub.add(this.cartService.openRequests$.subscribe(() => this.openCart()));

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

  goToReservations(): void {
    this.router.navigate(['/reservations']);
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

  // Dirección real del enlace al perfil (para que sea un enlace de verdad: se alcanza con
  // Tab y el lector de pantalla lo anuncia como enlace)
  get profileHref(): string | null {
    return this.isLoggedIn ? this.auth.homeRoute() : null;
  }

  // Lleva a la página de su rol: panel de admin, panel de operador o cuenta.
  // La ruta de cada rol se define en AuthService.homeRoute().
  goToProfile(): void {
    // Sin sesión no hay perfil al que ir, así que no hace nada.
    if (!this.isLoggedIn) {
      return;
    }

    this.router.navigate([this.auth.homeRoute()]);
    this.navBarStayed = true;
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
    this.currentUrl = currentUrl;

    // Considera como inicio tanto '/home' como '/'.
    const isHome = currentUrl === '/home' || currentUrl === '/';

    // En inicio, el modo vidrio solo se activa tras bajar más de 20 px; en las demás páginas está siempre activo.
    if (isHome) {
      this.navBarStayed = window.scrollY > 20;
    } else {
      this.navBarStayed = true;
    }
  }

  // Indica si la ruta actual es la de ese enlace (o una subruta de él).
  isActive(path: string): boolean {
    return this.currentUrl === path || this.currentUrl.startsWith(path + '/');
  }

  // El nombre del usuario se resalta en su panel: admin, operador o cuenta de cliente.
  isProfileActive(): boolean {
    return ['/admin', '/operator', '/account'].some((p) => this.isActive(p));
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
  // NOTIFICACIONES (campanita)
  // =========================

  readonly center = inject(NotificationCenterService);
  bellOpen = false;

  toggleBell(): void {
    this.bellOpen = !this.bellOpen;
  }

  // Clic en una notificación: la marca leída y va al seguimiento
  openNotification(notification: AppNotification): void {
    this.center.markRead(notification.id);
    this.bellOpen = false;
    this.closeMobileMenu();
    this.router.navigate([notification.link], {
      queryParams: notification.queryParams,
    });
  }

  // Cierra el panel al hacer clic fuera o con Escape
  @HostListener('document:click', ['$event'])
  onDocumentClick(event: MouseEvent): void {
    if (
      this.bellOpen &&
      !(event.target as HTMLElement).closest('[data-bell]')
    ) {
      this.bellOpen = false;
    }
  }

  @HostListener('document:keydown.escape')
  onEscape(): void {
    this.bellOpen = false;
  }

  timeAgo(iso: string): string {
    const minutes = Math.floor((Date.now() - new Date(iso).getTime()) / 60_000);
    if (minutes < 1) return 'ahora';
    if (minutes < 60) return `hace ${minutes} min`;
    const hours = Math.floor(minutes / 60);
    if (hours < 24) return `hace ${hours} h`;
    return `hace ${Math.floor(hours / 24)} d`;
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

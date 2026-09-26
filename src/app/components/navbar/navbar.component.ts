import {
  Component,
  HostListener,
  OnDestroy,
  OnInit
} from '@angular/core';

import {
  NavigationEnd,
  Router,
  RouterLink,
  RouterLinkActive
} from '@angular/router';

import {
  filter,
  Subscription
} from 'rxjs';


@Component({
  selector: 'app-navbar',

  standalone: true,

  imports: [
    RouterLink,
    RouterLinkActive
  ],

  templateUrl: './navbar.component.html',

  styleUrl: './navbar.component.scss'
})
export class NavbarComponent implements OnInit, OnDestroy {

  /* ============================================================
     NAVEGACIÓN
     ============================================================ */

  /**
   * Indica si estamos en la página Home.
   *
   * En develop esto venía de:
   *
   * th:fragment="navbar(isHome)"
   *
   * y posteriormente:
   *
   * data-is-home="true/false"
   *
   * En Angular lo calculamos a partir de la ruta actual.
   */
  isHome = false;


  /**
   * Indica si la navbar está en estado "stayed".
   *
   * En Home:
   *
   *   scroll <= 20  -> transparente
   *   scroll > 20   -> glass
   *
   * En cualquier otra página:
   *
   *   -> glass siempre
   */
  navBarStayed = false;


  /* ============================================================
     MENÚ MOBILE
     ============================================================ */

  /**
   * Estado del menú desplegable móvil.
   *
   * Antes se controlaba mediante:
   *
   * mobileMenu.classList
   */
  mobileMenuOpen = false;


  /* ============================================================
     AUTENTICACIÓN
     ============================================================ */

  /**
   * Estos valores son temporales.
   *
   * Después los conectaremos con AuthService.
   */
  isLoggedIn = false;

  isLoggedAdmin = false;

  isClient = false;

  username = '';

  userId: number | null = null;


  /* ============================================================
     CARRITO
     ============================================================ */

  /**
   * Temporalmente 0.
   *
   * Después vendrá de CartService.
   */
  cartCount = 0;


  /* ============================================================
     ROUTER
     ============================================================ */

  private routerSubscription?: Subscription;


  constructor(
    private readonly router: Router
  ) {}


  /* ============================================================
     INIT
     ============================================================ */

  ngOnInit(): void {

    /*
     * Comprobamos la ruta inicial.
     */
    this.updateHomeState(this.router.url);


    /*
     * Detectamos los cambios de ruta.
     *
     * Esto sustituye la necesidad de:
     *
     * markActiveNavLink()
     *
     * porque routerLinkActive se encarga
     * directamente de los enlaces activos.
     */

    this.routerSubscription = this.router.events
      .pipe(
        filter(
          (event): event is NavigationEnd =>
            event instanceof NavigationEnd
        )
      )
      .subscribe((event) => {

        this.updateHomeState(event.urlAfterRedirects);

        /*
         * Al cambiar de página cerramos el menú mobile.
         */
        this.closeMobileMenu();

      });


    /*
     * Calculamos el estado inicial de la navbar.
     */
    this.updateNavBarState();

  }


  /* ============================================================
     RUTA ACTUAL
     ============================================================ */

  private updateHomeState(url: string): void {

    /*
     * Eliminamos query params y fragmentos.
     *
     * Ejemplo:
     *
     * /home?foo=bar
     *
     * pasa a:
     *
     * /home
     */

    const cleanUrl = url.split('?')[0].split('#')[0];


    this.isHome =
      cleanUrl === '/home' ||
      cleanUrl === '/';

    /*
     * Al entrar en otra página:
     *
     * la navbar debe estar directamente en modo stayed.
     */

    this.updateNavBarState();

  }


  /* ============================================================
     SCROLL
     ============================================================ */

  /**
   * Equivalente Angular de:
   *
   * window.addEventListener("scroll", updateNavBar)
   *
   * que estaba en script.js.
   */

  @HostListener('window:scroll')
  onWindowScroll(): void {

    this.updateNavBarState();

  }


  /**
   * Calcula si debemos aplicar:
   *
   * nav-bar--stayed
   */

  private updateNavBarState(): void {

    /*
     * En páginas que no son Home:
     *
     * siempre stayed.
     */

    if (!this.isHome) {

      this.navBarStayed = true;

      return;
    }


    /*
     * En Home:
     *
     * stayed únicamente después
     * de 20px de scroll.
     */

    this.navBarStayed =
      window.scrollY > 20;

  }


  /* ============================================================
     MENÚ MOBILE
     ============================================================ */

  /**
   * Equivalente a:
   *
   * mobileMenuBtn.addEventListener(...)
   */

  toggleMobileMenu(): void {

    this.mobileMenuOpen =
      !this.mobileMenuOpen;

  }


  /**
   * Equivalente a la función:
   *
   * closeMobileMenu()
   *
   * del script original.
   */

  closeMobileMenu(): void {

    this.mobileMenuOpen = false;

  }


  /* ============================================================
     SCROLL AL HERO
     ============================================================ */

  /**
   * En develop el logo mobile tenía:
   *
   * data-scroll="hero"
   *
   * y script.js hacía:
   *
   * document.getElementById(id)
   *   ?.scrollIntoView({ behavior: "smooth" });
   *
   * Aquí hacemos exactamente lo mismo
   * utilizando Angular/DOM.
   */

  scrollToHero(): void {

    const hero =
      document.getElementById('hero');

    hero?.scrollIntoView({
      behavior: 'smooth'
    });

    this.closeMobileMenu();

  }


  /* ============================================================
     CARRITO
     ============================================================ */

  /**
   * Por ahora solamente cerramos el menú mobile.
   *
   * El modal del carrito NO pertenece al Navbar.
   *
   * Posteriormente este método se conectará
   * con CartService / CartComponent.
   */

  openCart(): void {

    this.closeMobileMenu();

    /*
     * TODO:
     *
     * conectar con CartComponent / CartService.
     */

    console.log('Abrir carrito');

  }


  /* ============================================================
     USUARIO
     ============================================================ */

  /**
   * Ruta al perfil del usuario.
   *
   * En develop:
   *
   * Admin:
   * /admin/{id}
   *
   * Cliente:
   * /account/{id}
   *
   * Los demás roles no tenían enlace.
   */

  get profileUrl(): string {

    if (this.userId === null) {

      return '/';

    }


    if (this.isLoggedAdmin) {

      return `/admin/${this.userId}`;

    }


    return `/account/${this.userId}`;

  }


  /**
   * Nombre completo que se mostraba en develop.
   */

  get fullName(): string {

    return this.username.toUpperCase();

  }


  /* ============================================================
     DESTROY
     ============================================================ */

  ngOnDestroy(): void {

    this.routerSubscription?.unsubscribe();

  }

}
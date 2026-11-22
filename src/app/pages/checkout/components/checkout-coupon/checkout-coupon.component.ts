import { TranslatePipe } from '../../../../pipes/translate.pipe';
import { DecimalPipe } from '@angular/common';
import {
  Component,
  EventEmitter,
  Input,
  OnChanges,
  OnInit,
  Output,
  SimpleChanges,
  inject,
} from '@angular/core';
import { ButtonComponent } from '../../../../components/button/button.component';
import { Coupon, CouponDiscount } from '../../../../models/coupon';
import { CouponService } from '../../../../services/coupon.service';
import { getErrorMessage } from '../../../../utils/http-error';

// Cupón de descuento del checkout: el cliente escribe un código o elige uno de
// los suyos. El descuento que se muestra es solo informativo: el back vuelve a
// validar el cupón y a calcular todo al confirmar el pedido.
@Component({
  selector: 'app-checkout-coupon',
  standalone: true,
  imports: [DecimalPipe, ButtonComponent, TranslatePipe],
  templateUrl: './checkout-coupon.component.html',
})
export class CheckoutCouponComponent implements OnInit, OnChanges {
  private couponService = inject(CouponService);

  // Cliente con sesión (el cupón se valida contra él)
  @Input({ required: true }) clientId!: number;

  // Subtotal del carrito (sin domicilio). Si cambia, el cupón se vuelve a validar
  @Input({ required: true }) subtotal!: number;

  // Código guardado en el borrador (si el cliente vuelve del paso 2): se aplica al abrir
  @Input() initialCode = '';

  // Avisa al padre cuando se aplica un cupón (con su descuento), cambia su
  // valor o se quita (null)
  @Output() couponChange = new EventEmitter<CouponDiscount | null>();

  // Texto escrito en el campo
  code = '';

  // true mientras se espera la respuesta del back (evita doble clic)
  applying = false;

  // Mensaje de error del cupón (vacío = sin error)
  error = '';

  // Cupón aplicado ahora mismo (null = ninguno)
  current: CouponDiscount | null = null;

  // Cupones que el cliente puede usar ahora (personales y generales)
  available: Coupon[] = [];

  ngOnInit(): void {
    // Si falla la carga, igual puede escribir el código a mano
    this.couponService.getAvailableCoupons().subscribe({
      next: (coupons) => (this.available = coupons),
      error: (error) => console.error('Error cargando cupones:', error),
    });

    // Volvió del paso 2 con un cupón aplicado: se vuelve a validar y aplicar
    if (this.initialCode) {
      this.code = this.initialCode;
      this.apply();
    }
  }

  // Si cambia el subtotal (por ejemplo, al quitar un producto del carrito), el
  // descuento y hasta la validez del cupón pueden cambiar
  ngOnChanges(changes: SimpleChanges): void {
    if (changes['subtotal'] && !changes['subtotal'].firstChange && this.current) {
      this.revalidate();
    }
  }

  // Guarda lo que escribe el cliente
  setCode(event: Event): void {
    this.code = (event.target as HTMLInputElement).value;
  }

  // Enter dentro del campo aplica el cupón. Se evita el envío del formulario
  // del checkout (que sería "Continuar")
  onEnter(event: Event): void {
    event.preventDefault();
    this.apply();
  }

  // Botón "Aplicar": pregunta al back cuánto descontaría el cupón
  apply(): void {
    const code = this.code.trim();

    if (!code) {
      this.error = 'Escribe el código de tu cupón.';
      return;
    }

    if (this.applying) {
      return;
    }

    this.applying = true;
    this.error = '';

    this.couponService
      .validateCoupon(code, this.subtotal)
      .subscribe({
        next: (result) => {
          this.applying = false;
          this.current = result;
          this.code = '';
          this.couponChange.emit(result);
        },
        error: (error) => {
          this.applying = false;
          // El mensaje viene del back (vencido, ya usado, mínimo, no existe...)
          this.error = getErrorMessage(
            error,
            'No se pudo validar el cupón. Inténtalo de nuevo.',
          );
        },
      });
  }

  // Elige uno de los cupones de la lista: rellena el código y lo aplica
  choose(coupon: Coupon): void {
    this.code = coupon.code;
    this.apply();
  }

  // Botón "Quitar cupón"
  remove(): void {
    this.current = null;
    this.code = '';
    this.error = '';
    this.couponChange.emit(null);
  }

  // Vuelve a validar el cupón aplicado con el subtotal nuevo. Si ya no sirve
  // (por ejemplo, ya no llega al mínimo), se quita y se avisa por qué
  private revalidate(): void {
    if (!this.current) {
      return;
    }

    const code = this.current.code;

    this.couponService
      .validateCoupon(code, this.subtotal)
      .subscribe({
        next: (result) => {
          this.current = result;
          this.couponChange.emit(result);
        },
        error: (error) => {
          this.current = null;
          this.code = '';
          this.error =
            `Quitamos el cupón ${code}. ` +
            getErrorMessage(error, 'Ya no se puede aplicar.');
          this.couponChange.emit(null);
        },
      });
  }

  // ¿El subtotal actual llega al mínimo que pide el cupón?
  isEligible(coupon: Coupon): boolean {
    return !coupon.minOrderAmount || this.subtotal >= coupon.minOrderAmount;
  }

  // Texto corto del descuento: "10%" o "$5.000"
  valueText(coupon: Coupon): string {
    return coupon.discountType === 'PERCENTAGE'
      ? `${coupon.discountValue}%`
      : `$${coupon.discountValue.toLocaleString('es-CO')}`;
  }
}

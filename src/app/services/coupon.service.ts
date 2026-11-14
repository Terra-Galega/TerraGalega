import { API_URL } from '../api-url';
import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable, Subject } from 'rxjs';
import {
  ClientCoupon,
  Coupon,
  CouponDiscount,
  CouponPayload,
} from '../models/coupon';

/**
 * Servicio que habla con el back en /coupons: CRUD de cupones, "Mis cupones"
 * y validación de un cupón en el checkout.
 * Es un servicio único (providedIn: 'root'), compartido por toda la app.
 */
@Injectable({
  providedIn: 'root',
})
export class CouponService {
  // Dirección base de los endpoints de cupones en el back
  private apiUrl = `${API_URL}/coupons`;

  // Aviso de "los cupones cambiaron": quien modifica un cupón llama a
  // refresh() y los componentes que escuchan refresh$ recargan su lista
  private refreshSubject = new Subject<void>();

  refresh$ = this.refreshSubject.asObservable();

  constructor(private http: HttpClient) {}

  // Dispara el aviso para que las listas se vuelvan a cargar
  refresh() {
    this.refreshSubject.next();
  }

  // Todos los cupones, activos e inactivos (admin)
  getCoupons(): Observable<Coupon[]> {
    return this.http.get<Coupon[]>(this.apiUrl);
  }

  // Crea un cupón nuevo
  createCoupon(coupon: CouponPayload): Observable<Coupon> {
    return this.http.post<Coupon>(this.apiUrl, coupon);
  }

  // Actualiza un cupón existente (el id va en la dirección)
  updateCoupon(id: number, coupon: CouponPayload): Observable<Coupon> {
    return this.http.put<Coupon>(`${this.apiUrl}/${id}`, coupon);
  }

  // Elimina un cupón (el back lo rechaza si ya está en pedidos)
  deleteCoupon(id: number): Observable<void> {
    return this.http.delete<void>(`${this.apiUrl}/${id}`);
  }

  // Cupones del cliente con sesión iniciada y su estado (para "Mis cupones")
  getClientCoupons(): Observable<ClientCoupon[]> {
    return this.http.get<ClientCoupon[]>(`${this.apiUrl}/me/all`);
  }

  // Cupones (personales y generales) que el cliente puede usar ahora mismo
  // (para el selector del checkout)
  getAvailableCoupons(): Observable<Coupon[]> {
    return this.http.get<Coupon[]>(`${this.apiUrl}/me`);
  }

  // Comprueba un código y devuelve cuánto descontaría sobre el subtotal. Es solo
  // para mostrarlo antes de pagar: el back vuelve a validar al confirmar el pedido.
  // Si el cupón no sirve, responde con un error cuyo mensaje explica el motivo.
  validateCoupon(code: string, subtotal: number): Observable<CouponDiscount> {
    return this.http.get<CouponDiscount>(`${this.apiUrl}/validate`, {
      params: { code, subtotal },
    });
  }

  // Alterna el cupón entre activo e inactivo
  toggleCouponActiveStatus(id: number): Observable<Coupon> {
    return this.http.put<Coupon>(`${this.apiUrl}/${id}/toggle`, {});
  }
}
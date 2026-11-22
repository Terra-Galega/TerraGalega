import { API_URL } from '../api-url';
import { Injectable } from '@angular/core';
import { SKIP_BUSY } from '../interceptors/busy.interceptor';
import { CartItem } from './cart.service';
import { HttpClient, HttpContext } from '@angular/common/http';
import { Observable, catchError, combineLatest, distinctUntilChanged, map, of, startWith, switchMap } from 'rxjs';
import { CheckoutRequest, PointsQuote } from '../models/checkout';

// Servicio encargado de crear el pedido al finalizar la compra.
@Injectable({ providedIn: 'root' })
export class CheckoutService {
    // URL del endpoint de checkout en Spring Boot.
    private apiUrl = `${API_URL}/checkout`;

    // Inyecta el cliente HTTP para realizar solicitudes al backend.
    constructor(private http: HttpClient) {}

    watchPoints(items$: Observable<CartItem[]>, coupon$: Observable<string | null>): Observable<PointsQuote | null> {
        const cart$ = items$.pipe(
            map(items => items.map(item => ({
                productId: item.product.id, quantity: item.quantity,
                addOnIds: item.addOns.map(extra => extra.id),
            }))),
            distinctUntilChanged((a, b) => JSON.stringify(a) === JSON.stringify(b)),
        );
        return combineLatest([cart$, coupon$]).pipe(
            switchMap(([items, couponCode]) => items.length
                ? this.http.post<PointsQuote>(this.apiUrl + '/points/quote', { items, couponCode }, {
                    context: new HttpContext().set(SKIP_BUSY, true),
                }).pipe(startWith(null), catchError(() => of(null)))
                : of(null)),
        );
    }

    payWithPoints(checkout: CheckoutRequest, requestId: string, quote: PointsQuote): Observable<void> {
        return this.http.post<void>(this.apiUrl + '/points', {
            checkout, requestId, expectedPoints: quote.required, expectedTotal: quote.total,
        });
    }

    // Crea el pedido del cliente con la dirección y los productos del carrito.
    // El back identifica al cliente por el token.
    // El servidor responde 201 sin cuerpo: solo importa si el pedido se creó
    checkout(request: CheckoutRequest): Observable<void> {
        return this.http.post<void>(this.apiUrl, request);
    }
}
import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { CheckoutRequest } from '../models/checkout';

// Servicio encargado de crear el pedido al finalizar la compra.
@Injectable({ providedIn: 'root' })
export class CheckoutService {
    // URL del endpoint de checkout en Spring Boot.
    private apiUrl = 'http://localhost:8080/checkout';

    // Inyecta el cliente HTTP para realizar solicitudes al backend.
    constructor(private http: HttpClient) {}

    // Crea el pedido del cliente con la dirección y los productos del carrito.
    // El clientId viaja como parámetro de la URL y el resto en el cuerpo.
    // El servidor responde 201 sin cuerpo: solo importa si el pedido se creó
    checkout(clientId: number, request: CheckoutRequest): Observable<void> {
        return this.http.post<void>(this.apiUrl, request, { params: { clientId } });
    }
}
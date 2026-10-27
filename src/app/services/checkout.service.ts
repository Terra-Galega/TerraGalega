import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { CheckoutRequest } from '../models/checkout';

@Injectable({ providedIn: 'root' })
export class CheckoutService {
    private apiUrl = 'http://localhost:8080/checkout';

    constructor(private http: HttpClient) {}

    // El servidor responde 201 sin cuerpo: solo importa si el pedido se creó
    checkout(clientId: number, request: CheckoutRequest): Observable<void> {
        return this.http.post<void>(this.apiUrl, request, { params: { clientId } });
    }
}
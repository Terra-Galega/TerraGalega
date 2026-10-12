
import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { CheckoutRequest } from '../models/checkout';
import { Order } from '../models/order';

@Injectable({ providedIn: 'root' })
export class CheckoutService {
    private apiUrl = 'http://localhost:8080/checkout';

    constructor(private http: HttpClient) {}

    checkout(clientId: number, request: CheckoutRequest): Observable<Order> {
        return this.http.post<Order>(this.apiUrl, request, { params: { clientId } });
    }
}

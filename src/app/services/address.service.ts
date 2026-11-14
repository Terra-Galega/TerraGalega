import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { Address, AddressInput } from '../models/address';
import { API_URL } from './auth.service';

// Direcciones guardadas del cliente con sesión iniciada (el back lo identifica por el token).
@Injectable({
  providedIn: 'root',
})
export class AddressService {
  private apiUrl = `${API_URL}/clients/me/addresses`;

  constructor(private http: HttpClient) {}

  // Direcciones guardadas, con la predeterminada primero.
  getAddresses(): Observable<Address[]> {
    return this.http.get<Address[]>(this.apiUrl);
  }

  // Agrega una dirección nueva.
  addAddress(data: AddressInput): Observable<Address> {
    return this.http.post<Address>(this.apiUrl, data);
  }

  // Cambia el nombre, el texto o la marca de predeterminada de una dirección.
  updateAddress(addressId: number, data: AddressInput): Observable<Address> {
    return this.http.put<Address>(`${this.apiUrl}/${addressId}`, data);
  }

  // Elimina una dirección.
  deleteAddress(addressId: number): Observable<void> {
    return this.http.delete<void>(`${this.apiUrl}/${addressId}`);
  }
}

import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { Address, AddressInput } from '../models/address';

// Servicio encargado de gestionar las direcciones guardadas de un cliente.
@Injectable({
  providedIn: 'root',
})
export class AddressService {
  // URL base de los endpoints de clientes en Spring Boot.
  private apiUrl = 'http://localhost:8080/clients';

  constructor(private http: HttpClient) {}

  // Endpoint de las direcciones de un cliente.
  private url(clientId: number): string {
    return `${this.apiUrl}/${clientId}/addresses`;
  }

  // Obtiene las direcciones guardadas del cliente (la predeterminada primero).
  getAddresses(clientId: number): Observable<Address[]> {
    return this.http.get<Address[]>(this.url(clientId));
  }

  // Agrega una dirección nueva al cliente.
  addAddress(clientId: number, data: AddressInput): Observable<Address> {
    return this.http.post<Address>(this.url(clientId), data);
  }

  // Cambia el nombre, el texto o la marca de predeterminada de una dirección.
  updateAddress(
    clientId: number,
    addressId: number,
    data: AddressInput,
  ): Observable<Address> {
    return this.http.put<Address>(`${this.url(clientId)}/${addressId}`, data);
  }

  // Elimina una dirección del cliente.
  deleteAddress(clientId: number, addressId: number): Observable<void> {
    return this.http.delete<void>(`${this.url(clientId)}/${addressId}`);
  }
}
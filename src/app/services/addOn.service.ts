import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable, Subject } from 'rxjs';
import { AddOn, AddOnOption, CreateAddOn } from '../models/addOn';

/**
 * Servicio que habla con el back en /addons: consultas y CRUD de adicionales.
 * Es un servicio único (providedIn: 'root'), compartido por toda la app.
 */
@Injectable({
  providedIn: 'root',
})
export class AddOnService {
  // Dirección base de los endpoints de adicionales en el back
  private apiUrl = 'http://localhost:8080/addons';

  // Aviso de "los adicionales cambiaron": quien modifica un adicional llama a
  // refresh() y los componentes que escuchan refresh$ recargan su lista
  private refreshSubject = new Subject<void>();

  refresh$ = this.refreshSubject.asObservable();

  constructor(private http: HttpClient) {}

  // Dispara el aviso para que las listas se vuelvan a cargar
  refresh() {
    this.refreshSubject.next();
  }

  // Todos los adicionales, activos e inactivos (admin)
  getAddOns(): Observable<AddOn[]> {
    return this.http.get<AddOn[]>(this.apiUrl);
  }

  // Solo los adicionales activos
  getActiveAddOns(): Observable<AddOn[]> {
    return this.http.get<AddOn[]>(`${this.apiUrl}/active`);
  }

  // Un adicional por su id
  getAddOnById(id: number): Observable<AddOn> {
    return this.http.get<AddOn>(`${this.apiUrl}/${id}`);
  }

  // Adicionales activos de una categoría, en versión reducida (id, nombre y
  // precio). Se usan en el detalle de un plato para que el cliente elija.
  getActiveAddOnsByCategory(categoryId: number): Observable<AddOnOption[]> {
    return this.http.get<AddOnOption[]>(
      `${this.apiUrl}/active/category/${categoryId}`,
    );
  }

  // Crea un adicional nuevo
  createAddOn(addOn: CreateAddOn): Observable<AddOn> {
    return this.http.post<AddOn>(this.apiUrl, this.toPayload(addOn));
  }

  // Actualiza un adicional existente (el id va en la dirección)
  updateAddOn(addOn: AddOn): Observable<AddOn> {
    return this.http.put<AddOn>(
      `${this.apiUrl}/${addOn.id}`,
      this.toPayload(addOn),
    );
  }

  // Elimina un adicional (el back lo rechaza si ya está en pedidos)
  deleteAddOn(id: number): Observable<void> {
    return this.http.delete<void>(`${this.apiUrl}/${id}`);
  }

  // Alterna el adicional entre activo e inactivo
  toggleAddOnActiveStatus(id: number): Observable<AddOn> {
    return this.http.put<AddOn>(`${this.apiUrl}/${id}/toggle`, {});
  }

  // Solo se manda el id de cada categoría, igual que hace ProductService
  private toPayload(addOn: AddOn | CreateAddOn) {
    return {
      ...addOn,
      categories: (addOn.categories ?? []).map((c) => ({ id: c.id })),
    };
  }
}

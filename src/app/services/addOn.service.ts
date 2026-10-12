import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable, Subject } from 'rxjs';
import { AddOn, CreateAddOn } from '../models/addOn';

@Injectable({
  providedIn: 'root',
})
export class AddOnService {
  private apiUrl = 'http://localhost:8080/addons';
  private refreshSubject = new Subject<void>();

  refresh$ = this.refreshSubject.asObservable();

  constructor(private http: HttpClient) {}

  refresh() {
    this.refreshSubject.next();
  }

  getAddOns(): Observable<AddOn[]> {
    return this.http.get<AddOn[]>(this.apiUrl);
  }

  getActiveAddOns(): Observable<AddOn[]> {
    return this.http.get<AddOn[]>(`${this.apiUrl}/active`);
  }

  getAddOnById(id: number): Observable<AddOn> {
    return this.http.get<AddOn>(`${this.apiUrl}/${id}`);
  }

  createAddOn(addOn: CreateAddOn): Observable<AddOn> {
    return this.http.post<AddOn>(this.apiUrl, this.toPayload(addOn));
  }

  updateAddOn(addOn: AddOn): Observable<AddOn> {
    return this.http.put<AddOn>(
      `${this.apiUrl}/${addOn.id}`,
      this.toPayload(addOn),
    );
  }

  deleteAddOn(id: number): Observable<void> {
    return this.http.delete<void>(`${this.apiUrl}/${id}`);
  }

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

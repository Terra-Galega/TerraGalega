import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';

import { API_URL } from './auth.service';
import { MapAddress } from '../models/map-address';

@Injectable({ providedIn: 'root' })
export class MapAddressService {
  private http = inject(HttpClient);

  search(address: string) {
    return this.http.get<MapAddress | null>(`${API_URL}/maps/search`, {
      params: { q: address },
    });
  }

  reverse(lat: number, lon: number) {
    return this.http.get<MapAddress>(`${API_URL}/maps/reverse`, {
      params: { lat, lon },
    });
  }
}

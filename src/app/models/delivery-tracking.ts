export interface MapPoint {
  lat: number;
  lon: number;
}

export interface OperatorDelivery {
  orderId: number;
  address: string | null;
  deliveryPersonName: string;
  deliveryPersonPhone: string | null;
  latitude: number | null;
  longitude: number | null;
  locationUpdatedAt: string | null;
  startedAt: string;
  route: MapPoint[];
}

// Lo que ve el cliente de su pedido en camino: PIN, repartidor y ruta.
// Es la misma respuesta que usa la app móvil (GET /delivery/client/{id}/active).
export interface ClientDelivery {
  orderId: number;
  status: string;
  address: string | null;
  pin: string | null;
  deliveryPersonName: string | null;
  deliveryPersonPhone: string | null;
  latitude: number | null;
  longitude: number | null;
  locationUpdatedAt: string | null;
  // null mientras el repartidor aún no pulsa "Iniciar entrega"
  startedAt: string | null;
  route: MapPoint[] | null;
  // Avance del recorrido de 0 a 1 (null si aún no inicia)
  progress: number | null;
}

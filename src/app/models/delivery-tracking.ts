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

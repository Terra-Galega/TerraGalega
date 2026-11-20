import { TranslatePipe } from '../../../../services/translate.pipe';
import {
  AfterViewInit,
  Component,
  DestroyRef,
  ElementRef,
  NgZone,
  inject,
} from '@angular/core';
import { DatePipe } from '@angular/common';
import { ActivatedRoute, RouterLink } from '@angular/router';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';

import type { FeatureCollection } from 'geojson';
import type { GeoJSONSource, Map as MapLibreMap, Marker } from 'maplibre-gl';
import { loadMapLibre } from '../../../../utils/maplibre';

import { NavbarComponent } from '../../../../components/navbar/navbar.component';
import { FooterComponent } from '../../../../components/footer/footer.component';
import { ButtonComponent } from '../../../../components/button/button.component';
import { ClientDelivery, MapPoint } from '../../../../models/delivery-tracking';
import { ClientDeliveryWatcherService } from '../../../../services/client-delivery-watcher.service';
import {
  COURIER_SVG,
  RESTAURANT_SVG,
  markerElement,
  setFacingLeft,
} from '../../../../utils/map-markers';
import { MAP_LOCALE_ES } from '../../../../utils/map-locale';

// Seguimiento del pedido en camino (equivalente a la pantalla "Seguir mi pedido" de la app móvil):
// mapa con el restaurante, la ruta, el repartidor y el destino; además el PIN de entrega.
// Los datos llegan del servicio que vigila el pedido (una sola consulta cada pocos segundos).
@Component({
  selector: 'app-order-tracking',
  standalone: true,
  imports: [
    DatePipe,
    RouterLink,
    NavbarComponent,
    FooterComponent,
    ButtonComponent,
    TranslatePipe],
  templateUrl: './order-tracking.component.html',
})
export class OrderTrackingComponent implements AfterViewInit {
  readonly watcher = inject(ClientDeliveryWatcherService);
  private route = inject(ActivatedRoute);
  private host = inject<ElementRef<HTMLElement>>(ElementRef);
  private destroyRef = inject(DestroyRef);
  private zone = inject(NgZone);

  // Pedido que se está siguiendo (null si no hay ninguno en camino)
  delivery: ClientDelivery | null = null;
  // true hasta la primera respuesta del back
  loaded = false;
  // true si hubo un pedido en camino y dejó de estarlo (se entregó)
  delivered = false;
  following = true;
  mapError = '';

  private requestedOrderId: number | null = null;
  private map: MapLibreMap | null = null;
  private ready = false;
  private loadingMap = false;
  private destroyed = false;
  private frame = 0;
  private fittedOrderId: number | null = null;

  private MarkerClass: typeof Marker | null = null;
  private restaurantMarker: Marker | null = null;
  private courierMarker: Marker | null = null;
  private lastCourierLon: number | null = null;

  constructor() {
    const order = Number(this.route.snapshot.queryParamMap.get('order'));
    this.requestedOrderId = Number.isFinite(order) && order > 0 ? order : null;

    this.destroyRef.onDestroy(() => {
      this.destroyed = true;
      cancelAnimationFrame(this.frame);
      this.disposeMap();
    });
  }

  ngAfterViewInit(): void {
    this.watcher.loaded$
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe((loaded) => (this.loaded = loaded));

    this.watcher.deliveries$
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe((list) => {
        const next =
          (this.requestedOrderId !== null
            ? list.find((d) => d.orderId === this.requestedOrderId)
            : undefined) ??
          list[0] ??
          null;

        if (!next && this.delivery) this.delivered = true;
        if (next) this.delivered = false;

        this.delivery = next;
        this.schedulePaint();
      });

    // Consulta inmediata, sin esperar al siguiente ciclo
    this.watcher.refresh();
  }

  // ---------- Datos para la plantilla ----------

  // El mapa solo aparece cuando el repartidor ya inició el recorrido
  get started(): boolean {
    return !!this.delivery?.startedAt && (this.delivery.route?.length ?? 0) > 1;
  }

  get percent(): number {
    return Math.round((this.delivery?.progress ?? 0) * 100);
  }

  get pinDigits(): string[] {
    return (this.delivery?.pin ?? '').split('');
  }

  get notificationsPending(): boolean {
    return this.watcher.notificationPermission === 'default';
  }

  get complaintLink(): string {
    const subject = `Quejas, Reclamos o Comentarios del pedido #${this.delivery?.orderId ?? ''}`;
    return `mailto:contacto@terragalega.co?subject=${encodeURIComponent(subject)}`;
  }

  enableNotifications(): void {
    void this.watcher.enableNotifications();
  }

  toggleFollow(): void {
    this.following = !this.following;
    this.schedulePaint();
  }

  showWholeRoute(): void {
    this.following = false;
    this.fitRoute();
  }

  // ---------- Mapa ----------

  private schedulePaint(): void {
    cancelAnimationFrame(this.frame);
    // Espera a que Angular haya pintado (o quitado) el contenedor del mapa
    this.frame = requestAnimationFrame(() => void this.paint());
  }

  private async paint(): Promise<void> {
    if (this.destroyed) return;

    const delivery = this.delivery;

    // Sin recorrido iniciado: no hay mapa que mostrar
    if (!delivery || !this.started) {
      this.disposeMap();
      return;
    }

    if (!this.map) {
      await this.createMap();
      return;
    }
    if (!this.ready) return;

    const route = delivery.route ?? [];

    const routeData: FeatureCollection = {
      type: 'FeatureCollection',
      features: [
        {
          type: 'Feature',
          properties: {},
          geometry: {
            type: 'LineString',
            coordinates: route.map((p) => [p.lon, p.lat]),
          },
        },
      ],
    };

    // El destino (último punto de la ruta) es un punto rojo
    const destination = route[route.length - 1];
    const pointData: FeatureCollection = {
      type: 'FeatureCollection',
      features: [
        {
          type: 'Feature',
          properties: { color: '#C62828', radius: 10 },
          geometry: {
            type: 'Point',
            coordinates: [destination.lon, destination.lat],
          },
        },
      ],
    };

    (this.map.getSource('tracking-route') as GeoJSONSource).setData(routeData);
    (this.map.getSource('tracking-points') as GeoJSONSource).setData(pointData);

    // Restaurante = inicio de la ruta
    this.placeRestaurant(route[0]);

    // Mientras el repartidor no reporte posición se asume que sigue en el restaurante
    const hasPosition =
      delivery.latitude !== null && delivery.longitude !== null;
    const courier: MapPoint = hasPosition
      ? { lat: delivery.latitude!, lon: delivery.longitude! }
      : route[0];
    this.placeCourier(courier);

    this.map.resize();

    if (this.fittedOrderId !== delivery.orderId) {
      this.fittedOrderId = delivery.orderId;
      this.fitRoute();
    } else if (this.following && hasPosition) {
      this.map.easeTo({
        center: [courier.lon, courier.lat],
        zoom: 15.5,
        duration: 800,
      });
    }
  }

  private placeRestaurant(point: MapPoint): void {
    if (!this.map || !this.MarkerClass) return;

    if (!this.restaurantMarker) {
      this.restaurantMarker = new this.MarkerClass({
        element: markerElement(
          RESTAURANT_SVG,
          56,
          60,
          'Restaurante Terra Galega',
        ),
        anchor: 'bottom',
      });
      this.restaurantMarker.setLngLat([point.lon, point.lat]).addTo(this.map);
    } else {
      this.restaurantMarker.setLngLat([point.lon, point.lat]);
    }
  }

  // Carrito del repartidor: mira hacia donde avanza (este u oeste)
  private placeCourier(point: MapPoint): void {
    if (!this.map || !this.MarkerClass) return;

    if (!this.courierMarker) {
      this.courierMarker = new this.MarkerClass({
        element: markerElement(COURIER_SVG, 58, 48, 'Repartidor'),
        anchor: 'bottom',
      });
      this.courierMarker.setLngLat([point.lon, point.lat]).addTo(this.map);
    } else {
      this.courierMarker.setLngLat([point.lon, point.lat]);
    }

    // Solo gira si de verdad se movió (así no "tiembla" con el GPS)
    const before = this.lastCourierLon;
    if (before === null || Math.abs(point.lon - before) > 0.00002) {
      if (before !== null) {
        setFacingLeft(this.courierMarker.getElement(), point.lon < before);
      }
      this.lastCourierLon = point.lon;
    }
  }

  private async createMap(): Promise<void> {
    if (this.loadingMap || this.destroyed) return;
    this.loadingMap = true;

    try {
      const { Map, NavigationControl, Marker } = await loadMapLibre();
      this.MarkerClass = Marker;

      const container = this.host.nativeElement.querySelector<HTMLElement>(
        '[data-tracking-map]',
      );
      if (this.destroyed || !container || !this.delivery?.route?.length) return;

      const first = this.delivery.route[0];

      const map = new Map({
        container,
        // Controles en español (para el lector de pantalla)
        locale: MAP_LOCALE_ES,
        style: 'https://tiles.openfreemap.org/styles/liberty',
        center: [first.lon, first.lat],
        zoom: 12,
      });
      this.map = map;

      map.addControl(
        new NavigationControl({ showCompass: false }),
        'top-right',
      );

      // Si el cliente mueve el mapa, deja de seguir al repartidor
      map.on('dragstart', () => this.zone.run(() => (this.following = false)));

      map.on('error', () =>
        this.zone.run(
          () =>
            (this.mapError =
              'No se pudo cargar parte del mapa. Revisa la conexión.'),
        ),
      );

      map.on('load', () => {
        if (this.destroyed) return;

        const empty: FeatureCollection = {
          type: 'FeatureCollection',
          features: [],
        };

        map.addSource('tracking-route', { type: 'geojson', data: empty });
        map.addLayer({
          id: 'tracking-route-line',
          type: 'line',
          source: 'tracking-route',
          layout: { 'line-join': 'round', 'line-cap': 'round' },
          paint: { 'line-color': '#B2571F', 'line-width': 5 },
        });

        map.addSource('tracking-points', { type: 'geojson', data: empty });
        map.addLayer({
          id: 'tracking-point-circles',
          type: 'circle',
          source: 'tracking-points',
          paint: {
            'circle-color': ['get', 'color'],
            'circle-radius': ['get', 'radius'],
            'circle-stroke-color': '#FFFFFF',
            'circle-stroke-width': 3,
          },
        });

        this.zone.run(() => {
          this.ready = true;
          this.mapError = '';
          this.schedulePaint();
        });
      });
    } catch {
      this.zone.run(
        () =>
          (this.mapError =
            'No se pudo iniciar el mapa. Revisa la conexión y el soporte WebGL del navegador.'),
      );
    } finally {
      this.loadingMap = false;
    }
  }

  // Quita el mapa (cuando el pedido se entrega o aún no inicia el recorrido)
  private disposeMap(): void {
    this.restaurantMarker?.remove();
    this.courierMarker?.remove();
    this.restaurantMarker = null;
    this.courierMarker = null;
    this.lastCourierLon = null;
    this.fittedOrderId = null;
    this.ready = false;
    this.map?.remove();
    this.map = null;
  }

  private fitRoute(): void {
    const route = this.delivery?.route;
    if (!this.map || !this.ready || !route?.length) return;

    const lons = route.map((p) => p.lon);
    const lats = route.map((p) => p.lat);

    this.map.fitBounds(
      [
        [Math.min(...lons), Math.min(...lats)],
        [Math.max(...lons), Math.max(...lats)],
      ],
      { padding: 50, maxZoom: 16, duration: 700 },
    );
  }
}

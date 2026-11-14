import {
  AfterViewInit,
  Component,
  DestroyRef,
  ElementRef,
  NgZone,
  inject,
} from '@angular/core';
import { DatePipe } from '@angular/common';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { EMPTY, catchError, exhaustMap, filter, timer } from 'rxjs';

import type { FeatureCollection } from 'geojson';
import type { GeoJSONSource, Map as MapLibreMap } from 'maplibre-gl';

import {
  MapPoint,
  OperatorDelivery,
} from '../../../../models/delivery-tracking';
import { DeliveryTrackingService } from '../../../../services/delivery-tracking.service';
import { getErrorMessage } from '../../../../utils/http-error';

@Component({
  selector: 'app-operator-tracking',
  standalone: true,
  imports: [DatePipe],
  templateUrl: './operator-tracking.component.html',
  styleUrl: './operator-tracking.component.scss',
})
export class OperatorTrackingComponent implements AfterViewInit {
  private service = inject(DeliveryTrackingService);
  private host = inject<ElementRef<HTMLElement>>(ElementRef);
  private destroyRef = inject(DestroyRef);
  private zone = inject(NgZone);

  deliveries: OperatorDelivery[] = [];
  selected: OperatorDelivery | null = null;

  errorMessage = '';
  mapError = '';
  checkedAt: Date | null = null;
  following = true;

  private map: MapLibreMap | null = null;
  private ready = false;
  private loadingMap = false;
  private destroyed = false;
  private frame = 0;
  private fittedOrderId: number | null = null;

  constructor() {
    this.destroyRef.onDestroy(() => {
      this.destroyed = true;
      cancelAnimationFrame(this.frame);
      this.map?.remove();
      this.map = null;
    });
  }

  ngAfterViewInit(): void {
    timer(0, 3000)
      .pipe(
        filter(() => document.visibilityState === 'visible'),

        // Evita acumular peticiones si una respuesta tarda.
        exhaustMap(() =>
          this.service.getActiveDeliveries().pipe(
            catchError((error) => {
              this.errorMessage = getErrorMessage(
                error,
                'No se pudo actualizar el seguimiento.',
              );

              // Conserva la última información, mostrando el error.
              return EMPTY;
            }),
          ),
        ),

        takeUntilDestroyed(this.destroyRef),
      )
      .subscribe((deliveries) => {
        const previousId = this.selected?.orderId;

        this.deliveries = deliveries;
        this.selected =
          deliveries.find((item) => item.orderId === previousId) ??
          deliveries[0] ??
          null;

        this.errorMessage = '';
        this.checkedAt = new Date();

        if (!this.selected) {
          this.fittedOrderId = null;
        }

        this.schedulePaint();
      });
  }

  selectDelivery(delivery: OperatorDelivery): void {
    this.selected = delivery;
    this.following = true;
    this.schedulePaint();
  }

  toggleFollow(): void {
    this.following = !this.following;
    this.schedulePaint();
  }

  showWholeRoute(): void {
    this.following = false;
    this.fitRoute();
  }

  private schedulePaint(): void {
    cancelAnimationFrame(this.frame);

    // Espera a que Angular haya mostrado el contenedor.
    this.frame = requestAnimationFrame(() => {
      void this.paint();
    });
  }

  private async paint(): Promise<void> {
    if (this.destroyed || !this.selected) return;

    if (!this.map) {
      await this.createMap();
      return;
    }

    if (!this.ready) return;

    const delivery = this.selected;
    const route = delivery.route;

    const routeData: FeatureCollection = {
      type: 'FeatureCollection',
      features:
        route.length > 1
          ? [
              {
                type: 'Feature',
                properties: {},
                geometry: {
                  type: 'LineString',
                  coordinates: route.map((point) => [point.lon, point.lat]),
                },
              },
            ]
          : [],
    };

    const pointData: FeatureCollection = {
      type: 'FeatureCollection',
      features: [],
    };

    const addPoint = (point: MapPoint, color: string, radius: number) => {
      pointData.features.push({
        type: 'Feature',
        properties: { color, radius },
        geometry: {
          type: 'Point',
          coordinates: [point.lon, point.lat],
        },
      });
    };

    if (route.length > 1) {
      addPoint(route[0], '#B2571F', 8);
      addPoint(route[route.length - 1], '#C62828', 10);
    }

    const hasPosition =
      delivery.latitude !== null && delivery.longitude !== null;

    if (hasPosition) {
      addPoint(
        {
          lat: delivery.latitude!,
          lon: delivery.longitude!,
        },
        '#1565C0',
        9,
      );
    }

    (this.map.getSource('delivery-route') as GeoJSONSource).setData(routeData);

    (this.map.getSource('delivery-points') as GeoJSONSource).setData(pointData);

    this.map.resize();

    if (this.fittedOrderId !== delivery.orderId) {
      this.fittedOrderId = delivery.orderId;
      this.fitRoute();
    } else if (this.following && hasPosition) {
      this.map.easeTo({
        center: [delivery.longitude!, delivery.latitude!],
        zoom: 15.5,
        duration: 800,
      });
    }
  }

  private async createMap(): Promise<void> {
    if (this.loadingMap || this.destroyed) return;

    this.loadingMap = true;

    try {
      const { Map, NavigationControl } = await import('maplibre-gl');

      if (this.destroyed || !this.selected) return;

      const container = this.host.nativeElement.querySelector<HTMLElement>(
        '[data-delivery-map]',
      );

      if (!container) return;

      const first = this.selected.route[0];

      const map = new Map({
        container,
        style: 'https://tiles.openfreemap.org/styles/liberty',
        center: first ? [first.lon, first.lat] : [-74.08, 4.65],
        zoom: 12,
      });

      this.map = map;

      map.addControl(
        new NavigationControl({ showCompass: false }),
        'top-right',
      );

      map.on('dragstart', () => {
        this.zone.run(() => {
          this.following = false;
        });
      });

      map.on('error', () => {
        this.zone.run(() => {
          this.mapError =
            'No se pudo cargar parte del mapa. Revisa la conexión.';
        });
      });

      map.on('load', () => {
        if (this.destroyed) return;

        const empty: FeatureCollection = {
          type: 'FeatureCollection',
          features: [],
        };

        map.addSource('delivery-route', {
          type: 'geojson',
          data: empty,
        });

        map.addLayer({
          id: 'delivery-route-line',
          type: 'line',
          source: 'delivery-route',
          layout: {
            'line-join': 'round',
            'line-cap': 'round',
          },
          paint: {
            'line-color': '#B2571F',
            'line-width': 5,
          },
        });

        map.addSource('delivery-points', {
          type: 'geojson',
          data: empty,
        });

        map.addLayer({
          id: 'delivery-point-circles',
          type: 'circle',
          source: 'delivery-points',
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
      this.zone.run(() => {
        this.mapError =
          'No se pudo iniciar el mapa. Revisa la conexión y el soporte WebGL del navegador.';
      });
    } finally {
      this.loadingMap = false;
    }
  }

  private fitRoute(): void {
    const route = this.selected?.route;

    if (!this.map || !this.ready || !route?.length) return;

    const longitudes = route.map((point) => point.lon);
    const latitudes = route.map((point) => point.lat);

    this.map.fitBounds(
      [
        [Math.min(...longitudes), Math.min(...latitudes)],
        [Math.max(...longitudes), Math.max(...latitudes)],
      ],
      {
        padding: 50,
        maxZoom: 16,
        duration: 700,
      },
    );
  }
}

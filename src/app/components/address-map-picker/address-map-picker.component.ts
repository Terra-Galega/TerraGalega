import {
  AfterViewInit,
  Component,
  DestroyRef,
  ElementRef,
  EventEmitter,
  Input,
  NgZone,
  Output,
  inject,
} from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { EMPTY, Subject, catchError, switchMap, timer } from 'rxjs';

import type { Map as MapLibreMap } from 'maplibre-gl';

import { MapAddressService } from '../../services/map-address.service';
import { getErrorMessage } from '../../utils/http-error';

type Lookup =
  | { kind: 'search'; address: string }
  | { kind: 'reverse'; lat: number; lon: number };

@Component({
  selector: 'app-address-map-picker',
  standalone: true,
  templateUrl: './address-map-picker.component.html',
  styleUrl: './address-map-picker.component.scss',
})
export class AddressMapPickerComponent implements AfterViewInit {
  @Input() address = '';
  @Output() picked = new EventEmitter<string>();

  private service = inject(MapAddressService);
  private host = inject<ElementRef<HTMLElement>>(ElementRef);
  private destroyRef = inject(DestroyRef);
  private zone = inject(NgZone);

  private tasks = new Subject<Lookup | null>();
  private map: MapLibreMap | null = null;

  private destroyed = false;
  private userMoving = false;
  private sequence = 0;

  loadingMap = true;
  resolving = false;
  error = '';
  mapError = '';

  constructor() {
    this.tasks
      .pipe(
        switchMap((task) => {
          this.resolving = task !== null;
          this.error = '';

          if (!task) return EMPTY;

          const sequence = this.sequence;

          // Espera a que termine el movimiento y cancela
          // la consulta anterior cuando se elige otro punto.
          return timer(700).pipe(
            switchMap(() =>
              task.kind === 'search'
                ? this.service.search(task.address)
                : this.service.reverse(task.lat, task.lon),
            ),

            switchMap((result) => {
              if (sequence !== this.sequence) return EMPTY;

              this.resolving = false;

              if (!result) {
                this.error =
                  'No se pudo ubicar esa dirección. Mueve el mapa para elegir el punto.';
              } else if (task.kind === 'search') {
                // Centrar no modifica una dirección ya escrita.
                this.map?.jumpTo({
                  center: [result.lon, result.lat],
                  zoom: 16,
                });
              } else {
                this.picked.emit(result.label);
              }

              return EMPTY;
            }),

            catchError((error) => {
              this.resolving = false;
              this.error = getErrorMessage(
                error,
                'No pudimos leer esta dirección. Mueve el mapa o escríbela.',
              );

              return EMPTY;
            }),
          );
        }),

        takeUntilDestroyed(this.destroyRef),
      )
      .subscribe();

    this.destroyRef.onDestroy(() => {
      this.destroyed = true;
      this.sequence++;
      this.map?.remove();
      this.tasks.complete();
    });
  }

  ngAfterViewInit(): void {
    void this.createMap();
  }

  private beginSelection(): void {
    this.userMoving = true;
    this.sequence++;
    this.tasks.next(null);

    // Evita continuar con la dirección anterior mientras
    // se está consultando el punto nuevo.
    this.picked.emit('');
  }

  private async createMap(): Promise<void> {
    try {
      const { Map, NavigationControl } = await import('maplibre-gl');

      if (this.destroyed) return;

      const container =
        this.host.nativeElement.querySelector<HTMLElement>(
          '[data-address-map]',
        );

      if (!container) return;

      const map = new Map({
        container,
        style: 'https://tiles.openfreemap.org/styles/liberty',
        center: [-74.0503, 4.6767],
        zoom: 14,
        dragRotate: false,
        pitchWithRotate: false,
      });

      this.map = map;

      map.touchZoomRotate.disableRotation();

      map.addControl(
        new NavigationControl({ showCompass: false }),
        'top-right',
      );

      map.on('load', () =>
        this.zone.run(() => {
          if (this.destroyed) return;

          this.loadingMap = false;
          this.mapError = '';

          // Solo busca el texto al abrir el selector.
          // No consulta por cada letra que escribe el usuario.
          if (this.address.trim() && !this.userMoving && this.sequence === 0) {
            this.tasks.next({
              kind: 'search',
              address: this.address.trim(),
            });
          }
        }),
      );

      map.on('movestart', (event) =>
        this.zone.run(() => {
          if (event.originalEvent) {
            this.beginSelection();
          }
        }),
      );

      map.on('moveend', () =>
        this.zone.run(() => {
          if (!this.userMoving || this.destroyed) return;

          this.userMoving = false;
          const point = map.getCenter();

          this.tasks.next({
            kind: 'reverse',
            lat: point.lat,
            lon: point.lng,
          });
        }),
      );

      map.on('click', (event) =>
        this.zone.run(() => {
          this.beginSelection();

          // jumpTo emite moveend y permite elegir con un clic.
          map.jumpTo({
            center: event.lngLat,
          });
        }),
      );

      map.on('error', () =>
        this.zone.run(() => {
          this.loadingMap = false;
          this.mapError =
            'No se pudo cargar parte del mapa. Puedes escribir la dirección.';
        }),
      );
    } catch {
      this.zone.run(() => {
        this.loadingMap = false;
        this.mapError =
          'No se pudo abrir el mapa. Puedes escribir la dirección.';
      });
    }
  }
}

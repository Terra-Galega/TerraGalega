import {
  AfterViewInit,
  Component,
  DestroyRef,
  ElementRef,
  EventEmitter,
  Input,
  NgZone,
  OnChanges,
  Output,
  SimpleChanges,
  inject,
} from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { EMPTY, Subject, catchError, switchMap, timer } from 'rxjs';

import type { Map as MapLibreMap } from 'maplibre-gl';

import { MapAddressService } from '../../services/map-address.service';
import { getErrorMessage } from '../../utils/http-error';
import { MAP_LOCALE_ES } from '../../utils/map-locale';

type Lookup =
  | { kind: 'search'; address: string }
  | { kind: 'reverse'; lat: number; lon: number };

@Component({
  selector: 'app-address-map-picker',
  standalone: true,
  templateUrl: './address-map-picker.component.html',
  styleUrl: './address-map-picker.component.scss',
})
// Mapa que acompaña al campo de dirección y se mantiene sincronizado con él:
// - cuando cambia el texto (lo escribe el cliente), centra el mapa en esa dirección;
// - cuando el cliente mueve el mapa o hace clic, emite la dirección de ese punto.
// El texto que llega desde el propio mapa no vuelve a mover el mapa (sin bucles).
export class AddressMapPickerComponent implements AfterViewInit, OnChanges {
  @Input() address = '';
  @Output() picked = new EventEmitter<string>();

  // Última dirección que salió del mapa: si vuelve por @Input no se busca
  private lastPicked: string | null = null;

  ngOnChanges(changes: SimpleChanges): void {
    if (!changes['address'] || changes['address'].firstChange) return;

    const text = this.address.trim();
    if (text === (this.lastPicked ?? '').trim()) return;
    this.lastPicked = null;

    // Muy corto para ser una dirección: no consulta todavía
    if (text.length < 6 || !/\d/.test(text)) {
      this.sequence++;
      this.tasks.next(null);
      this.error = '';
      return;
    }

    // switchMap + timer(700) del constructor: espera a que deje de escribir
    this.sequence++;
    if (!this.loadingMap) {
      this.tasks.next({ kind: 'search', address: text });
    }
  }

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
                  'No ubicamos esa dirección en el mapa. Mueve el mapa hasta el punto exacto.';
              } else if (task.kind === 'search') {
                // Centrar no modifica lo que escribió el cliente (apto, torre...)
                this.map?.flyTo({
                  center: [result.lon, result.lat],
                  zoom: 17,
                  duration: 800,
                });
              } else {
                this.lastPicked = result.label;
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

  // El cliente empezó a mover el mapa: se cancela cualquier búsqueda del texto.
  // El texto se conserva hasta tener la dirección del punto nuevo.
  private beginSelection(): void {
    this.userMoving = true;
    this.sequence++;
    this.tasks.next(null);
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
        // Controles en español (para el lector de pantalla)
        locale: MAP_LOCALE_ES,
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

          // Si ya había texto al abrir el mapa, lo ubica
          if (this.address.trim() && !this.userMoving) {
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

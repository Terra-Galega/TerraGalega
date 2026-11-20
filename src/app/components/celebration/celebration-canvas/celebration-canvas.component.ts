import {
  AfterViewInit,
  Component,
  ElementRef,
  Input,
  OnChanges,
  OnDestroy,
  ViewChild,
} from '@angular/core';
import { Celebration } from '../../../models/celebration';
import { ParticleField } from '../particles';

// Cuánto sigue cayendo la lluvia en la portada (la nieve de Navidad no se detiene)
const HOME_RAIN_MS = 10_000;

// Lienzo con las figuras de la festividad cayendo. Ocupa todo el espacio de su contenedor.
@Component({
  selector: 'app-celebration-canvas',
  templateUrl: './celebration-canvas.component.html',
  styleUrl: './celebration-canvas.component.scss',
})
export class CelebrationCanvasComponent implements AfterViewInit, OnChanges, OnDestroy {
  @Input({ required: true }) celebration!: Celebration;
  // Versión pequeña (tarjeta de la cuenta)
  @Input() compact = false;
  @Input() forceMotion = false;
  @Input() onLight = false;
  @Input() keepGoing = false;
  @Input() startAcross = false;

  @ViewChild('canvas') canvas!: ElementRef<HTMLCanvasElement>;

  private field: ParticleField | null = null;
  private viewReady = false;

  ngAfterViewInit(): void {
    this.viewReady = true;
    this.restart();
  }

  // Si cambia la festividad (vista previa) se vuelve a empezar
  ngOnChanges(): void {
    if (this.viewReady) this.restart();
  }

  ngOnDestroy(): void {
    this.field?.destroy();
  }

  private restart(): void {
    this.field?.destroy();
    const { shape, colors, continuous } = this.celebration;

    this.field = new ParticleField(this.canvas.nativeElement, shape, colors, {
      compact: this.compact,
      forceMotion: this.forceMotion,
      durationMs: this.compact || continuous || this.keepGoing ? undefined : HOME_RAIN_MS,
      onLight: this.onLight,
      startAcross: this.startAcross,
    });
  }
}

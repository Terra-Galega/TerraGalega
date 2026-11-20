import { CelebrationShape } from '../../models/celebration';
import { CELEBRATION_ICONS } from './celebration-icons';

const random = (min: number, max: number) => min + Math.random() * (max - min);

interface Particle {
  x: number;
  y: number;
  size: number;
  speed: number;
  sway: number;
  phase: number;
  rotation: number;
  spin: number;
  alpha: number;
  color: string;
}

export interface ParticleOptions {
  // Versión pequeña para la tarjeta de la cuenta
  compact: boolean;
  // Dibuja con movimiento aunque el usuario pida menos animaciones (vista previa)
  forceMotion: boolean;
  // Cuánto tiempo siguen apareciendo figuras nuevas (ms). Sin valor: todo el rato
  durationMs?: number;
  // Usa trazos oscuros sobre fondos claros.
  onLight: boolean;
  // Distribuye las primeras figuras por toda una página larga.
  startAcross: boolean;
}

interface EngravingPath {
  path: Path2D;
  width: number;
  gold: boolean;
}

// Reutiliza los mismos dibujos que los adornos fijos. Los contornos se compilan
// una sola vez por forma; no se crean objetos Path2D en cada fotograma.
const engravingPaths = new Map<CelebrationShape, EngravingPath[]>();
function pathsFor(shape: CelebrationShape): EngravingPath[] {
  let paths = engravingPaths.get(shape);
  if (!paths) {
    paths = CELEBRATION_ICONS[shape]
      // El rayado de detalle se omite en las partículas pequeñas.
      .filter((part) => part.tone !== 'detail')
      .map((part) => ({
        path: new Path2D(part.d),
        width: part.stroke,
        gold: part.tone === 'gold',
      }));
    engravingPaths.set(shape, paths);
  }
  return paths;
}

function paint(
  ctx: CanvasRenderingContext2D,
  paths: EngravingPath[],
  size: number,
  color: string,
  compact: boolean,
  onLight: boolean,
): void {
  const scale = size / 100;
  ctx.save();
  ctx.scale(scale, scale);
  ctx.translate(-50, -50);
  ctx.lineCap = 'round';
  ctx.lineJoin = 'round';
  for (const part of paths) {
    ctx.strokeStyle = part.gold ? (onLight ? '#99733e' : '#d4b477') : color;
    // Trazo legible en móvil, sin el relleno ni el halo de los antiguos emojis.
    ctx.lineWidth = Math.max(part.width, (compact ? 0.55 : 0.7) / scale);
    ctx.stroke(part.path);
  }
  ctx.restore();
}
// Figuras que caen sobre un canvas. Se detiene con destroy().
// El tamaño lo toma del elemento padre del canvas.
export class ParticleField {
  private readonly ctx: CanvasRenderingContext2D;
  private readonly paths: EngravingPath[];
  private readonly stage: HTMLElement;
  private readonly observer: ResizeObserver;
  // Con "menos animaciones" activado las figuras se dibujan quietas
  private readonly still: boolean;

  private particles: Particle[] = [];
  private populated = false;
  private width = 1;
  private height = 1;
  private frame = 0;
  private last = 0;
  private startedAt = 0;
  private alive = true;

  constructor(
    private readonly canvas: HTMLCanvasElement,
    private readonly shape: CelebrationShape,
    private readonly colors: string[],
    private readonly options: ParticleOptions,
  ) {
    this.ctx = canvas.getContext('2d')!;
    this.paths = pathsFor(shape);
    this.stage = canvas.parentElement ?? canvas;
    this.still =
      window.matchMedia('(prefers-reduced-motion: reduce)').matches && !options.forceMotion;

    this.observer = new ResizeObserver(() => this.resize());
    this.observer.observe(this.stage);
    this.resize();

    if (!this.still) {
      this.startedAt = performance.now();
      this.frame = requestAnimationFrame((time) => this.tick(time));
    }
  }

  destroy(): void {
    this.alive = false;
    cancelAnimationFrame(this.frame);
    this.observer.disconnect();
  }

  private create(initial: boolean): Particle {
    const { compact } = this.options;
    const size = random(compact ? 12 : 22, compact ? 19 : 34);
    // En la portada las figuras empiezan por encima para ir entrando poco a poco
    const y = initial ? (compact || this.options.startAcross ? random(0, this.height) : random(-this.height * 0.15, 0)) : -size;

    return {
      x: random(0, this.width),
      y,
      size,
      speed: compact ? random(0.2, 0.5) : random(0.85, 1.5),
      sway: compact ? random(6, 18) : random(12, 34),
      phase: random(0, 7),
      rotation: random(-0.3, 0.3),
      spin: random(-0.002, 0.002),
      alpha: compact ? random(0.3, 0.5) : random(0.5, 0.75),
      color: this.options.onLight
        ? (Math.random() < 0.5 ? '#5e5143' : '#806744')
        : (this.colors[Math.floor(random(0, this.colors.length))] ?? '#f5f2eb'),
    };
  }

  private resize(): void {
    const rect = this.stage.getBoundingClientRect();
    this.width = Math.max(1, rect.width);
    this.height = Math.max(1, rect.height);

    const ratio = Math.min(window.devicePixelRatio || 1, 2);
    this.canvas.width = Math.round(this.width * ratio);
    this.canvas.height = Math.round(this.height * ratio);
    this.ctx.setTransform(ratio, 0, 0, ratio, 0, 0);

    if (!this.populated) {
      this.populated = true;
      const { compact } = this.options;
      const count = compact ? (this.still ? 3 : 6) : this.still ? 0 : 18;
      for (let i = 0; i < count; i++) this.particles.push(this.create(true));
    }

    if (this.still) this.draw(0);
  }

  private draw(time: number): void {
    const { compact } = this.options;
    this.ctx.clearRect(0, 0, this.width, this.height);

    for (const p of this.particles) {
      const x = p.x + Math.sin(time * 0.0008 + p.phase) * p.sway;
      this.ctx.save();
      this.ctx.translate(x, p.y);
      this.ctx.rotate(p.rotation);
      this.ctx.globalAlpha = p.alpha;
      this.ctx.shadowBlur = 0;

      paint(this.ctx, this.paths, p.size, p.color, compact, this.options.onLight);
      this.ctx.restore();
    }
  }

  private tick(time: number): void {
    if (!this.alive) return;

    const delta = Math.min(2, (time - (this.last || time)) / 16.67);
    this.last = time;

    if (!document.hidden) {
      const { durationMs } = this.options;
      // Pasado el tiempo, las figuras que salen de la pantalla ya no se reemplazan
      const renew = durationMs === undefined || time - this.startedAt < durationMs;

      for (let i = this.particles.length - 1; i >= 0; i--) {
        const p = this.particles[i];
        p.y += p.speed * delta;
        p.rotation += p.spin * delta;

        if (p.y > this.height + p.size) {
          if (renew) this.particles[i] = this.create(false);
          else this.particles.splice(i, 1);
        }
      }
      this.draw(time);
    }

    // Sin figuras ya no hay nada que animar
    if (this.particles.length === 0) return;
    this.frame = requestAnimationFrame((t) => this.tick(t));
  }
}

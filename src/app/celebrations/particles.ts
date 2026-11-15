import { CelebrationShape } from './celebrations.data';

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
}

// Dibuja una figura centrada en (0, 0) del contexto
function paint(ctx: CanvasRenderingContext2D, shape: CelebrationShape, size: number, color: string): void {
  ctx.fillStyle = color;
  ctx.strokeStyle = color;
  ctx.lineWidth = Math.max(1.2, size * 0.075);

  if (shape === 'snow') {
    for (let i = 0; i < 3; i++) {
      ctx.save();
      ctx.rotate((Math.PI * i) / 3);
      ctx.beginPath();
      ctx.moveTo(-size / 2, 0);
      ctx.lineTo(size / 2, 0);
      ctx.stroke();
      ctx.restore();
    }
  } else if (shape === 'heart') {
    ctx.beginPath();
    ctx.moveTo(0, size * 0.42);
    ctx.bezierCurveTo(-size * 0.62, 0, -size * 0.43, -size * 0.46, 0, -size * 0.19);
    ctx.bezierCurveTo(size * 0.43, -size * 0.46, size * 0.62, 0, 0, size * 0.42);
    ctx.fill();
  } else if (shape === 'crown') {
    ctx.beginPath();
    ctx.moveTo(-size * 0.47, -size * 0.25);
    ctx.lineTo(-size * 0.26, size * 0.02);
    ctx.lineTo(0, -size * 0.36);
    ctx.lineTo(size * 0.26, size * 0.02);
    ctx.lineTo(size * 0.47, -size * 0.25);
    ctx.lineTo(size * 0.37, size * 0.34);
    ctx.lineTo(-size * 0.37, size * 0.34);
    ctx.closePath();
    ctx.fill();
  } else if (shape === 'petal') {
    ctx.beginPath();
    ctx.moveTo(-size * 0.34, size * 0.35);
    ctx.bezierCurveTo(-size * 0.55, -size * 0.3, size * 0.05, -size * 0.52, size * 0.36, -size * 0.3);
    ctx.bezierCurveTo(size * 0.38, size * 0.16, size * 0.06, size * 0.45, -size * 0.34, size * 0.35);
    ctx.fill();
  } else if (shape === 'flag') {
    const stripe = size * 0.18;
    ctx.fillStyle = '#fcd116';
    ctx.fillRect(-size * 0.44, -stripe * 2, size * 0.88, stripe * 1.5);
    ctx.fillStyle = '#003893';
    ctx.fillRect(-size * 0.44, -stripe * 0.5, size * 0.88, stripe);
    ctx.fillStyle = '#ce1126';
    ctx.fillRect(-size * 0.44, stripe * 0.5, size * 0.88, stripe);
  } else if (shape === 'ghost') {
    ctx.beginPath();
    ctx.moveTo(-size * 0.4, size * 0.38);
    ctx.lineTo(-size * 0.4, -size * 0.08);
    ctx.arc(0, -size * 0.08, size * 0.4, Math.PI, 0);
    ctx.lineTo(size * 0.4, size * 0.38);
    ctx.lineTo(size * 0.2, size * 0.24);
    ctx.lineTo(0, size * 0.38);
    ctx.lineTo(-size * 0.2, size * 0.24);
    ctx.closePath();
    ctx.fill();
    // Ojos
    ctx.fillStyle = '#372842';
    ctx.beginPath();
    ctx.arc(-size * 0.13, -size * 0.06, size * 0.045, 0, Math.PI * 2);
    ctx.arc(size * 0.13, -size * 0.06, size * 0.045, 0, Math.PI * 2);
    ctx.fill();
  } else {
    // spark: estrella de cuatro puntas
    ctx.beginPath();
    for (let i = 0; i < 8; i++) {
      const angle = (Math.PI * i) / 4 - Math.PI / 2;
      const radius = i % 2 ? size * 0.14 : size * 0.5;
      const x = Math.cos(angle) * radius;
      const y = Math.sin(angle) * radius;
      if (i) ctx.lineTo(x, y);
      else ctx.moveTo(x, y);
    }
    ctx.closePath();
    ctx.fill();
  }
}

// Figuras que caen sobre un canvas. Se detiene con destroy().
// El tamaño lo toma del elemento padre del canvas.
export class ParticleField {
  private readonly ctx: CanvasRenderingContext2D;
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
    const size = random(compact ? 7 : 18, compact ? 15 : 40);
    // En la portada las figuras empiezan por encima para ir entrando poco a poco
    const y = initial ? (compact ? random(0, this.height) : random(-this.height * 0.5, 0)) : -size;

    return {
      x: random(0, this.width),
      y,
      size,
      speed: compact ? random(0.25, 0.75) : random(1.6, 3.2),
      sway: compact ? random(9, 35) : random(20, 80),
      phase: random(0, 7),
      rotation: random(0, 6),
      spin: random(-0.012, 0.012),
      alpha: compact ? random(0.45, 0.9) : random(0.6, 0.95),
      color: this.colors[Math.floor(random(0, this.colors.length))],
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
      const count = compact ? (this.still ? 3 : 10) : this.still ? 0 : 28;
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
      this.ctx.shadowBlur = compact ? 5 : 12;
      this.ctx.shadowColor = p.color;
      paint(this.ctx, this.shape, p.size, p.color);
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

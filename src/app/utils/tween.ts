// Anima una lista de números desde `from` hasta `to` (easeOutCubic).
// Devuelve una función para cancelar la animación en curso.
export function tween(
  from: number[],
  to: number[],
  durationMs: number,
  onFrame: (values: number[]) => void,
): () => void {
  // Si las listas no tienen el mismo largo se parte desde cero
  const start = from.length === to.length ? from : to.map(() => 0);
  const t0 = performance.now();
  let id = 0;

  const step = (now: number) => {
    const p = Math.min(1, (now - t0) / durationMs);
    const eased = 1 - Math.pow(1 - p, 3);
    onFrame(to.map((target, i) => start[i] + (target - start[i]) * eased));
    if (p < 1) id = requestAnimationFrame(step);
  };

  id = requestAnimationFrame(step);
  return () => cancelAnimationFrame(id);
}

// Máximo "redondo" para el eje Y: 1, 2, 2.5, 5 o 10 × una potencia de 10.
export function niceMax(value: number): number {
  if (value <= 0) return 1;
  const pow = Math.pow(10, Math.floor(Math.log10(value)));
  const n = value / pow;
  const step = n <= 1 ? 1 : n <= 2 ? 2 : n <= 2.5 ? 2.5 : n <= 5 ? 5 : 10;
  return step * pow;
}

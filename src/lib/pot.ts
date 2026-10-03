import type { CSSProperties } from 'react';

/** Pot forms by session length. Each form has a coil count and a width profile f(t), t∈[0,1] from base to rim. */
export type Shape = 'bowl' | 'cup' | 'jar' | 'vase';

export const SHAPES: Record<Shape, { n: number; f: (t: number) => number }> = {
  bowl: { n: 6, f: (t) => 0.6 + 0.4 * Math.sin((t * Math.PI) / 2) },
  cup: { n: 9, f: (t) => 0.64 + 0.14 * t },
  jar: { n: 12, f: (t) => 0.48 + 0.52 * Math.sin(Math.PI * (0.1 + 0.78 * t)) },
  vase: {
    n: 14,
    f: (t) => (t < 0.74 ? 0.42 + 0.58 * Math.sin((Math.PI * t) / 0.74) : 0.3 + (t - 0.74) * 1.5),
  },
};

export const ARTICLE: Record<Shape, string> = { bowl: 'A bowl', cup: 'A cup', jar: 'A jar', vase: 'A vase' };

export const shapeOf = (min: number): Shape => (min <= 15 ? 'bowl' : min <= 30 ? 'cup' : min <= 50 ? 'jar' : 'vase');

/** How a pot is drawn: raw clay while growing, a ghost preview, fired in its glaze, glazing (animated), or slumped. */
export type PotMode = 'raw' | 'ghost' | 'fired' | 'glaze' | 'slump';

export interface Coil {
  key: number;
  style: CSSProperties;
}

interface PotOpts {
  /** Animate coil width while growing. */
  anim?: boolean;
  /** For 'glaze' mode: whether the glaze has arrived yet. */
  glazed?: boolean;
  /** Reduced motion: no transitions. */
  rm?: boolean;
}

const INK = '#201e1d';

/**
 * Builds the coil stack for a pot. Coils are listed base-first; render them in a
 * `flex-direction: column-reverse` container so the first coil sits at the bottom.
 */
export function potCoils(
  min: number,
  color: string | null,
  maxW: number,
  coilH: number,
  prog: number,
  mode: PotMode,
  opt: PotOpts = {},
): Coil[] {
  const s = SHAPES[shapeOf(min)];
  const n = s.n;
  const ex = prog * n;
  const k = Math.floor(ex + 1e-6);
  const out: Coil[] = [];
  for (let i = 0; i < n; i++) {
    let fr = i < k ? 1 : i === k && mode === 'raw' ? ex - k : 0;
    if (mode === 'ghost' || mode === 'fired' || mode === 'glaze') fr = 1;
    if (fr <= 0.001 && !(opt.anim && i === k && prog < 1)) break;
    const w = s.f(n === 1 ? 0 : i / (n - 1)) * maxW * Math.max(fr, 0);
    const fired = mode === 'fired' || (mode === 'glaze' && opt.glazed);
    const bg =
      mode === 'ghost'
        ? 'var(--c-ghost)'
        : fired
          ? i % 2
            ? `color-mix(in oklab, ${color} 80%, ${INK})`
            : (color ?? 'var(--c-clay)')
          : i % 2
            ? 'var(--c-clay-2)'
            : 'var(--c-clay)';
    const style: CSSProperties = {
      width: w.toFixed(1) + 'px',
      height: coilH + 'px',
      borderRadius: '999px',
      background: bg,
      flex: 'none',
    };
    if (opt.anim) style.transition = 'width 0.8s linear';
    if (mode === 'glaze' && !opt.rm) style.transition = `background 0.9s ease ${(i * 0.07).toFixed(2)}s`;
    if (mode === 'slump') {
      const j = [-1, 0.6, -0.3, 1, -0.7, 0.4][i % 6];
      style.width = (w * 1.14).toFixed(1) + 'px';
      style.height = (coilH * 0.8).toFixed(1) + 'px';
      style.marginTop = (-coilH * 0.36).toFixed(1) + 'px';
      style.transform = `translateX(${(j * coilH * 0.5).toFixed(1)}px) rotate(${(j * 5).toFixed(1)}deg)`;
    }
    out.push({ key: i, style });
  }
  return out;
}

/** Number of coils fully laid at a given progress. */
export const coilsLaid = (min: number, prog: number) => Math.floor(prog * SHAPES[shapeOf(min)].n + 1e-6);
export const coilCount = (min: number) => SHAPES[shapeOf(min)].n;

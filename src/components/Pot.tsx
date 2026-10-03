import type { CSSProperties } from 'react';
import type { Coil } from '../lib/pot';

/** A coil stack, base first, bottom-aligned. */
export function Pot({ coils, gap = 2, style }: { coils: Coil[]; gap?: number; style?: CSSProperties }) {
  return (
    <div className="stack" style={{ gap, ...style }}>
      {coils.map((c) => (
        <div key={c.key} style={c.style} />
      ))}
    </div>
  );
}

export const Dot = ({ color, size = 10 }: { color: string; size?: number }) => (
  <span className="dot" style={{ width: size, height: size, background: color }} />
);

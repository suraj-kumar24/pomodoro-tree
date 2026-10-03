import { addDays, startOfDay } from './dates';
import type { Session, Tag } from './types';

/** Small deterministic PRNG so sample data looks the same on every device. */
function mulberry32(a: number) {
  return () => {
    a |= 0;
    a = (a + 0x6d2b79f5) | 0;
    let t = Math.imul(a ^ (a >>> 15), 1 | a);
    t = (t + Math.imul(t ^ (t >>> 7), 61 | t)) ^ t;
    return ((t ^ (t >>> 14)) >>> 0) / 4294967296;
  };
}

const pick = <T,>(r: () => number, items: [T, number][]): T => {
  let x = r() * items.reduce((a, [, w]) => a + w, 0);
  for (const [v, w] of items) if ((x -= w) < 0) return v;
  return items[items.length - 1][0];
};

/**
 * About ten weeks of plausible history ending now: mostly 25-minute sessions,
 * a few long writing blocks, some quiet days and the occasional give-up.
 */
export function sampleSessions(tags: Tag[], now: number): Session[] {
  const r = mulberry32(20261002);
  const ids = tags.map((t) => t.id);
  const weights: [string, number][] = ids.map((id, i) => [id, [50, 25, 15, 10][i] ?? 8]);
  const out: Session[] = [];
  const today = startOfDay(now);
  for (let back = 70; back >= 0; back--) {
    const day = addDays(today, -back);
    const weekend = day.getDay() === 0 || day.getDay() === 6;
    if (back > 0 && r() < (weekend ? 0.45 : 0.12)) continue;
    let count = back === 0 ? 2 : Math.max(weekend ? 1 : 2, Math.round(r() * (weekend ? 3 : 6)));
    if (back === 16) count = 11;
    let t = day.getTime() + (8 + r() * 2) * 3_600_000;
    for (let i = 0; i < count; i++) {
      const planned = pick(r, [[25, 70], [50, 10], [15, 6], [90, 4], [10, 4], [45, 3], [60, 3]]);
      const tagId = ids.length ? pick(r, weights) : '';
      const abandoned = r() < 0.07;
      const focused = abandoned ? Math.max(1, Math.round(planned * (0.2 + r() * 0.6))) : planned;
      const end = t + focused * 60_000;
      if (end > now) break;
      out.push({
        id: `demo-${back}-${i}`,
        start: t,
        end,
        planned,
        focused,
        tagId,
        status: abandoned ? 'abandoned' : 'done',
        ...(abandoned ? { reason: r() < 0.7 ? ('gaveup' as const) : ('away' as const) } : {}),
        demo: true,
      });
      t = end + (5 + r() * 40) * 60_000;
    }
  }
  return out;
}

import { Icon } from '../components/Icon';
import { Pot } from '../components/Pot';
import { addDays, dayDiff, dayKey, startOfDay, startOfWeek } from '../lib/dates';
import { WEEKDAYS, fmt, range, shortDate } from '../lib/format';
import { ARTICLE, potCoils, shapeOf } from '../lib/pot';
import { done, minutes } from '../lib/stats';
import type { Session } from '../lib/types';
import { useStore } from '../store';

interface Group {
  key: string;
  title: string;
  sub: string;
  items: Session[];
}

function groupByDay(ss: Session[], now: number): Group[] {
  const map = new Map<string, Session[]>();
  for (const s of ss) {
    const k = dayKey(s.end);
    map.set(k, [...(map.get(k) ?? []), s]);
  }
  return [...map.entries()]
    .sort((a, b) => (a[0] < b[0] ? 1 : -1))
    .map(([k, items]) => {
      const d = startOfDay(items[0].end);
      const diff = dayDiff(d, new Date(now));
      const title = diff === 0 ? 'Today' : diff === 1 ? 'Yesterday' : diff < 7 ? WEEKDAYS[(d.getDay() + 6) % 7] : shortDate(d);
      return { key: k, title, sub: diff < 7 ? shortDate(d) : '', items };
    });
}

function groupByWeek(ss: Session[], now: number): Group[] {
  const map = new Map<number, Session[]>();
  for (const s of ss) {
    const k = startOfWeek(s.end).getTime();
    map.set(k, [...(map.get(k) ?? []), s]);
  }
  const thisWeek = startOfWeek(now).getTime();
  return [...map.entries()]
    .sort((a, b) => b[0] - a[0])
    .map(([k, items]) => {
      const a = new Date(k);
      const r = range(a, addDays(a, 6));
      const back = Math.round(dayDiff(a, new Date(thisWeek)) / 7);
      return back === 0
        ? { key: String(k), title: 'This week', sub: r, items }
        : back === 1
          ? { key: String(k), title: 'Last week', sub: r, items }
          : { key: String(k), title: r, sub: '', items };
    });
}

export function Shelf() {
  const s = useStore();
  const { data, ui } = s;
  const now = Date.now();
  const pots = done(data.sessions).sort((a, b) => a.end - b.end);
  const groups = ui.shelfMode === 'day' ? groupByDay(pots, now) : groupByWeek(pots, now);
  const n = pots.length;

  const tagLine = (items: Session[]) => {
    const c = new Map<string, number>();
    items.forEach((x) => c.set(x.tagId, (c.get(x.tagId) ?? 0) + 1));
    return [...c.entries()]
      .sort((a, b) => b[1] - a[1])
      .map(([t, k]) => `${s.tagOf(t).name} ×${k}`)
      .join(' · ');
  };

  return (
    <div style={{ display: 'flex', flexDirection: 'column', gap: 16, padding: '4px 20px 28px' }}>
      <div style={{ display: 'flex', alignItems: 'baseline', justifyContent: 'space-between', gap: 10, paddingLeft: 4 }}>
        <h1 style={{ margin: 0, fontSize: 'var(--fs-32)' }}>Shelf</h1>
        <div className="muted" style={{ fontSize: 'var(--fs-13)', fontWeight: 700 }}>
          {n} pot{n === 1 ? '' : 's'} fired
        </div>
      </div>
      <div className="seg" role="tablist">
        <button role="tab" aria-selected={ui.shelfMode === 'day'} onClick={() => s.patch({ shelfMode: 'day', pot: null })}>
          By day
        </button>
        <button role="tab" aria-selected={ui.shelfMode === 'week'} onClick={() => s.patch({ shelfMode: 'week', pot: null })}>
          By week
        </button>
      </div>
      {!n && (
        <div style={{ display: 'flex', flexDirection: 'column', gap: 12, padding: '40px 8px 0' }}>
          <div style={{ display: 'flex', gap: 18, alignItems: 'flex-end', padding: '0 18px', opacity: 0.9 }} aria-hidden="true">
            <div style={{ width: 40, height: 44, borderRadius: '14px 14px 10px 10px', border: '2px dashed var(--c-bar)' }} />
            <div style={{ width: 34, height: 64, borderRadius: 999, border: '2px dashed var(--c-bar)' }} />
            <div style={{ width: 48, height: 30, borderRadius: '10px 10px 24px 24px', border: '2px dashed var(--c-bar)' }} />
          </div>
          <div style={{ height: 10, borderRadius: 999, background: 'var(--c-plank)' }} />
          <div className="heading" style={{ fontSize: 'var(--fs-24)', marginTop: 12 }}>
            Your shelf is empty, for now.
          </div>
          <div className="muted" style={{ fontSize: 'var(--fs-15)' }}>
            Every session you finish is fired and placed here, glazed in its tag&rsquo;s colour.
          </div>
        </div>
      )}
      {groups.map((g) => {
        const rows: Session[][] = [];
        for (let i = 0; i < g.items.length; i += 6) rows.push(g.items.slice(i, i + 6));
        return (
          <div key={g.key} style={{ display: 'flex', flexDirection: 'column', gap: 6, marginBottom: 8 }}>
            <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'baseline', gap: 10, padding: '0 4px' }}>
              <div style={{ display: 'flex', alignItems: 'baseline', gap: 8, flexWrap: 'wrap' }}>
                <span className="heading" style={{ fontSize: 'var(--fs-20)' }}>
                  {g.title}
                </span>
                {g.sub && (
                  <span className="muted" style={{ fontSize: 'var(--fs-13)', fontWeight: 600 }}>
                    {g.sub}
                  </span>
                )}
              </div>
              <div className="muted" style={{ fontSize: 'var(--fs-13)', fontWeight: 700, whiteSpace: 'nowrap' }}>
                {fmt(minutes(g.items))} · {g.items.length} pot{g.items.length === 1 ? '' : 's'}
              </div>
            </div>
            {rows.map((r, ri) => (
              <div key={ri} style={{ display: 'flex', flexDirection: 'column' }}>
                <div style={{ display: 'flex', alignItems: 'flex-end', gap: 8, padding: '8px 10px 0', minHeight: 60 }}>
                  {r.map((p) => {
                    const t = s.tagOf(p.tagId);
                    return (
                      <button
                        key={p.id}
                        className="pot-btn"
                        aria-label={`${ARTICLE[shapeOf(p.planned)]}, ${p.planned} minutes, ${t.name}`}
                        onClick={() => s.patch({ pot: p })}
                      >
                        {potCoils(p.planned, t.color, 44, 5, 1, 'fired').map((c) => (
                          <div key={c.key} style={c.style} />
                        ))}
                      </button>
                    );
                  })}
                </div>
                <div style={{ height: 10, borderRadius: 999, background: 'var(--c-plank)' }} />
              </div>
            ))}
            <div className="muted" style={{ fontSize: 'var(--fs-13)', padding: '2px 4px 0', overflowWrap: 'anywhere' }}>
              {tagLine(g.items)}
            </div>
          </div>
        );
      })}
    </div>
  );
}

export function PotSheet() {
  const s = useStore();
  const p = s.ui.pot;
  if (!p) return null;
  const t = s.tagOf(p.tagId);
  const d = new Date(p.end);
  const hh = `${String(d.getHours()).padStart(2, '0')}:${String(d.getMinutes()).padStart(2, '0')}`;
  return (
    <div
      className="sheet-in"
      role="dialog"
      aria-label="Pot details"
      style={{ position: 'absolute', left: 12, right: 12, bottom: 'calc(100px + var(--sab))', zIndex: 25, padding: '14px 10px 14px 18px', borderRadius: 28, background: 'var(--c-sheet)', boxShadow: 'var(--shadow-lg)', display: 'flex', alignItems: 'center', gap: 14 }}
    >
      <Pot coils={potCoils(p.planned, t.color, 40, 4.5, 1, 'fired')} gap={1} style={{ width: 44, flex: 'none' }} />
      <div style={{ flex: 1, minWidth: 0 }}>
        <div style={{ fontWeight: 800, fontSize: 'var(--fs-15)' }}>
          {ARTICLE[shapeOf(p.planned)]} · {p.planned} min
        </div>
        <div className="muted" style={{ fontSize: 'var(--fs-13)', overflowWrap: 'anywhere' }}>
          {t.name} · {shortDate(d)}, {hh}
        </div>
      </div>
      <button onClick={() => s.patch({ pot: null })} aria-label="Close" className="ibtn" style={{ width: 44, height: 44 }}>
        <Icon name="x" size={18} />
      </button>
    </div>
  );
}

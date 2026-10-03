import { Icon } from '../components/Icon';
import { Dot, Pot } from '../components/Pot';
import { ARTICLE, potCoils, shapeOf } from '../lib/pot';
import { isLongBreak } from '../lib/stats';
import { useStore } from '../store';

export function Complete({ rm }: { rm: boolean }) {
  const s = useStore();
  const { ui, data } = s;
  const d = ui.done;
  if (!d) return null;
  const goal = data.settings.goal;
  const tag = s.tagOf(d.tagId);
  const long = isLongBreak(d.n, data.settings.every);
  const bMin = long ? data.settings.long : data.settings.short;

  return (
    <div className="fill" style={{ display: 'flex', flexDirection: 'column', gap: 20, padding: '0 24px calc(40px + var(--sab))' }}>
      <div style={{ display: 'flex', justifyContent: 'flex-end' }}>
        <button onClick={() => s.go('home')} aria-label="Close" className="ibtn filled" style={{ width: 44, height: 44 }}>
          <Icon name="x" size={20} />
        </button>
      </div>
      <div style={{ flex: 1, display: 'flex', flexDirection: 'column', alignItems: 'center', justifyContent: 'center', position: 'relative', minHeight: 250 }}>
        <div style={{ position: 'absolute', width: 260, height: 260, borderRadius: '50%', background: 'var(--c-tint-a)' }} />
        <Pot
          coils={potCoils(d.min, tag.color, 190, 15, 1, 'glaze', { glazed: ui.glazed || rm, rm })}
          style={{ position: 'relative', width: 190 }}
        />
        <div className="wheel" style={{ position: 'relative', width: 220, height: 20, marginTop: 4 }} />
      </div>
      <div style={{ display: 'flex', flexDirection: 'column', gap: 8 }}>
        <div className="kicker" style={{ color: 'var(--c-ink-a)' }}>
          {d.n === goal ? 'Fired · daily goal reached' : `Fired · ${d.n} of ${goal} today`}
        </div>
        <h1 style={{ margin: 0, fontSize: 'var(--fs-32)' }}>{ARTICLE[shapeOf(d.min)]} for your shelf</h1>
        <div className="muted" style={{ display: 'flex', alignItems: 'center', gap: 8, flexWrap: 'wrap', fontSize: 'var(--fs-15)', fontWeight: 600 }}>
          <span>{d.min} min</span>
          <span aria-hidden="true">·</span>
          <Dot color={tag.color} />
          <span style={{ color: 'var(--color-text)', overflowWrap: 'anywhere' }}>{tag.name}</span>
        </div>
      </div>
      <div style={{ padding: 18, borderRadius: 30, background: 'var(--c-card)', display: 'flex', flexDirection: 'column', gap: 10 }}>
        <div style={{ display: 'flex', gap: 12, alignItems: 'center' }}>
          <div style={{ width: 44, height: 44, flex: 'none', borderRadius: 999, background: 'var(--c-tint-s)', color: 'var(--c-ink-s)', display: 'grid', placeItems: 'center' }}>
            <Icon name="cup" size={22} />
          </div>
          <div>
            <div style={{ fontWeight: 700, fontSize: 'var(--fs-17)' }}>{long ? `That’s ${d.n}. Take a long break.` : 'Take a short break.'}</div>
            <div className="muted" style={{ fontSize: 'var(--fs-13)' }}>
              {long ? `${bMin} minutes away from the screen.` : `${bMin} minutes. Stand, stretch, look out a window.`}
            </div>
          </div>
        </div>
        <button onClick={s.startBreak} className="btn btn-break" style={{ width: '100%', minHeight: 54, fontSize: 'var(--fs-17)' }}>
          Start {bMin}-min break
        </button>
        <button onClick={() => s.start()} className="btn btn-secondary btn-out" style={{ width: '100%', minHeight: 54, fontSize: 'var(--fs-17)' }}>
          Start next focus · {data.minutes} min
        </button>
      </div>
    </div>
  );
}

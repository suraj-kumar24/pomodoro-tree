import { Icon } from '../components/Icon';
import { breakRemainSec } from '../lib/engine';
import { mmss } from '../lib/format';
import { SPEED, useStore } from '../store';

/** Full-bleed sage screen so a break never looks like focus mode. */
export function BreakScreen({ rm }: { rm: boolean }) {
  const s = useStore();
  const b = s.data.brk;
  if (!b) return null;
  const tag = s.tagOf(s.data.tagId);

  return (
    <div
      className="fade-in"
      style={{ position: 'absolute', inset: 0, zIndex: 20, background: 'var(--c-break)', color: '#f5ead8', display: 'flex', flexDirection: 'column', paddingTop: 'var(--sat)', overflowY: 'auto' }}
    >
      <div style={{ flex: 1, display: 'flex', flexDirection: 'column', alignItems: 'center', padding: '20px 28px calc(56px + var(--sab))', gap: 18 }}>
        <div style={{ display: 'flex', alignItems: 'center', gap: 8, fontWeight: 800, fontSize: 'var(--fs-15)', letterSpacing: '.06em', textTransform: 'uppercase' }}>
          <Icon name="cup" size={20} />
          <span>{b.long ? 'Long break' : 'Short break'}</span>
        </div>
        <div style={{ flex: 1, display: 'flex', alignItems: 'center', justifyContent: 'center' }}>
          <div style={{ position: 'relative', width: 280, height: 280, display: 'grid', placeItems: 'center' }}>
            <div
              style={{
                position: 'absolute',
                inset: 0,
                borderRadius: '50%',
                background: 'rgba(245,234,216,.1)',
                border: '2px solid rgba(245,234,216,.35)',
                animation: rm ? 'none' : 'coilBreathe 8s ease-in-out infinite',
              }}
            />
            <div role="timer" style={{ position: 'relative', fontWeight: 700, fontSize: 72, lineHeight: 1, fontVariantNumeric: 'tabular-nums', letterSpacing: '-0.03em' }}>
              {mmss(breakRemainSec(b, s.now, SPEED))}
            </div>
          </div>
        </div>
        <div className="heading" style={{ fontSize: 'var(--fs-24)', textAlign: 'center', textWrap: 'balance' }}>
          Stand up. Let your eyes rest on something far away.
        </div>
        <div style={{ fontSize: 'var(--fs-15)', fontWeight: 600, opacity: 0.92, textAlign: 'center', overflowWrap: 'anywhere' }}>
          Up next · {tag.name} · {s.data.minutes} min
        </div>
        <button
          onClick={() => s.endBreak(false)}
          className="btn btn-skip"
          style={{ minHeight: 54, padding: '0 34px', borderRadius: 999, border: '1.5px solid rgba(245,234,216,.7)', color: '#f5ead8', fontSize: 'var(--fs-17)', whiteSpace: 'nowrap' }}
        >
          Skip break
        </button>
      </div>
    </div>
  );
}

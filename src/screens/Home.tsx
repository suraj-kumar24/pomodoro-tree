import { useRef, type KeyboardEvent, type PointerEvent } from 'react';
import { Icon } from '../components/Icon';
import { Dot, Pot } from '../components/Pot';
import { fmt, longDate } from '../lib/format';
import { ARTICLE, potCoils, shapeOf } from '../lib/pot';
import { onDay, minutes, streaks, today } from '../lib/stats';
import { openNotificationSettings } from '../lib/native';
import { useStore } from '../store';

const pct = (m: number) => (((m - 5) / 85) * 100).toFixed(1) + '%';

export function Home() {
  const s = useStore();
  const { data } = s;
  const now = Date.now();
  const goal = data.settings.goal;
  const todays = today(data.sessions, now);
  const doneN = todays.length;
  const todayMin = minutes(todays);
  const st = streaks(data.sessions, now);
  const sel = s.tagOf(data.tagId);
  const m = data.minutes;
  const drag = useRef<HTMLElement | null>(null);

  const setFromX = (x: number, el: HTMLElement) => {
    const r = el.getBoundingClientRect();
    const q = Math.max(0, Math.min(1, (x - r.left) / r.width));
    s.setMinutes(Math.round((5 + q * 85) / 5) * 5);
  };
  const down = (e: PointerEvent<HTMLDivElement>) => {
    const el = e.currentTarget;
    try {
      el.setPointerCapture(e.pointerId);
    } catch {
      /* capture unsupported */
    }
    drag.current = el;
    setFromX(e.clientX, el);
  };
  const key = (e: KeyboardEvent) => {
    if (e.key === 'ArrowRight' || e.key === 'ArrowUp') (e.preventDefault(), s.setMinutes(m + 5));
    if (e.key === 'ArrowLeft' || e.key === 'ArrowDown') (e.preventDefault(), s.setMinutes(m - 5));
  };

  const streakLabel = st.current > 0 ? `${st.current}-day streak` : st.any ? 'New streak today' : 'No streak yet';
  const firstDay = !data.sessions.length;

  return (
    <div className="fill" style={{ display: 'flex', flexDirection: 'column', gap: 18, padding: '4px 24px 22px' }}>
      <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', gap: 12, minHeight: 44 }}>
        <div className="muted" style={{ fontSize: 'var(--fs-15)', fontWeight: 600 }}>
          {longDate(new Date(now))}
        </div>
        <div
          style={{
            display: 'flex',
            alignItems: 'center',
            gap: 6,
            padding: '8px 14px',
            borderRadius: 999,
            background: 'var(--c-tint-a)',
            color: 'var(--c-ink-a)',
            fontSize: 'var(--fs-13)',
            fontWeight: 700,
            whiteSpace: 'nowrap',
          }}
        >
          <Icon name="flame" size={16} />
          <span>{streakLabel}</span>
        </div>
      </div>

      <div style={{ display: 'flex', flexDirection: 'column', gap: 10 }}>
        <div style={{ display: 'flex', alignItems: 'baseline', justifyContent: 'space-between', gap: 12, flexWrap: 'wrap' }}>
          <div className="heading" style={{ fontSize: 'var(--fs-24)', lineHeight: 1.15 }}>
            {doneN > goal ? `${doneN} sessions today` : `${doneN} of ${goal} sessions`}
          </div>
          <div className="muted" style={{ fontSize: 'var(--fs-13)', fontWeight: 600 }}>
            {doneN >= goal ? `${fmt(todayMin)} · goal met` : doneN ? `${fmt(todayMin)} focused` : 'Nothing yet today'}
          </div>
        </div>
        <div style={{ display: 'flex', gap: 6, alignItems: 'center' }} role="img" aria-label={`${doneN} of ${goal} sessions done today`}>
          {[...Array(goal)].map((_, i) => (
            <div key={i} style={{ flex: 1, height: 10, borderRadius: 999, background: i < doneN ? 'var(--color-accent)' : 'var(--c-track)' }} />
          ))}
          {doneN > goal && (
            <div style={{ fontSize: 'var(--fs-13)', fontWeight: 800, color: 'var(--c-ink-a)', paddingLeft: 4 }}>+{doneN - goal}</div>
          )}
        </div>
      </div>

      {s.notifDenied && (
        <div style={{ display: 'flex', gap: 12, alignItems: 'flex-start', padding: '14px 16px 6px', borderRadius: 24, background: 'var(--c-card)' }}>
          <Icon name="bellOff" size={20} style={{ flex: 'none', marginTop: 2 }} />
          <div style={{ flex: 1, display: 'flex', flexDirection: 'column', gap: 2 }}>
            <div style={{ fontSize: 'var(--fs-15)', fontWeight: 700 }}>Notifications are off</div>
            <div className="muted" style={{ fontSize: 'var(--fs-13)' }}>
              The timer still works, but we can&rsquo;t tell you when a session or break ends.
            </div>
            <button
              onClick={() => void openNotificationSettings()}
              style={{ alignSelf: 'flex-start', minHeight: 44, padding: 0, border: 0, background: 'none', color: 'var(--c-ink-a)', fontWeight: 800, fontSize: 'var(--fs-13)', cursor: 'pointer' }}
            >
              Turn on in Settings
            </button>
          </div>
        </div>
      )}
      {st.broken && doneN === 0 && (
        <div style={{ display: 'flex', flexDirection: 'column', gap: 2, padding: '14px 16px', borderRadius: 24, background: 'var(--c-card)' }}>
          <div style={{ fontSize: 'var(--fs-15)', fontWeight: 700 }}>
            Your {st.broken.length}-day streak ended {onDay(st.broken.endedOn, now)}.
          </div>
          <div className="muted" style={{ fontSize: 'var(--fs-13)' }}>
            Your longest is still {st.longest} days. One finished pot today starts a new streak.
          </div>
        </div>
      )}
      {firstDay && (
        <div style={{ padding: '14px 16px', borderRadius: 24, background: 'var(--c-tint-s)', fontSize: 'var(--fs-15)' }}>
          <b>Welcome.</b> Pick a length and tap Start. Your first pot begins with the first coil.
        </div>
      )}

      <div style={{ flex: 1, minHeight: 150, display: 'flex', flexDirection: 'column', alignItems: 'center', justifyContent: 'flex-end' }} aria-hidden="true">
        <Pot coils={potCoils(m, null, 150, 9, 1, 'ghost')} style={{ width: 150 }} />
        <div className="wheel" style={{ width: 190, height: 16, marginTop: 3 }} />
      </div>

      <div style={{ display: 'flex', flexDirection: 'column', gap: 4 }}>
        <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', gap: 8 }}>
          <button onClick={() => s.setMinutes(m - 5)} aria-label="5 minutes shorter" className="ibtn outline" style={{ width: 52, height: 52 }}>
            <Icon name="minus" size={22} />
          </button>
          <div style={{ display: 'flex', alignItems: 'baseline', gap: 6 }}>
            <span className="heading" style={{ fontSize: 'var(--fs-num)', lineHeight: 1, fontVariantNumeric: 'tabular-nums' }}>
              {m}
            </span>
            <span className="muted" style={{ fontSize: 'var(--fs-17)', fontWeight: 700 }}>
              min
            </span>
          </div>
          <button onClick={() => s.setMinutes(m + 5)} aria-label="5 minutes longer" className="ibtn outline" style={{ width: 52, height: 52 }}>
            <Icon name="plus" size={22} />
          </button>
        </div>
        <div className="muted" style={{ textAlign: 'center', fontSize: 'var(--fs-13)', fontWeight: 600 }}>
          {m} minutes makes {ARTICLE[shapeOf(m)].toLowerCase()}
        </div>
        <div
          role="slider"
          tabIndex={0}
          aria-label="Focus length"
          aria-valuemin={5}
          aria-valuemax={90}
          aria-valuenow={m}
          aria-valuetext={`${m} minutes`}
          onPointerDown={down}
          onPointerMove={(e) => drag.current && setFromX(e.clientX, drag.current)}
          onPointerUp={() => (drag.current = null)}
          onPointerCancel={() => (drag.current = null)}
          onKeyDown={key}
          className="slider"
        >
          <div style={{ position: 'absolute', left: 0, right: 0, height: 8, borderRadius: 999, background: 'var(--c-track)' }} />
          <div style={{ position: 'absolute', left: 0, height: 8, borderRadius: 999, background: 'var(--color-accent)', width: pct(m) }} />
          <div
            style={{
              position: 'absolute',
              left: pct(m),
              width: 28,
              height: 28,
              marginLeft: -14,
              borderRadius: 999,
              background: 'var(--color-bg)',
              border: '3px solid var(--color-accent)',
              boxShadow: 'var(--shadow-md)',
            }}
          />
        </div>
        <div className="muted" style={{ position: 'relative', height: 16, margin: '0 14px', fontSize: 'var(--fs-11)', fontWeight: 700 }} aria-hidden="true">
          {[5, 30, 60, 90].map((v) => (
            <span key={v} style={{ position: 'absolute', left: pct(v), transform: 'translateX(-50%)' }}>
              {v}
            </span>
          ))}
        </div>
      </div>

      <div style={{ display: 'flex', flexDirection: 'column', gap: 6 }}>
        <div style={{ display: 'flex', gap: 8, overflowX: 'auto', margin: '0 -24px', padding: '2px 24px' }}>
          {data.tags.map((t) => {
            const on = t.id === sel.id;
            return (
              <button key={t.id} className="chip" onClick={() => s.setTag(t.id)} aria-pressed={on}>
                <Dot color={t.color} size={12} />
                <span className="ellipsis" style={{ maxWidth: 140 }}>
                  {t.name}
                </span>
                {on && <Icon name="check" size={16} stroke={3} />}
              </button>
            );
          })}
          <button
            onClick={s.goTags}
            aria-label="Manage tags"
            className="ibtn"
            style={{ minHeight: 44, width: 44, border: '1.5px dashed var(--color-divider)' }}
          >
            <Icon name="plus" size={18} />
          </button>
        </div>
        {sel.name.length > 18 && (
          <div className="muted clamp2" style={{ fontSize: 'var(--fs-13)', overflowWrap: 'anywhere' }}>
            {sel.name}
          </div>
        )}
      </div>

      <button
        onClick={() => s.start()}
        className="btn btn-primary btn-start"
        style={{ minHeight: 64, flex: 'none', fontSize: 'var(--fs-24)', boxShadow: 'var(--shadow-md)' }}
      >
        Start
      </button>
    </div>
  );
}

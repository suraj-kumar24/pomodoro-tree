import type { MouseEvent } from 'react';
import { Icon } from '../components/Icon';
import { Dot, Pot } from '../components/Pot';
import { elapsedSec } from '../lib/engine';
import { mmss, plural } from '../lib/format';
import { coilCount, coilsLaid, potCoils, shapeOf } from '../lib/pot';
import { SPEED, useStore } from '../store';

export function Running({ rm }: { rm: boolean }) {
  const s = useStore();
  const { data, ui, now } = s;
  const run = data.run;
  if (!run) return null;

  const sec = elapsedSec(run, now, SPEED);
  const prog = Math.min(1, sec / run.totalSec);
  const totMin = Math.round(run.totalSec / 60);
  const n = coilCount(totMin);
  const laid = coilsLaid(totMin, prog);
  const elMin = Math.max(1, Math.floor(sec / 60));
  const paused = run.pausedAt != null;
  const revealed = (ui.confirm || ui.revealedAt != null) && !paused;
  const tag = s.tagOf(run.tagId);
  const left = mmss(run.totalSec - sec);
  const shape = shapeOf(totMin);

  const askGiveUp = (e?: MouseEvent) => {
    e?.stopPropagation();
    s.patch({ confirm: true, revealedAt: Date.now() });
  };

  return (
    <>
      <div
        onClick={() => !ui.confirm && !paused && s.patch({ revealedAt: revealed ? null : Date.now() })}
        className="fill"
        style={{ position: 'relative', display: 'flex', flexDirection: 'column', alignItems: 'center', justifyContent: 'center', padding: '70px 24px 120px', cursor: 'pointer' }}
        aria-label={revealed ? undefined : 'Tap to show the tag and Give up'}
      >
        {revealed && (
          <div className="fade-in" style={{ position: 'absolute', top: 8, left: 24, right: 24, display: 'flex', justifyContent: 'center' }}>
            <div style={{ display: 'flex', alignItems: 'center', gap: 8, maxWidth: '100%', minHeight: 44, padding: '0 18px', borderRadius: 999, background: 'var(--c-card)', fontWeight: 700, fontSize: 'var(--fs-15)' }}>
              <Dot color={tag.color} />
              <span className="ellipsis">{tag.name}</span>
            </div>
          </div>
        )}
        <Pot
          coils={potCoils(totMin, null, 220, 17, prog, 'raw', { anim: !rm })}
          style={{ height: 272, width: 230, justifyContent: 'flex-start' }}
        />
        <div className="wheel" style={{ width: 270, height: 24, marginTop: 4 }} />
        <div
          role="timer"
          aria-label={`${left} left`}
          style={{ marginTop: 40, fontWeight: 700, fontSize: 'var(--fs-timer)', lineHeight: 1, fontVariantNumeric: 'tabular-nums', letterSpacing: '-0.03em' }}
        >
          {left}
        </div>
        {revealed && (
          <div className="muted fade-in" style={{ marginTop: 14, fontSize: 'var(--fs-15)', fontWeight: 600 }}>
            of {totMin} min · coil {Math.min(n, laid + 1)} of {n}
          </div>
        )}
        {paused && (
          <div className="muted" style={{ marginTop: 14, fontSize: 'var(--fs-15)', fontWeight: 700 }}>
            Paused
          </div>
        )}
        {revealed && (
          <div className="fade-in" style={{ position: 'absolute', left: 24, right: 24, bottom: 'calc(48px + var(--sab))', display: 'flex', justifyContent: 'center' }}>
            <button onClick={askGiveUp} className="btn btn-secondary btn-out" style={{ minHeight: 52, padding: '0 30px', fontSize: 'var(--fs-17)', whiteSpace: 'nowrap', flex: 'none' }}>
              Give up
            </button>
          </div>
        )}
      </div>

      {paused && !ui.confirm && (
        <div className="scrim fade-in" style={{ zIndex: 28 }}>
          <div role="dialog" aria-label="Paused for a call" className="sheet sheet-in" style={{ gap: 12 }}>
            <div style={{ width: 48, height: 48, borderRadius: 999, background: 'var(--c-tint-s)', color: 'var(--c-ink-s)', display: 'grid', placeItems: 'center' }}>
              <Icon name="phone" size={22} />
            </div>
            <div className="heading" style={{ fontSize: 'var(--fs-24)' }}>
              Paused for a call
            </div>
            <div style={{ fontSize: 'var(--fs-15)' }}>
              Calls pause your session automatically. Your {plural(laid, 'coil')} {laid === 1 ? 'is' : 'are'} safe — carry on when you&rsquo;re ready.
            </div>
            <div className="muted" style={{ fontSize: 'var(--fs-13)' }}>
              Leaving Coil for anything else gives you 60 seconds to come back before the session ends.
            </div>
            <div style={{ display: 'flex', flexDirection: 'column', gap: 8, marginTop: 6 }}>
              <button onClick={s.resume} className="btn btn-primary btn-start" style={{ minHeight: 56, fontSize: 'var(--fs-17)' }}>
                Resume · {left} left
              </button>
              <button onClick={() => askGiveUp()} className="btn btn-secondary btn-out" style={{ width: '100%', minHeight: 52, fontSize: 'var(--fs-17)' }}>
                Give up
              </button>
            </div>
          </div>
        </div>
      )}

      {ui.confirm && (
        <div className="scrim fade-in">
          <div role="dialog" aria-label="Give up this pot?" className="sheet sheet-in">
            <div className="heading" style={{ fontSize: 'var(--fs-24)' }}>
              Give up this pot?
            </div>
            <div style={{ display: 'flex', flexDirection: 'column', gap: 10, fontSize: 'var(--fs-15)' }}>
              <div style={{ display: 'flex', gap: 10, alignItems: 'flex-start' }}>
                <Icon name="x" size={20} style={{ flex: 'none', marginTop: 1 }} />
                <span>
                  This {shape} won&rsquo;t be fired or go on your shelf — you&rsquo;ve laid {laid} of {n} coils.
                </span>
              </div>
              <div style={{ display: 'flex', gap: 10, alignItems: 'flex-start' }}>
                <Icon name="x" size={20} style={{ flex: 'none', marginTop: 1 }} />
                <span>The {plural(elMin, 'minute')} so far won&rsquo;t count toward today&rsquo;s goal.</span>
              </div>
              <div className="muted" style={{ display: 'flex', gap: 10, alignItems: 'flex-start' }}>
                <Icon name="check" size={20} style={{ flex: 'none', marginTop: 1 }} />
                <span>Your streak and earlier pots stay as they are.</span>
              </div>
            </div>
            <div style={{ display: 'flex', flexDirection: 'column', gap: 8, marginTop: 6 }}>
              <button
                onClick={() => {
                  if (paused) s.resume();
                  s.patch({ confirm: false, revealedAt: Date.now() });
                }}
                className="btn btn-primary btn-start"
                style={{ minHeight: 56, fontSize: 'var(--fs-17)' }}
              >
                Keep focusing
              </button>
              <button
                onClick={() => s.abandon(Date.now(), false)}
                className="btn btn-secondary btn-out"
                style={{ width: '100%', minHeight: 52, fontSize: 'var(--fs-17)', color: 'var(--c-ink-a)' }}
              >
                Give up
              </button>
            </div>
          </div>
        </div>
      )}
    </>
  );
}

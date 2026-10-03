import { Icon } from '../components/Icon';
import { Pot } from '../components/Pot';
import { fmt } from '../lib/format';
import { potCoils } from '../lib/pot';
import { useStore } from '../store';

const TITLES = ['One tap, one pot.', 'Leaving early is allowed.', 'Set a daily goal.'];
const BODIES = [
  'Each focus session builds a pot, coil by coil. Stay to the end and it’s fired and placed on your shelf.',
  'If you give up, the unfired clay slumps and that pot isn’t kept. No penalties, no lectures — just an honest shelf.',
  'Small and steady beats big and brittle. You can change this any time in Settings.',
];

export function Onboarding() {
  const s = useStore();
  const st = s.ui.step;
  const goal = s.data.settings.goal;
  const focus = s.data.settings.focus;

  return (
    <div className="fill" style={{ display: 'flex', flexDirection: 'column', gap: 22, padding: '0 28px 44px' }}>
      <div style={{ display: 'flex', justifyContent: 'flex-end', minHeight: 44 }}>
        {st < 2 && (
          <button
            onClick={() => s.patch({ step: 2 })}
            className="btn btn-ghost"
            style={{ minHeight: 44, padding: '0 12px', fontFamily: 'var(--font-body)', fontWeight: 700, fontSize: 'var(--fs-15)', color: 'var(--c-ink-a)' }}
          >
            Skip
          </button>
        )}
      </div>
      <div style={{ flex: 1, display: 'flex', alignItems: 'center', justifyContent: 'center', position: 'relative', minHeight: 280 }}>
        {st === 0 && (
          <div style={{ position: 'relative', display: 'flex', flexDirection: 'column', alignItems: 'center' }}>
            <div style={{ position: 'absolute', top: -30, width: 280, height: 280, borderRadius: '50%', background: 'var(--c-tint-a)' }} />
            <Pot coils={potCoils(45, null, 200, 15, 0.62, 'raw')} style={{ position: 'relative', height: 200, width: 200 }} />
            <div className="wheel" style={{ position: 'relative', width: 240, height: 22, marginTop: 4 }} />
          </div>
        )}
        {st === 1 && (
          <div style={{ display: 'flex', gap: 22, alignItems: 'flex-end' }}>
            {[
              [potCoils(25, '#d0784a', 130, 15, 1, 'fired'), 'Finished: fired'],
              [potCoils(25, null, 130, 15, 0.6, 'slump'), 'Left early: slumped'],
            ].map(([coils, label]) => (
              <div key={label as string} style={{ display: 'flex', flexDirection: 'column', alignItems: 'center', gap: 10 }}>
                <Pot coils={coils as ReturnType<typeof potCoils>} style={{ height: 170, width: 140 }} />
                <div className="wheel" style={{ width: 150, height: 16 }} />
                <div style={{ fontWeight: 700, fontSize: 'var(--fs-15)' }}>{label as string}</div>
              </div>
            ))}
          </div>
        )}
        {st === 2 && (
          <div style={{ display: 'flex', flexDirection: 'column', alignItems: 'center', gap: 14, width: '100%' }}>
            <div style={{ display: 'flex', alignItems: 'center', gap: 26 }}>
              <button onClick={() => s.setGoal(goal - 1)} aria-label="Fewer sessions" className="ibtn outline" style={{ width: 56, height: 56 }}>
                <Icon name="minus" size={24} />
              </button>
              <div className="heading" style={{ fontSize: 'var(--fs-num)', lineHeight: 1, minWidth: 80, textAlign: 'center' }} aria-live="polite">
                {goal}
              </div>
              <button onClick={() => s.setGoal(goal + 1)} aria-label="More sessions" className="ibtn outline" style={{ width: 56, height: 56 }}>
                <Icon name="plus" size={24} />
              </button>
            </div>
            <div style={{ fontWeight: 700, fontSize: 'var(--fs-17)' }}>sessions a day</div>
            <div className="muted" style={{ fontSize: 'var(--fs-15)' }}>
              About {fmt(goal * focus)} of focus at {focus} min each
            </div>
            <div style={{ display: 'flex', gap: 8, marginTop: 6 }}>
              {[2, 4, 6, 8].map((n) => (
                <button
                  key={n}
                  onClick={() => s.setGoal(n)}
                  aria-pressed={n === goal}
                  style={{
                    minWidth: 56,
                    minHeight: 44,
                    borderRadius: 999,
                    cursor: 'pointer',
                    fontWeight: 800,
                    fontSize: 'var(--fs-15)',
                    color: 'inherit',
                    border: n === goal ? '2px solid var(--color-text)' : '1.5px solid var(--color-divider)',
                    background: n === goal ? 'var(--c-card)' : 'transparent',
                  }}
                >
                  {n}
                </button>
              ))}
            </div>
          </div>
        )}
      </div>
      <div style={{ display: 'flex', flexDirection: 'column', gap: 12 }}>
        <div style={{ display: 'flex', gap: 6 }} aria-label={`Step ${st + 1} of 3`}>
          {[0, 1, 2].map((i) => (
            <div
              key={i}
              style={{
                height: 8,
                width: i === st ? 28 : 8,
                borderRadius: 999,
                background: i === st ? 'var(--color-accent)' : 'var(--c-track)',
                transition: 'width .2s',
              }}
            />
          ))}
        </div>
        <h1 style={{ margin: 0, fontSize: 'var(--fs-32)', textWrap: 'balance' }}>{TITLES[st]}</h1>
        <p className="muted" style={{ margin: 0, fontSize: 'var(--fs-17)', textWrap: 'pretty' }}>
          {BODIES[st]}
        </p>
      </div>
      <button
        onClick={() => (st < 2 ? s.patch({ step: st + 1 }) : s.finishOnboarding())}
        className="btn btn-primary btn-start"
        style={{ minHeight: 60, fontSize: 'var(--fs-20)' }}
      >
        {st < 2 ? 'Next' : 'Start focusing'}
      </button>
    </div>
  );
}

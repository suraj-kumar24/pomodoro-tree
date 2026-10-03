import { Pot } from '../components/Pot';
import { plural } from '../lib/format';
import { potCoils, shapeOf } from '../lib/pot';
import { useStore } from '../store';

export function Abandoned() {
  const s = useStore();
  const L = s.ui.lost;
  if (!L) return null;
  const shape = shapeOf(L.total);

  return (
    <div className="fill" style={{ display: 'flex', flexDirection: 'column', gap: 22, padding: '0 24px calc(40px + var(--sab))' }}>
      <div style={{ minHeight: 44 }} />
      <div style={{ flex: 1, display: 'flex', flexDirection: 'column', alignItems: 'center', justifyContent: 'center', minHeight: 240 }}>
        <Pot coils={potCoils(L.total, null, 200, 16, L.prog, 'slump')} style={{ width: 200 }} />
        <div className="wheel" style={{ width: 240, height: 20, marginTop: 2 }} />
      </div>
      <div style={{ display: 'flex', flexDirection: 'column', gap: 10 }}>
        <div className="kicker muted">{L.away ? 'You were away for over a minute' : 'Session ended early'}</div>
        <h1 style={{ margin: 0, fontSize: 'var(--fs-32)' }}>Back to the clay.</h1>
        <p style={{ margin: 0, fontSize: 'var(--fs-17)', textWrap: 'pretty' }}>
          {L.away
            ? `Coil ends a session when you leave for more than 60 seconds (calls pause it instead). You’d focused for ${plural(L.min, 'minute')}, so this ${shape} won’t be fired.`
            : `You focused for ${plural(L.min, 'minute')}, then stopped. Unfired clay can’t be kept, so this ${shape} won’t go on your shelf or count toward today’s goal.`}
        </p>
        <p className="muted" style={{ margin: 0, fontSize: 'var(--fs-15)' }}>
          Your streak and the pots already on your shelf are untouched.
        </p>
      </div>
      <div style={{ display: 'flex', flexDirection: 'column', gap: 10 }}>
        <button onClick={() => s.start()} className="btn btn-primary btn-start" style={{ minHeight: 60, fontSize: 'var(--fs-20)' }}>
          Start again · {s.data.minutes} min
        </button>
        <button onClick={() => s.go('home')} className="btn btn-secondary btn-out" style={{ width: '100%', minHeight: 54, fontSize: 'var(--fs-17)' }}>
          Back to home
        </button>
      </div>
    </div>
  );
}

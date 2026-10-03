import { Icon } from '../components/Icon';
import { fmt } from '../lib/format';
import { done } from '../lib/stats';
import { PALETTE } from '../lib/types';
import { useStore } from '../store';

export function Tags() {
  const s = useStore();
  const { data, ui } = s;
  const pots = done(data.sessions);

  return (
    <div style={{ display: 'flex', flexDirection: 'column', gap: 14, padding: '4px 20px 28px' }}>
      <div style={{ display: 'flex', alignItems: 'center', gap: 8 }}>
        <button onClick={() => s.go(ui.prev === 'tags' ? 'settings' : ui.prev)} aria-label="Back" className="ibtn" style={{ width: 44, height: 44, marginLeft: -8 }}>
          <Icon name="back" size={22} />
        </button>
        <h1 style={{ margin: 0, fontSize: 'var(--fs-32)', flex: 1 }}>Tags</h1>
        <button
          onClick={() => s.patch({ edit: { id: null, name: '', color: PALETTE[5].hex }, delConfirm: false })}
          className="btn btn-secondary btn-out"
          style={{ minHeight: 44, padding: '0 16px', fontSize: 'var(--fs-15)' }}
        >
          <Icon name="plus" size={16} />
          New
        </button>
      </div>
      <div className="muted" style={{ fontSize: 'var(--fs-15)', paddingLeft: 4 }}>
        Tags glaze your pots and split your stats. Each one also shows its first letter, so colour is never the only clue.
      </div>
      {data.tags.length > 0 ? (
        <div style={{ padding: '4px 8px', borderRadius: 30, background: 'var(--c-card)' }}>
          {data.tags.map((t, i) => {
            const mine = pots.filter((p) => p.tagId === t.id);
            const mins = mine.reduce((a, p) => a + p.focused, 0);
            return (
              <div
                key={t.id}
                style={{ display: 'flex', alignItems: 'center', gap: 14, padding: '12px 4px 12px 8px', minHeight: 68, borderTop: i ? '1px solid var(--color-divider)' : 'none' }}
              >
                <div
                  aria-hidden="true"
                  style={{ width: 40, height: 40, flex: 'none', borderRadius: 999, background: t.color, display: 'grid', placeItems: 'center', color: '#201e1d', fontWeight: 800, fontSize: 'var(--fs-15)' }}
                >
                  {t.name.trim()[0]?.toUpperCase()}
                </div>
                <div style={{ flex: 1, minWidth: 0 }}>
                  <div style={{ fontWeight: 700, fontSize: 'var(--fs-17)', lineHeight: 1.3, overflowWrap: 'anywhere' }}>{t.name}</div>
                  <div className="muted" style={{ fontSize: 'var(--fs-13)' }}>
                    {mine.length ? `${fmt(mins)} in total · ${mine.length} pot${mine.length === 1 ? '' : 's'}` : 'No sessions yet'}
                  </div>
                </div>
                <button
                  onClick={() => s.patch({ edit: { id: t.id, name: t.name, color: t.color }, delConfirm: false })}
                  aria-label={`Edit ${t.name}`}
                  className="ibtn on-card"
                  style={{ width: 44, height: 44 }}
                >
                  <Icon name="pencil" size={18} />
                </button>
              </div>
            );
          })}
        </div>
      ) : (
        <div className="muted" style={{ padding: '18px 20px', borderRadius: 30, background: 'var(--c-card)', fontSize: 'var(--fs-15)' }}>
          No tags yet. Sessions without one are glazed as Untagged.
        </div>
      )}
    </div>
  );
}

export function TagEditor() {
  const s = useStore();
  const E = s.ui.edit;
  if (!E) return null;
  const pots = E.id ? done(s.data.sessions).filter((p) => p.tagId === E.id).length : 0;
  const close = () => s.patch({ edit: null, delConfirm: false });

  return (
    <div className="scrim fade-in" onClick={(e) => e.target === e.currentTarget && close()}>
      <div role="dialog" aria-label={E.id ? 'Edit tag' : 'New tag'} className="sheet sheet-in" style={{ padding: '22px 20px 28px' }}>
        <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between' }}>
          <div className="heading" style={{ fontSize: 'var(--fs-24)' }}>
            {E.id ? 'Edit tag' : 'New tag'}
          </div>
          <button onClick={close} aria-label="Close" className="ibtn filled" style={{ width: 44, height: 44 }}>
            <Icon name="x" size={18} />
          </button>
        </div>
        <label className="field" style={{ display: 'flex', flexDirection: 'column', gap: 6 }}>
          <span className="muted" style={{ fontSize: 'var(--fs-13)', fontWeight: 700 }}>
            Name
          </span>
          <input
            className="input"
            value={E.name}
            onChange={(e) => s.patch({ edit: { ...E, name: e.target.value.slice(0, 60) } })}
            onKeyDown={(e) => e.key === 'Enter' && s.saveTag()}
            maxLength={60}
            placeholder="e.g. Reading"
            autoFocus={!E.id}
            style={{ minHeight: 50, fontSize: 'var(--fs-17)' }}
          />
          <span className="muted" style={{ fontSize: 'var(--fs-11)', fontWeight: 700, alignSelf: 'flex-end' }}>
            {E.name.length}/60
          </span>
        </label>
        <div style={{ display: 'flex', flexDirection: 'column', gap: 8 }}>
          <span className="muted" style={{ fontSize: 'var(--fs-13)', fontWeight: 700 }}>
            Glaze
          </span>
          <div style={{ display: 'grid', gridTemplateColumns: 'repeat(6,1fr)', gap: 4 }}>
            {PALETTE.map((c) => {
              const on = E.color === c.hex;
              return (
                <button
                  key={c.hex}
                  onClick={() => s.patch({ edit: { ...E, color: c.hex } })}
                  aria-label={c.name}
                  aria-pressed={on}
                  style={{ display: 'flex', flexDirection: 'column', alignItems: 'center', gap: 4, border: 0, background: 'none', padding: 0, cursor: 'pointer', color: 'inherit' }}
                >
                  <span
                    style={{ width: 44, height: 44, borderRadius: 999, background: c.hex, display: 'grid', placeItems: 'center', boxShadow: on ? '0 0 0 3px var(--c-sheet), 0 0 0 5px var(--color-text)' : 'none' }}
                  >
                    {on && <Icon name="check" size={20} stroke={3.25} color="#201e1d" />}
                  </span>
                  <span className="muted" style={{ fontSize: 'var(--fs-11)', fontWeight: 700 }}>
                    {c.name}
                  </span>
                </button>
              );
            })}
          </div>
        </div>
        <button onClick={s.saveTag} disabled={!E.name.trim()} className="btn btn-primary btn-start" style={{ minHeight: 56, fontSize: 'var(--fs-17)', marginTop: 4 }}>
          Save
        </button>
        {E.id && !s.ui.delConfirm && (
          <button
            onClick={() => s.patch({ delConfirm: true })}
            className="btn btn-ghost"
            style={{ alignSelf: 'center', minHeight: 44, padding: '0 16px', fontFamily: 'var(--font-body)', fontWeight: 800, fontSize: 'var(--fs-15)', color: 'var(--c-ink-a)', whiteSpace: 'nowrap' }}
          >
            <Icon name="trash" size={18} />
            Delete tag
          </button>
        )}
        {E.id && s.ui.delConfirm && (
          <div style={{ padding: '14px 16px', borderRadius: 24, background: 'var(--c-card)', display: 'flex', flexDirection: 'column', gap: 10 }}>
            <div style={{ fontSize: 'var(--fs-15)', overflowWrap: 'anywhere' }}>
              Delete &ldquo;{s.tagOf(E.id).name}&rdquo;? {pots ? `Its ${pots} pot${pots === 1 ? '' : 's'} stay on your shelf, marked Untagged.` : 'It has no pots yet.'}
            </div>
            <div style={{ display: 'flex', gap: 8 }}>
              <button onClick={() => s.patch({ delConfirm: false })} className="btn btn-secondary btn-out" style={{ flex: 1, minHeight: 48, fontSize: 'var(--fs-15)' }}>
                Keep it
              </button>
              <button onClick={s.deleteTag} className="btn" style={{ flex: 1, minHeight: 48, fontSize: 'var(--fs-15)', background: 'var(--c-ink-a)', color: 'var(--color-bg)', borderRadius: 999 }}>
                Delete
              </button>
            </div>
          </div>
        )}
      </div>
    </div>
  );
}

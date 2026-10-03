import { Icon, type IconName } from '../components/Icon';
import { openNotificationSettings } from '../lib/native';
import type { Appearance, Settings } from '../lib/types';
import { useStore } from '../store';

type NumKey = 'focus' | 'short' | 'long' | 'every' | 'goal';
const STEPPERS: [NumKey, string, string, number, number, number][] = [
  ['focus', 'Focus length', 'min', 5, 90, 5],
  ['short', 'Short break', 'min', 1, 15, 1],
  ['long', 'Long break', 'min', 5, 30, 5],
  ['every', 'Long break after', 'sessions', 2, 8, 1],
  ['goal', 'Daily goal', 'sessions', 1, 12, 1],
];

type BoolKey = 'notif' | 'sound' | 'haptic';
const TOGGLES: [BoolKey, string, string][] = [
  ['notif', 'Notifications', 'When a session or break ends'],
  ['sound', 'Sounds', 'A soft chime at the end'],
  ['haptic', 'Haptics', 'A tap when each coil is laid'],
];

const Section = ({ children, first }: { children: string; first?: boolean }) => (
  <div className="label" style={{ padding: first ? '6px 6px 0' : '10px 6px 0' }}>
    {children}
  </div>
);

function LinkRow({ icon, title, sub, right, chevron, onClick }: { icon: IconName; title: string; sub?: string; right?: string; chevron?: boolean; onClick: () => void }) {
  return (
    <button onClick={onClick} className="row-link">
      <Icon name={icon} />
      <span style={{ flex: 1 }}>
        <span style={{ display: 'block', fontWeight: 600, fontSize: 'var(--fs-15)' }}>{title}</span>
        {sub && (
          <span className="muted" style={{ display: 'block', fontSize: 'var(--fs-13)' }}>
            {sub}
          </span>
        )}
      </span>
      {right && (
        <span className="muted" style={{ fontSize: 'var(--fs-13)', fontWeight: 700 }}>
          {right}
        </span>
      )}
      {chevron && <Icon name="next" size={18} />}
    </button>
  );
}

export function SettingsScreen() {
  const s = useStore();
  const S = s.data.settings;
  const denied = s.notifDenied || (S.notif && s.perm === 'denied');
  const hasDemo = s.data.sessions.some((x) => x.demo);
  const toggles = TOGGLES.filter(([k]) => !(k === 'notif' && denied));

  return (
    <div style={{ display: 'flex', flexDirection: 'column', gap: 10, padding: '4px 20px 28px' }}>
      <h1 style={{ margin: '0 0 6px', fontSize: 'var(--fs-32)', paddingLeft: 4 }}>Settings</h1>

      <Section first>Timer</Section>
      <div style={{ padding: '4px 14px', borderRadius: 30, background: 'var(--c-card)' }}>
        {STEPPERS.map(([k, label, unit, mn, mx, step], i) => {
          const val = S[k];
          const set = (x: number) => (k === 'goal' ? s.setGoal(x) : s.setSetting(k as keyof Settings, x as never));
          return (
            <div key={k} style={{ display: 'flex', alignItems: 'center', gap: 10, minHeight: 60, borderTop: i ? '1px solid var(--color-divider)' : 'none' }}>
              <div style={{ flex: 1, minWidth: 0, fontWeight: 600, fontSize: 'var(--fs-15)' }}>{label}</div>
              <div style={{ display: 'flex', alignItems: 'center', gap: 2 }}>
                <button onClick={() => set(Math.max(mn, val - step))} disabled={val <= mn} aria-label={`Decrease ${label}`} className="ibtn on-card" style={{ width: 44, height: 44 }}>
                  <Icon name="minus" size={18} />
                </button>
                <div style={{ minWidth: 78, textAlign: 'center', fontWeight: 800, fontSize: 'var(--fs-15)', whiteSpace: 'nowrap' }} aria-live="polite">
                  {val} {unit === 'sessions' && val === 1 ? 'session' : unit}
                </div>
                <button onClick={() => set(Math.min(mx, val + step))} disabled={val >= mx} aria-label={`Increase ${label}`} className="ibtn on-card" style={{ width: 44, height: 44 }}>
                  <Icon name="plus" size={18} />
                </button>
              </div>
            </div>
          );
        })}
      </div>

      <Section>Feedback</Section>
      <div style={{ padding: '4px 14px', borderRadius: 30, background: 'var(--c-card)' }}>
        {denied && (
          <div style={{ display: 'flex', flexDirection: 'column', gap: 4, padding: '14px 0' }}>
            <div style={{ display: 'flex', alignItems: 'center', gap: 10 }}>
              <div style={{ flex: 1, fontWeight: 600, fontSize: 'var(--fs-15)' }}>Notifications</div>
              <div className="muted" style={{ fontSize: 'var(--fs-13)', fontWeight: 800 }}>
                Blocked
              </div>
            </div>
            <div className="muted" style={{ fontSize: 'var(--fs-13)' }}>
              Notifications for Coil are turned off in Android settings, so session-end and break-end alerts can&rsquo;t be shown.
            </div>
            <button
              onClick={() => void openNotificationSettings()}
              className="btn btn-secondary btn-out"
              style={{ alignSelf: 'flex-start', minHeight: 44, marginTop: 6, padding: '0 18px', fontSize: 'var(--fs-15)', whiteSpace: 'nowrap' }}
            >
              Open Android settings
            </button>
          </div>
        )}
        {toggles.map(([k, label, sub], i) => (
          <div key={k} style={{ display: 'flex', alignItems: 'center', gap: 12, minHeight: 64, padding: '8px 0', borderTop: i || denied ? '1px solid var(--color-divider)' : 'none' }}>
            <div style={{ flex: 1, minWidth: 0 }}>
              <div style={{ fontWeight: 600, fontSize: 'var(--fs-15)' }}>{label}</div>
              <div className="muted" style={{ fontSize: 'var(--fs-13)' }}>
                {sub}
              </div>
            </div>
            <button onClick={() => s.setSetting(k, !S[k])} role="switch" aria-checked={S[k]} aria-label={label} className="toggle">
              <span />
            </button>
          </div>
        ))}
      </div>

      <Section>Appearance</Section>
      <div className="seg" role="radiogroup" aria-label="Theme">
        {(
          [
            ['light', 'Light'],
            ['dark', 'Dark'],
            ['system', 'System'],
          ] as [Appearance, string][]
        ).map(([k, l]) => (
          <button key={k} role="radio" aria-checked={S.appearance === k} onClick={() => s.setSetting('appearance', k)}>
            {l}
          </button>
        ))}
      </div>

      <Section>Data</Section>
      <div style={{ padding: '4px 14px', borderRadius: 30, background: 'var(--c-card)' }}>
        <LinkRow icon="tag" title="Manage tags" right={String(s.data.tags.length)} chevron onClick={s.goTags} />
        <LinkRow icon="download" title="Export data" sub="Every session as a CSV file" onClick={() => void s.exportData()} />
        <LinkRow
          icon="sparkles"
          title={hasDemo ? 'Remove sample data' : 'Load sample data'}
          sub={hasDemo ? 'Your own sessions stay' : 'Ten weeks of example sessions to explore stats'}
          onClick={hasDemo ? s.removeSample : s.loadSample}
        />
      </div>
      <div className="muted" style={{ fontSize: 'var(--fs-13)', textAlign: 'center', paddingTop: 10 }}>
        Coil 1.0 · your data stays on this phone
      </div>
    </div>
  );
}

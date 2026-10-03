import { useEffect, useState } from 'react';
import { Icon, type IconName } from './components/Icon';
import { deviceInfo, setBarsDark } from './lib/native';
import { useStore, type Screen } from './store';
import { Onboarding } from './screens/Onboarding';
import { Home } from './screens/Home';
import { Running } from './screens/Running';
import { Complete } from './screens/Complete';
import { Abandoned } from './screens/Abandoned';
import { BreakScreen } from './screens/Break';
import { Stats } from './screens/Stats';
import { Shelf, PotSheet } from './screens/Shelf';
import { Tags, TagEditor } from './screens/Tags';
import { SettingsScreen } from './screens/Settings';

const media = (q: string) => (typeof matchMedia === 'function' ? matchMedia(q) : null);

function useMedia(q: string) {
  const [on, setOn] = useState(() => !!media(q)?.matches);
  useEffect(() => {
    const m = media(q);
    if (!m) return;
    const fn = () => setOn(m.matches);
    m.addEventListener('change', fn);
    return () => m.removeEventListener('change', fn);
  }, [q]);
  return on;
}

const params = new URLSearchParams(location.search);

/** Theme, text size and motion preferences, from settings and the system. */
export function usePrefs() {
  const { data } = useStore();
  const sysDark = useMedia('(prefers-color-scheme: dark)');
  const sysRm = useMedia('(prefers-reduced-motion: reduce)');
  const [dev, setDev] = useState({ fontScale: 1, animationsOff: false });
  useEffect(() => {
    void deviceInfo().then(setDev);
  }, []);
  const a = data.settings.appearance;
  const dark = a === 'system' ? sysDark : a === 'dark';
  const large = params.get('text') === 'large' || dev.fontScale >= 1.15;
  const rm = sysRm || dev.animationsOff || params.has('rm');
  return { dark, large, rm };
}

const TABS: [Screen, IconName, string][] = [
  ['home', 'timer', 'Focus'],
  ['stats', 'chart', 'Stats'],
  ['shelf', 'shelf', 'Shelf'],
  ['settings', 'sliders', 'Settings'],
];

export function App() {
  const s = useStore();
  const { ui } = s;
  const { dark, large, rm } = usePrefs();
  const scr = ui.screen;

  useEffect(() => {
    setBarsDark(dark || scr === 'break');
    document.documentElement.style.background = dark ? '#1c1a17' : '#f5ead8';
  }, [dark, scr]);

  const showTabs = ['home', 'stats', 'shelf', 'settings', 'tags'].includes(scr);

  return (
    <div
      className={`app${rm ? '' : ' rm-off'}`}
      data-coil=""
      data-theme={dark ? 'dark' : 'light'}
      data-text={large ? 'large' : 'default'}
    >
      <div className="scroll" key={scr}>
        {scr === 'onboard' && <Onboarding />}
        {scr === 'home' && <Home />}
        {scr === 'running' && <Running rm={rm} />}
        {scr === 'complete' && <Complete rm={rm} />}
        {scr === 'abandoned' && <Abandoned />}
        {scr === 'stats' && <Stats dark={dark} />}
        {scr === 'shelf' && <Shelf />}
        {scr === 'tags' && <Tags />}
        {scr === 'settings' && <SettingsScreen />}
      </div>

      {showTabs && (
        <nav className="tabbar" aria-label="Main">
          {TABS.map(([k, icon, label]) => {
            const on = k === scr || (k === 'settings' && scr === 'tags');
            return (
              <button key={k} onClick={() => s.go(k)} aria-current={on ? 'page' : undefined}>
                <span className="pill">
                  <Icon name={icon} size={22} />
                </span>
                <span className="lab">{label}</span>
              </button>
            );
          })}
        </nav>
      )}

      {ui.pot && <PotSheet />}
      {ui.edit && <TagEditor />}
      {scr === 'break' && <BreakScreen rm={rm} />}
      {ui.toast && (
        <div role="status" className="toast fade-in">
          {ui.toast}
        </div>
      )}
    </div>
  );
}

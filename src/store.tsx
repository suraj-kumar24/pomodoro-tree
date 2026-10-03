import { createContext, useCallback, useContext, useEffect, useMemo, useRef, useState, type ReactNode } from 'react';
import { breakRemainSec, elapsedSec, finishAt, onReturn } from './lib/engine';
import { sessionsCsv } from './lib/csv';
import { sampleSessions } from './lib/demo';
import { ARTICLE, coilsLaid, shapeOf } from './lib/pot';
import * as native from './lib/native';
import { isLongBreak, today as todayDone } from './lib/stats';
import {
  DEFAULT_SETTINGS,
  DEFAULT_TAGS,
  UNTAGGED,
  type Break,
  type Run,
  type Session,
  type Settings,
  type Tag,
} from './lib/types';

export type Screen =
  | 'onboard' | 'home' | 'running' | 'complete' | 'abandoned' | 'break' | 'stats' | 'shelf' | 'tags' | 'settings';

interface Data {
  v: 1;
  onboarded: boolean;
  settings: Settings;
  tags: Tag[];
  sessions: Session[];
  /** Last chosen length and tag, so Home opens ready for one tap. */
  minutes: number;
  tagId: string;
  run: Run | null;
  brk: Break | null;
}

export interface TagEdit {
  id: string | null;
  name: string;
  color: string;
}

interface Ui {
  screen: Screen;
  prev: Screen;
  step: number;
  statsTab: 'week' | 'month';
  weekOff: number;
  monthOff: number;
  shelfMode: 'day' | 'week';
  pot: Session | null;
  confirm: boolean;
  /** When the running screen was last tapped; the tag and Give up show for 5 seconds. */
  revealedAt: number | null;
  edit: TagEdit | null;
  delConfirm: boolean;
  toast: string;
  glazed: boolean;
  done: { min: number; tagId: string; n: number } | null;
  lost: { min: number; prog: number; total: number; away: boolean } | null;
}

const KEY = 'coil.v1';
const REVEAL_MS = 5000;

function load(): Data {
  const fresh: Data = {
    v: 1,
    onboarded: false,
    settings: { ...DEFAULT_SETTINGS },
    tags: DEFAULT_TAGS.map((t) => ({ ...t })),
    sessions: [],
    minutes: DEFAULT_SETTINGS.focus,
    tagId: DEFAULT_TAGS[0].id,
    run: null,
    brk: null,
  };
  try {
    const raw = localStorage.getItem(KEY);
    if (!raw) return fresh;
    const d = JSON.parse(raw) as Data;
    return { ...fresh, ...d, settings: { ...fresh.settings, ...d.settings } };
  } catch {
    return fresh;
  }
}

const params = new URLSearchParams(typeof location !== 'undefined' ? location.search : '');
/** Dev-only time multiplier, e.g. ?speed=40 runs a 25-minute session in about 40 seconds. */
export const SPEED = Math.max(1, Number(params.get('speed')) || 1);

function useStoreValue() {
  const [data, setData] = useState<Data>(load);
  const [ui, setUi] = useState<Ui>(() => ({
    screen: data.run ? 'running' : data.brk ? 'break' : data.onboarded ? 'home' : 'onboard',
    prev: 'home',
    step: 0,
    statsTab: 'week',
    weekOff: 0,
    // Early in a month there's too little to read, so Month opens on the last full one.
    monthOff: new Date().getDate() <= 7 ? -1 : 0,
    shelfMode: 'day',
    pot: null,
    confirm: false,
    revealedAt: null,
    edit: null,
    delConfirm: false,
    toast: '',
    glazed: true,
    done: null,
    lost: null,
  }));
  const [now, setNow] = useState(Date.now);
  const [perm, setPerm] = useState<native.NotifPermission>('granted');

  // Latest state for async callbacks.
  const ref = useRef({ data, ui });
  ref.current = { data, ui };

  useEffect(() => {
    try {
      localStorage.setItem(KEY, JSON.stringify(data));
    } catch {
      /* storage full or unavailable */
    }
  }, [data]);

  const patch = useCallback((p: Partial<Ui> | ((u: Ui) => Partial<Ui>)) => {
    setUi((u) => ({ ...u, ...(typeof p === 'function' ? p(u) : p) }));
  }, []);
  const patchData = useCallback((p: Partial<Data> | ((d: Data) => Partial<Data>)) => {
    setData((d) => ({ ...d, ...(typeof p === 'function' ? p(d) : p) }));
  }, []);

  const toastTimer = useRef<number>(0);
  const flash = useCallback(
    (t: string) => {
      patch({ toast: t });
      clearTimeout(toastTimer.current);
      toastTimer.current = window.setTimeout(() => patch({ toast: '' }), 2600);
    },
    [patch],
  );

  const tagOf = useCallback((id: string) => ref.current.data.tags.find((t) => t.id === id) ?? UNTAGGED, []);

  const refreshPerm = useCallback(() => {
    void native.notifPermission().then(setPerm);
  }, []);

  // — focus —

  const scheduleFocusEnd = (run: Run) => {
    const { settings } = ref.current.data;
    if (!settings.notif) return;
    const min = Math.round(run.totalSec / 60);
    void native.schedule(
      native.NOTIF.focusEnd,
      finishAt(run, SPEED),
      `${ARTICLE[shapeOf(min)]} is fired`,
      `${min} min of ${tagOf(run.tagId).name}. Time for a break.`,
    );
  };

  const start = useCallback(
    (min?: number) => {
      const { data: d } = ref.current;
      const m = min ?? d.minutes;
      const tagId = d.tags.some((t) => t.id === d.tagId) ? d.tagId : (d.tags[0]?.id ?? '');
      const run: Run = { startedAt: Date.now(), totalSec: m * 60, tagId, pausedMs: 0, pausedAt: null, awayAt: null };
      patchData({ run, brk: null, minutes: m, tagId });
      patch({ screen: 'running', revealedAt: null, confirm: false, pot: null });
      void native.cancel(native.NOTIF.breakEnd);
      if (d.settings.notif && perm === 'prompt') void native.requestNotifPermission().then(setPerm);
      scheduleFocusEnd(run);
    },
    [perm],
  );

  const finish = useCallback(
    (endAt: number) => {
      const { data: d } = ref.current;
      const run = d.run;
      if (!run) return;
      const min = Math.round(run.totalSec / 60);
      const s: Session = {
        id: `s${run.startedAt}`,
        start: run.startedAt,
        end: endAt,
        planned: min,
        focused: min,
        tagId: run.tagId,
        status: 'done',
      };
      const sessions = [...d.sessions, s];
      const n = todayDone(sessions, endAt).length;
      patchData({ run: null, sessions });
      patch({ screen: 'complete', done: { min, tagId: run.tagId, n }, glazed: false, confirm: false, revealedAt: null });
      setTimeout(() => patch({ glazed: true }), 120);
      void native.cancel(native.NOTIF.focusEnd);
      if (document.visibilityState === 'visible') {
        if (d.settings.sound) native.chime();
        if (d.settings.haptic) native.tap();
      }
    },
    [patch, patchData],
  );

  const abandon = useCallback(
    (at: number, away: boolean) => {
      const { data: d } = ref.current;
      const run = d.run;
      if (!run) return;
      const sec = elapsedSec(run, at, SPEED);
      const total = Math.round(run.totalSec / 60);
      const focused = Math.floor(sec / 60);
      const s: Session = {
        id: `s${run.startedAt}`,
        start: run.startedAt,
        end: at,
        planned: total,
        focused,
        tagId: run.tagId,
        status: 'abandoned',
        reason: away ? 'away' : 'gaveup',
      };
      patchData({ run: null, sessions: [...d.sessions, s] });
      patch({
        screen: 'abandoned',
        lost: { min: Math.max(1, focused), prog: Math.min(1, sec / run.totalSec), total, away },
        confirm: false,
        revealedAt: null,
      });
      void native.cancel(native.NOTIF.focusEnd);
    },
    [patch, patchData],
  );

  const pause = useCallback(
    (at: number) => {
      patchData((d) => (d.run ? { run: { ...d.run, pausedAt: at, awayAt: null } } : {}));
      void native.cancel(native.NOTIF.focusEnd);
    },
    [patchData],
  );

  const resume = useCallback(() => {
    const run = ref.current.data.run;
    if (!run || run.pausedAt == null) return;
    const next = { ...run, pausedMs: run.pausedMs + (Date.now() - run.pausedAt), pausedAt: null };
    patchData({ run: next });
    patch({ revealedAt: null });
    scheduleFocusEnd(next);
  }, [patch, patchData]);

  // — break —

  const startBreak = useCallback(() => {
    const { data: d, ui: u } = ref.current;
    const n = u.done?.n ?? todayDone(d.sessions, Date.now()).length;
    const long = isLongBreak(n, d.settings.every);
    const min = long ? d.settings.long : d.settings.short;
    const brk: Break = { startedAt: Date.now(), totalSec: min * 60, long };
    patchData({ brk });
    patch({ screen: 'break' });
    if (d.settings.notif)
      void native.schedule(
        native.NOTIF.breakEnd,
        brk.startedAt + (brk.totalSec * 1000) / SPEED,
        "Break's over",
        `Ready when you are · ${tagOf(d.tagId).name} · ${d.minutes} min`,
      );
  }, [patch, patchData, tagOf]);

  const endBreak = useCallback(
    (natural: boolean) => {
      patchData({ brk: null });
      patch({ screen: 'home' });
      void native.cancel(native.NOTIF.breakEnd);
      if (natural) {
        flash("Break's over. Ready when you are.");
        const s = ref.current.data.settings;
        if (document.visibilityState === 'visible') {
          if (s.sound) native.chime();
          if (s.haptic) native.tap();
        }
      }
    },
    [flash, patch, patchData],
  );

  // — ticking —

  const active = !!data.run || !!data.brk;
  useEffect(() => {
    if (!active) return;
    setNow(Date.now());
    const t = setInterval(() => setNow(Date.now()), 250);
    return () => clearInterval(t);
  }, [active]);

  const lastLaid = useRef(-1);
  useEffect(() => {
    const { run, brk, settings } = data;
    if (run && run.pausedAt == null && run.awayAt == null) {
      const sec = elapsedSec(run, now, SPEED);
      if (sec >= run.totalSec) {
        finish(Math.min(now, finishAt(run, SPEED)));
        return;
      }
      const laid = coilsLaid(Math.round(run.totalSec / 60), sec / run.totalSec);
      if (lastLaid.current >= 0 && laid > lastLaid.current && settings.haptic) native.tap();
      lastLaid.current = laid;
    } else lastLaid.current = -1;
    if (brk && breakRemainSec(brk, now, SPEED) <= 0) endBreak(true);
    if (ui.revealedAt && !ui.confirm && now - ui.revealedAt > REVEAL_MS) patch({ revealedAt: null });
  }, [now, data, ui.revealedAt, ui.confirm, finish, endBreak, patch]);

  // — leaving and returning —

  const handleReturn = useCallback(async () => {
    refreshPerm();
    const run = ref.current.data.run;
    if (!run || run.awayAt == null) return;
    const info = await native.awayInfo();
    const t = Date.now();
    const out = onReturn(run, t, info, SPEED);
    if (out.kind === 'call') {
      patchData((d) => (d.run ? { run: { ...d.run, pausedAt: run.awayAt, awayAt: null } } : {}));
      patch({ screen: 'running', confirm: false });
      void native.cancel(native.NOTIF.focusEnd);
    } else if (out.kind === 'finish') {
      finish(out.at);
    } else if (out.kind === 'abandon') {
      abandon(out.at, true);
    } else {
      patchData((d) => (d.run ? { run: { ...d.run, awayAt: null } } : {}));
    }
    setNow(t);
  }, [abandon, finish, patch, patchData, refreshPerm]);

  useEffect(() => {
    refreshPerm();
    // A run restored from storage that was left in the background is judged now.
    if (ref.current.data.run?.awayAt != null) void handleReturn();
    return native.onActiveChange((isActive) => {
      if (isActive) void handleReturn();
      else
        patchData((d) =>
          d.run && d.run.pausedAt == null && d.run.awayAt == null ? { run: { ...d.run, awayAt: Date.now() } } : {},
        );
    });
  }, [handleReturn, patchData, refreshPerm]);

  // — navigation —

  const go = useCallback(
    (screen: Screen, extra?: Partial<Ui>) =>
      patch((u) => ({ screen, prev: u.screen, pot: null, confirm: false, edit: null, delConfirm: false, ...extra })),
    [patch],
  );

  const goTags = useCallback(
    () => patch((u) => ({ screen: 'tags', prev: u.screen === 'tags' ? u.prev : u.screen, pot: null })),
    [patch],
  );

  // Android back button: close the top layer first; returns false to let the app minimise.
  useEffect(
    () =>
      native.onBackButton(() => {
        const { ui: u, data: d } = ref.current;
        if (u.edit) return patch({ edit: null, delConfirm: false }), true;
        if (u.pot) return patch({ pot: null }), true;
        if (u.confirm) return patch({ confirm: false, revealedAt: Date.now() }), true;
        if (u.screen === 'running' && d.run) return patch({ confirm: true, revealedAt: Date.now() }), true;
        if (u.screen === 'onboard' && u.step > 0) return patch({ step: u.step - 1 }), true;
        if (u.screen === 'tags') return go(u.prev === 'tags' ? 'settings' : u.prev), true;
        if (['stats', 'shelf', 'settings', 'complete', 'abandoned'].includes(u.screen)) return go('home'), true;
        return false;
      }),
    [go, patch],
  );

  // — tags, settings, data —

  const actions = useMemo(
    () => ({
      setMinutes: (m: number) => patchData({ minutes: Math.max(5, Math.min(90, m)) }),
      setTag: (id: string) => patchData({ tagId: id }),
      setGoal: (g: number) => patchData((d) => ({ settings: { ...d.settings, goal: Math.max(1, Math.min(12, g)) } })),
      setSetting: <K extends keyof Settings>(k: K, v: Settings[K]) => {
        patchData((d) => ({ settings: { ...d.settings, [k]: v }, ...(k === 'focus' ? { minutes: v as number } : {}) }));
        if (k === 'notif' && v && perm !== 'granted') void native.requestNotifPermission().then(setPerm);
      },
      finishOnboarding: () => {
        patchData({ onboarded: true });
        go('home');
        void native.requestNotifPermission().then(setPerm);
      },
      saveTag: () => {
        const e = ref.current.ui.edit;
        if (!e || !e.name.trim()) return;
        const name = e.name.trim();
        patchData((d) => ({
          tags: e.id
            ? d.tags.map((t) => (t.id === e.id ? { ...t, name, color: e.color } : t))
            : [...d.tags, { id: `t${Date.now()}`, name, color: e.color }],
        }));
        patch({ edit: null });
        flash(e.id ? 'Tag saved' : 'Tag created');
      },
      deleteTag: () => {
        const id = ref.current.ui.edit?.id;
        if (!id) return;
        patchData((d) => {
          const tags = d.tags.filter((t) => t.id !== id);
          return { tags, tagId: d.tagId === id ? (tags[0]?.id ?? '') : d.tagId };
        });
        patch({ edit: null, delConfirm: false });
        flash('Tag deleted');
      },
      loadSample: () => {
        patchData((d) => {
          const real = d.sessions.filter((s) => !s.demo);
          return { sessions: [...sampleSessions(d.tags, Date.now()), ...real].sort((a, b) => a.start - b.start) };
        });
        flash('Sample data loaded');
      },
      removeSample: () => {
        patchData((d) => ({ sessions: d.sessions.filter((s) => !s.demo) }));
        flash('Sample data removed');
      },
      exportData: async () => {
        const d = ref.current.data;
        try {
          flash(await native.exportCsv('coil-sessions.csv', sessionsCsv(d.sessions, d.tags)));
        } catch {
          flash("Couldn't export. Try again.");
        }
      },
    }),
    [flash, go, patch, patchData, perm],
  );

  return {
    data,
    ui,
    now,
    perm,
    notifDenied: data.settings.notif && perm === 'denied',
    tagOf,
    patch,
    flash,
    go,
    goTags,
    start,
    finish,
    abandon,
    pause,
    resume,
    startBreak,
    endBreak,
    ...actions,
  };
}

export type Store = ReturnType<typeof useStoreValue>;

const Ctx = createContext<Store | null>(null);

export function StoreProvider({ children }: { children: ReactNode }) {
  const store = useStoreValue();
  return <Ctx.Provider value={store}>{children}</Ctx.Provider>;
}

export const useStore = () => {
  const s = useContext(Ctx);
  if (!s) throw new Error('useStore outside StoreProvider');
  return s;
};

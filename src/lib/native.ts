import { Capacitor, registerPlugin, SystemBars, SystemBarsStyle } from '@capacitor/core';
import { App } from '@capacitor/app';
import { Haptics, ImpactStyle } from '@capacitor/haptics';
import { LocalNotifications } from '@capacitor/local-notifications';
import { Filesystem, Directory, Encoding } from '@capacitor/filesystem';
import { Share } from '@capacitor/share';

export const isNative = Capacitor.isNativePlatform();

/** What happened while the app was in the background; filled in natively (android/…/DeviceStatePlugin.java). */
export interface AwayInfo {
  /** A call was ringing or active when the app was left, or is still active now. */
  call: boolean;
  /** The screen was turned off while away (phone locked), which isn't leaving the app. */
  screenOff: boolean;
}

interface DeviceStatePlugin {
  awayInfo(): Promise<AwayInfo>;
  info(): Promise<{ fontScale: number; animationsOff: boolean }>;
  openNotificationSettings(): Promise<void>;
}

const DeviceState = registerPlugin<DeviceStatePlugin>('DeviceState');

export async function awayInfo(): Promise<AwayInfo> {
  if (!isNative) return { call: false, screenOff: false };
  try {
    return await DeviceState.awayInfo();
  } catch {
    return { call: false, screenOff: false };
  }
}

export async function deviceInfo() {
  if (!isNative) return { fontScale: 1, animationsOff: false };
  try {
    return await DeviceState.info();
  } catch {
    return { fontScale: 1, animationsOff: false };
  }
}

export async function openNotificationSettings() {
  if (isNative) await DeviceState.openNotificationSettings().catch(() => {});
}

/** Lifecycle: fires `false` when the app leaves the foreground and `true` when it returns. */
export function onActiveChange(cb: (active: boolean) => void): () => void {
  if (isNative) {
    const h = App.addListener('appStateChange', ({ isActive }) => cb(isActive));
    return () => void h.then((x) => x.remove());
  }
  const fn = () => cb(document.visibilityState === 'visible');
  document.addEventListener('visibilitychange', fn);
  return () => document.removeEventListener('visibilitychange', fn);
}

export function onBackButton(cb: () => boolean): () => void {
  if (!isNative) return () => {};
  const h = App.addListener('backButton', () => {
    if (!cb()) void App.minimizeApp();
  });
  return () => void h.then((x) => x.remove());
}

// — notifications —

export type NotifPermission = 'granted' | 'denied' | 'prompt';

export async function notifPermission(): Promise<NotifPermission> {
  if (!isNative) return 'granted';
  try {
    const p = await LocalNotifications.checkPermissions();
    return p.display === 'granted' ? 'granted' : p.display === 'denied' ? 'denied' : 'prompt';
  } catch {
    return 'granted';
  }
}

export async function requestNotifPermission(): Promise<NotifPermission> {
  if (!isNative) return 'granted';
  try {
    const p = await LocalNotifications.requestPermissions();
    return p.display === 'granted' ? 'granted' : 'denied';
  } catch {
    return 'denied';
  }
}

export const NOTIF = { focusEnd: 1, breakEnd: 2, comeBack: 3 } as const;

let channelReady: Promise<void> | null = null;
function ensureChannel() {
  channelReady ??= LocalNotifications.createChannel({
    id: 'timer',
    name: 'Timer',
    description: 'When a focus session or break ends',
    importance: 4,
    visibility: 1,
    vibration: true,
  }).catch(() => {});
  return channelReady;
}

/**
 * USE_EXACT_ALARM normally grants exact alarms. If it's ever missing, ask for inexact ones:
 * requesting exact without the grant makes the plugin open the system settings screen on every call.
 */
async function exactAllowed() {
  try {
    return (await LocalNotifications.checkExactNotificationSetting()).exact_alarm === 'granted';
  } catch {
    return false;
  }
}

export async function schedule(id: number, at: number, title: string, body: string) {
  if (!isNative) return;
  try {
    await ensureChannel();
    await LocalNotifications.cancel({ notifications: [{ id }] });
    await LocalNotifications.schedule({
      notifications: [
        {
          id,
          title,
          body,
          channelId: 'timer',
          isExactNotification: await exactAllowed(),
          schedule: { at: new Date(at), allowWhileIdle: true },
        },
      ],
    });
  } catch {
    /* permission missing: the in-app timer still works */
  }
}

export async function cancel(...ids: number[]) {
  if (!isNative) return;
  await LocalNotifications.cancel({ notifications: ids.map((id) => ({ id })) }).catch(() => {});
}

// — feedback —

export function tap() {
  if (isNative) void Haptics.impact({ style: ImpactStyle.Light }).catch(() => {});
  else navigator.vibrate?.(10);
}

let audio: AudioContext | null = null;
/** A soft two-note chime, synthesised so there's no asset to ship. */
export function chime() {
  try {
    audio ??= new AudioContext();
    const t = audio.currentTime;
    [523.25, 783.99].forEach((f, i) => {
      const o = audio!.createOscillator();
      const g = audio!.createGain();
      o.type = 'sine';
      o.frequency.value = f;
      g.gain.setValueAtTime(0, t + i * 0.18);
      g.gain.linearRampToValueAtTime(0.18, t + i * 0.18 + 0.02);
      g.gain.exponentialRampToValueAtTime(0.0001, t + i * 0.18 + 1.6);
      o.connect(g).connect(audio!.destination);
      o.start(t + i * 0.18);
      o.stop(t + i * 0.18 + 1.7);
    });
  } catch {
    /* no audio */
  }
}

export function setBarsDark(dark: boolean) {
  const meta = document.querySelector('meta[name="theme-color"]');
  meta?.setAttribute('content', dark ? '#1c1a17' : '#f5ead8');
  if (isNative) void SystemBars.setStyle({ style: dark ? SystemBarsStyle.Dark : SystemBarsStyle.Light }).catch(() => {});
}

// — export —

/** Writes the CSV and opens the share sheet (native) or downloads it (web). Returns a confirmation line. */
export async function exportCsv(name: string, csv: string): Promise<string> {
  if (!isNative) {
    const url = URL.createObjectURL(new Blob([csv], { type: 'text/csv' }));
    const a = document.createElement('a');
    a.href = url;
    a.download = name;
    a.click();
    setTimeout(() => URL.revokeObjectURL(url), 1000);
    return `Downloaded ${name}`;
  }
  const res = await Filesystem.writeFile({ path: name, data: csv, directory: Directory.Cache, encoding: Encoding.UTF8 });
  await Share.share({ title: 'Coil sessions', files: [res.uri] }).catch(() => {});
  return `Exported ${name}`;
}

export interface Tag {
  id: string;
  name: string;
  color: string;
}

export interface Session {
  id: string;
  /** Epoch ms when focus began. */
  start: number;
  /** Epoch ms when the session ended (finished or given up). */
  end: number;
  /** Planned length in minutes; decides the pot's form. */
  planned: number;
  /** Minutes actually focused (= planned when done). */
  focused: number;
  tagId: string;
  status: 'done' | 'abandoned';
  /** Why an abandoned session ended. */
  reason?: 'gaveup' | 'away';
  /** Created by "Load sample data". */
  demo?: boolean;
}

export type Appearance = 'light' | 'dark' | 'system';

export interface Settings {
  focus: number;
  short: number;
  long: number;
  every: number;
  goal: number;
  notif: boolean;
  sound: boolean;
  haptic: boolean;
  appearance: Appearance;
}

/** A focus session in progress. Times are wall-clock ms so the timer survives the app being killed. */
export interface Run {
  startedAt: number;
  totalSec: number;
  tagId: string;
  /** Total ms spent paused so far (calls). */
  pausedMs: number;
  /** Set while paused for a call. */
  pausedAt: number | null;
  /** Set while the app is in the background (not paused). */
  awayAt: number | null;
}

export interface Break {
  startedAt: number;
  totalSec: number;
  long: boolean;
}

export const PALETTE = [
  { hex: '#d0784a', name: 'Clay' },
  { hex: '#8a9b6c', name: 'Sage' },
  { hex: '#c49a3a', name: 'Ochre' },
  { hex: '#4f9a92', name: 'Teal' },
  { hex: '#6c88a8', name: 'Slate' },
  { hex: '#a06d8c', name: 'Plum' },
] as const;

export const UNTAGGED: Tag = { id: '', name: 'Untagged', color: '#a19786' };

export const DEFAULT_TAGS: Tag[] = [
  { id: 'study', name: 'Study', color: '#d0784a' },
  { id: 'writing', name: 'Writing', color: '#4f9a92' },
  { id: 'reading', name: 'Reading', color: '#c49a3a' },
  { id: 'admin', name: 'Admin', color: '#6c88a8' },
];

export const DEFAULT_SETTINGS: Settings = {
  focus: 25,
  short: 5,
  long: 15,
  every: 4,
  goal: 4,
  notif: true,
  sound: true,
  haptic: true,
  appearance: 'system',
};

/** Leaving the app for longer than this ends the session (calls pause instead). */
export const AWAY_LIMIT_MS = 60_000;

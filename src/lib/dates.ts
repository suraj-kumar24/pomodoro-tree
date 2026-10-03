import { wd } from './format';

const DAY = 86_400_000;

export const startOfDay = (t: number | Date) => {
  const d = new Date(t);
  d.setHours(0, 0, 0, 0);
  return d;
};

export const addDays = (d: Date, n: number) => {
  const r = new Date(d);
  r.setDate(r.getDate() + n);
  return r;
};

/** Monday 00:00 of the week containing t. */
export const startOfWeek = (t: number | Date) => {
  const d = startOfDay(t);
  return addDays(d, -wd(d));
};

export const startOfMonth = (t: number | Date) => {
  const d = startOfDay(t);
  d.setDate(1);
  return d;
};

export const addMonths = (d: Date, n: number) => new Date(d.getFullYear(), d.getMonth() + n, 1);

export const daysInMonth = (d: Date) => new Date(d.getFullYear(), d.getMonth() + 1, 0).getDate();

/** Stable local-day key, e.g. "2026-10-02". */
export const dayKey = (t: number | Date) => {
  const d = new Date(t);
  return `${d.getFullYear()}-${String(d.getMonth() + 1).padStart(2, '0')}-${String(d.getDate()).padStart(2, '0')}`;
};

export const sameDay = (a: number | Date, b: number | Date) => dayKey(a) === dayKey(b);

/** Whole calendar days between two dates (DST-safe). */
export const dayDiff = (a: Date, b: Date) => Math.round((startOfDay(b).getTime() - startOfDay(a).getTime()) / DAY);

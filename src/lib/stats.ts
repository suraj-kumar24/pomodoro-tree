import { addDays, addMonths, dayDiff, dayKey, daysInMonth, startOfDay, startOfMonth, startOfWeek } from './dates';
import { WEEKDAYS, dayMon, sum } from './format';
import type { Session } from './types';

export const done = (ss: Session[]) => ss.filter((s) => s.status === 'done');

/** Sessions whose end falls within [from, to). */
export const between = (ss: Session[], from: Date, to: Date) => {
  const a = from.getTime();
  const b = to.getTime();
  return ss.filter((s) => s.end >= a && s.end < b);
};

export const minutes = (ss: Session[]) => sum(ss.map((s) => s.focused));

export const today = (ss: Session[], now: number) => {
  const d = startOfDay(now);
  return done(between(ss, d, addDays(d, 1)));
};

export interface Streaks {
  current: number;
  longest: number;
  /** Set when a streak ended recently and nothing is finished today. */
  broken: { length: number; endedOn: Date } | null;
  /** True once any session has ever been finished. */
  any: boolean;
}

/** A streak is a run of consecutive days with at least one finished pot. Today without a pot doesn't break it yet. */
export function streaks(ss: Session[], now: number): Streaks {
  const days = [...new Set(done(ss).map((s) => dayKey(s.end)))].sort();
  if (!days.length) return { current: 0, longest: 0, broken: null, any: false };
  const toDate = (k: string) => {
    const [y, m, d] = k.split('-').map(Number);
    return new Date(y, m - 1, d);
  };
  let longest = 1;
  let run = 1;
  for (let i = 1; i < days.length; i++) {
    run = dayDiff(toDate(days[i - 1]), toDate(days[i])) === 1 ? run + 1 : 1;
    longest = Math.max(longest, run);
  }
  // Length of the run ending at the last recorded day.
  let tail = 1;
  for (let i = days.length - 1; i > 0 && dayDiff(toDate(days[i - 1]), toDate(days[i])) === 1; i--) tail++;
  const last = toDate(days[days.length - 1]);
  const gap = dayDiff(last, new Date(now));
  const current = gap <= 1 ? tail : 0;
  const endedOn = addDays(last, 1);
  const broken = current === 0 && dayDiff(endedOn, new Date(now)) <= 6 && tail > 1 ? { length: tail, endedOn } : null;
  return { current, longest, broken, any: true };
}

/** "Wednesday" within the past week, otherwise "14 Sep". */
export const dayName = (d: Date, now: number) => {
  const diff = dayDiff(d, new Date(now));
  if (diff === 0) return 'today';
  if (diff === 1) return 'yesterday';
  return diff < 7 ? WEEKDAYS[(d.getDay() + 6) % 7] : dayMon(d);
};

/** "yesterday", "on Wednesday", "on 14 Sep" */
export const onDay = (d: Date, now: number) => {
  const n = dayName(d, now);
  return n === 'today' || n === 'yesterday' ? n : `on ${n}`;
};

export interface TagShare {
  tagId: string;
  min: number;
}

export const byTag = (ss: Session[]): TagShare[] => {
  const agg = new Map<string, number>();
  for (const s of done(ss)) agg.set(s.tagId, (agg.get(s.tagId) ?? 0) + s.focused);
  return [...agg.entries()].map(([tagId, min]) => ({ tagId, min })).sort((a, b) => b.min - a.min);
};

export interface WeekStats {
  start: Date;
  end: Date;
  /** Focus minutes per day, Monday first. */
  days: number[];
  /** Whether each day has at least one finished pot. */
  dayDone: boolean[];
  total: number;
  prevTotal: number;
  completed: number;
  abandoned: number;
  tags: TagShare[];
}

export function week(ss: Session[], weekStart: Date): WeekStats {
  const start = startOfWeek(weekStart);
  const end = addDays(start, 7);
  const inWeek = between(ss, start, end);
  const days = [...Array(7)].map((_, i) => done(between(inWeek, addDays(start, i), addDays(start, i + 1))));
  return {
    start,
    end: addDays(start, 6),
    days: days.map(minutes),
    dayDone: days.map((d) => d.length > 0),
    total: minutes(done(inWeek)),
    prevTotal: minutes(done(between(ss, addDays(start, -7), start))),
    completed: done(inWeek).length,
    abandoned: inWeek.length - done(inWeek).length,
    tags: byTag(inWeek),
  };
}

export interface MonthStats {
  start: Date;
  /** Focus minutes per day of month (index 0 = the 1st). */
  days: number[];
  /** Days counted toward the average: all of them for past months, elapsed ones for the current month. */
  elapsed: number;
  total: number;
  prevTotal: number;
  /** Previous month's focus over the same number of days, for a fair in-progress comparison. */
  prevSameDays: number;
  best: number;
  tags: TagShare[];
}

export function month(ss: Session[], monthStart: Date, now: number): MonthStats {
  const start = startOfMonth(monthStart);
  const end = addMonths(start, 1);
  const n = daysInMonth(start);
  const inMonth = done(between(ss, start, end));
  const days = [...Array(n)].map((_, i) => minutes(between(inMonth, addDays(start, i), addDays(start, i + 1))));
  const cur = startOfMonth(now).getTime() === start.getTime();
  const total = sum(days);
  const max = Math.max(...days);
  const elapsed = cur ? new Date(now).getDate() : n;
  const prev = addMonths(start, -1);
  return {
    start,
    days,
    elapsed,
    total,
    prevTotal: minutes(done(between(ss, prev, start))),
    prevSameDays: minutes(done(between(ss, prev, addDays(prev, Math.min(elapsed, daysInMonth(prev)))))),
    best: max > 0 ? days.indexOf(max) : -1,
    tags: byTag(inMonth),
  };
}

/** The long break comes after every Nth finished session of the day. */
export const isLongBreak = (doneToday: number, every: number) => doneToday > 0 && doneToday % every === 0;

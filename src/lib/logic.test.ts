import { describe, expect, it } from 'vitest';
import { elapsedSec, finishAt, onReturn } from './engine';
import { fmt, fmtS, mmss, range } from './format';
import { coilCount, coilsLaid, potCoils, shapeOf } from './pot';
import { isLongBreak, month, streaks, today, week } from './stats';
import { sampleSessions } from './demo';
import { sessionsCsv } from './csv';
import { DEFAULT_TAGS, type Run, type Session } from './types';

const at = (y: number, m: number, d: number, h = 10, min = 0) => new Date(y, m - 1, d, h, min).getTime();
const NOW = at(2026, 10, 2, 15); // Friday 2 October 2026

let id = 0;
const sess = (end: number, focused = 25, status: Session['status'] = 'done', tagId = 'study'): Session => ({
  id: String(id++),
  start: end - focused * 60_000,
  end,
  planned: 25,
  focused,
  tagId,
  status,
});

describe('format', () => {
  it('formats durations', () => {
    expect(fmt(0)).toBe('0m');
    expect(fmt(45)).toBe('45m');
    expect(fmt(120)).toBe('2h');
    expect(fmt(100)).toBe('1h 40m');
    expect(fmtS(100)).toBe('1h40');
    expect(fmtS(65)).toBe('1h05');
    expect(mmss(1500)).toBe('25:00');
    expect(mmss(0.2)).toBe('0:01');
    expect(range(new Date(2026, 8, 28), new Date(2026, 9, 4))).toBe('28 Sep – 4 Oct');
    expect(range(new Date(2026, 8, 21), new Date(2026, 8, 27))).toBe('21 – 27 Sep');
  });
});

describe('pot', () => {
  it('maps length to form', () => {
    expect([10, 15, 25, 30, 45, 50, 60, 90].map(shapeOf)).toEqual(['bowl', 'bowl', 'cup', 'cup', 'jar', 'jar', 'vase', 'vase']);
  });
  it('lays coils with progress', () => {
    expect(coilCount(25)).toBe(9);
    expect(coilsLaid(25, 0.56)).toBe(5);
    expect(potCoils(25, null, 220, 17, 0, 'raw')).toHaveLength(0);
    expect(potCoils(25, null, 220, 17, 0.56, 'raw')).toHaveLength(6); // 5 whole + the one drawing out
    expect(potCoils(25, '#d0784a', 130, 15, 1, 'fired')).toHaveLength(9);
    expect(potCoils(25, null, 150, 9, 1, 'ghost')[0].style.background).toBe('var(--c-ghost)');
  });
});

describe('streaks', () => {
  it('counts consecutive days and keeps today open', () => {
    const ss = [sess(at(2026, 9, 29)), sess(at(2026, 9, 30)), sess(at(2026, 10, 1))];
    expect(streaks(ss, NOW)).toMatchObject({ current: 3, longest: 3, broken: null, any: true });
    expect(streaks([...ss, sess(at(2026, 10, 2, 11))], NOW).current).toBe(4);
  });
  it('reports a recently broken streak', () => {
    const ss = [sess(at(2026, 9, 27)), sess(at(2026, 9, 28)), sess(at(2026, 9, 29))];
    const st = streaks(ss, NOW);
    expect(st.current).toBe(0);
    expect(st.longest).toBe(3);
    expect(st.broken?.length).toBe(3);
    expect(st.broken?.endedOn.getDate()).toBe(30); // Wednesday
  });
  it('ignores given-up sessions', () => {
    expect(streaks([sess(at(2026, 10, 1), 10, 'abandoned')], NOW)).toMatchObject({ current: 0, any: false });
  });
});

describe('week and month', () => {
  const ss = [
    sess(at(2026, 9, 28), 50),
    sess(at(2026, 9, 28, 14), 25, 'done', 'writing'),
    sess(at(2026, 9, 30), 12, 'abandoned'),
    sess(at(2026, 10, 2, 9), 25),
    sess(at(2026, 9, 22), 90), // last week
  ];
  it('summarises the week', () => {
    const w = week(ss, new Date(NOW));
    expect(w.start.getDate()).toBe(28);
    expect(w.days).toEqual([75, 0, 0, 0, 25, 0, 0]);
    expect(w.total).toBe(100);
    expect(w.prevTotal).toBe(90);
    expect(w.completed).toBe(3);
    expect(w.abandoned).toBe(1);
    expect(w.tags).toEqual([
      { tagId: 'study', min: 75 },
      { tagId: 'writing', min: 25 },
    ]);
  });
  it('summarises the month', () => {
    const m = month(ss, new Date(2026, 8, 1), NOW);
    expect(m.days).toHaveLength(30);
    expect(m.total).toBe(165);
    expect(m.elapsed).toBe(30);
    expect(m.best).toBe(21); // 22 Sep, 90 min
    const cur = month(ss, new Date(NOW), NOW);
    expect(cur.elapsed).toBe(2);
    expect(cur.prevTotal).toBe(165);
  });
  it('counts today and long breaks', () => {
    expect(today(ss, NOW)).toHaveLength(1);
    expect(isLongBreak(4, 4)).toBe(true);
    expect(isLongBreak(3, 4)).toBe(false);
  });
});

describe('engine', () => {
  const run: Run = { startedAt: NOW, totalSec: 1500, tagId: 'study', pausedMs: 0, pausedAt: null, awayAt: null };
  it('excludes paused time', () => {
    expect(elapsedSec(run, NOW + 60_000)).toBe(60);
    expect(elapsedSec({ ...run, pausedMs: 30_000 }, NOW + 60_000)).toBe(30);
    expect(elapsedSec({ ...run, pausedAt: NOW + 10_000 }, NOW + 60_000)).toBe(10);
    expect(finishAt({ ...run, pausedMs: 5000 })).toBe(NOW + 1_505_000);
  });
  const away = (leftAfter: number) => ({ ...run, awayAt: NOW + leftAfter });
  const plain = { call: false, screenOff: false };
  it('gives 60 seconds to come back', () => {
    expect(onReturn(away(60_000), NOW + 100_000, plain).kind).toBe('continue');
    expect(onReturn(away(60_000), NOW + 130_000, plain)).toEqual({ kind: 'abandon', at: NOW + 60_000, focusedSec: 60 });
  });
  it('pauses for calls and ignores locking', () => {
    expect(onReturn(away(60_000), NOW + 900_000, { call: true, screenOff: false }).kind).toBe('call');
    expect(onReturn(away(60_000), NOW + 900_000, { call: false, screenOff: true }).kind).toBe('continue');
    expect(onReturn(away(60_000), NOW + 2_000_000, { call: false, screenOff: true })).toEqual({ kind: 'finish', at: NOW + 1_500_000 });
  });
  it('finishes a session that ended within the grace period', () => {
    expect(onReturn(away(1_480_000), NOW + 1_700_000, plain)).toEqual({ kind: 'finish', at: NOW + 1_500_000 });
  });
});

describe('demo and export', () => {
  it('generates history ending before now', () => {
    const ss = sampleSessions(DEFAULT_TAGS, NOW);
    expect(ss.length).toBeGreaterThan(100);
    expect(ss.every((s) => s.end <= NOW && s.demo)).toBe(true);
    expect(ss.some((s) => s.status === 'abandoned')).toBe(true);
  });
  it('writes CSV with escaping', () => {
    const csv = sessionsCsv([sess(at(2026, 10, 1, 9, 30), 25, 'done', 'x')], [{ id: 'x', name: 'Thesis, "ch. 3"', color: '#000' }]);
    expect(csv.split('\n')[1]).toBe('2026-10-01,09:05,09:30,25,25,"Thesis, ""ch. 3""",completed,cup');
  });
});

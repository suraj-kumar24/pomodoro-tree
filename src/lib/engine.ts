import type { AwayInfo } from './native';
import { AWAY_LIMIT_MS, type Break, type Run } from './types';

/** Focused seconds so far. `speed` > 1 is a dev-only time multiplier (?speed=40). */
export const elapsedSec = (run: Run, now: number, speed = 1) =>
  Math.max(0, (((run.pausedAt ?? now) - run.startedAt - run.pausedMs) / 1000) * speed);

/** Wall-clock ms at which the run reaches its full length, assuming no further pauses. */
export const finishAt = (run: Run, speed = 1) => run.startedAt + run.pausedMs + (run.totalSec * 1000) / speed;

export const breakRemainSec = (b: Break, now: number, speed = 1) => b.totalSec - ((now - b.startedAt) / 1000) * speed;

export type ReturnOutcome =
  | { kind: 'continue' }
  | { kind: 'call' }
  | { kind: 'finish'; at: number }
  | { kind: 'abandon'; at: number; focusedSec: number };

/**
 * Decides what happens to a session when the app comes back to the foreground.
 * Calls pause the session; locking the phone isn't leaving; anything else gets 60 seconds.
 */
export function onReturn(run: Run, now: number, info: AwayInfo, speed = 1): ReturnOutcome {
  if (run.awayAt == null || run.pausedAt != null) return { kind: 'continue' };
  if (info.call) return { kind: 'call' };
  const leftAt = run.awayAt;
  const end = finishAt(run, speed);
  const forgiven = info.screenOff;
  if (now >= end) {
    if (forgiven || end - leftAt <= AWAY_LIMIT_MS) return { kind: 'finish', at: end };
  } else if (forgiven || now - leftAt <= AWAY_LIMIT_MS) {
    return { kind: 'continue' };
  }
  return { kind: 'abandon', at: leftAt, focusedSec: elapsedSec(run, leftAt, speed) };
}

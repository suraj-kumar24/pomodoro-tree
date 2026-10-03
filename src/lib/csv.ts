import { dayKey } from './dates';
import { shapeOf } from './pot';
import { UNTAGGED, type Session, type Tag } from './types';

const cell = (v: string | number) => {
  const s = String(v);
  return /[",\n]/.test(s) ? `"${s.replace(/"/g, '""')}"` : s;
};

const time = (t: number) => {
  const d = new Date(t);
  return `${String(d.getHours()).padStart(2, '0')}:${String(d.getMinutes()).padStart(2, '0')}`;
};

export function sessionsCsv(sessions: Session[], tags: Tag[]): string {
  const name = (id: string) => tags.find((t) => t.id === id)?.name ?? UNTAGGED.name;
  const rows = [['date', 'start', 'end', 'planned_min', 'focused_min', 'tag', 'status', 'pot']];
  for (const s of [...sessions].sort((a, b) => a.start - b.start)) {
    rows.push([
      dayKey(s.start),
      time(s.start),
      time(s.end),
      String(s.planned),
      String(s.focused),
      name(s.tagId),
      s.status === 'done' ? 'completed' : s.reason === 'away' ? 'left app' : 'gave up',
      s.status === 'done' ? shapeOf(s.planned) : '',
    ]);
  }
  return rows.map((r) => r.map(cell).join(',')).join('\n') + '\n';
}

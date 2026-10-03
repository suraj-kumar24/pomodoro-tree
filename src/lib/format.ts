/** "1h 40m", "45m", "2h" */
export const fmt = (m: number) => {
  m = Math.round(m);
  const h = Math.floor(m / 60);
  const r = m % 60;
  return h ? (r ? `${h}h ${r}m` : `${h}h`) : `${r}m`;
};

/** Compact form for chart labels: "1h40", "45m", "2h" */
export const fmtS = (m: number) => {
  m = Math.round(m);
  const h = Math.floor(m / 60);
  const r = m % 60;
  return h ? `${h}h${r ? String(r).padStart(2, '0') : ''}` : `${r}m`;
};

/** Seconds → "m:ss", rounding up so the display never shows 0:00 early. */
export const mmss = (s: number) => {
  s = Math.max(0, Math.ceil(s));
  return `${Math.floor(s / 60)}:${String(s % 60).padStart(2, '0')}`;
};

export const sum = (a: number[]) => a.reduce((x, y) => x + y, 0);

export const plural = (n: number, word: string, many = word + 's') => `${n} ${n === 1 ? word : many}`;

export const WEEKDAYS = ['Monday', 'Tuesday', 'Wednesday', 'Thursday', 'Friday', 'Saturday', 'Sunday'];
export const WD3 = ['Mon', 'Tue', 'Wed', 'Thu', 'Fri', 'Sat', 'Sun'];
export const MONTHS = [
  'January', 'February', 'March', 'April', 'May', 'June',
  'July', 'August', 'September', 'October', 'November', 'December',
];
export const MON3 = ['Jan', 'Feb', 'Mar', 'Apr', 'May', 'Jun', 'Jul', 'Aug', 'Sep', 'Oct', 'Nov', 'Dec'];

/** Monday-based weekday index, 0 = Monday. */
export const wd = (d: Date) => (d.getDay() + 6) % 7;

/** "Friday, 2 October" */
export const longDate = (d: Date) => `${WEEKDAYS[wd(d)]}, ${d.getDate()} ${MONTHS[d.getMonth()]}`;
/** "Fri 2 Oct" */
export const shortDate = (d: Date) => `${WD3[wd(d)]} ${d.getDate()} ${MON3[d.getMonth()]}`;
/** "2 Oct" */
export const dayMon = (d: Date) => `${d.getDate()} ${MON3[d.getMonth()]}`;

/** "28 Sep – 4 Oct", or "21 – 27 Sep" within one month. */
export const range = (a: Date, b: Date) =>
  a.getMonth() === b.getMonth() ? `${a.getDate()} – ${dayMon(b)}` : `${dayMon(a)} – ${dayMon(b)}`;

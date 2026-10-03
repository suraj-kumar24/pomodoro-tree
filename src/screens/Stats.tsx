import { Icon } from '../components/Icon';
import { Dot } from '../components/Pot';
import { addDays, addMonths, startOfMonth, startOfWeek } from '../lib/dates';
import { MON3, MONTHS, WD3, WEEKDAYS, dayMon, fmt, fmtS, range, wd } from '../lib/format';
import { month, streaks, week, type TagShare } from '../lib/stats';
import type { Tag } from '../lib/types';
import { useStore } from '../store';

const DAY_LETTERS = ['M', 'T', 'W', 'T', 'F', 'S', 'S'];

function Nav({ title, sub, onPrev, onNext, nextDisabled, what }: { title: string; sub: string; onPrev: () => void; onNext: () => void; nextDisabled: boolean; what: string }) {
  return (
    <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between' }}>
      <button aria-label={`Previous ${what}`} onClick={onPrev} className="ibtn" style={{ width: 44, height: 44 }}>
        <Icon name="back" />
      </button>
      <div style={{ textAlign: 'center' }} aria-live="polite">
        <div style={{ fontWeight: 800, fontSize: 'var(--fs-15)' }}>{title}</div>
        <div className="muted" style={{ fontSize: 'var(--fs-13)' }}>
          {sub}
        </div>
      </div>
      <button aria-label={`Next ${what}`} onClick={onNext} disabled={nextDisabled} className="ibtn" style={{ width: 44, height: 44 }}>
        <Icon name="next" />
      </button>
    </div>
  );
}

function Delta({ diff, vs }: { diff: number; vs: string }) {
  const up = diff >= 0;
  return (
    <div style={{ display: 'flex', alignItems: 'center', gap: 8, marginTop: 10, color: up ? 'var(--c-ink-s)' : 'var(--c-muted)', fontWeight: 800, fontSize: 'var(--fs-15)' }}>
      <Icon name={up ? 'trendUp' : 'trendDown'} />
      <span>{diff === 0 ? `Same as ${vs}` : `${fmt(Math.abs(diff))} ${up ? 'more' : 'less'} than ${vs}`}</span>
    </div>
  );
}

function TagBars({ tags, total, tagOf }: { tags: TagShare[]; total: number; tagOf: (id: string) => Tag }) {
  return (
    <div style={{ padding: 18, borderRadius: 30, background: 'var(--c-card)', display: 'flex', flexDirection: 'column', gap: 14 }}>
      <div style={{ fontWeight: 800, fontSize: 'var(--fs-15)' }}>By tag</div>
      {!tags.length && (
        <div className="muted" style={{ fontSize: 'var(--fs-13)' }}>
          No finished sessions yet.
        </div>
      )}
      {tags.map(({ tagId, min }) => {
        const t = tagOf(tagId);
        return (
          <div key={tagId || 'untagged'} style={{ display: 'flex', flexDirection: 'column', gap: 6 }}>
            <div style={{ display: 'flex', justifyContent: 'space-between', gap: 12, alignItems: 'baseline' }}>
              <div style={{ display: 'flex', gap: 8, alignItems: 'baseline', minWidth: 0 }}>
                <Dot color={t.color} />
                <span style={{ fontWeight: 600, overflowWrap: 'anywhere' }}>{t.name}</span>
              </div>
              <div style={{ whiteSpace: 'nowrap', fontWeight: 800 }}>
                {fmt(min)}{' '}
                <span className="muted" style={{ fontWeight: 600 }}>
                  {Math.round((min / total) * 100)}%
                </span>
              </div>
            </div>
            <div style={{ height: 10, borderRadius: 999, background: 'var(--c-track)' }}>
              <div style={{ height: '100%', width: ((min / total) * 100).toFixed(1) + '%', borderRadius: 999, background: t.color }} />
            </div>
          </div>
        );
      })}
    </div>
  );
}

function Week() {
  const s = useStore();
  const { data, ui } = s;
  const now = Date.now();
  const thisWeek = startOfWeek(now);
  const ws = addDays(thisWeek, ui.weekOff * 7);
  const W = week(data.sessions, ws);
  const cur = ui.weekOff === 0;
  const todayIdx = cur ? wd(new Date(now)) : 7;
  const st = streaks(data.sessions, now);
  const goalMin = data.settings.goal * data.settings.focus;
  const maxV = Math.max(160, goalMin, ...W.days);
  const H = 118;
  const empty = W.total === 0 && W.abandoned === 0;
  const firstEver = !data.sessions.length;
  const title = cur ? 'This week' : ui.weekOff === -1 ? 'Last week' : `Week of ${dayMon(W.start)}`;

  return (
    <div style={{ display: 'flex', flexDirection: 'column', gap: 16 }}>
      <Nav
        what="week"
        title={title}
        sub={range(W.start, W.end)}
        onPrev={() => s.patch({ weekOff: ui.weekOff - 1 })}
        onNext={() => s.patch({ weekOff: Math.min(0, ui.weekOff + 1) })}
        nextDisabled={cur}
      />
      {empty ? (
        <div style={{ display: 'flex', flexDirection: 'column', gap: 14, padding: '22px 20px', borderRadius: 30, background: 'var(--c-card)' }}>
          <div style={{ display: 'grid', gridTemplateColumns: 'repeat(7,1fr)', alignItems: 'end', height: 90, gap: 6 }} aria-hidden="true">
            {DAY_LETTERS.map((l, i) => (
              <div key={i} style={{ display: 'flex', flexDirection: 'column', alignItems: 'center', gap: 8 }}>
                <div style={{ width: 24, height: 10, borderRadius: 999, border: '2px dashed var(--c-bar)' }} />
                <div className="muted" style={{ fontSize: 'var(--fs-11)', fontWeight: 700 }}>
                  {l}
                </div>
              </div>
            ))}
          </div>
          <div className="heading" style={{ fontSize: 'var(--fs-24)', lineHeight: 1.15 }}>
            {firstEver ? 'Your first week starts today.' : cur ? 'No focus yet this week.' : 'No focus this week.'}
          </div>
          <div className="muted" style={{ fontSize: 'var(--fs-15)' }}>
            {firstEver
              ? 'Finish a session and this page fills in: time per day, tags, streaks.'
              : cur
                ? W.prevTotal
                  ? `Last week you focused for ${fmt(W.prevTotal)}. Your next finished session shows up here.`
                  : 'Your next finished session shows up here.'
                : 'Nothing was finished or started in these seven days.'}
          </div>
          {cur && (
            <button
              onClick={() => s.go('home')}
              className="btn btn-primary btn-start"
              style={{ alignSelf: 'flex-start', width: 'auto', minHeight: 52, padding: '0 26px', fontSize: 'var(--fs-17)', whiteSpace: 'nowrap' }}
            >
              Start a session
            </button>
          )}
        </div>
      ) : (
        <div style={{ display: 'flex', flexDirection: 'column', gap: 16 }}>
          <div style={{ padding: '0 4px' }}>
            <div className="heading" style={{ fontSize: 52, lineHeight: 1 }}>
              {fmt(W.total)}
            </div>
            <div className="muted" style={{ fontSize: 'var(--fs-15)', marginTop: 4 }}>
              focused {cur ? 'this week' : 'that week'}
            </div>
            <Delta diff={W.total - W.prevTotal} vs="last week" />
            <div className="muted" style={{ fontSize: 'var(--fs-13)', marginTop: 2 }}>
              {cur ? 'Last week' : 'Week before'}: {fmt(W.prevTotal)}
            </div>
          </div>
          <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: 10 }}>
            {[
              ['circleCheck', W.completed, 'completed', 'var(--c-ink-s)'],
              ['circleX', W.abandoned, 'given up', 'var(--c-muted)'],
            ].map(([icon, n, label, color]) => (
              <div key={label as string} style={{ padding: 16, borderRadius: 26, background: 'var(--c-card)', display: 'flex', flexDirection: 'column', gap: 4 }}>
                <Icon name={icon as 'circleCheck'} size={22} style={{ color: color as string }} />
                <div className="heading" style={{ fontSize: 'var(--fs-32)', lineHeight: 1.1 }}>
                  {n as number}
                </div>
                <div className="muted" style={{ fontSize: 'var(--fs-13)', fontWeight: 600 }}>
                  {label as string}
                </div>
              </div>
            ))}
          </div>
          <div style={{ padding: '18px 16px 14px', borderRadius: 30, background: 'var(--c-card)', display: 'flex', flexDirection: 'column', gap: 6 }}>
            <div style={{ fontWeight: 800, fontSize: 'var(--fs-15)', paddingLeft: 4 }}>By day</div>
            <div
              style={{ position: 'relative', height: 190, display: 'grid', gridTemplateColumns: 'repeat(7,minmax(0,1fr))', gap: 4 }}
              role="img"
              aria-label={W.days.map((m, i) => `${WEEKDAYS[i]} ${fmt(m)}`).join(', ') + `. Daily goal ${fmt(goalMin)}.`}
            >
              <div style={{ position: 'absolute', left: 0, right: 0, bottom: (22 + (goalMin / maxV) * H).toFixed(0) + 'px', borderTop: '2px dashed var(--c-bar)', zIndex: 0 }}>
                <span className="muted" style={{ position: 'absolute', right: 0, top: -18, fontSize: 'var(--fs-11)', fontWeight: 700, background: 'var(--c-card)', paddingLeft: 4 }}>
                  Goal {fmtS(goalMin)}
                </span>
              </div>
              {W.days.map((m, i) => {
                const fut = i > todayIdx;
                const td = i === todayIdx;
                return (
                  <div key={i} style={{ position: 'relative', display: 'flex', flexDirection: 'column', alignItems: 'center', justifyContent: 'flex-end', gap: 6, height: '100%' }}>
                    <div style={{ fontSize: 'var(--fs-11)', fontWeight: 800, whiteSpace: 'nowrap', color: td ? 'var(--c-ink-a)' : 'var(--color-text)', position: 'relative', zIndex: 1 }}>
                      {fut ? '' : m ? fmtS(m) : '0'}
                    </div>
                    <div
                      style={
                        fut
                          ? { width: 10, height: 10, borderRadius: 999, border: '2px dashed var(--c-bar)' }
                          : { width: 24, height: Math.max(8, (m / maxV) * H).toFixed(0) + 'px', borderRadius: 999, background: td ? 'var(--color-accent)' : 'var(--c-bar)', position: 'relative', zIndex: 1 }
                      }
                    />
                    <div style={{ fontSize: 'var(--fs-11)', fontWeight: td ? 800 : 600, color: td ? 'var(--color-text)' : 'var(--c-muted)', whiteSpace: 'nowrap' }}>
                      {td ? 'Today' : WD3[i]}
                    </div>
                  </div>
                );
              })}
            </div>
          </div>
          <div style={{ padding: 18, borderRadius: 30, background: 'var(--c-card)', display: 'flex', flexDirection: 'column', gap: 14 }}>
            <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: 12 }}>
              <div>
                <div className="muted" style={{ fontSize: 'var(--fs-13)', fontWeight: 600 }}>
                  Current streak
                </div>
                <div className="heading" style={{ fontSize: 'var(--fs-24)' }}>
                  {st.current} day{st.current === 1 ? '' : 's'}
                </div>
              </div>
              <div>
                <div className="muted" style={{ fontSize: 'var(--fs-13)', fontWeight: 600 }}>
                  Longest
                </div>
                <div className="heading" style={{ fontSize: 'var(--fs-24)' }}>
                  {st.longest} day{st.longest === 1 ? '' : 's'}
                </div>
              </div>
            </div>
            <div style={{ display: 'grid', gridTemplateColumns: 'repeat(7,1fr)', gap: 4 }}>
              {DAY_LETTERS.map((l, i) => {
                const on = i <= todayIdx && W.dayDone[i];
                const fut = i > todayIdx;
                return (
                  <div key={i} style={{ display: 'flex', flexDirection: 'column', alignItems: 'center', gap: 4 }}>
                    <div
                      aria-label={`${WEEKDAYS[i]}: ${on ? 'pot finished' : fut ? 'to come' : 'no pot'}`}
                      role="img"
                      style={{ width: 34, height: 34, borderRadius: 999, display: 'grid', placeItems: 'center', background: on ? 'var(--color-accent)' : 'transparent', color: 'var(--c-on-a)', border: on ? 'none' : fut ? '2px dashed var(--c-bar)' : '2px solid var(--c-bar)' }}
                    >
                      {on && <Icon name="check" size={16} stroke={3.25} />}
                    </div>
                    <div className="muted" style={{ fontSize: 'var(--fs-11)', fontWeight: 700 }}>
                      {l}
                    </div>
                  </div>
                );
              })}
            </div>
          </div>
          <TagBars tags={W.tags} total={W.total || 1} tagOf={s.tagOf} />
        </div>
      )}
    </div>
  );
}

function Month({ dark }: { dark: boolean }) {
  const s = useStore();
  const { data, ui } = s;
  const now = Date.now();
  const ms = addMonths(startOfMonth(now), ui.monthOff);
  const M = month(data.sessions, ms, now);
  const cur = ui.monthOff === 0;
  const name = MONTHS[ms.getMonth()];
  const prevName = MONTHS[addMonths(ms, -1).getMonth()];
  const heat = dark
    ? [['var(--color-neutral-800)', '#f3ead9'], ['var(--color-accent-900)', '#f3ead9'], ['var(--color-accent-800)', '#f3ead9'], ['var(--color-accent-500)', '#1c1a17'], ['var(--color-accent-300)', '#1c1a17']]
    : [['var(--color-neutral-200)', '#201e1d'], ['var(--color-accent-200)', '#201e1d'], ['var(--color-accent-300)', '#201e1d'], ['var(--color-accent-500)', '#201e1d'], ['var(--color-accent-700)', '#fff7ec']];
  const bk = (m: number) => (m === 0 ? 0 : m <= 60 ? 1 : m <= 120 ? 2 : m <= 180 ? 3 : 4);
  // A month in progress is compared with the same days of the previous one.
  const base = cur ? M.prevSameDays : M.prevTotal;
  const diff = M.total - base;
  const vs = cur ? `the same days of ${prevName}` : prevName;
  const lead = wd(ms);
  const todayN = cur ? new Date(now).getDate() : 99;
  const best = M.best >= 0 ? new Date(ms.getFullYear(), ms.getMonth(), M.best + 1) : null;

  return (
    <div style={{ display: 'flex', flexDirection: 'column', gap: 16 }}>
      <Nav
        what="month"
        title={`${name} ${ms.getFullYear()}`}
        sub={cur ? 'This month so far' : ui.monthOff === -1 ? 'Last full month' : `${M.days.length} days`}
        onPrev={() => s.patch({ monthOff: ui.monthOff - 1 })}
        onNext={() => s.patch({ monthOff: Math.min(0, ui.monthOff + 1) })}
        nextDisabled={cur}
      />
      <div style={{ padding: '0 4px' }}>
        <div className="heading" style={{ fontSize: 52, lineHeight: 1 }}>
          {fmt(M.total)}
        </div>
        <div className="muted" style={{ fontSize: 'var(--fs-15)', marginTop: 4 }}>
          focused in {name}
        </div>
        <div style={{ display: 'flex', alignItems: 'center', gap: 8, marginTop: 10, color: diff >= 0 ? 'var(--c-ink-s)' : 'var(--c-muted)', fontWeight: 800, fontSize: 'var(--fs-15)' }}>
          <Icon name={diff >= 0 ? 'trendUp' : 'trendDown'} />
          <span>
            {diff === 0
              ? `Same as ${vs}`
              : `${fmt(Math.abs(diff))} ${diff > 0 ? 'more' : 'less'} than ${vs}${base ? ` (${diff > 0 ? '+' : '−'}${Math.round((Math.abs(diff) / base) * 100)}%)` : ''}`}
          </span>
        </div>
        <div className="muted" style={{ fontSize: 'var(--fs-13)', marginTop: 2 }}>
          {cur ? `${MON3[addMonths(ms, -1).getMonth()]} 1–${M.elapsed}: ${fmt(base)}` : `${prevName}: ${fmt(M.prevTotal)}`}
        </div>
      </div>
      <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: 10 }}>
        <div style={{ padding: 16, borderRadius: 26, background: 'var(--c-card)' }}>
          <div className="muted" style={{ fontSize: 'var(--fs-13)', fontWeight: 600 }}>
            Daily average
          </div>
          <div className="heading" style={{ fontSize: 'var(--fs-24)', marginTop: 2 }}>
            {fmt(M.total / M.elapsed)}
          </div>
          <div className="muted" style={{ fontSize: 'var(--fs-13)' }}>
            over {M.elapsed} day{M.elapsed === 1 ? '' : 's'}
          </div>
        </div>
        <div style={{ padding: 16, borderRadius: 26, background: 'var(--c-card)' }}>
          <div className="muted" style={{ fontSize: 'var(--fs-13)', fontWeight: 600 }}>
            Best day
          </div>
          <div className="heading" style={{ fontSize: 'var(--fs-24)', marginTop: 2 }}>
            {best ? fmt(M.days[M.best]) : '–'}
          </div>
          <div className="muted" style={{ fontSize: 'var(--fs-13)' }}>
            {best ? `${WD3[wd(best)]} ${best.getDate()} ${name}` : 'Nothing yet'}
          </div>
        </div>
      </div>
      <div style={{ padding: '18px 14px', borderRadius: 30, background: 'var(--c-card)', display: 'flex', flexDirection: 'column', gap: 10 }}>
        <div style={{ fontWeight: 800, fontSize: 'var(--fs-15)', paddingLeft: 4 }}>Focus by day</div>
        <div className="muted" style={{ display: 'grid', gridTemplateColumns: 'repeat(7,minmax(0,1fr))', gap: 5, fontSize: 'var(--fs-11)', fontWeight: 700, textAlign: 'center' }} aria-hidden="true">
          {DAY_LETTERS.map((l, i) => (
            <span key={i}>{l}</span>
          ))}
        </div>
        <div style={{ display: 'grid', gridTemplateColumns: 'repeat(7,minmax(0,1fr))', gap: 5 }}>
          {[...Array(lead)].map((_, i) => (
            <div key={`b${i}`} />
          ))}
          {M.days.map((m, i) => {
            const fut = i + 1 > todayN;
            const h = heat[bk(m)];
            return (
              <div
                key={i}
                aria-label={`${i + 1} ${name}: ${fut ? 'to come' : m ? fmt(m) : 'no focus'}${i === M.best ? ', best day' : ''}`}
                role="img"
                style={{
                  height: 50,
                  borderRadius: 14,
                  background: fut ? 'transparent' : h[0],
                  color: fut ? 'var(--c-muted)' : h[1],
                  border: fut ? '1.5px dashed var(--color-divider)' : 'none',
                  display: 'flex',
                  flexDirection: 'column',
                  alignItems: 'center',
                  justifyContent: 'center',
                  gap: 4,
                  outline: i === M.best ? '2.5px solid var(--color-text)' : 'none',
                  outlineOffset: 1.5,
                }}
              >
                <span style={{ fontSize: 'var(--fs-13)', fontWeight: 800, lineHeight: 1 }}>{i + 1}</span>
                <span style={{ fontSize: 'var(--fs-11)', fontWeight: 600, lineHeight: 1 }}>{fut ? '' : m ? fmtS(m) : '–'}</span>
              </div>
            );
          })}
        </div>
        <div className="muted" style={{ display: 'flex', alignItems: 'center', gap: 6, flexWrap: 'wrap', fontSize: 'var(--fs-11)', fontWeight: 700, padding: '4px 4px 0' }}>
          <span>Less</span>
          {['0', '<1h', '1–2h', '2–3h', '3h+'].map((t, i) => (
            <div key={t} style={{ display: 'flex', alignItems: 'center', gap: 3 }}>
              <span style={{ width: 14, height: 14, borderRadius: 5, background: heat[i][0], display: 'inline-block', border: '1px solid var(--color-divider)' }} />
              <span>{t}</span>
            </div>
          ))}
          <span>More</span>
        </div>
        {best && (
          <div className="muted" style={{ fontSize: 'var(--fs-11)', fontWeight: 600, paddingLeft: 4 }}>
            Ringed: best day
          </div>
        )}
      </div>
      <TagBars tags={M.tags} total={M.total || 1} tagOf={s.tagOf} />
    </div>
  );
}

export function Stats({ dark }: { dark: boolean }) {
  const s = useStore();
  const tab = s.ui.statsTab;
  return (
    <div style={{ display: 'flex', flexDirection: 'column', gap: 16, padding: '4px 20px 28px' }}>
      <h1 style={{ margin: 0, fontSize: 'var(--fs-32)', paddingLeft: 4 }}>Stats</h1>
      <div className="seg" role="tablist">
        <button role="tab" aria-selected={tab === 'week'} onClick={() => s.patch({ statsTab: 'week' })}>
          Week
        </button>
        <button role="tab" aria-selected={tab === 'month'} onClick={() => s.patch({ statsTab: 'month' })}>
          Month
        </button>
      </div>
      {tab === 'week' ? <Week /> : <Month dark={dark} />}
    </div>
  );
}

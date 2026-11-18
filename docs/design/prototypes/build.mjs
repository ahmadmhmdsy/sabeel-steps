
import { writeFileSync } from 'node:fs';

const ICONS = {
  home: '<svg width="24" height="24" viewBox="0 0 24 24"><path d="M10 20v-6h4v6h5v-8h3L12 3 2 12h3v8z" fill="currentColor"/></svg>',
  history: '<svg width="24" height="24" viewBox="0 0 24 24"><path d="M19 3h-1V1h-2v2H8V1H6v2H5a2 2 0 0 0-2 2v14a2 2 0 0 0 2 2h14a2 2 0 0 0 2-2V5a2 2 0 0 0-2-2zm0 16H5V8h14v11zM7 10h5v2H7zm0 4h5v2H7z" fill="currentColor"/></svg>',
  insights: '<svg width="24" height="24" viewBox="0 0 24 24"><path d="M12 17.27 18.18 21l-1.64-7.03L22 9.24l-7.19-.61L12 2 9.19 8.63 2 9.24l5.46 4.73L5.82 21z" fill="currentColor"/></svg>',
  settings: '<svg width="24" height="24" viewBox="0 0 24 24"><path d="M19.14 12.94c.04-.3.06-.61.06-.94 0-.32-.02-.64-.07-.94l2.03-1.58c.18-.14.23-.41.12-.61l-1.92-3.32c-.12-.22-.37-.29-.59-.22l-2.39.96c-.5-.38-1.03-.7-1.62-.94l-.36-2.54c-.04-.24-.24-.41-.48-.41h-3.84c-.24 0-.43.17-.47.41l-.36 2.54c-.59.24-1.13.57-1.62.94l-2.39-.96c-.22-.08-.47 0-.59.22L2.74 8.87c-.12.21-.08.47.12.61l2.03 1.58c-.05.3-.09.63-.09.94s.02.64.07.94l-2.03 1.58c-.18.14-.23.41-.12.61l1.92 3.32c.12.22.37.29.59.22l2.39-.96c.5.38 1.03.7 1.62.94l.36 2.54c.05.24.24.41.48.41h3.84c.24 0 .44-.17.47-.41l.36-2.54c.59-.24 1.13-.56 1.62-.94l2.39.96c.22.08.47 0 .59-.22l1.92-3.32c.12-.22.07-.47-.12-.61l-2.01-1.58zM12 15.6c-1.98 0-3.6-1.62-3.6-3.6s1.62-3.6 3.6-3.6 3.6 1.62 3.6 3.6-1.62 3.6-3.6 3.6z" fill="currentColor"/></svg>',
};
const CICONS = {
  home: '<svg width="24" height="24" viewBox="0 0 24 24" fill="none"><path d="M4 17.5v-5a8 8 0 0 1 16 0v5" stroke="currentColor" stroke-width="1.8" stroke-linecap="round"/><path d="M2.5 20h19" stroke="currentColor" stroke-width="1.8" stroke-linecap="round"/><circle cx="12" cy="18" r="1.2" fill="currentColor"/></svg>',
  history: '<svg width="24" height="24" viewBox="0 0 24 24" fill="none"><path d="M5 6.5h14M5 12h9.5M5 17.5h6" stroke="currentColor" stroke-width="1.9" stroke-linecap="round"/><circle cx="18.5" cy="12" r="1.4" fill="currentColor"/><circle cx="14.5" cy="17.5" r="1.4" fill="currentColor"/></svg>',
  insights: '<svg width="24" height="24" viewBox="0 0 24 24" fill="none"><circle cx="12" cy="12" r="3.6" stroke="currentColor" stroke-width="1.8"/><path d="M12 3.2v2.2M12 18.6v2.2M3.2 12h2.2M18.6 12h2.2M5.8 5.8l1.6 1.6M16.6 16.6l1.6 1.6M18.2 5.8l-1.6 1.6M7.4 16.6l-1.6 1.6" stroke="currentColor" stroke-width="1.8" stroke-linecap="round"/></svg>',
  settings: '<svg width="24" height="24" viewBox="0 0 24 24" fill="none"><path d="M3.5 8.5h17M3.5 15.5h17" stroke="currentColor" stroke-width="1.8" stroke-linecap="round"/><circle cx="9" cy="8.5" r="2.4" fill="#FAF7F2" stroke="currentColor" stroke-width="1.8"/><circle cx="15" cy="15.5" r="2.4" fill="#FAF7F2" stroke="currentColor" stroke-width="1.8"/></svg>',
};
const STAR = '<svg width="14" height="14" viewBox="0 0 24 24"><path d="M12 17.27 18.18 21l-1.64-7.03L22 9.24l-7.19-.61L12 2 9.19 8.63 2 9.24l5.46 4.73L5.82 21z" fill="#2A1E00"/></svg>';
const CHECK = '<svg width="15" height="15" viewBox="0 0 24 24"><path d="M9 16.2 4.8 12l-1.4 1.4L9 19 21 7l-1.4-1.4z" fill="currentColor"/></svg>';
const PLAY = '<svg width="18" height="18" viewBox="0 0 24 24"><path d="M8 5v14l11-7z" fill="currentColor"/></svg>';
const WALK = '<svg width="20" height="20" viewBox="0 0 24 24" fill="none"><circle cx="13.2" cy="4.6" r="2.1" stroke="currentColor" stroke-width="1.7"/><path d="M11.2 8.6 8.4 14l3.1 1.9 1.9 4.6M11.2 8.6l3.6 1.6 1.9 3.4M8.7 13.6l-2 5.4" stroke="currentColor" stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round"/></svg>';
const SUN = '<svg width="22" height="22" viewBox="0 0 24 24" fill="none"><circle cx="12" cy="12" r="3.4" stroke="currentColor" stroke-width="1.7"/><path d="M12 3.6v2M12 18.4v2M3.6 12h2M18.4 12h2M6.1 6.1l1.4 1.4M16.5 16.5l1.4 1.4M17.9 6.1l-1.4 1.4M7.5 16.5l-1.4 1.4" stroke="currentColor" stroke-width="1.7" stroke-linecap="round"/></svg>';
const CHEV = '<svg class="chev" width="18" height="18" viewBox="0 0 24 24"><path d="M9 6l6 6-6 6" stroke="currentColor" stroke-width="2" fill="none" stroke-linecap="round" stroke-linejoin="round"/></svg>';
const TRASH = '<svg width="20" height="20" viewBox="0 0 24 24" fill="none"><path d="M5 7h14M10 7V5h4v2M7 7l1 13h8l1-13" stroke="currentColor" stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round"/><path d="M10.5 11v5M13.5 11v5" stroke="currentColor" stroke-width="1.8" stroke-linecap="round"/></svg>';
const WARNING = '<svg width="22" height="22" viewBox="0 0 24 24"><path d="M12 2 1 21h22L12 2zm0 4.2 8.1 13.8H3.9L12 6.2zM11 10h2v6h-2zm0 8h2v2h-2z" fill="#8A6A00"/></svg>';

const STATUS = '<div class="statusbar"><span>11:35</span><span class="icons">' +
  '<svg width="14" height="11" viewBox="0 0 14 11"><rect x="0" y="7" width="3" height="4" rx=".6" fill="#444"/><rect x="4.4" y="4.6" width="3" height="6.4" rx=".6" fill="#444"/><rect x="8.8" y="2" width="3" height="9" rx=".6" fill="#444"/></svg>' +
  '<svg width="16" height="12" viewBox="0 0 16 12" fill="none"><path d="M1.2 4.3a10 10 0 0 1 13.6 0" stroke="#444" stroke-width="1.9" stroke-linecap="round"/><path d="M4 7.2a6 6 0 0 1 8 0" stroke="#444" stroke-width="1.9" stroke-linecap="round"/><circle cx="8" cy="10" r="1.4" fill="#444"/></svg>' +
  '<svg width="17" height="10" viewBox="0 0 17 10"><rect x=".6" y=".6" width="13.6" height="8.8" rx="2" stroke="#444" stroke-width="1.1" fill="none"/><rect x="2.4" y="2.4" width="9" height="5.2" rx="1.2" fill="#444"/><rect x="15" y="3" width="1.6" height="4" rx=".8" fill="#444"/></svg>' +
  '</span></div>';
const SYSNAV = '<div class="sysnav">' +
  '<svg width="14" height="14" viewBox="0 0 14 14"><path d="M2 2 L12 7 L2 12 Z" fill="none" stroke="#1A1C1B" stroke-width="1.4" stroke-linejoin="round"/></svg>' +
  '<svg width="13" height="13" viewBox="0 0 13 13"><circle cx="6.5" cy="6.5" r="5.2" fill="none" stroke="#1A1C1B" stroke-width="1.4"/></svg>' +
  '<svg width="13" height="13" viewBox="0 0 13 13"><rect x="1" y="1" width="11" height="11" rx="2.4" fill="none" stroke="#1A1C1B" stroke-width="1.4"/></svg>' +
  '</div>';

function navbar(sel, icons) {
  const items = [['home','Home','home'],['history','History','history'],['insights','Insights','insights'],['settings','Settings','settings']];
  return '<nav class="navbar">' + items.map(([k,label,icon]) =>
    '<div class="navitem ' + (sel === k ? 'sel' : '') + '"><div class="indicator">' + icons[icon] + '</div><span class="navlabel">' + label + '</span></div>'
  ).join('') + '</nav>';
}
function shell(title, bodyClass, sel, content, icons) {
  return '<!doctype html>\n<html><head><meta charset="utf-8"><title>' + title + '</title><link rel="stylesheet" href="proto.css"></head>\n' +
    '<body class="' + bodyClass + '">\n<div class="phone">\n' + STATUS + '\n<div class="app">\n' + content + '\n</div>\n' +
    navbar(sel, icons) + '\n' + SYSNAV + '\n</div>\n</body></html>';
}

const ring = (done) => '<div class="ring done"><div class="ring-inner"><div class="ring-value">8,400</div><div class="ring-sub">of 8,000 steps</div></div>' +
  '<div class="badge">' + CHECK + '</div></div>';

const homeBody = (opts) => {
  opts = opts || {};
  const icons = opts.icons || ICONS;
  const cta = opts.walkIcon ? '<div class="btn-primary">' + WALK + 'Start a walk</div>' : '<div class="btn-primary">' + PLAY + 'Start a walk</div>';
  return '<div class="topbar"><h1>' + (opts.title || 'Today') + '</h1></div>' +
    '<div class="content">' +
      '<div class="goalrow"><span class="chip">' + STAR + (opts.streak || '5 days in a row') + '</span><span class="goal-label">Goal 8,000</span></div>' +
      '<div class="ringwrap">' + ring(true) + '</div>' +
      '<div class="active-caption">84 active minutes</div>' +
      '<div class="status">Goal reached. Enjoy the rest of your day.</div>' +
      '<div class="updated">Updated 9:15 PM</div>' +
      cta +
      '<div class="stats">' +
        '<div class="stat"><div class="value">6.3 km</div><div class="label">Distance (estimate)</div></div>' +
        '<div class="stat"><div class="value">336 kcal</div><div class="label">Energy (estimate)</div></div>' +
      '</div>' +
      '<div class="section' + (opts.overline ? ' overline' : '') + '">' + (opts.section || 'Last 7 days') + '</div>' +
      '<div class="chart"><div class="bars">' + [78,82,74,80,88,84,90].map((h,i) => '<div class="bar ' + (i===6?'full':'') + '" style="height:' + h + '%"></div>').join('') + '</div><div class="goal"></div></div>' +
    '</div>';
};

const p1home = shell('Sabeel prototype P1 — Home', '', 'home', homeBody({}), ICONS);

const stat = (v,l) => '<div class="stat"><div class="value">' + v + '</div><div class="label">' + l + '</div></div>';
const insights = shell('Sabeel prototype P1 — Insights', '', 'insights',
  '<div class="topbar"><h1>Insights</h1></div><div class="content">' +
    '<div class="goalrow" style="margin-top:12px"><span class="chip">' + STAR + '5 days in a row</span></div>' +
    '<div class="stats" style="margin-top:16px">' +
      stat('4,966','30-day average') + stat('8,400','Best day') +
      stat('17%','Goal hit rate') + stat('111.8 km','All-time distance') +
    '</div>' +
    '<div class="pcard"><div class="ptitle">Last 30 days</div>' +
      '<div class="pbody">You averaged 4,966 steps a day and reached your goal on 5 of 30 days.</div>' +
      '<div class="pline">Your best day was Today with 8,400 steps.</div></div>' +
    '<div class="note" style="margin-top:16px">Distance and energy are estimates, not medical measurements.</div>' +
  '</div>', ICONS);

const sw = '<div class="switch"><div class="knob"></div></div>';
const row = (title, sub, trailing) => '<div class="row"><div class="grow"><div class="rtitle">' + title + '</div>' + (sub ? '<div class="rsub">' + sub + '</div>' : '') + '</div>' + (trailing || '') + '</div>';
const rowlink = (title, sub, value) => '<div class="row"><div class="grow"><div class="rtitle">' + title + '</div>' + (sub ? '<div class="rsub">' + sub + '</div>' : '') + '</div>' + (value ? '<span class="rvalue">' + value + '</span>' : '') + CHEV + '</div>';
const stepper = '<div class="stepper"><div class="sbtn">&minus;</div><div class="sval">8,000<span class="sunit">steps</span></div><div class="sbtn">+</div></div>';

const settingsTop = shell('Sabeel prototype P2 — Settings top', '', 'settings',
  '<div class="topbar"><h1>Settings</h1></div><div class="content">' +
    '<div class="section">Daily goal</div>' +
    '<div class="rowset">' + row('Steps per day','', stepper) + rowlink('Suggest from my routine','Uses your last 20 days. Changes the goal at most 10%.') + '</div>' +
    '<div class="section">Tracking</div>' +
    '<div class="rowset">' + row('Rest day today','Pauses your streak for today without breaking it.', sw) + row('Daily reminder','A quiet nudge around a time you choose.', sw) + rowlink('Why is my count low?','Explains what to check when the count looks low.') + '</div>' +
    '<div class="section">Units &amp; language</div>' +
    '<div class="rowset">' + rowlink('Units','','Metric (km, kg)') + rowlink('Language','','English') + '</div>' +
  '</div>', ICONS);

const settingsBottom = shell('Sabeel prototype P2 — Settings bottom', '', 'settings',
  '<div class="topbar"><h1>Settings</h1></div><div class="content">' +
    '<div class="section">Your data</div>' +
    '<div class="note" style="margin:-4px 0 10px">Save a copy of your daily totals. Nothing is uploaded.</div>' +
    '<div class="rowset">' + rowlink('Export a copy','Daily totals as a CSV file.') + rowlink('Import from a file','Restore daily totals from a Sabeel CSV.') + '</div>' +
    '<div class="section">App</div>' +
    '<div class="rowset">' + rowlink('Privacy promise','Steps stay on this phone.') + rowlink('About Sabeel','Version 1.0.0') + '</div>' +
    '<div class="danger"><div class="drow"><span style="color:var(--error)">' + TRASH + '</span><div class="grow"><div class="dtitle">Delete all data</div><div class="dsub">Removes 20 days of totals, saved walks and all settings. This cannot be undone.</div></div>' + CHEV + '</div></div>' +
    '<div class="note" style="margin-top:16px">Distance and energy are estimates, not medical measurements.</div>' +
  '</div>', ICONS);

const p3a = shell('Sabeel prototype Q2-b — Authored accents', 'identity', 'home',
  homeBody({ walkIcon: true, icons: CICONS }), CICONS);

const p3bContent = '<div class="topbar head"><div><div class="dateline">Tuesday &middot; 17 November</div><div class="wordmark">Sabeel</div></div><div class="sunmark">' + SUN + '</div></div>' +
  '<div class="content">' +
    '<div class="goalrow" style="margin-top:8px"><span class="chip">' + STAR + '5 days in a row</span><span class="goal-label">Goal 8,000</span></div>' +
    '<div class="ringwrap full"><div class="ringbg"></div><div class="ringinner"><div class="ring-value">8,400</div><div class="ring-sub">of 8,000 steps</div></div><div class="badge">' + CHECK + '</div></div>' +
    '<div class="status">Goal reached. Enjoy the rest of your day.</div>' +
    '<div class="updated">Updated 9:15 PM</div>' +
    '<div class="btn-primary">' + WALK + 'Start a walk</div>' +
    '<div class="tripanel"><div class="trip"><div class="tv">6.3 km</div><div class="tl">Distance</div></div><div class="trip"><div class="tv">336 kcal</div><div class="tl">Energy</div></div><div class="trip"><div class="tv">84 min</div><div class="tl">Active</div></div></div>' +
    '<div class="section overline">Last 7 days</div>' +
    '<div class="chart"><div class="bars">' + [78,82,74,80,88,84,90].map((h,i) => '<div class="bar ' + (i===6?'full':'') + '" style="height:' + h + '%"></div>').join('') + '</div><div class="goal"></div></div>' +
  '</div>';
const p3b = shell('Sabeel prototype Q2-c — Full Desert Dawn', 'identity-full', 'home', p3bContent, CICONS);

writeFileSync(new URL('./p1-home.html', import.meta.url), p1home);
writeFileSync(new URL('./p1-insights.html', import.meta.url), insights);
writeFileSync(new URL('./p2-settings-top.html', import.meta.url), settingsTop);
writeFileSync(new URL('./p2-settings-bottom.html', import.meta.url), settingsBottom);
writeFileSync(new URL('./p3a-home.html', import.meta.url), p3a);
writeFileSync(new URL('./p3b-home.html', import.meta.url), p3b);
console.log('written');

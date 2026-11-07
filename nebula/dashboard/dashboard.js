// Nébula — dashboard (full-page stats).

const PROTECTION_KEYS = [
  'canvas', 'webgl', 'audio', 'fonts', 'clientRects', 'navigator', 'hardware', 'screen',
  'timezone', 'locale', 'webrtc', 'battery', 'mediaDevices', 'plugins', 'speech', 'timing',
  'uaData', 'connection', 'webdriver', 'toStringMask', 'headers', 'trackers', 'sensors',
  'webgpu', 'dnt', 'urlClean', 'referer'
];
const THEMES = {
  purple: ['#7b7bff', '#39d8c8'], teal: ['#1fd6c4', '#4db8ff'], blue: ['#4d7cff', '#39d8c8'],
  pink: ['#ff5cc8', '#9b6bff'], orange: ['#ff9f43', '#ff6b6b'], green: ['#2ee676', '#39d8c8']
};
function applyTheme(t) {
  const c = THEMES[t] || THEMES.purple;
  document.documentElement.style.setProperty('--accent', c[0]);
  document.documentElement.style.setProperty('--accent2', c[1]);
}

const $ = (id) => document.getElementById(id);
const send = (type, extra) => new Promise((r) => chrome.runtime.sendMessage({ type, ...extra }, r));

function dayKey(d) { return d.toISOString().slice(0, 10); }
function last7() {
  const out = [];
  for (let i = 6; i >= 0; i--) {
    const d = new Date(); d.setDate(d.getDate() - i);
    out.push(dayKey(d));
  }
  return out;
}
const DOW = ['dom', 'lun', 'mar', 'mié', 'jue', 'vie', 'sáb'];

function esc(s) { return String(s).replace(/[&<>"]/g, (c) => ({ '&': '&amp;', '<': '&lt;', '>': '&gt;', '"': '&quot;' }[c])); }

function buildChart(days, values) {
  const W = 860, H = 240, padL = 34, padB = 28, padT = 14, padR = 10;
  const cw = W - padL - padR, ch = H - padB - padT;
  const max = Math.max(1, ...values);
  const n = values.length;
  const slot = cw / n;
  const bw = Math.min(64, slot * 0.6);
  let bars = '';
  values.forEach((v, i) => {
    const h = Math.round((v / max) * ch);
    const x = padL + i * slot + (slot - bw) / 2;
    const y = padT + ch - h;
    bars += `<rect class="bar" x="${x.toFixed(1)}" y="${y.toFixed(1)}" width="${bw.toFixed(1)}" height="${h}" rx="6"><title>${esc(days[i])}: ${v}</title></rect>`;
    bars += `<text class="barlabel" x="${(x + bw / 2).toFixed(1)}" y="${H - 9}" text-anchor="middle">${DOW[new Date(days[i] + 'T00:00:00').getDay()]}</text>`;
    if (v > 0) bars += `<text class="barval" x="${(x + bw / 2).toFixed(1)}" y="${(y - 5).toFixed(1)}" text-anchor="middle">${v}</text>`;
  });
  const axis = `<line class="axis" x1="${padL}" y1="${padT + ch}" x2="${W - padR}" y2="${padT + ch}"/>`;
  return `<svg viewBox="0 0 ${W} ${H}" role="img" aria-label="Rastreadores bloqueados últimos 7 días">${axis}${bars}</svg>`;
}

function buildTop(top) {
  const entries = Object.entries(top || {}).sort((a, b) => b[1] - a[1]).slice(0, 8);
  if (!entries.length) return '<div class="empty">Aún no se han bloqueado rastreadores. Navega un poco y vuelve.</div>';
  const max = Math.max(...entries.map((e) => e[1]));
  return entries.map(([dom, n]) => `
    <div class="trow"><span class="tname">${esc(dom)}</span><span class="tcount">${n}</span>
    <div class="tbar"><div style="width:${Math.round((n / max) * 100)}%"></div></div></div>`).join('');
}

function shortUA(ua) {
  let br = 'Navegador', ver = '';
  if (/Edg\//.test(ua)) { br = 'Edge'; ver = (ua.match(/Edg\/(\d+)/) || [])[1] || ''; }
  else if (/Firefox\//.test(ua)) { br = 'Firefox'; ver = (ua.match(/Firefox\/(\d+)/) || [])[1] || ''; }
  else if (/Chrome\//.test(ua)) { br = 'Chrome'; ver = (ua.match(/Chrome\/(\d+)/) || [])[1] || ''; }
  else if (/Version\/[\d.]+.*Safari/.test(ua)) { br = 'Safari'; ver = (ua.match(/Version\/(\d+)/) || [])[1] || ''; }
  const os = /Windows/.test(ua) ? 'Windows' : /iPhone|iPad/.test(ua) ? 'iOS' : /Mac OS/.test(ua) ? 'macOS' : /Android/.test(ua) ? 'Android' : /Linux/.test(ua) ? 'Linux' : '';
  return br + (ver ? ' ' + ver : '') + (os ? ' · ' + os : '');
}

async function load() {
  const res = await send('getStatsFull');
  if (!res || !res.ok) { $('status').textContent = 'No se pudieron cargar las estadísticas'; return; }
  const { stats, settings, profile } = res;
  applyTheme(settings.theme);

  const days = last7();
  const values = days.map((d) => (stats.days && stats.days[d]) || 0);
  const today = values[values.length - 1];
  const week = values.reduce((a, b) => a + b, 0);

  $('kToday').textContent = today;
  $('kWeek').textContent = week;
  $('kTotal').textContent = stats.total || 0;
  $('chart').innerHTML = buildChart(days, values);
  $('topList').innerHTML = buildTop(stats.top);

  const on = PROTECTION_KEYS.filter((k) => settings.protections[k]).length;
  $('covNum').textContent = `${on}/${PROTECTION_KEYS.length}`;
  $('covFill').style.width = Math.round((on / PROTECTION_KEYS.length) * 100) + '%';

  $('idblock').innerHTML = `
    <div><b>${esc(shortUA(profile.userAgent))}</b></div>
    <div>Pantalla: <b>${profile.screen.width}×${profile.screen.height}</b> · DPR ${profile.screen.dpr}</div>
    <div>Zona horaria: <b>${esc(profile.timezone)}</b></div>
    <div>GPU: <b>${esc((profile.webgl && profile.webgl.renderer) || '—').replace(/^ANGLE \(/, '').replace(/\)$/, '')}</b></div>
    <div>Idioma: <b>${esc((profile.languages || []).join(', '))}</b></div>`;

  $('status').textContent = 'Actualizado ' + new Date().toLocaleTimeString();
}

$('refresh').addEventListener('click', load);
load();

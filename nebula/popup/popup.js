// Nébula — popup controller.

const PROTECTION_LABELS = {
  canvas: 'Canvas',
  webgl: 'WebGL',
  audio: 'Audio',
  fonts: 'Fuentes',
  clientRects: 'Client Rects',
  navigator: 'User-Agent',
  hardware: 'Hardware',
  screen: 'Pantalla',
  timezone: 'Zona horaria',
  locale: 'Idioma',
  webrtc: 'WebRTC',
  battery: 'Batería',
  mediaDevices: 'Dispositivos',
  plugins: 'Plugins',
  speech: 'Voces',
  timing: 'Precisión de reloj',
  uaData: 'Client Hints',
  connection: 'Red',
  webdriver: 'WebDriver',
  toStringMask: 'Anti-detección',
  headers: 'Cabeceras HTTP'
};

const $ = (id) => document.getElementById(id);
let STATE = null;
let currentHost = null;

function send(type, extra) {
  return new Promise((res) => chrome.runtime.sendMessage({ type, ...extra }, res));
}

async function getActiveHost() {
  try {
    const [tab] = await chrome.tabs.query({ active: true, currentWindow: true });
    if (tab && tab.url) return new URL(tab.url).hostname;
  } catch {}
  return null;
}

function etld(host) {
  if (!host) return host;
  const parts = host.split('.');
  return parts.length <= 2 ? host : parts.slice(-2).join('.');
}

function render() {
  const s = STATE.settings;
  const p = STATE.profile;

  $('master').checked = s.enabled;
  document.body.classList.toggle('disabled', !s.enabled);
  $('mode').value = s.mode;

  // presets
  const presetSel = $('preset');
  presetSel.innerHTML = '';
  STATE.presets.forEach((pr) => {
    const o = document.createElement('option');
    o.value = pr.id; o.textContent = pr.label;
    presetSel.appendChild(o);
  });
  presetSel.value = s.preset;
  $('presetRow').hidden = !(s.mode === 'blend' || s.mode === 'custom');

  // identity
  $('idUA').textContent = shortUA(p.userAgent);
  $('idPlatform').textContent = p.platform + (p.uaData ? ` · ${p.uaData.platform}` : '');
  $('idScreen').textContent = `${p.screen.width}×${p.screen.height} @${p.screen.dpr}x · ${p.screen.colorDepth}bit`;
  $('idTZ').textContent = p.timezone;
  $('idGPU').textContent = p.webgl ? p.webgl.renderer.replace(/^ANGLE \(/, '').replace(/\)$/, '') : '—';
  $('idHW').textContent = `${p.hardwareConcurrency} núcleos · ${p.deviceMemory} GB`;
  $('idLang').textContent = (p.languages || []).join(', ');

  // site allow
  $('host').textContent = currentHost || '—';
  const base = etld(currentHost);
  $('siteAllow').checked = !!(currentHost && s.allowlist.some((d) => currentHost === d || currentHost.endsWith('.' + d)));
  $('siteAllow').disabled = !currentHost;

  // toggles
  const grid = $('toggles');
  grid.innerHTML = '';
  Object.keys(PROTECTION_LABELS).forEach((key) => {
    const lbl = document.createElement('label');
    lbl.className = 'tog';
    const cb = document.createElement('input');
    cb.type = 'checkbox';
    cb.checked = !!s.protections[key];
    cb.addEventListener('change', async () => {
      await send('setSettings', { patch: { protections: { [key]: cb.checked } } });
      await load();
    });
    lbl.appendChild(cb);
    lbl.appendChild(document.createTextNode(PROTECTION_LABELS[key]));
    grid.appendChild(lbl);
  });
}

function shortUA(ua) {
  const m = ua.match(/Chrome\/(\d+)/);
  const os = /Windows/.test(ua) ? 'Windows' : /Mac OS/.test(ua) ? 'macOS' : /Android/.test(ua) ? 'Android' : /Linux/.test(ua) ? 'Linux' : '';
  return `Chrome ${m ? m[1] : '?'} · ${os}`;
}

async function load() {
  STATE = await send('getState');
  currentHost = await getActiveHost();
  render();
}

// ---- events ----
$('master').addEventListener('change', async (e) => {
  await send('setSettings', { patch: { enabled: e.target.checked } });
  await load();
});
$('mode').addEventListener('change', async (e) => {
  await send('setSettings', { patch: { mode: e.target.value } });
  await load();
});
$('preset').addEventListener('change', async (e) => {
  await send('setSettings', { patch: { preset: e.target.value } });
  await load();
});
$('regen').addEventListener('click', async () => {
  await send('regenerateSeed');
  await load();
});
$('siteAllow').addEventListener('change', async (e) => {
  if (!currentHost) return;
  const base = etld(currentHost);
  let list = STATE.settings.allowlist.slice();
  if (e.target.checked) {
    if (!list.includes(base)) list.push(base);
  } else {
    list = list.filter((d) => !(currentHost === d || currentHost.endsWith('.' + d)));
  }
  await send('setSettings', { patch: { allowlist: list } });
  await load();
});
$('openOptions').addEventListener('click', () => chrome.runtime.openOptionsPage());

load();

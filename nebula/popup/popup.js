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
  // Device picker is always available; choosing one switches to "blend".
  $('pool').value = s.randomPool || 'desktop';
  $('region').value = s.timezoneRegion || 'americas';
  $('timezoneSource').value = s.timezoneSource || 'ip';
  const geo = STATE.geo;
  $('geoStatus').textContent = (s.timezoneSource === 'ip')
    ? (geo && geo.timezone ? 'IP detectada: ' + geo.timezone : 'Detectando ubicación…')
    : '';
  const isRandom = s.mode === 'per-session' || s.mode === 'per-site';
  // Device picker only in "blend" (mimic) mode; pool+region only in random modes.
  $('presetRow').hidden = s.mode !== 'blend';
  $('presetHint').hidden = s.mode !== 'blend';
  $('poolRow').hidden = !isRandom;
  $('regionRow').hidden = !isRandom;

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
  // Only shown in blend mode; just set the device, keep the mode as-is.
  await send('setSettings', { patch: { preset: e.target.value } });
  await load();
});
$('pool').addEventListener('change', async (e) => {
  await send('setSettings', { patch: { randomPool: e.target.value } });
  await load();
});
$('region').addEventListener('change', async (e) => {
  await send('setSettings', { patch: { timezoneRegion: e.target.value } });
  await load();
});
$('timezoneSource').addEventListener('change', async (e) => {
  await send('setSettings', { patch: { timezoneSource: e.target.value } });
  if (e.target.value === 'ip') { $('geoStatus').textContent = 'Detectando ubicación…'; await send('refreshGeo'); }
  await load();
});
$('regen').addEventListener('click', async () => {
  await send('regenerateSeed');
  await load();
});
let npArmed = false;
let npTimer = null;
const NP_LABEL = '🆕 Crear perfil nuevo y limpio';

async function doNewProfile() {
  const btn = $('newProfile');
  btn.textContent = 'Creando…';
  btn.disabled = true;
  btn.classList.remove('warn');
  let res;
  try {
    res = await send('newIdentity', { timezoneSource: STATE.settings.timezoneSource });
  } catch (_) { res = null; }
  await load();
  btn.textContent = NP_LABEL;
  btn.disabled = false;
  const vr = $('verifyResult');
  vr.className = 'verify-result ok';
  vr.textContent = '✨ Perfil nuevo creado' + (res && res.cleared ? ' + datos limpiados.' : '.') + ' Recargando…';
  try {
    const [tab] = await chrome.tabs.query({ active: true, currentWindow: true });
    if (tab && /^https?:/.test(tab.url || '')) chrome.tabs.reload(tab.id);
  } catch {}
}

$('newProfile').addEventListener('click', async () => {
  const btn = $('newProfile');
  // Two-tap confirm (window.confirm is unreliable inside mobile popups).
  if (!npArmed) {
    npArmed = true;
    btn.classList.add('warn');
    btn.textContent = '⚠️ Toca otra vez: borrará cookies';
    npTimer = setTimeout(() => { npArmed = false; btn.classList.remove('warn'); btn.textContent = NP_LABEL; }, 4000);
    return;
  }
  clearTimeout(npTimer);
  npArmed = false;
  await doNewProfile();
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

$('verify').addEventListener('click', async () => {
  const el = $('verifyResult');
  el.className = 'verify-result';
  el.textContent = 'Comprobando…';
  try {
    const [tab] = await chrome.tabs.query({ active: true, currentWindow: true });
    if (!tab || !/^https?:/.test(tab.url || '')) {
      el.className = 'verify-result warn';
      el.textContent = 'Abre una página web normal (http/https) y vuelve a verificar.';
      return;
    }
    const expected = STATE.profile.userAgent;

    // Preferred: read navigator.userAgent as the PAGE itself sees it (MAIN world).
    let pageUA = null;
    try {
      const res = await chrome.scripting.executeScript({
        target: { tabId: tab.id },
        world: 'MAIN',
        func: () => navigator.userAgent
      });
      pageUA = res && res[0] ? res[0].result : null;
    } catch (_) {
      pageUA = null; // browser may not support MAIN-world executeScript
    }

    if (pageUA != null) {
      if (!STATE.settings.enabled) {
        el.className = 'verify-result warn';
        el.textContent = 'Nébula está desactivada. Actívala con el interruptor de arriba.';
      } else if (pageUA === expected) {
        el.className = 'verify-result ok';
        el.innerHTML = '✅ Protección ACTIVA en esta página.<br>La web te ve como: <b>' + shortUA(pageUA) + '</b>';
      } else {
        el.className = 'verify-result bad';
        el.innerHTML = '⚠️ NO se está aplicando aquí.<br>La web ve tu navegador real. Cierra y reabre esta pestaña tras instalar/activar. Si persiste, tu navegador puede no soportar la inyección en el mundo principal.';
      }
      return;
    }

    // Fallback: shared-DOM marker written by protect.js.
    const res2 = await chrome.scripting.executeScript({
      target: { tabId: tab.id },
      func: () => document.documentElement.getAttribute('data-nebula')
    });
    const mark = res2 && res2[0] ? res2[0].result : null;
    if (mark === 'active') {
      el.className = 'verify-result warn';
      el.textContent = 'Nébula cargó y aplicó el perfil, pero este navegador no permite confirmar al 100% que llegue a la página. En Cover Your Tracks puede seguir apareciendo "única".';
    } else if (mark) {
      el.className = 'verify-result warn';
      el.textContent = 'Nébula cargó pero aún no aplicó el perfil. Recarga la página e inténtalo de nuevo.';
    } else {
      el.className = 'verify-result bad';
      el.textContent = '⚠️ Nébula no se está inyectando en esta página. Recarga la pestaña; si sigue, tu navegador no es compatible con la inyección.';
    }
  } catch (e) {
    el.className = 'verify-result bad';
    el.textContent = 'No se pudo verificar: ' + e.message;
  }
});

load();

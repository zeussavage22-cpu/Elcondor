// Nébula — options controller.

const PROTECTION_LABELS = {
  canvas: 'Canvas', webgl: 'WebGL', audio: 'Audio', fonts: 'Fuentes',
  clientRects: 'Client Rects', navigator: 'User-Agent', hardware: 'Hardware',
  screen: 'Pantalla', timezone: 'Zona horaria', locale: 'Idioma', webrtc: 'WebRTC',
  battery: 'Batería', mediaDevices: 'Dispositivos', plugins: 'Plugins', speech: 'Voces',
  timing: 'Precisión reloj', uaData: 'Client Hints', connection: 'Red',
  webdriver: 'WebDriver', toStringMask: 'Anti-detección', headers: 'Cabeceras HTTP',
  trackers: 'Bloquear rastreadores', sensors: 'Sensores/USB/HID', webgpu: 'WebGPU', dnt: 'No rastrear (DNT/GPC)',
  urlClean: 'Limpiar URLs', referer: 'Ocultar Referer', thirdPartyCookies: 'Cookies de terceros'
};

const THEMES = {
  purple: ['#7b7bff', '#39d8c8'], teal: ['#1fd6c4', '#4db8ff'], blue: ['#4d7cff', '#39d8c8'],
  pink: ['#ff5cc8', '#9b6bff'], orange: ['#ff9f43', '#ff6b6b'], green: ['#2ee676', '#39d8c8']
};
function applyTheme(t) {
  const c = THEMES[t] || THEMES.purple;
  document.documentElement.style.setProperty('--accent', c[0]);
  document.documentElement.style.setProperty('--accent2', c[1]);
}

const COUNTRIES = { US: 'Estados Unidos', MX: 'México', CO: 'Colombia', AR: 'Argentina', BR: 'Brasil', ES: 'España', GB: 'Reino Unido', DE: 'Alemania', FR: 'Francia' };
const SHIELD_LEVELS = {
  basic: ['navigator', 'hardware', 'screen', 'timezone', 'locale', 'headers', 'uaData', 'webdriver', 'toStringMask', 'trackers', 'dnt', 'urlClean'],
  balanced: ['canvas', 'webgl', 'audio', 'fonts', 'clientRects', 'navigator', 'hardware', 'screen', 'timezone', 'locale', 'webrtc', 'plugins', 'timing', 'uaData', 'connection', 'webdriver', 'toStringMask', 'headers', 'trackers', 'dnt', 'urlClean', 'referer', 'thirdPartyCookies'],
  max: null
};
function protectionsForLevel(level) {
  const out = {};
  const list = SHIELD_LEVELS[level];
  Object.keys(PROTECTION_LABELS).forEach((k) => { out[k] = list === null ? true : list.includes(k); });
  return out;
}

const $ = (id) => document.getElementById(id);
let STATE = null;

const send = (type, extra) => new Promise((r) => chrome.runtime.sendMessage({ type, ...extra }, r));

function flash(msg) {
  $('status').textContent = msg;
  setTimeout(() => ($('status').textContent = ''), 1800);
}

function render() {
  const s = STATE.settings;
  $('master').checked = s.enabled;
  $('mode').value = s.mode;
  $('noise').value = s.noise;
  $('noiseStrategy').value = s.noiseStrategy || 'per-read';
  $('timezoneSource').value = s.timezoneSource || 'ip';
  $('webrtcMode').value = s.webrtcMode;

  const geo = STATE.geo;
  const gs = $('geoStatus');
  if (s.timezoneSource === 'ip') {
    gs.textContent = geo && geo.timezone
      ? `Detectado: ${geo.timezone}${geo.country ? ' (' + geo.country + ')' : ''}${geo.fallback ? ' — vía navegador' : ''}.`
      : 'Detectando…';
  } else {
    gs.textContent = '';
  }
  $('timingPrecisionMs').value = String(s.timingPrecisionMs);

  const presetSel = $('preset');
  presetSel.innerHTML = '';
  STATE.presets.forEach((p) => {
    const o = document.createElement('option');
    o.value = p.id; o.textContent = p.label;
    presetSel.appendChild(o);
  });
  presetSel.value = s.preset;
  $('pool').value = s.randomPool || 'desktop';
  $('region').value = s.timezoneRegion || 'americas';
  // Device only in blend (mimic) mode; pool/region only in random modes.
  const rnd = (s.mode === 'per-session' || s.mode === 'per-site') ? '' : 'none';
  $('poolField').style.display = rnd;
  $('regionField').style.display = rnd;
  $('presetField').style.display = (s.mode === 'blend') ? '' : 'none';

  const grid = $('toggles');
  grid.innerHTML = '';
  Object.keys(PROTECTION_LABELS).forEach((key) => {
    const lbl = document.createElement('label');
    lbl.className = 'tog';
    const cb = document.createElement('input');
    cb.type = 'checkbox';
    cb.checked = !!s.protections[key];
    cb.dataset.key = key;
    cb.addEventListener('change', () => save({ protections: { [key]: cb.checked } }));
    lbl.appendChild(cb);
    lbl.appendChild(document.createTextNode(PROTECTION_LABELS[key]));
    grid.appendChild(lbl);
  });

  const ai = s.ai || {};
  $('ai_enabled').checked = !!ai.enabled;
  $('ai_provider').value = ai.provider || 'gemini';
  $('ai_apiKey').value = ai.apiKey || '';
  $('ai_model').value = ai.model || '';
  updateAIHelp();

  $('theme').value = s.theme || 'purple';
  applyTheme(s.theme);
  renderProfiles(s);

  // v2.1 extras
  $('shieldLevel').value = s.shieldLevel || 'balanced';
  $('autoRotateMin').value = String(s.autoRotateMin || 0);
  $('cookieBanners').checked = s.cookieBanners !== false;
  $('fpLog').checked = s.fpLog !== false;
  const tc = $('travelCountry');
  if (tc.children.length <= 1) {
    Object.entries(COUNTRIES).forEach(([code, name]) => {
      const o = document.createElement('option'); o.value = code; o.textContent = name; tc.appendChild(o);
    });
  }
  tc.value = s.travelCountry || '';

  const cd = s.cleanData || { cookies: true, storage: true, cache: true };
  $('clean_cookies').checked = !!cd.cookies;
  $('clean_storage').checked = !!cd.storage;
  $('clean_cache').checked = !!cd.cache;

  $('allowlist').value = (s.allowlist || []).join('\n');
  if (s.customProfile) $('customProfile').value = JSON.stringify(s.customProfile, null, 2);
}

async function save(patch) {
  const res = await send('setSettings', { patch });
  STATE.settings = res.settings;
  flash('Guardado ✓');
}

async function load() {
  STATE = await send('getState');
  render();
}

// ---- events ----
$('master').addEventListener('change', (e) => save({ enabled: e.target.checked }));
$('mode').addEventListener('change', (e) => { save({ mode: e.target.value }).then(load); });
$('preset').addEventListener('change', (e) => { save({ preset: e.target.value }).then(load); });
$('pool').addEventListener('change', (e) => { save({ randomPool: e.target.value }).then(load); });
$('region').addEventListener('change', (e) => { save({ timezoneRegion: e.target.value }).then(load); });
$('timezoneSource').addEventListener('change', (e) => { save({ timezoneSource: e.target.value }).then(load); });
const MODE_NAME = { 'per-session': 'Aleatorio/sesión', 'per-site': 'Aleatorio/sitio', blend: 'Mimetizar', custom: 'Personalizado' };
function renderProfiles(s) {
  const list = $('profList');
  list.innerHTML = '';
  const profs = s.savedProfiles || [];
  if (!profs.length) { list.innerHTML = '<p class="hint">Aún no hay perfiles guardados.</p>'; return; }
  profs.forEach((pr) => {
    const item = document.createElement('div');
    item.className = 'prof-item';
    const dev = pr.config.mode === 'blend' ? (pr.config.preset || '') : (MODE_NAME[pr.config.mode] || pr.config.mode);
    item.innerHTML = `<div><div class="pname">${escapeHtml(pr.name)}</div><div class="pmeta">${escapeHtml(dev)}</div></div>
      <div class="pacts"><button class="apply">Aplicar</button><button class="del">✕</button></div>`;
    item.querySelector('.apply').addEventListener('click', async () => { await save(pr.config); flash('Perfil «' + pr.name + '» aplicado ✓'); load(); });
    item.querySelector('.del').addEventListener('click', async () => {
      const next = (STATE.settings.savedProfiles || []).filter((x) => x.id !== pr.id);
      await save({ savedProfiles: next }); load();
    });
    list.appendChild(item);
  });
}
function escapeHtml(s) { return String(s).replace(/[&<>"]/g, (c) => ({ '&': '&amp;', '<': '&lt;', '>': '&gt;', '"': '&quot;' }[c])); }

function saveClean() {
  save({ cleanData: { cookies: $('clean_cookies').checked, storage: $('clean_storage').checked, cache: $('clean_cache').checked } });
}
$('clean_cookies').addEventListener('change', saveClean);
$('clean_storage').addEventListener('change', saveClean);
$('clean_cache').addEventListener('change', saveClean);

$('theme').addEventListener('change', (e) => { applyTheme(e.target.value); save({ theme: e.target.value }); });
$('shieldLevel').addEventListener('change', (e) => { save({ shieldLevel: e.target.value, protections: protectionsForLevel(e.target.value) }).then(load); });
$('autoRotateMin').addEventListener('change', (e) => save({ autoRotateMin: Number(e.target.value) }));
$('travelCountry').addEventListener('change', (e) => { save({ travelCountry: e.target.value }).then(load); });
$('cookieBanners').addEventListener('change', (e) => save({ cookieBanners: e.target.checked }));
$('fpLog').addEventListener('change', (e) => save({ fpLog: e.target.checked }));
$('openDash2').addEventListener('click', () => chrome.tabs.create({ url: chrome.runtime.getURL('dashboard/dashboard.html') }));
$('profSave').addEventListener('click', async () => {
  const name = $('profName').value.trim();
  if (!name) { flash('Escribe un nombre'); return; }
  const s = STATE.settings;
  const config = {
    mode: s.mode, preset: s.preset, randomPool: s.randomPool, timezoneRegion: s.timezoneRegion,
    timezoneSource: s.timezoneSource, customProfile: s.customProfile, protections: { ...s.protections }
  };
  const prof = { id: 'p' + Date.now().toString(36), name, config };
  await save({ savedProfiles: [...(s.savedProfiles || []), prof] });
  $('profName').value = '';
  await load();
  flash('Perfil guardado ✓');
});

const AI_HELP = {
  pollinations: { key: 'No necesita API key. Es un servicio público y gratuito (tercero); se le envía solo el dominio y tu User-Agent. Puede saturarse (1 consulta cada ~30s).', model: 'Por defecto <code>openai</code>. Déjalo vacío.' },
  gemini: { key: 'Key GRATIS en <b>aistudio.google.com/app/apikey</b> (sin tarjeta).', model: 'Por defecto <code>gemini-3.8-flash</code> (gratis). Si falla, prueba <code>gemini-flash-latest</code>.' },
  groq: { key: 'Key GRATIS en <b>console.groq.com/keys</b>.', model: 'Por defecto <code>llama-3.3-70b-versatile</code> (gratis).' },
  openrouter: { key: 'Key en <b>openrouter.ai/keys</b>. Para <b>DeepSeek GRATIS</b> pon abajo el modelo <code>deepseek/deepseek-chat-v3-0324:free</code>.', model: 'Por defecto <code>meta-llama/llama-3.3-70b-instruct:free</code>. DeepSeek gratis: <code>deepseek/deepseek-chat-v3-0324:free</code>.' },
  deepseek: { key: 'Key en <b>platform.deepseek.com/api_keys</b> (de pago, muy barato; a veces dan crédito gratis al registrarte). ¿Gratis total? Usa OpenRouter con un modelo <code>deepseek/...:free</code>.', model: 'Por defecto <code>deepseek-chat</code> (V3). Razonador: <code>deepseek-reasoner</code> (R1).' },
  anthropic: { key: 'Key de pago en <b>console.anthropic.com/settings/keys</b> (requiere saldo).', model: 'Por defecto <code>claude-opus-5-5</code>. Más barato: <code>claude-haiku-5-5</code>.' },
  openai: { key: 'Key de pago en <b>platform.openai.com/api-keys</b> (requiere saldo).', model: 'Por defecto <code>gpt-4o-mini</code>.' }
};
function updateAIHelp() {
  const p = $('ai_provider').value;
  const h = AI_HELP[p] || AI_HELP.gemini;
  $('ai_keyhelp').innerHTML = h.key;
  $('ai_modelhelp').innerHTML = h.model;
  // Pollinations needs no key — hide the key field.
  $('ai_keyfield').style.display = (p === 'pollinations') ? 'none' : '';
}

function saveAI() {
  save({ ai: {
    enabled: $('ai_enabled').checked,
    provider: $('ai_provider').value,
    apiKey: $('ai_apiKey').value.trim(),
    model: $('ai_model').value.trim()
  } });
}
$('ai_enabled').addEventListener('change', saveAI);
$('ai_provider').addEventListener('change', () => { updateAIHelp(); saveAI(); });
let aiKeyT;
$('ai_apiKey').addEventListener('input', () => { clearTimeout(aiKeyT); aiKeyT = setTimeout(saveAI, 500); });
let aiModelT;
$('ai_model').addEventListener('input', () => { clearTimeout(aiModelT); aiModelT = setTimeout(saveAI, 500); });
$('refreshGeo').addEventListener('click', async () => {
  $('geoStatus').textContent = 'Detectando…';
  await send('refreshGeo');
  await load();
  flash('Ubicación actualizada ✓');
});
$('noise').addEventListener('change', (e) => save({ noise: e.target.value }));
$('noiseStrategy').addEventListener('change', (e) => save({ noiseStrategy: e.target.value }));
$('webrtcMode').addEventListener('change', (e) => save({ webrtcMode: e.target.value }));
$('timingPrecisionMs').addEventListener('change', (e) => save({ timingPrecisionMs: parseFloat(e.target.value) }));

let allowT;
$('allowlist').addEventListener('input', (e) => {
  clearTimeout(allowT);
  allowT = setTimeout(() => {
    const list = e.target.value.split('\n').map((x) => x.trim()).filter(Boolean);
    save({ allowlist: list });
  }, 500);
});

$('loadPreset').addEventListener('click', async () => {
  // fetch current resolved profile and dump it as the custom base
  const st = await send('getState');
  $('customProfile').value = JSON.stringify(st.profile, null, 2);
  flash('Preset cargado — revisa y guarda');
});

let custT;
$('customProfile').addEventListener('input', (e) => {
  clearTimeout(custT);
  custT = setTimeout(() => {
    $('jsonErr').textContent = '';
    const txt = e.target.value.trim();
    if (!txt) return;
    try {
      const obj = JSON.parse(txt);
      save({ customProfile: obj });
    } catch (err) {
      $('jsonErr').textContent = 'JSON inválido: ' + err.message;
    }
  }, 600);
});

$('export').addEventListener('click', () => {
  const blob = new Blob([JSON.stringify(STATE.settings, null, 2)], { type: 'application/json' });
  const url = URL.createObjectURL(blob);
  const a = document.createElement('a');
  a.href = url; a.download = 'nebula-ajustes.json';
  a.click();
  URL.revokeObjectURL(url);
});
$('import').addEventListener('click', () => $('importFile').click());
$('importFile').addEventListener('change', (e) => {
  const file = e.target.files[0];
  if (!file) return;
  const reader = new FileReader();
  reader.onload = async () => {
    try {
      const obj = JSON.parse(reader.result);
      await send('setSettings', { patch: obj });
      await load();
      flash('Importado ✓');
    } catch (err) {
      flash('Archivo inválido');
    }
  };
  reader.readAsText(file);
});
$('reset').addEventListener('click', async () => {
  if (!confirm('¿Restablecer todos los ajustes de Nébula?')) return;
  await send('resetSettings');
  await load();
  flash('Restablecido ✓');
});

load();

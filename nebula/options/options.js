// Nébula — options controller.

const PROTECTION_LABELS = {
  canvas: 'Canvas', webgl: 'WebGL', audio: 'Audio', fonts: 'Fuentes',
  clientRects: 'Client Rects', navigator: 'User-Agent', hardware: 'Hardware',
  screen: 'Pantalla', timezone: 'Zona horaria', locale: 'Idioma', webrtc: 'WebRTC',
  battery: 'Batería', mediaDevices: 'Dispositivos', plugins: 'Plugins', speech: 'Voces',
  timing: 'Precisión reloj', uaData: 'Client Hints', connection: 'Red',
  webdriver: 'WebDriver', toStringMask: 'Anti-detección', headers: 'Cabeceras HTTP'
};

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
  $('webrtcMode').value = s.webrtcMode;
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
  // Device picker always visible; pool only matters for the random modes.
  $('poolField').style.display = (s.mode === 'per-session' || s.mode === 'per-site') ? '' : 'none';

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
$('preset').addEventListener('change', (e) => { save({ mode: 'blend', preset: e.target.value }).then(load); });
$('pool').addEventListener('change', (e) => { save({ randomPool: e.target.value }).then(load); });
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

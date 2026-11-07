// Nébula — background service worker (MV3, ES module).
// Responsibilities:
//  * own the settings + the per-session seed
//  * resolve the one coherent "active profile"
//  * publish a runtime config that content scripts read
//  * keep HTTP headers (UA / Accept-Language / Sec-CH-UA) in sync via DNR

import { DEFAULT_SETTINGS, PRESETS, randomProfile, RANDOM_POOLS } from './lib/profiles.js';
import { mulberry32, seedFrom } from './lib/rng.js';

const SETTINGS_KEY = 'nebula:settings';
const CONFIG_KEY = 'nebula:config'; // resolved runtime config read by content scripts
const SEED_KEY = 'nebula:sessionSeed';
const GEO_KEY = 'nebula:geo';
const GEO_TTL_MS = 60 * 60 * 1000; // re-check the IP's timezone at most hourly
const DNR_RULE_BASE = 1000;
const TRACKER_RULE_ID = 2000;

// Curated list of common analytics / advertising / session-replay trackers.
const TRACKER_DOMAINS = [
  'google-analytics.com', 'analytics.google.com', 'googletagmanager.com', 'googleadservices.com',
  'doubleclick.net', 'googlesyndication.com', 'adservice.google.com', 'pagead2.googlesyndication.com',
  'connect.facebook.net', 'graph.facebook.com', 'scorecardresearch.com',
  'quantserve.com', 'quantcount.com', 'adnxs.com', 'criteo.com', 'criteo.net', 'rubiconproject.com',
  'pubmatic.com', 'openx.net', 'taboola.com', 'outbrain.com', 'hotjar.com', 'static.hotjar.com',
  'mixpanel.com', 'api.mixpanel.com', 'segment.com', 'cdn.segment.com', 'amplitude.com',
  'api.amplitude.com', 'fullstory.com', 'mouseflow.com', 'clarity.ms', 'bat.bing.com',
  'ads.linkedin.com', 'px.ads.linkedin.com', 'analytics.twitter.com', 'ads-twitter.com',
  'snap.licdn.com', 'cdn.branch.io', 'app.link', 'bugsnag.com', 'nr-data.net', 'newrelic.com',
  'sentry.io', 'matomo.cloud', 'plausible.io', 'chartbeat.com', 'crazyegg.com', 'optimizely.com',
  'mc.yandex.ru', 'adroll.com', 'bluekai.com', 'demdex.net', 'omtrdc.net',
  'everesttech.net', 'moatads.com', 'adsrvr.org', 'casalemedia.com', 'sharethrough.com'
];

// ---- settings --------------------------------------------------------------

async function getSettings() {
  const got = await chrome.storage.local.get(SETTINGS_KEY);
  const stored = got[SETTINGS_KEY] || {};
  const merged = { ...DEFAULT_SETTINGS, ...stored };
  // Deep-merge nested objects so new keys (e.g. added protections) default on.
  merged.protections = { ...DEFAULT_SETTINGS.protections, ...(stored.protections || {}) };
  merged.cleanData = { ...DEFAULT_SETTINGS.cleanData, ...(stored.cleanData || {}) };
  merged.ai = { ...DEFAULT_SETTINGS.ai, ...(stored.ai || {}) };
  return merged;
}

async function setSettings(patch) {
  const cur = await getSettings();
  const next = { ...cur, ...patch };
  if (patch.protections) next.protections = { ...cur.protections, ...patch.protections };
  if (patch.cleanData) next.cleanData = { ...cur.cleanData, ...patch.cleanData };
  if (patch.ai) next.ai = { ...cur.ai, ...patch.ai };
  await chrome.storage.local.set({ [SETTINGS_KEY]: next });
  await rebuild();
  return next;
}

// ---- session seed ----------------------------------------------------------

async function getSeed() {
  let got;
  try {
    got = await chrome.storage.session.get(SEED_KEY);
  } catch {
    got = await chrome.storage.local.get(SEED_KEY);
  }
  let seed = got[SEED_KEY];
  if (typeof seed !== 'number') {
    seed = (Math.random() * 0xffffffff) >>> 0;
    await saveSeed(seed);
  }
  return seed;
}

async function saveSeed(seed) {
  try {
    await chrome.storage.session.set({ [SEED_KEY]: seed });
  } catch {
    await chrome.storage.local.set({ [SEED_KEY]: seed });
  }
}

async function regenerateSeed() {
  const seed = (Math.random() * 0xffffffff) >>> 0;
  await saveSeed(seed);
  await rebuild();
  return seed;
}

// Wipe browsing state so a fresh identity is not linkable to the old one via
// cookies / site storage / cache. Leaves history, passwords and downloads alone.
async function clearBrowsingState(opts) {
  opts = opts || { cookies: true, storage: true, cache: true };
  const types = {};
  if (opts.cookies) types.cookies = true;
  if (opts.storage) {
    types.localStorage = true;
    types.indexedDB = true;
    types.cacheStorage = true;
    types.serviceWorkers = true;
    types.webSQL = true;
    types.fileSystems = true;
  }
  if (opts.cache) types.cache = true;
  if (!Object.keys(types).length) return false;
  try {
    await chrome.browsingData.remove({ since: 0 }, types);
    return true;
  } catch (e) {
    return false;
  }
}

// ---- profile resolution ----------------------------------------------------

function resolveProfile(settings, seed) {
  switch (settings.mode) {
    case 'blend':
      return structuredClone(PRESETS[settings.preset] || PRESETS['win-chrome']);
    case 'custom':
      return settings.customProfile
        ? structuredClone(settings.customProfile)
        : structuredClone(PRESETS[settings.preset] || PRESETS['win-chrome']);
    case 'per-site':
    case 'per-session':
    default: {
      const pool = RANDOM_POOLS[settings.randomPool] || RANDOM_POOLS.desktop;
      return randomProfile(mulberry32(seed), pool, settings.timezoneRegion);
    }
  }
}

// ---- declarativeNetRequest header sync -------------------------------------

function buildSecChUa(brands) {
  // "Chromium";v="131", "Not_A Brand";v="24", "Google Chrome";v="131"
  return brands.map((b) => `"${b.brand}";v="${b.version}"`).join(', ');
}

function buildFullVersionList(brands, fullVersion) {
  return brands
    .map((b) => {
      const v = b.brand === 'Not_A Brand' ? `${b.version}.0.0.0` : fullVersion;
      return `"${b.brand}";v="${v}"`;
    })
    .join(', ');
}

async function updateHeaderRules(settings, profile) {
  // Remove all of our rules first (header rules + tracker block rule).
  const existing = await chrome.declarativeNetRequest.getDynamicRules();
  const removeRuleIds = existing
    .filter((r) => (r.id >= DNR_RULE_BASE && r.id < DNR_RULE_BASE + 100) || r.id === TRACKER_RULE_ID)
    .map((r) => r.id);

  const addRules = [];
  const excluded = normalizeDomains(settings.allowlist);

  // --- tracker blocking -----------------------------------------------------
  if (settings.enabled && settings.protections.trackers) {
    const cond = { requestDomains: TRACKER_DOMAINS, resourceTypes: ['main_frame', 'sub_frame', 'xmlhttprequest', 'script', 'image', 'ping', 'media', 'websocket', 'other'] };
    if (excluded.length) cond.excludedInitiatorDomains = excluded; // don't block on allowlisted sites
    addRules.push({ id: TRACKER_RULE_ID, priority: 2, action: { type: 'block' }, condition: cond });
  }

  // --- header rewriting (UA + DNT/GPC) --------------------------------------
  if (settings.enabled && (settings.protections.headers || settings.protections.dnt)) {
    const ua = profile.uaData || {};
    const headers = [];
    if (settings.protections.headers) {
      headers.push({ header: 'user-agent', operation: 'set', value: profile.userAgent });
      headers.push({ header: 'accept-language', operation: 'set', value: `${profile.language},${profile.language.split('-')[0]};q=0.9` });
    }
    if (settings.protections.dnt) {
      headers.push({ header: 'dnt', operation: 'set', value: '1' });
      headers.push({ header: 'sec-gpc', operation: 'set', value: '1' });
    }
    if (settings.protections.headers) {
      if (ua.brands && ua.brands.length) {
        headers.push({ header: 'sec-ch-ua', operation: 'set', value: buildSecChUa(ua.brands) });
        headers.push({ header: 'sec-ch-ua-mobile', operation: 'set', value: ua.mobile ? '?1' : '?0' });
        headers.push({ header: 'sec-ch-ua-platform', operation: 'set', value: `"${ua.platform}"` });
        headers.push({ header: 'sec-ch-ua-platform-version', operation: 'set', value: `"${ua.platformVersion || ''}"` });
        headers.push({ header: 'sec-ch-ua-arch', operation: 'set', value: `"${ua.architecture || ''}"` });
        headers.push({ header: 'sec-ch-ua-bitness', operation: 'set', value: `"${ua.bitness || ''}"` });
        headers.push({ header: 'sec-ch-ua-model', operation: 'set', value: `"${ua.model || ''}"` });
        headers.push({ header: 'sec-ch-ua-full-version', operation: 'set', value: `"${ua.uaFullVersion || ''}"` });
        headers.push({ header: 'sec-ch-ua-full-version-list', operation: 'set', value: buildFullVersionList(ua.brands, ua.uaFullVersion || '') });
      } else {
        // Safari / non-Chromium profiles never send Client Hints — strip them.
        for (const h of ['sec-ch-ua', 'sec-ch-ua-mobile', 'sec-ch-ua-platform', 'sec-ch-ua-platform-version',
          'sec-ch-ua-arch', 'sec-ch-ua-bitness', 'sec-ch-ua-model', 'sec-ch-ua-full-version', 'sec-ch-ua-full-version-list']) {
          headers.push({ header: h, operation: 'remove' });
        }
      }
    }

    const condition = {
      urlFilter: '*',
      resourceTypes: ['main_frame', 'sub_frame', 'xmlhttprequest', 'script', 'image', 'stylesheet', 'font', 'media', 'websocket', 'other']
    };
    if (excluded.length) condition.excludedRequestDomains = excluded;

    if (headers.length) {
      addRules.push({
        id: DNR_RULE_BASE,
        priority: 1,
        action: { type: 'modifyHeaders', requestHeaders: headers },
        condition
      });
    }
  }

  await chrome.declarativeNetRequest.updateDynamicRules({ removeRuleIds, addRules });
}

function normalizeDomains(list) {
  const out = [];
  for (const raw of list || []) {
    let d = String(raw).trim().toLowerCase();
    if (!d) continue;
    d = d.replace(/^https?:\/\//, '').replace(/\/.*$/, '').replace(/^\*\./, '');
    if (d) out.push(d);
  }
  return out;
}

// ---- IP-based timezone -----------------------------------------------------
// When enabled, the exposed timezone is made to match the geolocation of the
// user's current IP (their real location, or their VPN/proxy exit) so that
// sites which cross-check "JS timezone vs IP geolocation" see no mismatch.
// This is the only feature that makes a network request, and only when on.

async function getGeo(force) {
  if (!force) {
    const cached = (await chrome.storage.local.get(GEO_KEY))[GEO_KEY];
    if (cached && cached.timezone && Date.now() - cached.ts < GEO_TTL_MS) return cached;
  }
  const endpoints = [
    { url: 'https://ipapi.co/json/', map: (d) => ({ timezone: d.timezone, country: d.country_code || d.country }) },
    { url: 'https://ipwho.is/', map: (d) => (d && d.success === false ? null : { timezone: d.timezone && d.timezone.id, country: d.country_code }) },
    { url: 'https://worldtimeapi.org/api/ip', map: (d) => ({ timezone: d.timezone, country: null }) }
  ];
  for (const e of endpoints) {
    try {
      const res = await fetch(e.url, { cache: 'no-store' });
      if (!res.ok) continue;
      const d = await res.json();
      const m = e.map(d);
      if (m && m.timezone) {
        const geo = { timezone: m.timezone, country: m.country || null, ts: Date.now(), source: e.url };
        await chrome.storage.local.set({ [GEO_KEY]: geo });
        return geo;
      }
    } catch {}
  }
  // Fallback: the browser's own configured timezone usually matches the home IP.
  try {
    const tz = Intl.DateTimeFormat().resolvedOptions().timeZone;
    const geo = { timezone: tz, country: null, ts: Date.now(), source: 'browser', fallback: true };
    await chrome.storage.local.set({ [GEO_KEY]: geo });
    return geo;
  } catch {}
  return null;
}

// ---- publish runtime config for content scripts ----------------------------

async function rebuild() {
  const settings = await getSettings();
  const seed = await getSeed();
  const profile = resolveProfile(settings, seed);

  // Align the timezone with the IP geolocation when that option is on.
  if (settings.timezoneSource === 'ip' && settings.protections.timezone) {
    const geo = await getGeo(false);
    if (geo && geo.timezone) profile.timezone = geo.timezone;
  }

  const config = {
    enabled: settings.enabled,
    mode: settings.mode,
    noise: settings.noise,
    noiseStrategy: settings.noiseStrategy,
    protections: settings.protections,
    webrtcMode: settings.webrtcMode,
    timingPrecisionMs: settings.timingPrecisionMs,
    allowlist: normalizeDomains(settings.allowlist),
    baseSeed: seed,
    profile
  };
  await chrome.storage.local.set({ [CONFIG_KEY]: config });
  await updateHeaderRules(settings, profile);
  updateBadge(settings.enabled);
  return config;
}

function updateBadge(enabled) {
  try {
    chrome.action.setBadgeBackgroundColor({ color: enabled ? '#6d5efc' : '#888888' });
    chrome.action.setBadgeText({ text: enabled ? '' : 'off' });
  } catch {}
}

// ---- lifecycle --------------------------------------------------------------

chrome.runtime.onInstalled.addListener(async () => {
  const got = await chrome.storage.local.get(SETTINGS_KEY);
  if (!got[SETTINGS_KEY]) await chrome.storage.local.set({ [SETTINGS_KEY]: DEFAULT_SETTINGS });
  await getSeed();
  await rebuild();
});

chrome.runtime.onStartup.addListener(async () => {
  // New browser session → fresh seed for per-session mode.
  await regenerateSeed();
  // Refresh IP geolocation (the exit IP may have changed since last run).
  const settings = await getSettings();
  if (settings.timezoneSource === 'ip') { await getGeo(true); await rebuild(); }
});

// Make sure config exists whenever the worker wakes.
rebuild().catch(() => {});

// ---- messaging --------------------------------------------------------------

chrome.runtime.onMessage.addListener((msg, _sender, sendResponse) => {
  (async () => {
    switch (msg && msg.type) {
      case 'getState': {
        const settings = await getSettings();
        const seed = await getSeed();
        const profile = resolveProfile(settings, seed);
        if (settings.timezoneSource === 'ip' && settings.protections.timezone) {
          const geo = await getGeo(false);
          if (geo && geo.timezone) profile.timezone = geo.timezone;
        }
        const geo = (await chrome.storage.local.get(GEO_KEY))[GEO_KEY] || null;
        sendResponse({ settings, seed, profile, presets: presetList(), geo });
        break;
      }
      case 'refreshGeo': {
        const geo = await getGeo(true);
        await rebuild();
        sendResponse({ ok: true, geo });
        break;
      }
      case 'setSettings': {
        const next = await setSettings(msg.patch || {});
        sendResponse({ ok: true, settings: next });
        break;
      }
      case 'regenerateSeed': {
        const seed = await regenerateSeed();
        sendResponse({ ok: true, seed });
        break;
      }
      case 'newIdentity': {
        // Optionally switch to a chosen device first.
        if (msg.device) await setSettings({ mode: 'blend', preset: msg.device });
        const seed = await regenerateSeed(); // fresh fingerprint (+ rebuild)
        if (msg.timezoneSource === 'ip') await getGeo(true).then(() => rebuild());
        let cleared = false;
        if (msg.clean !== false) {
          const settings = await getSettings();
          cleared = await clearBrowsingState(settings.cleanData);
        }
        const settings = await getSettings();
        const profile = resolveProfile(settings, seed);
        if (settings.timezoneSource === 'ip' && settings.protections.timezone) {
          const geo = await getGeo(false);
          if (geo && geo.timezone) profile.timezone = geo.timezone;
        }
        sendResponse({ ok: true, seed, cleared, profile });
        break;
      }
      case 'resetSettings': {
        await chrome.storage.local.set({ [SETTINGS_KEY]: DEFAULT_SETTINGS });
        await rebuild();
        sendResponse({ ok: true, settings: DEFAULT_SETTINGS });
        break;
      }
      case 'aiSuggest': {
        const res = await aiSuggest(msg.host);
        sendResponse(res);
        break;
      }
      default:
        sendResponse({ ok: false, error: 'unknown message' });
    }
  })();
  return true; // async
});

function presetList() {
  return Object.entries(PRESETS).map(([id, p]) => ({ id, label: p.label }));
}

// ---- optional cloud-AI profile advisor -------------------------------------

function aiSystemPrompt(presets) {
  const list = presets.map((p) => `${p.id} = ${p.label}`).join('; ');
  return [
    'Eres el asesor de perfiles de "Nébula", una extensión de privacidad anti-fingerprinting.',
    'El usuario está en un sitio web y quiere la configuración más PRIVADA y COHERENTE.',
    'Elige UNA recomendación:',
    '- "allowlist": desactivar la extensión en este sitio (ideal para bancos, pagos, gobierno, o sitios donde el usuario inicia sesión con su identidad real, o que se rompen al falsear datos).',
    '- "device": imitar un dispositivo que pase desapercibido Y sea coherente con el sistema/motor REAL del usuario (nunca elijas un SO que contradiga el real; p. ej. no elijas iPhone si el navegador real es Android/Chromium).',
    '- "keep": mantener la protección aleatoria actual (bien para navegación general).',
    'Reglas: SOLO ayudas con privacidad y coherencia interna. NO ayudas a evadir el antifraude, la detección de bots ni la verificación de ningún servicio concreto. Si falsear datos en este sitio es inútil o arriesgado, prefiere "allowlist".',
    `Presets disponibles: ${list}.`,
    'Responde SOLO con un objeto JSON compacto, sin prosa ni markdown:',
    '{"action":"allowlist"|"device"|"keep","preset":"<id del preset o null>","reason":"<una frase corta en español>"}'
  ].join('\n');
}

function parseSuggestion(text) {
  if (!text) return null;
  let t = text.trim().replace(/^```(?:json)?/i, '').replace(/```$/, '').trim();
  const a = t.indexOf('{');
  const b = t.lastIndexOf('}');
  if (a >= 0 && b > a) t = t.slice(a, b + 1);
  try {
    const o = JSON.parse(t);
    if (!o || !['allowlist', 'device', 'keep'].includes(o.action)) return null;
    return { action: o.action, preset: o.preset || null, reason: String(o.reason || '') };
  } catch { return null; }
}

async function aiSuggest(host) {
  const settings = await getSettings();
  const ai = settings.ai || {};
  if (!ai.enabled) return { ok: false, error: 'El asistente IA está desactivado.' };
  if (!ai.apiKey) return { ok: false, error: 'Falta la API key en Ajustes.' };

  const presets = presetList();
  const system = aiSystemPrompt(presets);
  const realUA = (typeof navigator !== 'undefined' && navigator.userAgent) || 'desconocido';
  const userMsg = `Sitio: ${host || 'desconocido'}\nNavegador real del usuario: ${realUA}`;

  // OpenAI-compatible providers (same request/response shape, different endpoint).
  const OPENAI_COMPAT = {
    openai: { url: 'https://api.openai.com/v1/chat/completions', model: 'gpt-4o-mini' },
    groq: { url: 'https://api.groq.com/openai/v1/chat/completions', model: 'llama-3.3-70b-versatile' },
    openrouter: { url: 'https://openrouter.ai/api/v1/chat/completions', model: 'meta-llama/llama-3.3-70b-instruct:free' }
  };

  try {
    let text = '';
    if (ai.provider === 'gemini') {
      const model = ai.model || 'gemini-2.0-flash';
      const url = `https://generativelanguage.googleapis.com/v1beta/models/${encodeURIComponent(model)}:generateContent?key=${encodeURIComponent(ai.apiKey)}`;
      const res = await fetch(url, {
        method: 'POST',
        headers: { 'content-type': 'application/json' },
        body: JSON.stringify({
          system_instruction: { parts: [{ text: system }] },
          contents: [{ role: 'user', parts: [{ text: userMsg }] }],
          generationConfig: { maxOutputTokens: 500, temperature: 0.2 }
        })
      });
      const data = await res.json();
      if (!res.ok) throw new Error((data.error && data.error.message) || ('HTTP ' + res.status));
      const parts = data.candidates && data.candidates[0] && data.candidates[0].content && data.candidates[0].content.parts;
      text = (parts || []).map((p) => p.text || '').join('');
    } else if (OPENAI_COMPAT[ai.provider]) {
      const cfg = OPENAI_COMPAT[ai.provider];
      const headers = { 'content-type': 'application/json', authorization: 'Bearer ' + ai.apiKey };
      if (ai.provider === 'openrouter') headers['x-title'] = 'Nebula Anti-Fingerprint';
      const res = await fetch(cfg.url, {
        method: 'POST',
        headers,
        body: JSON.stringify({
          model: ai.model || cfg.model,
          max_tokens: 500,
          messages: [{ role: 'system', content: system }, { role: 'user', content: userMsg }]
        })
      });
      const data = await res.json();
      if (!res.ok) throw new Error((data.error && data.error.message) || ('HTTP ' + res.status));
      text = (data.choices && data.choices[0] && data.choices[0].message && data.choices[0].message.content) || '';
    } else {
      const res = await fetch('https://api.anthropic.com/v1/messages', {
        method: 'POST',
        headers: {
          'content-type': 'application/json',
          'x-api-key': ai.apiKey,
          'anthropic-version': '2023-06-01',
          'anthropic-dangerous-direct-browser-access': 'true'
        },
        body: JSON.stringify({
          model: ai.model || 'claude-opus-5-5',
          max_tokens: 1024,
          system,
          messages: [{ role: 'user', content: userMsg }]
        })
      });
      const data = await res.json();
      if (!res.ok) throw new Error((data.error && data.error.message) || ('HTTP ' + res.status));
      text = (data.content || []).filter((b) => b.type === 'text').map((b) => b.text).join('');
    }
    const suggestion = parseSuggestion(text);
    if (!suggestion) return { ok: false, error: 'La IA no devolvió una sugerencia válida.' };
    return { ok: true, suggestion };
  } catch (e) {
    return { ok: false, error: String((e && e.message) || e) };
  }
}

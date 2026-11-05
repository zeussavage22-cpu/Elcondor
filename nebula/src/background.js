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
const DNR_RULE_BASE = 1000;

// ---- settings --------------------------------------------------------------

async function getSettings() {
  const got = await chrome.storage.local.get(SETTINGS_KEY);
  return { ...DEFAULT_SETTINGS, ...(got[SETTINGS_KEY] || {}) };
}

async function setSettings(patch) {
  const cur = await getSettings();
  const next = { ...cur, ...patch };
  if (patch.protections) next.protections = { ...cur.protections, ...patch.protections };
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
      return randomProfile(mulberry32(seed), pool);
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
  // Remove our rules first.
  const existing = await chrome.declarativeNetRequest.getDynamicRules();
  const removeRuleIds = existing.filter((r) => r.id >= DNR_RULE_BASE && r.id < DNR_RULE_BASE + 100).map((r) => r.id);

  const addRules = [];
  if (settings.enabled && settings.protections.headers) {
    const ua = profile.uaData || {};
    const headers = [
      { header: 'user-agent', operation: 'set', value: profile.userAgent },
      { header: 'accept-language', operation: 'set', value: `${profile.language},${profile.language.split('-')[0]};q=0.9` }
    ];
    if (ua.brands && ua.brands.length) {
      headers.push({ header: 'sec-ch-ua', operation: 'set', value: buildSecChUa(ua.brands) });
      headers.push({ header: 'sec-ch-ua-mobile', operation: 'set', value: ua.mobile ? '?1' : '?0' });
      headers.push({ header: 'sec-ch-ua-platform', operation: 'set', value: `"${ua.platform}"` });
      // High-entropy client hints: only present them if the server asks, but
      // rewriting unconditionally keeps them consistent when they are sent.
      headers.push({ header: 'sec-ch-ua-platform-version', operation: 'set', value: `"${ua.platformVersion || ''}"` });
      headers.push({ header: 'sec-ch-ua-arch', operation: 'set', value: `"${ua.architecture || ''}"` });
      headers.push({ header: 'sec-ch-ua-bitness', operation: 'set', value: `"${ua.bitness || ''}"` });
      headers.push({ header: 'sec-ch-ua-model', operation: 'set', value: `"${ua.model || ''}"` });
      headers.push({ header: 'sec-ch-ua-full-version', operation: 'set', value: `"${ua.uaFullVersion || ''}"` });
      headers.push({ header: 'sec-ch-ua-full-version-list', operation: 'set', value: buildFullVersionList(ua.brands, ua.uaFullVersion || '') });
    } else {
      // Safari / non-Chromium profiles never send Client Hints — strip them so
      // the underlying Chromium browser does not betray the disguise.
      for (const h of ['sec-ch-ua', 'sec-ch-ua-mobile', 'sec-ch-ua-platform', 'sec-ch-ua-platform-version',
        'sec-ch-ua-arch', 'sec-ch-ua-bitness', 'sec-ch-ua-model', 'sec-ch-ua-full-version', 'sec-ch-ua-full-version-list']) {
        headers.push({ header: h, operation: 'remove' });
      }
    }

    const condition = {
      urlFilter: '*',
      resourceTypes: ['main_frame', 'sub_frame', 'xmlhttprequest', 'script', 'image', 'stylesheet', 'font', 'media', 'websocket', 'other']
    };
    const excluded = normalizeDomains(settings.allowlist);
    if (excluded.length) condition.excludedRequestDomains = excluded;

    addRules.push({
      id: DNR_RULE_BASE,
      priority: 1,
      action: { type: 'modifyHeaders', requestHeaders: headers },
      condition
    });
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

// ---- publish runtime config for content scripts ----------------------------

async function rebuild() {
  const settings = await getSettings();
  const seed = await getSeed();
  const profile = resolveProfile(settings, seed);

  const config = {
    enabled: settings.enabled,
    mode: settings.mode,
    noise: settings.noise,
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
        sendResponse({ settings, seed, profile, presets: presetList() });
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
      case 'resetSettings': {
        await chrome.storage.local.set({ [SETTINGS_KEY]: DEFAULT_SETTINGS });
        await rebuild();
        sendResponse({ ok: true, settings: DEFAULT_SETTINGS });
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

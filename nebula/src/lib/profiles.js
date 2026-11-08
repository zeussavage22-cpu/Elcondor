// Nébula — profile presets and a consistency-preserving randomizer.
// A "profile" is the coherent set of values we expose everywhere (JS APIs + HTTP
// headers) so that the browser looks like one plausible, internally consistent
// machine instead of a contradictory mash-up that is trivial to flag.

export const DEFAULT_SETTINGS = {
  enabled: true,
  // mode: how profile values are chosen.
  //   'blend'      -> use a fixed, very common preset (hide in the crowd)
  //   'per-session'-> randomize once per browser session
  //   'per-site'   -> randomize deterministically per eTLD+1
  //   'custom'     -> use the user-edited custom profile
  mode: 'per-session',
  preset: 'win-chrome',
  // which device families the random modes draw from:
  //   'desktop' -> win | mac | linux   'mobile' -> android | ios   'all' -> every family
  randomPool: 'desktop',
  // timezone source:
  //   'ip'     -> match the geolocation of the current IP (real or VPN/proxy exit),
  //               so sites that cross-check timezone vs IP see no mismatch (makes
  //               one network request to a geo-IP service when on)
  //   'region' -> use the random/preset timezone from the chosen region (no network)
  timezoneSource: 'ip',
  // which region the random modes draw timezone+locale from: 'americas' | 'europe' | 'any'
  timezoneRegion: 'americas',
  noise: 'medium', // low | medium | high — intensity of canvas/audio/webgl noise
  // noiseStrategy:
  //   'session'  -> stable within a session, different per site (DEFAULT; stealthy —
  //                 repeated reads match, so it does not look like a randomizing bot)
  //   'per-read' -> fresh randomness on every canvas/audio/webgl read (tools like
  //                 Cover Your Tracks flag this as "randomized", but anti-fraud
  //                 systems can notice the per-read changes)
  noiseStrategy: 'session',
  // per-protection master switches
  protections: {
    canvas: true,
    webgl: true,
    audio: true,
    fonts: true,
    clientRects: true,
    navigator: true,
    hardware: true,
    screen: true,
    timezone: true,
    locale: true,
    webrtc: true,
    battery: true,
    mediaDevices: true,
    plugins: true,
    speech: true,
    timing: true,
    uaData: true,
    connection: true,
    webdriver: true,
    toStringMask: true,
    headers: true, // rewrite UA / Accept-Language / Sec-CH-UA at the HTTP layer
    trackers: true, // block known analytics/ad trackers (network layer)
    sensors: true, // block Gamepad / WebUSB / Serial / HID / Bluetooth / keyboard-layout
    webgpu: true, // hide navigator.gpu (WebGPU fingerprinting)
    dnt: true, // Do Not Track + Global Privacy Control signal and headers
    urlClean: true, // strip tracking params (utm_*, fbclid, gclid, ...) from URLs
    referer: true, // strip the Referer header on cross-site (third-party) requests
    thirdPartyCookies: true // block third-party cookies (strip Cookie/Set-Cookie cross-site)
  },
  webrtcMode: 'safe', // safe (hide local IP) | relay (relay only) | disabled | off
  timingPrecisionMs: 0.1, // rounding applied to performance.now / Date.now
  // what "new clean profile" wipes along with generating a fresh identity
  cleanData: { cookies: true, storage: true, cache: true },
  // optional cloud-AI advisor (OFF by default; uses the user's own API key and
  // sends only the site hostname + the real browser UA to the chosen provider).
  ai: { enabled: false, provider: 'pollinations', apiKey: '', model: '' },
  // sites where Nébula does nothing (e.g. your bank). eTLD+1 or full hostnames.
  allowlist: [],
  customProfile: null, // filled from a preset the first time the user edits it
  // accent color for the UI
  theme: 'purple', // purple | teal | blue | pink | orange | green
  // named identities the user can save and switch between
  savedProfiles: [], // [{ id, name, config: { mode, preset, randomPool, timezoneRegion, protections } }]
  // v2.1 extras
  shieldLevel: 'balanced', // basic | balanced | max (quick protection presets)
  autoRotateMin: 0, // 0 = off; otherwise minutes between automatic new identities
  cookieBanners: true, // auto-hide / dismiss cookie-consent banners
  fpLog: true, // record which sites tried to fingerprint you (shown in the panel)
  travelCountry: '' // '' = off; else a country code (ES, US, MX, ...) forcing tz+locale
};

// Travel mode: coherent timezone + locale per country code.
export const TRAVEL_COUNTRIES = {
  US: { label: 'Estados Unidos', tz: 'America/New_York', language: 'en-US', languages: ['en-US', 'en'] },
  MX: { label: 'México', tz: 'America/Mexico_City', language: 'es-MX', languages: ['es-MX', 'es'] },
  CO: { label: 'Colombia', tz: 'America/Bogota', language: 'es-419', languages: ['es-419', 'es'] },
  AR: { label: 'Argentina', tz: 'America/Argentina/Buenos_Aires', language: 'es-AR', languages: ['es-AR', 'es'] },
  BR: { label: 'Brasil', tz: 'America/Sao_Paulo', language: 'pt-BR', languages: ['pt-BR', 'pt'] },
  ES: { label: 'España', tz: 'Europe/Madrid', language: 'es-ES', languages: ['es-ES', 'es'] },
  GB: { label: 'Reino Unido', tz: 'Europe/London', language: 'en-GB', languages: ['en-GB', 'en'] },
  DE: { label: 'Alemania', tz: 'Europe/Berlin', language: 'de-DE', languages: ['de-DE', 'de'] },
  FR: { label: 'Francia', tz: 'Europe/Paris', language: 'fr-FR', languages: ['fr-FR', 'fr'] }
};

// Shield-level presets → which protections turn on. Keys not listed stay as-is.
export const SHIELD_LEVELS = {
  basic: ['navigator', 'hardware', 'screen', 'timezone', 'locale', 'headers', 'uaData', 'webdriver', 'toStringMask', 'trackers', 'dnt', 'urlClean'],
  balanced: ['canvas', 'webgl', 'audio', 'fonts', 'clientRects', 'navigator', 'hardware', 'screen', 'timezone', 'locale', 'webrtc', 'plugins', 'timing', 'uaData', 'connection', 'webdriver', 'toStringMask', 'headers', 'trackers', 'dnt', 'urlClean', 'referer', 'thirdPartyCookies'],
  max: null // null = everything on
};

export const ALL_PROTECTIONS = Object.keys(DEFAULT_SETTINGS.protections);

// Build a full protections object for a shield level.
export function protectionsForLevel(level) {
  const out = {};
  const list = SHIELD_LEVELS[level];
  for (const k of ALL_PROTECTIONS) out[k] = list === null ? true : list.includes(k);
  return out;
}

// ---- Presets ---------------------------------------------------------------
// Kept deliberately "boringly common" so blending in actually reduces entropy.

export const PRESETS = {
  'win-chrome': {
    label: 'Windows 11 · Chrome',
    platform: 'Win32',
    oscpu: undefined,
    vendor: 'Google Inc.',
    userAgent:
      'Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/131.0.0.0 Safari/537.36',
    appVersion:
      '5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/131.0.0.0 Safari/537.36',
    hardwareConcurrency: 8,
    deviceMemory: 8,
    maxTouchPoints: 0,
    language: 'en-US',
    languages: ['en-US', 'en'],
    screen: { width: 1920, height: 1080, availWidth: 1920, availHeight: 1032, colorDepth: 24, pixelDepth: 24, dpr: 1 },
    timezone: 'America/New_York',
    webgl: { vendor: 'Google Inc. (NVIDIA)', renderer: 'ANGLE (NVIDIA, NVIDIA GeForce RTX 3060 Direct3D11 vs_5_0 ps_5_0, D3D11)' },
    uaData: {
      mobile: false,
      platform: 'Windows',
      platformVersion: '15.0.0',
      architecture: 'x86',
      bitness: '64',
      model: '',
      uaFullVersion: '131.0.6778.86',
      brands: [
        { brand: 'Chromium', version: '131' },
        { brand: 'Not_A Brand', version: '24' },
        { brand: 'Google Chrome', version: '131' }
      ]
    }
  },
  'mac-chrome': {
    label: 'macOS · Chrome',
    platform: 'MacIntel',
    vendor: 'Google Inc.',
    userAgent:
      'Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/131.0.0.0 Safari/537.36',
    appVersion:
      '5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/131.0.0.0 Safari/537.36',
    hardwareConcurrency: 8,
    deviceMemory: 8,
    maxTouchPoints: 0,
    language: 'en-US',
    languages: ['en-US', 'en'],
    screen: { width: 1512, height: 982, availWidth: 1512, availHeight: 944, colorDepth: 30, pixelDepth: 30, dpr: 2 },
    timezone: 'America/Los_Angeles',
    webgl: { vendor: 'Google Inc. (Apple)', renderer: 'ANGLE (Apple, ANGLE Metal Renderer: Apple M2, Unspecified Version)' },
    uaData: {
      mobile: false,
      platform: 'macOS',
      platformVersion: '14.6.1',
      architecture: 'arm',
      bitness: '64',
      model: '',
      uaFullVersion: '131.0.6778.86',
      brands: [
        { brand: 'Chromium', version: '131' },
        { brand: 'Not_A Brand', version: '24' },
        { brand: 'Google Chrome', version: '131' }
      ]
    }
  },
  'android-chrome': {
    label: 'Android · Chrome',
    platform: 'Linux armv81',
    vendor: 'Google Inc.',
    userAgent:
      'Mozilla/5.0 (Linux; Android 14; Pixel 7) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/131.0.0.0 Mobile Safari/537.36',
    appVersion:
      '5.0 (Linux; Android 14; Pixel 7) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/131.0.0.0 Mobile Safari/537.36',
    hardwareConcurrency: 8,
    deviceMemory: 8,
    maxTouchPoints: 5,
    language: 'en-US',
    languages: ['en-US', 'en'],
    screen: { width: 412, height: 915, availWidth: 412, availHeight: 915, colorDepth: 24, pixelDepth: 24, dpr: 2.625 },
    timezone: 'America/New_York',
    webgl: { vendor: 'Qualcomm', renderer: 'Adreno (TM) 730' },
    uaData: {
      mobile: true,
      platform: 'Android',
      platformVersion: '14.0.0',
      architecture: '',
      bitness: '',
      model: 'Pixel 7',
      uaFullVersion: '131.0.6778.86',
      brands: [
        { brand: 'Chromium', version: '131' },
        { brand: 'Not_A Brand', version: '24' },
        { brand: 'Google Chrome', version: '131' }
      ]
    }
  },
  'linux-chrome': {
    label: 'Linux · Chrome',
    platform: 'Linux x86_64',
    vendor: 'Google Inc.',
    userAgent:
      'Mozilla/5.0 (X11; Linux x86_64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/131.0.0.0 Safari/537.36',
    appVersion:
      '5.0 (X11; Linux x86_64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/131.0.0.0 Safari/537.36',
    hardwareConcurrency: 12,
    deviceMemory: 8,
    maxTouchPoints: 0,
    language: 'en-US',
    languages: ['en-US', 'en'],
    screen: { width: 1920, height: 1080, availWidth: 1920, availHeight: 1053, colorDepth: 24, pixelDepth: 24, dpr: 1 },
    timezone: 'America/New_York',
    webgl: { vendor: 'Google Inc. (Intel)', renderer: 'ANGLE (Intel, Mesa Intel(R) UHD Graphics (CML GT2), OpenGL 4.6)' },
    uaData: {
      mobile: false,
      platform: 'Linux',
      platformVersion: '6.5.0',
      architecture: 'x86',
      bitness: '64',
      model: '',
      uaFullVersion: '131.0.6778.86',
      brands: [
        { brand: 'Chromium', version: '131' },
        { brand: 'Not_A Brand', version: '24' },
        { brand: 'Google Chrome', version: '131' }
      ]
    }
  }
  ,
  'ios-safari': {
    label: 'iPhone · Safari',
    platform: 'iPhone',
    vendor: 'Apple Computer, Inc.',
    userAgent:
      'Mozilla/5.0 (iPhone; CPU iPhone OS 17_6 like Mac OS X) AppleWebKit/605.1.15 (KHTML, like Gecko) Version/17.6 Mobile/15E148 Safari/604.1',
    appVersion:
      '5.0 (iPhone; CPU iPhone OS 17_6 like Mac OS X) AppleWebKit/605.1.15 (KHTML, like Gecko) Version/17.6 Mobile/15E148 Safari/604.1',
    hardwareConcurrency: 4,
    // Safari does not expose deviceMemory — leave undefined so we hide it.
    deviceMemory: undefined,
    maxTouchPoints: 5,
    language: 'en-US',
    languages: ['en-US', 'en'],
    screen: { width: 393, height: 852, availWidth: 393, availHeight: 852, colorDepth: 24, pixelDepth: 24, dpr: 3 },
    timezone: 'America/New_York',
    webgl: { vendor: 'Apple Inc.', renderer: 'Apple GPU' },
    // Safari has NO Client Hints — null tells the shield to hide userAgentData
    // and the background to strip Sec-CH-UA-* headers.
    uaData: null
  },
  'ipad-safari': {
    label: 'iPad · Safari',
    platform: 'MacIntel', // iPadOS Safari reports MacIntel
    vendor: 'Apple Computer, Inc.',
    userAgent:
      'Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/605.1.15 (KHTML, like Gecko) Version/17.6 Safari/605.1.15',
    appVersion:
      '5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/605.1.15 (KHTML, like Gecko) Version/17.6 Safari/605.1.15',
    hardwareConcurrency: 4,
    deviceMemory: undefined,
    maxTouchPoints: 5,
    language: 'en-US',
    languages: ['en-US', 'en'],
    screen: { width: 1024, height: 1366, availWidth: 1024, availHeight: 1366, colorDepth: 24, pixelDepth: 24, dpr: 2 },
    timezone: 'America/New_York',
    webgl: { vendor: 'Apple Inc.', renderer: 'Apple GPU' },
    uaData: null
  },
  'android-samsung': {
    label: 'Android · Samsung Galaxy',
    platform: 'Linux armv81',
    vendor: 'Google Inc.',
    userAgent:
      'Mozilla/5.0 (Linux; Android 14; SM-S918B) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/131.0.0.0 Mobile Safari/537.36',
    appVersion:
      '5.0 (Linux; Android 14; SM-S918B) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/131.0.0.0 Mobile Safari/537.36',
    hardwareConcurrency: 8,
    deviceMemory: 8,
    maxTouchPoints: 5,
    language: 'en-US',
    languages: ['en-US', 'en'],
    screen: { width: 384, height: 824, availWidth: 384, availHeight: 824, colorDepth: 24, pixelDepth: 24, dpr: 2.8125 },
    timezone: 'America/New_York',
    webgl: { vendor: 'Qualcomm', renderer: 'Adreno (TM) 740' },
    uaData: {
      mobile: true, platform: 'Android', platformVersion: '14.0.0', architecture: '', bitness: '', model: 'SM-S918B',
      uaFullVersion: '131.0.6778.86',
      brands: [
        { brand: 'Chromium', version: '131' },
        { brand: 'Not_A Brand', version: '24' },
        { brand: 'Google Chrome', version: '131' }
      ]
    }
  },
  'win-edge': {
    label: 'Windows 11 · Edge',
    platform: 'Win32', vendor: 'Google Inc.',
    userAgent: 'Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/131.0.0.0 Safari/537.36 Edg/131.0.0.0',
    appVersion: '5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/131.0.0.0 Safari/537.36 Edg/131.0.0.0',
    hardwareConcurrency: 8, deviceMemory: 8, maxTouchPoints: 0,
    language: 'en-US', languages: ['en-US', 'en'],
    screen: { width: 1920, height: 1080, availWidth: 1920, availHeight: 1032, colorDepth: 24, pixelDepth: 24, dpr: 1 },
    timezone: 'America/New_York',
    webgl: { vendor: 'Google Inc. (Intel)', renderer: 'ANGLE (Intel, Intel(R) Iris(R) Xe Graphics Direct3D11 vs_5_0 ps_5_0, D3D11)' },
    uaData: {
      mobile: false, platform: 'Windows', platformVersion: '15.0.0', architecture: 'x86', bitness: '64', model: '', uaFullVersion: '131.0.2903.70',
      brands: [{ brand: 'Chromium', version: '131' }, { brand: 'Not_A Brand', version: '24' }, { brand: 'Microsoft Edge', version: '131' }]
    }
  },
  'mac-safari': {
    label: 'macOS · Safari',
    platform: 'MacIntel', vendor: 'Apple Computer, Inc.',
    userAgent: 'Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/605.1.15 (KHTML, like Gecko) Version/17.6 Safari/605.1.15',
    appVersion: '5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/605.1.15 (KHTML, like Gecko) Version/17.6 Safari/605.1.15',
    hardwareConcurrency: 8, deviceMemory: undefined, maxTouchPoints: 0,
    language: 'en-US', languages: ['en-US', 'en'],
    screen: { width: 1512, height: 982, availWidth: 1512, availHeight: 944, colorDepth: 30, pixelDepth: 30, dpr: 2 },
    timezone: 'America/New_York',
    webgl: { vendor: 'Apple Inc.', renderer: 'Apple GPU' },
    uaData: null
  },
  'android-xiaomi': {
    label: 'Android · Xiaomi',
    platform: 'Linux armv81', vendor: 'Google Inc.',
    userAgent: 'Mozilla/5.0 (Linux; Android 14; 23049PCD8G) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/131.0.0.0 Mobile Safari/537.36',
    appVersion: '5.0 (Linux; Android 14; 23049PCD8G) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/131.0.0.0 Mobile Safari/537.36',
    hardwareConcurrency: 8, deviceMemory: 8, maxTouchPoints: 5,
    language: 'en-US', languages: ['en-US', 'en'],
    screen: { width: 393, height: 873, availWidth: 393, availHeight: 873, colorDepth: 24, pixelDepth: 24, dpr: 2.75 },
    timezone: 'America/New_York',
    webgl: { vendor: 'Qualcomm', renderer: 'Adreno (TM) 725' },
    uaData: {
      mobile: true, platform: 'Android', platformVersion: '14.0.0', architecture: '', bitness: '', model: '23049PCD8G', uaFullVersion: '131.0.6778.86',
      brands: [{ brand: 'Chromium', version: '131' }, { brand: 'Not_A Brand', version: '24' }, { brand: 'Google Chrome', version: '131' }]
    }
  },
  'android-oneplus': {
    label: 'Android · OnePlus',
    platform: 'Linux armv81', vendor: 'Google Inc.',
    userAgent: 'Mozilla/5.0 (Linux; Android 14; CPH2581) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/131.0.0.0 Mobile Safari/537.36',
    appVersion: '5.0 (Linux; Android 14; CPH2581) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/131.0.0.0 Mobile Safari/537.36',
    hardwareConcurrency: 8, deviceMemory: 12, maxTouchPoints: 5,
    language: 'en-US', languages: ['en-US', 'en'],
    screen: { width: 412, height: 919, availWidth: 412, availHeight: 919, colorDepth: 24, pixelDepth: 24, dpr: 3.5 },
    timezone: 'America/New_York',
    webgl: { vendor: 'Qualcomm', renderer: 'Adreno (TM) 750' },
    uaData: {
      mobile: true, platform: 'Android', platformVersion: '14.0.0', architecture: '', bitness: '', model: 'CPH2581', uaFullVersion: '131.0.6778.86',
      brands: [{ brand: 'Chromium', version: '131' }, { brand: 'Not_A Brand', version: '24' }, { brand: 'Google Chrome', version: '131' }]
    }
  },
  'ios-15promax': {
    label: 'iPhone 15 Pro Max · Safari',
    platform: 'iPhone', vendor: 'Apple Computer, Inc.',
    userAgent: 'Mozilla/5.0 (iPhone; CPU iPhone OS 17_6 like Mac OS X) AppleWebKit/605.1.15 (KHTML, like Gecko) Version/17.6 Mobile/15E148 Safari/604.1',
    appVersion: '5.0 (iPhone; CPU iPhone OS 17_6 like Mac OS X) AppleWebKit/605.1.15 (KHTML, like Gecko) Version/17.6 Mobile/15E148 Safari/604.1',
    hardwareConcurrency: 6, deviceMemory: undefined, maxTouchPoints: 5,
    language: 'en-US', languages: ['en-US', 'en'],
    screen: { width: 430, height: 932, availWidth: 430, availHeight: 932, colorDepth: 24, pixelDepth: 24, dpr: 3 },
    timezone: 'America/New_York',
    webgl: { vendor: 'Apple Inc.', renderer: 'Apple GPU' },
    uaData: null
  }
};

// Pools used by the randomizer. Everything is chosen so the pieces stay coherent.
const POOLS = {
  chromeVersions: [
    { major: '129', full: '129.0.6668.100' },
    { major: '130', full: '130.0.6723.92' },
    { major: '131', full: '131.0.6778.86' }
  ],
  win: {
    screens: [
      { width: 1920, height: 1080, availHeight: 1032 },
      { width: 1536, height: 864, availHeight: 816 },
      { width: 2560, height: 1440, availHeight: 1392 },
      { width: 1366, height: 768, availHeight: 728 }
    ],
    webgl: [
      { vendor: 'Google Inc. (NVIDIA)', renderer: 'ANGLE (NVIDIA, NVIDIA GeForce RTX 3060 Direct3D11 vs_5_0 ps_5_0, D3D11)' },
      { vendor: 'Google Inc. (NVIDIA)', renderer: 'ANGLE (NVIDIA, NVIDIA GeForce GTX 1660 Direct3D11 vs_5_0 ps_5_0, D3D11)' },
      { vendor: 'Google Inc. (Intel)', renderer: 'ANGLE (Intel, Intel(R) UHD Graphics 630 Direct3D11 vs_5_0 ps_5_0, D3D11)' },
      { vendor: 'Google Inc. (AMD)', renderer: 'ANGLE (AMD, AMD Radeon RX 580 Direct3D11 vs_5_0 ps_5_0, D3D11)' }
    ]
  },
  mac: {
    screens: [
      { width: 1512, height: 982, availHeight: 944, dpr: 2, colorDepth: 30 },
      { width: 1440, height: 900, availHeight: 862, dpr: 2, colorDepth: 30 },
      { width: 1728, height: 1117, availHeight: 1079, dpr: 2, colorDepth: 30 }
    ],
    webgl: [
      { vendor: 'Google Inc. (Apple)', renderer: 'ANGLE (Apple, ANGLE Metal Renderer: Apple M1, Unspecified Version)' },
      { vendor: 'Google Inc. (Apple)', renderer: 'ANGLE (Apple, ANGLE Metal Renderer: Apple M2, Unspecified Version)' },
      { vendor: 'Google Inc. (Apple)', renderer: 'ANGLE (Apple, ANGLE Metal Renderer: Apple M3, Unspecified Version)' }
    ]
  },
  cores: [4, 8, 12, 16],
  memory: [4, 8],
  timezones: ['America/New_York', 'America/Chicago', 'America/Los_Angeles', 'Europe/London', 'Europe/Madrid', 'Europe/Berlin'],
  locales: [
    { language: 'en-US', languages: ['en-US', 'en'] },
    { language: 'en-GB', languages: ['en-GB', 'en'] },
    { language: 'es-ES', languages: ['es-ES', 'es'] },
    { language: 'es-419', languages: ['es-419', 'es'] }
  ]
};

function pick(rng, arr) {
  return arr[Math.floor(rng() * arr.length)];
}

// Extra device pools used by the randomizer (kept local for clarity).
const DEVICE_POOLS = {
  linux: {
    screens: [
      { width: 1920, height: 1080, availHeight: 1053 },
      { width: 1366, height: 768, availHeight: 741 },
      { width: 2560, height: 1440, availHeight: 1413 }
    ],
    webgl: [
      { vendor: 'Google Inc. (Intel)', renderer: 'ANGLE (Intel, Mesa Intel(R) UHD Graphics (CML GT2), OpenGL 4.6)' },
      { vendor: 'Google Inc. (NVIDIA Corporation)', renderer: 'ANGLE (NVIDIA Corporation, NVIDIA GeForce GTX 1060/PCIe/SSE2, OpenGL 4.6)' },
      { vendor: 'Google Inc. (AMD)', renderer: 'ANGLE (AMD, AMD Radeon RX 6600 (navi23 LLVM 15.0.7), OpenGL 4.6)' }
    ]
  },
  android: [
    { model: 'Pixel 7', ua: 'Pixel 7', screen: { width: 412, height: 915, dpr: 2.625 }, webgl: { vendor: 'Qualcomm', renderer: 'Adreno (TM) 730' } },
    { model: 'Pixel 8', ua: 'Pixel 8', screen: { width: 412, height: 915, dpr: 2.625 }, webgl: { vendor: 'Qualcomm', renderer: 'Adreno (TM) 735' } },
    { model: 'SM-S918B', ua: 'SM-S918B', screen: { width: 384, height: 824, dpr: 2.8125 }, webgl: { vendor: 'Qualcomm', renderer: 'Adreno (TM) 740' } },
    { model: 'SM-A536B', ua: 'SM-A536B', screen: { width: 384, height: 854, dpr: 2.8125 }, webgl: { vendor: 'ARM', renderer: 'Mali-G68' } },
    { model: '23049PCD8G', ua: '23049PCD8G', screen: { width: 393, height: 873, dpr: 2.75 }, webgl: { vendor: 'Qualcomm', renderer: 'Adreno (TM) 725' } },
    { model: 'CPH2581', ua: 'CPH2581', screen: { width: 412, height: 919, dpr: 3.5 }, webgl: { vendor: 'Qualcomm', renderer: 'Adreno (TM) 750' } }
  ],
  ios: [
    { ver: '17_6', version: '17.6', screen: { width: 393, height: 852, dpr: 3 } }, // iPhone 15/14 Pro
    { ver: '17_5_1', version: '17.5.1', screen: { width: 390, height: 844, dpr: 3 } }, // iPhone 13/14
    { ver: '16_7', version: '16.7', screen: { width: 428, height: 926, dpr: 3 } }, // iPhone 13 Pro Max
    { ver: '17_6', version: '17.6', screen: { width: 430, height: 932, dpr: 3 } } // iPhone 15 Pro Max
  ]
};

export const RANDOM_POOLS = {
  desktop: ['win', 'mac', 'linux'],
  mobile: ['android', 'ios'],
  all: ['win', 'mac', 'linux', 'android', 'ios']
};

// Coherent timezone + locale pairs so the region never contradicts the language
// (e.g. no "es-ES in New York"). Picked together by the randomizer.
const AMERICAS = [
  { tz: 'America/New_York', language: 'en-US', languages: ['en-US', 'en'] },
  { tz: 'America/Chicago', language: 'en-US', languages: ['en-US', 'en'] },
  { tz: 'America/Los_Angeles', language: 'en-US', languages: ['en-US', 'en'] },
  { tz: 'America/Denver', language: 'en-US', languages: ['en-US', 'en'] },
  { tz: 'America/Mexico_City', language: 'es-MX', languages: ['es-MX', 'es'] },
  { tz: 'America/Bogota', language: 'es-419', languages: ['es-419', 'es'] },
  { tz: 'America/Lima', language: 'es-419', languages: ['es-419', 'es'] },
  { tz: 'America/Argentina/Buenos_Aires', language: 'es-AR', languages: ['es-AR', 'es'] },
  { tz: 'America/Sao_Paulo', language: 'pt-BR', languages: ['pt-BR', 'pt'] }
];
const EUROPE = [
  { tz: 'Europe/London', language: 'en-GB', languages: ['en-GB', 'en'] },
  { tz: 'Europe/Madrid', language: 'es-ES', languages: ['es-ES', 'es'] },
  { tz: 'Europe/Berlin', language: 'de-DE', languages: ['de-DE', 'de'] },
  { tz: 'Europe/Paris', language: 'fr-FR', languages: ['fr-FR', 'fr'] }
];
const REGION_PAIRS = {
  americas: AMERICAS,
  europe: EUROPE,
  any: AMERICAS.concat(EUROPE)
};

// Build a fully coherent profile from a seeded RNG.
// `pool` is an array of os keys; defaults to the desktop set.
export function randomProfile(rng, pool, region) {
  pool = pool && pool.length ? pool : RANDOM_POOLS.desktop;
  const os = pick(rng, pool);
  const cv = pick(rng, POOLS.chromeVersions);
  const pair = pick(rng, REGION_PAIRS[region] || REGION_PAIRS.americas);
  const loc = { language: pair.language, languages: pair.languages };
  const tz = pair.tz;
  const cores = pick(rng, POOLS.cores);
  const mem = pick(rng, POOLS.memory);
  const chromeBrands = [
    { brand: 'Chromium', version: cv.major },
    { brand: 'Not_A Brand', version: '24' },
    { brand: 'Google Chrome', version: cv.major }
  ];

  if (os === 'mac') {
    const sc = pick(rng, POOLS.mac.screens);
    return {
      platform: 'MacIntel', vendor: 'Google Inc.',
      userAgent: `Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/${cv.major}.0.0.0 Safari/537.36`,
      appVersion: `5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/${cv.major}.0.0.0 Safari/537.36`,
      hardwareConcurrency: cores, deviceMemory: mem, maxTouchPoints: 0,
      language: loc.language, languages: loc.languages,
      screen: { width: sc.width, height: sc.height, availWidth: sc.width, availHeight: sc.availHeight, colorDepth: sc.colorDepth, pixelDepth: sc.colorDepth, dpr: sc.dpr },
      timezone: tz, webgl: pick(rng, POOLS.mac.webgl),
      uaData: { mobile: false, platform: 'macOS', platformVersion: '14.6.1', architecture: 'arm', bitness: '64', model: '', uaFullVersion: cv.full, brands: chromeBrands }
    };
  }

  if (os === 'linux') {
    const sc = pick(rng, DEVICE_POOLS.linux.screens);
    return {
      platform: 'Linux x86_64', vendor: 'Google Inc.',
      userAgent: `Mozilla/5.0 (X11; Linux x86_64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/${cv.major}.0.0.0 Safari/537.36`,
      appVersion: `5.0 (X11; Linux x86_64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/${cv.major}.0.0.0 Safari/537.36`,
      hardwareConcurrency: cores, deviceMemory: mem, maxTouchPoints: 0,
      language: loc.language, languages: loc.languages,
      screen: { width: sc.width, height: sc.height, availWidth: sc.width, availHeight: sc.availHeight, colorDepth: 24, pixelDepth: 24, dpr: 1 },
      timezone: tz, webgl: pick(rng, DEVICE_POOLS.linux.webgl),
      uaData: { mobile: false, platform: 'Linux', platformVersion: '6.5.0', architecture: 'x86', bitness: '64', model: '', uaFullVersion: cv.full, brands: chromeBrands }
    };
  }

  if (os === 'android') {
    const d = pick(rng, DEVICE_POOLS.android);
    return {
      platform: 'Linux armv81', vendor: 'Google Inc.',
      userAgent: `Mozilla/5.0 (Linux; Android 14; ${d.ua}) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/${cv.major}.0.0.0 Mobile Safari/537.36`,
      appVersion: `5.0 (Linux; Android 14; ${d.ua}) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/${cv.major}.0.0.0 Mobile Safari/537.36`,
      hardwareConcurrency: 8, deviceMemory: 8, maxTouchPoints: 5,
      language: loc.language, languages: loc.languages,
      screen: { width: d.screen.width, height: d.screen.height, availWidth: d.screen.width, availHeight: d.screen.height, colorDepth: 24, pixelDepth: 24, dpr: d.screen.dpr },
      timezone: tz, webgl: d.webgl,
      uaData: { mobile: true, platform: 'Android', platformVersion: '14.0.0', architecture: '', bitness: '', model: d.model, uaFullVersion: cv.full, brands: chromeBrands }
    };
  }

  if (os === 'ios') {
    const d = pick(rng, DEVICE_POOLS.ios);
    return {
      platform: 'iPhone', vendor: 'Apple Computer, Inc.',
      userAgent: `Mozilla/5.0 (iPhone; CPU iPhone OS ${d.ver} like Mac OS X) AppleWebKit/605.1.15 (KHTML, like Gecko) Version/${d.version} Mobile/15E148 Safari/604.1`,
      appVersion: `5.0 (iPhone; CPU iPhone OS ${d.ver} like Mac OS X) AppleWebKit/605.1.15 (KHTML, like Gecko) Version/${d.version} Mobile/15E148 Safari/604.1`,
      hardwareConcurrency: 4, deviceMemory: undefined, maxTouchPoints: 5,
      language: loc.language, languages: loc.languages,
      screen: { width: d.screen.width, height: d.screen.height, availWidth: d.screen.width, availHeight: d.screen.height, colorDepth: 24, pixelDepth: 24, dpr: d.screen.dpr },
      timezone: tz, webgl: { vendor: 'Apple Inc.', renderer: 'Apple GPU' },
      uaData: null
    };
  }

  // default: Windows
  const sc = pick(rng, POOLS.win.screens);
  return {
    platform: 'Win32', vendor: 'Google Inc.',
    userAgent: `Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/${cv.major}.0.0.0 Safari/537.36`,
    appVersion: `5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/${cv.major}.0.0.0 Safari/537.36`,
    hardwareConcurrency: cores, deviceMemory: mem, maxTouchPoints: 0,
    language: loc.language, languages: loc.languages,
    screen: { width: sc.width, height: sc.height, availWidth: sc.width, availHeight: sc.availHeight, colorDepth: 24, pixelDepth: 24, dpr: 1 },
    timezone: tz, webgl: pick(rng, POOLS.win.webgl),
    uaData: { mobile: false, platform: 'Windows', platformVersion: '15.0.0', architecture: 'x86', bitness: '64', model: '', uaFullVersion: cv.full, brands: chromeBrands }
  };
}

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
  noise: 'medium', // low | medium | high — intensity of canvas/audio/webgl noise
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
    headers: true // rewrite UA / Accept-Language / Sec-CH-UA at the HTTP layer
  },
  webrtcMode: 'safe', // safe (hide local IP) | relay (relay only) | off (no change)
  timingPrecisionMs: 0.1, // rounding applied to performance.now / Date.now
  // sites where Nébula does nothing (e.g. your bank). eTLD+1 or full hostnames.
  allowlist: [],
  customProfile: null // filled from a preset the first time the user edits it
};

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

// Build a fully coherent profile from a seeded RNG. os is 'win' | 'mac'.
export function randomProfile(rng, os) {
  os = os || (rng() < 0.6 ? 'win' : 'mac');
  const cv = pick(rng, POOLS.chromeVersions);
  const loc = pick(rng, POOLS.locales);
  const tz = pick(rng, POOLS.timezones);
  const cores = pick(rng, POOLS.cores);
  const mem = pick(rng, POOLS.memory);

  if (os === 'mac') {
    const sc = pick(rng, POOLS.mac.screens);
    const gl = pick(rng, POOLS.mac.webgl);
    return {
      platform: 'MacIntel',
      vendor: 'Google Inc.',
      userAgent: `Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/${cv.major}.0.0.0 Safari/537.36`,
      appVersion: `5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/${cv.major}.0.0.0 Safari/537.36`,
      hardwareConcurrency: cores,
      deviceMemory: mem,
      maxTouchPoints: 0,
      language: loc.language,
      languages: loc.languages,
      screen: { width: sc.width, height: sc.height, availWidth: sc.width, availHeight: sc.availHeight, colorDepth: sc.colorDepth, pixelDepth: sc.colorDepth, dpr: sc.dpr },
      timezone: tz,
      webgl: gl,
      uaData: {
        mobile: false, platform: 'macOS', platformVersion: '14.6.1', architecture: 'arm', bitness: '64', model: '',
        uaFullVersion: cv.full,
        brands: [
          { brand: 'Chromium', version: cv.major },
          { brand: 'Not_A Brand', version: '24' },
          { brand: 'Google Chrome', version: cv.major }
        ]
      }
    };
  }

  const sc = pick(rng, POOLS.win.screens);
  const gl = pick(rng, POOLS.win.webgl);
  return {
    platform: 'Win32',
    vendor: 'Google Inc.',
    userAgent: `Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/${cv.major}.0.0.0 Safari/537.36`,
    appVersion: `5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/${cv.major}.0.0.0 Safari/537.36`,
    hardwareConcurrency: cores,
    deviceMemory: mem,
    maxTouchPoints: 0,
    language: loc.language,
    languages: loc.languages,
    screen: { width: sc.width, height: sc.height, availWidth: sc.width, availHeight: sc.availHeight, colorDepth: 24, pixelDepth: 24, dpr: 1 },
    timezone: tz,
    webgl: gl,
    uaData: {
      mobile: false, platform: 'Windows', platformVersion: '15.0.0', architecture: 'x86', bitness: '64', model: '',
      uaFullVersion: cv.full,
      brands: [
        { brand: 'Chromium', version: cv.major },
        { brand: 'Not_A Brand', version: '24' },
        { brand: 'Google Chrome', version: cv.major }
      ]
    }
  };
}

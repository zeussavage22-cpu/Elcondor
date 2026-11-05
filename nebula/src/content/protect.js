// Nébula — protection core (MAIN world, document_start).
// Installs all API hooks immediately and reads a live CFG that the ISOLATED
// bridge delivers. Each hook passes through untouched while disabled, so it is
// safe to install before we know the user's settings.
//
// Design goals:
//   * deterministic, per-origin noise (stable within a session, different per
//     site) so canvas/audio/webgl hashes cannot link you across sites
//   * internally coherent spoofed identity (matches the HTTP headers the
//     background sets)
//   * native-looking wrappers so sites cannot trivially detect the shield

(() => {
  'use strict';
  if (window.__nebulaInstalled) return;
  window.__nebulaInstalled = true;

  // Shared-DOM marker so the popup can verify (across worlds) that this MAIN-world
  // script actually ran on the page. The DOM is shared between the page and the
  // extension's isolated world, so a content/executeScript read can see it.
  function mark(state, ua) {
    try {
      const de = document.documentElement;
      if (!de) return;
      de.setAttribute('data-nebula', state);
      if (ua) de.setAttribute('data-nebula-ua', ua);
    } catch {}
  }
  mark('loaded');

  // --------------------------------------------------------------------- CFG
  let CFG = { enabled: false };

  // ------------------------------------------------------------------- PRNG
  function imul32(a, b) { return Math.imul(a, b); }
  function hash32() {
    // mix all integer args into a 32-bit unsigned hash
    let h = 2166136261 >>> 0;
    for (let i = 0; i < arguments.length; i++) {
      let x = arguments[i] | 0;
      h ^= x;
      h = imul32(h, 16777619);
      h ^= h >>> 13;
    }
    h ^= h >>> 15;
    return h >>> 0;
  }
  function strHash(str) {
    let h = 1779033703 ^ str.length;
    for (let i = 0; i < str.length; i++) {
      h = imul32(h ^ str.charCodeAt(i), 3432918353);
      h = (h << 13) | (h >>> 19);
    }
    return h >>> 0;
  }
  function mulberry32(a) {
    return function () {
      a |= 0; a = (a + 0x6d2b79f5) | 0;
      let t = Math.imul(a ^ (a >>> 15), 1 | a);
      t = (t + Math.imul(t ^ (t >>> 7), 61 | t)) ^ t;
      return ((t ^ (t >>> 14)) >>> 0) / 4294967296;
    };
  }

  let ORIGIN_SEED = (Math.random() * 0xffffffff) >>> 0; // bootstrap until CFG
  function reseed() {
    const host = CFG.host || location.hostname || 'about';
    ORIGIN_SEED = (hash32(CFG.baseSeed | 0, strHash(host)) >>> 0);
  }
  function noiseAmp() {
    return CFG.noise === 'low' ? 1 : CFG.noise === 'high' ? 4 : 2;
  }

  // ---------------------------------------------------- native-code masking
  const realFnToString = Function.prototype.toString;
  const fakeStrings = new WeakMap();

  function maskString(fn, str) { try { fakeStrings.set(fn, str); } catch {} return fn; }

  function nativeStr(name) { return 'function ' + name + '() { [native code] }'; }

  // Wrap a function so toString() reports native code and name/length match.
  function wrap(orig, impl, name) {
    name = name || (orig && orig.name) || '';
    try {
      Object.defineProperty(impl, 'name', { value: name, configurable: true });
      if (orig) Object.defineProperty(impl, 'length', { value: orig.length, configurable: true });
    } catch {}
    maskString(impl, nativeStr(name));
    return impl;
  }

  // Install the single toString override that consults our table.
  (function installToStringMask() {
    const patched = wrap(realFnToString, function toString() {
      if (fakeStrings.has(this)) return fakeStrings.get(this);
      return realFnToString.call(this);
    }, 'toString');
    try {
      Object.defineProperty(Function.prototype, 'toString', {
        value: patched, configurable: true, writable: true
      });
    } catch {}
  })();

  function defineGetter(target, prop, getter, label) {
    if (!target) return;
    let desc;
    try { desc = Object.getOwnPropertyDescriptor(target, prop); } catch {}
    const g = wrap(null, getter, 'get ' + prop);
    maskString(g, 'function get ' + prop + '() { [native code] }');
    try {
      Object.defineProperty(target, prop, {
        configurable: true,
        enumerable: desc ? desc.enumerable : true,
        get: g,
        set: desc && desc.set ? desc.set : undefined
      });
    } catch {}
  }

  function on(cat) { return CFG.enabled && CFG.protections && CFG.protections[cat]; }

  // ====================================================================== //
  //  Canvas 2D                                                             //
  // ====================================================================== //
  (function canvas() {
    const C2D = window.CanvasRenderingContext2D && CanvasRenderingContext2D.prototype;
    const HC = window.HTMLCanvasElement && HTMLCanvasElement.prototype;
    if (!C2D || !HC) return;

    const origGetImageData = C2D.getImageData;
    const origToDataURL = HC.toDataURL;
    const origToBlob = HC.toBlob;

    function perturb(data, w, h) {
      const amp = noiseAmp();
      const seed = ORIGIN_SEED;
      const n = data.length;
      // cap heavy work on very large surfaces (real content, not fingerprints)
      const step = w * h > 2000000 ? 8 : 1;
      for (let i = 0; i < n; i += 4 * step) {
        const px = (i >> 2);
        const d = hash32(seed, px);
        data[i] = clamp(data[i] + ((d & 7) - amp));
        data[i + 1] = clamp(data[i + 1] + (((d >> 3) & 7) - amp));
        data[i + 2] = clamp(data[i + 2] + (((d >> 6) & 7) - amp));
      }
    }
    function clamp(v) { return v < 0 ? 0 : v > 255 ? 255 : v; }

    C2D.getImageData = wrap(origGetImageData, function getImageData(x, y, w, h, settings) {
      const img = origGetImageData.call(this, x, y, w, h, settings);
      if (on('canvas')) { try { perturb(img.data, img.width, img.height); } catch {} }
      return img;
    }, 'getImageData');

    function noisyCopy(canvas) {
      const w = canvas.width, h = canvas.height;
      const copy = document.createElement('canvas');
      copy.width = w; copy.height = h;
      const ctx = copy.getContext('2d');
      ctx.drawImage(canvas, 0, 0);
      const img = origGetImageData.call(ctx, 0, 0, w, h);
      perturb(img.data, w, h);
      ctx.putImageData(img, 0, 0);
      return copy;
    }

    HC.toDataURL = wrap(origToDataURL, function toDataURL(type, quality) {
      if (on('canvas') && this.width && this.height) {
        try { return origToDataURL.call(noisyCopy(this), type, quality); } catch {}
      }
      return origToDataURL.call(this, type, quality);
    }, 'toDataURL');

    if (origToBlob) {
      HC.toBlob = wrap(origToBlob, function toBlob(cb, type, quality) {
        if (on('canvas') && this.width && this.height) {
          try { return origToBlob.call(noisyCopy(this), cb, type, quality); } catch {}
        }
        return origToBlob.call(this, cb, type, quality);
      }, 'toBlob');
    }

    // measureText jitter (also used for font fingerprinting)
    const origMeasure = C2D.measureText;
    C2D.measureText = wrap(origMeasure, function measureText(text) {
      const m = origMeasure.call(this, text);
      if (!on('fonts')) return m;
      const j = ((hash32(ORIGIN_SEED, strHash(String(text) + '|' + this.font)) & 1023) / 1023 - 0.5) * 0.02;
      return new Proxy(m, {
        get(t, p) {
          if (p === 'width') return t.width + j;
          const v = t[p];
          return typeof v === 'function' ? v.bind(t) : v;
        }
      });
    }, 'measureText');
  })();

  // ====================================================================== //
  //  WebGL / WebGL2                                                        //
  // ====================================================================== //
  (function webgl() {
    const protos = [];
    if (window.WebGLRenderingContext) protos.push(WebGLRenderingContext.prototype);
    if (window.WebGL2RenderingContext) protos.push(WebGL2RenderingContext.prototype);
    const UNMASKED_VENDOR = 37445, UNMASKED_RENDERER = 37446;

    for (const proto of protos) {
      const origGetParameter = proto.getParameter;
      proto.getParameter = wrap(origGetParameter, function getParameter(p) {
        if (on('webgl') && CFG.profile && CFG.profile.webgl) {
          if (p === UNMASKED_VENDOR) return CFG.profile.webgl.vendor;
          if (p === UNMASKED_RENDERER) return CFG.profile.webgl.renderer;
        }
        return origGetParameter.call(this, p);
      }, 'getParameter');

      const origReadPixels = proto.readPixels;
      proto.readPixels = wrap(origReadPixels, function readPixels(x, y, w, h, fmt, type, pixels) {
        const r = origReadPixels.call(this, x, y, w, h, fmt, type, pixels);
        if (on('webgl') && pixels && pixels.length && pixels.BYTES_PER_ELEMENT === 1 && w * h < 500000) {
          const amp = noiseAmp();
          for (let i = 0; i < pixels.length; i += 4) {
            const d = hash32(ORIGIN_SEED, i);
            pixels[i] = clampB(pixels[i] + ((d & 3) - (amp >> 1)));
          }
        }
        return r;
      }, 'readPixels');
    }
    function clampB(v) { return v < 0 ? 0 : v > 255 ? 255 : v; }
  })();

  // ====================================================================== //
  //  Web Audio                                                             //
  // ====================================================================== //
  (function audio() {
    const amp = 1e-7;
    function noiseArr(arr, floats) {
      const seed = ORIGIN_SEED;
      for (let i = 0; i < arr.length; i += 1) {
        const d = hash32(seed, i) / 4294967296 - 0.5;
        if (floats) arr[i] = arr[i] + d * amp * 100;
      }
    }
    if (window.AudioBuffer) {
      const AB = AudioBuffer.prototype;
      const origGet = AB.getChannelData;
      AB.getChannelData = wrap(origGet, function getChannelData(ch) {
        const data = origGet.call(this, ch);
        if (on('audio')) { try { noiseArr(data, true); } catch {} }
        return data;
      }, 'getChannelData');

      const origCopy = AB.copyFromChannel;
      if (origCopy) {
        AB.copyFromChannel = wrap(origCopy, function copyFromChannel(dest, ch, start) {
          origCopy.call(this, dest, ch, start);
          if (on('audio')) { try { noiseArr(dest, true); } catch {} }
        }, 'copyFromChannel');
      }
    }
    if (window.AnalyserNode) {
      const AN = AnalyserNode.prototype;
      const oF = AN.getFloatFrequencyData;
      AN.getFloatFrequencyData = wrap(oF, function getFloatFrequencyData(a) {
        oF.call(this, a);
        if (on('audio')) { try { noiseArr(a, true); } catch {} }
      }, 'getFloatFrequencyData');
    }
  })();

  // ====================================================================== //
  //  getBoundingClientRect noise (font + layout fingerprinting)            //
  // ====================================================================== //
  (function rects() {
    function jitter(rect) {
      const base = Math.round((rect.x + rect.y + rect.width + rect.height) * 100);
      const d = hash32(ORIGIN_SEED, base) / 4294967296 - 0.5;
      const o = d * 0.0005;
      return new DOMRect(rect.x + o, rect.y + o, rect.width + o, rect.height + o);
    }
    if (window.Element) {
      const E = Element.prototype;
      const orig = E.getBoundingClientRect;
      E.getBoundingClientRect = wrap(orig, function getBoundingClientRect() {
        const r = orig.call(this);
        return on('clientRects') ? jitter(r) : r;
      }, 'getBoundingClientRect');
    }
    if (window.Range) {
      const R = Range.prototype;
      const orig = R.getBoundingClientRect;
      R.getBoundingClientRect = wrap(orig, function getBoundingClientRect() {
        const r = orig.call(this);
        return on('clientRects') ? jitter(r) : r;
      }, 'getBoundingClientRect');
    }
  })();

  // ====================================================================== //
  //  navigator identity                                                    //
  // ====================================================================== //
  (function nav() {
    const NP = window.Navigator && Navigator.prototype;
    if (!NP) return;
    const P = () => CFG.profile || {};

    const map = {
      userAgent: ['navigator', () => P().userAgent],
      appVersion: ['navigator', () => P().appVersion],
      platform: ['navigator', () => P().platform],
      vendor: ['navigator', () => P().vendor],
      language: ['locale', () => P().language],
      languages: ['locale', () => Object.freeze((P().languages || []).slice())],
      hardwareConcurrency: ['hardware', () => P().hardwareConcurrency],
      deviceMemory: ['hardware', () => P().deviceMemory],
      maxTouchPoints: ['hardware', () => P().maxTouchPoints]
    };
    for (const prop in map) {
      const [cat, getVal] = map[prop];
      const real = navigator[prop];
      defineGetter(NP, prop, function () {
        return on(cat) && getVal() !== undefined ? getVal() : real;
      });
    }

    if ('webdriver' in NP || 'webdriver' in navigator) {
      defineGetter(NP, 'webdriver', function () { return on('webdriver') ? false : undefined; });
    }

    // navigator.connection
    defineGetter(NP, 'connection', (function () {
      const real = navigator.connection;
      const fake = { effectiveType: '4g', rtt: 50, downlink: 10, saveData: false, type: 'wifi',
        onchange: null, addEventListener() {}, removeEventListener() {}, dispatchEvent() { return false; } };
      return function () { return on('connection') ? fake : real; };
    })());
  })();

  // ====================================================================== //
  //  navigator.userAgentData (Client Hints)                                //
  // ====================================================================== //
  (function uaData() {
    const NP = window.Navigator && Navigator.prototype;
    if (!NP) return;
    const real = navigator.userAgentData;
    defineGetter(NP, 'userAgentData', function () {
      if (!on('uaData') || !CFG.profile) return real;
      // Explicit null => a non-Chromium profile (Safari): hide Client Hints.
      if (CFG.profile.uaData === null) return undefined;
      if (!CFG.profile.uaData) return real;
      const u = CFG.profile.uaData;
      const lowEntropy = { brands: u.brands.map((b) => ({ brand: b.brand, version: b.version })), mobile: u.mobile, platform: u.platform };
      return {
        ...lowEntropy,
        getHighEntropyValues: wrap(null, function getHighEntropyValues(hints) {
          const out = { ...lowEntropy };
          (hints || []).forEach((h) => {
            if (h === 'platformVersion') out.platformVersion = u.platformVersion;
            else if (h === 'architecture') out.architecture = u.architecture;
            else if (h === 'bitness') out.bitness = u.bitness;
            else if (h === 'model') out.model = u.model;
            else if (h === 'uaFullVersion') out.uaFullVersion = u.uaFullVersion;
            else if (h === 'fullVersionList') out.fullVersionList = u.brands.map((b) => ({ brand: b.brand, version: b.brand === 'Not_A Brand' ? b.version + '.0.0.0' : u.uaFullVersion }));
          });
          return Promise.resolve(out);
        }, 'getHighEntropyValues'),
        toJSON: wrap(null, function toJSON() { return lowEntropy; }, 'toJSON')
      };
    });
  })();

  // ====================================================================== //
  //  plugins / mimeTypes  (match a real Chrome install)                    //
  // ====================================================================== //
  (function plugins() {
    const NP = window.Navigator && Navigator.prototype;
    if (!NP) return;
    const names = ['PDF Viewer', 'Chrome PDF Viewer', 'Chromium PDF Viewer', 'Microsoft Edge PDF Viewer', 'WebKit built-in PDF'];
    let cachedPlugins = null, cachedMimes = null;
    function build() {
      if (cachedPlugins) return;
      const mimePDF = { type: 'application/pdf', suffixes: 'pdf', description: 'Portable Document Format' };
      const mimeText = { type: 'text/pdf', suffixes: 'pdf', description: 'Portable Document Format' };
      const plugins = names.map((n) => ({ name: n, filename: 'internal-pdf-viewer', description: 'Portable Document Format', length: 2, 0: mimePDF, 1: mimeText,
        item(i) { return this[i] || null; }, namedItem(t) { return t === mimePDF.type ? mimePDF : t === mimeText.type ? mimeText : null; } }));
      plugins.forEach((p) => { try { Object.setPrototypeOf(p, window.Plugin ? Plugin.prototype : Object.prototype); } catch {} });
      const arr = plugins.slice();
      arr.item = function (i) { return this[i] || null; };
      arr.namedItem = function (n) { return plugins.find((p) => p.name === n) || null; };
      arr.refresh = function () {};
      try { Object.setPrototypeOf(arr, window.PluginArray ? PluginArray.prototype : Object.prototype); } catch {}
      cachedPlugins = arr;
      const mimes = [mimePDF, mimeText];
      mimes.item = function (i) { return this[i] || null; };
      mimes.namedItem = function (n) { return mimes.find((m) => m.type === n) || null; };
      cachedMimes = mimes;
    }
    const realP = navigator.plugins, realM = navigator.mimeTypes;
    defineGetter(NP, 'plugins', function () { if (!on('plugins')) return realP; build(); return cachedPlugins; });
    defineGetter(NP, 'mimeTypes', function () { if (!on('plugins')) return realM; build(); return cachedMimes; });
  })();

  // ====================================================================== //
  //  screen / devicePixelRatio                                             //
  // ====================================================================== //
  (function screenSpoof() {
    const SP = window.Screen && Screen.prototype;
    const S = () => (CFG.profile && CFG.profile.screen) || {};
    if (SP) {
      const defs = {
        width: () => S().width, height: () => S().height,
        availWidth: () => S().availWidth, availHeight: () => S().availHeight,
        colorDepth: () => S().colorDepth, pixelDepth: () => S().pixelDepth
      };
      for (const prop in defs) {
        const getVal = defs[prop];
        const real = screen[prop];
        defineGetter(SP, prop, function () { return on('screen') && getVal() !== undefined ? getVal() : real; });
      }
    }
    const realDpr = window.devicePixelRatio;
    defineGetter(window, 'devicePixelRatio', function () {
      return on('screen') && S().dpr !== undefined ? S().dpr : realDpr;
    });
  })();

  // ====================================================================== //
  //  timezone + locale                                                     //
  // ====================================================================== //
  (function timezone() {
    const RealDTF = window.Intl && Intl.DateTimeFormat;
    function offsetFor(tz, date) {
      try {
        const dtf = new RealDTF('en-US', { timeZone: tz, hour12: false, year: 'numeric', month: '2-digit', day: '2-digit', hour: '2-digit', minute: '2-digit', second: '2-digit' });
        const parts = {};
        for (const p of dtf.formatToParts(date)) parts[p.type] = p.value;
        const asUTC = Date.UTC(+parts.year, +parts.month - 1, +parts.day, +parts.hour, +parts.minute, +parts.second);
        return Math.round((date.getTime() - asUTC) / 60000);
      } catch { return date.getTimezoneOffset(); }
    }

    const origGetTZO = Date.prototype.getTimezoneOffset;
    Date.prototype.getTimezoneOffset = wrap(origGetTZO, function getTimezoneOffset() {
      if (on('timezone') && CFG.profile && CFG.profile.timezone) return offsetFor(CFG.profile.timezone, this);
      return origGetTZO.call(this);
    }, 'getTimezoneOffset');

    if (RealDTF) {
      const patched = function DateTimeFormat(locales, options) {
        options = options || {};
        if (on('timezone') && CFG.profile && CFG.profile.timezone && !options.timeZone) options = { ...options, timeZone: CFG.profile.timezone };
        if (on('locale') && CFG.profile && CFG.profile.language && locales === undefined) locales = CFG.profile.language;
        const inst = new RealDTF(locales, options);
        const origResolved = inst.resolvedOptions.bind(inst);
        inst.resolvedOptions = wrap(null, function resolvedOptions() {
          const r = origResolved();
          if (on('timezone') && CFG.profile && CFG.profile.timezone) r.timeZone = CFG.profile.timezone;
          if (on('locale') && CFG.profile && CFG.profile.language) r.locale = CFG.profile.language;
          return r;
        }, 'resolvedOptions');
        return inst;
      };
      patched.prototype = RealDTF.prototype;
      patched.supportedLocalesOf = RealDTF.supportedLocalesOf.bind(RealDTF);
      wrap(RealDTF, patched, 'DateTimeFormat');
      try { Intl.DateTimeFormat = patched; } catch {}
    }
  })();

  // ====================================================================== //
  //  battery / mediaDevices / speech voices                                //
  // ====================================================================== //
  (function misc() {
    const NP = window.Navigator && Navigator.prototype;
    if (NP && 'getBattery' in NP) {
      const orig = NP.getBattery;
      NP.getBattery = wrap(orig, function getBattery() {
        if (!on('battery')) return orig.call(this);
        return Promise.resolve({ charging: true, chargingTime: 0, dischargingTime: Infinity, level: 1,
          addEventListener() {}, removeEventListener() {}, dispatchEvent() { return false; },
          onchargingchange: null, onchargingtimechange: null, ondischargingtimechange: null, onlevelchange: null });
      }, 'getBattery');
    }

    if (window.MediaDevices && MediaDevices.prototype.enumerateDevices) {
      const orig = MediaDevices.prototype.enumerateDevices;
      MediaDevices.prototype.enumerateDevices = wrap(orig, function enumerateDevices() {
        if (!on('mediaDevices')) return orig.call(this);
        const mk = (kind, id) => ({ deviceId: id, kind, label: '', groupId: '', toJSON() { return this; } });
        return Promise.resolve([mk('audioinput', 'default'), mk('videoinput', ''), mk('audiooutput', 'default')]);
      }, 'enumerateDevices');
    }

    if (window.speechSynthesis && typeof speechSynthesis.getVoices === 'function') {
      const orig = speechSynthesis.getVoices.bind(speechSynthesis);
      const fake = [
        { voiceURI: 'Google US English', name: 'Google US English', lang: 'en-US', localService: false, default: true },
        { voiceURI: 'Google UK English Female', name: 'Google UK English Female', lang: 'en-GB', localService: false, default: false }
      ];
      speechSynthesis.getVoices = wrap(speechSynthesis.getVoices, function getVoices() {
        return on('speech') ? fake.slice() : orig();
      }, 'getVoices');
    }
  })();

  // ====================================================================== //
  //  timing precision                                                      //
  // ====================================================================== //
  (function timing() {
    if (window.performance && performance.now) {
      const orig = performance.now.bind(performance);
      performance.now = wrap(performance.now, function now() {
        const t = orig();
        if (!on('timing')) return t;
        const p = CFG.timingPrecisionMs || 0.1;
        return Math.floor(t / p) * p;
      }, 'now');
    }
  })();

  // ====================================================================== //
  //  WebRTC local-IP leak protection                                       //
  // ====================================================================== //
  (function webrtc() {
    const RTC = window.RTCPeerConnection || window.webkitRTCPeerConnection;
    if (!RTC) return;

    function mode() { return on('webrtc') ? (CFG.webrtcMode || 'safe') : 'off'; }
    function isLocalHostCandidate(c) {
      if (!c || !c.candidate) return false;
      const s = c.candidate;
      if (/ \.local /.test(s) || /\.local /.test(s)) return false; // mDNS, already private
      return / typ host /.test(s) && /(10\.|192\.168\.|172\.(1[6-9]|2\d|3[01])\.|169\.254\.)/.test(s);
    }

    const Patched = function RTCPeerConnection(config, ...rest) {
      const m = mode();
      if (m === 'relay' && config) config = { ...config, iceTransportPolicy: 'relay' };
      const pc = new RTC(config, ...rest);
      if (m === 'off') return pc;

      const realAdd = pc.addEventListener.bind(pc);
      pc.addEventListener = function (type, listener, opts) {
        if (type === 'icecandidate' && typeof listener === 'function') {
          const wrapped = function (ev) {
            if (ev && ev.candidate && isLocalHostCandidate(ev.candidate)) return; // drop local IP leak
            return listener.call(this, ev);
          };
          return realAdd(type, wrapped, opts);
        }
        return realAdd(type, listener, opts);
      };
      let userOnIce = null;
      Object.defineProperty(pc, 'onicecandidate', {
        configurable: true,
        get() { return userOnIce; },
        set(fn) {
          userOnIce = fn;
          pc.addEventListener('icecandidate', function (ev) { if (userOnIce) userOnIce.call(pc, ev); });
        }
      });
      return pc;
    };
    Patched.prototype = RTC.prototype;
    try { Patched.generateCertificate = RTC.generateCertificate && RTC.generateCertificate.bind(RTC); } catch {}
    wrap(RTC, Patched, 'RTCPeerConnection');
    try { window.RTCPeerConnection = Patched; } catch {}
    try { if (window.webkitRTCPeerConnection) window.webkitRTCPeerConnection = Patched; } catch {}
  })();

  // ====================================================================== //
  //  config intake                                                         //
  // ====================================================================== //
  window.addEventListener('nebula:config', function (ev) {
    try {
      const data = JSON.parse(ev.detail);
      CFG = data && data.enabled ? data : { enabled: false };
      if (CFG.enabled) reseed();
      mark(CFG.enabled ? 'active' : 'off', CFG.enabled && CFG.profile ? CFG.profile.userAgent : '');
    } catch {}
  }, true);

  // Ask the bridge for config (it may have loaded after us).
  try { window.dispatchEvent(new CustomEvent('nebula:ready')); } catch {}
})();

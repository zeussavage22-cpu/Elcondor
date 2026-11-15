// =====================================================================
// [MEJORA] Utilidades añadidas en la versión mejorada (ver MEJORAS.md)
// =====================================================================
function ui_lang() {
  let l = ((self.navigator && navigator.language) || "en").toLowerCase();
  return l.startsWith("es") ? "es" : l.startsWith("zh") ? "zh" : "en";
}
// Devuelve el texto en español, chino o inglés según el idioma del navegador.
function ui_t(es, zh, en) {
  let l = ui_lang();
  return l === "es" ? es : l === "zh" ? zh : en;
}
// Normaliza un código de idioma ("ja-jp" -> "ja-JP") y lo expande como lo
// haría un navegador real: "es-ES" -> "es-ES,es;q=0.9".
function langList(l) {
  if (!l) return l;
  l = (l + "").trim();
  if (l.includes(",")) return l;
  l = l.replace(
    /^([a-z]{2,3})[-_]([a-z]{2})$/i,
    (m, a, b) => a.toLowerCase() + "-" + b.toUpperCase(),
  );
  let b = l.split("-")[0];
  return b && b !== l ? `${l},${b};q=0.9` : l;
}
// Normaliza el nombre de sistema operativo de ua-parser a las claves internas.
function osKey(n = "") {
  n = (n + "").toLowerCase();
  if (!n) return "";
  if (n.includes("win")) return "windows";
  if (n.includes("ios") || n.includes("iphone") || n.includes("ipad"))
    return "ios";
  if (n.includes("mac")) return "macos";
  if (n.includes("android")) return "android";
  return "linux";
}
// Navegadores que existen de verdad en cada sistema (evita "Safari en Windows").
const REAL_BROWSERS = {
  windows: ["Chrome", "Chrome", "Edge", "Firefox"],
  macos: ["Chrome", "Safari", "Safari", "Firefox", "Edge"],
  linux: ["Chrome", "Chrome", "Firefox"],
  android: ["Chrome", "Chrome", "Chrome", "Firefox"],
  ios: ["Safari", "Safari", "Chrome"],
};
// Genera un User-Agent moderno y bien formado (formato "UA reducido" que usan
// Chrome/Edge actuales), con versiones cercanas a la del navegador real.
function realUA(os, browser, type, t) {
  let realMajor =
      +((navigator.userAgent.match(/Chrome\/(\d+)/) || [])[1] || 0) || 142,
    cMaj = realMajor - t.int(0, 3),
    fx = realMajor + 3 - t.int(0, 3),
    sfx = t.item(["17.6", "18.3", "18.5", "18.6", "26.0", "26.1"]),
    iosV = sfx.startsWith("26") ? "18_6" : sfx.replace(".", "_"),
    mobile = type === "mobile";
  os = osKey(os) || "windows";
  let list = REAL_BROWSERS[os] || REAL_BROWSERS.windows,
    b = (browser || "").toLowerCase();
  b = list.find((x) => x.toLowerCase() === b) || t.item(list);
  const plat = {
    windows: "Windows NT 10.0; Win64; x64",
    macos: "Macintosh; Intel Mac OS X 10_15_7",
    linux: "X11; Linux x86_64",
    android: "Linux; Android 10; K",
    ios:
      type === "tablet"
        ? `iPad; CPU OS ${iosV} like Mac OS X`
        : `iPhone; CPU iPhone OS ${iosV} like Mac OS X`,
  }[os];
  if (os === "ios") {
    let tail =
      b === "Chrome"
        ? `CriOS/${cMaj}.0.0.0 Mobile/15E148 Safari/604.1`
        : `Version/${sfx} Mobile/15E148 Safari/604.1`;
    return `Mozilla/5.0 (${plat}) AppleWebKit/605.1.15 (KHTML, like Gecko) ${tail}`;
  }
  if (b === "Firefox") {
    if (os === "android")
      return `Mozilla/5.0 (Android 14; ${mobile ? "Mobile" : "Tablet"}; rv:${fx}.0) Gecko/${fx}.0 Firefox/${fx}.0`;
    let fp = { macos: "Macintosh; Intel Mac OS X 10.15" }[os] || plat;
    return `Mozilla/5.0 (${fp}; rv:${fx}.0) Gecko/20100101 Firefox/${fx}.0`;
  }
  if (b === "Safari")
    return `Mozilla/5.0 (${plat}) AppleWebKit/605.1.15 (KHTML, like Gecko) Version/${sfx} Safari/605.1.15`;
  let ua = `Mozilla/5.0 (${plat}) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/${cMaj}.0.0.0 ${mobile ? "Mobile " : ""}Safari/537.36`;
  return b === "Edge"
    ? ua + (os === "android" ? ` EdgA/${cMaj}.0.0.0` : ` Edg/${cMaj}.0.0.0`)
    : ua;
}
// Valores de hardware/pantalla plausibles según el tipo de dispositivo y SO.
function realHW(os, type, t) {
  os = osKey(os);
  let desk = type !== "mobile" && type !== "tablet";
  let depth = os === "macos" ? t.item([24, 30]) : 24;
  return {
    colorDepth: depth,
    pixelDepth: depth,
    dpr: desk
      ? os === "macos"
        ? 2
        : t.item([1, 1, 1.25, 1.5])
      : os === "ios"
        ? type === "tablet"
          ? 2
          : t.item([2, 3, 3])
        : type === "tablet"
          ? t.item([1.5, 2])
          : t.item([2, 2.625, 2.75, 3]),
    maxTouchPoints: desk ? 0 : t.item([5, 10]),
    memory: desk ? t.item([4, 8, 8]) : t.item([4, 8]),
    processors: desk
      ? os === "macos"
        ? t.item([8, 10, 12])
        : t.item([4, 6, 8, 8, 12, 16])
      : t.item([6, 8, 8]),
  };
}
function at() {
  function e() {
    return (((1 + Math.random()) * 65536) | 0).toString(16).substring(1);
  }
  return e() + e() + "-" + e() + "-" + e() + "-" + e() + "-" + e() + e() + e();
}
function At(e = "") {
  let t = 0,
    i,
    r;
  if (e.length === 0) return t;
  for (i = 0; i < e.length; i++)
    ((r = e.charCodeAt(i)), (t = (t << 5) - t + r), (t |= 0));
  return t;
}
function _t(e) {
  return /^(\d{1,2}|1\d\d|2[0-4]\d|25[0-5])\.(\d{1,2}|1\d\d|2[0-4]\d|25[0-5])\.(\d{1,2}|1\d\d|2[0-4]\d|25[0-5])\.(\d{1,2}|1\d\d|2[0-4]\d|25[0-5])$/.test(
    e,
  );
}
function Ct(e = {}) {
  const t = (p, h) => `Linux; Android ${p}; ${h}`,
    i = (p, h = "") => (
      (h = h.replace(".", "_")),
      p === "Macintosh"
        ? `Macintosh; Intel Mac OS X ${h}`
        : `${p}; CPU ${p !== "iPad" ? "iPhone " : ""}OS ${h} like Mac OS X`
    ),
    r = (p) => `Windows NT ${p}; Win64; x64`,
    u = () => "X11; Linux x86_64",
    n = (p = "", h, w) => (
      (w = w || "537.36"),
      (p = p.toLowerCase()),
      p === "safari"
        ? `AppleWebKit/${w} (KHTML, like Gecko) Version/${h} Safari/${w}`
        : p === "chrome"
          ? `AppleWebKit/${w} (KHTML, like Gecko) Chrome/${h} Safari/${w}`
          : p === "firefox"
            ? ((w = w || h), `Gecko/${w} Firefox/${h}`)
            : `AppleWebKit/${w} (KHTML, like Gecko) Chrome/${w} Safari/${w} ${p}/${h}`
    ),
    d = (p, h, w) => (
      (w = w || "534.46"),
      (p = p.toLowerCase()),
      p === "safari"
        ? `AppleWebKit/${w} (KHTML, like Gecko) Version/5.1 Mobile/9A334 Safari/${w}`
        : p === "chrome"
          ? `AppleWebKit/${w} (KHTML, like Gecko) CriOS/${h} Mobile/10B350 Safari/${w}`
          : p === "firefox"
            ? ((w = w || h), `Gecko/${w} Mobile Firefox/${h}`)
            : `AppleWebKit/${w} (KHTML, like Gecko) Chrome/${w} Mobile/10B351 Safari/${w} ${p}/${h}`
    ),
    b = (p, h, w = "") => (
      (p = p.toLowerCase()),
      p === "windows"
        ? r(h)
        : p === "linux"
          ? u()
          : p === "macos"
            ? i("Macintosh", h)
            : p === "ios"
              ? ((w = w.toLowerCase()),
                i(w === "tablet" ? "iPad" : "iPhone", h))
              : p === "android"
                ? t(h, w)
                : `${p} ${h}; ${w} `
    ),
    f = (p, h, w, g) => (
      (p = p.toLowerCase()),
      p === "windows" || p === "linux" || p === "macos"
        ? n(h, w, g)
        : p === "ios" || p === "android"
          ? d(h, w, g)
          : n(h, w, g)
    );
  let {
      osName: m = "unknown",
      deviceName: y = "unknown",
      browserName: E = "unknown",
      osVersion: o = "1.0.0",
      browserVersion: l = "1.0.0",
      engineVersion: a = "",
    } = e,
    s = b(m, o, y),
    c = f(m, E, l, a);
  return `Mozilla/5.0 (${s}) ${c}`;
}
function Ot(e = {}, t = new oe(Math.random())) {
  const i = () => t.item(["10.0", "5.1", "6.1", "6.2", "6.3"]),
    r = () => {
      let y = t.int(6, 15),
        E = t.int(0, 15),
        o = t.int(0, 9),
        l = y + "_" + E;
      return (o !== 0 && (l += "_" + o), l);
    },
    u = () => {
      let y = t.int(4, 16);
      if (y < 9) {
        let E = t.item(["0", "1", "0.0", "1.0", "1.1", "0.1", "1.2"]);
        return y + "." + E;
      }
      return y + "";
    },
    n = (y = 1, E = 10) => {
      let o = t.int(y, E),
        l = t.int(0, 10),
        a = t.int(0, 20);
      return [o, l, a].join(".");
    };
  let { osName: d, osVersion: b, browserVersion: f, browserName: m } = e;
  if (
    ((d = d.toLowerCase()),
    b ||
      (d === "windows"
        ? (b = i())
        : d === "mac" || d === "ios"
          ? (b = r())
          : d === "android"
            ? (b = u())
            : d === "linux" && (b = "")),
    !f)
  ) {
    let y = m.toLowerCase();
    y === "chrome" || y === "edge"
      ? (f = n(76, 123))
      : y === "safari"
        ? (f = n(1, 15))
        : y === "firefox"
          ? (f = n(71, 112))
          : (f = n());
  }
  return Ct({
    ...e,
    osName: d,
    osVersion: b,
    browserVersion: f,
    browserName: m,
  });
}
class oe {
  constructor(t = 0) {
    this.setSeed(t);
  }
  setSeed(t = 0) {
    (t | 0 || (t *= 1e4), (this.seed = t), (this.rand = It(t)));
  }
  int(t, i) {
    const u = this.rand() * (i - t) * 1e6;
    return (Math.abs(u | 0) % (i - t)) + t;
  }
  item(t = []) {
    let i = this.int(0, t.length);
    return t[i];
  }
  str(
    t = 16,
    i = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789",
  ) {
    let r = "";
    const u = i.length;
    for (let n = 0; n < t; n++) {
      const d = this.int(0, u);
      r += i.charAt(d);
    }
    return r;
  }
}
function It(e = 0) {
  return function () {
    let t = (e += 1831565813);
    return (
      (t = Math.imul(t ^ (t >>> 15), t | 1)),
      (t ^= t + Math.imul(t ^ (t >>> 7), t | 61)),
      ((t ^ (t >>> 14)) >>> 0) / 4294967296
    );
  };
}
let Ae = new Date(),
  Rt = Ae.getFullYear() + ":" + Ae.getMonth() + ":" + Ae.getDate();
function Ze(e, t = 0) {
  if (((e === "SCOPE_CHEATER" || e === "SCOPE_BROWSER") && (t = Rt), !t))
    return e;
  let i = At(e + t);
  return new oe(i).str(32);
}
const W = self.chrome || self.browser,
  Q = {
    languages: void 0,
    regions: void 0,
    devices: void 0,
    setting: void 0,
    webglInfos: void 0,
    defaultConfig: void 0,
  };
async function Mt() {
  if (!(W != null && W.runtime)) return;
  const e = {
      languages: W.runtime.getURL("languages.json"),
      regions: W.runtime.getURL("regions.json"),
      devices: W.runtime.getURL("devices.json"),
      webglInfos: W.runtime.getURL("webglInfos.json"),
      defaultConfig: W.runtime.getURL("defaultConfig.json"),
    },
    t = Object.entries(e).map(([i, r]) =>
      fetch(r, { method: "get" }).then(async (u) => {
        Q[i] = await u.json();
      }),
    );
  (await Promise.all(t), console.log("configs loaded", Q));
}
const rt = (self.chrome || {}).storage
  ? [
      ["canvas", "audio", "webgl", "webgpu"],
      ["voice", "plugins", "fonts", "clientRect"],
    ]
  : [
      ["canvas", "audio", "webgpu", "webgl", "voice"],
      ["clientRect", "plugins", "fonts", "ja3", "h2"],
    ];
[
  { label: "Canvas(绘图)", key: "canvas" },
  { label: "Audio(声音)", key: "audio" },
  { label: "Webgl(3D绘图)", key: "webgl" },
  { label: "WebGPU(GPU)", key: "webgpu" },
  { label: "Plugins(插件)", key: "plugins" },
  { label: "Fonts(字体)", key: "fonts" },
  { label: "ClientRect(视口)", key: "clientRect" },
  { label: "Voice(语音)", key: "voice" },
  { label: "Ja3(Tls)", key: "ja3" },
  { label: "h2(Http2)", key: "h2" },
].filter((e) => rt.flat().includes(e.key));
const Bt = [
  "audio",
  "canvas",
  "webgl",
  "fonts",
  "webgpu",
  "clientRect",
  "voice",
  "plugin",
  "native",
  "webrtc",
  "date",
  "screen",
  "location",
  "navigator",
  "feature",
  "media",
  "worker",
  "iframe",
  "trace",
  "driver",
].sort();
// [MEJORA] Se eliminó la "fecha de caducidad" oculta (31/12/2029) que dejaba
// la extensión sin poder leer/guardar configuración a partir de esa fecha.
const Re = 1 / 0,
  ae = location.host.startsWith("localhost") ? "dev" : "",
  _e = self.chrome || self.browser;
class Le {
  constructor(t, i = !1) {
    ((this.name = t),
      (this.indexKey = "__index__"),
      (this.isSession = i),
      (this.storage = i
        ? _e.storage.session
        : ae === "dev"
          ? {}
          : _e.storage.local || _e.storage.sync));
  }
  keys() {
    return this.get(this.indexKey).then(
      (t) => ((t = new Set(t || [])), t.delete(this.indexKey), t),
    );
  }
  get(t) {
    return new Promise(async (i, r) => {
      if (!t || new Date() > Re) return i();
      if (((t = this.name + "-" + t), ae === "dev")) {
        let b =
            (this.isSession ? sessionStorage : localStorage).getItem(t) || "",
          f = JSON.parse(b);
        return i(f);
      }
      let d = (await this.storage.get([t]))[t];
      i(d);
    });
  }
  set(t, i) {
    return new Promise((r, u) => {
      if (!t || new Date() > Re) return r(!1);
      let d = this.name + "-" + t;
      if (ae === "dev")
        return (
          (this.isSession ? sessionStorage : localStorage).setItem(
            d,
            JSON.stringify(i),
          ),
          r(!0)
        );
      t !== this.indexKey &&
        this.keys().then(
          (f) => ((f || new Set()).add(t), this.set(this.indexKey, [...f])),
        );
      let b = {};
      return (
        (b[d] = typeof i > "u" ? "" : i),
        this.storage.set(b, () => r(!0))
      );
    });
  }
  remove(t) {
    return new Promise(async (i, r) => {
      if (!t) return i();
      let u = this.name + "-" + t;
      return ae === "dev"
        ? ((this.isSession ? sessionStorage : localStorage).removeItem(u), i())
        : (await this.keys().then(
            (n) => (
              (n || new Set()).delete(t),
              this.set(this.indexKey, [...n])
            ),
          ),
          this.storage.remove(u, i));
    });
  }
  clear() {
    return new Promise(async (t, i) => {
      let r = await this.keys();
      if (ae === "dev") {
        for (let n of r)
          (this.isSession ? sessionStorage : localStorage).removeItem(
            this.name + "-" + n,
          );
        return t();
      }
      let u = [this.name + "-" + this.indexKey];
      return (
        r.forEach((n) => {
          u.push(this.name + "-" + n);
        }),
        this.storage.remove(u).then(t)
      );
    });
  }
  iterWith(t, i) {
    return this.keys().then(async (r) => {
      for (let u of r) {
        if (!(u + "").startsWith(t)) continue;
        if ((await this.get(u).then((d) => i(u, d))) === !1) return;
      }
    });
  }
}
const H = new Le("browser"),
  R = new Le("config"),
  be = new Le("scope", !1);
self.browserTree = H;
self.configTree = R;
self.scopeTree = be;
const Qe = self.chrome || self.browser;
async function Ve(e = []) {
  if (e.length === 0)
    return await Qe.declarativeNetRequest.updateDynamicRules(
      { removeRuleIds: [10], addRules: [] },
      () => {
        console.log("请求头动态修改规则 清空成功", e);
      },
    );
  if (!e.length) return;
  let i = {},
    r = await me();
  if (r) {
    let d = await X();
    (r === "white" && (i.initiatorDomains = d.white),
      r === "black" && (i.excludedInitiatorDomains = d.black));
  }
  let u = {
      urlFilter: "|http",
      resourceTypes: [
        "main_frame",
        "sub_frame",
        "stylesheet",
        "script",
        "image",
        "font",
        "object",
        "xmlhttprequest",
        "ping",
        "csp_report",
        "media",
        "websocket",
        "other",
      ],
      ...i,
    },
    n = [
      {
        id: 10,
        priority: 1,
        action: { type: "modifyHeaders", requestHeaders: e },
        condition: u,
      },
    ];
  return await Qe.declarativeNetRequest.updateDynamicRules(
    { removeRuleIds: [10], addRules: n },
    () => {
      console.log(" 请求头动态修改规则 添加成功", e);
    },
  );
}
const et = {
  Chrome: "Google",
  Edge: "Microsoft",
  IE: "Microsoft",
  Firefox: "Mozilla",
  Safari: "Apple",
  QQ: "Tencent",
};
async function ot(e) {
  var f, m, y, E, o, l, a;
  if (!e) {
    await Ve([]);
    return;
  }
  let t = new oe(e.seed),
    i = e.uaInfo,
    r = e.userAgent || "",
    u = langList(e.language),
    n = [],
    d = [];
  (d.push(
    "Sec-Ch-Ua-Form-Factors",
    "Sec-Ch-Ua-Full-Version",
    "Sec-Ch-Ua-Full-Version-List",
  ),
    u && n.push({ header: "Accept-Language", operation: "set", value: u }));
  e: if (r) {
    let M = function (T) {
      var A, L, $;
      let D = e.uaInfo;
      if (!D) return T;
      switch (
        (L = (A = D.os) == null ? void 0 : A.name) == null
          ? void 0
          : L.toString().toLowerCase()
      ) {
        case "ios":
          return D.device.type === "mobile" ? "iPhone" : "iPad";
        case "windows":
          if (T != null && T.toLowerCase().includes("win")) return T;
      }
      return (($ = D.os) == null ? void 0 : $.name) || T;
    };
    var b = M;
    if (
      (r && n.push({ header: "User-Agent", operation: "set", value: r }),
      r.startsWith(navigator.userAgent) || !i)
    )
      break e;
    if (r.toLowerCase().includes("firefox")) {
      d.push(
        "Sec-Ch-Ua-Platform",
        "Sec-Ch-Ua-Mobile",
        "Sec-Ch-Ua-Arch",
        "Sec-Ch-Ua-Bitness",
        "Sec-Ch-Ua",
      );
      break e;
    }
    let s = "",
      c,
      p = "",
      h,
      w,
      g;
    if (
      ((s = M(
        navigator.platform ||
          ((f = navigator.userAgentData) == null ? void 0 : f.platform),
      )),
      (c = i.device.type === "mobile"),
      navigator && navigator.userAgentData && navigator.userAgentData.brands)
    ) {
      let T = [...navigator.userAgentData.brands],
        D = i.browser || i.product,
        O = D.name || "";
      for (let A in et)
        if (O.toUpperCase().includes(A.toUpperCase())) {
          let L = [
            { brand: et[A] + " " + O, version: D.major },
            { brand: "Not=A?Brand", version: "8" },
          ];
          (T[2] && L.push({ brand: T[2].brand, version: D.major }), (T = L));
          break;
        }
      p = T.map((A) => `"${A.brand}";v="${A.version}"`).join(", ");
    }
    if (
      ((i.cpu || i.os) &&
        ((
          ((y = (m = i.os) == null ? void 0 : m.name) == null
            ? void 0
            : y.toLowerCase()) || ""
        ).includes("win")
          ? (h = t.item(["x86", "x64"]))
          : (h = (E = i.cpu) == null ? void 0 : E.architecture)),
      (w = (o = e.arch) != null && o.toString().includes("32") ? "32" : "64"),
      i.os)
    ) {
      let T =
        ((a = (l = i.os) == null ? void 0 : l.version) == null
          ? void 0
          : a.split(".")) || [];
      (T.length > 3
        ? (T = T.slice(0, 3))
        : T.length < 3 &&
          (T.length === 1 ? T.push("0.0") : T.length === 2 && T.push("0")),
        (g = /^\d+(\.\d+)*$/.test(T.join(".")) ? T.join(".") : ""));
    }
    (s &&
      n.push({
        header: "Sec-Ch-Ua-Platform",
        operation: "set",
        value: '"' + s + '"',
      }),
      g &&
        n.push({
          header: "Sec-Ch-Ua-Platform-Version",
          operation: "set",
          value: '"' + g + '"',
        }),
      c !== void 0 &&
        n.push({
          header: "Sec-Ch-Ua-Mobile",
          operation: "set",
          value: c ? "?1" : "?0",
        }),
      p && n.push({ header: "Sec-Ch-Ua", operation: "set", value: p }),
      h &&
        n.push({
          header: "Sec-Ch-Ua-Arch",
          operation: "set",
          value: '"' + h + '"',
        }),
      w &&
        n.push({
          header: "Sec-Ch-Ua-Bitness",
          operation: "set",
          value: '"' + w + '"',
        }));
  }
  (d.forEach((s) => {
    n.push({ header: s, operation: "remove" });
  }),
    n.length && (await Ve(n)));
}
const nt = self.chrome || self.browser;
async function he(e) {
  (await ei(e), await ot(e), await Lt(e));
}
async function st() {
  (await ht(), await ot(void 0), await lt());
}
async function Lt(e) {
  if (new Date().getTime() > Re) return;
  await lt();
  const t = JSON.stringify({ ...e, language: langList(e.language) }),
    ps = await G("perSite", !1);
  let i = Ze("SCOPE_CHEATER", 0);
  // [MEJORA] "Huella por sitio": cada sitio (dominio principal) recibe un
  // ruido distinto pero estable, así dos sitios no pueden cruzar tu huella.
  const u = `
    (() => {
      let c = ${t};
      if (${ps ? "true" : "false"}) try {
        let h = location.hostname;
        try {
          const ao = location.ancestorOrigins;
          if (ao && ao.length) h = new URL(ao[ao.length - 1]).hostname;
        } catch (e) {}
        h = h.replace(/^www\\./, "");
        let p = h.split(".");
        if (p.length > 2) h = (p[p.length - 2].length <= 3 ? p.slice(-3) : p.slice(-2)).join(".");
        let x = 0, str = h + ":" + (c.seed || 0);
        for (let k = 0; k < str.length; k++) x = (Math.imul(x, 31) + str.charCodeAt(k)) | 0;
        const r = () => {
          x = (x + 0x6d2b79f5) | 0;
          let q = Math.imul(x ^ (x >>> 15), 1 | x);
          q ^= q + Math.imul(q ^ (q >>> 7), 61 | q);
          return ((q ^ (q >>> 14)) >>> 0) / 4294967296;
        };
        c.seed = Math.floor(r() * 1e9) || 1;
        if (c.factors) for (const k in c.factors) if (c.factors[k]) c.factors[k] = (Math.floor(r() * 5000) + 1) / 1000;
        if (c.screen && c.screen.noise) c.screen.noise = (Math.floor(r() * 5000) + 1) / 1000;
      } catch (e) {}
      self['${Ze("SCOPE_BROWSER", 0)}'] = c;
    })();
    let cheat = self['${i}'];
    if (cheat) cheat.run()`,
    n = { matches: ["<all_urls>"] };
  let d = await me();
  if (d) {
    let b = await X();
    d === "white"
      ? (n.matches = b.white.map((f) => "*://" + f + "/*"))
      : d === "black" &&
        ((n.matches = ["file:///foo"]),
        (n.excludeMatches = b.black.map((f) => "*://" + f + "/*")));
  }
  await nt.userScripts.register([
    {
      id: "browser",
      runAt: "document_start",
      world: "MAIN",
      js: [{ code: u }],
      ...n,
    },
  ]);
}
async function lt() {
  try {
    await nt.userScripts.unregister({ ids: ["browser"] });
  } catch {}
}
async function S(e) {
  const t = await R.get(e);
  return t === void 0 || t == null ? Q.defaultConfig[e] : t;
}
async function ct({ key: e, value: t }) {
  e === "randomSeed" && (Ue.seed = t);
  let r = await R.set(e, t);
  if (e === "perSite") {
    let cur = await F();
    cur && (await he(cur));
  }
  if (e === "ipLookup") (await W.storage.session.remove("ipCache").catch(() => {}), await safeIp());
  return r;
}
async function Pt(e) {
  return await R.remove(e);
}
async function Dt() {
  let e = await pe(),
    t = await S("autoSwitch"),
    i = await X(),
    r = await me(),
    u = await Pe(),
    n = await Kt(),
    d = await S("os"),
    b = await G("safeMode", !0),
    f = await G("webglSafeMode", !1),
    m = await G("functionTrace"),
    y = await G("disableWorker"),
    E = await G("antiDebugger"),
    o = await Ut("randomSeed"),
    l = await De(),
    a = await dt(),
    s = await S("fixedBrowserId"),
    c = await S("fixedUA"),
    p = await S("fixedLanguage"),
    h = await S("fixedTimezone"),
    ipLookup = await G("ipLookup", !0),
    realistic = await G("realistic", !0),
    perSite = await G("perSite", !1),
    notify = await G("notify", !0);
  return {
    ipLookup,
    realistic,
    perSite,
    notify,
    fixedBrowserId: s,
    fixedUA: c,
    fixedLanguage: p,
    fixedTimezone: h,
    enables: a,
    option: e,
    autoSwitch: t,
    hostTable: i,
    hostEnable: r,
    deviceType: u,
    os: d,
    safeMode: b,
    webglSafeMode: f,
    disableWorker: y,
    functionTrace: m,
    iceServers: l,
    antiDebugger: E,
    randomSeed: o,
    shortcuts: n,
  };
}
async function ut(e, t) {
  return await R.set("deviceType", e);
}
async function Pe() {
  return (await S("deviceType")) || "";
}
async function G(e, t) {
  const i = await S(e);
  return i === void 0 || i === "" || i == null ? t || !1 : JSON.parse(i + "");
}
async function Ut(e = "") {
  return JSON.parse((await S(e)) || "0");
}
async function dt() {
  let e = (await S("enables")) || {};
  for (let t of Bt) t in e || (e[t] = 1);
  return ((e.driver = 0), e);
}
async function De() {
  let e = (await S("iceServers")) || [];
  return (e.length || e.push({ urls: "stun:stun.l.google.com:19302" }), e);
}
async function wt(e, t) {
  return await R.set("option", e);
}
async function pe() {
  return (await S("option")) || "随机生成";
}
async function Nt({ host: e, isWhite: t }, i) {
  t == null && (t = !0);
  let r = await X();
  if (t) {
    if (r.white.includes(e)) throw new Error(ui_t(`El dominio ${e} ya está en la lista blanca`, `域名 ${e} 在白名单中已存在`, `Domain ${e} is already whitelisted`));
    (r.white.push(e), await R.set("whiteList", r.white));
  } else {
    if (r.black.includes(e)) throw new Error(ui_t(`El dominio ${e} ya está en la lista negra`, `域名 ${e} 在黑名单中已存在`, `Domain ${e} is already blacklisted`));
    (r.black.push(e), await R.set("blackList", r.black));
  }
}
async function jt({ host: e, isWhite: t }, i) {
  let r = await X();
  t
    ? ((r.white = r.white.filter((u) => u !== e)),
      await R.set("whiteList", r.white))
    : ((r.black = r.black.filter((u) => u !== e)),
      await R.set("blackList", r.black));
}
async function me() {
  return (await S("hostEnable")) || 0;
}
async function zt(e, t) {
  // [MEJORA] Antes usaba `e in [...]` (que revisa índices, no valores) y
  // siempre lanzaba error. Además llamaba he() sin perfil.
  if (((e = e || ""), !["white", "black", ""].includes(e)))
    throw new Error(ui_t("Valor no válido", "值错误", "Invalid value"));
  await R.set("hostEnable", e);
  let cur = await F();
  cur && (await he(cur));
}
async function X() {
  let e = (await S("whiteList")) || [],
    t = (await S("blackList")) || [];
  return { white: e, black: t };
}
async function Kt() {
  return (await S("shortcuts")) || {};
}
async function Gt(e, t) {
  let i = (await S("proxies")) || {};
  return ((i[e] = 1), await R.set("proxies", i));
}
async function Wt(e, t) {
  let i = (await S("proxies")) || {};
  return (delete i[e], R.set("proxies", i));
}
async function qt({ page: e, size: t }, i) {
  let r = (await S("proxies")) || {},
    u = Object.keys(r),
    n = [];
  return (
    ft(u, e, t, (d, b, f) => {
      n.push({ proxy: d, index: f + 1 });
    }),
    { data: n, total: u.length }
  );
}
async function Ht({ regex: e, browserId: t }, i) {
  let r = (await S("matches")) || {};
  if (r[e]) throw new Error(e + ui_t(" ya está definido", " 已定义", " already defined"));
  return ((r[e] = t), R.set("matches", r));
}
async function $t(e, t) {
  let i = (await S("matches")) || {};
  return (delete i[e], R.set("matches", i));
}
async function Ft({ page: e, size: t }, i) {
  let r = (await S("matches")) || {},
    u = [],
    n = Object.keys(r);
  return (
    ft(n, e, t, (d, b, f) => {
      u.push({ regex: d, browserId: r[d], index: f + 1 });
    }),
    { data: u, total: n.length }
  );
}
function ft(e = [], t = 1, i = 5, r) {
  if (i <= 0) throw new Error(ui_t("size debe ser mayor que 0", "size必须大于0", "size must be > 0"));
  let u = ((t || 1) - 1) * i;
  for (let n = u; n < u + i; n++) {
    if (n >= e.length) return;
    let d = e[n];
    r(d, n, n - u);
  }
}
async function Xt() {
  let e = await pe(),
    t = await S("autoSwitch"),
    i = (await S("hostTable")) || {},
    r = await Pe(),
    u = await S("os"),
    n = await S("matches"),
    d = await S("fixedBrowserId"),
    b = await S("fixedUA"),
    f = await S("fixedLanguage"),
    m = await S("fixedTimezone"),
    y = await De(),
    E = [];
  return (
    await H.iterWith("", (o, l) => {
      E.push(l);
    }),
    {
      browsers: E,
      matches: n,
      hostTable: i,
      deviceType: r,
      os: u,
      option: e,
      autoSwitch: t,
      fixedBrowserId: d,
      fixedUA: b,
      fixedLanguage: f,
      fixedTimezone: m,
      iceServers: y,
    }
  );
}
async function Jt(e = {}, t) {
  let i = e.matches || {},
    r = e.hostTable || {},
    u = e.deviceType || "",
    n = e.os || "",
    d = e.option || "无",
    b = e.autoSwitch || "",
    f = e.fixedBrowserId || "",
    m = e.fixedUA || "",
    y = e.fixedLanguage || "",
    E = e.fixedTimezone || "",
    o = e.browsers || [],
    l = e.iceServers || [],
    a = (await S("matches")) || {},
    s = (await S("hostTable")) || {};
  (Object.keys(i).forEach((c) => {
    a[c] = i[c];
  }),
    Object.keys(r).forEach((c) => {
      s[c] = r[c];
    }),
    await Promise.all([
      ut(u),
      wt(d),
      R.set("autoSwitch", b),
      R.set("os", n),
      R.set("fixedBrowserId", f),
      R.set("fixedUA", m),
      R.set("fixedLanguage", y),
      R.set("fixedTimezone", E),
      R.set("matches", i),
      R.set("hostTable", r),
      R.set("iceServers", l),
    ]),
    o.forEach((c) => (c.id || (c.id = at()), H.set(c.id, c))));
}
var Yt =
    typeof globalThis < "u"
      ? globalThis
      : typeof window < "u"
        ? window
        : typeof global < "u"
          ? global
          : typeof self < "u"
            ? self
            : {},
  Me = { exports: {} };
(function (e, t) {
  (function (i, r) {
    var u = "1.0.39",
      n = "",
      d = "?",
      b = "function",
      f = "undefined",
      m = "object",
      y = "string",
      E = "major",
      o = "model",
      l = "name",
      a = "type",
      s = "vendor",
      c = "version",
      p = "architecture",
      h = "console",
      w = "mobile",
      g = "tablet",
      M = "smarttv",
      T = "wearable",
      D = "embedded",
      O = 500,
      A = "Amazon",
      L = "Apple",
      $ = "ASUS",
      je = "BlackBerry",
      V = "Browser",
      ne = "Chrome",
      St = "Edge",
      se = "Firefox",
      le = "Google",
      ze = "Huawei",
      ye = "LG",
      xe = "Microsoft",
      Ke = "Motorola",
      ee = "Opera",
      te = "Samsung",
      Ge = "Sharp",
      ce = "Sony",
      Se = "Xiaomi",
      ke = "Zebra",
      We = "Facebook",
      qe = "Chromium OS",
      He = "Mac OS",
      $e = " Browser",
      kt = function (k, _) {
        var x = {};
        for (var I in k)
          _[I] && _[I].length % 2 === 0
            ? (x[I] = _[I].concat(k[I]))
            : (x[I] = k[I]);
        return x;
      },
      ue = function (k) {
        for (var _ = {}, x = 0; x < k.length; x++) _[k[x].toUpperCase()] = k[x];
        return _;
      },
      Fe = function (k, _) {
        return typeof k === y ? J(_).indexOf(J(k)) !== -1 : !1;
      },
      J = function (k) {
        return k.toLowerCase();
      },
      Et = function (k) {
        return typeof k === y ? k.replace(/[^\d\.]/g, n).split(".")[0] : r;
      },
      Ee = function (k, _) {
        if (typeof k === y)
          return (
            (k = k.replace(/^\s\s*/, n)),
            typeof _ === f ? k : k.substring(0, O)
          );
      },
      ie = function (k, _) {
        for (var x = 0, I, K, j, C, v, z; x < _.length && !v; ) {
          var Te = _[x],
            Ye = _[x + 1];
          for (I = K = 0; I < Te.length && !v && Te[I]; )
            if (((v = Te[I++].exec(k)), v))
              for (j = 0; j < Ye.length; j++)
                ((z = v[++K]),
                  (C = Ye[j]),
                  typeof C === m && C.length > 0
                    ? C.length === 2
                      ? typeof C[1] == b
                        ? (this[C[0]] = C[1].call(this, z))
                        : (this[C[0]] = C[1])
                      : C.length === 3
                        ? typeof C[1] === b && !(C[1].exec && C[1].test)
                          ? (this[C[0]] = z ? C[1].call(this, z, C[2]) : r)
                          : (this[C[0]] = z ? z.replace(C[1], C[2]) : r)
                        : C.length === 4 &&
                          (this[C[0]] = z
                            ? C[3].call(this, z.replace(C[1], C[2]))
                            : r)
                    : (this[C] = z || r));
          x += 2;
        }
      },
      de = function (k, _) {
        for (var x in _)
          if (typeof _[x] === m && _[x].length > 0) {
            for (var I = 0; I < _[x].length; I++)
              if (Fe(_[x][I], k)) return x === d ? r : x;
          } else if (Fe(_[x], k)) return x === d ? r : x;
        return _.hasOwnProperty("*") ? _["*"] : k;
      },
      Tt = {
        "1.0": "/8",
        1.2: "/1",
        1.3: "/3",
        "2.0": "/412",
        "2.0.2": "/416",
        "2.0.3": "/417",
        "2.0.4": "/419",
        "?": "/",
      },
      Xe = {
        ME: "4.90",
        "NT 3.11": "NT3.51",
        "NT 4.0": "NT4.0",
        2e3: "NT 5.0",
        XP: ["NT 5.1", "NT 5.2"],
        Vista: "NT 6.0",
        7: "NT 6.1",
        8: "NT 6.2",
        8.1: "NT 6.3",
        10: ["NT 6.4", "NT 10.0"],
        RT: "ARM",
      },
      Je = {
        browser: [
          [/\b(?:crmo|crios)\/([\w\.]+)/i],
          [c, [l, "Chrome"]],
          [/edg(?:e|ios|a)?\/([\w\.]+)/i],
          [c, [l, "Edge"]],
          [
            /(opera mini)\/([-\w\.]+)/i,
            /(opera [mobiletab]{3,6})\b.+version\/([-\w\.]+)/i,
            /(opera)(?:.+version\/|[\/ ]+)([\w\.]+)/i,
          ],
          [l, c],
          [/opios[\/ ]+([\w\.]+)/i],
          [c, [l, ee + " Mini"]],
          [/\bop(?:rg)?x\/([\w\.]+)/i],
          [c, [l, ee + " GX"]],
          [/\bopr\/([\w\.]+)/i],
          [c, [l, ee]],
          [/\bb[ai]*d(?:uhd|[ub]*[aekoprswx]{5,6})[\/ ]?([\w\.]+)/i],
          [c, [l, "Baidu"]],
          [
            /(kindle)\/([\w\.]+)/i,
            /(lunascape|maxthon|netfront|jasmine|blazer|sleipnir)[\/ ]?([\w\.]*)/i,
            /(avant|iemobile|slim)\s?(?:browser)?[\/ ]?([\w\.]*)/i,
            /(?:ms|\()(ie) ([\w\.]+)/i,
            /(flock|rockmelt|midori|epiphany|silk|skyfire|ovibrowser|bolt|iron|vivaldi|iridium|phantomjs|bowser|qupzilla|falkon|rekonq|puffin|brave|whale(?!.+naver)|qqbrowserlite|duckduckgo|klar|helio)\/([-\w\.]+)/i,
            /(heytap|ovi)browser\/([\d\.]+)/i,
            /(weibo)__([\d\.]+)/i,
          ],
          [l, c],
          [/quark(?:pc)?\/([-\w\.]+)/i],
          [c, [l, "Quark"]],
          [/\bddg\/([\w\.]+)/i],
          [c, [l, "DuckDuckGo"]],
          [/(?:\buc? ?browser|(?:juc.+)ucweb)[\/ ]?([\w\.]+)/i],
          [c, [l, "UC" + V]],
          [
            /microm.+\bqbcore\/([\w\.]+)/i,
            /\bqbcore\/([\w\.]+).+microm/i,
            /micromessenger\/([\w\.]+)/i,
          ],
          [c, [l, "WeChat"]],
          [/konqueror\/([\w\.]+)/i],
          [c, [l, "Konqueror"]],
          [/trident.+rv[: ]([\w\.]{1,9})\b.+like gecko/i],
          [c, [l, "IE"]],
          [/ya(?:search)?browser\/([\w\.]+)/i],
          [c, [l, "Yandex"]],
          [/slbrowser\/([\w\.]+)/i],
          [c, [l, "Smart Lenovo " + V]],
          [/(avast|avg)\/([\w\.]+)/i],
          [[l, /(.+)/, "$1 Secure " + V], c],
          [/\bfocus\/([\w\.]+)/i],
          [c, [l, se + " Focus"]],
          [/\bopt\/([\w\.]+)/i],
          [c, [l, ee + " Touch"]],
          [/coc_coc\w+\/([\w\.]+)/i],
          [c, [l, "Coc Coc"]],
          [/dolfin\/([\w\.]+)/i],
          [c, [l, "Dolphin"]],
          [/coast\/([\w\.]+)/i],
          [c, [l, ee + " Coast"]],
          [/miuibrowser\/([\w\.]+)/i],
          [c, [l, "MIUI " + V]],
          [/fxios\/([-\w\.]+)/i],
          [c, [l, se]],
          [/\bqihu|(qi?ho?o?|360)browser/i],
          [[l, "360" + $e]],
          [/\b(qq)\/([\w\.]+)/i],
          [[l, /(.+)/, "$1Browser"], c],
          [/(oculus|sailfish|huawei|vivo|pico)browser\/([\w\.]+)/i],
          [[l, /(.+)/, "$1" + $e], c],
          [/samsungbrowser\/([\w\.]+)/i],
          [c, [l, te + " Internet"]],
          [/(comodo_dragon)\/([\w\.]+)/i],
          [[l, /_/g, " "], c],
          [/metasr[\/ ]?([\d\.]+)/i],
          [c, [l, "Sogou Explorer"]],
          [/(sogou)mo\w+\/([\d\.]+)/i],
          [[l, "Sogou Mobile"], c],
          [
            /(electron)\/([\w\.]+) safari/i,
            /(tesla)(?: qtcarbrowser|\/(20\d\d\.[-\w\.]+))/i,
            /m?(qqbrowser|2345Explorer)[\/ ]?([\w\.]+)/i,
          ],
          [l, c],
          [/(lbbrowser|rekonq)/i, /\[(linkedin)app\]/i],
          [l],
          [/((?:fban\/fbios|fb_iab\/fb4a)(?!.+fbav)|;fbav\/([\w\.]+);)/i],
          [[l, We], c],
          [
            /(Klarna)\/([\w\.]+)/i,
            /(kakao(?:talk|story))[\/ ]([\w\.]+)/i,
            /(naver)\(.*?(\d+\.[\w\.]+).*\)/i,
            /safari (line)\/([\w\.]+)/i,
            /\b(line)\/([\w\.]+)\/iab/i,
            /(alipay)client\/([\w\.]+)/i,
            /(twitter)(?:and| f.+e\/([\w\.]+))/i,
            /(chromium|instagram|snapchat)[\/ ]([-\w\.]+)/i,
          ],
          [l, c],
          [/\bgsa\/([\w\.]+) .*safari\//i],
          [c, [l, "GSA"]],
          [/musical_ly(?:.+app_?version\/|_)([\w\.]+)/i],
          [c, [l, "TikTok"]],
          [/headlesschrome(?:\/([\w\.]+)| )/i],
          [c, [l, ne + " Headless"]],
          [/ wv\).+(chrome)\/([\w\.]+)/i],
          [[l, ne + " WebView"], c],
          [/droid.+ version\/([\w\.]+)\b.+(?:mobile safari|safari)/i],
          [c, [l, "Android " + V]],
          [/(chrome|omniweb|arora|[tizenoka]{5} ?browser)\/v?([\w\.]+)/i],
          [l, c],
          [/version\/([\w\.\,]+) .*mobile\/\w+ (safari)/i],
          [c, [l, "Mobile Safari"]],
          [/version\/([\w(\.|\,)]+) .*(mobile ?safari|safari)/i],
          [c, l],
          [/webkit.+?(mobile ?safari|safari)(\/[\w\.]+)/i],
          [l, [c, de, Tt]],
          [/(webkit|khtml)\/([\w\.]+)/i],
          [l, c],
          [/(navigator|netscape\d?)\/([-\w\.]+)/i],
          [[l, "Netscape"], c],
          [/(wolvic)\/([\w\.]+)/i],
          [l, c],
          [/mobile vr; rv:([\w\.]+)\).+firefox/i],
          [c, [l, se + " Reality"]],
          [
            /ekiohf.+(flow)\/([\w\.]+)/i,
            /(swiftfox)/i,
            /(icedragon|iceweasel|camino|chimera|fennec|maemo browser|minimo|conkeror)[\/ ]?([\w\.\+]+)/i,
            /(seamonkey|k-meleon|icecat|iceape|firebird|phoenix|palemoon|basilisk|waterfox)\/([-\w\.]+)$/i,
            /(firefox)\/([\w\.]+)/i,
            /(mozilla)\/([\w\.]+) .+rv\:.+gecko\/\d+/i,
            /(polaris|lynx|dillo|icab|doris|amaya|w3m|netsurf|obigo|mosaic|(?:go|ice|up)[\. ]?browser)[-\/ ]?v?([\w\.]+)/i,
            /(links) \(([\w\.]+)/i,
          ],
          [l, [c, /_/g, "."]],
          [/(cobalt)\/([\w\.]+)/i],
          [l, [c, /master.|lts./, ""]],
        ],
        cpu: [
          [/(?:(amd|x(?:(?:86|64)[-_])?|wow|win)64)[;\)]/i],
          [[p, "amd64"]],
          [/(ia32(?=;))/i],
          [[p, J]],
          [/((?:i[346]|x)86)[;\)]/i],
          [[p, "ia32"]],
          [/\b(aarch64|arm(v?8e?l?|_?64))\b/i],
          [[p, "arm64"]],
          [/\b(arm(?:v[67])?ht?n?[fl]p?)\b/i],
          [[p, "armhf"]],
          [/windows (ce|mobile); ppc;/i],
          [[p, "arm"]],
          [/((?:ppc|powerpc)(?:64)?)(?: mac|;|\))/i],
          [[p, /ower/, n, J]],
          [/(sun4\w)[;\)]/i],
          [[p, "sparc"]],
          [
            /((?:avr32|ia64(?=;))|68k(?=\))|\barm(?=v(?:[1-7]|[5-7]1)l?|;|eabi)|(?=atmel )avr|(?:irix|mips|sparc)(?:64)?\b|pa-risc)/i,
          ],
          [[p, J]],
        ],
        device: [
          [
            /\b(sch-i[89]0\d|shw-m380s|sm-[ptx]\w{2,4}|gt-[pn]\d{2,4}|sgh-t8[56]9|nexus 10)/i,
          ],
          [o, [s, te], [a, g]],
          [
            /\b((?:s[cgp]h|gt|sm)-(?![lr])\w+|sc[g-]?[\d]+a?|galaxy nexus)/i,
            /samsung[- ]((?!sm-[lr])[-\w]+)/i,
            /sec-(sgh\w+)/i,
          ],
          [o, [s, te], [a, w]],
          [/(?:\/|\()(ip(?:hone|od)[\w, ]*)(?:\/|;)/i],
          [o, [s, L], [a, w]],
          [
            /\((ipad);[-\w\),; ]+apple/i,
            /applecoremedia\/[\w\.]+ \((ipad)/i,
            /\b(ipad)\d\d?,\d\d?[;\]].+ios/i,
          ],
          [o, [s, L], [a, g]],
          [/(macintosh);/i],
          [o, [s, L]],
          [/\b(sh-?[altvz]?\d\d[a-ekm]?)/i],
          [o, [s, Ge], [a, w]],
          [/\b((?:ag[rs][23]?|bah2?|sht?|btv)-a?[lw]\d{2})\b(?!.+d\/s)/i],
          [o, [s, ze], [a, g]],
          [
            /(?:huawei|honor)([-\w ]+)[;\)]/i,
            /\b(nexus 6p|\w{2,4}e?-[atu]?[ln][\dx][012359c][adn]?)\b(?!.+d\/s)/i,
          ],
          [o, [s, ze], [a, w]],
          [
            /\b(poco[\w ]+|m2\d{3}j\d\d[a-z]{2})(?: bui|\))/i,
            /\b; (\w+) build\/hm\1/i,
            /\b(hm[-_ ]?note?[_ ]?(?:\d\w)?) bui/i,
            /\b(redmi[\-_ ]?(?:note|k)?[\w_ ]+)(?: bui|\))/i,
            /oid[^\)]+; (m?[12][0-389][01]\w{3,6}[c-y])( bui|; wv|\))/i,
            /\b(mi[-_ ]?(?:a\d|one|one[_ ]plus|note lte|max|cc)?[_ ]?(?:\d?\w?)[_ ]?(?:plus|se|lite|pro)?)(?: bui|\))/i,
          ],
          [
            [o, /_/g, " "],
            [s, Se],
            [a, w],
          ],
          [
            /oid[^\)]+; (2\d{4}(283|rpbf)[cgl])( bui|\))/i,
            /\b(mi[-_ ]?(?:pad)(?:[\w_ ]+))(?: bui|\))/i,
          ],
          [
            [o, /_/g, " "],
            [s, Se],
            [a, g],
          ],
          [
            /; (\w+) bui.+ oppo/i,
            /\b(cph[12]\d{3}|p(?:af|c[al]|d\w|e[ar])[mt]\d0|x9007|a101op)\b/i,
          ],
          [o, [s, "OPPO"], [a, w]],
          [/\b(opd2\d{3}a?) bui/i],
          [o, [s, "OPPO"], [a, g]],
          [/vivo (\w+)(?: bui|\))/i, /\b(v[12]\d{3}\w?[at])(?: bui|;)/i],
          [o, [s, "Vivo"], [a, w]],
          [/\b(rmx[1-3]\d{3})(?: bui|;|\))/i],
          [o, [s, "Realme"], [a, w]],
          [
            /\b(milestone|droid(?:[2-4x]| (?:bionic|x2|pro|razr))?:?( 4g)?)\b[\w ]+build\//i,
            /\bmot(?:orola)?[- ](\w*)/i,
            /((?:moto[\w\(\) ]+|xt\d{3,4}|nexus 6)(?= bui|\)))/i,
          ],
          [o, [s, Ke], [a, w]],
          [/\b(mz60\d|xoom[2 ]{0,2}) build\//i],
          [o, [s, Ke], [a, g]],
          [/((?=lg)?[vl]k\-?\d{3}) bui| 3\.[-\w; ]{10}lg?-([06cv9]{3,4})/i],
          [o, [s, ye], [a, g]],
          [
            /(lm(?:-?f100[nv]?|-[\w\.]+)(?= bui|\))|nexus [45])/i,
            /\blg[-e;\/ ]+((?!browser|netcast|android tv)\w+)/i,
            /\blg-?([\d\w]+) bui/i,
          ],
          [o, [s, ye], [a, w]],
          [
            /(ideatab[-\w ]+)/i,
            /lenovo ?(s[56]000[-\w]+|tab(?:[\w ]+)|yt[-\d\w]{6}|tb[-\d\w]{6})/i,
          ],
          [o, [s, "Lenovo"], [a, g]],
          [/(?:maemo|nokia).*(n900|lumia \d+)/i, /nokia[-_ ]?([-\w\.]*)/i],
          [
            [o, /_/g, " "],
            [s, "Nokia"],
            [a, w],
          ],
          [/(pixel c)\b/i],
          [o, [s, le], [a, g]],
          [/droid.+; (pixel[\daxl ]{0,6})(?: bui|\))/i],
          [o, [s, le], [a, w]],
          [
            /droid.+ (a?\d[0-2]{2}so|[c-g]\d{4}|so[-gl]\w+|xq-a\w[4-7][12])(?= bui|\).+chrome\/(?![1-6]{0,1}\d\.))/i,
          ],
          [o, [s, ce], [a, w]],
          [/sony tablet [ps]/i, /\b(?:sony)?sgp\w+(?: bui|\))/i],
          [
            [o, "Xperia Tablet"],
            [s, ce],
            [a, g],
          ],
          [
            / (kb2005|in20[12]5|be20[12][59])\b/i,
            /(?:one)?(?:plus)? (a\d0\d\d)(?: b|\))/i,
          ],
          [o, [s, "OnePlus"], [a, w]],
          [
            /(alexa)webm/i,
            /(kf[a-z]{2}wi|aeo(?!bc)\w\w)( bui|\))/i,
            /(kf[a-z]+)( bui|\)).+silk\//i,
          ],
          [o, [s, A], [a, g]],
          [/((?:sd|kf)[0349hijorstuw]+)( bui|\)).+silk\//i],
          [
            [o, /(.+)/g, "Fire Phone $1"],
            [s, A],
            [a, w],
          ],
          [/(playbook);[-\w\),; ]+(rim)/i],
          [o, s, [a, g]],
          [/\b((?:bb[a-f]|st[hv])100-\d)/i, /\(bb10; (\w+)/i],
          [o, [s, je], [a, w]],
          [
            /(?:\b|asus_)(transfo[prime ]{4,10} \w+|eeepc|slider \w+|nexus 7|padfone|p00[cj])/i,
          ],
          [o, [s, $], [a, g]],
          [/ (z[bes]6[027][012][km][ls]|zenfone \d\w?)\b/i],
          [o, [s, $], [a, w]],
          [/(nexus 9)/i],
          [o, [s, "HTC"], [a, g]],
          [
            /(htc)[-;_ ]{1,2}([\w ]+(?=\)| bui)|\w+)/i,
            /(zte)[- ]([\w ]+?)(?: bui|\/|\))/i,
            /(alcatel|geeksphone|nexian|panasonic(?!(?:;|\.))|sony(?!-bra))[-_ ]?([-\w]*)/i,
          ],
          [s, [o, /_/g, " "], [a, w]],
          [
            /droid [\w\.]+; ((?:8[14]9[16]|9(?:0(?:48|60|8[01])|1(?:3[27]|66)|2(?:6[69]|9[56])|466))[gqswx])\w*(\)| bui)/i,
          ],
          [o, [s, "TCL"], [a, g]],
          [/(itel) ((\w+))/i],
          [[s, J], o, [a, de, { tablet: ["p10001l", "w7001"], "*": "mobile" }]],
          [/droid.+; ([ab][1-7]-?[0178a]\d\d?)/i],
          [o, [s, "Acer"], [a, g]],
          [/droid.+; (m[1-5] note) bui/i, /\bmz-([-\w]{2,})/i],
          [o, [s, "Meizu"], [a, w]],
          [/; ((?:power )?armor(?:[\w ]{0,8}))(?: bui|\))/i],
          [o, [s, "Ulefone"], [a, w]],
          [/droid.+; (a(?:015|06[35]|142p?))/i],
          [o, [s, "Nothing"], [a, w]],
          [
            /(blackberry|benq|palm(?=\-)|sonyericsson|acer|asus|dell|meizu|motorola|polytron|infinix|tecno)[-_ ]?([-\w]*)/i,
            /(hp) ([\w ]+\w)/i,
            /(asus)-?(\w+)/i,
            /(microsoft); (lumia[\w ]+)/i,
            /(lenovo)[-_ ]?([-\w]+)/i,
            /(jolla)/i,
            /(oppo) ?([\w ]+) bui/i,
          ],
          [s, o, [a, w]],
          [
            /(kobo)\s(ereader|touch)/i,
            /(archos) (gamepad2?)/i,
            /(hp).+(touchpad(?!.+tablet)|tablet)/i,
            /(kindle)\/([\w\.]+)/i,
            /(nook)[\w ]+build\/(\w+)/i,
            /(dell) (strea[kpr\d ]*[\dko])/i,
            /(le[- ]+pan)[- ]+(\w{1,9}) bui/i,
            /(trinity)[- ]*(t\d{3}) bui/i,
            /(gigaset)[- ]+(q\w{1,9}) bui/i,
            /(vodafone) ([\w ]+)(?:\)| bui)/i,
          ],
          [s, o, [a, g]],
          [/(surface duo)/i],
          [o, [s, xe], [a, g]],
          [/droid [\d\.]+; (fp\du?)(?: b|\))/i],
          [o, [s, "Fairphone"], [a, w]],
          [/(u304aa)/i],
          [o, [s, "AT&T"], [a, w]],
          [/\bsie-(\w*)/i],
          [o, [s, "Siemens"], [a, w]],
          [/\b(rct\w+) b/i],
          [o, [s, "RCA"], [a, g]],
          [/\b(venue[\d ]{2,7}) b/i],
          [o, [s, "Dell"], [a, g]],
          [/\b(q(?:mv|ta)\w+) b/i],
          [o, [s, "Verizon"], [a, g]],
          [/\b(?:barnes[& ]+noble |bn[rt])([\w\+ ]*) b/i],
          [o, [s, "Barnes & Noble"], [a, g]],
          [/\b(tm\d{3}\w+) b/i],
          [o, [s, "NuVision"], [a, g]],
          [/\b(k88) b/i],
          [o, [s, "ZTE"], [a, g]],
          [/\b(nx\d{3}j) b/i],
          [o, [s, "ZTE"], [a, w]],
          [/\b(gen\d{3}) b.+49h/i],
          [o, [s, "Swiss"], [a, w]],
          [/\b(zur\d{3}) b/i],
          [o, [s, "Swiss"], [a, g]],
          [/\b((zeki)?tb.*\b) b/i],
          [o, [s, "Zeki"], [a, g]],
          [/\b([yr]\d{2}) b/i, /\b(dragon[- ]+touch |dt)(\w{5}) b/i],
          [[s, "Dragon Touch"], o, [a, g]],
          [/\b(ns-?\w{0,9}) b/i],
          [o, [s, "Insignia"], [a, g]],
          [/\b((nxa|next)-?\w{0,9}) b/i],
          [o, [s, "NextBook"], [a, g]],
          [/\b(xtreme\_)?(v(1[045]|2[015]|[3469]0|7[05])) b/i],
          [[s, "Voice"], o, [a, w]],
          [/\b(lvtel\-)?(v1[12]) b/i],
          [[s, "LvTel"], o, [a, w]],
          [/\b(ph-1) /i],
          [o, [s, "Essential"], [a, w]],
          [/\b(v(100md|700na|7011|917g).*\b) b/i],
          [o, [s, "Envizen"], [a, g]],
          [/\b(trio[-\w\. ]+) b/i],
          [o, [s, "MachSpeed"], [a, g]],
          [/\btu_(1491) b/i],
          [o, [s, "Rotor"], [a, g]],
          [/(shield[\w ]+) b/i],
          [o, [s, "Nvidia"], [a, g]],
          [/(sprint) (\w+)/i],
          [s, o, [a, w]],
          [/(kin\.[onetw]{3})/i],
          [
            [o, /\./g, " "],
            [s, xe],
            [a, w],
          ],
          [/droid.+; (cc6666?|et5[16]|mc[239][23]x?|vc8[03]x?)\)/i],
          [o, [s, ke], [a, g]],
          [/droid.+; (ec30|ps20|tc[2-8]\d[kx])\)/i],
          [o, [s, ke], [a, w]],
          [/smart-tv.+(samsung)/i],
          [s, [a, M]],
          [/hbbtv.+maple;(\d+)/i],
          [
            [o, /^/, "SmartTV"],
            [s, te],
            [a, M],
          ],
          [/(nux; netcast.+smarttv|lg (netcast\.tv-201\d|android tv))/i],
          [
            [s, ye],
            [a, M],
          ],
          [/(apple) ?tv/i],
          [s, [o, L + " TV"], [a, M]],
          [/crkey/i],
          [
            [o, ne + "cast"],
            [s, le],
            [a, M],
          ],
          [/droid.+aft(\w+)( bui|\))/i],
          [o, [s, A], [a, M]],
          [/\(dtv[\);].+(aquos)/i, /(aquos-tv[\w ]+)\)/i],
          [o, [s, Ge], [a, M]],
          [/(bravia[\w ]+)( bui|\))/i],
          [o, [s, ce], [a, M]],
          [/(mitv-\w{5}) bui/i],
          [o, [s, Se], [a, M]],
          [/Hbbtv.*(technisat) (.*);/i],
          [s, o, [a, M]],
          [
            /\b(roku)[\dx]*[\)\/]((?:dvp-)?[\d\.]*)/i,
            /hbbtv\/\d+\.\d+\.\d+ +\([\w\+ ]*; *([\w\d][^;]*);([^;]*)/i,
          ],
          [
            [s, Ee],
            [o, Ee],
            [a, M],
          ],
          [/\b(android tv|smart[- ]?tv|opera tv|tv; rv:)\b/i],
          [[a, M]],
          [/(ouya)/i, /(nintendo) ([wids3utch]+)/i],
          [s, o, [a, h]],
          [/droid.+; (shield) bui/i],
          [o, [s, "Nvidia"], [a, h]],
          [/(playstation [345portablevi]+)/i],
          [o, [s, ce], [a, h]],
          [/\b(xbox(?: one)?(?!; xbox))[\); ]/i],
          [o, [s, xe], [a, h]],
          [/\b(sm-[lr]\d\d[05][fnuw]?s?)\b/i],
          [o, [s, te], [a, T]],
          [/((pebble))app/i],
          [s, o, [a, T]],
          [/(watch)(?: ?os[,\/]|\d,\d\/)[\d\.]+/i],
          [o, [s, L], [a, T]],
          [/droid.+; (glass) \d/i],
          [o, [s, le], [a, T]],
          [/droid.+; (wt63?0{2,3})\)/i],
          [o, [s, ke], [a, T]],
          [/(quest( \d| pro)?)/i],
          [o, [s, We], [a, T]],
          [/(tesla)(?: qtcarbrowser|\/[-\w\.]+)/i],
          [s, [a, D]],
          [/(aeobc)\b/i],
          [o, [s, A], [a, D]],
          [/droid .+?; ([^;]+?)(?: bui|; wv\)|\) applew).+? mobile safari/i],
          [o, [a, w]],
          [/droid .+?; ([^;]+?)(?: bui|\) applew).+?(?! mobile) safari/i],
          [o, [a, g]],
          [/\b((tablet|tab)[;\/]|focus\/\d(?!.+mobile))/i],
          [[a, g]],
          [/(phone|mobile(?:[;\/]| [ \w\/\.]*safari)|pda(?=.+windows ce))/i],
          [[a, w]],
          [/(android[-\w\. ]{0,9});.+buil/i],
          [o, [s, "Generic"]],
        ],
        engine: [
          [/windows.+ edge\/([\w\.]+)/i],
          [c, [l, St + "HTML"]],
          [/webkit\/537\.36.+chrome\/(?!27)([\w\.]+)/i],
          [c, [l, "Blink"]],
          [
            /(presto)\/([\w\.]+)/i,
            /(webkit|trident|netfront|netsurf|amaya|lynx|w3m|goanna)\/([\w\.]+)/i,
            /ekioh(flow)\/([\w\.]+)/i,
            /(khtml|tasman|links)[\/ ]\(?([\w\.]+)/i,
            /(icab)[\/ ]([23]\.[\d\.]+)/i,
            /\b(libweb)/i,
          ],
          [l, c],
          [/rv\:([\w\.]{1,9})\b.+(gecko)/i],
          [c, l],
        ],
        os: [
          [/microsoft (windows) (vista|xp)/i],
          [l, c],
          [/(windows (?:phone(?: os)?|mobile))[\/ ]?([\d\.\w ]*)/i],
          [l, [c, de, Xe]],
          [
            /windows nt 6\.2; (arm)/i,
            /windows[\/ ]?([ntce\d\. ]+\w)(?!.+xbox)/i,
            /(?:win(?=3|9|n)|win 9x )([nt\d\.]+)/i,
          ],
          [
            [c, de, Xe],
            [l, "Windows"],
          ],
          [
            /ip[honead]{2,4}\b(?:.*os ([\w]+) like mac|; opera)/i,
            /(?:ios;fbsv\/|iphone.+ios[\/ ])([\d\.]+)/i,
            /cfnetwork\/.+darwin/i,
          ],
          [
            [c, /_/g, "."],
            [l, "iOS"],
          ],
          [/(mac os x) ?([\w\. ]*)/i, /(macintosh|mac_powerpc\b)(?!.+haiku)/i],
          [
            [l, He],
            [c, /_/g, "."],
          ],
          [/droid ([\w\.]+)\b.+(android[- ]x86|harmonyos)/i],
          [c, l],
          [
            /(android|webos|qnx|bada|rim tablet os|maemo|meego|sailfish)[-\/ ]?([\w\.]*)/i,
            /(blackberry)\w*\/([\w\.]*)/i,
            /(tizen|kaios)[\/ ]([\w\.]+)/i,
            /\((series40);/i,
          ],
          [l, c],
          [/\(bb(10);/i],
          [c, [l, je]],
          [/(?:symbian ?os|symbos|s60(?=;)|series60)[-\/ ]?([\w\.]*)/i],
          [c, [l, "Symbian"]],
          [
            /mozilla\/[\d\.]+ \((?:mobile|tablet|tv|mobile; [\w ]+); rv:.+ gecko\/([\w\.]+)/i,
          ],
          [c, [l, se + " OS"]],
          [/web0s;.+rt(tv)/i, /\b(?:hp)?wos(?:browser)?\/([\w\.]+)/i],
          [c, [l, "webOS"]],
          [/watch(?: ?os[,\/]|\d,\d\/)([\d\.]+)/i],
          [c, [l, "watchOS"]],
          [/crkey\/([\d\.]+)/i],
          [c, [l, ne + "cast"]],
          [/(cros) [\w]+(?:\)| ([\w\.]+)\b)/i],
          [[l, qe], c],
          [
            /panasonic;(viera)/i,
            /(netrange)mmh/i,
            /(nettv)\/(\d+\.[\w\.]+)/i,
            /(nintendo|playstation) ([wids345portablevuch]+)/i,
            /(xbox); +xbox ([^\);]+)/i,
            /\b(joli|palm)\b ?(?:os)?\/?([\w\.]*)/i,
            /(mint)[\/\(\) ]?(\w*)/i,
            /(mageia|vectorlinux)[; ]/i,
            /([kxln]?ubuntu|debian|suse|opensuse|gentoo|arch(?= linux)|slackware|fedora|mandriva|centos|pclinuxos|red ?hat|zenwalk|linpus|raspbian|plan 9|minix|risc os|contiki|deepin|manjaro|elementary os|sabayon|linspire)(?: gnu\/linux)?(?: enterprise)?(?:[- ]linux)?(?:-gnu)?[-\/ ]?(?!chrom|package)([-\w\.]*)/i,
            /(hurd|linux) ?([\w\.]*)/i,
            /(gnu) ?([\w\.]*)/i,
            /\b([-frentopcghs]{0,5}bsd|dragonfly)[\/ ]?(?!amd|[ix346]{1,2}86)([\w\.]*)/i,
            /(haiku) (\w+)/i,
          ],
          [l, c],
          [/(sunos) ?([\w\.\d]*)/i],
          [[l, "Solaris"], c],
          [
            /((?:open)?solaris)[-\/ ]?([\w\.]*)/i,
            /(aix) ((\d)(?=\.|\)| )[\w\.])*/i,
            /\b(beos|os\/2|amigaos|morphos|openvms|fuchsia|hp-ux|serenityos)/i,
            /(unix) ?([\w\.]*)/i,
          ],
          [l, c],
        ],
      },
      U = function (k, _) {
        if ((typeof k === m && ((_ = k), (k = r)), !(this instanceof U)))
          return new U(k, _).getResult();
        var x = typeof i !== f && i.navigator ? i.navigator : r,
          I = k || (x && x.userAgent ? x.userAgent : n),
          K = x && x.userAgentData ? x.userAgentData : r,
          j = _ ? kt(Je, _) : Je,
          C = x && x.userAgent == I;
        return (
          (this.getBrowser = function () {
            var v = {};
            return (
              (v[l] = r),
              (v[c] = r),
              ie.call(v, I, j.browser),
              (v[E] = Et(v[c])),
              C &&
                x &&
                x.brave &&
                typeof x.brave.isBrave == b &&
                (v[l] = "Brave"),
              v
            );
          }),
          (this.getCPU = function () {
            var v = {};
            return ((v[p] = r), ie.call(v, I, j.cpu), v);
          }),
          (this.getDevice = function () {
            var v = {};
            return (
              (v[s] = r),
              (v[o] = r),
              (v[a] = r),
              ie.call(v, I, j.device),
              C && !v[a] && K && K.mobile && (v[a] = w),
              C &&
                v[o] == "Macintosh" &&
                x &&
                typeof x.standalone !== f &&
                x.maxTouchPoints &&
                x.maxTouchPoints > 2 &&
                ((v[o] = "iPad"), (v[a] = g)),
              v
            );
          }),
          (this.getEngine = function () {
            var v = {};
            return ((v[l] = r), (v[c] = r), ie.call(v, I, j.engine), v);
          }),
          (this.getOS = function () {
            var v = {};
            return (
              (v[l] = r),
              (v[c] = r),
              ie.call(v, I, j.os),
              C &&
                !v[l] &&
                K &&
                K.platform &&
                K.platform != "Unknown" &&
                (v[l] = K.platform
                  .replace(/chrome os/i, qe)
                  .replace(/macos/i, He)),
              v
            );
          }),
          (this.getResult = function () {
            return {
              ua: this.getUA(),
              browser: this.getBrowser(),
              engine: this.getEngine(),
              os: this.getOS(),
              device: this.getDevice(),
              cpu: this.getCPU(),
            };
          }),
          (this.getUA = function () {
            return I;
          }),
          (this.setUA = function (v) {
            return ((I = typeof v === y && v.length > O ? Ee(v, O) : v), this);
          }),
          this.setUA(I),
          this
        );
      };
    ((U.VERSION = u),
      (U.BROWSER = ue([l, c, E])),
      (U.CPU = ue([p])),
      (U.DEVICE = ue([o, s, a, h, w, M, g, T, D])),
      (U.ENGINE = U.OS = ue([l, c])),
      e.exports && (t = e.exports = U),
      (t.UAParser = U));
    var Y = typeof i !== f && (i.jQuery || i.Zepto);
    if (Y && !Y.ua) {
      var we = new U();
      ((Y.ua = we.getResult()),
        (Y.ua.get = function () {
          return we.getUA();
        }),
        (Y.ua.set = function (k) {
          we.setUA(k);
          var _ = we.getResult();
          for (var x in _) Y.ua[x] = _[x];
        }));
    }
  })(typeof window == "object" ? window : Yt);
})(Me, Me.exports);
var Zt = Me.exports;
const fe = navigator.language.includes("zh"),
  B = {
    userAgent: navigator.userAgent,
    deviceType: "",
    osName: "",
    browserName: "",
    timezone: "",
    language: navigator.language,
    hardwareConcurrency: navigator.hardwareConcurrency,
    webglInfo: {
      VENDOR: "",
      RENDERER: "",
      VERSION: "",
      SHADING_LANGUAGE_VERSION: "",
      UNMASKED_VENDOR_WEBGL: "",
      UNMASKED_RENDERER_WEBGL: "",
    },
  },
  Ue = new oe(0),
  Qt = {
    id: "1",
    name: "默认配置（全部随机-安全模式）",
    seed: 2,
    safeMode: !0,
    disableWorker: !1,
    antiDebugger: !1,
    userAgent: "auto",
    webrtc: "auto",
    location: { lng: -1, lat: -1 },
    timezone: "auto",
    language: "auto",
    factors: {
      audio: -1,
      canvas: -1,
      fonts: -1,
      plugins: -1,
      webgl: -1,
      webgpu: -1,
      voice: -1,
      clientRect: -1,
    },
    webglInfo: "auto",
    screen: {
      noise: -1,
      pixelDepth: -1,
      colorDepth: -1,
      maxTouchPoints: -1,
      dpr: 0,
      scheme: "auto",
    },
    memoryCapacity: -1,
    processors: -1,
  };
function bt() {
  return JSON.parse(JSON.stringify(Qt));
}
function ge(e) {
  let t = new Zt.UAParser(e).getResult(),
    i = {};
  return (
    ["browser", "os", "device", "cpu", "engine"].forEach((r) => {
      i[r] = t[r];
    }),
    (i.product = i.browser),
    delete i.browser,
    i
  );
}
function Vt() {
  var r, u, n, d;
  B.userAgent = navigator.userAgent;
  let e = ge(B.userAgent);
  if (
    ((B.deviceType = (r = e.device) == null ? void 0 : r.type),
    (B.browserName = (u = e.product) == null ? void 0 : u.name),
    (B.osName = ((n = e.os) == null ? void 0 : n.name) || ""),
    !B.deviceType)
  ) {
    let b = ((d = B.osName) == null ? void 0 : d.toLowerCase()) || "";
    ((b.startsWith("win") || b.includes("linux") || b.includes("mac")) &&
      (B.deviceType = "desktop"),
      (b.includes("ios") || b.includes("android")) &&
        (B.deviceType = "mobile"));
  }
  ((B.timezone = new Intl.DateTimeFormat().resolvedOptions().timeZone),
    (B.webglInfo = {}));
  const t = new OffscreenCanvas(1, 1),
    i = t.getContext("webgl") || t.getContext("webgl2");
  if (i) {
    Object.keys(WebGLRenderingContext).forEach((f) => {
      try {
        const m = WebGLRenderingContext[f];
        if (typeof m == "number") {
          const y = (i == null ? void 0 : i.getParameter(m)) || "";
          B.webglInfo[f] = y;
        }
      } catch {}
    });
    const b = i.getExtension("WEBGL_debug_renderer_info");
    Object.keys(b.__proto__).forEach((f) => {
      B.webglInfo[f] = i.getParameter(b[f]);
    });
  }
}
async function ei(e) {
  return await be.set("global-0", e);
}
async function F() {
  return await be.get("global-0");
}
async function ht() {
  return await be.remove("global-0");
}
async function ti(e, t) {
  let i = await Ne(e);
  if (!i) throw new Error(ui_t("Configuración de navegador no válida", "无效的浏览器配置", "invalid browserId"));
  return (await he(i), e);
}
async function ii() {
  await st();
}
async function ai() {
  let e = await F();
  return (e == null ? void 0 : e.id) || 0;
}
function pt() {
  return Ue.int(100, 1e5);
}
async function ri({ keyword: e, page: t = 1, size: i = 10 }, r) {
  var b;
  t -= 1;
  let u = [],
    n = await F();
  return (
    await H.iterWith("", (f, m) => {
      let { id: y, name: E } = m;
      if (n && n.id === y) {
        ((n = m), u.unshift(n));
        return;
      }
      if (e) {
        ((E || "") + "").includes(e) && u.push(m);
        return;
      }
      u.push(m);
    }),
    n && !e && n.id !== ((b = u[0]) == null ? void 0 : b.id) && u.unshift(n),
    i < 0
      ? { data: u, total: u.length }
      : { data: u.slice(t * i, t * i + i), total: u.length }
  );
}
async function oi(e, t) {
  if (e) return await H.remove(e);
}
async function mt(e) {
  if (e) return await H.get(e);
}
async function ni(e, t) {
  let {
    id: i,
    name: r,
    timezone: u,
    language: n,
    webrtc: d,
    proxy: b,
    userAgent: f,
    factors: m,
    screen: y,
  } = e;
  if (!r)
    throw new Error(
      ui_t("El nombre de la configuración es obligatorio", "浏览器名称不能为空", "browser's name required"),
    );
  let { height: E, width: o } = y || {};
  if (d && !P(d) && !_t(d))
    throw new Error(
      ui_t("La IP de WebRTC tiene un formato no válido", "webrtc ip格式不正确", "webrtc ip with invalid format"),
    );
  if (E && E < 0) throw new Error(ui_t("La altura de pantalla no puede ser menor que 0", "屏幕的高度不能小于0", "Screen height cannot be negative"));
  if (o && o < 0) throw new Error(ui_t("El ancho de pantalla no puede ser menor que 0", "屏幕的宽度不能小于0", "Screen width cannot be negative"));
  return (
    f && (e.uaInfo = ge(f)),
    i || (e.id = at()),
    (e.updateTime = Date.now()),
    await H.set(e.id, e),
    e.id
  );
}
const Z = { last: 0, data: void 0 },
  gt = {
    browserscan: async () => {
      let t = await (
        await fetch(
          "https://ip-scan.browserscan.net/sys/config/ip/get-visitor-ip?type=ip-api",
          { credentials: "omit", cache: "no-store" },
        )
      ).json();
      if (!t.data)
        throw new Error(ui_t("No se pudo obtener la IP actual", "获取当前代理ip信息失败", "Failed to get current IP"));
      let {
        ip: i,
        language: r,
        ip_data: { timezone: u, latitude: n, longitude: d },
      } = t.data;
      return { ip: i, language: r, timezone: u, longitude: d, latitude: n };
    },
    // [MEJORA] Ya no envía cookies (credentials: "include"), ni un
    // Accept-Language chino falso ni un referrer inventado.
    nexkits: async () => {
      let t = await (
          await fetch("https://nexkits.com/api/network/ip-geo", {
            headers: { accept: "*/*", "content-type": "application/json" },
            body: '{"ip":""}',
            method: "POST",
            mode: "cors",
            credentials: "omit",
            cache: "no-store",
          })
        ).json(),
        {
          ip: i,
          latitude: r,
          longitude: u,
          timezone: { id: n },
        } = t;
      return {
        ip: i,
        language: navigator.language,
        timezone: n,
        longitude: u,
        latitude: r,
      };
    },
    // [MEJORA] Tercer proveedor de respaldo (sin clave, sin cookies).
    ipwhois: async () => {
      let t = await (
        await fetch("https://ipwho.is/", { credentials: "omit", cache: "no-store" })
      ).json();
      if (!t || t.success === !1 || !t.ip) throw new Error("ipwho.is");
      return {
        ip: t.ip,
        language: navigator.language,
        timezone: (t.timezone && t.timezone.id) || "",
        longitude: t.longitude,
        latitude: t.latitude,
      };
    },
  },
  tt = Object.keys(gt);
// Datos "locales" cuando la consulta en línea está desactivada o falla:
// zona horaria e idioma reales del sistema, sin ubicación ni IP.
function localIpInfo() {
  return {
    ip: "",
    language: navigator.language,
    timezone: new Intl.DateTimeFormat().resolvedOptions().timeZone,
    longitude: 0,
    latitude: 0,
    local: !0,
  };
}
const IP_CACHE_MS = 10 * 60 * 1e3;
async function ve(force) {
  var i;
  const e = "ipGetters";
  let r = ((i = Z.data) == null ? void 0 : i.ip) || "";
  // [MEJORA] Opción para no consultar ningún servicio externo.
  if (!(await G("ipLookup", !0))) {
    Z.data = localIpInfo();
  } else {
    let cached;
    if (!force)
      try {
        cached = ((await W.storage.session.get("ipCache")) || {}).ipCache;
      } catch {}
    if (cached && Date.now() - cached.t < IP_CACHE_MS) Z.data = cached.d;
    else {
      let first = (await configTree.get(e)) || tt[0],
        order = [first, ...tt.filter((x) => x !== first)],
        ok = !1;
      for (let u of order) {
        if (!gt[u]) continue;
        try {
          ((Z.data = await gt[u]()), configTree.set(e, u).then(), (ok = !0));
          break;
        } catch (err) {
          console.warn("IP provider failed:", u, err);
        }
      }
      if (ok)
        try {
          await W.storage.session.set({ ipCache: { t: Date.now(), d: Z.data } });
        } catch {}
      else {
        Z.data || (Z.data = localIpInfo());
        throw new Error(
          ui_t(
            "No se pudo obtener la información de IP; revisa tu conexión. Se usan los datos locales.",
            "获取ip信息失败，请检查网络连接",
            "Failed to obtain IP information, please check network connection",
          ),
        );
      }
    }
  }
  if (r !== ((i = Z.data) == null ? void 0 : i.ip)) {
    let d = await F();
    d && (await xt(d), await he(d));
  }
  return Z.data;
}
// [MEJORA] Versión que nunca lanza error: antes, si no había internet al
// arrancar, la extensión no llegaba a activarse.
async function safeIp() {
  try {
    return await ve();
  } catch (err) {
    console.warn(err);
    return Z.data || localIpInfo();
  }
}
async function vt() {
  return Z.data || (await safeIp());
}
async function si() {
  let e = await pe();
  if (e === "无") {
    console.debug("option is no-ops");
    return;
  }
  if (e === "随机选取") {
    let t = await H.keys();
    return Ue.item([...t]);
  }
  if (e === "随机生成") return "randomGenerate";
  if (e === "静态") return S("fixedBrowserId");
}
const yt = async (e, t) => {
  var y, E, o, l;
  let i,
    r,
    u,
    n = {};
  (P(e.language) ? (i = t.language || "") : (i = e.language || ""),
    P(e.timezone) ? (r = t.timezone || "") : (r = e.timezone || ""),
    P(e.webrtc) ? (u = t.ip || "") : (u = e.webrtc || ""),
    P((y = e.location) == null ? void 0 : y.lat)
      ? (n.lat = t.latitude || 0)
      : (n.lat = ((E = e.location) == null ? void 0 : E.lat) || 0),
    P((o = e.location) == null ? void 0 : o.lng)
      ? (n.lng = t.longitude || 0)
      : (n.lng = ((l = e.location) == null ? void 0 : l.lng) || 0));
  let { languages: d, regions: b } = Q,
    f = await S("fixedLanguage");
  f !== void 0 &&
    (f !== "" && f / 1 === f / 1 ? (i = d[f].code) : f && (i = f + ""));
  let m = await S("fixedTimezone");
  return (
    m !== void 0 &&
      (m !== "" && m / 1 === m / 1 ? (r = b[m].timezone) : m && (r = m + "")),
    { language: i, timezone: r, webrtc: u, location: n }
  );
};
function it(e, t, i) {
  let r = !1;
  if (i.length)
    for (let { p: u, d: n } of i) {
      let d = e.replace(u, (b) => ((r = !0), b + n + t));
      if (r) return d;
    }
  return e + " v" + t;
}
const li = async (e) => {
  const t = new oe(e.seed || pt());
  let { languages: i, regions: r, devices: u } = Q,
    n = Z.data || (await safeIp()) || {},
    { language: d, timezone: b, webrtc: f, location: m } = await yt(e, n),
    y = e.safeMode,
    E = e.disableWorker,
    o = e.functionTrace,
    l = e.antiDebugger,
    a = e.deviceType || B.deviceType,
    s = e.userAgent || "",
    rl = e.realistic !== void 0 ? e.realistic : await G("realistic", !0);
  if (P(s))
    if (y)
      s = it(navigator.userAgent, t.int(11111, 99999), [
        { p: /Chrome\/(\d+)(\.\d+)+/, d: "." },
        { p: /Edg\/(\d+)(\.\d+)+/, d: "." },
        { p: /Safari\/(\d+)(\.\d+)+/, d: "." },
        { p: /Firefox\/(\d+)(\.\d+)+/, d: "." },
        { p: /Version\/(\d+)(\.\d+)+/, d: "." },
        { p: /(_\d)+/, d: "_" },
      ]);
    else {
      let O = B.osName,
        A = B.browserName;
      (a !== B.deviceType && ((O = ""), (A = "")),
        e.os && (O = e.os),
        a || (a = t.item(["desktop", "tablet", "mobile"])),
        O ||
          (a === "desktop"
            ? (O = t.item(["macos", "windows", "linux"]))
            : (O = t.item(["ios", "android"]))),
        A || (A = t.item(["Chrome", "Safari", "Firefox", "Edge"])));
      if (rl) s = realUA(O, A, a, t);
      else {
        let L = (u[a] || {})[O] || [],
          $ = t.item(L);
        s = Ot({ osName: O, browserName: A, deviceName: $ }, t);
      }
    }
  let c = await S("fixedUA");
  c && (s = c);
  let p = e.uaInfo || ge(s || navigator.userAgent);
  a = p.device.type;
  let h = e.screen || {},
    ok_ = osKey((p.os && p.os.name) || B.osName),
    hw = rl ? realHW(ok_, a, t) : null;
  // [MEJORA] Valores coherentes (antes: profundidad de color de 4 bits,
  // pixelDepth distinto de colorDepth, DPR como 1.37, etc.).
  hw &&
    (P(h.dpr) && (h.dpr = hw.dpr),
    P(h.maxTouchPoints) && (h.maxTouchPoints = hw.maxTouchPoints),
    P(h.colorDepth) && (h.colorDepth = hw.colorDepth),
    P(h.pixelDepth) && (h.pixelDepth = h.colorDepth));
  (P(h.noise) &&
    ((h.noise = t.int(0, 5e3) / 1e3), (h.height = 0), (h.width = 0)),
    P(h.dpr) && (h.dpr = t.int(100, 300) / 100),
    P(h.maxTouchPoints) &&
      (a === "desktop"
        ? (h.maxTouchPoints = 0)
        : a === "mobile" || a === "tablet"
          ? (h.maxTouchPoints = t.int(2, 10))
          : (h.maxTouchPoints = 0)),
    P(h.colorDepth) && (h.colorDepth = t.item([4, 6, 8, 10, 16, 24])),
    P(h.pixelDepth) && (h.pixelDepth = t.item([4, 6, 8, 10, 16, 24])),
    P(h.scheme) && (h.scheme = t.item(["no-preference", "light", "dark"])));
  const w = {};
  if (P(e.webglInfo))
    if (e.webglSafeMode) {
      w.UNMASKED_VENDOR_WEBGL = B.webglInfo.UNMASKED_VENDOR_WEBGL;
      const O = t.int(1, 1e4);
      let A = B.webglInfo.UNMASKED_RENDERER_WEBGL;
      w.UNMASKED_RENDERER_WEBGL = it(A, O, [
        { p: /(\d+)\.(\d+)/, d: "." },
        { p: /(_\d)+/, d: "_" },
      ]);
    } else {
      // [MEJORA] En modo realista, la GPU simulada corresponde al sistema
      // operativo simulado (sin GPU de Windows en un Mac, sin SwiftShader).
      let pool = Q.webglInfos.filter((x) => !x._added);
      if (rl) {
        let fp = Q.webglInfos.filter((x) => (x._os || []).includes(ok_));
        fp.length && (pool = fp);
      }
      const O = t.item(pool);
      for (let A of Object.keys(O)) {
        if (A[0] === "_") continue;
        const L = O[A];
        L instanceof Array ? (w[A] = t.item(L)) : (w[A] = L);
      }
    }
  let g = {};
  rt.flat().forEach((O) => {
    let A = e.factors || {};
    A[O] && (g[O] = P(A[O]) ? t.int(0, 5e3) / 1e3 : A[O] || 0);
  });
  let M = await dt();
  M.driver = 0;
  let T = await De(),
    D = {
      ...e,
      id: e.id,
      name: e.name,
      safeMode: y,
      disableWorker: E,
      functionTrace: o,
      antiDebugger: l,
      userAgent: s,
      enables: M,
      webglInfo: w,
      customProtos: e.customProtos || [],
      customVars: e.customVars || [],
      webrtc: f,
      timezone: b,
      language: d,
      location: m,
      factors: g,
      screen: h,
      memoryCapacity: P(e.memoryCapacity)
        ? hw
          ? hw.memory
          : t.item([0.25, 0.5, 1, 2, 4, 8])
        : e.memoryCapacity || 0,
      processors: P(e.processors)
        ? hw
          ? hw.processors
          : t.item([1, 2, 4, 8, 16])
        : e.processors || 0,
      uaInfo: p,
      iceServers: T,
      javaEnabled: e.javaEnabled,
      doNotTrack: e.doNotTrack,
      hideFlash: e.hideFlash,
    };
  return (
    (a === "mobile" || a === "tablet") &&
      ["ontouchstart", "ontouchend", "ontouchmove", "ontouchcancel"].forEach(
        (A) => {
          (D.customVars.push({
            path: "HTMLElement.prototype." + A,
            value: null,
          }),
            D.customVars.push({ path: "window." + A, value: null }));
        },
      ),
    D
  );
};
async function Ne(e) {
  if (!e) return null;
  const t = pt() | 0;
  let i;
  return e === "randomGenerate"
    ? ((i = bt()),
      (i.id = e),
      (i.seed = t),
      (i.name = ui_t("Perfil aleatorio temporal", "随机生成临时配置", "Random Temp Browser") + ":" + t),
      (i.safeMode = await G("safeMode", !0)),
      (i.webglSafeMode = await G("webglSafeMode", !1)),
      (i.disableWorker = await G("disableWorker")),
      (i.antiDebugger = await G("antiDebugger")),
      (i.deviceType = await S("deviceType")),
      (i.os = await S("os")),
      (i.isTemp = !0),
      await li(i))
    : ((i = await mt(e)), await xt(i), i);
}
async function Be(e = !0) {
  let t = await si();
  if (!t) {
    await st();
    return;
  }
  let i = await Ne(t);
  if (i)
    return (
      await he(i),
      console.log("Random switching browser,seed=" + i.seed),
      i
    );
}
function P(e) {
  return e === "auto" || e === "dynamic" || e === "-1" || e === -1;
}
async function xt(e) {
  let t = await vt();
  const i = { ...bt(), ...e };
  let { language: r, timezone: u, webrtc: n, location: d } = await yt(i, t),
    b = e.userAgent || "",
    f = await S("fixedUA");
  (f && (b = f),
    (e.userAgent = b),
    (e.language = r),
    (e.timezone = u),
    (e.webrtc = n),
    (e.location = d));
}
async function Ce(e = "") {
  let t = await me();
  if (t) {
    let i = await X();
    if (t === "white" && !i.white.includes(e))
      return (console.debug(e + " 不在白名单"), !1);
    if (t === "black" && i.black.includes(e))
      return (console.debug(e + " 在黑名单"), !1);
  }
  return !0;
}
const ci = self.chrome || self.browser,
  ui = {
    beat: () => 1,
    uaParse: ge,
    selectBrowsers: ri,
    refreshIpInfo: () => ve(!0),
    getIpInfo: vt,
    delGlobalBrowser: ht,
    setBrowser: ni,
    getBrowser: mt,
    delBrowser: oi,
    randomBrowser: Be,
    obtainBrowser: Ne,
    setEnable: ti,
    removeEnable: ii,
    getEnable: ai,
    clearBrowsingData: async (e, t) => {
      await ci.browsingData.remove(
        { since: 0, originTypes: { unprotectedWeb: !0 } },
        {
          appcache: !0,
          history: !0,
          cacheStorage: !0,
          cookies: !0,
          formData: !0,
          indexedDB: !0,
          localStorage: !0,
          pluginData: !0,
          serviceWorkers: !0,
          webSQL: !0,
        },
      );
    },
    getConfig: (e) => S(e.key),
    setConfig: ct,
    delConfig: Pt,
    configs: Dt,
    delHost: jt,
    getHostTable: X,
    setHostEnable: zt,
    addHost: Nt,
    getDevice: Pe,
    setDevice: ut,
    getOption: pe,
    setOption: (...e) => wt(...e).then(() => Be(...e)),
    getProxies: qt,
    delProxy: Wt,
    addProxy: Gt,
    getMatches: Ft,
    delMatch: $t,
    addMatch: Ht,
    importData: Jt,
    exportData: Xt,
  },
  q = self.chrome || self.browser;
async function di(e) {
  let t = await q.windows.getCurrent(),
    i = {
      id: e,
      host: "",
      port: 0,
      scheme: "",
      tabId: e,
      windowId: t.id,
      frameId: 0,
      email: "",
      ua: navigator.userAgent,
      ip: "127.0.0.1",
    };
  if (e <= 0) return i;
  let r = await q.tabs.get(e);
  if (!r) return;
  let { url: u, windowId: n } = r,
    { host: d, port: b = 0, protocol: f } = new URL(u);
  return { ...i, host: d, port: b, scheme: f, tabId: e, windowId: n };
}
async function Oe(e, t) {
  t
    ? await Promise.all([
        q.action.setTitle({ title: t.name, tabId: e }),
        q.action.setBadgeBackgroundColor({ color: "#84fc7a", tabId: e }),
        q.action.setBadgeText({ text: "ON", tabId: e }),
      ])
    : await Promise.all([
        q.action.setTitle({ title: "", tabId: e }),
        q.action.setBadgeBackgroundColor({
          color: "rgba(255,255,255,0)",
          tabId: e,
        }),
        q.action.setBadgeText({ text: "", tabId: e }),
      ]);
}
const N = self.chrome || self.browser;
function Ie(e, t) {
  N.notifications.create({
    type: "basic",
    iconUrl: "/assets/logo.png",
    title: e,
    message: t || "",
  });
}
(async () => {
  if (!N.userScripts) {
    Ie(
      "Error",
      ui_t(
        "Activa el «Modo de desarrollador» en chrome://extensions y permite «Scripts de usuario» para esta extensión.",
        "请在扩展管理中，打开开发者模式,并允许插件执行用户脚本",
        "Enable Developer mode in chrome://extensions and allow user scripts for this extension.",
      ),
    );
    return;
  }
  (await Mt(), await safeIp(), Vt());
  const e = await S("autoSwitch");
  let t = await F();
  // [MEJORA] Chrome reinicia el service worker a menudo. Antes, con
  // "al abrir el navegador", el perfil cambiaba en cada reinicio interno.
  let firstRun = !0;
  try {
    firstRun = !((await N.storage.session.get("booted")) || {}).booted;
    await N.storage.session.set({ booted: !0 });
  } catch {}
  (!t || (e === "openBrowser" && firstRun)) && (t = await Be());
  async function i() {
    await N.scripting.unregisterContentScripts();
  }
  (await i(),
    await N.scripting.registerContentScripts([
      {
        id: "api",
        js: ["content-scripts/api.js"],
        matches: ["*://*/*"],
        allFrames: !0,
        runAt: "document_start",
        world: "ISOLATED",
      },
      {
        id: "window",
        js: ["content-scripts/window.js"],
        matches: ["*://*/*"],
        allFrames: !0,
        runAt: "document_start",
        world: "MAIN",
      },
    ]),
    await (async () => {
      // [MEJORA] Antes mostraba una notificación cada vez que Chrome
      // despertaba el service worker. Ahora solo una vez por sesión.
      let shown = !1;
      try {
        shown = !!((await N.storage.session.get("notified")) || {}).notified;
        await N.storage.session.set({ notified: !0 });
      } catch {}
      t && console.log("injected", t);
      if (shown || !(await G("notify", !0))) return;
      t
        ? Ie(ui_t("Protección activa", "Injected", "Injected"), t.name)
        : Ie(ui_t("Iniciada", "Started", "Started"), "");
    })(),
    setInterval(() => {
      ct({ key: "beat", value: Date.now() }).then(() => {
        console.log("beat...");
      });
    }, 1e3 * 15),
    N.windows.onRemoved.addListener(function () {}));
  async function r(n, d, b) {
    let f = ui[n];
    if (!f) throw new Error(ui_t("Acción desconocida: ", "路径不存在,key=", "Unknown action: ") + n);
    try {
      let m = f(d, b);
      return (m instanceof Promise && (m = await m), { code: 200, data: m });
    } catch (m) {
      return (console.error(m), { code: 500, msg: m.message });
    }
  }
  (N.runtime.onConnect.addListener((n) => {
    n.onMessage.addListener(async (d) => {
      var E, o;
      let b =
        ((o = (E = n.sender) == null ? void 0 : E.tab) == null
          ? void 0
          : o.id) ||
        d.tabId ||
        0;
      if (!b) {
        let [l] = await N.tabs.query({ active: !0, currentWindow: !0 });
        b = (l == null ? void 0 : l.id) || 0;
      }
      if (!b)
        return (
          n.postMessage({
            code: 500,
            msg: ui_t("¡Recarga la página primero!", "请先刷新页面!", "Please reload the page first!"),
            requestId: d.requestId,
          }),
          !0
        );
      const f = di(b);
      let m = d.key,
        y = d.data;
      f.then(async (l) => {
        let a = await r(m, y, l);
        return (
          console.log(`request==${b}==${m}`, d, a),
          n.postMessage({ ...a, requestId: d.requestId }),
          !0
        );
      });
    });
  }),
    N.tabs.onActivated.addListener(function () {}));
  function u(n) {
    return !n.url || ("frameType" in n && n.frameType !== "outermost_frame")
      ? !1
      : n.url.toString().startsWith("http");
  }
  (N.webNavigation.onCreatedNavigationTarget.addListener(async (n) => {
    if (!u(n) || !(await Ce(new URL(n.url).host))) return;
    const d = await F();
    await Oe(n.tabId || n.sourceTabId, d);
  }),
    N.webNavigation.onBeforeNavigate.addListener(async (n) => {
      if (!u(n) || !(await Ce(new URL(n.url).host))) return;
      const d = await F();
      await Oe(n.tabId, d);
    }),
    N.webNavigation.onCommitted.addListener(async (n) => {
      if (!u(n) || !(await Ce(new URL(n.url).host))) return;
      const d = await F();
      await Oe(n.tabId, d);
    }));
})();

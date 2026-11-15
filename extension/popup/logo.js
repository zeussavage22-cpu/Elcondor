import { v as K } from "./popup.js";
const h = "/assets/icon/edge.png",
  w = "/assets/icon/chrome.png",
  B = "/assets/icon/firefox.png",
  Q = "/assets/icon/safari.png",
  U = "/assets/icon/360Browser.png",
  I = "/assets/icon/baidu.png",
  m = "/assets/icon/qq.png",
  H = "/assets/icon/uc.png",
  S =
    "data:image/jpeg;base64,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",
  q =
    "data:image/jpeg;base64,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",
  E = "/assets/icon/default.png",
  b = self.chrome || self.browser,
  Y = location.host.startsWith("localhost") ? "dev" : "",
  s = { port: void 0, times: 0 };
function y() {
  if (s.port) return s.port;
  const e = b.runtime.connect({ name: "popup_channel" });
  return (
    e.onMessage.addListener((t) => {
      let n = t.requestId,
        a = s[n];
      if ((delete s[n], !!a)) return C(t, a.resole, a.reject);
    }),
    e.onDisconnect.addListener(() => {
      s.port = void 0;
    }),
    (s.port = e),
    e
  );
}
setInterval(() => {
  for (let [e, t] of Object.entries(s))
    e === "port" ||
      e === "times" ||
      (Date.now() - t.start > 5e3 &&
        (delete s[e], C({ code: 500, msg: "Timeout!" }, t.resole, t.reject)));
}, 1e3);
function x(e = 1e3) {
  return new Promise((t) => setTimeout(t, e));
}
async function R(e, t) {
  return new Promise(async (n, a) => {
    var f, d;
    if (Y === "dev") return n({ code: 200, data: void 0 });
    let i = Date.now(),
      A = i + "-" + s.times++;
    s[A] = { start: i, resole: n, reject: a };
    let l = await ((f = b.tabs) == null
        ? void 0
        : f.query({ active: !0, currentWindow: !0 })),
      u = 0;
    l && (u = ((d = l[0]) == null ? void 0 : d.id) || 0);
    for (let g = 0; g < 3; g++) {
      try {
        y().postMessage({ requestId: A, key: e, data: t, start: i, tabId: u });
        break;
      } catch (p) {
        p.message.toString().includes("Could not establish connection") &&
          (s.port = void 0);
      }
      await x(300);
    }
  });
}
function C(e, t, n) {
  return e.code !== 200 ? n(e.msg) : t(e.data);
}
const r = {
  chrome: w,
  edge: h,
  firefox: B,
  safari: Q,
  b360: U,
  baidu: I,
  qq: m,
  uc: H,
  opera: S,
  brave: q,
  bDefault: E,
};
function O(e = "") {
  if (e) {
    let t = e.toLowerCase();
    if (t.includes("edg") || t.includes("edge")) return r.edge;
    if (t.includes("firefox")) return r.firefox;
    if (t.includes("chrome")) return r.chrome;
    if (t.includes("safari")) return r.safari;
    if (t.includes("360")) return r.b360;
    if (t.includes("baidu")) return r.baidu;
    if (t.includes("qq")) return r.qq;
    if (t.includes("uc")) return r.uc;
    if (t.includes("opera")) return r.opera;
    if (t.includes("brave")) return r.brave;
  }
  return r.bDefault;
}
function o() {
  return R(...arguments).catch((e) => {
    throw (K.error(e), e);
  });
}
function D(e) {
  return o("uaParse", e);
}
function k(e) {
  return o("setEnable", e);
}
function j() {
  return o("removeEnable");
}
function V() {
  return o("getEnable");
}
function G() {
  return o("refreshIpInfo");
}
function N() {
  return o("clearBrowsingData");
}
function T() {
  return o("getIpInfo");
}
function v(e) {
  return o("delBrowser", e);
}
function F(e) {
  return o("getBrowser", e);
}
function Z(e) {
  return o("setBrowser", e);
}
function L(e, t) {
  return o("getMatches", { page: e, size: t });
}
function W(e, t) {
  return o("getProxies", { page: e, size: t });
}
function X(e) {
  return o("setOption", e);
}
function J(e, t) {
  return o("addHost", { host: e, isWhite: t });
}
function z(e, t) {
  return o("delHost", { host: e, isWhite: t });
}
function _() {
  return o("getHostTable");
}
async function $(e, t) {
  const n = document.createElement("a");
  ((n.href = e),
    (n.download = t),
    (n.style.display = "none"),
    document.body.appendChild(n),
    n.click(),
    document.body.removeChild(n));
}
function ee() {
  return o("randomBrowser");
}
function te(e) {
  return o("obtainBrowser", e);
}
function oe(e, t = 1, n = -1) {
  return o("selectBrowsers", { keyword: e, page: t, size: n });
}
function ne() {
  return o("configs");
}
function re(e, t) {
  return o("setConfig", { key: e, value: t });
}
function se(e = {}) {
  return o("importData", e);
}
function ae() {
  return o("exportData");
}
const ce = {
    canvas: { type: "danger", color: "#c45656" },
    audio: { type: "success", color: "#529b2e" },
    webgl: { type: "warning", color: "#b88230" },
    plugins: { type: "primary", color: "#337ecc" },
    fonts: { type: "info", color: "#73767a" },
    webgpu: { type: "info", color: "#77a7e7" },
    clientRect: { type: "info", color: "#5050f4" },
    voice: { type: "info", color: "#8d1c51" },
  },
  M = (self.chrome || {}).storage
    ? [
        ["canvas", "audio", "webgl", "webgpu"],
        ["voice", "plugins", "fonts", "clientRect"],
      ]
    : [
        ["canvas", "audio", "webgpu", "webgl", "voice"],
        ["clientRect", "plugins", "fonts", "ja3", "h2"],
      ],
  ie = [
    { label: self.__lbl("Canvas(绘图)"), key: "canvas" },
    { label: self.__lbl("Audio(声音)"), key: "audio" },
    { label: self.__lbl("Webgl(3D绘图)"), key: "webgl" },
    { label: self.__lbl("WebGPU(GPU)"), key: "webgpu" },
    { label: self.__lbl("Plugins(插件)"), key: "plugins" },
    { label: self.__lbl("Fonts(字体)"), key: "fonts" },
    { label: self.__lbl("ClientRect(视口)"), key: "clientRect" },
    { label: self.__lbl("Voice(语音)"), key: "voice" },
    { label: "Ja3(Tls)", key: "ja3" },
    { label: "h2(Http2)", key: "h2" },
  ].filter((e) => M.flat().includes(e.key)),
  Ae = [
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
let c;
try {
  c = 1 / 0; // [MEJORA] sin fecha de caducidad
} catch {
  c = 1 / 0;
}
const le = "/assets/logo.png";
export {
  ae as A,
  $ as B,
  X as C,
  J as D,
  z as E,
  T as a,
  E as b,
  V as c,
  v as d,
  oe as e,
  M as f,
  O as g,
  G as h,
  ee as i,
  N as j,
  ce as k,
  le as l,
  ie as m,
  Z as n,
  te as o,
  Ae as p,
  ne as q,
  j as r,
  k as s,
  W as t,
  L as u,
  _ as v,
  F as w,
  D as x,
  re as y,
  se as z,
};

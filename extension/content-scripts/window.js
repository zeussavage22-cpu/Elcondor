(() => {
  "use strict";
  var e = {};
  function t(e) {
    let t,
      n,
      r = 0;
    if (!e || 0 === e.length) return r;
    for (t = 0; t < e.length; t++)
      ((n = e.charCodeAt(t)), (r = (r << 5) - r + n), (r |= 0));
    return r;
  }
  e.g = (function () {
    if ("object" == typeof globalThis) return globalThis;
    try {
      return this || new Function("return this")();
    } catch (e) {
      if ("object" == typeof window) return window;
    }
  })();
  const n = (e, t, n, r = 1, i = []) => {
    if (t === n) return !1;
    if (t < 0 || n < 0 || t + r > e.length || n + r > e.length) return !1;
    for (let o = 0; o < r; o++) {
      let r = e[t + o];
      if (i.includes(r)) return !1;
      if (0 === r) return;
      ((e[t + o] = e[n + o]), (e[n + o] = r));
    }
    return !0;
  };
  function r(e) {
    return null == e;
  }
  class i {
    constructor(e = 0) {
      this.setSeed(e);
    }
    setSeed(e = 0) {
      (0 | e || (e *= 1e4),
        (this.seed = e),
        (this.rand = (function (e = 0) {
          return function () {
            let t = (e += 1831565813);
            return (
              (t = Math.imul(t ^ (t >>> 15), 1 | t)),
              (t ^= t + Math.imul(t ^ (t >>> 7), 61 | t)),
              ((t ^ (t >>> 14)) >>> 0) / 4294967296
            );
          };
        })(e)));
    }
    int(e, t) {
      const n = this.rand() * (t - e) * 1e6;
      return (Math.abs(0 | n) % (t - e)) + e;
    }
    item(e = []) {
      return e[this.int(0, e.length)];
    }
    str(
      e = 16,
      t = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789",
    ) {
      let n = "";
      const r = t.length;
      for (let i = 0; i < e; i++) {
        const e = this.int(0, r);
        n += t.charAt(e);
      }
      return n;
    }
  }
  let o = new Date(),
    s = o.getFullYear() + ":" + o.getMonth() + ":" + o.getDate();
  const a = {
      guid: function () {
        function e() {
          return ((65536 * (1 + Math.random())) | 0).toString(16).substring(1);
        }
        return (
          e() + e() + "-" + e() + "-" + e() + "-" + e() + "-" + e() + e() + e()
        );
      },
      ipv4ToIpv6: function (e) {
        if (!e) return "";
        let t = "",
          n = e.split(".");
        for (let e = 0; e < n.length; e++) {
          let r = parseInt(n[e]).toString(16);
          (1 === r.length && (r = "0" + r), (t += r), e % 2 != 0 && (t += ":"));
        }
        return "0:0:0:0:0:ffff:" + t;
      },
      randomSwap: function (
        e,
        t,
        r = { times: 10, unit: 2, loopTimes: 50, skips: [] },
      ) {
        if (!t) return;
        if (t.length <= r.times * r.unit * 2) return;
        let o = (((t.length / (2 * r.times)) | 0) / r.unit) | 0,
          s = new i(e),
          a = 0;
        for (let e = 0; e < r.loopTimes; e++) {
          let i = e * r.times * 2 + s.int(0, o) * r.unit * 2;
          if ((n(t, i, i + r.unit, r.unit, r.skips) && a++, a >= r.times))
            break;
        }
        return a;
      },
      randomNoise: function (e, t, n = [0]) {
        if (!t) return 0;
        let r = new i(e),
          o = r.int(1, 5e3) / 1e3,
          s = 0;
        for (let e = 1; e < t.length - 1; e++)
          n.includes(t[e]) ||
            ((t[e] += o), (e += r.int(1, (t.length / 300) | 0)), s++);
        return s;
      },
      randomPeek: function (e, t, n, r, o = 50) {
        if (!t) return;
        r = r || Math.min((t.length / 500) | 0, 8) || 1;
        let s = new i(e).int(1, 100),
          a = 0;
        for (let e = 0; e < t.length && !(a >= o); )
          if (t[e]) {
            if (e % s <= 3) {
              let r = n(t[e], e);
              void 0 !== r && ((t[e] = r), a++);
            }
            e += r;
          } else e += r;
        return a;
      },
      fakeName: function (e, n = "", r = 0) {
        if (
          (("SCOPE_CHEATER" !== e &&
            "SCOPE_BROWSER" !== e &&
            "CTHULHUJS_API" !== e &&
            "attach" !== e) ||
            (r = s),
          !r)
        )
          return n;
        let o = t(e + r);
        return new i(o).str(32);
      },
      randomReverse: function (e, t, n, r = []) {
        if (!t) return;
        let o = 0,
          s = (t.length / n) | 0,
          a = (n / 2) | 0,
          l = new i(e),
          u = l.int(3, 13);
        for (let e = 0; e < s; e++) {
          let i = e * n;
          if (i >= t.length) break;
          if (e % u <= 1)
            for (let e = 0; e < a; e++) {
              let s = i + e,
                a = i + (n - 1 - e);
              if (r.filter((e) => e === t[s] || e === t[a]).length) continue;
              let l = t[s];
              ((t[s] = t[a]), (t[a] = l), o++);
            }
          e += l.int(1, 10);
        }
        return o;
      },
      Random: i,
      hashcode: t,
      arrayHashcode: function (e) {
        let t,
          n,
          r = 0;
        if (!e || 0 === e.length) return r;
        for (t = 0; t < e.length; t++)
          ((n = e[t]), (r = (r << 5) - r + n), (r |= 0));
        return r;
      },
      getStyle: function (e, t) {
        return window.getComputedStyle
          ? getComputedStyle(e, null)[t]
          : e.currentStyle[t];
      },
      formatString: function (e, t) {
        return (e += "").length > t ? e.slice(0, t - 3) + "..." : e.padEnd(t);
      },
      getValue: function (e, t) {
        let n = t.split(/[.\[\]]/).filter((e) => "" !== e),
          r = e;
        for (let e of n) {
          if (void 0 === r[e]) return;
          r = r[e];
        }
        return r;
      },
      deleteValue: function (e, t) {
        let n = t.split(/[.\[\]]/).filter((e) => "" !== e),
          r = e;
        for (let e = 0; e < n.length - 1; e++)
          if (((r = r[n[e]]), void 0 === r)) return;
        return delete r[n[n.length - 1]];
      },
      swap: n,
      isNullify: r,
      isEmpty: function (e) {
        if (r(e)) return !0;
        if (e && !r(e.length)) return 0 === e.length;
        if (0 === e || 0 === e || "" === e) return !0;
        for (const t in e) return !1;
        return !0;
      },
      toDataURL: (e, t) => `'data:${t};base64,${btoa(e)}`,
      getGlobal: function () {
        try {
          return self;
        } catch (e) {}
        try {
          return e.g;
        } catch (e) {}
        try {
          return globalThis;
        } catch (e) {}
      },
    },
    l = {
      native: "__nativeCheat__",
      remove: "__removeCheat__",
      add: "__addCheat__",
      window: "__windowCheat__",
      trace: "__traceCheat__",
      proxyId: "__proxyIdCheat__",
      isProxy: "__isProxyCheat__",
      origin: "__originCheat__",
      create: "__createCheat__",
      attach: "__attachCheat__",
      fake: "__fakeCheat__",
      cheaters: "__cheatersCheat__",
      SCOPE_BROWSER: "SCOPE_BROWSER",
      SCOPE_CHEATER: "SCOPE_CHEATER",
      CTHULHUJS_API: "CTHULHUJS_API",
      BROWSER_GENERATOR: "BROWSER_GENERATOR",
    };
  function u(e = 0) {
    let t = "";
    for (let n of Object.keys(l))
      ((l[n] = a.fakeName(n, l[n], e)), (t += n + ": " + l[n] + "\n"));
    console.log("browser-" + e, t);
  }
  const c = {
    Chrome: "Google",
    Edge: "Microsoft",
    IE: "Microsoft",
    Firefox: "Mozilla",
    Safari: "Apple",
    QQ: "Tencent",
  };
  class d {
    constructor() {
      this.value = "";
    }
    get get() {}
    set set(e) {}
    func() {}
  }
  const m = a.getGlobal(),
    h = sessionStorage.getItem,
    f = sessionStorage.setItem,
    p = sessionStorage.removeItem,
    g = Array.prototype.push,
    y = Set.prototype.has,
    b = m.eval,
    w = console.log,
    S = m.constructor.name.replace("GlobalScope", "");
  function v(e) {
    const t = {};
    let n = {
      isFirst: !0,
      errors: [],
      warns: [],
      debugs: t,
      logs: new Set(),
      proxies: [],
      cheaters: [],
      addDebugger: (n, r) => {
        let i = "DEBUG:" + n;
        e.sessionStorage ? A(f, e.sessionStorage, [i, r]) : (t[i] = r);
      },
      removeDebugger: (n) => {
        let r = "DEBUG:" + n;
        e.sessionStorage ? A(p, e.sessionStorage, [r]) : delete t[r];
      },
      isDebugger: (t) => {
        let n = "DEBUG:" + t;
        if (!e.sessionStorage) return !1;
        let r = A(h, e.sessionStorage, [n]);
        if (!r) return !1;
        let i = r / 1 || 0;
        return i > 0 && (A(f, e.sessionStorage, [n, i - 1 + ""]), !0);
      },
    };
    return ((e[l.attach] = n), n);
  }
  const k = m.Object;
  m.Date;
  let E = 0;
  function C(e, t, n, r) {
    (A(y, m[l.attach].logs, [e]) && A(w, console, [t, r]),
      m[l.attach].isDebugger(e) && A(b, m, ["debugger"]));
    try {
      return n();
    } catch (e) {
      throw (A(g, m[l.attach].errors, [e]), e);
    }
  }
  function D(e, t, n, r = { skipAbsent: !1 }) {
    let i = k.getOwnPropertyDescriptor(e, t);
    const o =
        ("function" == typeof e ? e.name : e.constructor?.name) || "<unkonw>",
      s = m[l.attach];
    if (!i) {
      if (r.skipAbsent) return;
      (s.warns.push(`${S}: ${t} descriptor not in ${o}`),
        (i = k.getOwnPropertyDescriptor(d.prototype, "get")));
    }
    const a = `!!!${S}: 浏览器正在获取 ${o}.${t}`,
      u = `${o}.${t}`;
    let c = i.get;
    return (
      s.isFirst && s.proxies.push(`Getter:${u}`),
      i.get[l.native] ||
        ((i.get = k.getOwnPropertyDescriptor(
          class {
            get fn() {
              let e = this,
                t = arguments;
              return C(u, a, () => n(c, e, t), t);
            }
          }.prototype,
          "fn",
        ).get),
        k.defineProperty(i.get, "name", { value: "get " + t }),
        (i.get[l.origin] = c),
        (i.get[l.proxyId] = t + "_" + E++),
        (i.get[l.native] = 2),
        k.defineProperty(e, t, { ...i })),
      i.get
    );
  }
  function T(e, t, n, r = { skipAbsent: !1 }) {
    let i = k.getOwnPropertyDescriptor(e, t);
    const o =
        ("function" == typeof e ? e.name : e.constructor?.name) || "<unkonw>",
      s = m[l.attach];
    if (!i) {
      if (r.skipAbsent)
        return void s.warns.push(`${S}: ${t} descriptor not in ${o}`);
      i = k.getOwnPropertyDescriptor(d.prototype, "set");
    }
    const a = `!!!${S}: 浏览器正在设置 ${o}.${t}`,
      u = `${o}.${t}`;
    let c = i.set;
    return (
      s.isFirst && s.proxies.push(`Setter:${u}`),
      i.set[l.native] ||
        ((i.set = k.getOwnPropertyDescriptor(
          class {
            set fn(e) {
              let t = this,
                r = arguments;
              return C(u, a, () => n(c, t, r), r);
            }
          }.prototype,
          "fn",
        ).set),
        k.defineProperty(i.set, "name", { value: "set " + t }),
        (i.set[l.proxyId] = t + "_" + E++),
        (i.set[l.native] = 3),
        k.defineProperty(e, t, { ...i })),
      i.set
    );
  }
  function O(e, t, n, r = { skipAbsent: !1 }) {
    let i = k.getOwnPropertyDescriptor(e, t);
    const o =
        ("function" == typeof e ? e.name : e.constructor?.name) || "<unkonw>",
      s = m[l.attach];
    if (!i) {
      if (r.skipAbsent)
        return void s.warns.push(`${S}: ${t} descriptor not in ${o}`);
      i = k.getOwnPropertyDescriptor(d.prototype, "func");
    }
    const a = `!!!${S}: 浏览器正在调用 ${o}.${t}`,
      u = `${o}.${t}`;
    let c = i.value;
    return (
      s.isFirst && s.proxies.push(`Function:${u}`),
      i.value[l.native] ||
        ((i.value = k.getOwnPropertyDescriptor(
          class {
            fn() {
              let e = this,
                t = arguments;
              return C(u, a, () => n(c, e, t), t);
            }
          }.prototype,
          "fn",
        ).value),
        k.defineProperty(i.value, "name", { value: t }),
        i.value.length !== c.length &&
          k.defineProperty(i.value, "length", { value: c.length }),
        (i.value[l.proxyId] = t + "_" + E++),
        (i.value[l.native] = 1),
        (i.value[l.origin] = c),
        k.defineProperty(e, t, { ...i, writable: !0 })),
      i.value
    );
  }
  const M = Function.prototype.call;
  function A(e, t, n) {
    return ((M.call = M), M.call(e, t, ...n));
  }
  function x(e, t, n, r = { skipAbsent: !1 }) {
    let i = e[t];
    const o =
        ("function" == typeof e ? e.name : e.constructor?.name) || "<unkonw>",
      s = m[l.attach];
    if (!i) {
      if (r.skipAbsent)
        return void s.warns.push(`${S}: ${t} descriptor not in ${o}`);
      i = d;
    }
    const a = `!!!${S}: 浏览器正在构建 ${o}.${t}`,
      u = `${o}.${t}`;
    function c(e, t, r) {
      return C(u, a, () => n(e, t, r), t);
    }
    (s.isFirst && s.proxies.push(`Construct:${u}`),
      (e[t] = new Proxy(i, {
        get: (e, t, n) =>
          "constructor" === t || t === l.origin ? i : Reflect.get(e, t, n),
        construct: (e, t, n) => c(e, t, n),
      })),
      O(i, "constructor", c),
      (e[t][l.proxyId] = t + "_" + E++),
      (e[t][l.native] = 1));
  }
  function I(e, t, n) {
    try {
      if (e[t] === n) return;
      if (((e[t] = n), e[t] === n)) return;
    } catch (r) {
      return void D(k.getPrototypeOf(e), t, () => n);
    }
    let r = k.getOwnPropertyDescriptor(e, t);
    if ((r || (r = k.getOwnPropertyDescriptor(new d(), "value")), r.value))
      return (
        (r.value = n),
        void k.defineProperty(e, t, { ...r, writable: !0 })
      );
    D(e, t, () => n);
  }
  function N(e, t) {
    const n = self[l.SCOPE_CHEATER],
      r = e.HTMLIFrameElement;
    if (!r) return !1;
    const i = e.Document,
      o = Object.getOwnPropertyDescriptor(r.prototype, "contentWindow").get;
    function s(e, r) {
      if ("IFRAME" === e.tagName) {
        if (e.src && e.src.toString().startsWith("javascript:")) {
          let t = `window.${l.SCOPE_BROWSER}=window.top.${l.SCOPE_BROWSER};window.${l.SCOPE_CHEATER}=window.top.${l.SCOPE_CHEATER};window.${l.SCOPE_CHEATER}(self,window.${l.SCOPE_BROWSER},'direct js');`;
          e.src = `javascript:${t};${e.src.toString().substring(11)};`;
        }
        let i = A(o, e, []);
        i && n(i, t, r);
      }
    }
    i &&
      (O(
        i.prototype,
        "getElementById",
        (e, t, n) => {
          let r = A(e, t, n);
          return r ? (s(r, "getElementById"), r) : r;
        },
        { skipAbsent: !0 },
      ),
      [
        "getElementsByClassName",
        "getElementsByName",
        "getElementsByTagName",
        "getElementsByTagNameNS",
      ].forEach((e) => {
        O(
          i.prototype,
          e,
          (t, n, r) => {
            let i = A(t, n, r);
            if (!i) return i;
            for (let t = 0; t < i.length; t++) s(i[t], e);
            return i;
          },
          { skipAbsent: !0 },
        );
      }));
    const a = e.Node;
    a &&
      O(
        a.prototype,
        "appendChild",
        (r, i, o) => {
          let a = e.self.length,
            l = A(r, i, o);
          if (o[0] instanceof u || o[0] instanceof DocumentFragment) {
            s(o[0], "appendChild");
            try {
              let e = o[0].querySelectorAll("iframe");
              for (let t of e) s(t, "appendChild");
            } catch (e) {
              throw e;
            }
          }
          let c = e.self[a];
          return (c && e !== c && n(c, t, "appendChild"), l);
        },
        { skipAbsent: !0 },
      );
    const u = e.Element;
    if (
      (u &&
        (T(
          u.prototype,
          "innerHTML",
          (r, i, o) => {
            let s = e.self.length,
              a = A(r, i, o),
              l = e.self[s];
            return (l && n(l, t, "innerHTML"), a);
          },
          { skipAbsent: !0 },
        ),
        T(
          u.prototype,
          "outerHTML",
          (r, i, o) => {
            let s = e.self.length,
              a = A(r, i, o),
              l = e.self[s];
            return (l && n(l, t, "outerHTML"), a);
          },
          { skipAbsent: !0 },
        )),
      t.uaInfo && t.uaInfo.product)
    ) {
      let c = t.uaInfo.product.name.toString().toLowerCase();
      function d() {
        return 0;
      }
      ((d[l.native] = 1),
        c.match(/.*(edge|chrome).*/) ||
          ((e.chrome = {}), e.chrome && I(e.chrome, "valueOf", d)),
        c.match(/.*(safari).*/) ||
          ((e.safari = void 0), e.safari && I(e.safari, "valueOf", d)));
    }
    return (
      [
        "contentWindow",
        "contentDocument",
        "src",
        "srcdoc",
        "name",
        "sandbox",
        "allowFullscreen",
        "width",
        "height",
        "contentDocument",
        "referrerPolicy",
        "csp",
        "allow",
        "featurePolicy",
        "align",
        "scrolling",
        "frameBorder",
        "longDesc",
        "marginHeight",
        "marginWidth",
        "loading",
        "allowPaymentRequest",
        "privateToken",
        "credentialless",
      ].forEach((e) => {
        D(
          r.prototype,
          e,
          (r, i, s) => {
            let a = A(r, i, s),
              l = A(o, i, s);
            return (l && n(l, t, "contentWindow:" + e), a);
          },
          { skipAbsent: !1 },
        );
      }),
      !0
    );
  }
  const R = (
    Object.getOwnPropertyDescriptor(Navigator.prototype, "deviceMemory") || {}
  ).get;
  function L(e, t) {
    !(function (e, t) {
      const n = e.Navigator || e.WorkerNavigator;
      if (n) {
        if (
          ("boolean" == typeof t.doNotTrack &&
            D(
              n.prototype,
              "doNotTrack",
              (e, n, r) => (A(e, n, r), t.doNotTrack ? "1" : "0"),
              { skipAbsent: !0 },
            ),
          "boolean" == typeof t.javaEnabled &&
            O(
              n.prototype,
              "javaEnabled",
              (e, n, r) => (A(e, n, r), t.javaEnabled),
              { skipAbsent: !0 },
            ),
          t.screen?.maxTouchPoints &&
            D(
              n.prototype,
              "maxTouchPoints",
              (e, n, r) => {
                let i = A(e, n, r);
                return t.screen?.maxTouchPoints || i;
              },
              { skipAbsent: !0 },
            ),
          t.userAgent &&
            (D(
              n.prototype,
              "appVersion",
              (e, n, r) => {
                let i = A(e, n, r);
                return t.appVersion || i;
              },
              { skipAbsent: !0 },
            ),
            D(
              n.prototype,
              "userAgent",
              (e, n, r) => {
                let i = A(e, n, r);
                return t.userAgent || i;
              },
              { skipAbsent: !0 },
            )),
          t.uaInfo &&
            (D(
              n.prototype,
              "vendor",
              (e, n, r) => {
                let i = A(e, n, r);
                return t.browserVendor || i;
              },
              { skipAbsent: !0 },
            ),
            D(
              n.prototype,
              "platform",
              (e, n, r) => {
                let i = A(e, n, r);
                return t.getPlatform(i) || i;
              },
              { skipAbsent: !0 },
            )),
          t.memoryCapacity && R)
        ) {
          const r = R.call(e.navigator),
            i = t.memoryCapacity / r;
          if (e.navigator.deviceMemory && e.performance.memory)
            for (const t in e.performance.memory) {
              let n = e.performance.memory[t];
              I(e.performance.memory, t, Math.round(n * i));
            }
          D(
            n.prototype,
            "deviceMemory",
            (e, n, r) => {
              let i = A(e, n, r);
              return t.memoryCapacity || i;
            },
            { skipAbsent: !0 },
          );
          const o = e.performance.memory;
          if (o) {
            let e = Object.getPrototypeOf(o);
            for (let t of [
              "jsHeapSizeLimit",
              "totalJSHeapSize",
              "usedJSHeapSize",
            ])
              D(e, t, (e, t, n) => (A(e, t, n) * i) | 0, { skipAbsent: !0 });
          }
        }
        (t.processors &&
          D(
            n.prototype,
            "hardwareConcurrency",
            (e, n, r) => {
              let i = A(e, n, r);
              return t.processors || i;
            },
            { skipAbsent: !0 },
          ),
          t.language &&
            (D(
              n.prototype,
              "language",
              (e, n, r) => {
                let i = A(e, n, r);
                // [MEJORA] navigator.language debe ser un solo idioma
                return (t.languages && t.languages[0]) || t.languageCode || i;
              },
              { skipAbsent: !0 },
            ),
            D(
              n.prototype,
              "languages",
              (e, n, r) => {
                let i = A(e, n, r);
                return t.languages || i;
              },
              { skipAbsent: !0 },
            )));
      }
    })(e, t);
    const n = e.Number,
      r = [
        "toFixed() digits argument must be between 0 and 100",
        "Gecko toFixed() failed len 29",
        "Webkit toFixed() failed len 49 between 0 and 100",
      ];
    n &&
      t.uaInfo?.engine &&
      O(
        n.prototype,
        "toFixed",
        (e, n, i) => {
          try {
            return A(e, n, i);
          } catch (e) {
            let n = -1;
            if ("RangeError" === e.constructor.name && t.uaInfo) {
              let e = ((t.uaInfo.engine || {}).name || "").toLowerCase();
              "blink" === e
                ? (n = 0)
                : "gecko" === e
                  ? (n = 1)
                  : "webkit" === e && (n = 2);
            }
            throw (r[n] && (e.message = r[n]), e);
          }
        },
        { skipAbsent: !1 },
      );
    const i = e.NavigatorUAData;
    return (
      i &&
        t.uaInfo &&
        (t.uaInfo.os &&
          D(i.prototype, "platform", (e, n, r) => {
            let i = A(e, n, r);
            return t.getPlatform(i) || i;
          }),
        D(i.prototype, "brands", (e, n, r) => {
          let i = A(e, n, r);
          return t.brands || i;
        }),
        D(i.prototype, "mobile", (e, n, r) => {
          let i = A(e, n, r);
          return "boolean" == typeof t.mobile ? t.mobile : i;
        }),
        O(i.prototype, "getHighEntropyValues", (e, n, r) => {
          let i = n;
          return A(e, n, r).then(
            (e) => (
              ((e = { ...e }).brands = i.brands),
              "platform" in e &&
                (e.platform = t.getPlatform(e.platform) || e.platform),
              "model" in e && (e.model = t.model || ""),
              "mobile" in e &&
                (e.mobile = void 0 === i.mobile ? e.mobile : i.mobile),
              "architecture" in e &&
                (e.architecture = t.arch || e.architecture),
              "bitness" in e && (e.bitness = t.bitness || e.bitness),
              "uaFullVersion" in e &&
                (e.uaFullVersion = t.uaFullVersion || e.uaFullVersion),
              "fullVersionList" in e &&
                (e.fullVersionList = t.fullVersionList || e.fullVersionList),
              "platformVersion" in e &&
                (e.platformVersion = t.platformVersion || e.platformVersion),
              e
            ),
          );
        }),
        O(i.prototype, "toJSON", (e, t, n) => {
          let r = A(e, t, n);
          return (
            (r = { ...r }),
            (r.mobile = t.mobile),
            (r.brands = t.brands),
            (r.platform = t.platform),
            r
          );
        })),
      !0
    );
  }
  function V(e, t) {
    null != e &&
      "object" == typeof e &&
      (function (e, t = []) {
        if (!(0 > 10)) {
          if (e instanceof Array) for (let n of e) V(n, t);
          if (!e.__lock) {
            e.__lock = 1;
            for (let n of Object.keys(e)) {
              for (let r of t) {
                let t = typeof r.check,
                  i = !1;
                if (
                  ("function" === t
                    ? (i = r.check(n, e[n]))
                    : "string" === t
                      ? (i = n === r.check)
                      : r.check instanceof RegExp && (i = !!n.match(r.check)),
                  i)
                ) {
                  e[n] = r.handle(n, e[n]);
                  break;
                }
              }
              V(e[n], t);
            }
            delete e.__lock;
          }
        }
      })(e, t);
  }
  function F(e, t) {
    const n = e.EventTarget,
      r = e.MessageEvent;
    let i = (function (e) {
      let t = [];
      (e.timezone &&
        (t.push({
          check: (e, t) => "timezoneOffset" === e && "number" == typeof t,
          handle: (t, n) => e.timezoneOffset || n,
        }),
        t.push({
          check: (e, t) => {
            if (!e.includes("time") || "string" != typeof t || !t) return !1;
            let n = [
              "Africa",
              "America",
              "Antarctica",
              "Asia",
              "Atlantic",
              "Australia",
              "Europe",
              "Indian",
              "Pacific",
            ];
            for (let e of n) if (t.startsWith(e + "/")) return !0;
            return !1;
          },
          handle: (t, n) => e.timezone || n,
        })),
        e.memoryCapacity &&
          R &&
          t.push({
            check: (e, t) => "deviceMemory" === e && "number" == typeof t,
            handle: (t, n) => e.memoryCapacity || n,
          }),
        e.processors &&
          t.push({
            check: (e, t) =>
              "hardwareConcurrency" === e && "number" == typeof t,
            handle: (t, n) => e.processors || n,
          }),
        e.language &&
          (t.push({ check: "language", handle: (t, n) => (e.languages && e.languages[0]) || e.languageCode || n }),
          t.push({ check: "languages", handle: (t, n) => e.languages || n })));
      return (
        e.webglInfo?.UNMASKED_VENDOR_WEBGL &&
          t.push({
            check: (e, t) =>
              e.toString().match(/.*((webgl|unmasked)(R|_r)(enderer)).*/) &&
              "string" == typeof t &&
              t,
            handle: (t, n) => e.webglInfo?.UNMASKED_RENDERER_WEBGL || n,
          }),
        e.webglInfo?.UNMASKED_RENDERER_WEBGL &&
          t.push({
            check: (e, t) =>
              e.toString().match(/.*((webgl|unmasked)(V|_v)(endor|ender)).*/) &&
              "string" == typeof t &&
              t,
            handle: (t, n) => e.webglInfo?.UNMASKED_VENDOR_WEBGL || n,
          }),
        e.userAgent &&
          (t.push({
            check: (e, t) =>
              !(
                !e.toString().match(/.*(((user)(A|_a)(gent))|ua|Ua|UA).*/) ||
                "string" != typeof t ||
                !t
              ) && t.startsWith("Mozilla"),
            handle: (t, n) => e.userAgent || n,
          }),
          t.push({ check: "appVersion", handle: (t, n) => e.appVersion || n })),
        t.push({ check: "platform", handle: (t, n) => e.getPlatform(n) || n }),
        e.uaInfo &&
          (t.push({
            check: (e, t) =>
              e.toString().includes("vendor") &&
              "string" == typeof t &&
              t.endsWith("Inc."),
            handle: (t, n) => e.browserVendor || n,
          }),
          t.push({
            check: /brands|brand_list|brandList/,
            handle: (t, n) => e.brands || n,
          }),
          t.push({
            check: "brandsVersion",
            handle: (t, n) => e.brandsVersion || n,
          }),
          t.push({
            check: (e, t) =>
              e.toLowerCase().includes("mobile") && "boolean" == typeof t,
            handle: (t, n) => (void 0 === e.mobile ? n : e.mobile),
          }),
          t.push({ check: /arch(itecture)*/, handle: (t, n) => e.arch || n }),
          t.push({
            check: (e, t) =>
              e.toLowerCase().includes("bit") &&
              "number" == typeof t &&
              [16, 32, 64].some((e) => e == t),
            handle: (t, n) => e.bitness || n,
          }),
          t.push({
            check: "uaFullVersion",
            handle: (t, n) => e.uaFullVersion || n,
          }),
          t.push({
            check: "platformVersion",
            handle: (t, n) => e.platformVersion || n,
          }),
          t.push({
            check: "fullVersionList",
            handle: (t, n) => e.fullVersionList || n,
          })),
        t
      );
    })(t);
    if (!i.length) return !1;
    function o(t) {
      return (
        (e.Worker && t instanceof e.Worker) ||
        (e.SharedWorker && t instanceof e.SharedWorker) ||
        (e.MessagePort && t instanceof e.MessagePort) ||
        (e.ServiceWorkerContainer && t instanceof e.ServiceWorkerContainer)
      );
    }
    return (
      O(n.prototype, "addEventListener", (e, n, r) =>
        o(n) && t.disableWorker ? void 0 : A(e, n, r),
      ),
      D(r.prototype, "data", (e, t, n) => {
        let r = t[l.fake] || A(e, t, n);
        return o(t.currentTarget) ? (V(r, i), (t[l.fake] = r), r) : r;
      }),
      !0
    );
  }
  function P(e = "", t) {
    switch (e) {
      case "get":
        return `function ${t.startsWith("get ") ? t : "get " + t}() { [native code] }`;
      case "set":
        return `function ${t.startsWith("set ") ? t : "set " + t}() { [native code] }`;
      default:
        return `function ${t}() { [native code] }`;
    }
  }
  const W = Array.prototype.includes;
  function j(e) {
    let t = Object.values(l);
    const n = e.Object,
      r = e.Function,
      i = e.Reflect;
    function o(n, r, i) {
      let o = A(n, r, i);
      if (!o || !r) return o;
      try {
        let e =
            i[0][l.add] ||
            (Object.getPrototypeOf(i[0]) || {}).constructor[l.add] ||
            [],
          n = [
            ...(i[0][l.remove] ||
              (Object.getPrototypeOf(i[0]) || {}).constructor[l.remove] ||
              []),
            ...t,
          ];
        return (
          (o = o.filter((e) => !A(W, n, [e]))),
          e.forEach((e) => o.push(e)),
          [...o]
        );
      } catch (t) {
        return (e[l.attach].errors.push(t), o);
      }
    }
    return (
      O(
        n.prototype,
        "hasOwnProperty",
        (e, t, n) => {
          let r = A(e, t, n);
          return "function" != typeof t
            ? r
            : t[l.native]
              ? !A(
                  W,
                  ["arguments", "caller", "toString", "prototype"],
                  [n[1]],
                ) && r
              : r;
        },
        { skipAbsent: !1 },
      ),
      (n.hasOwnProperty = n.prototype.hasOwnProperty),
      O(
        r.prototype,
        "toString",
        function (e, t, n) {
          let r = typeof t;
          if (("function" !== r && "object" !== r) || !t) return A(e, t, n);
          let i = t[l.native],
            o = t[l.create];
          try {
            if (!i || o) return A(e, t, n);
          } catch (e) {
            throw (
              e instanceof TypeError &&
                t[l.proxyId] &&
                (t[l.isProxy]
                  ? (e.stack =
                      "TypeError: Function.prototype.toString requires that 'this' be a Function\n    at Object.toString (<anonymous>)\n    at <anonymous>:1:34")
                  : (e.stack =
                      "TypeError: Function.prototype.toString requires that 'this' be a Function\n    at Function.toString (<anonymous>)\n    at <anonymous>:1:55")),
              e
            );
          }
          return P(1 === i ? "function" : 2 === i ? "get" : "set", t.name);
        },
        { skipAbsent: !1 },
      ),
      O(
        n,
        "create",
        (e, t, n) => {
          if (n[0] && n[0][l.proxyId]) {
            let r = A(e, t, n);
            return (
              (r[l.proxyId] = n[0][l.proxyId]),
              (r[l.isProxy] = n[0][l.isProxy]),
              (r[l.create] = 1),
              r
            );
          }
          return A(e, t, n);
        },
        { skipAbsent: !1 },
      ),
      O(n, "keys", o, { skipAbsent: !1 }),
      O(n, "getOwnPropertyNames", o, { skipAbsent: !1 }),
      O(i, "ownKeys", o, { skipAbsent: !1 }),
      O(
        n,
        "getOwnPropertyDescriptor",
        (e, n, r) => {
          let i = A(e, n, r);
          if (!i || !n || !r[1]) return i;
          if (
            i.value &&
            (i.value instanceof Window || i.value.top instanceof Window)
          )
            try {
              i.value[0];
            } catch (e) {
              return i;
            }
          let o = r[1];
          return (((i.value && i.value[l.proxyId]) ||
            (i.get && i.get[l.proxyId]) ||
            (i.set && i.set[l.proxyId])) &&
            t.includes(o)) ||
            (
              n[l.remove] ||
              (Object.getPrototypeOf(n) || {}).constructor[l.remove] ||
              []
            ).includes(o)
            ? void 0
            : (
                  n[l.add] ||
                  (Object.getPrototypeOf(n) || {}).constructor[l.add] ||
                  []
                ).includes(o)
              ? i || {
                  configurable: !0,
                  enumerable: !0,
                  value: void 0,
                  writable: !0,
                }
              : i;
        },
        { skipAbsent: !1 },
      ),
      O(
        n,
        "getOwnPropertyDescriptors",
        (e, n, r) => {
          let i = A(e, n, r);
          if (!i || !n) return i;
          let o =
              n[l.remove] ||
              (Object.getPrototypeOf(n) || {}).constructor[l.remove] ||
              [],
            s =
              n[l.add] ||
              (Object.getPrototypeOf(n) || {}).constructor[l.add] ||
              [];
          return (
            [...o, ...t].forEach((e) => {
              delete i[e];
            }),
            s.forEach((e) => {
              i[e] = i[e] || {
                configurable: !0,
                enumerable: !0,
                value: 1,
                writable: !0,
              };
            }),
            i
          );
        },
        { skipAbsent: !1 },
      ),
      x(
        e,
        "Proxy",
        (e, t, n) => {
          let r = t[1];
          if (t[0] && r) {
            let e = r.get,
              n = t[0][l.proxyId];
            if (n) {
              let i = (t, r, i) =>
                r === l.isProxy ||
                (r === l.proxyId ? n : e ? e(t, r, i) : Reflect.get(t, r, i));
              t[1] = { ...r, get: i };
            }
          }
          return Reflect.construct(e, t, n);
        },
        { skipAbsent: !1 },
      ),
      !0
    );
  }
  function z(e, t) {
    const n = e.UserActivation;
    if (
      (n &&
        (D(n.prototype, "hasBeenActive", () => !1),
        D(n.prototype, "isActive", () => !1)),
      !("getInterestGroupAdAuctionData" in e.navigator))
    ) {
      const a = Object.getOwnPropertyDescriptor(
        Navigator.prototype,
        "userAgent",
      ).get;
      O(
        e.Navigator.prototype,
        "getInterestGroupAdAuctionData",
        (e, t, n) => {
          a.call(t);
        },
        { skipAbsent: !1 },
      );
    }
    (D(window, "outerWidth", (e, t, n) => A(e, t, n) || screen.availWidth),
      D(window, "screenX", (e, t, n) => (A(e, t, n), 0)),
      D(window, "screenLeft", (e, t, n) => (A(e, t, n), 0)),
      D(window, "outerHeight", (e, t, n) => A(e, t, n) || screen.availHeight),
      D(e, "originAgentCluster", (e, t, n) => (A(e, t, n), !0), {
        skipAbsent: !0,
      }),
      O(console, "debug", () => {}, { skipAbsent: !0 }),
      O(console, "clear", () => {}, { skipAbsent: !0 }),
      speechSynthesis &&
        O(speechSynthesis, "getVoices", (e, n, r) => {
          let i = A(e, n, r);
          if (i?.length) return i;
          let o = (t.languages && t.languages[0]) || navigator.language || "",
            s = "";
          s = o.startsWith("zh")
            ? "Microsoft Huihui - Chinese (Simplified, PRC)"
            : o.startsWith("en")
              ? "Microsoft Mark - English (United States)"
              : "Google " + o;
          let a = {
            default: !0,
            lang: o,
            localService: !0,
            name: s,
            voiceURI: s,
          };
          return (
            Object.setPrototypeOf(a, SpeechSynthesisVoice.prototype),
            [a]
          );
        }));
    const r = e.Object,
      i = e.Reflect,
      o = /^([a-z]){3}_.*_(Array|Promise|Symbol|JSON|Object|Proxy|Window)$/;
    function s(e, t, n) {
      let r = A(e, t, n);
      return r && t
        ? r.filter((e) => !o.test(e)).filter((e) => "ret_nodes" !== e)
        : r;
    }
    if (
      (O(r, "keys", s, { skipAbsent: !1 }),
      O(r, "getOwnPropertyNames", s, { skipAbsent: !1 }),
      O(i, "ownKeys", s, { skipAbsent: !1 }),
      t.antiDebugger)
    ) {
      function l(e) {
        let t = e.toString(),
          n = t.indexOf("{"),
          r = t.lastIndexOf("}");
        return t.substring(n + 1, r);
      }
      O(window, "setInterval", (e, t, n) => {
        let [r, i, ...o] = n,
          s = typeof r;
        if ("function" !== s && "string" !== s) return A(e, t, n);
        let a = r.toString().trim();
        return a.includes("debugger")
          ? "string" === s
            ? ((a = a.replaceAll(/(?<!\\w)debugger(?!\\w)/g, "")),
              (n[0] = a),
              A(e, t, n))
            : ((a = l(r).trim().replaceAll(";;", ";")),
              a.length > 10 && r(...o),
              (n[0] = () => {}),
              A(e, t, n))
          : A(e, t, n);
      });
    }
    return !0;
  }
  function $(e, t, n) {
    if (!t) return e;
    let r = t.toLowerCase().trim(),
      i = _[r];
    return i && (n + "").trim() !== i + "" ? "(false)" : e;
  }
  const _ = {},
    U = (e, t) => (_[e] = t);
  function Z(e) {
    return (
      !!Object.keys(_).length &&
      (O(e.window, "matchMedia", (e, t, n) => {
        let r = A(e, t, n);
        if (!r) return r;
        let i = r.media;
        if ("not all" === i) return r;
        let o = (function (e, t) {
          const n = new RegExp(
            `(^|[^a-zA-Z-])(${["all", "braille", "embossed", "handheld", "print", "projection", "screen", "speech", "tty", "tv"].join("|")})($|[^a-zA-Z-])`,
            "g",
          );
          return e.replace(/\(([^)]+):([^)]+)\)/g, $).replace(n, (e) => {
            let n = t[e];
            return e in t ? (n ? "<non>" : "(false)") : e;
          });
        })(i, _);
        if (o === i)
          return (
            r.matches || Object.defineProperty(r, "matches", { value: !0 }),
            r
          );
        if ("" === o && !0 === r.matches) return r;
        let s = e.call(t, o);
        return (Object.defineProperty(s, "media", { value: n[0] }), s);
      }),
      !0)
    );
  }
  const H = JSON.parse(
      '{"js":{"76":["Document.onsecuritypolicyviolation","Promise.allSettled"],"77":["Document.onformdata","Document.onpointerrawupdate"],"78":["Element.elementTiming"],"79":["Document.onanimationend","Document.onanimationiteration","Document.onanimationstart","Document.ontransitionend"],"80":["!Document.registerElement","!Element.createShadowRoot","!Element.getDestinationInsertionPoints"],"81":["Document.onwebkitanimationend","Document.onwebkitanimationiteration","Document.onwebkitanimationstart","Document.onwebkittransitionend","Element.ariaAtomic","Element.ariaAutoComplete","Element.ariaBusy","Element.ariaChecked","Element.ariaColCount","Element.ariaColIndex","Element.ariaColSpan","Element.ariaCurrent","Element.ariaDisabled","Element.ariaExpanded","Element.ariaHasPopup","Element.ariaHidden","Element.ariaKeyShortcuts","Element.ariaLabel","Element.ariaLevel","Element.ariaLive","Element.ariaModal","Element.ariaMultiLine","Element.ariaMultiSelectable","Element.ariaOrientation","Element.ariaPlaceholder","Element.ariaPosInSet","Element.ariaPressed","Element.ariaReadOnly","Element.ariaRelevant","Element.ariaRequired","Element.ariaRoleDescription","Element.ariaRowCount","Element.ariaRowIndex","Element.ariaRowSpan","Element.ariaSelected","Element.ariaSort","Element.ariaValueMax","Element.ariaValueMin","Element.ariaValueNow","Element.ariaValueText","Intl.DisplayNames"],"83":["Element.ariaDescription","Element.onbeforexrselect"],"84":["Document.getAnimations","Document.timeline","Element.ariaSetSize","Element.getAnimations"],"85":["Promise.any","String.replaceAll"],"86":["Document.fragmentDirective","Document.replaceChildren","Element.replaceChildren","!Atomics.wake"],"90":["Document.onbeforexrselect","RegExp.hasIndices","!Element.onbeforexrselect"],"91":["Element.getInnerHTML"],"92":["Array.at","String.at"],"93":["Error.cause","Object.hasOwn"],"94":["!Error.cause","Object.hasOwn"],"102":["Element.ariaInvalid","Document.onbeforematch"],"110":["Array.toReversed","Array.toSorted","Array.toSpliced","Array.with"],"111":["String.isWellFormed","String.toWellFormed","Document.startViewTransition"],"116":["WebAssembly.JSTag"],"121":["Array.fromAsync"],"124":["Document.parseHTMLUnsafe","Element.setHTMLUnsafe"],"127":["Symbol.asyncDispose"],"128":["Promise.try","Document.caretPositionFromPoint","Element.currentCSSZoom","Element.ariaColIndexText","Element.ariaRowIndexText"],"129":["Intl.DurationFormat","Document.onscrollsnapchange","Document.onscrollsnapchanging"],"130":["!Element.getInnerHTML"],"133":["Atomics.pause","Document.moveBefore","Element.moveBefore"],"134":["Error.isError"],"135":["Math.f16round","Document.oncommand","Element.ariaActiveDescendantElement","Element.ariaControlsElements","Element.ariaDescribedByElements","Element.ariaDetailsElements","Element.ariaErrorMessageElements","Element.ariaFlowToElements","Element.ariaLabelledByElements"],"136":["RegExp.escape"],"87-89":["Atomics.waitAsync","Document.ontransitioncancel","Document.ontransitionrun","Document.ontransitionstart","Intl.Segmenter"],"95-96":["WebAssembly.Exception","WebAssembly.Tag"],"97-98":["Array.findLast","Array.findLastIndex","Document.onslotchange"],"99-101":["Intl.supportedValuesOf","Document.oncontextlost","Document.oncontextrestored"],"103-106":["Element.role"],"107-109":["Element.ariaBrailleLabel","Element.ariaBrailleRoleDescription"],"112-113":["RegExp.unicodeSets"],"114-115":["JSON.rawJSON","JSON.isRawJSON"],"117-118":["Object.groupBy","Map.groupBy","Document.hasPrivateToken","Document.hasRedemptionRecord"],"119-120":["Promise.withResolvers","Document.hasStorageAccess","Document.requestStorageAccess","Document.requestStorageAccessFor","!Element.setHTML"],"122-123":["Set.union","Set.intersection","Set.difference","Set.symmetricDifference","Set.isSubsetOf","Set.isSupersetOf","Set.isDisjointFrom"],"125-126":["Symbol.dispose","Document.hasUnpartitionedCookieAccess","Element.getHTML"],"131-132":["Document.browsingTopics"]},"A":{"76":["backdrop-filter"],"81":["color-scheme","image-orientation"],"83":["contain-intrinsic-size"],"84":["appearance","ruby-position"],"87":["ascent-override","border-block","border-block-color","border-block-style","border-block-width","border-inline","border-inline-color","border-inline-style","border-inline-width","descent-override","inset","inset-block","inset-block-end","inset-block-start","inset-inline","inset-inline-end","inset-inline-start","line-gap-override","margin-block","margin-inline","padding-block","padding-inline","text-decoration-thickness","text-underline-offset"],"88":["aspect-ratio"],"89":["border-end-end-radius","border-end-start-radius","border-start-end-radius","border-start-start-radius","forced-color-adjust"],"90":["overflow-clip-margin"],"91":["additive-symbols","fallback","negative","pad","prefix","range","speak-as","suffix","symbols","system"],"92":["size-adjust"],"93":["accent-color"],"94":["scrollbar-gutter"],"104":["object-view-box"],"105":["container-name","container-type","container"],"108":["hyphenate-character","!orientation","!max-zoom","!min-zoom","!user-zoom"],"109":["hyphenate-limit-chars","math-depth","math-shift","math-style"],"110":["initial-letter"],"116":["animation-range-end","animation-range-start","animation-timeline","offset-anchor","offset-position","scroll-timeline-axis","scroll-timeline-name","timeline-scope","view-timeline-axis","view-timeline-inset","view-timeline-name","animation-range","scroll-timeline","view-timeline"],"120":["mask-clip","mask-composite","mask-image","mask-mode","mask-origin","mask-position","mask-repeat","mask-size","!background-repeat-x","!background-repeat-y","!-webkit-mask-repeat-x","!-webkit-mask-repeat-y"],"125":["anchor-name","inset-area","position-anchor","position-try-options","position-try-order","position-visibility","view-transition-class","anchorName","insetArea","positionAnchor","positionTry","positionTryOptions","positionTryOrder","positionVisibility","viewTransitionClass","position-try"],"126":["navigation","types"],"127":["font-size-adjust","fontSizeAdjust"],"128":["position-try-fallbacks","ruby-align","positionTryFallbacks","rubyAlign","!position-try-options","!positionTryOptions"],"129":["interpolate-size","position-area","interpolateSize","positionArea"],"135":["interactivity","overflow-block","overflow-inline","scroll-marker-group","overflowBlock","overflowInline","scrollMarkerGroup"],"136":["!-webkit-print-color-adjust","dynamic-range-limit","print-color-adjust","dynamicRangeLimit","printColorAdjust","webkit-print-color-adjust"],"77-80":["overscroll-behavior-block","overscroll-behavior-inline"],"85-86":["content-visibility","counter-set","inherits","initial-value","page-orientation","syntax"],"95-96":["app-region","contain-intrinsic-block-size","contain-intrinsic-height","contain-intrinsic-inline-size","contain-intrinsic-width"],"97-98":["font-synthesis-small-caps","font-synthesis-style","font-synthesis-weight","font-synthesis"],"99-100":["text-emphasis-color","text-emphasis-position","text-emphasis-style","text-emphasis"],"101-103":["font-palette","base-palette","override-colors"],"106-107":["hyphenate-character"],"111-113":["baseline-source","font-variant-alternates","view-transition-name"],"114-115":["text-wrap","white-space-collapse"],"117-119":["font-variant-position","overlay","transition-behavior","!-webkit-highlight"],"121-122":["scrollbar-color","scrollbar-width"],"123-124":["field-sizing","text-spacing-trim","fieldSizing","textSpacingTrim"],"130-132":["box-decoration-break","text-wrap-mode","text-wrap-style","boxDecorationBreak","textWrapMode","textWrapStyle"],"133-134":["scroll-initial-target","text-box-edge","text-box-trim","scrollInitialTarget","textBox","textBoxEdge","textBoxTrim","text-box"]},"x":{"80":["CompressionStream","DecompressionStream","FeaturePolicy","FragmentDirective","PeriodicSyncManager","VideoPlaybackQuality"],"81":["SubmitEvent","XRHitTestResult","XRHitTestSource","XRRay","XRTransientInputHitTestResult","XRTransientInputHitTestSource"],"83":["BarcodeDetector","XRDOMOverlayState","XRSystem"],"84":["AnimationPlaybackEvent","AnimationTimeline","CSSAnimation","CSSTransition","DocumentTimeline","FinalizationRegistry","LayoutShiftAttribution","ResizeObserverSize","WakeLock","WakeLockSentinel","WeakRef","XRLayer"],"85":["AggregateError","CSSPropertyRule","EventCounts","XRAnchor","XRAnchorSet"],"86":["RTCEncodedAudioFrame","RTCEncodedVideoFrame"],"87":["CookieChangeEvent","CookieStore","CookieStoreManager","Scheduling"],"88":["Scheduling","!BarcodeDetector"],"89":["ReadableByteStreamController","ReadableStreamBYOBReader","ReadableStreamBYOBRequest","ReadableStreamDefaultController","XRWebGLBinding"],"90":["AbstractRange","CustomStateSet","NavigatorUAData","XRCPUDepthInformation","XRDepthInformation","XRLightEstimate","XRLightProbe","XRWebGLDepthInformation"],"91":["CSSCounterStyleRule","GravitySensor","NavigatorManagedData"],"92":["CSSCounterStyleRule","!SharedArrayBuffer"],"93":["WritableStreamDefaultController"],"94":["AudioData","AudioDecoder","AudioEncoder","EncodedAudioChunk","EncodedVideoChunk","IdleDetector","ImageDecoder","ImageTrack","ImageTrackList","VideoColorSpace","VideoDecoder","VideoEncoder","VideoFrame","MediaStreamTrackGenerator","MediaStreamTrackProcessor","Profiler","VirtualKeyboard","DelegatedInkTrailPresenter","Ink","Scheduler","TaskController","TaskPriorityChangeEvent","TaskSignal","VirtualKeyboardGeometryChangeEvent"],"99":["CanvasFilter","CSSLayerBlockRule","CSSLayerStatementRule"],"100":["CSSMathClamp"],"109":["MathMLElement"],"110":["AudioSinkInfo"],"116":["documentPictureInPicture","DocumentPictureInPicture","WGSLLanguageFeatures","IdentityProvider","DocumentPictureInPictureEvent","ScrollTimeline","ViewTimeline","VisibilityStateEntry"],"117":["CSSStartingStyleRule","Iterator"],"118":["CSSScopeRule"],"119":["!Sanitizer","!openDatabase"],"120":["MediaStreamTrackVideoStats","sharedStorage","fence","Fence","FencedFrameConfig","HTMLFencedFrameElement","IdentityCredentialError","NavigatorLogin","SharedStorage","SharedStorageWorklet"],"121":["CharacterBoundsUpdateEvent","EditContext","TextFormat","TextFormatUpdateEvent","TextUpdateEvent","SpeechSynthesis","SpeechSynthesisVoice"],"122":["Iterator","StorageBucket","StorageBucketManager"],"123":["onpagereveal","NavigationActivation","PageRevealEvent","PerformanceLongAnimationFrameTiming","PerformanceScriptTiming"],"124":["onpageswap","PageSwapEvent","WebSocketError","WebSocketStream"],"125":["MediaStreamTrackAudioStats","PressureObserver","PressureRecord","ProtectedAudience","CSSPositionTryDescriptors","CSSPositionTryRule","ViewTransitionTypeSet"],"126":["WebGLObject","CSSViewTransitionRule","CloseWatcher","caches"],"127":["ChapterInformation","!MutationEvent","!eslint"],"128":["CaretPosition"],"129":["onscrollsnapchange","onscrollsnapchanging","SnapEvent"],"130":["CSSNestedDeclarations"],"131":["XRHand","XRJointPose","XRJointSpace","CSSMarginRule"],"132":["AICreateMonitor","DevicePosture","RestrictionTarget"],"133":["ReportBody","CSPViolationReportBody","FileSystemObserver"],"134":["CSSFontFeatureValuesRule","SharedStorageAppendMethod","SharedStorageClearMethod","SharedStorageDeleteMethod","SharedStorageModifierMethod","SharedStorageSetMethod"],"95-96":["URLPattern"],"97-98":["WebTransport","WebTransportBidirectionalStream","WebTransportDatagramDuplexStream","WebTransportError"],"101-104":["CSSFontPaletteValuesRule"],"105-106":["CSSContainerRule"],"107-108":["XRCamera"],"111-112":["ViewTransition"],"113-115":["ViewTransition","!CanvasFilter"],"135-136":["SuppressedError","DisposableStack","AsyncDisposableStack","Float16Array","FetchLaterResult","fetchLater","oncommand","CommandEvent","Observable","Subscriber"]}}',
    ),
    B = JSON.parse(
      '{"js":{"71":["Promise.allSettled"],"74":["!Array.toSource","!Boolean.toSource","!Date.toSource","!Error.toSource","!Function.toSource","!Intl.toSource","!JSON.toSource","!Math.toSource","!Number.toSource","!Object.toSource","!RegExp.toSource","!String.toSource","!WebAssembly.toSource"],"77":["String.replaceAll"],"78":["Atomics.add","Atomics.and","Atomics.compareExchange","Atomics.exchange","Atomics.isLockFree","Atomics.load","Atomics.notify","Atomics.or","Atomics.store","Atomics.sub","Atomics.wait","Atomics.wake","Atomics.xor","Document.replaceChildren","Element.replaceChildren","Intl.ListFormat","RegExp.dotAll"],"85":["!Document.onshow","Promise.any"],"86":["Intl.DisplayNames"],"87":["Document.onbeforeinput"],"92":["Object.hasOwn"],"100":["WebAssembly.Tag","WebAssembly.Exception"],"125":["Intl.Segmenter","Document.onbeforetoggle","Document.oncontextlost","Document.oncontextrestored","Element.ariaBrailleLabel","Element.ariaBrailleRoleDescription"],"126":["Element.currentCSSZoom"],"127":["Set.union","Set.difference","Set.intersection","Set.symmetricDifference","Set.isSubsetOf","Set.isSupersetOf","Set.isDisjointFrom"],"128":["Element.getHTML"],"129":["Math.f16round"],"130":["Document.oncontentvisibilityautostatechange"],"134":["RegExp.escape","Promise.try"],"135":["JSON.isRawJSON","JSON.rawJSON"],"136":["Intl.DurationFormat","Element.ariaActiveDescendantElement","Element.ariaControlsElements","Element.ariaDescribedByElements","Element.ariaDetailsElements","Element.ariaErrorMessageElements","Element.ariaFlowToElements","Element.ariaLabelledByElements","Element.ariaOwnsElements"],"137":["Math.sumPrecise","Atomics.pause"],"138":["Error.isError","Error.captureStackTrace"],"72-73":["Document.onformdata","Element.part"],"75-76":["Document.getAnimations","Document.timeline","Element.getAnimations","Intl.Locale"],"79-84":["Promise.any"],"88-89":["RegExp.hasIndices"],"90-91":["Array.at","String.at"],"93-99":["Intl.supportedValuesOf","Document.onsecuritypolicyviolation","Document.onslotchange"],"101-103":["Document.adoptedStyleSheets"],"104-108":["Array.findLast","Array.findLastIndex"],"109-114":["Document.onscrollend"],"115-117":["RegExp.unicodeSets","Array.fromAsync","Array.toReversed","Array.toSorted","Array.toSpliced","Array.with","Document.oncancel"],"118-120":["Object.groupBy","String.isWellFormed","String.toWellFormed","Element.role","Element.ariaAtomic","Element.ariaAutoComplete","Element.ariaBusy","Element.ariaChecked","Element.ariaColCount","Element.ariaColIndex","Element.ariaColIndexText","Element.ariaColSpan","Element.ariaCurrent","Element.ariaDescription","Element.ariaDisabled","Element.ariaExpanded","Element.ariaHasPopup","Element.ariaHidden","Element.ariaInvalid","Element.ariaKeyShortcuts","Element.ariaLabel","Element.ariaLevel","Element.ariaLive","Element.ariaModal","Element.ariaMultiLine","Element.ariaMultiSelectable","Element.ariaOrientation","Element.ariaPlaceholder","Element.ariaPosInSet","Element.ariaPressed","Element.ariaReadOnly","Element.ariaRelevant","Element.ariaRequired","Element.ariaRoleDescription","Element.ariaRowCount","Element.ariaRowIndex","Element.ariaRowIndexText","Element.ariaRowSpan","Element.ariaSelected","Element.ariaSetSize","Element.ariaSort","Element.ariaValueMax","Element.ariaValueMin","Element.ariaValueNow","Element.ariaValueText"],"121-122":["Promise.withResolvers","Map.groupBy"],"123-124":["Document.parseHTMLUnsafe","Element.setHTMLUnsafe"],"131-133":["WebAssembly.JSTag","Document.fragmentDirective"]},"A":{"71":["-moz-column-span"],"72":["offset","offset-anchor","offset-distance","offset-path","offset-rotate","rotate","scale","translate"],"73":["overscroll-behavior-block","overscroll-behavior-inline"],"91":["tab-size"],"96":["color-scheme"],"97":["print-color-adjust","scrollbar-gutter","d"],"102":["overflow-clip-margin"],"109":["-webkit-clip-path"],"110":["container-type","container-name","page","container"],"111":["font-synthesis-small-caps","font-synthesis-style","font-synthesis-weight"],"112":["font-synthesis-small-caps","!-moz-image-region"],"121":["text-wrap"],"122":["offset-position"],"123":["!MozUserFocus","!-moz-user-focus"],"124":["textWrapMode","text-wrap-mode","textWrapStyle","text-wrap-style","whiteSpaceCollapse","white-space-collapse"],"125":["contentVisibility","content-visibility"],"132":["!MozUserModify","!-moz-user-modify"],"74-79":["!-moz-stack-sizing","text-underline-position"],"80-88":["appearance"],"89-90":["!-moz-outline-radius","!-moz-outline-radius-bottomleft","!-moz-outline-radius-bottomright","!-moz-outline-radius-topleft","!-moz-outline-radius-topright","aspect-ratio"],"92-95":["accent-color"],"98-101":["hyphenate-character"],"103-106":["scroll-snap-stop"],"107-108":["backdrop-filter","font-palette","contain-intrinsic-block-size","contain-intrinsic-height","contain-intrinsic-inline-size","contain-intrinsic-width","contain-intrinsic-size"],"113-114":["forced-color-adjust","-webkit-text-security"],"115-117":["baseline-source","math-depth","math-style","animation-composition"],"118-120":["font-synthesis-position"],"126-128":["zoom","!MozPerspective","!-moz-perspective","!MozPerspectiveOrigin","!-moz-perspective-origin","!MozBackfaceVisibility","!-moz-backface-visibility","!MozTransformStyle","!-moz-transform-style","!MozTransitionDuration","!-moz-transition-duration","!MozTransitionTimingFunction","!-moz-transition-timing-function","!MozTransitionProperty","!-moz-transition-property","!MozTransitionDelay","!-moz-transition-delay","!MozTransition","!-moz-transition"],"129-131":["WebkitFontFeatureSettings","-webkit-font-feature-settings","webkitFontFeatureSettings","transitionBehavior","transition-behavior"],"133-134":["MozPerspective","-moz-perspective","MozPerspectiveOrigin","-moz-perspective-origin","MozBackfaceVisibility","-moz-backface-visibility","MozTransformStyle","-moz-transform-style"],"135-136":["!MozUserInput","!-moz-user-input"],"137-138":["hyphenateLimitChars","hyphenate-limit-chars"]},"x":{"71":["MathMLElement","!SVGZoomAndPan"],"74":["FormDataEvent","!uneval"],"75":["AnimationTimeline","CSSAnimation","CSSTransition","DocumentTimeline","SubmitEvent"],"78":["Atomics"],"82":["MediaMetadata","MediaSession","Sanitizer"],"83":["MediaMetadata","MediaSession","!Sanitizer"],"84":["PerformancePaintTiming"],"87":["onbeforeinput"],"88":["onbeforeinput","!VisualViewport"],"96":["Lock","LockManager"],"97":["CSSLayerBlockRule","CSSLayerStatementRule"],"98":["HTMLDialogElement"],"99":["NavigationPreloadManager"],"110":["CSSContainerRule"],"111":["FileSystemFileHandle","FileSystemDirectoryHandle"],"112":["FileSystemFileHandle","!U2F"],"121":["UserActivation","ToggleEvent","!sizeToContent"],"124":["!DOMRequest"],"125":["ContentVisibilityAutoStateChangeEvent","RTCIceTransport","onbeforetoggle","oncontextlost","oncontextrestored"],"126":["WakeLockSentinel","WakeLock","CustomStateSet"],"127":["ClipboardItem"],"128":["CSSPropertyRule"],"129":["!MediaCapabilitiesInfo","Float16Array","CSSStartingStyleRule","TextEvent","CSSPageDescriptors"],"130":["AudioDecoder","EncodedVideoChunk","AudioData","VideoDecoder","VideoFrame","AudioEncoder","VideoColorSpace","VideoEncoder","EncodedAudioChunk","oncontentvisibilityautostatechange"],"131":["Iterator","FragmentDirective"],"132":["CSSNestedDeclarations"],"136":["SVGDiscardElement"],"137":["CookieChangeEvent","CookieStore","cookieStore"],"138":["NavigatorLogin","originAgentCluster","!CookieChangeEvent","!CookieStore","!cookieStore"],"72-73":["!BatteryManager","FormDataEvent","Geolocation","GeolocationCoordinates","GeolocationPosition","GeolocationPositionError","!mozPaintCount"],"76-77":["AudioParamMap","AudioWorklet","AudioWorkletNode","Worklet"],"79-81":["AggregateError","FinalizationRegistry"],"85-86":["PerformancePaintTiming","!HTMLMenuItemElement","!onshow"],"89-92":["!ondevicelight","!ondeviceproximity","!onuserproximity"],"93-95":["ElementInternals"],"100-104":["WritableStream"],"105-106":["TextDecoderStream","OffscreenCanvasRenderingContext2D","OffscreenCanvas","TextEncoderStream"],"107-109":["CSSFontPaletteValuesRule"],"113-114":["CompressionStream","DecompressionStream","WebTransportSendStream","WebTransportDatagramDuplexStream","WebTransportError","WebTransportReceiveStream","WebTransport","WebTransportBidirectionalStream","!mozRTCPeerConnection","!mozRTCSessionDescription","!mozRTCIceCandidate"],"115-120":["RTCEncodedAudioFrame","RTCSctpTransport","RTCEncodedVideoFrame","RTCRtpScriptTransform","oncancel","!OfflineResourceList","!applicationCache"],"122-123":["LargestContentfulPaint"],"133-135":["ImageDecoder","ImageTrackList","ImageTrack"]}}',
    );
  function G(e = {}, t = 0) {
    let n = new Set(),
      r = new Set();
    for (let [i, o] of Object.entries(e)) {
      o = o || [];
      let e = (i + "").split("-"),
        s = +e[0],
        a = +e[e.length - 1];
      (s < t &&
        o.forEach((e) => {
          let t = e;
          "!" === e.toString().charAt(0)
            ? ((t = e.toString().substring(1)), r.add(t))
            : n.add(t);
        }),
        s >= t &&
          a <= t &&
          o.forEach((e) => {
            let t = e;
            "!" === e.toString().charAt(0)
              ? ((t = e.toString().substring(1)), r.add(t))
              : n.add(t);
          }),
        a > t &&
          o.forEach((e) => {
            let t = e;
            if ("!" === e.toString().charAt(0)) {
              if (((t = e.toString().substring(1)), r.has(t))) return;
              n.add(t);
            } else {
              if (n.has(t)) return;
              r.add(t);
            }
          }));
    }
    return { removes: [...r], adds: [...n] };
  }
  const q = () =>
      class {
        constructor() {
          let e = new TypeError("Illegal constructor");
          throw ((e.stack = "Illegal constructor\n    at <anonymous>:1:1"), e);
        }
      },
    J = new Set();
  function Y(e, t) {
    if (t.safeMode || !t.uaInfo) return !1;
    let n = t.uaInfo.engine;
    if (!n.name) return !1;
    let r = n.version ? n.version : "0";
    const i = "BLINK" === n.name.toUpperCase(),
      o = "GECKO" === n.name.toUpperCase();
    let s = Number(/\d+/.exec(r + "")[0]);
    if ((i && s < 76) || (o && s < 71)) return !1;
    let u = (function (e, t) {
        let {
            css: n,
            win: r,
            js: i,
          } = (function (e) {
            const t = "BLINK" === e.toUpperCase(),
              n = "GECKO" === e.toUpperCase();
            return {
              css: t ? H.A : n ? B.A : {},
              win: t ? H.x : n ? B.x : {},
              js: t ? H.js : n ? B.js : {},
            };
          })(e),
          o = G(n, t),
          s = G(r, t),
          a = G(i, t);
        return {
          adds: { css: o.adds, win: s.adds, js: a.adds },
          removes: { css: o.removes, win: s.removes, js: a.removes },
        };
      })(n.name, s),
      {
        adds: { js: c, css: d, win: m },
        removes: { js: h, css: f, win: p },
      } = u;
    return (
      ((e, t = [], n = []) => {
        (t.forEach((t) => {
          let [n, r] = t.split(".");
          if (a.getValue(e, t)) return;
          let i = a.getValue(e, n + ".prototype");
          if (!i) {
            if (r.charAt(0) === r.charAt(0).toUpperCase()) {
              let t = q();
              return void I(e, r, t);
            }
            return void I(e, t, function () {});
          }
          if (Object.getOwnPropertyDescriptor(i, r)) return;
          Object.defineProperty(i, r, { value: function () {}, writable: !0 });
        }),
          n.forEach((t) => {
            let [n, r] = t.split(".");
            if (a.getValue(e, t)) {
              J.add(t);
              try {
                a.deleteValue(e, t);
              } catch (e) {
                self[l.attach].errors.push(e);
              }
              return;
            }
            let i = a.getValue(e, n + ".prototype");
            if (i) {
              J.add(n + ".prototype." + r);
              try {
                delete i[r];
              } catch (t) {
                let i = a.getValue(e, n);
                (i[l.remove] || (i[l.remove] = []), i[l.remove].push(r));
              }
            }
          }));
      })(e, c, h),
      ((e, t = [], n = []) => {
        (t.forEach((t) => {
          if (!a.getValue(self, t)) {
            if (t.charAt(0) === t.charAt(0).toUpperCase()) {
              let n = q();
              return void I(e, t, n);
            }
            I(e, t, function () {});
          }
        }),
          n.forEach((t) => {
            J.add(t);
            try {
              (delete e[t], delete e.self[t]);
            } catch (e) {
              self[l.attach].errors.push(e);
            }
          }));
      })(e, m, p),
      "Window" === e.constructor.name &&
        ((e, t = [], n = []) => {
          let r = (e = "") => {
            if ("string" != typeof e) return e;
            let t = e.split("-"),
              n = "";
            return (
              t.forEach((e) => {
                if (e)
                  return n
                    ? void (n += e.charAt(0).toUpperCase() + e.substring(1))
                    : (n += e);
              }),
              n
            );
          };
          ((t = new Set(t.map(r))), (n = new Set(n.map(r))));
          let i = (e) =>
            e
              ? ((e[l.add] = t),
                (e[l.remove] = n),
                new Proxy(e, {
                  get: (e, t) =>
                    (function (e, t) {
                      if ("__origin__" === t) return e;
                      let i = e[t];
                      if ("function" == typeof i)
                        return i.prototype
                          ? i
                          : i + "".startsWith("function")
                            ? i.bind(e)
                            : i;
                      let o = r(t);
                      return n.has(o)
                        ? void 0
                        : n.has(r(i))
                          ? ""
                          : ((e.fakeCss = e.fakeCss || {}),
                            i || e.fakeCss[o] || "");
                    })(e, t),
                  set: (e, i, o) =>
                    (function (e, i, o) {
                      if ("fakeCss" === i) return !0;
                      if ("function" == typeof o || "object" == typeof o)
                        return ((e[i] = o), !0);
                      let s = r(i);
                      return i in e
                        ? (n.has(s) || (e.setProperty(i, o), (e[i] = o)), !0)
                        : !t.has(s) ||
                            ((e.fakeCss = e.fakeCss || {}),
                            (e.fakeCss[s] = o),
                            !0);
                    })(e, i, o),
                  has: (e, i) =>
                    (function (e, i) {
                      let o = r(i);
                      return !!n.has(o) || !!t.has(o) || i in e;
                    })(e, i),
                }))
              : e;
          const o = e.CSSStyleDeclaration;
          (O(o.prototype, "getPropertyValue", (e, t, n) => t[n[0]] || "", {
            skipAbsent: !1,
          }),
            O(o.prototype, "setProperty", (e, t, n) => (t[n[0]] = n[1]), {
              skipAbsent: !1,
            }),
            O(
              o.prototype,
              "item",
              (e, t, n) => {
                let r = A(e, t, n);
                return r in t ? r : "";
              },
              { skipAbsent: !1 },
            ),
            O(
              o.prototype,
              "item",
              (e, t, n) => {
                let r = A(e, t, n);
                return r in t ? r : "";
              },
              { skipAbsent: !1 },
            ));
          const s = e.HTMLElement,
            a = e.CSSStyleRule;
          (D(
            s.prototype,
            "style",
            (e, t, n) => {
              let r = A(e, t, n);
              return i(r);
            },
            { skipAbsent: !1 },
          ),
            O(
              e.window,
              "getComputedStyle",
              (e, t, n) => {
                let r = A(e, t, n);
                return i(r);
              },
              { skipAbsent: !1 },
            ),
            D(
              a.prototype,
              "style",
              (e, t, n) => {
                let r = A(e, t, n);
                return i(r);
              },
              { skipAbsent: !1 },
            ));
        })(e, d, f),
      !0
    );
  }
  const K = {
      7: ["Cambria Math", "Lucida Console"],
      8: ["Aldhabi", "Gadugi", "Myanmar Text", "Nirmala UI"],
      8.1: ["Leelawadee UI", "Javanese Text", "Segoe UI Emoji"],
      10: [
        "HoloLens MDL2 Assets",
        "Segoe MDL2 Assets",
        "Bahnschrift",
        "Ink Free",
      ],
      11: ["Segoe Fluent Icons"],
    },
    X = {
      10.9: ["Helvetica Neue", "Geneva"],
      "10.10": ["Kohinoor Devanagari Medium", "Luminari"],
      10.11: ["PingFang HK Light"],
      10.12: [
        "American Typewriter Semibold",
        "Futura Bold",
        "SignPainter-HouseScript Semibold",
      ],
      "10.13-10.14": ["InaiMathi Bold"],
      "10.15-11": ["Galvji", "MuktaMahee Regular"],
      12: [
        "Noto Sans Gunjala Gondi Regular",
        "Noto Sans Masaram Gondi Regular",
        "Noto Serif Yezidi Regular",
      ],
      13: [
        "Apple SD Gothic Neo ExtraBold",
        "STIX Two Math Regular",
        "STIX Two Text Regular",
        "Noto Sans Canadian Aboriginal Regular",
      ],
    },
    Q = {
      "Microsoft Outlook": ["MS Outlook"],
      "Adobe Acrobat": ["ZWAdobeF"],
      LibreOffice: [
        "Amiri",
        "KACSTOffice",
        "Liberation Mono",
        "Source Code Pro",
      ],
      OpenOffice: ["DejaVu Sans", "Gentium Book Basic", "OpenSymbol"],
    },
    ee = Object.keys(X)
      .map((e) => X[e])
      .flat(),
    te = Object.keys(K)
      .map((e) => K[e])
      .flat(),
    ne = Object.keys(Q)
      .map((e) => Q[e])
      .flat();
  new Set([
    ...ee,
    ...te,
    "Arimo",
    "Chilanka",
    "Cousine",
    "Jomolhari",
    "MONO",
    "Noto Color Emoji",
    "Ubuntu",
    "Dancing Script",
    "Droid Sans Mono",
    "Roboto",
    ...ne,
  ]);
  const re = {
    android: ["Dancing Script", "Droid Sans Mono", "Roboto"],
    linux: [
      "Arimo",
      "Chilanka",
      "Cousine",
      "Jomolhari",
      "MONO",
      "Noto Color Emoji",
      "Ubuntu",
    ],
    apple: [
      "Helvetica Neue",
      "Geneva",
      "Kohinoor Devanagari Medium",
      "Luminari",
      "PingFang HK Light",
      "American Typewriter Semibold",
      "Futura Bold",
      "SignPainter-HouseScript Semibold",
      "InaiMathi Bold",
      "Galvji",
      "MuktaMahee Regular",
      "Bai Jamjuree",
      "Chakra Petch",
      "Charmonman",
      "Kodchasan",
    ],
    windows: [
      "Cambria Math",
      "Lucida Console",
      "Aldhabi",
      "Gadugi",
      "Myanmar Text",
      "Nirmala UI",
      "Leelawadee UI",
      "Javanese Text",
      "Segoe UI Emoji",
      "HoloLens MDL2 Assets",
      "Segoe MDL2 Assets",
      "Bahnschrift",
      "Ink Free",
      "Segoe Fluent Icons",
    ],
  };
  function ie(e = "", t = []) {
    if (!e) return !1;
    let n = e.split(" "),
      r = (n[n.length - 1] || "").toUpperCase();
    return t.find((e) => e.toUpperCase() === r);
  }
  const oe = ["monospace", "sans-serif", "serif"];
  let se = Object.getOwnPropertyDescriptor(
      HTMLElement.prototype,
      "offsetWidth",
    ).get,
    ae = Object.getOwnPropertyDescriptor(
      HTMLElement.prototype,
      "offsetHeight",
    ).get;
  function le(e, t) {
    const n = t.factors.fonts;
    if (!n) return !1;
    let r = new a.Random(n);
    const i = e.HTMLElement,
      o = r.int(5, 20);
    let s;
    if (t.uaInfo) s = ((t.uaInfo.os || {}).name || "").toLowerCase();
    else {
      let w = e.navigator.userAgent.toLowerCase();
      w.includes("windows")
        ? (s = "windows")
        : w.includes("ios") || w.includes("macintosh")
          ? (s = "apple")
          : w.includes("android")
            ? (s = "android")
            : w.includes("linux") && (s = "linux");
    }
    let l = s;
    ("ios" !== s && "mac" !== s) || (l = "apple");
    let u = re[l] || [];
    if (!u.length) return !1;
    let c =
      Object.keys(re)
        .filter((e) => e !== l)
        .map((e) => re[e])
        .flat() || [];
    const d = {};
    function m(e) {
      if (ie(e, c)) return !0;
      let t = (function (e, t) {
        if (!e || !t || !t.length) return !1;
        const n = t.reduce((e, t) => ((e[t] = !0), e), {}),
          r =
            "Cambria Math" in n ||
            "Nirmala UI" in n ||
            "Leelawadee UI" in n ||
            "HoloLens MDL2 Assets" in n ||
            "Segoe Fluent Icons" in n,
          i =
            "Helvetica Neue" in n ||
            "Luminari" in n ||
            "PingFang HK Light" in n ||
            "Futura Bold" in n ||
            "InaiMathi Bold" in n ||
            "Galvji" in n ||
            "Chakra Petch" in n,
          o =
            "Arimo" in n ||
            "MONO" in n ||
            "Ubuntu" in n ||
            "Noto Color Emoji" in n ||
            "Dancing Script" in n ||
            "Droid Sans Mono" in n ||
            "Roboto" in n;
        let s = e.toLowerCase();
        return (
          !(!r || "windows" === s) ||
          !(!i || "apple" === s) ||
          !(!o || "linux" === s)
        );
      })(l, [e]);
      if (t) return !0;
      if (ie(e, u)) return !1;
      let r = d[e];
      return (
        r || ((d[e] = a.hashcode(e)), (r = d[e])),
        1 === new a.Random(n + r).int(1, 4)
      );
    }
    const h = {};
    if (i) {
      function S(e, t, r) {
        let i = A(e, t, r),
          o = a.getStyle(t, "font-family"),
          s = a.getStyle(t, "font-size");
        if (!o || !s) return i;
        if ((s.match(/\d+/g)?.map(Number)[0] || 0) > 200) return i;
        let l = o.split(",").map((e) => e.trim());
        for (let e of l) {
          if (oe.includes(e)) return ((h[e] = i), i);
          if (m(e)) continue;
          if (!Object.values(h).includes(i)) return i;
          let t = Math.abs(a.hashcode(e));
          return i + new a.Random(n + t).int(1, 50) / 100;
        }
        return Object.values(h)[0] || i;
      }
      D(i.prototype, "offsetWidth", S, { skipAbsent: !1 });
    }
    const f = e.TextMetrics;
    f &&
      D(f.prototype, "width", (e, t, n) => {
        let i = A(e, t, n);
        return i ? (i % o === 0 ? i : i + (r.int(0, 5e3) - 2500) / 5e3) : i;
      });
    const p = e.SVGTextContentElement;
    p &&
      O(p.prototype, "getComputedTextLength", (e, t, n) => {
        let i = A(e, t, n);
        return i ? (i % o === 0 ? i : i + (r.int(0, 5e3) - 2500) / 5e3) : i;
      });
    const g = e.FontFace;
    let y = g.prototype.load;
    const b = Object.getPrototypeOf(e.document.fonts);
    if (g) {
      function v(e, t) {
        const n = new g(e, `local(${e})`);
        return y
          .call(n)
          .then((e) => (Object.defineProperty(e, "family", { value: t }), e))
          .catch((e) => {
            throw e;
          });
      }
      O(g.prototype, "load", (t, n, r) => {
        const i = n.family;
        let o = A(t, n, r);
        return o instanceof e.Promise
          ? "CTHULHU_JS" === i || oe.includes(i)
            ? o
            : m(i)
              ? v("CTHULHU_JS", i)
              : o.then((e) => e).catch((e) => v("Arial", i))
          : o;
      });
    }
    return (
      b &&
        O(b, "check", (e, t, n) => {
          let r = A(e, t, n),
            i = n[0];
          if (!i) return r;
          let o = /".+"/.exec(i);
          if (!o) return r;
          let s = o[1];
          return s ? (oe.includes(s) ? r : !m(s)) : r;
        }),
      !0
    );
  }
  function ue(e, t) {
    const n = t.factors.clientRect;
    if (!n) return !1;
    let r = new a.Random(n);
    const i = e.Element;
    return (
      !!i &&
      (O(
        i.prototype,
        "getClientRects",
        function (e, t, n) {
          let i = A(e, t, n);
          if (
            t[l.fake] ||
            ["rect-known", "rect-ghost", "rects"].includes(t.className)
          )
            return i;
          if ("SVG" === t.tagName) return i;
          if ((t.className + "")?.includes("svg")) return i;
          let o = !0;
          for (let e in i[0])
            if ("toJSON" !== e && (0 !== i[0][e] || 0 !== i[0][e])) {
              o = !1;
              break;
            }
          if (o) return i;
          const s = 1e-4 * r.int(-999, 999);
          if ("absolute" === t.style.position) {
            let e = (t.style.left.match(/\d+/g) || []).map(Number)[0] || 0;
            t.style.left = e + s + "px";
          } else t.style.marginLeft = s + "%";
          return ((t[l.fake] = 1), A(e, t, n));
        },
        { skipAbsent: !1 },
      ),
      !0)
    );
  }
  function ce(e, t) {
    const n = e.GeolocationCoordinates,
      r = t.location;
    return !(
      !r ||
      (!r.lat && !r.lng) ||
      (n &&
        r &&
        (D(n.prototype, "longitude", (e, t, n) => {
          let i = A(e, t, n);
          return r.lng / 1 || i;
        }),
        D(n.prototype, "latitude", (e, t, n) => {
          let i = A(e, t, n);
          return r.lat / 1 || i;
        })),
      0)
    );
  }
  function de(e, t) {
    const n = e.Plugin;
    if (!t.factors.plugins || !n) return !1;
    let r = new a.Random(t.factors.plugins).int(0, 999999);
    const i = (e, t, n) => {
      let i = A(e, t, n),
        o = new String(i);
      return (
        (o.valueOf = function () {
          return i + " " + r;
        }),
        (o.valueOf[l.native] = 1),
        (o.toString = function () {
          return i + " " + r;
        }),
        (o.toString[l.native] = 1),
        o
      );
    };
    return (
      D(n.prototype, "description", i, { skipAbsent: !0 }),
      D(n.prototype, "name", i, { skipAbsent: !0 }),
      t.hideFlash &&
        navigator.plugins &&
        new Proxy(navigator.plugins, {
          get(e, t, r) {
            const i = Reflect.get(e, t, r);
            if (!(i instanceof n && i.name.includes("Shockwave Flash")))
              return i;
          },
        }),
      !0
    );
  }
  function me(e, t) {
    if (!t.humanLike) return !1;
    x(e, "UIEvent", (e, t, n) => {
      let r = Reflect.construct(e, t, n);
      return r
        ? new Proxy(r, {
            get: (e, t, n) => "isTrusted" === t || Reflect.get(e, t, n),
          })
        : r;
    });
    const n = e.Navigator || e.WorkerNavigator;
    return (
      !!n &&
      (D(n.prototype, "webdriver", (e, t, n) => (A(e, t, n), !1), {
        skipAbsent: !0,
      }),
      D(
        Document.prototype,
        "visibilityState",
        (e, t, n) => {
          const r = A(e, t, n);
          return "hidden" === r ? "visible" : r;
        },
        { skipAbsent: !0 },
      ),
      D(Document.prototype, "hidden", (e, t, n) => (A(e, t, n), !1), {
        skipAbsent: !0,
      }),
      !0)
    );
  }
  !(function (e) {
    try {
      const t = document.createElement("span");
      ((t.textContent = "lLmMwWiI0O&18"),
        (t.style.visibility = "hidden"),
        (t.style.fontFamily = e),
        document.body.appendChild(t));
      let n = A(se, t, []),
        r = A(ae, t, []);
      return (document.body.removeChild(t), { width: n, height: r });
    } catch (e) {
      return -1;
    }
  })(oe.join(","));
  const he = Array.prototype.includes;
  function fe(e, t) {
    const n = t.factors.audio;
    if (!n) return;
    (t.safeMode, new a.Random(n));
    const r = e.AudioBuffer,
      i = e.AnalyserNode;
    function o(e) {
      let t = 0;
      const r = [-1, 0, 1];
      a.randomPeek(
        n,
        e,
        (n, i) => {
          if (!A(he, r, [n])) {
            if (t > 0 && !A(he, r, [e[t]])) {
              let r = e[t];
              return ((e[t] = n), (t = 0), r);
            }
            t = i;
          }
        },
        1,
        Math.min(e.length, 1024),
      );
    }
    return (
      O(r.prototype, "getChannelData", (e, t, n) => {
        let r = A(e, t, n);
        return r ? (o(r), r) : r;
      }),
      O(r.prototype, "copyFromChannel", (e, t, n) => {
        let r = A(e, t, n);
        return (n[0] && o(n[0]), r);
      }),
      O(
        i.prototype,
        "getFloatFrequencyData",
        (e, t, r) => {
          let i = A(e, t, r);
          return (r[0] && a.randomNoise(n, r[0], [0]), i);
        },
        { skipAbsent: !0 },
      ),
      O(
        i.prototype,
        "getFloatTimeDomainData",
        (e, t, r) => {
          let i = A(e, t, r);
          return (r[0] && a.randomNoise(n, r[0], [0]), i);
        },
        { skipAbsent: !0 },
      ),
      !0
    );
  }
  const pe = self.Date;
  function ge(e, t = 0) {
    if (t > 3) return !1;
    let n = 0;
    for (let r of e) {
      if (n > 50) break;
      n++;
      let e = ye(r, t + 1);
      if (e) return e;
    }
    return !1;
  }
  function ye(e, t = 0) {
    if (t > 3) return !1;
    if (null == e) return !1;
    let n = e[l.trace];
    if (n && "path" in n && "handlers" in n && "source" in n) return n;
    if ("object" != typeof e) return !1;
    if (e instanceof Map) return ge(e.values(), t + 1);
    if (e instanceof Array) return ge(e, t + 1);
    if (e instanceof Set) return ge(e, t + 1);
    if (e.constructor !== Object) return !1;
    let r = Object.getOwnPropertyDescriptors(e);
    for (let n in r) {
      if ("function" == typeof r[n].get) continue;
      let i = ye(e[n], t + 1);
      if (i) return i;
    }
    return !1;
  }
  function be(e, t) {
    if (null == e) return e;
    let n = (function (e) {
      let t = e;
      return (
        "number" != typeof e || e instanceof Number || (t = new Number(e)),
        "boolean" != typeof e || e instanceof Boolean || (t = new Boolean(e)),
        "string" != typeof e || e instanceof String || (t = new String(e)),
        t
      );
    })(e);
    return ((n[l.trace] = t), n);
  }
  const we = [
    "sort",
    "splice",
    "slice",
    "includes",
    "join",
    "filter",
    "map",
    "reduce",
    "toReversed",
    "toSorted",
    "toSpliced",
  ];
  function Se(e, t, n) {
    function r() {
      return n;
    }
    function i() {
      return P(t, n);
    }
    ((r[l.native] = 2),
      Object.defineProperty(e, "name", { get: r }),
      (i[l.native] = 1),
      Object.defineProperty(e, "toString", { value: i }));
  }
  function ve(e, t) {
    return (
      !(null == t || !e.functionTrace) &&
      !(!e.dev && (new pe().getTime() - e.start) / 1e3 > 30)
    );
  }
  function ke(e, t, n, r) {
    let i = ye(n);
    if ((i || ((i = ye(r)), i && (n[l.trace] = i)), i)) {
      let o = i.handlers[e];
      return o ? o(t, n, r, i) : (i.path.push(e), be(A(t, n, r), i));
    }
    return A(t, n, r);
  }
  function Ee(e, t) {
    return (
      !!t.functionTrace &&
      ((function (e, t) {
        if (!e) return;
        let n = Object.getOwnPropertyDescriptors(e.prototype);
        for (let r of we) {
          let i = n[r];
          if (i && i.value && "function" == typeof i.value) {
            if ("valueOf" === r || "toString" === r) continue;
            O(e.prototype, r, (e, n, i) =>
              ve(t, n) ? ke("Array." + r, e, n, i) : A(e, n, i),
            );
          }
        }
      })(e.Array, t),
      !0)
    );
  }
  console.log;
  const Ce = 255,
    De = {
      OffscreenCanvasRenderingContext2D: {
        getImageData: OffscreenCanvasRenderingContext2D.prototype.getImageData,
        putImageData: OffscreenCanvasRenderingContext2D.prototype.putImageData,
      },
      CanvasRenderingContext2D: {
        getImageData: CanvasRenderingContext2D.prototype.getImageData,
        putImageData: CanvasRenderingContext2D.prototype.putImageData,
      },
    };
  function Te(e, t, n) {
    const r = e.width,
      i = e.height;
    let o, s;
    const l = self.OffscreenCanvas;
    l && e instanceof l
      ? ((o = new l(r, i)), (s = "OffscreenCanvasRenderingContext2D"))
      : ((o = document.createElement("canvas")),
        (o.width = r),
        (o.height = i),
        (s = "CanvasRenderingContext2D"));
    const u = De[s].getImageData,
      c = De[s].putImageData,
      d = o.getContext("2d", { willReadFrequently: !0 });
    let m = A(u, t, [0, 0, Math.min(r, 300), Math.min(i, 300)]);
    return (
      (function (e, t) {
        if (!e) return;
        if ("srgb" !== e.colorSpace) return;
        let n = e.data,
          r = 4 * e.width;
        (e.height,
          a.randomPeek(
            t,
            n,
            (e, t) => {
              if (!e || (t + 1) % 4 || e < Ce) return;
              let i = n[t - 3],
                o = n[t - 2],
                s = n[t - 1];
              if (!i || !o || !s) return;
              let a = t / r,
                l = t % r,
                u = n[r * (a - 1) + l],
                c = n[r * (a + 1) + l],
                d = n[l > 4 ? l + 4 : -1],
                m = n[l < r ? l - 4 : -1];
              return (u && u < Ce) ||
                (c && c < Ce) ||
                (d && d < Ce) ||
                (m && m < Ce)
                ? void 0
                : ((function (e, t, n) {
                    let r = e[t];
                    ((e[t] = e[n]), (e[n] = r));
                  })(n, t - 1, t - 2),
                  e);
            },
            3,
            10,
          ));
      })(m, n),
      A(c, d, [m, 0, 0]),
      [o, d]
    );
  }
  function Oe(e, t) {
    const n = t.factors.canvas;
    if (!n) return !1;
    t.safeMode;
    const r = e.HTMLCanvasElement;
    function i(e, r, i) {
      const o = De[r.constructor.name];
      if (!o) return A(e, r, i);
      let s = A(o.getImageData, r, i);
      if (!s || !s.data) return s;
      if (i) {
        let [e, t, n, r] = i;
        if (n <= 1 && r <= 1) return s;
      }
      const [a, u] = Te(r.canvas, r, n);
      return (
        (s = A(o.getImageData, u, i)),
        s &&
          ve(t, s.data) &&
          ((function (e) {
            for (let t of we) {
              let n = e[t];
              "function" == typeof n &&
                ((e[t] = function () {
                  return ke("Array." + t, n, this, arguments);
                }),
                Se(e[t], "function", t));
            }
          })(s.data),
          (s.data[l.trace] = {
            source: "Canvas.image",
            path: [],
            handlers: {
              "Array.includes": (e, t, n, r) =>
                "Array.join" === r.path[r.path.length - 1] || A(e, t, n),
            },
          })),
        s
      );
    }
    function o(e, t, r) {
      let i = (function (e) {
        const { width: t, height: n } = e;
        if (!(t <= 5 && n <= 5)) return e.getContext("2d");
      })(t);
      if (!i) return A(e, t, r);
      const [o, s] = Te(t, i, n);
      return A(e, o, r);
    }
    (O(e.CanvasRenderingContext2D.prototype, "getImageData", i),
      O(r.prototype, "toBlob", o),
      O(r.prototype, "toDataURL", o));
    const s = e.OffscreenCanvas,
      a = e.OffscreenCanvasRenderingContext2D;
    return (
      s &&
        a &&
        (O(a.prototype, "getImageData", i), O(s.prototype, "convertToBlob", o)),
      !0
    );
  }
  class Me extends Error {}
  class Ae extends Me {
    constructor(e) {
      super(`Invalid DateTime: ${e.toMessage()}`);
    }
  }
  class xe extends Me {
    constructor(e) {
      super(`Invalid Interval: ${e.toMessage()}`);
    }
  }
  class Ie extends Me {
    constructor(e) {
      super(`Invalid Duration: ${e.toMessage()}`);
    }
  }
  class Ne extends Me {}
  class Re extends Me {
    constructor(e) {
      super(`Invalid unit ${e}`);
    }
  }
  class Le extends Me {}
  class Ve extends Me {
    constructor() {
      super("Zone is an abstract class");
    }
  }
  const Fe = "numeric",
    Pe = "short",
    We = "long",
    je = { year: Fe, month: Fe, day: Fe },
    ze = { year: Fe, month: Pe, day: Fe },
    $e = { year: Fe, month: Pe, day: Fe, weekday: Pe },
    _e = { year: Fe, month: We, day: Fe },
    Ue = { year: Fe, month: We, day: Fe, weekday: We },
    Ze = { hour: Fe, minute: Fe },
    He = { hour: Fe, minute: Fe, second: Fe },
    Be = { hour: Fe, minute: Fe, second: Fe, timeZoneName: Pe },
    Ge = { hour: Fe, minute: Fe, second: Fe, timeZoneName: We },
    qe = { hour: Fe, minute: Fe, hourCycle: "h23" },
    Je = { hour: Fe, minute: Fe, second: Fe, hourCycle: "h23" },
    Ye = {
      hour: Fe,
      minute: Fe,
      second: Fe,
      hourCycle: "h23",
      timeZoneName: Pe,
    },
    Ke = {
      hour: Fe,
      minute: Fe,
      second: Fe,
      hourCycle: "h23",
      timeZoneName: We,
    },
    Xe = { year: Fe, month: Fe, day: Fe, hour: Fe, minute: Fe },
    Qe = { year: Fe, month: Fe, day: Fe, hour: Fe, minute: Fe, second: Fe },
    et = { year: Fe, month: Pe, day: Fe, hour: Fe, minute: Fe },
    tt = { year: Fe, month: Pe, day: Fe, hour: Fe, minute: Fe, second: Fe },
    nt = { year: Fe, month: Pe, day: Fe, weekday: Pe, hour: Fe, minute: Fe },
    rt = {
      year: Fe,
      month: We,
      day: Fe,
      hour: Fe,
      minute: Fe,
      timeZoneName: Pe,
    },
    it = {
      year: Fe,
      month: We,
      day: Fe,
      hour: Fe,
      minute: Fe,
      second: Fe,
      timeZoneName: Pe,
    },
    ot = {
      year: Fe,
      month: We,
      day: Fe,
      weekday: We,
      hour: Fe,
      minute: Fe,
      timeZoneName: We,
    },
    st = {
      year: Fe,
      month: We,
      day: Fe,
      weekday: We,
      hour: Fe,
      minute: Fe,
      second: Fe,
      timeZoneName: We,
    };
  class at {
    get type() {
      throw new Ve();
    }
    get name() {
      throw new Ve();
    }
    get ianaName() {
      return this.name;
    }
    get isUniversal() {
      throw new Ve();
    }
    offsetName(e, t) {
      throw new Ve();
    }
    formatOffset(e, t) {
      throw new Ve();
    }
    offset(e) {
      throw new Ve();
    }
    equals(e) {
      throw new Ve();
    }
    get isValid() {
      throw new Ve();
    }
  }
  let lt = null;
  class ut extends at {
    static get instance() {
      return (null === lt && (lt = new ut()), lt);
    }
    get type() {
      return "system";
    }
    get name() {
      return new Intl.DateTimeFormat().resolvedOptions().timeZone;
    }
    get isUniversal() {
      return !1;
    }
    offsetName(e, { format: t, locale: n }) {
      return xn(e, t, n);
    }
    formatOffset(e, t) {
      return Ln(this.offset(e), t);
    }
    offset(e) {
      return -new Date(e).getTimezoneOffset();
    }
    equals(e) {
      return "system" === e.type;
    }
    get isValid() {
      return !0;
    }
  }
  const ct = new Map(),
    dt = { year: 0, month: 1, day: 2, era: 3, hour: 4, minute: 5, second: 6 },
    mt = new Map();
  class ht extends at {
    static create(e) {
      let t = mt.get(e);
      return (void 0 === t && mt.set(e, (t = new ht(e))), t);
    }
    static resetCache() {
      (mt.clear(), ct.clear());
    }
    static isValidSpecifier(e) {
      return this.isValidZone(e);
    }
    static isValidZone(e) {
      if (!e) return !1;
      try {
        return (new Intl.DateTimeFormat("en-US", { timeZone: e }).format(), !0);
      } catch (e) {
        return !1;
      }
    }
    constructor(e) {
      (super(), (this.zoneName = e), (this.valid = ht.isValidZone(e)));
    }
    get type() {
      return "iana";
    }
    get name() {
      return this.zoneName;
    }
    get isUniversal() {
      return !1;
    }
    offsetName(e, { format: t, locale: n }) {
      return xn(e, t, n, this.name);
    }
    formatOffset(e, t) {
      return Ln(this.offset(e), t);
    }
    offset(e) {
      if (!this.valid) return NaN;
      const t = new Date(e);
      if (isNaN(t)) return NaN;
      const n = (function (e) {
        let t = ct.get(e);
        return (
          void 0 === t &&
            ((t = new Intl.DateTimeFormat("en-US", {
              hour12: !1,
              timeZone: e,
              year: "numeric",
              month: "2-digit",
              day: "2-digit",
              hour: "2-digit",
              minute: "2-digit",
              second: "2-digit",
              era: "short",
            })),
            ct.set(e, t)),
          t
        );
      })(this.name);
      let [r, i, o, s, a, l, u] = n.formatToParts
        ? (function (e, t) {
            const n = e.formatToParts(t),
              r = [];
            for (let e = 0; e < n.length; e++) {
              const { type: t, value: i } = n[e],
                o = dt[t];
              "era" === t ? (r[o] = i) : un(o) || (r[o] = parseInt(i, 10));
            }
            return r;
          })(n, t)
        : (function (e, t) {
            const n = e.format(t).replace(/\u200E/g, ""),
              r = /(\d+)\/(\d+)\/(\d+) (AD|BC),? (\d+):(\d+):(\d+)/.exec(n),
              [, i, o, s, a, l, u, c] = r;
            return [s, i, o, a, l, u, c];
          })(n, t);
      "BC" === s && (r = 1 - Math.abs(r));
      let c = +t;
      const d = c % 1e3;
      return (
        (c -= d >= 0 ? d : 1e3 + d),
        (Tn({
          year: r,
          month: i,
          day: o,
          hour: 24 === a ? 0 : a,
          minute: l,
          second: u,
          millisecond: 0,
        }) -
          c) /
          6e4
      );
    }
    equals(e) {
      return "iana" === e.type && e.name === this.name;
    }
    get isValid() {
      return this.valid;
    }
  }
  let ft = {};
  const pt = new Map();
  function gt(e, t = {}) {
    const n = JSON.stringify([e, t]);
    let r = pt.get(n);
    return (
      void 0 === r && ((r = new Intl.DateTimeFormat(e, t)), pt.set(n, r)),
      r
    );
  }
  const yt = new Map(),
    bt = new Map();
  let wt = null;
  const St = new Map();
  function vt(e) {
    let t = St.get(e);
    return (
      void 0 === t &&
        ((t = new Intl.DateTimeFormat(e).resolvedOptions()), St.set(e, t)),
      t
    );
  }
  const kt = new Map();
  function Et(e, t, n, r) {
    const i = e.listingMode();
    return "error" === i ? null : "en" === i ? n(t) : r(t);
  }
  class Ct {
    constructor(e, t, n) {
      ((this.padTo = n.padTo || 0), (this.floor = n.floor || !1));
      const { padTo: r, floor: i, ...o } = n;
      if (!t || Object.keys(o).length > 0) {
        const t = { useGrouping: !1, ...n };
        (n.padTo > 0 && (t.minimumIntegerDigits = n.padTo),
          (this.inf = (function (e, t = {}) {
            const n = JSON.stringify([e, t]);
            let r = yt.get(n);
            return (
              void 0 === r && ((r = new Intl.NumberFormat(e, t)), yt.set(n, r)),
              r
            );
          })(e, t)));
      }
    }
    format(e) {
      if (this.inf) {
        const t = this.floor ? Math.floor(e) : e;
        return this.inf.format(t);
      }
      return bn(this.floor ? Math.floor(e) : kn(e, 3), this.padTo);
    }
  }
  class Dt {
    constructor(e, t, n) {
      let r;
      if (((this.opts = n), (this.originalZone = void 0), this.opts.timeZone))
        this.dt = e;
      else if ("fixed" === e.zone.type) {
        const t = (e.offset / 60) * -1,
          n = t >= 0 ? `Etc/GMT+${t}` : `Etc/GMT${t}`;
        0 !== e.offset && ht.create(n).valid
          ? ((r = n), (this.dt = e))
          : ((r = "UTC"),
            (this.dt =
              0 === e.offset
                ? e
                : e.setZone("UTC").plus({ minutes: e.offset })),
            (this.originalZone = e.zone));
      } else
        "system" === e.zone.type
          ? (this.dt = e)
          : "iana" === e.zone.type
            ? ((this.dt = e), (r = e.zone.name))
            : ((r = "UTC"),
              (this.dt = e.setZone("UTC").plus({ minutes: e.offset })),
              (this.originalZone = e.zone));
      const i = { ...this.opts };
      ((i.timeZone = i.timeZone || r), (this.dtf = gt(t, i)));
    }
    format() {
      return this.originalZone
        ? this.formatToParts()
            .map(({ value: e }) => e)
            .join("")
        : this.dtf.format(this.dt.toJSDate());
    }
    formatToParts() {
      const e = this.dtf.formatToParts(this.dt.toJSDate());
      return this.originalZone
        ? e.map((e) => {
            if ("timeZoneName" === e.type) {
              const t = this.originalZone.offsetName(this.dt.ts, {
                locale: this.dt.locale,
                format: this.opts.timeZoneName,
              });
              return { ...e, value: t };
            }
            return e;
          })
        : e;
    }
    resolvedOptions() {
      return this.dtf.resolvedOptions();
    }
  }
  class Tt {
    constructor(e, t, n) {
      ((this.opts = { style: "long", ...n }),
        !t &&
          mn() &&
          (this.rtf = (function (e, t = {}) {
            const { base: n, ...r } = t,
              i = JSON.stringify([e, r]);
            let o = bt.get(i);
            return (
              void 0 === o &&
                ((o = new Intl.RelativeTimeFormat(e, t)), bt.set(i, o)),
              o
            );
          })(e, n)));
    }
    format(e, t) {
      return this.rtf
        ? this.rtf.format(e, t)
        : (function (e, t, n = "always", r = !1) {
            const i = {
                years: ["year", "yr."],
                quarters: ["quarter", "qtr."],
                months: ["month", "mo."],
                weeks: ["week", "wk."],
                days: ["day", "day", "days"],
                hours: ["hour", "hr."],
                minutes: ["minute", "min."],
                seconds: ["second", "sec."],
              },
              o = -1 === ["hours", "minutes", "seconds"].indexOf(e);
            if ("auto" === n && o) {
              const n = "days" === e;
              switch (t) {
                case 1:
                  return n ? "tomorrow" : `next ${i[e][0]}`;
                case -1:
                  return n ? "yesterday" : `last ${i[e][0]}`;
                case 0:
                  return n ? "today" : `this ${i[e][0]}`;
              }
            }
            const s = Object.is(t, -0) || t < 0,
              a = Math.abs(t),
              l = 1 === a,
              u = i[e],
              c = r ? (l ? u[1] : u[2] || u[1]) : l ? i[e][0] : e;
            return s ? `${a} ${c} ago` : `in ${a} ${c}`;
          })(t, e, this.opts.numeric, "long" !== this.opts.style);
    }
    formatToParts(e, t) {
      return this.rtf ? this.rtf.formatToParts(e, t) : [];
    }
  }
  const Ot = { firstDay: 1, minimalDays: 4, weekend: [6, 7] };
  class Mt {
    static fromOpts(e) {
      return Mt.create(
        e.locale,
        e.numberingSystem,
        e.outputCalendar,
        e.weekSettings,
        e.defaultToEN,
      );
    }
    static create(e, t, n, r, i = !1) {
      const o = e || Bt.defaultLocale,
        s =
          o ||
          (i
            ? "en-US"
            : wt ||
              ((wt = new Intl.DateTimeFormat().resolvedOptions().locale), wt)),
        a = t || Bt.defaultNumberingSystem,
        l = n || Bt.defaultOutputCalendar,
        u = gn(r) || Bt.defaultWeekSettings;
      return new Mt(s, a, l, u, o);
    }
    static resetCache() {
      ((wt = null), pt.clear(), yt.clear(), bt.clear(), St.clear(), kt.clear());
    }
    static fromObject({
      locale: e,
      numberingSystem: t,
      outputCalendar: n,
      weekSettings: r,
    } = {}) {
      return Mt.create(e, t, n, r);
    }
    constructor(e, t, n, r, i) {
      const [o, s, a] = (function (e) {
        const t = e.indexOf("-x-");
        -1 !== t && (e = e.substring(0, t));
        const n = e.indexOf("-u-");
        if (-1 === n) return [e];
        {
          let t, r;
          try {
            ((t = gt(e).resolvedOptions()), (r = e));
          } catch (i) {
            const o = e.substring(0, n);
            ((t = gt(o).resolvedOptions()), (r = o));
          }
          const { numberingSystem: i, calendar: o } = t;
          return [r, i, o];
        }
      })(e);
      ((this.locale = o),
        (this.numberingSystem = t || s || null),
        (this.outputCalendar = n || a || null),
        (this.weekSettings = r),
        (this.intl = (function (e, t, n) {
          return n || t
            ? (e.includes("-u-") || (e += "-u"),
              n && (e += `-ca-${n}`),
              t && (e += `-nu-${t}`),
              e)
            : e;
        })(this.locale, this.numberingSystem, this.outputCalendar)),
        (this.weekdaysCache = { format: {}, standalone: {} }),
        (this.monthsCache = { format: {}, standalone: {} }),
        (this.meridiemCache = null),
        (this.eraCache = {}),
        (this.specifiedLocale = i),
        (this.fastNumbersCached = null));
    }
    get fastNumbers() {
      var e;
      return (
        null == this.fastNumbersCached &&
          (this.fastNumbersCached =
            (!(e = this).numberingSystem || "latn" === e.numberingSystem) &&
            ("latn" === e.numberingSystem ||
              !e.locale ||
              e.locale.startsWith("en") ||
              "latn" === vt(e.locale).numberingSystem)),
        this.fastNumbersCached
      );
    }
    listingMode() {
      const e = this.isEnglish(),
        t = !(
          (null !== this.numberingSystem && "latn" !== this.numberingSystem) ||
          (null !== this.outputCalendar && "gregory" !== this.outputCalendar)
        );
      return e && t ? "en" : "intl";
    }
    clone(e) {
      return e && 0 !== Object.getOwnPropertyNames(e).length
        ? Mt.create(
            e.locale || this.specifiedLocale,
            e.numberingSystem || this.numberingSystem,
            e.outputCalendar || this.outputCalendar,
            gn(e.weekSettings) || this.weekSettings,
            e.defaultToEN || !1,
          )
        : this;
    }
    redefaultToEN(e = {}) {
      return this.clone({ ...e, defaultToEN: !0 });
    }
    redefaultToSystem(e = {}) {
      return this.clone({ ...e, defaultToEN: !1 });
    }
    months(e, t = !1) {
      return Et(this, e, jn, () => {
        const n = "ja" === this.intl || this.intl.startsWith("ja-"),
          r = (t &= !n) ? { month: e, day: "numeric" } : { month: e },
          i = t ? "format" : "standalone";
        if (!this.monthsCache[i][e]) {
          const t = n
            ? (e) => this.dtFormatter(e, r).format()
            : (e) => this.extract(e, r, "month");
          this.monthsCache[i][e] = (function (e) {
            const t = [];
            for (let n = 1; n <= 12; n++) {
              const r = $i.utc(2009, n, 1);
              t.push(e(r));
            }
            return t;
          })(t);
        }
        return this.monthsCache[i][e];
      });
    }
    weekdays(e, t = !1) {
      return Et(this, e, Un, () => {
        const n = t
            ? { weekday: e, year: "numeric", month: "long", day: "numeric" }
            : { weekday: e },
          r = t ? "format" : "standalone";
        return (
          this.weekdaysCache[r][e] ||
            (this.weekdaysCache[r][e] = (function (e) {
              const t = [];
              for (let n = 1; n <= 7; n++) {
                const r = $i.utc(2016, 11, 13 + n);
                t.push(e(r));
              }
              return t;
            })((e) => this.extract(e, n, "weekday"))),
          this.weekdaysCache[r][e]
        );
      });
    }
    meridiems() {
      return Et(
        this,
        void 0,
        () => Zn,
        () => {
          if (!this.meridiemCache) {
            const e = { hour: "numeric", hourCycle: "h12" };
            this.meridiemCache = [
              $i.utc(2016, 11, 13, 9),
              $i.utc(2016, 11, 13, 19),
            ].map((t) => this.extract(t, e, "dayperiod"));
          }
          return this.meridiemCache;
        },
      );
    }
    eras(e) {
      return Et(this, e, qn, () => {
        const t = { era: e };
        return (
          this.eraCache[e] ||
            (this.eraCache[e] = [$i.utc(-40, 1, 1), $i.utc(2017, 1, 1)].map(
              (e) => this.extract(e, t, "era"),
            )),
          this.eraCache[e]
        );
      });
    }
    extract(e, t, n) {
      const r = this.dtFormatter(e, t)
        .formatToParts()
        .find((e) => e.type.toLowerCase() === n);
      return r ? r.value : null;
    }
    numberFormatter(e = {}) {
      return new Ct(this.intl, e.forceSimple || this.fastNumbers, e);
    }
    dtFormatter(e, t = {}) {
      return new Dt(e, this.intl, t);
    }
    relFormatter(e = {}) {
      return new Tt(this.intl, this.isEnglish(), e);
    }
    listFormatter(e = {}) {
      return (function (e, t = {}) {
        const n = JSON.stringify([e, t]);
        let r = ft[n];
        return (r || ((r = new Intl.ListFormat(e, t)), (ft[n] = r)), r);
      })(this.intl, e);
    }
    isEnglish() {
      return (
        "en" === this.locale ||
        "en-us" === this.locale.toLowerCase() ||
        vt(this.intl).locale.startsWith("en-us")
      );
    }
    getWeekSettings() {
      return this.weekSettings
        ? this.weekSettings
        : hn()
          ? (function (e) {
              let t = kt.get(e);
              if (!t) {
                const n = new Intl.Locale(e);
                ((t = "getWeekInfo" in n ? n.getWeekInfo() : n.weekInfo),
                  "minimalDays" in t || (t = { ...Ot, ...t }),
                  kt.set(e, t));
              }
              return t;
            })(this.locale)
          : Ot;
    }
    getStartOfWeek() {
      return this.getWeekSettings().firstDay;
    }
    getMinDaysInFirstWeek() {
      return this.getWeekSettings().minimalDays;
    }
    getWeekendDays() {
      return this.getWeekSettings().weekend;
    }
    equals(e) {
      return (
        this.locale === e.locale &&
        this.numberingSystem === e.numberingSystem &&
        this.outputCalendar === e.outputCalendar
      );
    }
    toString() {
      return `Locale(${this.locale}, ${this.numberingSystem}, ${this.outputCalendar})`;
    }
  }
  let At = null;
  class xt extends at {
    static get utcInstance() {
      return (null === At && (At = new xt(0)), At);
    }
    static instance(e) {
      return 0 === e ? xt.utcInstance : new xt(e);
    }
    static parseSpecifier(e) {
      if (e) {
        const t = e.match(/^utc(?:([+-]\d{1,2})(?::(\d{2}))?)?$/i);
        if (t) return new xt(In(t[1], t[2]));
      }
      return null;
    }
    constructor(e) {
      (super(), (this.fixed = e));
    }
    get type() {
      return "fixed";
    }
    get name() {
      return 0 === this.fixed ? "UTC" : `UTC${Ln(this.fixed, "narrow")}`;
    }
    get ianaName() {
      return 0 === this.fixed
        ? "Etc/UTC"
        : `Etc/GMT${Ln(-this.fixed, "narrow")}`;
    }
    offsetName() {
      return this.name;
    }
    formatOffset(e, t) {
      return Ln(this.fixed, t);
    }
    get isUniversal() {
      return !0;
    }
    offset() {
      return this.fixed;
    }
    equals(e) {
      return "fixed" === e.type && e.fixed === this.fixed;
    }
    get isValid() {
      return !0;
    }
  }
  class It extends at {
    constructor(e) {
      (super(), (this.zoneName = e));
    }
    get type() {
      return "invalid";
    }
    get name() {
      return this.zoneName;
    }
    get isUniversal() {
      return !1;
    }
    offsetName() {
      return null;
    }
    formatOffset() {
      return "";
    }
    offset() {
      return NaN;
    }
    equals() {
      return !1;
    }
    get isValid() {
      return !1;
    }
  }
  function Nt(e, t) {
    if (un(e) || null === e) return t;
    if (e instanceof at) return e;
    if ("string" == typeof e) {
      const n = e.toLowerCase();
      return "default" === n
        ? t
        : "local" === n || "system" === n
          ? ut.instance
          : "utc" === n || "gmt" === n
            ? xt.utcInstance
            : xt.parseSpecifier(n) || ht.create(e);
    }
    return cn(e)
      ? xt.instance(e)
      : "object" == typeof e && "offset" in e && "function" == typeof e.offset
        ? e
        : new It(e);
  }
  const Rt = {
      arab: "[٠-٩]",
      arabext: "[۰-۹]",
      bali: "[᭐-᭙]",
      beng: "[০-৯]",
      deva: "[०-९]",
      fullwide: "[０-９]",
      gujr: "[૦-૯]",
      hanidec: "[〇|一|二|三|四|五|六|七|八|九]",
      khmr: "[០-៩]",
      knda: "[೦-೯]",
      laoo: "[໐-໙]",
      limb: "[᥆-᥏]",
      mlym: "[൦-൯]",
      mong: "[᠐-᠙]",
      mymr: "[၀-၉]",
      orya: "[୦-୯]",
      tamldec: "[௦-௯]",
      telu: "[౦-౯]",
      thai: "[๐-๙]",
      tibt: "[༠-༩]",
      latn: "\\d",
    },
    Lt = {
      arab: [1632, 1641],
      arabext: [1776, 1785],
      bali: [6992, 7001],
      beng: [2534, 2543],
      deva: [2406, 2415],
      fullwide: [65296, 65303],
      gujr: [2790, 2799],
      khmr: [6112, 6121],
      knda: [3302, 3311],
      laoo: [3792, 3801],
      limb: [6470, 6479],
      mlym: [3430, 3439],
      mong: [6160, 6169],
      mymr: [4160, 4169],
      orya: [2918, 2927],
      tamldec: [3046, 3055],
      telu: [3174, 3183],
      thai: [3664, 3673],
      tibt: [3872, 3881],
    },
    Vt = Rt.hanidec.replace(/[\[|\]]/g, "").split(""),
    Ft = new Map();
  function Pt({ numberingSystem: e }, t = "") {
    const n = e || "latn";
    let r = Ft.get(n);
    void 0 === r && ((r = new Map()), Ft.set(n, r));
    let i = r.get(t);
    return (void 0 === i && ((i = new RegExp(`${Rt[n]}${t}`)), r.set(t, i)), i);
  }
  let Wt,
    jt = () => Date.now(),
    zt = "system",
    $t = null,
    _t = null,
    Ut = null,
    Zt = 60,
    Ht = null;
  class Bt {
    static get now() {
      return jt;
    }
    static set now(e) {
      jt = e;
    }
    static set defaultZone(e) {
      zt = e;
    }
    static get defaultZone() {
      return Nt(zt, ut.instance);
    }
    static get defaultLocale() {
      return $t;
    }
    static set defaultLocale(e) {
      $t = e;
    }
    static get defaultNumberingSystem() {
      return _t;
    }
    static set defaultNumberingSystem(e) {
      _t = e;
    }
    static get defaultOutputCalendar() {
      return Ut;
    }
    static set defaultOutputCalendar(e) {
      Ut = e;
    }
    static get defaultWeekSettings() {
      return Ht;
    }
    static set defaultWeekSettings(e) {
      Ht = gn(e);
    }
    static get twoDigitCutoffYear() {
      return Zt;
    }
    static set twoDigitCutoffYear(e) {
      Zt = e % 100;
    }
    static get throwOnInvalid() {
      return Wt;
    }
    static set throwOnInvalid(e) {
      Wt = e;
    }
    static resetCaches() {
      (Mt.resetCache(), ht.resetCache(), $i.resetCache(), Ft.clear());
    }
  }
  class Gt {
    constructor(e, t) {
      ((this.reason = e), (this.explanation = t));
    }
    toMessage() {
      return this.explanation
        ? `${this.reason}: ${this.explanation}`
        : this.reason;
    }
  }
  const qt = [0, 31, 59, 90, 120, 151, 181, 212, 243, 273, 304, 334],
    Jt = [0, 31, 60, 91, 121, 152, 182, 213, 244, 274, 305, 335];
  function Yt(e, t) {
    return new Gt(
      "unit out of range",
      `you specified ${t} (of type ${typeof t}) as a ${e}, which is invalid`,
    );
  }
  function Kt(e, t, n) {
    const r = new Date(Date.UTC(e, t - 1, n));
    e < 100 && e >= 0 && r.setUTCFullYear(r.getUTCFullYear() - 1900);
    const i = r.getUTCDay();
    return 0 === i ? 7 : i;
  }
  function Xt(e, t, n) {
    return n + (En(e) ? Jt : qt)[t - 1];
  }
  function Qt(e, t) {
    const n = En(e) ? Jt : qt,
      r = n.findIndex((e) => e < t);
    return { month: r + 1, day: t - n[r] };
  }
  function en(e, t) {
    return ((e - t + 7) % 7) + 1;
  }
  function tn(e, t = 4, n = 1) {
    const { year: r, month: i, day: o } = e,
      s = Xt(r, i, o),
      a = en(Kt(r, i, o), n);
    let l,
      u = Math.floor((s - a + 14 - t) / 7);
    return (
      u < 1
        ? ((l = r - 1), (u = Mn(l, t, n)))
        : u > Mn(r, t, n)
          ? ((l = r + 1), (u = 1))
          : (l = r),
      { weekYear: l, weekNumber: u, weekday: a, ...Vn(e) }
    );
  }
  function nn(e, t = 4, n = 1) {
    const { weekYear: r, weekNumber: i, weekday: o } = e,
      s = en(Kt(r, 1, t), n),
      a = Cn(r);
    let l,
      u = 7 * i + o - s - 7 + t;
    u < 1
      ? ((l = r - 1), (u += Cn(l)))
      : u > a
        ? ((l = r + 1), (u -= Cn(r)))
        : (l = r);
    const { month: c, day: d } = Qt(l, u);
    return { year: l, month: c, day: d, ...Vn(e) };
  }
  function rn(e) {
    const { year: t, month: n, day: r } = e;
    return { year: t, ordinal: Xt(t, n, r), ...Vn(e) };
  }
  function on(e) {
    const { year: t, ordinal: n } = e,
      { month: r, day: i } = Qt(t, n);
    return { year: t, month: r, day: i, ...Vn(e) };
  }
  function sn(e, t) {
    if (!un(e.localWeekday) || !un(e.localWeekNumber) || !un(e.localWeekYear)) {
      if (!un(e.weekday) || !un(e.weekNumber) || !un(e.weekYear))
        throw new Ne(
          "Cannot mix locale-based week fields with ISO-based week fields",
        );
      return (
        un(e.localWeekday) || (e.weekday = e.localWeekday),
        un(e.localWeekNumber) || (e.weekNumber = e.localWeekNumber),
        un(e.localWeekYear) || (e.weekYear = e.localWeekYear),
        delete e.localWeekday,
        delete e.localWeekNumber,
        delete e.localWeekYear,
        {
          minDaysInFirstWeek: t.getMinDaysInFirstWeek(),
          startOfWeek: t.getStartOfWeek(),
        }
      );
    }
    return { minDaysInFirstWeek: 4, startOfWeek: 1 };
  }
  function an(e) {
    const t = dn(e.year),
      n = yn(e.month, 1, 12),
      r = yn(e.day, 1, Dn(e.year, e.month));
    return t
      ? n
        ? !r && Yt("day", e.day)
        : Yt("month", e.month)
      : Yt("year", e.year);
  }
  function ln(e) {
    const { hour: t, minute: n, second: r, millisecond: i } = e,
      o = yn(t, 0, 23) || (24 === t && 0 === n && 0 === r && 0 === i),
      s = yn(n, 0, 59),
      a = yn(r, 0, 59),
      l = yn(i, 0, 999);
    return o
      ? s
        ? a
          ? !l && Yt("millisecond", i)
          : Yt("second", r)
        : Yt("minute", n)
      : Yt("hour", t);
  }
  function un(e) {
    return void 0 === e;
  }
  function cn(e) {
    return "number" == typeof e;
  }
  function dn(e) {
    return "number" == typeof e && e % 1 == 0;
  }
  function mn() {
    try {
      return "undefined" != typeof Intl && !!Intl.RelativeTimeFormat;
    } catch (e) {
      return !1;
    }
  }
  function hn() {
    try {
      return (
        "undefined" != typeof Intl &&
        !!Intl.Locale &&
        ("weekInfo" in Intl.Locale.prototype ||
          "getWeekInfo" in Intl.Locale.prototype)
      );
    } catch (e) {
      return !1;
    }
  }
  function fn(e, t, n) {
    if (0 !== e.length)
      return e.reduce((e, r) => {
        const i = [t(r), r];
        return e && n(e[0], i[0]) === e[0] ? e : i;
      }, null)[1];
  }
  function pn(e, t) {
    return Object.prototype.hasOwnProperty.call(e, t);
  }
  function gn(e) {
    if (null == e) return null;
    if ("object" != typeof e) throw new Le("Week settings must be an object");
    if (
      !yn(e.firstDay, 1, 7) ||
      !yn(e.minimalDays, 1, 7) ||
      !Array.isArray(e.weekend) ||
      e.weekend.some((e) => !yn(e, 1, 7))
    )
      throw new Le("Invalid week settings");
    return {
      firstDay: e.firstDay,
      minimalDays: e.minimalDays,
      weekend: Array.from(e.weekend),
    };
  }
  function yn(e, t, n) {
    return dn(e) && e >= t && e <= n;
  }
  function bn(e, t = 2) {
    let n;
    return (
      (n =
        e < 0 ? "-" + ("" + -e).padStart(t, "0") : ("" + e).padStart(t, "0")),
      n
    );
  }
  function wn(e) {
    return un(e) || null === e || "" === e ? void 0 : parseInt(e, 10);
  }
  function Sn(e) {
    return un(e) || null === e || "" === e ? void 0 : parseFloat(e);
  }
  function vn(e) {
    if (!un(e) && null !== e && "" !== e) {
      const t = 1e3 * parseFloat("0." + e);
      return Math.floor(t);
    }
  }
  function kn(e, t, n = "round") {
    const r = 10 ** t;
    switch (n) {
      case "expand":
        return e > 0 ? Math.ceil(e * r) / r : Math.floor(e * r) / r;
      case "trunc":
        return Math.trunc(e * r) / r;
      case "round":
        return Math.round(e * r) / r;
      case "floor":
        return Math.floor(e * r) / r;
      case "ceil":
        return Math.ceil(e * r) / r;
      default:
        throw new RangeError(`Value rounding ${n} is out of range`);
    }
  }
  function En(e) {
    return e % 4 == 0 && (e % 100 != 0 || e % 400 == 0);
  }
  function Cn(e) {
    return En(e) ? 366 : 365;
  }
  function Dn(e, t) {
    const n = (r = t - 1) - 12 * Math.floor(r / 12) + 1;
    var r;
    return 2 === n
      ? En(e + (t - n) / 12)
        ? 29
        : 28
      : [31, null, 31, 30, 31, 30, 31, 31, 30, 31, 30, 31][n - 1];
  }
  function Tn(e) {
    let t = Date.UTC(
      e.year,
      e.month - 1,
      e.day,
      e.hour,
      e.minute,
      e.second,
      e.millisecond,
    );
    return (
      e.year < 100 &&
        e.year >= 0 &&
        ((t = new Date(t)), t.setUTCFullYear(e.year, e.month - 1, e.day)),
      +t
    );
  }
  function On(e, t, n) {
    return -en(Kt(e, 1, t), n) + t - 1;
  }
  function Mn(e, t = 4, n = 1) {
    const r = On(e, t, n),
      i = On(e + 1, t, n);
    return (Cn(e) - r + i) / 7;
  }
  function An(e) {
    return e > 99 ? e : e > Bt.twoDigitCutoffYear ? 1900 + e : 2e3 + e;
  }
  function xn(e, t, n, r = null) {
    const i = new Date(e),
      o = {
        hourCycle: "h23",
        year: "numeric",
        month: "2-digit",
        day: "2-digit",
        hour: "2-digit",
        minute: "2-digit",
      };
    r && (o.timeZone = r);
    const s = { timeZoneName: t, ...o },
      a = new Intl.DateTimeFormat(n, s)
        .formatToParts(i)
        .find((e) => "timezonename" === e.type.toLowerCase());
    return a ? a.value : null;
  }
  function In(e, t) {
    let n = parseInt(e, 10);
    Number.isNaN(n) && (n = 0);
    const r = parseInt(t, 10) || 0;
    return 60 * n + (n < 0 || Object.is(n, -0) ? -r : r);
  }
  function Nn(e) {
    const t = Number(e);
    if ("boolean" == typeof e || "" === e || !Number.isFinite(t))
      throw new Le(`Invalid unit value ${e}`);
    return t;
  }
  function Rn(e, t) {
    const n = {};
    for (const r in e)
      if (pn(e, r)) {
        const i = e[r];
        if (null == i) continue;
        n[t(r)] = Nn(i);
      }
    return n;
  }
  function Ln(e, t) {
    const n = Math.trunc(Math.abs(e / 60)),
      r = Math.trunc(Math.abs(e % 60)),
      i = e >= 0 ? "+" : "-";
    switch (t) {
      case "short":
        return `${i}${bn(n, 2)}:${bn(r, 2)}`;
      case "narrow":
        return `${i}${n}${r > 0 ? `:${r}` : ""}`;
      case "techie":
        return `${i}${bn(n, 2)}${bn(r, 2)}`;
      default:
        throw new RangeError(
          `Value format ${t} is out of range for property format`,
        );
    }
  }
  function Vn(e) {
    return (function (e) {
      return ["hour", "minute", "second", "millisecond"].reduce(
        (t, n) => ((t[n] = e[n]), t),
        {},
      );
    })(e);
  }
  const Fn = [
      "January",
      "February",
      "March",
      "April",
      "May",
      "June",
      "July",
      "August",
      "September",
      "October",
      "November",
      "December",
    ],
    Pn = [
      "Jan",
      "Feb",
      "Mar",
      "Apr",
      "May",
      "Jun",
      "Jul",
      "Aug",
      "Sep",
      "Oct",
      "Nov",
      "Dec",
    ],
    Wn = ["J", "F", "M", "A", "M", "J", "J", "A", "S", "O", "N", "D"];
  function jn(e) {
    switch (e) {
      case "narrow":
        return [...Wn];
      case "short":
        return [...Pn];
      case "long":
        return [...Fn];
      case "numeric":
        return ["1", "2", "3", "4", "5", "6", "7", "8", "9", "10", "11", "12"];
      case "2-digit":
        return [
          "01",
          "02",
          "03",
          "04",
          "05",
          "06",
          "07",
          "08",
          "09",
          "10",
          "11",
          "12",
        ];
      default:
        return null;
    }
  }
  const zn = [
      "Monday",
      "Tuesday",
      "Wednesday",
      "Thursday",
      "Friday",
      "Saturday",
      "Sunday",
    ],
    $n = ["Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun"],
    _n = ["M", "T", "W", "T", "F", "S", "S"];
  function Un(e) {
    switch (e) {
      case "narrow":
        return [..._n];
      case "short":
        return [...$n];
      case "long":
        return [...zn];
      case "numeric":
        return ["1", "2", "3", "4", "5", "6", "7"];
      default:
        return null;
    }
  }
  const Zn = ["AM", "PM"],
    Hn = ["Before Christ", "Anno Domini"],
    Bn = ["BC", "AD"],
    Gn = ["B", "A"];
  function qn(e) {
    switch (e) {
      case "narrow":
        return [...Gn];
      case "short":
        return [...Bn];
      case "long":
        return [...Hn];
      default:
        return null;
    }
  }
  function Jn(e, t) {
    let n = "";
    for (const r of e) r.literal ? (n += r.val) : (n += t(r.val));
    return n;
  }
  const Yn = {
    D: je,
    DD: ze,
    DDD: _e,
    DDDD: Ue,
    t: Ze,
    tt: He,
    ttt: Be,
    tttt: Ge,
    T: qe,
    TT: Je,
    TTT: Ye,
    TTTT: Ke,
    f: Xe,
    ff: et,
    fff: rt,
    ffff: ot,
    F: Qe,
    FF: tt,
    FFF: it,
    FFFF: st,
  };
  class Kn {
    static create(e, t = {}) {
      return new Kn(e, t);
    }
    static parseFormat(e) {
      let t = null,
        n = "",
        r = !1;
      const i = [];
      for (let o = 0; o < e.length; o++) {
        const s = e.charAt(o);
        "'" === s
          ? ((n.length > 0 || r) &&
              i.push({
                literal: r || /^\s+$/.test(n),
                val: "" === n ? "'" : n,
              }),
            (t = null),
            (n = ""),
            (r = !r))
          : r || s === t
            ? (n += s)
            : (n.length > 0 && i.push({ literal: /^\s+$/.test(n), val: n }),
              (n = s),
              (t = s));
      }
      return (
        n.length > 0 && i.push({ literal: r || /^\s+$/.test(n), val: n }),
        i
      );
    }
    static macroTokenToFormatOpts(e) {
      return Yn[e];
    }
    constructor(e, t) {
      ((this.opts = t), (this.loc = e), (this.systemLoc = null));
    }
    formatWithSystemDefault(e, t) {
      return (
        null === this.systemLoc &&
          (this.systemLoc = this.loc.redefaultToSystem()),
        this.systemLoc.dtFormatter(e, { ...this.opts, ...t }).format()
      );
    }
    dtFormatter(e, t = {}) {
      return this.loc.dtFormatter(e, { ...this.opts, ...t });
    }
    formatDateTime(e, t) {
      return this.dtFormatter(e, t).format();
    }
    formatDateTimeParts(e, t) {
      return this.dtFormatter(e, t).formatToParts();
    }
    formatInterval(e, t) {
      return this.dtFormatter(e.start, t).dtf.formatRange(
        e.start.toJSDate(),
        e.end.toJSDate(),
      );
    }
    resolvedOptions(e, t) {
      return this.dtFormatter(e, t).resolvedOptions();
    }
    num(e, t = 0, n = void 0) {
      if (this.opts.forceSimple) return bn(e, t);
      const r = { ...this.opts };
      return (
        t > 0 && (r.padTo = t),
        n && (r.signDisplay = n),
        this.loc.numberFormatter(r).format(e)
      );
    }
    formatDateTimeFromString(e, t) {
      const n = "en" === this.loc.listingMode(),
        r = this.loc.outputCalendar && "gregory" !== this.loc.outputCalendar,
        i = (t, n) => this.loc.extract(e, t, n),
        o = (t) =>
          e.isOffsetFixed && 0 === e.offset && t.allowZ
            ? "Z"
            : e.isValid
              ? e.zone.formatOffset(e.ts, t.format)
              : "",
        s = (t, r) =>
          n
            ? (function (e, t) {
                return jn(t)[e.month - 1];
              })(e, t)
            : i(r ? { month: t } : { month: t, day: "numeric" }, "month"),
        a = (t, r) =>
          n
            ? (function (e, t) {
                return Un(t)[e.weekday - 1];
              })(e, t)
            : i(
                r
                  ? { weekday: t }
                  : { weekday: t, month: "long", day: "numeric" },
                "weekday",
              ),
        l = (t) => {
          const n = Kn.macroTokenToFormatOpts(t);
          return n ? this.formatWithSystemDefault(e, n) : t;
        },
        u = (t) =>
          n
            ? (function (e, t) {
                return qn(t)[e.year < 0 ? 0 : 1];
              })(e, t)
            : i({ era: t }, "era");
      return Jn(Kn.parseFormat(t), (t) => {
        switch (t) {
          case "S":
            return this.num(e.millisecond);
          case "u":
          case "SSS":
            return this.num(e.millisecond, 3);
          case "s":
            return this.num(e.second);
          case "ss":
            return this.num(e.second, 2);
          case "uu":
            return this.num(Math.floor(e.millisecond / 10), 2);
          case "uuu":
            return this.num(Math.floor(e.millisecond / 100));
          case "m":
            return this.num(e.minute);
          case "mm":
            return this.num(e.minute, 2);
          case "h":
            return this.num(e.hour % 12 == 0 ? 12 : e.hour % 12);
          case "hh":
            return this.num(e.hour % 12 == 0 ? 12 : e.hour % 12, 2);
          case "H":
            return this.num(e.hour);
          case "HH":
            return this.num(e.hour, 2);
          case "Z":
            return o({ format: "narrow", allowZ: this.opts.allowZ });
          case "ZZ":
            return o({ format: "short", allowZ: this.opts.allowZ });
          case "ZZZ":
            return o({ format: "techie", allowZ: this.opts.allowZ });
          case "ZZZZ":
            return e.zone.offsetName(e.ts, {
              format: "short",
              locale: this.loc.locale,
            });
          case "ZZZZZ":
            return e.zone.offsetName(e.ts, {
              format: "long",
              locale: this.loc.locale,
            });
          case "z":
            return e.zoneName;
          case "a":
            return n
              ? (function (e) {
                  return Zn[e.hour < 12 ? 0 : 1];
                })(e)
              : i({ hour: "numeric", hourCycle: "h12" }, "dayperiod");
          case "d":
            return r ? i({ day: "numeric" }, "day") : this.num(e.day);
          case "dd":
            return r ? i({ day: "2-digit" }, "day") : this.num(e.day, 2);
          case "c":
          case "E":
            return this.num(e.weekday);
          case "ccc":
            return a("short", !0);
          case "cccc":
            return a("long", !0);
          case "ccccc":
            return a("narrow", !0);
          case "EEE":
            return a("short", !1);
          case "EEEE":
            return a("long", !1);
          case "EEEEE":
            return a("narrow", !1);
          case "L":
            return r
              ? i({ month: "numeric", day: "numeric" }, "month")
              : this.num(e.month);
          case "LL":
            return r
              ? i({ month: "2-digit", day: "numeric" }, "month")
              : this.num(e.month, 2);
          case "LLL":
            return s("short", !0);
          case "LLLL":
            return s("long", !0);
          case "LLLLL":
            return s("narrow", !0);
          case "M":
            return r ? i({ month: "numeric" }, "month") : this.num(e.month);
          case "MM":
            return r ? i({ month: "2-digit" }, "month") : this.num(e.month, 2);
          case "MMM":
            return s("short", !1);
          case "MMMM":
            return s("long", !1);
          case "MMMMM":
            return s("narrow", !1);
          case "y":
            return r ? i({ year: "numeric" }, "year") : this.num(e.year);
          case "yy":
            return r
              ? i({ year: "2-digit" }, "year")
              : this.num(e.year.toString().slice(-2), 2);
          case "yyyy":
            return r ? i({ year: "numeric" }, "year") : this.num(e.year, 4);
          case "yyyyyy":
            return r ? i({ year: "numeric" }, "year") : this.num(e.year, 6);
          case "G":
            return u("short");
          case "GG":
            return u("long");
          case "GGGGG":
            return u("narrow");
          case "kk":
            return this.num(e.weekYear.toString().slice(-2), 2);
          case "kkkk":
            return this.num(e.weekYear, 4);
          case "W":
            return this.num(e.weekNumber);
          case "WW":
            return this.num(e.weekNumber, 2);
          case "n":
            return this.num(e.localWeekNumber);
          case "nn":
            return this.num(e.localWeekNumber, 2);
          case "ii":
            return this.num(e.localWeekYear.toString().slice(-2), 2);
          case "iiii":
            return this.num(e.localWeekYear, 4);
          case "o":
            return this.num(e.ordinal);
          case "ooo":
            return this.num(e.ordinal, 3);
          case "q":
            return this.num(e.quarter);
          case "qq":
            return this.num(e.quarter, 2);
          case "X":
            return this.num(Math.floor(e.ts / 1e3));
          case "x":
            return this.num(e.ts);
          default:
            return l(t);
        }
      });
    }
    formatDurationFromString(e, t) {
      const n = "negativeLargestOnly" === this.opts.signMode ? -1 : 1,
        r = (e) => {
          switch (e[0]) {
            case "S":
              return "milliseconds";
            case "s":
              return "seconds";
            case "m":
              return "minutes";
            case "h":
              return "hours";
            case "d":
              return "days";
            case "w":
              return "weeks";
            case "M":
              return "months";
            case "y":
              return "years";
            default:
              return null;
          }
        },
        i = Kn.parseFormat(t),
        o = i.reduce((e, { literal: t, val: n }) => (t ? e : e.concat(n)), []),
        s = e.shiftTo(...o.map(r).filter((e) => e));
      return Jn(
        i,
        ((e, t) => (i) => {
          const o = r(i);
          if (o) {
            const r = t.isNegativeDuration && o !== t.largestUnit ? n : 1;
            let s;
            return (
              (s =
                "negativeLargestOnly" === this.opts.signMode &&
                o !== t.largestUnit
                  ? "never"
                  : "all" === this.opts.signMode
                    ? "always"
                    : "auto"),
              this.num(e.get(o) * r, i.length, s)
            );
          }
          return i;
        })(s, {
          isNegativeDuration: s < 0,
          largestUnit: Object.keys(s.values)[0],
        }),
      );
    }
  }
  const Xn =
    /[A-Za-z_+-]{1,256}(?::?\/[A-Za-z0-9_+-]{1,256}(?:\/[A-Za-z0-9_+-]{1,256})?)?/;
  function Qn(...e) {
    const t = e.reduce((e, t) => e + t.source, "");
    return RegExp(`^${t}$`);
  }
  function er(...e) {
    return (t) =>
      e
        .reduce(
          ([e, n, r], i) => {
            const [o, s, a] = i(t, r);
            return [{ ...e, ...o }, s || n, a];
          },
          [{}, null, 1],
        )
        .slice(0, 2);
  }
  function tr(e, ...t) {
    if (null == e) return [null, null];
    for (const [n, r] of t) {
      const t = n.exec(e);
      if (t) return r(t);
    }
    return [null, null];
  }
  function nr(...e) {
    return (t, n) => {
      const r = {};
      let i;
      for (i = 0; i < e.length; i++) r[e[i]] = wn(t[n + i]);
      return [r, null, n + i];
    };
  }
  const rr = /(?:([Zz])|([+-]\d\d)(?::?(\d\d))?)/,
    ir = /(\d\d)(?::?(\d\d)(?::?(\d\d)(?:[.,](\d{1,30}))?)?)?/,
    or = RegExp(`${ir.source}(?:${rr.source}?(?:\\[(${Xn.source})\\])?)?`),
    sr = RegExp(`(?:[Tt]${or.source})?`),
    ar = nr("weekYear", "weekNumber", "weekDay"),
    lr = nr("year", "ordinal"),
    ur = RegExp(`${ir.source} ?(?:${rr.source}|(${Xn.source}))?`),
    cr = RegExp(`(?: ${ur.source})?`);
  function dr(e, t, n) {
    const r = e[t];
    return un(r) ? n : wn(r);
  }
  function mr(e, t) {
    return [
      {
        hours: dr(e, t, 0),
        minutes: dr(e, t + 1, 0),
        seconds: dr(e, t + 2, 0),
        milliseconds: vn(e[t + 3]),
      },
      null,
      t + 4,
    ];
  }
  function hr(e, t) {
    const n = !e[t] && !e[t + 1],
      r = In(e[t + 1], e[t + 2]);
    return [{}, n ? null : xt.instance(r), t + 3];
  }
  function fr(e, t) {
    return [{}, e[t] ? ht.create(e[t]) : null, t + 1];
  }
  const pr = RegExp(`^T?${ir.source}$`),
    gr =
      /^-?P(?:(?:(-?\d{1,20}(?:\.\d{1,20})?)Y)?(?:(-?\d{1,20}(?:\.\d{1,20})?)M)?(?:(-?\d{1,20}(?:\.\d{1,20})?)W)?(?:(-?\d{1,20}(?:\.\d{1,20})?)D)?(?:T(?:(-?\d{1,20}(?:\.\d{1,20})?)H)?(?:(-?\d{1,20}(?:\.\d{1,20})?)M)?(?:(-?\d{1,20})(?:[.,](-?\d{1,20}))?S)?)?)$/;
  function yr(e) {
    const [t, n, r, i, o, s, a, l, u] = e,
      c = "-" === t[0],
      d = l && "-" === l[0],
      m = (e, t = !1) => (void 0 !== e && (t || (e && c)) ? -e : e);
    return [
      {
        years: m(Sn(n)),
        months: m(Sn(r)),
        weeks: m(Sn(i)),
        days: m(Sn(o)),
        hours: m(Sn(s)),
        minutes: m(Sn(a)),
        seconds: m(Sn(l), "-0" === l),
        milliseconds: m(vn(u), d),
      },
    ];
  }
  const br = {
    GMT: 0,
    EDT: -240,
    EST: -300,
    CDT: -300,
    CST: -360,
    MDT: -360,
    MST: -420,
    PDT: -420,
    PST: -480,
  };
  function wr(e, t, n, r, i, o, s) {
    const a = {
      year: 2 === t.length ? An(wn(t)) : wn(t),
      month: Pn.indexOf(n) + 1,
      day: wn(r),
      hour: wn(i),
      minute: wn(o),
    };
    return (
      s && (a.second = wn(s)),
      e && (a.weekday = e.length > 3 ? zn.indexOf(e) + 1 : $n.indexOf(e) + 1),
      a
    );
  }
  const Sr =
    /^(?:(Mon|Tue|Wed|Thu|Fri|Sat|Sun),\s)?(\d{1,2})\s(Jan|Feb|Mar|Apr|May|Jun|Jul|Aug|Sep|Oct|Nov|Dec)\s(\d{2,4})\s(\d\d):(\d\d)(?::(\d\d))?\s(?:(UT|GMT|[ECMP][SD]T)|([Zz])|(?:([+-]\d\d)(\d\d)))$/;
  function vr(e) {
    const [, t, n, r, i, o, s, a, l, u, c, d] = e,
      m = wr(t, i, r, n, o, s, a);
    let h;
    return ((h = l ? br[l] : u ? 0 : In(c, d)), [m, new xt(h)]);
  }
  const kr =
      /^(Mon|Tue|Wed|Thu|Fri|Sat|Sun), (\d\d) (Jan|Feb|Mar|Apr|May|Jun|Jul|Aug|Sep|Oct|Nov|Dec) (\d{4}) (\d\d):(\d\d):(\d\d) GMT$/,
    Er =
      /^(Monday|Tuesday|Wednesday|Thursday|Friday|Saturday|Sunday), (\d\d)-(Jan|Feb|Mar|Apr|May|Jun|Jul|Aug|Sep|Oct|Nov|Dec)-(\d\d) (\d\d):(\d\d):(\d\d) GMT$/,
    Cr =
      /^(Mon|Tue|Wed|Thu|Fri|Sat|Sun) (Jan|Feb|Mar|Apr|May|Jun|Jul|Aug|Sep|Oct|Nov|Dec) ( \d|\d\d) (\d\d):(\d\d):(\d\d) (\d{4})$/;
  function Dr(e) {
    const [, t, n, r, i, o, s, a] = e;
    return [wr(t, i, r, n, o, s, a), xt.utcInstance];
  }
  function Tr(e) {
    const [, t, n, r, i, o, s, a] = e;
    return [wr(t, a, n, r, i, o, s), xt.utcInstance];
  }
  const Or = Qn(/([+-]\d{6}|\d{4})(?:-?(\d\d)(?:-?(\d\d))?)?/, sr),
    Mr = Qn(/(\d{4})-?W(\d\d)(?:-?(\d))?/, sr),
    Ar = Qn(/(\d{4})-?(\d{3})/, sr),
    xr = Qn(or),
    Ir = er(
      function (e, t) {
        return [
          { year: dr(e, t), month: dr(e, t + 1, 1), day: dr(e, t + 2, 1) },
          null,
          t + 3,
        ];
      },
      mr,
      hr,
      fr,
    ),
    Nr = er(ar, mr, hr, fr),
    Rr = er(lr, mr, hr, fr),
    Lr = er(mr, hr, fr),
    Vr = er(mr),
    Fr = Qn(/(\d{4})-(\d\d)-(\d\d)/, cr),
    Pr = Qn(ur),
    Wr = er(mr, hr, fr),
    jr = "Invalid Duration",
    zr = {
      weeks: {
        days: 7,
        hours: 168,
        minutes: 10080,
        seconds: 604800,
        milliseconds: 6048e5,
      },
      days: { hours: 24, minutes: 1440, seconds: 86400, milliseconds: 864e5 },
      hours: { minutes: 60, seconds: 3600, milliseconds: 36e5 },
      minutes: { seconds: 60, milliseconds: 6e4 },
      seconds: { milliseconds: 1e3 },
    },
    $r = {
      years: {
        quarters: 4,
        months: 12,
        weeks: 52,
        days: 365,
        hours: 8760,
        minutes: 525600,
        seconds: 31536e3,
        milliseconds: 31536e6,
      },
      quarters: {
        months: 3,
        weeks: 13,
        days: 91,
        hours: 2184,
        minutes: 131040,
        seconds: 7862400,
        milliseconds: 78624e5,
      },
      months: {
        weeks: 4,
        days: 30,
        hours: 720,
        minutes: 43200,
        seconds: 2592e3,
        milliseconds: 2592e6,
      },
      ...zr,
    },
    _r = {
      years: {
        quarters: 4,
        months: 12,
        weeks: 52.1775,
        days: 365.2425,
        hours: 8765.82,
        minutes: 525949.2,
        seconds: 525949.2 * 60,
        milliseconds: 525949.2 * 60 * 1e3,
      },
      quarters: {
        months: 3,
        weeks: 13.044375,
        days: 91.310625,
        hours: 2191.455,
        minutes: 131487.3,
        seconds: (525949.2 * 60) / 4,
        milliseconds: 7889237999.999999,
      },
      months: {
        weeks: 4.3481250000000005,
        days: 30.436875,
        hours: 730.485,
        minutes: 43829.1,
        seconds: 2629746,
        milliseconds: 2629746e3,
      },
      ...zr,
    },
    Ur = [
      "years",
      "quarters",
      "months",
      "weeks",
      "days",
      "hours",
      "minutes",
      "seconds",
      "milliseconds",
    ],
    Zr = Ur.slice(0).reverse();
  function Hr(e, t, n = !1) {
    const r = {
      values: n ? t.values : { ...e.values, ...(t.values || {}) },
      loc: e.loc.clone(t.loc),
      conversionAccuracy: t.conversionAccuracy || e.conversionAccuracy,
      matrix: t.matrix || e.matrix,
    };
    return new Jr(r);
  }
  function Br(e, t) {
    let n = t.milliseconds ?? 0;
    for (const r of Zr.slice(1)) t[r] && (n += t[r] * e[r].milliseconds);
    return n;
  }
  function Gr(e, t) {
    const n = Br(e, t) < 0 ? -1 : 1;
    (Ur.reduceRight((r, i) => {
      if (un(t[i])) return r;
      if (r) {
        const o = t[r] * n,
          s = e[i][r],
          a = Math.floor(o / s);
        ((t[i] += a * n), (t[r] -= a * s * n));
      }
      return i;
    }, null),
      Ur.reduce((n, r) => {
        if (un(t[r])) return n;
        if (n) {
          const i = t[n] % 1;
          ((t[n] -= i), (t[r] += i * e[n][r]));
        }
        return r;
      }, null));
  }
  function qr(e) {
    const t = {};
    for (const [n, r] of Object.entries(e)) 0 !== r && (t[n] = r);
    return t;
  }
  class Jr {
    constructor(e) {
      const t = "longterm" === e.conversionAccuracy || !1;
      let n = t ? _r : $r;
      (e.matrix && (n = e.matrix),
        (this.values = e.values),
        (this.loc = e.loc || Mt.create()),
        (this.conversionAccuracy = t ? "longterm" : "casual"),
        (this.invalid = e.invalid || null),
        (this.matrix = n),
        (this.isLuxonDuration = !0));
    }
    static fromMillis(e, t) {
      return Jr.fromObject({ milliseconds: e }, t);
    }
    static fromObject(e, t = {}) {
      if (null == e || "object" != typeof e)
        throw new Le(
          "Duration.fromObject: argument expected to be an object, got " +
            (null === e ? "null" : typeof e),
        );
      return new Jr({
        values: Rn(e, Jr.normalizeUnit),
        loc: Mt.fromObject(t),
        conversionAccuracy: t.conversionAccuracy,
        matrix: t.matrix,
      });
    }
    static fromDurationLike(e) {
      if (cn(e)) return Jr.fromMillis(e);
      if (Jr.isDuration(e)) return e;
      if ("object" == typeof e) return Jr.fromObject(e);
      throw new Le(`Unknown duration argument ${e} of type ${typeof e}`);
    }
    static fromISO(e, t) {
      const [n] = (function (e) {
        return tr(e, [gr, yr]);
      })(e);
      return n
        ? Jr.fromObject(n, t)
        : Jr.invalid(
            "unparsable",
            `the input "${e}" can't be parsed as ISO 8601`,
          );
    }
    static fromISOTime(e, t) {
      const [n] = (function (e) {
        return tr(e, [pr, Vr]);
      })(e);
      return n
        ? Jr.fromObject(n, t)
        : Jr.invalid(
            "unparsable",
            `the input "${e}" can't be parsed as ISO 8601`,
          );
    }
    static invalid(e, t = null) {
      if (!e) throw new Le("need to specify a reason the Duration is invalid");
      const n = e instanceof Gt ? e : new Gt(e, t);
      if (Bt.throwOnInvalid) throw new Ie(n);
      return new Jr({ invalid: n });
    }
    static normalizeUnit(e) {
      const t = {
        year: "years",
        years: "years",
        quarter: "quarters",
        quarters: "quarters",
        month: "months",
        months: "months",
        week: "weeks",
        weeks: "weeks",
        day: "days",
        days: "days",
        hour: "hours",
        hours: "hours",
        minute: "minutes",
        minutes: "minutes",
        second: "seconds",
        seconds: "seconds",
        millisecond: "milliseconds",
        milliseconds: "milliseconds",
      }[e ? e.toLowerCase() : e];
      if (!t) throw new Re(e);
      return t;
    }
    static isDuration(e) {
      return (e && e.isLuxonDuration) || !1;
    }
    get locale() {
      return this.isValid ? this.loc.locale : null;
    }
    get numberingSystem() {
      return this.isValid ? this.loc.numberingSystem : null;
    }
    toFormat(e, t = {}) {
      const n = { ...t, floor: !1 !== t.round && !1 !== t.floor };
      return this.isValid
        ? Kn.create(this.loc, n).formatDurationFromString(this, e)
        : jr;
    }
    toHuman(e = {}) {
      if (!this.isValid) return jr;
      const t = !1 !== e.showZeros,
        n = Ur.map((n) => {
          const r = this.values[n];
          return un(r) || (0 === r && !t)
            ? null
            : this.loc
                .numberFormatter({
                  style: "unit",
                  unitDisplay: "long",
                  ...e,
                  unit: n.slice(0, -1),
                })
                .format(r);
        }).filter((e) => e);
      return this.loc
        .listFormatter({
          type: "conjunction",
          style: e.listStyle || "narrow",
          ...e,
        })
        .format(n);
    }
    toObject() {
      return this.isValid ? { ...this.values } : {};
    }
    toISO() {
      if (!this.isValid) return null;
      let e = "P";
      return (
        0 !== this.years && (e += this.years + "Y"),
        (0 === this.months && 0 === this.quarters) ||
          (e += this.months + 3 * this.quarters + "M"),
        0 !== this.weeks && (e += this.weeks + "W"),
        0 !== this.days && (e += this.days + "D"),
        (0 === this.hours &&
          0 === this.minutes &&
          0 === this.seconds &&
          0 === this.milliseconds) ||
          (e += "T"),
        0 !== this.hours && (e += this.hours + "H"),
        0 !== this.minutes && (e += this.minutes + "M"),
        (0 === this.seconds && 0 === this.milliseconds) ||
          (e += kn(this.seconds + this.milliseconds / 1e3, 3) + "S"),
        "P" === e && (e += "T0S"),
        e
      );
    }
    toISOTime(e = {}) {
      if (!this.isValid) return null;
      const t = this.toMillis();
      return t < 0 || t >= 864e5
        ? null
        : ((e = {
            suppressMilliseconds: !1,
            suppressSeconds: !1,
            includePrefix: !1,
            format: "extended",
            ...e,
            includeOffset: !1,
          }),
          $i.fromMillis(t, { zone: "UTC" }).toISOTime(e));
    }
    toJSON() {
      return this.toISO();
    }
    toString() {
      return this.toISO();
    }
    [Symbol.for("nodejs.util.inspect.custom")]() {
      return this.isValid
        ? `Duration { values: ${JSON.stringify(this.values)} }`
        : `Duration { Invalid, reason: ${this.invalidReason} }`;
    }
    toMillis() {
      return this.isValid ? Br(this.matrix, this.values) : NaN;
    }
    valueOf() {
      return this.toMillis();
    }
    plus(e) {
      if (!this.isValid) return this;
      const t = Jr.fromDurationLike(e),
        n = {};
      for (const e of Ur)
        (pn(t.values, e) || pn(this.values, e)) &&
          (n[e] = t.get(e) + this.get(e));
      return Hr(this, { values: n }, !0);
    }
    minus(e) {
      if (!this.isValid) return this;
      const t = Jr.fromDurationLike(e);
      return this.plus(t.negate());
    }
    mapUnits(e) {
      if (!this.isValid) return this;
      const t = {};
      for (const n of Object.keys(this.values)) t[n] = Nn(e(this.values[n], n));
      return Hr(this, { values: t }, !0);
    }
    get(e) {
      return this[Jr.normalizeUnit(e)];
    }
    set(e) {
      return this.isValid
        ? Hr(this, { values: { ...this.values, ...Rn(e, Jr.normalizeUnit) } })
        : this;
    }
    reconfigure({
      locale: e,
      numberingSystem: t,
      conversionAccuracy: n,
      matrix: r,
    } = {}) {
      return Hr(this, {
        loc: this.loc.clone({ locale: e, numberingSystem: t }),
        matrix: r,
        conversionAccuracy: n,
      });
    }
    as(e) {
      return this.isValid ? this.shiftTo(e).get(e) : NaN;
    }
    normalize() {
      if (!this.isValid) return this;
      const e = this.toObject();
      return (Gr(this.matrix, e), Hr(this, { values: e }, !0));
    }
    rescale() {
      return this.isValid
        ? Hr(this, { values: qr(this.normalize().shiftToAll().toObject()) }, !0)
        : this;
    }
    shiftTo(...e) {
      if (!this.isValid) return this;
      if (0 === e.length) return this;
      e = e.map((e) => Jr.normalizeUnit(e));
      const t = {},
        n = {},
        r = this.toObject();
      let i;
      for (const o of Ur)
        if (e.indexOf(o) >= 0) {
          i = o;
          let e = 0;
          for (const t in n) ((e += this.matrix[t][o] * n[t]), (n[t] = 0));
          cn(r[o]) && (e += r[o]);
          const s = Math.trunc(e);
          ((t[o] = s), (n[o] = (1e3 * e - 1e3 * s) / 1e3));
        } else cn(r[o]) && (n[o] = r[o]);
      for (const e in n)
        0 !== n[e] && (t[i] += e === i ? n[e] : n[e] / this.matrix[i][e]);
      return (Gr(this.matrix, t), Hr(this, { values: t }, !0));
    }
    shiftToAll() {
      return this.isValid
        ? this.shiftTo(
            "years",
            "months",
            "weeks",
            "days",
            "hours",
            "minutes",
            "seconds",
            "milliseconds",
          )
        : this;
    }
    negate() {
      if (!this.isValid) return this;
      const e = {};
      for (const t of Object.keys(this.values))
        e[t] = 0 === this.values[t] ? 0 : -this.values[t];
      return Hr(this, { values: e }, !0);
    }
    removeZeros() {
      return this.isValid ? Hr(this, { values: qr(this.values) }, !0) : this;
    }
    get years() {
      return this.isValid ? this.values.years || 0 : NaN;
    }
    get quarters() {
      return this.isValid ? this.values.quarters || 0 : NaN;
    }
    get months() {
      return this.isValid ? this.values.months || 0 : NaN;
    }
    get weeks() {
      return this.isValid ? this.values.weeks || 0 : NaN;
    }
    get days() {
      return this.isValid ? this.values.days || 0 : NaN;
    }
    get hours() {
      return this.isValid ? this.values.hours || 0 : NaN;
    }
    get minutes() {
      return this.isValid ? this.values.minutes || 0 : NaN;
    }
    get seconds() {
      return this.isValid ? this.values.seconds || 0 : NaN;
    }
    get milliseconds() {
      return this.isValid ? this.values.milliseconds || 0 : NaN;
    }
    get isValid() {
      return null === this.invalid;
    }
    get invalidReason() {
      return this.invalid ? this.invalid.reason : null;
    }
    get invalidExplanation() {
      return this.invalid ? this.invalid.explanation : null;
    }
    equals(e) {
      if (!this.isValid || !e.isValid) return !1;
      if (!this.loc.equals(e.loc)) return !1;
      function t(e, t) {
        return void 0 === e || 0 === e ? void 0 === t || 0 === t : e === t;
      }
      for (const n of Ur) if (!t(this.values[n], e.values[n])) return !1;
      return !0;
    }
  }
  const Yr = "Invalid Interval";
  class Kr {
    constructor(e) {
      ((this.s = e.start),
        (this.e = e.end),
        (this.invalid = e.invalid || null),
        (this.isLuxonInterval = !0));
    }
    static invalid(e, t = null) {
      if (!e) throw new Le("need to specify a reason the Interval is invalid");
      const n = e instanceof Gt ? e : new Gt(e, t);
      if (Bt.throwOnInvalid) throw new xe(n);
      return new Kr({ invalid: n });
    }
    static fromDateTimes(e, t) {
      const n = _i(e),
        r = _i(t),
        i = (function (e, t) {
          return e && e.isValid
            ? t && t.isValid
              ? t < e
                ? Kr.invalid(
                    "end before start",
                    `The end of an interval must be after its start, but you had start=${e.toISO()} and end=${t.toISO()}`,
                  )
                : null
              : Kr.invalid("missing or invalid end")
            : Kr.invalid("missing or invalid start");
        })(n, r);
      return null == i ? new Kr({ start: n, end: r }) : i;
    }
    static after(e, t) {
      const n = Jr.fromDurationLike(t),
        r = _i(e);
      return Kr.fromDateTimes(r, r.plus(n));
    }
    static before(e, t) {
      const n = Jr.fromDurationLike(t),
        r = _i(e);
      return Kr.fromDateTimes(r.minus(n), r);
    }
    static fromISO(e, t) {
      const [n, r] = (e || "").split("/", 2);
      if (n && r) {
        let e, i, o, s;
        try {
          ((e = $i.fromISO(n, t)), (i = e.isValid));
        } catch (r) {
          i = !1;
        }
        try {
          ((o = $i.fromISO(r, t)), (s = o.isValid));
        } catch (r) {
          s = !1;
        }
        if (i && s) return Kr.fromDateTimes(e, o);
        if (i) {
          const n = Jr.fromISO(r, t);
          if (n.isValid) return Kr.after(e, n);
        } else if (s) {
          const e = Jr.fromISO(n, t);
          if (e.isValid) return Kr.before(o, e);
        }
      }
      return Kr.invalid(
        "unparsable",
        `the input "${e}" can't be parsed as ISO 8601`,
      );
    }
    static isInterval(e) {
      return (e && e.isLuxonInterval) || !1;
    }
    get start() {
      return this.isValid ? this.s : null;
    }
    get end() {
      return this.isValid ? this.e : null;
    }
    get lastDateTime() {
      return this.isValid && this.e ? this.e.minus(1) : null;
    }
    get isValid() {
      return null === this.invalidReason;
    }
    get invalidReason() {
      return this.invalid ? this.invalid.reason : null;
    }
    get invalidExplanation() {
      return this.invalid ? this.invalid.explanation : null;
    }
    length(e = "milliseconds") {
      return this.isValid ? this.toDuration(e).get(e) : NaN;
    }
    count(e = "milliseconds", t) {
      if (!this.isValid) return NaN;
      const n = this.start.startOf(e, t);
      let r;
      return (
        (r = t?.useLocaleWeeks
          ? this.end.reconfigure({ locale: n.locale })
          : this.end),
        (r = r.startOf(e, t)),
        Math.floor(r.diff(n, e).get(e)) + (r.valueOf() !== this.end.valueOf())
      );
    }
    hasSame(e) {
      return (
        !!this.isValid && (this.isEmpty() || this.e.minus(1).hasSame(this.s, e))
      );
    }
    isEmpty() {
      return this.s.valueOf() === this.e.valueOf();
    }
    isAfter(e) {
      return !!this.isValid && this.s > e;
    }
    isBefore(e) {
      return !!this.isValid && this.e <= e;
    }
    contains(e) {
      return !!this.isValid && this.s <= e && this.e > e;
    }
    set({ start: e, end: t } = {}) {
      return this.isValid ? Kr.fromDateTimes(e || this.s, t || this.e) : this;
    }
    splitAt(...e) {
      if (!this.isValid) return [];
      const t = e
          .map(_i)
          .filter((e) => this.contains(e))
          .sort((e, t) => e.toMillis() - t.toMillis()),
        n = [];
      let { s: r } = this,
        i = 0;
      for (; r < this.e; ) {
        const e = t[i] || this.e,
          o = +e > +this.e ? this.e : e;
        (n.push(Kr.fromDateTimes(r, o)), (r = o), (i += 1));
      }
      return n;
    }
    splitBy(e) {
      const t = Jr.fromDurationLike(e);
      if (!this.isValid || !t.isValid || 0 === t.as("milliseconds")) return [];
      let n,
        { s: r } = this,
        i = 1;
      const o = [];
      for (; r < this.e; ) {
        const e = this.start.plus(t.mapUnits((e) => e * i));
        ((n = +e > +this.e ? this.e : e),
          o.push(Kr.fromDateTimes(r, n)),
          (r = n),
          (i += 1));
      }
      return o;
    }
    divideEqually(e) {
      return this.isValid ? this.splitBy(this.length() / e).slice(0, e) : [];
    }
    overlaps(e) {
      return this.e > e.s && this.s < e.e;
    }
    abutsStart(e) {
      return !!this.isValid && +this.e === +e.s;
    }
    abutsEnd(e) {
      return !!this.isValid && +e.e === +this.s;
    }
    engulfs(e) {
      return !!this.isValid && this.s <= e.s && this.e >= e.e;
    }
    equals(e) {
      return (
        !(!this.isValid || !e.isValid) &&
        this.s.equals(e.s) &&
        this.e.equals(e.e)
      );
    }
    intersection(e) {
      if (!this.isValid) return this;
      const t = this.s > e.s ? this.s : e.s,
        n = this.e < e.e ? this.e : e.e;
      return t >= n ? null : Kr.fromDateTimes(t, n);
    }
    union(e) {
      if (!this.isValid) return this;
      const t = this.s < e.s ? this.s : e.s,
        n = this.e > e.e ? this.e : e.e;
      return Kr.fromDateTimes(t, n);
    }
    static merge(e) {
      const [t, n] = e
        .sort((e, t) => e.s - t.s)
        .reduce(
          ([e, t], n) =>
            t
              ? t.overlaps(n) || t.abutsStart(n)
                ? [e, t.union(n)]
                : [e.concat([t]), n]
              : [e, n],
          [[], null],
        );
      return (n && t.push(n), t);
    }
    static xor(e) {
      let t = null,
        n = 0;
      const r = [],
        i = e.map((e) => [
          { time: e.s, type: "s" },
          { time: e.e, type: "e" },
        ]),
        o = Array.prototype.concat(...i).sort((e, t) => e.time - t.time);
      for (const e of o)
        ((n += "s" === e.type ? 1 : -1),
          1 === n
            ? (t = e.time)
            : (t && +t !== +e.time && r.push(Kr.fromDateTimes(t, e.time)),
              (t = null)));
      return Kr.merge(r);
    }
    difference(...e) {
      return Kr.xor([this].concat(e))
        .map((e) => this.intersection(e))
        .filter((e) => e && !e.isEmpty());
    }
    toString() {
      return this.isValid ? `[${this.s.toISO()} – ${this.e.toISO()})` : Yr;
    }
    [Symbol.for("nodejs.util.inspect.custom")]() {
      return this.isValid
        ? `Interval { start: ${this.s.toISO()}, end: ${this.e.toISO()} }`
        : `Interval { Invalid, reason: ${this.invalidReason} }`;
    }
    toLocaleString(e = je, t = {}) {
      return this.isValid
        ? Kn.create(this.s.loc.clone(t), e).formatInterval(this)
        : Yr;
    }
    toISO(e) {
      return this.isValid ? `${this.s.toISO(e)}/${this.e.toISO(e)}` : Yr;
    }
    toISODate() {
      return this.isValid ? `${this.s.toISODate()}/${this.e.toISODate()}` : Yr;
    }
    toISOTime(e) {
      return this.isValid
        ? `${this.s.toISOTime(e)}/${this.e.toISOTime(e)}`
        : Yr;
    }
    toFormat(e, { separator: t = " – " } = {}) {
      return this.isValid
        ? `${this.s.toFormat(e)}${t}${this.e.toFormat(e)}`
        : Yr;
    }
    toDuration(e, t) {
      return this.isValid
        ? this.e.diff(this.s, e, t)
        : Jr.invalid(this.invalidReason);
    }
    mapEndpoints(e) {
      return Kr.fromDateTimes(e(this.s), e(this.e));
    }
  }
  class Xr {
    static hasDST(e = Bt.defaultZone) {
      const t = $i.now().setZone(e).set({ month: 12 });
      return !e.isUniversal && t.offset !== t.set({ month: 6 }).offset;
    }
    static isValidIANAZone(e) {
      return ht.isValidZone(e);
    }
    static normalizeZone(e) {
      return Nt(e, Bt.defaultZone);
    }
    static getStartOfWeek({ locale: e = null, locObj: t = null } = {}) {
      return (t || Mt.create(e)).getStartOfWeek();
    }
    static getMinimumDaysInFirstWeek({
      locale: e = null,
      locObj: t = null,
    } = {}) {
      return (t || Mt.create(e)).getMinDaysInFirstWeek();
    }
    static getWeekendWeekdays({ locale: e = null, locObj: t = null } = {}) {
      return (t || Mt.create(e)).getWeekendDays().slice();
    }
    static months(
      e = "long",
      {
        locale: t = null,
        numberingSystem: n = null,
        locObj: r = null,
        outputCalendar: i = "gregory",
      } = {},
    ) {
      return (r || Mt.create(t, n, i)).months(e);
    }
    static monthsFormat(
      e = "long",
      {
        locale: t = null,
        numberingSystem: n = null,
        locObj: r = null,
        outputCalendar: i = "gregory",
      } = {},
    ) {
      return (r || Mt.create(t, n, i)).months(e, !0);
    }
    static weekdays(
      e = "long",
      { locale: t = null, numberingSystem: n = null, locObj: r = null } = {},
    ) {
      return (r || Mt.create(t, n, null)).weekdays(e);
    }
    static weekdaysFormat(
      e = "long",
      { locale: t = null, numberingSystem: n = null, locObj: r = null } = {},
    ) {
      return (r || Mt.create(t, n, null)).weekdays(e, !0);
    }
    static meridiems({ locale: e = null } = {}) {
      return Mt.create(e).meridiems();
    }
    static eras(e = "short", { locale: t = null } = {}) {
      return Mt.create(t, null, "gregory").eras(e);
    }
    static features() {
      return { relative: mn(), localeWeek: hn() };
    }
  }
  function Qr(e, t) {
    const n = (e) => e.toUTC(0, { keepLocalTime: !0 }).startOf("day").valueOf(),
      r = n(t) - n(e);
    return Math.floor(Jr.fromMillis(r).as("days"));
  }
  function ei(e, t = (e) => e) {
    return {
      regex: e,
      deser: ([e]) =>
        t(
          (function (e) {
            let t = parseInt(e, 10);
            if (isNaN(t)) {
              t = "";
              for (let n = 0; n < e.length; n++) {
                const r = e.charCodeAt(n);
                if (-1 !== e[n].search(Rt.hanidec)) t += Vt.indexOf(e[n]);
                else
                  for (const e in Lt) {
                    const [n, i] = Lt[e];
                    r >= n && r <= i && (t += r - n);
                  }
              }
              return parseInt(t, 10);
            }
            return t;
          })(e),
        ),
    };
  }
  const ti = `[ ${String.fromCharCode(160)}]`,
    ni = new RegExp(ti, "g");
  function ri(e) {
    return e.replace(/\./g, "\\.?").replace(ni, ti);
  }
  function ii(e) {
    return e.replace(/\./g, "").replace(ni, " ").toLowerCase();
  }
  function oi(e, t) {
    return null === e
      ? null
      : {
          regex: RegExp(e.map(ri).join("|")),
          deser: ([n]) => e.findIndex((e) => ii(n) === ii(e)) + t,
        };
  }
  function si(e, t) {
    return { regex: e, deser: ([, e, t]) => In(e, t), groups: t };
  }
  function ai(e) {
    return { regex: e, deser: ([e]) => e };
  }
  const li = {
    year: { "2-digit": "yy", numeric: "yyyyy" },
    month: { numeric: "M", "2-digit": "MM", short: "MMM", long: "MMMM" },
    day: { numeric: "d", "2-digit": "dd" },
    weekday: { short: "EEE", long: "EEEE" },
    dayperiod: "a",
    dayPeriod: "a",
    hour12: { numeric: "h", "2-digit": "hh" },
    hour24: { numeric: "H", "2-digit": "HH" },
    minute: { numeric: "m", "2-digit": "mm" },
    second: { numeric: "s", "2-digit": "ss" },
    timeZoneName: { long: "ZZZZZ", short: "ZZZ" },
  };
  let ui = null;
  function ci(e, t) {
    return Array.prototype.concat(
      ...e.map((e) =>
        (function (e, t) {
          if (e.literal) return e;
          const n = hi(Kn.macroTokenToFormatOpts(e.val), t);
          return null == n || n.includes(void 0) ? e : n;
        })(e, t),
      ),
    );
  }
  class di {
    constructor(e, t) {
      if (
        ((this.locale = e),
        (this.format = t),
        (this.tokens = ci(Kn.parseFormat(t), e)),
        (this.units = this.tokens.map((t) =>
          (function (e, t) {
            const n = Pt(t),
              r = Pt(t, "{2}"),
              i = Pt(t, "{3}"),
              o = Pt(t, "{4}"),
              s = Pt(t, "{6}"),
              a = Pt(t, "{1,2}"),
              l = Pt(t, "{1,3}"),
              u = Pt(t, "{1,6}"),
              c = Pt(t, "{1,9}"),
              d = Pt(t, "{2,4}"),
              m = Pt(t, "{4,6}"),
              h = (e) => {
                return {
                  regex: RegExp(
                    ((t = e.val),
                    t.replace(/[\-\[\]{}()*+?.,\\\^$|#\s]/g, "\\$&")),
                  ),
                  deser: ([e]) => e,
                  literal: !0,
                };
                var t;
              },
              f = ((f) => {
                if (e.literal) return h(f);
                switch (f.val) {
                  case "G":
                    return oi(t.eras("short"), 0);
                  case "GG":
                    return oi(t.eras("long"), 0);
                  case "y":
                    return ei(u);
                  case "yy":
                  case "kk":
                    return ei(d, An);
                  case "yyyy":
                  case "kkkk":
                    return ei(o);
                  case "yyyyy":
                    return ei(m);
                  case "yyyyyy":
                    return ei(s);
                  case "M":
                  case "L":
                  case "d":
                  case "H":
                  case "h":
                  case "m":
                  case "q":
                  case "s":
                  case "W":
                    return ei(a);
                  case "MM":
                  case "LL":
                  case "dd":
                  case "HH":
                  case "hh":
                  case "mm":
                  case "qq":
                  case "ss":
                  case "WW":
                    return ei(r);
                  case "MMM":
                    return oi(t.months("short", !0), 1);
                  case "MMMM":
                    return oi(t.months("long", !0), 1);
                  case "LLL":
                    return oi(t.months("short", !1), 1);
                  case "LLLL":
                    return oi(t.months("long", !1), 1);
                  case "o":
                  case "S":
                    return ei(l);
                  case "ooo":
                  case "SSS":
                    return ei(i);
                  case "u":
                    return ai(c);
                  case "uu":
                    return ai(a);
                  case "uuu":
                  case "E":
                  case "c":
                    return ei(n);
                  case "a":
                    return oi(t.meridiems(), 0);
                  case "EEE":
                    return oi(t.weekdays("short", !1), 1);
                  case "EEEE":
                    return oi(t.weekdays("long", !1), 1);
                  case "ccc":
                    return oi(t.weekdays("short", !0), 1);
                  case "cccc":
                    return oi(t.weekdays("long", !0), 1);
                  case "Z":
                  case "ZZ":
                    return si(
                      new RegExp(`([+-]${a.source})(?::(${r.source}))?`),
                      2,
                    );
                  case "ZZZ":
                    return si(new RegExp(`([+-]${a.source})(${r.source})?`), 2);
                  case "z":
                    return ai(/[a-z_+-/]{1,256}?/i);
                  case " ":
                    return ai(/[^\S\n\r]/);
                  default:
                    return h(f);
                }
              })(e) || {
                invalidReason:
                  "missing Intl.DateTimeFormat.formatToParts support",
              };
            return ((f.token = e), f);
          })(t, e),
        )),
        (this.disqualifyingUnit = this.units.find((e) => e.invalidReason)),
        !this.disqualifyingUnit)
      ) {
        const [e, t] = [
          `^${(n = this.units).map((e) => e.regex).reduce((e, t) => `${e}(${t.source})`, "")}$`,
          n,
        ];
        ((this.regex = RegExp(e, "i")), (this.handlers = t));
      }
      var n;
    }
    explainFromTokens(e) {
      if (this.isValid) {
        const [t, n] = (function (e, t, n) {
            const r = e.match(t);
            if (r) {
              const e = {};
              let t = 1;
              for (const i in n)
                if (pn(n, i)) {
                  const o = n[i],
                    s = o.groups ? o.groups + 1 : 1;
                  (!o.literal &&
                    o.token &&
                    (e[o.token.val[0]] = o.deser(r.slice(t, t + s))),
                    (t += s));
                }
              return [r, e];
            }
            return [r, {}];
          })(e, this.regex, this.handlers),
          [r, i, o] = n
            ? (function (e) {
                let t,
                  n = null;
                return (
                  un(e.z) || (n = ht.create(e.z)),
                  un(e.Z) || (n || (n = new xt(e.Z)), (t = e.Z)),
                  un(e.q) || (e.M = 3 * (e.q - 1) + 1),
                  un(e.h) ||
                    (e.h < 12 && 1 === e.a
                      ? (e.h += 12)
                      : 12 === e.h && 0 === e.a && (e.h = 0)),
                  0 === e.G && e.y && (e.y = -e.y),
                  un(e.u) || (e.S = vn(e.u)),
                  [
                    Object.keys(e).reduce((t, n) => {
                      const r = ((e) => {
                        switch (e) {
                          case "S":
                            return "millisecond";
                          case "s":
                            return "second";
                          case "m":
                            return "minute";
                          case "h":
                          case "H":
                            return "hour";
                          case "d":
                            return "day";
                          case "o":
                            return "ordinal";
                          case "L":
                          case "M":
                            return "month";
                          case "y":
                            return "year";
                          case "E":
                          case "c":
                            return "weekday";
                          case "W":
                            return "weekNumber";
                          case "k":
                            return "weekYear";
                          case "q":
                            return "quarter";
                          default:
                            return null;
                        }
                      })(n);
                      return (r && (t[r] = e[n]), t);
                    }, {}),
                    n,
                    t,
                  ]
                );
              })(n)
            : [null, null, void 0];
        if (pn(n, "a") && pn(n, "H"))
          throw new Ne("Can't include meridiem when specifying 24-hour format");
        return {
          input: e,
          tokens: this.tokens,
          regex: this.regex,
          rawMatches: t,
          matches: n,
          result: r,
          zone: i,
          specificOffset: o,
        };
      }
      return {
        input: e,
        tokens: this.tokens,
        invalidReason: this.invalidReason,
      };
    }
    get isValid() {
      return !this.disqualifyingUnit;
    }
    get invalidReason() {
      return this.disqualifyingUnit
        ? this.disqualifyingUnit.invalidReason
        : null;
    }
  }
  function mi(e, t, n) {
    return new di(e, n).explainFromTokens(t);
  }
  function hi(e, t) {
    if (!e) return null;
    const n = Kn.create(t, e).dtFormatter(
        (ui || (ui = $i.fromMillis(1555555555555)), ui),
      ),
      r = n.formatToParts(),
      i = n.resolvedOptions();
    return r.map((t) =>
      (function (e, t, n) {
        const { type: r, value: i } = e;
        if ("literal" === r) {
          const e = /^\s+$/.test(i);
          return { literal: !e, val: e ? " " : i };
        }
        const o = t[r];
        let s = r;
        "hour" === r &&
          (s =
            null != t.hour12
              ? t.hour12
                ? "hour12"
                : "hour24"
              : null != t.hourCycle
                ? "h11" === t.hourCycle || "h12" === t.hourCycle
                  ? "hour12"
                  : "hour24"
                : n.hour12
                  ? "hour12"
                  : "hour24");
        let a = li[s];
        if (("object" == typeof a && (a = a[o]), a))
          return { literal: !1, val: a };
      })(t, e, i),
    );
  }
  const fi = "Invalid DateTime",
    pi = 864e13;
  function gi(e) {
    return new Gt("unsupported zone", `the zone "${e.name}" is not supported`);
  }
  function yi(e) {
    return (null === e.weekData && (e.weekData = tn(e.c)), e.weekData);
  }
  function bi(e) {
    return (
      null === e.localWeekData &&
        (e.localWeekData = tn(
          e.c,
          e.loc.getMinDaysInFirstWeek(),
          e.loc.getStartOfWeek(),
        )),
      e.localWeekData
    );
  }
  function wi(e, t) {
    const n = {
      ts: e.ts,
      zone: e.zone,
      c: e.c,
      o: e.o,
      loc: e.loc,
      invalid: e.invalid,
    };
    return new $i({ ...n, ...t, old: n });
  }
  function Si(e, t, n) {
    let r = e - 60 * t * 1e3;
    const i = n.offset(r);
    if (t === i) return [r, t];
    r -= 60 * (i - t) * 1e3;
    const o = n.offset(r);
    return i === o ? [r, i] : [e - 60 * Math.min(i, o) * 1e3, Math.max(i, o)];
  }
  function vi(e, t) {
    const n = new Date((e += 60 * t * 1e3));
    return {
      year: n.getUTCFullYear(),
      month: n.getUTCMonth() + 1,
      day: n.getUTCDate(),
      hour: n.getUTCHours(),
      minute: n.getUTCMinutes(),
      second: n.getUTCSeconds(),
      millisecond: n.getUTCMilliseconds(),
    };
  }
  function ki(e, t, n) {
    return Si(Tn(e), t, n);
  }
  function Ei(e, t) {
    const n = e.o,
      r = e.c.year + Math.trunc(t.years),
      i = e.c.month + Math.trunc(t.months) + 3 * Math.trunc(t.quarters),
      o = {
        ...e.c,
        year: r,
        month: i,
        day:
          Math.min(e.c.day, Dn(r, i)) +
          Math.trunc(t.days) +
          7 * Math.trunc(t.weeks),
      },
      s = Jr.fromObject({
        years: t.years - Math.trunc(t.years),
        quarters: t.quarters - Math.trunc(t.quarters),
        months: t.months - Math.trunc(t.months),
        weeks: t.weeks - Math.trunc(t.weeks),
        days: t.days - Math.trunc(t.days),
        hours: t.hours,
        minutes: t.minutes,
        seconds: t.seconds,
        milliseconds: t.milliseconds,
      }).as("milliseconds"),
      a = Tn(o);
    let [l, u] = Si(a, n, e.zone);
    return (0 !== s && ((l += s), (u = e.zone.offset(l))), { ts: l, o: u });
  }
  function Ci(e, t, n, r, i, o) {
    const { setZone: s, zone: a } = n;
    if ((e && 0 !== Object.keys(e).length) || t) {
      const r = t || a,
        i = $i.fromObject(e, { ...n, zone: r, specificOffset: o });
      return s ? i : i.setZone(a);
    }
    return $i.invalid(
      new Gt("unparsable", `the input "${i}" can't be parsed as ${r}`),
    );
  }
  function Di(e, t, n = !0) {
    return e.isValid
      ? Kn.create(Mt.create("en-US"), {
          allowZ: n,
          forceSimple: !0,
        }).formatDateTimeFromString(e, t)
      : null;
  }
  function Ti(e, t, n) {
    const r = e.c.year > 9999 || e.c.year < 0;
    let i = "";
    if (
      (r && e.c.year >= 0 && (i += "+"),
      (i += bn(e.c.year, r ? 6 : 4)),
      "year" === n)
    )
      return i;
    if (t) {
      if (((i += "-"), (i += bn(e.c.month)), "month" === n)) return i;
      i += "-";
    } else if (((i += bn(e.c.month)), "month" === n)) return i;
    return ((i += bn(e.c.day)), i);
  }
  function Oi(e, t, n, r, i, o, s) {
    let a = !n || 0 !== e.c.millisecond || 0 !== e.c.second,
      l = "";
    switch (s) {
      case "day":
      case "month":
      case "year":
        break;
      default:
        if (((l += bn(e.c.hour)), "hour" === s)) break;
        if (t) {
          if (((l += ":"), (l += bn(e.c.minute)), "minute" === s)) break;
          a && ((l += ":"), (l += bn(e.c.second)));
        } else {
          if (((l += bn(e.c.minute)), "minute" === s)) break;
          a && (l += bn(e.c.second));
        }
        if ("second" === s) break;
        !a ||
          (r && 0 === e.c.millisecond) ||
          ((l += "."), (l += bn(e.c.millisecond, 3)));
    }
    return (
      i &&
        (e.isOffsetFixed && 0 === e.offset && !o
          ? (l += "Z")
          : e.o < 0
            ? ((l += "-"),
              (l += bn(Math.trunc(-e.o / 60))),
              (l += ":"),
              (l += bn(Math.trunc(-e.o % 60))))
            : ((l += "+"),
              (l += bn(Math.trunc(e.o / 60))),
              (l += ":"),
              (l += bn(Math.trunc(e.o % 60))))),
      o && (l += "[" + e.zone.ianaName + "]"),
      l
    );
  }
  const Mi = {
      month: 1,
      day: 1,
      hour: 0,
      minute: 0,
      second: 0,
      millisecond: 0,
    },
    Ai = {
      weekNumber: 1,
      weekday: 1,
      hour: 0,
      minute: 0,
      second: 0,
      millisecond: 0,
    },
    xi = { ordinal: 1, hour: 0, minute: 0, second: 0, millisecond: 0 },
    Ii = ["year", "month", "day", "hour", "minute", "second", "millisecond"],
    Ni = [
      "weekYear",
      "weekNumber",
      "weekday",
      "hour",
      "minute",
      "second",
      "millisecond",
    ],
    Ri = ["year", "ordinal", "hour", "minute", "second", "millisecond"];
  function Li(e) {
    const t = {
      year: "year",
      years: "year",
      month: "month",
      months: "month",
      day: "day",
      days: "day",
      hour: "hour",
      hours: "hour",
      minute: "minute",
      minutes: "minute",
      quarter: "quarter",
      quarters: "quarter",
      second: "second",
      seconds: "second",
      millisecond: "millisecond",
      milliseconds: "millisecond",
      weekday: "weekday",
      weekdays: "weekday",
      weeknumber: "weekNumber",
      weeksnumber: "weekNumber",
      weeknumbers: "weekNumber",
      weekyear: "weekYear",
      weekyears: "weekYear",
      ordinal: "ordinal",
    }[e.toLowerCase()];
    if (!t) throw new Re(e);
    return t;
  }
  function Vi(e) {
    switch (e.toLowerCase()) {
      case "localweekday":
      case "localweekdays":
        return "localWeekday";
      case "localweeknumber":
      case "localweeknumbers":
        return "localWeekNumber";
      case "localweekyear":
      case "localweekyears":
        return "localWeekYear";
      default:
        return Li(e);
    }
  }
  function Fi(e, t) {
    const n = Nt(t.zone, Bt.defaultZone);
    if (!n.isValid) return $i.invalid(gi(n));
    const r = Mt.fromObject(t);
    let i, o;
    if (un(e.year)) i = Bt.now();
    else {
      for (const t of Ii) un(e[t]) && (e[t] = Mi[t]);
      const t = an(e) || ln(e);
      if (t) return $i.invalid(t);
      const r = (function (e) {
        if ((void 0 === ji && (ji = Bt.now()), "iana" !== e.type))
          return e.offset(ji);
        const t = e.name;
        let n = zi.get(t);
        return (void 0 === n && ((n = e.offset(ji)), zi.set(t, n)), n);
      })(n);
      [i, o] = ki(e, r, n);
    }
    return new $i({ ts: i, zone: n, loc: r, o });
  }
  function Pi(e, t, n) {
    const r = !!un(n.round) || n.round,
      i = un(n.rounding) ? "trunc" : n.rounding,
      o = (e, o) => (
        (e = kn(e, r || n.calendary ? 0 : 2, n.calendary ? "round" : i)),
        t.loc.clone(n).relFormatter(n).format(e, o)
      ),
      s = (r) =>
        n.calendary
          ? t.hasSame(e, r)
            ? 0
            : t.startOf(r).diff(e.startOf(r), r).get(r)
          : t.diff(e, r).get(r);
    if (n.unit) return o(s(n.unit), n.unit);
    for (const e of n.units) {
      const t = s(e);
      if (Math.abs(t) >= 1) return o(t, e);
    }
    return o(e > t ? -0 : 0, n.units[n.units.length - 1]);
  }
  function Wi(e) {
    let t,
      n = {};
    return (
      e.length > 0 && "object" == typeof e[e.length - 1]
        ? ((n = e[e.length - 1]), (t = Array.from(e).slice(0, e.length - 1)))
        : (t = Array.from(e)),
      [n, t]
    );
  }
  let ji;
  const zi = new Map();
  class $i {
    constructor(e) {
      const t = e.zone || Bt.defaultZone;
      let n =
        e.invalid ||
        (Number.isNaN(e.ts) ? new Gt("invalid input") : null) ||
        (t.isValid ? null : gi(t));
      this.ts = un(e.ts) ? Bt.now() : e.ts;
      let r = null,
        i = null;
      if (!n)
        if (e.old && e.old.ts === this.ts && e.old.zone.equals(t))
          [r, i] = [e.old.c, e.old.o];
        else {
          const o = cn(e.o) && !e.old ? e.o : t.offset(this.ts);
          ((r = vi(this.ts, o)),
            (n = Number.isNaN(r.year) ? new Gt("invalid input") : null),
            (r = n ? null : r),
            (i = n ? null : o));
        }
      ((this._zone = t),
        (this.loc = e.loc || Mt.create()),
        (this.invalid = n),
        (this.weekData = null),
        (this.localWeekData = null),
        (this.c = r),
        (this.o = i),
        (this.isLuxonDateTime = !0));
    }
    static now() {
      return new $i({});
    }
    static local() {
      const [e, t] = Wi(arguments),
        [n, r, i, o, s, a, l] = t;
      return Fi(
        {
          year: n,
          month: r,
          day: i,
          hour: o,
          minute: s,
          second: a,
          millisecond: l,
        },
        e,
      );
    }
    static utc() {
      const [e, t] = Wi(arguments),
        [n, r, i, o, s, a, l] = t;
      return (
        (e.zone = xt.utcInstance),
        Fi(
          {
            year: n,
            month: r,
            day: i,
            hour: o,
            minute: s,
            second: a,
            millisecond: l,
          },
          e,
        )
      );
    }
    static fromJSDate(e, t = {}) {
      const n =
        ((r = e),
        "[object Date]" === Object.prototype.toString.call(r)
          ? e.valueOf()
          : NaN);
      var r;
      if (Number.isNaN(n)) return $i.invalid("invalid input");
      const i = Nt(t.zone, Bt.defaultZone);
      return i.isValid
        ? new $i({ ts: n, zone: i, loc: Mt.fromObject(t) })
        : $i.invalid(gi(i));
    }
    static fromMillis(e, t = {}) {
      if (cn(e))
        return e < -pi || e > pi
          ? $i.invalid("Timestamp out of range")
          : new $i({
              ts: e,
              zone: Nt(t.zone, Bt.defaultZone),
              loc: Mt.fromObject(t),
            });
      throw new Le(
        `fromMillis requires a numerical input, but received a ${typeof e} with value ${e}`,
      );
    }
    static fromSeconds(e, t = {}) {
      if (cn(e))
        return new $i({
          ts: 1e3 * e,
          zone: Nt(t.zone, Bt.defaultZone),
          loc: Mt.fromObject(t),
        });
      throw new Le("fromSeconds requires a numerical input");
    }
    static fromObject(e, t = {}) {
      e = e || {};
      const n = Nt(t.zone, Bt.defaultZone);
      if (!n.isValid) return $i.invalid(gi(n));
      const r = Mt.fromObject(t),
        i = Rn(e, Vi),
        { minDaysInFirstWeek: o, startOfWeek: s } = sn(i, r),
        a = Bt.now(),
        l = un(t.specificOffset) ? n.offset(a) : t.specificOffset,
        u = !un(i.ordinal),
        c = !un(i.year),
        d = !un(i.month) || !un(i.day),
        m = c || d,
        h = i.weekYear || i.weekNumber;
      if ((m || u) && h)
        throw new Ne(
          "Can't mix weekYear/weekNumber units with year/month/day or ordinals",
        );
      if (d && u) throw new Ne("Can't mix ordinal dates with month/day");
      const f = h || (i.weekday && !m);
      let p,
        g,
        y = vi(a, l);
      f
        ? ((p = Ni), (g = Ai), (y = tn(y, o, s)))
        : u
          ? ((p = Ri), (g = xi), (y = rn(y)))
          : ((p = Ii), (g = Mi));
      let b = !1;
      for (const e of p) un(i[e]) ? (i[e] = b ? g[e] : y[e]) : (b = !0);
      const w = f
          ? (function (e, t = 4, n = 1) {
              const r = dn(e.weekYear),
                i = yn(e.weekNumber, 1, Mn(e.weekYear, t, n)),
                o = yn(e.weekday, 1, 7);
              return r
                ? i
                  ? !o && Yt("weekday", e.weekday)
                  : Yt("week", e.weekNumber)
                : Yt("weekYear", e.weekYear);
            })(i, o, s)
          : u
            ? (function (e) {
                const t = dn(e.year),
                  n = yn(e.ordinal, 1, Cn(e.year));
                return t ? !n && Yt("ordinal", e.ordinal) : Yt("year", e.year);
              })(i)
            : an(i),
        S = w || ln(i);
      if (S) return $i.invalid(S);
      const v = f ? nn(i, o, s) : u ? on(i) : i,
        [k, E] = ki(v, l, n),
        C = new $i({ ts: k, zone: n, o: E, loc: r });
      return i.weekday && m && e.weekday !== C.weekday
        ? $i.invalid(
            "mismatched weekday",
            `you can't specify both a weekday of ${i.weekday} and a date of ${C.toISO()}`,
          )
        : C.isValid
          ? C
          : $i.invalid(C.invalid);
    }
    static fromISO(e, t = {}) {
      const [n, r] = (function (e) {
        return tr(e, [Or, Ir], [Mr, Nr], [Ar, Rr], [xr, Lr]);
      })(e);
      return Ci(n, r, t, "ISO 8601", e);
    }
    static fromRFC2822(e, t = {}) {
      const [n, r] = (function (e) {
        return tr(
          (function (e) {
            return e
              .replace(/\([^()]*\)|[\n\t]/g, " ")
              .replace(/(\s\s+)/g, " ")
              .trim();
          })(e),
          [Sr, vr],
        );
      })(e);
      return Ci(n, r, t, "RFC 2822", e);
    }
    static fromHTTP(e, t = {}) {
      const [n, r] = (function (e) {
        return tr(e, [kr, Dr], [Er, Dr], [Cr, Tr]);
      })(e);
      return Ci(n, r, t, "HTTP", t);
    }
    static fromFormat(e, t, n = {}) {
      if (un(e) || un(t))
        throw new Le("fromFormat requires an input string and a format");
      const { locale: r = null, numberingSystem: i = null } = n,
        o = Mt.fromOpts({ locale: r, numberingSystem: i, defaultToEN: !0 }),
        [s, a, l, u] = (function (e, t, n) {
          const {
            result: r,
            zone: i,
            specificOffset: o,
            invalidReason: s,
          } = mi(e, t, n);
          return [r, i, o, s];
        })(o, e, t);
      return u ? $i.invalid(u) : Ci(s, a, n, `format ${t}`, e, l);
    }
    static fromString(e, t, n = {}) {
      return $i.fromFormat(e, t, n);
    }
    static fromSQL(e, t = {}) {
      const [n, r] = (function (e) {
        return tr(e, [Fr, Ir], [Pr, Wr]);
      })(e);
      return Ci(n, r, t, "SQL", e);
    }
    static invalid(e, t = null) {
      if (!e) throw new Le("need to specify a reason the DateTime is invalid");
      const n = e instanceof Gt ? e : new Gt(e, t);
      if (Bt.throwOnInvalid) throw new Ae(n);
      return new $i({ invalid: n });
    }
    static isDateTime(e) {
      return (e && e.isLuxonDateTime) || !1;
    }
    static parseFormatForOpts(e, t = {}) {
      const n = hi(e, Mt.fromObject(t));
      return n ? n.map((e) => (e ? e.val : null)).join("") : null;
    }
    static expandFormat(e, t = {}) {
      return ci(Kn.parseFormat(e), Mt.fromObject(t))
        .map((e) => e.val)
        .join("");
    }
    static resetCache() {
      ((ji = void 0), zi.clear());
    }
    get(e) {
      return this[e];
    }
    get isValid() {
      return null === this.invalid;
    }
    get invalidReason() {
      return this.invalid ? this.invalid.reason : null;
    }
    get invalidExplanation() {
      return this.invalid ? this.invalid.explanation : null;
    }
    get locale() {
      return this.isValid ? this.loc.locale : null;
    }
    get numberingSystem() {
      return this.isValid ? this.loc.numberingSystem : null;
    }
    get outputCalendar() {
      return this.isValid ? this.loc.outputCalendar : null;
    }
    get zone() {
      return this._zone;
    }
    get zoneName() {
      return this.isValid ? this.zone.name : null;
    }
    get year() {
      return this.isValid ? this.c.year : NaN;
    }
    get quarter() {
      return this.isValid ? Math.ceil(this.c.month / 3) : NaN;
    }
    get month() {
      return this.isValid ? this.c.month : NaN;
    }
    get day() {
      return this.isValid ? this.c.day : NaN;
    }
    get hour() {
      return this.isValid ? this.c.hour : NaN;
    }
    get minute() {
      return this.isValid ? this.c.minute : NaN;
    }
    get second() {
      return this.isValid ? this.c.second : NaN;
    }
    get millisecond() {
      return this.isValid ? this.c.millisecond : NaN;
    }
    get weekYear() {
      return this.isValid ? yi(this).weekYear : NaN;
    }
    get weekNumber() {
      return this.isValid ? yi(this).weekNumber : NaN;
    }
    get weekday() {
      return this.isValid ? yi(this).weekday : NaN;
    }
    get isWeekend() {
      return this.isValid && this.loc.getWeekendDays().includes(this.weekday);
    }
    get localWeekday() {
      return this.isValid ? bi(this).weekday : NaN;
    }
    get localWeekNumber() {
      return this.isValid ? bi(this).weekNumber : NaN;
    }
    get localWeekYear() {
      return this.isValid ? bi(this).weekYear : NaN;
    }
    get ordinal() {
      return this.isValid ? rn(this.c).ordinal : NaN;
    }
    get monthShort() {
      return this.isValid
        ? Xr.months("short", { locObj: this.loc })[this.month - 1]
        : null;
    }
    get monthLong() {
      return this.isValid
        ? Xr.months("long", { locObj: this.loc })[this.month - 1]
        : null;
    }
    get weekdayShort() {
      return this.isValid
        ? Xr.weekdays("short", { locObj: this.loc })[this.weekday - 1]
        : null;
    }
    get weekdayLong() {
      return this.isValid
        ? Xr.weekdays("long", { locObj: this.loc })[this.weekday - 1]
        : null;
    }
    get offset() {
      return this.isValid ? +this.o : NaN;
    }
    get offsetNameShort() {
      return this.isValid
        ? this.zone.offsetName(this.ts, {
            format: "short",
            locale: this.locale,
          })
        : null;
    }
    get offsetNameLong() {
      return this.isValid
        ? this.zone.offsetName(this.ts, { format: "long", locale: this.locale })
        : null;
    }
    get isOffsetFixed() {
      return this.isValid ? this.zone.isUniversal : null;
    }
    get isInDST() {
      return (
        !this.isOffsetFixed &&
        (this.offset > this.set({ month: 1, day: 1 }).offset ||
          this.offset > this.set({ month: 5 }).offset)
      );
    }
    getPossibleOffsets() {
      if (!this.isValid || this.isOffsetFixed) return [this];
      const e = 864e5,
        t = 6e4,
        n = Tn(this.c),
        r = this.zone.offset(n - e),
        i = this.zone.offset(n + e),
        o = this.zone.offset(n - r * t),
        s = this.zone.offset(n - i * t);
      if (o === s) return [this];
      const a = n - o * t,
        l = n - s * t,
        u = vi(a, o),
        c = vi(l, s);
      return u.hour === c.hour &&
        u.minute === c.minute &&
        u.second === c.second &&
        u.millisecond === c.millisecond
        ? [wi(this, { ts: a }), wi(this, { ts: l })]
        : [this];
    }
    get isInLeapYear() {
      return En(this.year);
    }
    get daysInMonth() {
      return Dn(this.year, this.month);
    }
    get daysInYear() {
      return this.isValid ? Cn(this.year) : NaN;
    }
    get weeksInWeekYear() {
      return this.isValid ? Mn(this.weekYear) : NaN;
    }
    get weeksInLocalWeekYear() {
      return this.isValid
        ? Mn(
            this.localWeekYear,
            this.loc.getMinDaysInFirstWeek(),
            this.loc.getStartOfWeek(),
          )
        : NaN;
    }
    resolvedLocaleOptions(e = {}) {
      const {
        locale: t,
        numberingSystem: n,
        calendar: r,
      } = Kn.create(this.loc.clone(e), e).resolvedOptions(this);
      return { locale: t, numberingSystem: n, outputCalendar: r };
    }
    toUTC(e = 0, t = {}) {
      return this.setZone(xt.instance(e), t);
    }
    toLocal() {
      return this.setZone(Bt.defaultZone);
    }
    setZone(e, { keepLocalTime: t = !1, keepCalendarTime: n = !1 } = {}) {
      if ((e = Nt(e, Bt.defaultZone)).equals(this.zone)) return this;
      if (e.isValid) {
        let r = this.ts;
        if (t || n) {
          const t = e.offset(this.ts),
            n = this.toObject();
          [r] = ki(n, t, e);
        }
        return wi(this, { ts: r, zone: e });
      }
      return $i.invalid(gi(e));
    }
    reconfigure({ locale: e, numberingSystem: t, outputCalendar: n } = {}) {
      return wi(this, {
        loc: this.loc.clone({
          locale: e,
          numberingSystem: t,
          outputCalendar: n,
        }),
      });
    }
    setLocale(e) {
      return this.reconfigure({ locale: e });
    }
    set(e) {
      if (!this.isValid) return this;
      const t = Rn(e, Vi),
        { minDaysInFirstWeek: n, startOfWeek: r } = sn(t, this.loc),
        i = !un(t.weekYear) || !un(t.weekNumber) || !un(t.weekday),
        o = !un(t.ordinal),
        s = !un(t.year),
        a = !un(t.month) || !un(t.day),
        l = s || a,
        u = t.weekYear || t.weekNumber;
      if ((l || o) && u)
        throw new Ne(
          "Can't mix weekYear/weekNumber units with year/month/day or ordinals",
        );
      if (a && o) throw new Ne("Can't mix ordinal dates with month/day");
      let c;
      i
        ? (c = nn({ ...tn(this.c, n, r), ...t }, n, r))
        : un(t.ordinal)
          ? ((c = { ...this.toObject(), ...t }),
            un(t.day) && (c.day = Math.min(Dn(c.year, c.month), c.day)))
          : (c = on({ ...rn(this.c), ...t }));
      const [d, m] = ki(c, this.o, this.zone);
      return wi(this, { ts: d, o: m });
    }
    plus(e) {
      return this.isValid ? wi(this, Ei(this, Jr.fromDurationLike(e))) : this;
    }
    minus(e) {
      return this.isValid
        ? wi(this, Ei(this, Jr.fromDurationLike(e).negate()))
        : this;
    }
    startOf(e, { useLocaleWeeks: t = !1 } = {}) {
      if (!this.isValid) return this;
      const n = {},
        r = Jr.normalizeUnit(e);
      switch (r) {
        case "years":
          n.month = 1;
        case "quarters":
        case "months":
          n.day = 1;
        case "weeks":
        case "days":
          n.hour = 0;
        case "hours":
          n.minute = 0;
        case "minutes":
          n.second = 0;
        case "seconds":
          n.millisecond = 0;
      }
      if ("weeks" === r)
        if (t) {
          const e = this.loc.getStartOfWeek(),
            { weekday: t } = this;
          (t < e && (n.weekNumber = this.weekNumber - 1), (n.weekday = e));
        } else n.weekday = 1;
      if ("quarters" === r) {
        const e = Math.ceil(this.month / 3);
        n.month = 3 * (e - 1) + 1;
      }
      return this.set(n);
    }
    endOf(e, t) {
      return this.isValid
        ? this.plus({ [e]: 1 })
            .startOf(e, t)
            .minus(1)
        : this;
    }
    toFormat(e, t = {}) {
      return this.isValid
        ? Kn.create(this.loc.redefaultToEN(t)).formatDateTimeFromString(this, e)
        : fi;
    }
    toLocaleString(e = je, t = {}) {
      return this.isValid
        ? Kn.create(this.loc.clone(t), e).formatDateTime(this)
        : fi;
    }
    toLocaleParts(e = {}) {
      return this.isValid
        ? Kn.create(this.loc.clone(e), e).formatDateTimeParts(this)
        : [];
    }
    toISO({
      format: e = "extended",
      suppressSeconds: t = !1,
      suppressMilliseconds: n = !1,
      includeOffset: r = !0,
      extendedZone: i = !1,
      precision: o = "milliseconds",
    } = {}) {
      if (!this.isValid) return null;
      const s = "extended" === e;
      let a = Ti(this, s, (o = Li(o)));
      return (
        Ii.indexOf(o) >= 3 && (a += "T"),
        (a += Oi(this, s, t, n, r, i, o)),
        a
      );
    }
    toISODate({ format: e = "extended", precision: t = "day" } = {}) {
      return this.isValid ? Ti(this, "extended" === e, Li(t)) : null;
    }
    toISOWeekDate() {
      return Di(this, "kkkk-'W'WW-c");
    }
    toISOTime({
      suppressMilliseconds: e = !1,
      suppressSeconds: t = !1,
      includeOffset: n = !0,
      includePrefix: r = !1,
      extendedZone: i = !1,
      format: o = "extended",
      precision: s = "milliseconds",
    } = {}) {
      return this.isValid
        ? ((s = Li(s)),
          (r && Ii.indexOf(s) >= 3 ? "T" : "") +
            Oi(this, "extended" === o, t, e, n, i, s))
        : null;
    }
    toRFC2822() {
      return Di(this, "EEE, dd LLL yyyy HH:mm:ss ZZZ", !1);
    }
    toHTTP() {
      return Di(this.toUTC(), "EEE, dd LLL yyyy HH:mm:ss 'GMT'");
    }
    toSQLDate() {
      return this.isValid ? Ti(this, !0) : null;
    }
    toSQLTime({
      includeOffset: e = !0,
      includeZone: t = !1,
      includeOffsetSpace: n = !0,
    } = {}) {
      let r = "HH:mm:ss.SSS";
      return (
        (t || e) && (n && (r += " "), t ? (r += "z") : e && (r += "ZZ")),
        Di(this, r, !0)
      );
    }
    toSQL(e = {}) {
      return this.isValid ? `${this.toSQLDate()} ${this.toSQLTime(e)}` : null;
    }
    toString() {
      return this.isValid ? this.toISO() : fi;
    }
    [Symbol.for("nodejs.util.inspect.custom")]() {
      return this.isValid
        ? `DateTime { ts: ${this.toISO()}, zone: ${this.zone.name}, locale: ${this.locale} }`
        : `DateTime { Invalid, reason: ${this.invalidReason} }`;
    }
    valueOf() {
      return this.toMillis();
    }
    toMillis() {
      return this.isValid ? this.ts : NaN;
    }
    toSeconds() {
      return this.isValid ? this.ts / 1e3 : NaN;
    }
    toUnixInteger() {
      return this.isValid ? Math.floor(this.ts / 1e3) : NaN;
    }
    toJSON() {
      return this.toISO();
    }
    toBSON() {
      return this.toJSDate();
    }
    toObject(e = {}) {
      if (!this.isValid) return {};
      const t = { ...this.c };
      return (
        e.includeConfig &&
          ((t.outputCalendar = this.outputCalendar),
          (t.numberingSystem = this.loc.numberingSystem),
          (t.locale = this.loc.locale)),
        t
      );
    }
    toJSDate() {
      return new Date(this.isValid ? this.ts : NaN);
    }
    diff(e, t = "milliseconds", n = {}) {
      if (!this.isValid || !e.isValid)
        return Jr.invalid("created by diffing an invalid DateTime");
      const r = {
          locale: this.locale,
          numberingSystem: this.numberingSystem,
          ...n,
        },
        i = ((a = t), Array.isArray(a) ? a : [a]).map(Jr.normalizeUnit),
        o = e.valueOf() > this.valueOf(),
        s = (function (e, t, n, r) {
          let [i, o, s, a] = (function (e, t, n) {
            const r = [
                ["years", (e, t) => t.year - e.year],
                [
                  "quarters",
                  (e, t) => t.quarter - e.quarter + 4 * (t.year - e.year),
                ],
                [
                  "months",
                  (e, t) => t.month - e.month + 12 * (t.year - e.year),
                ],
                [
                  "weeks",
                  (e, t) => {
                    const n = Qr(e, t);
                    return (n - (n % 7)) / 7;
                  },
                ],
                ["days", Qr],
              ],
              i = {},
              o = e;
            let s, a;
            for (const [l, u] of r)
              n.indexOf(l) >= 0 &&
                ((s = l),
                (i[l] = u(e, t)),
                (a = o.plus(i)),
                a > t
                  ? (i[l]--,
                    (e = o.plus(i)) > t && ((a = e), i[l]--, (e = o.plus(i))))
                  : (e = a));
            return [e, i, a, s];
          })(e, t, n);
          const l = t - i,
            u = n.filter(
              (e) =>
                ["hours", "minutes", "seconds", "milliseconds"].indexOf(e) >= 0,
            );
          0 === u.length &&
            (s < t && (s = i.plus({ [a]: 1 })),
            s !== i && (o[a] = (o[a] || 0) + l / (s - i)));
          const c = Jr.fromObject(o, r);
          return u.length > 0
            ? Jr.fromMillis(l, r)
                .shiftTo(...u)
                .plus(c)
            : c;
        })(o ? this : e, o ? e : this, i, r);
      var a;
      return o ? s.negate() : s;
    }
    diffNow(e = "milliseconds", t = {}) {
      return this.diff($i.now(), e, t);
    }
    until(e) {
      return this.isValid ? Kr.fromDateTimes(this, e) : this;
    }
    hasSame(e, t, n) {
      if (!this.isValid) return !1;
      const r = e.valueOf(),
        i = this.setZone(e.zone, { keepLocalTime: !0 });
      return i.startOf(t, n) <= r && r <= i.endOf(t, n);
    }
    equals(e) {
      return (
        this.isValid &&
        e.isValid &&
        this.valueOf() === e.valueOf() &&
        this.zone.equals(e.zone) &&
        this.loc.equals(e.loc)
      );
    }
    toRelative(e = {}) {
      if (!this.isValid) return null;
      const t = e.base || $i.fromObject({}, { zone: this.zone }),
        n = e.padding ? (this < t ? -e.padding : e.padding) : 0;
      let r = ["years", "months", "days", "hours", "minutes", "seconds"],
        i = e.unit;
      return (
        Array.isArray(e.unit) && ((r = e.unit), (i = void 0)),
        Pi(t, this.plus(n), { ...e, numeric: "always", units: r, unit: i })
      );
    }
    toRelativeCalendar(e = {}) {
      return this.isValid
        ? Pi(e.base || $i.fromObject({}, { zone: this.zone }), this, {
            ...e,
            numeric: "auto",
            units: ["years", "months", "days"],
            calendary: !0,
          })
        : null;
    }
    static min(...e) {
      if (!e.every($i.isDateTime))
        throw new Le("min requires all arguments be DateTimes");
      return fn(e, (e) => e.valueOf(), Math.min);
    }
    static max(...e) {
      if (!e.every($i.isDateTime))
        throw new Le("max requires all arguments be DateTimes");
      return fn(e, (e) => e.valueOf(), Math.max);
    }
    static fromFormatExplain(e, t, n = {}) {
      const { locale: r = null, numberingSystem: i = null } = n;
      return mi(
        Mt.fromOpts({ locale: r, numberingSystem: i, defaultToEN: !0 }),
        e,
        t,
      );
    }
    static fromStringExplain(e, t, n = {}) {
      return $i.fromFormatExplain(e, t, n);
    }
    static buildFormatParser(e, t = {}) {
      const { locale: n = null, numberingSystem: r = null } = t,
        i = Mt.fromOpts({ locale: n, numberingSystem: r, defaultToEN: !0 });
      return new di(i, e);
    }
    static fromFormatParser(e, t, n = {}) {
      if (un(e) || un(t))
        throw new Le(
          "fromFormatParser requires an input string and a format parser",
        );
      const { locale: r = null, numberingSystem: i = null } = n,
        o = Mt.fromOpts({ locale: r, numberingSystem: i, defaultToEN: !0 });
      if (!o.equals(t.locale))
        throw new Le(
          `fromFormatParser called with a locale of ${o}, but the format parser was created for ${t.locale}`,
        );
      const {
        result: s,
        zone: a,
        specificOffset: l,
        invalidReason: u,
      } = t.explainFromTokens(e);
      return u ? $i.invalid(u) : Ci(s, a, n, `format ${t.format}`, e, l);
    }
    static get DATE_SHORT() {
      return je;
    }
    static get DATE_MED() {
      return ze;
    }
    static get DATE_MED_WITH_WEEKDAY() {
      return $e;
    }
    static get DATE_FULL() {
      return _e;
    }
    static get DATE_HUGE() {
      return Ue;
    }
    static get TIME_SIMPLE() {
      return Ze;
    }
    static get TIME_WITH_SECONDS() {
      return He;
    }
    static get TIME_WITH_SHORT_OFFSET() {
      return Be;
    }
    static get TIME_WITH_LONG_OFFSET() {
      return Ge;
    }
    static get TIME_24_SIMPLE() {
      return qe;
    }
    static get TIME_24_WITH_SECONDS() {
      return Je;
    }
    static get TIME_24_WITH_SHORT_OFFSET() {
      return Ye;
    }
    static get TIME_24_WITH_LONG_OFFSET() {
      return Ke;
    }
    static get DATETIME_SHORT() {
      return Xe;
    }
    static get DATETIME_SHORT_WITH_SECONDS() {
      return Qe;
    }
    static get DATETIME_MED() {
      return et;
    }
    static get DATETIME_MED_WITH_SECONDS() {
      return tt;
    }
    static get DATETIME_MED_WITH_WEEKDAY() {
      return nt;
    }
    static get DATETIME_FULL() {
      return rt;
    }
    static get DATETIME_FULL_WITH_SECONDS() {
      return it;
    }
    static get DATETIME_HUGE() {
      return ot;
    }
    static get DATETIME_HUGE_WITH_SECONDS() {
      return st;
    }
  }
  function _i(e) {
    if ($i.isDateTime(e)) return e;
    if (e && e.valueOf && cn(e.valueOf())) return $i.fromJSDate(e);
    if (e && "object" == typeof e) return $i.fromObject(e);
    throw new Le(`Unknown datetime argument: ${e}, of type ${typeof e}`);
  }
  function Ui(e, t) {
    let n = t.timezone;
    if (!n && !t.language) return !1;
    const r = e.Intl.DateTimeFormat;
    r &&
      O(r.prototype, "resolvedOptions", (e, i, o) => {
        let s = A(e, i, o);
        if ((n && (s.timeZone = n), t.language)) {
          let e = t.language.split(",")[0],
            n = r.supportedLocalesOf(e);
          s.locale = n[0] || "";
        }
        return s;
      });
    const i = e.Date;
    if (i) {
      let t = new i(),
        r =
          new i(t.toLocaleString("en", { timeZone: n })).getTime() -
          t.getTime();
      O(
        i.prototype,
        "toString",
        (e, t, r) => {
          try {
            return $i
              .fromJSDate(t)
              .setZone(n)
              .toFormat("ccc LLL dd yyyy TT 'GMT'ZZZ (ZZZZZ)");
          } catch (n) {
            return A(e, t, r);
          }
        },
        { skipAbsent: !1 },
      );
      for (let e of ["toUTCString", "toGMTString", "toISOString"])
        O(i.prototype, e, (e, t, n) => A(e, (t = new i(t.getTime() + r)), n), {
          skipAbsent: !1,
        });
      x(
        e,
        "Date",
        (e, t, n) => {
          let i = Reflect.construct(e, t, n);
          return 1 !== t?.length || "number" != typeof t[0]
            ? i
            : new e(i.getTime() - r);
        },
        { skipAbsent: !1 },
      );
    }
    return !0;
  }
  function Zi(e, t) {
    if (!e?.startsWith("candidate:")) return e;
    let n = e.split(" "),
      r = n[4];
    return "0.0.0.0" === r || "127.0.0.1" === r ? e : ((n[4] = t), n.join(" "));
  }
  function Hi(e, t) {
    return e
      ? e
          .split("\r\n")
          .map((e) => Zi(e, t))
          .join("\r\n")
      : e;
  }
  function Bi(e, t) {
    if (!t.webrtc) return !1;
    const n = e.RTCSessionDescription,
      r = e.RTCIceCandidate;
    (n &&
      D(n.prototype, "sdp", function (e, n, r) {
        let i = A(e, n, r);
        return i
          ? "disable" === t.webrtc
            ? Hi(i, "0.0.0.0")
            : Hi(i, t.webrtc)
          : i;
      }),
      r &&
        (O(r.prototype, "toJSON", function (e, t, n) {
          let r = A(e, t, n);
          return ((r.candidate = t.candidate), r);
        }),
        D(r.prototype, "address", function (e, n, r) {
          let i = A(e, n, r);
          return "disable" === t.webrtc ? "0.0.0.0" : t.webrtc ? t.webrtc : i;
        }),
        D(r.prototype, "candidate", function (e, n, r) {
          let i = A(e, n, r);
          return i
            ? "disable" === t.webrtc
              ? Zi(i, "0.0.0.0")
              : Zi(i, t.webrtc)
            : i;
        })));
    const i = t.iceServers;
    return (
      i &&
        [
          "RTCPeerConnection",
          "mozRTCPeerConnection",
          "webkitRTCPeerConnection",
        ].forEach((t) => {
          x(
            e,
            t,
            (e, t, n) => {
              let r = t[0];
              return (r && (r.iceServers = i), Reflect.construct(e, t, n));
            },
            { skipAbsent: !0 },
          );
        }),
      !0
    );
  }
  const Gi = {
    37445: "UNMASKED_VENDOR_WEBGL",
    37446: "UNMASKED_RENDERER_WEBGL",
  };
  function qi(e) {
    Object.keys(e).forEach((t) => {
      try {
        const n = e[t];
        "number" == typeof n && (Gi[n] = t);
      } catch (e) {}
    });
  }
  function Ji(e, t, n, r) {
    let i = t.factors.webgl;
    const o = t.webglInfo || {},
      s = Gi[n];
    if (s) {
      const e = o[s];
      if (void 0 !== e) return e;
    }
    if (i && "number" == typeof r) {
      let e = new Number(r);
      return (
        (e.toString = function () {
          return Number.prototype.toString.call(e + i, ...arguments);
        }),
        (e.toString[l.native] = 1),
        e
      );
    }
    return r;
  }
  (qi(WebGLRenderingContext.prototype), qi(WebGL2RenderingContext.prototype));
  const Yi = {
    "Array.reduce": (e, t, n, r) => 0,
    "Array.map": (e, t, n, r) => {
      let i = A(e, t, n);
      return (i[0] && (i[0] = be(i[0], r)), i);
    },
    "Array.sort": (e, t, n, r) => "",
  };
  function Ki(e, t) {
    const n = t.factors.webglParams,
      r = (t.safeMode, e.WebGLRenderingContext);
    r &&
      O(r.prototype, "getParameter", (e, r, i) => {
        let o = A(e, r, i);
        return (
          (o = Ji(0, t, i[0], o) || o),
          ve(t, o) && o instanceof Number && n
            ? be(o, { source: "WebGL.param", path: [], handlers: Yi })
            : o
        );
      });
    const i = e.WebGL2RenderingContext;
    return (
      i &&
        O(i.prototype, "getParameter", (e, r, i) => {
          let o = A(e, r, i);
          return (
            (o = Ji(0, t, i[0], o) || o),
            ve(t, o) && o instanceof Number && n
              ? be(o, { source: "WebGL2.param", path: [], handlers: Yi })
              : o
          );
        }),
      !0
    );
  }
  const Xi = /gl_FragColor\s*=\s*(.+);/;
  function Qi(e, t) {
    const n = t.factors.webgl;
    t.safeMode;
    let r = new a.Random(n);
    r.int(-1e4, 1e4);
    let i =
      "vec4(" +
      r.int(-50, 50) / 20 +
      "," +
      r.int(-50, 50) / 20 +
      "," +
      r.int(-50, 50) / 20 +
      ",0)";
    const o = (e) =>
        e && "string" == typeof e
          ? (function (e) {
              const t = e.match(Xi);
              if (t) {
                if (
                  t[1]
                    .split(/[,+\-*/()]/)
                    .map((e) => e.trim())
                    .filter((e) => !isNaN(parseFloat(e))).length >= 3
                )
                  return e;
                {
                  const n = `gl_FragColor = ${i} + ${t[1]};`;
                  return e.replace(Xi, n);
                }
              }
              return e;
            })(e)
          : e,
      s = e.WebGLRenderingContext;
    s &&
      n &&
      O(
        s.prototype,
        "shaderSource",
        (e, t, n) => ((n = [...n])[1] && (n[1] = o(n[1])), A(e, t, n)),
      );
    const l = e.WebGL2RenderingContext;
    return (
      l &&
        n &&
        O(
          l.prototype,
          "shaderSource",
          (e, t, n) => ((n = [...n])[1] && (n[1] = o(n[1])), A(e, t, n)),
        ),
      !0
    );
  }
  function eo(e, t) {
    const n = t.factors.webgpu,
      r = (t.webglInfo || {}).UNMASKED_VENDOR_WEBGL;
    if (!r && !n) return !1;
    const i = e.GPUAdapterInfo,
      o = e.GPUSupportedLimits;
    let s = new a.Random(n);
    if (
      (i &&
        r &&
        D(i.prototype, "vendor", (e, t, n) => {
          let i = A(e, t, n);
          if (r) {
            let e = (/\(.+\)/.exec(r) || [])[0];
            return e ? e.substring(1, e.length - 1).toLowerCase() : "";
          }
          return i;
        }),
      o && n)
    ) {
      let e = Object.getOwnPropertyDescriptors(o.prototype);
      Object.keys(e).forEach((t) => {
        "function" == typeof e[t].get &&
          D(
            o.prototype,
            t,
            (e, t, n) => {
              let r = A(e, t, n);
              if (r <= 64) return r;
              let i = 2 * s.int(0, 10);
              return r % 2 == 0 ? r - ((i / 2) | 0) : r - i - 1;
            },
            { skipAbsent: !0 },
          );
      });
    }
    return !0;
  }
  function to(e, t) {
    const n = e.Screen;
    if (!n) return !1;
    if (!t.screen) return !1;
    if (
      !(
        t.screen.noise ||
        t.screen.width ||
        t.screen.height ||
        t.screen.dpr ||
        t.screen.scheme ||
        t.screen.colorDepth ||
        t.screen.pixelDepth
      )
    )
      return !1;
    (t.screen?.dpr &&
      e.window.devicePixelRatio / 1 != t.screen?.dpr / 1 &&
      (I(e.window, "devicePixelRatio", t.screen.dpr),
      U("resolution", t.screen.dpr + "dppx")),
      t.screen?.scheme && U("prefers-color-scheme", t.screen?.scheme),
      t.screen?.pixelDepth &&
        D(n.prototype, "pixelDepth", (e, n, r) => {
          let i = A(e, n, r);
          return t.screen.pixelDepth || i;
        }),
      t.screen?.colorDepth &&
        D(n.prototype, "colorDepth", (e, n, r) => {
          let i = A(e, n, r);
          return t.screen.colorDepth || i;
        }));
    const r = t.screen.noise;
    let { width: i, height: o } = e.screen,
      s = i,
      l = o,
      u = new a.Random(r);
    if (r) {
      const e = u.int(-50, 50) / 1e3;
      ((s = (i * (1 + e)) | 0), (l = (o * (1 + e)) | 0));
    } else {
      let { width: e, height: n } = t.screen;
      ((s = e), (l = n));
    }
    let c = s / i,
      d = l / o;
    function m(e, t, n) {
      let r = A(e, t, n);
      return (c * r) | 0;
    }
    function h(e, t, n) {
      let r = A(e, t, n);
      return (d * r) | 0;
    }
    (D(n.prototype, "width", (e, t, n) => (A(e, t, n) * c) | 0),
      D(n.prototype, "height", (e, t, n) => (A(e, t, n) * d) | 0),
      U("device-width", s + "px"),
      U("device-height", l + "px"),
      D(n.prototype, "availWidth", m),
      D(n.prototype, "availHeight", h),
      D(e, "outerWidth", m),
      D(e, "outerHeight", h),
      D(e, "innerWidth", m),
      D(e, "innerHeight", h));
    const f = e.VisualViewport;
    f &&
      (D(f.prototype, "width", m),
      D(f.prototype, "height", h),
      D(f.prototype, "pageTop", (e, t, n) => (A(e, t, n), 0)));
    const p = e.ScreenOrientation;
    return (
      p &&
        D(p.prototype, "type", (e, t, n) => (A(e, t, n), "landscape-primary")),
      !0
    );
  }
  function no(e, t) {
    const n = t.factors.voice;
    if (!n) return !1;
    const r = e.SpeechSynthesis;
    return (
      !!r &&
      (O(
        r.prototype,
        "getVoices",
        (e, t, r) => {
          let i = A(e, t, r);
          if (i instanceof Array) {
            let e,
              t = -1;
            a.randomPeek(
              n,
              i,
              (n, r) => {
                if (t >= 0) return ((i[t] = n), (t = -1), e);
                ((t = r), (e = i[r]));
              },
              1,
              i.length,
            );
          }
          return i;
        },
        { skipAbsent: !0 },
      ),
      !0)
    );
  }
  function ro(e, t) {
    return (
      (t.customVars || []).forEach((t) => {
        let { path: n, value: r } = t,
          i = n.split("."),
          o = i.pop();
        n = i.join(".");
        let s = a.getValue(e, n);
        s
          ? (I(s, o, r),
            console.info(
              `${e.constructor.name.toUpperCase()} Path:${a.formatString(n, 50)} ---MODIFIED`,
            ))
          : e[l.attach].warns.push(`${n} is undefined`);
      }),
      (t.customProtos || []).forEach((t) => {
        let n = t.name;
        t.properties.forEach((t) => {
          let r = t.key,
            i = t.value;
          switch (t.type) {
            case "string":
              i += "";
              break;
            case "number":
              i = +i;
              break;
            case "boolean":
              i = !!i;
              break;
            case "json":
              i = JSON.parse(i);
              break;
            case "undefined":
              i = void 0;
          }
          let o = a.getValue(e, n);
          o && D(o.prototype, r, (e, t, n) => (A(e, t, n), i));
        });
      }),
      !0
    );
  }
  const io = console.log;
  let oo = 0;
  function so(e, t, n) {
    try {
      if (e[l.window]) return;
      e[l.SCOPE_BROWSER] = t;
    } catch (e) {
      return io(n + " cross domain");
    }
    let r = n + "_" + oo++;
    const i = {
      iframeCheater: N,
      nativeCheater: j,
      workerCheater: F,
      featureCheater: Y,
      navigatorCheater: L,
      locationCheater: ce,
      pluginCheater: de,
      screenCheater: to,
      mediaCheater: Z,
      humanLikeCheater: me,
      driverCheater: z,
      clientRectCheater: ue,
      dateCheater: Ui,
      audioCheater: fe,
      canvasCheater: Oe,
      webglParamsCheater: Ki,
      webglRenderCheater: Qi,
      fontsCheater: le,
      webgpuCheater: eo,
      webrtcCheater: Bi,
      voiceCheater: no,
      envCheater: ro,
      traceCheater: Ee,
    };
    try {
      let n = e[l.attach] || v(e);
      const r = t.enables || {};
      for (let [o, s] of Object.entries(i)) {
        let i = o.replace("Cheater", "");
        (i in r && !r[i]) || (s(e, t) && n.cheaters.push(i));
      }
      n.isFirst = !1;
    } catch (e) {
      io("Injected failed: ", e, n);
    }
    (io(
      `%c${e.constructor.name.toUpperCase()} HAS CHEATED!!!!!!!!!!!!!!!!!!! FROM ${r || "unknown"}`,
      "color:green",
    ),
      (e[l.window] = r));
  }
  function ao(e, t, n) {
    try {
      if (e.__workerCheat__) return;
    } catch (t) {
      return e[l.attach].warns.push(n + " cross domain");
    }
    const r = {
      iframeCheater: N,
      nativeCheater: j,
      navigatorCheater: L,
      locationCheater: ce,
      pluginCheater: de,
      humanLikeCheater: me,
      driverCheater: z,
      featureCheater: Y,
      dateCheater: Ui,
      webrtcCheater: Bi,
      webglParamsCheater: Ki,
      webglRenderCheater: Qi,
      webgpuCheater: eo,
      envCheater: ro,
    };
    try {
      let n = e[l.attach] || v(e);
      const i = t.enables || {};
      for (let [o, s] of Object.entries(r)) {
        let r = o.replace("Cheater", "");
        (r in i && !i[r]) ||
          n.cheaters.includes(r) ||
          (s(e, t) && n.isFirst && n.cheaters.push(r));
      }
      n.isFirst = !1;
    } catch (e) {
      io("Injected failed: ", e);
    }
    (io(
      `${e.constructor.name.toUpperCase()} HAS CHEATED!!!!!!!!!!!!!!!!!!! FROM ${n || "unknown"}`,
    ),
      (e.__workerCheat__ = n));
  }
  const lo = self.Date,
    uo = console.log;
  function co() {
    let e = self[l.SCOPE_BROWSER];
    if (e)
      return (
        (self.top !== self && self.top) || uo("browser from background"),
        e
      );
    try {
      let t = sessionStorage.getItem(l.SCOPE_BROWSER);
      if (!t) return;
      return (
        (e = JSON.parse(t)),
        (self[l.SCOPE_BROWSER] = e),
        (self.top !== self && self.top) || uo("browser from session"),
        e
      );
    } catch (e) {
      return;
    }
  }
  (u(0),
    v(self),
    (() => {
      let t;
      try {
        t = globalThis;
      } catch (e) {}
      try {
        t = e.g;
      } catch (e) {}
      const n = "Window" === t.constructor.name ? so : ao;
      ((t[l.SCOPE_CHEATER] = n),
        (n.run = function () {
          let e = co();
          if (e) {
            if (!e.start) {
              (!(function (e) {
                u(e.seed || 0);
                let t = new a.Random(e.seed);
                if (e.device?.dpr) {
                  let t = e.device.dpr.toFixed(2);
                  window.devicePixelRatio.toFixed(2) === t
                    ? (e.device.dpr = void 0)
                    : (e.device.dpr = t / 1);
                }
                e.userAgent
                  ? e.userAgent.startsWith(navigator.userAgent)
                    ? (e.uaInfo = void 0)
                    : (e.appVersion = e.userAgent.replace("Mozilla/", ""))
                  : (e.uaInfo = void 0);
                const n = e.enables || {};
                e.functionTrace =
                  e.functionTrace ||
                  void 0 === n.functionTrace ||
                  n.functionTrace;
                let r = new lo();
                if (
                  ((e.start = r.getTime()),
                  e.language &&
                    ((e.languages = (e.language + "")
                      .split(",")
                      .map((e) => e.split(";")[0])),
                    (e.languageCode = e.languages.join(","))),
                  e.timezone)
                ) {
                  let t = new lo(
                    r.toLocaleString("en", { timeZone: e.timezone }),
                  );
                  e.timezoneOffset = Math.round(t - r);
                }
                (navigator.userAgent.toLowerCase().includes("firefox") &&
                  (e.memoryCapacity = navigator.deviceMemory),
                  (e.getPlatform = (t) => {
                    let n = e.uaInfo;
                    if (!n) return t;
                    switch (n.os?.name.toString().toLowerCase() || "") {
                      case "ios":
                        return "mobile" === n.device.type ? "iPhone" : "iPad";
                      case "windows":
                        if (t.toLowerCase().includes("win")) return t;
                    }
                    return n.os?.name || t;
                  }));
                let i = e.uaInfo;
                if (i) {
                  let n = e.uaInfo?.engine?.name;
                  if (!n)
                    if (/iphone|ipad/i.test(e.userAgent || "")) n = "Webkit";
                    else {
                      let t = e.uaInfo?.product?.name.toLowerCase() || "";
                      n =
                        "firefox" === t
                          ? "Gecko"
                          : "safari" === t
                            ? "Webkit"
                            : "Blink";
                    }
                  if (((e.uaInfo.engine.name = n), i.engine)) {
                    let t = i.engine.name;
                    /blink.*/i.test(t)
                      ? (e.browserVendor = "Google Inc.")
                      : /webkit.*/i.test(t)
                        ? (e.browserVendor = "Apple Computer, Inc.")
                        : /gecko.*/i.test(t) && (e.browserVendor = "");
                  }
                  let r = i.product || i.browser;
                  if (r) {
                    if (navigator.userAgentData) {
                      let t = [...navigator.userAgentData.brands],
                        n = r.name || "";
                      for (let e in c)
                        if (n.toUpperCase().includes(e.toUpperCase())) {
                          let i = [
                            { brand: c[e] + " " + n, version: r.major },
                            { brand: "Not=A?Brand", version: "8" },
                          ];
                          (t[2] &&
                            i.push({ brand: t[2].brand, version: r.major }),
                            (t = i));
                          break;
                        }
                      e.brands = t;
                    }
                    e.uaFullVersion = r.version;
                  }
                  let o = () => {
                    let t = e.brands || [];
                    if (t.length)
                      return (
                        t[0].brand
                          ? (t = t
                              .filter((e) => !/Not/.test(e.brand))
                              .map((e) => `${e.brand} ${e.version}`))
                          : i.product &&
                            (t = t.map((e) =>
                              e.toString().startsWith("Not;")
                                ? e + " 24"
                                : e + " " + i.product.major,
                            )),
                        t.length > 1 &&
                          (t = t.filter((e) => !/Chromium/.test(e))),
                        t
                      );
                  };
                  if (
                    ((e.brandsVersion = o()),
                    i.device &&
                      ((e.mobile = "mobile" === i.device.type),
                      (e.model = i.device.model || "")),
                    e.brands &&
                      (e.fullVersionList = e.brands.map((e) => ({
                        brand: e.brand,
                        version: e.version + ".0.0.0",
                      }))),
                    (i.cpu || i.os) &&
                      ((i.os?.name.toLowerCase() || "").includes("win")
                        ? (e.arch = t.item(["x86", "x64"]))
                        : (e.arch = (i.cpu || {}).architecture)),
                    (e.bitness = (e.arch || "").toString().includes("32")
                      ? "32"
                      : "64"),
                    i.os)
                  ) {
                    let t = (i.os.version || "").split(".");
                    (t.length > 3
                      ? (t = t.slice(0, 3))
                      : t.length < 3 &&
                        (1 === t.length
                          ? t.push("0.0")
                          : 2 === t.length && t.push("0")),
                      (e.platformVersion = /^\d+(\.\d+)*$/.test(t.join(".")) ? t.join(".") : ""));
                  }
                }
              })(e),
                console.log("%cInjected:" + e.name, "color: green"));
              try {
                if ((n(t, e, "direct"), document)) {
                  document.getElementsByTagName("iframe");
                  for (let r = 0; r < t.length; r++)
                    n(t[r], e, `self array [${r}]`);
                }
                (e.functionTrace && O(console, "clear", function (e, t, n) {}),
                  (e.status = "ok"));
              } catch (t) {
                ((e.status = "err"), console.error(t));
              }
            }
          } else
            console.log(
              "There is no injectable information on the current page...",
            );
        }),
        n.run());
    })());
})();

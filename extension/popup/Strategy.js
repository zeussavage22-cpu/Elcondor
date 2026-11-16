import {
  I as je,
  _ as se,
  k as q,
  r as z,
  o as y,
  g as $,
  F as C,
  p as L,
  c as I,
  w,
  h as oe,
  a as A,
  t as f,
  L as F,
  e as B,
  l as O,
  d as n,
  u as d,
  J as Te,
  f as $e,
  z as Se,
  b as c,
  B as Ee,
  q as Ce,
  x as Z,
  s as Le,
  j as Ae,
  v as E,
  K as Ie,
} from "./popup.js";
import {
  g as ae,
  e as Me,
  p as ee,
  q as He,
  t as Pe,
  u as Be,
  v as De,
  w as Re,
  x as We,
  l as qe,
  y as j,
  z as Ne,
  A as Fe,
  B as Oe,
  C as Ge,
  D as Je,
  E as Qe,
} from "./logo.js";
var Ke = function (o, i) {
    i === !0 && (i = 0);
    var x = "";
    if (typeof o == "string")
      try {
        x = new URL(o).protocol;
      } catch {}
    else o && o.constructor === URL && (x = o.protocol);
    var e = x.split(/\:|\+/).filter(Boolean);
    return typeof i == "number" ? e[i] : e;
  },
  Xe = Ke;
function Ye(r) {
  var o = {
    protocols: [],
    protocol: null,
    port: null,
    resource: "",
    host: "",
    user: "",
    password: "",
    pathname: "",
    hash: "",
    search: "",
    href: r,
    query: {},
    parse_failed: !1,
  };
  try {
    var i = new URL(r);
    ((o.protocols = Xe(i)),
      (o.protocol = o.protocols[0]),
      (o.port = i.port),
      (o.resource = i.hostname),
      (o.host = i.host),
      (o.user = i.username || ""),
      (o.password = i.password || ""),
      (o.pathname = i.pathname),
      (o.hash = i.hash.slice(1)),
      (o.search = i.search.slice(1)),
      (o.href = i.href),
      (o.query = Object.fromEntries(i.searchParams)));
  } catch {
    ((o.protocols = ["file"]),
      (o.protocol = o.protocols[0]),
      (o.port = ""),
      (o.resource = ""),
      (o.user = ""),
      (o.pathname = ""),
      (o.hash = ""),
      (o.search = ""),
      (o.href = r),
      (o.query = {}),
      (o.parse_failed = !0));
  }
  return o;
}
var Ze = Ye;
const et = je(Ze),
  tt = "text/plain",
  st = "us-ascii",
  te = (r, o) => o.some((i) => (i instanceof RegExp ? i.test(r) : i === r)),
  ot = (r, { stripHash: o }) => {
    const i = /^data:(?<type>[^,]*?),(?<data>[^#]*?)(?:#(?<hash>.*))?$/.exec(r);
    if (!i) throw new Error(`Invalid URL: ${r}`);
    let { type: x, data: e, hash: g } = i.groups;
    const h = x.split(";");
    g = o ? "" : g;
    let U = !1;
    h[h.length - 1] === "base64" && (h.pop(), (U = !0));
    const u = (h.shift() || "").toLowerCase(),
      b = [
        ...h
          .map((t) => {
            let [k, m = ""] = t.split("=").map((V) => V.trim());
            return k === "charset" && ((m = m.toLowerCase()), m === st)
              ? ""
              : `${k}${m ? `=${m}` : ""}`;
          })
          .filter(Boolean),
      ];
    return (
      U && b.push("base64"),
      (b.length > 0 || (u && u !== tt)) && b.unshift(u),
      `data:${b.join(";")},${U ? e.trim() : e}${g ? `#${g}` : ""}`
    );
  };
function at(r, o) {
  if (
    ((o = {
      defaultProtocol: "http:",
      normalizeProtocol: !0,
      forceHttp: !1,
      forceHttps: !1,
      stripAuthentication: !0,
      stripHash: !1,
      stripTextFragment: !0,
      stripWWW: !0,
      removeQueryParameters: [/^utm_\w+/i],
      removeTrailingSlash: !0,
      removeSingleSlash: !0,
      removeDirectoryIndex: !1,
      sortQueryParameters: !0,
      ...o,
    }),
    (r = r.trim()),
    /^data:/i.test(r))
  )
    return ot(r, o);
  if (/^view-source:/i.test(r))
    throw new Error(
      "`view-source:` is not supported as it is a non-standard protocol",
    );
  const i = r.startsWith("//");
  (!i && /^\.*\//.test(r)) ||
    (r = r.replace(/^(?!(?:\w+:)?\/\/)|^\/\//, o.defaultProtocol));
  const e = new URL(r);
  if (o.forceHttp && o.forceHttps)
    throw new Error(
      "The `forceHttp` and `forceHttps` options cannot be used together",
    );
  if (
    (o.forceHttp && e.protocol === "https:" && (e.protocol = "http:"),
    o.forceHttps && e.protocol === "http:" && (e.protocol = "https:"),
    o.stripAuthentication && ((e.username = ""), (e.password = "")),
    o.stripHash
      ? (e.hash = "")
      : o.stripTextFragment && (e.hash = e.hash.replace(/#?:~:text.*?$/i, "")),
    e.pathname)
  ) {
    const h = /\b[a-z][a-z\d+\-.]{1,50}:\/\//g;
    let U = 0,
      u = "";
    for (;;) {
      const b = h.exec(e.pathname);
      if (!b) break;
      const t = b[0],
        k = b.index,
        m = e.pathname.slice(U, k);
      ((u += m.replace(/\/{2,}/g, "/")), (u += t), (U = k + t.length));
    }
    const _ = e.pathname.slice(U, e.pathname.length);
    ((u += _.replace(/\/{2,}/g, "/")), (e.pathname = u));
  }
  if (e.pathname)
    try {
      e.pathname = decodeURI(e.pathname);
    } catch {}
  if (
    (o.removeDirectoryIndex === !0 &&
      (o.removeDirectoryIndex = [/^index\.[a-z]+$/]),
    Array.isArray(o.removeDirectoryIndex) && o.removeDirectoryIndex.length > 0)
  ) {
    let h = e.pathname.split("/");
    const U = h[h.length - 1];
    te(U, o.removeDirectoryIndex) &&
      ((h = h.slice(0, -1)), (e.pathname = h.slice(1).join("/") + "/"));
  }
  if (
    (e.hostname &&
      ((e.hostname = e.hostname.replace(/\.$/, "")),
      o.stripWWW &&
        /^www\.(?!www\.)[a-z\-\d]{1,63}\.[a-z.\-\d]{2,63}$/.test(e.hostname) &&
        (e.hostname = e.hostname.replace(/^www\./, ""))),
    Array.isArray(o.removeQueryParameters))
  )
    for (const h of [...e.searchParams.keys()])
      te(h, o.removeQueryParameters) && e.searchParams.delete(h);
  if (
    (o.removeQueryParameters === !0 && (e.search = ""), o.sortQueryParameters)
  ) {
    e.searchParams.sort();
    try {
      e.search = decodeURIComponent(e.search);
    } catch {}
  }
  o.removeTrailingSlash && (e.pathname = e.pathname.replace(/\/$/, ""));
  const g = r;
  return (
    (r = e.toString()),
    !o.removeSingleSlash &&
      e.pathname === "/" &&
      !g.endsWith("/") &&
      e.hash === "" &&
      (r = r.replace(/\/$/, "")),
    (o.removeTrailingSlash || e.pathname === "/") &&
      e.hash === "" &&
      o.removeSingleSlash &&
      (r = r.replace(/\/$/, "")),
    i && !o.normalizeProtocol && (r = r.replace(/^http:\/\//, "//")),
    o.stripProtocol && (r = r.replace(/^(?:https?:)?\/\//, "")),
    r
  );
}
const G = (r, o = !1) => {
  const i =
      /^(?:([a-z_][a-z0-9_-]{0,31})@|https?:\/\/)([\w\.\-@]+)[\/:]([\~,\.\w,\-,\_,\/]+?(?:\.git|\/)?)$/,
    x = (g) => {
      const h = new Error(g);
      throw ((h.subject_url = r), h);
    };
  ((typeof r != "string" || !r.trim()) && x("Invalid url."),
    r.length > G.MAX_INPUT_LENGTH &&
      x(
        "Input exceeds maximum length. If needed, change the value of parseUrl.MAX_INPUT_LENGTH.",
      ),
    o && (typeof o != "object" && (o = { stripHash: !1 }), (r = at(r, o))));
  const e = et(r);
  if (e.parse_failed) {
    const g = e.href.match(i);
    g
      ? ((e.protocols = ["ssh"]),
        (e.protocol = "ssh"),
        (e.resource = g[2]),
        (e.host = g[2]),
        (e.user = g[1]),
        (e.pathname = `/${g[3]}`),
        (e.parse_failed = !1))
      : x("URL parsing failed.");
  }
  return e;
};
G.MAX_INPUT_LENGTH = 2048;
const lt = { class: "col col-between" },
  nt = {
    __name: "OptionItems",
    props: { modelValue: null, items: { type: Array, default: [] } },
    emits: ["update:modelValue"],
    setup(r, { emit: o }) {
      const i = r,
        x = q(() => {
          let u = [],
            _ = i.items.length,
            b = _ <= 5 ? _ : _ % 3 === 0 ? 3 : _ % 4 === 0 ? 4 : 5;
          return (
            i.items.forEach((t, k) => {
              let m = (k / b) | 0,
                V = u[m] || [];
              (V.push(t), (u[m] = V));
            }),
            u
          );
        }),
        e = q(() => {
          let u = 100,
            _ = !1,
            b = x.value[0].length;
          return (
            b > 0 &&
              (typeof i.items[0] == "object" && (_ = !0), (u = 100 / b + "%")),
            { width: u, isObj: _ }
          );
        }),
        g = q(() => i.modelValue),
        h = o;
      function U(u) {
        h("update:modelValue", u);
      }
      return (u, _) => {
        const b = z("el-radio-button"),
          t = z("el-radio-group");
        return (
          y(),
          $("div", lt, [
            (y(!0),
            $(
              C,
              null,
              L(
                x.value,
                (k) => (
                  y(),
                  I(
                    t,
                    {
                      modelValue: g.value,
                      "onUpdate:modelValue":
                        _[0] || (_[0] = (m) => (g.value = m)),
                      onChange: U,
                      style: { width: "100%" },
                      size: "small",
                    },
                    {
                      default: w(() => [
                        (y(!0),
                        $(
                          C,
                          null,
                          L(
                            k,
                            (m) => (
                              y(),
                              I(
                                b,
                                {
                                  label: m.value,
                                  style: oe(`width:${e.value.width};`),
                                },
                                { default: w(() => [A(f(m.key), 1)]), _: 2 },
                                1032,
                                ["label", "style"],
                              )
                            ),
                          ),
                          256,
                        )),
                      ]),
                      _: 2,
                    },
                    1032,
                    ["modelValue"],
                  )
                ),
              ),
              256,
            )),
          ])
        );
      };
    },
  },
  N = se(nt, [["__scopeId", "data-v-5f82826e"]]),
  rt = { class: "row", style: { height: "100%", width: "100%" } },
  it = {
    class: "row",
    style: { float: "left", width: "20%", "align-items": "center" },
  },
  dt = ["src"],
  ct = {
    style: {
      float: "right",
      width: "100%",
      color: "var(--el-text-color-secondary)",
      "font-size": "13px",
      "text-align": "right",
    },
  },
  ht = {
    __name: "BrowserSelector",
    emits: ["update:modelValue"],
    setup(r, { emit: o }) {
      let { tr: i, ident: x, defined: e } = F.tr();
      const g = o,
        h = B([]),
        U = O(!1),
        u = O(-1);
      function _(t = "") {
        ((U.value = !0),
          Me(t, 1, 5e3).then((k) => {
            (h.splice(0, h.length),
              (
                (k.data || [])
                  .filter((V) => !V.isTemp)
                  .filter((V) => !(V.browserId + "").startsWith("random")) || []
              ).forEach((V) => h.push(V)),
              (U.value = !1));
          }));
      }
      function b() {
        g("update:modelValue", u);
      }
      return (
        _(),
        (t, k) => {
          const m = z("el-option"),
            V = z("el-select");
          return (
            y(),
            I(
              V,
              {
                modelValue: u.value,
                "onUpdate:modelValue": k[0] || (k[0] = (v) => (u.value = v)),
                filterable: "",
                remote: "",
                placeholder: d(x)("selectB"),
                "remote-show-suffix": "",
                "remote-method": _,
                onChange: b,
                loading: U.value,
                size: "small",
              },
              {
                default: w(() => [
                  (y(!0),
                  $(
                    C,
                    null,
                    L(
                      h,
                      (v) => (
                        y(),
                        I(
                          m,
                          { key: v.id, label: v.name, value: v.id },
                          {
                            default: w(() => [
                              n("div", rt, [
                                n("div", it, [
                                  n(
                                    "img",
                                    {
                                      src: d(ae)(v.uaInfo.product.name),
                                      alt: "logo",
                                      style: { width: "20px", height: "20px" },
                                    },
                                    null,
                                    8,
                                    dt,
                                  ),
                                ]),
                                n("div", ct, f(v.name), 1),
                              ]),
                            ]),
                            _: 2,
                          },
                          1032,
                          ["label", "value"],
                        )
                      ),
                    ),
                    128,
                  )),
                ]),
                _: 1,
              },
              8,
              ["modelValue", "placeholder", "loading"],
            )
          );
        }
      );
    },
  },
  ut = {
    class: "col",
    style: { height: "100%", width: "96%", margin: "auto" },
  },
  mt = {
    class: "row",
    style: {
      height: "6%",
      "box-shadow": "0 8px 6px -8px #737373",
      position: "relative",
      "z-index": "5",
      "justify-content": "center",
    },
  },
  pt = {
    class: "row",
    style: {
      "align-items": "center",
      "align-content": "center",
      width: "100%",
    },
  },
  ft = {
    class: "row",
    style: {
      "align-items": "center",
      height: "10%",
      width: "100%",
      "margin-bottom": "1%",
    },
  },
  gt = { class: "item-blue", style: { "text-align": "left", width: "38%" } },
  bt = { class: "row", style: { width: "auto", "margin-right": "4%" } },
  wt = {
    class: "col",
    style: { height: "10%", width: "100%", margin: "3% 0 1% 0" },
  },
  vt = { class: "item-blue", style: { "text-align": "left", width: "90%" } },
  yt = {
    class: "col",
    style: { height: "25%", width: "100%", margin: "2% 0 1% 0" },
  },
  _t = { class: "item-blue", style: { "text-align": "left", width: "90%" } },
  zt = {
    key: 0,
    class: "col",
    style: { height: "25%", width: "100%", margin: "2% 0 1% 0" },
  },
  xt = { class: "item-blue", style: { "text-align": "left", width: "90%" } },
  Ut = {
    class: "row",
    style: {
      "align-items": "center",
      height: "10%",
      width: "100%",
      "margin-bottom": "1%",
    },
  },
  Vt = { class: "item-blue", style: { "text-align": "left", width: "19%" } },
  kt = { class: "row", style: { width: "80%", "margin-right": "4%" } },
  jt = {
    class: "col",
    style: { height: "25%", width: "100%", margin: "2% 0 1% 0" },
  },
  Tt = { class: "item-blue" },
  $t = { class: "col", style: { width: "50%", "margin-right": "4%" } },
  St = { class: "item-green" },
  Et = { class: "col" },
  Ct = {
    class: "col",
    style: {
      "vertical-align": "center",
      "place-content": "center",
      width: "96%",
    },
  },
  Lt = {
    style: {
      width: "90%",
      "font-size": "1.6rem",
      "font-weight": "bold",
      color: "#363636",
    },
  },
  At = { class: "col", style: { margin: "0.2rem 0" } },
  It = { class: "item-gray" },
  Mt = { class: "col", style: { margin: "0.2rem 0" } },
  Ht = { class: "item-gray" },
  Pt = { class: "col", style: { margin: "0.2rem 0" } },
  Bt = { class: "item-gray" },
  Dt = { class: "row-between", style: { width: "100%" } },
  Rt = { class: "omit", style: { "margin-right": "10px", width: "50%" } },
  Wt = {
    style: { color: "var(--el-text-color-secondary)", "font-size": "1rem" },
  },
  qt = { class: "col", style: { margin: "0.2rem 0" } },
  Nt = { class: "item-gray" },
  Ft = { class: "row-between", style: { width: "100%" } },
  Ot = {
    style: { color: "var(--el-text-color-secondary)", "font-size": "1rem" },
  },
  Gt = { class: "col", style: { margin: "0.2rem 0" } },
  Jt = { class: "col", style: { margin: "0.5rem 0" } },
  Qt = { class: "item-gray" },
  Kt = {
    class: "row-between",
    style: { "flex-wrap": "wrap", width: "98%", margin: "auto" },
  },
  Xt = { class: "col", style: { width: "100%", "margin-top": "5px" } },
  Yt = {
    class: "row",
    style: {
      width: "100%",
      "justify-content": "space-between",
      "margin-bottom": "5px",
    },
  },
  Zt = { class: "row", style: { width: "49%" } },
  es = {
    style: {
      width: "78%",
      "font-size": "1.6rem",
      "font-weight": "bold",
      color: "#363636",
    },
  },
  ts = { class: "row", style: { width: "49%" } },
  ss = {
    style: {
      width: "78%",
      "font-size": "1.6rem",
      "font-weight": "bold",
      color: "#363636",
    },
  },
  os = { class: "row", style: { width: "100%" } },
  as = {
    class: "row",
    style: { width: "100%", "justify-content": "space-between" },
  },
  ls = {
    __name: "Strategy",
    setup(r) {
      let { languages: o, regions: i } = Le,
        { tr: x, ident: e, defined: g } = F.tr();
      g({
        onNew: { zh: "打开新页面时", en: "On new tab" },
        none: { zh: "无", en: "no-ops" },
        jt: { zh: "静态", en: "static" },
        pp: { zh: "匹配", en: "match" },
        sjxq: { zh: "随机选取", en: "random select" },
        sjsc: { zh: "随机生成", en: "random generate" },
        sjyc: { zh: "随机一次", en: "random once" },
        autoSwitch: { zh: "自动切换指纹", en: "Auto switching fp" },
        openBrowser: { zh: "打开浏览器时", en: "opening browser" },
        reloadPage: { zh: "加载页面时", en: "reloading page" },
        shortcuts: { zh: "快捷键", en: "Shortcuts" },
        newMachine: { zh: "一键新机", en: "New machine" },
        switchEnv: { zh: "切换环境", en: "Switching env" },
        mlsb: { zh: "模拟设备", en: "Simulated device" },
        mlxt: { zh: "模拟系统", en: "Simulated os" },
        dl: { zh: "电脑", en: "computer" },
        sj: { zh: "手机", en: "mobile phone" },
        pb: { zh: "平板", en: "tablet PC" },
        data: { zh: "数据", en: "Data" },
        drsj: { zh: "导入数据", en: "Import data" },
        drcg: { zh: "导入成功", en: "Import successful" },
        drsb: { zh: "导入失败", en: "Import failure" },
        dcsj: { zh: "导出数据", en: "Export data" },
        dccg: { zh: "导出成功", en: "Export successful" },
        dcsb: { zh: "导出失败", en: "Export failure" },
        ppgz: { zh: "匹配规则", en: "Matching rules" },
        hbmd: { zh: "黑白名单", en: "Black and White List" },
        jtpz: { zh: "静态配置", en: "Static configuration" },
        pplb: { zh: "匹配列表", en: "Match List" },
        selectB: {
          zh: "选择你的浏览器",
          en: "Please select your browser configuration",
        },
        bds: { zh: "表达式", en: "matching expression" },
        llqpz: { zh: "浏览器配置", en: "browser configuration" },
        bmd: { zh: "白名单", en: "Whitelist" },
        hmd: { zh: "黑名单", en: "Blacklist" },
        inputB: {
          zh: "输入白名单域名或URL",
          en: "input a whitelist domain name or URL",
        },
        inputH: {
          zh: "输入黑名单域名或URL",
          en: "input a blacklist domain name or URL",
        },
        ym: { zh: "域名", en: "domain name" },
        aqms: { zh: "安全模式", en: "Safe mode" },
        webglaqms: { zh: "webgl安全模式", en: "Webgl safe mode" },
        aqmsSM: {
          zh: "安全模式启用时，随机的UA不会与真实环境具有较大的差异，避免因为模拟的环境与正式的环境过大、暴露的方面越多；同时canvas、audio和webgl等缓冲区的干扰也会从加噪音算法切换为其他更保守的算法",
          en: "When the safe mode is enabled,the extension will not switch the UA with large differences in version, and will only make changes on the source UA;At the same time, the interference in buffers such as canvas, audio, and webgl will switch from noise-adding algorithms to other more conservative algorithms.",
        },
        webglaqmsSM: {
          zh: "启用时，不会完全修改webgl的参数信息",
          en: "When enabled,the parameter information of WebGL will not be completely modified",
        },
        jyW: { zh: "禁用worker", en: "Disable Worker" },
        jyWSM: {
          zh: "强制worker之间不允许通讯",
          en: "Block all types of Worker from communicating with the current page",
        },
        hszz: { zh: "函数追踪", en: "Function Trace" },
        hszzSM: {
          zh: "开启后可以使部分高级的检测手段失效（严重影响浏览器性能，慎用）",
          en: "Enabling this may disable some advanced detection methods (severely impacting browser performance, use with caution)",
        },
        fts: { zh: "绕过反调试", en: "Bypass anti-debugger" },
        ftsSM: {
          zh: "开启后可以禁用掉网页无限调试",
          en: "After enabling, unlimited debugging of web pages can be disabled",
        },
        jtllq: { zh: "静态浏览器", en: "Static browser configuration" },
        jtua: { zh: "静态UA", en: "Static userAgent" },
        jtsq: { zh: "静态时区", en: "Static timezone" },
        jtyy: { zh: "静态语言", en: "Static language" },
        gnqykz: { zh: "功能启用控制", en: "Function activation control" },
        inputM: { zh: "输入匹配表达式", en: "Enter matching expression" },
        ymError: { zh: "无法解析出域名", en: "Unable to resolve domain name" },
        ymInvalid: {
          zh: "请输入正确的域名或URL",
          en: "Please enter the correct domain name or URL",
        },
        bdsEmp: { zh: "表达式不能为空", en: "The expression cannot be empty" },
        inputUA: { zh: "请输入userAgent", en: "Please enter a userAgent" },
        selectLang: { zh: "请选择语言", en: "Please select a language" },
        selectTime: { zh: "请选择时区", en: "Please select a timezone" },
        fmtError: { zh: "格式错误", en: "Format error" },
        randomSeed: { zh: "全局随机数种子", en: "Global random seed" },
        ipcx: { zh: "在线查询IP", en: "Online IP lookup" },
        ipcxSM: {
          zh: "关闭后不会向任何外部服务发送请求，时区和语言使用本机的真实值",
          en: "When disabled, no request is sent to any external service; the real local timezone and language are used",
        },
        realistic: { zh: "真实化配置", en: "Realistic profiles" },
        realisticSM: {
          zh: "生成与模拟系统相符的现代UA、显卡、内存、处理器和屏幕参数（关闭则使用原来的完全随机算法）",
          en: "Generate a modern UA, GPU, memory, CPU and screen values that match the simulated system (disable to use the original fully random algorithm)",
        },
        perSite: { zh: "每个站点不同指纹", en: "Per-site fingerprint" },
        perSiteSM: {
          zh: "每个网站获得不同但稳定的噪声，网站之间无法关联你的指纹",
          en: "Each website gets a different but stable noise, so sites cannot link your fingerprint to each other",
        },
        notify: { zh: "通知", en: "Notifications" },
        notifySM: {
          zh: "浏览器启动时显示扩展状态通知",
          en: "Show a notification with the extension status when the browser starts",
        },
      });
      const h = Ae(),
        U = [
          {
            statusName: "safeMode",
            label: "aqms",
            description: "aqmsSM",
            switch() {
              let a = !t.safeMode;
              return ((t.safeMode = a), j("safeMode", a), !1);
            },
            enable: !0,
          },
          {
            statusName: "webglSafeMode",
            label: "webglaqms",
            description: "webglaqmsSM",
            switch() {
              let a = !t.webglSafeMode;
              return ((t.webglSafeMode = a), j("webglSafeMode", a), !1);
            },
            enable: !0,
          },
          {
            statusName: "disableWorker",
            label: "jyW",
            description: "jyWSM",
            switch() {
              let a = !t.disableWorker;
              return ((t.disableWorker = a), j("disableWorker", a), !1);
            },
            enable: !0,
          },
          {
            statusName: "ipLookup",
            label: "ipcx",
            description: "ipcxSM",
            switch() {
              let a = !t.ipLookup;
              return ((t.ipLookup = a), j("ipLookup", a), !1);
            },
            enable: !0,
          },
          {
            statusName: "realistic",
            label: "realistic",
            description: "realisticSM",
            switch() {
              let a = !t.realistic;
              return ((t.realistic = a), j("realistic", a), !1);
            },
            enable: !0,
          },
          {
            statusName: "perSite",
            label: "perSite",
            description: "perSiteSM",
            switch() {
              let a = !t.perSite;
              return ((t.perSite = a), j("perSite", a), !1);
            },
            enable: !0,
          },
          {
            statusName: "notify",
            label: "notify",
            description: "notifySM",
            switch() {
              let a = !t.notify;
              return ((t.notify = a), j("notify", a), !1);
            },
            enable: !0,
          },
          {
            statusName: "functionTrace",
            label: "hszz",
            description: "hszzSM",
            switch() {
              let a = !t.functionTrace;
              return ((t.functionTrace = a), j("functionTrace", a), !1);
            },
            enable: !0,
          },
          {
            statusName: "antiDebugger",
            label: "fts",
            description: "ftsSM",
            switch() {
              let a = !t.antiDebugger;
              return ((t.antiDebugger = a), j("antiDebugger", a), !1);
            },
            enable: !1,
          },
        ],
        u = ["none", "jt", "sjxq", "sjsc"].map((a) => ({
          value: e(a, { lang: "zh" }),
          key: e(a),
        })),
        _ = [
          { value: "desktop", key: e("dl") },
          { value: "mobile", key: e("sj") },
          { value: "tablet", key: e("pb") },
        ],
        b = O([]),
        t = B({
          loading: !1,
          option: "无",
          autoSwitch: "",
          deviceType: "",
          os: "",
          tabId: "",
          safeMode: !1,
          webglSafeMode: !1,
          disableWorker: !1,
          functionTrace: !1,
          antiDebugger: !1,
          ipLookup: !0,
          realistic: !0,
          perSite: !1,
          notify: !0,
          randomSeed: void 0,
          fixedBrowserId: "",
          fixedUA: "",
          fixedTimezone: "",
          fixedLanguage: "",
          iceServers: "",
          enables: (() => {
            let a = {};
            for (let s of ee) a[s] = 0;
            return a;
          })(),
          hostEnable: "",
          disabled: { export: !1, import: !1 },
          shortcuts: { switchEnv: "", newMachine: "" },
        }),
        k = {
          desktop: ["windows", "macos", "linux"],
          mobile: ["android", "ios"],
          tablet: ["android", "ios"],
        };
      Te(
        () => t.deviceType,
        (a) => {
          if (!a) {
            b.value = [];
            return;
          }
          b.value = k[a] || [];
        },
      );
      const m = B({
          page: 1,
          size: 5,
          total: 1,
          matches: [],
          regex: "",
          browserId: "",
        }),
        V = B({ page: 1, size: 5, total: 1, proxies: [], input: "" }),
        v = B({ whiteHost: "", blackHost: "", white: [], black: [] });
      $e(() => {
        le();
      });
      function le() {
        (we(),
          R(),
          be(),
          He().then((a) => {
            const s = t.enables,
              p = t.shortcuts;
            for (let [S, H] of Object.entries(a)) t[S] = H;
            ((t.safeMode = t.safeMode || !1),
              (t.webglSafeMode = t.webglSafeMode || !1),
              (t.disableWorker = t.disableWorker || !1),
              (t.functionTrace = t.functionTrace || !1),
              (t.antiDebugger = t.antiDebugger || !1),
              (t.randomSeed = t.randomSeed || 0),
              (t.enables = { ...s, ...t.enables }),
              (t.shortcuts = { ...p, ...t.shortcuts }),
              (t.iceServers = t.iceServers
                ? JSON.stringify(t.iceServers)
                : ""));
          }));
      }
      function ne(a) {
        const s = a.target.files[0];
        let p = new FileReader();
        ((p.onload = async () => {
          try {
            let S = p.result || "{}",
              H = JSON.parse(S);
            (await Ne(H), E.success(`${e("drcg")}!`));
          } catch (S) {
            E.error(`${e("drsb")}!${S.message}`);
          }
        }),
          (p.onerror = (S) => {
            E.error(e("drsb!"));
          }),
          p.readAsText(s));
      }
      async function re() {
        t.disabled.export = !0;
        try {
          let a = await Fe(),
            s = JSON.stringify(a),
            p = new Blob([s], { type: "application/json" }),
            S = URL.createObjectURL(p);
          Oe(S, "CthulhuJs.json").then(() => {
            E.success(`${e("dccg")}!`);
          });
        } catch (a) {
          E.error(`${e("dcsb")}!${a.message}`);
        } finally {
          setTimeout(() => {
            t.disabled.export = !1;
          }, 500);
        }
      }
      function ie(a) {
        Ge(a);
      }
      function de(a) {
        (j("deviceType", a), (t.os = ""), j("os", ""));
      }
      function ce(a) {
        j("os", a);
      }
      function he(a) {
        ((t.fixedBrowserId = a), j("fixedBrowserId", a));
      }
      function ue(a) {
        ((t.fixedLanguage = a), j("fixedLanguage", a));
      }
      function me(a) {
        ((t.fixedTimezone = a), j("fixedTimezone", a));
      }
      function pe(a) {
        ((t.fixedUA = a), j("fixedUA", a));
      }
      function fe(a, s) {
        if (
          ((t.shortcuts[a] = s),
          s && t.shortcuts.switchEnv === t.shortcuts.newMachine)
        ) {
          E.error("Shortcut conflict!");
          return;
        }
        j("shortcuts", t.shortcuts);
      }
      function ge() {
        h.push({ name: "Home" });
      }
      function be() {
        Pe(V.page, V.size).then((a) => {
          ((V.proxies = a.data), (V.total = a.total));
        });
      }
      function we() {
        Be(m.page, m.size).then((a) => {
          ((m.matches = a.data || []),
            m.matches.forEach(ve),
            (m.total = a.total));
        });
      }
      function R() {
        De().then((a) => {
          ((v.white = (a.white || []).map((s, p) => ({ host: s, index: p }))),
            (v.black = (a.black || []).map((s, p) => ({ host: s, index: p }))));
        });
      }
      async function ve(a) {
        let s = await Re(a.browserId);
        if (!s || !s.userAgent) return;
        let p = await We(s.userAgent);
        p &&
          ((a.logo = ae((p.product || p.browser || {}).name)),
          (a.name = s.name));
      }
      function J(a, s) {
        try {
          let p = G(a);
          if (!p.resource) {
            E.warning(e("ymError"));
            return;
          }
          a = p.resource;
        } catch {
          E.warning(e("ymInvalid"));
          return;
        }
        Je(a, s)
          .then(() => {
            ((v.whiteHost = ""), (v.blackHost = ""), R());
          })
          .catch((p) => {
            E.error(`${e("tjsb")},` + p.message);
          });
      }
      function ye() {
        return j("enables", t.enables).then((a) => {});
      }
      function _e() {
        let a = JSON.parse(t.iceServers || "[]");
        if (
          !(a instanceof Array) ||
          (a.length && !a.filter((s) => s.urls).length)
        ) {
          E.error(`iceServers: ${e("fmtError")}`);
          return;
        }
        return j("iceServers", a).then((s) => {});
      }
      function Q(a, s) {
        Qe(a, s).then(() => {
          R();
        });
      }
      return (a, s) => {
        const p = z("el-avatar"),
          S = z("el-page-header"),
          H = z("el-icon"),
          ze = z("el-popover"),
          W = z("el-switch"),
          D = z("el-button"),
          P = z("el-input"),
          K = z("el-option"),
          X = z("el-select"),
          xe = z("el-checkbox"),
          M = z("el-table-column"),
          Y = z("el-table"),
          Ue = z("el-divider"),
          Ve = z("el-scrollbar"),
          ke = Ee("loading");
        return Se(
          (y(),
          $("div", ut, [
            n("div", mt, [
              c(
                S,
                { onBack: ge, style: { width: "100%" } },
                {
                  content: w(() => [
                    n("div", pt, [
                      c(
                        p,
                        {
                          class: "mr-3",
                          size: 28,
                          src: d(qe),
                          style: { "margin-right": "3px", float: "left" },
                        },
                        null,
                        8,
                        ["src"],
                      ),
                    ]),
                  ]),
                  _: 1,
                },
              ),
              n(
                "div",
                {
                  class: "text-large font-600",
                  style: oe(
                    `font-weight:bold;${d(F).curLang === "zh" ? "letter-spacing: 5px;" : ""}width:60%;text-align: center;position: absolute;top:10%;height:100%`,
                  ),
                },
                f(d(e)("setT")),
                5,
              ),
            ]),
            c(
              Ve,
              { style: { width: "100%", height: "90%" }, id: "scrollbar" },
              {
                default: w(() => [
                  (y(),
                  $(
                    C,
                    null,
                    L(U, (l) =>
                      n("div", ft, [
                        c(
                          ze,
                          {
                            placement: "top-start",
                            trigger: "click",
                            content: d(e)(l.description),
                          },
                          {
                            reference: w(() => [
                              c(
                                H,
                                {
                                  style: {
                                    color: "orange",
                                    "margin-right": "2px",
                                    cursor: "pointer",
                                  },
                                },
                                { default: w(() => [c(d(Ie))]), _: 1 },
                              ),
                            ]),
                            _: 2,
                          },
                          1032,
                          ["content"],
                        ),
                        n("div", gt, f(d(e)(l.label)), 1),
                        n("div", bt, [
                          c(
                            W,
                            {
                              modelValue: t[l.statusName],
                              "onUpdate:modelValue": (T) =>
                                (t[l.statusName] = T),
                              "active-value": !0,
                              "inactive-value": !1,
                              "before-change": l.switch,
                              disabled: !l.enable,
                            },
                            null,
                            8,
                            [
                              "modelValue",
                              "onUpdate:modelValue",
                              "before-change",
                              "disabled",
                            ],
                          ),
                        ]),
                      ]),
                    ),
                    64,
                  )),
                  n("div", wt, [
                    n("div", vt, f(d(e)("onNew")), 1),
                    c(
                      N,
                      {
                        modelValue: t.option,
                        "onUpdate:modelValue":
                          s[0] || (s[0] = (l) => (t.option = l)),
                        items: d(u),
                        onChange: s[1] || (s[1] = (l) => ie(t.option)),
                        style: { width: "96%" },
                      },
                      null,
                      8,
                      ["modelValue", "items"],
                    ),
                  ]),
                  n("div", yt, [
                    n("div", _t, f(d(e)("mlsb")), 1),
                    c(
                      N,
                      {
                        modelValue: t.deviceType,
                        "onUpdate:modelValue":
                          s[2] || (s[2] = (l) => (t.deviceType = l)),
                        items: _,
                        onChange: s[3] || (s[3] = (l) => de(t.deviceType)),
                        style: { width: "96%" },
                      },
                      null,
                      8,
                      ["modelValue"],
                    ),
                  ]),
                  b.value.length
                    ? (y(),
                      $("div", zt, [
                        n("div", xt, f(d(e)("mlxt")), 1),
                        c(
                          N,
                          {
                            modelValue: t.os,
                            "onUpdate:modelValue":
                              s[4] || (s[4] = (l) => (t.os = l)),
                            items: b.value.map((l) => ({ value: l, key: l })),
                            onChange: s[5] || (s[5] = (l) => ce(t.os)),
                            style: { width: "96%", "margin-top": "0.6rem" },
                          },
                          null,
                          8,
                          ["modelValue", "items"],
                        ),
                      ]))
                    : Ce("", !0),
                  n("div", Ut, [
                    n("div", Vt, f(d(e)("data")), 1),
                    n("div", kt, [
                      c(
                        D,
                        {
                          type: "warning",
                          plain: "",
                          round: "",
                          size: "small",
                          class: "file-box",
                          disabled: t.disabled.import,
                          style: { width: "50%", float: "left" },
                        },
                        {
                          default: w(() => [
                            n(
                              "input",
                              {
                                name: "file",
                                accept: "application/json",
                                type: "file",
                                onChange: ne,
                                class: "file-btn",
                              },
                              null,
                              32,
                            ),
                            A(" " + f(d(e)("drsj")), 1),
                          ]),
                          _: 1,
                        },
                        8,
                        ["disabled"],
                      ),
                      c(
                        D,
                        {
                          type: "success",
                          plain: "",
                          round: "",
                          onClick: re,
                          size: "small",
                          disabled: t.disabled.export,
                          style: { width: "50%", float: "right" },
                        },
                        { default: w(() => [A(f(d(e)("dcsj")), 1)]), _: 1 },
                        8,
                        ["disabled"],
                      ),
                    ]),
                  ]),
                  n("div", jt, [
                    n("div", Tt, f(d(e)("shortcuts")), 1),
                    n("div", $t, [
                      (y(!0),
                      $(
                        C,
                        null,
                        L(
                          Object.keys(t.shortcuts),
                          (l) => (
                            y(),
                            $(
                              "div",
                              {
                                class: "row",
                                style: {
                                  "justify-content": "space-between",
                                  "align-items": "center",
                                },
                                key: l,
                              },
                              [
                                n("span", St, f(d(e)(l)), 1),
                                s[22] ||
                                  (s[22] = n(
                                    "span",
                                    { class: "item-gray" },
                                    "Ctrl + Shift +",
                                    -1,
                                  )),
                                c(
                                  P,
                                  {
                                    modelValue: t.shortcuts[l],
                                    "onUpdate:modelValue": (T) =>
                                      (t.shortcuts[l] = T),
                                    size: "small",
                                    style: {
                                      width: "50px",
                                      margin: "0 0.5rem",
                                      height: "1.5rem",
                                    },
                                    formatter: (T) => T.toUpperCase(),
                                    maxlength: 1,
                                    onChange: (T) => fe(l, T),
                                  },
                                  null,
                                  8,
                                  [
                                    "modelValue",
                                    "onUpdate:modelValue",
                                    "formatter",
                                    "onChange",
                                  ],
                                ),
                              ],
                            )
                          ),
                        ),
                        128,
                      )),
                    ]),
                  ]),
                  n("div", Et, [
                    n("div", Ct, [
                      n("span", Lt, f(d(e)("jtpz")), 1),
                      n("div", At, [
                        n("span", It, f(d(e)("jtllq")), 1),
                        c(
                          ht,
                          {
                            modelValue: t.fixedBrowserId,
                            "onUpdate:modelValue":
                              s[6] || (s[6] = (l) => (t.fixedBrowserId = l)),
                            onChange: he,
                          },
                          null,
                          8,
                          ["modelValue"],
                        ),
                      ]),
                      n("div", Mt, [
                        n("span", Ht, f(d(e)("jtua")), 1),
                        c(
                          P,
                          {
                            modelValue: t.fixedUA,
                            "onUpdate:modelValue":
                              s[7] || (s[7] = (l) => (t.fixedUA = l)),
                            placeholder: d(e)("inputUA"),
                            clearable: "",
                            onFocusout: s[8] || (s[8] = (l) => pe(t.fixedUA)),
                            size: "small",
                          },
                          null,
                          8,
                          ["modelValue", "placeholder"],
                        ),
                      ]),
                      n("div", Pt, [
                        n("span", Bt, f(d(e)("jtsq")), 1),
                        c(
                          X,
                          {
                            modelValue: t.fixedTimezone,
                            "onUpdate:modelValue":
                              s[9] || (s[9] = (l) => (t.fixedTimezone = l)),
                            placeholder: d(e)("selectTime"),
                            filterable: "",
                            "allow-create": "",
                            clearable: "",
                            onChange:
                              s[10] || (s[10] = (l) => me(t.fixedTimezone)),
                            size: "small",
                          },
                          {
                            default: w(() => [
                              (y(!0),
                              $(
                                C,
                                null,
                                L(
                                  d(i),
                                  (l, T) => (
                                    y(),
                                    I(
                                      K,
                                      {
                                        value: l.timezone,
                                        label:
                                          self.__tzName(l) +
                                          "-" +
                                          l.timezone,
                                      },
                                      {
                                        default: w(() => [
                                          n("div", Dt, [
                                            n(
                                              "div",
                                              Rt,
                                              f(self.__tzName(l)),
                                              1,
                                            ),
                                            n("span", Wt, f(l.timezone), 1),
                                          ]),
                                        ]),
                                        _: 2,
                                      },
                                      1032,
                                      ["value", "label"],
                                    )
                                  ),
                                ),
                                256,
                              )),
                            ]),
                            _: 1,
                          },
                          8,
                          ["modelValue", "placeholder"],
                        ),
                      ]),
                      n("div", qt, [
                        n("span", Nt, f(d(e)("jtyy")), 1),
                        c(
                          X,
                          {
                            modelValue: t.fixedLanguage,
                            "onUpdate:modelValue":
                              s[11] || (s[11] = (l) => (t.fixedLanguage = l)),
                            placeholder: d(e)("selectLang"),
                            filterable: "",
                            "allow-create": "",
                            clearable: "",
                            onChange:
                              s[12] || (s[12] = (l) => ue(t.fixedLanguage)),
                            size: "small",
                          },
                          {
                            default: w(() => [
                              (y(!0),
                              $(
                                C,
                                null,
                                L(
                                  d(o),
                                  (l, T) => (
                                    y(),
                                    I(
                                      K,
                                      {
                                        value: l.code,
                                        label: l.name + "-" + l.code,
                                      },
                                      {
                                        default: w(() => [
                                          n("div", Ft, [
                                            n("span", null, f(l.name), 1),
                                            n("span", Ot, f(l.code), 1),
                                          ]),
                                        ]),
                                        _: 2,
                                      },
                                      1032,
                                      ["value", "label"],
                                    )
                                  ),
                                ),
                                256,
                              )),
                            ]),
                            _: 1,
                          },
                          8,
                          ["modelValue", "placeholder"],
                        ),
                      ]),
                      n("div", Gt, [
                        s[23] ||
                          (s[23] = n(
                            "span",
                            { class: "item-gray" },
                            "ICE Servers",
                            -1,
                          )),
                        c(
                          P,
                          {
                            modelValue: t.iceServers,
                            "onUpdate:modelValue":
                              s[13] || (s[13] = (l) => (t.iceServers = l)),
                            size: "small",
                            clearable: "",
                            onChange: _e,
                          },
                          null,
                          8,
                          ["modelValue"],
                        ),
                      ]),
                      n("div", Jt, [
                        n("div", Qt, f(d(e)("gnqykz")), 1),
                        n("div", Kt, [
                          (y(!0),
                          $(
                            C,
                            null,
                            L(
                              d(ee),
                              (l) => (
                                y(),
                                I(
                                  xe,
                                  {
                                    modelValue: t.enables[l],
                                    "onUpdate:modelValue": (T) =>
                                      (t.enables[l] = T),
                                    label: l,
                                    "true-value": 1,
                                    "false-value": 0,
                                    size: "small",
                                    border: "",
                                    style: {
                                      margin: `2px 0 2px\r
                           2px`,
                                      width: "8rem",
                                    },
                                    onChange: ye,
                                  },
                                  null,
                                  8,
                                  [
                                    "modelValue",
                                    "onUpdate:modelValue",
                                    "label",
                                  ],
                                )
                              ),
                            ),
                            256,
                          )),
                        ]),
                      ]),
                    ]),
                    n("div", Xt, [
                      n("div", Yt, [
                        n("div", Zt, [
                          n("span", es, f(d(e)("bmd")), 1),
                          c(
                            W,
                            {
                              modelValue: t.hostEnable,
                              "onUpdate:modelValue":
                                s[14] || (s[14] = (l) => (t.hostEnable = l)),
                              "active-value": "white",
                              "inactive-value": 0,
                              onClick:
                                s[15] ||
                                (s[15] = (l) => j("hostEnable", t.hostEnable)),
                            },
                            null,
                            8,
                            ["modelValue"],
                          ),
                        ]),
                        n("div", ts, [
                          n("span", ss, f(d(e)("hmd")), 1),
                          c(
                            W,
                            {
                              modelValue: t.hostEnable,
                              "onUpdate:modelValue":
                                s[16] || (s[16] = (l) => (t.hostEnable = l)),
                              "active-value": "black",
                              "inactive-value": 0,
                              onClick:
                                s[17] ||
                                (s[17] = (l) => j("hostEnable", t.hostEnable)),
                            },
                            null,
                            8,
                            ["modelValue"],
                          ),
                        ]),
                      ]),
                      n("div", os, [
                        c(
                          P,
                          {
                            clearable: "",
                            placeholder: d(e)("inputB"),
                            modelValue: v.whiteHost,
                            "onUpdate:modelValue":
                              s[18] || (s[18] = (l) => (v.whiteHost = l)),
                            onKeydown:
                              s[19] ||
                              (s[19] = Z((l) => J(v.whiteHost, !0), ["enter"])),
                            style: { width: "47%" },
                            size: "small",
                          },
                          {
                            suffix: w(() => s[24] || (s[24] = [A(" ↲ ")])),
                            _: 1,
                          },
                          8,
                          ["placeholder", "modelValue"],
                        ),
                        s[26] ||
                          (s[26] = n(
                            "div",
                            { style: { width: "3%" } },
                            null,
                            -1,
                          )),
                        c(
                          P,
                          {
                            clearable: "",
                            placeholder: d(e)("inputH"),
                            modelValue: v.blackHost,
                            "onUpdate:modelValue":
                              s[20] || (s[20] = (l) => (v.blackHost = l)),
                            onKeydown:
                              s[21] ||
                              (s[21] = Z((l) => J(v.blackHost, !1), ["enter"])),
                            style: { width: "47%" },
                            size: "small",
                          },
                          {
                            suffix: w(() => s[25] || (s[25] = [A(" ↲ ")])),
                            _: 1,
                          },
                          8,
                          ["placeholder", "modelValue"],
                        ),
                      ]),
                      n("div", as, [
                        c(
                          Y,
                          {
                            data: v.white,
                            style: { width: "49%" },
                            size: "small",
                            height: "200",
                          },
                          {
                            default: w(() => [
                              c(M, { prop: "index", label: "#", width: "15" }),
                              c(
                                M,
                                { prop: "host", label: d(e)("ym") },
                                null,
                                8,
                                ["label"],
                              ),
                              c(
                                M,
                                { prop: "", label: " ", width: "45" },
                                {
                                  default: w((l) => [
                                    c(
                                      D,
                                      {
                                        size: "small",
                                        plain: "",
                                        style: { width: "5%", height: "4%" },
                                        type: "danger",
                                        onClick: (T) => Q(l.row.host, !0),
                                      },
                                      {
                                        default: w(
                                          () => s[27] || (s[27] = [A("－ ")]),
                                        ),
                                        _: 2,
                                      },
                                      1032,
                                      ["onClick"],
                                    ),
                                  ]),
                                  _: 1,
                                },
                              ),
                            ]),
                            _: 1,
                          },
                          8,
                          ["data"],
                        ),
                        c(Ue, {
                          direction: "vertical",
                          style: {
                            height: "90%",
                            position: "relative",
                            top: "10%",
                          },
                        }),
                        c(
                          Y,
                          {
                            data: v.black,
                            style: { width: "49%" },
                            size: "small",
                            height: "200",
                            "row-class-name": "blackhost",
                          },
                          {
                            default: w(() => [
                              c(M, { prop: "index", label: "#", width: "15" }),
                              c(
                                M,
                                { prop: "host", label: d(e)("ym") },
                                null,
                                8,
                                ["label"],
                              ),
                              c(
                                M,
                                { prop: "", label: " ", width: "45" },
                                {
                                  default: w((l) => [
                                    c(
                                      D,
                                      {
                                        size: "small",
                                        plain: "",
                                        style: { width: "5%", height: "4%" },
                                        type: "danger",
                                        onClick: (T) => Q(l.row.host, !1),
                                      },
                                      {
                                        default: w(
                                          () => s[28] || (s[28] = [A("－ ")]),
                                        ),
                                        _: 2,
                                      },
                                      1032,
                                      ["onClick"],
                                    ),
                                  ]),
                                  _: 1,
                                },
                              ),
                            ]),
                            _: 1,
                          },
                          8,
                          ["data"],
                        ),
                      ]),
                    ]),
                  ]),
                ]),
                _: 1,
              },
            ),
          ])),
          [[ke, t.loading]],
        );
      };
    },
  },
  is = se(ls, [["__scopeId", "data-v-07bb7045"]]);
export { is as default };

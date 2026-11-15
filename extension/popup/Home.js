import {
  _ as Y,
  r as u,
  o as d,
  c as S,
  w as i,
  a as z,
  t as c,
  b as s,
  n as se,
  d as t,
  e as j,
  f as de,
  g as f,
  h as U,
  u as o,
  F as Q,
  L as x,
  i as ge,
  j as ue,
  k as we,
  l as q,
  m as fe,
  p as H,
  q as F,
  s as le,
  E as ne,
  v as K,
  x as pe,
  y as ye,
  z as ve,
  A as be,
  B as ze,
  C as ie,
} from "./popup.js";
import {
  b as Ee,
  f as Ce,
  g as Re,
  r as xe,
  s as ke,
  d as Be,
  a as Ie,
  c as Ne,
  e as De,
  l as Ue,
  h as Se,
  i as ae,
  j as Ve,
} from "./logo.js";
import { c as _e } from "./util.js";
const Pe = {
    name: "Tag",
    props: ["name", "value"],
    data() {
      return { clazz: this.name };
    },
    mounted() {
      this.value || (this.clazz = "gray");
    },
    methods: {},
  },
  Te = { style: { width: "48px", "text-align": "center" } },
  Me = { style: { width: "48px", "text-align": "center" } };
function Ke(I, B, l, M, n, N) {
  const m = u("el-tag"),
    w = u("el-tooltip");
  return n.clazz !== "gray"
    ? (d(),
      S(
        w,
        { key: 0, effect: "light", trigger: "hover" },
        {
          content: i(() => [z(c(l.value || 0), 1)]),
          default: i(() => [
            s(
              m,
              { class: se(n.clazz), size: "small" },
              { default: i(() => [t("div", Te, c(l.name), 1)]), _: 1 },
              8,
              ["class"],
            ),
          ]),
          _: 1,
        },
      ))
    : (d(),
      S(
        m,
        { key: 1, class: se(n.clazz), size: "small" },
        { default: i(() => [t("div", Me, c(l.name), 1)]), _: 1 },
        8,
        ["class"],
      ));
}
const ce = Y(Pe, [
    ["render", Ke],
    ["__scopeId", "data-v-38046aa5"],
  ]),
  je = {
    class: "row",
    style: { width: "100%", "align-items": "flex-end", "line-height": "16px" },
  },
  Qe = { style: { flex: "1" } },
  Ge = { class: "row", style: { width: "100%", "text-align": "center" } },
  Oe = {
    class: "row-between",
    style: { margin: "auto", width: "100%", "font-size": "0.9rem" },
  },
  Le = { class: "omit" },
  He = {
    class: "omit",
    style: { "max-width": "80vw", "word-wrap": "break-word" },
  },
  Xe = {
    __name: "Field",
    props: {
      name: String,
      value: String,
      content: String,
      nameConfig: Object,
      valueConfig: Object,
    },
    setup(I) {
      let B = I,
        l = j({
          isOrigin: !1,
          nameConfig_: { size: "1rem", width: "40%" },
          valueConfig_: {
            size: "12px",
            width: "60%",
            color: "gray",
            align: "center",
          },
          nameStyle: "",
          valueStyle: "",
          value_: B.value,
        });
      return (
        de(() => {
          let M = { ...l.nameConfig_, ...B.nameConfig },
            n = { ...l.valueConfig_, ...B.valueConfig };
          /\[.*]/g.test(B.value || "") &&
            ((l.value_ = (B.value || "").substring(1, B.value.length - 1)),
            (l.isOrigin = !0),
            (n.color = "#969595"));
          let N = (m, w) => {
            m = m || {};
            let r = "";
            for (let p in m) {
              let V = m[p] || w[p];
              ((w[p] = V),
                p !== "width" &&
                  (p === "size" && (p = "font-size"),
                  p === "align" && (p = "text-align"),
                  (r += p + ":" + V + ";")));
            }
            return r;
          };
          ((l.nameStyle = N(M, l.nameConfig_) + ";font-weight: bold;"),
            (l.valueStyle = N(n, l.valueConfig_)));
        }),
        (M, n) => {
          var m;
          const N = u("el-tooltip");
          return (
            d(),
            f("div", je, [
              t(
                "div",
                { style: U(`width: ${o(l).nameConfig_.width}`) },
                [
                  t(
                    "div",
                    { class: "key omit", style: U(o(l).nameStyle) },
                    c(I.name) + "： ",
                    5,
                  ),
                ],
                4,
              ),
              t("div", Qe, [
                o(l).isOrigin
                  ? (d(),
                    f(
                      "div",
                      {
                        key: 0,
                        style: U([
                          "color: #969595;text-align: center",
                          { "margin-left": "5%" },
                        ]),
                      },
                      [
                        t("div", Ge, [
                          t("div", Oe, [
                            n[0] ||
                              (n[0] = t(
                                "span",
                                { style: { "margin-left": "10%" } },
                                "[",
                                -1,
                              )),
                            t("span", Le, c(o(l).value_), 1),
                            n[1] ||
                              (n[1] = t(
                                "span",
                                { style: { "margin-right": "10%" } },
                                "]",
                                -1,
                              )),
                          ]),
                        ]),
                      ],
                    ))
                  : (d(),
                    f(
                      Q,
                      { key: 1 },
                      [
                        ((m = o(l).value_) == null
                          ? void 0
                          : m.toString().length) < 12
                          ? (d(),
                            f(
                              "div",
                              {
                                key: 0,
                                style: U(o(l).valueStyle),
                                class: "value omit",
                              },
                              c(o(l).value_),
                              5,
                            ))
                          : (d(),
                            S(
                              N,
                              { key: 1, placement: "bottom", effect: "light" },
                              {
                                content: i(() => [
                                  t("div", He, c(I.content), 1),
                                ]),
                                default: i(() => [
                                  t(
                                    "div",
                                    {
                                      style: U(o(l).valueStyle),
                                      class: "value omit",
                                    },
                                    c(o(l).value_),
                                    5,
                                  ),
                                ]),
                                _: 1,
                              },
                            )),
                      ],
                      64,
                    )),
              ]),
            ])
          );
        }
      );
    },
  },
  k = Y(Xe, [["__scopeId", "data-v-9f44bb52"]]),
  qe = { class: "cardWrap" },
  Fe = { class: "col", style: { "align-items": "center", height: "100%" } },
  Ye = {
    class: "row",
    style: { width: "100%", height: "20%", "align-items": "center" },
  },
  Ze = { style: { width: "3rem", height: "3rem" }, class: "col" },
  We = ["src"],
  Je = {
    class: "col",
    style: {
      width: "90%",
      "text-align": "left",
      height: "100%",
      "align-self": "center",
    },
  },
  $e = {
    style: {
      color: "#2d2d2d",
      "font-size": "20px",
      "font-weight": "bold",
      margin: "auto 0",
    },
  },
  et = { style: { width: "18%" } },
  tt = { class: "el-dropdown-link" },
  nt = {
    class: "col",
    style: { width: "96%", height: "25%", "justify-content": "space-between" },
  },
  ot = {
    class: "omit",
    style: {
      color: "#9f9801",
      "font-size": "1.1rem",
      "text-align": "left",
      "font-weight": "bold",
    },
  },
  st = {
    class: "col",
    style: { width: "96%", "justify-content": "space-between" },
  },
  lt = { class: "col-between" },
  it = { class: "label" },
  at = { style: { width: "100%" }, class: "row" },
  ct = { style: { width: "100%" }, class: "row" },
  rt = { class: "col-between" },
  dt = { class: "label" },
  gt = { style: { width: "100%" }, class: "col-between" },
  ut = { style: { width: "100%" }, class: "col-between" },
  ht = { class: "col-between" },
  At = { class: "label" },
  mt = { style: { width: "100%" }, class: "row" },
  wt = { key: 0, class: "col-between", style: { width: "96%", height: "15%" } },
  ft = {
    class: "row-between tags",
    style: { "align-items": "center", width: "100%" },
  },
  pt = {
    class: "row-between tags",
    style: { "align-items": "center", width: "100%" },
  },
  yt = {
    key: 1,
    class: "row",
    style: {
      width: "90%",
      height: "15%",
      "align-items": "center",
      "margin-top": "auto",
    },
  },
  vt = { style: { width: "90%" }, class: "row" },
  bt = { class: "row", style: { width: "40%", "align-items": "center" } },
  zt = { class: "key" },
  Et = { class: "value", style: { color: "#f51717" } },
  Ct = { class: "row", style: { width: "40%", "align-items": "center" } },
  Rt = { class: "key" },
  xt = { class: "value", style: { color: "#f51717" } },
  kt = {
    __name: "BrowserCard",
    props: { browser: {} },
    emits: ["onUpdate", "onDelete"],
    setup(I, { expose: B, emit: l }) {
      let { tr: M, ident: n, defined: N } = x.tr();
      N({
        curIP: { zh: "当前IP", en: "Current IP" },
        zs: { zh: "真实", en: "Real" },
        auto: { zh: "自动填充", en: "Auto" },
        ua: { zh: "客户端", en: "UserAgent" },
        time: { zh: "时区", en: "Timezone" },
        lang: { zh: "语言", en: "Language" },
        lng: { zh: "经度", en: "Longitude" },
        lat: { zh: "纬度", en: "Latitude" },
        mem: { zh: "内存", en: "Memory capacity" },
        nop: { zh: "处理器", en: "Processor cores" },
        vendor: { zh: "显卡厂商", en: "Vendor" },
        renderer: { zh: "显卡型号", en: "Renderer" },
        sh: { zh: "高度", en: "Height" },
        sw: { zh: "宽度", en: "Width" },
        r_ua: { zh: "[{zs}+{ua}]", en: "[{zs}]" },
        r_time: { zh: "[{zs}+{time}]", en: "[{zs}]" },
        r_lang: { zh: "[{zs}+{lang}]", en: "[{zs}]" },
        r_mem: { zh: "[{zs}+{mem}]", en: "[{zs}]" },
        r_nop: { zh: "[{zs}+{nop}]", en: "[{zs}]" },
        r_vendor: { zh: "[{zs}+{vendor}]", en: "[{zs}]" },
        r_renderer: { zh: "[{zs}+{renderer}]", en: "[{zs}]" },
        r_sh: { zh: "[{zs}+{sh}]", en: "[{zs}]" },
        r_sw: { zh: "[{zs}+{sw}]", en: "[{zs}]" },
        r_ip: { zh: "[真实IP]", en: "[Real IP]" },
        flNet: { zh: "[跟随网络]", en: "[According Net]" },
        bl: { zh: "变量", en: "Variable" },
        yx: { zh: "原型", en: "Prototype" },
        px: { zh: "像素", en: "px" },
        core: { zh: "核", en: "Core" },
        wlxx: { zh: "网络信息", en: "Network information" },
        sbxx: { zh: "设备信息", en: "Device information" },
        pmxx: { zh: "屏幕信息", en: "Screen information" },
      });
      const m = I,
        w = l,
        r = ge(),
        p = ue(),
        V = Ce,
        _ = we(() => r.state.enableId === m.browser.id),
        v = j({
          userAgent: !1,
          webrtc: !1,
          timezone: !1,
          language: !1,
          memoryCapacity: !1,
          processors: !1,
          webglInfo: {
            VENDOR: "",
            RENDERER: !1,
            VERSION: !1,
            SHADING_LANGUAGE_VERSION: !1,
            UNMASKED_VENDOR_WEBGL: !1,
            UNMASKED_RENDERER_WEBGL: !1,
          },
          screen: { height: !1, width: !1 },
        }),
        P = q("0");
      j([]);
      const e = j({
          isTemp: !1,
          userAgent: "",
          webrtc: "",
          proxy: "",
          timezone: "",
          language: "",
          location: { lat: "", lng: "" },
          factors: {},
          memoryCapacity: -1,
          processors: -1,
          webglInfo: {
            VENDOR: "",
            RENDERER: "",
            VERSION: "",
            SHADING_LANGUAGE_VERSION: "",
            UNMASKED_VENDOR_WEBGL: "",
            UNMASKED_RENDERER_WEBGL: "",
          },
          screen: { height: -1, width: -1 },
        }),
        X = q(Ee);
      function Z() {
        var b, g, a, C;
        let y = _e(m.browser);
        if (
          (Object.keys(y).forEach((A) => {
            e[A] = y[A];
          }),
          e.webrtc
            ? E(e.webrtc) && ((e.webrtc = n("flNet")), (v.webrtc = !0))
            : ((e.webrtc = n("zs")), (v.webrtc = !0)),
          E(e.userAgent)
            ? (e.userAgent = n("auto"))
            : e.userAgent
              ? (X.value = Re(
                  (
                    ((b = y.uaInfo) == null ? void 0 : b.product) ||
                    ((g = y.uaInfo) == null ? void 0 : g.browser) ||
                    {}
                  ).name,
                ))
              : ((e.userAgent = n("zs")), (v.userAgent = !0)),
          !e.language)
        )
          ((e.language = n("zs")),
            (e.languageCode = n("zs")),
            (v.language = !0));
        else if (E(e.language))
          ((e.language = n("flNet")), (e.languageCode = n("flNet")));
        else {
          let A = (le.languages || []).find((D) => D.code == e.language) || {
            name: "unknown",
            code: e.language,
          };
          ((e.language = `${A.name} ${A.code}`), (e.languageCode = A.code));
        }
        if (!e.timezone)
          ((e.timezone = n("zs")),
            (e.timezoneCode = n("zs")),
            (v.timezone = !0));
        else if (E(e.timezone))
          ((e.timezone = n("flNet")), (e.timezoneCode = n("flNet")));
        else {
          let A = (le.regions || []).find((D) => D.timezone == e.timezone) || {
            region_zh: "未知",
            city_zh: "未知",
            region_en: "Unknown",
            city_en: "Unknown",
            timezone: y.timezone,
            utc_offset: "--",
          };
          (x.is("zh")
            ? (e.timezone = `${A.region_zh}_${A.city_zh} ${A.timezone} ${A.utc_offset}`)
            : (e.timezone = `${A.region_en}_${A.city_en} ${A.timezone} ${A.utc_offset}`),
            (e.timezoneCode = `${A.timezone}`));
        }
        (e.location &&
          (e.location.lat
            ? E(e.location.lat) && (e.location.lat = n("flNet"))
            : (e.location.lat = n("zs")),
          e.location.lng
            ? E(e.location.lng) && (e.location.lng = n("flNet"))
            : (e.location.lng = n("zs"))),
          E(e.memoryCapacity)
            ? (e.memoryCapacity = n("auto"))
            : e.memoryCapacity
              ? (e.memoryCapacity += "GB")
              : ((e.memoryCapacity = n("zs")), (v.memoryCapacity = !0)),
          E(e.processors)
            ? (e.processors = n("auto"))
            : e.processors
              ? (e.processors += n("core"))
              : ((e.processors = n("zs")), (v.processors = !0)),
          e.webglInfo.UNMASKED_VENDOR_WEBGL
            ? E(e.webglInfo.UNMASKED_VENDOR_WEBGL) &&
              (e.webglInfo.UNMASKED_VENDOR_WEBGL = n("auto"))
            : ((e.webglInfo.UNMASKED_VENDOR_WEBGL = n("zs")),
              (v.webglInfo.UNMASKED_VENDOR_WEBGL = !0)),
          e.webglInfo.UNMASKED_RENDERER_WEBGL
            ? E(e.webglInfo.UNMASKED_RENDERER_WEBGL) &&
              (e.webglInfo.UNMASKED_RENDERER_WEBGL = n("auto"))
            : ((e.webglInfo.UNMASKED_RENDERER_WEBGL = n("zs")),
              (v.webglInfo.UNMASKED_RENDERER_WEBGL = !0)),
          e.screen.height
            ? E(e.screen.height)
              ? (e.screen.height = n("auto"))
              : (e.screen.height += n("px"))
            : ((e.screen.height = n("zs")), (v.screen.height = !0)),
          e.screen.width
            ? E(e.screen.width)
              ? (e.screen.width = n("auto"))
              : (e.screen.width += n("px"))
            : ((e.screen.width = n("zs")), (v.screen.width = !0)),
          (e.vars = ((a = e.customVars) == null ? void 0 : a.length) || 0),
          (e.protos = ((C = e.customProtos) == null ? void 0 : C.length) || 0));
      }
      const T = async () => {
        let y = ne.service({});
        return (
          _.value === !0
            ? (await xe(), r.commit("enable", void 0), y.close())
            : (await ke(m.browser.id),
              r.commit("enable", m.browser.id),
              y.close()),
          !1
        );
      };
      function W() {
        Be(m.browser.id).then(() => {
          (K.success(n("sccg")), w("onDelete"));
        });
      }
      function J() {
        (r.commit("build", m.browser), p.push({ name: "Build" }));
      }
      function E(y) {
        return y === "auto" || y === "dynamic" || y === "-1" || y === -1;
      }
      return (
        B({ isEnabled: _, enabled: T }),
        Z(),
        (y, b) => {
          const g = u("el-tooltip"),
            a = u("el-tag"),
            C = u("el-icon"),
            A = u("el-button"),
            D = u("el-dropdown-item"),
            $ = u("el-dropdown-menu"),
            G = u("el-dropdown"),
            ee = u("el-scrollbar"),
            O = u("el-collapse-item"),
            te = u("el-collapse"),
            L = u("el-switch");
          return (
            d(),
            f(
              "div",
              {
                class: "col",
                style: U(`height: ${P.value !== "1" ? "150px" : "180px"}`),
              },
              [
                t("div", qe, [
                  t("div", Fe, [
                    t("div", Ye, [
                      t("div", Ze, [
                        s(
                          g,
                          {
                            content: e.userAgent,
                            placement: "bottom",
                            effect: "light",
                          },
                          {
                            default: i(() => [
                              t(
                                "img",
                                {
                                  src: X.value,
                                  alt: "logo",
                                  style: { margin: "auto", width: "2.8rem" },
                                },
                                null,
                                8,
                                We,
                              ),
                            ]),
                            _: 1,
                          },
                          8,
                          ["content"],
                        ),
                      ]),
                      t("div", Je, [t("div", $e, c(e.name), 1)]),
                      t("div", et, [
                        I.browser.isTemp
                          ? (d(),
                            S(
                              a,
                              { key: 0, type: "danger", size: "small" },
                              {
                                default: i(
                                  () => b[3] || (b[3] = [z("Temporary")]),
                                ),
                                _: 1,
                              },
                            ))
                          : (d(),
                            S(
                              G,
                              { key: 1 },
                              {
                                dropdown: i(() => [
                                  s($, null, {
                                    default: i(() => [
                                      s(
                                        D,
                                        { icon: "Setting" },
                                        {
                                          default: i(() => [
                                            s(
                                              A,
                                              {
                                                type: "primary",
                                                text: "",
                                                style: { width: "50%" },
                                                onClick: J,
                                              },
                                              {
                                                default: i(() => [
                                                  z(c(o(n)("xg")), 1),
                                                ]),
                                                _: 1,
                                              },
                                            ),
                                          ]),
                                          _: 1,
                                        },
                                      ),
                                      s(
                                        D,
                                        { icon: "DeleteFilled" },
                                        {
                                          default: i(() => [
                                            s(
                                              A,
                                              {
                                                type: "danger",
                                                text: "",
                                                style: { width: "50%" },
                                                onClick:
                                                  b[0] ||
                                                  (b[0] = (R) => W(I.browser)),
                                              },
                                              {
                                                default: i(() => [
                                                  z(c(o(n)("sc")), 1),
                                                ]),
                                                _: 1,
                                              },
                                            ),
                                          ]),
                                          _: 1,
                                        },
                                      ),
                                    ]),
                                    _: 1,
                                  }),
                                ]),
                                default: i(() => [
                                  t("span", tt, [
                                    s(
                                      C,
                                      { style: { "font-size": "30px" } },
                                      { default: i(() => [s(o(fe))]), _: 1 },
                                    ),
                                  ]),
                                ]),
                                _: 1,
                              },
                            )),
                      ]),
                    ]),
                    t("div", nt, [
                      s(
                        te,
                        {
                          accordion: "",
                          style: {
                            width: "100%",
                            "border-top": "unset",
                            margin: "auto",
                          },
                          modelValue: P.value,
                          "onUpdate:modelValue":
                            b[1] || (b[1] = (R) => (P.value = R)),
                        },
                        {
                          default: i(() => [
                            s(
                              O,
                              { name: "1" },
                              {
                                title: i(() => [
                                  t("div", ot, c(e.userAgent), 1),
                                ]),
                                default: i(() => [
                                  s(
                                    ee,
                                    { always: "", height: "120px" },
                                    {
                                      default: i(() => [
                                        t("div", st, [
                                          t("div", lt, [
                                            t("div", it, c(o(n)("wlxx")), 1),
                                            s(
                                              k,
                                              {
                                                style: { width: "50%" },
                                                name: "IPv4",
                                                value: e.webrtc,
                                                content: e.webrtc,
                                                "value-config": {
                                                  color: "#9f9801",
                                                  align: "left",
                                                },
                                              },
                                              null,
                                              8,
                                              ["value", "content"],
                                            ),
                                            t("div", at, [
                                              s(
                                                k,
                                                {
                                                  style: { width: "50%" },
                                                  name: o(n)("time"),
                                                  value: e.timezoneCode,
                                                  content: e.timezone,
                                                  "value-config": {
                                                    color: "#c59e03",
                                                  },
                                                },
                                                null,
                                                8,
                                                ["name", "value", "content"],
                                              ),
                                              s(
                                                k,
                                                {
                                                  style: { width: "50%" },
                                                  name: o(n)("lang"),
                                                  value: e.languageCode,
                                                  content: e.language,
                                                  "value-config": {
                                                    color: "#c59e03",
                                                  },
                                                },
                                                null,
                                                8,
                                                ["name", "value", "content"],
                                              ),
                                            ]),
                                            t("div", ct, [
                                              s(
                                                k,
                                                {
                                                  style: { width: "50%" },
                                                  name: o(n)("lat"),
                                                  value: e.location.lat,
                                                  content: e.location.lat,
                                                  "value-config": {
                                                    color: "#c59e03",
                                                  },
                                                },
                                                null,
                                                8,
                                                ["name", "value", "content"],
                                              ),
                                              s(
                                                k,
                                                {
                                                  style: { width: "50%" },
                                                  name: o(n)("lng"),
                                                  value: e.location.lng,
                                                  content: e.location.lng,
                                                  "value-config": {
                                                    color: "#c59e03",
                                                  },
                                                },
                                                null,
                                                8,
                                                ["name", "value", "content"],
                                              ),
                                            ]),
                                          ]),
                                          t("div", rt, [
                                            t("div", dt, c(o(n)("sbxx")), 1),
                                            t("div", gt, [
                                              s(
                                                k,
                                                {
                                                  style: { width: "50%" },
                                                  name: o(n)("mem"),
                                                  value: e.memoryCapacity,
                                                  content: e.memoryCapacity,
                                                  "name-config": {
                                                    width: o(x).is("zh")
                                                      ? "50%"
                                                      : "70%",
                                                  },
                                                  "value-config": {
                                                    color: "#03bfc5",
                                                  },
                                                },
                                                null,
                                                8,
                                                [
                                                  "name",
                                                  "value",
                                                  "content",
                                                  "name-config",
                                                ],
                                              ),
                                              s(
                                                k,
                                                {
                                                  style: { width: "50%" },
                                                  name: o(n)("nop"),
                                                  value: e.processors,
                                                  content: e.processors,
                                                  "name-config": {
                                                    width: o(x).is("zh")
                                                      ? "50%"
                                                      : "70%",
                                                  },
                                                  "value-config": {
                                                    color: "#03bfc5",
                                                  },
                                                },
                                                null,
                                                8,
                                                [
                                                  "name",
                                                  "value",
                                                  "content",
                                                  "name-config",
                                                ],
                                              ),
                                            ]),
                                            t("div", ut, [
                                              s(
                                                k,
                                                {
                                                  style: { width: "50%" },
                                                  name: o(n)("vendor"),
                                                  value:
                                                    e.webglInfo
                                                      .UNMASKED_VENDOR_WEBGL,
                                                  content:
                                                    e.webglInfo
                                                      .UNMASKED_VENDOR_WEBGL,
                                                  "name-config": {
                                                    width: o(x).is("zh")
                                                      ? "50%"
                                                      : "40%",
                                                  },
                                                  "value-config": {
                                                    color: "#34c503",
                                                    align: "left",
                                                  },
                                                },
                                                null,
                                                8,
                                                [
                                                  "name",
                                                  "value",
                                                  "content",
                                                  "name-config",
                                                ],
                                              ),
                                              s(
                                                k,
                                                {
                                                  style: { width: "50%" },
                                                  name: o(n)("renderer"),
                                                  value:
                                                    e.webglInfo
                                                      .UNMASKED_RENDERER_WEBGL,
                                                  content:
                                                    e.webglInfo
                                                      .UNMASKED_RENDERER_WEBGL,
                                                  "name-config": {
                                                    width: o(x).is("zh")
                                                      ? "50%"
                                                      : "40%",
                                                  },
                                                  "value-config": {
                                                    color: "#34c503",
                                                    align: "left",
                                                  },
                                                },
                                                null,
                                                8,
                                                [
                                                  "name",
                                                  "value",
                                                  "content",
                                                  "name-config",
                                                ],
                                              ),
                                            ]),
                                          ]),
                                          t("div", ht, [
                                            t("div", At, c(o(n)("pmxx")), 1),
                                            t("div", mt, [
                                              s(
                                                k,
                                                {
                                                  style: { width: "50%" },
                                                  name: o(n)("sh"),
                                                  value: e.screen.height,
                                                  content: e.screen.height,
                                                  "name-config": {
                                                    width: "30%",
                                                  },
                                                  "value-config": {
                                                    color: "#035ec5",
                                                  },
                                                },
                                                null,
                                                8,
                                                ["name", "value", "content"],
                                              ),
                                              s(
                                                k,
                                                {
                                                  style: { width: "50%" },
                                                  name: o(n)("sw"),
                                                  value: e.screen.width,
                                                  content: e.screen.width,
                                                  "name-config": {
                                                    width: "30%",
                                                  },
                                                  "value-config": {
                                                    color: "#035ec5",
                                                  },
                                                },
                                                null,
                                                8,
                                                ["name", "value", "content"],
                                              ),
                                            ]),
                                          ]),
                                        ]),
                                        b[4] ||
                                          (b[4] = t(
                                            "div",
                                            { style: { height: "20px" } },
                                            null,
                                            -1,
                                          )),
                                      ]),
                                      _: 1,
                                    },
                                  ),
                                ]),
                                _: 1,
                              },
                            ),
                          ]),
                          _: 1,
                        },
                        8,
                        ["modelValue"],
                      ),
                    ]),
                    P.value !== "1"
                      ? (d(),
                        f("div", wt, [
                          t("div", ft, [
                            (d(!0),
                            f(
                              Q,
                              null,
                              H(
                                o(V)[0],
                                (R) => (
                                  d(),
                                  S(
                                    ce,
                                    { name: R, value: e.factors[R] },
                                    null,
                                    8,
                                    ["name", "value"],
                                  )
                                ),
                              ),
                              256,
                            )),
                          ]),
                          t("div", pt, [
                            (d(!0),
                            f(
                              Q,
                              null,
                              H(
                                o(V)[1],
                                (R) => (
                                  d(),
                                  S(
                                    ce,
                                    { name: R, value: e.factors[R] },
                                    null,
                                    8,
                                    ["name", "value"],
                                  )
                                ),
                              ),
                              256,
                            )),
                          ]),
                        ]))
                      : F("", !0),
                    P.value !== "1"
                      ? (d(),
                        f("div", yt, [
                          t("div", vt, [
                            t("div", bt, [
                              t("div", zt, c(o(n)("yx")) + ":", 1),
                              t("div", Et, c(e.protos), 1),
                            ]),
                            t("div", Ct, [
                              t("div", Rt, c(o(n)("bl")) + ":", 1),
                              t("div", xt, c(e.vars), 1),
                            ]),
                          ]),
                          s(
                            L,
                            {
                              modelValue: _.value,
                              "onUpdate:modelValue":
                                b[2] || (b[2] = (R) => (_.value = R)),
                              "inline-prompt": "",
                              "before-change": T,
                              "active-text": o(n)("qy"),
                              "inactive-text": o(n)("gb"),
                              "active-value": !0,
                              "inactive-value": !1,
                              size: "large",
                              style: {
                                float: "right",
                                "margin-right": "5px",
                                "align-items": "center",
                              },
                            },
                            null,
                            8,
                            ["modelValue", "active-text", "inactive-text"],
                          ),
                        ]))
                      : F("", !0),
                  ]),
                ]),
              ],
              4,
            )
          );
        }
      );
    },
  },
  Bt = Y(kt, [["__scopeId", "data-v-0073752d"]]),
  It = "/assets/emoji.gif",
  Nt = "/assets/siteicon/browserscan.ico",
  Dt = "/assets/siteicon/coveryourtracks.ico",
  Ut = "/assets/siteicon/creepjs.ico",
  St =
    "data:image/png;base64,iVBORw0KGgoAAAANSUhEUgAAABAAAAAQCAQAAAC1+jfqAAAABGdBTUEAALGPC/xhBQAAACBjSFJNAAB6JgAAgIQAAPoAAACA6AAAdTAAAOpgAAA6mAAAF3CculE8AAAAAmJLR0QA/4ePzL8AAAAHdElNRQfkAwgXOwtxHCJOAAABc0lEQVQoz2XRsUuUcQCH8c97d/b+DitNVM5AhQsxgiyChha5jNbWHEsI/wENbGxpCZokcAgKIhqCxoa0koYEI8rIQDLroA4U40L0Xsr7NegW3/2B5/kKwnCYDTshprGwvzSGGHbCXKgESRg2Y5Cmbif1S6xZsgGsGBeeh5jGtngtLsTfsRF/xc34Oo7sUWJ4kYRtxdSEs7rs+KnVX8vuqCOSFRSbRnVZ9UbZoqPyFtQcN+iZJC1EJec9cNmSIzZ0WnBFt3PK3lqX2zXkhy1b1qU+OqHmrjZ1ZUN2Fej1XUOLr9q1eGTKsrxjWvUhBzlVhx3w1IQ1N31Ws60IClRdtOmdMbcwqS7TrkfmG3J5H5QMuKfTlEWTHnplVdEnS/KSEJuuOu2GgjFn1P1xyJayJ+7L2Q91XYdpKzr0SR10ybrbMrIkzBqJglEX1Kxp6tdjzmMNCfNJqJgxQFPJKb2oeq+2p/fFuCBUwsuQ/Xd3FubDSPAPg6OPE6f+6qwAAAAldEVYdGRhdGU6Y3JlYXRlADIwMjAtMDMtMDhUMjM6NTk6MTErMDE6MDDfXjywAAAAJXRFWHRkYXRlOm1vZGlmeQAyMDIwLTAzLTA4VDIzOjU5OjExKzAxOjAwrgOEDAAAAFd6VFh0UmF3IHByb2ZpbGUgdHlwZSBpcHRjAAB4nOPyDAhxVigoyk/LzEnlUgADIwsuYwsTIxNLkxQDEyBEgDTDZAMjs1Qgy9jUyMTMxBzEB8uASKBKLgDqFxF08kI1lQAAAABJRU5ErkJggg==",
  Vt = "/assets/siteicon/amiunique.ico",
  _t =
    "data:image/png;base64,iVBORw0KGgoAAAANSUhEUgAAACAAAAAgCAYAAABzenr0AAAFi0lEQVR4Xq1XTWwbRRj9ELQqUAnUqoCCRJHgxI0LQj0gQQ6cUdtjDwiJYxXOCAQNPcIBDpw4UVUCLqhUkVpRVUpjx7vrXe/G9tretePYTpvU+XVc2/n/+N7Yjr3r4KbQJz1ldr43783OzE4SoqMiWnmerEejpNeukLZ2g6aXLYqteIpoow81aKB9aphaGiG7/i2ZG3ma2WTKMlN6h8lpMSUabaKNPtSgidcKwnGZyOthuyeDuTZGidoSeWJs15m0JaZYdTihgRZjMNZY+SJs+3hM+mfIXJ0gb19M1pmmF8UcfHhELrbHYKwvHub6hPI8EiKzZ8ladcnb7YQu/H/CC57wHgpt/jTFq1nKyj7G7styPng6hFcOZ0O8/5aMflSr1ZMcjx+7mK6ePKbP3z7mb8nyVWTgvBCTGELolPaQWpiig/dxo3ILWchENjUajZ/29/aKrd3dsmwX51vb/MviBlO0JANh/i+MlvnD9KIi2gP1fkZK/Hlhmf3mNiMDWchsNps/YgXe3d/f3+UQzqVl76bnmPTy4ZRaqrGpOFSnlfiUUeaV7WAEMpGttiGxWr8eqArurjfFeFZMSoOMzvIF7+GB9ry00Teg62h/Xqz1ObeRWK1dbx+Cu8UTL8V878HOXljDH2fkAEULYlTsUSvycW2WvebWgc6XNvpQC2tHzCLbtUecqTcPOLXR5Jc130M2kV4YJWeBP80v9EW3YdTlhovlxUzMjQ6jPo/NVcNS1YfagQ7UZPJRj2kqy3Qv02YkJ/3imZZVM+c+IjLyVym/JsudY0NmGsYFX1ZhWkwMrESeT5kFXt7eCctUH2rQKK3m8zvJEn99f5W/7PCr+2vit6BqKlOfHccEblIO12iOP0gWw77sNlr8nCaz1mVQNMs/PFgJSw7wvdSgIcNXfpFaIyzhTzJlVVOZuv+XTMBLkCvfaVzeMury79XV8Bj+bFZmHXH5bafAm3uDZ6WLLalBA+15fz5cVt7IUFnINLIJmUDOp5TMKo63zPKbVo6bu8FPZm5zi5+NpfnXpfVA/2G4JppnplOca24G+uXbV97IUFkqM+vJBDIyAfmO4xl0yH4n+bvy4IH8Y3mNm6G3n6k/UuxHSzS/iTYMeMJbZSCrnSkTiLuyBXjA0rR5Uk9xpRV8g8PwXtJTfBzmxQue/RmUQWYaW5C+Sb5cl2a6R83hS/5c2CeAP5dkP2O2ItrDcMnHneAEM1RmSg6hmbxKRTkQZjJILcGx2kbYSwH7+ZYtJsaMItroOwzwgNeA/ywO/ox8hpYzSlm5NCwUZnrUbfl9kMWlHfbk8bL8poxZPa200XcYzqWyyivgjSxkmo5cREW5DuOJCrlyMi0nSC3O16tLAcO5ZotfiCPc7umkjT7U+oGx8BjwRRYyka1g2eNUwp7IUll9FNM3bIcbO73lvejJJaPDFBPoam3Vh1oXjZ0dNRYeAU9kIMuyr3T+JBHEYq+SbdcoLftqYUAfNY2/KZeV6Z01uT41fVDTp4UGwBg8D2iQYdvr5Div9CYAWPHLVJFPI2EGacX5RaFbr/P7rnxKpj6o6VJq0ECLMRg7oCnPof9yMLwLS79NJRxIQ8Rdwsjg19RPhMO0vx7W6h1t57lbw3MZB8+4FY7tQdNOU9LMUlHuakuWL4HADsPPwxjW4hmeKTujMoYicucspRMulWWArT0dVuTguuJpRR7zZ3kXkxNnKGVOUEUmkZGDk5gWIzB2RE63x2AsPFzxmpw84j8m/UgZY5Qxq1SRyyQrZg4mER1OaKDFGNdckhcZC9s+GaZuj5CrXxHmKS/fcFk+o6LcZr60c2abaKMPtbzcCWmtIPpxsqZGwnb/HcW7Jyinj5IbG6dk9AbNTFnkRDxy7nmqjb6sTBSaaPTI/57/AypU4hZC0m88AAAAAElFTkSuQmCC",
  Pt =
    "data:image/png;base64,iVBORw0KGgoAAAANSUhEUgAAACAAAAAgCAYAAABzenr0AAACVklEQVR4Xu2Ua1EsMRSEjwQkRAISIgEJkYCEOEDCSkDCSEDCSLgSLvOR7ppMYNksu/yjq7qS835kdiP+cD3SxrzxeWMVuaPD9it43HjauG78f4H44EvMzSDJEnvyt40vG5+iTdwTHTZ87E/sTY2wWhIxUTqaPhJncSzijRFLjrsgx+cJR3pD+N4NrHaNYxGmqwPR9X7cif0xHja+Rkv2L9pkqXc4gxTNlxhiyUGuq8AbetV9AvQkX2Tria7G3mQ/ALnG7+QsCPQqi3QpjkWxuyDk7onhSTGgSId9ahOevEjmdHISfzdJjr1RYvwNFOnIfRE4Vt2LZJJl6ZgCPc1QDHJH5wkp7KbRA54OeRo59q6d+DmOq8bmjblRfACbQobfbe0s1jgGMyVF0Jc4vqe3gg0ffEGWvEieRonjUzDVuA3WjB26SWzeiDfhxrPkKbxGCyIhZBNMZ7lfu8mUtuNLDPckO41Mg2CvrURLwAnQI9doyaGndJEimRPQME1NwwWAk381DTIE49Z6vxfJ0+gbWCSDrDunbRDUwcYWaaq3TaNvwJOBrHuRTHHW29uqZD7MpDt+NDQNEp10r5L7J2ClAB9k9GCV7F8Aevs4ZgpM5clKtARPkikCQZbNq2ZqJkXXc4nj/8ZF+KNJIncmAVVykewn8tQUwlZFN34VCOqTMoEbogBTQn/xbMuT5o+IOyDHvjbuLgDcIIXdhDcx0s9zM8ZVV8lr7FMn6fGlWc4i281gSopRtEjH2U/6o/e+Bo9x/JsGKfaf2l1Xfg5+8xEp2kZo8g/TeAcRBgqxmcfddgAAAABJRU5ErkJggg==",
  Tt =
    "data:image/png;base64,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",
  Mt =
    "data:image/png;base64,iVBORw0KGgoAAAANSUhEUgAAACAAAAAgCAYAAABzenr0AAADuUlEQVR4Xu2UTWwbRRTHXegBhECCYIioYq/jTwKJABUhPqLEH6ljQ+RaapAACaGinssF0Vzatb27duIqpVSUtBWtWqoebDdxahIkAkSQCg4IJDhxQaIgKKVqHHv2a3bXfsxsimg2TQ0UTvgv/TQr7cx7b97/7dpsbbXVVls3ITixndHfSw4bJ5MJOPnck3Dilduse/4T4YNbE1quZ0HhPDJM+AH2B0DPegELnu+VfI+Ajsfs1jP/ihYX2c2I7zkC426AHAM47QSJdZjIKQc0eScpphtw1n9BOfh00Hr+plXn/Kdg0g1KygkiuzGQJcXlfAgf7u+zxvjHQtneUdjvJjddn9AKokWQDknCg18BO7jZGquloFC4VR7vfcPIepdUwXMe5XpTIuf7tsGvT7YR1BaYcAOefCxpjX9DAcAmxHlPwwG32UoQGDOQxrkA7Vuf6EbAhAvkbO/pnwqjt8PSnrutua4rZaKvnw5ZK5//CpAlBeQfeV2bDn/crMSWjUp8wZjf8ZI15xrVU+4k5LvXBfu7ULsQ6/7ZmB5J6DMRgNkQwPwQwIfDoJdj5y7P7rxzTeIr3ONdWv6h1yQhcB5nmDXBqJ/iPkoXSH9AP72rKCmKE9T0KqZteTc5631RnR+JwSfDYJSCoJcGzRU+igIuDr0PwN5iJkfjfTuMnPs3enN6eI3XKVJMxtOUOB9IfIBONiEAIu9viLxPFzkvJu8VMd0tSmlXTc24qnrO+52U7dlJY8PiaKdRjn3RqGyT8NkIVothXSuSIhaGQSsnXrZJ4w9vVfnuBk1sbaOaIbfO+C9Ib4XflQ5HjylHRqbko9sPyUeTeWlqJKW8E98jT8V3S29Hd8kHBl7Ak08l4M0nniGTvOna7tY+Z++BL3e/2vh0V0abe/64Wgxi+CAC6uyzSzZS+Tk6rfTbtRZAQSzTRCkGiyyjkD0ysUJEex2IUCfU0N6uFUKVvFsm+68YAnNJ4v3HSF6zCL0SZ41K9AetFKqqpbBMO4BpB2aCgKejyzaJ9yEQnKAR3zdCzzhB55xgXKXBMavwDPn9/onpfc4FTcEFl1n/A9pM5BB8FgeYo0MYBiiHVpkhzxXSgVJEoR34EacdK+RmLUEtEFlHVeeYao11fS2eGRjC0yGknw3V1UKwhouDKxS1MGA+Qzm4opbj39guCo/a0Vinvc767r045jHXVvxyHepCoKM+tqUDhC0dv+b77gD685mLd146E74fndp2HyrE7BQw1347zMcod107K2211db/U78D26WFFOSh6DgAAAAASUVORK5CYII=",
  Kt =
    "data:image/png;base64,iVBORw0KGgoAAAANSUhEUgAAACAAAAAgCAYAAABzenr0AAAIuUlEQVR4XqWXWVCU2RXHz5jZsi+VeUpVHlKVTFUepvKWxyQvmadUMpVKqvIwj0lNqrKLqIDsiyI7DgKKzNhsgkA33dAN9LfQQLO1QAOOZlxGg1HjUoqOONDLze9+SLsGncqpOnWXvt/Zz//eFtmKIpFXZMH8hyxahbJkpjNmsm6TqN0ti/YxWbI6HdZzZ89olwV7j3N2wSrg/HY5M/Dak2JfnLQB83aezJl1cLHMW2UyaxuMYxK1bJmzRxzWc703b5kyb1Y4Z6P2+8zz5RPr9SfFfj6KEIEIAiPWTongXQSPT9gemcHjiNXjsJ7rvYjRJSesHOfsjF3EuP1Jcc8ny3rZ8dxh75dkxsxAWLlMm9kyQyqmUTRtBZj7ZMrud1jP9d6U1ce5YufsNNGaImWW9ZWH8uAtSR8YJ9wTCJq0P5Rxq4X1qEzYUxujOSFh61MJ22uc+cwZH51PmKvO2bAVcsYJ0hKmZrSssOWWCeMQBm2RkgEKZtTwyqi5KKPkNYTSUTsqIWsJnnfGUUs9xWO2EntYSTDA2j4Jzzlnx4yFDRnI0nJGiZKO6pYUMo+KTWHZRq+MEIkRvB4xZ5iH4WmxzXVGleIQyocGlVim+t7kREKCwRm+H+ec/mbSkaFlhUyLsV2UeulxhZ2dX3Dyrsdw+ItiEvYgBgQJmUE0DJQaxqwYhNSwT8BrEjSTjAkUJWQgkBCfPzl667bS9O6pU2ek2z0uHu8s+zNijfgcWSYGGLSrrolHdcqw9Sc4B/4bvAsOyRDhHsTjIZQPk/MhlA4Mr0nA+Ix5DMZT2DeYkB5fwn3tRtLRDv18KqKkx3u/4NyFtV/OL65yZobz4zJkziFvjDGD8a+M2TjyF5EAfR6wDsog1Rug2v14GiCPfisCYwgh7iW3A6YSL3nuDyadPT3v6FP1y5c3davfRz9S0u5W47fvpPbEPbggPtISoCb8ph4rkV8kAYp9ELASH73qtUoYd4oPq3wUjQ/FXqzuJwLHfOs/Dc+qs/dWlWv5SvIl93BC3Ch3uVXB2YspRVmnzytp9aiZlbupvT8unI5Lpz+CvDHxEgEt02vlMk+Xfhzut3eIuAm7B889gEwffe6m6NxY24vyXnLf7l9bXl1zBCahbR4rLvVdyfei/0wpqj67nJQOfzJy+6Hyd6cWk1LfGeP7eWROidtelD6i24tij5UFl0kf6ZAeerQHCO2hQI5TLD3mXek2YxiwLs3etR+HZmObQo9cvJKUitbYr8PRVM67Ll0j52Zy7tad1N7PjGklDd3J3uX/JLovX1/f1hVcl65gjHOr0k1X9Fod6DPQ2yIo7ZBOvO4C4Trp0+MU13Haqwtu8qneS9dSgqUlkPyhJ5TYXHctX1PfD0yqpZVPN7fUW/0T6pWWQfbupfZebg9SG6RtQ+6Q42g3Rd4FdEsbVnRQ+R3g+TEgtZ2KP8bB5oB6wxNOCak4vaxebTPUKmnY3Gu9cFWdfKAorpLqu55x9QP3GPMNurK+rn7kDTuy0IERpK/DGoCPSxut3gYuSIvhklYWrWy20vcthKmNKNT2xTPnz6WUnbh5R91YT2XjMZq7eSf5pncy8buxpdTe8OWbyPDGpbIXpaG4I7MF51zcHW147gIdW4BpcWGAXrgwwEV+XPY9aTbi0hiITV9fSRmwFTWfvZwsOXlh03GVM3tebWs2Er8Zmo+96Z2Oy+HhuBzFexd3xlGi7MKAowDTh9qAZrtVPqD1jnCtNmPdEaxsJF8f2Orqg+r/PPSL4QX1RktIRW9udMR7kx8rKe8jDaTgCEY0mX70dLMOwR0ih8nDIQriMEXYRBEe0m1G0TQa6uzK6hPi/zedunVP/aR/Tr0diKb2Dp36t3q9DmcaDCWHrU0egt3SBCYc5m3Bjy45SAoaCUsDKWigBhrMhJT6Yn0XbzyiYmvycrZicTm1Th//OCmFnqTUDCWk0Y5LAzVQT3TrSUEDihtJQaNOQR1gUA88HuQ+qAMk6gCLOqC4wDvxTnDpk4cqXpx+1TenJNuTkJrhGIqj8j4QXA+4bYx7pR7EPcjTrQ5AkgM8uWpRfAAoPsAPteSmFiiuMsakyD89d+Pus0v/GfQvauatZtouy6OkhrujFs9rUFoLFNcCxc4IFNcCxQe4D2qtNJFqPK/iAVkNDFda+6WaCFQQgZqRiOT2z367xk5cW3u+DaNXVtTXy8l1FgVXw2VVTb6r4EoiUAmsVxGBKoypBvaruISqrQPozRMps/+OwiIpw6pK7oNyrKzgowrQscqekQzv/e/UhGL2hZupNnuSmuYvJSTXn5Cs/gQy4nwbc7ictivnDVFGkVcQVT1W4nAFl1A5DlfoR2sub7QKHiJ6rBn4mpTSlqWkoZRKLQOY9mNEZv+sZAemf9u9cL7ro6uJc7dW1YWV+8p75rp653hUyU4U5w7GURaT/SgqxeN9hLyUK30fRadllVLopRRfyeg3H9P5FJXwJCsCGYvAhRJas4RI7MWIvOC4bPeOy86BNSkmv3sJbwYQm+ZTUsR6H2Ev5qVUgtISolhMuEt4kpXQWXt5khVT9cUaerei3M5XJR9rC/GikNuqAKv1vIBHagEeFfPILEBZNr29B85nXoQhhY/xkhRyjTvfkMpCnNGy9F4hQJe71aNUG5BrNxGaIckDpfKB5zwikIc3+eQvn9Dmm/f5Lf5s5sGaTwTyAJmNbyYlH9TTsnIBuTyiq0O/Je0f/LLsG/uqM+Za35A9tGa2VSU5VGwOL6dscrrHHIb9zAMO67mzh4c59r4HZyv5LUdyA99KyUxjfOpV/DzKoksy6ZIMKjcT4Mji4ZJJmjKJTgbeadbzjb1OycTgTDoqiyrPBGP+L/oD/5R2481uHpAZgNUunlG7yecuimwX9ZFBfjXrud7byctqFwinz2aALRn8OX1mpb8oaQPS8CIdb3bgVRr/9XaA32l4nU4kduCxZj139qjyNLBEn90B2KTT53/e+u/5fwE34o9IOnLQ+wAAAABJRU5ErkJggg==",
  jt =
    "data:image/png;base64,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",
  Qt =
    "data:image/png;base64,iVBORw0KGgoAAAANSUhEUgAAACAAAAAgCAYAAABzenr0AAALaElEQVR4Xq2WaVRTZxqA6Uxtf8w5bbV2cRuxgrjgiKVVi7buItUudoxkX8geCLLIFpZGEZBNEVBEQaoIWGRHCQWRJQFBECEGAgQChM2EbDeYkAC538Q5AyhHe9qZec65v753eZLz3fe+VlZ/hpaWRX8r7HFYXNTH+qCgP33JnaGqpb8OP/g0e+z68lsKd+sshYNVbsc7C9P+Z94rb1jyaXnbpo9Lu3/8pHAg/JP8/pqP7wxql5VqwYqiCbDq5hj0WYa81j5NfXZbsvLYzmTVpq/OQ0sW1vmvWHG/6cNlvMfOK+4+jVhe2F2xsqC/Z0XuoHbl7aHpVdkjwPrmGLD5RT698aoCckzV9Oy8qKnce0Ed6RyjOvxdnG7pwnp/jJbURSuqy1ZaP+BvX1XxmLj6Xmfa6sJO6eoCKbDOHZyyzh7qXZ09UvlZ5ugt24xnt+yvjVc6pCh7nZI1pv0XIXAkTt2PPPc8nRKuI50MNWxn+6tWOqaCRQvbvB5g9dZyAc/OurqaYP1b0+U1d5/w15b2jNjk9QGbnIFJ26yhVtubI4l2t0axazPlTuuvy522XFZidySrLu5PUD8+Gqs2nIiGACnSMMLkQny/YCglOEhDCPfX2VlxwV8Wtvs3e6q5bzs8Kfzgs9pS2zW1ZYfWNgj8bGqaCteWNY/YlLTP2BVJtXa3pZ0bModzNtwY8dt8fWz3thvDH87mO58HSw7Fab75LkZ16kSUOht/Vt3JCtdrfULVM6FBmpF4P6gozfe5/02G3LmAOrzuLlO7uJoL3p4TeNF8XSNvl21dub9tdUWBnaC5fX1Vu8KuqH1q/R2xfNOvUp59loxjnzFy0CFjyNYxVf3+XPJ/QJxTv4+IgmzQ4coDtDNqzimu7l4oR/Us2k9lSvPTK2776IW59GeFPNpooICi+FrIHFw8l7zpcbHNhvpy//X8mvr1NfUGe34H2FwuNtnndXRuut2TuvGmFLEhf3T1S/1+F/dgzWr/ENXxSD/VlWRveUeOj87E8zGBCpZqspGubGglKwLa8MPr5hI2CCp3bKiqyrGvf6Tb0tAFHMq7wed5XfLPb0puO94cRNgkSj56qf4fwpc7uvSK19jxTI+xnGIPubyGDQGRpwl00bQTHaSRX8WkEae54I0PHuy3r6irdRCIwRfVg+DLnB6wK0Mq3391qOhIopKJjtA4YKJVK10Swbsv9XgtiWzJu7fYkpX3WD0O5fR+RhV9oLCGOiBvpA6BIXc9UDCegx78AH+QOHhwLmlz+cMD9sWNfMeKHrC9ZADsvN47sy9FZjyaPC77Z/y4ABOlvISNUtGw8c+3bOS+edKl0sCiTLZ0Sx5bSi1lSpMr6dK6etqgrIksMba79c6MUuVAS1WCYaxEICdID80lbs1vPbA1t616W654yilTMr7nar/wcPKo+KcEpcI1WgljI1U6YoS2ln4WCnfnqo/6cMfXEbn9H8zmnwvoez/Dc8g2h913JN9DeqaE0VdbTu+FHtAkcBNNqmhzk4i7Cd3CYXz3uArfO6XEimu0BPG8wFdZYuftmeLanTe6oN3p0gbnlKGEHxLkMa6xmjxcpGaAHKEFjHBIz+Zqu3xD1cUBoepADmdsbwRHvyzVa3zZNa/+PVns/oBc9+6iYlZvVwWzR19H7wYPKeLBNnJXXhexK2YI/zRBiWurh3BCCMK21ekxQpc5gd3pvS570qR1+1L7VIdSZEU/XhgjomKV+0hRWldyuPYc44y2hn0aGvQN1Zg4waqZ08Gq9pggZXKS7ygjw1PGuH1yIKnAXdJWzOye5jG7pmroXYONlM7aJ2RRdCep03WY1LFPg3tMnMA1Fz7HPVLpsY8ERmzTkTkBl8tDLs6XZPwjSUOaYwljxegYFYIYpbGmccaXWd7pzWwuhPf5GUoKDta0nQ5SzsQGaqaT/JUDqV5DLZme0pYSz8F+nrtkuoLRMfOAJmoX0ITJLSQRQUwUbu7EtiybJLeuNmEajxswgiIjVqCxPIIZQv28gOUXuxyLH+Mj4p/pMNEKHjkCQjK4zz6ePedyoSXBwRPfnAlQB57zUxYl+qt6rvko9Nm+clDkNQx4Hn36KlZPTy1VVNRAbQ96RG3dLcY3zU1KQKteasJXu5qwVWXTuErdDL7CIlA5L4A5p3DBRozz8REqHeWMmscOg1DcUxNzAi9I5IL3IoN06+ICVM7XvBVRt7xkbYWeMsBjS0Elo7O9ltl5roHefrjZrdmuhVb5yqQEiLKPTDgeEiaUlpkJhTrYrUAAyPnzAtTTkAv1tIZv+XjoToZCPA7nOZrrr1/+Uo1XKGH07itmdd0qo3coq+hCJZ/yJItPbT4ArMBbC2NfALD5y2YIhSiYlFUGk27oYEq6RSBtXsArTO/iFQLxfTkTuiB/He9MwCQ62uPNAnx665YH1LbEOkpr70Nyc28TqSnpCbHeYWHcLC8EACkTBZNTymDaRR3MiBcAWuy8QFiA0SXMX88P89NDZ30NpTF+BkQkG7xx/PLpTVsaKY/imimNQiFJIBQR6+I7iNVvFkCVLgWUK8dhekwJbPmbYTZHANiceYE4X6NLtLdeEONl0F04OXk32dt0Ipbx6h14mVY3/pY2N35cB6la2E24L+zDV8T3oyveLEBK/AjQohAwM7AU9mTrYG9GPfBizgske858e8nD1HiFbZq+5mF6eMPDyEpjQTYv1XiFDnLlVjGh6nwP4TfRAO6uaBhXcmEcW/L5wrhZAC1kLXD3YcJsSgPsi5mG/RCPgN/xo3MBaSzj4QzWdH02G4AsmnHkDt2QXEyDjpTSXr/TSfBlWy2Nz8twJaJRTIFIgbtjEch/rQCg+S4FdPq3Zg9CEux1fBhwvgdw4MGHIGjvt3NBOSzglMOczi1kwhNFNBP4jTbVzqdOXqknKUiPif07GjGSlS20+Z1OQijZNIYviH2GyRWNY3JEGkx2nBp32372HNBSFwF88ApAOrUdUBlEswcmBT55rB34OgM4ZPdzOHRHHvjZcddsvFUeHdgW0M0BZfSZBh550lhPNRtbaCZZI3FU0EbsSxESO4hdbu12s/FjmNyN49jsGDUq8ymEvi7Soa/GTqB+2TR7DvDcdYDghzcT3C/DZAIfZiNksK+LEfbbZYQ5XzSCsH8EgXC7uXpWWUyw+C5l+utysjngPtlQ9IhiFrdTJrVtBBkQ4cXjvbhW3iCuNnAMX7rtGS53zTg6fa8alZ6oRV0V6lEpQgMqKXEaHb93EndqDSCe/NKM8wyE8ax7sBtRAahIAHv+AMGn9olh/y+LAcc+CJzZ+DWI+vv8SvZiQbyLBovv443rqsnTh59QTMEdbuO8DrxUKcEJwTChSTuCKRfJsflXlOhMDwh5yXsCmZA9iUoQTaLiLE90thkd5m3GenuYMawUGE97CkgELaAgAEw7qoLZB8uB764QELDVBYTY2b1oDrhW80vpywDL6ixEKdeLcTJiL7brSj/mcb0cXzcixxYDJSZLCWGuCiyNeUbXmBYTMko2hT4zaHYNbYExp8pgLFNgxlKUAE8AsBtyFKYcrTfT96cCDycS8LLfABBWf13Y77UAx5ZFEsvF68d07pBhG0kKfNl1JTq7X4NON+vRyQYTKkY1hTwLTSHDDNOoQIMZ5QPBGHcVjKUYzFis2Yx3HTATf8oA1ENugO70FWDZr7I0f+Mm9buMWqaYCpt5WItKj4JQlyqNyPOSKVT4BMD8DAA+BFguGwBYdwBjKBMwFtcLYxCVZuIPUcDtkAugOb72Nf7TDB+L/HDCNcXegLx4zISOjJhGhtSYXX00AOsFAI4FzCiiFkZha80YVCTAHf8JkI/YA/y2uc/x/40OBOKdKWTgVjPSx92MZKXDSMp9+AS+yoxCXwdohAfAf78VWGIW5v0e/wK5A6tvJ2U2IAAAAABJRU5ErkJggg==",
  Gt =
    "data:image/png;base64,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",
  Ot = "/assets/fingerprint.png",
  Lt = "/assets/bot.png",
  Ht = { class: "row head", style: { position: "relative" } },
  Xt = { style: { width: "10%", "margin-right": "auto" }, class: "row" },
  qt = ["src"],
  Ft = {
    style: {
      "font-size": "13px",
      color: "#03c6f5",
      cursor: "pointer",
      "font-weight": "bold",
    },
  },
  Yt = {
    class: "row",
    style: { width: "80%", "text-align": "center", margin: "auto" },
  },
  Zt = {
    key: 0,
    style: { width: "80%", margin: "auto", "text-align": "center" },
    class: "col",
  },
  Wt = {
    style: { "font-weight": "bold", "font-size": "16px", color: "#d97523" },
  },
  Jt = { class: "row-between", style: { width: "10%", "margin-left": "auto" } },
  $t = {
    class: "row",
    style: {
      width: "100%",
      "text-align": "center",
      "justify-content": "center",
    },
  },
  en = {
    key: 0,
    class: "col",
    style: { width: "100%", "justify-content": "space-between" },
  },
  tn = { key: 1 },
  nn = { style: { color: "gray" } },
  on = {
    style: {
      height: "6%",
      margin: "auto",
      position: "absolute",
      bottom: "3px",
      width: "100%",
      "align-items": "center",
    },
    class: "col-between",
  },
  sn = {
    class: "row",
    style: {
      "text-align": "center",
      margin: "5px auto",
      "align-items": "center",
    },
  },
  ln = {
    style: { "font-size": "13px", color: "deepskyblue", "margin-left": "3px" },
  },
  an = { class: "row btnArea" },
  cn = { class: "col" },
  rn = ["src"],
  dn = { style: { "margin-left": "auto", "z-index": "2" } },
  gn = { class: "col" },
  un = { class: "row", style: { "font-size": "20px" } },
  hn = { style: { "font-weight": "bold" } },
  An = { class: "row", style: { "font-size": "13px", "font-weight": "bold" } },
  mn = {
    class: "row",
    style: {
      width: "100%",
      "justify-content": "center",
      "text-align": "center",
    },
  },
  wn = {
    class: "row",
    style: { width: "70%", "justify-content": "space-between" },
  },
  fn = { class: "row-between", style: { height: "13.2rem" } },
  pn = { class: "col", style: { width: "50%" } },
  yn = {
    class: "row-between",
    style: { width: "80%", "align-items": "center", "margin-bottom": "1rem" },
  },
  vn = ["src"],
  bn = { style: { width: "72%", "margin-left": "5%" } },
  zn = ["href"],
  En = { class: "col", style: { width: "50%" } },
  Cn = {
    class: "row-between",
    style: { width: "80%", "align-items": "center", "margin-bottom": "1rem" },
  },
  Rn = ["src"],
  xn = { style: { width: "72%", "margin-left": "5%" } },
  kn = ["href"],
  re = 38,
  Bn = {
    __name: "Home",
    setup(I) {
      let { tr: B, ident: l, defined: M } = x.tr();
      M({
        myBC: { zh: "我的浏览器配置", en: "Browser configurations" },
        onNewMachine: {
          zh: "将会清空当前浏览器下的全部使用数据(缓存、cookie、登录信息等),并且随机切换浏览器配置,是否确认？",
          en: "All usage data (cache, cookies, login information, etc.) under the current browser will be cleared, and the browser configuration will be randomly switched. Are you sure?",
        },
        newBr: { zh: "新增配置", en: "New browser" },
        setPl: { zh: "设置策略", en: "Set Policy" },
        newMc: { zh: "一键新机", en: "New machine" },
        reNet: { zh: "更新网络", en: "Refresh net" },
        changeEnv: { zh: "切换环境", en: "Switching env" },
        cfgEmp: {
          zh: "您的浏览器配置为空",
          en: "Your browser configuration is empty",
        },
        clickToAdd: {
          zh: "点击左上角添加浏览器配置吧",
          en: "Click on the top left corner to add browser configuration",
        },
        query: { zh: "回车键查询", en: "Press Enter to query" },
        curIP: { zh: "当前IP", en: "Your IP" },
        yxq: { zh: "有效期", en: "Validity period" },
        yj: { zh: "永久", en: "Permanent" },
        cswz: { zh: "测试网站", en: "Test sites" },
        donate: { zh: "捐赠", en: "Donate" },
      });
      const n = [
          { name: "browserscan", icon: Nt, url: "https://www.browserscan.net" },
          {
            name: "creepjs",
            icon: Ut,
            url: "https://abrahamjuliot.github.io/creepjs/",
          },
          { name: "pixelscan", icon: St, url: "https://pixelscan.net" },
          {
            name: "amiunique",
            icon: Vt,
            url: "https://amiunique.org/fingerprint",
          },
          {
            name: "coveryourtracks",
            icon: Dt,
            url: "https://coveryourtracks.eff.org/",
          },
          { name: "whoer", icon: _t, url: "https://whoer.net/" },
          { name: "browserleaks", icon: Pt, url: "https://browserleaks.com/" },
          { name: "leaksradar", icon: Tt, url: "https://leaksradar.com/" },
        ],
        N = [
          {
            name: "cloudflare",
            icon: Mt,
            url: "https://nopecha.com/captcha/turnstile",
          },
          {
            name: "reCAPTCHA",
            icon: jt,
            url: "https://nopecha.com/captcha/recaptcha",
          },
          {
            name: "hCAPTCHA",
            icon: Kt,
            url: "https://nopecha.com/demo/hcaptcha",
          },
          {
            name: "awsCAPTCHA",
            icon: Qt,
            url: "https://nopecha.com/demo/awscaptcha",
          },
          {
            name: "geetest",
            icon: Gt,
            url: "https://nopecha.com/demo/geetest",
          },
        ],
        m = () => {
          window.open("https://github.com/shawn-li-zh/CthulhuJs", "_blank");
        },
        w = j({ keyword: "", page: 1, size: 5, total: 0 }),
        r = j({
          onMake: !1,
          onFeedback: !1,
          onPay: !1,
          onStrategy: !1,
          onNewMachine: !1,
          loading: !0,
          isSearch: !1,
          drawer: !1,
          ipInfo: { ip: "fail" },
        }),
        p = q([]);
      q();
      const V = ge(),
        _ = ue();
      de(
        async () => (
          Ie().then((g) => {
            r.ipInfo = g;
          }),
          T()
        ),
      );
      function v() {
        return De(w.keyword, w.page, w.size).then((g) => {
          g &&
            ((p.value = []),
            requestAnimationFrame(() => {
              ((p.value = g.data || []), (w.total = g.total));
            }));
        });
      }
      function P() {
        let g = ne.service();
        Se()
          .then((a) => (r.ipInfo = a || {}))
          .finally(() => {
            g.close();
          });
      }
      function e() {
        let g = ne.service();
        ae()
          .then((a) => {
            a ? (K.success(l("qhcg")), T()) : K.warning(l("qhsb"));
          })
          .finally(() => {
            g.close();
          });
      }
      function X(g) {
        (V.commit("build", g), _.push({ name: "Build" }));
      }
      function Z() {
        _.push({ name: "Strategy" });
      }
      function T() {
        return (
          (r.loading = !0),
          Promise.all([v(), Ne().then((g) => V.commit("enable", g))]).then(
            (g) => {
              r.loading = !1;
            },
          )
        );
      }
      function W() {
        window.open("https://www.paypal.com/ncp/payment/DBRXFKAP2AL9C");
      }
      function J() {
        ((r.loading = !0),
          v().then((g) => {
            r.loading = !1;
          }));
      }
      function E() {
        ((r.onNewMachine = !1), K({ type: "info", message: l("qhqx") }));
      }
      function y() {
        ae()
          .then((g) => {
            g ? (b(), K.success(l("qhcg")), T()) : K.warning(l("qhsb"));
          })
          .finally(() => {
            r.onNewMachine = !1;
          });
      }
      function b() {
        (window.localStorage.clear(),
          window.sessionStorage.clear(),
          Ve().then((g) => {
            console.log("browsingData cleared");
          }));
      }
      return (g, a) => {
        var oe;
        const C = u("el-dropdown-item"),
          A = u("el-dropdown-menu"),
          D = u("el-dropdown"),
          $ = u("el-input"),
          G = u("el-button"),
          ee = u("el-empty"),
          O = u("el-scrollbar"),
          te = u("el-pagination"),
          L = u("el-icon"),
          R = u("el-dialog"),
          he = u("el-drawer"),
          Ae = ze("loading");
        return (
          d(),
          f("div", { id: "home", class: "fill", key: o(x).curLang }, [
            t("div", Ht, [
              t("div", Xt, [
                s(
                  D,
                  { trigger: "click" },
                  {
                    dropdown: i(() => [
                      s(A, null, {
                        default: i(() => [
                          s(
                            C,
                            {
                              icon: "Plus",
                              onClick: a[0] || (a[0] = (h) => X({})),
                            },
                            {
                              default: i(() => [z(c(o(l)("newBr")), 1)]),
                              _: 1,
                            },
                          ),
                          s(
                            C,
                            { icon: "Setting", onClick: Z },
                            {
                              default: i(() => [z(c(o(l)("setPl")), 1)]),
                              _: 1,
                            },
                          ),
                          s(
                            C,
                            { icon: "RefreshRight", onClick: e },
                            {
                              default: i(() => [z(c(o(l)("changeEnv")), 1)]),
                              _: 1,
                            },
                          ),
                          s(
                            C,
                            { icon: "RefreshLeft", onClick: P },
                            {
                              default: i(() => [z(c(o(l)("reNet")), 1)]),
                              _: 1,
                            },
                          ),
                          s(
                            C,
                            {
                              icon: "Pointer",
                              onClick:
                                a[1] || (a[1] = (h) => (r.onNewMachine = !0)),
                            },
                            {
                              default: i(() => [z(c(o(l)("newMc")), 1)]),
                              _: 1,
                            },
                          ),
                        ]),
                        _: 1,
                      }),
                    ]),
                    default: i(() => [
                      t("div", null, [
                        t(
                          "img",
                          {
                            src: o(Ue),
                            id: "logo",
                            alt: "logo",
                            style: {
                              width: "40px",
                              height: "40px",
                              "border-radius": "20px",
                            },
                          },
                          null,
                          8,
                          qt,
                        ),
                      ]),
                    ]),
                    _: 1,
                  },
                ),
              ]),
              s(
                D,
                {
                  trigger: "click",
                  style: {
                    "margin-left": "10px",
                    position: "absolute",
                    left: "40px",
                  },
                },
                {
                  dropdown: i(() => [
                    s(A, null, {
                      default: i(() => [
                        s(
                          C,
                          {
                            onClick: a[2] || (a[2] = (h) => o(x).setLang("zh")),
                          },
                          {
                            default: i(() => a[10] || (a[10] = [z("中文")])),
                            _: 1,
                          },
                        ),
                        s(
                          C,
                          {
                            onClick: a[3] || (a[3] = (h) => o(x).setLang("en")),
                          },
                          {
                            default: i(() => a[11] || (a[11] = [z("English")])),
                            _: 1,
                          },
                        ),
                        s(
                          C,
                          {
                            onClick: (h) => o(x).setLang("es"),
                          },
                          {
                            default: i(() => [z("Español")]),
                            _: 1,
                          },
                        ),
                      ]),
                      _: 1,
                    }),
                  ]),
                  default: i(() => [
                    t("div", Ft, c(o(x).is("zh") ? "语言" : o(x).is("es") ? "Idioma" : "Language"), 1),
                  ]),
                  _: 1,
                },
              ),
              t("div", Yt, [
                r.isSearch
                  ? F("", !0)
                  : (d(),
                    f("div", Zt, [
                      t(
                        "div",
                        {
                          style: U(
                            `font-size: 18px;font-weight: bold;${o(x).curLang === "zh" ? "letter-spacing: 5px" : ""}`,
                          ),
                        },
                        [
                          z(c(o(l)("myBC")) + ": ", 1),
                          t("span", Wt, c(w.total), 1),
                        ],
                        4,
                      ),
                    ])),
                r.isSearch
                  ? (d(),
                    S(
                      $,
                      {
                        key: 1,
                        modelValue: w.keyword,
                        "onUpdate:modelValue":
                          a[4] || (a[4] = (h) => (w.keyword = h)),
                        placeholder: o(l)("query"),
                        clearable: "",
                        size: "small",
                        "prefix-icon": "Search",
                        style: { width: "80%", margin: "auto" },
                        onKeyup: pe(T, ["enter", "native"]),
                      },
                      null,
                      8,
                      ["modelValue", "placeholder"],
                    ))
                  : F("", !0),
              ]),
              t("div", Jt, [
                s(
                  G,
                  {
                    size: "small",
                    type: "success",
                    icon: o(ye),
                    circle: "",
                    plain: "",
                    onClick: a[5] || (a[5] = (h) => (r.isSearch = !r.isSearch)),
                  },
                  null,
                  8,
                  ["icon"],
                ),
              ]),
            ]),
            s(
              O,
              { style: { width: "98%", height: "84%", margin: "0 auto" } },
              {
                default: i(() => [
                  ve(
                    (d(),
                    f("div", $t, [
                      p.value.length !== 0
                        ? (d(),
                          f("div", en, [
                            (d(!0),
                            f(
                              Q,
                              null,
                              H(
                                p.value,
                                (h, me) => (
                                  d(),
                                  S(
                                    Bt,
                                    {
                                      browser: h,
                                      onOnDelete: J,
                                      style: { margin: "1.5rem 0" },
                                      key: me,
                                    },
                                    null,
                                    8,
                                    ["browser"],
                                  )
                                ),
                              ),
                              128,
                            )),
                            a[12] ||
                              (a[12] = t(
                                "div",
                                { style: { height: "50px" } },
                                null,
                                -1,
                              )),
                          ]))
                        : (d(),
                          f("div", tn, [
                            s(
                              ee,
                              {
                                description: o(l)("cfgEmp"),
                                style: { "margin-top": "20%" },
                              },
                              {
                                default: i(() => [
                                  t("div", nn, c(o(l)("clickToAdd")), 1),
                                ]),
                                _: 1,
                              },
                              8,
                              ["description"],
                            ),
                          ])),
                    ])),
                    [[Ae, r.loading]],
                  ),
                ]),
                _: 1,
              },
            ),
            t("div", on, [
              s(
                te,
                {
                  style: { "justify-content": "center" },
                  "current-page": w.page,
                  "onUpdate:currentPage": a[6] || (a[6] = (h) => (w.page = h)),
                  "page-size": w.size,
                  background: "",
                  small: "",
                  layout: "prev, pager, next",
                  total: w.total,
                  onCurrentChange: v,
                },
                null,
                8,
                ["current-page", "page-size", "total"],
              ),
              t("div", sn, [
                z(c(o(l)("curIP")) + ": ", 1),
                t(
                  "span",
                  ln,
                  c(((oe = r.ipInfo) == null ? void 0 : oe.ip) || "fail"),
                  1,
                ),
              ]),
            ]),
            t("div", an, [
              t("div", cn, [
                t("div", { class: "github", onClick: m }, " ⭐ GitHub "),
                t(
                  "div",
                  {
                    class: "testSite",
                    style: { color: "#33af02" },
                    onClick: a[7] || (a[7] = (h) => (r.drawer = !r.drawer)),
                  },
                  c(o(l)("cswz")),
                  1,
                ),
              ]),
              t("div", { class: "row donate", onClick: W }, [
                t(
                  "img",
                  {
                    class: "gif",
                    src: o(It),
                    alt: "fire",
                    height: re,
                    width: re,
                  },
                  null,
                  8,
                  rn,
                ),
                t("a", dn, c(o(l)("donate")), 1),
              ]),
            ]),
            s(
              R,
              {
                ref: "dialog",
                modelValue: r.onNewMachine,
                "onUpdate:modelValue":
                  a[8] || (a[8] = (h) => (r.onNewMachine = h)),
                title: o(l)("jg"),
                center: "",
                style: { width: "85%" },
              },
              {
                footer: i(() => [
                  t("div", mn, [
                    t("div", wn, [
                      s(
                        G,
                        { onClick: E },
                        { default: i(() => [z(c(o(l)("qx")), 1)]), _: 1 },
                      ),
                      s(
                        G,
                        { type: "primary", onClick: y },
                        { default: i(() => [z(c(o(l)("qr")), 1)]), _: 1 },
                      ),
                    ]),
                  ]),
                ]),
                default: i(() => [
                  t("div", gn, [
                    t("div", un, [
                      s(
                        L,
                        { style: { color: "orange" } },
                        { default: i(() => [s(o(be))]), _: 1 },
                      ),
                      t("span", hn, c(o(l)("newMc")), 1),
                    ]),
                    t("div", An, c(o(l)("onNewMachine")), 1),
                  ]),
                ]),
                _: 1,
              },
              8,
              ["modelValue", "title"],
            ),
            s(
              he,
              {
                modelValue: r.drawer,
                "onUpdate:modelValue": a[9] || (a[9] = (h) => (r.drawer = h)),
                direction: "btt",
                "with-header": !1,
                style: {
                  "border-top-right-radius": "12px",
                  "border-top-left-radius": "12px",
                },
              },
              {
                default: i(() => [
                  t("div", fn, [
                    t("div", pn, [
                      a[13] ||
                        (a[13] = t(
                          "div",
                          { class: "testTitle" },
                          "Fingerprint test",
                          -1,
                        )),
                      s(
                        O,
                        {
                          style: U([
                            { width: "100%", height: "100%", margin: "0 auto" },
                            `background: url(${o(Ot)}) no-repeat center/62%`,
                          ]),
                        },
                        {
                          default: i(() => [
                            (d(),
                            f(
                              Q,
                              null,
                              H(n, (h) =>
                                t("div", yn, [
                                  t(
                                    "img",
                                    {
                                      src: h.icon,
                                      alt: "logo",
                                      style: {
                                        width: "26px",
                                        height: "26px",
                                        "border-radius": "18px",
                                      },
                                    },
                                    null,
                                    8,
                                    vn,
                                  ),
                                  t("div", bn, [
                                    t(
                                      "a",
                                      {
                                        href: h.url,
                                        target: "_blank",
                                        style: {
                                          "margin-right": "auto",
                                          "font-size": "15px",
                                        },
                                      },
                                      c(h.name),
                                      9,
                                      zn,
                                    ),
                                  ]),
                                  s(
                                    L,
                                    {
                                      style: {
                                        color: "#8dbd47",
                                        "font-size": "20px",
                                        "margin-left": "auto",
                                      },
                                    },
                                    { default: i(() => [s(o(ie))]), _: 1 },
                                  ),
                                ]),
                              ),
                              64,
                            )),
                          ]),
                          _: 1,
                        },
                        8,
                        ["style"],
                      ),
                    ]),
                    t("div", En, [
                      a[14] ||
                        (a[14] = t(
                          "div",
                          { class: "testTitle" },
                          "Bot test",
                          -1,
                        )),
                      s(
                        O,
                        {
                          style: U([
                            { width: "100%", height: "100%", margin: "0 auto" },
                            `background: url(${o(Lt)}) no-repeat center/58%`,
                          ]),
                        },
                        {
                          default: i(() => [
                            (d(),
                            f(
                              Q,
                              null,
                              H(N, (h) =>
                                t("div", Cn, [
                                  t(
                                    "img",
                                    {
                                      src: h.icon,
                                      alt: "logo",
                                      style: {
                                        width: "26px",
                                        height: "26px",
                                        "border-radius": "18px",
                                      },
                                    },
                                    null,
                                    8,
                                    Rn,
                                  ),
                                  t("div", xn, [
                                    t(
                                      "a",
                                      {
                                        href: h.url,
                                        target: "_blank",
                                        style: {
                                          "margin-right": "auto",
                                          "font-size": "15px",
                                        },
                                      },
                                      c(h.name),
                                      9,
                                      kn,
                                    ),
                                  ]),
                                  s(
                                    L,
                                    {
                                      style: {
                                        color: "#8dbd47",
                                        "font-size": "20px",
                                        "margin-left": "auto",
                                      },
                                    },
                                    { default: i(() => [s(o(ie))]), _: 1 },
                                  ),
                                ]),
                              ),
                              64,
                            )),
                          ]),
                          _: 1,
                        },
                        8,
                        ["style"],
                      ),
                    ]),
                  ]),
                ]),
                _: 1,
              },
              8,
              ["modelValue"],
            ),
          ])
        );
      };
    },
  },
  Un = Y(Bn, [["__scopeId", "data-v-09c7ae64"]]);
export { Un as default };

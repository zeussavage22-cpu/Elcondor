import {
  _ as ee,
  L as H,
  e as te,
  f as ne,
  v as T,
  r as f,
  B as se,
  z as ie,
  u as t,
  o as g,
  g as U,
  d as l,
  b as n,
  w as u,
  a as C,
  c as k,
  F as R,
  p as O,
  t as S,
  q as A,
  D as de,
  h as re,
  l as le,
  s as ue,
  i as me,
  G as pe,
  H as ce,
  j as he,
} from "./popup.js";
import { k as fe, m as oe, o as ge, n as ye, l as we } from "./logo.js";
import { g as Q, s as ve, a as ae, c as be } from "./util.js";
const _e = {
    class: "col",
    style: { width: "100%", height: "100%", "justify-content": "left" },
  },
  Ve = { class: "row line", style: { "margin-bottom": "10px" } },
  ze = { style: { width: "90%" }, class: "row" },
  xe = { style: { width: "10%" } },
  Pe = {
    style: { width: "100%", height: "30px", "place-items": "center" },
    class: "row line",
  },
  De = {
    style: {
      width: "85%",
      "font-size": "13px",
      color: "#b4038b",
      "font-weight": "bold",
      "text-align": "left",
    },
  },
  Se = { style: { "font-weight": "bold", color: "#c0bebe" } },
  Ue = {
    style: { width: "10%", float: "right", "justify-content": "right" },
    class: "row line",
  },
  Ne = { class: "col" },
  ke = { style: { width: "100%", "text-align": "center" } },
  Ee = { key: 0, style: { color: "#989797", height: "25px" } },
  Ce = { key: 1, style: { padding: "0 5px 0 5px" } },
  je = {
    key: 4,
    style: {
      color: "red",
      height: "25px",
      width: "100%",
      "background-color": "#f4f6f9",
      "border-radius": "5px",
      "justify-content": "center",
      "align-items": "center",
    },
    class: "col",
  },
  $e = {
    key: 1,
    style: { "text-align": "center", "font-size": "14px", color: "gray" },
  },
  Ae = {
    __name: "Prototypes",
    props: { customProtos: Array },
    setup(B) {
      let { tr: M, ident: d, defined: x } = H.tr();
      x({
        type: { zh: "类型", en: "type" },
        pName: { zh: "属性名", en: "Property name" },
        pType: { zh: "属性类型", en: "Property type" },
        pValue: { zh: "属性值", en: "Property value" },
        inputJSON: { zh: "请输入json串", en: "Please enter a JSON string" },
        inputStr: { zh: "请输入字符串", en: "Please enter a string" },
        inputBool: { zh: "请选择布尔值", en: "Please select a Boolean value" },
        inputNum: { zh: "请输入数字", en: "Please enter a number" },
        inputAttr: {
          zh: "请输入自定义属性的值",
          en: "Please enter the value of the custom attribute",
        },
        inputPCN: {
          zh: "请输入原型对象构造函数名称",
          en: "Please input a prototype's constructor name",
        },
        inputCPV: {
          zh: "请输入自定义属性值",
          en: "Please input a custom property values",
        },
        notFindC: {
          zh: "无法找到 [cname] 构造函数",
          en: "Unable to find [cname] constructor",
        },
        notFindP: {
          zh: "[cname] 构造函数的原型不存在",
          en: "The prototype of the [cname] constructor does not exist",
        },
        reAdd: { zh: "重复添加 [cname]", en: "Repeat adding [cname]" },
      });
      let $ = B,
        v = te({
          active: -1,
          constructorName: "",
          loading: !1,
          options: [],
          winProtos: [],
          nameSet: new Set(),
        });
      ((v.winProtos = o(window)),
        ne(() => {
          $.customProtos.length &&
            $.customProtos.forEach((c) => {
              V(c.name).properties.push(...c.properties);
            });
        }));
      function V(c) {
        let r = Q(window, c);
        if (!r) {
          T.error({ message: d("notFindC", { vars: { cname: c } }) });
          return;
        }
        let b = r.prototype;
        if (!b) {
          T.error({ message: d("notFindP", { vars: { cname: c } }) });
          return;
        }
        let _ = Object.getOwnPropertyDescriptors(b),
          s = [];
        for (let e in _) {
          let N = _[e];
          e !== "Symbol(Symbol.toStringTag)" &&
            typeof N.value != "function" &&
            N.get &&
            s.push(e);
        }
        return (v.nameSet.add(c), { name: c, allProperty: s, properties: [] });
      }
      function I() {
        v.loading = !0;
        try {
          if (v.constructorName) {
            if (
              ((v.constructorName.startsWith("window.") ||
                v.constructorName.startsWith("self.")) &&
                (v.constructorName = v.constructorName
                  .replace("window.", "")
                  .replace("self.", "")),
              v.nameSet.has(v.constructorName))
            ) {
              T.error({
                message: d("reAdd", { vars: { cname: v.constructorName } }),
              });
              return;
            }
            let c = V(v.constructorName);
            $.customProtos.push(c);
          }
        } finally {
          setTimeout(() => {
            v.loading = !1;
          }, 300);
        }
      }
      function o(c) {
        let r = [],
          b = Object.getOwnPropertyDescriptors(c);
        for (let _ in b)
          /^[A-Z]/.test(_) && typeof c[_] == "function" && r.push(_);
        return r;
      }
      function W(c = "", r) {
        let b = v.winProtos;
        if (!c) {
          r(b.map((_) => ({ value: _ })));
          return;
        }
        if (/^[A-Z]/.test(c)) {
          if (c.includes(".")) {
            let s = c.lastIndexOf("."),
              e = c.substring(0, s),
              N = Q(window, e);
            b = o(N);
          }
          let _ = b.filter((s) => s.startsWith(c)).map((s) => ({ value: s }));
          setTimeout(() => {
            r(_);
          }, 300);
        } else r([]);
      }
      function Y(c, r = "") {
        if (!r) {
          v.options = c.map((b) => ({ value: b }));
          return;
        }
        v.options = c.filter((b) => b.startsWith(r)).map((b) => ({ value: b }));
      }
      function h(c, r) {
        c.stopPropagation();
        let b = {};
        ($.customProtos[r].properties.push(b), (v.active = r));
      }
      function w(c) {
        $.customProtos.splice(c, 1);
      }
      function z(c, r) {
        $.customProtos[c].properties.splice(r, 1);
      }
      return (c, r) => {
        const b = f("el-autocomplete"),
          _ = f("el-button"),
          s = f("el-table-column"),
          e = f("el-option"),
          N = f("el-select"),
          j = f("el-input"),
          m = f("el-input-number"),
          P = f("el-icon"),
          G = f("el-table"),
          F = f("el-collapse-item"),
          K = f("el-collapse"),
          L = se("loading");
        return ie(
          (g(),
          U("div", _e, [
            l("div", Ve, [
              l("div", ze, [
                n(
                  b,
                  {
                    modelValue: t(v).constructorName,
                    "onUpdate:modelValue":
                      r[0] || (r[0] = (D) => (t(v).constructorName = D)),
                    "fetch-suggestions": W,
                    placeholder: t(d)("inputPCN"),
                    style: { height: "25px", width: "100%" },
                    size: "small",
                    clearable: "",
                  },
                  {
                    prepend: u(() => r[2] || (r[2] = [C(" window. ")])),
                    append: u(() => r[3] || (r[3] = [C(" .prototype ")])),
                    _: 1,
                  },
                  8,
                  ["modelValue", "placeholder"],
                ),
              ]),
              l("div", xe, [
                n(
                  _,
                  {
                    type: "warning",
                    plain: "",
                    onClick: I,
                    style: { height: "25px", width: "26px", float: "right" },
                  },
                  { default: u(() => r[4] || (r[4] = [C("＋ ")])), _: 1 },
                ),
              ]),
            ]),
            B.customProtos.length !== 0
              ? (g(),
                k(
                  K,
                  {
                    key: 0,
                    accordion: "",
                    style: { padding: "0 15px 0 15px" },
                    modelValue: t(v).active,
                    "onUpdate:modelValue":
                      r[1] || (r[1] = (D) => (t(v).active = D)),
                  },
                  {
                    default: u(() => [
                      (g(!0),
                      U(
                        R,
                        null,
                        O(
                          B.customProtos,
                          (D, E) => (
                            g(),
                            k(
                              F,
                              { name: E },
                              {
                                title: u(() => [
                                  l("div", Pe, [
                                    l("div", De, [
                                      C(S(D.name + ": ") + " ", 1),
                                      l("span", Se, S(D.properties.length), 1),
                                    ]),
                                    l("div", Ue, [
                                      n(
                                        _,
                                        {
                                          type: "primary",
                                          plain: "",
                                          onClick: (y) => h(y, E),
                                          size: "small",
                                          style: {
                                            height: "18px",
                                            width: "19px",
                                          },
                                        },
                                        {
                                          default: u(
                                            () => r[5] || (r[5] = [C("+ ")]),
                                          ),
                                          _: 2,
                                        },
                                        1032,
                                        ["onClick"],
                                      ),
                                      n(
                                        _,
                                        {
                                          type: "danger",
                                          plain: "",
                                          onClick: (y) => w(E),
                                          size: "small",
                                          style: {
                                            height: "18px",
                                            width: "19px",
                                          },
                                        },
                                        {
                                          default: u(
                                            () => r[6] || (r[6] = [C("- ")]),
                                          ),
                                          _: 2,
                                        },
                                        1032,
                                        ["onClick"],
                                      ),
                                    ]),
                                  ]),
                                ]),
                                default: u(() => [
                                  l("div", Ne, [
                                    n(
                                      G,
                                      {
                                        data: D.properties,
                                        style: { width: "100%" },
                                        size: "small",
                                        height: "200",
                                      },
                                      {
                                        default: u(() => [
                                          n(
                                            s,
                                            {
                                              type: "index",
                                              index: (y) => y + 1,
                                              label: "#",
                                              width: "25",
                                              fixed: "",
                                            },
                                            null,
                                            8,
                                            ["index"],
                                          ),
                                          n(
                                            s,
                                            {
                                              prop: "key",
                                              label: t(d)("pName"),
                                              width: "120",
                                            },
                                            {
                                              default: u((y) => [
                                                n(
                                                  N,
                                                  {
                                                    modelValue: y.row.key,
                                                    "onUpdate:modelValue": (
                                                      p,
                                                    ) => (y.row.key = p),
                                                    placeholder: t(d)("pName"),
                                                    style: {
                                                      height: "25px",
                                                      width: "100%",
                                                    },
                                                    size: "small",
                                                    remote: "",
                                                    filterable: "",
                                                    clearable: "",
                                                    "remote-method": (p) =>
                                                      Y(D.allProperty, p),
                                                  },
                                                  {
                                                    default: u(() => [
                                                      (g(!0),
                                                      U(
                                                        R,
                                                        null,
                                                        O(
                                                          t(v).options,
                                                          (p) => (
                                                            g(),
                                                            k(
                                                              e,
                                                              {
                                                                key: p.value,
                                                                label: p.value,
                                                                value: p.value,
                                                              },
                                                              null,
                                                              8,
                                                              [
                                                                "label",
                                                                "value",
                                                              ],
                                                            )
                                                          ),
                                                        ),
                                                        128,
                                                      )),
                                                    ]),
                                                    _: 2,
                                                  },
                                                  1032,
                                                  [
                                                    "modelValue",
                                                    "onUpdate:modelValue",
                                                    "placeholder",
                                                    "remote-method",
                                                  ],
                                                ),
                                              ]),
                                              _: 2,
                                            },
                                            1032,
                                            ["label"],
                                          ),
                                          n(
                                            s,
                                            {
                                              prop: "type",
                                              label: t(d)("pType"),
                                              width: "100",
                                            },
                                            {
                                              default: u((y) => [
                                                n(
                                                  N,
                                                  {
                                                    modelValue: y.row.type,
                                                    "onUpdate:modelValue": (
                                                      p,
                                                    ) => (y.row.type = p),
                                                    size: "small",
                                                    placeholder: t(d)("type"),
                                                    style: {
                                                      width: "100%",
                                                      height: "25px",
                                                    },
                                                  },
                                                  {
                                                    default: u(() => [
                                                      n(e, {
                                                        label: "number",
                                                        value: "number",
                                                      }),
                                                      n(e, {
                                                        label: "string",
                                                        value: "string",
                                                      }),
                                                      n(e, {
                                                        label: "boolean",
                                                        value: "boolean",
                                                      }),
                                                      n(e, {
                                                        label: "json",
                                                        value: "json",
                                                      }),
                                                      n(e, {
                                                        label: "undefined",
                                                        value: "undefined",
                                                      }),
                                                    ]),
                                                    _: 2,
                                                  },
                                                  1032,
                                                  [
                                                    "modelValue",
                                                    "onUpdate:modelValue",
                                                    "placeholder",
                                                  ],
                                                ),
                                              ]),
                                              _: 1,
                                            },
                                            8,
                                            ["label"],
                                          ),
                                          n(
                                            s,
                                            {
                                              prop: "value",
                                              label: t(d)("pValue"),
                                              width: "180",
                                            },
                                            {
                                              default: u((y) => [
                                                l("div", ke, [
                                                  y.row.type
                                                    ? (g(),
                                                      U("div", Ce, [
                                                        y.row.type === "string"
                                                          ? (g(),
                                                            k(
                                                              j,
                                                              {
                                                                key: 0,
                                                                modelValue:
                                                                  y.row.value,
                                                                "onUpdate:modelValue":
                                                                  (p) =>
                                                                    (y.row.value =
                                                                      p),
                                                                placeholder:
                                                                  t(d)(
                                                                    "inputStr",
                                                                  ),
                                                                clearable: "",
                                                                size: "small",
                                                                style: {
                                                                  height:
                                                                    "25px",
                                                                  width: "100%",
                                                                },
                                                              },
                                                              null,
                                                              8,
                                                              [
                                                                "modelValue",
                                                                "onUpdate:modelValue",
                                                                "placeholder",
                                                              ],
                                                            ))
                                                          : A("", !0),
                                                        y.row.type === "json"
                                                          ? (g(),
                                                            k(
                                                              j,
                                                              {
                                                                key: 1,
                                                                modelValue:
                                                                  y.row.value,
                                                                "onUpdate:modelValue":
                                                                  (p) =>
                                                                    (y.row.value =
                                                                      p),
                                                                placeholder:
                                                                  t(d)(
                                                                    "inputJSON",
                                                                  ),
                                                                clearable: "",
                                                                size: "small",
                                                                style: {
                                                                  height: "20%",
                                                                  width: "100%",
                                                                },
                                                              },
                                                              null,
                                                              8,
                                                              [
                                                                "modelValue",
                                                                "onUpdate:modelValue",
                                                                "placeholder",
                                                              ],
                                                            ))
                                                          : A("", !0),
                                                        y.row.type === "number"
                                                          ? (g(),
                                                            k(
                                                              m,
                                                              {
                                                                key: 2,
                                                                modelValue:
                                                                  y.row.value,
                                                                "onUpdate:modelValue":
                                                                  (p) =>
                                                                    (y.row.value =
                                                                      p),
                                                                placeholder:
                                                                  t(d)(
                                                                    "inputNum",
                                                                  ),
                                                                clearable: "",
                                                                size: "small",
                                                                style: {
                                                                  height:
                                                                    "25px",
                                                                  width: "100%",
                                                                },
                                                              },
                                                              null,
                                                              8,
                                                              [
                                                                "modelValue",
                                                                "onUpdate:modelValue",
                                                                "placeholder",
                                                              ],
                                                            ))
                                                          : A("", !0),
                                                        y.row.type === "boolean"
                                                          ? (g(),
                                                            k(
                                                              N,
                                                              {
                                                                key: 3,
                                                                style: {
                                                                  height:
                                                                    "25px",
                                                                  width: "100%",
                                                                },
                                                                size: "small",
                                                                modelValue:
                                                                  y.row.value,
                                                                "onUpdate:modelValue":
                                                                  (p) =>
                                                                    (y.row.value =
                                                                      p),
                                                                placeholder:
                                                                  t(d)(
                                                                    "inputBool",
                                                                  ),
                                                              },
                                                              {
                                                                default: u(
                                                                  () => [
                                                                    n(e, {
                                                                      label:
                                                                        "true",
                                                                      value: !0,
                                                                    }),
                                                                    n(e, {
                                                                      label:
                                                                        "false",
                                                                      value: !1,
                                                                    }),
                                                                  ],
                                                                ),
                                                                _: 2,
                                                              },
                                                              1032,
                                                              [
                                                                "modelValue",
                                                                "onUpdate:modelValue",
                                                                "placeholder",
                                                              ],
                                                            ))
                                                          : A("", !0),
                                                        y.row.type ===
                                                        "undefined"
                                                          ? (g(),
                                                            U("div", je, [
                                                              n(P, null, {
                                                                default: u(
                                                                  () => [
                                                                    n(t(de)),
                                                                  ],
                                                                ),
                                                                _: 1,
                                                              }),
                                                            ]))
                                                          : A("", !0),
                                                      ]))
                                                    : (g(),
                                                      U(
                                                        "div",
                                                        Ee,
                                                        S(t(d)("inputAttr")),
                                                        1,
                                                      )),
                                                ]),
                                              ]),
                                              _: 1,
                                            },
                                            8,
                                            ["label"],
                                          ),
                                          n(
                                            s,
                                            {
                                              prop: "",
                                              label: t(d)("cz"),
                                              width: "60",
                                              fixed: "right",
                                            },
                                            {
                                              default: u((y) => [
                                                n(
                                                  _,
                                                  {
                                                    size: "small",
                                                    plain: "",
                                                    style: {
                                                      width: "5%",
                                                      height: "4%",
                                                    },
                                                    type: "danger",
                                                    onClick: (p) =>
                                                      z(E, y.$index),
                                                  },
                                                  {
                                                    default: u(
                                                      () =>
                                                        r[7] ||
                                                        (r[7] = [C("x ")]),
                                                    ),
                                                    _: 2,
                                                  },
                                                  1032,
                                                  ["onClick"],
                                                ),
                                              ]),
                                              _: 2,
                                            },
                                            1032,
                                            ["label"],
                                          ),
                                        ]),
                                        _: 2,
                                      },
                                      1032,
                                      ["data"],
                                    ),
                                  ]),
                                ]),
                                _: 2,
                              },
                              1032,
                              ["name"],
                            )
                          ),
                        ),
                        256,
                      )),
                    ]),
                    _: 1,
                  },
                  8,
                  ["modelValue"],
                ))
              : (g(),
                U(
                  "div",
                  $e,
                  r[8] ||
                    (r[8] = [
                      l(
                        "div",
                        {
                          style: {
                            "margin-top": "20px",
                            "margin-bottom": "30px",
                          },
                        },
                        " No Data ",
                        -1,
                      ),
                    ]),
                )),
          ])),
          [[L, t(v).loading]],
        );
      };
    },
  },
  Be = ee(Ae, [["__scopeId", "data-v-7eb50af1"]]),
  Le = { class: "col" },
  Te = {
    style: { "font-size": "13px", color: "#181818", "font-weight": "bold" },
  },
  Ie = {
    style: {
      "word-break": "normal",
      "font-size": "11px",
      color: "#605c5c",
      "font-weight": "bold",
    },
  },
  Re = {
    class: "row",
    style: {
      width: "12px",
      height: "100%",
      "justify-content": "center",
      position: "relative",
    },
  },
  Oe = {
    __name: "Prompt",
    props: { content: String, color: String },
    setup(B) {
      let { tr: M, ident: d, defined: x } = H.tr();
      return (
        x({ zdsm: { zh: "字段说明", en: "Field description" } }),
        ($, v) => {
          const V = f("Document"),
            I = f("el-icon"),
            o = f("el-popover");
          return (
            g(),
            k(
              o,
              { placement: "top-start", trigger: "hover", content: B.content },
              {
                reference: u(() => [
                  l("div", null, [
                    l("div", Re, [
                      n(
                        I,
                        {
                          class: "icon",
                          style: re(`color:${B.color || "#7c9dfe"};`),
                        },
                        { default: u(() => [n(V)]), _: 1 },
                        8,
                        ["style"],
                      ),
                    ]),
                  ]),
                ]),
                default: u(() => [
                  l("div", Le, [
                    l("div", Te, [
                      C(S(t(d)("zdsm")) + ": ", 1),
                      l("span", Ie, S(B.content), 1),
                    ]),
                  ]),
                ]),
                _: 1,
              },
              8,
              ["content"],
            )
          );
        }
      );
    },
  },
  q = ee(Oe, [["__scopeId", "data-v-a244a87d"]]),
  Me = {
    class: "col",
    style: { width: "100%", height: "100%", "justify-content": "left" },
  },
  We = { class: "row line", style: { "margin-bottom": "3px" } },
  Ge = { style: { width: "90%" } },
  Fe = { style: { width: "10%" } },
  Ke = { style: { "text-align": "left", color: "#3333d5" }, class: "omit" },
  Je = { style: { width: "100%", "text-align": "center" } },
  He = {
    __name: "VariableTree",
    props: { customVars: Array },
    setup(B, { expose: M }) {
      let { tr: d, ident: x, defined: $ } = H.tr();
      $({
        vPath: { zh: "变量路径", en: "Variable Path" },
        vType: { zh: "变量类型", en: "Variable Type" },
        vValue: { zh: "变量值", en: "Variable Value" },
        inputJSON: { zh: "请输入json串", en: "Please enter a JSON string" },
        inputStr: { zh: "请输入字符串", en: "Please enter a string" },
        inputBool: { zh: "请选择布尔值", en: "Please select a Boolean value" },
        inputNum: { zh: "请输入数字", en: "Please enter a number" },
        inputGVP: {
          zh: "请输入全局变量路径",
          en: "Please input a global variable path",
        },
        reAdd: { zh: "重复添加 [path]", en: "Repeat adding [path]" },
        isFuncE: {
          zh: "该变量数据类型是函数 [path]",
          en: "The data type of this variable is a function [path]",
        },
        forbid: { zh: "禁止修改 [path]", en: "Prohibit modify [path]" },
      });
      let v = B,
        V = te({
          typeMapColor: {
            string: "#9d0606",
            number: "#07b9cb",
            boolean: "#1a1ab9",
            object: "#5f6368",
            undefined: "#423f3f",
            array: "#33cc09",
          },
          trees: [],
          vars: [],
          rootPath: "",
          rootPathSet: new Set(),
          pathMapEditValue: {},
          disablePath: new Set([
            "navigator.appVersion",
            "navigator.deviceMemory",
            "navigator.hardwareConcurrency",
            "navigator.language",
            "navigator.languages",
            "navigator.userAgent",
            "window.devicePixelRatio",
            "screen.width",
            "screen.height",
          ]),
          selectValue: "",
        });
      ne(() => {
        (v.customVars || []).forEach((h) => {
          let w = Q(window, h.path),
            z = typeof w;
          ((z === "undefined" || z === "object") && (z = "json"),
            V.vars.push({
              path: h.path,
              newValue: h.value,
              type: z,
              value: w,
            }));
        });
      });
      function I(h = "", w) {
        const z = (m) => {
          let P = [];
          for (const G in m) typeof m[G] != "function" && P.push(G);
          return P;
        };
        let c = [];
        if (!h) {
          ((c = z(window)), w(c.map((m) => ({ value: m }))));
          return;
        }
        let r = ve(h),
          b,
          _;
        h.endsWith(".") || h.endsWith("[")
          ? ((b = ""), (_ = ae(r.slice(0, r.length))))
          : ((b = r[r.length - 1] + ""), (_ = ae(r.slice(0, r.length - 1))));
        let s = Q(window, _);
        c = z(s);
        let e = _,
          N =
            s &&
            ((typeof s.forEach == "function" && Number.isInteger(s.length)) ||
              s.constructor.name.endsWith("Array"));
        if ((r.length === 1 && (e = ""), typeof s != "object")) {
          w([{ value: _ }]);
          return;
        }
        let j = c
          .filter((m) => (m + "").startsWith(b))
          .map((m) => ({
            value: N ? e + "[" + m + "]" : (e ? e + "." : "") + m,
          }));
        setTimeout(() => {
          w(j);
        }, 150);
      }
      function o(h) {
        if (h) {
          if (
            ((h.startsWith("window.") || h.startsWith("self.")) &&
              (h = h.replace("window.", "").replace("self.", "")),
            V.disablePath.has(h) && V.rootPathSet.has(h))
          ) {
            T({
              showClose: !1,
              message: x("forbid", { vars: { path: h } }),
              center: !0,
              type: "warning",
            });
            return;
          }
          if (V.rootPathSet.has(h)) {
            T({
              showClose: !1,
              message: x("reAdd", { vars: { path: h } }),
              center: !0,
              type: "warning",
            });
            return;
          }
          let w = Q(window, h),
            z = typeof w;
          if (z === "function") {
            T({
              showClose: !1,
              message: x("isFuncE", { vars: { path: h } }),
              center: !0,
              type: "warning",
            });
            return;
          }
          ((z === "undefined" || z === "object") && (z = "json"),
            V.vars.push({ path: h, value: w, type: z }),
            V.rootPathSet.add(h));
        }
      }
      function W(h) {
        let w = V.vars[h].path;
        (V.vars.splice(h, 1), V.rootPathSet.delete(w));
      }
      function Y() {
        return V.vars
          .filter(({ path: h, value: w, newValue: z }) =>
            z === void 0 ? !1 : w + "" != z + "",
          )
          .map(({ path: h, newValue: w }) => ({ path: h, value: w }));
      }
      return (
        M({ getEdited: Y }),
        (h, w) => {
          const z = f("el-autocomplete"),
            c = f("el-button"),
            r = f("el-table-column"),
            b = f("el-tooltip"),
            _ = f("el-input"),
            s = f("el-input-number"),
            e = f("el-option"),
            N = f("el-select"),
            j = f("el-table");
          return (
            g(),
            U("div", Me, [
              l("div", We, [
                l("div", Ge, [
                  n(
                    z,
                    {
                      ref: "autocomplete",
                      modelValue: t(V).rootPath,
                      "onUpdate:modelValue":
                        w[0] || (w[0] = (m) => (t(V).rootPath = m)),
                      placeholder: t(x)("inputGVP"),
                      clearable: "",
                      "fetch-suggestions": I,
                      style: { height: "25px", width: "100%" },
                      size: "small",
                    },
                    null,
                    8,
                    ["modelValue", "placeholder"],
                  ),
                ]),
                l("div", Fe, [
                  n(
                    c,
                    {
                      type: "primary",
                      plain: "",
                      onClick: w[1] || (w[1] = (m) => o(t(V).rootPath)),
                      style: { height: "25px", width: "26px", float: "right" },
                    },
                    { default: u(() => w[2] || (w[2] = [C("＋ ")])), _: 1 },
                  ),
                ]),
              ]),
              n(
                j,
                {
                  data: t(V).vars,
                  style: { width: "100%" },
                  size: "small",
                  height: "200",
                },
                {
                  default: u(() => [
                    n(
                      r,
                      {
                        type: "index",
                        index: (m) => m + 1,
                        label: "#",
                        width: "25",
                        fixed: "",
                      },
                      null,
                      8,
                      ["index"],
                    ),
                    n(
                      r,
                      { prop: "path", label: t(x)("vPath"), width: "180" },
                      {
                        default: u((m) => [
                          n(
                            b,
                            {
                              content: m.row.path,
                              placement: "bottom",
                              effect: "light",
                              style: { width: "100%" },
                            },
                            {
                              default: u(() => [
                                l("div", Ke, S(m.row.path), 1),
                              ]),
                              _: 2,
                            },
                            1032,
                            ["content"],
                          ),
                        ]),
                        _: 1,
                      },
                      8,
                      ["label"],
                    ),
                    n(
                      r,
                      { prop: "type", label: t(x)("vType"), width: "100" },
                      null,
                      8,
                      ["label"],
                    ),
                    n(
                      r,
                      { prop: "value", label: t(x)("vValue"), width: "180" },
                      {
                        default: u((m) => [
                          l("div", Je, [
                            m.row.type === "json"
                              ? (g(),
                                k(
                                  _,
                                  {
                                    key: 0,
                                    modelValue: m.row.newValue,
                                    "onUpdate:modelValue": (P) =>
                                      (m.row.newValue = P),
                                    placeholder: t(x)("inputJSON"),
                                    clearable: "",
                                    size: "small",
                                    style: { height: "20%", width: "100%" },
                                  },
                                  null,
                                  8,
                                  [
                                    "modelValue",
                                    "onUpdate:modelValue",
                                    "placeholder",
                                  ],
                                ))
                              : A("", !0),
                            m.row.type === "string"
                              ? (g(),
                                k(
                                  _,
                                  {
                                    key: 1,
                                    modelValue: m.row.newValue,
                                    "onUpdate:modelValue": (P) =>
                                      (m.row.newValue = P),
                                    placeholder: t(x)("inputStr"),
                                    clearable: "",
                                    size: "small",
                                    style: { height: "25px", width: "100%" },
                                  },
                                  null,
                                  8,
                                  [
                                    "modelValue",
                                    "onUpdate:modelValue",
                                    "placeholder",
                                  ],
                                ))
                              : A("", !0),
                            m.row.type === "number"
                              ? (g(),
                                k(
                                  s,
                                  {
                                    key: 2,
                                    modelValue: m.row.newValue,
                                    "onUpdate:modelValue": (P) =>
                                      (m.row.newValue = P),
                                    placeholder: t(x)("inputNum"),
                                    clearable: "",
                                    size: "small",
                                    style: { height: "25px", width: "100%" },
                                  },
                                  null,
                                  8,
                                  [
                                    "modelValue",
                                    "onUpdate:modelValue",
                                    "placeholder",
                                  ],
                                ))
                              : A("", !0),
                            m.row.type === "boolean"
                              ? (g(),
                                k(
                                  N,
                                  {
                                    key: 3,
                                    style: { height: "25px", width: "100%" },
                                    size: "small",
                                    modelValue: m.row.newValue,
                                    "onUpdate:modelValue": (P) =>
                                      (m.row.newValue = P),
                                    placeholder: t(x)("inputBool"),
                                  },
                                  {
                                    default: u(() => [
                                      n(e, { label: "true", value: !0 }),
                                      n(e, { label: "false", value: !1 }),
                                    ]),
                                    _: 2,
                                  },
                                  1032,
                                  [
                                    "modelValue",
                                    "onUpdate:modelValue",
                                    "placeholder",
                                  ],
                                ))
                              : A("", !0),
                          ]),
                        ]),
                        _: 1,
                      },
                      8,
                      ["label"],
                    ),
                    n(
                      r,
                      {
                        prop: "",
                        label: t(x)("cz"),
                        width: "60",
                        fixed: "right",
                      },
                      {
                        default: u((m) => [
                          n(
                            c,
                            {
                              size: "small",
                              plain: "",
                              style: { width: "5%", height: "4%" },
                              type: "danger",
                              onClick: (P) => W(m.$index),
                            },
                            {
                              default: u(() => w[3] || (w[3] = [C("x ")])),
                              _: 2,
                            },
                            1032,
                            ["onClick"],
                          ),
                        ]),
                        _: 1,
                      },
                      8,
                      ["label"],
                    ),
                  ]),
                  _: 1,
                },
                8,
                ["data"],
              ),
            ])
          );
        }
      );
    },
  },
  Ye = { id: "build", class: "col", style: { height: "100%" } },
  Ze = {
    class: "row",
    style: {
      height: "10%",
      width: "100%",
      "box-shadow": "0 8px 6px -8px #737373",
      position: "relative",
      "z-index": "5",
      "justify-content": "center",
    },
  },
  Qe = { class: "col", style: { width: "100%", height: "100%" } },
  Xe = {
    class: "row",
    style: { width: "100%", height: "100%", position: "relative" },
  },
  qe = ["src"],
  et = {
    class: "col",
    style: {
      "align-items": "center",
      "align-content": "center",
      width: "100%",
      height: "100%",
      position: "absolute",
    },
  },
  tt = { class: "row-between", style: { width: "80%", margin: "auto 0 3px" } },
  lt = { style: { height: "90%", "margin-top": "auto" } },
  ot = {
    class: "col",
    style: { height: "95%", "margin-top": "5px", width: "100%" },
  },
  at = { class: "col line item", style: { width: "100%" } },
  nt = { class: "row" },
  st = { class: "col line item" },
  it = { class: "row" },
  rt = { class: "col line item" },
  dt = { class: "row" },
  ut = { class: "row input", style: { width: "100%" } },
  mt = { class: "col item" },
  pt = {
    style: {
      width: "100%",
      "justify-content": "space-around",
      "align-items": "center",
    },
    class: "row",
  },
  ct = {
    class: "row",
    style: {
      width: "50%",
      "justify-content": "space-around",
      "align-items": "center",
      "margin-top": "5px",
    },
  },
  ht = { class: "row", style: { width: "90vw" } },
  ft = { class: "omit", style: { "margin-right": "2px", width: "50%" } },
  gt = {
    class: "omit",
    style: { color: "var(--el-text-color-secondary)", "font-size": "1rem" },
  },
  yt = {
    class: "row",
    style: {
      width: "50%",
      "justify-content": "space-around",
      "align-items": "center",
      "margin-top": "5px",
    },
  },
  wt = {
    class: "row",
    style: { "justify-content": "space-between", width: "100%" },
  },
  vt = {
    style: { color: "var(--el-text-color-secondary)", "font-size": "1rem" },
  },
  bt = { class: "col item" },
  _t = {
    style: {
      width: "100%",
      "justify-content": "space-around",
      "align-items": "center",
    },
    class: "row",
  },
  Vt = {
    class: "row",
    style: {
      width: "50%",
      "justify-content": "space-around",
      "align-items": "center",
      "margin-top": "5px",
    },
  },
  zt = {
    class: "row",
    style: {
      width: "50%",
      "justify-content": "space-around",
      "align-items": "center",
      "margin-top": "5px",
    },
  },
  xt = { class: "col item" },
  Pt = {
    style: { width: "100%", "justify-content": "space-between" },
    class: "row",
  },
  Dt = { class: "col item" },
  St = { class: "col", style: { width: "90%", "margin-left": "10%" } },
  Ut = {
    style: { width: "100%", "justify-content": "space-between" },
    class: "row",
  },
  Nt = { class: "col item" },
  kt = { class: "row line", style: { width: "90%", "margin-left": "10%" } },
  Et = { class: "row line", style: { width: "90%", "margin-left": "10%" } },
  Ct = { class: "row line", style: { width: "50%" } },
  jt = { class: "row line", style: { width: "50%" } },
  $t = { class: "row line", style: { width: "90%", "margin-left": "10%" } },
  At = { class: "row line", style: { width: "50%" } },
  Bt = { class: "row line", style: { width: "50%" } },
  Lt = { class: "row line", style: { width: "90%", "margin-left": "10%" } },
  Tt = { class: "row line", style: { width: "90%", "margin-left": "10%" } },
  It = { class: "row line", style: { width: "90%", "margin-left": "10%" } },
  Rt = { class: "col item" },
  Ot = { class: "row big-label", style: { width: "60%" } },
  Mt = {
    class: "row",
    style: { "flex-wrap": "wrap", width: "90%", "margin-left": "10%" },
  },
  Wt = { class: "row", style: { width: "100%", "margin-bottom": "15px" } },
  Gt = { class: "small-label", style: { width: "60%" } },
  Ft = { class: "col item" },
  Kt = { class: "col item" },
  Jt = {
    __name: "Build",
    setup(B) {
      let { tr: M, ident: d, defined: x } = H.tr();
      x({
        inputName: {
          zh: "请输入配置名称",
          en: "Please enter the configuration name",
        },
        inputUA: { zh: "请输入UserAgent", en: "Please input a userAgent" },
        inputIP: { zh: "请输入IP地址", en: "Please input a IP address" },
        inputVendor: { zh: "请输入显卡厂商", en: "Please input a GPU vendor" },
        inputModel: { zh: "请输入显卡型号", en: "Please input a GPU model" },
        inputLng: { zh: "请输入经度", en: "Please input longitude" },
        inputLat: { zh: "请输入纬度", en: "Please input latitude" },
        selectLang: { zh: "请选择语言", en: "Please select a language" },
        selectTime: { zh: "请选择时区", en: "Please select a timezone" },
        selectMC: {
          zh: "请选择内存容量",
          en: "Please select a memory capacity",
        },
        inputNOP: {
          zh: "请输入cpu核心数",
          en: "Please enter the number of CPU cores",
        },
        cj: { zh: "重置", en: "reset" },
        sj: { zh: "随机", en: "randomly" },
        bc: { zh: "保存", en: "save" },
        dt: { zh: "动态", en: "auto" },
        jt: { zh: "静态", en: "static" },
        sm: { zh: "说明", en: "description" },
        path: { zh: "路径", en: "path" },
        type: { zh: "类型", en: "type" },
        value: { zh: "值", en: "value" },
        actions: { zh: "操作", en: "actions" },
        pName: { zh: "属性名", en: "Property name" },
        pType: { zh: "属性类型", en: "Property type" },
        pValue: { zh: "属性值", en: "Property value" },
        timeFL: {
          zh: "时区的值是否跟随网络",
          en: "Does the timezone value follow the network",
        },
        langFL: {
          zh: "语言的值是否跟随网络",
          en: "Does the language value follow the network",
        },
        lngFL: {
          zh: "经度的值是否跟随网络",
          en: "Does the value of longitude follow the network",
        },
        latFL: {
          zh: "纬度的值是否跟随网络",
          en: "Does the value of latitude follow the network",
        },
        vrp: {
          zh: "请输入全局变量根路径",
          en: "Please enter the root path of the global variable",
        },
        ipPmt: {
          zh: "当选择静态ip时请输入一个ipv4的地址;当选择动态ip时,插件将在您每次加载网页时自动获取您的公网ip",
          en: "When selecting a static IP, please enter an IPv4 address; When selecting a dynamic IP address, the plugin will automatically obtain your public IP address every time you load a webpage",
        },
        uaPmt: {
          zh: "userAgent中的系统名称和浏览器厂商尽量和自己的环境保持一致,浏览器的版本不要和真实版本相差太大",
          en: "The system name and browser manufacturer in userAgent should be as consistent as possible with their own environment, and the browser version should not differ too much from the actual version",
        },
        namePmt: {
          zh: "此配置的名称,必填项",
          en: "The name of this configuration is required",
        },
        warning: {
          zh: "确认清空当前表单配置信息?",
          en: "Confirm to clear the current form configuration information?",
        },
        nameNotEmp: {
          zh: "浏览器配置名称不能为空!",
          en: "Browser configuration name cannot be empty!",
        },
        nameExceed: {
          zh: "浏览器配置名称长度不能超过20！",
          en: "The length of the browser configuration name cannot exceed 20!",
        },
      });
      let $ = le(null),
        v = le(null),
        { languages: V, regions: I } = ue,
        o = te({
          curLanguage: H.curLang,
          itemMapType: fe,
          languages: V,
          regions: I,
          loading: !1,
          factorsKey: oe,
          webrtcStatus: "static",
          timezoneStatus: "static",
          languageStatus: "static",
          lngStatus: "static",
          latStatus: "static",
          formData: {
            name: "",
            userAgent: "",
            language: "",
            timezone: "",
            webrtc: "",
            webglInfo: {
              VENDOR: "",
              RENDERER: "",
              VERSION: "",
              SHADING_LANGUAGE_VERSION: "",
              UNMASKED_VENDOR_WEBGL: "",
              UNMASKED_RENDERER_WEBGL: "",
            },
            screen: {
              height: 0,
              width: 0,
              noise: 0,
              dpr: 0,
              maxTouchPoints: 0,
              colorDepth: 0,
              pixelDepth: 0,
              scheme: "",
            },
            location: { lng: 0, lat: 0 },
            memoryCapacity: 0,
            processors: 0,
            factors: (() => {
              let s = {};
              for (let e of oe) s[e.key] = 0;
              return s;
            })(),
            customVars: [],
            customProtos: [],
          },
        }),
        W = me(),
        Y = he();
      ((I || []).forEach((s, e) => (s.id = e)),
        (V || []).forEach((s, e) => (s.id = e)));
      let h = W.state.buildBrowser;
      ((o.empty = JSON.stringify(o.formData)),
        (o.formData = { ...o.formData, ...be(h) }),
        w(h),
        (o.loading = !1));
      function w(s) {
        (Object.keys(o.formData).forEach((e) => {
          o.formData[e] = s[e] || o.formData[e];
        }),
          s.webrtc === "auto" &&
            ((o.webrtcStatus = s.webrtc), (o.formData.webrtc = "")),
          s.timezone === "auto" &&
            ((o.timezoneStatus = s.timezone), (o.formData.timezone = "")),
          s.language === "auto" &&
            ((o.languageStatus = s.language), (o.formData.language = "")),
          (s.location = s.location || {}),
          s.location.lng === "auto" &&
            ((o.lngStatus = s.location.lng), (o.formData.location.lng = "")),
          s.location.lat === "auto" &&
            ((o.latStatus = s.location.lat), (o.formData.location.lat = "")),
          "noise" in (s.screen || {}),
          o.formData.memoryCapacity || (o.formData.memoryCapacity = void 0),
          o.formData.processors || (o.formData.processors = void 0),
          (o.formData.customVars = s.customVars || []),
          (o.formData.customProtos = s.customProtos || []));
      }
      navigator.language.includes("zh");
      function z() {
        ((o.loading = !0),
          ge("randomGenerate")
            .then((s) => {
              ((s.id = ""),
                (s.webrtc = "auto"),
                (s.language = "auto"),
                (s.timezone = "auto"),
                (s.location.lng = "auto"),
                (s.location.lat = "auto"),
                w(s));
            })
            .finally((s) => {
              o.loading = !1;
            }));
      }
      function c() {
        for (let s in o.formData.factors) {
          let e = (Math.random() * 10).toFixed(5);
          o.formData.factors[s] = Number(e);
        }
      }
      function r() {
        Y.push({ name: "Home" });
      }
      function b() {
        pe.confirm(d("warning"), "Warning", {
          confirmButtonText: d("qr"),
          cancelButtonText: d("qx"),
          type: "warning",
        })
          .then(() => {
            ((o.formData = JSON.parse(o.empty)),
              (o.formData.id = W.state.buildBrowser.id));
          })
          .catch(() => {});
      }
      function _() {
        let {
          name: s,
          userAgent: e,
          language: N,
          timezone: j,
          webrtc: m,
          location: P,
          webglInfo: G,
          screen: F,
          memoryCapacity: K,
          processors: L,
          factors: D,
          customVars: E,
          customProtos: y,
        } = o.formData;
        if (!s) {
          T.error(d("nameNotEmp"));
          return;
        }
        if (s.length > 30) {
          T.error(d("nameExceed"));
          return;
        }
        let p = W.state.buildBrowser;
        ((p.name = s),
          (p.userAgent = e),
          (p.language = N),
          (p.timezone = j),
          o.webrtcStatus === "auto" ? (p.webrtc = "auto") : (p.webrtc = m),
          o.timezoneStatus === "auto" && (p.timezone = "auto"),
          o.languageStatus === "auto" && (p.language = "auto"),
          (p.location = P || {}),
          o.lngStatus === "auto" && (p.location.lng = "auto"),
          o.latStatus === "auto" && (p.location.lat = "auto"),
          (p.webglInfo = G),
          (p.screen = F),
          (p.memoryCapacity = K),
          (p.processors = L),
          (p.factors = D),
          (p.customVars = []),
          (p.customProtos = y.map((a) => {
            for (let J = 0; J < a.properties.length; J++) {
              let Z = a.properties[J];
              if (!Z.key || (!Z.type && !Z.value)) {
                a.properties.splice(J, 1);
                continue;
              }
              if (Z.type !== "undefined") {
                let X = Z.value + "";
                (X === "undefined" || X === "null" || X === "") &&
                  a.properties.splice(J, 1);
              }
            }
            return (
              a.properties.length || y.splice(i, 1),
              { name: a.name, properties: a.properties }
            );
          })),
          p.customVars.push(...$.value.getEdited()),
          ye(p).then((a) => {
            (o.formData.id ? T.warning(d("xgcg")) : T.success(d("cjcg")),
              (o.formData.id = a),
              W.commit("build", p));
          }));
      }
      return (s, e) => {
        const N = f("el-icon"),
          j = f("el-button"),
          m = f("el-input"),
          P = f("el-radio-button"),
          G = f("el-radio-group"),
          F = f("el-switch"),
          K = f("el-popover"),
          L = f("el-option"),
          D = f("el-select"),
          E = f("el-input-number"),
          y = f("el-scrollbar"),
          p = se("loading");
        return ie(
          (g(),
          U("div", Ye, [
            l("div", Ze, [
              l("div", Qe, [
                l("div", Xe, [
                  l(
                    "div",
                    {
                      onClick: r,
                      class: "row",
                      style: {
                        "align-items": "center",
                        height: "50%",
                        "z-index": "6",
                        "font-size": "1.3rem",
                      },
                    },
                    [
                      n(N, null, { default: u(() => [n(t(ce))]), _: 1 }),
                      e[24] ||
                        (e[24] = l(
                          "span",
                          { style: { margin: "0 0.2rem" } },
                          "back",
                          -1,
                        )),
                      e[25] ||
                        (e[25] = l(
                          "span",
                          { style: { margin: "0 0.2rem" } },
                          "|",
                          -1,
                        )),
                      l(
                        "img",
                        {
                          src: t(we),
                          id: "avatar",
                          alt: "avatar",
                          style: {
                            width: "30px",
                            height: "30px",
                            "border-radius": "15px",
                          },
                        },
                        null,
                        8,
                        qe,
                      ),
                    ],
                  ),
                  l("div", et, [
                    l(
                      "div",
                      {
                        style: re([
                          {
                            "font-weight": "bold",
                            "text-align": "center",
                            width: "100%",
                          },
                          `color:${t(o).formData.id ? "#cb6706" : "#0493c7"};${t(H).curLang === "zh" ? "letter-spacing: 5px" : ""}`,
                        ]),
                      },
                      S(t(d)(t(o).formData.id ? "xg" : "xz")),
                      5,
                    ),
                    l("div", tt, [
                      t(o).formData.id
                        ? A("", !0)
                        : (g(),
                          k(
                            j,
                            {
                              key: 0,
                              size: "small",
                              plain: "",
                              type: "danger",
                              onClick: b,
                              style: { margin: "auto" },
                            },
                            { default: u(() => [C(S(t(d)("cj")), 1)]), _: 1 },
                          )),
                      t(o).formData.id
                        ? A("", !0)
                        : (g(),
                          k(
                            j,
                            {
                              key: 1,
                              size: "small",
                              plain: "",
                              type: "warning",
                              onClick: z,
                              style: { margin: "auto" },
                            },
                            { default: u(() => [C(S(t(d)("sj")), 1)]), _: 1 },
                          )),
                      n(
                        j,
                        {
                          size: "small",
                          plain: "",
                          type: "primary",
                          onClick: _,
                          style: { margin: "auto" },
                        },
                        { default: u(() => [C(S(t(d)("bc")), 1)]), _: 1 },
                      ),
                    ]),
                  ]),
                ]),
              ]),
            ]),
            l("div", lt, [
              n(
                y,
                { style: { width: "100%", height: "100%" } },
                {
                  default: u(() => [
                    l("div", ot, [
                      l("div", at, [
                        l("div", nt, [
                          n(
                            q,
                            { color: "#e10000", content: t(d)("namePmt") },
                            null,
                            8,
                            ["content"],
                          ),
                          e[26] ||
                            (e[26] = l(
                              "div",
                              { class: "big-label", style: { width: "35%" } },
                              self.__lbl("Name(名称):"),
                              -1,
                            )),
                        ]),
                        n(
                          m,
                          {
                            modelValue: t(o).formData.name,
                            "onUpdate:modelValue":
                              e[0] || (e[0] = (a) => (t(o).formData.name = a)),
                            style: { width: "100%" },
                            clearable: "",
                            class: "input",
                            placeholder: t(d)("inputName"),
                            maxlength: "20",
                            size: "small",
                            minlength: "1",
                          },
                          null,
                          8,
                          ["modelValue", "placeholder"],
                        ),
                      ]),
                      l("div", st, [
                        l("div", it, [
                          n(q, { content: t(d)("uaPmt") }, null, 8, [
                            "content",
                          ]),
                          e[27] ||
                            (e[27] = l(
                              "div",
                              { class: "big-label", style: { width: "50%" } },
                              self.__lbl(" UserAgent(客户端):"),
                              -1,
                            )),
                        ]),
                        n(
                          m,
                          {
                            modelValue: t(o).formData.userAgent,
                            "onUpdate:modelValue":
                              e[1] ||
                              (e[1] = (a) => (t(o).formData.userAgent = a)),
                            style: { width: "100%" },
                            clearable: "",
                            class: "input",
                            size: "small",
                            placeholder: t(d)("inputUA"),
                          },
                          null,
                          8,
                          ["modelValue", "placeholder"],
                        ),
                      ]),
                      l("div", rt, [
                        l("div", dt, [
                          n(q, { content: t(d)("ipPmt") }, null, 8, [
                            "content",
                          ]),
                          e[28] ||
                            (e[28] = l(
                              "div",
                              { class: "big-label", style: { width: "50%" } },
                              self.__lbl("Webrtc-IP(IP地址):"),
                              -1,
                            )),
                        ]),
                        l("div", ut, [
                          n(
                            G,
                            {
                              modelValue: t(o).webrtcStatus,
                              "onUpdate:modelValue":
                                e[2] || (e[2] = (a) => (t(o).webrtcStatus = a)),
                              style: { width: "38%" },
                              size: "small",
                            },
                            {
                              default: u(() => [
                                n(
                                  P,
                                  { label: "auto" },
                                  {
                                    default: u(() => [C(S(t(d)("dt")), 1)]),
                                    _: 1,
                                  },
                                ),
                                n(
                                  P,
                                  { label: "static" },
                                  {
                                    default: u(() => [C(S(t(d)("jt")), 1)]),
                                    _: 1,
                                  },
                                ),
                              ]),
                              _: 1,
                            },
                            8,
                            ["modelValue"],
                          ),
                          n(
                            m,
                            {
                              style: { width: "62%" },
                              disabled: t(o).webrtcStatus !== "static",
                              size: "small",
                              modelValue: t(o).formData.webrtc,
                              "onUpdate:modelValue":
                                e[3] ||
                                (e[3] = (a) => (t(o).formData.webrtc = a)),
                              clearable: "",
                              placeholder: t(d)("inputIP"),
                            },
                            null,
                            8,
                            ["disabled", "modelValue", "placeholder"],
                          ),
                        ]),
                      ]),
                      l("div", mt, [
                        e[30] ||
                          (e[30] = l(
                            "div",
                            { style: { width: "100%" }, class: "row" },
                            [
                              l(
                                "div",
                                { class: "big-label", style: { width: "50%" } },
                                self.__lbl("TimeZone(时区):"),
                              ),
                              l("div", { style: { width: "1%" } }),
                              l(
                                "div",
                                { class: "big-label", style: { width: "50%" } },
                                self.__lbl("Language(语言):"),
                              ),
                            ],
                            -1,
                          )),
                        l("div", pt, [
                          l("div", ct, [
                            n(
                              K,
                              {
                                placement: "top-start",
                                title: t(M)("{sm}："),
                                width: 200,
                                trigger: "hover",
                                content: t(d)("timeFL"),
                              },
                              {
                                reference: u(() => [
                                  n(
                                    F,
                                    {
                                      modelValue: t(o).timezoneStatus,
                                      "onUpdate:modelValue":
                                        e[4] ||
                                        (e[4] = (a) =>
                                          (t(o).timezoneStatus = a)),
                                      style: { width: "25%" },
                                      "inline-prompt": "",
                                      "active-value": "auto",
                                      "inactive-value": "static",
                                      "active-text": "Yes",
                                      "inactive-text": "Not",
                                    },
                                    null,
                                    8,
                                    ["modelValue"],
                                  ),
                                ]),
                                _: 1,
                              },
                              8,
                              ["title", "content"],
                            ),
                            n(
                              D,
                              {
                                modelValue: t(o).formData.timezone,
                                "onUpdate:modelValue":
                                  e[5] ||
                                  (e[5] = (a) => (t(o).formData.timezone = a)),
                                disabled: t(o).timezoneStatus !== "static",
                                placeholder: t(d)("selectTime"),
                                style: { height: "20%", width: "70%" },
                                filterable: "",
                                clearable: "",
                                size: "small",
                              },
                              {
                                default: u(() => [
                                  (g(!0),
                                  U(
                                    R,
                                    null,
                                    O(
                                      t(I),
                                      (a) => (
                                        g(),
                                        k(
                                          L,
                                          {
                                            value: a.timezone,
                                            label:
                                              self.__tzName(a) +
                                              "-" +
                                              a.timezone,
                                            style: { width: "90vw" },
                                          },
                                          {
                                            default: u(() => [
                                              l("div", ht, [
                                                l(
                                                  "div",
                                                  ft,
                                                  S(
                                                    self.__tzName(a),
                                                  ),
                                                  1,
                                                ),
                                                l("span", gt, S(a.timezone), 1),
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
                              ["modelValue", "disabled", "placeholder"],
                            ),
                          ]),
                          e[29] ||
                            (e[29] = l(
                              "div",
                              { style: { width: "1%" } },
                              null,
                              -1,
                            )),
                          l("div", yt, [
                            n(
                              K,
                              {
                                placement: "top-start",
                                title: t(M)("{sm}："),
                                width: 200,
                                trigger: "hover",
                                content: t(d)("langFL"),
                              },
                              {
                                reference: u(() => [
                                  n(
                                    F,
                                    {
                                      style: { width: "25%" },
                                      modelValue: t(o).languageStatus,
                                      "onUpdate:modelValue":
                                        e[6] ||
                                        (e[6] = (a) =>
                                          (t(o).languageStatus = a)),
                                      "inline-prompt": "",
                                      "active-value": "auto",
                                      "inactive-value": "static",
                                      "active-text": "Yes",
                                      "inactive-text": "Not",
                                    },
                                    null,
                                    8,
                                    ["modelValue"],
                                  ),
                                ]),
                                _: 1,
                              },
                              8,
                              ["title", "content"],
                            ),
                            n(
                              D,
                              {
                                modelValue: t(o).formData.language,
                                "onUpdate:modelValue":
                                  e[7] ||
                                  (e[7] = (a) => (t(o).formData.language = a)),
                                disabled: t(o).languageStatus !== "static",
                                placeholder: t(d)("selectLang"),
                                style: { height: "20%", width: "70%" },
                                filterable: "",
                                clearable: "",
                                size: "small",
                              },
                              {
                                default: u(() => [
                                  (g(!0),
                                  U(
                                    R,
                                    null,
                                    O(
                                      t(V),
                                      (a) => (
                                        g(),
                                        k(
                                          L,
                                          {
                                            value: a.code,
                                            label: a.name + "-" + a.code,
                                          },
                                          {
                                            default: u(() => [
                                              l("div", wt, [
                                                l("span", null, S(a.name), 1),
                                                l("span", vt, S(a.code), 1),
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
                              ["modelValue", "disabled", "placeholder"],
                            ),
                          ]),
                        ]),
                      ]),
                      l("div", bt, [
                        e[32] ||
                          (e[32] = l(
                            "div",
                            { style: { width: "100%" }, class: "row" },
                            [
                              l(
                                "div",
                                { class: "big-label", style: { width: "50%" } },
                                self.__lbl("Longitude(经度):"),
                              ),
                              l("div", { style: { width: "1%" } }),
                              l(
                                "div",
                                { class: "big-label", style: { width: "50%" } },
                                self.__lbl("Latitude(纬度):"),
                              ),
                            ],
                            -1,
                          )),
                        l("div", _t, [
                          l("div", Vt, [
                            n(
                              K,
                              {
                                placement: "top-start",
                                title: t(M)("{sm}："),
                                width: 200,
                                trigger: "hover",
                                content: t(d)("lngFL"),
                              },
                              {
                                reference: u(() => [
                                  n(
                                    F,
                                    {
                                      modelValue: t(o).lngStatus,
                                      "onUpdate:modelValue":
                                        e[8] ||
                                        (e[8] = (a) => (t(o).lngStatus = a)),
                                      style: { width: "25%" },
                                      "inline-prompt": "",
                                      "active-value": "auto",
                                      "inactive-value": "static",
                                      "active-text": "Yes",
                                      "inactive-text": "Not",
                                    },
                                    null,
                                    8,
                                    ["modelValue"],
                                  ),
                                ]),
                                _: 1,
                              },
                              8,
                              ["title", "content"],
                            ),
                            n(
                              E,
                              {
                                modelValue: t(o).formData.location.lng,
                                "onUpdate:modelValue":
                                  e[9] ||
                                  (e[9] = (a) =>
                                    (t(o).formData.location.lng = a)),
                                disabled: t(o).lngStatus !== "static",
                                placeholder: t(d)("inputLng"),
                                clearable: "",
                                "controls-position": "right",
                                size: "small",
                                style: { width: "70%" },
                              },
                              null,
                              8,
                              ["modelValue", "disabled", "placeholder"],
                            ),
                          ]),
                          e[31] ||
                            (e[31] = l(
                              "div",
                              { style: { width: "1%" } },
                              null,
                              -1,
                            )),
                          l("div", zt, [
                            n(
                              K,
                              {
                                placement: "top-start",
                                title: t(M)("{sm}："),
                                width: 200,
                                trigger: "hover",
                                content: t(d)("latFL"),
                              },
                              {
                                reference: u(() => [
                                  n(
                                    F,
                                    {
                                      style: { width: "25%" },
                                      modelValue: t(o).latStatus,
                                      "onUpdate:modelValue":
                                        e[10] ||
                                        (e[10] = (a) => (t(o).latStatus = a)),
                                      "inline-prompt": "",
                                      "active-value": "auto",
                                      "inactive-value": "static",
                                      "active-text": "Yes",
                                      "inactive-text": "Not",
                                    },
                                    null,
                                    8,
                                    ["modelValue"],
                                  ),
                                ]),
                                _: 1,
                              },
                              8,
                              ["title", "content"],
                            ),
                            n(
                              E,
                              {
                                modelValue: t(o).formData.location.lat,
                                "onUpdate:modelValue":
                                  e[11] ||
                                  (e[11] = (a) =>
                                    (t(o).formData.location.lat = a)),
                                disabled: t(o).latStatus !== "static",
                                placeholder: t(d)("inputLat"),
                                clearable: "",
                                "controls-position": "right",
                                size: "small",
                                style: { width: "70%" },
                              },
                              null,
                              8,
                              ["modelValue", "disabled", "placeholder"],
                            ),
                          ]),
                        ]),
                      ]),
                      l("div", xt, [
                        e[34] ||
                          (e[34] = l(
                            "div",
                            { style: { width: "100%" }, class: "row" },
                            [
                              l(
                                "div",
                                { class: "big-label", style: { width: "50%" } },
                                self.__lbl("Memory Capacity(内存容量/GB):"),
                              ),
                              l("div", { style: { width: "1%" } }),
                              l(
                                "div",
                                { class: "big-label", style: { width: "50%" } },
                                self.__lbl("Processors(处理器数量/个):"),
                              ),
                            ],
                            -1,
                          )),
                        l("div", Pt, [
                          n(
                            D,
                            {
                              modelValue: t(o).formData.memoryCapacity,
                              "onUpdate:modelValue":
                                e[12] ||
                                (e[12] = (a) =>
                                  (t(o).formData.memoryCapacity = a)),
                              placeholder: t(d)("selectMC"),
                              style: { width: "50%", "margin-top": "5px" },
                              filterable: "",
                              clearable: "",
                              size: "small",
                            },
                            {
                              default: u(() => [
                                (g(),
                                U(
                                  R,
                                  null,
                                  O([0.25, 0.5, 1, 2, 4, 8], (a) =>
                                    n(
                                      L,
                                      { value: a, label: a + "GB" },
                                      null,
                                      8,
                                      ["value", "label"],
                                    ),
                                  ),
                                  64,
                                )),
                              ]),
                              _: 1,
                            },
                            8,
                            ["modelValue", "placeholder"],
                          ),
                          e[33] ||
                            (e[33] = l(
                              "div",
                              { style: { width: "1%" } },
                              null,
                              -1,
                            )),
                          n(
                            E,
                            {
                              modelValue: t(o).formData.processors,
                              "onUpdate:modelValue":
                                e[13] ||
                                (e[13] = (a) => (t(o).formData.processors = a)),
                              min: 0,
                              placeholder: t(d)("inputNOP"),
                              size: "small",
                              style: { width: "50%", "margin-top": "5px" },
                            },
                            null,
                            8,
                            ["modelValue", "placeholder"],
                          ),
                        ]),
                      ]),
                      l("div", Dt, [
                        e[36] ||
                          (e[36] = l(
                            "div",
                            { class: "big-label" },
                            self.__lbl("GPU(显卡):"),
                            -1,
                          )),
                        l("div", St, [
                          e[35] ||
                            (e[35] = l(
                              "div",
                              { style: { width: "100%" }, class: "row" },
                              [
                                l(
                                  "div",
                                  {
                                    class: "small-label",
                                    style: {
                                      "text-align": "center",
                                      width: "50%",
                                    },
                                  },
                                  self.__lbl("Vendor(厂商):"),
                                ),
                                l(
                                  "div",
                                  {
                                    class: "small-label",
                                    style: {
                                      "text-align": "center",
                                      width: "50%",
                                    },
                                  },
                                  self.__lbl("Renderer(型号):"),
                                ),
                              ],
                              -1,
                            )),
                          l("div", Ut, [
                            n(
                              m,
                              {
                                modelValue:
                                  t(o).formData.webglInfo.UNMASKED_VENDOR_WEBGL,
                                "onUpdate:modelValue":
                                  e[14] ||
                                  (e[14] = (a) =>
                                    (t(
                                      o,
                                    ).formData.webglInfo.UNMASKED_VENDOR_WEBGL =
                                      a)),
                                clearable: "",
                                class: "input",
                                placeholder: t(d)("inputVendor"),
                                size: "small",
                                style: { width: "48%" },
                              },
                              null,
                              8,
                              ["modelValue", "placeholder"],
                            ),
                            n(
                              m,
                              {
                                modelValue:
                                  t(o).formData.webglInfo
                                    .UNMASKED_RENDERER_WEBGL,
                                "onUpdate:modelValue":
                                  e[15] ||
                                  (e[15] = (a) =>
                                    (t(
                                      o,
                                    ).formData.webglInfo.UNMASKED_RENDERER_WEBGL =
                                      a)),
                                clearable: "",
                                class: "input",
                                placeholder: t(d)("inputModel"),
                                size: "small",
                                style: { width: "48%" },
                              },
                              null,
                              8,
                              ["modelValue", "placeholder"],
                            ),
                          ]),
                        ]),
                      ]),
                      l("div", Nt, [
                        e[47] ||
                          (e[47] = l(
                            "div",
                            { class: "big-label" },
                            self.__lbl("Screen(屏幕):"),
                            -1,
                          )),
                        l("div", kt, [
                          e[37] ||
                            (e[37] = l(
                              "div",
                              {
                                class: "small-label",
                                style: { "line-height": "30px" },
                              },
                              self.__lbl("Noise(噪音):"),
                              -1,
                            )),
                          n(
                            E,
                            {
                              modelValue: t(o).formData.screen.noise,
                              "onUpdate:modelValue":
                                e[16] ||
                                (e[16] = (a) =>
                                  (t(o).formData.screen.noise = a)),
                              min: 0,
                              size: "small",
                              clearable: "",
                              placeholder: "seed",
                              "controls-position": "right",
                              disabled:
                                t(o).formData.screen.height ||
                                t(o).formData.screen.width,
                              style: { width: "50%" },
                            },
                            null,
                            8,
                            ["modelValue", "disabled"],
                          ),
                        ]),
                        l("div", Et, [
                          l("div", Ct, [
                            e[38] ||
                              (e[38] = l(
                                "div",
                                {
                                  class: "small-label",
                                  style: { "line-height": "30px" },
                                },
                                self.__lbl("Height(高):"),
                                -1,
                              )),
                            n(
                              E,
                              {
                                modelValue: t(o).formData.screen.height,
                                "onUpdate:modelValue":
                                  e[17] ||
                                  (e[17] = (a) =>
                                    (t(o).formData.screen.height = a)),
                                min: 0,
                                size: "small",
                                style: { width: "58%" },
                                clearable: "",
                                "controls-position": "right",
                                disabled: t(o).formData.screen.noise,
                              },
                              null,
                              8,
                              ["modelValue", "disabled"],
                            ),
                          ]),
                          e[40] ||
                            (e[40] = l(
                              "div",
                              { style: { width: "1%" } },
                              null,
                              -1,
                            )),
                          l("div", jt, [
                            e[39] ||
                              (e[39] = l(
                                "div",
                                {
                                  class: "small-label",
                                  style: { "line-height": "30px" },
                                },
                                self.__lbl("Width(宽):"),
                                -1,
                              )),
                            n(
                              E,
                              {
                                modelValue: t(o).formData.screen.width,
                                "onUpdate:modelValue":
                                  e[18] ||
                                  (e[18] = (a) =>
                                    (t(o).formData.screen.width = a)),
                                min: 0,
                                size: "small",
                                style: { width: "58%" },
                                clearable: "",
                                "controls-position": "right",
                                disabled: t(o).formData.screen.noise,
                              },
                              null,
                              8,
                              ["modelValue", "disabled"],
                            ),
                          ]),
                        ]),
                        l("div", $t, [
                          l("div", At, [
                            e[41] ||
                              (e[41] = l(
                                "div",
                                {
                                  class: "small-label",
                                  style: { "line-height": "30px" },
                                },
                                "ColorDepth:",
                                -1,
                              )),
                            n(
                              D,
                              {
                                modelValue: t(o).formData.screen.colorDepth,
                                "onUpdate:modelValue":
                                  e[19] ||
                                  (e[19] = (a) =>
                                    (t(o).formData.screen.colorDepth = a)),
                                style: { width: "58%" },
                                size: "small",
                                clearable: "",
                                placeholder: "",
                              },
                              {
                                default: u(() => [
                                  (g(),
                                  U(
                                    R,
                                    null,
                                    O([4, 6, 8, 10, 16, 24], (a) =>
                                      n(L, { value: a, label: a }, null, 8, [
                                        "value",
                                        "label",
                                      ]),
                                    ),
                                    64,
                                  )),
                                ]),
                                _: 1,
                              },
                              8,
                              ["modelValue"],
                            ),
                          ]),
                          e[43] ||
                            (e[43] = l(
                              "div",
                              { style: { width: "1%" } },
                              null,
                              -1,
                            )),
                          l("div", Bt, [
                            e[42] ||
                              (e[42] = l(
                                "div",
                                {
                                  class: "small-label",
                                  style: { "line-height": "30px" },
                                },
                                "PixelDepth:",
                                -1,
                              )),
                            n(
                              D,
                              {
                                modelValue: t(o).formData.screen.pixelDepth,
                                "onUpdate:modelValue":
                                  e[20] ||
                                  (e[20] = (a) =>
                                    (t(o).formData.screen.pixelDepth = a)),
                                style: { width: "58%" },
                                size: "small",
                                clearable: "",
                                placeholder: "",
                              },
                              {
                                default: u(() => [
                                  (g(),
                                  U(
                                    R,
                                    null,
                                    O([4, 6, 8, 10, 16, 24], (a) =>
                                      n(L, { value: a, label: a }, null, 8, [
                                        "value",
                                        "label",
                                      ]),
                                    ),
                                    64,
                                  )),
                                ]),
                                _: 1,
                              },
                              8,
                              ["modelValue"],
                            ),
                          ]),
                        ]),
                        l("div", Lt, [
                          e[44] ||
                            (e[44] = l(
                              "div",
                              {
                                class: "small-label",
                                style: { "line-height": "30px" },
                              },
                              self.__lbl("Dpr(像素比率):"),
                              -1,
                            )),
                          n(
                            E,
                            {
                              modelValue: t(o).formData.screen.dpr,
                              "onUpdate:modelValue":
                                e[21] ||
                                (e[21] = (a) => (t(o).formData.screen.dpr = a)),
                              min: 1,
                              max: 3,
                              step: 0.01,
                              size: "small",
                              clearable: "",
                              "controls-position": "right",
                              placeholder: "1 <-> 3",
                              style: { width: "50%" },
                            },
                            null,
                            8,
                            ["modelValue"],
                          ),
                        ]),
                        l("div", Tt, [
                          e[45] ||
                            (e[45] = l(
                              "div",
                              {
                                class: "small-label",
                                style: { "line-height": "30px" },
                              },
                              self.__lbl("TouchPoints(触摸点位):"),
                              -1,
                            )),
                          n(
                            E,
                            {
                              modelValue: t(o).formData.screen.maxTouchPoints,
                              "onUpdate:modelValue":
                                e[22] ||
                                (e[22] = (a) =>
                                  (t(o).formData.screen.maxTouchPoints = a)),
                              min: 0,
                              max: 10,
                              step: 1,
                              size: "small",
                              clearable: "",
                              "controls-position": "right",
                              style: { width: "50%" },
                            },
                            null,
                            8,
                            ["modelValue"],
                          ),
                        ]),
                        l("div", It, [
                          e[46] ||
                            (e[46] = l(
                              "div",
                              {
                                class: "small-label",
                                style: { "line-height": "30px" },
                              },
                              self.__lbl("ColorScheme(颜色主题):"),
                              -1,
                            )),
                          n(
                            D,
                            {
                              modelValue: t(o).formData.screen.scheme,
                              "onUpdate:modelValue":
                                e[23] ||
                                (e[23] = (a) =>
                                  (t(o).formData.screen.scheme = a)),
                              size: "small",
                              clearable: "",
                              style: { width: "50%" },
                              placeholder: "",
                            },
                            {
                              default: u(() => [
                                (g(),
                                U(
                                  R,
                                  null,
                                  O(["no-preference", "light", "dark"], (a) =>
                                    n(L, { value: a, label: a }, null, 8, [
                                      "value",
                                      "label",
                                    ]),
                                  ),
                                  64,
                                )),
                              ]),
                              _: 1,
                            },
                            8,
                            ["modelValue"],
                          ),
                        ]),
                      ]),
                      l("div", Rt, [
                        l("div", Ot, [
                          e[48] ||
                            (e[48] = l(
                              "div",
                              {
                                style: {
                                  "line-height": "28px",
                                  "margin-right": "5px",
                                },
                              },
                              self.__lbl("Random Seeds(随机数种子):"),
                              -1,
                            )),
                          n(j, {
                            onClick: c,
                            size: "small",
                            style: {
                              "margin-top": "3px",
                              "justify-self": "flex-end",
                            },
                            circle: "",
                            plain: "",
                            icon: "Refresh",
                          }),
                        ]),
                        l("div", Mt, [
                          (g(!0),
                          U(
                            R,
                            null,
                            O(
                              t(o).factorsKey,
                              (a) => (
                                g(),
                                U("div", Wt, [
                                  l("div", Gt, S(a.label + ":"), 1),
                                  n(
                                    E,
                                    {
                                      style: { width: "40%" },
                                      modelValue: t(o).formData.factors[a.key],
                                      "onUpdate:modelValue": (J) =>
                                        (t(o).formData.factors[a.key] = J),
                                      precision: 5,
                                      size: "small",
                                    },
                                    null,
                                    8,
                                    ["modelValue", "onUpdate:modelValue"],
                                  ),
                                ])
                              ),
                            ),
                            256,
                          )),
                        ]),
                      ]),
                      l("div", Ft, [
                        e[49] ||
                          (e[49] = l(
                            "div",
                            { class: "row big-label", style: { width: "80%" } },
                            [
                              l(
                                "div",
                                {
                                  style: {
                                    "line-height": "28px",
                                    "margin-right": "5px",
                                  },
                                },
                                self.__lbl("Custom Global Variable(自定义全局变量):"),
                              ),
                            ],
                            -1,
                          )),
                        n(
                          He,
                          {
                            ref_key: "vars",
                            ref: $,
                            "custom-vars": t(o).formData.customVars,
                          },
                          null,
                          8,
                          ["custom-vars"],
                        ),
                      ]),
                      l("div", Kt, [
                        e[50] ||
                          (e[50] = l(
                            "div",
                            { class: "row big-label", style: { width: "80%" } },
                            [
                              l(
                                "div",
                                {
                                  style: {
                                    "line-height": "28px",
                                    "margin-right": "5px",
                                  },
                                },
                                self.__lbl("Custom Prototype(自定义原型链):"),
                              ),
                            ],
                            -1,
                          )),
                        n(
                          Be,
                          {
                            ref_key: "protos",
                            ref: v,
                            "custom-protos": t(o).formData.customProtos,
                          },
                          null,
                          8,
                          ["custom-protos"],
                        ),
                      ]),
                      e[51] ||
                        (e[51] = l(
                          "div",
                          { style: { height: "10%" } },
                          null,
                          -1,
                        )),
                    ]),
                  ]),
                  _: 1,
                },
              ),
            ]),
          ])),
          [[p, t(o).loading]],
        );
      };
    },
  },
  Qt = ee(Jt, [["__scopeId", "data-v-c11f6f0e"]]);
export { Qt as default };

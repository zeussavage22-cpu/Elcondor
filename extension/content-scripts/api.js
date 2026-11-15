const m = self.chrome || self.browser,
  g = location.host.startsWith("localhost") ? "dev" : "",
  o = { port: void 0, times: 0 };
function x() {
  if (o.port) return o.port;
  const t = m.runtime.connect({ name: "popup_channel" });
  return (
    t.onMessage.addListener((e) => {
      let n = e.requestId,
        r = o[n];
      if ((delete o[n], !!r)) return y(e, r.resole, r.reject);
    }),
    t.onDisconnect.addListener(() => {
      o.port = void 0;
    }),
    (o.port = t),
    t
  );
}
setInterval(() => {
  for (let [t, e] of Object.entries(o))
    t === "port" ||
      t === "times" ||
      (Date.now() - e.start > 5e3 &&
        (delete o[t], y({ code: 500, msg: "Timeout!" }, e.resole, e.reject)));
}, 1e3);
function I(t = 1e3) {
  return new Promise((e) => setTimeout(e, t));
}
async function c(t, e) {
  return new Promise(async (n, r) => {
    var l, f;
    if (g === "dev") return n({ code: 200, data: void 0 });
    let a = Date.now(),
      i = a + "-" + o.times++;
    o[i] = { start: a, resole: n, reject: r };
    let d = await ((l = m.tabs) == null
        ? void 0
        : l.query({ active: !0, currentWindow: !0 })),
      u = 0;
    d && (u = ((f = d[0]) == null ? void 0 : f.id) || 0);
    for (let p = 0; p < 3; p++) {
      try {
        x().postMessage({ requestId: i, key: t, data: e, start: a, tabId: u });
        break;
      } catch (w) {
        w.message.toString().includes("Could not establish connection") &&
          (o.port = void 0);
      }
      await I(300);
    }
  });
}
function y(t, e, n) {
  return t.code !== 200 ? n(t.msg) : e(t.data);
}
const v = "CTHULHUJS_API",
  s = {
    randomBrowser: () => c("randomBrowser"),
    refreshIpInfo: () => c("refreshIpInfo"),
    clearBrowsingData: () => c("clearBrowsingData"),
    newMachine: async () => {
      (await s.refreshIpInfo(),
        await s.randomBrowser(),
        await s.clearBrowsingData());
    },
  };
self[v] = s;
window.addEventListener("message", async (t) => {
  // [MEJORA] Solo se aceptan mensajes de la propia ventana (no de iframes de
  // terceros) y con datos válidos.
  if (t.source !== window || !t.data || typeof t.data !== "object") return;
  let { target: e, data: n, key: r } = t.data;
  if (e === "cthulhuJs")
    switch (r) {
      case "randomBrowser":
        await s.randomBrowser();
        break;
      case "refreshIpInfo":
        await s.refreshIpInfo();
        break;
      case "newMachine":
        // [MEJORA] "Equipo nuevo" borra cookies, sesiones e historial. Antes
        // cualquier página podía ejecutarlo sin preguntar; ahora se pide
        // confirmación al usuario. El atajo de teclado sigue funcionando igual.
        if (
          !window.confirm(
            (navigator.language || "").startsWith("es")
              ? `La página ${location.host} pide a CthulhuJs ejecutar «Equipo nuevo» (borra cookies, sesiones e historial). ¿Permitirlo?`
              : `${location.host} asks CthulhuJs to run "New machine" (clears cookies, logins and history). Allow?`,
          )
        )
          break;
        await s.newMachine();
        break;
    }
});
function h(t, e = 3e3) {
  const n = document.createElement("div");
  n.style.cssText =
    "position: fixed; top: 20px; right: 20px; z-index: 2147483647;";
  const r = n.attachShadow({ mode: "open" }),
    a = document.createElement("div");
  a.textContent = t;
  const i = document.createElement("style");
  ((i.textContent = `
    div {
      background-color: #333;
      color: #fff;
      padding: 12px 24px;
      border-radius: 4px;
      font-family: system-ui, -apple-system, sans-serif;
      font-size: 14px;
      box-shadow: 0 4px 12px rgba(0,0,0,0.15);
      opacity: 0;
      transform: translateY(-20px);
      transition: all 0.3s ease;
    }
    div.show {
      opacity: 1;
      transform: translateY(0);
    }
  `),
    r.appendChild(i),
    r.appendChild(a),
    document.body.appendChild(n),
    requestAnimationFrame(() => {
      a.classList.add("show");
    }),
    setTimeout(() => {
      (a.classList.remove("show"),
        setTimeout(() => {
          n.remove();
        }, 300));
    }, e));
}
console.log("shortcuts");
c("getConfig", { key: "shortcuts" }).then((t) => {
  window.addEventListener("keydown", async function (e) {
    if (!t.switchEnv && !t.newMachine) return;
    const n = e.key.toUpperCase();
    if (e.ctrlKey && e.shiftKey && n === t.switchEnv) {
      (e.preventDefault(),
        e.stopPropagation(),
        await s.randomBrowser(),
        h("Switch Env Success！✅"));
      return;
    }
    if (e.ctrlKey && e.shiftKey && n === t.newMachine) {
      (e.preventDefault(),
        e.stopPropagation(),
        await s.newMachine(),
        h("New Machine Success！✅"));
      return;
    }
  });
});

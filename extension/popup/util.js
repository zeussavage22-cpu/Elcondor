function s(n, t) {
  let i = t.split(/[.\[\]]/).filter((l) => l !== ""),
    e = n;
  for (let l of i) {
    if (e[l] === void 0) return;
    e = e[l];
  }
  return e;
}
function f(n) {
  return JSON.parse(JSON.stringify(n));
}
function o(n = "") {
  let t = [],
    i = 0;
  for (let e = 0; e < n.length; e++) {
    let l = n[e];
    if (l === "." || l === "[") {
      let u = n.substring(i, e);
      (t.push(u), (i = e + 1));
      continue;
    }
    if (l === "]") {
      let u = n.substring(i, e);
      (t.push(Number(u)), (i = e + 1));
    }
  }
  if (i < n.length) {
    let e = n.substring(i);
    n[i - 1] === "[" ? t.push(Number(e)) : t.push(e);
  }
  return t;
}
function g(n = []) {
  let t = "";
  for (let i = 0; i < n.length; i++) {
    let e = n[i];
    e === "" ||
      e === void 0 ||
      e === null ||
      (Number.isInteger(e) ? (t += "[" + e + "]") : (t += e + "."));
  }
  return (t.endsWith(".") && (t = t.substring(0, t.length - 1)), t);
}
let r = new Date();
r.getFullYear() + "" + r.getMonth() + r.getDate();
export { g as a, f as c, s as g, o as s };

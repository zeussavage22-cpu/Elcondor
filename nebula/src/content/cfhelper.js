// Nébula — Cloudflare/anti-bot challenge helper (ISOLATED world, document_idle).
// Anti-fingerprinting inevitably conflicts with bot-verification challenges
// (they look for exactly the tampering we do). When a challenge is stuck, offer
// the user a one-tap way to disable Nébula on this site so the real browser
// passes. This NEVER tries to defeat the challenge — it turns protection OFF.

(() => {
  const CONFIG_KEY = 'nebula:config';

  function isChallenge() {
    try {
      const t = (document.title || '').toLowerCase();
      if (t.includes('just a moment') || t.includes('un momento') || t.includes('attention required')) return true;
      if (document.querySelector('#challenge-running, #cf-challenge-running, #challenge-form, script[src*="challenges.cloudflare.com"], iframe[src*="challenges.cloudflare.com"], #px-captcha, [id^="px-"]')) return true;
      const b = document.body ? (document.body.innerText || '').toLowerCase() : '';
      if (/verifying you are human|verificando que eres (humano|un ser humano)|checking (your|if the) browser|needs to review the security|un momento…?/.test(b)) return true;
    } catch {}
    return false;
  }

  function showBanner() {
    if (document.getElementById('nebula-cf-banner')) return;
    const bar = document.createElement('div');
    bar.id = 'nebula-cf-banner';
    bar.style.cssText = 'position:fixed;left:0;right:0;bottom:0;z-index:2147483647;background:#171a28;color:#e8eaf2;font:14px/1.45 system-ui,-apple-system,sans-serif;padding:14px 16px;box-shadow:0 -6px 24px rgba(0,0,0,.45);display:flex;gap:12px;align-items:center;justify-content:space-between;flex-wrap:wrap;border-top:2px solid #7b7bff';
    const msg = document.createElement('span');
    msg.innerHTML = '◓ <b>Nébula</b> puede estar interfiriendo con la verificación (Cloudflare) de este sitio.';
    const right = document.createElement('div');
    right.style.cssText = 'display:flex;gap:8px;align-items:center';
    const btn = document.createElement('button');
    btn.textContent = 'Desactivar aquí y recargar';
    btn.style.cssText = 'background:linear-gradient(90deg,#7b7bff,#39d8c8);color:#0c0e16;border:none;border-radius:10px;padding:10px 14px;font-weight:700;cursor:pointer;font-family:inherit';
    btn.onclick = () => {
      try { chrome.runtime.sendMessage({ type: 'allowlistSite', host: location.hostname }, () => { setTimeout(() => location.reload(), 150); }); }
      catch { location.reload(); }
    };
    const close = document.createElement('button');
    close.textContent = '✕';
    close.style.cssText = 'background:transparent;color:#9fa3c0;border:none;font-size:18px;cursor:pointer;line-height:1';
    close.onclick = () => bar.remove();
    right.appendChild(btn); right.appendChild(close);
    bar.appendChild(msg); bar.appendChild(right);
    (document.body || document.documentElement).appendChild(bar);
  }

  (async () => {
    try {
      const c = (await chrome.storage.local.get(CONFIG_KEY))[CONFIG_KEY];
      if (!c || !c.enabled || c.cfHelper === false) return; // off or already allowlisted
    } catch { return; }
    // If a challenge is still showing a few seconds in, it's likely stuck.
    setTimeout(() => { if (isChallenge()) showBanner(); }, 4500);
    setTimeout(() => { if (isChallenge()) showBanner(); }, 10000);
  })();
})();

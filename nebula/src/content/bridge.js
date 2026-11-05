// Nébula — bridge (ISOLATED world).
// Content scripts in the ISOLATED world can read chrome.storage; the MAIN-world
// protection script cannot. This relays the resolved config across the world
// boundary using a DOM CustomEvent (strings cross cleanly), and keeps it live.

(() => {
  const CONFIG_KEY = 'nebula:config';
  const EVT_CONFIG = 'nebula:config';
  const EVT_READY = 'nebula:ready';

  // Robustness: some Chromium-based mobile browsers ignore the manifest's
  // content_scripts "world":"MAIN" and would run protect.js in the isolated
  // world, where our overrides never reach the page. Inject it as a <script>
  // element too; the __nebulaInstalled guard prevents a double install when
  // the MAIN-world content script already ran.
  try {
    const url = chrome.runtime.getURL('src/content/protect.js');
    const s = document.createElement('script');
    s.src = url;
    s.async = false;
    s.setAttribute('data-nebula', '1');
    (document.head || document.documentElement || document).appendChild(s);
    s.onload = () => { try { s.remove(); } catch {} };
  } catch {}

  function hostname() {
    try {
      return location.hostname || (top && top.location && top.location.hostname) || 'about';
    } catch {
      return 'about';
    }
  }

  function isAllowlisted(host, allowlist) {
    if (!host || !allowlist) return false;
    return allowlist.some((d) => host === d || host.endsWith('.' + d));
  }

  function send(config) {
    const host = hostname();
    let payload;
    if (!config || !config.enabled || isAllowlisted(host, config.allowlist)) {
      payload = { enabled: false };
    } else {
      payload = {
        enabled: true,
        host,
        noise: config.noise,
        noiseStrategy: config.noiseStrategy,
        protections: config.protections,
        webrtcMode: config.webrtcMode,
        timingPrecisionMs: config.timingPrecisionMs,
        baseSeed: config.baseSeed,
        profile: config.profile
      };
    }
    try {
      window.dispatchEvent(new CustomEvent(EVT_CONFIG, { detail: JSON.stringify(payload) }));
    } catch {}
  }

  async function load() {
    try {
      const got = await chrome.storage.local.get(CONFIG_KEY);
      send(got[CONFIG_KEY]);
    } catch {
      send(null);
    }
  }

  // MAIN may boot before we deliver config and will ask for it.
  window.addEventListener(EVT_READY, load, true);

  // Live updates when settings change in the popup/options.
  try {
    chrome.storage.onChanged.addListener((changes, area) => {
      if (area === 'local' && changes[CONFIG_KEY]) send(changes[CONFIG_KEY].newValue);
    });
  } catch {}

  load();
})();

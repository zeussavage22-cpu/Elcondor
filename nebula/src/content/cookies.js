// Nébula — cookie-consent banner auto-dismisser (ISOLATED world, document_idle).
// Hides common CMP banners via CSS and clicks their "reject"/"accept" button so
// the choice is recorded. Curated, conservative selectors to avoid false hits.

(() => {
  const CONFIG_KEY = 'nebula:config';

  // Specific, well-known consent banner containers/overlays.
  const HIDE = [
    '#onetrust-banner-sdk', '#onetrust-consent-sdk', '.onetrust-pc-dark-filter', '.ot-sdk-row',
    '#CybotCookiebotDialog', '#CybotCookiebotDialogBodyUnderlay',
    '#usercentrics-root', '#usercentrics-cmp-ui',
    '#didomi-host', '.didomi-popup-open', '.didomi-consent-popup',
    '#cmpbox', '#cmpwrapper', '.cmp-container', '.cmpboxBG',
    '.fc-consent-root', '.fc-dialog-overlay',
    '.qc-cmp2-container', '.qc-cmp-cleanslate',
    '#sp_message_container_', '[id^="sp_message_container_"]',
    '.cc-window', '.cc-banner', '.cookie-notice', '#cookie-notice', '#cookie-law-info-bar',
    '#cookie-banner', '.cookie-banner', '#cookiebanner', '#cookieConsent', '#cookie-consent',
    '.gdpr-banner', '#gdpr-cookie-message', '.js-cookie-consent', '#truste-consent-track',
    '#iubenda-cs-banner', '.iubenda-cs-container', '.osano-cm-window', '.termly-styles-root'
  ];

  // Prefer "reject" buttons; fall back to "accept" so the banner goes away either way.
  const REJECT = [
    '#onetrust-reject-all-handler', '.ot-pc-refuse-all-handler',
    '#CybotCookiebotDialogBodyButtonDecline', '#CybotCookiebotDialogBodyLevelButtonLevelOptinDeclineAll',
    '.didomi-continue-without-agreeing', '#didomi-notice-disagree-button',
    '.fc-cta-do-not-consent', '.qc-cmp2-summary-buttons > button[mode="secondary"]',
    '.iubenda-cs-reject-btn', '.osano-cm-denyAll', 'button.cmptxt_btn_no',
    'button[aria-label*="reject" i]', 'button[aria-label*="decline" i]',
    'button[title*="reject" i]', 'button[title*="rechazar" i]'
  ];
  const ACCEPT = [
    '#onetrust-accept-btn-handler', '#CybotCookiebotDialogBodyLevelButtonLevelOptinAllowAll',
    '#CybotCookiebotDialogBodyButtonAccept', '.fc-cta-consent', '#didomi-notice-agree-button',
    '.qc-cmp2-summary-buttons > button[mode="primary"]', '.iubenda-cs-accept-btn', '.osano-cm-acceptAll',
    '.cc-allow', '.cc-dismiss', 'button.cmptxt_btn_yes',
    'button[aria-label*="accept" i]', 'button[title*="accept" i]', 'button[title*="aceptar" i]'
  ];

  function hide() {
    try {
      const style = document.createElement('style');
      style.setAttribute('data-nebula-cookies', '1');
      style.textContent = HIDE.join(',') + '{display:none !important;visibility:hidden !important;opacity:0 !important;pointer-events:none !important;}'
        + 'html.didomi-popup-open,body.didomi-popup-open,body.cmp--open,html.cmp--open{overflow:auto !important;}';
      (document.head || document.documentElement).appendChild(style);
    } catch {}
  }

  function clickOne(list) {
    for (const sel of list) {
      let el;
      try { el = document.querySelector(sel); } catch { continue; }
      if (el && el.offsetParent !== null) { try { el.click(); return true; } catch {} }
    }
    return false;
  }

  function run() {
    hide();
    let tries = 0;
    const tick = () => {
      if (!clickOne(REJECT)) clickOne(ACCEPT);
      if (++tries < 6) setTimeout(tick, 700);
    };
    tick();
  }

  (async () => {
    try {
      const c = (await chrome.storage.local.get(CONFIG_KEY))[CONFIG_KEY];
      if (c && c.enabled && c.cookieBanners) run();
    } catch {}
  })();
})();

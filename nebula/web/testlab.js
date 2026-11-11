// Nébula Test Lab — live, in-page fingerprint probes (no data leaves your browser).
(() => {
  async function sha(str) {
    const buf = await crypto.subtle.digest('SHA-256', new TextEncoder().encode(str));
    return [...new Uint8Array(buf)].map((b) => b.toString(16).padStart(2, '0')).join('').slice(0, 24);
  }
  function canvasFP() {
    const c = document.createElement('canvas'); c.width = 240; c.height = 60;
    const x = c.getContext('2d');
    x.textBaseline = 'top'; x.font = '16px Arial';
    x.fillStyle = '#f60'; x.fillRect(10, 10, 120, 30);
    x.fillStyle = '#069'; x.fillText('Nébula Lab 😃 1234', 14, 16);
    x.strokeStyle = 'rgba(120,0,200,.5)'; x.beginPath(); x.arc(60, 30, 20, 0, 7); x.stroke();
    return c.toDataURL();
  }
  function webglFP() {
    try {
      const gl = document.createElement('canvas').getContext('webgl');
      const d = gl.getExtension('WEBGL_debug_renderer_info');
      return (d ? gl.getParameter(d.UNMASKED_VENDOR_WEBGL) + ' · ' + gl.getParameter(d.UNMASKED_RENDERER_WEBGL) : 'n/a');
    } catch { return 'n/a'; }
  }
  async function audioFP() {
    try {
      const ctx = new (window.OfflineAudioContext || window.webkitOfflineAudioContext)(1, 44100, 44100);
      const osc = ctx.createOscillator(); osc.type = 'triangle'; osc.frequency.value = 10000;
      const comp = ctx.createDynamicsCompressor(); osc.connect(comp); comp.connect(ctx.destination); osc.start(0);
      const buf = await ctx.startRendering();
      const data = buf.getChannelData(0).slice(4000, 4100);
      let s = 0; for (const v of data) s += Math.abs(v);
      return s.toString();
    } catch { return 'n/a'; }
  }
  function row(k, v, cls) { return `<tr><td>${k}</td><td class="${cls || ''}">${v}</td></tr>`; }

  async function run() {
    const tb = document.querySelector('#tbl tbody');
    tb.innerHTML = '<tr><td>Midiendo…</td><td></td></tr>';
    const gpu = webglFP();
    const rows = [];
    rows.push(row('Canvas (hash)', await sha(canvasFP()), 'hash'));
    rows.push(row('WebGL', gpu));
    rows.push(row('WebGL (hash)', await sha(gpu), 'hash'));
    rows.push(row('Audio (hash)', await sha(await audioFP()), 'hash'));
    rows.push(row('User-Agent', navigator.userAgent));
    rows.push(row('Plataforma', navigator.platform));
    rows.push(row('Idioma(s)', (navigator.languages || []).join(', ')));
    rows.push(row('Pantalla', `${screen.width}×${screen.height} @${window.devicePixelRatio}x · ${screen.colorDepth}bit`));
    rows.push(row('Zona horaria', Intl.DateTimeFormat().resolvedOptions().timeZone));
    rows.push(row('Offset horario', new Date().getTimezoneOffset()));
    rows.push(row('Núcleos / RAM', `${navigator.hardwareConcurrency} · ${navigator.deviceMemory || 'n/d'} GB`));
    rows.push(row('WebDriver', String(navigator.webdriver)));
    rows.push(row('Do Not Track', String(navigator.doNotTrack)));
    rows.push(row('Plugins', [...navigator.plugins].map((p) => p.name).join(', ') || '(ninguno)'));
    tb.innerHTML = rows.join('');
  }
  document.getElementById('run').addEventListener('click', run);
  run();
})();

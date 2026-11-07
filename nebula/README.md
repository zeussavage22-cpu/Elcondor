# ◓ Nébula — Anti-Fingerprint Shield

Extensión de navegador **Manifest V3** (Chrome / Edge / Brave / cualquier Chromium) que protege tu
privacidad frente al **browser fingerprinting**: la técnica con la que los sitios web y las redes
publicitarias construyen una "huella" única de tu navegador (canvas, WebGL, audio, fuentes,
User-Agent, zona horaria, hardware…) para rastrearte **aunque borres las cookies o uses modo
incógnito**.

Nébula no te hace invisible —eso no existe—, pero sí **rompe la unicidad y la estabilidad** de tu
huella: añade ruido determinista por sitio a las señales más delatoras y presenta una identidad
coherente y común, de modo que seas mucho más difícil de identificar y de seguir entre sesiones y
entre sitios.

100% **código abierto**, licencia **MIT**. Sin telemetría, sin servidores, sin recoger nada.

---

## ✨ Qué protege

| Categoría | Qué hace |
|---|---|
| **Canvas 2D** | Ruido determinista por origen en `getImageData` / `toDataURL` / `toBlob`. Repetir la lectura da el mismo resultado (no detectable por doble-lectura), pero difiere entre sitios y sesiones. |
| **WebGL / WebGL2** | Falsea `UNMASKED_VENDOR/RENDERER` (GPU) y añade ruido a `readPixels`. |
| **Audio** | Ruido mínimo en `AudioBuffer.getChannelData`, `copyFromChannel` y `AnalyserNode`. |
| **Fuentes** | Jitter en `measureText` para frenar la enumeración de fuentes instaladas. |
| **Client Rects** | Ruido sub-píxel en `getBoundingClientRect` (fingerprinting por layout y fuentes). |
| **User-Agent** | `userAgent`, `appVersion`, `platform`, `vendor` coherentes **y sincronizados con las cabeceras HTTP**. |
| **Client Hints** | `navigator.userAgentData` + `getHighEntropyValues()` + cabeceras `Sec-CH-UA-*`. |
| **Hardware** | `hardwareConcurrency`, `deviceMemory`, `maxTouchPoints`. |
| **Pantalla** | `screen.*`, `devicePixelRatio`, profundidad de color. |
| **Zona horaria** | `Date.getTimezoneOffset()` e `Intl.DateTimeFormat().resolvedOptions().timeZone`. |
| **Idioma** | `navigator.language(s)` y locale de `Intl`, coherente con `Accept-Language`. |
| **WebRTC** | Evita la fuga de tu **IP local** por ICE candidates (modos: seguro / solo relay / desactivado). |
| **Batería / Dispositivos / Voces / Red** | Valores fijos y plausibles en `getBattery`, `enumerateDevices`, `speechSynthesis`, `navigator.connection`. |
| **Plugins** | Expone el set estándar de Chrome (ni vacío ni raro). |
| **WebDriver** | `navigator.webdriver = false`. |
| **Precisión de reloj** | Reduce la resolución de `performance.now()` contra el fingerprinting por *timing*. |
| **Bloqueador de rastreadores** | Bloquea (capa de red, vía DNR) ~55 dominios de analytics/ads/session-replay conocidos (Google Analytics, Doubleclick, Facebook, Hotjar, Segment…). |
| **Sensores / enumeración** | Oculta `getGamepads`, WebUSB, Web Serial, WebHID, Web Bluetooth y `keyboard.getLayoutMap`. |
| **WebGPU** | Oculta `navigator.gpu` (nuevo vector de huella por GPU). |
| **Do Not Track / GPC** | Señal `doNotTrack`/`globalPrivacyControl` + cabeceras `DNT: 1` y `Sec-GPC: 1`. |
| **Almacenamiento** | `storage.estimate()` devuelve una cuota fija (anti-huella por tamaño de disco). |
| **Coherencia Safari** | Oculta `window.chrome` en perfiles Safari (que no lo tienen). |
| **Precisión de reloj** | Reduce la resolución de `performance.now()` contra el fingerprinting por *timing*. |
| **Anti-detección** | Todas las funciones interceptadas devuelven `[native code]` en `toString()`, con `name`/`length` correctos, para que los sitios no detecten el escudo. |

## 🧠 Filosofía de diseño

- **Coherencia > caos.** Una huella contradictoria (UA de Windows + GPU de Mac) es *más* única, no
  menos. Nébula expone un perfil internamente consistente en JS **y** en las cabeceras HTTP.
- **Ruido determinista por origen.** Los hashes de canvas/audio/WebGL son estables dentro de una
  sesión y un sitio (no se detecta por doble-lectura) pero **distintos en cada sitio**, así tu
  canvas no puede enlazarte entre dominios.
- **Mimetizar o aleatorizar, tú eliges.**
  - *Mimetizar (blend):* pareces la configuración más común → menos entropía.
  - *Aleatorio por sesión:* nueva identidad en cada arranque del navegador.
  - *Aleatorio por sitio:* el ruido de las señales enlazables cambia por sitio.
  - *Perfil personalizado:* edita cada valor en JSON.

## 📦 Instalación (modo desarrollador)

1. Descarga/clona esta carpeta `nebula/`.
2. Ve a `chrome://extensions`.
3. Activa **Modo de desarrollador** (arriba a la derecha).
4. **Cargar descomprimida** → selecciona la carpeta `nebula/`.
5. Fija el icono ◓ y abre el popup. Atajo: **Ctrl+Shift+N**.

> Compatible con Chrome, Edge, Brave, Opera y Vivaldi (todos Chromium, MV3).

## 🧪 Comprobar que funciona

- Abre `nebula/test/fingerprint-test.html` en el navegador.
- Compara con la extensión activada/desactivada.
- Pulsa **↻ Generar nueva identidad** en el popup y recarga: los hashes deben cambiar.
- Sitios externos: [coveryourtracks.eff.org](https://coveryourtracks.eff.org/),
  [browserleaks.com](https://browserleaks.com/), [amiunique.org](https://amiunique.org/).

## ⚙️ Arquitectura

```
manifest.json            Manifest V3
src/
  background.js          Service worker: ajustes, semilla, perfil, cabeceras DNR
  lib/
    profiles.js          Presets realistas + generador aleatorio coherente
    rng.js               PRNG con semilla (mulberry32 + xmur3)
  content/
    bridge.js            (mundo ISOLATED) lee storage y pasa config al MAIN
    protect.js           (mundo MAIN) instala todos los hooks de protección
popup/                   Control rápido (on/off, modo, identidad, toggles)
options/                 Ajustes avanzados (ruido, WebRTC, lista blanca, perfil JSON, backup)
test/                    Página de prueba de huella
```

**Flujo:** el service worker resuelve un perfil coherente y lo publica en `storage.local`, además de
reescribir las cabeceras HTTP (`User-Agent`, `Accept-Language`, `Sec-CH-UA-*`) con
`declarativeNetRequest`. En cada página, `bridge.js` (mundo aislado) lee esa config y la entrega por
un `CustomEvent` a `protect.js` (mundo principal), que ya tenía los hooks instalados desde
`document_start` y empieza a aplicar la protección.

## 🔒 Privacidad

Nébula **no** tiene analítica, **no** rastrea nada y **no** envía tus datos a ningún sitio. Todo
ocurre localmente en tu navegador y puedes auditar cada línea.

**Única excepción — "Zona horaria según mi IP":** si activas esta opción (lo está por defecto), la
extensión consulta un servicio de geo-IP (ipapi.co, con respaldo ipwho.is / worldtimeapi.org) para
saber la zona horaria de tu IP y hacer que **coincida** con lo que geolocalizan los sitios (evita que
tu zona JS contradiga tu IP, algo que los sistemas antifraude detectan). Esa consulta solo envía lo
normal de cualquier petición (tu IP, que el servicio ya ve) y se cachea 1 hora. Si prefieres cero
peticiones de red, cambia «Zona horaria» a **«Región aleatoria»** y Nébula no contactará con nadie.

## ⚠️ Limitaciones honestas

- El fingerprinting es una carrera armamentística; ninguna herramienta es perfecta.
- Falsear señales a veces rompe sitios. Usa la **lista blanca** para tu banco o cualquier sitio que
  falle, o desactiva protecciones concretas.
- La protección de WebRTC reduce fugas de IP local pero no sustituye a una VPN/proxy para la IP
  pública.
- Para máxima resistencia considera también **Tor Browser** o **Brave**; Nébula es una capa extra
  muy configurable para tu Chromium de diario.

## 📄 Licencia

MIT © 2026. Hecho para uso personal y comunitario. Haz un fork, mejóralo, compártelo.

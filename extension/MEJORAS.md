# CthulhuJs (Anti-Fingerprint) · versión mejorada

Basada en la versión **9.0.5** de la Chrome Web Store
(ID `pmcpffnpjncfplinfnjebjoonbncnjfl`). Se conservan **todas** las funciones
originales: perfiles de navegador, modo seguro, "equipo nuevo", lista blanca y
lista negra, atajos, importar y exportar, sitios de prueba, etc. Cada cambio en
el código está marcado con el comentario `[MEJORA]`.

## Correcciones de errores

| # | Problema en la original | Solución |
|---|---|---|
| 1 | **Fecha de caducidad oculta**: a partir del 31/12/2029 la extensión dejaba de leer y guardar su configuración (`"1_8_9_3_5_1_3_5_9_9_0_0_0"` en `background.js` y `logo.js`). | Eliminada. |
| 2 | Si al iniciar el navegador **no había internet**, o los servicios de IP fallaban, la extensión **no se activaba**. | Si la consulta falla, se usan la zona horaria y el idioma locales, y la protección se activa igualmente. |
| 3 | `setHostEnable` (modo lista blanca/negra) **siempre daba error**, porque usaba `e in [...]`, que compara índices y no valores. | Corregido con `.includes()`; además se vuelve a aplicar el perfil actual. |
| 4 | `navigator.language` devolvía textos inválidos como `"es-ES,es"` cuando había varios idiomas. | Ahora devuelve solo el primer idioma, como un navegador real. |
| 5 | Los UA aleatorios estaban **mal formados**: `Chrome/537.36`, `Gecko/537.36 Firefox/…`, Chrome de Android con formato de iOS (`CriOS`), y "mac os undefined" cuando el sistema era macOS. | Generador nuevo con el formato real que usan los navegadores actuales. |
| 6 | Combinaciones imposibles: Safari en Windows o Linux, Windows XP/7, Chrome 76–123 (muy viejo), GPU de Windows (Direct3D) en un Mac, GPU SwiftShader (típica de bots y máquinas virtuales). | El navegador, la versión y la GPU se eligen según el sistema simulado. |
| 7 | Valores de pantalla y hardware sospechosos: profundidad de color de 4 o 6 bits, `pixelDepth` distinto de `colorDepth`, DPR como `1.37`, `deviceMemory` de 0.25 GB en PC, 1 núcleo de CPU. | Valores realistas según el dispositivo y el SO. |
| 8 | Con "cambiar al abrir el navegador", el perfil cambiaba **cada vez que Chrome reiniciaba internamente** el service worker, no solo al abrir el navegador. | Solo cambia una vez por sesión del navegador. |
| 9 | Notificación "Injected" repetida en cada reinicio interno. | Una sola vez por sesión, y se puede desactivar. |
| 10 | Cabecera `Accept-Language` con un solo idioma (`es-ES`), algo que ningún navegador real envía. | `es-ES,es;q=0.9`, igual que Chrome. |
| 11 | `Sec-Ch-Ua-Platform-Version` con valores inválidos como `"x86_64.0.0"` en Linux. | Solo se envían versiones numéricas. |
| 12 | Los menús de zona horaria siempre salían en chino. | Salen en el idioma de la interfaz. |
| 13 | Código de idioma `ja-jp` y códigos inexistentes (`ru-MI`, `ru-mo`, `ro-MO`). | Corregidos a `ja-JP`, `ru-RU` y `ru-MD`/`ro-MD`, conservando el mismo orden. |
| 14 | Datos de GPU para Linux mezclados con nombres de GPU de Windows (Direct3D). | Separados: cada sistema solo usa sus propias GPU. |
| 15 | Si `os.version` no existía, la preparación del perfil en la página podía fallar (`undefined.split`). | Protegido. |

## Privacidad

- **Nueva opción "Consultar IP en línea"** (activada por defecto, como en la
  original). Si se desactiva, la extensión **no se conecta a ningún servicio
  externo**.
- Las consultas de IP ya **no envían cookies** (`credentials: "omit"`). Antes,
  la de *nexkits.com* usaba `credentials: "include"` y además mandaba una
  cabecera `Accept-Language` china falsa y un *referrer* inventado.
- El resultado se guarda **10 minutos** en memoria de sesión. Antes se
  consultaba la IP cada vez que se despertaba el service worker. El botón
  "Actualizar red" sigue forzando una consulta nueva.
- Se añadió un tercer proveedor de respaldo (*ipwho.is*), así la extensión no
  depende de uno solo.

## Funciones nuevas (en Ajustes)

| Opción | Por defecto | Qué hace |
|---|---|---|
| Consultar IP en línea | Activada | Permite desactivar todas las conexiones externas. |
| Perfiles realistas | Activada | UA, GPU, memoria, CPU y pantalla coherentes con el sistema simulado. Desactívala para volver al algoritmo original, totalmente aleatorio. |
| Huella distinta por sitio | Desactivada | Cada sitio (dominio principal) recibe un ruido de canvas, audio, WebGL, fuentes, etc. distinto pero **estable**. Así dos sitios no pueden cruzar tu huella, y un mismo sitio no ve cambios que lo hagan sospechar. |
| Notificaciones | Activada | Muestra o no la notificación de estado al iniciar. |

## Interfaz

- **Traducción completa al español** (167 textos), en `popup/es.js`. Se elige
  en el menú "Idioma → Español". Si el navegador está en español, se activa
  sola.
- Las etiquetas fijas del formulario de perfiles (`Name(名称):`,
  `GPU(显卡):`…) también se muestran en español.
- Errores del service worker en español, chino o inglés según el navegador.
  Antes muchos salían solo en chino.
- Lista de idiomas ampliada (es-ES, es-US, fr-FR, de-DE, it-IT, ru-RU, pt…),
  con nombres en español y en chino.
- Perfiles de GPU nuevos y reales: Intel, AMD y NVIDIA en Windows; Apple
  M1–M4; Adreno y Mali en Android; Apple GPU en iOS.

## Auditoría de seguridad

Se revisó todo el código propio de la extensión: `background.js`,
`content-scripts/api.js`, `content-scripts/window.js`, `popup/logo.js`,
`Home.js`, `Build.js` y `Strategy.js`. `popup.js` es la librería Vue +
Element Plus empaquetada.

- **No hay malware**: no envía tu historial, tus cookies ni tus datos a
  ningún servidor.
- Las únicas conexiones externas son las consultas de IP descritas arriba.
- Usa `eval`/`new Function` solo en el polyfill estándar de `globalThis`.
- **Corregido (importante):** `content-scripts/api.js` escucha mensajes
  `postMessage` de las páginas. En la original, **cualquier página web podía
  ordenar "Equipo nuevo"**, que **borra todas tus cookies, sesiones e
  historial**, sin preguntarte. Ahora solo se aceptan mensajes de la propia
  ventana (no de iframes de terceros), y "Equipo nuevo" pide confirmación.
  Cambiar de perfil o refrescar la IP por mensaje siguen funcionando, porque
  la original lo usa para automatización.
- Permisos amplios (`<all_urls>`, `userScripts`, `browsingData`): son
  necesarios para su función, y por eso solo deberías instalar extensiones
  así de fuentes en las que confíes.

## Cómo instalarla

1. Abre `chrome://extensions` y activa el **Modo de desarrollador**, arriba a
   la derecha.
2. Pulsa **Cargar descomprimida** y elige la carpeta `extension/`.
3. En los detalles de la extensión, activa **Permitir scripts de usuario**
   (*Allow user scripts*). En Chrome 138 o posterior es obligatorio.
4. Desinstala la versión original de la Web Store para que no choquen.

> No se actualiza sola desde la Web Store: es tu propia copia.

## Pruebas realizadas

Se probó en Chromium con la extensión cargada:

- La interfaz carga sin errores en español (Inicio, Ajustes y Crear perfil).
- La inyección funciona en las páginas: UA, idioma (`es-ES` y `["es-ES","es"]`),
  memoria, CPU y GPU.
- Sin conexión a internet, la extensión se activa igualmente con los datos
  locales.
- Se generaron 30 perfiles aleatorios de Windows, macOS, Linux, Android e iOS:
  todos con UA moderno y bien formado, y con una GPU que corresponde al
  sistema.
- "Huella por sitio": cada dominio recibe una semilla y un ruido distintos, y
  el mismo dominio recibe siempre los mismos.
- La lista blanca y negra (`setHostEnable`) ya no da error.

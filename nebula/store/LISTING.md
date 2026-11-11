# Ficha para la Chrome Web Store — Nébula

## Nombre
Nébula — Anti-Fingerprint Shield

## Resumen (máx. 132 caracteres)
Protege tu privacidad: rompe tu huella digital (canvas, WebGL, User-Agent…) y bloquea rastreadores. Código abierto, sin telemetría.

## Categoría
Privacidad y seguridad

## Idioma
Español (y añade inglés si traduces la ficha)

## Descripción detallada
Nébula es una extensión de código abierto que te protege del **browser fingerprinting**: la técnica con la que los sitios y las redes publicitarias crean una "huella" única de tu navegador para rastrearte aunque borres las cookies o uses modo incógnito.

Nébula no te hace invisible —eso no existe—, pero sí rompe la unicidad y estabilidad de tu huella y bloquea rastreadores, de modo que seas mucho más difícil de identificar y seguir.

🛡️ QUÉ PROTEGE
• Canvas 2D, WebGL, Audio (ruido determinista por sitio)
• Fuentes, Client Rects, WebGPU
• User-Agent, Client Hints y cabeceras HTTP (coherentes)
• Pantalla, hardware, zona horaria e idioma
• WebRTC (evita fugas de IP local)
• Batería, dispositivos, voces, plugins, WebDriver
• Bloqueo de rastreadores (analytics/ads/session-replay)
• Limpiador de URLs (utm_, fbclid, gclid…) y Referer
• Bloqueo de cookies de terceros y auto-cierre de avisos de cookies

✨ FUNCIONES
• 12 dispositivos a imitar (Windows, Mac, Linux, iPhone, iPad, Android)
• Panel con estadísticas en vivo y registro de intentos de fingerprinting
• Asesor con IA (gratis con Pollinations, o tu propia API key)
• Modo viaje, rotación automática, perfiles guardados, niveles de escudo
• Validador de coherencia y ayuda para sitios con Cloudflare
• Temas de color, atajos de teclado y modo pánico

🔒 PRIVACIDAD
Cero telemetría. Cero servidores. Todo ocurre en tu navegador. Código abierto (MIT): puedes auditar cada línea. La única petición de red opcional es para ajustar la zona horaria a tu IP (desactivable) y, si lo activas, el asesor con IA.

Código fuente: https://github.com/zeussavage22-cpu/Elcondor

## Justificación de permisos (para la revisión de la tienda)
• storage — guardar tus ajustes localmente.
• scripting / content scripts — aplicar las protecciones en las páginas.
• declarativeNetRequest — reescribir cabeceras y bloquear rastreadores/cookies de terceros.
• webRequest — contar rastreadores (solo observación, no bloquea).
• tabs / activeTab — saber el sitio actual para el popup y la lista blanca.
• webNavigation — reiniciar el contador al navegar.
• browsingData — el botón "perfil nuevo limpio" (borra cookies/datos a petición del usuario).
• alarms — rotación automática y guardado periódico de estadísticas.
• contextMenus — acciones de clic derecho.
• host_permissions <all_urls> — el fingerprinting puede ocurrir en cualquier sitio, así que la protección debe poder aplicarse en todos.

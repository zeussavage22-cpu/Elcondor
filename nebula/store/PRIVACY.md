# Política de privacidad — Nébula

**Última actualización:** 2026

Nébula ("la extensión") está diseñada para **proteger tu privacidad**, y por tanto está construida
para recoger lo mínimo posible: **nada**.

## Qué datos recogemos
**Ninguno.** La extensión no tiene servidores propios, no envía analítica, no rastrea tu actividad
y no comparte información con terceros.

## Dónde se guardan tus datos
Todos tus ajustes, perfiles y estadísticas se guardan **localmente en tu navegador**
(`chrome.storage.local`). Nunca salen de tu dispositivo. Las estadísticas (rastreadores
bloqueados, intentos de fingerprinting) son contadores locales; puedes borrarlas restableciendo la
extensión.

## Peticiones de red
La extensión funciona de forma local. Hay **dos excepciones opcionales**, y solo si tú las activas:

1. **Zona horaria según mi IP** (activada por defecto, desactivable): consulta un servicio de
   geolocalización por IP (ipapi.co, con respaldos) para ajustar la zona horaria a tu ubicación y
   evitar incoherencias. Solo se envía lo normal de cualquier petición web (tu IP, que el servicio
   ya ve).
2. **Asesor con IA** (desactivado por defecto): si lo activas, envía el **dominio del sitio actual**
   y tu **User-Agent** al proveedor de IA que tú elijas (Pollinations, Gemini, OpenAI, etc.) para
   sugerirte un perfil. Tu clave de API, si la usas, se guarda solo en tu dispositivo.

Puedes desactivar ambas en los ajustes para que la extensión no haga ninguna petición de red.

## Permisos
Los permisos que pide la extensión se usan únicamente para aplicar las protecciones de privacidad
en las páginas que visitas. Ningún permiso se usa para recoger ni transmitir tus datos personales.

## Cambios
Si esta política cambia, se actualizará en el repositorio del proyecto:
https://github.com/zeussavage22-cpu/Elcondor

## Contacto
A través del repositorio de GitHub (issues).

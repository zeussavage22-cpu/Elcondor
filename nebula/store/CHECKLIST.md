# Checklist para publicar Nébula en la Chrome Web Store

> Nota: la cuenta de desarrollador de Chrome tiene un **pago único de 5 USD** (de Google) y la
> revisión puede tardar de horas a días. Esto lo haces tú; aquí tienes todo preparado.

## 1. Cuenta
- [ ] Crea una cuenta de desarrollador en https://chrome.google.com/webstore/devconsole
- [ ] Paga la tarifa única de registro (5 USD).

## 2. Empaquetar la extensión
- [ ] Comprime SOLO la carpeta `nebula/` en un `.zip` (que el `manifest.json` quede en la raíz del zip).
      En este repo ya se generan zips listos (p. ej. `nebula-antifingerprint-X.Y.Z.zip`).
- [ ] Verifica que carga sin errores en `chrome://extensions` (Modo desarrollador → Cargar descomprimida).

## 3. Ficha de la tienda
- [ ] Nombre, resumen y descripción: copia de `store/LISTING.md`.
- [ ] Categoría: Privacidad y seguridad.
- [ ] Icono 128×128: `nebula/icons/128.png`.
- [ ] Imagen promocional pequeña 440×280: `store/img/promo-440x280.png`.
- [ ] (Opcional) Marquee 1400×560 si quieres destacar.
- [ ] Capturas 1280×800 (mín. 1, recomendado 3–5): `store/img/screenshot-*.png`.

## 4. Privacidad y permisos
- [ ] Política de privacidad: sube `store/PRIVACY.md` a una URL pública
      (puedes activar GitHub Pages con la carpeta `web/` y enlazar `PRIVACY`).
- [ ] En "Prácticas de privacidad" declara: **no se recopilan datos de usuario**.
- [ ] Justifica cada permiso con el texto de `store/LISTING.md` → "Justificación de permisos".
- [ ] Justifica `host_permissions <all_urls>`: el fingerprinting ocurre en cualquier sitio, así que
      la protección debe poder aplicarse en todos.

## 5. Enviar
- [ ] Sube el `.zip`, rellena la ficha, adjunta imágenes y la URL de privacidad.
- [ ] Envía a revisión. Responde rápido si Google pide aclaraciones sobre permisos.

## Consejos para pasar la revisión
- Sé explícito en la justificación de permisos (sobre todo `<all_urls>`, `webRequest` y `browsingData`).
- Deja claro que no hay recopilación de datos.
- Mantén el `.zip` solo con los archivos necesarios (sin carpetas `web/`, `store/`, `test/` si quieres
  una build mínima; no es obligatorio, pero reduce dudas en la revisión).

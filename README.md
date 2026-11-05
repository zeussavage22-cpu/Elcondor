# Elcondor Cloner

App Android para crear copias independientes de las apps instaladas (por ejemplo, dos
WhatsApp o dos Instagram en el mismo teléfono). Escrita desde cero en Kotlin.

## Funciones

- Lista de apps instaladas, con buscador y opción para mostrar las apps del sistema.
- Clonar una app con:
  - **otro nombre** visible bajo el icono,
  - **otro paquete** (para que se instale junto a la original),
  - **bloqueo de permisos** opcional en el clon: internet, ubicación, cámara, micrófono,
    contactos, teléfono, SMS, archivos, calendario y notificaciones.
- Soporta apps divididas en varios APK (*split APKs*), las de Play Store.
- Pestaña **Mis clones** para abrir o desinstalar los clones.
- Todos los clones se firman con una clave propia que se crea la primera vez y se guarda en
  el teléfono, así puedes volver a clonar una app encima de su clon anterior.

## Cómo funciona

1. Lee los APK de la app instalada (`ApplicationInfo.sourceDir` + `splitSourceDirs`).
2. Reescribe el `AndroidManifest.xml` binario (`core/.../axml/Axml.kt`):
   - cambia el `package`,
   - convierte los nombres de clase relativos (`.MainActivity`) en absolutos,
   - renombra lo que debe ser único en el teléfono: *authorities* de los content providers y
     permisos declarados (y todas sus referencias),
   - quita `sharedUserId`, cambia el nombre visible y elimina los permisos bloqueados.
3. Copia el resto de archivos sin recomprimirlos, alineando los no comprimidos
   (4 bytes, y 16 KiB para librerías nativas) como hace `zipalign`.
4. Firma con `apksig` (esquemas v1 si hace falta, v2 y v3).
5. Instala todo con `PackageInstaller`.

## Limitaciones

Como cualquier clonador basado en cambiar el paquete, algunas apps no funcionan bien clonadas:

- Apps que comprueban su propia firma o integridad (bancos, algunos juegos, apps con
  Play Integrity).
- Notificaciones push de Firebase: suelen dejar de llegar al clon, porque están ligadas al
  paquete original.
- Funciones que usan un *content provider* con el nombre escrito en el código (por ejemplo,
  compartir archivos en algunas apps).
- Apps que exigen haberse instalado desde Play Store.

## Compilar

Requisitos: JDK 17+ y Android SDK (platform 35).

```bash
./gradlew :core:test            # tests del motor de clonación
./gradlew :app:assembleRelease  # APK en app/build/outputs/apk/release/
```

Para probar el motor con cualquier APK:

```bash
./gradlew :core:test -PtestApk=/ruta/a/app.apk
```

## Estructura

- `core/` – motor de clonación en Kotlin puro (se prueba sin Android).
- `app/` – la app Android (Jetpack Compose).

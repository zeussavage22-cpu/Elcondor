# El Cóndor

Personalización profunda de sistema y UI para Android sin root, usando [Shizuku](https://shizuku.rikka.app/).

## Paso 1 (actual)
- Integración con Shizuku vía **UserService** (AIDL) en lugar del `Shizuku.newProcess()`, que ya es privado.
- `ShizukuShellManager`: estado observable (`StateFlow`), permisos, reconexión y `executeCommand(): Result<String>`.
- Tarjeta de DPI con temporizador de seguridad de 10 s y "Restaurar fábrica".

## Compilar
Requiere JDK 17+ y Android SDK 35:
```
./gradlew assembleDebug
```
Si un DPI te deja la pantalla inutilizable: `adb shell wm density reset`.

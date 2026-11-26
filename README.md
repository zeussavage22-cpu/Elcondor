# El Cóndor

Personalización profunda de sistema y UI para Android sin root, usando [Shizuku](https://shizuku.rikka.app/).

## Funciones
- **Shizuku** vía UserService (AIDL) en lugar de `Shizuku.newProcess()`, que ya es privado.
  `ShizukuShellManager`: estado observable, permisos, reconexión y `executeCommand(): Result<String>`.
- **Pantalla**: DPI (`wm density`) y resolución (`wm size`) con temporizador de seguridad
  de 10 s y "Restaurar fábrica".
- **Paquetes**: lista de apps del sistema, desinstalación para el usuario 0
  (`pm uninstall -k --user 0`) y **Papelera** con restauración en un toque
  (`cmd package install-existing`). Los paquetes críticos (SystemUI, launcher y
  teclado activos, Shizuku…) están bloqueados.

## Compilar
Requiere JDK 17+ y Android SDK 35:
```
./gradlew assembleDebug
./gradlew testDebugUnitTest   # parsers y políticas
```
Si un DPI o una resolución te dejan la pantalla inutilizable:
`adb shell wm density reset` / `adb shell wm size reset`.
Para restaurar un paquete a mano: `adb shell cmd package install-existing <paquete>`.

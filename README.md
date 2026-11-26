# El Cóndor

Personalización profunda de sistema y UI para Android sin root, usando [Shizuku](https://shizuku.rikka.app/).

## Funciones
- **Shizuku** vía UserService (AIDL) en lugar de `Shizuku.newProcess()`, que ya es privado.
  `ShizukuShellManager`: estado observable, permisos, reconexión y `executeCommand(): Result<String>`.
- **DPI por app** (emulado, sin root): al abrir una app con regla se aplica su DPI
  y al salir se restaura el anterior. Detecta la app en primer plano con un servicio
  de accesibilidad mínimo que se activa vía Shizuku.
- **Pantalla**: DPI global (`wm density`) y resolución (`wm size`) con temporizador de seguridad
  de 10 s y "Restaurar fábrica".
- **Paquetes**: lista de apps del sistema, desinstalación para el usuario 0
  (`pm uninstall -k --user 0`) y **Papelera** con restauración en un toque
  (`cmd package install-existing`). Los paquetes críticos (SystemUI, launcher y
  teclado activos, Shizuku…) están bloqueados.
- **Animaciones**: las tres escalas (presets 0x–1.5x o ajuste fino) y modo inmersivo
  (`policy_control`, solo efectivo hasta Android 10).
- **Mosaico de Ajustes rápidos**: alterna 0.25x ↔ 1x con un toque; en Android 13+ se
  añade al panel desde la propia app.
- **Servicio de seguridad**: durante la cuenta atrás de DPI/resolución, una
  notificación en primer plano con "Mantener / Revertir" protege la reversión.

## Compilar
Requiere JDK 17+ y Android SDK 35:
```
./gradlew assembleDebug
./gradlew testDebugUnitTest   # parsers y políticas
```
Si un DPI o una resolución te dejan la pantalla inutilizable:
`adb shell wm density reset` / `adb shell wm size reset`.
Para restaurar un paquete a mano: `adb shell cmd package install-existing <paquete>`.

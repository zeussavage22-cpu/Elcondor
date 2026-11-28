# SensorOff

App Android para auriculares tipo AirPods (originales o copias) cuyo sensor de oído pausa o corta la música.

1. **Apagar sensor de oído**: envía el comando de Apple para desactivar la detección automática de oído. Funciona con AirPods originales y con algunas copias; la mayoría de las copias baratas no lo entienden.
2. **Anti-pausa automática** (plan B): si el sensor pausa la música, la app la vuelve a reproducir sola, y también la reanuda cuando los auriculares se reconectan. Para pausar de verdad, pulsa pausa dos veces seguidas.

Instalar: `SensorOff.apk`. Compilar: `./gradlew assembleDebug`.

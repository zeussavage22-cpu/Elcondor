# DarkNet

Comparte el internet de tu Android por hotspot o USB a través de un túnel propio,
sin root (con [Shizuku](https://shizuku.rikka.app/)). Android 13 o superior.

- `engine/`: motor en Go. IPv4 TCP va por el TCP del kernel (camino rápido, con
  autoprueba); IPv6, UDP e ICMP por una pila gVisor (camino de respaldo).
- `app/`: app Android (Kotlin + Compose). `service/PrivilegedService.kt` corre como
  usuario shell dentro de Shizuku: crea la red de prueba, hace que el hotspot la use
  como salida y arranca el motor.
- `lab/`: laboratorio de red (netns) que imita el tethering de Android; `lab/ci.sh`
  lo usa en la compilación automática.

Compilar a mano: `cd engine && gomobile bind -target=android/arm64 -androidapi 33 -o ../app/libs/engine.aar .`
y luego `./gradlew assembleDebug`.

Código original. Dependencias: Shizuku (Apache-2.0), gVisor (Apache-2.0),
HiddenApiBypass (Apache-2.0), AndroidX (Apache-2.0).

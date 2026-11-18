# Motor "stack del sistema" — investigación

- `informe.html`: informe completo (hallazgos, mediciones, riesgos, plan).
- `prototipo/`: motor prototipo en Go (IPv4). Reescribe los paquetes del túnel para
  que el TCP del kernel termine las conexiones y une los sockets con `splice`.
- `lab/lab.sh`: laboratorio con netns que imita el tethering de Android
  (cliente → NAT → TUN → motor → servidor). `lab/bench.sh <motor> <etiqueta>` mide.

Uso (Linux, root): `lab/lab.sh up`, compilar el motor (`go build`), `lab/bench.sh ./sysengine "nuevo"`.

# DNS privado y rápido (solo para ti)

```
 Tus dispositivos ──(cifrado DoT/DoH + tu ClientID secreto)──► AdGuard Home ──► Unbound ──► servidores raíz / autoritativos
                                                               (bloquea anuncios,  (resolver propio,
                                                                rechaza a extraños) DNSSEC, caché)
```

**Privado**
- Unbound resuelve todo por sí mismo: tus consultas **no pasan por Google, Cloudflare ni tu proveedor de internet**.
- Todo viaja cifrado (DNS-over-TLS / DNS-over-HTTPS). El puerto 53 sin cifrar no se abre a internet.
- Solo responde a quien tenga tu **ClientID secreto**. A cualquier otro lo rechaza.
- No se guardan registros de consultas en Unbound (`log-queries: no`). En AdGuard puedes desactivar el registro de consultas en *Configuración general*.
- QNAME minimisation: cada servidor ve solo la parte del dominio que necesita.

**Rápido**
- Caché grande, con `prefetch` y `serve-expired`: los dominios que usas seguido responden en ~1 ms.
- La caché optimista de AdGuard + el bloqueo de anuncios y rastreadores también aceleran la navegación.

## Qué necesitas
1. Un VPS barato con Ubuntu/Debian (1 GB de RAM sobra). Hetzner, Oracle Free Tier, DigitalOcean, etc.
2. Un dominio (o subdominio gratis, por ejemplo de DuckDNS) con **dos registros A** hacia la IP del VPS:
   - `dns.tudominio.com`
   - `*.dns.tudominio.com` (o el nombre exacto que te dará el script: `yo-xxxx.dns.tudominio.com`)

## Instalación
```bash
git clone <este repo> && cd Elcondor/dns
sudo ./instalar.sh dns.tudominio.com tu@correo.com
```
El script instala Docker, saca el certificado TLS, configura el firewall, levanta los contenedores
y al final te muestra los pasos para el panel de AdGuard y **tus direcciones privadas**.

## Usarlo en tus dispositivos
| Dispositivo | Dónde | Valor |
|---|---|---|
| Android 9+ | Ajustes › Red › DNS privado | `yo-xxxx.dns.tudominio.com` |
| Chrome / Edge | Configuración › Privacidad › DNS seguro › Personalizado | `https://dns.tudominio.com/dns-query/yo-xxxx` |
| Firefox | Configuración › Privacidad › DNS sobre HTTPS › Máxima › Personalizado | igual que arriba |
| Windows 11 | Configuración › Red › DNS › Cifrado: solo HTTPS | igual que arriba |
| iPhone / Mac | Panel de AdGuard › *Instrucciones de configuración* › descarga el perfil `.mobileconfig` | — |

## Comprobar que funciona
- https://dnsleaktest.com → debe aparecer solo la IP de tu VPS.
- https://1.1.1.1/help → "Using DNS over HTTPS/TLS: Yes" (según el dispositivo).
- Desde el servidor: `docker exec unbound drill @127.0.0.1 google.com` (la 2ª vez el *Query time* debe ser ~0 ms).

## Mantenimiento
```bash
docker compose pull && docker compose up -d --build   # actualizar
docker compose logs -f                                # ver logs
```
El certificado se renueva solo (certbot) y AdGuard se reinicia para cargarlo.

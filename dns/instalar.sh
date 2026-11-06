#!/usr/bin/env bash
# Instala tu DNS privado en un servidor Ubuntu/Debian (VPS).
# Uso:  sudo ./instalar.sh dns.tudominio.com tu@correo.com
set -euo pipefail

DOMINIO="${1:?Uso: sudo ./instalar.sh dns.tudominio.com tu@correo.com}"
CORREO="${2:?Falta el correo para el certificado TLS}"
cd "$(dirname "$0")"

# Tu identificador secreto: solo quien lo conozca podrá usar el DNS.
if [ -f .clientid ]; then CLIENTID="$(cat .clientid)"; else
  CLIENTID="yo-$(od -An -N6 -tx1 /dev/urandom | tr -d " \n")"; echo "$CLIENTID" > .clientid
fi

[ "$(id -u)" -eq 0 ] || { echo "Ejecuta con sudo"; exit 1; }

echo "==> Instalando Docker y certbot"
if ! command -v docker >/dev/null; then
  curl -fsSL https://get.docker.com | sh
fi
apt-get update -qq && apt-get install -y -qq certbot

echo "==> Obteniendo certificado TLS (el puerto 80 debe estar libre)"
echo "    Requiere registros DNS tipo A hacia este servidor para:"
echo "      $DOMINIO  y  $CLIENTID.$DOMINIO"
certbot certonly --standalone --non-interactive --agree-tos --expand \
  -m "$CORREO" -d "$DOMINIO" -d "$CLIENTID.$DOMINIO" --keep-until-expiring
# Renovación automática: reinicia AdGuard para que cargue el certificado nuevo
mkdir -p /etc/letsencrypt/renewal-hooks/deploy
cat > /etc/letsencrypt/renewal-hooks/deploy/adguard.sh <<HOOK
#!/bin/sh
docker restart adguard
HOOK
chmod +x /etc/letsencrypt/renewal-hooks/deploy/adguard.sh

echo "==> Firewall: solo SSH, DoT (853) y DoH (443)"
if command -v ufw >/dev/null; then
  ufw allow OpenSSH
  ufw allow 80/tcp   # renovación del certificado
  ufw allow 853/tcp
  ufw allow 443/tcp
  ufw --force enable
fi

echo "==> Levantando Unbound + AdGuard Home"
docker compose up -d --build

cat <<MSG

Listo. Falta configurar AdGuard Home (una sola vez):

1) Desde TU computadora abre un túnel al panel:
     ssh -L 3000:127.0.0.1:3000 usuario@IP_DEL_SERVIDOR
   y entra a http://localhost:3000

2) Asistente inicial:
     - Interfaz web: Todas las interfaces, puerto 3000
     - Servidor DNS: Todas las interfaces, puerto 53
     - Crea usuario y contraseña fuerte

3) Configuración > Configuración DNS:
     - Servidores DNS upstream:   172.28.0.53
     - Servidores bootstrap:      172.28.0.53
     - Activa "Habilitar caché" y "Caché optimista"
     - Control de acceso > Clientes permitidos:  $CLIENTID
       (cualquier otro será rechazado)

4) Configuración > Configuración de cifrado:
     - Habilitar cifrado, nombre del servidor: $DOMINIO
     - Puerto HTTPS 443, puerto DNS-over-TLS 853
     - Ruta del certificado: /etc/letsencrypt/live/$DOMINIO/fullchain.pem
     - Ruta de la clave:     /etc/letsencrypt/live/$DOMINIO/privkey.pem

Tus direcciones privadas (guárdalas, no las compartas):
   DoT (Android "DNS privado"):  $CLIENTID.$DOMINIO
   DoH (Chrome/Firefox/Windows): https://$DOMINIO/dns-query/$CLIENTID
MSG

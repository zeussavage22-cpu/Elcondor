FROM alpine:3.20
RUN apk add --no-cache unbound dnssec-root drill \
 && mkdir -p /var/lib/unbound \
 && cp /usr/share/dnssec-root/trusted-key.key /var/lib/unbound/root.key \
 && chown -R unbound:unbound /var/lib/unbound
COPY unbound.conf /etc/unbound/unbound.conf
RUN unbound-checkconf /etc/unbound/unbound.conf
EXPOSE 53/udp 53/tcp
HEALTHCHECK --interval=30s --timeout=5s CMD drill @127.0.0.1 cloudflare.com >/dev/null || exit 1
CMD ["unbound", "-d", "-c", "/etc/unbound/unbound.conf"]

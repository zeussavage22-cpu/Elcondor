package dev.darknet;

// Runs as the shell user inside the Shizuku process.
interface IDarkNetService {
    // Starts sharing. options: JSON {"fastEngine", "usb", "dns", "band", "mtu", "bbr", "combine"}. Returns status JSON.
    String start(String options) = 1;

    // Stops sharing and restores the system. Returns status JSON.
    String stop() = 2;

    // Current status JSON (state, detail, traffic, clients, engine).
    String status() = 3;

    // Recent log lines, newest last.
    String log() = 4;

    // Pins outbound traffic to a network handle (a VPN); 0 unpins.
    void setNetwork(long handle) = 5;

    // Build id, so the app can replace a stale service after an update.
    int version() = 6;

    // The hotspot's name and password, for the QR code: JSON {"ssid","password","security"}.
    String hotspotInfo() = 7;

    // Checks that traffic really goes through DarkNet: JSON array of {"name","state","detail"}.
    String diagnose() = 8;

    // Downloads for a few seconds, "engine" (through DarkNet's TUN) or "direct"
    // (the phone's own network): JSON {"mbps","bytes","ms"} or {"error"}.
    String speedTest(String mode) = 9;

    // Turns mobile data off and on again: JSON {"seconds","before","after"} (public IPs) or {"error"}.
    String restartData() = 10;

    // Like speedTest, from a given URL (streams ask for consecutive byte ranges).
    String speedTestUrl(String mode, String url) = 11;

    // One escalating attempt to get a new IP: 1 = data off 5 s, 2 = data off 15 s,
    // 3 = airplane mode for a few seconds (data off 30 s instead while sharing).
    // JSON {"before4","before6","after4","after6","seconds","method"} or {"error"}.
    String renewIp(int attempt) = 12;

    // Shizuku's reserved code: kill the service process.
    void destroy() = 16777114;
}

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

    // Shizuku's reserved code: kill the service process.
    void destroy() = 16777114;
}

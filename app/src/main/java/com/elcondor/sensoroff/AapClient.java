package com.elcondor.sensoroff;

import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.os.Build;
import android.os.ParcelUuid;

import org.lsposed.hiddenapibypass.HiddenApiBypass;

import java.io.InputStream;
import java.io.OutputStream;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;

/**
 * Talks the Apple accessory protocol (AAP) over classic L2CAP PSM 0x1001.
 * Genuine AirPods and some high-end clones understand it; most cheap clones don't.
 */
public class AapClient {

    public interface Log { void log(String msg); }

    private static final int TYPE_L2CAP = 3;
    private static final int PSM_AAP = 0x1001;

    private static final byte[] HANDSHAKE = hex("00 00 04 00 01 00 02 00 00 00 00 00 00 00 00 00");
    private static final byte[] SET_FEATURES = hex("04 00 04 00 4D 00 FF 00 00 00 00 00 00 00");
    private static final byte[] REQUEST_NOTIFS = hex("04 00 04 00 0F 00 FF FF FE FF");
    // Control command 0x09, identifier 0x0A = automatic ear detection; 0x01 on, 0x02 off.
    private static final byte[] EAR_DETECTION_ON = hex("04 00 04 00 09 00 0A 01 00 00 00");
    private static final byte[] EAR_DETECTION_OFF = hex("04 00 04 00 09 00 0A 02 00 00 00");

    private final Log log;

    public AapClient(Log log) { this.log = log; }

    public boolean setEarDetection(BluetoothDevice device, boolean enabled) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            try { HiddenApiBypass.addHiddenApiExemptions(""); } catch (Throwable ignored) { }
        }
        BluetoothSocket socket = openSocket(device);
        if (socket == null) {
            log.log("❌ No se pudo abrir el canal L2CAP. Tu Android o tus auriculares no lo permiten.");
            return false;
        }
        try {
            OutputStream out = socket.getOutputStream();
            InputStream in = socket.getInputStream();
            send(out, HANDSHAKE, "saludo");
            readSome(in);
            send(out, SET_FEATURES, "funciones");
            readSome(in);
            send(out, REQUEST_NOTIFS, "notificaciones");
            readSome(in);
            send(out, enabled ? EAR_DETECTION_ON : EAR_DETECTION_OFF,
                    enabled ? "ENCENDER sensor" : "APAGAR sensor");
            Thread.sleep(300);
            int got = readSome(in);
            log.log(got > 0
                    ? "✅ Comando enviado y los auriculares respondieron. Quítate y ponte uno para probar."
                    : "⚠️ Comando enviado pero sin respuesta. Puede que tus auriculares no entiendan el protocolo.");
            return true;
        } catch (Exception e) {
            log.log("❌ Error en la comunicación: " + e);
            return false;
        } finally {
            try { socket.close(); } catch (Exception ignored) { }
        }
    }

    private BluetoothSocket openSocket(BluetoothDevice device) {
        // 1) Old hidden helper methods on BluetoothDevice.
        for (String name : new String[]{"createInsecureL2capSocket", "createL2capSocket"}) {
            try {
                Method m = BluetoothDevice.class.getDeclaredMethod(name, int.class);
                m.setAccessible(true);
                BluetoothSocket s = (BluetoothSocket) m.invoke(device, PSM_AAP);
                if (tryConnect(s, name)) return s;
            } catch (NoSuchMethodException ignored) {
            } catch (Throwable t) {
                log.log("· " + name + ": " + rootCause(t));
            }
        }
        // 2) Hidden BluetoothSocket constructors (signature differs between Android versions).
        List<Constructor<?>> ctors = new ArrayList<>(Arrays.asList(BluetoothSocket.class.getDeclaredConstructors()));
        ctors.sort(Comparator.comparingInt(Constructor::getParameterCount));
        for (Constructor<?> c : ctors) {
            Object[] args = buildArgs(c.getParameterTypes(), device);
            if (args == null) continue;
            try {
                c.setAccessible(true);
                BluetoothSocket s = (BluetoothSocket) c.newInstance(args);
                if (tryConnect(s, "constructor(" + c.getParameterCount() + ")")) return s;
            } catch (Throwable t) {
                log.log("· constructor(" + c.getParameterCount() + "): " + rootCause(t));
            }
        }
        return null;
    }

    /** Fills constructor args: first int = socket type, first int after the device = port. */
    private static Object[] buildArgs(Class<?>[] types, BluetoothDevice device) {
        int deviceIdx = -1;
        for (int i = 0; i < types.length; i++) if (types[i] == BluetoothDevice.class) deviceIdx = i;
        if (deviceIdx < 0) return null;
        Object[] args = new Object[types.length];
        boolean typeSet = false, portSet = false;
        for (int i = 0; i < types.length; i++) {
            Class<?> t = types[i];
            if (t == BluetoothDevice.class) args[i] = device;
            else if (t == int.class) {
                if (!typeSet) { args[i] = TYPE_L2CAP; typeSet = true; }
                else if (i > deviceIdx && !portSet) { args[i] = PSM_AAP; portSet = true; }
                else args[i] = i < deviceIdx ? -1 : 0;
            }
            else if (t == boolean.class) args[i] = false;
            else if (t == long.class) args[i] = 0L;
            else if (t == ParcelUuid.class || !t.isPrimitive()) args[i] = null;
            else return null;
        }
        return portSet ? args : null;
    }

    private boolean tryConnect(BluetoothSocket s, String how) {
        if (s == null) return false;
        try {
            s.connect();
            log.log("✔ Canal abierto (" + how + ")");
            return true;
        } catch (Throwable t) {
            log.log("· " + how + ": " + rootCause(t));
            try { s.close(); } catch (Exception ignored) { }
            return false;
        }
    }

    private void send(OutputStream out, byte[] data, String what) throws Exception {
        out.write(data);
        out.flush();
        log.log("→ " + what);
    }

    private int readSome(InputStream in) throws Exception {
        int total = 0;
        long end = System.currentTimeMillis() + 700;
        byte[] buf = new byte[512];
        while (System.currentTimeMillis() < end) {
            if (in.available() > 0) {
                int n = in.read(buf);
                if (n > 0) {
                    total += n;
                    log.log("← " + toHex(buf, Math.min(n, 24)) + (n > 24 ? "…" : ""));
                }
            } else {
                Thread.sleep(50);
            }
        }
        return total;
    }

    private static String rootCause(Throwable t) {
        while (t.getCause() != null) t = t.getCause();
        return t.getClass().getSimpleName() + (t.getMessage() != null ? ": " + t.getMessage() : "");
    }

    private static byte[] hex(String s) {
        String[] parts = s.split(" ");
        byte[] b = new byte[parts.length];
        for (int i = 0; i < parts.length; i++) b[i] = (byte) Integer.parseInt(parts[i], 16);
        return b;
    }

    private static String toHex(byte[] b, int n) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < n; i++) sb.append(String.format("%02X ", b[i]));
        return sb.toString().trim();
    }
}

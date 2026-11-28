package com.elcondor.sensoroff;

import android.Manifest;
import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothManager;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Typeface;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.Spinner;
import android.widget.Switch;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;

public class MainActivity extends Activity {

    private Spinner deviceSpinner;
    private TextView logView;
    private Switch antiPause;
    private final List<BluetoothDevice> devices = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        int pad = dp(16);

        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(pad, pad, pad, pad);

        TextView title = new TextView(this);
        title.setText("SensorOff");
        title.setTextSize(26);
        title.setTypeface(Typeface.DEFAULT_BOLD);
        root.addView(title);

        root.addView(text("1. Elige tus auriculares (deben estar conectados):"));
        deviceSpinner = new Spinner(this);
        root.addView(deviceSpinner);

        Button off = button("APAGAR sensor de oído");
        off.setOnClickListener(v -> runAap(false));
        root.addView(off);
        Button on = button("Volver a encender sensor");
        on.setOnClickListener(v -> runAap(true));
        root.addView(on);

        root.addView(text("\n2. Plan B, funciona con casi cualquier auricular: si el sensor pausa la música, "
                + "la app la vuelve a poner sola. Si pausas tú, pulsa pausa dos veces seguidas."));
        antiPause = new Switch(this);
        antiPause.setText("Anti-pausa automática");
        antiPause.setOnCheckedChangeListener((b, checked) -> {
            prefs().edit().putBoolean(AntiPauseService.KEY_ENABLED, checked).apply();
            if (checked && !listenerGranted()) {
                log("Activa \"SensorOff\" en la lista que se abre y vuelve aquí.");
                startActivity(new Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS));
            }
        });
        root.addView(antiPause);

        root.addView(text("\nRegistro:"));
        logView = new TextView(this);
        logView.setTypeface(Typeface.MONOSPACE);
        logView.setTextSize(12);
        root.addView(logView);

        ScrollView scroll = new ScrollView(this);
        scroll.addView(root);
        setContentView(scroll);

        if (Build.VERSION.SDK_INT >= 31
                && checkSelfPermission(Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[]{Manifest.permission.BLUETOOTH_CONNECT}, 1);
        } else {
            loadDevices();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        antiPause.setChecked(prefs().getBoolean(AntiPauseService.KEY_ENABLED, false) && listenerGranted());
    }

    @Override
    public void onRequestPermissionsResult(int code, String[] perms, int[] results) {
        if (results.length > 0 && results[0] == PackageManager.PERMISSION_GRANTED) loadDevices();
        else log("Sin permiso de Bluetooth no puedo ver tus auriculares.");
    }

    @SuppressWarnings("MissingPermission")
    private void loadDevices() {
        BluetoothAdapter adapter = ((BluetoothManager) getSystemService(BLUETOOTH_SERVICE)).getAdapter();
        if (adapter == null || !adapter.isEnabled()) {
            log("Enciende el Bluetooth y vuelve a abrir la app.");
            return;
        }
        devices.clear();
        List<String> names = new ArrayList<>();
        for (BluetoothDevice d : adapter.getBondedDevices()) {
            devices.add(d);
            names.add(d.getName() + "  (" + d.getAddress() + ")");
        }
        deviceSpinner.setAdapter(new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, names));
        for (int i = 0; i < names.size(); i++) {
            String n = names.get(i).toLowerCase();
            if (n.contains("pod") || n.contains("air")) { deviceSpinner.setSelection(i); break; }
        }
        log(devices.size() + " dispositivos vinculados encontrados.");
    }

    private void runAap(boolean enable) {
        int i = deviceSpinner.getSelectedItemPosition();
        if (i < 0 || i >= devices.size()) { log("Primero elige tus auriculares."); return; }
        BluetoothDevice d = devices.get(i);
        log("— Probando con " + d.getAddress() + " —");
        new Thread(() -> new AapClient(this::log).setEarDetection(d, enable)).start();
    }

    private void log(String msg) {
        runOnUiThread(() -> logView.append(msg + "\n"));
    }

    private boolean listenerGranted() {
        String flat = Settings.Secure.getString(getContentResolver(), "enabled_notification_listeners");
        return flat != null && flat.contains(getPackageName());
    }

    private SharedPreferences prefs() { return getSharedPreferences(AntiPauseService.PREFS, MODE_PRIVATE); }

    private TextView text(String s) {
        TextView t = new TextView(this);
        t.setText(s);
        t.setTextSize(15);
        t.setPadding(0, dp(8), 0, dp(4));
        return t;
    }

    private Button button(String s) {
        Button b = new Button(this);
        b.setText(s);
        b.setAllCaps(false);
        return b;
    }

    private int dp(int v) { return Math.round(v * getResources().getDisplayMetrics().density); }
}

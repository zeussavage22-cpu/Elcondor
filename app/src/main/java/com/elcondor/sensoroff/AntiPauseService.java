package com.elcondor.sensoroff;

import android.bluetooth.BluetoothA2dp;
import android.bluetooth.BluetoothProfile;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.media.AudioDeviceInfo;
import android.media.AudioManager;
import android.media.session.MediaController;
import android.media.session.MediaSessionManager;
import android.media.session.PlaybackState;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.service.notification.NotificationListenerService;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Backup plan: when the earbuds' faulty sensor pauses music (or the buds drop and reconnect),
 * resume playback automatically. Pausing twice in a row (within 8 s) is respected as a real pause.
 */
public class AntiPauseService extends NotificationListenerService {

    static final String PREFS = "sensoroff";
    static final String KEY_ENABLED = "antipause";

    private static final long RESUME_DELAY_MS = 600;
    private static final long USER_PAUSE_WINDOW_MS = 8000;
    private static final long RECONNECT_WINDOW_MS = 60000;

    private final Handler handler = new Handler(Looper.getMainLooper());
    private final Map<MediaController, MediaController.Callback> callbacks = new HashMap<>();
    private MediaSessionManager msm;
    private long lastAutoResume = 0;
    private long lastPlayingAt = 0;
    private String lastPlayingPkg = null;

    private final MediaSessionManager.OnActiveSessionsChangedListener sessionsListener = this::attach;

    private final BroadcastReceiver btReceiver = new BroadcastReceiver() {
        @Override public void onReceive(Context ctx, Intent intent) {
            int state = intent.getIntExtra(BluetoothProfile.EXTRA_STATE, -1);
            if (state == BluetoothProfile.STATE_CONNECTED && enabled()
                    && lastPlayingPkg != null
                    && System.currentTimeMillis() - lastPlayingAt < RECONNECT_WINDOW_MS) {
                handler.postDelayed(() -> resume(lastPlayingPkg), 1500);
            }
        }
    };

    @Override
    public void onListenerConnected() {
        msm = (MediaSessionManager) getSystemService(MEDIA_SESSION_SERVICE);
        ComponentName me = new ComponentName(this, AntiPauseService.class);
        try {
            msm.addOnActiveSessionsChangedListener(sessionsListener, me);
            attach(msm.getActiveSessions(me));
        } catch (SecurityException ignored) { }
        IntentFilter f = new IntentFilter(BluetoothA2dp.ACTION_CONNECTION_STATE_CHANGED);
        if (Build.VERSION.SDK_INT >= 33) registerReceiver(btReceiver, f, Context.RECEIVER_EXPORTED);
        else registerReceiver(btReceiver, f);
    }

    @Override
    public void onListenerDisconnected() {
        try { msm.removeOnActiveSessionsChangedListener(sessionsListener); } catch (Exception ignored) { }
        try { unregisterReceiver(btReceiver); } catch (Exception ignored) { }
        detachAll();
    }

    private void attach(List<MediaController> controllers) {
        detachAll();
        if (controllers == null) return;
        for (MediaController c : controllers) {
            MediaController.Callback cb = new MediaController.Callback() {
                int last = stateOf(c);
                @Override public void onPlaybackStateChanged(PlaybackState s) {
                    int now = s == null ? PlaybackState.STATE_NONE : s.getState();
                    if (now == PlaybackState.STATE_PLAYING) {
                        lastPlayingAt = System.currentTimeMillis();
                        lastPlayingPkg = c.getPackageName();
                    } else if (last == PlaybackState.STATE_PLAYING && now == PlaybackState.STATE_PAUSED) {
                        lastPlayingAt = System.currentTimeMillis();
                        onPaused(c);
                    }
                    last = now;
                }
            };
            c.registerCallback(cb, handler);
            callbacks.put(c, cb);
        }
    }

    private void onPaused(MediaController c) {
        if (!enabled() || !headsetConnected()) return;
        // A second pause right after we auto-resumed means the user really wants to stop.
        if (System.currentTimeMillis() - lastAutoResume < USER_PAUSE_WINDOW_MS) return;
        handler.postDelayed(() -> {
            if (stateOf(c) == PlaybackState.STATE_PAUSED) {
                lastAutoResume = System.currentTimeMillis();
                c.getTransportControls().play();
            }
        }, RESUME_DELAY_MS);
    }

    private void resume(String pkg) {
        for (MediaController c : callbacks.keySet()) {
            if (c.getPackageName().equals(pkg) && stateOf(c) != PlaybackState.STATE_PLAYING) {
                lastAutoResume = System.currentTimeMillis();
                c.getTransportControls().play();
                return;
            }
        }
    }

    private void detachAll() {
        for (Map.Entry<MediaController, MediaController.Callback> e : callbacks.entrySet()) {
            try { e.getKey().unregisterCallback(e.getValue()); } catch (Exception ignored) { }
        }
        callbacks.clear();
    }

    private boolean enabled() {
        SharedPreferences p = getSharedPreferences(PREFS, MODE_PRIVATE);
        return p.getBoolean(KEY_ENABLED, false);
    }

    private boolean headsetConnected() {
        AudioManager am = (AudioManager) getSystemService(AUDIO_SERVICE);
        for (AudioDeviceInfo d : am.getDevices(AudioManager.GET_DEVICES_OUTPUTS)) {
            int t = d.getType();
            if (t == AudioDeviceInfo.TYPE_BLUETOOTH_A2DP || t == AudioDeviceInfo.TYPE_BLE_HEADSET) return true;
        }
        return false;
    }

    private static int stateOf(MediaController c) {
        PlaybackState s = c.getPlaybackState();
        return s == null ? PlaybackState.STATE_NONE : s.getState();
    }
}

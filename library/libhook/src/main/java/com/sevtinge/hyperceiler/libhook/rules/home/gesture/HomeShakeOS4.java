/* SPDX-License-Identifier: AGPL-3.0-or-later */
package com.sevtinge.hyperceiler.libhook.rules.home.gesture;

import android.app.Activity;
import android.app.Application;
import android.app.KeyguardManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.database.ContentObserver;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.PowerManager;

import com.sevtinge.hyperceiler.common.log.ModuleLog;

/** OS4 launcher sensor lifecycle. The framework broker still executes the configured action. */
public final class HomeShakeOS4 implements Application.ActivityLifecycleCallbacks, SensorEventListener {
    private static final String TAG = "HyperCeiler.HomeGesture";
    private static final Uri PREFS = Uri.parse("content://com.sevtinge.hyperceiler.provider.sharedprefs/home_gestures");
    private static final Uri ROOT = Uri.parse("content://com.sevtinge.hyperceiler.provider.sharedprefs");
    private static boolean installed;

    private final Application app;
    private final String launcherClass;
    private final Handler worker;
    private final HomeExtraGestureState.Shake shake = new HomeExtraGestureState.Shake();
    private Activity launcher;
    private volatile boolean resumed;
    // Worker-only state. No Binder or Provider calls on the launcher main thread.
    private SensorManager sensors;
    private int action;
    private boolean observersReady;
    private boolean sensorRegistered;
    private final Runnable refresh = this::refresh;

    public static void install(Activity launcher) {
        if (installed || !"com.miui.home".equals(Application.getProcessName())) return;
        installed = true;
        var sensorHook = new HomeShakeOS4(launcher.getApplication(), launcher.getClass().getName());
        sensorHook.app.registerActivityLifecycleCallbacks(sensorHook);
    }

    private HomeShakeOS4(Application app, String launcherClass) {
        this.app = app;
        this.launcherClass = launcherClass;
        HandlerThread thread = new HandlerThread("HC-HomeShake");
        thread.start();
        worker = new Handler(thread.getLooper());
        worker.post(() -> {
            try {
                app.getContentResolver().registerContentObserver(ROOT, true, new ContentObserver(worker) {
                    @Override public void onChange(boolean self, Uri uri) {
                        String key = uri == null ? null : uri.getLastPathSegment();
                        if (key == null || !key.startsWith("prefs_key_")
                            || key.startsWith("prefs_key_home_gesture_enable")
                            || key.startsWith("prefs_key_home_gesture_shake")) schedule();
                    }
                });
                IntentFilter screen = new IntentFilter(Intent.ACTION_SCREEN_OFF);
                screen.addAction(Intent.ACTION_SCREEN_ON);
                screen.addAction(Intent.ACTION_USER_PRESENT);
                app.registerReceiver(new BroadcastReceiver() {
                    @Override public void onReceive(Context context, Intent intent) {
                        if (Intent.ACTION_SCREEN_OFF.equals(intent.getAction())) stop();
                        else schedule();
                    }
                }, screen, null, worker, Context.RECEIVER_NOT_EXPORTED);
                observersReady = true;
            } catch (RuntimeException unavailable) {
                ModuleLog.w(TAG, "Desktop shake observers unavailable", unavailable);
            }
        });
    }

    @Override public void onActivityResumed(Activity activity) {
        if (!launcherClass.equals(activity.getClass().getName())) return;
        launcher = activity;
        resumed = true;
        worker.post(this::schedule);
    }

    @Override public void onActivityPaused(Activity activity) {
        if (launcher != activity) return;
        launcher = null;
        resumed = false;
        worker.post(this::stop);
    }

    private void schedule() {
        worker.removeCallbacks(refresh);
        if (resumed) worker.post(refresh);
    }

    private void refresh() {
        if (!observersReady) return;
        if (!resumed || !app.getSystemService(PowerManager.class).isInteractive()
            || app.getSystemService(KeyguardManager.class).isKeyguardLocked()) {
            stop();
            return;
        }
        int next = 0;
        try (var cursor = app.getContentResolver().query(PREFS, null, null, null, null)) {
            if (cursor != null && cursor.moveToFirst() && cursor.getInt(0) == 1) {
                int enabled = cursor.getColumnIndexOrThrow("home_gesture_enable");
                int selected = cursor.getColumnIndexOrThrow("home_gesture_shake_action");
                if (cursor.getInt(enabled) == 1) next = cursor.getInt(selected);
            }
        } catch (RuntimeException unavailable) {
            ModuleLog.w(TAG, "Desktop shake configuration unavailable", unavailable);
        }
        if (next <= 0 || next > 100) {
            stop();
            return;
        }
        if (action != next) shake.reset();
        action = next;
        if (sensorRegistered || !resumed) return;
        sensors = app.getSystemService(SensorManager.class);
        Sensor accelerometer = sensors == null ? null : sensors.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        if (accelerometer == null) return;
        sensorRegistered = sensors.registerListener(this, accelerometer, SensorManager.SENSOR_DELAY_GAME, worker);
        if (sensorRegistered) ModuleLog.i(TAG, "Desktop shake sensor active");
    }

    private void stop() {
        worker.removeCallbacks(refresh);
        shake.reset();
        if (!sensorRegistered) return;
        sensorRegistered = false;
        sensors.unregisterListener(this);
        ModuleLog.i(TAG, "Desktop shake sensor released");
    }

    @Override public void onSensorChanged(SensorEvent event) {
        float[] values = event.values;
        if (sensorRegistered && resumed && values.length >= 3
            && shake.sample(values[0], values[1], values[2], event.timestamp / 1_000_000)) {
            boolean performed = HomeShakeWireOS4.perform();
            ModuleLog.i(TAG, "Desktop shake action accepted=" + performed);
        }
    }

    @Override public void onAccuracyChanged(Sensor sensor, int accuracy) {}
    @Override public void onActivityCreated(Activity activity, Bundle state) {}
    @Override public void onActivityStarted(Activity activity) {}
    @Override public void onActivityStopped(Activity activity) {}
    @Override public void onActivitySaveInstanceState(Activity activity, Bundle state) {}
    @Override public void onActivityDestroyed(Activity activity) {}
}

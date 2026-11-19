/* SPDX-License-Identifier: AGPL-3.0-or-later */
package com.sevtinge.hyperceiler.libhook.rules.home.gesture;

import android.app.ActivityManager;
import android.app.KeyguardManager;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.drawable.Drawable;
import android.media.AudioAttributes;
import android.os.Binder;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.IBinder;
import android.os.Looper;
import android.os.Parcel;
import android.os.PowerManager;
import android.os.SystemClock;
import android.os.VibrationEffect;
import android.os.Vibrator;
import android.provider.Settings;

import com.sevtinge.hyperceiler.common.log.ModuleLog;

import java.io.FileInputStream;
import java.lang.reflect.Method;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

/** Preview and commit use the same task snapshot; native code owns back-event cancellation. */
public final class Os4BackFeedback {
    private static final String TAG = "HyperCeiler.BackFeedback";
    private static final int BINDER_EVENT = 13;
    private static final BackHoldState state = new BackHoldState();
    private static volatile Handler main;
    private static Handler worker;
    private static Context app;
    private static volatile NativePreview nativePreview;
    private static boolean hapticSent;
    private static Target target;
    private static Target pending;
    private static long pendingTime;
    private static volatile long cancellation;
    private static boolean armed;
    private static final Runnable expire = Os4BackFeedback::reset;

    private Os4BackFeedback() {}

    private static void install(Context context) {
        if (main != null) {
            return;
        }
        app = context;
        HandlerThread thread = new HandlerThread("HC-BackFeedback");
        thread.start();
        worker = new Handler(thread.getLooper());
        main = new Handler(Looper.getMainLooper());
        main.post(() -> {
            IntentFilter filter = new IntentFilter(Intent.ACTION_SCREEN_OFF);
            filter.addAction(Intent.ACTION_CONFIGURATION_CHANGED);
            app.registerReceiver(new BroadcastReceiver() {
                @Override
                public void onReceive(Context context, Intent intent) {
                    reset();
                }
            }, filter);
        });
        ModuleLog.i(TAG, "feedback ready via WMS Binder");
    }

    private static Context systemContext() throws Exception {
        Class<?> activityThread = Class.forName("android.app.ActivityThread");
        Object thread = activityThread.getMethod("currentActivityThread").invoke(null);
        return (Context) activityThread.getMethod("getSystemContext").invoke(thread);
    }

    private static boolean launcher(Context context, int uid, int pid) throws Exception {
        if (uid < 10000 || pid <= 0
            || context.getPackageManager().getApplicationInfo("com.miui.home", 0).uid != uid) {
            return false;
        }
        byte[] expected = "com.miui.home".getBytes(java.nio.charset.StandardCharsets.US_ASCII);
        byte[] command = new byte[64];
        try (FileInputStream input = new FileInputStream("/proc/" + pid + "/cmdline")) {
            int count = input.read(command);
            if (count <= expected.length || command[expected.length] != 0) {
                return false;
            }
            for (int i = 0; i < expected.length; i++) {
                if (command[i] != expected[i]) {
                    return false;
                }
            }
        }
        return true;
    }

    public static synchronized boolean receiveIfMatches(Parcel data, Parcel reply, int flags) {
        int position = data.dataPosition();
        boolean selected = false;
        try {
            data.enforceInterface("android.view.IWindowManager");
            if (data.readInt() != BINDER_EVENT) {
                return false;
            }
            selected = true;
            if (reply == null || (flags & IBinder.FLAG_ONEWAY) != 0) {
                return true;
            }
            reply.writeByteArray(receive(data));
        } catch (Exception error) {
            if (selected) {
                ModuleLog.w(TAG, "feedback Binder request failed", error);
                reply.writeByteArray(ack(false));
            }
        } finally {
            if (!selected) {
                data.setDataPosition(position);
            }
        }
        return selected;
    }

    private static byte[] readPacket(Parcel data) {
        long stamp = data.readLong();
        long now = SystemClock.uptimeMillis();
        if (android.os.Process.myUid() != 1000 || stamp <= 0 || stamp > now || now - stamp > 750
            || data.dataAvail() != 4 + BackFeedbackProtocol.PACKET_BYTES) {
            return null;
        }
        byte[] packet = data.createByteArray();
        return data.dataAvail() == 0 && BackFeedbackProtocol.validPacket(packet) ? packet : null;
    }

    private static byte[] receive(Parcel data) throws Exception {
        byte[] packet = readPacket(data);
        if (packet == null) {
            return ack(false);
        }
        Context context = app != null ? app : systemContext();
        if (context == null || !launcher(context, Binder.getCallingUid(), Binder.getCallingPid())) {
            return ack(false);
        }
        long identity = Binder.clearCallingIdentity();
        try {
            install(context);
            return dispatch(packet);
        } finally {
            Binder.restoreCallingIdentity(identity);
        }
    }

    private static byte[] ack(boolean accepted) {
        return BackFeedbackProtocol.acknowledgement(accepted);
    }

    private static byte[] dispatch(byte[] packet) throws InterruptedException {
        if (!BackFeedbackProtocol.validPacket(packet)) {
            return ack(false);
        }
        ByteBuffer data = ByteBuffer.wrap(packet).order(ByteOrder.LITTLE_ENDIAN);
        data.getInt();
        int operation = data.getInt();
        boolean accepted = false;
        if (operation == 1) {
            long owner = data.getLong();
            int action = data.getInt();
            int pointers = data.getInt();
            float x = data.getFloat();
            float y = data.getFloat();
            long time = data.getLong();
            float density = data.getFloat();
            float width = data.getFloat();
            long revision = data.getLong();
            if (action == 1) {
                CountDownLatch done = new CountDownLatch(1);
                AtomicBoolean result = new AtomicBoolean();
                main.post(() -> {
                    try {
                        result.set(touch(owner, action, pointers, x, y, time, density, width, revision));
                    } finally {
                        done.countDown();
                    }
                });
                accepted = done.await(40, TimeUnit.MILLISECONDS) && result.get();
            } else {
                main.post(() -> touch(owner, action, pointers, x, y, time, density, width, revision));
            }
        } else if (operation == 4) {
            long known = data.getLong();
            NativePreview snapshot = nativePreview;
            return snapshot == null
                ? BackFeedbackProtocol.preview(0, false, null, known)
                : BackFeedbackProtocol.preview(snapshot.session, snapshot.left, snapshot.pixels, known);
        } else if (operation == 5) {
            long session = data.getLong();
            main.post(() -> presented(session));
        } else if (operation == 6) {
            main.post(Os4BackFeedback::disarm);
        } else if (operation == 2) {
            main.post(Os4BackFeedback::commit);
        } else if (operation == 3) {
            cancel();
        }
        return ack(accepted);
    }

    public static boolean touch(long owner, int action, int pointers, float x, float y, long time,
        float density, float width, long revision) {
        if (main == null || app == null || Looper.myLooper() != Looper.getMainLooper()) {
            return false;
        }
        if (!state.update(owner, action, pointers, x, y, time, density, width, revision)) {
            reset();
            return false;
        }
        if (action == 0) {
            cancellation++;
            hide();
            target = pending = null;
            main.removeCallbacks(expire);
            main.postDelayed(expire, 10000);
            long serial = state.serial;
            worker.post(() -> prepare(serial));
            return false;
        }
        if (action == 1) {
            boolean commit = armed && target != null && nativePreview != null && state.geometry(true);
            pending = commit ? target : null;
            pendingTime = SystemClock.uptimeMillis();
            state.reset();
            hide();
            target = null;
            main.removeCallbacks(expire);
            return commit;
        }
        if (armed && !state.geometry(true)) {
            disarm();
        }
        return false;
    }

    public static void cancel() {
        Handler handler = main;
        if (handler == null) {
            return;
        }
        if (Looper.myLooper() == handler.getLooper()) {
            reset();
        } else {
            handler.post(Os4BackFeedback::reset);
        }
    }

    public static void commit() {
        if (main == null || Looper.myLooper() != main.getLooper()) {
            return;
        }
        Target selected = pending;
        long time = pendingTime;
        long generation = cancellation;
        pending = null;
        if (selected != null && SystemClock.uptimeMillis() - time <= 750) {
            worker.post(() -> launch(selected, time, generation));
        }
    }

    private static void prepare(long serial) {
        Target selected = null;
        try {
            if (interactive()) {
                selected = selectTarget();
            }
        } catch (Exception error) {
            ModuleLog.w(TAG, "target lookup failed", error);
        }
        Target result = selected;
        main.post(() -> {
            if (!state.active || state.serial != serial) {
                return;
            }
            target = result;
            if (result != null) {
                nativePreview = new NativePreview(state.start, state.left(), result.pixels);
                ModuleLog.i(TAG, "native target prepared task=" + result.id + " session=" + state.start
                    + " ageMs=" + (SystemClock.uptimeMillis() - state.start));
            }
        });
    }

    private static void presented(long session) {
        if (hapticSent || !state.active || state.start != session || target == null || nativePreview == null) {
            return;
        }
        armed = true;
        hapticSent = true;
        boolean requested = false;
        try {
            if (Settings.System.getInt(app.getContentResolver(), Settings.System.HAPTIC_FEEDBACK_ENABLED,
                1) != 0) {
                Vibrator vibrator = (Vibrator) app.getSystemService(Context.VIBRATOR_SERVICE);
                if (vibrator != null && vibrator.hasVibrator()) {
                    vibrator.vibrate(VibrationEffect.createPredefined(VibrationEffect.EFFECT_TICK),
                        new AudioAttributes.Builder()
                            .setUsage(AudioAttributes.USAGE_ASSISTANCE_SONIFICATION)
                            .build());
                    requested = true;
                }
            }
        } catch (Exception error) {
            ModuleLog.w(TAG, "ready haptic failed", error);
        }
        ModuleLog.i(TAG, "native icon confirmed task=" + target.id + " hapticRequested=" + requested);
    }

    private static void disarm() {
        armed = false;
        hapticSent = false;
    }

    private static void hide() {
        nativePreview = null;
        disarm();
    }

    private static void reset() {
        cancellation++;
        state.reset();
        target = pending = null;
        if (main != null) {
            main.removeCallbacks(expire);
        }
        hide();
    }

    private static final class NativePreview {
        final long session;
        final boolean left;
        final byte[] pixels;

        NativePreview(long session, boolean left, byte[] pixels) {
            this.session = session;
            this.left = left;
            this.pixels = pixels;
        }
    }

    private static boolean interactive() {
        return ((PowerManager) app.getSystemService(Context.POWER_SERVICE)).isInteractive()
            && !((KeyguardManager) app.getSystemService(Context.KEYGUARD_SERVICE)).isKeyguardLocked();
    }

    private static Object service() throws Exception {
        return Class.forName("android.app.ActivityTaskManager").getMethod("getService").invoke(null);
    }

    private static List<?> recent(Object service, int user) throws Exception {
        Method method = Class.forName("android.app.IActivityTaskManager")
            .getMethod("getRecentTasks", int.class, int.class, int.class);
        Object slice = method.invoke(service, 32, 2, user);
        return (List<?>) slice.getClass().getMethod("getList").invoke(slice);
    }

    private static int integer(Object object, String name) throws Exception {
        return object.getClass().getField(name).getInt(object);
    }

    private static ComponentName component(Object task) throws Exception {
        ComponentName name = (ComponentName) task.getClass().getField("baseActivity").get(task);
        if (name == null) {
            name = (ComponentName) task.getClass().getField("topActivity").get(task);
        }
        return name;
    }

    private static ActivityManager.RunningTaskInfo current() {
        List<ActivityManager.RunningTaskInfo> tasks =
            ((ActivityManager) app.getSystemService(Context.ACTIVITY_SERVICE)).getRunningTasks(1);
        return tasks.isEmpty() ? null : tasks.get(0);
    }

    private static Target selectTarget() throws Exception {
        ActivityManager.RunningTaskInfo current = current();
        if (current == null) {
            return null;
        }
        int user = integer(current, "userId");
        for (Object task : recent(service(), user)) {
            int id = integer(task, "taskId");
            if (id < 0 || id == current.taskId || integer(task, "userId") != user) {
                continue;
            }
            Object config = task.getClass().getField("configuration").get(task);
            Object window = config.getClass().getField("windowConfiguration").get(config);
            if (((Integer) window.getClass().getMethod("getActivityType").invoke(window)) != 1) {
                continue;
            }
            ComponentName component = component(task);
            if (component == null) {
                continue;
            }
            PackageManager pm = app.getPackageManager();
            try {
                android.content.pm.ApplicationInfo info =
                    pm.getApplicationInfo(component.getPackageName(), 0);
                return new Target(current.taskId, id, user, component, pm.getApplicationIcon(info));
            } catch (PackageManager.NameNotFoundException ignored) {
            }
        }
        return null;
    }

    private static boolean containsTarget(Object service, Target selected) throws Exception {
        for (Object task : recent(service, selected.user)) {
            if (integer(task, "taskId") == selected.id && integer(task, "userId") == selected.user
                && selected.component.equals(component(task))) {
                return true;
            }
        }
        return false;
    }

    private static void launch(Target selected, long released, long generation) {
        try {
            if (generation != cancellation || SystemClock.uptimeMillis() - released > 750 || !interactive()) {
                return;
            }
            ActivityManager.RunningTaskInfo current = current();
            if (current == null || current.taskId != selected.source
                || integer(current, "userId") != selected.user) {
                return;
            }
            Object service = service();
            if (!containsTarget(service, selected) || generation != cancellation
                || SystemClock.uptimeMillis() - released > 750) {
                return;
            }
            Object result = Class.forName("android.app.IActivityTaskManager")
                .getMethod("startActivityFromRecents", int.class, Bundle.class)
                .invoke(service, selected.id, null);
            ModuleLog.i(TAG, "commit task=" + selected.id + " app=" + selected.component.getPackageName()
                + " result=" + result);
        } catch (Exception error) {
            ModuleLog.w(TAG, "selected task launch failed", error);
        }
    }

    private static final class Target {
        final int source;
        final int id;
        final int user;
        final ComponentName component;
        final byte[] pixels;

        Target(int source, int id, int user, ComponentName component, Drawable icon) {
            this.source = source;
            this.id = id;
            this.user = user;
            this.component = component;
            Bitmap bitmap = Bitmap.createBitmap(96, 96, Bitmap.Config.ARGB_8888);
            try {
                icon.setBounds(0, 0, 96, 96);
                icon.draw(new Canvas(bitmap));
                ByteBuffer buffer = ByteBuffer.allocate(96 * 96 * 4);
                bitmap.copyPixelsToBuffer(buffer);
                pixels = buffer.array();
            } finally {
                bitmap.recycle();
            }
        }
    }
}

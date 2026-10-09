/* SPDX-License-Identifier: AGPL-3.0-or-later */
package com.sevtinge.hyperceiler.libhook.rules.weather;

import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.os.Binder;
import android.os.IBinder;
import android.os.Parcel;
import android.os.SystemClock;
import com.sevtinge.hyperceiler.libhook.base.BaseLoad;
import com.sevtinge.hyperceiler.libhook.base.FrameworkHookGate;
import com.sevtinge.hyperceiler.libhook.provider.WeatherPrefsSnapshot;
import com.sevtinge.hyperceiler.libhook.utils.api.ContextUtils;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.UUID;

/** HWS1 configuration transport; transporting a setting does not implement its native consumer. */
public final class WeatherNativeEndpointOS4 {
    public static final int TRANSACTION_CODE = 0x00485753;
    public static final int ACK = 0x48575331;
    public static final int REPLY_BYTES = 48;
    private static final long LEASE_NS = 5_000_000_000L;
    private static final String PACKAGE = "com.miui.weather2";
    private static final String[][] SPECS = WeatherPrefsSnapshot.specs();
    private static final Object WORKER_LOCK = new Object();
    // One query lane per module classloader, even when a retired provider query ignores interrupt.
    private static Thread worker;
    private static WeatherNativeEndpointOS4 owner;
    private final Object lock = new Object();
    private final CallerVerifier verifier;
    private volatile boolean retired;
    private volatile Published published;
    private long epoch, revision, ticket;

    @FunctionalInterface interface CallerVerifier { boolean isWeather(int uid, int pid); }
    private record Values(int level, int card, int blur, boolean allow, boolean frameworkAllow,
        String reason, FrameworkHookGate.Fingerprint framework) { }
    private record Published(Values values, long epoch, long revision, long deadline) { }
    public record Snapshot(int available, int flags, long epoch, long revision, long deadline,
        int deviceLevel, int cardType, int blur) { }

    public WeatherNativeEndpointOS4() { this(WeatherNativeEndpointOS4::verifyWeatherProcess); }
    WeatherNativeEndpointOS4(CallerVerifier verifier) { this.verifier = verifier; }

    /** Install routing first. Neither receive nor readSnapshot starts or waits for this worker. */
    public void startPrefetch() {
        synchronized (WORKER_LOCK) {
            if (retired || (owner != null && owner != this && !owner.retired)) return;
            owner = this;
            if (worker != null) return;
            worker = new Thread(WeatherNativeEndpointOS4::workerLoop, "hc-weather-prefs");
            worker.setDaemon(true);
            worker.start();
        }
    }

    private static void workerLoop() {
        long delay = 1500;
        try {
            for (;;) {
                final WeatherNativeEndpointOS4 current;
                synchronized (WORKER_LOCK) {
                    current = owner;
                    if (current == null) return;
                }
                Thread.interrupted();
                delay = current.refreshFromProvider() ? 1500 : Math.min(delay * 2, 30000);
                synchronized (WORKER_LOCK) { if (owner != current) continue; }
                try { Thread.sleep(delay); } catch (InterruptedException changedOwner) { }
            }
        } finally {
            synchronized (WORKER_LOCK) {
                if (worker == Thread.currentThread()) {
                    worker = null;
                    // A replacement may have arrived between the null-owner check and finally.
                    if (owner != null && !owner.retired) owner.startPrefetch();
                }
            }
        }
    }

    /** Permanent retirement: an old query can never reactivate this generation. No blocking join. */
    public void retire() {
        synchronized (lock) {
            retired = true;
            ++ticket;
            invalidateLocked();
        }
        synchronized (WORKER_LOCK) {
            if (owner == this) {
                owner = null;
                if (worker != null) worker.interrupt();
            }
        }
    }

    private void invalidateLocked() {
        if (epoch == 0 || revision == Long.MAX_VALUE) { published = null; return; }
        published = new Published(null, epoch, ++revision, 0);
    }

    boolean refreshFromProvider() {
        final long attempt;
        synchronized (lock) {
            if (retired) return false;
            if (ticket == Long.MAX_VALUE || revision == Long.MAX_VALUE) {
                invalidateLocked();
                return false;
            }
            attempt = ++ticket;
        }
        final long started = SystemClock.elapsedRealtimeNanos();
        boolean success;
        try { success = refresh(attempt, started); }
        catch (RuntimeException | Error unavailable) { success = false; }
        synchronized (lock) {
            if (!success && ticket == attempt && !retired) invalidateLocked();
        }
        return success;
    }

    private boolean refresh(long attempt, long started) {
        if (started <= 0 || started > Long.MAX_VALUE - LEASE_NS) return false;
        final Context context = ContextUtils.getContextNoError(ContextUtils.FlAG_ONLY_ANDROID);
        if (context == null || context.getContentResolver() == null) return false;
        Object[] row = new Object[SPECS.length];
        try (Cursor cursor = context.getContentResolver().query(Uri.parse(
                "content://com.sevtinge.hyperceiler.provider.sharedprefs/weather_config"),
                null, null, null, null)) {
            if (cursor == null || cursor.getCount() != 1 || !cursor.moveToFirst()
                || cursor.getColumnCount() != SPECS.length + 1
                || !"schema".equals(cursor.getColumnName(0))
                || cursor.getType(0) != Cursor.FIELD_TYPE_INTEGER
                || cursor.getInt(0) != WeatherPrefsSnapshot.SCHEMA) return false;
            for (int i = 0; i < SPECS.length; i++) {
                int c = i + 1;
                if (!SPECS[i][1].equals(cursor.getColumnName(c))) return false;
                if (cursor.isNull(c)) continue;
                if (SPECS[i][0].equals("text")) {
                    if (cursor.getType(c) != Cursor.FIELD_TYPE_STRING) return false;
                    row[i] = cursor.getString(c);
                } else {
                    if (cursor.getType(c) != Cursor.FIELD_TYPE_INTEGER) return false;
                    if (SPECS[i][0].equals("long")) row[i] = cursor.getLong(c);
                    else {
                        long n = cursor.getLong(c);
                        if (n < Integer.MIN_VALUE || n > Integer.MAX_VALUE) return false;
                        row[i] = (int) n;
                    }
                }
            }
        }
        int level = number(row[0], 3), card = number(row[1], 0), blur = number(row[2], 0);
        int allow = number(row[3], 0), frameworkAllow = number(row[4], 1);
        if (!validPayload(level, card, blur) || !bit(allow) || !bit(frameworkAllow)) return false;
        Values values = new Values(level, card, blur, allow == 1, frameworkAllow == 1,
            (String) row[5], new FrameworkHookGate.Fingerprint((String) row[6], (String) row[7],
                row[8] == null ? Long.MIN_VALUE : (Long) row[8]));
        long nonce = UUID.randomUUID().getMostSignificantBits() & Long.MAX_VALUE;
        if (nonce == 0) nonce = 1;
        synchronized (lock) {
            long now = SystemClock.elapsedRealtimeNanos();
            if (retired || ticket != attempt || revision == Long.MAX_VALUE
                || now < started || now >= started + LEASE_NS) return false;
            if (epoch == 0) epoch = nonce;
            published = new Published(values, epoch, ++revision, started + LEASE_NS);
        }
        return true;
    }

    private static int number(Object value, int fallback) { return value == null ? fallback : (Integer) value; }
    private static boolean bit(int n) { return n == 0 || n == 1; }
    private static boolean validPayload(int level, int card, int blur) {
        return level >= 0 && level <= 3 && (card == 0 || card == 2 || card == 3) && bit(blur);
    }
    private static Snapshot unavailable(Published value) {
        return new Snapshot(0, 0, value == null ? 0 : value.epoch(),
            value == null ? 0 : value.revision(), 0, 3, 0, 0);
    }

    Snapshot readSnapshot() {
        for (int i = 0; i < 2; i++) {
            Published value = published;
            long now = SystemClock.elapsedRealtimeNanos();
            if (retired || value == null || value.values() == null || now <= 0
                || now >= value.deadline() || value.deadline() - now > LEASE_NS)
                return unavailable(value);
            Values v = value.values();
            boolean allowed = v.allow() && FrameworkHookGate.isAllowed(v.frameworkAllow(), v.reason(),
                () -> {
                    var current = BaseLoad.getXposed();
                    return current == null ? null : new FrameworkHookGate.Fingerprint(
                        current.getFrameworkName(), current.getFrameworkVersion(), current.getFrameworkVersionCode());
                }, v::framework);
            long finished = SystemClock.elapsedRealtimeNanos();
            if (retired || published != value) continue;
            if (finished < now || finished >= value.deadline()) return unavailable(value);
            if (!allowed) return new Snapshot(1, 0, value.epoch(), value.revision(), 0, 3, 0, 0);
            return new Snapshot(1, 1, value.epoch(), value.revision(), value.deadline(),
                v.level(), v.card(), v.blur());
        }
        return unavailable(published);
    }

    public Snapshot receive(Parcel data, int flags) {
        data.enforceInterface("android.view.IWindowManager");
        if ((flags & IBinder.FLAG_ONEWAY) != 0 || data.dataAvail() != 0) return unavailable(null);
        int uid = Binder.getCallingUid(), pid = Binder.getCallingPid();
        if (uid < 10000 || pid <= 0 || !verifier.isWeather(uid, pid)) return unavailable(null);
        return readSnapshot();
    }

    /** Fixed wire, neutral default level=3 (level=0 would incorrectly force low). */
    public static void writeReply(Parcel reply, Snapshot result) {
        Snapshot value = result;
        if (value == null || !bit(value.available()) || !bit(value.flags())
            || value.epoch() < 0 || value.revision() < 0
            || (value.epoch() == 0) != (value.revision() == 0)
            || (value.available() == 1 && value.epoch() == 0)
            || (value.flags() == 1 && (value.available() != 1 || value.deadline() <= 0
                || !validPayload(value.deviceLevel(), value.cardType(), value.blur())))) value = unavailable(null);
        if (value.flags() == 0) value = new Snapshot(value.available(), 0, value.epoch(), value.revision(), 0, 3, 0, 0);
        reply.setDataSize(0);
        reply.setDataPosition(0);
        reply.writeInt(ACK);
        reply.writeInt(value.available());
        reply.writeInt(value.flags());
        reply.writeLong(value.epoch());
        reply.writeLong(value.revision());
        reply.writeLong(value.deadline());
        reply.writeInt(value.deviceLevel());
        reply.writeInt(value.cardType());
        reply.writeInt(value.blur());
    }

    private static boolean verifyWeatherProcess(int uid, int pid) {
        try {
            Context context = ContextUtils.getContextNoError(ContextUtils.FlAG_ONLY_ANDROID);
            if (context == null || context.getPackageManager() == null) return false;
            String[] packages = context.getPackageManager().getPackagesForUid(uid);
            boolean owned = false;
            if (packages != null) for (String name : packages) if (PACKAGE.equals(name)) owned = true;
            if (!owned) return false;
            byte[] bytes = new byte[64];
            try (FileInputStream input = new FileInputStream("/proc/" + pid + "/cmdline")) {
                return exactProcess(bytes, input.read(bytes));
            }
        } catch (IOException | RuntimeException unavailable) { return false; }
    }

    static boolean exactProcess(byte[] bytes, int count) {
        byte[] expected = PACKAGE.getBytes(StandardCharsets.US_ASCII);
        if (count <= expected.length || count > bytes.length || bytes[expected.length] != 0) return false;
        for (int i = 0; i < expected.length; i++) if (bytes[i] != expected[i]) return false;
        return true;
    }
}

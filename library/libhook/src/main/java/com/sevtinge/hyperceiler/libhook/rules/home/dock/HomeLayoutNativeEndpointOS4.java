/* SPDX-License-Identifier: AGPL-3.0-or-later */
package com.sevtinge.hyperceiler.libhook.rules.home.dock;

import android.content.ContentResolver;
import android.database.ContentObserver;
import android.net.Uri;
import android.os.Binder;
import android.os.IBinder;
import android.os.Parcel;

import com.sevtinge.hyperceiler.common.utils.PrefsBridge;
import com.sevtinge.hyperceiler.libhook.base.BaseHook;
import com.sevtinge.hyperceiler.libhook.provider.HomeLayoutPrefsSnapshot;

import java.io.FileInputStream;
import java.io.IOException;

/** Synchronous, authenticated preference snapshot for the dex-less Rust launcher. */
public final class HomeLayoutNativeEndpointOS4 {
    public static final int TRANSACTION_CODE = 0x0048434C;
    public static final int ACK = 0x48434C34;
    private static final String DESCRIPTOR = "android.view.IWindowManager";
    /**
     * Where the current values come from.
     *
     * This endpoint runs in system_server, which cannot read the module's own preference file
     * (the app's data directory is not readable from here), so a file-based read silently falls
     * back to the LSPosed snapshot - and that snapshot only carries values that were pushed while
     * the module app was connected to its service, which is why a switch flipped in the settings
     * page used to have no effect until much later.
     *
     * The reliable source is the module app itself: it exposes the very same preferences through
     * its provider, and reading our own file is something it can always do. A background thread
     * pulls those values into a cache so that the launcher's synchronous transaction never waits
     * on a binder call. A refresh can start the module app, so idle refreshes must be sparse.
     */
    private static final String PREFS_AUTHORITY = "com.sevtinge.hyperceiler.provider.sharedprefs";
    // Physical settings writes notify /pref/<type>/<key>. Polling every 1.5 s kept the
    // module app and system_server waking even when the launcher was covered or asleep.
    private static final long PREFS_REFRESH_MS = 60000;
    private static final long UNOBSERVED_REFRESH_MS = 5000;
    private static final long INITIAL_RETRY_MS = 1500;
    private static final long CHANGE_MIN_INTERVAL_NS = 150_000_000L;
    /**
     * Cap on retries after failed cycles. Change notifications provide the prompt path while
     * this bounded retry also recovers from a provider that was unavailable during an update.
     */
    private static final long MAX_REFRESH_BACKOFF_MS = 30000;
    private static final String TAG = "HyperCeiler.HomeLayoutEndpoint";
    private static volatile java.util.Map<String, Object> cachedValues;
    private static volatile String lastDenied;
    private static volatile String lastAccepted;
    private static Thread refresher;
    private static boolean paused;
    private static long refreshEpoch;
    private static long notificationVersion;
    private static long queryVersion;
    private static long publishedQueryVersion;
    private static final String RELOAD_CACHE_KEY = "OS4.HomeLayout.ProviderSnapshot.v1";
    private static final Object refresherLock = new Object();

    /** Every key this endpoint reads, without the module's own key prefix. */
    private static final String[][] PREF_KEYS = HomeLayoutPrefsSnapshot.specs();

    private static boolean isLayoutChange(Uri uri) {
        if (uri == null) return true; // A broad invalidation must never be dropped.
        final String path = uri.toString();
        final int slash = path.lastIndexOf('/');
        final String segment = path.substring(slash + 1);
        final String key = segment.startsWith("prefs_key_")
            ? segment.substring("prefs_key_".length()) : segment;
        for (String[] spec : PREF_KEYS) {
            if (spec[1].equals(key)) return true;
        }
        return false;
    }

    private static void restoreProviderSnapshot() {
        if (cachedValues != null) return;
        synchronized (refresherLock) {
            if (cachedValues != null) return;
            final java.util.Map<?, ?> saved = BaseHook.getHotReloadRuntimeState(
                RELOAD_CACHE_KEY, java.util.Map.class);
            if (saved == null) return;
            final java.util.Map<String, Object> restored = new java.util.HashMap<>();
            for (java.util.Map.Entry<?, ?> entry : saved.entrySet()) {
                if (!(entry.getKey() instanceof String key)
                    || !(entry.getValue() instanceof Integer || entry.getValue() instanceof String)) return;
                Object value = entry.getValue();
                restored.put(key, value);
            }
            cachedValues = java.util.Collections.unmodifiableMap(restored);
        }
    }

    private static void ensureRefresher() {
        restoreProviderSnapshot();
        synchronized (refresherLock) {
            if (refresher != null || paused) return;
            final long epoch = refreshEpoch;
            refresher = new Thread(() -> {
                ContentResolver resolver = null;
                ContentObserver observer = null;
                long retry = INITIAL_RETRY_MS;
                long lastQueryAtNanos = 0;
                try {
                    try {
                        final android.content.Context context =
                            com.sevtinge.hyperceiler.libhook.utils.api.ContextUtils.getContextNoError(
                                com.sevtinge.hyperceiler.libhook.utils.api.ContextUtils.FlAG_ONLY_ANDROID);
                        if (context != null) {
                            resolver = context.getContentResolver();
                        }
                        if (resolver != null) {
                            final ContentObserver candidate = new ContentObserver(null) {
                                @Override public void onChange(boolean selfChange, Uri uri) {
                                    if (!isLayoutChange(uri)) return;
                                    synchronized (refresherLock) {
                                        ++notificationVersion;
                                        refresherLock.notifyAll();
                                    }
                                }
                            };
                            resolver.registerContentObserver(Uri.parse(
                                "content://" + PREFS_AUTHORITY + "/pref"), true, candidate);
                            observer = candidate;
                        }
                    } catch (RuntimeException | Error unavailable) {
                        // A broken content service must not crash system_server's worker.
                        // Unobserved systems continue with a bounded five-second fallback.
                    }
                    for (;;) {
                        final long beforeQuery;
                        synchronized (refresherLock) {
                            if (paused || refreshEpoch != epoch) return;
                            // Slider drags may emit dozens of writes per second. Coalesce them
                            // without delaying the first write or making the UI wait for release.
                            if (lastQueryAtNanos != 0) {
                                final long remaining = CHANGE_MIN_INTERVAL_NS
                                    - (System.nanoTime() - lastQueryAtNanos);
                                if (remaining > 0) {
                                    refresherLock.wait(Math.max(1, (remaining + 999_999) / 1_000_000));
                                    continue;
                                }
                            }
                            beforeQuery = notificationVersion;
                        }
                        final boolean success = refreshFromProvider();
                        lastQueryAtNanos = System.nanoTime();
                        final long period;
                        if (success) {
                            retry = INITIAL_RETRY_MS;
                            period = observer == null ? UNOBSERVED_REFRESH_MS : PREFS_REFRESH_MS;
                        } else {
                            retry = Math.min(retry * 2, MAX_REFRESH_BACKOFF_MS);
                            period = retry;
                        }
                        synchronized (refresherLock) {
                            if (paused || refreshEpoch != epoch) return;
                            // A write during the query needs a new whole-file revision now.
                            if (notificationVersion != beforeQuery) continue;
                            if (success && observer != null) {
                                // A committed snapshot is sufficient while settings do not change.
                                // Spurious wakeups must not query/start the app again either.
                                while (!paused && refreshEpoch == epoch
                                    && notificationVersion == beforeQuery) refresherLock.wait();
                            } else {
                                refresherLock.wait(period);
                            }
                        }
                    }
                } catch (InterruptedException cancelled) {
                    Thread.currentThread().interrupt();
                } finally {
                    if (resolver != null && observer != null) {
                        try { resolver.unregisterContentObserver(observer); }
                        catch (RuntimeException | Error unavailable) { /* Retiring after provider death. */ }
                    }
                    synchronized (refresherLock) {
                        if (refresher == Thread.currentThread()) refresher = null;
                    }
                }
            }, "hc-layout-prefs");
            refresher.setDaemon(true);
            refresher.start();
        }
    }

    /** Pause before the entry captures cross-generation state, never under WMS locks. */
    public static boolean pauseForHotReload() {
        final Thread worker;
        synchronized (refresherLock) {
            paused = true;
            ++refreshEpoch; // A query already in flight must not publish after retirement.
            worker = refresher;
            if (worker == null) return true;
            worker.interrupt();
            refresherLock.notifyAll();
        }
        try { worker.join(500); }
        catch (InterruptedException interrupted) { Thread.currentThread().interrupt(); return false; }
        synchronized (refresherLock) {
            if (worker.isAlive()) return false; // A stuck Binder query rejects the reload.
            if (refresher == worker) refresher = null;
            return true;
        }
    }

    public static void resumeAfterRejectedHotReload() {
        synchronized (refresherLock) { paused = false; }
    }

    /** A missing provider revision is NOT an authoritative zero/default native configuration. */
    private static Snapshot readReadyPreferences() {
        ensureRefresher();
        if (cachedValues == null) return null;
        return readPreferences();
    }

    /** One Binder query / one physical settings revision, including an explicit empty reset. */
    static boolean refreshFromProvider() {
        final long epoch;
        final long notificationAtQuery;
        final long query;
        synchronized (refresherLock) {
            if (paused) return false;
            epoch = refreshEpoch;
            notificationAtQuery = notificationVersion;
            query = ++queryVersion;
        }
        final android.content.Context context =
            com.sevtinge.hyperceiler.libhook.utils.api.ContextUtils.getContextNoError(
                com.sevtinge.hyperceiler.libhook.utils.api.ContextUtils.FlAG_ONLY_ANDROID);
        if (context == null) return false;
        final java.util.Map<String, Object> values = new java.util.HashMap<>();
        try {
            final android.content.ContentResolver resolver = context.getContentResolver();
            if (resolver == null) return false;
            try (final android.database.Cursor cursor = resolver.query(android.net.Uri.parse(
                    "content://" + PREFS_AUTHORITY + "/home_layout"), null, null, null, null)) {
                // Older providers return an empty / one-column cursor for an unknown path.
                // It must not erase a complete cache, especially while an APK is being replaced.
                if (cursor == null || !cursor.moveToFirst()
                    || cursor.getColumnCount() != PREF_KEYS.length + 1
                    || !"schema".equals(cursor.getColumnName(0))
                    || cursor.isNull(0) || cursor.getInt(0) != HomeLayoutPrefsSnapshot.SCHEMA) return false;
                for (int index = 0; index < PREF_KEYS.length; ++index) {
                    final String key = PREF_KEYS[index][1];
                    final int column = index + 1;
                    if (!key.equals(cursor.getColumnName(column))) return false;
                    if (!cursor.isNull(column)) {
                        Object value = "stringset".equals(PREF_KEYS[index][0])
                            ? cursor.getString(column) : cursor.getInt(column);
                        if (value instanceof String packet) HomeLayoutPrefsSnapshot.decodeTitleRecords(packet);
                        values.put(key, value);
                    }
                }
            }
        } catch (RuntimeException | Error unavailable) { return false; }
        final java.util.Map<String, Object> complete = java.util.Collections.unmodifiableMap(values);
        synchronized (refresherLock) {
            // An update received while this cursor was being read invalidates its
            // whole revision. Keep the last-good height until the existing worker
            // retries; never expose an older position for even one layout frame.
            // Also reject an older query completing after a newer accepted query.
            if (paused || refreshEpoch != epoch
                || notificationVersion != notificationAtQuery
                || query <= publishedQueryVersion) return false;
            publishedQueryVersion = query;
            // Boot-classloader Map/String/Integer only; no endpoint, callback or Thread crosses reload.
            BaseHook.putHotReloadRuntimeState(RELOAD_CACHE_KEY, complete);
            cachedValues = complete;
        }
        return true;
    }

    /**
     * One row per geometry knob, in the native table's order (HC_LAYOUT_KNOBS):
     * settings key stem, settings-page default in dp, then the page's own range.
     * The default is the neutral point - a slider left at its default produces a
     * zero delta and the launcher stays untouched.
     */
    private static final Object[][] KNOB_ROWS = {
        {"prefs_key_home_layout_hotseats_margin_bottom", 70, 0, 150},
        {"prefs_key_home_folder_vertical_spacing", 0, 0, 100},
        {"prefs_key_home_layout_workspace_padding_top", 30, 0, 150},
        {"prefs_key_home_layout_workspace_padding_bottom", 120, 0, 240},
        {"prefs_key_home_layout_workspace_padding_horizontal", 20, 0, 100},
        {"prefs_key_home_layout_indicator_margin_bottom", 70, -300, 700},
        {"prefs_key_home_layout_searchbar_margin_bottom", 30, 0, 150},
        {"prefs_key_home_layout_searchbar_width", 30, 0, 400},
    };
    /** The largest per-knob delta the wire accepts, in dp; the native side re-checks the same bound. */
    private static final int MAX_DELTA_DP = 2400;

    /**
     * The number of geometry knobs, so the host test can pin the length that must equal the native
     * side's HC_LAYOUT_KNOBS expansion. A drift between the two would silently stop every knob from
     * being accepted.
     */
    static int knobCount() {
        return KNOB_ROWS.length;
    }

    public record Snapshot(int acknowledgment, int gridEnabled, int cellX, int cellY,
        int[] knobEnabled, int[] knobDeltaDp, int[] tweaks, int[] title, String[][] customTitles,
        boolean widgetAllowMove, int[] folder, int[] backGesture, boolean folderAutoClose) {
        public Snapshot(int acknowledgment, int gridEnabled, int cellX, int cellY,
            int[] knobEnabled, int[] knobDeltaDp, int[] tweaks, int[] title, String[][] customTitles,
            boolean widgetAllowMove, int[] folder, int[] backGesture) {
            this(acknowledgment, gridEnabled, cellX, cellY, knobEnabled, knobDeltaDp, tweaks, title,
                customTitles, widgetAllowMove, folder, backGesture, false);
        }
        public Snapshot(int acknowledgment, int gridEnabled, int cellX, int cellY,
            int[] knobEnabled, int[] knobDeltaDp, int[] tweaks, int[] title, String[][] customTitles,
            boolean widgetAllowMove, int[] folder) {
            this(acknowledgment, gridEnabled, cellX, cellY, knobEnabled, knobDeltaDp, tweaks, title,
                customTitles, widgetAllowMove, folder, new int[]{60, 100});
        }
        public Snapshot(int acknowledgment, int gridEnabled, int cellX, int cellY,
            int[] knobEnabled, int[] knobDeltaDp, int[] tweaks, int[] title, String[][] customTitles,
            boolean widgetAllowMove) {
            this(acknowledgment, gridEnabled, cellX, cellY, knobEnabled, knobDeltaDp, tweaks, title,
                customTitles, widgetAllowMove, new int[FOLDER_COUNT]);
        }
        public Snapshot(int acknowledgment, int gridEnabled, int cellX, int cellY,
            int[] knobEnabled, int[] knobDeltaDp, int[] tweaks, int[] title, String[][] customTitles) {
            this(acknowledgment, gridEnabled, cellX, cellY, knobEnabled, knobDeltaDp, tweaks, title,
                customTitles, false);
        }
        public Snapshot(int acknowledgment, int gridEnabled, int cellX, int cellY,
            int[] knobEnabled, int[] knobDeltaDp, int[] tweaks, int[] title) {
            this(acknowledgment, gridEnabled, cellX, cellY, knobEnabled, knobDeltaDp, tweaks, title,
                new String[0][]);
        }
        public Snapshot(int acknowledgment, int gridEnabled, int cellX, int cellY,
            int[] knobEnabled, int[] knobDeltaDp, int[] tweaks) {
            this(acknowledgment, gridEnabled, cellX, cellY, knobEnabled, knobDeltaDp,
                tweaks, new int[]{12, 12, -1, 0});
        }
    }

    @FunctionalInterface
    interface CallerVerifier {
        boolean isLauncher(int uid, int pid);
    }

    @FunctionalInterface
    interface PreferenceReader {
        Snapshot read();
    }

    private final CallerVerifier verifier;
    private final PreferenceReader preferences;

    public HomeLayoutNativeEndpointOS4() {
        this(HomeLayoutNativeEndpointOS4::verifyLauncherProcess,
            HomeLayoutNativeEndpointOS4::readReadyPreferences);
    }

    HomeLayoutNativeEndpointOS4(CallerVerifier verifier, PreferenceReader preferences) {
        this.verifier = verifier;
        this.preferences = preferences;
    }

    public Snapshot receive(Parcel data, int flags) {
        data.enforceInterface(DESCRIPTOR);
        // An asynchronous Binder call does not carry a trustworthy caller PID on this ROM.
        if ((flags & IBinder.FLAG_ONEWAY) != 0 || data.dataAvail() != 0) {
            return denied("oneway or trailing data");
        }
        final int uid = Binder.getCallingUid();
        final int pid = Binder.getCallingPid();
        if (uid < 10000 || pid <= 0 || !verifier.isLauncher(uid, pid)) {
            return denied("caller " + uid + "/" + pid + " is not the launcher");
        }
        try {
            Snapshot read = preferences.read();
            if (read == null || (read.gridEnabled() != 0 && read.gridEnabled() != 1)
                    || read.cellX() < 3 || read.cellX() > 9
                    || read.cellY() < 4 || read.cellY() > 13
                    || read.knobEnabled().length != KNOB_ROWS.length
                    || read.knobDeltaDp().length != KNOB_ROWS.length
                    || read.tweaks().length != TWEAK_COUNT
                    || read.title() == null || read.title().length != TITLE_COUNT
                    || read.customTitles() == null || read.customTitles().length > 1024
                    || !validFolder(read.folder()) || !validBackGesture(read.backGesture())) {
                return denied("snapshot shape is wrong: " + describe(read));
            }
            for (int index = 0; index < KNOB_ROWS.length; ++index) {
                final int enabled = read.knobEnabled()[index];
                if (enabled != 0 && enabled != 1) return denied("knob " + index + " enable=" + enabled);
                if (Math.abs(read.knobDeltaDp()[index]) > MAX_DELTA_DP) {
                    return denied("knob " + index + " delta=" + read.knobDeltaDp()[index]);
                }
            }
            for (int index = 0; index < TWEAK_COUNT; ++index) {
                final int value = read.tweaks()[index];
                if (value < TWEAK_MIN[index] || value > TWEAK_MAX[index]) {
                    return denied("tweak " + index + " value=" + value);
                }
            }
            if (read.title()[0] < 0 || read.title()[0] > 20
                || read.title()[1] < 0 || read.title()[1] > 20
                || read.title()[3] < 0 || read.title()[3] > 1) {
                return denied("title size out of range");
            }
            final Snapshot accepted = new Snapshot(ACK, read.gridEnabled(), read.cellX(), read.cellY(),
            read.knobEnabled(), read.knobDeltaDp(), read.tweaks(), read.title(), read.customTitles(),
            read.widgetAllowMove(), read.folder().clone(), read.backGesture().clone(), read.folderAutoClose());
            final String summary = describe(accepted);
            if (!summary.equals(lastAccepted)) {
                lastAccepted = summary;
                android.util.Log.i(TAG, "layout endpoint serving " + summary);
            }
            int units = 0;
            for (String[] pair : accepted.customTitles()) {
                if (pair == null || pair.length != 2 || pair[0] == null || pair[1] == null
                    || pair[0].isEmpty() || pair[0].length() > 255
                    || pair[1].isEmpty() || pair[1].length() > 512) return denied("custom title shape");
                units += pair[0].length() + pair[1].length();
            }
            if (units > 32768) return denied("custom title packet bound");
            return accepted;
        } catch (RuntimeException exception) {
            return denied("reader threw " + exception.getClass().getSimpleName());
        }
    }

    /** One line per distinct outcome: the launcher's "config unavailable" cannot say which check failed. */
    private static Snapshot denied(String why) {
        if (!why.equals(lastDenied)) {
            lastDenied = why;
            android.util.Log.w(TAG, "layout endpoint denied: " + why);
        }
        return new Snapshot(ACK, 0, 0, 0, new int[KNOB_ROWS.length], new int[KNOB_ROWS.length],
            new int[TWEAK_COUNT]);
    }

    private static String describe(Snapshot snapshot) {
        if (snapshot == null) return "null";
        final StringBuilder text = new StringBuilder("grid=").append(snapshot.gridEnabled())
            .append(" cell=").append(snapshot.cellX()).append('x').append(snapshot.cellY());
        if (snapshot.knobEnabled() != null && snapshot.knobDeltaDp() != null) {
            text.append(" knobs=[");
            for (int index = 0; index < snapshot.knobEnabled().length
                     && index < snapshot.knobDeltaDp().length; ++index) {
                if (index != 0) text.append(',');
                text.append(snapshot.knobEnabled()[index]).append(':')
                    .append(snapshot.knobDeltaDp()[index]);
            }
            text.append(']');
        }
        if (snapshot.tweaks() != null) text.append(" tweaks=").append(snapshot.tweaks().length);
        if (snapshot.title() != null) text.append(" title=")
            .append(java.util.Arrays.toString(snapshot.title()));
        if (snapshot.customTitles() != null) text.append(" custom_titles=").append(snapshot.customTitles().length);
        text.append(" folder=").append(java.util.Arrays.toString(snapshot.folder()));
        return text.toString();
    }

    /**
     * The launcher's own geometry is in logical pixels (dp): `GridController.searchBarWidthPx`
     * answers 337 on a 369 dp-wide panel, `hotSeatsMarginBottom` 32, and every accessor wraps its
     * result in `PixelPerfect.alignPixel`, which only means anything for dp. The dp stored by the
     * settings page is therefore forwarded as-is: multiplying by the display density here used to
     * make every slider move 3.25x further than it said. A value outside the page's own range falls
     * back to its default, which keeps the neutral point exact; the resulting deltas are the knob's
     * distance from that neutral point.
     */
    static Snapshot readPreferences() {
        ensureRefresher();
        // Capture once: the refresher may publish during this Binder transaction. Mixing an old
        // enable with a new value would otherwise produce a position nobody actually selected.
        final java.util.Map<String, Object> values = cachedValues;
        boolean gridEnabled = readBoolean(values, "home_layout_unlock_grids_new", false);
        int cellX = readInt(values, "home_layout_unlock_grids_cell_x", 4);
        int cellY = readInt(values, "home_layout_unlock_grids_cell_y", 6);

        final int count = KNOB_ROWS.length;
        final int[] enabled = new int[count];
        final int[] deltas = new int[count];
        for (int index = 0; index < count; ++index) {
            // OS4 renders the old search bar as the Indicator capsule. Retired controls remain
            // visible but disabled in settings; ignore saved values too, so a previously enabled
            // search-bar tweak cannot keep moving the capsule behind that disabled UI.
            if (index == 6 || index == 7) continue;
            final String key = (String) KNOB_ROWS[index][0];
            final int fallback = (Integer) KNOB_ROWS[index][1];
            final int min = (Integer) KNOB_ROWS[index][2];
            final int max = (Integer) KNOB_ROWS[index][3];
            if (!readBoolean(values, key + "_enable", false)) continue;
            int value = readInt(values, key, fallback);
            if (value < min || value > max) value = fallback;
            enabled[index] = 1;
            deltas[index] = clampDelta(value - fallback);
        }
        // Reuse the two retired search-bar wire columns; no Binder payload/array ABI change.
        final Integer cachedMode = cached(values, "home_other_seek_points");
        int indicatorMode = cachedMode != null ? cachedMode
            : values != null ? 0 : PrefsBridge.getStringAsInt("home_other_seek_points", 0);
        if (indicatorMode < 0 || indicatorMode > 2) indicatorMode = 0;
        enabled[6] = enabled[7] = indicatorMode == 0 ? 0 : 1;
        deltas[6] = deltas[7] = indicatorMode;
        final int[] tweaks = new int[TWEAK_COUNT];
        /* The existing folder-column slider is the single control. Its default keeps the launcher
         * untouched; choosing any other value enables the native patch automatically. */
        final int folderColumns = readIntInRange(values, "home_folder_columns", 3, 0);
        tweaks[0] = folderColumns;
        tweaks[1] = folderColumns != 3 ? 1 : 0;
        tweaks[2] = readIntInRange(values, "home_layout_pad_major", 8, 2);
        tweaks[3] = readIntInRange(values, "home_layout_pad_minor", 5, 3);
        tweaks[4] = readBoolean(values, "home_layout_pad_grid_enable", false) ? 1 : 0;
        tweaks[5] = readIntInRange(values, "home_layout_fold_major", 8, 5);
        tweaks[6] = readIntInRange(values, "home_layout_fold_minor", 5, 6);
        tweaks[7] = readBoolean(values, "home_layout_fold_grid_enable", false) ? 1 : 0;
        /* Default level 70 = the page's "system default size" level; must match the SeekBar's
         * android:defaultValue and the native kIconScaleCodeDefault (both 0x66 for this level). */
        tweaks[8] = iconScaleCodeFor(readInt(values, "home_layout_icon_scale", 70));
        tweaks[9] = readBoolean(values, "home_layout_icon_scale_enable", false) ? 1 : 0;
        tweaks[10] = readBoolean(values, "home_layout_recents_hide_clear", false) ? 1 : 0;
        tweaks[11] = readBoolean(values, "home_layout_recents_no_clear", false) ? 1 : 0;
        /* Two fully independent animation controls, each with its own gate and duration ratio.
         * The page enforces 30..200 on both sliders (above 100 deliberately allowed so animations
         * can run slower); a stale or hand-edited value is clamped into that window here rather
         * than falling back to identity, so "as fast as allowed" survives a schema change. */
        tweaks[12] = readBoolean(values, "home_animation_open_rate_enable", false) ? 1 : 0;
        tweaks[13] = Math.max(30, Math.min(200, readInt(values, "home_animation_open_rate", 100)));
        tweaks[14] = readBoolean(values, "home_animation_recents_enable", false) ? 1 : 0;
        tweaks[15] = Math.max(30, Math.min(200, readInt(values, "home_animation_recents_rate", 100)));
        tweaks[16] = readBoolean(values, "home_dock_unlock_hotseat", false) ? 1 : 0;
        final int[] title = {
            readInt(values, "home_title_font_size", 12),
            readInt(values, "home_drawer_title_font_size", 12),
            readInt(values, "home_title_title_color", -1),
            readBoolean(values, "home_title_title_new_install", false) ? 1 : 0
        };
        for (int index = 0; index < 2; ++index) {
            if (title[index] < 0 || title[index] > 20) title[index] = 12;
        }
        return new Snapshot(ACK, gridEnabled ? 1 : 0, cellX, cellY, enabled, deltas, tweaks,
            title, readBoolean(values, "home_title_title_icontitlecustomization_onoff", false)
                ? HomeLayoutPrefsSnapshot.customTitles(values != null
                    && values.get("home_title_title_icontitlecustomization") instanceof String packet ? packet : null)
                : new String[0][],
            readBoolean(values, "home_widget_allow_moved_to_minus_one_screen", false), readFolder(values), new int[]{
                boundedBack(readInt(values, "home_navigation_back_area_height", 60), 10, 100, 60),
                boundedBack(readInt(values, "home_navigation_back_area_width", 100), 100, 400, 100)}, readBoolean(values, "home_folder_auto_close", false));
    }

    public static final int FOLDER_AUTO_CLOSE_MAGIC = 0x48434641;
    public static final int BACK_GESTURE_MAGIC = 0x48434231;
    private static int boundedBack(int value, int low, int high, int def) {
        return value >= low && value <= high ? value : def;
    }
    private static boolean validBackGesture(int[] values) {
        return values != null && values.length == 2 && values[0] >= 10 && values[0] <= 100
            && values[1] >= 100 && values[1] <= 400;
    }
    public static final int FOLDER_MAGIC = 0x48434631;
    public static final int FOLDER_COUNT = 7;
    private static final int[] FOLDER_MAX = {1, 1, 1, 50, 450, 200, 1};
    private static boolean validFolder(int[] values) {
        if (values == null || values.length != FOLDER_COUNT) return false;
        for (int i = 0; i < FOLDER_COUNT; i++)
            if (values[i] < 0 || values[i] > FOLDER_MAX[i]) return false;
        return true;
    }
    private static int[] readFolder(java.util.Map<String, Object> values) {
        Integer position = cached(values, "home_folder_title_pos");
        int alignment = position != null ? position : values != null ? 0
            : PrefsBridge.getStringAsInt("home_folder_title_pos", 0);
        int[] folder = {alignment,
            readBoolean(values, "home_folder_width", false) ? 1 : 0,
            readBoolean(values, "home_folder_horizontal_padding_enable", false) ? 1 : 0,
            readInt(values, "home_folder_horizontal_padding", 0),
            readInt(values, "home_folder_horizontal_padding_pad_h", 0),
            readInt(values, "home_folder_horizontal_padding_pad_v", 0),
            com.sevtinge.hyperceiler.libhook.utils.api.DeviceHelper.Miui.isPad() ? 1 : 0};
        for (int i = 0; i < FOLDER_COUNT; i++)
            if (folder[i] < 0 || folder[i] > FOLDER_MAX[i]) folder[i] = 0;
        return folder;
    }

    public static final int TITLE_COUNT = 4;

    /** Number of code-patch feature values, in the order the native side reads them. */
    static final int TWEAK_COUNT = 17;
    /** Accepted range per entry, so a bad value is refused here rather than applied to the launcher. */
    private static final int[] TWEAK_MIN = {1, 0, 2, 2, 0, 2, 2, 0, 0, 0, 0, 0, 0, 30, 0, 30, 0};
    private static final int[] TWEAK_MAX = {16, 1, 16, 16, 1, 16, 16, 1, 0xFF, 1, 1, 1, 1, 200, 1, 200, 1};

    /**
     * The launcher stores an icon scale as a packed code, not as a size, so the percentage shown on
     * the settings page has to be turned into the nearest code the launcher already accepts. The
     * packing is the launcher's own: a one-bit flag selects between two exponent forms, and the low
     * nibble carries the mantissa bits.
     *
     * <p>Note that only 80 codes are legal, and inside the settings page's 60..140 window they
     * collapse to just 19 distinct footprints - 62.5, 65.625, 68.75, 71.875, 75, ... on a 3.125
     * step up to 100 and a 6.25 step above it (the exponent boundary). The slider was narrowed to a
     * 1% step so the value can be <i>set</i> at that granularity, but the launcher quantises it back
     * to the code: several adjacent percentages resolve to the same code and therefore render
     * identically. Do not "fix" a reported "it does not change" by widening the step again - the
     * quantisation is the launcher's, not the page's. Anything that wants a visible per-detent
     * change has to snap the slider to one of these 19 values instead.
     */
    private static int iconScaleCodeFor(int percent) {
        final double wanted = percent / 100.0d;
        int best = 0x70;
        double bestDistance = Double.MAX_VALUE;
        for (int code = 0; code <= 0xFF; ++code) {
            if (!iconScaleCodeAllowed(code)) continue;
            final double distance = Math.abs(iconScaleValueOf(code) - wanted);
            if (distance < bestDistance) {
                bestDistance = distance;
                best = code;
            }
        }
        return best;
    }

    private static boolean iconScaleCodeAllowed(int code) {
        if (code < 0 || code > 0xFF || (code & 0x80) != 0) return false;
        if (((code >> 6) & 1) == 1) return true;
        return ((code >> 4) & 3) == 0;
    }

    private static double iconScaleValueOf(int code) {
        final int flag = (code >> 6) & 1;
        final int exponent = ((1 - flag) << 10) | ((flag != 0 ? 255 : 0) << 2) | ((code >> 4) & 3);
        final long bits = ((code >> 7) & 1L) << 63 | ((long) exponent) << 52
            | ((code & 15L) << 48);
        return Double.longBitsToDouble(bits);
    }

    /**
     * A value from the captured provider snapshot, or null when this key is absent.
     *
     * The cache is keyed the way the provider answers - without the module's `prefs_key_` prefix -
     * while the knob table stores keys exactly as the settings page spells them. Normalising here is
     * what makes the provider path reach the knob rows at all: before this, the lookup always missed
     * and every geometry knob silently fell back to the stale LSPosed snapshot, which is why the grid
     * (read through unprefixed keys) worked while the margins did not.
     */
    private static Integer cached(java.util.Map<String, Object> values, String key) {
        if (values == null) return null;
        final String normalized = key != null && key.startsWith("prefs_key_")
            ? key.substring("prefs_key_".length()) : key;
        return values.get(normalized) instanceof Integer value ? value : null;
    }

    /** Reads a boolean from one provider snapshot; known absence uses the page default. */
    private static boolean readBoolean(java.util.Map<String, Object> values, String key, boolean def) {
        final Integer value = cached(values, key);
        if (value != null) return value != 0;
        if (values != null) return def;
        return PrefsBridge.getBoolean(key, def);
    }

    /** Reads an int from one provider snapshot; LSPosed is only the startup fallback. */
    private static int readInt(java.util.Map<String, Object> values, String key, int def) {
        final Integer value = cached(values, key);
        if (value != null) return value;
        if (values != null) return def;
        return PrefsBridge.getInt(key, def);
    }

    /**
     * Reads an int for tweaks slot `index`, in that slot's own accepted range.
     *
     * The transaction refuses a whole snapshot whose tweaks run holds an out-of-range value, and a
     * snapshot refused here means every knob falls back to the stale cache with it. The settings
     * pages cannot write such a value, but a debug write or an older schema can, so the same
     * out-of-range condition the knob rows already treat as "use the default" is resolved here
     * rather than being allowed to take the whole snapshot down.
     */
    private static int readIntInRange(java.util.Map<String, Object> values, String key, int def, int index) {
        final int value = readInt(values, key, def);
        return value < TWEAK_MIN[index] || value > TWEAK_MAX[index] ? def : value;
    }

    /** The file stores every key with the module's prefix, whether or not the caller wrote one. */
    private static String fileKey(String key) {
        return key.startsWith("prefs_key_") ? key : "prefs_key_" + key;
    }

    private static int clampDelta(int deltaDp) {
        return Math.max(-MAX_DELTA_DP, Math.min(MAX_DELTA_DP, deltaDp));
    }

    /** Binder's synchronous PID plus its exact process name identify this caller before WMS binds a window. */
    private static boolean verifyLauncherProcess(int uid, int pid) {
        if (uid < 10000 || pid <= 0) return false;
        final byte[] command = new byte[64];
        try (FileInputStream input = new FileInputStream("/proc/" + pid + "/cmdline")) {
            final int count = input.read(command);
            final byte[] expected = "com.miui.home".getBytes(java.nio.charset.StandardCharsets.US_ASCII);
            if (count < expected.length + 1 || command[expected.length] != 0) return false;
            for (int index = 0; index < expected.length; ++index) {
                if (command[index] != expected[index]) return false;
            }
            return true;
        } catch (IOException | SecurityException ignored) {
            return false;
        }
    }
}

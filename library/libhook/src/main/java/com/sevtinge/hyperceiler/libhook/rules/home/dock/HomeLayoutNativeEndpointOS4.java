/* SPDX-License-Identifier: AGPL-3.0-or-later */
package com.sevtinge.hyperceiler.libhook.rules.home.dock;

import android.content.ContentResolver;
import android.database.ContentObserver;
import android.net.Uri;
import android.os.Binder;
import android.os.IBinder;
import android.os.Parcel;
import android.os.SystemClock;

import com.sevtinge.hyperceiler.common.utils.PrefsBridge;
import com.sevtinge.hyperceiler.libhook.base.BaseHook;
import com.sevtinge.hyperceiler.libhook.base.BaseLoad;
import com.sevtinge.hyperceiler.libhook.base.FrameworkHookGate;
import com.sevtinge.hyperceiler.libhook.provider.HomeLayoutPrefsSnapshot;

import java.io.FileInputStream;
import java.io.IOException;

/** Synchronous, authenticated preference snapshot for the dex-less Rust launcher. */
public final class HomeLayoutNativeEndpointOS4 {
    public static final int TRANSACTION_CODE = 0x0048434C;
    public static final int ACK = 0x48434C34;
    /** Separate versioned reply; older layout clients keep their existing wire format. */
    public static final int HYOS_TRANSACTION_CODE = 0x00484359;
    public static final int HYOS_ACK = 0x48435931;
    public static final int HYOS_ALLOWED = 1;
    public static final int HYOS_HIGH_MODELS = 2;
    public static final int HYOS_DISABLE_PRESTART = 4;
    public static final int HYOS_SOFT_GLASS = 8;
    public static final int HYOS_DOCK = 16;
    public static final int HYOS_HIDE_REPORT = 32;
    /** HCS1 is a fixed 188-byte atomic authorization + layout reply. HCY1/HCL4 stay unchanged. */
    public static final int COMBINED_TRANSACTION_CODE = 0x00484353;
    public static final int COMBINED_ACK = 0x48435331;
    public static final int COMBINED_REPLY_BYTES = 188;
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
    // HCS1 authorization expires after five BOOTTIME seconds, even without a settings write.
    // Renew on the existing worker; notifications still coalesce slider updates promptly.
    private static final long PREFS_REFRESH_MS = 1500;
    private static final long INITIAL_RETRY_MS = 1500;
    private static final long CHANGE_MIN_INTERVAL_NS = 150_000_000L;
    /**
     * Cap on retries after failed cycles. Change notifications provide the prompt path while
     * this bounded retry also recovers from a provider that was unavailable during an update.
     */
    private static final long MAX_REFRESH_BACKOFF_MS = 30000;
    private static final String TAG = "HyperCeiler.HomeLayoutEndpoint";
    private static volatile java.util.Map<String, Object> cachedValues;
    // Published with the same provider revision, never restored as live authorization.
    private static String cachedFrameworkReason;
    private static FrameworkHookGate.Fingerprint cachedFrameworkFingerprint;
    private static boolean hyosSnapshotLive;
    private static long hyosSnapshotAtNanos;
    private static final long HYOS_MAX_SNAPSHOT_AGE_NANOS = 5_000_000_000L;
    private static volatile String lastDenied;
    private static volatile String lastAccepted;
    private static Thread refresher;
    private static volatile boolean paused;
    private static long refreshEpoch;
    private static long notificationVersion;
    private static long refreshTicket;
    private static long combinedEpoch;
    private static long combinedRevision;
    private static volatile PublishedSnapshot combinedPublished;
    private static final String RELOAD_CACHE_KEY = "OS4.HomeLayout.ProviderSnapshot.v1";
    private static final Object refresherLock = new Object();

    /** Every key this endpoint reads, without the module's own key prefix. */
    private static final String[][] PREF_KEYS = HomeLayoutPrefsSnapshot.specs();

    // One immutable provider revision owns both authorization and the decoded layout.
    private record PublishedSnapshot(java.util.Map<String, Object> values, String reason,
        FrameworkHookGate.Fingerprint framework, Snapshot layout, long epoch, long revision,
        long deadline) { }

    public record CombinedSnapshot(int available, int flags, long epoch, long revision,
        long deadline, Snapshot layout) {
        public CombinedSnapshot { layout = copyLayout(layout); }
        @Override public Snapshot layout() { return copyLayout(layout); }
    }

    private static Snapshot copyLayout(Snapshot value) {
        if (value == null) return new Snapshot(ACK, 0, 0, 0, new int[KNOB_ROWS.length],
            new int[KNOB_ROWS.length], new int[TWEAK_COUNT]);
        final String[][] titles = new String[value.customTitles().length][];
        for (int i = 0; i < titles.length; ++i) titles[i] = value.customTitles()[i].clone();
        return new Snapshot(value.acknowledgment(), value.gridEnabled(), value.cellX(), value.cellY(),
            value.knobEnabled().clone(), value.knobDeltaDp().clone(), value.tweaks().clone(),
            value.title().clone(), titles, value.widgetAllowMove(), value.folder().clone(),
            value.backGesture().clone(), value.folderAutoClose(), value.objectScales().clone(),
            value.dockShowTitle(), value.recent().clone(), value.drawerHideSearch());
    }

    private static CombinedSnapshot unavailableCombined(PublishedSnapshot value) {
        return new CombinedSnapshot(0, 0, value == null ? 0 : value.epoch(),
            value == null ? 0 : value.revision(), 0, null);
    }

    /** Caller holds refresherLock. Revocation advances the same publication sequence. */
    private static void invalidateCombinedLocked() {
        hyosSnapshotLive = false;
        if (combinedEpoch == 0) { combinedPublished = null; return; }
        combinedPublished = new PublishedSnapshot(null, null, null, null, combinedEpoch,
            ++combinedRevision, 0);
    }

    public static void startCombinedPrefetch() { ensureRefresher(); }

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
                                        invalidateCombinedLocked();
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
                            period = PREFS_REFRESH_MS;
                        } else {
                            retry = Math.min(retry * 2, MAX_REFRESH_BACKOFF_MS);
                            period = retry;
                        }
                        synchronized (refresherLock) {
                            if (paused || refreshEpoch != epoch) return;
                            // A write during the query needs a new whole-file revision now.
                            if (notificationVersion != beforeQuery) continue;
                            refresherLock.wait(period);
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
            invalidateCombinedLocked();
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
        final long ticket;
        final long notificationAtQuery;
        synchronized (refresherLock) {
            if (paused) return false;
            epoch = refreshEpoch;
            ticket = ++refreshTicket;
            notificationAtQuery = notificationVersion;
        }
        // The lease begins BEFORE the provider call. A slow response cannot renew stale data.
        final long started = SystemClock.elapsedRealtimeNanos();
        boolean success;
        try { success = tryRefreshFromProvider(epoch, ticket, notificationAtQuery, started); }
        catch (RuntimeException | Error unavailable) { success = false; }
        synchronized (refresherLock) {
            if (!success && refreshEpoch == epoch && refreshTicket == ticket) {
                hyosSnapshotLive = false;
                invalidateCombinedLocked();
            }
        }
        return success;
    }

    private static boolean tryRefreshFromProvider(long epoch, long ticket, long notificationAtQuery, long started) {
        if (started <= 0 || started > Long.MAX_VALUE - HYOS_MAX_SNAPSHOT_AGE_NANOS) return false;
        if (!ensurePublicationEpoch(epoch, ticket)) return false;
        final ProviderSnapshot source = queryProviderSnapshot();
        if (source == null) return false;
        final java.util.Map<String, Object> complete = source.values();
        final Snapshot layout = decodeLayout(complete);
        final long deadline = started + HYOS_MAX_SNAPSHOT_AGE_NANOS;
        synchronized (refresherLock) {
            final long now = SystemClock.elapsedRealtimeNanos();
            if (paused || refreshEpoch != epoch || refreshTicket != ticket
                || notificationVersion != notificationAtQuery || now < started || now >= deadline) return false;
            // Boot-classloader Map/String/Integer only; no endpoint, callback or Thread crosses reload.
            BaseHook.putHotReloadRuntimeState(RELOAD_CACHE_KEY, complete);
            cachedValues = complete;
            cachedFrameworkReason = source.reason();
            cachedFrameworkFingerprint = source.framework();
            hyosSnapshotAtNanos = System.nanoTime();
            hyosSnapshotLive = true;
            combinedPublished = new PublishedSnapshot(complete, cachedFrameworkReason,
                cachedFrameworkFingerprint, layout, combinedEpoch, ++combinedRevision, deadline);
        }
        return true;
    }

    private static boolean ensurePublicationEpoch(long epoch, long ticket) {
        final boolean needsEpoch;
        synchronized (refresherLock) {
            if (paused || refreshEpoch != epoch || refreshTicket != ticket) return false;
            needsEpoch = combinedEpoch == 0;
        }
        if (needsEpoch) {
            // Once per generation, on the provider worker, outside publication/WMS locks.
            long nonce = java.util.UUID.randomUUID().getMostSignificantBits() & Long.MAX_VALUE;
            if (nonce == 0) nonce = 1;
            synchronized (refresherLock) {
                if (paused || refreshEpoch != epoch || refreshTicket != ticket) return false;
                if (combinedEpoch == 0) combinedEpoch = nonce;
            }
        }
        return true;
    }

    private record ProviderSnapshot(java.util.Map<String, Object> values, String reason,
        FrameworkHookGate.Fingerprint framework) { }

    private static ProviderSnapshot queryProviderSnapshot() {
        final android.content.Context context =
            com.sevtinge.hyperceiler.libhook.utils.api.ContextUtils.getContextNoError(
                com.sevtinge.hyperceiler.libhook.utils.api.ContextUtils.FlAG_ONLY_ANDROID);
        if (context == null) return null;
        final java.util.Map<String, Object> values = new java.util.HashMap<>();
        final java.util.Map<String, String> frameworkText = new java.util.HashMap<>();
        long frameworkVersionCode = Long.MIN_VALUE;
        try {
            final android.content.ContentResolver resolver = context.getContentResolver();
            if (resolver == null) return null;
            try (final android.database.Cursor cursor = resolver.query(android.net.Uri.parse(
                    "content://" + PREFS_AUTHORITY + "/home_layout"), null, null, null, null)) {
                // Older providers return an empty / one-column cursor for an unknown path.
                // It must not erase a complete cache, especially while an APK is being replaced.
                if (!isProviderSchema(cursor)) return null;
                for (int index = 0; index < PREF_KEYS.length; ++index) {
                    final String key = PREF_KEYS[index][1];
                    final int column = index + 1;
                    if (!key.equals(cursor.getColumnName(column))) return null;
                    if (cursor.isNull(column)) continue;
                    switch (PREF_KEYS[index][0]) {
                        case "text" -> frameworkText.put(key, cursor.getString(column));
                        case "stringset" -> {
                            final String packet = cursor.getString(column);
                            HomeLayoutPrefsSnapshot.decodeTitleRecords(packet);
                            values.put(key, packet);
                        }
                        case "long" -> frameworkVersionCode = cursor.getLong(column);
                        default -> values.put(key, cursor.getInt(column));
                    }
                }
            }
        } catch (RuntimeException | Error unavailable) { return null; }
        return new ProviderSnapshot(java.util.Collections.unmodifiableMap(values),
            frameworkText.get("framework_check_reason"),
            new FrameworkHookGate.Fingerprint(frameworkText.get("framework_check_name"),
                frameworkText.get("framework_check_version"), frameworkVersionCode));
    }

    private static boolean isProviderSchema(android.database.Cursor cursor) {
        return !(cursor == null || !cursor.moveToFirst()
                    || cursor.getColumnCount() != PREF_KEYS.length + 1
                    || !"schema".equals(cursor.getColumnName(0))
                    || cursor.isNull(0) || cursor.getInt(0) != HomeLayoutPrefsSnapshot.SCHEMA);
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
        boolean widgetAllowMove, int[] folder, int[] backGesture, boolean folderAutoClose, int[] objectScales,
        boolean dockShowTitle, int[] recent, boolean drawerHideSearch) {
        public Snapshot { recent = recent == null ? null : recent.clone(); }
        public Snapshot(int acknowledgment, int gridEnabled, int cellX, int cellY,
            int[] knobEnabled, int[] knobDeltaDp, int[] tweaks, int[] title, String[][] customTitles,
            boolean widgetAllowMove, int[] folder, int[] backGesture, boolean folderAutoClose,
            int[] objectScales, boolean dockShowTitle, int[] recent) {
            this(acknowledgment, gridEnabled, cellX, cellY, knobEnabled, knobDeltaDp, tweaks, title,
                customTitles, widgetAllowMove, folder, backGesture, folderAutoClose, objectScales,
                dockShowTitle, recent, false);
        }

        @Override public int[] recent() { return recent == null ? null : recent.clone(); }
        public Snapshot(int acknowledgment, int gridEnabled, int cellX, int cellY,
            int[] knobEnabled, int[] knobDeltaDp, int[] tweaks, int[] title, String[][] customTitles,
            boolean widgetAllowMove, int[] folder, int[] backGesture, boolean folderAutoClose,
            int[] objectScales, boolean dockShowTitle) {
            this(acknowledgment, gridEnabled, cellX, cellY, knobEnabled, knobDeltaDp, tweaks, title,
                customTitles, widgetAllowMove, folder, backGesture, folderAutoClose, objectScales,
                dockShowTitle, RECENT_DEFAULTS.clone());
        }
        public Snapshot(int acknowledgment, int gridEnabled, int cellX, int cellY,
            int[] knobEnabled, int[] knobDeltaDp, int[] tweaks, int[] title, String[][] customTitles,
            boolean widgetAllowMove, int[] folder, int[] backGesture, boolean folderAutoClose, int[] objectScales) {
            this(acknowledgment, gridEnabled, cellX, cellY, knobEnabled, knobDeltaDp, tweaks, title,
                customTitles, widgetAllowMove, folder, backGesture, folderAutoClose, objectScales, false);
        }
        public Snapshot(int acknowledgment, int gridEnabled, int cellX, int cellY,
            int[] knobEnabled, int[] knobDeltaDp, int[] tweaks, int[] title, String[][] customTitles,
            boolean widgetAllowMove, int[] folder, int[] backGesture, boolean folderAutoClose) {
            this(acknowledgment, gridEnabled, cellX, cellY, knobEnabled, knobDeltaDp, tweaks, title,
                customTitles, widgetAllowMove, folder, backGesture, folderAutoClose,
                new int[]{0, 100, 0, 100, 0, 100});
        }
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

    /** -1 means unavailable/denied, 0 is an authoritative disabled configuration. */
    public int receiveHyosFlags(Parcel data, int flags) {
        data.enforceInterface(DESCRIPTOR);
        if ((flags & IBinder.FLAG_ONEWAY) != 0 || data.dataAvail() != 0) return -1;
        final int uid = Binder.getCallingUid();
        final int pid = Binder.getCallingPid();
        if (uid < 10000 || pid <= 0 || !verifier.isLauncher(uid, pid)) return -1;
        return readHyosFlags();
    }

    static int readHyosFlags() {
        ensureRefresher();
        final java.util.Map<String, Object> values;
        final String frameworkReason;
        final FrameworkHookGate.Fingerprint checkedFramework;
        synchronized (refresherLock) {
            if (!hyosSnapshotLive || paused
                || System.nanoTime() - hyosSnapshotAtNanos > HYOS_MAX_SNAPSHOT_AGE_NANOS) return -1;
            values = cachedValues;
            frameworkReason = cachedFrameworkReason;
            checkedFramework = cachedFrameworkFingerprint;
        }
        // No JNI/remote-preference fallback in a Rust child. Activation requires
        // the module application's authoritative snapshot and normal hook gates.
        if (values == null) return -1;
        return deriveHyosFlags(values, frameworkReason, checkedFramework);
    }

    private static int deriveHyosFlags(java.util.Map<String, Object> values, String frameworkReason,
        FrameworkHookGate.Fingerprint checkedFramework) {
        if (!readBoolean(values, "allow_hook", false)) return 0;
        if (!FrameworkHookGate.isAllowed(readBoolean(values, "framework_api_allow_hook", true),
                frameworkReason, () -> {
                    final var framework = BaseLoad.getXposed();
                    if (framework == null) return null;
                    return new FrameworkHookGate.Fingerprint(framework.getFrameworkName(),
                        framework.getFrameworkVersion(), framework.getFrameworkVersionCode());
                }, () -> checkedFramework)) return 0;
        int flags = HYOS_ALLOWED;
        if (readBoolean(values, "home_other_high_models", false)) flags |= HYOS_HIGH_MODELS;
        if (readBoolean(values, "home_other_disable_prestart", false)) flags |= HYOS_DISABLE_PRESTART;
        final boolean dock = readBoolean(values, "home_dock_bg_custom_enable", false);
        if (dock) {
            flags |= HYOS_DOCK;
            if (readInt(values, "home_dock_add_blur", 0) == 1) flags |= HYOS_SOFT_GLASS;
        }
        if (readBoolean(values, "home_title_hide_report_text", false)) flags |= HYOS_HIDE_REPORT;
        return flags;
    }

    public CombinedSnapshot receiveCombinedSnapshot(Parcel data, int flags) {
        data.enforceInterface(DESCRIPTOR);
        if ((flags & IBinder.FLAG_ONEWAY) != 0 || data.dataAvail() != 0)
            return unavailableCombined(null);
        final int uid = Binder.getCallingUid();
        final int pid = Binder.getCallingPid();
        if (uid < 10000 || pid <= 0 || !verifier.isLauncher(uid, pid))
            return unavailableCombined(null);
        return readCombinedSnapshot();
    }

    static CombinedSnapshot readCombinedSnapshot() {
        ensureRefresher();
        // Framework inspection is outside the publication lock. Retry once when a concurrent
        // publication changes the identity; never mix old flags with a newer layout.
        CombinedSnapshot result = tryReadCombinedSnapshot();
        if (result != null) return result;
        result = tryReadCombinedSnapshot();
        return result == null ? unavailableCombined(combinedPublished) : result;
    }

    /** Null means the publication changed while its framework gate was inspected. */
    // A refreshed record with equal contents is still a new publication; validate its identity.
    @SuppressWarnings("PMD.CompareObjectsWithEquals")
    private static CombinedSnapshot tryReadCombinedSnapshot() {
        final PublishedSnapshot value = combinedPublished;
        final long now = SystemClock.elapsedRealtimeNanos();
        if (!isCombinedLeaseLive(value, now)) return unavailableCombined(value);
        final int flags = deriveHyosFlags(value.values(), value.reason(), value.framework());
        final long finished = SystemClock.elapsedRealtimeNanos();
        if (combinedPublished != value || paused) return null;
        if (finished < now || finished >= value.deadline()) return unavailableCombined(value);
        if (flags == 0) return new CombinedSnapshot(1, 0, value.epoch(), value.revision(), 0, null);
        if (!validCombinedLayout(value.layout())) return unavailableCombined(value);
        return new CombinedSnapshot(1, flags, value.epoch(), value.revision(),
            value.deadline(), value.layout());
    }

    private static boolean isCombinedLeaseLive(PublishedSnapshot value, long now) {
        return value != null && !paused && value.values() != null && now > 0
            && now < value.deadline() && value.deadline() - now <= HYOS_MAX_SNAPSHOT_AGE_NANOS;
    }

    private static boolean validCombinedLayout(Snapshot value) {
        if (!hasValidLayoutShape(value)) return false;
        for (int i = 0; i < KNOB_ROWS.length; ++i) {
            int enabled = value.knobEnabled()[i];
            int delta = value.knobDeltaDp()[i];
            if ((enabled != 0 && enabled != 1) || delta < -MAX_DELTA_DP || delta > MAX_DELTA_DP
                || (enabled == 0 && delta != 0)) return false;
        }
        for (int i = 0; i < TWEAK_COUNT; ++i)
            if (value.tweaks()[i] < TWEAK_MIN[i] || value.tweaks()[i] > TWEAK_MAX[i]) return false;
        return true;
    }

    private static boolean hasValidLayoutShape(Snapshot value) {
        return !(value == null || (value.gridEnabled() != 0 && value.gridEnabled() != 1)
            || value.cellX() < 3 || value.cellX() > 9 || value.cellY() < 4 || value.cellY() > 13
            || value.knobEnabled() == null || value.knobDeltaDp() == null || value.tweaks() == null
            || value.knobEnabled().length != KNOB_ROWS.length
            || value.knobDeltaDp().length != KNOB_ROWS.length || value.tweaks().length != TWEAK_COUNT);
    }

    /** Fixed HCS1 wire, including the canonical zero payload for denied/unavailable replies. */
    public static void writeCombinedReply(Parcel reply, CombinedSnapshot result) {
        final CombinedSnapshot value = result == null ? unavailableCombined(null) : result;
        final Snapshot layout = value.layout();
        reply.setDataSize(0);
        reply.setDataPosition(0);
        reply.writeInt(COMBINED_ACK);
        reply.writeInt(value.available());
        reply.writeInt(value.flags());
        reply.writeLong(value.epoch());
        reply.writeLong(value.revision());
        reply.writeLong(value.deadline());
        reply.writeInt(layout.gridEnabled());
        reply.writeInt(layout.cellX());
        reply.writeInt(layout.cellY());
        reply.writeInt(KNOB_ROWS.length);
        for (int i = 0; i < KNOB_ROWS.length; ++i) {
            reply.writeInt(layout.knobEnabled()[i]);
            reply.writeInt(layout.knobDeltaDp()[i]);
        }
        reply.writeInt(TWEAK_COUNT);
        for (int valuePart : layout.tweaks()) reply.writeInt(valuePart);
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
                    || !validFolder(read.folder()) || !validBackGesture(read.backGesture())
                    || !validObjectScales(read.objectScales()) || !validRecent(read.recent())) {
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
            read.widgetAllowMove(), read.folder().clone(), read.backGesture().clone(), read.folderAutoClose(), read.objectScales().clone(), read.dockShowTitle(), read.recent().clone(), read.drawerHideSearch());
            final String summary = describe(accepted);
            if (!summary.equals(lastAccepted)) {
                lastAccepted = summary;
                com.sevtinge.hyperceiler.common.log.ModuleLog.i(TAG, "layout endpoint serving " + summary);
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
            com.sevtinge.hyperceiler.common.log.ModuleLog.w(TAG, "layout endpoint denied: " + why);
        }
        return new Snapshot(ACK, 0, 0, 0, new int[KNOB_ROWS.length], new int[KNOB_ROWS.length],
            new int[TWEAK_COUNT]);
    }

    private static String describe(Snapshot snapshot) {
        if (snapshot == null) return "null";
        final StringBuilder text = new StringBuilder("endpoint_revision=" + ENDPOINT_REVISION + " grid=").append(snapshot.gridEnabled())
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
        text.append(" dock_show_title=").append(snapshot.dockShowTitle());
        text.append(" drawer_hide_search=").append(snapshot.drawerHideSearch());
        text.append(" recent=").append(java.util.Arrays.toString(snapshot.recent()));
        text.append(" object_scales=").append(java.util.Arrays.toString(snapshot.objectScales()));
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
        return decodeLayout(values);
    }

    private static Snapshot decodeLayout(java.util.Map<String, Object> values) {
        boolean gridEnabled = readBoolean(values, "home_layout_unlock_grids_new", false);
        int cellX = readInt(values, "home_layout_unlock_grids_cell_x", 4);
        int cellY = readInt(values, "home_layout_unlock_grids_cell_y", 6);

        final int count = KNOB_ROWS.length;
        final int[] enabled = new int[count];
        final int[] deltas = new int[count];
        decodeGeometryKnobs(values, enabled, deltas);
        decodeIndicatorMode(values, enabled, deltas);
        final int[] tweaks = decodeTweaks(values);
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
                boundedBack(readInt(values, "home_navigation_back_area_width", 100), 100, 400, 100)}, readBoolean(values, "home_folder_auto_close", false), readObjectScales(values),
            readBoolean(values, "home_dock_icon_title", false), readRecent(values), readBoolean(values, "home_drawer_hide_search", false));
    }

    private static void decodeGeometryKnobs(java.util.Map<String, Object> values,
                                            int[] enabled, int[] deltas) {
        for (int index = 0; index < KNOB_ROWS.length; ++index) {
            // OS4 renders the old search bar as the Indicator capsule. Retired controls remain
            // removed from the OS4 settings page; ignore saved values too, so a previously enabled
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
    }

    private static void decodeIndicatorMode(java.util.Map<String, Object> values,
                                           int[] enabled, int[] deltas) {
        // Reuse the two retired search-bar wire columns; no Binder payload/array ABI change.
        final Integer cachedMode = cached(values, "home_other_seek_points");
        int indicatorMode = cachedMode != null ? cachedMode
            : values != null ? 0 : PrefsBridge.getStringAsInt("home_other_seek_points", 0);
        if (indicatorMode < 0 || indicatorMode > 2) indicatorMode = 0;
        enabled[6] = enabled[7] = indicatorMode == 0 ? 0 : 1;
        deltas[6] = deltas[7] = indicatorMode;
    }

    private static int[] decodeTweaks(java.util.Map<String, Object> values) {
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
        tweaks[9] = readBit(values, "home_layout_icon_scale_enable");
        tweaks[10] = readBit(values, "home_layout_recents_hide_clear");
        tweaks[11] = readBit(values, "home_layout_recents_no_clear");
        /* Two fully independent animation controls, each with its own gate and duration ratio.
         * The page enforces 30..200 on both sliders (above 100 deliberately allowed so animations
         * can run slower); a stale or hand-edited value is clamped into that window here rather
         * than falling back to identity, so "as fast as allowed" survives a schema change. */
        tweaks[12] = readBit(values, "home_animation_open_rate_enable");
        tweaks[13] = Math.max(30, Math.min(200, readInt(values, "home_animation_open_rate", 100)));
        tweaks[14] = readBit(values, "home_animation_recents_enable");
        tweaks[15] = Math.max(30, Math.min(200, readInt(values, "home_animation_recents_rate", 100)));
        tweaks[16] = readBoolean(values, "home_dock_unlock_hotseat", false) ? 1 : 0;
        return tweaks;
    }

    public static final int ENDPOINT_DIAGNOSTICS_MAGIC = 0x48434431;
    public static final int ENDPOINT_DIAGNOSTICS_SCHEMA = 1;
    public static final int ENDPOINT_REVISION = 20261024;
    public static final int ENDPOINT_CAPABILITIES = 15; // independent object scales + Dock app titles + recents layout + drawer search

    public static final int DRAWER_MAGIC = 0x48434452;
    public static void writeDrawer(Parcel reply, boolean hideSearch) {
        reply.writeInt(DRAWER_MAGIC);
        reply.writeInt(hideSearch ? 1 : 0);
    }

    public static final int RECENT_MAGIC = 0x48435243;
    private static final int[] RECENT_DEFAULTS = {20,40,30,100,52,100,100,100,-1,-1,-1};
    private static final String[] RECENT_KEYS = {"task_view_corners", "task_view_header_height",
        "task_view_header_horizontal_offset", "home_recent_animation_speed", "home_recent_task_view_height",
        "task_view_horizontal1", "task_view_horizontal2", "home_recent_vertical_task_view_card_size",
        "home_recent_text_size", "home_recent_text_color", "home_recent_freeform_background_color"};
    public static void writeRecent(Parcel reply, int[] values) {
        final int[] snapshot = values == null ? null : values.clone();
        if (!validRecent(snapshot)) throw new IllegalArgumentException("invalid recents snapshot");
        reply.writeInt(RECENT_MAGIC);
        reply.writeInt(snapshot.length);
        for (int value : snapshot) reply.writeInt(value);
    }
    public static int[] recentDefaults() { return RECENT_DEFAULTS.clone(); }
    private static boolean validRecent(int[] values) {
        if (values==null || values.length!=RECENT_DEFAULTS.length) return false;
        final int[] low={0,0,0,10,30,50,50,50,-1}, high={180,150,150,500,70,150,150,150,30};
        for(int i=0;i<low.length;++i) if(values[i]<low[i] || values[i]>high[i]) return false;
        return true;
    }
    private static int[] readRecent(java.util.Map<String, Object> values) {
        final int[] result = RECENT_DEFAULTS.clone();
        final int[] low = {0,0,0,10,30,50,50,50,-1};
        final int[] high = {180,150,150,500,70,150,150,150,30};
        for (int i=0;i<result.length;++i) {
            int value = readInt(values, RECENT_KEYS[i], result[i]);
            if (i>=low.length || value>=low[i] && value<=high[i]) result[i]=value;
        }
        return result;
    }

    public static final int DOCK_TITLE_MAGIC = 0x48434454;
    public static final int OBJECT_SCALE_MAGIC = 0x48435331;
    private static boolean validObjectScales(int[] values) {
        if (values == null || values.length != 6) return false;
        for (int i = 0; i < 6; i += 2)
            if (values[i] < 0 || values[i] > 1 || values[i + 1] < 60 || values[i + 1] > 140) return false;
        return true;
    }
    private static int[] readObjectScales(java.util.Map<String, Object> values) {
        final String[] kinds = {"small_folder", "big_folder", "widget"};
        final int[] result = new int[6];
        for (int i = 0; i < kinds.length; ++i) {
            final String key = "home_layout_" + kinds[i] + "_scale";
            result[i * 2] = readBoolean(values, key + "_enable", false) ? 1 : 0;
            result[i * 2 + 1] = Math.max(60, Math.min(140, readInt(values, key, 100)));
        }
        return result;
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

    /** Uses one complete revision from the existing provider cache, without a second gesture poller. */
    public static int[] gestureConfig() {
        final java.util.Map<String, Object> values = cachedValues;
        int mask=0;
        String[] actions=com.sevtinge.hyperceiler.libhook.provider.HomeGesturePrefsSnapshot.ACTIONS;
        for(int i=0;i<com.sevtinge.hyperceiler.libhook.provider.HomeGesturePrefsSnapshot.NATIVE_ACTION_COUNT;i++) {
            int action=readInt(values,actions[i]+"_action",0);
            if(action>0 && action<=100)mask|=1<<i;
        }
        final android.content.Context context=com.sevtinge.hyperceiler.libhook.utils.api.ContextUtils.getContextNoError(
            com.sevtinge.hyperceiler.libhook.utils.api.ContextUtils.FlAG_ONLY_ANDROID);
        final android.util.DisplayMetrics dm=context!=null?context.getResources().getDisplayMetrics():null;
        return new int[]{readBoolean(values,"home_gesture_enable",false)?1:0,
            readBoolean(values,"home_navigation_quick_back",false)?1:0,mask,
            dm!=null?Math.max(80,Math.min(1280,dm.densityDpi)):160,
            dm!=null?Math.max(1,Math.min(32768,dm.widthPixels)):1};
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

    private static int readBit(java.util.Map<String, Object> values, String key) {
        return readBoolean(values, key, false) ? 1 : 0;
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

    /** Bind Binder's UID to the Home package as well as checking its exact live process name. */
    static boolean uidBelongsToLauncher(int uid) {
        if (uid < 10000) return false;
        try {
            final android.content.Context context =
                com.sevtinge.hyperceiler.libhook.utils.api.ContextUtils.getContextNoError(
                    com.sevtinge.hyperceiler.libhook.utils.api.ContextUtils.FlAG_ONLY_ANDROID);
            if (context == null || context.getPackageManager() == null) return false;
            final String[] packages = context.getPackageManager().getPackagesForUid(uid);
            if (packages == null) return false;
            for (String name : packages) if ("com.miui.home".equals(name)) return true;
        } catch (RuntimeException unavailable) { return false; }
        return false;
    }

    private static boolean verifyLauncherProcess(int uid, int pid) {
        if (pid <= 0 || !uidBelongsToLauncher(uid)) return false;
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

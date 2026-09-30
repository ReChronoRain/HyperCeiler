/* SPDX-License-Identifier: AGPL-3.0-or-later */
package com.sevtinge.hyperceiler.libhook.rules.home.dock;

import android.os.Binder;
import android.os.IBinder;
import android.os.Parcel;

import com.sevtinge.hyperceiler.common.utils.PrefsBridge;

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
     * on a binder call, and never has to start the module app if it is not running yet.
     */
    private static final String PREFS_AUTHORITY = "com.sevtinge.hyperceiler.provider.sharedprefs";
    private static final long PREFS_REFRESH_MS = 1500;
    /**
     * Cap on the refresh period after failed cycles (see refreshFromProvider). Large enough that a
     * blocked provider costs almost nothing, small enough that a value changed just after an unlock
     * still lands without the user wondering whether it worked.
     */
    private static final long MAX_REFRESH_BACKOFF_MS = 30000;
    private static final String TAG = "HyperCeiler.HomeLayoutEndpoint";
    private static volatile java.util.Map<String, Integer> cachedValues;
    private static volatile String lastDenied;
    private static volatile String lastAccepted;
    private static Thread refresher;
    private static final Object refresherLock = new Object();

    /** Every key this endpoint reads, without the module's own key prefix. */
    private static final String[][] PREF_KEYS = {
        {"boolean", "home_layout_unlock_grids_new"},
        {"integer", "home_layout_unlock_grids_cell_x"},
        {"integer", "home_layout_unlock_grids_cell_y"},
        {"boolean", "home_layout_hotseats_margin_bottom_enable"},
        {"integer", "home_layout_hotseats_margin_bottom"},
        {"boolean", "home_folder_vertical_spacing_enable"},
        {"integer", "home_folder_vertical_spacing"},
        {"boolean", "home_layout_workspace_padding_top_enable"},
        {"integer", "home_layout_workspace_padding_top"},
        {"boolean", "home_layout_workspace_padding_bottom_enable"},
        {"integer", "home_layout_workspace_padding_bottom"},
        {"boolean", "home_layout_workspace_padding_horizontal_enable"},
        {"integer", "home_layout_workspace_padding_horizontal"},
        {"boolean", "home_layout_indicator_margin_bottom_enable"},
        {"integer", "home_layout_indicator_margin_bottom"},
        {"string", "home_other_seek_points"},
        {"boolean", "home_dock_unlock_hotseat"},
        {"boolean", "home_layout_searchbar_margin_bottom_enable"},
        {"integer", "home_layout_searchbar_margin_bottom"},
        {"boolean", "home_layout_searchbar_width_enable"},
        {"integer", "home_layout_searchbar_width"},
        {"integer", "home_folder_columns"},
        {"boolean", "home_layout_pad_grid_enable"},
        {"integer", "home_layout_pad_major"},
        {"integer", "home_layout_pad_minor"},
        {"boolean", "home_layout_fold_grid_enable"},
        {"integer", "home_layout_fold_major"},
        {"integer", "home_layout_fold_minor"},
        {"boolean", "home_layout_icon_scale_enable"},
        {"integer", "home_layout_icon_scale"},
        {"boolean", "home_layout_recents_hide_clear"},
        {"boolean", "home_layout_recents_no_clear"},
        {"boolean", "home_animation_open_rate_enable"},
        {"integer", "home_animation_open_rate"},
        {"boolean", "home_animation_recents_enable"},
        {"integer", "home_animation_recents_rate"},
    };

    private static void ensureRefresher() {
        synchronized (refresherLock) {
            if (refresher != null) return;
            refresher = new Thread(() -> {
                long period = PREFS_REFRESH_MS;
                for (;;) {
                    /*
                     * A cycle that reached the provider keeps the normal period, so a settings change
                     * is still picked up in about a second. A cycle that could not reach it doubles
                     * the wait up to the cap: the provider lives in the module app, and MIUI's app
                     * freezer blocks it outright, which made the fixed period retry a refusal forever.
                     */
                    period = refreshFromProvider() ? PREFS_REFRESH_MS
                        : Math.min(period * 2, MAX_REFRESH_BACKOFF_MS);
                    try {
                        Thread.sleep(period);
                    } catch (InterruptedException ignored) {
                        return;
                    }
                }
            }, "hc-layout-prefs");
            refresher.setDaemon(true);
            refresher.start();
        }
    }

    /**
     * Ask the module app for every value in one pass. A failure leaves the previous cache intact
     * rather than clearing it: a moment without the module app must not turn every knob off.
     *
     * Returns whether the provider answered at all; false makes the caller back off.
     *
     * Only the first failing key is attempted: a refused query is a property of the provider, not of
     * the key. The module app being frozen answers every key with the same "Outgoing transactions
     * from this process must be FLAG_ONEWAY", and the Binder framework logs a stack trace for each
     * attempt, so walking the other eighteen keys bought eighteen more synchronous cross-process
     * calls and eighteen more stack traces for a result that could not differ. At the original fixed
     * period that was measured on device as a permanent twelve-calls-per-second rate with a matching
     * log flood inside system_server - and it never succeeded.
     */
    static boolean refreshFromProvider() {
        final android.content.Context context =
            com.sevtinge.hyperceiler.libhook.utils.api.ContextUtils.getContextNoError(
                com.sevtinge.hyperceiler.libhook.utils.api.ContextUtils.FlAG_ONLY_ANDROID);
        if (context == null) return false;
        final android.content.ContentResolver resolver = context.getContentResolver();
        if (resolver == null) return false;
        final java.util.Map<String, Integer> values = new java.util.HashMap<>();
        for (final String[] spec : PREF_KEYS) {
            final android.net.Uri uri = android.net.Uri.parse(
                "content://" + PREFS_AUTHORITY + "/pref/" + spec[0] + "/prefs_key_" + spec[1]);
            try (final android.database.Cursor cursor = resolver.query(uri, null, null, null, null)) {
                if (cursor == null || !cursor.moveToFirst()) continue;
                values.put(spec[1], cursor.getInt(0));
            } catch (RuntimeException | Error refused) {
                // One unreadable key must not cost the rest - but a provider that refuses the first
                // one refuses them all: keep whatever was read and report the cycle as unanswered.
                if (!values.isEmpty()) cachedValues = values;
                return false;
            }
        }
        if (values.isEmpty()) {
            // Either every key is genuinely absent or the provider answered nothing: the caller
            // should treat this like a failure and slow down, because the settings-page fallback
            // (the LSPosed snapshot) already covers "no value".
            return false;
        }
        cachedValues = values;
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
        int[] knobEnabled, int[] knobDeltaDp, int[] tweaks) { }

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
            HomeLayoutNativeEndpointOS4::readPreferences);
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
                    || read.tweaks().length != TWEAK_COUNT) {
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
            final Snapshot accepted = new Snapshot(ACK, read.gridEnabled(), read.cellX(), read.cellY(),
                read.knobEnabled(), read.knobDeltaDp(), read.tweaks());
            final String summary = describe(accepted);
            if (!summary.equals(lastAccepted)) {
                lastAccepted = summary;
                android.util.Log.i(TAG, "layout endpoint serving " + summary);
            }
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
        boolean gridEnabled = readBoolean("home_layout_unlock_grids_new", false);
        int cellX = readInt("home_layout_unlock_grids_cell_x", 4);
        int cellY = readInt("home_layout_unlock_grids_cell_y", 6);

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
            if (!readBoolean(key + "_enable", false)) continue;
            int value = readInt(key, fallback);
            if (value < min || value > max) value = fallback;
            enabled[index] = 1;
            deltas[index] = clampDelta(value - fallback);
        }
        // Reuse the two retired search-bar wire columns; no Binder payload/array ABI change.
        final Integer cachedMode = cached("home_other_seek_points");
        int indicatorMode = cachedMode != null ? cachedMode
            : PrefsBridge.getStringAsInt("home_other_seek_points", 0);
        if (indicatorMode < 0 || indicatorMode > 2) indicatorMode = 0;
        enabled[6] = enabled[7] = indicatorMode == 0 ? 0 : 1;
        deltas[6] = deltas[7] = indicatorMode;
        final int[] tweaks = new int[TWEAK_COUNT];
        /* The existing folder-column slider is the single control. Its default keeps the launcher
         * untouched; choosing any other value enables the native patch automatically. */
        final int folderColumns = readIntInRange("home_folder_columns", 3, 0);
        tweaks[0] = folderColumns;
        tweaks[1] = folderColumns != 3 ? 1 : 0;
        tweaks[2] = readIntInRange("home_layout_pad_major", 8, 2);
        tweaks[3] = readIntInRange("home_layout_pad_minor", 5, 3);
        tweaks[4] = readBoolean("home_layout_pad_grid_enable", false) ? 1 : 0;
        tweaks[5] = readIntInRange("home_layout_fold_major", 8, 5);
        tweaks[6] = readIntInRange("home_layout_fold_minor", 5, 6);
        tweaks[7] = readBoolean("home_layout_fold_grid_enable", false) ? 1 : 0;
        /* Default level 70 = the page's "system default size" level; must match the SeekBar's
         * android:defaultValue and the native kIconScaleCodeDefault (both 0x66 for this level). */
        tweaks[8] = iconScaleCodeFor(readInt("home_layout_icon_scale", 70));
        tweaks[9] = readBoolean("home_layout_icon_scale_enable", false) ? 1 : 0;
        tweaks[10] = readBoolean("home_layout_recents_hide_clear", false) ? 1 : 0;
        tweaks[11] = readBoolean("home_layout_recents_no_clear", false) ? 1 : 0;
        /* Two fully independent animation controls, each with its own gate and duration ratio.
         * The page enforces 30..200 on both sliders (above 100 deliberately allowed so animations
         * can run slower); a stale or hand-edited value is clamped into that window here rather
         * than falling back to identity, so "as fast as allowed" survives a schema change. */
        tweaks[12] = readBoolean("home_animation_open_rate_enable", false) ? 1 : 0;
        tweaks[13] = Math.max(30, Math.min(200, readInt("home_animation_open_rate", 100)));
        tweaks[14] = readBoolean("home_animation_recents_enable", false) ? 1 : 0;
        tweaks[15] = Math.max(30, Math.min(200, readInt("home_animation_recents_rate", 100)));
        tweaks[16] = readBoolean("home_dock_unlock_hotseat", false) ? 1 : 0;
        return new Snapshot(ACK, gridEnabled ? 1 : 0, cellX, cellY, enabled, deltas, tweaks);
    }

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
     * A cached value from the module app, or null when it has not delivered one yet.
     *
     * The cache is keyed the way the provider answers - without the module's `prefs_key_` prefix -
     * while the knob table stores keys exactly as the settings page spells them. Normalising here is
     * what makes the provider path reach the knob rows at all: before this, the lookup always missed
     * and every geometry knob silently fell back to the stale LSPosed snapshot, which is why the grid
     * (read through unprefixed keys) worked while the margins did not.
     */
    private static Integer cached(String key) {
        ensureRefresher();
        final java.util.Map<String, Integer> values = cachedValues;
        if (values == null) return null;
        final String normalized = key != null && key.startsWith("prefs_key_")
            ? key.substring("prefs_key_".length()) : key;
        return values.get(normalized);
    }

    /** Reads a boolean: the module's own provider when it has answered, else the LSPosed snapshot. */
    private static boolean readBoolean(String key, boolean def) {
        final Integer value = cached(key);
        if (value != null) return value != 0;
        return PrefsBridge.getBoolean(key, def);
    }

    /** Reads an int: the module's own provider when it has answered, else the LSPosed snapshot. */
    private static int readInt(String key, int def) {
        final Integer value = cached(key);
        if (value != null) return value;
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
    private static int readIntInRange(String key, int def, int index) {
        final int value = readInt(key, def);
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

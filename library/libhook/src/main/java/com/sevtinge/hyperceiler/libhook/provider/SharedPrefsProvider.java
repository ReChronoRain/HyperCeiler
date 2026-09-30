/*
 * This file is part of HyperCeiler.

 * HyperCeiler is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License.

 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.

 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.

 * Copyright (C) 2023-2026 HyperCeiler Contributions
 */
package com.sevtinge.hyperceiler.libhook.provider;

import android.content.ContentProvider;
import android.content.ContentValues;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.UriMatcher;
import android.content.res.AssetFileDescriptor;
import android.database.Cursor;
import android.database.MatrixCursor;
import android.net.Uri;
import android.os.Binder;
import android.os.ParcelFileDescriptor;
import android.os.Bundle;
import android.os.SystemProperties;
import android.util.Log;

import androidx.annotation.NonNull;

import com.sevtinge.hyperceiler.common.utils.PrefsBridge;
import com.sevtinge.hyperceiler.libhook.utils.hookapi.tool.AppsTool;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

public class SharedPrefsProvider extends ContentProvider {

    public static final String AUTHORITY = "com.sevtinge.hyperceiler.provider.sharedprefs";
    private static final UriMatcher uriMatcher = new UriMatcher(UriMatcher.NO_MATCH);

    /** Dock geometry snapshot kinds, see {@link #DOCK_GEOMETRY_KEYS}. */
    private static final String KIND_BOOLEAN = "b";
    private static final String KIND_INT = "i";
    private static final String KIND_STRING = "s";

    /** Prefix {@code PrefsBridge} adds to every key it stores or reads. */
    private static final String PREF_KEY_PREFIX = "prefs_key_";

    SharedPreferences prefs;
    private final DockGlassHost dockGlassHost = new DockGlassHost();

    @Override
    public Bundle call(String method, String arg, Bundle extras) {
        if (method != null && method.startsWith("dock_glass_")) {
            return dockGlassHost.call(getContext(), method, arg, extras);
        }
        if ("hc_debug_put".equals(method)) {
            return debugPut(arg);
        }
        return super.call(method, arg, extras);
    }

    /**
     * Debug-only preference writer.
     *
     * The settings page is the only other writer, and it is a UI: exercising a layout knob without a
     * tap needs a path that still goes through the real chain. This calls the very same
     * {@link PrefsBridge#putBoolean} / {@link PrefsBridge#putInt} the preference framework calls, so
     * physical storage, the remote snapshot and the change notification all behave exactly as they do
     * for a user tap - unlike a system property, which the launcher reads directly and which therefore
     * proves nothing about the real path.
     *
     * Off unless `debug.hyperceiler.prefs_write` is 1, and restricted to the shell/root caller, so a
     * normal application can never write another app's preferences through it.
     *
     * Usage from adb:
     *   adb shell setprop debug.hyperceiler.prefs_write 1
     *   adb shell content call --uri content://com.sevtinge.hyperceiler.provider.sharedprefs \
     *       --method hc_debug_put --arg boolean:home_layout_workspace_padding_top_enable:true
     */
    private Bundle debugPut(String arg) {
        final Bundle result = new Bundle();
        final int uid = Binder.getCallingUid();
        if (!SystemProperties.getBoolean("debug.hyperceiler.prefs_write", false)) {
            result.putBoolean("ok", false);
            result.putString("why", "debug.hyperceiler.prefs_write is not 1");
            return result;
        }
        if (uid != 2000 && uid != 0) {
            result.putBoolean("ok", false);
            result.putString("why", "caller uid " + uid + " is neither shell nor root");
            return result;
        }
        final String[] parts = arg == null ? new String[0] : arg.split(":", 3);
        if (parts.length != 3) {
            result.putBoolean("ok", false);
            result.putString("why", "argument must be <type>:<key>:<value>");
            return result;
        }
        try {
            switch (parts[0]) {
                case "boolean" -> PrefsBridge.putBoolean(parts[1], Boolean.parseBoolean(parts[2]));
                case "integer" -> PrefsBridge.putInt(parts[1], Integer.parseInt(parts[2]));
                case "string" -> PrefsBridge.putString(parts[1], parts[2]);
                default -> {
                    result.putBoolean("ok", false);
                    result.putString("why", "unknown type " + parts[0]);
                    return result;
                }
            }
        } catch (RuntimeException exception) {
            result.putBoolean("ok", false);
            result.putString("why", exception.getClass().getSimpleName());
            return result;
        }
        result.putBoolean("ok", true);
        return result;
    }

    static {
        uriMatcher.addURI(AUTHORITY, "string/*", 0);
        uriMatcher.addURI(AUTHORITY, "string/*/", 0);
        uriMatcher.addURI(AUTHORITY, "string/*/*", 1);
        uriMatcher.addURI(AUTHORITY, "integer/*", 2);
        uriMatcher.addURI(AUTHORITY, "integer/*/*", 2);
        uriMatcher.addURI(AUTHORITY, "boolean/*", 3);
        uriMatcher.addURI(AUTHORITY, "boolean/*/*", 3);
        uriMatcher.addURI(AUTHORITY, "stringset/*", 4);
        uriMatcher.addURI(AUTHORITY, "pref/*/*", 7);
        uriMatcher.addURI(AUTHORITY, "dock_geometry", 8);
        uriMatcher.addURI(AUTHORITY, "test/*", 5);
        uriMatcher.addURI(AUTHORITY, "shortcut_icon/*", 6);
    }

    /**
     * Preferences the Dock geometry snapshot is built from, in the column order
     * {@link #DOCK_GEOMETRY_COLUMNS} declares.
     *
     * <p>Each entry is {@code {key, kind}} where kind is one of
     * {@link #KIND_BOOLEAN}, {@link #KIND_INT} or {@link #KIND_STRING}. The keys are the raw
     * preference names without the {@code prefs_key_} prefix, matching what
     * {@code PrefsBridge} stores after its own wrapping.
     */
    private static final String[][] DOCK_GEOMETRY_KEYS = {
        {"home_dock_bg_custom_enable", "b"},
        {"home_dock_add_blur", "s"},
        {"home_dock_bg_color", "i"},
        {"home_dock_bg_height", "i"},
        {"home_dock_bg_margin_horizontal", "i"},
        {"home_dock_bg_margin_bottom", "i"},
        {"home_dock_bg_radius", "i"},
        {"home_other_home_mode", "s"},
    };

    /** Column names of the dock geometry cursor, one per {@link #DOCK_GEOMETRY_KEYS} row. */
    private static final String[] DOCK_GEOMETRY_COLUMNS = {
        "custom_enable", "add_blur", "bg_color", "bg_height",
        "margin_horizontal", "margin_bottom", "bg_radius", "home_mode"
    };

    @Override
    public boolean onCreate() {
        try {
            prefs = PrefsBridge.getSharedPreferences();
            if (prefs == null && getContext() != null) {
                prefs = getContext().getSharedPreferences(PrefsBridge.PREFS_NAME, Context.MODE_PRIVATE);
            }
            return true;
        } catch (Throwable throwable) {
            return false;
        }
    }

    @Override
    public Cursor query(Uri uri, String[] projection, String selection, String[] selectionArgs, String sortOrder) {
        if (AUTHORITY.equals(uri.getAuthority()) && "/diagnostics/dock_glass_api".equals(uri.getPath())) {
            return DockGlassDiagnostics.query();
        }
        if (prefs == null) {
            // A missing preference handle is unavailable, not a successful all-keys deletion.
            // Layout readers retain their last-good snapshot when the provider returns null.
            return null;
        }
        List<String> parts = uri.getPathSegments();
        MatrixCursor cursor = new MatrixCursor(new String[]{"data"});
        int match = uriMatcher.match(uri);
        if (!isValidPath(parts, match)) {
            return cursor;
        }

        switch (match) {
            case 0 -> {
                cursor.newRow().add("data", prefs.getString(parts.get(1), ""));
                return cursor;
            }
            case 1 -> {
                cursor.newRow().add("data", prefs.getString(parts.get(1), parts.get(2)));
                return cursor;
            }
            case 2 -> {
                int defValue = parts.size() >= 3 ? parseIntOrDefault(parts.get(2), 0) : 0;
                cursor.newRow().add("data", prefs.getInt(parts.get(1), defValue));
                return cursor;
            }
            case 3 -> {
                int defValue = parts.size() >= 3 ? parseIntOrDefault(parts.get(2), 0) : 0;
                cursor.newRow().add("data", prefs.getBoolean(parts.get(1), defValue == 1) ? 1 : 0);
                return cursor;
            }
            case 4 -> {
                Set<String> strings = prefs.getStringSet(parts.get(1), new LinkedHashSet<>());
                for (String str : strings) cursor.newRow().add("data", str);
                return cursor;
            }
            case 7 -> {
                if (parts.size() < 3) {
                    return cursor;
                }
                String prefType = parts.get(1);
                String prefName = parts.get(2);
                /*
                 * A key that was never written answers with no row, not with a type default.
                 *
                 * The reader is the layout endpoint, and "no row" is what lets it fall back to the
                 * settings page's own default. Answering 0 for every absent integer turned each
                 * untouched preference into a real zero, and a single out-of-range zero anywhere in
                 * the snapshot made the launcher reject the whole thing - so a settings page change
                 * had no effect at all. `prefs.contains` is the only way to tell absent from zero.
                 */
                if (prefs == null || !prefs.contains(prefName)) {
                    return cursor;
                }
                switch (prefType) {
                    case "string" -> cursor.newRow().add("data", prefs.getString(prefName, ""));
                    case "integer" -> cursor.newRow().add("data", prefs.getInt(prefName, 0));
                    case "boolean" -> cursor.newRow().add("data", prefs.getBoolean(prefName, false) ? 1 : 0);
                    case "stringset" -> {
                        Set<String> strings = prefs.getStringSet(prefName, new LinkedHashSet<>());
                        for (String str : strings) cursor.newRow().add("data", str);
                    }
                    default -> {
                    }
                }
                return cursor;
            }
            case 8 -> {
                return dockGeometryCursor();
            }
        }
        return null;
    }

    /**
     * One cursor row holding the whole Dock geometry snapshot.
     *
     * <p>The geometry knobs are consumed inside system_server, where LSPosed's remote
     * preferences are a snapshot that only advances when the daemon pushes an update. When
     * that push is lost the value stays stale for the rest of the process lifetime, and
     * neither a launcher restart nor a desktop reload refreshes it — so a height change
     * silently does nothing. This provider reads the very file the settings UI wrote, so a
     * single call is what makes every geometry knob take effect at all.
     *
     * <p>Answering all eight values in one cursor is deliberate: the caller polls this on the
     * one-second sweep, and eight separate queries would be eight synchronous Binder round
     * trips through system_server instead of one.
     *
     * <p>An absent key yields a null column rather than a type default. The caller then keeps
     * the value it read from {@code PrefsBridge}, which preserves the settings page's own
     * default instead of turning an untouched preference into a real zero.
     */
    private Cursor dockGeometryCursor() {
        MatrixCursor cursor = new MatrixCursor(DOCK_GEOMETRY_COLUMNS);
        if (prefs == null) {
            return cursor;
        }
        MatrixCursor.RowBuilder row = cursor.newRow();
        for (String[] entry : DOCK_GEOMETRY_KEYS) {
            String key = PREF_KEY_PREFIX + entry[0];
            if (!prefs.contains(key)) {
                row.add(null);
                continue;
            }
            try {
                switch (entry[1]) {
                    case KIND_BOOLEAN -> row.add(prefs.getBoolean(key, false) ? 1 : 0);
                    case KIND_INT -> row.add(prefs.getInt(key, 0));
                    default -> row.add(prefs.getString(key, null));
                }
            } catch (ClassCastException mismatch) {
                // A preference stored as one type but typed as another: report absent so the
                // caller keeps the PrefsBridge value instead of failing the whole snapshot.
                row.add(null);
            }
        }
        return cursor;
    }

    private boolean isValidPath(List<String> parts, int match) {
        if (parts == null) {
            return false;
        }
        return switch (match) {
            case 0, 2, 3, 4, 5, 6 -> parts.size() >= 2;
            case 1, 7 -> parts.size() >= 3;
            case 8 -> true;
            default -> false;
        };
    }

    private int parseIntOrDefault(String value, int defValue) {
        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException ignored) {
            return defValue;
        }
    }

    @Override
    public AssetFileDescriptor openAssetFile(@NonNull Uri uri, @NonNull String mode) throws FileNotFoundException {
        if (getContext() == null) return null;

        List<String> parts = uri.getPathSegments();
        if (uriMatcher.match(uri) == 5) {
            String filename = null;
            if ("0".equals(parts.get(1))) filename = "test0.png";
            else if ("1".equals(parts.get(1))) filename = "test1.mp3";
            else if ("2".equals(parts.get(1))) filename = "test2.mp4";
            else if ("3".equals(parts.get(1)) || "5".equals(parts.get(1))) filename = "test3.txt";
            else if ("4".equals(parts.get(1))) filename = "test4.zip";

            AssetFileDescriptor afd = null;
            if (filename != null) try {
                afd = getContext().getAssets().openFd(filename);
            } catch (Throwable t) {
                Log.i("afd", String.valueOf(t));
            }
            return afd;
        } else if (uriMatcher.match(uri) == 6) {
            Context context = AppsTool.getProtectedContext(getContext());
            File file = new File(context.getFilesDir() + "/shortcuts/" + parts.get(1) + "_shortcut.png");
            if (!file.exists()) return null;
            return new AssetFileDescriptor(ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_READ_ONLY), 0, AssetFileDescriptor.UNKNOWN_LENGTH);
        }

        return null;
    }

    @Override
    public String getType(@NonNull Uri uri) {
        return null;
    }

    @Override
    public Uri insert(@NonNull Uri uri, ContentValues values) {
        return null;
    }

    @Override
    public int delete(@NonNull Uri uri, String selection, String[] selectionArgs) {
        return 0;
    }

    @Override
    public int update(@NonNull Uri uri, ContentValues values, String selection, String[] selectionArgs) {
        return 0;
    }

}

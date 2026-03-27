package com.sevtinge.hyperceiler.common.utils;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.text.TextUtils;

import androidx.annotation.Nullable;

import com.sevtinge.hyperceiler.common.log.AndroidLog;

import java.util.Map;

public final class AppSettingsStore {

    private static final String TAG = "AppSettingsStore";
    private static final Uri GLOBAL_URI = Uri.parse("content://hyperceiler.provider.settings/global");
    private static final String COLUMN_NAME = "name";
    private static final String COLUMN_VALUE = "value";

    public static final String KEY_HIDE_APP_ICON = "settings_hide_app_icon";
    public static final String KEY_FLOAT_NAV = "settings_float_nav";
    public static final String KEY_SCOPE_SYNC = "settings_scope_sync";
    public static final String KEY_ICON = "settings_icon";
    public static final String KEY_ICON_MODE = "settings_icon_mode";
    public static final String KEY_APP_LANGUAGE = "settings_app_language";

    public static final String PREF_HIDE_APP_ICON = "prefs_key_settings_hide_app_icon";
    public static final String PREF_FLOAT_NAV = "prefs_key_settings_float_nav";
    public static final String PREF_SCOPE_SYNC = "prefs_key_settings_scope_sync";
    public static final String PREF_ICON = "prefs_key_settings_icon";
    public static final String PREF_ICON_MODE = "prefs_key_settings_icon_mode";
    public static final String PREF_APP_LANGUAGE = "prefs_key_settings_app_language";

    private AppSettingsStore() {
    }

    public static boolean isHideAppIconEnabled(@Nullable Context context) {
        return getBoolean(context, KEY_HIDE_APP_ICON, PREF_HIDE_APP_ICON, true);
    }

    public static void setHideAppIconEnabled(@Nullable Context context, boolean enabled) {
        putBoolean(context, KEY_HIDE_APP_ICON, PREF_HIDE_APP_ICON, enabled);
    }

    public static boolean isFloatNavEnabled(@Nullable Context context) {
        return getBoolean(context, KEY_FLOAT_NAV, PREF_FLOAT_NAV, false);
    }

    public static void setFloatNavEnabled(@Nullable Context context, boolean enabled) {
        putBoolean(context, KEY_FLOAT_NAV, PREF_FLOAT_NAV, enabled);
    }

    public static boolean isScopeSyncEnabled(@Nullable Context context) {
        return getBoolean(context, KEY_SCOPE_SYNC, PREF_SCOPE_SYNC, false);
    }

    public static void setScopeSyncEnabled(@Nullable Context context, boolean enabled) {
        putBoolean(context, KEY_SCOPE_SYNC, PREF_SCOPE_SYNC, enabled);
    }

    public static int getIconIndex(@Nullable Context context) {
        return getInt(context, KEY_ICON, PREF_ICON, 0);
    }

    public static void setIconIndex(@Nullable Context context, int index) {
        putInt(context, KEY_ICON, PREF_ICON, index);
    }

    public static int getIconModeIndex(@Nullable Context context) {
        return getInt(context, KEY_ICON_MODE, PREF_ICON_MODE, 0);
    }

    public static void setIconModeIndex(@Nullable Context context, int index) {
        putInt(context, KEY_ICON_MODE, PREF_ICON_MODE, index);
    }

    public static int getAppLanguageIndex(@Nullable Context context) {
        return getAppLanguageIndex(context, 0);
    }

    public static int getAppLanguageIndex(@Nullable Context context, int defValue) {
        String raw = getGlobalString(context, KEY_APP_LANGUAGE);
        if (!TextUtils.isEmpty(raw)) {
            try {
                return Integer.parseInt(raw);
            } catch (NumberFormatException ignored) {
            }
        }

        int fallback = PrefsBridge.getStringAsInt(PREF_APP_LANGUAGE, defValue);
        if (fallback != defValue) {
            putGlobalString(context, KEY_APP_LANGUAGE, Integer.toString(fallback));
        }
        return fallback;
    }

    public static void setAppLanguageIndex(@Nullable Context context, int index) {
        putInt(context, KEY_APP_LANGUAGE, PREF_APP_LANGUAGE, index);
    }

    public static void syncGlobalFromPrefs(@Nullable Context context) {
        if (context == null) {
            return;
        }

        Map<String, ?> allPrefs = PrefsBridge.getAll();
        syncBooleanIfPresent(context, allPrefs, PREF_HIDE_APP_ICON, KEY_HIDE_APP_ICON, true);
        syncBooleanIfPresent(context, allPrefs, PREF_FLOAT_NAV, KEY_FLOAT_NAV, false);
        syncBooleanIfPresent(context, allPrefs, PREF_SCOPE_SYNC, KEY_SCOPE_SYNC, false);
        syncIntIfPresent(context, allPrefs, PREF_ICON, KEY_ICON, 0);
        syncIntIfPresent(context, allPrefs, PREF_ICON_MODE, KEY_ICON_MODE, 0);
        syncIntIfPresent(context, allPrefs, PREF_APP_LANGUAGE, KEY_APP_LANGUAGE, 0);
    }

    public static void resetGlobalToDefaults(@Nullable Context context) {
        putGlobalString(context, KEY_HIDE_APP_ICON, Boolean.toString(true));
        putGlobalString(context, KEY_FLOAT_NAV, Boolean.toString(false));
        putGlobalString(context, KEY_SCOPE_SYNC, Boolean.toString(false));
        putGlobalString(context, KEY_ICON, Integer.toString(0));
        putGlobalString(context, KEY_ICON_MODE, Integer.toString(0));
        putGlobalString(context, KEY_APP_LANGUAGE, Integer.toString(0));
    }

    private static boolean getBoolean(
        @Nullable Context context,
        String globalKey,
        String prefKey,
        boolean defValue
    ) {
        String raw = getGlobalString(context, globalKey);
        if (!TextUtils.isEmpty(raw)) {
            if ("1".equals(raw)) return true;
            if ("0".equals(raw)) return false;
            return Boolean.parseBoolean(raw);
        }

        boolean fallback = PrefsBridge.getBoolean(prefKey, defValue);
        putGlobalString(context, globalKey, Boolean.toString(fallback));
        return fallback;
    }

    private static int getInt(
        @Nullable Context context,
        String globalKey,
        String prefKey,
        int defValue
    ) {
        String raw = getGlobalString(context, globalKey);
        if (!TextUtils.isEmpty(raw)) {
            try {
                return Integer.parseInt(raw);
            } catch (NumberFormatException ignored) {
            }
        }

        int fallback = PrefsBridge.getStringAsInt(prefKey, defValue);
        putGlobalString(context, globalKey, Integer.toString(fallback));
        return fallback;
    }

    private static void putBoolean(@Nullable Context context, String globalKey, String prefKey, boolean value) {
        putGlobalString(context, globalKey, Boolean.toString(value));
        PrefsBridge.putByApp(prefKey, value);
    }

    private static void putInt(@Nullable Context context, String globalKey, String prefKey, int value) {
        putGlobalString(context, globalKey, Integer.toString(value));
        PrefsBridge.putByApp(prefKey, Integer.toString(value));
    }

    private static void syncBooleanIfPresent(
        Context context,
        Map<String, ?> allPrefs,
        String prefKey,
        String globalKey,
        boolean defValue
    ) {
        if (!allPrefs.containsKey(prefKey)) {
            return;
        }
        Object rawValue = allPrefs.get(prefKey);
        boolean value;
        if (rawValue instanceof Boolean booleanValue) {
            value = booleanValue;
        } else if (rawValue instanceof String stringValue) {
            if ("1".equals(stringValue)) {
                value = true;
            } else if ("0".equals(stringValue)) {
                value = false;
            } else {
                value = Boolean.parseBoolean(stringValue);
            }
        } else {
            value = defValue;
        }
        putGlobalString(context, globalKey, Boolean.toString(value));
    }

    private static void syncIntIfPresent(
        Context context,
        Map<String, ?> allPrefs,
        String prefKey,
        String globalKey,
        int defValue
    ) {
        if (!allPrefs.containsKey(prefKey)) {
            return;
        }
        Object rawValue = allPrefs.get(prefKey);
        int value = defValue;
        if (rawValue instanceof Number number) {
            value = number.intValue();
        } else if (rawValue instanceof String stringValue) {
            try {
                value = Integer.parseInt(stringValue);
            } catch (NumberFormatException ignored) {
            }
        }
        putGlobalString(context, globalKey, Integer.toString(value));
    }

    @Nullable
    private static String getGlobalString(@Nullable Context context, String name) {
        if (context == null) {
            return null;
        }
        Cursor cursor = null;
        try {
            cursor = context.getContentResolver().query(
                GLOBAL_URI,
                new String[]{COLUMN_VALUE},
                COLUMN_NAME + "=?",
                new String[]{name},
                null
            );
            if (cursor != null && cursor.moveToFirst()) {
                return cursor.getString(0);
            }
        } catch (Throwable t) {
            AndroidLog.w(TAG, "Failed to read global setting: " + name, t);
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }
        return null;
    }

    private static void putGlobalString(@Nullable Context context, String name, String value) {
        if (context == null) {
            return;
        }
        try {
            ContentValues values = new ContentValues();
            values.put(COLUMN_NAME, name);
            values.put(COLUMN_VALUE, value);
            context.getContentResolver().insert(GLOBAL_URI, values);
        } catch (Throwable t) {
            AndroidLog.w(TAG, "Failed to write global setting: " + name, t);
        }
    }
}

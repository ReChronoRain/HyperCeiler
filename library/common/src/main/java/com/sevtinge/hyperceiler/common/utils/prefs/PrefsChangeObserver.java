package com.sevtinge.hyperceiler.common.utils.prefs;

import android.content.Context;
import android.database.ContentObserver;
import android.net.Uri;
import android.os.Handler;

import com.sevtinge.hyperceiler.common.utils.PrefsBridge;
import com.sevtinge.hyperceiler.common.utils.api.ProjectApi;

public class PrefsChangeObserver extends ContentObserver {
    private static final String SHARED_PREFS_AUTHORITY = ProjectApi.mAppModulePkg + ".provider.sharedprefs";

    private final boolean autoApplyChange;
    private final PrefType prefType;
    private final Object def;
    private final String name;

    public PrefsChangeObserver(Context context, Handler handler) {
        this(context, handler, false, PrefType.Any, null, null);
    }

    public PrefsChangeObserver(Context context, Handler handler, boolean autoApplyChange, String name) {
        this(context, handler, autoApplyChange, PrefType.StringSet, name, null);
    }

    public PrefsChangeObserver(Context context, Handler handler, PrefType type, String name, Object def) {
        this(context, handler, false, type, name, def);
    }

    public PrefsChangeObserver(Context context, Handler handler, boolean autoApplyChange, PrefType type, String name, Object def) {
        super(handler);
        this.def = def;
        Uri uri = null;
        this.name = name;
        prefType = type;
        this.autoApplyChange = autoApplyChange;
        switch (type) {
            case Any -> uri = PrefToUri.anyPrefToUri();
            case String -> uri = PrefToUri.stringPrefToUri(name, (String) def);
            case StringSet -> uri = PrefToUri.stringSetPrefToUri(name);
            case Integer -> uri = PrefToUri.intPrefToUri(name, (Integer) def);
            case Boolean -> uri = PrefToUri.boolPrefToUri(name, (boolean) def);
        }
        if (uri != null) {
            context.getContentResolver().registerContentObserver(uri, type == PrefType.Any, this);
        }
    }

    @Override
    public void onChange(boolean selfChange, Uri uri) {
        if (selfChange) return;
        if (autoApplyChange) {
            if (prefType == PrefType.Any) return;
            applyChange();
        }
        if (prefType == PrefType.Any)
            onChange(switch (uri.getPathSegments().get(1)) {
                case "string" -> PrefType.String;
                case "stringset" -> PrefType.StringSet;
                case "integer" -> PrefType.Integer;
                case "boolean" -> PrefType.Boolean;
                default -> PrefType.Any;
            }, uri, uri.getPathSegments().get(2), def);
        else onChange(prefType, uri, name, def);
    }

    public void onChange(PrefType type, Uri uri, String name, Object def) {
    }

    private void applyChange() {
        PrefsBridge.removeHookCache(name);
        PrefsBridge.putHookCache(name, switch (prefType) {
            case String -> PrefsBridge.getString(name, (String) def);
            case StringSet -> PrefsBridge.getStringSet(name);
            case Integer -> PrefsBridge.getInt(name, (Integer) def);
            case Boolean -> PrefsBridge.getBoolean(name, (boolean) def);
            default -> null;
        });
    }

    public static class PrefToUri {
        public static Uri stringPrefToUri(String name, String defValue) {
            return Uri.parse("content://" + SHARED_PREFS_AUTHORITY + "/string/" + name + "/" + defValue);
        }

        public static Uri stringSetPrefToUri(String name) {
            return Uri.parse("content://" + SHARED_PREFS_AUTHORITY + "/stringset/" + name);
        }

        public static Uri intPrefToUri(String name, int defValue) {
            return Uri.parse("content://" + SHARED_PREFS_AUTHORITY + "/integer/" + name + "/" + defValue);
        }

        public static Uri boolPrefToUri(String name, boolean defValue) {
            return Uri.parse("content://" + SHARED_PREFS_AUTHORITY + "/boolean/" + name + "/" + (defValue ? '1' : '0'));
        }

        public static Uri shortcutIconPrefToUri(String name) {
            return Uri.parse("content://" + SHARED_PREFS_AUTHORITY + "/shortcut_icon/" + name);
        }

        public static Uri anyPrefToUri() {
            return Uri.parse("content://" + SHARED_PREFS_AUTHORITY + "/pref/");
        }
    }
}

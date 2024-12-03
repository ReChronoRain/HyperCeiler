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

 * Copyright (C) 2023-2024 HyperCeiler Contributions
 */
package com.sevtinge.hyperceiler.utils.prefs;

import static com.sevtinge.hyperceiler.utils.prefs.PrefsChangeObserver.PrefToUri.anyPrefToUri;
import static com.sevtinge.hyperceiler.utils.prefs.PrefsChangeObserver.PrefToUri.boolPrefToUri;
import static com.sevtinge.hyperceiler.utils.prefs.PrefsChangeObserver.PrefToUri.intPrefToUri;
import static com.sevtinge.hyperceiler.utils.prefs.PrefsChangeObserver.PrefToUri.stringPrefToUri;
import static com.sevtinge.hyperceiler.utils.prefs.PrefsChangeObserver.PrefToUri.stringSetPrefToUri;
import static com.sevtinge.hyperceiler.utils.prefs.PrefsUtils.mPrefsMap;

import android.content.Context;
import android.database.ContentObserver;
import android.net.Uri;
import android.os.Handler;

import com.sevtinge.hyperceiler.provider.SharedPrefsProvider;

public class PrefsChangeObserver extends ContentObserver {
    private static final String TAG = "PrefsChangeObserver";
    private final boolean autoApplyChange;
    private final PrefType prefType;
    private final Context context;
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
        this.context = context;
        this.autoApplyChange = autoApplyChange;
        switch (type) {
            case PrefType.Any -> uri = anyPrefToUri();
            case PrefType.String -> uri = stringPrefToUri(name, (String) def);
            case PrefType.StringSet -> uri = stringSetPrefToUri(name);
            case PrefType.Integer -> uri = intPrefToUri(name, (Integer) def);
            case PrefType.Boolean -> uri = boolPrefToUri(name, (boolean) def);
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

    /**
     * @param type 类型
     * @param uri  uri
     * @param name 完整 key
     * @param def  传入的默认值
     */
    public void onChange(PrefType type, Uri uri, String name, Object def) {
    }

    private void applyChange() {
        mPrefsMap.put(name, switch (prefType) {
            case String -> PrefsUtils.getSharedStringPrefs(context, name, (String) def);
            case StringSet -> PrefsUtils.getSharedStringSetPrefs(context, name);
            case Integer -> PrefsUtils.getSharedIntPrefs(context, name, (Integer) def);
            case Boolean -> PrefsUtils.getSharedBoolPrefs(context, name, (boolean) def);
            default -> null;
        });
    }

    public static class PrefToUri {
        public static Uri stringPrefToUri(String name, String defValue) {
            return Uri.parse("content://" + SharedPrefsProvider.AUTHORITY + "/string/" + name + "/" + defValue);
        }

        public static Uri stringSetPrefToUri(String name) {
            return Uri.parse("content://" + SharedPrefsProvider.AUTHORITY + "/stringset/" + name);
        }

        public static Uri intPrefToUri(String name, int defValue) {
            return Uri.parse("content://" + SharedPrefsProvider.AUTHORITY + "/integer/" + name + "/" + defValue);
        }

        public static Uri boolPrefToUri(String name, boolean defValue) {
            return Uri.parse("content://" + SharedPrefsProvider.AUTHORITY + "/boolean/" + name + "/" + (defValue ? '1' : '0'));
        }

        public static Uri shortcutIconPrefToUri(String name) {
            return Uri.parse("content://" + SharedPrefsProvider.AUTHORITY + "/shortcut_icon/" + name);
        }

        public static Uri anyPrefToUri() {
            return Uri.parse("content://" + SharedPrefsProvider.AUTHORITY + "/pref/");
        }
    }
}

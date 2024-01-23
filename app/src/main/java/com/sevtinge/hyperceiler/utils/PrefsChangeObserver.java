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
package com.sevtinge.hyperceiler.utils;

import static com.sevtinge.hyperceiler.utils.PrefsChangeObserver.PrefToUri.anyPrefToUri;
import static com.sevtinge.hyperceiler.utils.PrefsChangeObserver.PrefToUri.boolPrefToUri;
import static com.sevtinge.hyperceiler.utils.PrefsChangeObserver.PrefToUri.intPrefToUri;
import static com.sevtinge.hyperceiler.utils.PrefsChangeObserver.PrefToUri.stringPrefToUri;
import static com.sevtinge.hyperceiler.utils.PrefsChangeObserver.PrefToUri.stringSetPrefToUri;

import android.content.Context;
import android.database.ContentObserver;
import android.net.Uri;
import android.os.Handler;

import com.sevtinge.hyperceiler.provider.SharedPrefsProvider;

public class PrefsChangeObserver extends ContentObserver {
    private enum PrefType {
        Any, String, StringSet, Integer, Boolean
    }

    private final PrefType prefType;
    private final Context ctx;
    private String prefName;
    private String prefDefValueString;
    private int prefDefValueInt;
    private boolean prefDefValueBool;

    public PrefsChangeObserver(Context context, Handler handler) {
        super(handler);
        ctx = context;
        prefType = PrefType.Any;
        registerObserver();
    }

    public PrefsChangeObserver(Context context, Handler handler, String name, String defValue) {
        super(handler);
        ctx = context;
        prefName = name;
        prefType = PrefType.String;
        prefDefValueString = defValue;
        registerObserver();
    }

    public PrefsChangeObserver(Context context, Handler handler, String name) {
        super(handler);
        ctx = context;
        prefName = name;
        prefType = PrefType.StringSet;
        registerObserver();
    }

    public PrefsChangeObserver(Context context, Handler handler, String name, int defValue) {
        super(handler);
        ctx = context;
        prefType = PrefType.Integer;
        prefName = name;
        prefDefValueInt = defValue;
        registerObserver();
    }

    public PrefsChangeObserver(Context context, Handler handler, String name, boolean defValue) {
        super(handler);
        ctx = context;
        prefType = PrefType.Boolean;
        prefName = name;
        prefDefValueBool = defValue;
        registerObserver();
    }

    private void registerObserver() {
        Uri uri = null;
        if (prefType == PrefType.String)
            uri = stringPrefToUri(prefName, prefDefValueString);
        else if (prefType == PrefType.StringSet)
            uri = stringSetPrefToUri(prefName);
        else if (prefType == PrefType.Integer)
            uri = intPrefToUri(prefName, prefDefValueInt);
        else if (prefType == PrefType.Boolean)
            uri = boolPrefToUri(prefName, prefDefValueBool);
        else if (prefType == PrefType.Any)
            uri = anyPrefToUri();
        if (uri != null)
            ctx.getContentResolver().registerContentObserver(uri, prefType == PrefType.Any, this);
    }

    @Override
    public void onChange(boolean selfChange, Uri uri) {
        if (prefType == PrefType.Any)
            onChange(uri);
        else
            onChange(selfChange);
    }

    @Override
    public void onChange(boolean selfChange) {
        if (selfChange) return;
        if (prefType == PrefType.String)
            onChange(prefName, prefDefValueString);
        else if (prefType == PrefType.StringSet)
            onChange(prefName);
        else if (prefType == PrefType.Integer)
            onChange(prefName, prefDefValueInt);
        else if (prefType == PrefType.Boolean)
            onChange(prefName, prefDefValueBool);
    }

    public void onChange(Uri uri) {
    }

    public void onChange(String name) {
    }

    public void onChange(String name, String defValue) {
    }

    public void onChange(String name, int defValue) {
    }

    public void onChange(String name, boolean defValue) {
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

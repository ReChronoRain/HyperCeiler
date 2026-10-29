/* SPDX-License-Identifier: AGPL-3.0-or-later */
package com.sevtinge.hyperceiler.libhook.provider;

import android.content.SharedPreferences;
import android.database.Cursor;
import android.database.MatrixCursor;

/** One authoritative physical preference read, also usable by dex-less Home. */
public final class HomeRecentsHapticPrefs {
    private HomeRecentsHapticPrefs() {}
    public static Cursor cursor(SharedPreferences prefs) {
        if(prefs==null)return null;
        var values=prefs.getAll();
        boolean enabled=Boolean.TRUE.equals(values.get("prefs_key_home_recent_card_haptic"))
            && !Boolean.FALSE.equals(values.get("prefs_key_framework_api_allow_hook"));
        boolean close=Boolean.TRUE.equals(values.get("prefs_key_home_recent_card_close_haptic"))
            && !Boolean.FALSE.equals(values.get("prefs_key_framework_api_allow_hook"));
        int type=0,strength=50;
        Object mode=values.get("prefs_key_home_recent_card_haptic_type");
        Object level=values.get("prefs_key_home_recent_card_haptic_strength");
        try {
            if(mode!=null)type=Integer.parseInt(mode.toString());
            if(level!=null)strength=Integer.parseInt(level.toString());
        } catch(NumberFormatException malformed){return null;}
        if(type<0||type>3||strength<10||strength>100)return null;
        var cursor=new MatrixCursor(new String[]{"schema","enabled","type","strength"});
        cursor.addRow(new Object[]{1,enabled?1:0,type,strength});
        var extras=new android.os.Bundle();extras.putInt("close_version",1);extras.putInt("close_enabled",close?1:0);
        cursor.setExtras(extras);return cursor;
    }
}

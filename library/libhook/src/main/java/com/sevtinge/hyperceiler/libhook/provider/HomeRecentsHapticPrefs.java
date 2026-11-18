/* SPDX-License-Identifier: AGPL-3.0-or-later */
package com.sevtinge.hyperceiler.libhook.provider;

import android.content.SharedPreferences;
import android.database.Cursor;
import android.database.MatrixCursor;
import java.util.Map;

/** One authoritative physical preference read, also usable by dex-less Home. */
public final class HomeRecentsHapticPrefs {
    private HomeRecentsHapticPrefs() {}
    public static int effectId(String text) {
        String value=text.trim();
        int id=value.startsWith("0x")||value.startsWith("0X")?Integer.parseInt(value.substring(2),16):Integer.parseInt(value,10);
        if(id<0)throw new NumberFormatException("Negative effect ID");
        return id;
    }
    public record Profile(boolean enabled,boolean close,int type,int strength,boolean custom,int effectId) {
        public Profile(boolean enabled,boolean close,int type,int strength){this(enabled,close,type,strength,false,0);}
        public int encoded(){return (enabled?1:0)|(type<<1)|(close?8:0)|(strength<<4);}
    }
    public static Profile profile(Map<String,?> values) {
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
        boolean custom=Boolean.TRUE.equals(values.get("prefs_key_home_recent_card_haptic_custom"));
        int id=0;
        Object effect=values.get("prefs_key_home_recent_card_haptic_effect_id");
        try {if(effect!=null)id=effectId(effect.toString());}
        catch(NumberFormatException invalid){if(custom)return null;}
        if(custom&&id<0)return null;
        return new Profile(enabled,close,type,strength,custom,id);
    }
    public static Profile decodeProfile(int word) {
        int strength = word >> 4;
        if (strength < 10 || strength > 100) return null;
        return new Profile((word & 1) != 0, (word & 8) != 0, (word >> 1) & 3, strength);
    }
    public static Cursor cursor(SharedPreferences prefs) {
        if(prefs==null)return null;
        var profile=profile(prefs.getAll());
        if(profile==null)return null;
        var cursor=new MatrixCursor(new String[]{"schema","enabled","type","strength"});
        cursor.addRow(new Object[]{1,profile.enabled()?1:0,profile.type(),profile.strength()});
        var extras=new android.os.Bundle();extras.putInt("close_version",1);extras.putInt("close_enabled",profile.close()?1:0);
        cursor.setExtras(extras);return cursor;
    }
}

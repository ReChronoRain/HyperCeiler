/* SPDX-License-Identifier: AGPL-3.0-or-later */
package com.sevtinge.hyperceiler.libhook.provider;

import java.util.Map;
import com.sevtinge.hyperceiler.libhook.rules.home.dock.DockUnlockReveal;

/** Dock appearance from the same physical preference revision as native layout. */
public final class HomeDockPrefs {
    private HomeDockPrefs() {}
    public record Profile(boolean enabled,int mode,int color,int height,int margin,int bottom,int radius,int night,int style,boolean capsule) {}
    public static Profile profile(Map<String,?> values) {
        Object enabled=values.get("prefs_key_home_dock_bg_custom_enable");
        Object style=values.get("prefs_key_home_dock_unlock_style");
        Object capsule=values.get("prefs_key_"+HomeLayoutPrefsSnapshot.XIAOHEIZI_KEY);
        if((enabled!=null&&!(enabled instanceof Boolean))||(style!=null&&!(style instanceof String))
            ||(capsule!=null&&!(capsule instanceof Boolean)))return null;
        Integer mode=integer(values,"home_dock_add_blur",1),color=integer(values,"home_dock_bg_color",0),
            height=integer(values,"home_dock_bg_height",150),margin=integer(values,"home_dock_bg_margin_horizontal",25),
            bottom=integer(values,"home_dock_bg_margin_bottom",15),radius=integer(values,"home_dock_bg_radius",30),
            night=integer(values,"home_other_home_mode",0);
        if(mode==null||color==null||height==null||margin==null||bottom==null||radius==null||night==null)return null;
        int animation=switch(DockUnlockReveal.Style.of((String)style)) {
            case DAYBREAK -> 0;case DEPTH_FLIP -> 1;case GALE -> 2;case ORBIT_SWEEP -> 3;
            case RIPPLE -> 4;case PERSPECTIVE_FOLD -> 5;case CAPSULE_FISSION -> 6;case AUTO_AIM -> 7;
        };
        return new Profile(Boolean.TRUE.equals(enabled)&&!Boolean.FALSE.equals(values.get("prefs_key_framework_api_allow_hook"))
            &&!Boolean.FALSE.equals(values.get("prefs_key_allow_hook")),
            mode==3?3:1,color,Math.clamp(height,0,300),Math.clamp(margin,0,150),Math.clamp(bottom,0,150),
            Math.clamp(radius,0,60),night==1||night==2?night:0,animation,Boolean.TRUE.equals(capsule));
    }
    private static Integer integer(Map<String,?> values,String key,int fallback) {
        Object value=values.get("prefs_key_"+key);
        if(value==null)return fallback;
        if(value instanceof Integer number)return number;
        if(!(value instanceof String text))return null;
        try{return Integer.parseInt(text);}catch(NumberFormatException invalid){return null;}
    }
}

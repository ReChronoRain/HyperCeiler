// SPDX-License-Identifier: AGPL-3.0-or-later
package com.sevtinge.hyperceiler.libhook.rules.home.dock;
import android.content.ContentResolver;
import android.os.SystemClock;
import com.sevtinge.hyperceiler.libhook.provider.HomeLayoutPrefsSnapshot;
import java.util.*;
import java.lang.reflect.*;
public class PreferenceTest {
    static int checks;
    static void c(boolean b){if(!b)throw new AssertionError("check "+checks);checks++;}
    public static void main(String[] args)throws Exception {
        String key=HomeLayoutPrefsSnapshot.RECENTS_BAR_KEY;
        Map<String,Object> rows=new HashMap<>();rows.put("prefs_key_"+key,true);
        int[] reads={0};
        try(var cursor=HomeLayoutPrefsSnapshot.cursor(()->{reads[0]++;return rows;})){
            c(reads[0]==1);c(cursor.getColumnCount()==HomeLayoutPrefsSnapshot.specs().length+1);
            rows.put("prefs_key_"+key,false);
            c(cursor.getExtras().getInt(HomeLayoutPrefsSnapshot.RECENTS_BAR_VERSION,0)==1);
            c(cursor.getExtras().getInt(key,-1)==1); // extras are the SAME physical-file snapshot
            for(String[] spec:HomeLayoutPrefsSnapshot.specs())c(!spec[1].equals(key)); // schema4 untouched
        }
        rows.put("prefs_key_"+key,"true");c(HomeLayoutPrefsSnapshot.cursor(()->rows)==null);
        Class<?> type=HomeLayoutNativeEndpointOS4.class;
        Field worker=type.getDeclaredField("refresher");worker.setAccessible(true);worker.set(null,new Thread());
        ContentResolver.setSpecs(HomeLayoutPrefsSnapshot.specs());
        ContentResolver.setRows(Map.of("prefs_key_allow_hook",1,"prefs_key_framework_api_allow_hook",1,"prefs_key_"+key,1));
        ContentResolver.recentsExtras=false;
        c(HomeLayoutNativeEndpointOS4.refreshFromProvider());c(!HomeLayoutNativeEndpointOS4.recentsBarPreference().available());
        ContentResolver.recentsExtras=true;c(HomeLayoutNativeEndpointOS4.refreshFromProvider());
        var pref=HomeLayoutNativeEndpointOS4.recentsBarPreference();c(pref.available());c(pref.enabled());
        long deadline=pref.deadline();SystemClock.source=()->deadline;c(!HomeLayoutNativeEndpointOS4.recentsBarPreference().available());SystemClock.source=System::nanoTime;
        ContentResolver.setRows(Map.of("prefs_key_allow_hook",1,"prefs_key_framework_api_allow_hook",1,"prefs_key_"+key,0));
        c(HomeLayoutNativeEndpointOS4.refreshFromProvider());pref=HomeLayoutNativeEndpointOS4.recentsBarPreference();c(pref.available());c(!pref.enabled());
        ContentResolver.setRows(Map.of("prefs_key_allow_hook",0,"prefs_key_framework_api_allow_hook",1,"prefs_key_"+key,1));
        c(HomeLayoutNativeEndpointOS4.refreshFromProvider());c(!HomeLayoutNativeEndpointOS4.recentsBarPreference().enabled());
        c(HomeLayoutNativeEndpointOS4.pauseForHotReload());c(!HomeLayoutNativeEndpointOS4.recentsBarPreference().available());
        System.out.println("recents bar atomic preference "+checks+" checks passed");
    }
}

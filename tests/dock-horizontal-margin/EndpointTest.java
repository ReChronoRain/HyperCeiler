// SPDX-License-Identifier: AGPL-3.0-or-later
package com.sevtinge.hyperceiler.libhook.rules.home.dock;
import android.content.ContentResolver;
import android.os.Parcel;
import com.sevtinge.hyperceiler.libhook.provider.HomeLayoutPrefsSnapshot;
import java.util.*;
import java.lang.reflect.*;
public class EndpointTest {
    static int checks;
    static void c(boolean b){checks++;if(!b)throw new AssertionError("check "+checks);}
    public static void main(String[] args)throws Exception {
        String key=HomeLayoutPrefsSnapshot.DOCK_HORIZONTAL_KEY;
        Map<String,Object> values=new HashMap<>();values.put("prefs_key_allow_hook",true);values.put("prefs_key_framework_api_allow_hook",true);
        values.put("prefs_key_"+key+"_enable",true);values.put("prefs_key_"+key,80);
        values.put("prefs_key_home_layout_workspace_padding_horizontal_enable",true);values.put("prefs_key_home_layout_workspace_padding_horizontal",0);
        int[] reads={0};
        try(var cursor=HomeLayoutPrefsSnapshot.cursor(()->{reads[0]++;return values;})) {
            c(reads[0]==1);c(cursor.getColumnCount()==HomeLayoutPrefsSnapshot.specs().length+1);
            c(cursor.getExtras().getInt(key,-1)==80);values.put("prefs_key_"+key,20);
            c(cursor.getExtras().getInt(key,-1)==80); // extras and old columns belong to the same physical revision
        }
        Field worker=HomeLayoutNativeEndpointOS4.class.getDeclaredField("refresher");worker.setAccessible(true);worker.set(null,new Thread());
        for(int margin:new int[]{0,20,21,80,100})for(boolean enabled:new boolean[]{false,true}) {
            values.put("prefs_key_"+key,margin);values.put("prefs_key_"+key+"_enable",enabled);
            ContentResolver.values=Map.copyOf(values);c(HomeLayoutNativeEndpointOS4.refreshFromProvider());
            var snapshot=HomeLayoutNativeEndpointOS4.readPreferences();int expected=enabled?margin-20:0;
            c(snapshot.dockHorizontalDelta()==expected);c(snapshot.knobDeltaDp()[4]==-20);
            c(HomeLayoutNativeEndpointOS4.readCombinedSnapshot().layout().dockHorizontalDelta()==expected);
            Parcel wire=new Parcel("");HomeLayoutNativeEndpointOS4.writeDockHorizontal(wire,snapshot.dockHorizontalDelta());
            c(wire.dataSize()==12);wire.setDataPosition(0);c(wire.readInt()==0x4843484d);c(wire.readInt()==1);c(wire.readInt()==expected);
            Parcel combined=new Parcel("");HomeLayoutNativeEndpointOS4.writeCombinedReply(combined,HomeLayoutNativeEndpointOS4.readCombinedSnapshot());c(combined.dataSize()==188);
        }
        for(Object bad:new Object[]{-1,101,"50"}) {
            values.put("prefs_key_"+key,bad);ContentResolver.values=Map.copyOf(values);
            c(HomeLayoutNativeEndpointOS4.refreshFromProvider());c(HomeLayoutNativeEndpointOS4.readPreferences().dockHorizontalDelta()==0);
        }
        values.remove("prefs_key_"+key);values.remove("prefs_key_"+key+"_enable");ContentResolver.values=Map.copyOf(values);
        c(HomeLayoutNativeEndpointOS4.refreshFromProvider());c(HomeLayoutNativeEndpointOS4.readPreferences().dockHorizontalDelta()==0);
        System.out.println("dock horizontal endpoint "+checks+" checks passed");
    }
}

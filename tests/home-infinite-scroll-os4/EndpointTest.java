/* SPDX-License-Identifier: AGPL-3.0-or-later */
package com.sevtinge.hyperceiler.libhook.rules.home.dock;
import android.content.ContentResolver;
import android.os.Parcel;
import com.sevtinge.hyperceiler.libhook.provider.HomeLayoutPrefsSnapshot;
import java.util.*;
import java.lang.reflect.*;
public class EndpointTest {
    static int checks;
    static void c(boolean value){++checks;if(!value)throw new AssertionError("check "+checks);}
    public static void main(String[] args)throws Exception{
        Field worker=HomeLayoutNativeEndpointOS4.class.getDeclaredField("refresher");worker.setAccessible(true);worker.set(null,new Thread());
        Map<String,Object> values=new HashMap<>();values.put("prefs_key_allow_hook",true);values.put("prefs_key_framework_api_allow_hook",true);
        String key=HomeLayoutPrefsSnapshot.INFINITE_KEY;int[] calls={0};
        for(boolean enabled:new boolean[]{false,true,false,true}){
            values.put("prefs_key_"+key,enabled);values.put("prefs_key_home_layout_hotseats_margin_horizontal_enable",true);values.put("prefs_key_home_layout_hotseats_margin_horizontal",43);
            try(var cursor=HomeLayoutPrefsSnapshot.cursor(()->{calls[0]++;return values;})){
                c(cursor.getColumnCount()==HomeLayoutPrefsSnapshot.specs().length+1);c(cursor.getExtras().getInt(key,-1)==(enabled?1:0));
                values.put("prefs_key_"+key,!enabled);c(cursor.getExtras().getInt(key,-1)==(enabled?1:0));values.put("prefs_key_"+key,enabled);
            }
            ContentResolver.values=Map.copyOf(values);c(HomeLayoutNativeEndpointOS4.refreshFromProvider());
            var snapshot=HomeLayoutNativeEndpointOS4.readPreferences();c(snapshot.infiniteScroll()==enabled);c(snapshot.dockHorizontalDelta()==23);
            var combined=HomeLayoutNativeEndpointOS4.readCombinedSnapshot();c(combined.layout().infiniteScroll()==enabled);
            Parcel wire=new Parcel("");HomeLayoutNativeEndpointOS4.writeInfiniteScroll(wire,snapshot.infiniteScroll());c(wire.dataSize()==8);
            wire.setDataPosition(0);c(wire.readInt()==0x48434957);c(wire.readInt()==(enabled?1:0));
            Parcel fixed=new Parcel("");HomeLayoutNativeEndpointOS4.writeCombinedReply(fixed,combined);c(fixed.dataSize()==188);
            values.put("prefs_key_"+key,!enabled);c(snapshot.infiniteScroll()==enabled&&combined.layout().infiniteScroll()==enabled);
        }
        c(calls[0]==4);values.remove("prefs_key_"+key);ContentResolver.values=Map.copyOf(values);c(HomeLayoutNativeEndpointOS4.refreshFromProvider());c(!HomeLayoutNativeEndpointOS4.readPreferences().infiniteScroll());
        values.put("prefs_key_"+key,"true");c(HomeLayoutPrefsSnapshot.cursor(()->values)==null);
        System.out.println("Infinite scroll Provider/JVM PASS "+checks+" checks, one getAll snapshot, optional extras, immutable publication, unchanged fixed wire and bounded full-reply extension");
    }
}

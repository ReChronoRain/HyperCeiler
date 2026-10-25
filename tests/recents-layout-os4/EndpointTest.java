/* SPDX-License-Identifier: AGPL-3.0-or-later */
package com.sevtinge.hyperceiler.libhook.rules.home.dock;
import android.content.ContentResolver;
import android.os.Parcel;
import java.util.*;
public class EndpointTest {
    static void check(boolean value){if(!value)throw new AssertionError();}
    public static void main(String[] args)throws Exception{
        var worker=HomeLayoutNativeEndpointOS4.class.getDeclaredField("refresher");worker.setAccessible(true);worker.set(null,new Thread());
        ContentResolver.setSpecs(com.sevtinge.hyperceiler.libhook.provider.HomeLayoutPrefsSnapshot.specs());
        ContentResolver.setRows(Map.of());check(HomeLayoutNativeEndpointOS4.refreshFromProvider());
        check(Arrays.equals(HomeLayoutNativeEndpointOS4.readPreferences().recent(),HomeLayoutNativeEndpointOS4.recentDefaults()));
        String[] keys={"task_view_corners","task_view_header_height","task_view_header_horizontal_offset","home_recent_animation_speed","home_recent_task_view_height","task_view_horizontal1","task_view_horizontal2","home_recent_vertical_task_view_card_size","home_recent_text_size","home_recent_text_color","home_recent_freeform_background_color"};
        int[] values={42,65,51,150,60,70,130,80,22,0xff2468ac,0xffaabbcc};
        Map<String,Integer> rows=new HashMap<>();rows.put("prefs_key_home_drawer_hide_search",1);rows.put("prefs_key_home_title_hide_report_text",1);for(int i=0;i<keys.length;++i)rows.put("prefs_key_"+keys[i],values[i]);
        ContentResolver.setRows(rows);check(HomeLayoutNativeEndpointOS4.refreshFromProvider());
        var first=HomeLayoutNativeEndpointOS4.readPreferences();check(Arrays.equals(first.recent(),values)&&first.drawerHideSearch());
        check(first.policies()==1L);
        check(new HomeLayoutNativeEndpointOS4.CombinedSnapshot(1,0,1,1,100,first).layout().policies()==1L);
        var policyReply=new Parcel("");HomeLayoutNativeEndpointOS4.writePolicies(policyReply,first.policies());check(policyReply.dataSize()==16);policyReply.setDataPosition(0);
        check(policyReply.readInt()==HomeLayoutNativeEndpointOS4.POLICY_MAGIC&&policyReply.readInt()==2&&policyReply.readInt()==1&&policyReply.readInt()==0&&policyReply.dataAvail()==0);
        var drawerReply=new Parcel("");HomeLayoutNativeEndpointOS4.writeDrawer(drawerReply,first.drawerHideSearch());check(drawerReply.dataSize()==8);drawerReply.setDataPosition(0);
        check(drawerReply.readInt()==HomeLayoutNativeEndpointOS4.DRAWER_MAGIC&&drawerReply.readInt()==1&&drawerReply.dataAvail()==0);
        // New revisions never mutate older snapshots, including exposed arrays.
        int[] exposed=first.recent();exposed[0]=99;check(first.recent()[0]==42);
        rows.put("prefs_key_task_view_corners",64);ContentResolver.setRows(rows);check(HomeLayoutNativeEndpointOS4.refreshFromProvider());
        check(HomeLayoutNativeEndpointOS4.readPreferences().recent()[0]==64&&first.recent()[0]==42);
        var reply=new Parcel("");HomeLayoutNativeEndpointOS4.writeRecent(reply,first.recent());check(reply.dataSize()==52);reply.setDataPosition(0);
        check(reply.readInt()==HomeLayoutNativeEndpointOS4.RECENT_MAGIC&&reply.readInt()==11);
        for(int value:values)check(reply.readInt()==value);check(reply.dataAvail()==0);
        var old=new HomeLayoutNativeEndpointOS4.Snapshot(1,0,4,6,new int[8],new int[8],new int[17]);
        check(old.policies()==0);check(Arrays.equals(old.recent(),HomeLayoutNativeEndpointOS4.recentDefaults())&&!old.drawerHideSearch());
        rows.put("prefs_key_task_view_corners",999);rows.put("prefs_key_home_recent_animation_speed",0);
        ContentResolver.setRows(rows);check(HomeLayoutNativeEndpointOS4.refreshFromProvider());
        check(HomeLayoutNativeEndpointOS4.readPreferences().recent()[0]==20&&HomeLayoutNativeEndpointOS4.readPreferences().recent()[3]==100);
        ContentResolver.setRows(Map.of());check(HomeLayoutNativeEndpointOS4.refreshFromProvider());
        check(Arrays.equals(HomeLayoutNativeEndpointOS4.readPreferences().recent(),old.recent())&&!HomeLayoutNativeEndpointOS4.readPreferences().drawerHideSearch()&&first.drawerHideSearch());
        check(HomeLayoutNativeEndpointOS4.readPreferences().policies()==0&&first.policies()==1);
        var failure=new java.util.concurrent.atomic.AtomicReference<Throwable>();
        Thread producer=new Thread(()->{try{
            for(int turn=0;turn<150;++turn){Map<String,Integer> revision=new HashMap<>();
                int[] tuple=turn%2==0?values:HomeLayoutNativeEndpointOS4.recentDefaults();
                revision.put("prefs_key_home_title_hide_report_text",turn%2==0?1:0);
                for(int i=0;i<keys.length;++i)revision.put("prefs_key_"+keys[i],tuple[i]);
                ContentResolver.setRows(revision);check(HomeLayoutNativeEndpointOS4.refreshFromProvider());}
        }catch(Throwable problem){failure.set(problem);}});
        producer.start();for(int i=0;i<2000;++i){var snapshot=HomeLayoutNativeEndpointOS4.readPreferences();int[] tuple=snapshot.recent();
            check((Arrays.equals(tuple,values)&&snapshot.policies()==1)||(Arrays.equals(tuple,HomeLayoutNativeEndpointOS4.recentDefaults())&&snapshot.policies()==0));}
        producer.join();if(failure.get()!=null)throw new AssertionError(failure.get());
        boolean rejected=false;try{HomeLayoutNativeEndpointOS4.writeRecent(new Parcel(""),new int[10]);}catch(IllegalArgumentException expected){rejected=true;}check(rejected);
        System.out.println("recents provider/JVM PASS: 11 keys, immutable/concurrent revisions, exact 52-byte writer, old endpoint/defaults/ranges/reset");System.exit(0);
    }
}

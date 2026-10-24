/* SPDX-License-Identifier: AGPL-3.0-or-later */
package com.sevtinge.hyperceiler.libhook.rules.home.dock;
import android.content.ContentResolver;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
public class ObjectScaleEndpointTest {
    static void check(boolean value){if(!value)throw new AssertionError();}
    static void expect(int... values){
        check(Arrays.equals(HomeLayoutNativeEndpointOS4.readPreferences().objectScales(),values));
    }
    public static void main(String[] args)throws Exception{
        var worker=HomeLayoutNativeEndpointOS4.class.getDeclaredField("refresher");
        worker.setAccessible(true);worker.set(null,new Thread());
        ContentResolver.setSpecs(com.sevtinge.hyperceiler.libhook.provider.HomeLayoutPrefsSnapshot.specs());
        Map<String,Integer> rows=new HashMap<>();
        rows.put("prefs_key_home_layout_icon_scale_enable",1);
        rows.put("prefs_key_home_layout_icon_scale",60);
        ContentResolver.setRows(rows);
        check(HomeLayoutNativeEndpointOS4.refreshFromProvider());
        expect(0,100,0,100,0,100); // Ordinary icons never supply missing object settings.
        int iconCode=HomeLayoutNativeEndpointOS4.readPreferences().tweaks()[8];
        for(int height:new int[]{10,25,60,80,100})for(int width:new int[]{100,200,400}){
            rows.put("prefs_key_home_navigation_back_area_height",height);
            rows.put("prefs_key_home_navigation_back_area_width",width);
            ContentResolver.setRows(rows);check(HomeLayoutNativeEndpointOS4.refreshFromProvider());
            var snapshot=HomeLayoutNativeEndpointOS4.readPreferences();
            check(snapshot.tweaks()[8]==iconCode&&snapshot.tweaks()[9]==1);
            check(Arrays.equals(snapshot.backGesture(),new int[]{height,width}));
            expect(0,100,0,100,0,100);
        }
        rows.put("prefs_key_home_layout_small_folder_scale_enable",1);
        rows.put("prefs_key_home_layout_small_folder_scale",75);
        rows.put("prefs_key_home_layout_big_folder_scale_enable",1);
        rows.put("prefs_key_home_layout_big_folder_scale",125);
        rows.put("prefs_key_home_layout_widget_scale_enable",0);
        rows.put("prefs_key_home_layout_widget_scale",110);
        ContentResolver.setRows(rows);check(HomeLayoutNativeEndpointOS4.refreshFromProvider());
        expect(1,75,1,125,0,110);
        rows.put("prefs_key_home_navigation_back_area_height",25);
        ContentResolver.setRows(rows);check(HomeLayoutNativeEndpointOS4.refreshFromProvider());
        expect(1,75,1,125,0,110);
        check(HomeLayoutNativeEndpointOS4.readPreferences().tweaks()[8]==iconCode);
        check(HomeLayoutNativeEndpointOS4.readPreferences().tweaks()[9]==1);
        rows.put("prefs_key_home_layout_icon_scale",140);
        ContentResolver.setRows(rows);check(HomeLayoutNativeEndpointOS4.refreshFromProvider());expect(1,75,1,125,0,110);
        rows.put("prefs_key_home_layout_widget_scale_enable",1);
        rows.put("prefs_key_home_layout_widget_scale",999);
        ContentResolver.setRows(rows);check(HomeLayoutNativeEndpointOS4.refreshFromProvider());expect(1,75,1,125,1,140);
        ContentResolver.setRows(Map.of());check(HomeLayoutNativeEndpointOS4.refreshFromProvider());expect(0,100,0,100,0,100);
        var old=new HomeLayoutNativeEndpointOS4.Snapshot(1,0,4,6,new int[8],new int[8],new int[17]);
        check(Arrays.equals(old.objectScales(),new int[]{0,100,0,100,0,100}));
        System.out.println("object scale endpoint mapping / independent revisions / old constructor / reset: PASS");
        System.exit(0);
    }
}

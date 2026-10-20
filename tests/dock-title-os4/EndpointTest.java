/* SPDX-License-Identifier: AGPL-3.0-or-later */
package com.sevtinge.hyperceiler.libhook.rules.home.dock;
import android.content.ContentResolver;
import android.os.Binder;
import android.os.Parcel;
import com.sevtinge.hyperceiler.libhook.provider.HomeLayoutPrefsSnapshot;
import com.sevtinge.hyperceiler.common.log.ModuleLog;
import java.util.Map;

public class EndpointTest {
    static int checks;
    static void ok(boolean value){++checks;if(!value)throw new AssertionError("check "+checks);}
    public static void main(String[] args) {
        ContentResolver.setSpecs(HomeLayoutPrefsSnapshot.specs());
        boolean found=false;
        for(var spec:HomeLayoutPrefsSnapshot.specs())if(spec[1].equals("home_dock_icon_title")){ok(spec[0].equals("boolean"));found=true;}
        ok(found);
        for(int enabled:new int[]{0,1,0}){
            ContentResolver.setRows(Map.of("prefs_key_home_dock_icon_title",enabled));
            ok(HomeLayoutNativeEndpointOS4.refreshFromProvider());
            var snapshot=HomeLayoutNativeEndpointOS4.readPreferences();ok(snapshot.dockShowTitle()==(enabled==1));
            var endpoint=new HomeLayoutNativeEndpointOS4((uid,pid)->true,()->snapshot);
            Binder.setCallingIdentityForTest(10123,1234);
            var parcel=new Parcel("android.view.IWindowManager");
            var accepted=endpoint.receive(parcel,0);ok(accepted!=null&&accepted.dockShowTitle()==(enabled==1));
            ok(ModuleLog.last.contains("dock_show_title="+(enabled==1)));
        }
        ContentResolver.setRows(Map.of());ok(HomeLayoutNativeEndpointOS4.refreshFromProvider());
        var s=HomeLayoutNativeEndpointOS4.readPreferences();ok(!s.dockShowTitle());
        var old=new HomeLayoutNativeEndpointOS4.Snapshot(s.acknowledgment(),s.gridEnabled(),s.cellX(),s.cellY(),
            s.knobEnabled(),s.knobDeltaDp(),s.tweaks(),s.title(),s.customTitles(),s.widgetAllowMove(),
            s.folder(),s.backGesture(),s.folderAutoClose(),s.objectScales());
        ok(!old.dockShowTitle());ok((HomeLayoutNativeEndpointOS4.ENDPOINT_CAPABILITIES&2)!=0);
        System.out.println("dock title endpoint: "+checks+" checks passed (provider, on/off, authenticated copy, logging, old constructor)");
    }
}

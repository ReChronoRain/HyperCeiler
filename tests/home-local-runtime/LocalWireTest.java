package com.sevtinge.hyperceiler.libhook.rules.home.dock;

import android.content.ContentResolver;
import android.content.Context;
import android.os.Parcel;
import com.sevtinge.hyperceiler.home.runtime.HomeLocalWire;
import com.sevtinge.hyperceiler.libhook.provider.HomeRecentsHapticPrefs;
import com.sevtinge.hyperceiler.libhook.provider.HomeRecentCardsPrefs;
import com.sevtinge.hyperceiler.libhook.provider.HomeDockPrefs;
import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import com.sevtinge.hyperceiler.libhook.provider.HomeLayoutPrefsSnapshot;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Set;

public final class LocalWireTest {
    private static HomeLocalWire.ConfigurationAck acknowledgement(long sequence,int flags,long published,
                                                                 HomeLocalWire.ConfigurationAck previous) throws Exception {
        var bytes=new ByteArrayOutputStream();var output=new DataOutputStream(bytes);
        output.writeInt(HomeLocalWire.ACK);output.writeLong(sequence);output.writeInt(flags);
        return HomeLocalWire.readConfigurationAck(new java.io.DataInputStream(new java.io.ByteArrayInputStream(bytes.toByteArray())),()->published,previous);
    }
    private static void check(boolean value) { if (!value) throw new AssertionError(); }
    private static void write(Path out, String name) throws Exception {
        var snapshot = HomeLayoutNativeEndpointOS4.readLocalSnapshot(new Context());
        check(snapshot != null);
        Parcel p = new Parcel("");
        HomeLayoutWire.write(p, snapshot.layout(), snapshot.gestures(), 1234);
        Files.write(out.resolve(name + ".bin"), p.marshall());
        var stream=new ByteArrayOutputStream();
        var profile=HomeRecentsHapticPrefs.profile(ContentResolver.values);
        HomeLocalWire.writeSnapshot(new DataOutputStream(stream),p.marshall(),profile,HomeRecentCardsPrefs.selection(ContentResolver.values),HomeDockPrefs.profile(ContentResolver.values),1,HomeLocalWire.VERSION);
        Files.write(out.resolve("root-"+name+".bin"),stream.toByteArray());
        for(int version=4;version<=6;version++) {
            stream.reset();
            HomeLocalWire.writeSnapshot(new DataOutputStream(stream),p.marshall(),profile,
                HomeRecentCardsPrefs.selection(ContentResolver.values),HomeDockPrefs.profile(ContentResolver.values),1,version);
            Files.write(out.resolve("root-v"+version+"-"+name+".bin"),stream.toByteArray());
        }
        if(name.equals("enabled")) {
            byte[][] waves=new byte[8][];
            for(int i=0;i<waves.length;i++)waves[i]=new byte[]{(byte)(i+1),2,3,4};
            stream.reset();
            HomeLocalWire.writeSnapshot(new DataOutputStream(stream),p.marshall(),profile,
                HomeRecentCardsPrefs.selection(ContentResolver.values),HomeDockPrefs.profile(ContentResolver.values),waves,1,HomeLocalWire.VERSION);
            Files.write(out.resolve("root-wave-enabled.bin"),stream.toByteArray());
            stream.reset();
            var close=new HomeRecentsHapticPrefs.Profile(false,true,3,70);
            HomeLocalWire.writeSnapshot(new DataOutputStream(stream),p.marshall(),close,null,null,
                new byte[][]{waves[0]},1,HomeLocalWire.VERSION);
            Files.write(out.resolve("root-wave-close.bin"),stream.toByteArray());
        }
    }
    public static void main(String[] args) throws Exception {
        var hapticBytes=new ByteArrayOutputStream();
        var hapticOutput=new DataOutputStream(hapticBytes);
        hapticOutput.writeInt(HomeLocalWire.HAPTIC_PREPARE);hapticOutput.writeInt(0x46f);
        hapticOutput.writeInt(2);hapticOutput.writeLong(1234);hapticOutput.writeInt(1);
        var hapticInput=new java.io.DataInputStream(new java.io.ByteArrayInputStream(hapticBytes.toByteArray()));
        var request=HomeLocalWire.readHaptic(hapticInput);
        check(request.prepare()&&request.profile()==0x46f&&request.operation()==2
            &&request.timestamp()==1234&&request.count()==1&&hapticInput.available()==0);
        hapticBytes.reset();
        byte[] wave={1,2,3,4};
        HomeLocalWire.writeHapticWave(hapticOutput,request.timestamp(),wave);
        hapticInput=new java.io.DataInputStream(new java.io.ByteArrayInputStream(hapticBytes.toByteArray()));
        check(hapticInput.readInt()==HomeLocalWire.HAPTIC_WAVE&&hapticInput.readLong()==1234
            &&hapticInput.readInt()==wave.length&&java.util.Arrays.equals(hapticInput.readAllBytes(),wave));
        hapticBytes.reset();
        HomeLocalWire.writeHapticWave(hapticOutput,1234,null);
        check(hapticBytes.size()==16);
        for(int size:new int[]{0,3,HomeLocalWire.MAX_HAPTIC_WAVE+4}) {
            boolean refused=false;
            try{HomeLocalWire.writeHapticWave(hapticOutput,1234,new byte[size]);}
            catch(IllegalArgumentException expected){refused=true;}
            check(refused);
        }
        var ack=acknowledgement(1,1,1,null);
        check(ack.accepted()&&!ack.nativeCards());
        ack=acknowledgement(1,3,1,ack);check(ack.nativeCards());
        ack=acknowledgement(1,1,1,ack);check(!ack.nativeCards());
        var accepted=ack;
        for(long[] invalid:new long[][]{{1,1,1},{0,1,1},{2,1,1},{1,2,1},{1,4,1},{1,0,1}}) {
            boolean rejected=false;
            try{acknowledgement(invalid[0],(int)invalid[1],invalid[2],accepted);}
            catch(java.io.IOException expected){rejected=true;}
            check(rejected);
        }
        var rejected=acknowledgement(2,0,2,accepted);check(!rejected.accepted());
        check(acknowledgement(3,3,3,rejected).nativeCards());
        Path out = Path.of(args[0]);
        var values = new HashMap<String,Object>();
        // Schema 4 is frozen for already-running WMS clients. New feature data is optional extras.
        check(HomeLayoutPrefsSnapshot.specs().length == 89);
        values.put("prefs_key_" + HomeLayoutPrefsSnapshot.XIAOHEIZI_KEY, true);
        values.put("prefs_key_home_layout_unlock_grids_new", true);
        values.put("prefs_key_home_layout_unlock_grids_cell_x", 7);
        values.put("prefs_key_home_layout_unlock_grids_cell_y", 8);
        values.put("prefs_key_home_layout_workspace_padding_top_enable", true);
        values.put("prefs_key_home_layout_workspace_padding_top", 55);
        values.put("prefs_key_home_layout_small_folder_scale_enable", true);
        values.put("prefs_key_home_layout_small_folder_scale", 83);
        values.put("prefs_key_home_layout_big_folder_scale_enable", true);
        values.put("prefs_key_home_layout_big_folder_scale", 121);
        values.put("prefs_key_home_layout_widget_scale_enable", true);
        values.put("prefs_key_home_layout_widget_scale", 130);
        values.put("prefs_key_home_title_title_icontitlecustomization_onoff", true);
        values.put("prefs_key_home_title_title_icontitlecustomization", Set.of("pkg.name฿本地😀标题฿fixture"));
        values.put("prefs_key_home_drawer_hide_search", true);
        values.put("prefs_key_home_other_infinite_scroll", true);
        values.put("prefs_key_home_dock_icon_title", true);
        values.put("prefs_key_home_recent_card_haptic",true);
        values.put("prefs_key_home_recent_card_close_haptic",true);
        values.put("prefs_key_home_recent_card_haptic_type","3");
        values.put("prefs_key_home_recent_card_haptic_strength",70);
        values.put("prefs_key_home_recent_hide_card", Set.of("z.app", "a.app"));
        values.put("prefs_key_home_dock_bg_custom_enable",true);
        values.put("prefs_key_home_dock_add_blur","3");
        values.put("prefs_key_home_dock_bg_color",0x8844aaff);
        values.put("prefs_key_home_dock_bg_height",113);
        values.put("prefs_key_home_dock_bg_margin_horizontal",31);
        values.put("prefs_key_home_dock_bg_margin_bottom",22);
        values.put("prefs_key_home_dock_bg_radius",45);
        values.put("prefs_key_home_other_home_mode","2");
        values.put("prefs_key_home_dock_unlock_style","elastic_burst");
        var dock=HomeDockPrefs.profile(values);
        check(dock!=null&&dock.enabled()&&dock.mode()==3&&dock.color()==0x8844aaff&&dock.style()==7&&dock.capsule());
        check(HomeDockPrefs.profile(java.util.Map.of("prefs_key_home_dock_bg_height",301)).height()==300);
        check(HomeDockPrefs.profile(java.util.Map.of("prefs_key_home_dock_bg_margin_bottom",-1)).bottom()==0);
        check(HomeDockPrefs.profile(java.util.Map.of("prefs_key_home_dock_bg_height",false))==null);
        check(HomeDockPrefs.profile(java.util.Map.of("prefs_key_home_dock_bg_custom_enable","true"))==null);
        check(!HomeDockPrefs.profile(java.util.Map.of("prefs_key_home_dock_bg_custom_enable",true,
            "prefs_key_allow_hook",false)).enabled());
        var huge = new java.util.HashSet<String>();
        for (int i = 0; i < 1024; ++i) huge.add("pkg" + i + "." + "a".repeat(240));
        check(HomeRecentCardsPrefs.selection(java.util.Map.of("prefs_key_home_recent_hide_card", huge)) == null);
        // A valid independent layout still fits when an extension exceeds the
        // combined packet budget. The receiver sees unavailable, not a reset.
        var bounded = new ByteArrayOutputStream();
        HomeLocalWire.writeSnapshot(new DataOutputStream(bounded), new byte[16384],
            new HomeRecentsHapticPrefs.Profile(false, false, 0, 50), huge, null, 1,HomeLocalWire.VERSION);
        var input = new java.io.DataInputStream(new java.io.ByteArrayInputStream(bounded.toByteArray()));
        check(input.readInt() == 0x48435043 && input.readInt() == HomeLocalWire.VERSION);
        check(input.readInt() == 16444 && input.readLong() == 1);
        input.skipNBytes(12); check(input.readInt() == -1);
        ContentResolver.values = values;
        try (var cursor = HomeLayoutPrefsSnapshot.cursor(() -> values)) {
            check(cursor.getColumnCount() == 90);
            check(cursor.getExtras().getInt(HomeLayoutPrefsSnapshot.XIAOHEIZI_VERSION, 0) == 1);
            check(cursor.getExtras().getInt(HomeLayoutPrefsSnapshot.XIAOHEIZI_KEY, 0) == 1);
        }
        write(out, "enabled");
        check(ContentResolver.reads.get() == 1);
        ContentResolver.values = java.util.Map.of();
        write(out, "reset");
        check(ContentResolver.reads.get() == 2);
        values.remove("prefs_key_" + HomeLayoutPrefsSnapshot.XIAOHEIZI_KEY);
        ContentResolver.values=values;
        write(out,"dock-without-capsule");
        ContentResolver.values=java.util.Map.of("prefs_key_home_dock_bg_height",false);
        write(out,"dock-unavailable");
        check(HomeRecentCardsPrefs.selection(java.util.Map.of()).isEmpty());
        for (Object bad : new Object[]{"a.app", Set.of(3), Set.of("bad..app"), Set.of("a.应用")}) {
            check(HomeRecentCardsPrefs.selection(java.util.Map.of("prefs_key_home_recent_hide_card", bad)) == null);
        }
        for (String gate : new String[]{"prefs_key_allow_hook", "prefs_key_framework_api_allow_hook"}) {
            check(HomeRecentCardsPrefs.selection(java.util.Map.of(gate, false,
                "prefs_key_home_recent_hide_card", Set.of("a.app"))).isEmpty());
        }
        values.put("prefs_key_home_recent_hide_card", Set.of("bad..app"));
        ContentResolver.values = values;
        write(out, "cards-unavailable");
        values.put("prefs_key_home_recent_hide_card", Set.of("z.app", "a.app"));
        ContentResolver.values = values;
        for (String failure : new String[]{"null", "query", "empty", "schema1", "columnName", "read", "close"}) {
            ContentResolver.failure = failure;
            check(HomeLayoutNativeEndpointOS4.readLocalSnapshot(new Context()) == null);
        }
        var worker = HomeLayoutNativeEndpointOS4.class.getDeclaredField("refresher");
        worker.setAccessible(true);
        check(worker.get(null) == null);
        System.out.println("PASS actual Provider -> local snapshot -> shared HCL4 writer, reset/UTF16/errors, no WMS refresher");
    }
}

/* SPDX-License-Identifier: AGPL-3.0-or-later */
package com.sevtinge.hyperceiler.hooker.home;

import android.content.Context;
import com.sevtinge.hyperceiler.core.R;
import com.sevtinge.hyperceiler.common.utils.ShellUtils;
import com.sevtinge.hyperceiler.libhook.rules.home.recent.HomeHapticFirmwareNames;
import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

final class HomeHapticEffectCatalog {
    record Label(String name,int group) { }
    private static volatile Map<Integer,String> firmware=Map.of();
    private static boolean loaded;
    private static final Object LOAD_LOCK=new Object();
    private static String language="";
    private static Map<String,Label> translations=Map.of();
    private HomeHapticEffectCatalog() { }

    /** Background-only discovery; no user path or command input is accepted. */
    static void load(){synchronized(LOAD_LOCK){
        if(loaded)return;
        var names=new ArrayList<String>();boolean restricted=false;
        for(String path:new String[]{"/odm/firmware","/vendor/firmware","/vendor/firmware_mnt/image"}){
            String[] files=new File(path).list();
            if(files==null)restricted=true;else names.addAll(Arrays.asList(files));
        }
        if(restricted){
            String listing=ShellUtils.rootExecCmd("ls -1 /odm/firmware /vendor/firmware /vendor/firmware_mnt/image 2>/dev/null");
            if(listing!=null)names.addAll(Arrays.asList(listing.split("\\R")));
        }
        firmware=HomeHapticFirmwareNames.index(names);loaded=true;
    }}

    static synchronized Label find(Context context,int id){
        if(id<=21)return null; // Android's standard Effect namespace has its own names.
        String token=firmware.get(id);if(token==null)return null;
        String locale=context.getResources().getConfiguration().getLocales().toLanguageTags();
        if(!locale.equals(language)){
            var resources=context.getResources();String[] keys=resources.getStringArray(R.array.home_haptic_firmware_keys);
            String[] names=resources.getStringArray(R.array.home_haptic_firmware_names);
            String[] groups=resources.getStringArray(R.array.home_haptic_firmware_groups);
            var dictionary=new HashMap<String,Label>();
            for(int i=0;i<Math.min(keys.length,Math.min(names.length,groups.length));++i){
                int group=switch(groups[i]){case "system"->0;case "music"->2;case "game"->3;default->4;};
                dictionary.put(keys[i],new Label(names[i],group));
            }
            translations=Map.copyOf(dictionary);language=locale;
        }
        Label known=translations.get(token.toLowerCase(Locale.ROOT));
        // Retain an actual unpublished vendor name in readable form rather than inventing a meaning.
        return known!=null?known:new Label(token.replaceAll("([a-z])([A-Z])","$1 $2").replace('_',' '),4);
    }

    static String groupName(Context context,int group){
        return context.getString(switch(group){case 0->R.string.home_recent_haptic_group_system;
            case 1->R.string.home_recent_haptic_group_basic;case 2->R.string.home_recent_haptic_group_music;
            case 3->R.string.home_recent_haptic_group_game;default->R.string.home_recent_haptic_group_device;});
    }
}

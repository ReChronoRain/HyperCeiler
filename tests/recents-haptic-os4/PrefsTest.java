/* SPDX-License-Identifier: AGPL-3.0-or-later */
import com.sevtinge.hyperceiler.libhook.provider.HomeRecentsHapticPrefs;
import android.content.SharedPreferences;
import java.util.Map;
public final class PrefsTest {
    private static void check(boolean value){if(!value)throw new AssertionError();}
    private static final class Physical implements SharedPreferences {
        Map<String,Object> values;int reads;
        Physical(Map<String,Object> values){this.values=values;}
        public Map<String,?> getAll(){++reads;var captured=values;values=Map.of();return captured;}
    }
    public static void main(String[] args){
        for(boolean scroll:new boolean[]{false,true})for(boolean close:new boolean[]{false,true})
            for(boolean framework:new boolean[]{false,true})for(int type=0;type<4;++type)for(int strength:new int[]{10,50,100}){
                var physical=new Physical(Map.of("prefs_key_home_recent_card_haptic",scroll,
                    "prefs_key_home_recent_card_close_haptic",close,"prefs_key_framework_api_allow_hook",framework,
                    "prefs_key_home_recent_card_haptic_type",String.valueOf(type),"prefs_key_home_recent_card_haptic_strength",strength));
                try(var cursor=HomeRecentsHapticPrefs.cursor(physical)){
                    check(physical.reads==1&&cursor!=null&&cursor.moveToFirst()&&cursor.getColumnCount()==4);
                    check(cursor.getInt(0)==1&&cursor.getInt(1)==(scroll&&framework?1:0));
                    check(cursor.getInt(2)==type&&cursor.getInt(3)==strength);
                    check(cursor.getExtras().getInt("close_version",0)==1);
                    check(cursor.getExtras().getInt("close_enabled",0)==(close&&framework?1:0));
                }
            }
        var defaults=new Physical(Map.of());try(var c=HomeRecentsHapticPrefs.cursor(defaults)){
            check(c.moveToFirst()&&c.getInt(1)==0&&c.getInt(2)==0&&c.getInt(3)==50&&c.getExtras().getInt("close_enabled",1)==0);
        }
        for(Object invalid:new Object[]{"bad",-1,4})check(HomeRecentsHapticPrefs.cursor(new Physical(Map.of("prefs_key_home_recent_card_haptic_type",invalid)))==null);
        for(int invalid:new int[]{9,101})check(HomeRecentsHapticPrefs.cursor(new Physical(Map.of("prefs_key_home_recent_card_haptic_strength",invalid)))==null);
        check(HomeRecentsHapticPrefs.cursor(null)==null);
        System.out.println("haptic physical preference snapshot PASS: 96 switch/type/strength combinations, both master gates, one getAll read, V1 column compatibility and invalid/default values");
    }
}

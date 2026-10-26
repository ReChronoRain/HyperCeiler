from pathlib import Path
import sys,subprocess,json
W=Path(__file__).resolve().parents[2];R=Path(sys.argv[1]).resolve() if len(sys.argv)>1 else W
D=W/'handover/gesture-ui-gate-20261017'/('host-'+R.name);D.mkdir(parents=True,exist_ok=True)
def write(n,s):
 p=D/n;p.parent.mkdir(parents=True,exist_ok=True);p.write_text(s);return str(p)
files=[]
files.append(write('androidx/preference/Preference.java',"""package androidx.preference;public class Preference {private String key;private boolean enabled=true;public Preference(String k){key=k;}public String getKey(){return key;}public void setEnabled(boolean e){enabled=e;}public boolean isEnabled(){return enabled;}}"""))
files.append(write('androidx/preference/PreferenceGroup.java',"""package androidx.preference;import java.util.*;public class PreferenceGroup extends Preference {public List<Preference> list=new ArrayList<>();public PreferenceGroup(String k){super(k);}public void addPreference(Preference p){list.add(p);}public int getPreferenceCount(){return list.size();}public Preference getPreference(int i){return list.get(i);}public Preference findPreference(String k){for(Preference p:list){if(k.equals(p.getKey()))return p;if(p instanceof PreferenceGroup){Preference q=((PreferenceGroup)p).findPreference(k);if(q!=null)return q;}}return null;}}"""))
files.append(write('com/sevtinge/hyperceiler/core/R.java',"""package com.sevtinge.hyperceiler.core;public class R {public static class xml {public static final int home_dock=1,home_title_new=2,home_recent=3,home_other_new=4,home_gesture=5,home_drawer=6;}}"""))
files.append(write('com/sevtinge/hyperceiler/libhook/utils/api/DeviceHelper.java',"""package com.sevtinge.hyperceiler.libhook.utils.api;public class DeviceHelper {public static class System {public static float version=4;public static boolean isHyperOSVersion(float v){return version==v;}}}"""))
files.append(str(R/'library/core/src/main/java/com/sevtinge/hyperceiler/hooker/home/HomeOS4AdaptationGate.java'))
files.append(write('GateTest.java',r"""import androidx.preference.*;import com.sevtinge.hyperceiler.core.R;import com.sevtinge.hyperceiler.hooker.home.HomeOS4AdaptationGate;import com.sevtinge.hyperceiler.libhook.utils.api.DeviceHelper;
public class GateTest {static int checks,failed;static void ok(boolean b,String n){checks++;if(!b){failed++;System.out.println("FAIL="+n);}}
static String[] adapted={"home_gesture_enable","home_navigation_quick_back","home_gesture_double_tap","home_gesture_up_swipe","home_gesture_down_swipe","home_gesture_up_swipe2","home_gesture_down_swipe2","home_navigation_back_area_height","home_navigation_back_area_width","home_gesture_shake","home_navigation_assist_left_slide","home_navigation_assist_right_slide","home_gesture_line_long_press","home_gesture_line_double_click"};
static String[] pending={"home_navigation_predictive_progress","home_gesture_back_haptic","home_navigation_disable_full_screen_back_gesture"};
static PreferenceGroup screen(){PreferenceGroup root=new PreferenceGroup("root"),nested=new PreferenceGroup("prefs_key_home_gesture_actions");root.addPreference(nested);for(String k:adapted)nested.addPreference(new Preference("prefs_key_"+k));for(String k:pending)root.addPreference(new Preference("prefs_key_"+k));return root;}
public static void main(String[] args){PreferenceGroup s=screen();HomeOS4AdaptationGate.apply(s,R.xml.home_gesture);for(String k:adapted)ok(s.findPreference("prefs_key_"+k).isEnabled(),"OS4 enables "+k);for(String k:pending)ok(!s.findPreference("prefs_key_"+k).isEnabled(),"OS4 still gates "+k);ok(s.findPreference("prefs_key_home_gesture_actions").isEnabled(),"category not disabled");
s=screen();s.findPreference("prefs_key_home_gesture_up_swipe").setEnabled(false);HomeOS4AdaptationGate.apply(s,R.xml.home_gesture);ok(!s.findPreference("prefs_key_home_gesture_up_swipe").isEnabled(),"preserve existing dependency false");
for(float v:new float[]{2f,3f}){DeviceHelper.System.version=v;s=screen();HomeOS4AdaptationGate.apply(s,R.xml.home_gesture);for(String k:adapted)ok(s.findPreference("prefs_key_"+k).isEnabled(),"old OS "+k);for(String k:pending)ok(s.findPreference("prefs_key_"+k).isEnabled(),"old OS no new gate "+k);}
DeviceHelper.System.version=4;s=screen();HomeOS4AdaptationGate.apply(s,R.xml.home_drawer);for(String k:adapted)ok(!s.findPreference("prefs_key_"+k).isEnabled(),"unrelated drawer remains gated "+k);
System.out.println("GESTURE_UI_GATE checks="+checks+" failed="+failed+"; production gate with Preference/OS boundary stubs");if(failed>0)System.exit(1);}}
"""))
for cmd in [['javac','-encoding','UTF-8','-d',str(D)]+files,['java','-cp',str(D),'GateTest']]:
 p=subprocess.run(cmd,capture_output=True,text=True,encoding='utf8');print(p.stdout+p.stderr,end='')
 if p.returncode:raise SystemExit(p.returncode)

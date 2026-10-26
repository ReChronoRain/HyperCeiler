"""Compile the real HomeFragment and verify runtime-warning/listener lifecycle.
Android dependencies are host stubs; this is not a device UI test.
"""
from pathlib import Path
import subprocess,sys
W=Path(__file__).resolve().parents[2]
root=Path(sys.argv[1]).resolve() if len(sys.argv)>1 else W
O=W/'handover/runtime-warning-regression'
build=O/root.name;build.mkdir(parents=True,exist_ok=True)
name='library/core/src/main/java/com/sevtinge/hyperceiler/hooker/HomeFragment.java'
stubs={
'android/app/Activity.java':'package android.app;public class Activity {public void runOnUiThread(Runnable r){r.run();}}',
'android/content/Context.java':'package android.content;public class Context {public android.content.pm.PackageManager getPackageManager(){return new android.content.pm.PackageManager();}}',
'android/content/pm/PackageManager.java':'''package android.content.pm;public class PackageManager {public static boolean runtime=true;public static final int GET_META_DATA=1;public static class NameNotFoundException extends Exception{} public ApplicationInfo getApplicationInfo(String p,int f)throws NameNotFoundException{return new ApplicationInfo();}}''',
'android/content/pm/ApplicationInfo.java':'package android.content.pm;public class ApplicationInfo {public android.os.Bundle metaData=new android.os.Bundle();}',
'android/os/Bundle.java':'package android.os;public class Bundle {public boolean getBoolean(String k,boolean d){return android.content.pm.PackageManager.runtime;}}',
'android/content/SharedPreferences.java':'''package android.content; public interface SharedPreferences {boolean getBoolean(String k,boolean d);int getInt(String k,int d);long getLong(String k,long d);String getString(String k,String d);interface OnSharedPreferenceChangeListener{void onSharedPreferenceChanged(SharedPreferences p,String k);}void registerOnSharedPreferenceChangeListener(OnSharedPreferenceChangeListener l);void unregisterOnSharedPreferenceChangeListener(OnSharedPreferenceChangeListener l);}''',
'com/sevtinge/hyperceiler/core/R.java':'package com.sevtinge.hyperceiler.core;public class R {public static class xml {public static int home=1,home_new=2;}}',
'com/sevtinge/hyperceiler/prefs/LayoutPreference.java':'package com.sevtinge.hyperceiler.prefs;public class LayoutPreference {public boolean visible=true;public void setVisible(boolean v){visible=v;}}',
'com/sevtinge/hyperceiler/libhook/utils/api/DeviceHelper.java':'package com.sevtinge.hyperceiler.libhook.utils.api;public class DeviceHelper {public static class System {public static float version=4f;public static boolean isMoreHyperOSVersion(float v){return version>=v;}public static boolean isHyperOSVersion(float v){return version==v;}}}',
'com/sevtinge/hyperceiler/libhook/utils/pkg/CheckModifyUtils.java':'package com.sevtinge.hyperceiler.libhook.utils.pkg;public class CheckModifyUtils {public static final CheckModifyUtils INSTANCE=new CheckModifyUtils();public boolean getCheckResult(Object c,String p){return false;}}',
'com/sevtinge/hyperceiler/dashboard/DashboardFragment.java':'''package com.sevtinge.hyperceiler.dashboard;
import java.util.*;import android.content.*;import com.sevtinge.hyperceiler.prefs.*;
public class DashboardFragment {
 public final Map<String,LayoutPreference> entries=new HashMap<>();public final TestPrefs prefs=new TestPrefs();boolean resumed;
 public int getPreferenceScreenResId(){return 0;}public void initPrefs(){}public Context getContext(){return new Context();}
 public LayoutPreference findPreference(String k){return entries.get(k);}public SharedPreferences getSharedPreferences(){return prefs;}
 public void onResume(){resumed=true;}public void onPause(){resumed=false;}public boolean isResumed(){return resumed;}public android.app.Activity getActivity(){return new android.app.Activity();}
 public static class TestPrefs implements SharedPreferences {
  public final Map<String,Object> values=new HashMap<>();public final Set<OnSharedPreferenceChangeListener> listeners=new HashSet<>();
  public boolean getBoolean(String k,boolean d){Object v=values.get(k);return v instanceof Boolean?(Boolean)v:d;}
  public int getInt(String k,int d){Object v=values.get(k);return v instanceof Number?((Number)v).intValue():d;}
  public long getLong(String k,long d){Object v=values.get(k);return v instanceof Number?((Number)v).longValue():d;}
  public String getString(String k,String d){Object v=values.get(k);return v instanceof String?(String)v:d;}
  public void registerOnSharedPreferenceChangeListener(OnSharedPreferenceChangeListener l){listeners.add(l);}
  public void unregisterOnSharedPreferenceChangeListener(OnSharedPreferenceChangeListener l){listeners.remove(l);}
  public void put(String k,Object v){values.put(k,v);for(var l:new ArrayList<>(listeners))l.onSharedPreferenceChanged(this,k);}
 }
}''',
}
sources=[str(root/name),str(W/'tests/home-runtime-warning/RuntimeWarningTest.java')]
for n,s in stubs.items():
    p=build/'stubs'/n;p.parent.mkdir(parents=True,exist_ok=True);p.write_text(s,encoding='utf-8');sources.append(str(p))
jdk=Path('D:/build-tools/jdk-25.0.4.1+1/bin')
r=subprocess.run([str(jdk/'javac.exe'),'-J-Duser.language=en','-J-Duser.country=US','-encoding','UTF-8','-d',str(build)]+sources,capture_output=True,text=True,encoding='utf8',errors='replace')
assert r.returncode==0,r.stderr
r=subprocess.run([str(jdk/'java.exe'),'-ea','-cp',str(build),'com.sevtinge.hyperceiler.hooker.RuntimeWarningTest'],capture_output=True,text=True,encoding='utf8',errors='replace')
print(r.stdout,end='');print('TEST_EXIT='+str(r.returncode));assert r.returncode==0,r.stderr
print('HOME_RUNTIME_WARNING=20 checks; failed=0; actual class with Android host stubs')

"""Production snapshot + receive/perform bodies; Binder identity/services/actions are controlled stubs."""
from pathlib import Path
import sys,subprocess,json
W=Path(__file__).resolve().parents[2];R=Path(sys.argv[1]).resolve() if len(sys.argv)>1 else W
J=R/'library/libhook/src/main/java/com/sevtinge/hyperceiler/libhook';D=W/'handover/gesture-extra-os4-20261017'/('endpoint-'+R.name);D.mkdir(parents=True,exist_ok=True)
s=(J/'rules/home/gesture/HomeQuickSwitchOS4.java').read_text()
if 'static int actionIndex' not in s:print('EXTRA_ENDPOINT=missing wire mapping; failed=1');sys.exit(1)
def body(start):
 a=s.index(start);b=s.index('{',a);i=b+1;depth=1
 while depth:
  if s[i]=='{':depth+=1
  elif s[i]=='}':depth-=1
  i+=1
 return s[a:i]
files=[]
def write(n,t):
 p=D/n;p.parent.mkdir(parents=True,exist_ok=True);p.write_text(t);files.append(str(p))
stubs={
'android/net/Uri.java':'package android.net;public class Uri {}',
'android/database/Cursor.java':'package android.database;public interface Cursor extends AutoCloseable {boolean moveToFirst();int getInt(int i);String getString(int i);boolean isNull(int i);int getColumnCount();int getColumnIndex(String k);String getColumnName(int i);void close();}',
'android/database/MatrixCursor.java':'''package android.database;import java.util.*;public class MatrixCursor implements Cursor {String[] columns;List<Object> values=new ArrayList<>();public MatrixCursor(String[] c){columns=c;}public class RowBuilder {public RowBuilder add(Object o){values.add(o);return this;}}public RowBuilder newRow(){return new RowBuilder();}public boolean moveToFirst(){return true;}public int getInt(int i){Object v=values.get(i);return v instanceof Number?((Number)v).intValue():Integer.parseInt(v.toString());}public String getString(int i){return values.get(i).toString();}public boolean isNull(int i){return values.get(i)==null;}public int getColumnCount(){return columns.length;}public int getColumnIndex(String n){return Arrays.asList(columns).indexOf(n);}public String getColumnName(int i){return columns[i];}public void close(){}}''',
'android/content/SharedPreferences.java':'package android.content;import java.util.*;public interface SharedPreferences {Map<String,?> getAll();}',
'android/content/ComponentName.java':'package android.content;public class ComponentName {String p,c;public ComponentName(String p,String c){this.p=p;this.c=c;}public String getPackageName(){return p;}public String getClassName(){return c;}}',
'android/content/Intent.java':'package android.content;public class Intent {public static String ACTION_MAIN="MAIN",CATEGORY_HOME="HOME";public Intent(String a){}public Intent addCategory(String c){return this;}}',
'android/content/pm/PackageManager.java':'''package android.content.pm;public class PackageManager {public static int MATCH_DEFAULT_ONLY=1;public static class Info {public String packageName="resolved.home",name="DynamicActivity",targetActivity;}public static class Resolve {public Info activityInfo=new Info();}public Resolve resolveActivity(android.content.Intent i,int f){return new Resolve();}}''',
'android/app/ActivityManager.java':'''package android.app;import java.util.*;public class ActivityManager {public static boolean home=true;public static class Task {public android.content.ComponentName topActivity=new android.content.ComponentName(home?"resolved.home":"app",home?"DynamicActivity":"Other");}public List<Task> getRunningTasks(int n){return List.of(new Task());}}''',
'android/app/KeyguardManager.java':'package android.app;public class KeyguardManager {public static boolean locked;public boolean isKeyguardLocked(){return locked;}}',
'android/os/PowerManager.java':'package android.os;public class PowerManager {public static boolean interactive=true;public boolean isInteractive(){return interactive;}}',
'android/os/IBinder.java':'package android.os;public interface IBinder {int FLAG_ONEWAY=1;}',
'android/os/SystemClock.java':'package android.os;public class SystemClock {public static long now=10000;public static long uptimeMillis(){return now;}}',
'android/os/SystemProperties.java':'package android.os;public class SystemProperties {public static boolean getBoolean(String n,boolean b){return false;}}',
'android/os/Binder.java':'package android.os;public class Binder {public static int uid=1000,pid=222;public static int getCallingUid(){return uid;}public static int getCallingPid(){return pid;}public static long clearCallingIdentity(){return 1;}public static void restoreCallingIdentity(long t){}}',
'android/os/Parcel.java':'''package android.os;import java.util.*;public class Parcel {public List<Number> data=new ArrayList<>();int index;public Parcel(Number... n){data.addAll(Arrays.asList(n));}public void enforceInterface(String s){if(!s.equals("android.view.IWindowManager"))throw new IllegalArgumentException();}public int readInt(){return data.get(index++).intValue();}public long readLong(){return data.get(index++).longValue();}public int dataAvail(){return (data.size()-index)*4;}public void writeInt(int i){data.add(i);}}''',
'android/content/Context.java':'''package android.content;import java.util.*;import android.database.*;public class Context {
 public static String KEYGUARD_SERVICE="key",POWER_SERVICE="power";public static Map<String,Object> prefs=new HashMap<>();public static Cursor cursor(){return com.sevtinge.hyperceiler.libhook.provider.HomeGesturePrefsSnapshot.cursor(()->prefs);}
 public static class Resolver {public Cursor query(android.net.Uri u,Object a,Object b,Object c,Object d){return cursor();}}public Resolver getContentResolver(){return new Resolver();}public Object getSystemService(String s){return s.equals("key")?new android.app.KeyguardManager():new android.os.PowerManager();}public <T>T getSystemService(Class<T> c){try{return c.getConstructor().newInstance();}catch(Exception e){throw new RuntimeException(e);}}public android.content.pm.PackageManager getPackageManager(){return new android.content.pm.PackageManager();}}''',
'android/util/Log.java':'package android.util;public class Log {public static int i(String t,String s){return 0;}public static int w(String t,String s,Exception e){return 0;}}',
'com/sevtinge/hyperceiler/libhook/utils/api/ContextUtils.java':'package com.sevtinge.hyperceiler.libhook.utils.api;public class ContextUtils {public static int FlAG_ONLY_ANDROID=1;public static android.content.Context getContextNoError(int i){return new android.content.Context();}}',
'com/sevtinge/hyperceiler/common/utils/PrefsBridge.java':'package com.sevtinge.hyperceiler.common.utils;import java.util.*;public class PrefsBridge {public static Map<String,Object> cache=new HashMap<>();public static void putHookCache(String k,Object v){cache.put(k,v);}}',
'com/sevtinge/hyperceiler/libhook/appbase/systemframework/GlobalActionBridge.java':'package com.sevtinge.hyperceiler.libhook.appbase.systemframework;public class GlobalActionBridge {public static String key;public static boolean handleAction(android.content.Context c,String k){key=k;return true;}}'
}
for n,t in stubs.items():write(n,t)
pre='''import android.app.ActivityManager;import android.content.Context;import android.database.Cursor;import android.os.*;import android.net.Uri;import com.sevtinge.hyperceiler.libhook.utils.api.ContextUtils;
public class EndpointTest {
static int ACK=0x48435131;static long lastSwitch;static Uri PREFS=new Uri();static int switches;
static boolean isSystemUI(Context c,int uid,int pid){return uid==1000&&pid==222;}static boolean isLauncher(Context c,int uid,int pid){return uid==20001&&pid==111;}
static boolean switchPrevious(Context c,int expected){switches++;return true;}
'''
for n in ['static boolean synchronous(','private static void diagnostic(','public static synchronized void receive(','private static boolean enabled(','private static boolean performGesture(','static int actionIndex(','private static boolean homeForeground(']:pre+=body(n)+'\n'
pre+=r'''
static int checks,failed;static void ok(boolean b,String n){checks++;if(!b){failed++;System.out.println("FAIL="+n);}}
static boolean call(int event,int uid,int pid,int flags,long age,boolean extra) {
 Binder.uid=uid;Binder.pid=pid;SystemClock.now+=1000;
 Parcel in=event==7?new Parcel(event,SystemClock.now-age,42):new Parcel(event,SystemClock.now-age);
 if(extra)in.data.add(99);Parcel reply=new Parcel();receive(in,reply,flags);ok(reply.data.size()==2&&reply.data.get(0).intValue()==ACK,"ack shape");return reply.data.get(1).intValue()==1;
}
public static void main(String[]args) {
 Context.prefs.put("prefs_key_home_gesture_enable",true);Context.prefs.put("prefs_key_home_navigation_quick_back",true);
 var keys=com.sevtinge.hyperceiler.libhook.provider.HomeGesturePrefsSnapshot.ACTIONS;
 for(String key:keys){Context.prefs.put("prefs_key_"+key+"_action",8);Context.prefs.put("prefs_key_"+key+"_app","test.app");}
 try(Cursor c=Context.cursor()){ok(c.getColumnCount()==43&&c.moveToFirst(),"production snapshot 43 columns");for(String key:keys)ok(c.getInt(c.getColumnIndex(key+"_action"))==8,"every action serialized");}
 for(int event:new int[]{1,2,3,4,5,8,9,10,11,12}){
  boolean ui=event>=8;ok(call(event,ui?1000:20001,ui?222:111,16,0,false),"action accepted "+event);
  ok(com.sevtinge.hyperceiler.libhook.appbase.systemframework.GlobalActionBridge.key.equals(keys[actionIndex(event)]),"correct action "+event);
  ok(!call(event,ui?20001:1000,ui?111:222,0,0,false),"wrong process role "+event);
 }
 ok(call(6,20001,111,0,0,false)&&call(7,1000,222,0,0,false)&&switches==2,"Previous contract retained");
 for(int event:new int[]{0,13,-1})ok(!call(event,1000,222,0,0,false),"unknown wire event");
 ok(!call(9,1000,222,1,0,false),"one way rejected");ok(!call(9,1000,222,0,751,false),"stale");ok(!call(9,1000,222,0,-1,false),"future");ok(!call(9,1000,222,0,0,true),"trailing bytes");
 android.app.KeyguardManager.locked=true;ok(!call(9,1000,222,0,0,false),"locked");android.app.KeyguardManager.locked=false;
 PowerManager.interactive=false;ok(!call(9,1000,222,0,0,false),"screen off");PowerManager.interactive=true;
 android.app.ActivityManager.home=false;ok(!call(8,1000,222,0,0,false),"shake foreground rechecked in server");ok(call(9,1000,222,0,0,false),"corner works over app");android.app.ActivityManager.home=true;
 Context.prefs.put("prefs_key_home_gesture_enable",false);ok(!call(11,1000,222,0,0,false),"master off");ok(call(7,1000,222,0,0,false),"quick independent");Context.prefs.put("prefs_key_home_gesture_enable",true);
 Context.prefs.put("prefs_key_home_gesture_line_double_click_action",0);ok(!call(12,1000,222,0,0,false),"action off");ok(call(11,1000,222,0,0,false),"other action unaffected");
 Context.prefs.put("prefs_key_home_gesture_line_long_press_action",13);ok(call(11,1000,222,0,0,false)&&com.sevtinge.hyperceiler.common.utils.PrefsBridge.cache.get("home_gesture_line_long_press_action").equals(13),"fresh payload reload");
 System.out.println("EXTRA_ENDPOINT_CHECKS="+checks+" FAILED="+failed+"; production snapshot/receive/perform, caller identity/Binder/actions mocked");if(failed>0)System.exit(1);
}}
'''
write('EndpointTest.java',pre);files.append(str(J/'provider/HomeGesturePrefsSnapshot.java'))
rows=[]
for cmd in [['javac','-encoding','UTF-8','-d',str(D)]+files,['java','-cp',str(D),'EndpointTest']]:
 p=subprocess.run(cmd,capture_output=True,text=True,encoding='utf8',errors='replace');print(p.stdout+p.stderr,end='');rows.append(dict(command=cmd,input_root=str(R),stdout=p.stdout,stderr=p.stderr,exit=p.returncode))
 if p.returncode:break
(D/'results.json').write_text(json.dumps(rows,indent=2));raise SystemExit(rows[-1]['exit'])

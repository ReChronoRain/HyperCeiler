#!/usr/bin/env python3
# SPDX-License-Identifier: AGPL-3.0-or-later
"""Real pinned libsu manager + production client, with Android and remote Binder fixtures."""
from pathlib import Path
import io,os,re,subprocess,tempfile,zipfile,hashlib
from run import fixtures,root,here,jdk

version=re.search(r'^libsu\s*=\s*"([^"]+)"',(root/'gradle/libs.versions.toml').read_text(),re.M).group(1)
if version!='6.0.0':raise RuntimeError('Review the manager contract when updating libsu')
cache=Path(os.environ.get('GRADLE_USER_HOME',str(Path.home()/'.gradle')))/'caches/modules-2/files-2.1/com.github.topjohnwu.libsu/service'/version
artifacts=list(cache.glob('*/*.aar'))
if len(artifacts)!=1:raise RuntimeError('Resolve the pinned Gradle service dependency first')
artifact=artifacts[0]
print('Testing libsu',version,'AAR SHA256',hashlib.sha256(artifact.read_bytes()).hexdigest(),flush=True)
fixtures=fixtures.copy()
fixtures.update({
'androidx/annotation/RestrictTo.java':'package androidx.annotation;public @interface RestrictTo {Scope[] value();enum Scope {LIBRARY}}',
'android/content/Context.java':'''package android.content;import android.os.*;public class Context {
public BroadcastReceiver receiver;public Context getApplicationContext(){return this;}public String getPackageName(){return "com.sevtinge.hyperceiler";}
public Intent registerReceiver(BroadcastReceiver r,IntentFilter f,String p,Handler h,int flags){receiver=r;return null;} }''',
'android/content/ComponentName.java':'''package android.content;public record ComponentName(String pkg,String name){
public String getPackageName(){return pkg;}public String getClassName(){return name;}public String flattenToString(){return pkg+"/"+name;} }''',
'android/content/Intent.java':'''package android.content;import java.util.*;import android.os.*;public class Intent {
private ComponentName component;private final Set<String> categories=new HashSet<>();private final Map<String,Object> extras=new HashMap<>();
public Intent(String action){}public Intent(Context c,Class<?> t){component=new ComponentName(c.getPackageName(),t.getName());}
public Intent addCategory(String c){categories.add(c);return this;}public boolean hasCategory(String c){return categories.contains(c);}
public ComponentName getComponent(){return component;}public Intent setPackage(String p){return this;}public Intent addFlags(int f){return this;}
public Intent putExtra(String k,boolean v){extras.put(k,v);return this;}public Intent putExtra(String k,Bundle v){extras.put(k,v);return this;}
public Bundle getBundleExtra(String k){return (Bundle)extras.get(k);}public boolean getBooleanExtra(String k,boolean f){return extras.containsKey(k)?(boolean)extras.get(k):f;} }''',
'android/content/BroadcastReceiver.java':'package android.content;public abstract class BroadcastReceiver {public abstract void onReceive(Context c,Intent i);}',
'android/content/IntentFilter.java':'package android.content;public class IntentFilter {public IntentFilter(String s){}}',
'android/util/ArrayMap.java':'package android.util;public class ArrayMap<K,V> extends java.util.LinkedHashMap<K,V>{}',
'android/util/Pair.java':'''package android.util;public class Pair<F,S> {public final F first;public final S second;public Pair(F f,S s){first=f;second=s;}
@Override public boolean equals(Object o){return o instanceof Pair<?,?> p&&java.util.Objects.equals(first,p.first)&&java.util.Objects.equals(second,p.second);}
@Override public int hashCode(){return java.util.Objects.hash(first,second);} }''',
'android/util/Log.java':'package android.util;public class Log {public static int e(String t,String m){throw new AssertionError(m);}}',
'android/os/Build.java':'package android.os;public class Build {public static class VERSION {public static int SDK_INT=37;}}',
'android/os/Message.java':'package android.os;public class Message {public int what,arg1;public Object obj;}',
'android/os/Bundle.java':'''package android.os;public class Bundle {private final java.util.Map<String,Object> values=new java.util.HashMap<>();
public void putBinder(String k,IBinder b){values.put(k,b);}public IBinder getBinder(String k){return (IBinder)values.get(k);}
public void putInt(String k,int v){values.put(k,v);}public int getInt(String k,int f){return values.containsKey(k)?(int)values.get(k):f;}
public void putString(String k,String v){values.put(k,v);}public String getString(String k){return (String)values.get(k);} }''',
'android/os/Messenger.java':'''package android.os;public class Messenger {private final IBinder binder=new IBinder(){
public void linkToDeath(DeathRecipient r,int flags){}public boolean unlinkToDeath(DeathRecipient r,int flags){return true;}};
public Messenger(Handler h){}public IBinder getBinder(){return binder;} }''',
'com/topjohnwu/superuser/ShellUtils.java':'package com.topjohnwu.superuser;public class ShellUtils {public static boolean onMainThread(){return true;}}',
'com/topjohnwu/superuser/internal/HiddenAPIs.java':'package com.topjohnwu.superuser.internal;public class HiddenAPIs {public static final int FLAG_RECEIVER_FROM_SHELL=0x400000;}',
'com/topjohnwu/superuser/internal/Utils.java':'''package com.topjohnwu.superuser.internal;import android.content.*;
public class Utils {public static final Context context=new Context();public static Context getContext(){return context;}
public static void err(String tag,Throwable t){throw new AssertionError(t);} }''',
'com/topjohnwu/superuser/internal/BinderHolder.java':'''package com.topjohnwu.superuser.internal;import android.os.*;
public abstract class BinderHolder implements IBinder.DeathRecipient {public BinderHolder(IBinder b)throws RemoteException{b.linkToDeath(this,0);}
public void binderDied(){new Handler(Looper.getMainLooper()).post(this::onBinderDied);}protected abstract void onBinderDied();}''',
'com/topjohnwu/superuser/internal/IRootServiceManager.java':'''package com.topjohnwu.superuser.internal;import android.os.*;import android.content.*;
public interface IRootServiceManager {IBinder asBinder();void connect(IBinder client)throws RemoteException;IBinder bind(Intent i)throws RemoteException;
void unbind(ComponentName n)throws RemoteException;void stop(ComponentName n,int uid)throws RemoteException;
abstract class Stub {public static IRootServiceManager asInterface(IBinder b){return (IRootServiceManager)b;}} }''',
'com/topjohnwu/superuser/ipc/RootService.java':'''package com.topjohnwu.superuser.ipc;import android.content.*;import java.util.*;import com.topjohnwu.superuser.internal.RootServiceManager;
public class RootService {public static final String CATEGORY_DAEMON_MODE="com.topjohnwu.superuser.DAEMON_MODE";public static final List<ServiceConnection> binds=new ArrayList<>();public static int launches;
public static void bind(Intent i,ServiceConnection c){binds.add(c);if(RootServiceManager.getInstance().createBindTask(i,Runnable::run,c)!=null)++launches;}
public static void unbind(ServiceConnection c){RootServiceManager.getInstance().unbind(c);}public boolean onUnbind(Intent i){return false;} }''',
})
fixtures['com/topjohnwu/superuser/Shell.java']=fixtures['com/topjohnwu/superuser/Shell.java'].replace('public class Shell {','public class Shell {public interface Task {void run(java.io.OutputStream in,java.io.InputStream out,java.io.InputStream err)throws java.io.IOException;}')
fixtures['android/os/Handler.java']=fixtures['android/os/Handler.java'].replace('public Handler(Looper l){}','public interface Callback {boolean handleMessage(Message m);}public Handler(Looper l){}public Handler(Looper l,Callback c){}')
with tempfile.TemporaryDirectory(prefix='hc-libsu-manager-') as directory:
    out=Path(directory)
    with zipfile.ZipFile(artifact) as aar,zipfile.ZipFile(io.BytesIO(aar.read('classes.jar'))) as jar:
        for name in jar.namelist():
            if name.startswith('com/topjohnwu/superuser/internal/RootServiceManager') and name.endswith('.class'):
                target=out/name;target.parent.mkdir(parents=True,exist_ok=True);target.write_bytes(jar.read(name))
    files=[]
    for name,body in fixtures.items():
        target=out/name;target.parent.mkdir(parents=True,exist_ok=True);target.write_text(body);files.append(target)
    sdk=root/'tests/home-layout-native/stubs'
    files += [sdk/p for p in ['android/content/SharedPreferences.java','android/database/Cursor.java','android/database/MatrixCursor.java']]
    client=out/'production/HomeConfigRootClient.java';client.parent.mkdir(parents=True,exist_ok=True)
    client.write_bytes(Path(os.environ.get('HC_ROOT_CLIENT_SOURCE',str(root/'app/src/main/java/com/sevtinge/hyperceiler/home/runtime/HomeConfigRootClient.java'))).read_bytes())
    files += [client,root/'app/src/main/java/com/sevtinge/hyperceiler/home/runtime/HomeLocalWire.java']
    files += [root/'library/libhook/src/main/java/com/sevtinge/hyperceiler/libhook/provider/HomeRecentsHapticPrefs.java',here/'ManagerIntegrationTest.java']
    subprocess.run([str(jdk/'javac'),'--release','21','-encoding','UTF-8','-cp',str(out),'-d',str(out),*map(str,files)],check=True)
    for case in ['pending','late-disabled','late-new-owner','death']:
        subprocess.run([str(jdk/'java'),'-ea','-cp',str(out),'com.sevtinge.hyperceiler.home.runtime.ManagerIntegrationTest',case],check=True,timeout=10)

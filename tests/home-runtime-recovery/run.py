#!/usr/bin/env python3
"""Production client lifecycle; only Android scheduler/Binder and libsu transport fixtures."""
from pathlib import Path
import os,subprocess,tempfile

root=Path(__file__).resolve().parents[2]
here=Path(__file__).resolve().parent
jdk=Path(os.environ.get('JAVA_HOME','/opt/homebrew/opt/openjdk@25/libexec/openjdk.jdk/Contents/Home'))/'bin'
# Compile the production unbind body with the unavailable Android transport fixtures.
service=(root/'app/src/main/java/com/sevtinge/hyperceiler/home/runtime/HomeConfigRootService.java').read_text()
a=service.index('@Override public boolean onUnbind(');b=service.index('{',a);depth=1;end=b+1
while depth:
    if service[end]=='{':depth+=1
    if service[end]=='}':depth-=1
    end+=1
unbind=service[a:end]
fixtures={
'androidx/annotation/Keep.java':'package androidx.annotation; public @interface Keep {}',
'android/content/Context.java':'package android.content; public class Context { public Context getApplicationContext(){return this;} }',
'android/content/ComponentName.java':'package android.content; public class ComponentName {}',
'android/content/Intent.java':'package android.content; public class Intent { public Intent(Context c,Class<?> t){} public Intent addCategory(String c){return this;} }',
'android/content/ServiceConnection.java':'''package android.content; import android.os.IBinder; public interface ServiceConnection {
void onServiceConnected(ComponentName c,IBinder b);void onServiceDisconnected(ComponentName c);
default void onNullBinding(ComponentName c){} default void onBindingDied(ComponentName c){} }''',
'android/os/Looper.java':'package android.os; public class Looper { public static Looper getMainLooper(){return new Looper();} }',
'android/os/Handler.java':'''package android.os; import java.util.*; public class Handler {
private record Event(long at,long sequence,Runnable task){} private static long now,sequence;
private static final PriorityQueue<Event> events=new PriorityQueue<>(Comparator.comparingLong(Event::at).thenComparingLong(Event::sequence));
public Handler(Looper l){} public boolean post(Runnable r){return postDelayed(r,0);}
public boolean postDelayed(Runnable r,long ms){synchronized(events){events.add(new Event(now+ms,++sequence,r));}return true;}
public void removeCallbacks(Runnable r){synchronized(events){events.removeIf(e->e.task()==r);}}
public static void drain(){for(;;){Event e;synchronized(events){e=events.peek();if(e==null||e.at()>now)return;events.remove();}e.task().run();}}
public static long uptime(){return now;} public static void advance(long ms){synchronized(events){now+=ms;}drain();} }''',
'android/os/SystemClock.java':'package android.os; public class SystemClock {public static long uptimeMillis(){return Handler.uptime();}}',
'android/os/SystemProperties.java':'package android.os; public class SystemProperties { public static String backend="root-socket"; public static String get(String k,String d){return backend;} }',
'android/os/RemoteException.java':'package android.os; public class RemoteException extends Exception {}',
'android/os/IBinder.java':'''package android.os; public interface IBinder {
interface DeathRecipient{void binderDied();} void linkToDeath(DeathRecipient r,int flags)throws RemoteException;
boolean unlinkToDeath(DeathRecipient r,int flags); }''',
'android/os/ParcelFileDescriptor.java':'''package android.os; import java.io.*; public class ParcelFileDescriptor implements AutoCloseable {
public static ParcelFileDescriptor dup(FileDescriptor fd)throws IOException{return new ParcelFileDescriptor();}public void close()throws IOException{} }''',
'android/system/ErrnoException.java':'package android.system; public class ErrnoException extends Exception {}',
'android/system/Os.java':'package android.system; import java.io.*; public class Os {public static void close(FileDescriptor f)throws ErrnoException{}}',
'org/lsposed/hiddenapibypass/HiddenApiBypass.java':'''package org.lsposed.hiddenapibypass; public class HiddenApiBypass {public static Object invoke(Class<?> t,Object receiver,String n,Object... a)throws ReflectiveOperationException{throw new AssertionError("No platform shared-memory class exists on the host");}}''',
'com/topjohnwu/superuser/Shell.java':'package com.topjohnwu.superuser; public class Shell {public static Boolean grant=true; public static Boolean isAppGrantedRoot(){return grant;}}',
'com/topjohnwu/superuser/ipc/RootService.java':'''package com.topjohnwu.superuser.ipc; import java.util.*;import android.content.*;
public class RootService { public boolean onUnbind(Intent intent){return false;} public static final String CATEGORY_DAEMON_MODE="daemon";public static final List<ServiceConnection> binds=new ArrayList<>();public static int unbinds;public static boolean dispatching;public static final List<ServiceConnection> released=new ArrayList<>();
public static void bind(Intent i,ServiceConnection c){binds.add(c);}public static void unbind(ServiceConnection c){if(dispatching)throw new AssertionError("unbind during connection callback iteration");++unbinds;released.add(c);} }''',
'com/sevtinge/hyperceiler/common/log/ModuleLog.java':'''package com.sevtinge.hyperceiler.common.log; import java.util.*; public class ModuleLog {
public static final List<String> messages=Collections.synchronizedList(new ArrayList<>());
public static void i(String t,String m){messages.add(m);}public static void w(String t,String m){messages.add(m);}public static void w(String t,String m,Throwable e){messages.add(m);} }''',
'com/sevtinge/hyperceiler/home/runtime/HomeConfigRootService.java':'package com.sevtinge.hyperceiler.home.runtime; import android.content.Intent;import android.os.Handler;import android.os.Looper;import com.topjohnwu.superuser.ipc.RootService; public class HomeConfigRootService extends RootService { public int stops;private void requestStop(){++stops;}'+unbind+'}',
'com/sevtinge/hyperceiler/libhook/IHomeConfigAgent.java':'''package com.sevtinge.hyperceiler.libhook; import android.os.*; public interface IHomeConfigAgent {
int protocolVersion()throws RemoteException;boolean initializeFramework(ParcelFileDescriptor fd)throws RemoteException;
boolean initializeFromCallerMap(long address)throws RemoteException;boolean isReady()throws RemoteException;void refresh()throws RemoteException;void stop()throws RemoteException;
abstract class Stub implements IHomeConfigAgent,IBinder {public static IHomeConfigAgent asInterface(IBinder b){return (IHomeConfigAgent)b;}} }''',
}
if __name__=='__main__':
    with tempfile.TemporaryDirectory(prefix='hc-root-recovery-') as directory:
        out=Path(directory)
        files=[]
        for name,body in fixtures.items():
            target=out/name;target.parent.mkdir(parents=True,exist_ok=True);target.write_text(body);files.append(target)
        sdk=root/'tests/home-layout-native/stubs'
        files += [sdk/p for p in ['android/content/SharedPreferences.java','android/database/Cursor.java','android/database/MatrixCursor.java','android/os/Bundle.java']]
        files += [root/'app/src/main/java/com/sevtinge/hyperceiler/home/runtime'/name for name in ['HomeConfigRootClient.java','HomeLocalWire.java']]
        files += [root/'library/libhook/src/main/java/com/sevtinge/hyperceiler/libhook/provider/HomeRecentsHapticPrefs.java',here/'RecoveryTest.java',here/'ServiceLifecycleTest.java']
        files += [root/'library/libhook/src/main/java/com/sevtinge/hyperceiler/libhook'/p for p in ['provider/HomeDockPrefs.java','provider/HomeLayoutPrefsSnapshot.java','rules/home/dock/DockUnlockReveal.java']]
        subprocess.run([str(jdk/'javac'),'--release','21','-encoding','UTF-8','-d',str(out),*map(str,files)],check=True)
        for case in ['late-disabled','late-new-session','pending-timeout','late-framework-timeout','framework-timeout','callback-reentrancy','connected','death','dead-link','backoff','timeout','denied','revoked','probe','protocol','upgrade','upgrade6','upgrade7','upgrade8','upgrade9','null','disabled','disabled-retry','stale','partial']:
            subprocess.run([str(jdk/'java'),'-ea','-cp',str(out),'com.sevtinge.hyperceiler.home.runtime.RecoveryTest',case],check=True,timeout=10)

        subprocess.run([str(jdk/'java'),'-ea','-cp',str(out),'com.sevtinge.hyperceiler.home.runtime.ServiceLifecycleTest'],check=True,timeout=10)

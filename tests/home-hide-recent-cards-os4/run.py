#!/usr/bin/env python3
"""Run the actual Provider, runtime group filter and SystemUI hook against observable boundaries."""
from pathlib import Path
import os,subprocess,tempfile
R=Path(__file__).resolve().parents[2];H=Path(__file__).resolve().parent
sources={
'android/os/IBinder.java':'package android.os;public interface IBinder{}',
'android/os/IInterface.java':'package android.os;public interface IInterface{}',
'android/os/Binder.java':'package android.os;public class Binder implements IBinder{public static int uid=10139;public static int getCallingUid(){return uid;}}',
'android/os/SystemClock.java':'package android.os;public class SystemClock{public static long now=10000;public static long elapsedRealtime(){return now;}}',
'android/content/ComponentName.java':'package android.content;public class ComponentName{final String p;public ComponentName(String p){this.p=p;}public String getPackageName(){return p;}}',
'android/content/Intent.java':'package android.content;public class Intent{public ComponentName c;public String p;public ComponentName getComponent(){return c;}public String getPackage(){return p;}}',
'android/app/TaskInfo.java':'package android.app;public class TaskInfo{public android.content.ComponentName realActivity;public android.content.Intent baseIntent;public int taskId,userId;}',
'android/content/pm/ApplicationInfo.java':'package android.content.pm;public class ApplicationInfo{public int uid=10139;}',
'android/content/pm/PackageManager.java':'package android.content.pm;public class PackageManager{public ApplicationInfo getApplicationInfo(String n,int f){return new ApplicationInfo();}}',
'android/database/ContentObserver.java':'package android.database;public class ContentObserver{public ContentObserver(Object handler){}public void onChange(boolean self,android.net.Uri uri){}}',
'android/content/ContentResolver.java':'''package android.content;public class ContentResolver{
public java.util.Map<String,Object> values=new java.util.HashMap<>();public int queries,reads;public boolean unavailable;public Runnable duringQuery;public java.util.concurrent.CountDownLatch registerEntered,registerRelease;public volatile android.database.ContentObserver observer;
public android.database.Cursor query(android.net.Uri u,Object a,Object b,Object c,Object d){queries++;if(unavailable)return null;var snapshot=new java.util.HashMap<>(values);if(duringQuery!=null){var run=duringQuery;duringQuery=null;run.run();}return com.sevtinge.hyperceiler.libhook.provider.HomeLayoutPrefsSnapshot.cursor(()->{reads++;return snapshot;});}
public void registerContentObserver(android.net.Uri u,boolean b,android.database.ContentObserver o){if(registerEntered!=null){registerEntered.countDown();try{registerRelease.await();}catch(InterruptedException e){throw new RuntimeException(e);}}observer=o;}public void unregisterContentObserver(android.database.ContentObserver o){observer=null;}
public void change(){if(observer!=null)observer.onChange(false,android.net.Uri.parse("content://com.sevtinge.hyperceiler.provider.sharedprefs/stringset/home_recent_hide_card"));}
}''',
'android/content/Context.java':'''package android.content;public class Context{public final ContentResolver resolver=new ContentResolver();public String getPackageName(){return "com.android.systemui";}public Context getApplicationContext(){return this;}public ContentResolver getContentResolver(){return resolver;}public android.content.pm.PackageManager getPackageManager(){return new android.content.pm.PackageManager();}}''',
'android/app/Application.java':'package android.app;public class Application extends android.content.Context{public static String getProcessName(){return "com.android.systemui";}protected void attach(android.content.Context c){}}',
'com/sevtinge/hyperceiler/common/log/ModuleLog.java':'package com.sevtinge.hyperceiler.common.log;public class ModuleLog{public static int logs;public static void i(String a,String b){logs++;}public static void w(String a,String b){logs++;}}',
'com/sevtinge/hyperceiler/libhook/utils/api/ContextUtils.java':'package com.sevtinge.hyperceiler.libhook.utils.api;public class ContextUtils{public static final int FLAG_CURRENT_APP=0;public static android.content.Context app=new android.app.Application();public static android.content.Context getContextNoError(int v){return app;}}',
'com/sevtinge/hyperceiler/libhook/utils/api/ThreadPoolManager.java':'''package com.sevtinge.hyperceiler.libhook.utils.api;public class ThreadPoolManager{public static java.util.ArrayDeque<Runnable> queue=new java.util.ArrayDeque<>();public static void execute(Runnable r){queue.add(r);}public static void drain(){int count=0;while(!queue.isEmpty()){if(++count>30)throw new AssertionError("unbounded refresh");queue.remove().run();}}}''',
'com/sevtinge/hyperceiler/libhook/base/BaseHook.java':'''package com.sevtinge.hyperceiler.libhook.base;
import java.lang.reflect.*;import java.util.*;import io.github.lingqiqi5211.ezhooktool.xposed.java.IMethodHook;import io.github.libxposed.api.XposedInterface;
public class BaseHook{public static Map<Method,XposedInterface.Hooker> hooks=new HashMap<>();public static XposedInterface.Hooker constructor;public static java.util.List<Runnable> cleanup=new ArrayList<>();public void init(){}public static Class<?> findClass(String s)throws ClassNotFoundException{return Class.forName(s);}public void registerHotReloadCleanup(Runnable r){cleanup.add(r);}public static void hookAllConstructors(Class<?> c,IMethodHook h){constructor=OriginalArgumentsHook.of(h);}public static void hookMethod(Method m,IMethodHook h){chain(m,OriginalArgumentsHook.of(h));}public static void chain(Method m,XposedInterface.Hooker h){if(hooks.putIfAbsent(m,h)!=null)throw new AssertionError("duplicate hook");}
public static void constructed(Object o){try{constructor.intercept(new Invocation(o,null,new Object[0]));}catch(Throwable e){throw new AssertionError(e);}}
public static class Invocation implements XposedInterface.Chain{
final Object o;final Method m;final Object[] args;public Invocation(Object o,Method m,Object[] args){this.o=o;this.m=m;this.args=args;}
public Executable getExecutable(){try{return m==null?Object.class.getConstructor():m;}catch(Exception e){throw new AssertionError(e);}}public Object getThisObject(){return o;}public List<Object> getArgs(){return Arrays.asList(args);}public Object getArg(int i){return args[i];}
public Object proceed()throws Throwable{if(m==null)return null;try{return m.invoke(o,args);}catch(InvocationTargetException e){throw e.getCause();}}
public Object proceed(Object[] v){throw new AssertionError("unchanged arguments resubmitted");}public Object proceedWith(Object t){throw new AssertionError("unchanged receiver resubmitted");}public Object proceedWith(Object t,Object[] v){throw new AssertionError("unchanged boxed primitives resubmitted");}}
public static Object invoke(Object o,String name,Class<?>[] types,Object... args)throws Throwable{Method m=o.getClass().getMethod(name,types);var h=hooks.get(m);var call=new Invocation(o,m,args);return h==null?call.proceed():h.intercept(call);}}
''',
'com/android/wm/shell/recents/IRecentTasks.java':'''package com.android.wm.shell.recents;
public interface IRecentTasks extends android.os.IInterface{
public abstract static class Stub extends android.os.Binder implements IRecentTasks{public abstract Group[] getRecentTasks(int max,int flags,int user);public abstract void registerRecentTasksListener(Listener listener);public abstract void unregisterRecentTasksListener(Listener listener);}
public interface Listener extends android.os.IInterface{}
public static class Group{public java.util.List<android.app.TaskInfo> tasks;public boolean broken;public Group(android.app.TaskInfo... t){tasks=java.util.Arrays.asList(t);}public java.util.List<android.app.TaskInfo> getTaskInfoList(){if(broken)throw new IllegalStateException("OEM getter failed");return tasks;}}}
'''
}
with tempfile.TemporaryDirectory(prefix='hc-hide-recents-')as tmp:
 out=Path(tmp);files=[]
 for name,text in sources.items():
  p=out/name;p.parent.mkdir(parents=True,exist_ok=True);p.write_text(text);files.append(p)
 shared=R/'tests/home-layout-native/stubs'
 files += [shared/p for p in ['android/content/SharedPreferences.java','android/database/Cursor.java','android/database/MatrixCursor.java','android/os/Bundle.java','android/net/Uri.java']]
 lib=R/'library/libhook/src/main/java/com/sevtinge/hyperceiler/libhook'
 files += [lib/'base/OriginalArgumentsHook.java',lib/'provider/HomeLayoutPrefsSnapshot.java',lib/'rules/home/recent/HomeRecentCardsFilter.java',lib/'rules/home/recent/HomeHideRecentCardsOS4.java',H/'Test.java']
 jdk=Path(os.environ.get('JAVA_HOME','/opt/homebrew/opt/openjdk@25/libexec/openjdk.jdk/Contents/Home'))/'bin'
 cache=Path.home()/'.gradle/caches/modules-2/files-2.1'
 jars=[]
 for artifact,version in [('io.github.lingqiqi5211.ezhooktool/hook-xposed-102','1.3.0'),('io.github.libxposed/api','102.0.0'),('org.jetbrains.kotlin/kotlin-stdlib','2.4.10')]:
  jars.append(next((cache/artifact/version).rglob('*.jar')))
 cp=os.pathsep.join(map(str,jars))
 subprocess.run([str(jdk/'javac'),'-cp',cp,'--release','21','-encoding','UTF-8','-d',str(out),*map(str,files)],check=True)
 subprocess.run([str(jdk/'java'),'-ea','-cp',str(out)+os.pathsep+cp,'Test'],check=True)

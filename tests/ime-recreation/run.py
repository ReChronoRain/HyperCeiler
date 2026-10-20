#!/usr/bin/env python3
"""Run production IME support/routing code with hook and Android boundaries replaced."""
from pathlib import Path
import subprocess
import tempfile

ROOT = Path(__file__).resolve().parents[2]
JAVA = ROOT / 'library/libhook/src/main/java'
PKG = 'com/sevtinge/hyperceiler/'
stubs = {
    'androidx/annotation/NonNull.java': 'package androidx.annotation; public @interface NonNull {}',
    'androidx/annotation/Nullable.java': 'package androidx.annotation; public @interface Nullable {}',
    'android/content/Context.java': 'package android.content; public class Context {public static final String INPUT_METHOD_SERVICE="input";public Object getSystemService(String key){return null;}}',
    'android/content/pm/ServiceInfo.java': 'package android.content.pm; public class ServiceInfo {public String packageName;}',
    'android/view/inputmethod/InputMethodInfo.java': 'package android.view.inputmethod;public class InputMethodInfo {public android.content.pm.ServiceInfo getServiceInfo(){return null;}}',
    'android/view/inputmethod/InputMethodManager.java': 'package android.view.inputmethod;public class InputMethodManager {public java.util.List<InputMethodInfo> getEnabledInputMethodList(){return java.util.List.of();}}',
    'com/hchen/database/HookBase.java': 'package com.hchen.database;public @interface HookBase {String targetPackage();}',
    PKG+'common/log/ModuleLog.java': '''package com.sevtinge.hyperceiler.common.log;public class ModuleLog {public static final java.util.List<String> logs=new java.util.ArrayList<>();public static void i(String t,String m){logs.add(m);}public static void w(String t,String m){logs.add(m);}public static void w(String t,String m,Throwable e){logs.add(m);}}''',
    PKG+'common/log/XposedLog.java': 'package com.sevtinge.hyperceiler.common.log;public class XposedLog {public static void e(String t,String m){}public static void d(String t,String m){}}',
    PKG+'common/utils/PrefsBridge.java': '''package com.sevtinge.hyperceiler.common.utils;public class PrefsBridge {public static final java.util.Map<String,Object> values=new java.util.HashMap<>();public static String getString(String k,String d){return (String)values.getOrDefault(k,d);}public static boolean getBoolean(String k){return getBoolean(k,false);}public static boolean getBoolean(String k,boolean d){return (Boolean)values.getOrDefault(k,d);}@SuppressWarnings("unchecked") public static java.util.Set<String> getStringSet(String k){return (java.util.Set<String>)values.getOrDefault(k,java.util.Set.of());}}''',
    'io/github/lingqiqi5211/ezhooktool/xposed/common/HookParam.java': 'package io.github.lingqiqi5211.ezhooktool.xposed.common;public class HookParam {public Object result;public void setResult(Object value){result=value;}}',
    'io/github/lingqiqi5211/ezhooktool/xposed/java/IMethodHook.java': 'package io.github.lingqiqi5211.ezhooktool.xposed.java;public interface IMethodHook {default void before(io.github.lingqiqi5211.ezhooktool.xposed.common.HookParam p){}}',
    PKG+'libhook/base/BaseHook.java': '''package com.sevtinge.hyperceiler.libhook.base;
import java.util.*;import io.github.lingqiqi5211.ezhooktool.xposed.java.IMethodHook;
public class BaseHook {
 public static final Map<String,IMethodHook> hooks=new HashMap<>();public static final List<Runnable> cleanups=new ArrayList<>();
 public static Object getStaticObjectField(Class<?> c,String n){try{var f=c.getDeclaredField(n);f.setAccessible(true);return f.get(null);}catch(Exception e){throw new RuntimeException(e);}}
 public static void setStaticObjectField(Class<?> c,String n,Object v){try{var f=c.getDeclaredField(n);f.setAccessible(true);f.set(null,v);}catch(Exception e){throw new RuntimeException(e);}}
 public static List<IMethodHook> hookAllMethods(Class<?> c,String n,IMethodHook h){boolean found=Arrays.stream(c.getDeclaredMethods()).anyMatch(m->m.getName().equals(n));if(!found)return List.of();hooks.put(c.getName()+"."+n,h);return List.of(h);}
 public static void registerHotReloadCleanup(Runnable r){cleanups.add(r);}public static Class<?> findClass(String n,ClassLoader l){return null;}
}''',
    PKG+'libhook/base/BaseLoad.java': '''package com.sevtinge.hyperceiler.libhook.base;public class BaseLoad {public static String pkg="test.ime";public static final java.util.List<String> loaded=new java.util.ArrayList<>();public void onPackageLoaded(){}public String getPackageName(){return pkg;}public ClassLoader getClassLoader(){return getClass().getClassLoader();}public void initHook(Object h){loaded.add(h.getClass().getSimpleName());}public void initHook(Object h,boolean enabled){if(enabled)initHook(h);}}''',
    PKG+'libhook/utils/hookapi/tool/AppsTool.java': '''package com.sevtinge.hyperceiler.libhook.utils.hookapi.tool;public class AppsTool {public static final int FlAG_ONLY_ANDROID=1;public static int queries;public static android.content.Context findContext(int f){queries++;return null;}}''',
}
for name in ['appbase/input/InputMethodClassLoaderDispatcher', 'rules/various/clipboard/UnlockIme',
             'rules/various/clipboard/BaiduClipboard', 'rules/various/clipboard/SoGouClipboard',
             'rules/various/input/MiAospIme', 'rules/various/MusicHooks']:
    package, cls = name.rsplit('/', 1)
    stubs[PKG+'libhook/'+name+'.java'] = ('package com.sevtinge.hyperceiler.libhook.'
        +package.replace('/', '.')+';public class '+cls+' {public static final '+cls+' INSTANCE=new '+cls+'();}')

with tempfile.TemporaryDirectory(prefix='hc-ime-recreation-') as folder:
    out = Path(folder)
    sources = []
    for name, source in stubs.items():
        path = out / name
        path.parent.mkdir(parents=True, exist_ok=True)
        path.write_text(source)
        sources.append(path)
    sources += [Path(__file__).with_name('RecreationTest.java')]
    sources += [JAVA / (PKG+'libhook/'+name+'.java') for name in [
        'appbase/input/MiuiImeSupport', 'appbase/input/InputMethodConfig', 'app/Others/VariousThirdApps']]
    subprocess.run(['javac', '-encoding', 'UTF-8', '-d', str(out)]+list(map(str,sources)),check=True)
    subprocess.run(['java', '-ea', '-cp', str(out), 'RecreationTest'],check=True)

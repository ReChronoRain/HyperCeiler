#!/usr/bin/env python3
from pathlib import Path
import tempfile,subprocess
R=Path(__file__).resolve().parents[2]
stubs={
'android/os/Build.java':'package android.os;public class Build {public static final String DISPLAY="test";public static class VERSION {public static final int SDK_INT=37;}}',
'android/os/Process.java':'package android.os;public class Process {public static int myPid(){return 123;}}',
'com/sevtinge/hyperceiler/libhook/base/BaseLoad.java':'package com.sevtinge.hyperceiler.libhook.base;public class BaseLoad {public static String getPackageName(){return "com.miui.home";}}',
'com/sevtinge/hyperceiler/libhook/base/BaseHook.java':'package com.sevtinge.hyperceiler.libhook.base;public class BaseHook {public String TAG="Fixture";}',
'com/sevtinge/hyperceiler/common/log/ModuleLog.java':'package com.sevtinge.hyperceiler.common.log;public class ModuleLog {public static java.util.List<String> logs=new java.util.ArrayList<>();public static boolean fail;public static void i(String tag,String line){if(fail)throw new IllegalStateException("logger unavailable");logs.add(line);}}',
'io/github/libxposed/api/XposedInterface.java':'package io.github.libxposed.api;public interface XposedInterface {interface Chain {}interface Hooker {Object intercept(Chain chain)throws Throwable;}}',
'io/github/lingqiqi5211/ezhooktool/xposed/common/HookParam.java':'package io.github.lingqiqi5211.ezhooktool.xposed.common;public class HookParam {public final java.util.Map<String,Object> extras=new java.util.HashMap<>();}',
'io/github/lingqiqi5211/ezhooktool/xposed/java/IMethodHook.java':'package io.github.lingqiqi5211.ezhooktool.xposed.java;import io.github.lingqiqi5211.ezhooktool.xposed.common.HookParam;public interface IMethodHook {default void before(HookParam p){}default void after(HookParam p){}default void setObjectExtra(HookParam p,String k,Object v){p.extras.put(k,v);}default Object getObjectExtra(HookParam p,String k){return p.extras.get(k);}}',
'io/github/lingqiqi5211/ezhooktool/xposed/java/IReplaceHook.java':'package io.github.lingqiqi5211.ezhooktool.xposed.java;import io.github.lingqiqi5211.ezhooktool.xposed.common.HookParam;public interface IReplaceHook {Object replace(HookParam p);}',
}
test=r'''
import com.sevtinge.hyperceiler.libhook.base.*;
import com.sevtinge.hyperceiler.common.diagnostics.*;
import com.sevtinge.hyperceiler.common.log.ModuleLog;
import io.github.libxposed.api.XposedInterface;
import io.github.lingqiqi5211.ezhooktool.xposed.java.*;
import io.github.lingqiqi5211.ezhooktool.xposed.common.HookParam;
public class ObserverTest {
 static DiagnosticEvent last(){var logs=ModuleLog.logs;return DiagnosticEvent.parse(logs.get(logs.size()-1),"logcat",1);}
 public static void main(String[] args)throws Throwable {
  HookDiagnostics.bootstrap("com.android.systemui","/fixture.apk");
  IMethodHook untouched=new IMethodHook(){};
  assert HookDiagnostics.observe(HookDiagnostics.observation("nonhome"),untouched)==untouched;
  assert ModuleLog.logs.isEmpty();
  assert HookDiagnostics.isDesktopProcess("com.miui.home")&&!HookDiagnostics.isDesktopProcess("com.miui.home.fake");
  HookDiagnostics.bootstrap("com.miui.home","/fixture.apk");
  Object handle=new Object();var observation=HookDiagnostics.observation("installed");
  assert HookDiagnostics.install(observation,()->handle)==handle;assert last().phase().equals("INSTALLED");
  int[] before={0},after={0},extras={0};var param=new HookParam();
  IMethodHook original=new IMethodHook(){public void before(HookParam p){assert p==param;before[0]++;}public void after(HookParam p){after[0]++;}public void setObjectExtra(HookParam p,String k,Object v){extras[0]++;p.extras.put(k,v);}};
  var observed=HookDiagnostics.observe(observation,original);
  int count=ModuleLog.logs.size();for(int i=0;i<10000;i++){observed.before(param);observed.after(param);}
  assert before[0]==10000&&after[0]==10000&&ModuleLog.logs.size()==count+1;assert last().phase().equals("EXECUTED");
  observed.setObjectExtra(param,"k",handle);assert observed.getObjectExtra(param,"k")==handle&&extras[0]==1;
  var afterOnly=HookDiagnostics.observe(HookDiagnostics.observation("afterOnly"),new IMethodHook(){public void after(HookParam p){after[0]++;}});
  int onlyCount=ModuleLog.logs.size();afterOnly.before(param);assert ModuleLog.logs.size()==onlyCount;
  afterOnly.after(param);assert ModuleLog.logs.size()==onlyCount+1&&last().phase().equals("EXECUTED");
  var error=new IllegalStateException("secret argument must not be logged");
  var broken=HookDiagnostics.observe(HookDiagnostics.observation("broken"),new IMethodHook(){public void before(HookParam p){throw error;}});
  try{broken.before(param);throw new AssertionError();}catch(IllegalStateException same){assert same==error;}
  assert last().phase().equals("FAILED")&&!last().detail().contains("secret argument");
  var replace=HookDiagnostics.observe(HookDiagnostics.observation("replace"),(IReplaceHook)p->handle);assert replace.replace(param)==handle;
  var nullResult=HookDiagnostics.observe(HookDiagnostics.observation("null"),(IReplaceHook)p->null);assert nullResult.replace(param)==null;
  var chain=HookDiagnostics.observe(HookDiagnostics.observation("chain"),(XposedInterface.Hooker)c->{throw error;});
  try{chain.intercept(null);throw new AssertionError();}catch(IllegalStateException same){assert same==error;}
  assert last().phase().equals("DEGRADED")&&last().code().equals("CALLBACK_EXCEPTION_OBSERVED");
  HookDiagnostics.deferred(true);HookDiagnostics.install(HookDiagnostics.observation("reload"),()->handle);assert last().phase().equals("WAITING");HookDiagnostics.deferred(false);
  ModuleLog.fail=true;assert HookDiagnostics.install(HookDiagnostics.observation("logfail"),()->handle)==handle;replace.replace(param);ModuleLog.fail=false;
  try{HookDiagnostics.install(HookDiagnostics.observation("installfailed"),()->{throw error;});throw new AssertionError();}catch(IllegalStateException same){assert same==error;}
  assert last().phase().equals("FAILED");
  try(var scope=HookDiagnostics.scope(new BaseHook())){assert !HookDiagnostics.owner().equals("java.unattributed");}assert HookDiagnostics.owner().equals("java.unattributed");
  assert ModuleLog.logs.stream().map(s->DiagnosticEvent.parse(s,"logcat",1)).noneMatch(e->e.phase().equals("EFFECT_VERIFIED"));
  System.out.println("Shipping Java observer PASS: handles, 10000 callbacks, before/after/extras, null/identity returns, exact exceptions, original-target exception ambiguity, deferred reload, logger failure isolation, scope cleanup; no fabricated effect");
 }
}
'''
with tempfile.TemporaryDirectory(prefix='hc-hook-observer-')as temp:
 d=Path(temp);sources=[]
 for name,text in stubs.items():
  p=d/name;p.parent.mkdir(parents=True,exist_ok=True);p.write_text(text);sources.append(p)
 p=d/'ObserverTest.java';p.write_text(test);sources.append(p)
 sources+=list((R/'library/common/src/main/java/com/sevtinge/hyperceiler/common/diagnostics').glob('*.java'))
 sources.append(R/'library/libhook/src/main/java/com/sevtinge/hyperceiler/libhook/base/HookDiagnostics.java')
 subprocess.run(['javac','-encoding','UTF-8','-d',temp,*map(str,sources)],check=True)
 subprocess.run(['java','-ea','-cp',temp,'ObserverTest'],check=True)

#!/usr/bin/env python3
# SPDX-License-Identifier: AGPL-3.0-or-later
"""Exercise real entry/PrefsBridge/BaseLoad using API 102 and the real Ez dispatcher."""
import argparse
import hashlib
import os
from pathlib import Path
import subprocess
import tempfile

import sys
sys.path.insert(0, str(Path(__file__).resolve().parents[1]))
import hyperceiler_test_paths as test_paths


ROOT = Path(__file__).resolve().parents[2]
TESTS = Path(__file__).resolve().parent
parser = argparse.ArgumentParser(description=__doc__)
parser.add_argument("--source", type=Path, default=ROOT / "library/libhook/src/main/java/com/sevtinge/hyperceiler/libhook/base/XposedInitEntry.java")
parser.add_argument("--build-cache", type=Path, default=test_paths.BUILD_CACHE)
parser.add_argument("--cases", help="Comma-separated scenario names; default runs the full regression")
args = parser.parse_args()
cache = args.build_cache
jdk = test_paths.java_home(cache) / "bin"
deps = cache / "changed-hooks-compile"
stdlib = next((cache / "gradle-home/caches/modules-2/files-2.1/org.jetbrains.kotlin/kotlin-stdlib/2.4.10").rglob("*.jar"))
jars = [deps / "api-102.0.0.jar", deps / "hook-xposed-102-1.3.0.jar", stdlib,
        test_paths.android_home(cache) / 'platforms/android-37.0/android.jar']

STUBS = {
    "android/content/pm/ApplicationInfo.java": "package android.content.pm; public class ApplicationInfo { public String dataDir, sourceDir; }",
    "android/util/Log.java": "package android.util; public class Log { public static int i(String a,String b){return 0;} public static int w(String a,String b,Throwable t){return 0;} }",
    "com/sevtinge/hyperceiler/common/log/XposedLog.java": """package com.sevtinge.hyperceiler.common.log;
public class XposedLog {
 public static void init(io.github.libxposed.api.XposedInterface x){}
 public static void w(Object... x){} public static void e(Object... x){}
 public static void d(Object... x){} public static void i(Object... x){}
}""",
    "com/sevtinge/hyperceiler/common/log/LogStatusManager.java": """package com.sevtinge.hyperceiler.common.log;
public class LogStatusManager {
 public static int syncs;
 // Same preference read and exception boundary as LogLevelManager/LogStatusManager.
 public static void syncLogLevelFromPrefs(){syncs++; try {com.sevtinge.hyperceiler.common.utils.PrefsBridge.getStringAsInt("log_level_v2",1);}catch(IllegalStateException ignored){}}
 public static void detachHookLogLevelObserver(){}
 public static void attachHookLogLevelObserver(android.content.Context c){}
}""",
    "com/sevtinge/hyperceiler/common/utils/api/ProjectApi.java": "package com.sevtinge.hyperceiler.common.utils.api; public class ProjectApi { public static String mAppModulePkg=\"com.sevtinge.hyperceiler\"; }",
    "com/sevtinge/hyperceiler/libhook/utils/api/ContextUtils.java": """package com.sevtinge.hyperceiler.libhook.utils.api;
public class ContextUtils { public static void getWaitContext(java.util.function.Consumer<android.content.Context> c,boolean system){} }""",
    "com/sevtinge/hyperceiler/libhook/utils/api/ThreadPoolManager.java": """package com.sevtinge.hyperceiler.libhook.utils.api;
public class ThreadPoolManager {
 public static int shutdowns; public static boolean shutdownOk=true;
 public static boolean shutdownAndAwait(long n,java.util.concurrent.TimeUnit u){shutdowns++;return shutdownOk;}
 public static java.util.concurrent.ExecutorService getInstance(){throw new AssertionError("DexKit work is outside this fixture");}
}""",
    "com/sevtinge/hyperceiler/libhook/utils/hookapi/dexkit/DexKit.java": """package com.sevtinge.hyperceiler.libhook.utils.hookapi.dexkit;
public class DexKit { public static void ready(com.sevtinge.hyperceiler.libhook.base.PackageTarget t,String s){} public static void close(){} }""",
    "com/sevtinge/hyperceiler/libhook/base/BaseHook.java": """package com.sevtinge.hyperceiler.libhook.base;
public class BaseHook {
 public String TAG="FixtureHook";
 public boolean useDexKit(){return false;} public boolean initDexKit(){return true;}
 public void setDexKitInitInProgress(boolean b){} public void init(){}
 public static Object snapshotHotReloadRuntimeState(){return new java.util.HashMap<>();}
 public static int cleanups;
 public static void restoreHotReloadRuntimeState(Object state){} public static void prepareHotReload(){cleanups++;}
}""",
    "com/sevtinge/hyperceiler/libhook/base/DataBase.java": """package com.sevtinge.hyperceiler.libhook.base;
public class DataBase { public String targetPackage; public static java.util.HashMap<String,DataBase> get(){return new java.util.HashMap<>();} }""",
    "com/sevtinge/hyperceiler/libhook/base/ModuleMatcher.java": """package com.sevtinge.hyperceiler.libhook.base;
public class ModuleMatcher {
 public ModuleMatcher(MatchContext c){} public boolean shouldLoad(DataBase d,String p){throw new AssertionError("Rule discovery not exercised");}
 public static class MatchContext { public static Builder builder(){return new Builder();}
 public static class Builder { public Builder systemServer(boolean v){return this;} public Builder exactMatch(boolean v){return this;} public Builder debugMode(boolean v){return this;} public MatchContext build(){return new MatchContext();} }}
}""",
    "com/sevtinge/hyperceiler/libhook/app/CorePatch/CorePatch.java": """package com.sevtinge.hyperceiler.libhook.app.CorePatch;
public class CorePatch { public void onLoad(com.sevtinge.hyperceiler.libhook.base.PackageTarget t){throw new AssertionError("Unrelated core patch must stay disabled");} }""",
    "com/sevtinge/hyperceiler/libhook/rules/systemframework/others/FlagSecure.java": """package com.sevtinge.hyperceiler.libhook.rules.systemframework.others;
public class FlagSecure { public void onLoad(com.sevtinge.hyperceiler.libhook.base.PackageTarget t){throw new AssertionError("Unrelated flag hook must stay disabled");} }""",
    "com/sevtinge/hyperceiler/libhook/safecrash/CrashMonitor.java": """package com.sevtinge.hyperceiler.libhook.safecrash;
public class CrashMonitor { public static int instances; public CrashMonitor(com.sevtinge.hyperceiler.libhook.base.PackageTarget t){instances++;} }""",
    "com/sevtinge/hyperceiler/libhook/rules/home/other/NativeHomeHooksOS4.java": """package com.sevtinge.hyperceiler.libhook.rules.home.other;
public class NativeHomeHooksOS4 { public static final NativeHomeHooksOS4 INSTANCE=new NativeHomeHooksOS4(); public static int recoveries; public static boolean bootstrapReady=true; public void ensureLoadedFromModuleEntry(String p){} public void ensureLoadedFromModuleEntry(String p,boolean ready){bootstrapReady=ready;} public void onPreferencesReadyAfterRetry(){recoveries++;} }""",
    "com/sevtinge/hyperceiler/libhook/rules/home/dock/HomeLayoutNativeEndpointOS4.java": """package com.sevtinge.hyperceiler.libhook.rules.home.dock;
public class HomeLayoutNativeEndpointOS4 { public static boolean pauseForHotReload(){return true;} public static void resumeAfterRejectedHotReload(){} }""",
    "com/sevtinge/hyperceiler/libhook/rules/systemui/controlcenter/media3/AmbientNativeColorOS4.java": """package com.sevtinge.hyperceiler.libhook.rules.systemui.controlcenter.media3;
/** JNI backend state is a fixture here; actual Kotlin/ART evidence is separate. */
public class AmbientNativeColorOS4 {
 public static boolean loaded,paused; public static int pauses,resumes;
 public static boolean pauseForHotReload(){pauses++;if(loaded)return false;paused=true;return true;}
 public static void resumeAfterRejectedHotReload(){resumes++;paused=false;}
}""",
    "io/github/lingqiqi5211/ezhooktool/xposed/EzResources.java": "package io.github.lingqiqi5211.ezhooktool.xposed; public class EzResources { public static void inject(android.content.Context c){} }",
    "io/github/lingqiqi5211/ezhooktool/xposed/EzXposed.java": """package io.github.lingqiqi5211.ezhooktool.xposed;
import android.content.Context;
import io.github.libxposed.api.*;
import io.github.libxposed.api.XposedModuleInterface.*;
import kotlin.Unit;
/** Android lifecycle adapter only; dispatcher itself is the unmodified dependency class. */
public class EzXposed {
 public static final TargetReadyDispatcher dispatcher=new TargetReadyDispatcher();
 public static int callbacksRegistered, publishedReloads, savedReloads;
 public static boolean acceptSave=true, throwSave=false;
 private static Object currentEntry;
 private static String processName, packageName;
 private static boolean system;
 public static void initOnModuleLoaded(XposedInterface entry,ModuleLoadedParam p){
  if(currentEntry!=entry){dispatcher.reset();callbacksRegistered=0;currentEntry=entry;}
  processName=p.getProcessName();system=p.isSystemServer();
 }
 public static void onTargetReady(TargetReadyCallback callback){callbacksRegistered++;if(dispatcher.register(callback))callback.run();}
 private static void dispatch(boolean strict){if(dispatcher.start(()->Unit.INSTANCE))dispatcher.run(strict,null,t->Unit.INSTANCE);}
 public static void initOnPackageReady(PackageReadyParam p){packageName=p.getPackageName();dispatch(false);}
 public static void initOnSystemServerStarting(SystemServerStartingParam p){packageName="system";dispatch(false);}
 public static AutomaticHotReloadResult restoreHotReloadedAutomatically(XposedInterface e,HotReloadedParam p,java.util.function.Consumer<Object[]> restore){
  packageName=system?"system":"com.android.systemui";
  restore.accept(new Object[]{"HyperCeiler.HotReloadExtras",1,system?null:new android.content.pm.ApplicationInfo(),null,new java.util.HashMap<>()});
  dispatch(true);publishedReloads++;
  return new AutomaticHotReloadResult(0,0,0);
 }
 public static boolean handleHotReloading(HotReloadingParam p,Object[] extras){savedReloads++;if(throwSave)throw new IllegalStateException("save failed");return acceptSave;}
 public static String getProcessName(){return processName;} public static String getPackageName(){return packageName;}
 public static ClassLoader getClassLoader(){return EzXposed.class.getClassLoader();}
 public static boolean isSystemServer(){return system;}
 public static Context getAppContextOrNull(){return null;}
 public static void initAppContext(Context c,boolean a,boolean b){}
}""",
}

with tempfile.TemporaryDirectory(prefix="hc-init-prefs-") as work:
    work = Path(work)
    generated = []
    for name, text in STUBS.items():
        path = work / name
        path.parent.mkdir(parents=True, exist_ok=True)
        path.write_text(text)
        generated.append(path)
    common = ROOT / "library/common/src/main/java/com/sevtinge/hyperceiler/common"
    base = ROOT / "library/libhook/src/main/java/com/sevtinge/hyperceiler/libhook/base"
    sources = generated + list((ROOT / "tests/dock-style-refresh/stubs").rglob("*.java")) + [
        args.source, base / "BaseLoad.java", base / "PackageTarget.java", base / "FrameworkHookGate.java",
        common / "utils/PrefsBridge.java", common / "utils/prefs/PrefType.java", TESTS / "InitPrefsTest.java"]
    cp = os.pathsep.join(str(x) for x in jars)
    subprocess.run([str(jdk / "javac"), "-encoding", "UTF-8", "-cp", cp, "-d", str(work)] +
                   [str(x) for x in sources], check=True)
    failures = 0
    cases = ["package_transient", "system_transient", "healthy_idempotent", "secondary_package",
                 "persistent_failure", "allow_hook_gate", "framework_gate", "hot_reload_transient",
                 "hot_reload_persistent", "null_remote", "concurrent_init", "empty_success", "rule_failure",
                 "home_recovered", "home_gate", "home_framework_gate", "home_healthy", "home_bound_read_failure", "framework_changed_name", "framework_changed_version", "framework_changed_code", "framework_missing_reason", "framework_runtime_unavailable", "framework_whitespace_same", "framework_global_disabled", "framework_allowed_no_identity", "framework_missing_checked_fields", "ambient_loaded_reload", "ambient_unloaded_reload", "ambient_rule_failure_reload", "ambient_save_denied_reload", "ambient_save_throws_reload", "ambient_worker_failure_reload", "ambient_other_package_reload", "ambient_system_server_reload"]
    if args.cases:
        cases = args.cases.split(",")
    for case in cases:
        result = subprocess.run([str(jdk / "java"), "-ea", "-cp", str(work) + os.pathsep + cp,
                                 "InitPrefsTest", case])
        failures += result.returncode != 0
    print(f"Init preference lifecycle: {len(cases)-failures}/{len(cases)} PASS")
    for path in [args.source, base / "BaseLoad.java", common / "utils/PrefsBridge.java", *jars[:3]]:
        print(hashlib.sha256(path.read_bytes()).hexdigest(), path)
    raise SystemExit(bool(failures))

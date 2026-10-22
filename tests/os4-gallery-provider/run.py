#!/usr/bin/env python3
"""Exercise actual production Gallery provider hook and MediaEditor registration on host fixtures."""

from pathlib import Path
import argparse
import os
import subprocess  # nosec B404
import tempfile
import shutil

ROOT = Path(__file__).resolve().parents[2]
PACKAGE = "com/sevtinge/hyperceiler/libhook/"
p = argparse.ArgumentParser(description=__doc__)
p.add_argument("--source-root", type=Path, default=ROOT)
a = p.parse_args()
STUBS = {
    "android/content/ContentProvider.java": """package android.content;
public class ContentProvider { public String caller="com.miui.gallery"; public boolean failCaller;
public String getCallingPackage(){if(failCaller)throw new SecurityException("caller mismatch");return caller;} }""",
    "android/os/Bundle.java": """package android.os;
import java.util.HashMap;
public class Bundle { public final HashMap<String,Object> values=new HashMap<>();public boolean failGet,failCopy;
public Bundle(){} public Bundle(Bundle other){if(other.failCopy)throw new IllegalStateException("bad parcel"); values.putAll(other.values);}
public Object get(String key){if(failGet)throw new IllegalStateException("bad parcel");return values.get(key);}
public void putBoolean(String key,boolean value){values.put(key,value);} }""",
    "android/util/Log.java": """package android.util;
public class Log {public static int warnings; public static int w(String t,String m,Throwable e){warnings++;return 0;} }""",
    "com/hchen/database/HookBase.java": """package com.hchen.database;
public @interface HookBase {String targetPackage();}""",
    "com/sevtinge/hyperceiler/common/utils/PrefsBridge.java": """package com.sevtinge.hyperceiler.common.utils;
import java.util.HashSet;
public class PrefsBridge {public static final HashSet<String> enabled=new HashSet<>();public static boolean fail;public static int reads;
public static boolean getBoolean(String key){reads++;if(fail)throw new IllegalStateException("preferences unavailable");return enabled.contains(key);}
public static String getString(String key,String fallback){return fallback;} }""",
    PACKAGE
    + "utils/api/DeviceHelper.java": """package com.sevtinge.hyperceiler.libhook.utils.api;
public class DeviceHelper {public static class System {public static float version=4f;
public static boolean isMoreHyperOSVersion(float v){return version>=v;}}}""",
    "io/github/lingqiqi5211/ezhooktool/xposed/common/HookParam.java": """package io.github.lingqiqi5211.ezhooktool.xposed.common;
public class HookParam {public Object target,result;public Object[] args;public Throwable throwable;public boolean skipped; public int resultSets;
public HookParam(Object target,Object result,Object... args){this.target=target;this.result=result;this.args=args;}
public Object getThisObject(){return target;}public Object[] getArgs(){return args;}public Object getResult(){return result;}
public void setResult(Object result){this.result=result;resultSets++;}
public boolean getHasThrowable(){return throwable!=null;}public boolean isSkipped(){return skipped;} }""",
    "io/github/lingqiqi5211/ezhooktool/xposed/java/IMethodHook.java": """package io.github.lingqiqi5211.ezhooktool.xposed.java;
import io.github.lingqiqi5211.ezhooktool.xposed.common.HookParam;
public interface IMethodHook {default void before(HookParam p){}default void after(HookParam p){}}""",
    PACKAGE + "base/BaseHook.java": """package com.sevtinge.hyperceiler.libhook.base;
import io.github.lingqiqi5211.ezhooktool.xposed.java.IMethodHook;
public abstract class BaseHook {public static IMethodHook registered; public abstract void init();
public static void findAndHookMethod(String cls,String method,Object... params){
if(!cls.equals("com.miui.mediaeditor.provider.MediaEditorProviderForGallery")||!method.equals("call")
||params.length!=4||params[0]!=String.class||params[1]!=String.class||params[2]!=android.os.Bundle.class)
throw new AssertionError("unexpected lookup");
registered=(IMethodHook)params[3];} }""",
    PACKAGE
    + "base/BaseLoad.java": """package com.sevtinge.hyperceiler.libhook.base;
import java.util.ArrayList;
public abstract class BaseLoad {public static final ArrayList<String> installed=new ArrayList<>();public abstract void onPackageLoaded();
public static void initHook(BaseHook rule,boolean enabled){if(enabled){installed.add(rule.getClass().getSimpleName());rule.init();}} }""",
}
for name in [
    "CustomWatermark",
    "UnlockAigc",
    "UnlockCustomPhotoFrames",
    "UnlockDisney",
    "UnlockLeicaFilter",
    "UnlockMinimumCropLimit2",
]:
    STUBS[PACKAGE + "rules/mediaeditor/" + name + ".java"] = (
        "package com.sevtinge.hyperceiler.libhook.rules.mediaeditor; public class "
        + name
        + " extends com.sevtinge.hyperceiler.libhook.base.BaseHook { public static final "
        + name
        + " INSTANCE=new "
        + name
        + "(); public void init(){} }"
    )
java_home = (
    Path(os.environ["JAVA_HOME"])
    if os.environ.get("JAVA_HOME")
    else Path(shutil.which("javac") or "javac").resolve().parents[1]
)
with tempfile.TemporaryDirectory(prefix="hc-gallery-provider-") as temp:
    work = Path(temp)
    for rel, code in STUBS.items():
        f = work / rel
        f.parent.mkdir(parents=True, exist_ok=True)
        f.write_text(code)
    for rel in [
        PACKAGE + "app/MediaEditor.java",
        PACKAGE + "rules/mediaeditor/GalleryCreationAvailabilityOS4.java",
    ]:
        f = work / rel
        f.parent.mkdir(parents=True, exist_ok=True)
        f.write_text(
            (a.source_root / "library/libhook/src/main/java" / rel).read_text()
        )
    (work / "TestMain.java").write_text(
        (Path(__file__).parent / "TestMain.java").read_text()
    )
    # Developer-selected tools/fixtures only; argv is separate and no shell is used.
    # nosemgrep: python_exec_rule-subprocess-call, python_exec_rule-subprocess-call-array
    subprocess.run(  # nosec B603
        [
            str(java_home / "bin/javac"),
            "--release",
            "21",
            "-encoding",
            "UTF-8",
            "-d",
            str(work / "classes"),
            *map(str, work.rglob("*.java")),
        ],
        check=True,
    )
    # Developer-selected tools/fixtures only; argv is separate and no shell is used.
    raise SystemExit(
        # nosemgrep: python_exec_rule-subprocess-call, python_exec_rule-subprocess-call-array
        subprocess.run(  # nosec B603
            [
                str(java_home / "bin/java"),
                "-cp",
                str(work / "classes"),
                "fixture.TestMain",
            ]
        ).returncode
    )

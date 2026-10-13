#!/usr/bin/env python3
"""Compile complete production framework-status writer and bridge; Android/service fixtures."""
from pathlib import Path
import argparse, subprocess, tempfile, json, hashlib, re

import sys
sys.path.insert(0, str(Path(__file__).resolve().parents[1]))
import hyperceiler_test_paths as test_paths

p=argparse.ArgumentParser();p.add_argument('--output',required=True,type=Path);p.add_argument('--manager-source',type=Path);a=p.parse_args()
b=Path(__file__).resolve().parent;r=b.parents[1];a.output.mkdir(parents=True,exist_ok=True)
j=test_paths.JAVA_HOME / 'bin'
manager=a.manager_source or r/'app/src/main/java/com/sevtinge/hyperceiler/utils/FrameworkStatusManager.java'
prod=[manager,r/'library/common/src/main/java/com/sevtinge/hyperceiler/common/utils/PrefsBridge.java',r/'library/common/src/main/java/com/sevtinge/hyperceiler/common/utils/prefs/PrefType.java',r/'library/libhook/src/main/java/com/sevtinge/hyperceiler/libhook/base/FrameworkHookGate.java']
stubs=[x for x in (r/'tests/dock-style-refresh/stubs').rglob('*.java') if x.name not in ('Context.java','ContentResolver.java','AndroidLog.java')]
gen={
'android/annotation/SuppressLint.java':'package android.annotation; public @interface SuppressLint { String[] value(); }',
'android/content/Context.java':'''package android.content; public class Context { public static final int MODE_PRIVATE=0; public final SharedPreferences prefs; public final ContentResolver resolver=new ContentResolver(); public Context(SharedPreferences p){prefs=p;} public Context createDeviceProtectedStorageContext(){return this;} public SharedPreferences getSharedPreferences(String n,int m){return prefs;} public ContentResolver getContentResolver(){return resolver;} public String getString(int id,Object...v){return "resource";} }''',
'android/content/ContentResolver.java':'''package android.content; import android.net.Uri; import java.util.*; public class ContentResolver { public final List<String> events=new ArrayList<>(); public Runnable observer=()->{}; public void notifyChange(Uri u,Object o){events.add(u.toString());observer.run();} }''',
'com/sevtinge/hyperceiler/common/log/AndroidLog.java':'package com.sevtinge.hyperceiler.common.log; public class AndroidLog { public static void w(Object...v){} public static void e(Object...v){} }',
'io/github/libxposed/service/XposedService.java':'''package io.github.libxposed.service; public interface XposedService { String getFrameworkName(); String getFrameworkVersion(); long getFrameworkVersionCode(); int getApiVersion(); }''',
'com/sevtinge/hyperceiler/R.java':'package com.sevtinge.hyperceiler; public final class R { public static final class string {'+''.join('public static final int '+v+'='+str(i)+';' for i,v in enumerate(sorted(set(re.findall(r'R.string.(\w+)',manager.read_text())))))+'} }'}
with tempfile.TemporaryDirectory(prefix='hc-framework-status-') as t:
 t=Path(t)
 for f,s in gen.items():
  x=t/f;x.parent.mkdir(parents=True,exist_ok=True);x.write_text(s);stubs.append(x)
 cmd=[str(j/'javac'),'--release','21','-encoding','UTF-8','-d',str(t),*map(str,stubs+prod+[b/'FrameworkStatusTransactionTest.java'])]
 c=subprocess.run(cmd,text=True,stdout=subprocess.PIPE,stderr=subprocess.STDOUT);(a.output/'build.txt').write_text(c.stdout)
 if c.returncode:print(c.stdout);raise SystemExit(c.returncode)
 z=subprocess.run([str(j/'java'),'-ea','-cp',str(t),'FrameworkStatusTransactionTest'],text=True,stdout=subprocess.PIPE,stderr=subprocess.STDOUT,timeout=60)
 (a.output/'run.txt').write_text(z.stdout);print(z.stdout,end='')
 (a.output/'report.json').write_text(json.dumps({'exit_code':z.returncode,'sources':{str(f):hashlib.sha256(f.read_bytes()).hexdigest() for f in prod},'build':cmd,'boundary':'Actual full production FrameworkStatusManager/PrefsBridge/FrameworkHookGate with deterministic SharedPreferences editor, service and Android adapters. No Android Binder, disk durability, ART injection or phone evidence.'},indent=2)+'\n')
 raise SystemExit(z.returncode)

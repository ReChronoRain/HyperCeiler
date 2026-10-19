#!/usr/bin/env python3
"""Compile full production HCS1 endpoint and Kotlin WMS route with real SDK/dependencies."""
import argparse, hashlib, json, subprocess
from pathlib import Path

import sys
sys.path.insert(0, str(Path(__file__).resolve().parents[1]))
import hyperceiler_test_paths as test_paths

p=argparse.ArgumentParser();p.add_argument('--output',type=Path,required=True);p.add_argument('--classpath',required=True,help='Current machine classpath for SDK37, project peers and Xposed dependencies');a=p.parse_args()
root=Path(__file__).resolve().parents[2];project=test_paths.TEST_DATA_ROOT
cp=a.classpath
a.output.mkdir(parents=True,exist_ok=True);out=a.output/'classes';out.mkdir(exist_ok=True)
lib=root/'library/libhook/src/main/java/com/sevtinge/hyperceiler/libhook'
sources=[lib/x for x in ['base/FrameworkHookGate.java','provider/HomeLayoutPrefsSnapshot.java','rules/home/dock/HomeLayoutNativeEndpointOS4.java','rules/home/dock/HomeDockWindow.kt']]
jdk=test_paths.JAVA_HOME / 'bin';kh=test_paths.KOTLIN_HOME
friends=test_paths.SNAPSHOT_ROOT / 'source-os4-hyos-test4-20261009/library/libhook/build/intermediates/built_in_kotlinc/debug/compileDebugKotlin/classes'
commands=[
 [str(jdk/'javac'),'-encoding','UTF-8','--release','21','-cp',cp,'-d',str(out),*map(str,sources[:-1])],
 [str(jdk/'java'),'-cp',str(kh/'lib/kotlin-compiler.jar'),'org.jetbrains.kotlin.cli.jvm.K2JVMCompiler','-kotlin-home',str(kh),'-jvm-target','25','-Xfriend-paths='+str(friends),'-classpath',str(out)+':'+cp,'-d',str(out),str(sources[-1])]]
results=[]
for i,cmd in enumerate(commands):
 z=subprocess.run(cmd,text=True,stdout=subprocess.PIPE,stderr=subprocess.STDOUT,timeout=180);(a.output/f'compile-{i}.txt').write_text(z.stdout);print(z.stdout,end='');results.append({'exit_code':z.returncode,'command':cmd})
 if z.returncode:break
(a.output/'report.json').write_text(json.dumps({'passed':len(results)==2 and all(x['exit_code']==0 for x in results),'commands':results,'production_sources':{str(s):hashlib.sha256(s.read_bytes()).hexdigest() for s in sources},'limits':'Actual SDK37/API102/EzHook1.3.0/Kotlin2.4.10 and frozen test4 module classes; no Android lifecycle/Binder execution.'},indent=2)+'\n')
raise SystemExit(not(len(results)==2 and all(x['exit_code']==0 for x in results)))

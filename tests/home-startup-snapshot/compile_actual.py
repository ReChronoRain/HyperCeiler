#!/usr/bin/env python3
"""Compile full production HCS1 endpoint and Kotlin WMS route with real SDK/dependencies."""
import argparse, hashlib, json, subprocess
from pathlib import Path
p=argparse.ArgumentParser();p.add_argument('--output',type=Path,required=True);a=p.parse_args()
root=Path(__file__).resolve().parents[2];project=root.parent
baseline=project/'acceptance/java-next/framework-gate/actual-dependency-compile-v3/report.json'
base=json.loads(baseline.read_text());old=base['command'];cp=old[old.index('-cp')+1].replace('source-os4-recovery-20261009','source-os4-hyos-test4-20261009')
a.output.mkdir(parents=True,exist_ok=True);out=a.output/'classes';out.mkdir(exist_ok=True)
lib=root/'library/libhook/src/main/java/com/sevtinge/hyperceiler/libhook'
sources=[lib/x for x in ['base/FrameworkHookGate.java','provider/HomeLayoutPrefsSnapshot.java','rules/home/dock/HomeLayoutNativeEndpointOS4.java','rules/home/dock/HomeDockWindow.kt']]
jdk=Path('/home/hgn284/.cache/hyperceiler-build/jdk-25.0.4.1/bin');kh=project/'tools-cache/kotlin-2.4.10/kotlinc'
friends=Path('/home/hgn284/.cache/hyperceiler-build/source-os4-hyos-test4-20261009/library/libhook/build/intermediates/built_in_kotlinc/debug/compileDebugKotlin/classes')
commands=[
 [str(jdk/'javac'),'-encoding','UTF-8','--release','21','-cp',cp,'-d',str(out),*map(str,sources[:-1])],
 [str(jdk/'java'),'-cp',str(kh/'lib/kotlin-compiler.jar'),'org.jetbrains.kotlin.cli.jvm.K2JVMCompiler','-kotlin-home',str(kh),'-jvm-target','25','-Xfriend-paths='+str(friends),'-classpath',str(out)+':'+cp,'-d',str(out),str(sources[-1])]]
results=[]
for i,cmd in enumerate(commands):
 z=subprocess.run(cmd,text=True,stdout=subprocess.PIPE,stderr=subprocess.STDOUT,timeout=180);(a.output/f'compile-{i}.txt').write_text(z.stdout);print(z.stdout,end='');results.append({'exit_code':z.returncode,'command':cmd})
 if z.returncode:break
(a.output/'report.json').write_text(json.dumps({'passed':len(results)==2 and all(x['exit_code']==0 for x in results),'commands':results,'production_sources':{str(s):hashlib.sha256(s.read_bytes()).hexdigest() for s in sources},'limits':'Actual SDK37/API102/EzHook1.3.0/Kotlin2.4.10 and frozen test4 module classes; no Android lifecycle/Binder execution.'},indent=2)+'\n')
raise SystemExit(not(len(results)==2 and all(x['exit_code']==0 for x in results)))

#!/usr/bin/env python3
"""Compile complete changed Java files against SDK37 and real project/Xposed dependencies."""
from pathlib import Path
import argparse,hashlib,json,os,subprocess

import sys
sys.path.insert(0, str(Path(__file__).resolve().parents[1]))
import hyperceiler_test_paths as test_paths

p=argparse.ArgumentParser();p.add_argument('--output',type=Path,required=True);p.add_argument('--build-cache',type=Path,default=test_paths.BUILD_CACHE);a=p.parse_args();a.output.mkdir(parents=True,exist_ok=True)
b=Path(__file__).resolve().parents[2];root=b/'library/libhook/src/main/java/com/sevtinge/hyperceiler/libhook';cache=a.build_cache;snapshot=test_paths.snapshot_root(cache) / 'source-os4-hyos-test5-20261009-r3';j=test_paths.java_home(cache)/'bin'
cp=[snapshot/f'library/{m}/build/intermediates/javac/debug/compileDebugJavaWithJavac/classes' for m in ['common','libhook','hidden-api']]+[test_paths.android_home(cache) / 'platforms/android-37.0/android.jar']+[cache/'changed-hooks-compile'/f for f in ['hook-xposed-102-1.3.0.jar','core-1.3.0.jar','api-102.0.0.jar']]+list((cache/'gradle-home/caches/modules-2/files-2.1/org.jetbrains.kotlin/kotlin-stdlib/2.4.10').rglob('*.jar'))+[snapshot/f'library/{m}/build/intermediates/built_in_kotlinc/debug/compileDebugKotlin/classes' for m in ['common','libhook']]+[snapshot/'library/processor/build/classes/java/main',snapshot/'app/build/intermediates/compile_r_class_jar/debug/generateDebugRFile/R.jar',snapshot/'app/build/intermediates/javac/debug/compileDebugJavaWithJavac/classes']+list((cache/'gradle-home/caches').glob('*/transforms/*/transformed/service-102.0.0-api.jar'))+list((cache/'gradle-home/caches/modules-2/files-2.1/androidx.annotation/annotation-jvm/1.10.0').rglob('*.jar'))
assert all(x.exists() for x in cp),[str(x)for x in cp if not x.exists()]
files=[root/f for f in ['base/FrameworkHookGate.java','provider/WeatherPrefsSnapshot.java','provider/SharedPrefsProvider.java','rules/weather/WeatherNativeEndpointOS4.java','rules/weather/WeatherNativeBridgeOS4.java','app/SystemFramework/SystemFrameworkB.java']]
cmd=[str(j/'javac'),'--release','21','-encoding','UTF-8','-proc:none','-cp',os.pathsep.join(map(str,cp)),'-d',str(a.output/'classes'),*map(str,files)];z=subprocess.run(cmd,text=True,stdout=subprocess.PIPE,stderr=subprocess.STDOUT);(a.output/'compile.txt').write_text(z.stdout);print(z.stdout,end='')
dis=[str(j/'javap'),'-c','-p','-classpath',str(a.output/'classes'),'com.sevtinge.hyperceiler.libhook.app.SystemFramework.SystemFrameworkB'];d=subprocess.run(dis,text=True,stdout=subprocess.PIPE,stderr=subprocess.STDOUT)if z.returncode==0 else None
if d:(a.output/'SystemFrameworkB.javap.txt').write_text(d.stdout)
(a.output/'report.json').write_text(json.dumps({'passed':z.returncode==0 and d.returncode==0,'exit_code':z.returncode,'command':cmd,'route_disassembly_command':dis,'production_inputs':{str(f):hashlib.sha256(f.read_bytes()).hexdigest() for f in files},'dependencies':{str(f):hashlib.sha256(f.read_bytes()).hexdigest() if f.is_file() else 'compiled project directory' for f in cp},'limits':'SDK37 / real API102+EzHook1.3.0 / frozen test5 peer classes / project hidden API signatures; actual source compile only, not ART/LSPosed installation or phone test'},indent=2)+'\n');raise SystemExit(z.returncode)

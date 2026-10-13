#!/usr/bin/env python3
"""Compile both whole production files with SDK37, API102 and frozen peer classes."""
from pathlib import Path
import argparse,hashlib,json,os,subprocess

import sys
sys.path.insert(0, str(Path(__file__).resolve().parents[1]))
import hyperceiler_test_paths as test_paths

p=argparse.ArgumentParser();p.add_argument('--output',type=Path,required=True);a=p.parse_args();a.output.mkdir(parents=True,exist_ok=True)
b=Path(__file__).resolve().parents[2];root=b/'library/libhook/src/main/java/com/sevtinge/hyperceiler/libhook';cache=test_paths.BUILD_CACHE;snapshot=test_paths.SNAPSHOT_ROOT / 'source-os4-hyos-test7-20261009-r1';j=test_paths.JAVA_HOME / 'bin'
cp=[snapshot/f'library/{m}/build/intermediates/javac/debug/compileDebugJavaWithJavac/classes' for m in ['common','libhook','hidden-api']]+[test_paths.ANDROID_HOME / 'platforms/android-37.0/android.jar']+[cache/'changed-hooks-compile'/f for f in ['hook-xposed-102-1.3.0.jar','core-1.3.0.jar','api-102.0.0.jar']]+list((cache/'gradle-home/caches/modules-2/files-2.1/org.jetbrains.kotlin/kotlin-stdlib/2.4.10').rglob('*.jar'))+[snapshot/f'library/{m}/build/intermediates/built_in_kotlinc/debug/compileDebugKotlin/classes' for m in ['common','libhook']]+[snapshot/'library/processor/build/classes/java/main',snapshot/'app/build/intermediates/compile_r_class_jar/debug/generateDebugRFile/R.jar',snapshot/'app/build/intermediates/javac/debug/compileDebugJavaWithJavac/classes']+list((cache/'gradle-home/caches').glob('*/transforms/*/transformed/service-102.0.0-api.jar'))+list((cache/'gradle-home/caches/modules-2/files-2.1/androidx.annotation/annotation-jvm/1.10.0').rglob('*.jar'))
assert all(x.exists() for x in cp),[str(x)for x in cp if not x.exists()]
files=[root/f for f in ['rules/systemui/statusbar/icon/all/DataSaverIconOS4.java','app/SystemUI/SystemUIB.java']]
cmd=[str(j/'javac'),'--release','21','-encoding','UTF-8','-proc:none','-cp',os.pathsep.join(map(str,cp)),'-d',str(a.output/'classes'),*map(str,files)];z=subprocess.run(cmd,text=True,stdout=subprocess.PIPE,stderr=subprocess.STDOUT);(a.output/'compile.txt').write_text(z.stdout);print(z.stdout,end='')
checks=[]
for name in ['com.sevtinge.hyperceiler.libhook.rules.systemui.statusbar.icon.all.DataSaverIconOS4','com.sevtinge.hyperceiler.libhook.app.SystemUI.SystemUIB']:
 d=subprocess.run([str(j/'javap'),'-c','-p','-classpath',str(a.output/'classes'),name],text=True,stdout=subprocess.PIPE,stderr=subprocess.STDOUT)if z.returncode==0 else None
 if d:(a.output/(name.rsplit('.',1)[-1]+'.javap.txt')).write_text(d.stdout);checks.append(d.returncode==0)
sha=lambda p:hashlib.sha256(p.read_bytes()).hexdigest()
report={'passed':z.returncode==0 and len(checks)==2 and all(checks),'exit_code':z.returncode,'command':cmd,'production_inputs':{str(f.relative_to(b.parent)):sha(f) for f in files},'dependencies':{str(f):sha(f) if f.is_file() else 'compiled project directory' for f in cp},'limits':'Whole production class compilation against SDK37, real API102/EzHook1.3.0 and frozen test7 peer bytecode. Not APK/D8/ART or device evidence.'};(a.output/'report.json').write_text(json.dumps(report,indent=2)+'\n');raise SystemExit(0 if report['passed']else 1)

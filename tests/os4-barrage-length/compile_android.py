#!/usr/bin/env python3
"""Compile whole current Barrage Kotlin + Java route against actual SDK37 and frozen test10 peers."""
from pathlib import Path
import argparse,subprocess,json,hashlib,os
p=argparse.ArgumentParser();p.add_argument('--output',required=True,type=Path);a=p.parse_args();o=a.output;o.mkdir(parents=True,exist_ok=False)
r=Path(__file__).resolve().parents[3];c=Path('/home/hgn284/.cache/hyperceiler-build');s=c/'source-os4-hyos-test10-20261009-r1'
cp=[s/f'library/{m}/build/intermediates/javac/debug/compileDebugJavaWithJavac/classes'for m in['common','libhook','hidden-api']]+[s/f'library/{m}/build/intermediates/built_in_kotlinc/debug/compileDebugKotlin/classes'for m in['common','libhook']]+[c/'android-sdk/platforms/android-37.0/android.jar']+[c/'changed-hooks-compile'/f for f in['hook-xposed-102-1.3.0.jar','core-1.3.0.jar','api-102.0.0.jar']]+list((c/'gradle-home/caches').glob('*/transforms/*/transformed/core-ktx-*/jars/classes.jar'))+list((c/'gradle-home/caches').glob('*/transforms/*/transformed/core-ktx-*-api.jar'))
cp += list((c/'gradle-home/caches').glob('*/transforms/*/transformed/dexkit-*/jars/classes.jar'))
cp += [s/'library/processor/build/classes/java/main']
assert all(p.exists()for p in cp)
kh=r/'tools-cache/kotlin-2.4.10/kotlinc';src=r/'source/library/libhook/src/main/java/com/sevtinge/hyperceiler/libhook/rules/barrage/CustomBarrageLength.kt';sha=lambda p:hashlib.sha256(p.read_bytes()).hexdigest();route=r/'source/library/libhook/src/main/java/com/sevtinge/hyperceiler/libhook/app/Barrage.java';before={str(p.relative_to(r)):sha(p)for p in[src,route]}
cp += [kh/'lib/kotlin-stdlib.jar']
cmd=[str(c/'jdk-25.0.4.1/bin/java'),'-cp',str(kh/'lib/kotlin-compiler.jar'),'org.jetbrains.kotlin.cli.jvm.K2JVMCompiler','-kotlin-home',str(kh),'-jvm-target','21','-classpath',os.pathsep.join(map(str,cp)),'-d',str(o/'classes'),str(src)];z=subprocess.run(cmd,capture_output=True,text=True);(o/'compile.txt').write_text(z.stdout+z.stderr)
commands=[cmd]
if z.returncode==0:
 cmd=[str(c/'jdk-25.0.4.1/bin/javac'),'--release','21','-proc:none','-classpath',os.pathsep.join(map(str,[o/'classes',*cp])),'-d',str(o/'classes'),str(route)];commands.append(cmd);z=subprocess.run(cmd,capture_output=True,text=True);(o/'route-compile.txt').write_text(z.stdout+z.stderr)
after={str(p.relative_to(r)):sha(p)for p in[src,route]}
report={'passed':z.returncode==0 and before==after,'exit_code':z.returncode,'commands':commands,'source':after,'inputs_before':before,'source_unchanged':before==after,'runner':{str(Path(__file__).relative_to(r)):sha(Path(__file__))},'dependencies':{str(p):sha(p)if p.is_file()else'compiled frozen test10 peer classes'for p in cp},'device_status':'NOT_TESTED','scope':'Complete production Kotlin plus Barrage Java route against actual SDK37/API102/EzHook1.3.0/frozen test10 peer bytecode; no Android stubs. Not D8/ART/device evidence.'};(o/'report.json').write_text(json.dumps(report,indent=2)+'\n');print('compile passed',report['passed']);raise SystemExit(0 if report['passed']else 1)

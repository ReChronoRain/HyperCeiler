#!/usr/bin/env python3
"""Compile whole current media text Kotlin against actual SDK37 and frozen test13 peers."""
from pathlib import Path
import argparse,subprocess,json,hashlib,os
p=argparse.ArgumentParser();p.add_argument('--output',required=True,type=Path);a=p.parse_args();o=a.output;o.mkdir(parents=True,exist_ok=False)
r=Path(__file__).resolve().parents[3];c=Path('/home/hgn284/.cache/hyperceiler-build');s=c/'source-os4-hyos-test13-20261009-r1'
cp=[s/f'library/{m}/build/intermediates/javac/debug/compileDebugJavaWithJavac/classes'for m in['common','libhook','hidden-api']]+[s/f'library/{m}/build/intermediates/built_in_kotlinc/debug/compileDebugKotlin/classes'for m in['common','libhook']]+[c/'android-sdk/platforms/android-37.0/android.jar']+[c/'changed-hooks-compile'/f for f in['hook-xposed-102-1.3.0.jar','core-1.3.0.jar','api-102.0.0.jar']]+list((c/'gradle-home/caches').glob('*/transforms/*/transformed/core-ktx-*/jars/classes.jar'))+list((c/'gradle-home/caches').glob('*/transforms/*/transformed/core-ktx-*-api.jar'))
cp += list((c/'gradle-home/caches').glob('*/transforms/*/transformed/dexkit-*/jars/classes.jar'))
assert all(p.exists()for p in cp)
kh=r/'tools-cache/kotlin-2.4.10/kotlinc';src=r/'source/library/libhook/src/main/java/com/sevtinge/hyperceiler/libhook/rules/systemui/controlcenter/media3/AlwaysDarkOS4.kt';sha=lambda p:hashlib.sha256(p.read_bytes()).hexdigest();before={str(x):sha(x) for x in [src,src.with_name("AlwaysDark.kt")]}
def dependency_digest(p):
 if p.is_file(): return sha(p)
 return hashlib.sha256(''.join(str(f.relative_to(p))+' '+sha(f)+'\n' for f in sorted(p.rglob('*')) if f.is_file()).encode()).hexdigest()
deps_before={str(p):dependency_digest(p) for p in cp}
cmd=[str(c/'jdk-25.0.4.1/bin/java'),'-cp',str(kh/'lib/kotlin-compiler.jar'),'org.jetbrains.kotlin.cli.jvm.K2JVMCompiler','-kotlin-home',str(kh),'-jvm-target','21','-classpath',os.pathsep.join(map(str,cp)),'-d',str(o/'classes'),str(src),str(src.with_name('AlwaysDark.kt'))];z=subprocess.run(cmd,capture_output=True,text=True);(o/'compile.txt').write_text(z.stdout+z.stderr)
report={'passed':z.returncode==0 and before=={str(x):sha(x) for x in [src,src.with_name("AlwaysDark.kt")]} and deps_before=={str(p):dependency_digest(p)for p in cp},'exit_code':z.returncode,'command':cmd,'source':{str(Path(x).relative_to(r)):v for x,v in before.items()},'source_unchanged':before=={str(x):sha(x) for x in [src,src.with_name("AlwaysDark.kt")]},'runner':{str(Path(__file__).relative_to(r)):sha(Path(__file__))},'dependencies':deps_before,'dependency_digest_algorithm':'File SHA256 or SHA256 of sorted relative-path + space + file SHA256 + newline for every compiled peer directory file','device_status':'NOT_TESTED','scope':'Complete production Kotlin against actual SDK37/API102/EzHook1.3.0/frozen test13 peer bytecode; no Android stubs. Not D8/ART/device evidence.'};(o/'report.json').write_text(json.dumps(report,indent=2)+'\n');print('compile passed',report['passed']);raise SystemExit(0 if report['passed']else 1)

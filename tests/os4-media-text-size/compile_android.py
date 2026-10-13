#!/usr/bin/env python3
"""Compile whole current media text Kotlin against actual SDK37 and frozen test11 peers."""
from pathlib import Path
import argparse,subprocess,json,hashlib,os

import sys
sys.path.insert(0, str(Path(__file__).resolve().parents[1]))
import hyperceiler_test_paths as test_paths

p=argparse.ArgumentParser();p.add_argument('--output',required=True,type=Path);a=p.parse_args();o=a.output;o.mkdir(parents=True,exist_ok=False)
r=test_paths.TEST_DATA_ROOT;c=test_paths.BUILD_CACHE;s=test_paths.SNAPSHOT_ROOT / 'source-os4-hyos-test11-20261009-r1'
cp=[s/f'library/{m}/build/intermediates/javac/debug/compileDebugJavaWithJavac/classes'for m in['common','libhook','hidden-api']]+[s/f'library/{m}/build/intermediates/built_in_kotlinc/debug/compileDebugKotlin/classes'for m in['common','libhook']]+[test_paths.ANDROID_HOME / 'platforms/android-37.0/android.jar']+[c/'changed-hooks-compile'/f for f in['hook-xposed-102-1.3.0.jar','core-1.3.0.jar','api-102.0.0.jar']]+list((c/'gradle-home/caches').glob('*/transforms/*/transformed/core-ktx-*/jars/classes.jar'))+list((c/'gradle-home/caches').glob('*/transforms/*/transformed/core-ktx-*-api.jar'))
cp += list((c/'gradle-home/caches').glob('*/transforms/*/transformed/dexkit-*/jars/classes.jar'))
assert all(p.exists()for p in cp)
kh=test_paths.KOTLIN_HOME;src=test_paths.SOURCE_ROOT / 'library/libhook/src/main/java/com/sevtinge/hyperceiler/libhook/rules/systemui/controlcenter/media3/MediaViewSize.kt';sha=lambda p:hashlib.sha256(p.read_bytes()).hexdigest();before=sha(src)
def dependency_digest(p):
 if p.is_file(): return sha(p)
 return hashlib.sha256(''.join(str(f.relative_to(p))+' '+sha(f)+'\n' for f in sorted(p.rglob('*')) if f.is_file()).encode()).hexdigest()
deps_before={str(p):dependency_digest(p) for p in cp}
cmd=[str(test_paths.JAVA_HOME / 'bin/java'),'-cp',str(kh/'lib/kotlin-compiler.jar'),'org.jetbrains.kotlin.cli.jvm.K2JVMCompiler','-kotlin-home',str(kh),'-jvm-target','21','-classpath',os.pathsep.join(map(str,cp)),'-d',str(o/'classes'),str(src)];z=subprocess.run(cmd,capture_output=True,text=True);(o/'compile.txt').write_text(z.stdout+z.stderr)
report={'passed':z.returncode==0 and before==sha(src) and deps_before=={str(p):dependency_digest(p)for p in cp},'exit_code':z.returncode,'command':cmd,'source':{str(test_paths.report_path(src, r)):sha(src)},'source_unchanged':before==sha(src),'runner':{str(test_paths.report_path(Path(__file__), r)):sha(Path(__file__))},'dependencies':deps_before,'dependency_digest_algorithm':'File SHA256 or SHA256 of sorted relative-path + space + file SHA256 + newline for every compiled peer directory file','device_status':'NOT_TESTED','scope':'Complete production Kotlin against actual SDK37/API102/EzHook1.3.0/frozen test11 peer bytecode; no Android stubs. Not D8/ART/device evidence.'};(o/'report.json').write_text(json.dumps(report,indent=2)+'\n');print('compile passed',report['passed']);raise SystemExit(0 if report['passed']else 1)

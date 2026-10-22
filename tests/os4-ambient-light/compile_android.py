#!/usr/bin/env python3
"""Compile the complete route, OS4 adapter/resolver and shared shader against SDK37."""
from pathlib import Path
import argparse,subprocess,json,hashlib,os

import sys
sys.path.insert(0, str(Path(__file__).resolve().parents[1]))
import hyperceiler_test_paths as test_paths

p=argparse.ArgumentParser();p.add_argument('--output',required=True,type=Path);a=p.parse_args();o=a.output;o.mkdir(parents=True,exist_ok=False)
r=test_paths.TEST_DATA_ROOT;c=test_paths.BUILD_CACHE;s=test_paths.SNAPSHOT_ROOT / 'source-os4-hyos-test12-20261009-r1';kh=test_paths.KOTLIN_HOME
cp=[s/f'library/{m}/build/intermediates/compile_library_classes_jar/debug/bundleLibCompileToJarDebug/classes.jar'for m in ['common','libhook','hidden-api']]+[test_paths.ANDROID_HOME / 'platforms/android-37.0/android.jar']+[c/'changed-hooks-compile'/f for f in ['hook-xposed-102-1.3.0.jar','core-1.3.0.jar','api-102.0.0.jar']]+[kh/'lib/kotlin-stdlib.jar']
for name in ['core-ktx','constraintlayout','dexkit','annotation']:
 cp+=list((c/'gradle-home/caches').glob(f'*/transforms/*/transformed/{name}-*/jars/classes.jar'))
 cp+=list((c/'gradle-home/caches').glob(f'*/transforms/*/transformed/{name}-*-api.jar'))
assert all(x.exists()for x in cp)
base=test_paths.SOURCE_ROOT / 'library/libhook/src/main/java/com/sevtinge/hyperceiler/libhook';src=[base/f'rules/systemui/controlcenter/media3/{n}.kt'for n in ['AmbientLight','AmbientLightOS4','AmbientColorResolverOS4','AmbientNativeColorOS4']]+[base/'utils/hookapi/systemui/controlcenter/drawable/AmbientLightDrawable.kt']
sha=lambda p:hashlib.sha256(p.read_bytes()).hexdigest();before={str(x.relative_to(test_paths.SOURCE_ROOT)):sha(x)for x in src};deps={str(x):sha(x)for x in cp}
cmd=[str(test_paths.JAVA_HOME / 'bin/java'),'-cp',str(kh/'lib/kotlin-compiler.jar'),'org.jetbrains.kotlin.cli.jvm.K2JVMCompiler','-kotlin-home',str(kh),'-jvm-target','21','-classpath',os.pathsep.join(map(str,cp)),'-d',str(o/'classes'),*map(str,src)];z=subprocess.run(cmd,capture_output=True,text=True);(o/'compile.txt').write_text(z.stdout+z.stderr)
after={str(x.relative_to(test_paths.SOURCE_ROOT)):sha(x)for x in src};report={'passed':z.returncode==0 and before==after and deps=={str(x):sha(x)for x in cp},'exit_code':z.returncode,'command':cmd,'sources':before,'sources_after':after,'inputs_unchanged':before==after,'dependencies':deps,'runner':{str(test_paths.report_path(Path(__file__), r)):sha(Path(__file__))},'device_status':'NOT_TESTED','scope':'All five actual current production Kotlin files, real SDK37/API102/EzHook 1.3.0 and frozen test12 peer jars. No Android or resolver/shader host stand-ins. Compilation only, not ART/device/GPU verification.'};(o/'report.json').write_text(json.dumps(report,indent=2)+'\n');print('compile passed',report['passed']);raise SystemExit(0 if report['passed']else 1)

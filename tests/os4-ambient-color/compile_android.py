#!/usr/bin/env python3
from pathlib import Path
import argparse,hashlib,json,subprocess

import sys
sys.path.insert(0, str(Path(__file__).resolve().parents[1]))
import hyperceiler_test_paths as test_paths

HERE=Path(__file__).resolve().parent;ROOT=test_paths.TEST_DATA_ROOT;CACHE=test_paths.BUILD_CACHE;KH=test_paths.KOTLIN_HOME;SOURCE=test_paths.SOURCE_ROOT / 'library/libhook/src/main/java/com/sevtinge/hyperceiler/libhook/rules/systemui/controlcenter/media3/AmbientColorResolverOS4.kt'
sha=lambda p:hashlib.sha256(p.read_bytes()).hexdigest()
p=argparse.ArgumentParser();p.add_argument('--output',type=Path,required=True);a=p.parse_args();a.output.mkdir(parents=True,exist_ok=False)
jar=test_paths.ANDROID_HOME / 'platforms/android-37.0/android.jar';native=SOURCE.with_name('AmbientNativeColorOS4.kt');before=sha(SOURCE);native_before=sha(native);cmd=[test_paths.JAVA_HOME / 'bin/java','-cp',KH/'lib/kotlin-compiler.jar','org.jetbrains.kotlin.cli.jvm.K2JVMCompiler','-kotlin-home',KH,'-jvm-target','21','-classpath',jar,'-d',a.output/'classes',SOURCE,native];r=subprocess.run(list(map(str,cmd)),capture_output=True,text=True);(a.output/'compile.txt').write_text(r.stdout+r.stderr);d={'passed':r.returncode==0 and before==sha(SOURCE) and native_before==sha(native),'exit_code':r.returncode,'sources':{str(SOURCE.relative_to(test_paths.SOURCE_ROOT)):before,str(native.relative_to(test_paths.SOURCE_ROOT)):native_before},'dependencies':{str(x):sha(x)for x in [jar,KH/'lib/kotlin-compiler.jar',KH/'lib/kotlin-stdlib.jar']},'command':list(map(str,cmd)),'runner':{'path':str(test_paths.report_path(Path(__file__), ROOT)),'sha256':sha(Path(__file__))},'device_status':'NOT_TESTED','scope':'Whole production resolver compiled against actual Android37 SDK without host graphics stubs. No hook registration/API102 dependency in this helper; lifecycle integration is separately owned and tested. No ART or device execution.'};(a.output/'report.json').write_text(json.dumps(d,indent=2)+'\n');print(d['passed']);raise SystemExit(0 if d['passed']else 1)

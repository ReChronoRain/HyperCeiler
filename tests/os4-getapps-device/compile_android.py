#!/usr/bin/env python3
"""Compile all four production files against SDK37 and frozen test11 compiled peers."""
from pathlib import Path
import argparse,subprocess,json,hashlib,os,importlib.util

import sys
sys.path.insert(0, str(Path(__file__).resolve().parents[1]))
import hyperceiler_test_paths as test_paths

HERE=Path(__file__).resolve().parent;ROOT=test_paths.TEST_DATA_ROOT;CACHE=test_paths.BUILD_CACHE;FROZEN=test_paths.SNAPSHOT_ROOT / 'source-os4-hyos-test11-20261009-r1'
sp=importlib.util.spec_from_file_location('getapps_tests',HERE/'run.py');m=importlib.util.module_from_spec(sp);sp.loader.exec_module(m)
p=argparse.ArgumentParser();p.add_argument('--output',required=True,type=Path);a=p.parse_args();out=a.output;out.mkdir(parents=True,exist_ok=False)
cp=[test_paths.ANDROID_HOME / 'platforms/android-37.0/android.jar',FROZEN/'library/processor/build/libs/processor.jar']
for module in ['common','core','libhook','hidden-api']:
 for suffix in ['javac/debug/compileDebugJavaWithJavac/classes','built_in_kotlinc/debug/compileDebugKotlin/classes']:
  q=FROZEN/f'library/{module}/build/intermediates/{suffix}'
  if q.exists():cp.append(q)
 cp+=list((FROZEN/f'library/{module}/build/intermediates').glob('**/R.jar'))
cp+=sorted((CACHE/'gradle-home/caches/9.7.1/transforms').glob('*/transformed/*/jars/classes.jar'))
cp+=sorted((CACHE/'gradle-home/caches/modules-2/files-2.1').glob('**/*.jar'))
cp+=[CACHE/'changed-hooks-compile'/f for f in ['hook-xposed-102-1.3.0.jar','core-1.3.0.jar','api-102.0.0.jar']]
cp+=[test_paths.KOTLIN_HOME / 'lib/kotlin-stdlib.jar'];cp=list(dict.fromkeys(cp));assert all(x.exists()for x in cp)
sha=lambda p:hashlib.sha256(p.read_bytes()).hexdigest();before={str(test_paths.report_path(p, ROOT)):sha(p)for p in m.FILES}
cmd=[str(test_paths.JAVA_HOME / 'bin/javac'),'--release','21','-encoding','UTF-8','-proc:none','-cp',os.pathsep.join(map(str,cp)),'-d',str(out/'classes'),*map(str,m.FILES)]
r=subprocess.run(cmd,capture_output=True,text=True);(out/'compile.txt').write_text(r.stdout+r.stderr)
report={'passed':r.returncode==0 and before=={str(test_paths.report_path(p, ROOT)):sha(p)for p in m.FILES},'exit_code':r.returncode,'command':cmd,'sources':before,'dependencies':{str(p):sha(p)if p.is_file()else'frozen test11 compiled peer directory'for p in cp},'runner':{'path':str(test_paths.report_path(Path(__file__), ROOT)),'sha256':sha(Path(__file__))},'scope':'Four whole production Java files compiled against actual SDK37/API102/EzHook and frozen test11 peers, no host stubs. Not a full APK/D8 build or ART/device execution.','device_status':'NOT_TESTED'};(out/'report.json').write_text(json.dumps(report,indent=2)+'\n');print('passed',report['passed']);raise SystemExit(0 if report['passed']else 1)

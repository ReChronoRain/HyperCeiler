#!/usr/bin/env python3
"""Compile whole current PIN rule against actual SDK37 and frozen test8 peers."""
from pathlib import Path
import argparse,subprocess,json,hashlib,os
p=argparse.ArgumentParser();p.add_argument('--output',required=True,type=Path);a=p.parse_args();o=a.output;o.mkdir(parents=True,exist_ok=False)
r=Path(__file__).resolve().parents[3];c=Path('/home/hgn284/.cache/hyperceiler-build');s=c/'source-os4-hyos-test8-20261009-r2'
cp=[s/f'library/{m}/build/intermediates/javac/debug/compileDebugJavaWithJavac/classes'for m in['common','libhook','hidden-api']]+[s/f'library/{m}/build/intermediates/built_in_kotlinc/debug/compileDebugKotlin/classes'for m in['common','libhook']]+[c/'android-sdk/platforms/android-37.0/android.jar']+[c/'changed-hooks-compile'/f for f in['hook-xposed-102-1.3.0.jar','core-1.3.0.jar','api-102.0.0.jar']]+[r/'tools-cache/kotlin-2.4.10/kotlinc/lib/kotlin-stdlib.jar']
assert all(p.exists()for p in cp)
src=r/'source/library/libhook/src/main/java/com/sevtinge/hyperceiler/libhook/rules/systemui/lockscreen/ScramblePIN.java';sha=lambda p:hashlib.sha256(p.read_bytes()).hexdigest();before=sha(src)
cmd=[str(c/'jdk-25.0.4.1/bin/javac'),'--release','21','-encoding','UTF-8','-proc:none','-cp',os.pathsep.join(map(str,cp)),'-d',str(o/'classes'),str(src)];z=subprocess.run(cmd,capture_output=True,text=True);(o/'compile.txt').write_text(z.stdout+z.stderr)
report={'passed':z.returncode==0 and before==sha(src),'exit_code':z.returncode,'command':cmd,'source':{str(src.relative_to(r)):sha(src)},'source_unchanged':before==sha(src),'runner':{str(Path(__file__).relative_to(r)):sha(Path(__file__))},'dependencies':{str(p):sha(p)if p.is_file()else'compiled frozen test8 peer classes'for p in cp},'device_status':'NOT_TESTED','scope':'Complete production Java against actual SDK37/API102/EzHook1.3.0/frozen test8 peer bytecode; no Android stubs. Not D8/ART/device evidence.'};(o/'report.json').write_text(json.dumps(report,indent=2)+'\n');print('compile passed',report['passed']);raise SystemExit(0 if report['passed']else 1)

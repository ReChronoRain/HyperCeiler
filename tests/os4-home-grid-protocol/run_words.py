#!/usr/bin/env python3
"""Execute full original Dart grid arithmetic with declared allocation/log/pixel adapters."""
import argparse,hashlib,json,os,resource,subprocess,sys
from pathlib import Path

sys.path.insert(0, str(Path(__file__).resolve().parents[1]))
import hyperceiler_test_paths as test_paths

sys.dont_write_bytecode=True
W=Path(__file__).resolve().parent;R=test_paths.TEST_DATA_ROOT
ARM=test_paths.BUILD_CACHE / 'arm64-emulation/root'
ROM=test_paths.ROM_ROOT / 'launcher-native/lib/arm64-v8a/libapp.so';PIN='a37339e922e0dbebda37d0e10aba599bdec449a0bb8d170305b6cb7d5cfec992'
sha=lambda p:hashlib.sha256(p.read_bytes()).hexdigest()
def main():
 p=argparse.ArgumentParser();p.add_argument('--output',required=True,type=Path);a=p.parse_args();out=a.output.resolve();out.mkdir(parents=True,exist_ok=False)
 resource.setrlimit(resource.RLIMIT_CORE,(0,0));assert sha(ROM)==PIN
 sources=[Path(__file__).resolve(),W/'word_fixture.cpp',W/'geometry_v4_fixture.cpp',W/'word_probe.S',W/'producer_probe.S']
 inputs={str(test_paths.report_path(x, R)):sha(x)for x in sources};report={'passed':False,'device_status':'NOT_TESTED','production_changed':False,'inputs_before':inputs,'rom':{'path':str(test_paths.report_path(ROM, R)),'sha256':PIN},'runs':[]}
 def save():(out/'report.json').write_text(json.dumps(report,indent=2)+'\n')
 env=dict(os.environ,LD_LIBRARY_PATH=str(ARM/'usr/lib/x86_64-linux-gnu'))
 def run(label,cmd,expected=0,witness=None):
  cmd=list(map(str,cmd));r=subprocess.run(cmd,env=env,capture_output=True,text=True,timeout=60);(out/(label+'.stdout')).write_text(r.stdout);(out/(label+'.stderr')).write_text(r.stderr)
  okay=r.returncode==expected and(witness is None or witness in r.stderr);report['runs'].append({'label':label,'command':cmd,'exit_code':r.returncode,'expected_exit':expected,'witness':witness,'passed':okay});save();assert okay,(label,r.returncode,r.stderr);print(label,'PASS',flush=True);return r
 binary=out/'probe';run('compile',[ARM/'usr/bin/aarch64-linux-gnu-g++-13','--sysroot='+str(ARM),'-std=c++20','-O2','-Wall','-Wextra','-Werror',W/'word_fixture.cpp',W/'word_probe.S','-o',binary])
 cmd=[ARM/'usr/bin/qemu-aarch64-static','-L',ARM/'usr/aarch64-linux-gnu',binary,ROM];report['positive']=json.loads(run('positive',cmd).stdout);assert report['positive']['cases']==356 and report['positive']['checks']>0 and report['positive']['passed'] is True
 for name,witness in {'jeejen-missed':'typed column matches selected phone or exact untouched other-device branch','secondary-row-old':'secondary word-only shrink geometry uses both selected dimensions','secondary-redirect-missed':'four secondary original caller BLs reach fixed body','secondary-no-cache':'secondary original Rx setter publishes selected row'}.items():run(name,cmd+[name],3,witness)
 report['inputs_after']={str(test_paths.report_path(x, R)):sha(x)for x in sources};report['inputs_unchanged']=report['inputs_after']==inputs and sha(ROM)==PIN;assert report['inputs_unchanged']
 report['binary_sha256']=sha(binary);report['passed']=True;report['scope']=['Pure instruction replacements, no new runtime gateway: three PhoneRowCol column producers, four original secondary variable BL sites redirected to original fixedRows, one dead MOVE before original column stack capture, all three original row stack reads.','Full PhoneRowColHandler and full secondary fixedRows/centering normal bodies execute; four real class-guarded calGridSize caller slices execute, not the complete asynchronous 7372-byte caller. One original post-call Rx-to-payload store executes.','Outer caller frame/inputs, allocation/async wrappers/logging, preference lookup, final copyWith, Rx notification, pixel alignment, secondary final calVariableValues tail and foreign dynamic fallback are explicit adapters. Actual original variable callee is also a routing sentinel for missing-redirect negative control.','356 cases comprise 70 enabled/disabled producer cases, 280 caller/selected geometry/cache sequences, four foreign class cases and two restored-original fixed cases. Pre-producer cache remains original; same-owner original fixed publication updates subsequent zero-cell read.','No real Dart VM/GC, Android/phone, SQL migration, preview policy, page lifetime or production installer approval.'];save()
if __name__=='__main__':main()

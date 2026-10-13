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
 sources=[Path(__file__).resolve(),W/'producer_fixture.cpp',W/'geometry_v4_fixture.cpp',W/'producer_probe.S']
 inputs={str(test_paths.report_path(x, R)):sha(x)for x in sources};report={'passed':False,'device_status':'NOT_TESTED','production_changed':False,'inputs_before':inputs,'rom':{'path':str(test_paths.report_path(ROM, R)),'sha256':PIN},'runs':[]}
 def save():(out/'report.json').write_text(json.dumps(report,indent=2)+'\n')
 env=dict(os.environ,LD_LIBRARY_PATH=str(ARM/'usr/lib/x86_64-linux-gnu'))
 def run(label,cmd,expected=0,witness=None):
  cmd=list(map(str,cmd));r=subprocess.run(cmd,env=env,capture_output=True,text=True,timeout=60);(out/(label+'.stdout')).write_text(r.stdout);(out/(label+'.stderr')).write_text(r.stderr)
  okay=r.returncode==expected and(witness is None or witness in r.stderr);report['runs'].append({'label':label,'command':cmd,'exit_code':r.returncode,'expected_exit':expected,'witness':witness,'passed':okay});save();assert okay,(label,r.returncode,r.stderr);print(label,'PASS',flush=True);return r
 binary=out/'probe';run('compile',[ARM/'usr/bin/aarch64-linux-gnu-g++-13','--sysroot='+str(ARM),'-std=c++20','-O2','-Wall','-Wextra','-Werror',W/'producer_fixture.cpp',sources[-1],'-o',binary])
 cmd=[ARM/'usr/bin/qemu-aarch64-static','-L',ARM/'usr/aarch64-linux-gnu',binary,ROM];report['positive']=json.loads(run('positive',cmd).stdout);assert report['positive']['cases']==70 and report['positive']['checks']>0 and report['positive']['passed'] is True
 for name,witness in {'skip-typed':'typed column matches selected phone or exact untouched other-device branch','all-devices':'typed column matches selected phone or exact untouched other-device branch'}.items():run(name,cmd+[name],3,witness)
 report['inputs_after']={str(test_paths.report_path(x, R)):sha(x)for x in sources};report['inputs_unchanged']=report['inputs_after']==inputs and sha(ROM)==PIN;assert report['inputs_unchanged']
 report['binary_sha256']=sha(binary);report['passed']=True;report['scope']=['Full original PhoneRowColHandler.handle 1abe8f8/624 executes normal 4-max,4-minus,5-column,jeejen preference-derived and non-phone paths.','Cold selected columns3..9 and disabled control preserve original row marker -1 for phone,0 otherwise, same-context copyWith identity; a local direct B recipe changes only its named column argument.','Dart context allocation, async wrapper entry/return, preference lookup, logging and final copyWith are explicit adapters. This does not execute full async VM/GC, actual preference map or copyWith allocation/Rx publication.','Complete production gateway guard/rollback/lifetime/lease is not installed. No phone or Android execution.'];save()
if __name__=='__main__':main()

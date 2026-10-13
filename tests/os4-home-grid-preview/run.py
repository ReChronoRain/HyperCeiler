#!/usr/bin/env python3
"""Actual preview object provenance with explicit outer async-frame adapters."""
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
 p=argparse.ArgumentParser();p.add_argument('--output',required=True,type=Path);a=p.parse_args();out=a.output.resolve();out.mkdir(parents=True,exist_ok=False);resource.setrlimit(resource.RLIMIT_CORE,(0,0))
 sources=[Path(__file__).resolve(),W/'fixture.cpp',W/'probe.S',test_paths.SOURCE_ROOT / 'tests/os4-home-grid-protocol/quantized_fixture.cpp',test_paths.SOURCE_ROOT / 'tests/os4-home-grid-protocol/quantized_gateway.S',test_paths.SOURCE_ROOT / 'tests/os4-home-grid-secondary-spacing/probe.S']
 before={str(test_paths.report_path(x, R)):sha(x)for x in sources};assert sha(ROM)==PIN
 report=dict(passed=False,device_status='NOT_TESTED',candidate_consistency_passed=False,inputs_before=before,rom=dict(path=str(test_paths.report_path(ROM, R)),sha256=PIN),runs=[])
 def save():(out/'report.json').write_text(json.dumps(report,indent=2)+'\n')
 env=dict(os.environ,LD_LIBRARY_PATH=str(ARM/'usr/lib/x86_64-linux-gnu'))
 def run(label,cmd,expected=0,witness=None):
  cmd=list(map(str,cmd));r=subprocess.run(cmd,env=env,capture_output=True,text=True,timeout=90);(out/(label+'.stdout')).write_text(r.stdout);(out/(label+'.stderr')).write_text(r.stderr)
  okay=r.returncode==expected and(witness is None or witness in r.stderr);report['runs'].append(dict(label=label,command=cmd,exit_code=r.returncode,expected_exit=expected,witness=witness,passed=okay));save();assert okay,(label,r.returncode,r.stderr[-4000:]);print(label,'PASS',flush=True);return r
 binary=out/'probe';run('compile',[ARM/'usr/bin/aarch64-linux-gnu-g++-13','--sysroot='+str(ARM),'-std=c++20','-O2','-Wall','-Wextra','-Werror',W/'fixture.cpp',W/'probe.S',test_paths.SOURCE_ROOT / 'tests/os4-home-grid-secondary-spacing/probe.S',test_paths.SOURCE_ROOT / 'tests/os4-home-grid-protocol/quantized_gateway.S','-o',binary])
 cmd=[ARM/'usr/bin/qemu-aarch64-static','-L',ARM/'usr/aarch64-linux-gnu',binary,ROM];report['positive']=json.loads(run('positive',cmd).stdout);assert report['positive']['cases']==633 and report['positive']['option_mismatches']==180
 for name,witness in {'clamp-main-copy':'actual currentConfig to preview retains all selected columns','lost-payload-sync':'phone-class normalization synchronizes option column before original calculation','lost-row-write':'original reason4 caller writes computed selected rows to payload','overwrite-option-x':'original option payload retains constructor column argument'}.items():run(name,cmd+[name],3,witness)
 report['inputs_after']={str(test_paths.report_path(x, R)):sha(x)for x in sources};report['inputs_unchanged']=before==report['inputs_after'] and sha(ROM)==PIN;assert report['inputs_unchanged'];report['binary_sha256']=sha(binary);report['passed']=True
 report['scope']=['Complete original prepareCellCountXYInUse420B, createCellCountXY268B, reset28B, currentConfig100B and Rx.value100B normal paths execute. Populated singleton globals; logging/allocation are adapters.','Original reason4 calGridSize guarded slice1927524..19275fc executes with declared outer-frame and exit adapters; the whole7372B async function is not executed. loadGridConfig is a recorder only.','Original full fixedRows/tail/alignPixel arithmetic executes with previously frozen local quantization experiment. Synthetic geometry120x800, DPR1, all70selected pairs and three original UI option argument shapes.','Disabled test restores the exact original BL and runs full constructor/caller/cache/write/load with an explicitly recorded variable-calculation boundary returning row7. This proves OEM4/4/5 input and caller flow preservation, not the complete variable geometry algorithm.','A separate 12B experimental target at the already phone-class-guarded BL writes selected X into the exact payload then tail-branches to original fixedRows. Original LR/callsite is preserved, no added Dart/C call; actual runtime allocation/install/revoke not implemented.','PASS means reproduced provenance and inconsistency assertions; it explicitly does not mean this candidate has consistent preview metadata, complete UI/database support or a production installer.']
 save()
if __name__=='__main__':main()

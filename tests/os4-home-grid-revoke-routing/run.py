#!/usr/bin/env python3
"""Exact actual caller/callee routing cuts; not a scheduler or production revoke proof."""
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
 sources=[Path(__file__).resolve(),W/'fixture.cpp',W/'router.S',test_paths.SOURCE_ROOT / 'tests/os4-home-grid-singlepoint/fixture_v2.cpp',test_paths.SOURCE_ROOT / 'tests/os4-home-grid-singlepoint/leaf_v2.S',test_paths.SOURCE_ROOT / 'tests/os4-home-grid-secondary-spacing/probe.S',test_paths.SOURCE_ROOT / 'tests/os4-home-grid-singlepoint/run_v2.py',test_paths.TEST_DATA_ROOT / 'acceptance/home-grid-next/preview-typed-test16/final-v1/report.json',test_paths.TEST_DATA_ROOT / 'acceptance/home-grid-next/singlepoint-test16/arm-v2/report.json']
 before={str(test_paths.report_path(x, R)):sha(x)for x in sources};assert sha(ROM)==PIN
 report=dict(passed=False,device_status='NOT_TESTED',artifact_runtime_approval=False,inputs_before=before,rom=dict(path=str(test_paths.report_path(ROM, R)),sha256=PIN),runs=[])
 report['tool_inputs']={str(p):sha(p)for p in [ARM/'usr/bin/aarch64-linux-gnu-g++-13',ARM/'usr/bin/qemu-aarch64-static']}
 def save():(out/'report.json').write_text(json.dumps(report,indent=2)+'\n')
 env=dict(os.environ,LD_LIBRARY_PATH=str(ARM/'usr/lib/x86_64-linux-gnu'))
 def run(label,cmd,expected=0,witness=None):
  cmd=list(map(str,cmd));r=subprocess.run(cmd,env=env,capture_output=True,text=True,timeout=90);(out/(label+'.stdout')).write_text(r.stdout);(out/(label+'.stderr')).write_text(r.stderr)
  okay=r.returncode==expected and(witness is None or witness in r.stderr);report['runs'].append(dict(label=label,command=cmd,exit_code=r.returncode,expected_exit=expected,witness=witness,passed=okay));save();assert okay,(label,r.returncode,r.stderr[-4000:]);print(label,'PASS',flush=True);return r
 binary=out/'probe';run('compile',[ARM/'usr/bin/aarch64-linux-gnu-g++-13','--sysroot='+str(ARM),'-std=c++20','-O2','-Wall','-Wextra','-Werror',W/'fixture.cpp',W/'router.S',test_paths.SOURCE_ROOT / 'tests/os4-home-grid-secondary-spacing/probe.S',test_paths.SOURCE_ROOT / 'tests/os4-home-grid-singlepoint/leaf_v2.S','-o',binary])
 cmd=[ARM/'usr/bin/qemu-aarch64-static','-L',ARM/'usr/aarch64-linux-gnu',binary,ROM];report['positive']=json.loads(run('positive',cmd).stdout);assert report['positive']['cases']==30 and report['positive']['unsafe_restore_counterexamples']==4
 for name,witness in {'wrong-variable-target':'route chooses original variable versus fixed according to captured policy','late-recheck':'original fixed publishes the row actually captured by its prologue','lost-preview-sync':'preview payload mutates only after enabled router admission'}.items():run(name,cmd+[name],3,witness)
 report['inputs_after']={str(test_paths.report_path(x, R)):sha(x)for x in sources};report['inputs_unchanged']=before==report['inputs_after'] and sha(ROM)==PIN;assert report['inputs_unchanged'];report['binary_sha256']=sha(binary);report['passed']=True
 report['scope']=['Four original class-guarded caller slices execute and invoke complete original fixedRows/tails/alignPixel. Mixed restoration states deterministically demonstrate variable reason3/4/9/10 captured as fixed rows; this is an actual instruction/input counterexample, not a concurrent scheduler simulation PASS.', 'Experimental immutable fixed-entry router compares exact original LR, captures packed gate/selected values with one LDAR, sends closed former-variable calls to original variable and known fixed calls to original fixed; unknown fixed callers pass through.', 'Original variable first36B prologue executes, then a recorder reads its actual stack-captured column/reason/layout and returns. Full OEM variable numerical algorithm is not claimed. Both original natural-fixed caller suffixes execute their decoded register moves/BL and original fixed function with outer-frame/continuation adapters; the complete calGridSize async function is not executed.', 'Injected gate clear at the instruction immediately after router LDAR tests one explicit admission cut; no claim that actual owner/backend/threads or full mathematical in-flight policy are implemented.', 'ab78 mathematical leaf is not installed by this fixture. Its later global gate check and already-written preview/cache state remain separate unresolved whole-grid revoke requirements.', 'No production edits, real patch backend, GC/VM, true concurrent installation/unhook, UI/DB or phone validation.']
 save()
if __name__=='__main__':main()

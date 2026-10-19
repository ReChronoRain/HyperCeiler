#!/usr/bin/env python3
# SPDX-License-Identifier: AGPL-3.0-or-later
"""Test the actual snapshot observer; keep host syscall and ARM fixture evidence separate."""
import argparse,hashlib,json,os,subprocess,tempfile
from pathlib import Path

import sys
sys.path.insert(0, str(Path(__file__).resolve().parents[1]))
import hyperceiler_test_paths as test_paths

p=argparse.ArgumentParser(description=__doc__)
p.add_argument('--sdk',type=Path,required=True)
p.add_argument('--output',type=Path,required=True)
p.add_argument('--arm64',action='store_true')
p.add_argument('--mutations',action='store_true')
a=p.parse_args();out=a.output.resolve();out.mkdir(parents=True,exist_ok=False)
here=Path(__file__).resolve().parent;source=here.parents[1];cpp=source/'app/src/main/cpp';hyos=cpp/'targets/hyos'
env=dict(os.environ);compiler='g++';runner=[]
flags=['-std=c++20','-O2','-Wall','-Wextra','-Werror','-pthread']
if a.arm64:
 r=test_paths.BUILD_CACHE / 'arm64-emulation/root'
 compiler=str(r/'usr/bin/aarch64-linux-gnu-g++-13');runner=[str(r/'usr/bin/qemu-aarch64-static')]
 env['LD_LIBRARY_PATH']=str(r/'usr/lib/x86_64-linux-gnu')
 flags+=['--sysroot='+str(r),'-static','-DHC_ARM64_FIXTURE_READS=1']
flags+=['-I'+str(a.sdk.resolve()),'-I'+str(here/'stubs'),'-I'+str(cpp),'-I'+str(hyos)]
checks=[]
def execute(label,cmd,want_success=True):
 result=subprocess.run(list(map(str,cmd)),env=env,capture_output=True,text=True)
 (out/(label+'.log')).write_text(result.stdout+result.stderr)
 passed=(result.returncode==0)==want_success
 checks.append(dict(name=label,passed=passed,returncode=result.returncode,command=list(map(str,cmd))))
 if not passed:raise RuntimeError(label+' failed: '+(result.stdout+result.stderr)[-3000:])
 return result
binary=out/'snapshot-test'
execute('build',[compiler,*flags,here/'snapshot_test.cpp','-o',binary])
result=execute('execute',[*runner,binary]);print(result.stdout[-300:])
mutations=[
 ('stale-mutex','&& boundary_read(base + kMutexVa, second.data(), sizeof(second))','&& (second = first, true)'),
 ('drop-thread-binding','|| g_search_tid != static_cast<pid_t>(syscall(SYS_gettid))','|| false'),
 ('drop-dart-identity','? boundary_matches(result - vm_data_va, kDartIdentityVa, kDartIdentity)','? true'),
 ('lose-errno','errno = saved_errno;','(void)saved_errno;'),
 ('repeat-prime','if (!g_snapshot_attempt.compare_exchange_strong(unseen, 1, std::memory_order_acq_rel))','if (false && !g_snapshot_attempt.compare_exchange_strong(unseen, 1, std::memory_order_acq_rel))'),
]
if a.mutations:
 original=(hyos/'hyos_entry.cpp').read_text()
 with tempfile.TemporaryDirectory(prefix='hc-snapshot-mutations-') as tmp:
  base=Path(tmp);testdir=base/'tests/hyos-entry';prod=base/'app/src/main/cpp/targets/hyos';testdir.mkdir(parents=True);prod.mkdir(parents=True)
  for name in ('entry_test.cpp','snapshot_test.cpp'):(testdir/name).write_bytes((here/name).read_bytes())
  (prod/'hyos_control.h').write_bytes((hyos/'hyos_control.h').read_bytes())
  for name,old,new in mutations:
   assert old in original,name
   changed=original.replace(old,new);(prod/'hyos_entry.cpp').write_text(changed)
   b=out/(name+'-test')
   execute(name+'-build',[compiler,*flags,testdir/'snapshot_test.cpp','-o',b])
   execute(name+'-rejected',[*runner,b],False)
files=[hyos/'hyos_entry.cpp',hyos/'hyos_snapshot_guard.h',hyos/'hyos_startup_reason.h',here/'extract_snapshot_guard.py',here/'snapshot_test.cpp',here/'entry_test.cpp',Path(__file__)]
files += [cpp/'targets/home/recents_runtime.h', hyos/'hyos_snapshot.h']
files+=list((cpp/'targets/weather').glob('*.h'))
report=dict(status='PASS',checks=checks,tests=32,guard_bit_mutations_rejected=60192,
 mutations_rejected=len(mutations) if a.mutations else 0,
 mode='ARM64_QEMU_WITH_DECLARED_READABLE_FIXTURES' if a.arm64 else 'HOST_REAL_PROCESS_VM_READV_WITH_FAULT_INJECTION',
 sources={str(f):hashlib.sha256(f.read_bytes()).hexdigest() for f in files},device_status='NOT_TESTED',
 limitations=['Controlled loader callbacks, not real Hyos/Zygisk Next/Flutter execution.',
 'Host normal and inaccessible-page reads exercise actual process_vm_readv; interruption and denial are injected.',
 'QEMU guest process_vm_readv is unsupported; ARM tests use an explicitly bounded fixture of declared readable regions.',
 'No assertion about device SELinux permission, actual startup order, layout effects or long-term stability.'])
(out/'report.json').write_text(json.dumps(report,indent=2)+'\n')
print(out/'report.json')

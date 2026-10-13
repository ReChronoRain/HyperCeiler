#!/usr/bin/env python3
"""Production lifetime component: exact guards, transaction faults and bounded reclaim splitting."""
import argparse,hashlib,json,re,subprocess,tempfile
from pathlib import Path

import sys
sys.path.insert(0, str(Path(__file__).resolve().parents[1]))
import hyperceiler_test_paths as test_paths

HERE=Path(__file__).resolve().parent
SOURCE=HERE.parents[1]
ROOT=test_paths.TEST_DATA_ROOT
WEATHER=SOURCE/'app/src/main/cpp/targets/weather'
NDK=test_paths.ANDROID_NDK_HOME / 'toolchains/llvm/prebuilt/linux-x86_64/bin'
sha=lambda p:hashlib.sha256(p.read_bytes()).hexdigest()
def main():
 p=argparse.ArgumentParser();p.add_argument('--output',type=Path,required=True);p.add_argument('--negative-controls',action='store_true');a=p.parse_args()
 out=a.output.resolve();out.mkdir(parents=True,exist_ok=False)
 files=[HERE/'run.py',HERE/'generate.py',HERE/'lifetime_test.cpp',*(WEATHER/n for n in ['weather_card_lifetime.h','weather_card_lifetime.cpp','weather_card_lifetime_guard.generated.h'])]
 before={str(x):sha(x) for x in files};commands=[]
 def cmd(args,path):
  args=list(map(str,args));commands.append(args);r=subprocess.run(args,capture_output=True,text=True,timeout=90);path.with_suffix('.stdout').write_text(r.stdout);path.with_suffix('.stderr').write_text(r.stderr);return r
 with tempfile.TemporaryDirectory(prefix='hc-lifetime-') as tmp:
  tmp=Path(tmp)
  check=cmd(['python3',HERE/'generate.py','--check'],out/'generated-check')
  def execute(name,mutation=None):
   folder=out/name;folder.mkdir(parents=True);binary=tmp/name.replace('/','_')
   args=['g++','-std=c++20','-O1','-g','-pthread','-Wall','-Wextra','-Werror','-Wno-misleading-indentation','-fsanitize=undefined','-fno-sanitize-recover=all','-I'+str(WEATHER)]
   if mutation:args+=['-DHC_LIFETIME_SOURCE="'+str(mutation)+'"']
   args += [HERE/'lifetime_test.cpp','-o',binary]
   b=cmd(args,folder/'build');row={'compile_exit':b.returncode}
   if b.returncode:return row
   r=cmd([binary],folder/'run');m=re.search(r'^RESULT cases=(\d+) failed=(\d+)$',r.stdout,re.M)
   bits=re.search(r'^GUARD_BITS checked=(\d+) rejected=(\d+)$',r.stdout,re.M)
   row.update(guard_bits=int(bits[1]) if bits else None,guard_rejected=int(bits[2]) if bits else None,exit_code=r.returncode,cases=int(m[1]) if m else None,failed=int(m[2]) if m else None,failures=re.findall(r'^CASE_FAILED (.*?) status=',r.stderr,re.M),binary_sha256=sha(binary))
   return row
  fixed=execute('positive')
  text=(WEATHER/'weather_card_lifetime.cpp').read_text()
  cases=[
   ('allow_busy_reader','compare_exchange_strong(expected, 1, std::memory_order_acq_rel, std::memory_order_acquire)','exchange(1, std::memory_order_acq_rel) == expected','in flight reader denies forever'),
   ('ignore_protected_bit','if (!(previous & 1) || advice != MADV_DONTNEED','if ((previous == UINT64_MAX) || advice != MADV_DONTNEED','release resumes transparent'),
   ('only_first_page','index < 2; ++index) {\n        const uintptr_t skip','index < 1; ++index) {\n        const uintptr_t skip','three segment 4K'),
   ('all_advice_filtered','|| advice != MADV_DONTNEED','|| advice == -999','other advice untouched'),
   ('wrong_errno','const int saved_errno = errno;','const int saved_errno = 0;','three segment 4K'),
   ('omit_task_gate','|| !single_thread() || !images()','|| (false && !single_thread()) || !images()','multi task'),
   ('accept_truncated_maps','return okay && eof && used == 0 && closed == 0;','return okay && eof && closed == 0;','maps trailing truncation'),
   ('ignore_fork_identity','current == g_image.parent_pid || call(SYS_getppid) != g_image.parent_pid','current == -999 || call(SYS_getppid) == -999','not fork'),
   ('skip_libc_code_guard','guards(g_image.libc, guard::kLibcGuards)','(g_image.libc != 0)','child rereads guards'),
   ('release_during_reader','compare_exchange_strong(expected, 0, std::memory_order_acq_rel)','exchange(0, std::memory_order_acq_rel) == expected','release active reader refused'),
  ]
  negatives=[]
  if a.negative_controls and fixed.get('failed')==0:
   for name,old,new,witness in cases:
    assert text.count(old)==1,(name,text.count(old))
    candidate=tmp/(name+'.cpp');candidate.write_text(text.replace(old,new));row=execute('negative/'+name,candidate);row.update(name=name,witness=witness,mutated_source_sha256=sha(candidate))
    row['detected']=row.get('compile_exit')==0 and row.get('exit_code')==1 and witness in row.get('failures',[])
    negatives.append(row)
  ndk=cmd([NDK/'aarch64-linux-android35-clang++','-std=c++20','-O2','-Wall','-Wextra','-Werror','-fPIC','-fno-exceptions','-fno-rtti','-c',WEATHER/'weather_card_lifetime.cpp','-o',tmp/'lifetime.o'],out/'ndk-compile')
  obj=sha(tmp/'lifetime.o') if ndk.returncode==0 else None
 after={str(x):sha(x) for x in files}
 passed=check.returncode==0 and ndk.returncode==0 and fixed.get('failed')==0 and fixed.get('exit_code')==0 and fixed.get('cases',0)>=64 and fixed.get('guard_bits',0)>30000 and fixed.get('guard_rejected')==fixed.get('guard_bits') and all(x['detected'] for x in negatives) and before==after
 report=dict(passed=passed,positive=fixed,negative_controls=negatives,negative_controls_requested=a.negative_controls,generated_guard_check_exit=check.returncode,ndk_compile_exit=ndk.returncode,ndk_object_sha256=obj,inputs_before=before,inputs_after=after,inputs_unchanged=before==after,commands=commands,scope=['Actual production component, immutable generated ROM bytes, parser, aligned GOT CAS, page protections, reader handshake and splitter execute on host.','Test seam supplies parent/child identity, proc maps and task bytes, madvise continuation and fault injection. Real mapped memory, process_vm_readv and mprotect execute; no real vendor ARM code executes in this host suite.','NDK compilation checks real Android ARM64 translation unit; separate original-vendor ARM research and actual component joint suites cover additional boundaries.','A task scan is a finite observation, not a thread-creation lock. API1 specialization ordering and cooperative third-party-module behavior remain preconditions.','No real phone, Dart VM, LSPosed/Hyos loading, long-term UI behavior or kernel page reclamation on Android is proved.'],device_status='NOT_TESTED')
 (out/'report.json').write_text(json.dumps(report,indent=2)+'\n');print(json.dumps({k:report[k] for k in ['passed','positive','inputs_unchanged','ndk_compile_exit']}));return 0 if passed else 1
if __name__=='__main__':raise SystemExit(main())

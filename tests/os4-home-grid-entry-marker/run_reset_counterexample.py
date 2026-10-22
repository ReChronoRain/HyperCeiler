#!/usr/bin/env python3
"""Bounded injected original reset, not proof that OTA schedules it at this cut."""
import argparse,hashlib,json,os,resource,subprocess,sys
from pathlib import Path

sys.path.insert(0, str(Path(__file__).resolve().parents[1]))
import hyperceiler_test_paths as test_paths

sys.dont_write_bytecode=True
W=Path(__file__).resolve().parent;R=test_paths.TEST_DATA_ROOT
ARM=test_paths.BUILD_CACHE / 'arm64-emulation/root'
ROM=test_paths.ROM_ROOT / 'launcher-native/lib/arm64-v8a/libapp.so'
sha=lambda p:hashlib.sha256(p.read_bytes()).hexdigest()
def one(s,old,new):
 assert s.count(old)==1,(old,s.count(old));return s.replace(old,new,1)
def main():
 p=argparse.ArgumentParser();p.add_argument('--output',type=Path,required=True);a=p.parse_args();o=a.output.resolve();o.mkdir(parents=True,exist_ok=False);resource.setrlimit(resource.RLIMIT_CORE,(0,0))
 inputs=[Path(__file__).resolve(),W/'fixture.cpp',W/'leaf.S',test_paths.SOURCE_ROOT / 'tests/os4-home-grid-singlepoint/fixture_v2.cpp',test_paths.SOURCE_ROOT / 'tests/os4-home-grid-singlepoint/leaf_v2.S',test_paths.SOURCE_ROOT / 'tests/os4-home-grid-revoke-routing/router.S',test_paths.SOURCE_ROOT / 'tests/os4-home-grid-secondary-spacing/probe.S',ROM]
 before={str(test_paths.report_path(x, R)):sha(x)for x in inputs};assert before[str(test_paths.report_path(ROM, R))]=='a37339e922e0dbebda37d0e10aba599bdec449a0bb8d170305b6cb7d5cfec992'
 s=(W/'fixture.cpp').read_text();s=one(s,'#include "../os4-home-grid-singlepoint/fixture_v2.cpp"','#include "'+str(test_paths.SOURCE_ROOT / 'tests/os4-home-grid-singlepoint/fixture_v2.cpp')+'"')
 s=one(s,'unsigned admitted=0,closed=0,invalid_safe=0;','unsigned leaks=0,finished=0;\n const bool sourceGuard=mutant!="domain-only";')
 start=s.index(' for(int route=0;');end=s.index('  ++cases;',start)
 s=s[:start]+''' for(int selectedFlag=0;selectedFlag<2;selectedFlag++)for(int64_t layout:std::array<int64_t,4>{-1,0,1,2}){
  const int route=0,state=selectedFlag?3:0,cx=3,cy=13;const double dpr=1;
'''+s[end:]
 s=one(s,'\n  auto top=uintptr_t(&h.stack[1800]);','\n  h.put<uintptr_t>(thread,0x180,uintptr_t(i.base+0xeacb78));h.put<uintptr_t>(thread,0x188,h.obj(0x2e00));\n  auto top=uintptr_t(&h.stack[1800]);')
 # Remove unreachable preview route body, and use natural actual caller suffix only.
 begin=s.index('  if(route==0){');stop=s.index('  const uint64_t saved=',begin)
 body=s[begin:stop].split('\n  else{')[0]
 body=one(body,'if(route==0){','{')
 s=s[:begin]+body+'\n'+s[stop:]
 begin=s.index('  check(h.get<int64_t>(thread,0x148)==layout,');s=s[:begin]+'''
  const auto after=h.get<int64_t>(h.obj(0x2e00),0x1f),tail=h.get<int64_t>(thread,0x148);
  check(after==-1,"exact original CellCountXY.reset executed at injected synchronous cut");
  const bool leaked=tail!=layout;
  check(leaked==(sourceGuard&&selected&&layout!=-1),"injected reset has predicted source-reread marker leak only");
  if(leaked)++leaks;else ++finished;
  std::cerr<<"OBSERVED selected="<<selected<<" original_layout="<<layout<<" source_after_reset="<<after<<" original_tail_layout="<<tail<<" marker_leaked="<<leaked<<"\\n";
 }
 check(cases==8&&leaks==(sourceGuard?3u:0u),"exact bounded counterexample count");
 std::cout<<"{\\"counterexample_checks_passed\\":true,\\"source_reread\\":"<<(sourceGuard?"true":"false")<<",\\"cases\\":"<<cases<<",\\"checks\\":"<<checks<<",\\"marker_leaks\\":"<<leaks<<",\\"completed_frames\\":"<<finished<<",\\"feature_approved\\":false,\\"actual_reset_reachability_proven\\":false}\\n";
}
'''
 s=one(s,'const int route=0,state=selectedFlag?3:0,cx=3,cy=13;','const int route=0,state=selectedFlag?3:0,cx=3,cy=13;')
 (o/'counterexample.cpp').write_text(s)
 leaf=(W/'leaf.S').read_text();leaf=one(leaf,'observer_math:\n','''observer_math:
 // TEST-ONLY synchronous injection, real original reset body unchanged.
 stp x0,x1,[x15,#-32]!
 stp x2,x30,[x15,#16]
 ldr x1,[x26,#0x188]
 ldr x16,[x26,#0x180]
 blr x16
 ldp x2,x30,[x15,#16]
 ldp x0,x1,[x15],#32
''')
 (o/'source-reread.S').write_text(leaf)
 # Bounded alternative restores the captured slot itself; deliberately leaves
 # reserved original raw int collisions outside the proven typed domain OPEN.
 begin=leaf.index('1:\n mov x1,#-1\n',leaf.index('marked_math_begin:'));end=leaf.index('marked_math_restore:',begin)
 alternative='''1:
2:
3:
 ldr x16,[x29]
 ldur x17,[x16,#-0x20]
 mov x1,#0x4000000000000000
 eor x1,x17,x1
 lsr x17,x1,#2
 cbnz x17,9f
 sub x1,x1,#1
'''
 (o/'domain-only.S').write_text(leaf[:begin]+alternative+leaf[end:])
 env=dict(os.environ,LD_LIBRARY_PATH=str(ARM/'usr/lib/x86_64-linux-gnu'));runs=[]
 for variant in ('source-reread','domain-only'):
  binary=o/variant
  cmd=[ARM/'usr/bin/aarch64-linux-gnu-g++-13','--sysroot='+str(ARM),'-std=c++20','-O2','-Wall','-Wextra','-Werror',o/'counterexample.cpp',o/(variant+'.S'),test_paths.SOURCE_ROOT / 'tests/os4-home-grid-revoke-routing/router.S',test_paths.SOURCE_ROOT / 'tests/os4-home-grid-secondary-spacing/probe.S',test_paths.SOURCE_ROOT / 'tests/os4-home-grid-singlepoint/leaf_v2.S','-o',binary]
  for kind,c in [('compile',cmd),('execute',[ARM/'usr/bin/qemu-aarch64-static','-L',ARM/'usr/aarch64-linux-gnu',binary,ROM,variant])]:
   r=subprocess.run(list(map(str,c)),env=env,capture_output=True,text=True,timeout=90);(o/(variant+'-'+kind+'.stdout')).write_text(r.stdout);(o/(variant+'-'+kind+'.stderr')).write_text(r.stderr);assert r.returncode==0,(variant,kind,r.stderr[-4000:])
  runs.append(dict(variant=variant,binary_sha256=sha(binary),result=json.loads(r.stdout)))
 after={str(test_paths.report_path(x, R)):sha(x)for x in inputs};assert before==after
 report=dict(passed=True,status='NEGATIVE_COUNTEREXAMPLE_NOT_FEATURE_PASS',device_status='NOT_TESTED',inputs_before=before,inputs_after=after,inputs_unchanged=True,runs=runs,tool_inputs={str(p):sha(p)for p in [ARM/'usr/bin/aarch64-linux-gnu-g++-13',ARM/'usr/bin/qemu-aarch64-static']},scope=['Actual original 28-byte CellCountXY.reset executes synchronously at an explicit TEST-ONLY cut after admission and before quantizer; original full fixed/correct/tail arithmetic remains. This does NOT prove any actual OTA callback invokes reset at this cut.','Source-reread candidate leaks reserved layout marker for admitted original layouts 0/1/2 after reset; -1 and all closed frames keep original layout. This candidate must not be promoted as unconditional improvement.','Domain-only decoder restores the originally captured per-frame layout despite resetting source; it still requires the true input domain excluding artificial reserved-int64 collisions and is not a production backend/whole-VM proof.'])
 (o/'report.json').write_text(json.dumps(report,indent=2)+'\n');print(json.dumps({'report':str(o/'report.json'),'sha256':sha(o/'report.json'),'runs':runs}))
if __name__=='__main__':main()

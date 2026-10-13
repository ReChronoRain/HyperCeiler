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
 sources=[Path(__file__).resolve(),W/'geometry_v4_fixture.cpp',W/'geometry_v4_probe.S']
 inputs={str(test_paths.report_path(x, R)):sha(x)for x in sources};report={'passed':False,'device_status':'NOT_TESTED','production_changed':False,'inputs_before':inputs,'rom':{'path':str(test_paths.report_path(ROM, R)),'sha256':PIN},'runs':[]}
 def save():(out/'report.json').write_text(json.dumps(report,indent=2)+'\n')
 env=dict(os.environ,LD_LIBRARY_PATH=str(ARM/'usr/lib/x86_64-linux-gnu'))
 def run(label,cmd,expected=0,witness=None):
  cmd=list(map(str,cmd));r=subprocess.run(cmd,env=env,capture_output=True,text=True,timeout=60);(out/(label+'.stdout')).write_text(r.stdout);(out/(label+'.stderr')).write_text(r.stderr)
  okay=r.returncode==expected and(witness is None or witness in r.stderr);report['runs'].append({'label':label,'command':cmd,'exit_code':r.returncode,'expected_exit':expected,'witness':witness,'passed':okay});save();assert okay,(label,r.returncode,r.stderr);print(label,'PASS',flush=True);return r
 binary=out/'probe';run('compile',[ARM/'usr/bin/aarch64-linux-gnu-g++-13','--sysroot='+str(ARM),'-std=c++20','-O2','-Wall','-Wextra','-Werror',W/'geometry_v4_fixture.cpp',sources[-1],'-o',binary])
 cmd=[ARM/'usr/bin/qemu-aarch64-static','-L',ARM/'usr/aarch64-linux-gnu',binary,ROM];report['positive']=json.loads(run('positive',cmd).stdout);assert report['positive']['cases']==475 and report['positive']['checks']>0 and report['positive']['passed'] is True
 for name,witness in {'skip-candidate':'candidate row reaches original handler field','stale-row-remainder':'original remainder uses candidate row before return','lost-column-input':'original cell geometry consumes selected columns','fixed-mint-not-overridden':'fixedRows candidate replaces original six in both representations','secondary-missed':'secondary positive-cell candidate returns selected row','pair-missing-redirect':'paired original handle uses selected row in both aspect paths','secondary-rx-not-published':'original secondary fixed body publishes selected row through exact Rx setter'}.items():run(name,cmd+[name],3,witness)
 report['inputs_after']={str(test_paths.report_path(x, R)):sha(x)for x in sources};report['inputs_unchanged']=report['inputs_after']==inputs and sha(ROM)==PIN;assert report['inputs_unchanged']
 report['binary_sha256']=sha(binary);report['passed']=True;report['scope']=['Adds full primary handle 1abeb94/176 + paired fixedRows redirect (140 cases) and secondary fixedRows a49adc/900 + a4ab58 centering with typed stack-capture gateways (70 cases). Original same-owner publication fixes later zero-cell Rx read in these ordered scenarios; actual Rx notifications are an adapter. No stale distinct-owner or pre-producer-cache guarantee. Full original calGridSizeByVariable 152228c..1522768 and calGridSizeByFixedRows 15228e8..1522b68 execute selected normal paths.','Variable row SUB and fixedRows two decode instructions are candidate replacements per scenario; context allocation, logging, numeric addition, Rx notifications, async wrapper setup/return, calVariableValues tail and pixel alignment are explicit adapters. Tail observer retains d0 so remainder is measured at the actual final call boundary. Pixel alignment is identity for this arithmetic experiment; no actual display DPR or Dart VM/GC is exercised.','210 variable candidates cover the complete UI 3..9 columns x 4..13 rows x 3 OEM layout enum identities; 3 unmodified controls prove positive config row alone does not override variable policy; 10 fixedRows baselines and 20 Smi/Mint candidate cases plus one disable restoration expose its distinct cell-shrink policy.','Full secondary helper a4bbd0..a4bd44 adds 20 selected-row/old-cache sequences and one disabled arithmetic baseline. The zero-cell early exit remains old Rx6 despite selected candidates: a concrete unclosed producer/cache requirement, not a passed adaptation. No copyWith/pipeline/FRB/DB integration, real Rx scheduling, preview consistency, full UI bounds, production gateway, phone or Android execution is claimed.'];save()
if __name__=='__main__':main()

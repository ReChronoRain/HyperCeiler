#!/usr/bin/env python3
"""Execute actual normal loader/token paths; adapters exclude scheduler, SQL and full migration."""
import argparse,hashlib,json,os,resource,subprocess,sys
from pathlib import Path

sys.path.insert(0, str(Path(__file__).resolve().parents[1]))
import hyperceiler_test_paths as test_paths

sys.dont_write_bytecode=True
W=Path(__file__).resolve().parent;R=test_paths.TEST_DATA_ROOT
ARM=test_paths.BUILD_CACHE / 'arm64-emulation/root'
ROM=test_paths.ROM_ROOT / 'launcher-native/lib/arm64-v8a/libapp_launcher.so';PIN='4dc9fa82371dda8a0a8b9eff48301469d2f84752b52b71635b84e5fc0c47fa6e'
sha=lambda p:hashlib.sha256(p.read_bytes()).hexdigest()
def main():
 p=argparse.ArgumentParser();p.add_argument('--output',required=True,type=Path);a=p.parse_args();out=a.output.resolve();out.mkdir(parents=True,exist_ok=False)
 resource.setrlimit(resource.RLIMIT_CORE,(0,0));assert sha(ROM)==PIN
 sources=[Path(__file__).resolve(),W/'loader_fixture.cpp',test_paths.TEST_DATA_ROOT / 'acceptance/home-grid-next/evidence-v2/publisher_fixture.cpp',test_paths.TEST_DATA_ROOT / 'acceptance/home-grid-next/evidence-v2/publisher_probe.S']
 inputs={str(test_paths.report_path(x, R)):sha(x)for x in sources};report={'passed':False,'device_status':'NOT_TESTED','production_changed':False,'inputs_before':inputs,'rom':{'path':str(test_paths.report_path(ROM, R)),'sha256':PIN},'runs':[]}
 def save():(out/'report.json').write_text(json.dumps(report,indent=2)+'\n')
 env=dict(os.environ,LD_LIBRARY_PATH=str(ARM/'usr/lib/x86_64-linux-gnu'))
 def run(label,cmd,expected=0,witness=None):
  cmd=list(map(str,cmd));r=subprocess.run(cmd,env=env,capture_output=True,text=True,timeout=60);(out/(label+'.stdout')).write_text(r.stdout);(out/(label+'.stderr')).write_text(r.stderr)
  okay=r.returncode==expected and(witness is None or witness in r.stderr);report['runs'].append({'label':label,'command':cmd,'exit_code':r.returncode,'expected_exit':expected,'witness':witness,'passed':okay});save();assert okay,(label,r.returncode,r.stderr);print(label,'PASS',flush=True);return r
 binary=out/'probe';run('compile',[ARM/'usr/bin/aarch64-linux-gnu-g++-13','--sysroot='+str(ARM),'-std=c++20','-O2','-Wall','-Wextra','-Werror',W/'loader_fixture.cpp',sources[-1],'-o',binary])
 cmd=[ARM/'usr/bin/qemu-aarch64-static','-L',ARM/'usr/aarch64-linux-gnu',binary,ROM];report['positive']=json.loads(run('positive',cmd).stdout);assert report['positive']['sequences']==5 and report['positive']['assertions']>0 and report['positive']['failed']==0
 for name,witness in {'no-generation-increment':'original start_load stamps epoch and new generation','no-epoch-check':'old epoch filtered by original worker','no-generation-check':'old generation filtered by original worker','lost-device-param':'original start_load carries all four typed fields unchanged','mode-does-not-advance-epoch':'new mode and epoch precede actual DB switch callsite'}.items():run(name,cmd+[name],3,witness)
 report['inputs_after']={str(test_paths.report_path(x, R)):sha(x)for x in sources};report['inputs_unchanged']=report['inputs_after']==inputs and sha(ROM)==PIN;assert report['inputs_unchanged']
 report['binary_sha256']=sha(binary);report['passed']=True;report['scope']=['Full original start_load 791848..7919d0 and worker 68be84..68c0ac normal paths execute; full mode-change721da8..722a24 executes its non-laptop normal path.','Allocator/channel/enqueue/cancel/log/switch_db/start_load_inner are explicit adapters. No real Tokio task scheduling, mode-switch SQL, migration, Android runtime or production patch is exercised.','Original save_device_param/getters/Once initializer execute, demonstrating old values remain until explicit original later publication. This is not execution of full start_load_inner migration.','Four parameter fixtures retain cell_x_min/cell_y_min unchanged; module input selection and Dart geometry are not implemented here.','Mode-change window contains new mode/new epoch and not-yet-called DB switch; the pool value is an explicit marker, not a live pool.'];save()
if __name__=='__main__':main()

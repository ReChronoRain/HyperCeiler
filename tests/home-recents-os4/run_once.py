#!/usr/bin/env python3
"""Execute exact vendor Once and its actual argument shims with two real ARM threads."""
from pathlib import Path
import argparse,hashlib,json,os,subprocess

import sys
sys.path.insert(0, str(Path(__file__).resolve().parents[1]))
import hyperceiler_test_paths as test_paths

ROOT=test_paths.TEST_DATA_ROOT;HERE=Path(__file__).resolve().parent
EMU=test_paths.BUILD_CACHE / 'arm64-emulation/root'
ROM=test_paths.ROM_ROOT / 'launcher-native/lib/arm64-v8a/libapp_launcher.so'
sha=lambda p:hashlib.sha256(p.read_bytes()).hexdigest()
def main():
    p=argparse.ArgumentParser(description=__doc__);p.add_argument('--output',required=True,type=Path);a=p.parse_args();out=a.output.resolve();out.mkdir(parents=True,exist_ok=False)
    inputs=[Path(__file__).resolve(),HERE/'once_test.cpp',ROM];before={str(p):sha(p)for p in inputs}
    env=os.environ.copy();env['LD_LIBRARY_PATH']=str(EMU/'usr/lib/x86_64-linux-gnu');commands=[]
    def run(name,args):
        r=subprocess.run(list(map(str,args)),env=env,cwd=ROOT,text=True,capture_output=True,timeout=45)
        (out/(name+'.stdout')).write_text(r.stdout);(out/(name+'.stderr')).write_text(r.stderr);commands.append(dict(name=name,argv=list(map(str,args)),exit=r.returncode));return r
    binary=out/'once-arm64'
    build=run('compile',[EMU/'usr/bin/aarch64-linux-gnu-g++-13','--sysroot='+str(EMU),'-std=c++20','-O2','-pthread',HERE/'once_test.cpp','-o',binary]);assert build.returncode==0
    command=[EMU/'usr/bin/qemu-aarch64-static','-cpu','max','-L',EMU/'usr/aarch64-linux-gnu',binary,ROM]
    positive=run('original',command);assert positive.returncode==0 and 'PASSED=9 FAILED=0' in positive.stdout
    negative=[]
    for name,witness in [('still-cold','actual owner initializer begins only after Once becomes running1'),('publish-cold','original completion releases initialized0')]:
        r=run(name,command+[name]);negative.append(dict(name=name,rejected=r.returncode!=0 and 'FAIL '+witness in r.stderr,witness=witness))
    unchanged=before=={str(p):sha(p)for p in inputs}
    report=dict(passed=unchanged and all(x['rejected']for x in negative),inputs=before,inputs_unchanged=unchanged,checks=9,negative_controls=negative,commands=commands,
        actual_vendor_functions={'get_or_init':'0x4f0890/68','Once::call':'0xd8e0fc/708','outlined_shims':['0xdd6628/12','0xdd64f0/16','0xdd63d4/20']},
        observations=['cold3','initializer sees running1','second thread waiter5','only one initializer','release initialized0','cached fastpath'],
        adapters=['initializer body replaced by a paused tuple publisher','syscall and errno imports bound to QEMU guest libc'],
        device_status='NOT_TESTED',limitations=['Controlled native scheduling is not Android app scheduling','Original app owner allocation/provider initialization tested separately','Does not establish absence of arbitrary external callbacks before app_entry'])
    (out/'report.json').write_text(json.dumps(report,indent=2)+'\n');print(json.dumps(dict(passed=report['passed'],checks=9,negative_controls=len(negative))));assert report['passed']
if __name__=='__main__':main()

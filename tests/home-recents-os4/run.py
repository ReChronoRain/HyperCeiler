#!/usr/bin/env python3
"""Run actual Recents policy/gateway plus mapped original ARM owner, not a phone."""
import argparse
import hashlib
import json
import os
from pathlib import Path
import re
import subprocess

import sys
sys.path.insert(0, str(Path(__file__).resolve().parents[1]))
import hyperceiler_test_paths as test_paths


HERE=Path(__file__).resolve().parent
ROOT=test_paths.TEST_DATA_ROOT
CPP=test_paths.SOURCE_ROOT / 'app/src/main/cpp'
PROD=CPP/'targets/home'
ARM=test_paths.BUILD_CACHE / 'arm64-emulation/root'
NDK=test_paths.ANDROID_NDK_HOME / 'toolchains/llvm/prebuilt/linux-x86_64/bin'
RUST=test_paths.ROM_ROOT / 'launcher-native/lib/arm64-v8a/libapp_launcher.so'
DART=RUST.with_name('libapp.so')
def sha(p):return hashlib.sha256(p.read_bytes()).hexdigest()

def main():
    ap=argparse.ArgumentParser();ap.add_argument('--output',type=Path,required=True);args=ap.parse_args()
    out=args.output.resolve();out.mkdir(parents=True,exist_ok=False)
    inputs=[*sorted(HERE.glob('*.cpp')),*sorted(HERE.glob('*.S')),Path(__file__),
        PROD/'recents_runtime.h',PROD/'recents_policy.h',PROD/'recents_gateway_arm64.S',
        CPP/'targets/hyos/hyos_snapshot.h',PROD/'home_layout_config.h',PROD/'home_layout_knobs.h',RUST,DART]
    before={str(p):sha(p) for p in inputs}
    assert before[str(RUST)]=='4dc9fa82371dda8a0a8b9eff48301469d2f84752b52b71635b84e5fc0c47fa6e'
    assert before[str(DART)]=='a37339e922e0dbebda37d0e10aba599bdec449a0bb8d170305b6cb7d5cfec992'
    env=dict(os.environ,LD_LIBRARY_PATH=str(ARM/'usr/lib/x86_64-linux-gnu'))
    commands=[]
    def command(label,cmd,want):
        p=subprocess.run(list(map(str,cmd)),env=env,cwd=ROOT,capture_output=True,text=True,timeout=90)
        (out/(label+'.stdout')).write_text(p.stdout);(out/(label+'.stderr')).write_text(p.stderr)
        commands.append(dict(label=label,argv=list(map(str,cmd)),returncode=p.returncode,expected=want))
        assert p.returncode==want,(label,p.returncode,p.stderr[-3000:])
        return p.stdout+p.stderr
    cxx=[ARM/'usr/bin/aarch64-linux-gnu-g++-13','--sysroot='+str(ARM),'-std=c++20','-O2','-pthread','-Wall','-Wextra','-Werror','-I'+str(CPP)]
    qemu=[ARM/'usr/bin/qemu-aarch64-static','-cpu','max','-L',ARM/'usr/aarch64-linux-gnu']
    sources={'owner':[HERE/'owner_harness.cpp',HERE/'research_dart_probe.S'],
             'abi':[HERE/'abi_test.cpp',HERE/'abi_probe.S'], 'policy':[HERE/'policy_test.cpp']}
    def build(name,kind,asm=PROD/'recents_gateway_arm64.S'):
        binary=out/name;cmd=cxx+sources[kind]+([asm] if kind!='policy' else [])+['-o',binary]
        command(name+'-compile',cmd,0);return binary
    def run(name,binary,kind,want=0,extra=None):
        return command(name,qemu+[binary]+([RUST,DART] if kind=='owner' else [])+(extra or []),want)
    counts={}
    for kind in sources:
        binary=build(kind+'-arm64',kind);text=run(kind+'-positive',binary,kind)
        m=re.search(r'RESULT (\d+) passed, (\d+) failed',text);assert m and int(m[2])==0 and int(m[1])>0
        counts[kind]=int(m[1])
    assert counts['owner']==2354 and counts['abi']==8208
    mutations=[
        ('early-query-late-arm',' casal x12,x13,[x17]',' nop', 'owner','actual early gateway prevents late activation'),
        ('clobber-x16',' ldp x16,x17,[sp,#80]',' ldp x16,x17,[sp,#80]\n mov x16,#0','abi','all x0..x28'),
        ('clobber-x17',' ldp x16,x17,[sp,#80]',' ldp x16,x17,[sp,#80]\n mov x17,#0','abi','all x0..x28'),
        ('clear-q0-upper',' ins v0.s[0],w14',' fmov s0,w14','abi','all128bit SIMD'),
        ('skip-displaced-store',' str s0,[x29,#0x1c]',' nop','abi','displaced native store'),
        ('wrong-clock-syscall',' mov x8,#113',' mov x8,#114','owner','actual production gateway full'),
        ('shrink-maximum','.float 2.00','.float 1.00','owner','actual production gateway full'),
    ]
    controls=[];original=(PROD/'recents_gateway_arm64.S').read_text()
    for name,old,new,kind,witness in mutations:
        assert old in original,name
        changed=out/(name+'.S');changed.write_text(original.replace(old,new,1))
        text=run(name,build(name+'-arm64',kind,changed),kind,1)
        m=re.search(r'RESULT (\d+) passed, (\d+) failed',text)
        assert m and int(m[2])>0 and any(line.startswith('FAIL '+witness) for line in text.splitlines()),(name,witness)
        controls.append(dict(name=name,rejected=True,failures=int(m[2]),witness=witness,mutated_assembly_sha256=sha(changed)))
    for name,witness in [('skip-clamp',"actual Rust owner normalize '2'"),('skip-publication','actual Rust owner normalize'),('skip-cache','original Folme getter retains cached ratio')]:
        text=run(name,out/'owner-arm64','owner',1,[name]);m=re.search(r'RESULT (\d+) passed, (\d+) failed',text)
        assert m and int(m[2])>0 and any(line.startswith('FAIL '+witness) for line in text.splitlines())
        controls.append(dict(name=name,rejected=True,failures=int(m[2]),witness=witness,mutation_scope='actual vendor word in anonymous fixture mapping'))
    command('ndk-assembly',[NDK/'aarch64-linux-android35-clang++','-fPIC','-c',PROD/'recents_gateway_arm64.S','-o',out/'gateway-android.o'],0)
    command('ndk-policy',[NDK/'aarch64-linux-android35-clang++','-std=c++20','-fPIC','-fno-exceptions','-I'+str(CPP),'-c',HERE/'policy_test.cpp','-o',out/'policy-android.o'],0)
    unchanged={str(p):sha(p) for p in inputs}==before;assert unchanged
    report=dict(passed=True,status='PRODUCTION_COMPONENT_CONTROLLED_ARM64_NOT_INSTALLED_OR_DEVICE',
        device_status='NOT_TESTED',inputs=before,inputs_unchanged=unchanged,positive_checks=counts,
        negative_controls=controls,commands=commands,
        boundaries=['The actual production policy and copyable assembly execute. The runtime installer is tested separately and is not invoked by this suite.',
            'The exact vendor updater/parser/5 callbacks and Folme cache getter execute in anonymous mapped ROM bytes; provider, String disposal, simple-mode decision and Dart bridge/heap are explicit fixtures.',
            'ABI cases exercise all integer registers, SP, NZCV, FPCR, FPSR and full128bit SIMD, including selected low32 of v0. They do not establish asynchronous signal/unwind or actual Android executable-page lifetime.',
            'clock_gettime runs through real SVC113 under Linux/QEMU; this is not proof of the target Home thread seccomp/SELinux permission.',
            'Policy concurrency tests execute real atomic arm/early CAS on threads but do not simulate or prove OEM lifecycle scheduling.',
            'Cold restart is required after selection change, denial, lease lapse or missed initialization. Existing object caches are not immediately rolled back.'])
    (out/'report.json').write_text(json.dumps(report,indent=2)+'\n')
    print(json.dumps(dict(passed=True,counts=counts,negative_controls=len(controls),report=str(out/'report.json')),indent=2))
if __name__=='__main__':main()

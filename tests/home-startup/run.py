#!/usr/bin/env python3
"""Compile actual startup owner/gateways, fault tests and source mutation controls."""
import argparse, hashlib, json, os, pathlib, resource, subprocess

import sys
from pathlib import Path
sys.path.insert(0, str(Path(__file__).resolve().parents[1]))
import hyperceiler_test_paths as test_paths

ROOT=pathlib.Path(__file__).resolve().parents[2]
BASE=ROOT.parent
CPP=ROOT/'app/src/main/cpp'
TEST=ROOT/'tests/home-startup'
ARM=test_paths.BUILD_CACHE / 'arm64-emulation/root'

def main():
    parser=argparse.ArgumentParser(description=__doc__);parser.add_argument('--output',type=pathlib.Path,required=True)
    args=parser.parse_args();out=args.output.resolve();out.mkdir(parents=True,exist_ok=False)
    resource.setrlimit(resource.RLIMIT_CORE,(0,0))
    env=os.environ.copy();env['LD_LIBRARY_PATH']=str(ARM/'usr/lib/x86_64-linux-gnu')
    env.pop('HOME_STARTUP_TEST_FILTER',None);env.pop('HOME_STARTUP_TEST_LIMIT',None)
    report={'passed':False,'results':[],'source_files':{}}
    inputs=list(TEST.glob('*'))+[CPP/'targets/home/home_startup.cpp',CPP/'targets/home/home_startup.h',
        CPP/'targets/home/home_startup_arm64.S',CPP/'targets/home/home_startup_plan.generated.h',
        CPP/'targets/home/home_layout_hooks.cpp',CPP/'targets/home/tweaks/symtab.cpp',
        CPP/'targets/home/home_layout_config.h',CPP/'targets/home/home_layout_knobs.h',
        CPP/'nativehook/elf_image.h',CPP/'nativehook/nhk_base.h']
    initial_hashes={str(test_paths.report_path(p, ROOT)):hashlib.sha256(p.read_bytes()).hexdigest() for p in inputs if p.is_file()}
    report['source_files']=initial_hashes
    image=BASE/'firmware/launcher-native/lib/arm64-v8a/libapp.so'
    report['rom_image_sha256']=hashlib.sha256(image.read_bytes()).hexdigest()
    def run(name,cmd,expect=0):
        result=subprocess.run([str(x) for x in cmd],capture_output=True,text=True,env=env)
        (out/(name+'.stdout')).write_text(result.stdout);(out/(name+'.stderr')).write_text(result.stderr)
        okay=result.returncode==0 if expect==0 else result.returncode!=0
        item={'name':name,'returncode':result.returncode,'expected':'success' if expect==0 else 'failure','passed':okay,
              'command':[str(x) for x in cmd]}
        for line in reversed(result.stdout.splitlines()):
            if line.startswith('{'):
                try:item['summary']=json.loads(line);break
                except ValueError:pass
        report['results'].append(item);print(name, 'PASS' if okay else 'FAIL',flush=True)
        (out/'report.json').write_text(json.dumps(report,indent=2)+'\n')
        if not okay:raise RuntimeError(name+' unexpected result; inspect stderr/stdout')
        return item
    production=CPP/'targets/home/home_startup.cpp';text=production.read_text()
    def build(name,src,fixture,macro=True):
        binary=out/name
        cmd=['c++','-std=c++20','-O2','-Wall','-Wextra','-Werror','-pthread','-I'+str(CPP),'-I'+str(production.parent)]
        if macro:cmd+=['-DHOME_STARTUP_TEST']
        cmd += [TEST/fixture,src,'-o',binary]
        run(name+'-compile',cmd);return binary
    host=build('executor',production,'executor_test.cpp')
    assert run('executor-run',[host,image])['summary']=={'tests':984,'passed':984,'failed':0}
    osm=build('os-memory',production,'os_memory_test.cpp',False)
    run('os-memory-run',[osm,image])
    changes={
      'skip-full-guards': [('if (!guards()) return {Outcome::Declined, mask, 0, mask, Error::GuardMismatch};',
                            'if (false && !guards()) return {Outcome::Declined, mask, 0, mask, Error::GuardMismatch};')],
      'stale-publish-authorization': [
          ('detail::publish_consumers(config, mask & ~kUnsupported);\n    if (!live(generation, request.still_authorized, request.context)) return failure(Error::Revoked, mask, true);\n    if (!activate(generation)) return failure(Error::Revoked, mask, true);\n    if (!live(generation, request.still_authorized, request.context)) return failure(Error::Revoked, mask, true);',
           'detail::publish_consumers(config, mask & ~kUnsupported);\n    if (!activate(generation)) return failure(Error::Revoked, mask, true);')],
      'skip-rollback-words': [('const bool restored = write_words(true) == Error::None;','const bool restored = true;')],
      'ignore-rx-failure': [('return wrote && restored;','return wrote;'),
                          ('else okay = false;','else page.protection_pending = false;')],
      'partial-phone-grid': [('constexpr uint64_t kUnsupported = PhoneGridDart | PhoneGridRust | PadGrid | FoldGrid | RecentsAnimationRust;',
                              'constexpr uint64_t kUnsupported = PhoneGridRust | PadGrid | FoldGrid | RecentsAnimationRust;')],
      'partial-pad-fold-grid': [('constexpr uint64_t kUnsupported = PhoneGridDart | PhoneGridRust | PadGrid | FoldGrid | RecentsAnimationRust;',
                              'constexpr uint64_t kUnsupported = PhoneGridDart | PhoneGridRust | RecentsAnimationRust;')],
      'skip-tail-rollback-guard': [('const bool pristine = !unhook || (hooks && guards());','const bool pristine = true;')],
      'skip-backend-length-check': [('if (!detour_guards()) return Error::ForeignCode;',
                                    'if (false && !detour_guards()) return Error::ForeignCode;')],
    }
    filters={'skip-full-guards':'guard mutation','stale-publish-authorization':'changed authorization after publish',
        'skip-rollback-words':'write-after-mutation failure','ignore-rx-failure':'RX restoration failure',
        'partial-phone-grid':'production planner variant 0:',
        'partial-pad-fold-grid':'pad/fold model composite refusal',
        'skip-tail-rollback-guard':'unhook success cannot hide foreign tail',
        'skip-backend-length-check':'backend exceeding 16-byte contract'}
    for name,patches in changes.items():
        changed=text
        for old,new in patches:
            assert changed.count(old)==1,(name,old);changed=changed.replace(old,new)
        src=out/(name+'.cpp');src.write_text(changed)
        binary=build(name,src,'executor_test.cpp')
        env['HOME_STARTUP_TEST_FILTER']=filters[name]
        env['HOME_STARTUP_TEST_LIMIT']='1'
        item=run(name+'-run',[binary,image],1)
        del env['HOME_STARTUP_TEST_FILTER']
        del env['HOME_STARTUP_TEST_LIMIT']
        assert item.get('summary',{}).get('failed',0)>0, 'Mutation must be rejected by test assertion, not merely crash main'
    assembly=CPP/'targets/home/home_startup_arm64.S';asm=assembly.read_text()
    def arm_build_run(name,source,negative=False):
        binary=out/name
        run(name+'-compile',[ARM/'usr/bin/aarch64-linux-gnu-g++-13','--sysroot='+str(ARM),'-std=c++20','-O2',
            '-Wall','-Wextra','-Werror','-static',TEST/'gateway_test.cpp',TEST/'gateway_probe.S',source,'-o',binary])
        run(name+'-run',[ARM/'usr/bin/qemu-aarch64-static',binary],1 if negative else 0)
    arm_build_run('arm-gateway',assembly)
    arm_mutations={
        'clobber-x9': ('    ldar x16, [x16]','    ldar x16, [x16]\n    mov x9, #0'),
        'clobber-flags': ('    tbz x16, #0, 1f','    tst x16, #1\n    b.eq 1f'),
        'ignore-disabled': ('    tbz x16, #0, 1f','    nop'),
    }
    for name,(old,new) in arm_mutations.items():
        assert asm.count(old)==1;path=out/(name+'.S');path.write_text(asm.replace(old,new));arm_build_run(name,path,True)
    report['inputs_unchanged']=all(hashlib.sha256((ROOT/path).read_bytes()).hexdigest()==value
        for path,value in initial_hashes.items())
    report['inputs_unchanged'] &= hashlib.sha256(image.read_bytes()).hexdigest()==report['rom_image_sha256']
    assert report['inputs_unchanged'], 'Production/test input changed during test run'
    report['limits']=[
        'ROM AArch64 instructions are mapped as data in executor/OS tests, not executed.',
        'ARM test executes real production gateways with controlled target/original destinations; not the full Dart runtime.',
        'Real OS syscall tests run on Linux host; actual Android SELinux/process_vm_readv and ZygiskNext inline backend remain device acceptance.',
        'Fakes cover memory/backend failure and deterministic scheduling, not all interleavings or hard latency bounds.'
    ]
    report['passed']=all(r['passed'] for r in report['results'])
    (out/'report.json').write_text(json.dumps(report,indent=2)+'\n')
    print(json.dumps({'passed':report['passed'],'report':str(out/'report.json')}))
if __name__=='__main__':main()

#!/usr/bin/env python3
"""Exercise the experimental owner against the pinned ROM as host data."""
import argparse
import hashlib
import json
import os
from pathlib import Path
import resource
import shutil
import subprocess
import sys
sys.dont_write_bytecode=True
from generate_plan import generate, SOURCE, BASE, digest
from prepare import prepare, once

def main():
    ap=argparse.ArgumentParser(description=__doc__);ap.add_argument('--output',type=Path,required=True)
    args=ap.parse_args();out=args.output.resolve();out.mkdir(parents=True,exist_ok=False)
    resource.setrlimit(resource.RLIMIT_CORE,(0,0))
    plan=generate(out);inputs=prepare(out)
    for p in (Path(__file__),Path(__file__).with_name('generate_plan.py')):
        inputs[str(p.relative_to(SOURCE))]=digest(p)
    image=BASE/'firmware/launcher-native/lib/arm64-v8a/libapp.so'
    consumed=[image,
        BASE/'acceptance/home-startup-next/generated-plan-v5/production-plans.jsonl',
        SOURCE/'app/src/main/cpp/targets/home/home_startup_plan.generated.h',
        SOURCE/'app/src/main/cpp/targets/home/home_startup.h',
        SOURCE/'app/src/main/cpp/targets/home/home_layout_config.h',
        SOURCE/'app/src/main/cpp/targets/home/home_layout_knobs.h',
        SOURCE/'app/src/main/cpp/nativehook/elf_image.h',
        SOURCE/'app/src/main/cpp/nativehook/nhk_base.h',
        Path(shutil.which('c++')).resolve()]
    for p in consumed: inputs[str(p.resolve())]=digest(p)
    report=dict(status='IN_PROGRESS_EXPERIMENTAL_OWNER',device_status='NOT_TESTED',passed=False,
        plan=plan,source_inputs=inputs,results=[],production_changed=False)
    env=os.environ.copy();env.pop('HOME_STARTUP_TEST_FILTER',None);env.pop('HOME_STARTUP_TEST_LIMIT',None)
    def run(name,cmd,negative=False):
        p=subprocess.run([str(v) for v in cmd],capture_output=True,text=True,env=env)
        (out/(name+'.stdout')).write_text(p.stdout);(out/(name+'.stderr')).write_text(p.stderr)
        summary=None
        for line in p.stdout.splitlines():
            if line.startswith('{'):
                try: summary=json.loads(line)
                except ValueError: pass
        okay=(p.returncode!=0) if negative else (p.returncode==0)
        item=dict(name=name,returncode=p.returncode,passed=okay,negative_control=negative,
                  summary=summary,command=[str(v) for v in cmd])
        report['results'].append(item)
        (out/'report.json').write_text(json.dumps(report,indent=2)+'\n')
        print(name,'PASS' if okay else 'FAIL',summary,flush=True)
        assert okay,name
        if negative: assert summary and summary['failed']>0,'Require assertion failure, not runner/build error'
        return item
    def build(name,src):
        binary=out/name
        run(name+'-compile',['c++','-std=c++20','-O2','-Wall','-Wextra','-Werror','-pthread','-DHOME_STARTUP_TEST',
            '-I'+str(out),'-I'+str(SOURCE/'app/src/main/cpp'),
            '-I'+str(SOURCE/'app/src/main/cpp/targets/home'),out/'executor_test.cpp',src,'-o',binary])
        return binary
    owner=out/'home_startup.cpp'
    binary=build('owner',owner)
    positive=run('owner-run',[binary,image])
    expected_count=1018-101+plan['guard_ranges']+22
    assert positive['summary']=={'tests':expected_count,'passed':expected_count,'failed':0},'Exact positive suite must execute'
    report['positive_suite_expected']=expected_count
    text=owner.read_text()
    changes={
        'late-row-key':('if ((supported & PhoneGridDart) && grid_key(config) != g_state.initial_grid_key)',
                        'if (false && (supported & PhoneGridDart) && grid_key(config) != g_state.initial_grid_key)',
                        'row-only late change'),
        'shared-key-not-cleared':('__atomic_store_n(&hc_home_grid_config_key, uint64_t{0}, __ATOMIC_RELEASE);',
                                  '(void) hc_home_grid_config_key;','phone shared key is published'),
        'feature-ignored':('(applied & PhoneGridDart) && config.grid_enabled ? grid_key(config) : 0',
                           'grid_key(config)','phone feature disabled while'),
        'skip-full-guards':('if (!guards()) return {Outcome::Declined, mask, 0, mask, Error::GuardMismatch};',
                            'if (false && !guards()) return {Outcome::Declined, mask, 0, mask, Error::GuardMismatch};','guard mutation'),
        'skip-rollback-words':('const bool restored = write_words(true) == Error::None;',
                               'const bool restored = true;','write-after-mutation failure'),
        'skip-backend-length':('if (!detour_guards()) return Error::ForeignCode;',
                               'if (false && !detour_guards()) return Error::ForeignCode;','backend exceeding 16-byte'),
        'unhook-after-admission':('unhook = unhook && !g_state.ever_activated;',
                                  'unhook = unhook;','admitted frame keeps resident hooks'),
    }
    for name,(old,new,focus) in changes.items():
        src=out/(name+'.cpp');src.write_text(once(text,old,new))
        binary=build(name,src)
        env['HOME_STARTUP_TEST_FILTER']=focus;env['HOME_STARTUP_TEST_LIMIT']='1'
        run(name+'-run',[binary,image],True)
        del env['HOME_STARTUP_TEST_FILTER'];del env['HOME_STARTUP_TEST_LIMIT']
    report['inputs_unchanged']=all(digest(SOURCE/p)==v for p,v in inputs.items())
    assert report['inputs_unchanged']
    report['outputs']={p.name:digest(p) for p in out.iterdir() if p.is_file() and p.name!='report.json'}
    report['limits']=[
        'Candidate owner is derived from production; production code and deliverable APK remain unchanged.',
        'ROM is mapped as host data. Hook backend is a fault/scheduling adapter; no actual ROM instructions execute here.',
        'The resident router implementations are independently tested and not linked into this host ownership fixture.',
        'No physical OS4, LSPosed/Zygisk backend, Dart GC, UI, async database, or long-duration execution claimed.',
        'Revocation closes admission; existing cached results require a launcher process restart.']
    report['passed']=all(v['passed'] for v in report['results'])
    report['status']='EXPERIMENTAL_OWNER_HOST_PASS_NOT_INSTALLED'
    (out/'report.json').write_text(json.dumps(report,indent=2)+'\n')
    print(json.dumps({'passed':report['passed'],'sha256':digest(out/'report.json')}))

if __name__=='__main__':main()

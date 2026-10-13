#!/usr/bin/env python3
"""Actual shared controller, complete HCS cache and real Recents installer in one process."""
from pathlib import Path
import argparse,hashlib,importlib.util,json,re,subprocess

import sys
sys.path.insert(0, str(Path(__file__).resolve().parents[1]))
import hyperceiler_test_paths as test_paths

ROOT=test_paths.TEST_DATA_ROOT;HERE=Path(__file__).resolve().parent;CPP=test_paths.SOURCE_ROOT / 'app/src/main/cpp'
spec=importlib.util.spec_from_file_location('recents_linked_template',HERE/'run_installer.py');helper=importlib.util.module_from_spec(spec);spec.loader.exec_module(helper)
sha=lambda p:hashlib.sha256(p.read_bytes()).hexdigest()
def main():
    p=argparse.ArgumentParser(description=__doc__);p.add_argument('--output',required=True,type=Path);a=p.parse_args();out=a.output.resolve();out.mkdir(parents=True,exist_ok=False)
    sdk=test_paths.TEST_DATA_ROOT / 'acceptance/runtime-routing-next/zygisk-next-api'
    stubs=test_paths.SOURCE_ROOT / 'tests/hyos-control/stubs'
    inputs=[Path(__file__).resolve(),HERE/'joint_test.cpp',HERE/'run_installer.py',helper.ROM,helper.EXECUTED,sdk/'zygisk_next_api.h',*sorted(stubs.rglob('*.h')),
        *sorted((CPP/'targets/home').glob('recents_*')),
        *[CPP/'targets/hyos'/n for n in ['hyos_control.cpp','hyos_control.h','hyos_entry.h','hyos_report_runtime.h','hyos_snapshot.h']],
        *[CPP/'targets/home'/n for n in ['native_home_entry.h','home_startup.h','home_layout_config.cpp','home_layout_config.h','home_layout_knobs.h']]]
    before={str(p):sha(p)for p in inputs};commands=[]
    def run(name,command,expect=True):
        r=subprocess.run(list(map(str,command)),cwd=ROOT,capture_output=True,text=True,timeout=90)
        (out/(name+'.stdout')).write_text(r.stdout);(out/(name+'.stderr')).write_text(r.stderr);commands.append(dict(name=name,argv=list(map(str,command)),exit=r.returncode))
        if expect:assert r.returncode==0,(name,r.stderr[-3000:])
        return r
    run('ndk-template',[helper.NDK/'aarch64-linux-android35-clang','-shared','-nostdlib','-Wl,--no-undefined','-Wl,--build-id=none',CPP/'targets/home/recents_gateway_arm64.S','-o',out/'template.so'])
    template,sections=helper.linked_template(out/'template.so');assert not any(s[1]in(4,9) and s[5]for s in sections)
    assert template==helper.linked_template(helper.EXECUTED)[0]
    (out/'template.bin').write_bytes(template)
    assembly=out/'template.S';assembly.write_text('.section .rodata\n.global hc_home_recents_template,hc_home_recents_return_branch,hc_home_recents_lease_literal,hc_home_recents_template_end\nhc_home_recents_template:\n.incbin "'+str(out/'template.bin')+'",0,284\nhc_home_recents_return_branch:\n.incbin "'+str(out/'template.bin')+'",284,4\nhc_home_recents_lease_literal:\n.incbin "'+str(out/'template.bin')+'",288,692\nhc_home_recents_template_end:\n.section .note.GNU-stack,"",@progbits\n')
    def build(name,fixture):
        binary=out/name
        run(name+'-compile',['g++','-std=c++20','-O2','-Wall','-Wextra','-Werror','-pthread','-DHC_REAL_CONFIG','-DHYPERCEILER_HYOS_BACKEND',
            '-I'+str(sdk),'-I'+str(stubs),'-I'+str(CPP),'-I'+str(CPP/'targets/hyos'),'-I'+str(CPP/'targets/home'),fixture,
            CPP/'targets/home/home_layout_config.cpp',assembly,'-o',binary]);return binary
    positive=run('joint',[build('joint-host',HERE/'joint_test.cpp'),helper.ROM]);assert 'SCENARIOS=11 FAILED=0' in positive.stdout
    assert len(re.findall(r'^PASS ',positive.stdout,re.M))==11
    controller=CPP/'targets/hyos/hyos_control.cpp';installer=CPP/'targets/home/recents_runtime.cpp'
    mutations=[
        ('remove-service',controller,'home_recents::service();','(void)0;','same-selection'),
        ('remove-watchdog-cancel',controller,'home_recents::cancel();','(void)0;','watchdog'),
        ('prephase-late-arm',installer,'if(!writer.owns){g_lease.cancel();cancel();return false;}','if(!writer.owns){cancel();return false;}','prephase-entry'),
        ('late-revision',installer,'if(!same(initial))return fail(Error::Cancelled,true);\n    auto installing','if(false)return fail(Error::Cancelled,true);\n    auto installing','install-revision'),
    ]
    controls=[]
    for name,source,old,new,witness in mutations:
        text=source.read_text();assert old in text
        mutant=out/(name+'.cpp');mutant.write_text(text.replace(old,new));fixture=out/(name+'-fixture.cpp')
        include='targets/hyos/hyos_control.cpp'if source==controller else'targets/home/recents_runtime.cpp'
        fixture.write_text((HERE/'joint_test.cpp').read_text().replace('"'+include+'"','"'+str(mutant)+'"'))
        r=run(name,[build(name+'-host',fixture),helper.ROM],False)
        controls.append(dict(name=name,rejected=r.returncode!=0 and 'FAIL '+witness+':'in r.stderr,witness=witness,source_sha256=sha(mutant)))
    unchanged=before=={str(p):sha(p)for p in inputs}
    report=dict(passed=unchanged and all(x['rejected']for x in controls),inputs=before,inputs_unchanged=unchanged,
        scenarios=11,negative_controls=controls,commands=commands,device_status='NOT_TESTED',
        actual_production=['hyos_control.cpp poll_flags/query_flags/publish_reply/expire_lease/read_snapshot','complete hyos_snapshot Cache/decoder','recents_runtime.cpp','recents_binding and policy','home_layout_config.cpp'],
        actual_host_operations=['threads','mmap','process_vm_readv','mprotect','mlock','smaps','aligned atomic CAS'],
        adapters=['Android Binder transport and lifecycle worker launch','BOOTTIME clock','PinImage original-loader reference','HWCAP admission on x86','legacy startup/report/native_init backends'],
        not_proven=['Hyos entry injection and Android Binder','Dart or ARM code execution by this host suite','device animation, target memlock/SELinux or indefinite stability'],
        template={'sha256':sha(out/'template.bin'),'matches_executed_arm':True,'runtime_link_relocations':0})
    (out/'report.json').write_text(json.dumps(report,indent=2)+'\n');print(json.dumps(dict(passed=report['passed'],scenarios=11,negative_controls=len(controls))));assert report['passed']
if __name__=='__main__':main()

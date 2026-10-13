#!/usr/bin/env python3
"""Real production installer and exact-ROM binding; host queue/events are explicit fixtures."""
from pathlib import Path
import argparse,hashlib,json,re,struct,subprocess

import sys
sys.path.insert(0, str(Path(__file__).resolve().parents[1]))
import hyperceiler_test_paths as test_paths

ROOT=test_paths.TEST_DATA_ROOT
CPP=test_paths.SOURCE_ROOT / 'app/src/main/cpp'
HERE=Path(__file__).resolve().parent
ROM=test_paths.ROM_ROOT / 'launcher-native/lib/arm64-v8a/libapp_launcher.so'
NDK=test_paths.ANDROID_NDK_HOME / 'toolchains/llvm/prebuilt/linux-x86_64/bin'
sha=lambda p:hashlib.sha256(p.read_bytes()).hexdigest()
EXECUTED=test_paths.TEST_DATA_ROOT / 'acceptance/home-recents-next/test10-v1/components-v2/abi-arm64'

def linked_template(path):
    data=path.read_bytes();assert data[:6]==b'\x7fELF\x02\x01' and struct.unpack_from('<H',data,16)[0] in (2,3)
    shoff=struct.unpack_from('<Q',data,40)[0];shsize,n=struct.unpack_from('<HH',data,58);assert shsize==64
    sections=[struct.unpack_from('<IIQQQQIIQQ',data,shoff+i*shsize)for i in range(n)]
    symbols={}
    for s in sections:
        if s[1] not in (2,11):continue
        strings=sections[s[6]];names=data[strings[4]:strings[4]+strings[5]]
        for name,info,other,idx,value,size in struct.iter_unpack('<IBBHQQ',data[s[4]:s[4]+s[5]]):
            if idx:symbols[names[name:names.index(0,name)].decode()]=value
    first=symbols['hc_home_recents_template'];assert symbols['hc_home_recents_template_end']-first==980
    assert symbols['hc_home_recents_return_branch']-first==284 and symbols['hc_home_recents_lease_literal']-first==288
    containing=[s for s in sections if s[1]==1 and s[3]<=first and first+980<=s[3]+s[5]];assert len(containing)==1
    s=containing[0];offset=s[4]+first-s[3]
    return data[offset:offset+980],sections

def main():
    ap=argparse.ArgumentParser(description=__doc__);ap.add_argument('--output',type=Path,required=True);a=ap.parse_args();out=a.output.resolve();out.mkdir(parents=True,exist_ok=False)
    paths=[Path(__file__).resolve(),HERE/'runtime_test.cpp',ROM,EXECUTED,CPP/'targets/hyos/hyos_snapshot.h',CPP/'targets/home/home_layout_config.h',*sorted((CPP/'targets/home').glob('recents_*'))]
    before={str(p):sha(p) for p in paths};commands=[]
    def run(name,args,okay=True):
        result=subprocess.run(list(map(str,args)),cwd=ROOT,text=True,capture_output=True,timeout=90)
        (out/(name+'.stdout')).write_text(result.stdout);(out/(name+'.stderr')).write_text(result.stderr)
        commands.append(dict(name=name,argv=list(map(str,args)),exit=result.returncode))
        if okay:assert result.returncode==0,(name,result.stderr[-2000:])
        return result
    run('ndk-runtime',[NDK/'aarch64-linux-android35-clang++','-std=c++20','-fno-exceptions','-fno-rtti','-Wall','-Wextra','-Werror','-I'+str(CPP),'-c',CPP/'targets/home/recents_runtime.cpp','-o',out/'runtime-android.o'])
    run('ndk-template',[NDK/'aarch64-linux-android35-clang','-shared','-nostdlib','-Wl,--no-undefined','-Wl,--build-id=none',CPP/'targets/home/recents_gateway_arm64.S','-o',out/'template.so'])
    linked,sections=linked_template(out/'template.so');executed,_=linked_template(EXECUTED)
    assert not any(s[1] in (4,9) and s[5] for s in sections),'unexpected unresolved template relocations'
    assert linked==executed,'linked NDK and actually executed ARM gateway bytes differ'
    (out/'template.bin').write_bytes(linked)
    template=out/'template.S';template.write_text('.section .rodata\n.global hc_home_recents_template,hc_home_recents_return_branch,hc_home_recents_lease_literal,hc_home_recents_template_end\nhc_home_recents_template:\n.incbin "'+str(out/'template.bin')+'",0,284\nhc_home_recents_return_branch:\n.incbin "'+str(out/'template.bin')+'",284,4\nhc_home_recents_lease_literal:\n.incbin "'+str(out/'template.bin')+'",288,692\nhc_home_recents_template_end:\n.section .note.GNU-stack,"",@progbits\n')
    def build(name,fixture):
        binary=out/name
        run(name+'-compile',['g++','-std=c++20','-O2','-Wall','-Wextra','-Werror','-pthread','-I'+str(CPP),'-I'+str(CPP/'targets/home'),fixture,template,'-o',binary]);return binary
    positive=run('runtime', [build('runtime-host',HERE/'runtime_test.cpp'),ROM]);assert 'SCENARIOS=40 FAILED=0' in positive.stdout
    rows=re.findall(r'^([^\n:]+): passed=(\d+) failed=(\d+)$',positive.stdout,re.M);assert len(rows)==40 and all(int(f)==0 for _,_,f in rows)
    original=(CPP/'targets/home/recents_runtime.cpp').read_text()
    variants=[
        ('cold-cancel', 'if(phase==Phase::Cold)return;', 'if(phase==Phase::Cold){g_lease.cancel();return;}', 'cold-cancel'),
        ('concurrent-late-arm','if(!writer.owns){g_lease.cancel();cancel();return false;}','if(!writer.owns)return false;','thread-prepare'),
        ('prephase-late-arm','if(!writer.owns){g_lease.cancel();cancel();return false;}','if(!writer.owns){cancel();return false;}','prephase-thread'),
        ('skip-pin','if(!pin || !pin(base,g_binding.entry))','if(!pin)', 'pin-failure'),
        ('skip-image','return home_recents_binding::verify(g_binding.base,read_memory,patch);','(void)patch;return true;', 'pin-code-race'),
        ('skip-cold','!home_recents_binding::cold(g_binding,read_memory)','false', 'warm-once'),
        ('omit-lock','mlock(reinterpret_cast<void*>(begin),g_page_size)!=0 || !page_locked(begin)','false','rate30'),
        ('late-revision','if(!same(initial))return fail(Error::Cancelled,true);\n    auto installing','if(false)return fail(Error::Cancelled,true);\n    auto installing','last-revision'),
        ('skip-rollback-full-guard','&& image(home_recents_guard::kOriginalWord);',';','foreign-tail'),
        ('forget-protection-journal','if(g_protection_pending){','g_protection_pending=false;\n    if(g_protection_pending){','foreign-cas-rx'),
        ('service-skip-image','if(!image(g_branch) || !arena_intact() || protection(page(g_binding.patch),g_page_size)!=kRx\n        || !page_locked(page(g_binding.patch)))','if(false)','live-code'),
    ]
    negatives=[]
    for name,old,new,witness in variants:
        assert old in original,name
        mutant=out/(name+'.cpp');mutant.write_text(original.replace(old,new))
        fixture=out/(name+'-fixture.cpp');fixture.write_text((HERE/'runtime_test.cpp').read_text().replace('"targets/home/recents_runtime.cpp"','"'+str(mutant)+'"'))
        result=run(name,[build(name+'-host',fixture),ROM],okay=False)
        match=re.search(r'^'+re.escape(witness)+r': passed=\d+ failed=([1-9]\d*)$',result.stdout,re.M)
        negatives.append(dict(name=name,rejected=result.returncode!=0 and bool(match),witness=witness,source_sha256=sha(mutant)))
    after={str(p):sha(p) for p in paths}
    report=dict(passed=before==after and all(r['rejected'] for r in negatives),inputs=before,inputs_unchanged=before==after,
        scenarios=40,assertions=sum(int(n) for _,n,_ in rows),negative_controls=negatives,commands=commands,
        production_cpp_actual=True,guard_identity_actual=True,real_host_syscalls=['mmap','process_vm_readv','mprotect','mlock','smaps','madvise'],
        adapters=['HCS producer callback','PinImage loader lifetime','HWCAP_ATOMICS admission on x86','selected race/failure scheduling; actual gateway early CAS is tested separately on ARM'],
        template={'sha256':sha(out/'template.bin'),'bytes':980,'linked_et_dyn':True,'no_remaining_relocations':True,'equals_executed_arm_fixture':True,'executed_in_this_host_suite':False},
        device_status='NOT_TESTED',not_proven=['Android loader and SELinux','live Android mlock/resource budget','arbitrary code mappings changed by another module','async unwind of copied native frame'])
    (out/'report.json').write_text(json.dumps(report,indent=2)+'\n');print(json.dumps({k:report[k]for k in ['passed','scenarios','assertions','inputs_unchanged']}))
    assert report['passed'], 'A real negative control or immutable input check failed; retain this report.'
if __name__=='__main__':main()

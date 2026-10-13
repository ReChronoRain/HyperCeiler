#!/usr/bin/env python3
"""Bind completed Recents evidence without upgrading it to a device or APK approval."""
from pathlib import Path
import argparse,hashlib,json

import sys
sys.path.insert(0, str(Path(__file__).resolve().parents[1]))
import hyperceiler_test_paths as test_paths

ROOT=test_paths.TEST_DATA_ROOT
sha=lambda p:hashlib.sha256(p.read_bytes()).hexdigest()
def main():
    p=argparse.ArgumentParser(description=__doc__);p.add_argument('--output',type=Path,required=True);a=p.parse_args();out=a.output.resolve();out.mkdir(parents=True,exist_ok=False)
    relative={
        'exact_owner_lifecycle':'acceptance/home-recents-next/post-test8-v1/static-final-v4/report.json',
        'guards':'acceptance/home-recents-next/test10-v1/guard-generation-v2/report.json',
        'arm_components':'acceptance/home-recents-next/test10-v1/components-v3/report.json',
        'original_once':'acceptance/home-recents-next/test10-v1/once-v1/report.json',
        'installer':'acceptance/home-recents-next/test10-v1/installer-v3/report.json',
        'joint':'acceptance/home-recents-next/test10-v1/joint-v1/report.json',
        'page_lifetime':'acceptance/home-recents-next/page-lifetime-test10-v1/verified-v1/report.json',
        'seccomp':'acceptance/home-recents-next/page-lifetime-independent-v1/seccomp-v2/report.json',
        'independent_installer_replay':'acceptance/home-recents-next/test10-independent-apk-audit/installer-replay-v1/report.json',
    }
    reports={name:json.loads((ROOT/path).read_text())for name,path in relative.items()}
    assert all(r['passed'] is True for r in reports.values())
    production=sorted((test_paths.SOURCE_ROOT / 'app/src/main/cpp/targets/home').glob('recents_*'))
    assert len(production)==6
    sources={str(test_paths.report_path(path, ROOT)):sha(path)for path in production}
    for path in production:
        for key in ['installer','joint','independent_installer_replay']:
            assert reports[key]['inputs'][str(path)]==sha(path),(key,path)
    assert reports['guards']['header']['sha256']==sha(test_paths.SOURCE_ROOT / 'app/src/main/cpp/targets/home/recents_guard.generated.h')
    assert reports['installer']['scenarios']==40 and len(reports['installer']['negative_controls'])==11
    assert reports['joint']['scenarios']==11 and len(reports['joint']['negative_controls'])==4
    assert reports['arm_components']['positive_checks']==dict(owner=2354,abi=8208,policy=398)
    assert reports['original_once']['checks']==9
    supporting=[test_paths.SOURCE_ROOT / 'library/core/src/main/res/xml/home_animation.xml',
        test_paths.SOURCE_ROOT / 'library/libhook/src/main/java/com/sevtinge/hyperceiler/libhook/rules/home/dock/HomeLayoutNativeEndpointOS4.java']
    xml=supporting[0].read_text();assert 'app:quick_restart="com.miui.home"'in xml
    keys=['prefs_key_home_animation_recents_enable','prefs_key_home_animation_recents_rate'];assert all('android:key="'+key+'"'in xml for key in keys)
    endpoint=supporting[1].read_text();assert 'tweaks[14] = readBoolean(values, "home_animation_recents_enable", false)'in endpoint
    assert 'tweaks[15] = Math.max(30, Math.min(200, readInt(values, "home_animation_recents_rate", 100)))'in endpoint
    report=dict(passed=True,status='COMPONENT_AND_REAL_CONTROLLER_INSTALLER_JOINT_VERIFIED_OFFLINE',device_status='NOT_TESTED',
        production_sources=sources,evidence={name:dict(path=path,sha256=sha(ROOT/path))for name,path in relative.items()},
        supporting_inputs={str(test_paths.report_path(path, ROOT)):sha(path)for path in supporting},
        source_tests={str(test_paths.report_path(path, ROOT)):sha(path)for path in sorted(Path(__file__).resolve().parent.iterdir())if path.is_file()},
        settings=dict(keys=keys,count_keys=2,feature='Home Recents shared animation duration ratio',range_percent=[30,200],all_integer_selections=171,
            mapping='absolute f32 percentage/100 after original OEM normalization, before shared owner publication',
            restart='The existing screen already requests com.miui.home quick restart; change/disable/lapse/miss requires a fresh Home process.',
            independent_open_close_setting=True,writes_settings_global=False),
        lifecycle=dict(entry='original runtime returnPC0x63ecc -> Home app_entry0x9b544c',
            chain=['24-byte Application return','vtable e13838 -> PlatformApplicationFfi.on_attach73cf1c','App.on_attach9784dc','Tokio vtable e1e518/568 -> 5e04b4/5e05bc','poll6898f4 -> Once4f0890','initializer625708 -> updater7a57d8'],
            patch='single aligned word at0x7a58a8; replay STR s0,[x29,#0x1c], resume0x7a58ac',
            owner='f19db0 tuple, Oncef19dc0; ratio owner+16',
            capture_deadline='prepare before the authenticated original app_entry continues; Once cold and zero tuple required before and after patch',
            not_single_thread=True,
            concurrent_entry='loser closes the same pending lease word even before winner phase CAS; no late winner arming',
            known_constructor_scope='all53 actual init_array inventory constructors bound; this is not exhaustive dependency/external callback analysis'),
        memory_ownership=dict(original_library='PinImage exact namespace RTLD_NOLOAD reference with exact app_entry lookup; shared entry owns the reference for process life',
            rust_code_page='one actual 4K/16K page mlocked before writing, smaps VmFlags lo verified; no munlock by this component',
            known_reclaimer='actual Flutter advice4 selects this Rust page, including APK and standalone .so cases; VM_LOCKED rejection separately tested on host kernel',
            journal='only own exact aligned B word restored; independent RX protection journal; whole guard verification after rollback; foreign changes become Residue',
            retained_arena=True),
        lease=dict(clock='CLOCK_BOOTTIME via actual native SVC113',maximum_seconds=5,deadline_floor='milliseconds, at most999999ns early',
            renewal='same initial epoch and percentage only',revision='exact full identity during install; same selection newer valid revision can renew afterward',
            terminal=['disable','different percentage','new epoch','authorization loss','deadline lapse','early updater','duplicate entry'],
            cache_boundary='Existing Rust/Dart object caches are not synchronously restored on cancellation.'),
        verification=dict(arm_owner=2354,arm_abi=8208,policy=398,component_negative_controls=10,once_checks=9,once_negative_controls=2,
            installer_scenarios=40,installer_assertions=305,installer_negative_controls=11,joint_scenarios=11,joint_negative_controls=4,
            guard_spans=85,relocated_pointers=58,linked_template_bytes=980,independent_installer_replayed=True),
        limits=[
            'No device connection, real Hyos/LSPosed injection, Android Binder execution, visible animation or long-term stability has been tested.',
            'The real joint suite runs controller and installer on the host with transport/clock/loader/ISA adapters; its ARM template bytes are linked and checked, not executed by that host suite.',
            'ARM owner/parser/callbacks and gateway execute in controlled QEMU mappings; provider, allocation-adjacent services, FRB and Dart heap are explicit fixtures.',
            'The exact spawner base BPF admits113 and228; later thread filters or another runtime path cannot be ruled out statically. A SIGSYS/TRAP/KILL is not an errno that the gateway catches.',
            'Target mlock success, current memlock resource limit, SELinux and native namespace permissions are runtime conditions. Failure prevents code mutation.',
            'mlock does not exclude explicit munmap/mremap/munlock/DONTNEED_LOCKED, foreign writes, or altered kernels. The actual observed Flutter advice4 path is the protected path.',
            'A native copied frame temporarily changes SP; synchronous ABI is fully tested but arbitrary asynchronous signal/unwind/profiler behavior is not established.',
            'This report is neither complete APK/ELF artifact approval nor evidence that other previously declined Home settings are supported.'],
        generator=dict(path=str(test_paths.report_path(Path(__file__).resolve(), ROOT)),sha256=sha(Path(__file__).resolve())))
    (out/'report.json').write_text(json.dumps(report,ensure_ascii=False,indent=2)+'\n')
    print(json.dumps(dict(passed=True,report=str(out/'report.json'),sha256=sha(out/'report.json')),indent=2))
if __name__=='__main__':main()

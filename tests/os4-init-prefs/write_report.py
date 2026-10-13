#!/usr/bin/env python3
# SPDX-License-Identifier: AGPL-3.0-or-later
"""Collect the completed host regressions and exact source hashes; no device pass claims."""
import hashlib
import json
from pathlib import Path
import re

import sys
sys.path.insert(0, str(Path(__file__).resolve().parents[1]))
import hyperceiler_test_paths as test_paths


ROOT=Path(__file__).resolve().parents[2]
PROJECT=test_paths.TEST_DATA_ROOT
OUT=test_paths.TEST_DATA_ROOT / 'acceptance'
def digest(path): return hashlib.sha256(path.read_bytes()).hexdigest()
def record(path): return {'path':str(path),'sha256':digest(path)}
def result(name, pattern):
    path=OUT/name
    matches=re.findall(pattern,path.read_text())
    if len(matches)!=1: raise ValueError(f'Expected one result in {name}: {matches}')
    return {**record(path),'passed':int(matches[0][0]),'total':int(matches[0][1])}
entry=ROOT/'library/libhook/src/main/java/com/sevtinge/hyperceiler/libhook/base/XposedInitEntry.java'
native=ROOT/'library/libhook/src/main/java/com/sevtinge/hyperceiler/libhook/rules/home/other/NativeHomeHooksOS4.kt'
baseline=OUT/'os4-init-prefs-baseline/XposedInitEntry.java'
gate_marker='    private boolean prepareHookLoad('
assert entry.read_text().split(gate_marker,1)[1]==baseline.read_text().split(gate_marker,1)[1]
java=result('os4-init-prefs-after.log',r'Init preference lifecycle: (\d+)/(\d+) PASS')
before=result('os4-init-prefs-upstream-before.log',r'Init preference lifecycle: (\d+)/(\d+) PASS')
jni=result('os4-native-prefs-after.log',r'Native preference recovery: (\d+)/(\d+) PASS')
old_log=OUT/'os4-native-listener-regression.log'
assert 'NativeHome Dock listener: 8 passed, 0 failed' in old_log.read_text()
assert (java['passed'],java['total'],before['passed'],before['total'],jni['passed'],jni['total'])==(18,18,5,18,11,11)
report={
 'schema':1,'target_rom':'OS4.0.0.44.XPBCNXM','android_api':37,
 'evidence_level':'offline_production_source_host_regression','device_status':'NOT_TESTED',
 'production_sources':[record(entry),record(native)],
 'baseline':{'commit':'5e4686069','source':record(baseline),'results':before},
 'finding':'Bootstrap initPrefs failure had no normal target-ready retry. Real PrefsBridge throws when uninitialized; the actual EzHookTool 1.3.0 dispatcher does not replay a failed generation. Home could load its native library with false fallback values and latch loadStarted before preferences recovered.',
 'fixes':[
  'Retry the existing locked, idempotent initPrefs once at the shared target-ready callback, before any preference-dependent hook gate or rule.',
  'Persistent failure records both generation and hot-reload initialization failure, then aborts the callback. Valid empty maps remain initialized and keep the existing allow_hook=false default.',
  'Pass the explicit bootstrap preference-init result to the Home probe. Non-null remote handles do not hide failures from a later bootstrap log-level read.',
  'Defer listener attachment while bootstrap recovery is pending, so a successful retry returning another SharedPreferences handle cannot leave the listener attached to the obsolete handle.',
  'After successful preference retry and unchanged allow_hook/framework gates, repair only a pending Home configuration whose library actually loaded. Reconfigure JNI and attach the missing Dock listener without another library load.',
  'Configuration/read/listener failure leaves native recovery pending for a later legal rule-side callback. Successful recovery is idempotent; a second check after claiming the atomic recovery slot rejects a late concurrent caller.'
 ],
 'unchanged_gate_region_matches_upstream':True,
 'host_tests':{'java_lifecycle':java,'native_kotlin_jni':jni,'existing_dock_listener':{**record(old_log),'passed':8,'total':8}},
 'dependencies':{'libxposed':'102.0.0 actual API wrapper','ezhooktool':'1.3.0 actual TargetReadyDispatcher'},
 'lifecycle_evidence':[record(OUT/x) for x in ['libxposed-api102-lifecycle.javap.txt','ezxposed130-lifecycle.javap.txt','ezxposed130-target-dispatcher.javap.txt']],
 'affected_home_preferences':[{'key':key,'device_status':'NOT_TESTED','offline_evidence':'pending bootstrap recovery reads current preferences and passes expected values to the host JNI recorder'} for key in [
  'home_other_high_models','home_other_disable_prestart','home_dock_bg_custom_enable','home_dock_add_blur']],
 'commands':['python3 tests/os4-init-prefs/run.py','python3 tests/os4-init-prefs/run_native.py',
  'python3 tests/os4-init-prefs/run.py --source '+str(baseline)],
 'test_sources':[record(p) for p in sorted((ROOT/'tests/os4-init-prefs').iterdir()) if p.is_file()],
 'limits':[
  'No APK was rebuilt by this subtask; no device, real LSPosed injection or long-run OS4 stability was tested.',
  'The Java test executes complete production entry/PrefsBridge/BaseLoad/PackageTarget and actual dependency dispatcher; Android services, logging, rule discovery and lifecycle adapter are stubs.',
  'The native test executes complete production Kotlin plus a host JNI recorder, not the Android ARM64 module or Dart/Rust launcher. It verifies calls and arguments only.',
  'Hot-reload callback failure is verified; real framework atomic hook replacement is not simulated or claimed.',
  'Target-ready is one-shot. Persistent failure has no new background timer or retry without another legal callback, and same-generation hook installation is not replayed.',
  'Native recovery re-pushes flags and establishes the listener. It cannot prove that values cached by the launcher during early startup are recomputed. Such settings can still require launcher restart and real-device acceptance.',
  'This repair does not create new high-device-level/prestart/blur feature implementations or upgrade their support status.'
 ]
}
(OUT/'os4-init-prefs-audit.json').write_text(json.dumps(report,ensure_ascii=False,indent=2)+'\n')
text='OS4 启动偏好重试与桌面原生恢复审计\n\n'
text+='状态：离线生产代码回归通过；真实手机、LSPosed 注入与功能生效均 NOT_TESTED。\n'
text+='上游入口负对照 5/18；修复后入口 18/18；真实 Kotlin + 宿主 JNI 参数 11/11；旧 Dock listener 8/8。\n'
text+='门禁区域逐字匹配上游原文件，未放宽 allow_hook 或 framework 校验。\n\n'
text+='确认问题：bootstrap 失败后原入口不会在正常 target-ready 重试；PrefsBridge 并非自动空配置，而是未初始化异常。Home 已加载标志还可能锁住缺省全 false 的配置。\n'
text+='修复：target-ready 同步幂等重试；持续失败记账并中止；门禁通过后才修复明确 pending 且库确已加载的 Home 配置。只重推参数和补监听，不再加载库；并发晚到调用和成功后重复调用均不重配。\n'
text+='绑定成功但日志级别读取失败也会传递明确未就绪状态，避免仅依靠非 null 句柄误判。\n\n'
text+='源码：\n'+''.join(f'- {x["path"]}\n  SHA256 {x["sha256"]}\n' for x in report['production_sources'])
text+='\n验证边界：\n'+''.join('- '+x+'\n' for x in report['limits'])
(OUT/'os4-init-prefs-audit.txt').write_text(text)
print('Wrote os4-init-prefs-audit.json/.txt; no real-device pass claimed.')

#!/usr/bin/env python3
"""Validate exact OTA extraction, local Zen display paths and compiled callback contracts."""
from pathlib import Path
import argparse,hashlib,json,re,subprocess,tempfile,zipfile,importlib.util

import sys
sys.path.insert(0, str(Path(__file__).resolve().parents[1]))
import hyperceiler_test_paths as test_paths

ROOT=test_paths.TEST_DATA_ROOT;ROM=test_paths.TEST_DATA_ROOT / 'acceptance/java-test11/zen-mode/rom-v1';APK=test_paths.ROM_ROOT / 'components/system_ext/priv-app/MiuiSystemUI/MiuiSystemUI.apk';CACHE=test_paths.BUILD_CACHE;sha=lambda p:hashlib.sha256(p.read_bytes()).hexdigest()
p=argparse.ArgumentParser();p.add_argument('--output',type=Path,required=True);a=p.parse_args();a.output.mkdir(parents=True,exist_ok=False);rows=[]
def ck(n,v,detail=None):rows.append({'name':n,'passed':bool(v),'detail':detail})
def text(name):return (ROM/'smali'/Path(name.replace('.','/')+'.smali')).read_text()
def method(name,sig):
 s=text(name);matches=re.findall(r'^\.method [^\n]* '+re.escape(sig)+r'\n.*?^\.end method',s,re.M|re.S);assert len(matches)==1,(name,sig);return matches[0]
report=json.loads((ROM/'rom-extraction.json').read_text());ck('exact_OTA_APK',sha(APK)=='7da817a4c65299f7362fd530d72c09c0a4785c98cf2732395c721f2740e7662a')
ck('all_complete_extracted_classes_unchanged',all(sha(ROM/k)==v for k,v in report['files'].items()),len(report['files']))
spec=importlib.util.spec_from_file_location('di',test_paths.TEST_DATA_ROOT / 'tools/dex_inventory.py');d=importlib.util.module_from_spec(spec);spec.loader.exec_module(d)
classes={}
with zipfile.ZipFile(APK)as z:
 for n in z.namelist():
  if n.endswith('.dex'):classes.update(d.dex_classes(z.read(n)))
ck('old_OS3_controller_absent_all_DEX','com.android.systemui.statusbar.notification.zen.ZenModeViewController'not in classes)
lib=test_paths.ANDROID_HOME / 'cmdline-tools/latest/lib/external';cp=':'.join(str(p)for pat in ('**/smali-*.jar','**/guava-*.jar','**/failureaccess-*.jar')for p in lib.glob(pat))
with tempfile.TemporaryDirectory(prefix='hc-zen-refs-')as t:
 subprocess.run([str(test_paths.JAVA_HOME / 'bin/javac'),'-cp',cp,'-d',t,str(ROM/'DexRefs.java')],check=True,capture_output=True)
 r=subprocess.run([str(test_paths.JAVA_HOME / 'bin/java'),'-cp',t+':'+cp,'DexRefs',str(APK)],check=True,capture_output=True,text=True)
xrefs=r.stdout;ck('all_DEX_references_reproduced',xrefs==(ROM/'all-dex-xrefs.txt').read_text());(a.output/'all-dex-xrefs.txt').write_text(xrefs)
store='com.android.notification.tinypanel.FlipNotifDataStore';merge=method(store,'merge(ZLcom/android/systemui/media/controls/shared/model/MediaData;Ljava/util/List;)Ljava/util/List;')
ck('six_actual_merge_callers',sum('INVOKE_VIRTUAL' in s and '->merge('in s for s in xrefs.splitlines())==6)
ck('merge_false_path_is_local_UI_state',all(s in merge for s in ['if-eqz p1, :cond_1f',':cond_1f','->isItemDismiss:Z','iput-boolean p1, p0, Lcom/android/notification/tinypanel/FlipNotifDataStore;->mZenModeOn:Z','if-eqz p1, :cond_2d']))
ck('merge_preserves_original_notification_filters_and_capacity',all(s in merge for s in ['->mCanShowOnLockscreen:Z','->isSummaryWithChildren()Z','->shouldHideNotification(','->mIsFocusNotification:Z','const/4 v1, 0x4','if-lt p1, v1, :cond_70']))
ck('merge_no_system_Zen_write',not any(s in merge for s in ['->setZen','Settings$Global','->dismissNotification(']))
media=method(store,'onMediaUpdate(Lcom/android/systemui/media/controls/shared/model/MediaData;)V');ck('media_super_save_gate_and_live_data_preserved',all(s in media for s in ['->isActive()Z','->merge(','->postValue(']))
consumer='com.miui.systemui.notification.view.viewbinder.NotificationNumStateViewBinder$bind$1$1$1$1';emit=method(consumer,'emit(Ljava/lang/Object;Lkotlin/coroutines/Continuation;)Ljava/lang/Object;')
labels=re.search(r'\.packed-switch 0x0\n(.*?)\.end packed-switch',emit,re.S)[1].split();ck('consumer_R8_id0_is_Zen_only',labels[0]==':pswitch_1b4'and len(labels)==10)
zen=emit.split(':pswitch_1b4\n',1)[1].split(':pswitch_data_1dc',1)[0];ck('id0_calls_original_text_layout_and_post',all(s in zen for s in ['check-cast p1, Ljava/lang/Boolean;','->isZenModeEnabled:Z','->updateZenViewText()V','->requestLayout()V','->post(Ljava/lang/Runnable;)Z','invoke-direct {p2, v2}']))
ck('unique_view_state_writer_all_DEX',sum('IPUT_BOOLEAN' in s and 'NotificationNumStateView;->isZenModeEnabled:Z'in s for s in xrefs.splitlines())==1)
ck('unique_viewmodel_flow_reader_all_DEX',sum('IGET_OBJECT' in s and 'NotificationNumStateViewModel;->isZenModeEnabled:'in s for s in xrefs.splitlines())==1)
producer=method('com.miui.systemui.notification.view.viewbinder.NotificationNumStateViewBinder$bind$1$1$1','invokeSuspend(Ljava/lang/Object;)Ljava/lang/Object;')
ck('real_flow_constructs_id0_collector',all(s in producer for s in ['->isZenModeEnabled:Lkotlinx/coroutines/flow/ReadonlyStateFlow;','const/4 v5, 0x0','invoke-direct {v1, v5}','->collect(','if-ne p0, v0, :cond_33','return-object v0']))
ck('consumer_non_suspending_original_Unit_return','COROUTINE_SUSPENDED'not in emit and '->collect('not in emit and '->INSTANCE:Lkotlin/Unit;'in emit)
section=text('com.android.keyguard.blueprint.NotificationNumStateSection');ck('ordinary_view_bound_by_Keyguard_section',all(s in section for s in ['.super Lcom/android/systemui/keyguard/shared/model/KeyguardSection;','const v0, 0x7f0e037c','NotificationNumStateViewBinder;->bind(']))
run=method('com.miui.systemui.notification.view.NotificationNumStateView$updateZenMode$1','run()V');hide=run.split(':cond_1f1\n',1)[1];ck('false_Zen_animation_hides_Zen_and_divider_not_count',all(s in hide for s in ['->zenViewFolme:', '->dividingLineFolme:', 'const/4 v14, 0x0','->notificationCountViewFolme:']))
anim=method('com.miui.systemui.notification.ext.NumStateViewAnimateExt$animateUpdateViewVisibility$1','onComplete(Ljava/lang/Object;)V');ck('original_hide_animation_ends_INVISIBLE',all(s in anim for s in ['->$show:Z','if-nez v0, :cond_22','const/4 v0, 0x4','->setVisibility(I)V']))
layout=subprocess.run([str(test_paths.ANDROID_HOME / 'build-tools/37.0.0/aapt2'),'dump','xmltree','--file','res/layout/num_state_view.xml',str(APK)],check=True,text=True,capture_output=True).stdout
ck('actual_layout_reproduced',layout==(ROM/'num-state-view-layout.txt').read_text());ck('Zen_layout_initial_invisible_and_alpha_zero',all(s in layout for s in ['@0x7f0b0dee','visibility(0x010100dc)=1','alpha(0x0101031f)=0.000000e+00','@0x7f14079e']))
res=(test_paths.ROM_ROOT / 'indexes/systemui-resources.txt').read_text();ck('exact_keyguard_DND_resource',bool(re.search(r'resource 0x7f14079e string/keyguard_num_state_zen_mode_text.*?\(zh-rCN\) "勿扰"',res,re.S)))
source=test_paths.SOURCE_ROOT / 'library/libhook/src/main/java/com/sevtinge/hyperceiler/libhook/rules/systemui/lockscreen/HideLockscreenZenMode.kt';body=source.read_text();route=test_paths.SOURCE_ROOT / 'library/libhook/src/main/java/com/sevtinge/hyperceiler/libhook/app/SystemUI/SystemUIB.java';ck('existing_preference_gate_retained','initHook(HideLockscreenZenMode.INSTANCE, PrefsBridge.getBoolean("system_ui_lock_screen_not_disturb_mode"));'in route.read_text())
ck('no_global_Zen_API_in_rule',not any(s in body for s in ['setZenMode','Settings.Global','dismissNotification','ZenModeControllerImpl']))
old=ROOT/'../dummy';frozen=test_paths.SNAPSHOT_ROOT / 'source-os4-hyos-test10-20261009-r1/library/libhook/src/main/java/com/sevtinge/hyperceiler/libhook/rules/systemui/lockscreen/HideLockscreenZenMode.kt';legacy=frozen.read_text().split('        // hyperOS fix by hyper helper')[1].rsplit('\n}',1)[0];ck('OS3_original_hook_body_retained',('        // hyperOS fix by hyper helper'+legacy).strip()in body)
refs=[APK,source,route,Path(__file__),ROM/'DexRefs.java',ROM/'extract_rom.py',ROM/'rom-extraction.json',ROM/'all-dex-xrefs.txt',ROM/'num-state-view-layout.txt'];out={'passed':all(r['passed']for r in rows),'checks':rows,'sources':{str(test_paths.report_path(source, ROOT)):sha(source)},'inputs':{str(test_paths.report_path(p, ROOT)):sha(p)for p in refs},'complete_smali_files':report['files'],'device_status':'NOT_TESTED','scope':'Exact original OTA DEX reference scan and full selected method/resource review. Static contract evidence; no original Android UI/ART/LSPosed/device execution.'};(a.output/'report.json').write_text(json.dumps(out,indent=2)+'\n');print(len(rows),'checks',out['passed']);print([x for x in rows if not x['passed']]);raise SystemExit(0 if out['passed']else 1)

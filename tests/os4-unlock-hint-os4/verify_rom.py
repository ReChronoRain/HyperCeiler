#!/usr/bin/env python3
"""Bind the narrow stock consumer contract to complete fresh OTA smali methods."""
from pathlib import Path
import argparse,hashlib,json,re

import sys
sys.path.insert(0, str(Path(__file__).resolve().parents[1]))
import hyperceiler_test_paths as test_paths

ROOT=test_paths.TEST_DATA_ROOT; SOURCE=test_paths.SOURCE_ROOT
sha=lambda p:hashlib.sha256(p.read_bytes()).hexdigest()
def run(rom,out):
 rom=rom.resolve();out=out.resolve()
 out.mkdir(parents=True,exist_ok=False);sm=rom/'smali';inputs={};checks=[];methods=[]
 def pin(p):inputs[str(p.resolve())]=sha(p);return p.read_text()
 def check(n,v):checks.append(dict(name=n,passed=bool(v)))
 def method(owner,sig):
  p=sm/(owner.replace('.','/')+'.smali');text=pin(p);rows=[x.group(0)for x in re.finditer(r'^\.method .*?^\.end method',text,re.M|re.S)if x.group(0).splitlines()[0].endswith(' '+sig)];assert len(rows)==1,(owner,sig)
  body=rows[0];file=out/(str(len(methods)+1)+'-'+sig.split('(')[0].replace('<','').replace('>','')+'.smali.txt');file.write_text(body+'\n');methods.append(dict(owner=owner,signature=sig,full_body_sha256=hashlib.sha256(body.encode()).hexdigest(),excerpt=str(test_paths.report_path(file, ROOT)),line=text[:text.index(body)].count('\n')+1));return body
 R='com.android.systemui.keyguard.KeyguardIndicationRotateTextViewController';C='com.android.systemui.statusbar.KeyguardIndicationController'
 ex=json.loads(pin(rom/'extraction.json'));apk=Path(ex['apk']);inputs[str(apk)]=sha(apk)
 check('fresh actual OTA APK pinned',sha(apk)==ex['apk_sha256']=='7da817a4c65299f7362fd530d72c09c0a4785c98cf2732395c721f2740e7662a')
 check('every fresh class hash current',all(sha(rom/p)==h for p,h in ex['smali'].items()))
 producer=method(C,'updateDeviceEntryIndication(Z)V');update=method(R,'updateIndication(ILcom/android/systemui/keyguard/KeyguardIndication;Z)V');hide=method(R,'hideIndication(I)V');bind=method(C,'setIndicationArea(Landroid/view/ViewGroup;)V');bio=method(C,'updateBiometricMessage()V');transient=method(C,'updateTransient()V');organization=method(C+'$$ExternalSyntheticLambda8','run()V');theme=method('com.miui.systemui.util.CommonUtil','isDefaultLockScreenTheme()Z')
 prefix=producer[:producer.index('    :goto_a2')]
 for n,s in {'force dismissible gate':'->mForceIsDismissible:Z','current resources String':'->getString(I)Ljava/lang/String;','type thirteen':'const/16 v13, 0xd','actual swipe resource':'const v14, 0x7f1404dd','nonempty check':'Landroid/text/TextUtils;->isEmpty','original exact type13 call':'invoke-virtual {v5, v13, v15, v7}','true immediate':'const/4 v7, 0x1','null source register':'const/4 v10, 0x0','false source register':'const/4 v8, 0x0'}.items():check(n,s in prefix)
 for field in ['mOnClickListener:Landroid/view/View$OnClickListener;','mBackground:Landroid/graphics/drawable/Drawable;','mMinVisibilityMillis:Ljava/lang/Long;']:
  check('standard type13 null '+field,'iput-object v10, v15, Lcom/android/systemui/keyguard/KeyguardIndication;->'+field in prefix)
 check('type13 not assertive','iput-boolean v8, v15, Lcom/android/systemui/keyguard/KeyguardIndication;->mForceAccessibilityLiveRegionAssertive:Z'in prefix)
 check('only known type13 construction before other business',prefix.count('new-instance v15, Lcom/android/systemui/keyguard/KeyguardIndication;')==1 and '->mOrganizationOwnedDevice:Z'in producer[producer.index('    :goto_a2'):])
 check('biometric minimum remains other types',all(x in bio for x in ['const/16 v5, 0xb','const/16 v5, 0xc','const-wide/16 v0, 0xbb8']))
 check('transient minimum belongs to type5','const/4 v3, 0x5'in transient and 'const-wide/16 v4, 0xbb8'in transient)
 check('organization callback is type1','const/4 v0, 0x1'in organization and 'invoke-virtual {v1, v0, v2, p0}'in organization)
 check('default theme uses actual static wallpaper type','sKeyguardWallpaperType:Ljava/lang/String;'in theme and 'const-string v0, "maml"'in theme and 'xor-int/lit8'in theme)
 for n,s in {'original null map removal':'->remove(Ljava/lang/Object;)Ljava/lang/Object;','original all duplicate queue removal':'->removeIf(Ljava/util/function/Predicate;)Z','original dozing branch':'->mIsDozing:Z','original delayed cancellation':'ExecutorImpl$ExecutionToken;->run()V','original queued next call':'ShowNextIndication$$ExternalSyntheticLambda0;->run()V','original hidden transition':'invoke-virtual {p0, v0}, Lcom/android/systemui/keyguard/KeyguardIndicationRotateTextViewController;->showIndication(I)V'}.items():check(n,s in update)
 check('map and queue removal occur before dozing return',update.index('->removeIf(')<update.index('->mIsDozing:Z'))
 check('hide method calls same null path','invoke-virtual {p0, p1, v0, v1}'in hide and 'const/4 v0, 0x0'in hide and 'const/4 v1, 0x1'in hide)
 check('rotate receives actual lower text view',all(s in bind for s in ['const v0, 0x7f0b05f7','->mLockScreenIndicationView:', 'invoke-direct {p1, v0}, Lcom/android/systemui/util/ViewController;-><init>(Landroid/view/View;)V']))
 view=pin(sm/'com/android/systemui/util/ViewController.smali');check('ViewController final View field','.field public final mView:Landroid/view/View;'in view)
 layout=pin(rom/'keyguard_bottom_area.xml.txt');check('actual lower view and area XML','@0x7f0b05f7'in layout and '@0x7f0b05f5'in layout and 'layout_gravity(0x010100b3)=0x00000051'in layout)
 # XML route and original OS3 body are source facts, not proof of UI visibility on a device.
 folder=SOURCE/'library/libhook/src/main/java/com/sevtinge/hyperceiler/libhook/rules/systemui/lockscreen';route=pin(folder/'HideLockScreenHint.kt');rule=pin(folder/'HideLockScreenHintOS4.kt');manifest=ROOT/'validation/os4-hyos-test14-r2-build-snapshot.json';prior=json.loads(pin(manifest));old=pin(Path(prior['snapshot_directory'])/(folder/'HideLockScreenHint.kt').relative_to(SOURCE));marker='        val controllerClass = keyguardIndicationController ?: return';check('OS3 full legacy method tail byte identical',route[route.index(marker):]==old[old.index(marker):])
 check('old controller property byte identical',route[route.index('    private val'):route.index('    override fun')]==old[old.index('    private val'):old.index('    override fun')])
 xml=pin(SOURCE/'library/core/src/main/res/xml/system_ui_lock_screen.xml');system=pin(SOURCE/'library/libhook/src/main/java/com/sevtinge/hyperceiler/libhook/app/SystemUI/SystemUIB.java');check('canonical existing UI key through registration','prefs_key_system_ui_lock_screen_unlock_tip'in xml and 'initHook(HideLockScreenHint.INSTANCE, PrefsBridge.getBoolean("system_ui_lock_screen_unlock_tip"))'in system and 'PrefsBridge.getBoolean("system_ui_lock_screen_unlock_tip")'in rule)
 check('OS4 branches before legacy class resolution',route.index('HideLockScreenHintOS4.init()')<route.index(marker))
 report=dict(passed=all(x['passed']for x in checks),checks=checks,inputs=inputs,sources={str(p.relative_to(SOURCE)):sha(p)for p in [folder/'HideLockScreenHint.kt',folder/'HideLockScreenHintOS4.kt']},complete_methods=methods,device_status='NOT_TESTED',scope='Author static contract review of eight complete methods and actual XML/field identities; method extraction is not execution. Known stock soft-sleep type13 only; no all-bottom-hints or arbitrary theme claim.',limitations=['No proof that all dynamically loaded or reflective producers have been enumerated. Non-standard type13 contracts deliberately pass through.','Null retains the original map/queue/dozing/delay algorithm. Removing current13 may advance another queued message earlier as OEM deletion normally does.','Disable requires existing SystemUI reload/restart workflow; cleanup alone restores handling on the next original publisher refresh, without synthesizing a missing original message.'])
 (out/'report.json').write_text(json.dumps(report,indent=2)+'\n');print(report['passed'],len(checks));assert report['passed']
if __name__=='__main__':
 p=argparse.ArgumentParser();p.add_argument('--rom',type=Path,required=True);p.add_argument('--output',type=Path,required=True);a=p.parse_args();run(a.rom,a.output)

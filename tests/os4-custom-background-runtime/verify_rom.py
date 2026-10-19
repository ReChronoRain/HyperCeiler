#!/usr/bin/env python3
"""Re-extract actual OS4 resources and bind complete reviewed local consumer bodies."""
from pathlib import Path
import argparse,hashlib,json,re,subprocess,zipfile

import sys
sys.path.insert(0, str(Path(__file__).resolve().parents[1]))
import hyperceiler_test_paths as test_paths

ROOT=test_paths.TEST_DATA_ROOT
sha=lambda p:hashlib.sha256(p.read_bytes()).hexdigest()
p=argparse.ArgumentParser();p.add_argument('--output',required=True,type=Path);a=p.parse_args();out=a.output;out.mkdir(parents=True,exist_ok=False)
apk=test_paths.ROM_ROOT / 'components/system_ext/priv-app/MiuiSystemUI/MiuiSystemUI.apk';aapt=test_paths.ANDROID_HOME / 'build-tools/37.0.0/aapt2'
inputs={str(apk):sha(apk),str(aapt):sha(aapt),str(Path(__file__).resolve()):sha(Path(__file__))};checks=[]
def check(name,value):checks.append({'name':name,'passed':bool(value)});assert value,name
check('actual_supplied_OTA_SystemUI',sha(apk)=='7da817a4c65299f7362fd530d72c09c0a4785c98cf2732395c721f2740e7662a')
def run(cmd):return subprocess.run(cmd,check=True,capture_output=True,text=True).stdout
resources=run([str(aapt),'dump','resources',str(apk)])
blocks={int(m[1],16):m[0] for m in re.finditer(r'^    resource (0x[0-9a-f]+) [^\n]+\n(?:(?!    resource ).*\n)*',resources,re.M)}
styles=[0x7f1509bd,0x7f1509be,0x7f1509bf,0x7f1509c0,0x7f1509c1,0x7f1509c2,0x7f1509c3,0x7f1509c4]
(out/'styles.txt').write_text(''.join(blocks[x]for x in styles))
for n,sid in [('miui_media_session',0x7f1509c3),('miui_media_session_island',0x7f1509bf),('miui_media_session_island_dummy',0x7f1509bf)]:
 value=run([str(aapt),'dump','xmltree',str(apk),'--file',f'res/layout/{n}.xml']);(out/(n+'.xml.txt')).write_text(value)
 node=value.split('E: miuix.miuixbasewidget.widget.HyperProgressSeekBar',1)[1].split('\n        E:',1)[0]
 check(n+'_exact_style',f'style=@{sid:#x}'in node)
 check(n+'_no_direct_progress_mode','progressBarMode'not in node)
check('NC_normal_style_explicit_primary','progressBarMode(0x7f04085d)=0'in blocks[0x7f1509c1])
check('DI_day_night_base_no_explicit_mode',all('progressBarMode'not in blocks[x]for x in [0x7f1509bd,0x7f1509be,0x7f1509bf,0x7f1509c0]))
check('DI_day_night_parent_edges',all(text in blocks[x]for x,text in [(0x7f1509bf,'0x7f1509c0'),(0x7f1509bf,'0x7f1509be'),(0x7f1509be,'0x7f1509bd'),(0x7f1509c0,'0x7f1509bd')]))
paths={
 'NC':test_paths.TEST_DATA_ROOT / 'acceptance/java-test14/always-dark-v1/rom/smali/com/android/systemui/statusbar/notification/mediacontrol/MiuiMediaViewControllerImpl.smali',
 'Layout':test_paths.TEST_DATA_ROOT / 'acceptance/java-test14/always-dark-v1/rom-writers/smali/com/android/systemui/statusbar/notification/mediacontrol/MiuiMediaNotificationControllerImpl.smali',
 'DI':test_paths.TEST_DATA_ROOT / 'acceptance/java-test12/media-text-v1/rom/smali/com/android/systemui/statusbar/notification/mediaisland/MiuiIslandMediaViewBinderImpl.smali',
 'Pull':test_paths.TEST_DATA_ROOT / 'acceptance/java-test12/media-text-v1/rom/smali/com/android/systemui/statusbar/notification/mediaisland/MiuiIslandMediaViewBinderImpl$attach$4$1.smali',
 'Seek':test_paths.TEST_DATA_ROOT / 'acceptance/java-test14/always-dark-v1/rom-seekbar/smali/miuix/miuixbasewidget/widget/HyperProgressSeekBar.smali'}
selection={'NC':['attach(','detach(','bindMediaData(','updateForegroundColors(','onFullAodStateChanged('], 'Layout':['updateLayout$1('], 'DI':['attach(','detach(','bindMediaData(','updateForegroundColors('], 'Pull':['emit('], 'Seek':['<init>(Landroid/content/Context;Landroid/util/AttributeSet;I)','onDraw(','drawPrimaryMode(','drawHighEndMode(','initShaderConfig(','updatePrimaryColor$1(','setForegroundPrimaryColor(','setForegroundPrimaryDisableColor(','setBackgroundPrimaryColor(','setBackgroundPrimaryDisableColor(']}
methods={};review=[]
for key,path in paths.items():
 text=path.read_text();inputs[str(path)]=sha(path);items=re.findall(r'^\.method[^\n]*\n.*?^\.end method$',text,re.M|re.S)
 for prefix in selection[key]:
  rows=[b for b in items if (' '+prefix)in b.splitlines()[0]];check('unique_complete_'+key+'_'+prefix,len(rows)==1);body=rows[0];sig=body.splitlines()[0];name=key+'-'+str(len(review))+'.smali';(out/name).write_text(body+'\n');methods[key+':'+prefix]=body;review.append({'class':key,'method':sig,'source':str(path),'source_sha256':sha(path),'full_body_file':name,'sha256':sha(out/name)})
check('NC_no_removed_updateMediaBackground','updateMediaBackground('not in paths['NC'].read_text())
check('pull_uses_host_KotlinPair','check-cast p1, Lkotlin/Pair;'in methods['Pull:emit('])
check('DI_finish_does_not_reset_dummy_translation',methods['Pull:emit('].count('->setTranslationY(F)V')==1)
check('layout_independent_text_writer',methods['Layout:updateLayout$1('].count('->setTextColor(I)V')>=2)
ctor=methods['Seek:<init>(Landroid/content/Context;Landroid/util/AttributeSet;I)']
check('constructor_default_minus_one_then_device_level','const/4 v4, -0x1'in ctor and '->getDeviceLevel()I'in ctor and '->mProgressMode:I'in ctor)
for name in ['setForegroundPrimaryColor(','setForegroundPrimaryDisableColor(','setBackgroundPrimaryColor(','setBackgroundPrimaryDisableColor(']:
 b=methods['Seek:'+name];check('setter_device_level_gate_'+name,'->mDeviceLevel:I'in b and 'if-eqz 'in b and 'if-ne 'in b and '->updatePrimaryColor$1()V'in b)
check('high_draw_uses_exact_final_Paint','->mPaint:Landroid/graphics/Paint;'in methods['Seek:drawHighEndMode('] and '->drawRect(FFFFLandroid/graphics/Paint;)V'in methods['Seek:drawHighEndMode('])
check('shader_setup_preserves_RuntimeShader','->setShader(Landroid/graphics/Shader;)Landroid/graphics/Shader;'in methods['Seek:initShaderConfig('])
check('class_no_color_filter_writer','->setColorFilter('not in paths['Seek'].read_text())
assets={}
with zipfile.ZipFile(apk)as z:
 for name in ['res/raw/music_player_tracker.frag','res/raw/music_player_tracker_middle.frag','res/drawable/miuix_hyper_progressbar_light_head.png']:
  data=z.read(name);assets[name]={'sha256':hashlib.sha256(data).hexdigest(),'size':len(data)}
 # Record resource bytes hashes; raw vendor assets are external APK prerequisites.
 for name in ['res/raw/music_player_tracker.frag','res/raw/music_player_tracker_middle.frag']:
  text=z.read(name).decode();check(name+'_geometry_alpha_no_palette_uniform','uniform shader uTex;'in text and 'uniform vec2 uTrackSize;'in text and 'uniform'not in '\n'.join(x for x in text.splitlines()if 'Color' in x))
source=test_paths.SOURCE_ROOT / 'library/libhook/src/main/java/com/sevtinge/hyperceiler/libhook/rules/systemui/controlcenter/media3'
route=source/'CustomBackground.kt';old=test_paths.SNAPSHOT_ROOT / 'source-os4-hyos-test13-20261009-r1/library/libhook/src/main/java/com/sevtinge/hyperceiler/libhook/rules/systemui/controlcenter/media3/CustomBackground.kt'
body=route.read_text();delta='''        if (isMoreHyperOSVersion(4f)) {
            CustomBackgroundOS4.init()
            return
        }
'''
check('OS3_route_exact_previous_body',body.replace('import com.sevtinge.hyperceiler.libhook.utils.api.DeviceHelper.System.isMoreHyperOSVersion\n','').replace(delta,'')==old.read_text())
inputs[str(old)]=sha(old)
for n in ['CustomBackground.kt','CustomBackgroundOS4.kt','MediaSeekBarColorOS4.kt']:inputs[str(source/n)]=sha(source/n)
xml=test_paths.SOURCE_ROOT / 'library/core/src/main/res/xml'
for name,prefix in [('system_ui_control_center_media_cards.xml','system_ui_control_center_media_control'),('system_ui_status_bar_island_media_card.xml','system_ui_island_media_control')]:
 p=xml/name;inputs[str(p)]=sha(p);t=p.read_text()
 for suffix in ['_background_mode','_control_color_anim','_inverse_color','_panel_background_blur']:check(name+suffix,'prefs_key_'+prefix+suffix in t)
report={'passed':all(c['passed']for c in checks),'checks':checks,'inputs':inputs,'inputs_unchanged':all(sha(Path(p))==h for p,h in inputs.items()),'complete_methods':review,'vendor_assets_external':assets,'device_status':'NOT_TESTED','conclusions':{'NC':'Normal.DayNight inherits explicit progressBarMode0 regardless of device category; original primary setters remain appropriate.','DI':'Both real and dummy XML use DayNight without mode override; constructor defaults -1 and chooses DeviceUtils level. Level1 primary+shader and level2 shader-only require exact scoped shader tint. No assertion of observed device tier.','shader':'Original shader, uniforms, head texture, draw, alpha/geometry/RTL/animation stay active. Custom palette SRC_IN deliberately recolors shader/head hue during exact current-owned draw; original filter restored by identity afterward.','lifecycle':'Complete listed methods support synchronous before-detach capture, current owner and two-holder identities, Kotlin Pair pull stream and independent NC layout color writer. Color/render scheduling is independently guarded by generation.','limitations':['Full listed static bodies and resources, not all possible vendor/runtime/RRO/theme changes.','Encoded setter scan is bounded explicit-reference evidence, not a proof excluding alias/reflection writers.','Host fixtures and SDK compilation do not execute Xiaomi SystemUI, LSPosed, device UI or the original shader. Root Android shader/overlay probes are separate premises.','Value-equal external transitionAlpha0 writes are intrinsically indistinguishable from owned0 and are not claimed detected.']}}
check('all_inputs_unchanged',report['inputs_unchanged']);report['checks']=checks;(out/'report.json').write_text(json.dumps(report,indent=2)+'\n');print(len(checks),len(review),report['passed'])

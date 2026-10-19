#!/usr/bin/env python3
"""Reproduce finite original-OTA media holder/layout/consumer contracts."""
from pathlib import Path
import argparse,hashlib,importlib.util,json,re,subprocess,tempfile,zipfile,xml.etree.ElementTree as ET

import sys
sys.path.insert(0, str(Path(__file__).resolve().parents[1]))
import hyperceiler_test_paths as test_paths

ROOT=test_paths.TEST_DATA_ROOT;ROM=test_paths.TEST_DATA_ROOT / 'acceptance/java-test12/media-text-v1/rom';APK=test_paths.ROM_ROOT / 'components/system_ext/priv-app/MiuiSystemUI/MiuiSystemUI.apk';CACHE=test_paths.BUILD_CACHE;sha=lambda b:hashlib.sha256(b).hexdigest()
p=argparse.ArgumentParser();p.add_argument('--output',type=Path,required=True);a=p.parse_args();a.output.mkdir(parents=True,exist_ok=False);checks=[];inputs={};methods={}
def rb(p):
 p=Path(p);b=p.read_bytes();inputs[str(p)]=sha(b);return b
def ck(n,b,detail=None):checks.append(dict(name=n,passed=bool(b),detail=detail))
def text(n):return rb(ROM/'smali'/Path(n.replace('.','/')+'.smali')).decode()
def method(n,name):
 s=text(n);m=re.findall(r'^\.method [^\n]* '+re.escape(name)+r'\([^\n]*\n.*?^\.end method',s,re.M|re.S);assert len(m)==1,(n,name,len(m));methods[n+'#'+name]=sha(m[0].encode());return m[0]
for p in [Path(__file__),ROM/'extract_rom.py',ROM/'DexRefs.java',test_paths.TEST_DATA_ROOT / 'tools/dex_inventory.py']:
 rb(p)
report=json.loads(rb(ROM/'rom-extraction.json'));ck('exact supplied OTA SystemUI APK',sha(rb(APK))=='7da817a4c65299f7362fd530d72c09c0a4785c98cf2732395c721f2740e7662a');ck('107 full selected classes unchanged',len(report['files'])==107 and all(sha(rb(ROM/k))==h for k,h in report['files'].items()))
lib=test_paths.ANDROID_HOME / 'cmdline-tools/latest/lib/external';cp=':'.join(str(p)for pat in ['**/smali-*.jar','**/guava-*.jar','**/failureaccess-*.jar','**/jcommander-*.jar']for p in lib.glob(pat));java=str(test_paths.JAVA_HOME / 'bin/java')
for p in [*filter(None,cp.split(':')),test_paths.ANDROID_HOME / 'build-tools/37.0.0/aapt2']:
 rb(p)
with tempfile.TemporaryDirectory(prefix='hc-media-rom-')as t:
 t=Path(t);subprocess.run([str(test_paths.JAVA_HOME / 'bin/javac'),'-cp',cp,'-d',str(t),str(ROM/'DexRefs.java')],check=True,capture_output=True);r=subprocess.run([java,'-cp',str(t)+':'+cp,'DexRefs',str(APK)],check=True,capture_output=True);refs=r.stdout.decode();ck('full actual DEX reference scan reproduced',r.stdout==rb(ROM/'all-dex-xrefs.txt'))
 for ar in report['apks']:
  with zipfile.ZipFile(APK)as z:
   for e in ar['extractions']:
    ck('actual selected DEX '+e['dex'],sha(z.read(e['dex']))==e['sha256'])
    subprocess.run([java,'-cp',cp,'com.android.tools.smali.baksmali.Main','disassemble','--classes',','.join('L'+n.replace('.','/')+';'for n in e['classes']),'-o',str(t/'smali'),str(APK)+'/'+e['dex']],check=True,capture_output=True)
 ck('all107 fresh original DEX disassemblies equal archived full classes',all((t/k).read_bytes()==rb(ROM/k)for k in report['files']))
NC='com.android.systemui.statusbar.notification.mediacontrol.';DI='com.android.systemui.statusbar.notification.mediaisland.';F=['titleText','artistText','elapsedTimeView','totalTimeView']
for n in [NC+'MiuiMediaViewHolder',DI+'MiuiIslandMediaViewHolder']:
 s=text(n);ck(n+' constructor absent in complete raw class',' <init>('not in s);ck(n+' all4 exact instance TextView fields',all('.field public '+f+':Landroid/widget/TextView;'in s for f in F))
new=[s for s in refs.splitlines()if '\tNEW_INSTANCE\t'in s and s.endswith(('L'+(NC+'MiuiMediaViewHolder').replace('.','/')+';','L'+(DI+'MiuiIslandMediaViewHolder').replace('.','/')+';'))];ck('exact two allocation sites in all DEX',len(new)==2 and any('NotificationSectionsManager;->reinflateViews()V'in x for x in new)and any('MiuiIslandMediaViewHolder$Companion;->create('in x for x in new),new)
reinflate=method('com.android.systemui.statusbar.notification.stack.NotificationSectionsManager','reinflateViews');layout=method(NC+'MiuiMediaNotificationControllerImpl','updateLayout$1');create=method(DI+'MiuiIslandMediaViewHolder$Companion','create');reset=method(DI+'MiuiIslandMediaControllerImpl','resetPlayerHolder');redo=method(DI+'MiuiIslandMediaControllerImpl','reInflateView$2')
ck('NC original allocates fields publishes attaches then resets text appearance',all('->'+f+':Landroid/widget/TextView;'in reinflate for f in F)and reinflate.index('->mediaViewHolder:')<reinflate.index('->updateLayout$1()V') and '->attach('in reinflate and '->setTextAppearance(I)V'in layout)
ck('NC layout exact no-arg void and only2 appearance writes',layout.startswith('.method public final updateLayout$1()V')and layout.count('->setTextAppearance(I)V')==2 and all('->'+f+':'in layout for f in F[:2]))
ck('Island factory returns fully populated4 TextViews',all('->'+f+':Landroid/widget/TextView;'in create for f in F)and create.rstrip().endswith('return-object p2\n.end method'))
ck('Island reset creates2 and applies both layouts before return',reset.count('MiuiIslandMediaViewHolder$Companion;->create(')==2 and all('->'+f+':'in reset for f in ['miuiPlayerHolder','miuiDummyPlayerHolder'])and reset.count('->applyTo(')==2)
ck('Island reinflate preserves detach reset attach data and view publication',all('->'+n in redo for n in ['detach()','resetPlayerHolder()','attach(','bindMediaData(','addDynamicIslandView(']))
callbackNC=method(NC+'MiuiMediaNotificationControllerImpl$configurationCallback$1','onDensityOrFontScaleChanged');callbackDI=method(DI+'MiuiIslandMediaControllerImpl$configurationCallback$1','onDensityOrFontScaleChanged');run=method(DI+'MiuiIslandMediaControllerImpl$configurationCallback$1$onDensityOrFontScaleChanged$1','run')
ck('NC fontScale callback updates height without reinstalling text appearance','->mediaLockScreenHeight:I'in callbackNC and '->setText'not in callbackNC and '->updateLayout'not in callbackNC)
ck('Island density queues reinflate without swallowing pure fontScale callback','->density:F'in callbackDI and '->densityDpi:I'in callbackDI and 'if-eq v1, v3, :cond_69'in callbackDI and '->post(Ljava/lang/Runnable;)Z'in callbackDI and '->reInflateView$2()V'in run)
listener=method(DI+'MiuiIslandMediaControllerImpl$mediaDataChangeListener$1$$ExternalSyntheticLambda0','invoke');ck('first Island media initializes through same reset','->resetPlayerHolder()V'in listener and '->attach('in listener)
maxbounds=method(NC+'MiuiMediaNotificationControllerImpl$configurationCallback$1','onMaxBoundsChanged');ck('NC bounds changes reuse exact update method','->updateLayout$1()V'in maxbounds)
for owner,name in [(NC+'MiuiMediaViewControllerImpl','setInfoText'),(DI+'MiuiIslandMediaViewBinderImpl','setInfoText'),(NC+'MiuiMediaViewControllerImpl','updateForegroundColors'),(DI+'MiuiIslandMediaViewBinderImpl','updateForegroundColors'),(NC+'MiuiMediaViewControllerImpl','onFullAodStateChanged'),(NC+'MiuiSeekBarRenderStateApplier','apply')]:
 s=method(owner,name);ck('later consumer preserves size '+owner+'#'+name,not any(x in s for x in ['->setTextSize(','->setTextAppearance(','->setAutoSize']))
for owner,name in [(NC+'MiuiMediaViewControllerImpl','onRenderState'),(DI+'MiuiIslandMediaViewBinderImpl','seekBarChanged')]:
 s=method(owner,name);ck('both time fields go to original render applier '+owner,'->elapsedTimeView:Landroid/widget/TextView;'in s and '->totalTimeView:Landroid/widget/TextView;'in s and 'MiuiSeekBarRenderStateApplier;->apply('in s)
all_text_field_consumers={line.split('\t')[1].split(';->')[0]for line in refs.splitlines()if 'MediaViewHolder;->'in line and any('->'+f+':'in line for f in F)}
expected={('L'+n.replace('.','/'))for n in [NC+'MiuiMediaNotificationControllerImpl',NC+'MiuiMediaViewControllerImpl',DI+'MiuiIslandMediaViewBinderImpl',DI+'MiuiIslandMediaViewHolder$Companion','com.android.systemui.statusbar.notification.stack.NotificationSectionsManager']};ck('all exact holder TextView references contained in5 reviewed owner classes',all_text_field_consumers==expected)
for name in ['miui_media_session','miui_media_session_island','miui_media_session_island_dummy']:
 data=subprocess.run([str(test_paths.ANDROID_HOME / 'build-tools/37.0.0/aapt2'),'dump','xmltree','--file','res/layout/'+name+'.xml',str(APK)],check=True,capture_output=True).stdout;ck('actual layout reproduced '+name,data==rb(ROM/(name+'.xml.txt')));ck('4 actual view IDs present '+name,all(x.encode()in data for x in ['@0x7f0b0534','@0x7f0b0522','@0x7f0b070e','@0x7f0b0722']))
resources=rb(test_paths.ROM_ROOT / 'indexes/systemui-resources.txt').decode();ck('stock units differ from enabled module defaults',all(re.search(r'resource '+n+r'.*?\(\) '+v,resources,re.S)for n,v in [('0x7f070a60','12.000000sp'),('0x7f070a5d','18.000000sp'),('0x7f070a51','12.000000sp'),('0x7f070a50','12.000000dp'),('0x7f070a5c','18.000000dp')]))
ns={'a':'http://schemas.android.com/apk/res/android','p':'http://schemas.android.com/apk/res-auto'};oldbase=test_paths.SNAPSHOT_ROOT / 'source-os4-hyos-test11-20261009-r1'
for name,prefix in [('system_ui_control_center_media_cards.xml','system_ui_control_center_media_control'),('system_ui_status_bar_island_media_card.xml','system_ui_island_media_control')]:
 rel='library/core/src/main/res/xml/'+name;old=rb(oldbase/rel).decode();now=rb(test_paths.SOURCE_ROOT/rel).decode();tree=ET.fromstring(now);items={x.get('{'+ns['a']+'}key'):x for x in tree.iter()if x.get('{'+ns['a']+'}key')};key='prefs_key_'+prefix+'_media_button_size_switch';ck('actual XML switch false '+name,items[key].get('{'+ns['a']+'}defaultValue')=='false')
 for suffix,default,minimum,maximum in [('title_size',180,120,360),('artist_size',120,80,240),('time_view_text_size',130,50,200)]:
  e=items['prefs_key_'+prefix+'_'+suffix];actual={k:e.get('{'+ns['p']+'}'+k)for k in ['defaultValue','minValue','maxValue','dividerValue','format','stepValue']};ck('unchanged full UI domain '+prefix+'_'+suffix,actual==dict(defaultValue=str(default),minValue=str(minimum),maxValue=str(maximum),dividerValue='10',format='%.1f sp',stepValue='1')and e.get('{'+ns['a']+'}dependency')==key)
 pattern=r'(<SeekBarPreferenceCompat\s[^>]*android:key="prefs_key_'+prefix+r'_(?:title_size|artist_size|time_view_text_size)"[^>]*app:format=")%.1f dp(")';expected,n=re.subn(pattern,r'\1%.1f sp\2',old);ck('only3 unit-label changes '+name,n==3 and expected==now)
rule=test_paths.SOURCE_ROOT / 'library/libhook/src/main/java/com/sevtinge/hyperceiler/libhook/rules/systemui/controlcenter/media3/MediaViewSize.kt';body=rb(rule).decode();old=rb(oldbase/rule.relative_to(test_paths.SOURCE_ROOT)).decode();legacy=old[old.index('    // ==================== 通知中心 ===================='):];ck('complete OS3 hook bodies unchanged',body.endswith(legacy));ck('no global TextView hooks or resource replacement','TextView::class.java)'in body and 'TextView::class.java.'not in body and 'setTextAppearance('not in body)
ck('all input bytes stable',all(sha(Path(p).read_bytes())==h for p,h in inputs.items()))
out=dict(passed=all(x['passed']for x in checks),checks=checks,check_count=len(checks),inputs=inputs,full_selected_method_sha256=methods,device_status='NOT_TESTED',scope='Original OTA complete class re-disassembly, full DEX reference scan, selected full method/resource review and production/XML contract checks; not original ART/UI execution.',limits=['The finite bytecode reference scan does not claim exclusion of arbitrary injected hooks or downloaded third-party code.','Stock NC18sp/12sp/time12sp and Island18dp/12dp differ from module enabled defaults18sp/12sp/time13sp. Switch-off after SystemUI restart preserves stock values.'])
(a.output/'report.json').write_text(json.dumps(out,indent=2)+'\n');print(len(checks),'checks',out['passed']);print([x for x in checks if not x['passed']]);raise SystemExit(0 if out['passed']else 1)

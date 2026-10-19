#!/usr/bin/env python3
"""Finite exact-OTA shape/control-flow anchors + source/UI contract. Not ART execution."""
from pathlib import Path
import argparse,hashlib,json,re,subprocess,xml.etree.ElementTree as ET

import sys
sys.path.insert(0, str(Path(__file__).resolve().parents[1]))
import hyperceiler_test_paths as test_paths

ROOT=test_paths.TEST_DATA_ROOT;SRC=test_paths.SOURCE_ROOT;ROM=test_paths.TEST_DATA_ROOT / 'acceptance/java-test12/barrage-v1/rom-v1';SM=ROM/'smali/com/xiaomi/barrage'
RULE=SRC/'library/libhook/src/main/java/com/sevtinge/hyperceiler/libhook/rules/barrage/CustomBarrageLength.kt'
PRODUCTION=[RULE,SRC/'library/libhook/src/main/java/com/sevtinge/hyperceiler/libhook/app/Barrage.java',*[SRC/'library/core/src/main/res'/p for p in ['xml/barrage.xml','values/strings_app.xml','values-zh-rCN/strings_app.xml']]]
sha=lambda p:hashlib.sha256(p.read_bytes()).hexdigest()
def main(out):
 out.mkdir(parents=True,exist_ok=False); checks=[]; methods=[]
 def ck(name,value):checks.append({'name':name,'passed':bool(value)})
 def method(path,signature):
  p=SM/path;text=p.read_text();hits=[m for m in re.finditer(r'(?ms)^\.method [^\n]+\n.*?^\.end method',text)if m[0].splitlines()[0].endswith(' '+signature)]
  assert len(hits)==1,(path,signature);m=hits[0];b=m[0];methods.append({'path':str(test_paths.report_path(p, ROOT)),'method':signature,'start_line':text[:m.start()].count('\n')+1,'end_line':text[:m.end()].count('\n')+1,'complete_body_sha256':hashlib.sha256(b.encode()).hexdigest()});return b
 extraction=json.loads((ROM/'rom-extraction.json').read_text());ck('actual_APK_sha256',sha(Path(extraction['apk']))==extraction['sha256']=='a5224991c3d8d1682863bc6cd3354fa69eefbd09fafd5b3a24b503b1a26fa9d5')
 ck('all_extracted_classes_current',all(sha(ROM/p)==h for p,h in extraction['files'].items()))
 w='utils/BarrageWindowUtils.smali';b='danmu/model/BaseBarrage.smali';f='danmu/model/android/DanmuFactory.smali';manager='manager/BarrageShowManager.smali'
 notify=method(w,'addBarrageNotification(Landroid/service/notification/StatusBarNotification;Z)V');send=method(w,'sendBarrage(Ljava/lang/String;Z)V');span=method(w,'createSpannable(Landroid/graphics/drawable/Drawable;Ljava/lang/String;)Landroid/text/SpannableStringBuilder;');cut=method(w,'cutWeChatMsg(Ljava/lang/String;)Ljava/lang/String;');pub=method(manager,'addDanmu(Lcom/xiaomi/barrage/danmu/model/BaseBarrage;)V')
 for label,body,reg in [('notification',notify,'v0'),('send',send,'v2')]:
  ck(label+'_factory_type1',re.search(r'const/4 (v\d+), 0x1\b',body) is not None and '->createDanmu(I)'in body)
  ck(label+'_one_direct_publication',body.count('->addDanmu(')==1 and 'invoke-virtual'in body)
  ck(label+'_no_asynchronous_handoff_in_outer',not any(x in body for x in ['Landroid/os/Handler;','Ljava/lang/Thread;','Ljava/util/concurrent/','Lio/reactivex/']))
  after=body.split('->createDanmu(I)',1)[1]
  passed=[line.strip()for line in after.splitlines()if 'invoke-'in line and re.search(r'\{[^}]*\b'+reg+r'\b',line)]
  ck(label+'_fresh_object_only_passed_to_setTime_and_addDanmu',len(passed)==2 and all('->setTime(J)'in x or '->addDanmu('in x for x in passed))
  ck(label+'_fresh_object_not_stored_elsewhere',not re.search(r'\b(?:iput-object|sput-object|aput-object) '+reg+r'[, ]',after))
  ck(label+'_keeps_styles_time_alpha_font',all(x in body for x in ['bubbleStylePosition:I','barrageAlpha:F','textSize:F','->setTime(J)V','->getBarrageViewAlpha()F']))
 ck('notification_original_31_and_11_thresholds', '0x1f'in notify and '0xb'in notify and notify.count('->subSequence(II)')==2 and '->currentOrientation:I'in notify)
 ck('send_original_31_threshold', '0x1f'in send and send.count('->subSequence(II)')==1)
 ck('notification_flattens_input_CharSequence',notify.count('Ljava/lang/String;->valueOf(Ljava/lang/Object;)')==2)
 ck('notification_original_wechat_and_whatsapp_gates',all(x in notify for x in ['com.tencent.mm','com.whatsapp','->cutWeChatMsg(', '->getTag()Ljava/lang/String;']))
 ck('normalization_after_style_selection_before_publication',notify.index('->replaceAll(')<notify.index('->createSpannable(')<notify.index('->barrageMsg:')<notify.index('->addDanmu(') and send.index('->replaceAll(')<send.index('->text:')<send.index('->addDanmu('))
 ck('original_icon_type_range_flags',all(x in span for x in ['"icon"','Lcom/xiaomi/barrage/ui/view/ImageSpanCentre;','const/4 v3, 0x2','const/16 v1, 0x11','const/4 v3, 0x0','->length()I','->setSpan(Ljava/lang/Object;III)V']))
 ck('span_only_original_icon_before_append',span.count('->setSpan(')==1 and span.index('->setSpan(')<span.index('->append('))
 ctor=method('danmu/model/R2LBarrage.smali','<init>(Lcom/xiaomi/barrage/danmu/model/Duration;)V');base_ctor=method(b,'<init>()V');time=method(b,'setTime(J)V')
 ck('R2L_constructor_does_not_publish_this',len(re.findall(r'(?m)^\s*invoke-',ctor))==1 and 'BaseBarrage;-><init>()V'in ctor and not re.search(r'(?:iput-object|sput-object|aput-object) p0[, ]',ctor))
 ck('Base_constructor_does_not_publish_this',not re.search(r'(?:iput-object|sput-object|aput-object) p0[, ]',base_ctor) and all('Ljava/lang/Object;-><init>()'in x or 'Landroid/util/SparseArray;-><init>()'in x for x in base_ctor.splitlines()if 'invoke-'in x))
 ck('setTime_has_no_handoff',not re.search(r'(?m)^\s*invoke-',time) and '->time:J'in time and '->timeOffset:J'in time)
 core=method(f,'createDanmu(IFFFF)Lcom/xiaomi/barrage/danmu/model/BaseBarrage;')
 ck('factory_type1_new_R2L_not_pool',re.search(r'const/4 p4, 0x1\s+if-eq p1, p4, :cond_90',core)and re.search(r':cond_90\s+new-instance p1, Lcom/xiaomi/barrage/danmu/model/R2LBarrage;',core)and core[core.rindex(':cond_90\n'):].count('invoke-')==1)
 for sig in ['createDanmu(I)Lcom/xiaomi/barrage/danmu/model/BaseBarrage;','createDanmu(ILcom/xiaomi/barrage/danmu/model/android/DanmuContext;)Lcom/xiaomi/barrage/danmu/model/BaseBarrage;','createDanmu(ILcom/xiaomi/barrage/danmu/model/IDisplayer;FF)Lcom/xiaomi/barrage/danmu/model/BaseBarrage;','createDanmu(IIIFF)Lcom/xiaomi/barrage/danmu/model/BaseBarrage;']:method(f,sig)
 ck('manager_owns_original_schedule_and_synchronization',all(x in pub for x in ['declared-synchronized','monitor-enter p0','maxShowDanmuNumber:I','coolingTime:J','0x4b0','IBarrageView;->addDanmaku(']))
 red=method(w,'setTextTransparent(IZ)V');red2=method('utils/BarrageWindowUtils$11.smali','accept(Lcom/xiaomi/barrage/danmu/model/BaseBarrage;)I');reply=method(w,'showReplyWindow(Lcom/xiaomi/barrage/danmu/model/BaseBarrage;)V')
 ck('both_redraw_consumers_read_cache',all('->barrageMsg:Ljava/lang/String;'in x and '->text:Ljava/lang/CharSequence;'in x for x in [red,red2]))
 ck('reply_seed_reads_display_cache', '->barrageMsg:Ljava/lang/String;'in reply and '->setText(Ljava/lang/CharSequence;)'in reply)
 for sig in ['initFloatView()V','updateReplyWindowParams()V']:
  text=method(w,sig);ck(sig+'_inputfilter_uses_original_maxLength','Landroid/text/InputFilter$LengthFilter;'in text and '->maxLength:I'in text)
 service=method('service/NotificationMonitorService.smali','handleNotification(Landroid/service/notification/StatusBarNotification;)V')
 ck('notification_service_keeps_filter_before_local_entry',service.index('->filterNotification(')<service.index('->addBarrageNotification('))
 refs=(ROM/'all-dex-xrefs.txt').read_text().splitlines();ck('all_DEX_xref_count_45',len(refs)==45);ck('exact_single_notification_caller',sum('INVOKE_'in x and x.split('\t')[-1].startswith('Lcom/xiaomi/barrage/utils/BarrageWindowUtils;->addBarrageNotification(') for x in refs)==1);ck('no_encoded_send_caller_in_actual_DEX',not any(x.split('\t')[-1].startswith('Lcom/xiaomi/barrage/utils/BarrageWindowUtils;->sendBarrage(') for x in refs))
 xml=ET.parse(PRODUCTION[2]).getroot();android='{http://schemas.android.com/apk/res/android}';app='{http://schemas.android.com/apk/res-auto}';node=[e for e in xml.iter()if e.get(android+'key')=='prefs_key_barrage_custom_barrage_length'];ck('one_actual_UI_key',len(node)==1);node=node[0];ck('UI_range_sentinel_and_no_dp',node.get(android+'defaultValue')=='36'and node.get(app+'minValue')=='12'and node.get(app+'maxValue')=='108'and node.get(app+'format')=='%s')
 rule=RULE.read_text();route=PRODUCTION[1].read_text();ck('route_correctkey_and_range','getInt("barrage_custom_barrage_length", 36)'in route and 'length >= 12 && length <= 108 && length != 36'in route);ck('OS4_exact_version_branch','if (!isMoreHyperOSVersion(4f)) {\n            initLegacy()\n            return'in rule)
 old=subprocess.check_output(['git','show','HEAD:library/libhook/src/main/java/com/sevtinge/hyperceiler/libhook/rules/barrage/CustomBarrageLength.kt'],cwd=SRC,text=True);expected=old[old.index('    private val barrageLength by lazy'):].replace('"barrage_length"','"barrage_custom_barrage_length"').replace('    override fun init() {','    private fun initLegacy() {');ck('OS3_legacy_body_exact_except_correct_UI_key_and_method_name',rule[rule.index('    private val barrageLength by lazy'):]==expected)
 for p in PRODUCTION[3:]:ck(p.parent.name+'_summary_default_units_restart',any(e.get('name')=='barrage_custom_barrage_length_desc'and e.text for e in ET.parse(p).getroot()))
 inputs=[Path(__file__),ROM/'extract_rom.py',ROM/'rom-extraction.json',ROM/'DexRefs.java',ROM/'all-dex-xrefs.txt',*PRODUCTION,*[ROM/p for p in extraction['files']]]
 report={'passed':all(x['passed']for x in checks),'checks':checks,'sources':{str(p.relative_to(SRC)):sha(p)for p in PRODUCTION},'inputs':{str(test_paths.report_path(p, ROOT)):sha(p)for p in inputs},'actual_apk':{'path':extraction['apk'],'sha256':extraction['sha256']},'complete_methods':methods,'extracted_classes':len(extraction['files']),'xref_scope':'Every instruction in every DEX in this APK; only five named methods plus BaseBarrage.text/barrageMsg selected. 45 references. Not reflection/native/dynamic-code completeness.','scope':'Finite deterministic anchors plus saved complete method bodies, supplementing the manual chain review. No ART, LSPosed or device execution.','device_status':'NOT_TESTED'}
 (out/'report.json').write_text(json.dumps(report,indent=2)+'\n');print('checks',len(checks),'failed',[x['name']for x in checks if not x['passed']]);assert report['passed']
if __name__=='__main__':
 p=argparse.ArgumentParser();p.add_argument('--output',required=True,type=Path);main(p.parse_args().output)

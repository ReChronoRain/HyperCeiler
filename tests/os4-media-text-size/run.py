#!/usr/bin/env python3
from pathlib import Path
import argparse,hashlib,json,os,re,subprocess,tempfile

import sys
sys.path.insert(0, str(Path(__file__).resolve().parents[1]))
import hyperceiler_test_paths as test_paths

BASE=Path(__file__).resolve().parent;SOURCE=BASE.parents[1];ROOT=test_paths.TEST_DATA_ROOT;CACHE=test_paths.BUILD_CACHE;JAVA=test_paths.JAVA_HOME / 'bin';KH=test_paths.KOTLIN_HOME
RULE=SOURCE/'library/libhook/src/main/java/com/sevtinge/hyperceiler/libhook/rules/systemui/controlcenter/media3/MediaViewSize.kt'
DEPS=[CACHE/'changed-hooks-compile'/x for x in ['hook-xposed-102-1.3.0.jar','core-1.3.0.jar','api-102.0.0.jar']]+[KH/'lib/kotlin-stdlib.jar',test_paths.ANDROID_HOME / 'platforms/android-37.0/android.jar']+list((CACHE/'gradle-home/caches/modules-2/files-2.1/org.jetbrains.kotlinx/kotlinx-coroutines-core-jvm/1.9.0').glob('*/*.jar'))
ROUTE=SOURCE/'library/libhook/src/main/java/com/sevtinge/hyperceiler/libhook/app/SystemUI/SystemUIB.java'
XML=[SOURCE/'library/core/src/main/res/xml'/n for n in ['system_ui_control_center_media_cards.xml','system_ui_status_bar_island_media_card.xml']]
sha=lambda p:hashlib.sha256(p.read_bytes()).hexdigest()
def inputs():return {str(test_paths.report_path(p, ROOT)):sha(p) for p in [RULE,ROUTE,*XML,Path(__file__),*sorted((BASE/'fixtures').rglob('*'))]if p.is_file()}
def run(out,mutation=None):
 out.mkdir(parents=True,exist_ok=False);before=inputs();body=RULE.read_text()
 if mutation == 'ignore_exact_receiver':
  for old in [' || param.thisObject?.javaClass != owner','if (receiver?.javaClass != owner) return']:
   assert body.count(old)==1,(mutation,old);body=body.replace(old,'')
 elif mutation and mutation!='outer_gate_NC_only':
  old,new=MUTANTS[mutation];assert body.count(old)==1,(mutation,body.count(old));body=body.replace(old,new)
 with tempfile.TemporaryDirectory(prefix='hc-media-')as t:
  w=Path(t);classes=w/'classes';classes.mkdir();cp=os.pathsep.join(map(str,DEPS));allcp=str(classes)+os.pathsep+cp
  route=SOURCE/'library/libhook/src/main/java/com/sevtinge/hyperceiler/libhook/app/SystemUI/SystemUIB.java'
  line=next(x.strip()for x in route.read_text().splitlines()if 'initHook(MediaViewSize.INSTANCE,'in x)
  if mutation=='outer_gate_NC_only':line=line.replace(' || PrefsBridge.getBoolean("system_ui_island_media_control_media_button_size_switch")','')
  routejava=w/'Route.java';routejava.write_text('import com.sevtinge.hyperceiler.common.utils.PrefsBridge;import com.sevtinge.hyperceiler.libhook.rules.systemui.controlcenter.media3.MediaViewSize; public class Route { public static boolean enabled; static void initHook(Object o,boolean b){enabled=b;} public static void run(){'+line+'}}')
  files=[p for p in(BASE/'fixtures').rglob('*.java')if p.name not in ['TestMain.java','Targets.java']];cmd=[str(JAVA/'javac'),'--release','21','-proc:none','-cp',cp,'-d',str(classes),*map(str,files)]
  commands=[cmd];r=subprocess.run(cmd,capture_output=True,text=True);logs=r.stdout+r.stderr;assert r.returncode==0,logs
  prod=w/RULE.name;prod.write_text(body);cmd=[str(JAVA/'java'),'-cp',str(KH/'lib/kotlin-compiler.jar'),'org.jetbrains.kotlin.cli.jvm.K2JVMCompiler','-kotlin-home',str(KH),'-jvm-target','21','-classpath',allcp,'-d',str(classes),str(prod),*map(str,(BASE/'fixtures').rglob('*.kt'))];commands.append(cmd);r=subprocess.run(cmd,capture_output=True,text=True);logs+=r.stdout+r.stderr;(out/'build.txt').write_text(logs);assert r.returncode==0,logs
  cmd=[str(JAVA/'javac'),'--release','21','-proc:none','-cp',allcp,'-d',str(classes),str(BASE/'fixtures/TestMain.java'),str(BASE/'fixtures/Targets.java'),str(routejava)];commands.append(cmd);r=subprocess.run(cmd,capture_output=True,text=True);logs+=r.stdout+r.stderr;(out/'build.txt').write_text(logs);assert r.returncode==0,logs
  r=subprocess.run([str(JAVA/'java'),'-ea','-cp',allcp,'TestMain'],capture_output=True,text=True,timeout=60);(out/'tests.txt').write_text(r.stdout+r.stderr);print(r.stdout[-500:]);m=re.search(r'RESULT passes=(\d+) failures=(\d+)',r.stdout);assert m,r.stdout+r.stderr
  after=inputs();report={'passed':r.returncode==0 and int(m[2])==0 and before==after,'tests':{'passed':int(m[1]),'failed':int(m[2])},'exit_code':r.returncode,'inputs_before':before,'inputs_after':after,'inputs_unchanged':before==after,'dependencies':{str(p):sha(p)for p in DEPS},'commands':commands,'mutation':mutation,'device_status':'NOT_TESTED','scope':'Complete production Kotlin object; real EzHook1.3.0/API102 callback chain. Android TextViews/SP conversion, preferences/version, ROM-mapped controller bodies and hook installation backend are host fixtures; the OS3 helpers are compile-only fixtures; no ROM DEX/ART/LSPosed/device execution.'};(out/'report.json').write_text(json.dumps(report,indent=2)+'\n');return report
MUTANTS={
 'outer_gate_NC_only':None,
 'obsolete_inner_switch':('prefix + "_media_button_size_switch"','prefix + "_text_size"'),
 'NC_controls_both_domains':('if (!PrefsBridge.getBoolean(prefix + "_media_button_size_switch")) continue','if (!PrefsBridge.getBoolean("system_ui_control_center_media_control_media_button_size_switch")) continue'),
 'island_reads_NC_sizes':('if (island) "system_ui_island_media_control" else "system_ui_control_center_media_control"','if (island) "system_ui_control_center_media_control" else "system_ui_control_center_media_control"'),
 'omit_island_dummy':('listOf("miuiPlayerHolder", "miuiDummyPlayerHolder")','listOf("miuiPlayerHolder")'),
 'omit_elapsed':('"artistText", "elapsedTimeView", "totalTimeView"','"artistText", "totalTimeView"'),
 'omit_total':('"artistText", "elapsedTimeView", "totalTimeView"','"artistText", "elapsedTimeView"'),
 'time_uses_artist':('val time = readSize(prefix + "_time_view_text_size", 130, 50..200)','val time = readSize(prefix + "_artist_size", 120, 80..240)'),
 'wrong_unit_DP':('views[index].setTextSize(TypedValue.COMPLEX_UNIT_SP, sizes[index])','views[index].setTextSize(TypedValue.COMPLEX_UNIT_DIP, sizes[index])'),
 'integer_truncation':('return value / 10f','return (value / 10).toFloat()'),
 'no_configuration_reapply':('plans.add(fontScale to afterOriginal(active, configuration) { group.apply(controller.get(it)) })','/* no configuration hook */'),
 'before_instead_of_after':('override fun after(param: HookParam)','override fun before(param: HookParam)'),
 'ignore_original_exception':(' || param.hasThrowable',''),
 'ignore_prior_skip':(' || param.isSkipped',''),
 'ignore_exact_receiver':(' || param.thisObject?.javaClass != owner',''),
 'activate_partial':('val active = AtomicBoolean(false)','val active = AtomicBoolean(true)'),
 'omit_rollback':('try { handle.unhook() } catch (rollback: RuntimeException) { error.addSuppressed(rollback) }','try { /* retain registration */ } catch (rollback: RuntimeException) { error.addSuppressed(rollback) }'),
 'accept_invalid_range':('check(value in range) { "Invalid media text size: $key" }','/* no range check */'),
 'route_OS4_to_legacy':('if (isMoreHyperOSVersion(4f)) {','if (false) {'),
}
if __name__=='__main__':
 p=argparse.ArgumentParser();p.add_argument('--output',type=Path,required=True);p.add_argument('--negative-controls',action='store_true');a=p.parse_args();r=run(a.output);assert r['passed']
 if a.negative_controls:
  rows=[]
  for m in MUTANTS:
   q=run(a.output/m,m);rows.append({'mutation':m,'rejected':not q['passed'] and q['tests']['failed']>0 and q['inputs_unchanged'],'failed_tests':q['tests']['failed']})
  v={'passed':all(x['rejected']for x in rows),'checks':rows,'scope':'In-memory source-copy regressions. Production file unchanged.'};(a.output/'negative-controls.json').write_text(json.dumps(v,indent=2)+'\n');assert v['passed']

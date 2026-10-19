#!/usr/bin/env python3
from pathlib import Path
import argparse,hashlib,json,os,re,subprocess,tempfile

import sys
sys.path.insert(0, str(Path(__file__).resolve().parents[1]))
import hyperceiler_test_paths as test_paths

BASE=Path(__file__).resolve().parent;SOURCE=BASE.parents[1];ROOT=test_paths.TEST_DATA_ROOT;CACHE=test_paths.BUILD_CACHE;JAVA=test_paths.JAVA_HOME / 'bin';KH=test_paths.KOTLIN_HOME
RULE=SOURCE/'library/libhook/src/main/java/com/sevtinge/hyperceiler/libhook/rules/systemui/controlcenter/CustomCarrierText.kt'
DEPS=[CACHE/'changed-hooks-compile'/x for x in ['hook-xposed-102-1.3.0.jar','core-1.3.0.jar','api-102.0.0.jar']]+[KH/'lib/kotlin-stdlib.jar',test_paths.ANDROID_HOME / 'platforms/android-37.0/android.jar']
sha=lambda p:hashlib.sha256(p.read_bytes()).hexdigest()
def inputs():return {str(test_paths.report_path(p, ROOT)):sha(p) for p in [RULE,Path(__file__),*sorted((BASE/'fixtures').rglob('*'))]if p.is_file()}
def run(out,mutation=None):
 out.mkdir(parents=True,exist_ok=False);before=inputs();body=RULE.read_text()
 if mutation:
  old,new=MUTANTS[mutation];assert body.count(old)==1,(mutation,body.count(old));body=body.replace(old,new)
 with tempfile.TemporaryDirectory(prefix='hc-carrier-')as t:
  w=Path(t);classes=w/'classes';classes.mkdir();cp=os.pathsep.join(map(str,DEPS));allcp=str(classes)+os.pathsep+cp
  files=[p for p in(BASE/'fixtures').rglob('*.java')if p.name!='TestMain.java'];cmd=[str(JAVA/'javac'),'--release','21','-proc:none','-cp',cp,'-d',str(classes),*map(str,files)]
  commands=[cmd];r=subprocess.run(cmd,capture_output=True,text=True);logs=r.stdout+r.stderr;assert r.returncode==0,logs
  prod=w/RULE.name;prod.write_text(body);cmd=[str(JAVA/'java'),'-cp',str(KH/'lib/kotlin-compiler.jar'),'org.jetbrains.kotlin.cli.jvm.K2JVMCompiler','-kotlin-home',str(KH),'-jvm-target','21','-classpath',allcp,'-d',str(classes),str(prod),str(BASE/'fixtures/Visible.kt')];commands.append(cmd);r=subprocess.run(cmd,capture_output=True,text=True);logs+=r.stdout+r.stderr;(out/'build.txt').write_text(logs);assert r.returncode==0,logs
  cmd=[str(JAVA/'javac'),'--release','21','-proc:none','-cp',allcp,'-d',str(classes),str(BASE/'fixtures/TestMain.java')];commands.append(cmd);r=subprocess.run(cmd,capture_output=True,text=True);logs+=r.stdout+r.stderr;(out/'build.txt').write_text(logs);assert r.returncode==0,logs
  r=subprocess.run([str(JAVA/'java'),'-ea','-cp',allcp,'TestMain'],capture_output=True,text=True,timeout=60);(out/'tests.txt').write_text(r.stdout+r.stderr);print(r.stdout[-500:]);m=re.search(r'RESULT passes=(\d+) failures=(\d+)',r.stdout);assert m,r.stdout+r.stderr
  after=inputs();report={'passed':r.returncode==0 and int(m[2])==0 and before==after,'tests':{'passed':int(m[1]),'failed':int(m[2])},'exit_code':r.returncode,'inputs_before':before,'inputs_after':after,'inputs_unchanged':before==after,'dependencies':{str(p):sha(p)for p in DEPS},'commands':commands,'mutation':mutation,'device_status':'NOT_TESTED','scope':'Complete production Kotlin object; real EzHook1.3.0/API102 callback chain. Android views, version/prefs/property source, target classes and hook installation backend are host fixtures; no ROM DEX/ART/LSPosed/device execution.'};(out/'report.json').write_text(json.dumps(report,indent=2)+'\n');return report
MUTANTS={
 'legacy_argument_roles':('val text = param.args[1] as? String','val text = param.args[2] as? String'),
 'overwrite_slot':('param.args[1] = when (mode)','param.args[2] = when (mode)'),
 'remove_unknown_status_guard':('carrierCount !in 0..2 || slot !in 0..1','slot !in 0..1'),
 'wrong_primary_slot':('else if (slot == 0) name else ""','else if (slot == 1) name else ""'),
 'discard_empty_name_guard':('if (name.isNullOrBlank()) text else if','if (false) text else if'),
 'hide_measure_by_skip':('override fun after(param: HookParam) {\n                        if (!active.get() || param.hasThrowable) return','override fun before(param: HookParam) {\n                        param.result = null\n                        if (!active.get() || param.hasThrowable) return'),
 'drop_text_separator':('views = listOf(viewField(layout, "carrierSeparatorView"), viewField(layout, "carrierSeparatorText"))','views = listOf(viewField(layout, "carrierSeparatorView"))'),
 'drop_rollback':('try { handle.unhook() } catch (rollback: RuntimeException) { error.addSuppressed(rollback) }','try { /* retain registration */ } catch (rollback: RuntimeException) { error.addSuppressed(rollback) }'),
 'count_dependent_display_slot':('else if (slot == 0) name else ""','else if (carrierCount == 1 || slot == 0) name else ""'),
 'empty_primary_slot_skipped':('if (mode != 3 && text.isNullOrEmpty()) return','if (text.isNullOrEmpty()) return'),
 'drop_os4_gate':('if (isMoreHyperOSVersion(4f)) {','if (true) {'),
}
if __name__=='__main__':
 p=argparse.ArgumentParser();p.add_argument('--output',type=Path,required=True);p.add_argument('--negative-controls',action='store_true');a=p.parse_args();r=run(a.output);assert r['passed']
 if a.negative_controls:
  rows=[]
  for m in MUTANTS:
   q=run(a.output/m,m);rows.append({'mutation':m,'rejected':not q['passed'] and q['tests']['failed']>0 and q['inputs_unchanged'],'failed_tests':q['tests']['failed']})
  v={'passed':all(x['rejected']for x in rows),'checks':rows,'scope':'In-memory source-copy regressions. Production file unchanged.'};(a.output/'negative-controls.json').write_text(json.dumps(v,indent=2)+'\n');assert v['passed']

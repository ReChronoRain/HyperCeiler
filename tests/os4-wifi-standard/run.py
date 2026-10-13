#!/usr/bin/env python3
from pathlib import Path
import argparse,hashlib,json,os,re,subprocess,tempfile

import sys
sys.path.insert(0, str(Path(__file__).resolve().parents[1]))
import hyperceiler_test_paths as test_paths

BASE=Path(__file__).resolve().parent;SOURCE=BASE.parents[1];ROOT=test_paths.TEST_DATA_ROOT;CACHE=test_paths.BUILD_CACHE;JAVA=test_paths.JAVA_HOME / 'bin';KH=test_paths.KOTLIN_HOME
RULE=SOURCE/'library/libhook/src/main/java/com/sevtinge/hyperceiler/libhook/rules/systemui/statusbar/icon/v/WifiStandard.kt'
DEPS=[CACHE/'changed-hooks-compile'/x for x in ['hook-xposed-102-1.3.0.jar','core-1.3.0.jar','api-102.0.0.jar']]+[KH/'lib/kotlin-stdlib.jar',test_paths.ANDROID_HOME / 'platforms/android-37.0/android.jar']+list((CACHE/'gradle-home/caches').glob('*/transforms/*/transformed/dexkit-*/jars/classes.jar'))+list((CACHE/'gradle-home/caches/modules-2/files-2.1/org.jetbrains.kotlinx/kotlinx-coroutines-core-jvm/1.9.0').glob('*/*.jar'))
sha=lambda p:hashlib.sha256(p.read_bytes()).hexdigest()
def inputs():return {str(test_paths.report_path(p, ROOT)):sha(p) for p in [RULE,Path(__file__),*sorted((BASE/'fixtures').rglob('*'))]if p.is_file()}
def run(out,mutation=None):
 out.mkdir(parents=True,exist_ok=False);before=inputs();body=RULE.read_text()
 if mutation:
  old,new=MUTANTS[mutation];assert body.count(old)==1,(mutation,body.count(old));body=body.replace(old,new)
 with tempfile.TemporaryDirectory(prefix='hc-wifi-')as t:
  w=Path(t);classes=w/'classes';classes.mkdir();cp=os.pathsep.join(map(str,DEPS));allcp=str(classes)+os.pathsep+cp
  files=[p for p in(BASE/'fixtures').rglob('*.java')if p.name!='TestMain.java'];cmd=[str(JAVA/'javac'),'--release','21','-proc:none','-cp',cp,'-d',str(classes),*map(str,files)]
  commands=[cmd];r=subprocess.run(cmd,capture_output=True,text=True);logs=r.stdout+r.stderr;(out/'build.txt').write_text(logs);assert r.returncode==0,logs
  prod=w/RULE.name;prod.write_text(body);cmd=[str(JAVA/'java'),'-cp',str(KH/'lib/kotlin-compiler.jar'),'org.jetbrains.kotlin.cli.jvm.K2JVMCompiler','-kotlin-home',str(KH),'-jvm-target','21','-classpath',allcp,'-d',str(classes),str(prod),str(BASE/'fixtures/TestMain.kt')];commands.append(cmd);r=subprocess.run(cmd,capture_output=True,text=True);logs+=r.stdout+r.stderr;(out/'build.txt').write_text(logs);assert r.returncode==0,logs
  r=subprocess.run([str(JAVA/'java'),'-ea','-cp',allcp,'TestMainKt'],capture_output=True,text=True,timeout=60);(out/'tests.txt').write_text(r.stdout+r.stderr);print(r.stdout[-1600:]);m=re.search(r'RESULT passes=(\d+) failures=(\d+)',r.stdout);assert m,r.stdout+r.stderr
  after=inputs();report={'passed':r.returncode==0 and int(m[2])==0 and before==after,'tests':{'passed':int(m[1]),'failed':int(m[2])},'exit_code':r.returncode,'inputs_before':before,'inputs_after':after,'inputs_unchanged':before==after,'dependencies':{str(p):sha(p)for p in DEPS},'commands':commands,'mutation':mutation,'device_status':'NOT_TESTED','scope':'Complete production Kotlin object; real EzHook1.3.0/API102 callbacks and kotlinx-coroutines-core1.9.0 suspension/resume/cancellation. R8 field shapes/producer branches and ART hook backend are explicit host fixtures; no original ROM DEX/Android views/ART/LSPosed/device execution.'};(out/'report.json').write_text(json.dumps(report,indent=2)+'\n');return report
MUTANTS={
 'wrong_outer_id':('outerId.getInt(param.thisObject) != 3','outerId.getInt(param.thisObject) != 2'),
 'remove_outer_id_guard':('outerId.getInt(param.thisObject) != 3','false'),
 'remove_combine_id_guard':('combineId.getInt(upstream) != 2','false'),
 'remove_transform_guard':('if (transform.get(upstream)?.javaClass != producer) return','if (false) return'),
 'remove_flow_count_guard':('flows.size != 5 || flows.any { !flow.isInstance(it) }','flows.any { !flow.isInstance(it) }'),
 'drop_null_flow_guard':('flows.size != 5 || flows.any { !flow.isInstance(it) }','flows.size != 5'),
 'wrong_standard_hide_value':('param.result is Int) param.result = 0','param.result is Int) param.result = 6'),
 'hide_non_integer_results':('param.result is Int) param.result = 0','param.result is Number) param.result = 0'),
 'drop_skip_guard':('if (param.isSkipped) return','if (false) return'),
 'double_collect_after_exception':('param.throwable = error.targetException','debugCallbackError("mutant swallowed upstream", error)'),
 'wrap_original_exception':('param.throwable = error.targetException','param.throwable = error'),
 'keep_sync_nonunit_result':('if (result === suspended) suspended else unit','if (result === suspended) suspended else result'),
 'return_unit_for_suspension':('if (result === suspended) suspended else unit','unit'),
 'use_old_dexkit_on_os4':('override fun useDexKit() = !isMoreHyperOSVersion(4f)','override fun useDexKit() = true'),
 'run_old_lookup_in_os4_init':('if (!isMoreHyperOSVersion(4f)) makeWifiStandardZero','makeWifiStandardZero'),
}
if __name__=='__main__':
 p=argparse.ArgumentParser();p.add_argument('--output',type=Path,required=True);p.add_argument('--negative-controls',action='store_true');a=p.parse_args();r=run(a.output);assert r['passed']
 if a.negative_controls:
  rows=[]
  for m in MUTANTS:
   q=run(a.output/m,m);rows.append({'mutation':m,'rejected':not q['passed'] and q['tests']['failed']>0 and q['inputs_unchanged'],'failed_tests':q['tests']['failed']})
  v={'passed':all(x['rejected']for x in rows),'checks':rows,'scope':'In-memory source-copy regressions. Production file unchanged.'};(a.output/'negative-controls.json').write_text(json.dumps(v,indent=2)+'\n');assert v['passed']

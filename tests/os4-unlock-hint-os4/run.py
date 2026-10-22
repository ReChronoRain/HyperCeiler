#!/usr/bin/env python3
"""Production Kotlin + real EzHook/API102; OEM queue execution is a separate ART suite."""
from pathlib import Path
import argparse,concurrent.futures,hashlib,json,os,re,subprocess,tempfile

import sys
sys.path.insert(0, str(Path(__file__).resolve().parents[1]))
import hyperceiler_test_paths as test_paths

BASE=Path(__file__).resolve().parent;SOURCE=BASE.parents[1];ROOT=test_paths.TEST_DATA_ROOT;CACHE=test_paths.BUILD_CACHE;JAVA=test_paths.JAVA_HOME / 'bin';KH=test_paths.KOTLIN_HOME
RULE=SOURCE/'library/libhook/src/main/java/com/sevtinge/hyperceiler/libhook/rules/systemui/lockscreen/HideLockScreenHintOS4.kt';ROUTE=RULE.with_name('HideLockScreenHint.kt')
DEPS=[CACHE/'changed-hooks-compile'/x for x in ['hook-xposed-102-1.3.0.jar','core-1.3.0.jar','api-102.0.0.jar']]+[KH/'lib/kotlin-stdlib.jar',test_paths.ANDROID_HOME / 'platforms/android-37.0/android.jar']
SDK_REPORT=test_paths.TEST_DATA_ROOT / 'acceptance/java-test13/ambient-runtime-v1/sdk-first-v1/report.json'
sha=lambda p:hashlib.sha256(p.read_bytes()).hexdigest()
def inputs():return {str(test_paths.report_path(p, ROOT)):sha(p)for p in [RULE,ROUTE,SDK_REPORT,Path(__file__),*sorted((BASE/'fixtures').rglob('*'))]if p.is_file()and '__pycache__'not in str(p)}
MUTANTS={
 'wrong_pref_key':('rule','"system_ui_lock_screen_unlock_tip"','"system_ui_unlock_tip"','disabled_no_class_resolution'),
 'wrong_type':('rule','param.args[0] != 13','param.args[0] != 6','standard_null_input_original_once'),
 'lose_theme_gate':('rule','indication.javaClass != value || theme.invoke(null) != true','indication.javaClass != value','maml_theme_retained'),
 'lose_bottom_id':('rule','bottom.id != bottomId','false','top_view_retained'),
 'lose_current_locale_match':('rule','text != resources.getString(textId)','false','other_message_retained'),
 'allow_action_hint':('rule','action.get(indication) != null','false','unknown_contract_field_1'),
 'lose_preskip_gate':('rule',' || param.isSkipped','', 'before_skip_no_argument_change'),
 'replace_method_instead_of_null':('rule','param.args[1] = null','param.result = null','standard_null_input_original_once'),
 'lose_cleanup_gate':('rule','registerHotReloadCleanup { active.set(false) }','registerHotReloadCleanup { }','cleanup_next_refresh_restores_standard_input'),
 'early_active':('rule','AtomicBoolean(false)','AtomicBoolean(true)','hook_inactive_until_commit'),
 'OS4_legacy_route':('route','if (isMoreHyperOSVersion(4f)) {','if (false) {','OS4_route_never_legacy_fingerprint'),
}
def run(out,mutation=None,sdk=True):
 out.mkdir(parents=True,exist_ok=False);before=inputs();commands=[];logs='';bodies=[RULE.read_text(),ROUTE.read_text()]
 if mutation:
  target,old,new,_=MUTANTS[mutation];idx=0 if target=='rule'else 1;assert bodies[idx].count(old)==1;bodies[idx]=bodies[idx].replace(old,new)
 with tempfile.TemporaryDirectory(prefix='hc-unlock-hint-')as t:
  w=Path(t);classes=w/'classes';classes.mkdir();cp=os.pathsep.join(map(str,DEPS));allcp=str(classes)+os.pathsep+cp
  def cmd(c):
   nonlocal logs
   c=list(map(str,c));commands.append(c);p=subprocess.run(c,capture_output=True,text=True);logs+=p.stdout+p.stderr;(out/'build.txt').write_text(logs);assert p.returncode==0,logs
  def kotlin(dest,classpath,src):cmd([JAVA/'java','-cp',KH/'lib/kotlin-compiler.jar','org.jetbrains.kotlin.cli.jvm.K2JVMCompiler','-kotlin-home',KH,'-jvm-target','21','-classpath',classpath,'-d',dest,*src])
  prods=[]
  for p,body in zip([RULE,ROUTE],bodies):q=w/p.name;q.write_text(body);prods.append(q)
  sdkdeps=list(json.loads(SDK_REPORT.read_text())['dependencies'])
  if sdk:
   sdkout=w/'sdk';sdkout.mkdir();kotlin(sdkout,os.pathsep.join(sdkdeps),prods)
  fixtures=[p for p in (BASE/'fixtures').rglob('*.java')if p.name!='TestMain.java']
  cmd([JAVA/'javac','--release','21','-proc:none','-cp',cp,'-d',classes,*fixtures]);kotlin(classes,allcp,prods)
  cmd([JAVA/'javac','--release','21','-proc:none','-cp',allcp,'-d',classes,BASE/'fixtures/TestMain.java'])
  p=subprocess.run([str(JAVA/'java'),'-ea','-cp',allcp,'TestMain'],capture_output=True,text=True,timeout=60);(out/'tests.txt').write_text(p.stdout+p.stderr);m=re.search(r'RESULT passes=(\d+) failures=(\d+)',p.stdout);assert m,p.stdout+p.stderr
  after=inputs();report=dict(passed=p.returncode==0 and int(m[2])==0 and int(m[1])+int(m[2])==59 and before==after,tests=dict(passed=int(m[1]),failed=int(m[2])),exit_code=p.returncode,inputs_before=before,inputs_after=after,inputs_unchanged=before==after,dependencies={str(Path(p)):sha(Path(p))for p in [*DEPS,*sdkdeps]},commands=commands,mutation=mutation,sdk37_compiled=sdk,device_status='NOT_TESTED',scope='Full two production Kotlin files, real EzHook1.3.0/API102 chain. Android resources/Looper/views, class resolution/registration backend and OEM body are explicit host fixtures; actual original OTA queue execution is separate. No LSPosed injection or device pass.')
  (out/'report.json').write_text(json.dumps(report,indent=2)+'\n');print(out,report['tests'],flush=True);return report
if __name__=='__main__':
 p=argparse.ArgumentParser();p.add_argument('--output',type=Path,required=True);p.add_argument('--negative-controls',action='store_true');a=p.parse_args();r=run(a.output);assert r['passed']
 if a.negative_controls:
  def negative(name):
   q=run(a.output/name,name,sdk=False);witness=MUTANTS[name][3];seen='FAIL '+witness+':'in(a.output/name/'tests.txt').read_text();return dict(mutation=name,expected_witness=witness,witness_observed=seen,rejected=q['tests']['failed']>0 and q['inputs_unchanged']and seen,report_sha256=sha(a.output/name/'report.json'))
  with concurrent.futures.ThreadPoolExecutor(max_workers=3)as pool:rows=list(pool.map(negative,MUTANTS))
  v=dict(passed=len(rows)==len(MUTANTS)and all(x['rejected']for x in rows),checks=rows);(a.output/'negative-controls.json').write_text(json.dumps(v,indent=2)+'\n');assert v['passed']

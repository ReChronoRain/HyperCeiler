#!/usr/bin/env python3
from pathlib import Path
import argparse,hashlib,json,os,re,subprocess,tempfile

import sys
sys.path.insert(0, str(Path(__file__).resolve().parents[1]))
import hyperceiler_test_paths as test_paths

BASE=Path(__file__).resolve().parent;SOURCE=BASE.parents[1];ROOT=test_paths.TEST_DATA_ROOT;CACHE=test_paths.BUILD_CACHE;JAVA=test_paths.JAVA_HOME / 'bin';KH=test_paths.KOTLIN_HOME
RULE=SOURCE/'library/libhook/src/main/java/com/sevtinge/hyperceiler/libhook/rules/systemui/controlcenter/media3/CustomBackgroundOS4.kt'
ROUTE=RULE.with_name('CustomBackground.kt')
SHADER=RULE.with_name('MediaSeekBarColorOS4.kt')
PEER=test_paths.SNAPSHOT_ROOT / 'source-os4-hyos-test12-20261009-r1/library/libhook/build/intermediates/compile_library_classes_jar/debug/bundleLibCompileToJarDebug/classes.jar'
DEPS=[CACHE/'changed-hooks-compile'/x for x in ['hook-xposed-102-1.3.0.jar','core-1.3.0.jar','api-102.0.0.jar']]+[KH/'lib/kotlin-stdlib.jar',test_paths.ANDROID_HOME / 'platforms/android-37.0/android.jar']
sha=lambda p:hashlib.sha256(p.read_bytes()).hexdigest()
def inputs():return {str(test_paths.report_path(p, ROOT)):sha(p)for p in [RULE,ROUTE,SHADER,test_paths.TEST_DATA_ROOT / 'acceptance/java-test13/ambient-runtime-v1/sdk-first-v1/report.json',Path(__file__),*sorted((BASE/'fixtures').rglob('*'))]if p.is_file()}
def run(out,mutation=None):
 out.mkdir(parents=True,exist_ok=False);before=inputs();body=RULE.read_text();shaderbody=SHADER.read_text();commands=[];logs=''
 if mutation and mutation!='route_OS4_to_legacy':
  target,old,new,witness=MUTANTS[mutation];value=shaderbody if target=='shader' else body;assert value.count(old)==1,(mutation,value.count(old));value=value.replace(old,new)
  if target=='shader':shaderbody=value
  else:body=value
 with tempfile.TemporaryDirectory(prefix='hc-background-')as t:
  w=Path(t);classes=w/'classes';classes.mkdir();cp=os.pathsep.join(map(str,DEPS));allcp=str(classes)+os.pathsep+cp
  def cmd(c):
   nonlocal logs
   commands.append(c);r=subprocess.run(c,capture_output=True,text=True);logs+=r.stdout+r.stderr;(out/'build.txt').write_text(logs);assert r.returncode==0,logs
  files=[p for p in (BASE/'fixtures').rglob('*.java')if p.name not in ['TestMain.java','Targets.java']]
  cmd([str(JAVA/'javac'),'--release','21','-proc:none','-cp',cp,'-d',str(classes),*map(str,files)])
  prod=w/RULE.name;prod.write_text(body);shaderprod=w/SHADER.name;shaderprod.write_text(shaderbody)
  cmd([str(JAVA/'java'),'-cp',str(KH/'lib/kotlin-compiler.jar'),'org.jetbrains.kotlin.cli.jvm.K2JVMCompiler','-kotlin-home',str(KH),'-jvm-target','21','-classpath',allcp,'-d',str(classes),*map(str,(BASE/'fixtures').rglob('*.kt'))])
  cmd([str(JAVA/'java'),'-cp',str(KH/'lib/kotlin-compiler.jar'),'org.jetbrains.kotlin.cli.jvm.K2JVMCompiler','-kotlin-home',str(KH),'-jvm-target','21','-classpath',cp+os.pathsep+str(classes),'-d',str(classes),str(prod),str(shaderprod)])
  sdk_deps=list(json.loads((test_paths.TEST_DATA_ROOT / 'acceptance/java-test13/ambient-runtime-v1/sdk-first-v1/report.json').read_text())['dependencies'])
  routeclasses=w/'routeclasses';routeclasses.mkdir()
  routesrc=w/ROUTE.name;routesrc.write_text(ROUTE.read_text().replace('if (isMoreHyperOSVersion(4f)) {','if (false) {')if mutation=='route_OS4_to_legacy'else ROUTE.read_text())
  cmd([str(JAVA/'java'),'-cp',str(KH/'lib/kotlin-compiler.jar'),'org.jetbrains.kotlin.cli.jvm.K2JVMCompiler','-kotlin-home',str(KH),'-jvm-target','21','-classpath',os.pathsep.join([*sdk_deps,str(classes)]),'-d',str(routeclasses),str(routesrc)])
  allcp=os.pathsep.join([str(classes),str(routeclasses),cp,str(PEER)])
  if not (BASE/'fixtures/TestMain.java').exists(): return
  cmd([str(JAVA/'javac'),'--release','21','-proc:none','-cp',allcp,'-d',str(classes),str(BASE/'fixtures/Targets.java'),str(BASE/'fixtures/TestMain.java')])
  r=subprocess.run([str(JAVA/'java'),'-ea','-Droute.classes='+str(routeclasses),'-cp',allcp,'TestMain'],capture_output=True,text=True,timeout=60);(out/'tests.txt').write_text(r.stdout+r.stderr);print(r.stdout[-1500:]);m=re.search(r'RESULT passes=(\d+) failures=(\d+)',r.stdout);assert m,r.stdout+r.stderr
  after=inputs();report={'passed':r.returncode==0 and int(m[2])==0 and int(m[1])==132 and before==after,'tests':{'passed':int(m[1]),'failed':int(m[2])},'exit_code':r.returncode,'inputs_before':before,'inputs_after':after,'inputs_unchanged':before==after,'dependencies':{str(p):sha(Path(p))for p in [*DEPS,*sdk_deps]},'commands':commands,'mutation':mutation,'device_status':'NOT_TESTED','scope':'Complete production custom-background OS4 adapter with real EzHook 1.3.0/API102 before/after chain. Android views/Looper/drawer, ROM controller bodies, renderer and executor scheduling are explicit fixtures. Resolver and actual Android shader evidence are separate; no device/GPU/LSPosed execution here.'};(out/'report.json').write_text(json.dumps(report,indent=2)+'\n');return report
MUTANTS={
 'wrong_XML_animation_key':('runtime','prefix + "_control_color_anim"','prefix + "_color_anim"','independent_XML_options_including_double_control_color_anim'),
 'skip_NC_layout_writer':('runtime','methods.add(method(layoutOwner, "updateLayout\\$1", Void.TYPE) to Event.LAYOUT)','// lost layout writer','independent_NC_layout_writer_reapplies_then_restores_current_colors'),
 'wrong_host_pair':('runtime','pair = required("kotlin.Pair")','pair = required("android.util.Pair")','DI_only_ignores_NC_shapes'),
 'accept_stale_request':('runtime','session.state.request == ticket && group.media.get(receiver) === data && session.data.get() === data','true','rebind_old_result_cannot_write_new_data'),
 'skip_registration_rollback':('runtime','handle.unhook()','Unit','transaction_failure_2_false'),
 'early_active':('runtime','val active = AtomicBoolean(false)','val active = AtomicBoolean(true)','callbacks_inert_until_last_registration'),
 'lose_screen_receiver_cleanup':('runtime','screenContext.get()?.unregisterReceiver(it)','Unit','one_receiver_per_group_not_per_controller'),
 'omit_transition_suppression':('runtime','original.transitionAlpha = 0f','Unit','opaque_overlay_owns_transition_only_and_restores'),
 'shader_missing_scope_color':('runtime','if (active.get()) shaderColor(groups, seek) else null','null','DI_seekbar_real_level_path_2'),
 'shader_missing_restore':('shader','if (target != null && target.colorFilter === applied) target.colorFilter = previous','if (false) target!!.colorFilter = previous','shader_original_throw_still_restores_original_filter'),
 'shader_overwrites_foreign_filter':('shader','target != null && target.colorFilter === applied','target != null','shader_foreign_filter_write_not_overwritten'),
 'shader_excludes_middle':('shader','level.getInt(param.thisObject) !in 1..2','level.getInt(param.thisObject) != 2','DI_seekbar_real_level_path_1'),
 'route_OS4_to_legacy':('route','','','actual_route_OS4_early_branch_never_evaluates_legacy_targets'),
}
if __name__=='__main__':
 p=argparse.ArgumentParser();p.add_argument('--output',type=Path,required=True);p.add_argument('--negative-controls',action='store_true');a=p.parse_args();r=run(a.output);assert r is None or r['passed']
 if a.negative_controls:
  rows=[]
  for m in MUTANTS:
   q=run(a.output/m,m);witness=MUTANTS[m][3];observed='FAIL '+witness+':' in (a.output/m/'tests.txt').read_text();rows.append({'mutation':m,'expected_witness':witness,'witness_observed':observed,'rejected':not q['passed'] and q['tests']['failed']>0 and q['inputs_unchanged'] and observed,'failed_tests':q['tests']['failed']})
  v={'passed':bool(rows)and all(x['rejected']for x in rows),'checks':rows};(a.output/'negative-controls.json').write_text(json.dumps(v,indent=2)+'\n');assert v['passed']

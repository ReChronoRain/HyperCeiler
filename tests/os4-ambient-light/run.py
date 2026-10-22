#!/usr/bin/env python3
from pathlib import Path
import argparse,hashlib,json,os,re,subprocess,tempfile

import sys
sys.path.insert(0, str(Path(__file__).resolve().parents[1]))
import hyperceiler_test_paths as test_paths

BASE=Path(__file__).resolve().parent;SOURCE=BASE.parents[1];ROOT=test_paths.TEST_DATA_ROOT;CACHE=test_paths.BUILD_CACHE;JAVA=test_paths.JAVA_HOME / 'bin';KH=test_paths.KOTLIN_HOME
RULE=SOURCE/'library/libhook/src/main/java/com/sevtinge/hyperceiler/libhook/rules/systemui/controlcenter/media3/AmbientLightOS4.kt'
ROUTE=RULE.with_name('AmbientLight.kt')
PEER=test_paths.SNAPSHOT_ROOT / 'source-os4-hyos-test12-20261009-r1/library/libhook/build/intermediates/compile_library_classes_jar/debug/bundleLibCompileToJarDebug/classes.jar'
DEPS=[CACHE/'changed-hooks-compile'/x for x in ['hook-xposed-102-1.3.0.jar','core-1.3.0.jar','api-102.0.0.jar']]+[KH/'lib/kotlin-stdlib.jar',test_paths.ANDROID_HOME / 'platforms/android-37.0/android.jar']
sha=lambda p:hashlib.sha256(p.read_bytes()).hexdigest()
def inputs():return {str(test_paths.report_path(p, ROOT)):sha(p)for p in [RULE,ROUTE,test_paths.TEST_DATA_ROOT / 'acceptance/java-test13/ambient-runtime-v1/sdk-first-v1/report.json',Path(__file__),*sorted((BASE/'fixtures').rglob('*'))]if p.is_file()}
def run(out,mutation=None):
 out.mkdir(parents=True,exist_ok=False);before=inputs();body=RULE.read_text();commands=[];logs=''
 if mutation and mutation!='route_OS4_to_legacy':
  old,new=MUTANTS[mutation];assert body.count(old)==1,(mutation,body.count(old));body=body.replace(old,new)
 with tempfile.TemporaryDirectory(prefix='hc-ambient-')as t:
  w=Path(t);classes=w/'classes';classes.mkdir();cp=os.pathsep.join(map(str,DEPS));allcp=str(classes)+os.pathsep+cp
  def cmd(c):
   nonlocal logs
   commands.append(c);r=subprocess.run(c,capture_output=True,text=True);logs+=r.stdout+r.stderr;(out/'build.txt').write_text(logs);assert r.returncode==0,logs
  files=[p for p in (BASE/'fixtures').rglob('*.java')if p.name not in ['TestMain.java','Targets.java']]
  cmd([str(JAVA/'javac'),'--release','21','-proc:none','-cp',cp,'-d',str(classes),*map(str,files)])
  prod=w/RULE.name;prod.write_text(body)
  cmd([str(JAVA/'java'),'-cp',str(KH/'lib/kotlin-compiler.jar'),'org.jetbrains.kotlin.cli.jvm.K2JVMCompiler','-kotlin-home',str(KH),'-jvm-target','21','-classpath',allcp,'-d',str(classes),str(prod),*map(str,(BASE/'fixtures').rglob('*.kt'))])
  sdk_deps=list(json.loads((test_paths.TEST_DATA_ROOT / 'acceptance/java-test13/ambient-runtime-v1/sdk-first-v1/report.json').read_text())['dependencies'])
  routeclasses=w/'routeclasses';routeclasses.mkdir()
  routesrc=w/ROUTE.name;routesrc.write_text(ROUTE.read_text().replace('if (isMoreHyperOSVersion(4f)) {','if (false) {')if mutation=='route_OS4_to_legacy'else ROUTE.read_text())
  cmd([str(JAVA/'java'),'-cp',str(KH/'lib/kotlin-compiler.jar'),'org.jetbrains.kotlin.cli.jvm.K2JVMCompiler','-kotlin-home',str(KH),'-jvm-target','21','-classpath',os.pathsep.join([*sdk_deps,str(classes)]),'-d',str(routeclasses),str(routesrc)])
  allcp=os.pathsep.join([str(classes),str(routeclasses),cp,str(PEER)])
  if not (BASE/'fixtures/TestMain.java').exists(): return
  cmd([str(JAVA/'javac'),'--release','21','-proc:none','-cp',allcp,'-d',str(classes),str(BASE/'fixtures/Targets.java'),str(BASE/'fixtures/TestMain.java')])
  r=subprocess.run([str(JAVA/'java'),'-ea','-cp',allcp,'TestMain'],capture_output=True,text=True,timeout=60);(out/'tests.txt').write_text(r.stdout+r.stderr);print(r.stdout[-1500:]);m=re.search(r'RESULT passes=(\d+) failures=(\d+)',r.stdout);assert m,r.stdout+r.stderr
  after=inputs();report={'passed':r.returncode==0 and int(m[2])==0 and int(m[1])==97 and before==after,'tests':{'passed':int(m[1]),'failed':int(m[2])},'exit_code':r.returncode,'inputs_before':before,'inputs_after':after,'inputs_unchanged':before==after,'dependencies':{str(p):sha(Path(p))for p in [*DEPS,*sdk_deps]},'commands':commands,'mutation':mutation,'device_status':'NOT_TESTED','scope':'Complete production OS4 adapter with real EzHook 1.3.0/API102 before/after chain. Android views/Looper/shader, ROM controller bodies, resolver and executor scheduling are explicit fixtures. Resolver and actual Android shader evidence are separate; no device/GPU/LSPosed execution here.'};(out/'report.json').write_text(json.dumps(report,indent=2)+'\n');return report
MUTANTS={
 'canonical_key_regression':('PrefsBridge.getStringAsInt(DP + "_ambient_light", 0)','PrefsBridge.getStringAsInt(DP + "_ambient_light_type", 0)'),
 'drop_dummy_holder':('listOf("holder", "dummyHolder")','listOf("holder")'),
 'active_during_registration':('val active = AtomicBoolean(false)','val active = AtomicBoolean(true)'),
 'retain_failed_registration':('try { handle.unhook() }','try { /* no rollback */ }'),
 'detach_rereads_cleared_holder':('frame.session?.let(::retire)','if (group.actualHolders(owner).any { it != null }) frame.session?.let(::retire)'),
 'lose_original_outline':('view.outlineProvider = original.outlineProvider','/* wrong default outline */'),
 'stale_colors_on_failure':('session.colors = null\n                    cleanAll(session.slots.asReversed().map { slot -> { undo(slot) } })','/* retain previous media colors */'),
 'cleanup_stops_on_first_error':('failure.addSuppressed(error)','throw error'),
 'leak_busy_on_pair_mismatch':('frame?.let(::releaseFrame)\n                stack.forEach(::releaseFrame)','/* lost frames keep their busy counters */'),
 'ignore_current_AOD':('fun fullAod(): Boolean = aod?.getBoolean(null) == true','fun fullAod(): Boolean = false'),
 'allow_drag_late_resume':('(!session.dragging || slot.role != 0)','true'),
 'ignore_dummy_translation':('else if (slot.role == 1) view.translationY = group.offset!!.getFloat(owner)','/* missing dummy offset */'),
 'NC_fills_parent_instead_of_original':('params.width = original.width; params.height = original.height; view.layoutParams = params','params.width = -1; params.height = -1; view.layoutParams = params'),
 'drop_original_exception_check':('param.isSkipped || param.hasThrowable','param.isSkipped'),
 'drop_cached_media_at_attach':('if (data != null && data.javaClass == group.resolver!!.mediaDataClass && current(session, owner)) {','if (data != null && false) {'),
 'ignore_successful_null_bind':('if (group.media!!.get(owner) == null) clearMedia(session, owner)','/* null bind leaves old palette running */'),
 'route_OS4_to_legacy':None,
}

if __name__=='__main__':
 p=argparse.ArgumentParser();p.add_argument('--output',type=Path,required=True);p.add_argument('--negative-controls',action='store_true');a=p.parse_args();r=run(a.output);assert r is None or r['passed']
 if a.negative_controls:
  rows=[]
  for m in MUTANTS:
   q=run(a.output/m,m);rows.append({'mutation':m,'rejected':not q['passed'] and q['tests']['failed']>0 and q['inputs_unchanged'],'failed_tests':q['tests']['failed']})
  v={'passed':bool(rows)and all(x['rejected']for x in rows),'checks':rows,'scope':'Production source-copy mutations; actual files unchanged.'};(a.output/'negative-controls.json').write_text(json.dumps(v,indent=2)+'\n');assert v['passed']

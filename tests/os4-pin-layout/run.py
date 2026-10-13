#!/usr/bin/env python3
from pathlib import Path
import argparse,hashlib,json,os,re,subprocess,tempfile

import sys
sys.path.insert(0, str(Path(__file__).resolve().parents[1]))
import hyperceiler_test_paths as test_paths

BASE=Path(__file__).resolve().parent;SOURCE=BASE.parents[1];ROOT=test_paths.TEST_DATA_ROOT;CACHE=test_paths.BUILD_CACHE;JAVA=test_paths.JAVA_HOME / 'bin'
RULE=SOURCE/'library/libhook/src/main/java/com/sevtinge/hyperceiler/libhook/rules/systemui/lockscreen/ScramblePIN.java'
DEPS=[CACHE/'changed-hooks-compile'/x for x in['hook-xposed-102-1.3.0.jar','api-102.0.0.jar']]+[test_paths.KOTLIN_HOME / 'lib/kotlin-stdlib.jar',test_paths.ANDROID_HOME / 'platforms/android-37.0/android.jar']
sha=lambda p:hashlib.sha256(p.read_bytes()).hexdigest()
def inputs():return{str(test_paths.report_path(p, ROOT)):sha(p)for p in[RULE,Path(__file__),*sorted((BASE/'fixtures').rglob('*.java'))]}
MUTANTS={
 'old_missing_class':('Class<?> pin = findClassIfExists("com.android.keyguard.KeyguardPINView");','Class<?> pin = findClassIfExists("com.android.keyguard.widget.MiuiKeyguardPINView");'),
 'drop_bottom_animation':('numbers[3][1] = shuffled.get(9);','/* omit bottom animation matrix */'),
 'break_shared_rows':('views[row][column] = shuffled.get(index++);','{ views[row] = views[row].clone(); views[row][column] = shuffled.get(index++); }'),
 'drop_alias_guard':('|| numbers[row] != views[row + 1]',''),
 'drop_digit_guard':('value < 0 || value > 9 || digits[value]','value < 0 || value > 9'),
 'drop_shuffle':('Collections.shuffle(shuffled);','/* no shuffle */'),
 'drop_rollback':('try { moveDigits(rows, original, original, positions); }','try { /* no restore */ }'),
 'drop_subclass_guard':('param.getHasThrowable() || param.getThisObject().getClass() != pin','param.getHasThrowable()'),
 'drop_original_throw_guard':('param.getHasThrowable() || param.getThisObject().getClass() != pin','param.getThisObject().getClass() != pin'),
 'drop_os4_gate':('if (isMoreHyperOSVersion(4f)) {','if (true) {'),
}
def run(out,mut=None):
 out.mkdir(parents=True,exist_ok=False);before=inputs();body=RULE.read_text()
 if mut:
  a,b=MUTANTS[mut];assert body.count(a)==1;body=body.replace(a,b)
 with tempfile.TemporaryDirectory(prefix='hc-pin-')as t:
  w=Path(t);prod=w/RULE.name;prod.write_text(body);cp=os.pathsep.join(map(str,DEPS));cmd=[str(JAVA/'javac'),'--release','21','-proc:none','-cp',cp,'-d',str(w/'classes'),str(prod),*map(str,(BASE/'fixtures').rglob('*.java'))];z=subprocess.run(cmd,capture_output=True,text=True);(out/'compile.txt').write_text(z.stdout+z.stderr);assert z.returncode==0,z.stdout+z.stderr
  z=subprocess.run([str(JAVA/'java'),'-ea','-cp',str(w/'classes')+os.pathsep+cp,'TestMain'],capture_output=True,text=True,timeout=60);(out/'tests.txt').write_text(z.stdout+z.stderr);print(z.stdout[-200:]);m=re.search(r'RESULT passes=(\d+) failures=(\d+)',z.stdout);assert m,z.stdout+z.stderr
  after=inputs();r={'passed':z.returncode==0 and int(m[2])==0 and before==after,'tests':{'passed':int(m[1]),'failed':int(m[2])},'inputs_before':before,'inputs_after':after,'inputs_unchanged':before==after,'mutation':mut,'dependencies':{str(p):sha(p)for p in DEPS},'command':cmd,'scope':'Whole production Java through actual EzHook1.3.0/API102 chain. Android hierarchy, listeners, credential comparison and original inflate are ROM-shaped host fixtures, not ART/LockSettingsService/Gatekeeper/device execution. Randomization property checks50 draws; no assertion that every draw differs.','device_status':'NOT_TESTED'};(out/'report.json').write_text(json.dumps(r,indent=2)+'\n');return r
if __name__=='__main__':
 p=argparse.ArgumentParser();p.add_argument('--output',type=Path,required=True);p.add_argument('--negative-controls',action='store_true');a=p.parse_args();r=run(a.output);assert r['passed']
 if a.negative_controls:
  checks=[]
  for m in MUTANTS:
   q=run(a.output/m,m);checks.append({'mutation':m,'rejected':not q['passed']and q['tests']['failed']>0 and q['inputs_unchanged'],'failed_tests':q['tests']['failed']})
  v={'passed':all(x['rejected']for x in checks),'checks':checks,'scope':'Source-copy regressions; original production unchanged.'};(a.output/'negative-controls.json').write_text(json.dumps(v,indent=2)+'\n');assert v['passed']

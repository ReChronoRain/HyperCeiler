#!/usr/bin/env python3
from pathlib import Path
import argparse,hashlib,json,os,re,subprocess,tempfile

import sys
sys.path.insert(0, str(Path(__file__).resolve().parents[1]))
import hyperceiler_test_paths as test_paths

HERE=Path(__file__).resolve().parent;ROOT=test_paths.TEST_DATA_ROOT;CACHE=test_paths.BUILD_CACHE;JAVA=test_paths.JAVA_HOME / 'bin';KH=test_paths.KOTLIN_HOME;SOURCE=test_paths.SOURCE_ROOT / 'library/libhook/src/main/java/com/sevtinge/hyperceiler/libhook/rules/systemui/controlcenter/media3/CustomBackgroundRendererOS4.kt';CONFIG=test_paths.SOURCE_ROOT / 'library/libhook/src/main/java/com/sevtinge/hyperceiler/libhook/utils/hookapi/systemui/controlcenter/media/MediaViewColorConfig.kt';sha=lambda p:hashlib.sha256(p.read_bytes()).hexdigest()
MUTANTS={
 'wrong_palette_style':('newInstance(wallpaper, true, 6)','newInstance(wallpaper, true, 5)'),
 'drop_four_palette_size':('entries.size != 13 || entries.any { it !is Int }','entries.any { it !is Int }'),
 'mix_neutral_foreground':('MediaViewColorConfig(neutral1[1], neutral2[3], accent2[9], accent1[9])','MediaViewColorConfig(neutral1[2], neutral2[3], accent2[9], accent1[9])'),
 'ignore_per_request_inverse':('inverse && brightness(image) >= 192f','false && brightness(image) >= 192f'),
 'blur_zero_still_uses_gpu':('if (blur == 0) masked else hardwareBlur','if (false) masked else hardwareBlur'),
 'missing_gpu_failure_cleanup':('cleanup { reader?.close() }','/* fault: omit reader cleanup */'),
 'mainthread_allowed':('Looper.myLooper() === Looper.getMainLooper() ||','false ||'),
 'missing_cross_user_fallback':('acquireIcon.invoke(null, context, mediaData)','null'),
 'mutable_output_published':('val background = processed.copy(Bitmap.Config.ARGB_8888, false)','val background = processed.copy(Bitmap.Config.ARGB_8888, true)'),
}
def inputs():return{str(test_paths.report_path(p, ROOT)):sha(p)for p in [SOURCE,CONFIG,Path(__file__),*sorted((HERE/'fixtures').rglob('*'))]if p.is_file()}
def run(out,mutation=None):
 out.mkdir(parents=True,exist_ok=False);before=inputs();body=SOURCE.read_text();commands=[]
 if mutation:
  old,new=MUTANTS[mutation];assert body.count(old)==1,(mutation,body.count(old));body=body.replace(old,new)
 def command(cmd,name):
  cmd=list(map(str,cmd));commands.append(cmd);r=subprocess.run(cmd,capture_output=True,text=True,timeout=90);(out/name).write_text(r.stdout+r.stderr);assert r.returncode==0,(name,r.stdout+r.stderr);return r
 with tempfile.TemporaryDirectory(prefix='hc-background-renderer-')as t:
  w=Path(t);classes=w/'classes';classes.mkdir();cp=os.pathsep.join(map(str,[classes,KH/'lib/kotlin-stdlib.jar']))
  k=[JAVA/'java','-cp',KH/'lib/kotlin-compiler.jar','org.jetbrains.kotlin.cli.jvm.K2JVMCompiler','-kotlin-home',KH,'-jvm-target','21','-classpath',cp,'-d',classes]
  command([*k,CONFIG],'config-build.txt')
  command([JAVA/'javac','--release','21','-proc:none','-cp',cp,'-d',classes,*sorted((HERE/'fixtures').rglob('*.java'))],'java-build.txt')
  prod=w/SOURCE.name;prod.write_text(body);command([*k,prod,HERE/'fixtures/TestMain.kt'],'kotlin-build.txt')
  cmd=[str(JAVA/'java'),'-ea','-cp',cp,'TestMainKt'];commands.append(cmd);r=subprocess.run(cmd,capture_output=True,text=True,timeout=90);(out/'tests.txt').write_text(r.stdout+r.stderr);m=re.search(r'RESULT passes=(\d+) failures=(\d+)',r.stdout);assert m,r.stdout+r.stderr
  after=inputs();d={'passed':r.returncode==0 and int(m[2])==0 and before==after,'tests':{'passed':int(m[1]),'failed':int(m[2])},'exit_code':r.returncode,'inputs_before':before,'inputs_after':after,'inputs_unchanged':before==after,'commands':commands,'mutation':mutation,'device_status':'NOT_TESTED','scope':'Complete production renderer and MediaViewColorConfig; Android graphics, palette and output drawable are host boundary adapters. Resource-fault/ownership/typed-dependency tests do not prove real GPU output, which has a separate actual Android/OTA probe.'};(out/'report.json').write_text(json.dumps(d,indent=2)+'\n');print(out,d['tests']);return d
if __name__=='__main__':
 p=argparse.ArgumentParser();p.add_argument('--output',type=Path,required=True);p.add_argument('--negative-controls',action='store_true');a=p.parse_args();d=run(a.output);assert d['passed']
 if a.negative_controls:
  checks=[]
  for name in MUTANTS:
   d=run(a.output/name,name);checks.append({'name':name,'passed':not d['passed']and d['tests']['failed']>0 and d['inputs_unchanged'],'failed_tests':d['tests']['failed']})
  (a.output/'negative-controls.json').write_text(json.dumps({'passed':all(x['passed']for x in checks),'checks':checks,'scope':'Compiled copy of renderer, never production edit; actual runtime assertions must fail, compiler failure is not a witness.'},indent=2)+'\n');assert all(x['passed']for x in checks)

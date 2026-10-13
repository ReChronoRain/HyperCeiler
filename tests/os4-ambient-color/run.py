#!/usr/bin/env python3
from pathlib import Path
import argparse,hashlib,json,os,re,subprocess,tempfile

import sys
sys.path.insert(0, str(Path(__file__).resolve().parents[1]))
import hyperceiler_test_paths as test_paths

HERE=Path(__file__).resolve().parent;ROOT=test_paths.TEST_DATA_ROOT;CACHE=test_paths.BUILD_CACHE;JAVA=test_paths.JAVA_HOME / 'bin';KH=test_paths.KOTLIN_HOME
SOURCE=test_paths.SOURCE_ROOT / 'library/libhook/src/main/java/com/sevtinge/hyperceiler/libhook/rules/systemui/controlcenter/media3/AmbientColorResolverOS4.kt';NATIVE_SOURCE=SOURCE.with_name('AmbientNativeColorOS4.kt');sha=lambda p:hashlib.sha256(p.read_bytes()).hexdigest()
def inputs():return{str(test_paths.report_path(p, ROOT)):sha(p)for p in [SOURCE,NATIVE_SOURCE,Path(__file__),HERE/'native.cpp',*sorted((HERE/'fixtures').rglob('*'))]if p.is_file()}
def run(out,mutation=None):
 out.mkdir(parents=True,exist_ok=False);before=inputs();body=SOURCE.read_text();commands=[]
 if mutation:
  old,new=MUTANTS[mutation];assert body.count(old)==1,(mutation,body.count(old));body=body.replace(old,new)
 def command(cmd,name):
  commands.append(list(map(str,cmd)));r=subprocess.run(cmd,capture_output=True,text=True,timeout=90);(out/name).write_text(r.stdout+r.stderr);assert r.returncode==0,(name,r.stdout+r.stderr);return r
 with tempfile.TemporaryDirectory(prefix='hc-ambient-colors-')as t:
  w=Path(t);classes=w/'classes';classes.mkdir();so=w/'libHyperCeilerAmbientOS4.so';jdk=JAVA.parent
  command(['g++','-shared','-fPIC','-std=c++20','-Wall','-Wextra','-Werror','-I'+str(jdk/'include'),'-I'+str(jdk/'include/linux'),str(HERE/'native.cpp'),'-o',str(so)],'jni-build.txt')
  command([JAVA/'javac','--release','21','-proc:none','-d',classes,*sorted((HERE/'fixtures').rglob('*.java'))],'java-build.txt')
  prod=w/SOURCE.name;prod.write_text(body);command([JAVA/'java','-cp',KH/'lib/kotlin-compiler.jar','org.jetbrains.kotlin.cli.jvm.K2JVMCompiler','-kotlin-home',KH,'-jvm-target','21','-classpath',classes,'-d',classes,prod,NATIVE_SOURCE,HERE/'fixtures/TestMain.kt'],'kotlin-build.txt')
  cp=os.pathsep.join(map(str,[classes,KH/'lib/kotlin-stdlib.jar']));cmd=[str(JAVA/'java'),'-ea','-Djava.library.path='+str(w),'-Dfixture.native='+str(so),'-cp',cp,'TestMainKt'];commands.append(cmd);r=subprocess.run(cmd,capture_output=True,text=True,timeout=90);(out/'tests.txt').write_text(r.stdout+r.stderr);m=re.search(r'RESULT passes=(\d+) failures=(\d+)',r.stdout);assert m,r.stdout+r.stderr;after=inputs();d={'passed':r.returncode==0 and int(m[2])==0 and before==after,'tests':{'passed':int(m[1]),'failed':int(m[2])},'exit_code':r.returncode,'inputs_before':before,'inputs_after':after,'inputs_unchanged':before==after,'commands':commands,'mutation':mutation,'device_status':'NOT_TESTED','scope':'Complete production Kotlin. Android graphics/palette objects and the original pixel sampler are host adapters/transcription; actual typed native method dispatch uses a JNI recording function, not the proprietary ROM color algorithm. No artwork rendering, Android view/ART or phone PASS.'};(out/'report.json').write_text(json.dumps(d,indent=2)+'\n');print(out,d['tests']);return d
MUTANTS={
 'use_wrong_content_style':('newInstance(colors, true, 6)','newInstance(colors, true, 5)'),
 'swap_light_index':('Colors(shades[4] as Int, shades[7] as Int)','Colors(shades[7] as Int, shades[7] as Int)'),
 'wrong_dark_scheme':('newInstance(colors, true, 6)','newInstance(colors, false, 6)'),
 'drop_palette_size':('shades.size != 13 || shades.any { it !is Int }','shades.any { it !is Int }'),
 'accept_filtered_shades':('shades.any { it !is Int }','false'),
 'half_capacity':('IntArray(capacity)','IntArray(capacity / 2)'),
 'wrong_sampling_filter_dimensions':('ceil((width.toFloat() * scale).toDouble())','kotlin.math.floor((width.toFloat() * scale).toDouble())'),
 'drop_sample_dimension_check':('sampled.width != expectedWidth || sampled.height != expectedHeight ||','false ||'),
 'drop_late_max_area':('api.maxArea.getInt(null) != maxArea ||','false ||'),
 'omit_main_thread_guard':('Looper.myLooper() === Looper.getMainLooper() ||','false ||'),
 'borrow_artwork_bitmap':('val copy = bitmap.copy(Bitmap.Config.ARGB_8888, false) ?: return null','val copy = bitmap'),
 'allow_original_drawable_alias':('if (created === original) return null','if (created === original) return original'),
 'allow_original_mutate_alias':('if (private === original) return null','if (false) return null'),
 'skip_dimensionless_snapshot':('if (drawable.intrinsicWidth <= 0 || drawable.intrinsicHeight <= 0) {','if (false) {'),
 'skip_quiet_image':('val quiet = getter.invoke(original) as? Drawable','val quiet: Drawable? = null'),
 'ignore_maml_quiet_guard':('if (quietGetter != null) {','if (false) {'),
 'skip_cross_user_fallback':('acquireIcon.invoke(null, context, mediaData)','null'),
}
if __name__=='__main__':
 p=argparse.ArgumentParser();p.add_argument('--output',required=True,type=Path);p.add_argument('--negative-controls',action='store_true');a=p.parse_args();r=run(a.output);assert r['passed']
 if a.negative_controls:
  rows=[]
  for m in MUTANTS:
   q=run(a.output/m,m);rows.append({'mutation':m,'rejected':not q['passed']and q['tests']['failed']>0 and q['inputs_unchanged'],'failed_tests':q['tests']['failed']})
  d={'passed':all(r['rejected']for r in rows),'checks':rows,'scope':'Compiled source copies, production source unchanged.'};(a.output/'negative-controls.json').write_text(json.dumps(d,indent=2)+'\n');assert d['passed']
